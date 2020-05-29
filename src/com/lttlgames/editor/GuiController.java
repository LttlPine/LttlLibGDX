package com.lttlgames.editor;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglCanvas;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntMap;
import com.lttlgames.editor.LttlObjectGraphCrawler.FieldsMode;
import com.lttlgames.editor.annotations.GuiHideComponentList;
import com.lttlgames.editor.annotations.GuiShow;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.helpers.LttlHelper;
import com.lttlgames.helpers.LttlObjectHelper;

public class GuiController
{
	private JFrame frame;
	private LwjglCanvas lwjglCanvas;
	private JSplitPane mainSplitPane;
	private JSplitPane rightSplit;
	JPanel leftPanel;
	JPanel canvasPanel;
	private GuiSelectionController selectionController;
	private GuiPropertiesController propertiesController;
	private GuiStatusBarController statusBarController;
	private GuiMenuBarController menuBarController;
	private int copyComponentId;
	private boolean isCut;
	private IntArray copyTransformIds = new IntArray();
	IntArray moveTransformIds = new IntArray();
	private int captureTransformId = -1;
	private ArrayList<GuiSelectOptionContainer> componentOptions;
	ArrayList<GuiAnimationEditor> animEditors = new ArrayList<GuiAnimationEditor>();
	ArrayList<JMenuItem> rightClickMenuItems = new ArrayList<JMenuItem>();

	boolean disableGuiRefresh = false;

	ArrayList<GuiFieldObject<?>> saveGFOs = new ArrayList<GuiFieldObject<?>>();

	/**
	 * All the component classes, regardless if they show up in the gui dropdown or not.
	 */
	private ArrayList<Class<? extends LttlComponent>> componentClasses;

	private Color tmpColor = new Color();

	private boolean initialized = false;

	GuiController(ApplicationListener listener,
			LwjglApplicationConfiguration cfg)
	{
		frame = new JFrame("LttlEngine");

		Lttl.editor = new LttlEditor(this);

		lwjglCanvas = new LwjglCanvas(listener, cfg);

		frame.addWindowListener(new WindowAdapter()
		{
			public void windowClosed(WindowEvent event)
			{
				// lwjglCanvas.stop();
				System.exit(0);
				// Gdx.app.quit();
			}
		});

		createEssentials();

		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setVisible(true);

		initComponentClasses();
		// only needs to be done in editor/gui
		checkPersistAnnotations();
	}

	private void initComponentClasses()
	{
		IntMap<Class<?>> classMap = LttlGameStarter.get().getClassMap();
		componentOptions = new ArrayList<GuiSelectOptionContainer>();
		componentClasses = new ArrayList<Class<? extends LttlComponent>>();
		for (Class<?> clazz : classMap.values())
		{
			if (clazz == null) continue;
			if (!LttlComponent.class.isAssignableFrom(clazz)) continue;
			Class<? extends LttlComponent> compClass = (Class<? extends LttlComponent>) clazz;
			if (clazz.getEnclosingClass() != null) continue;
			componentClasses.add(compClass);
			if (clazz.isAnnotationPresent(GuiHideComponentList.class))
				continue;
			if (Modifier.isAbstract(clazz.getModifiers())) continue;
			componentOptions.add(new GuiSelectOptionContainer(clazz, clazz
					.getSimpleName()));
		}

		/*
		 * Deprecated, required reflections-.9.9-RC1.jar
		 */
		// Reflections lttlGamesReflections = new Reflections(
		// ConfigurationBuilder.build("com.lttlgames", LttlGameStarter
		// .get().getProjectPackageName()));
		// Set<Class<? extends LttlComponent>> subTypes = lttlGamesReflections
		// .getSubTypesOf(LttlComponent.class);
		//
		// componentOptions = new ArrayList<GuiSelectOptionContainer>();
		// componentClasses = new ArrayList<Class<? extends LttlComponent>>();
		// for (Iterator<Class<? extends LttlComponent>> it = subTypes.iterator(); it
		// .hasNext();)
		// {
		// Class<? extends LttlComponent> clazz = it.next();
		// if (clazz.getEnclosingClass() != null) continue;
		// componentClasses.add(clazz);
		// if (clazz.isAnnotationPresent(GuiHideComponentList.class))
		// continue;
		// if (Modifier.isAbstract(clazz.getModifiers())) continue;
		// componentOptions.add(new GuiSelectOptionContainer(clazz, clazz
		// .getSimpleName()));
		// }
	}

	/**
	 * Creates the initial components of the GUI, sets canvas
	 */
	private void createEssentials()
	{
		// MAIN SPLIT PANE
		mainSplitPane = new JSplitPane();
		frame.getContentPane().add(mainSplitPane, BorderLayout.CENTER);

		// RIGHT SPLIT PANE
		rightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		mainSplitPane.add(rightSplit, JSplitPane.RIGHT);

		// LEFT SPLIT, which only contains canvas;
		leftPanel = new JPanel(new BorderLayout());
		canvasPanel = new JPanel(new BorderLayout());
		canvasPanel.add(lwjglCanvas.getCanvas());
		leftPanel.add(canvasPanel);
		mainSplitPane.add(leftPanel, JSplitPane.LEFT);

		// create selection controller
		// create properties controller
		initIndependent();
	}

	/**
	 * Initializes and sets up the gui components, after world loads
	 */
	void initializeComponents()
	{
		initialized = true;

		// set window size
		frame.setSize(Lttl.editor.getSettings().guiWindowSizeX,
				Lttl.editor.getSettings().guiWindowSizeY);
		frame.setLocation((int) Lttl.editor.getSettings().guiWindowLoc.x,
				(int) Lttl.editor.getSettings().guiWindowLoc.y);

		// set canvas panel size
		mainSplitPane
				.setDividerLocation(Lttl.editor.getSettings().guiCanvasPanelSize);

		// Window resize listener
		frame.getContentPane().addComponentListener(new GuiComponentListener()
		{
			int lastFrameWidth = frame.getSize().width;

			@Override
			public void componentResized(ComponentEvent e)
			{
				// keeps the right panel fixed size, and canvas panel (left) dynamic
				// calculate and save new guiCanvasPanelSize based on window change
				Lttl.editor.getSettings().guiCanvasPanelSize += frame.getSize().width
						- lastFrameWidth;

				// set the new gui panel size
				mainSplitPane.setDividerLocation(Lttl.editor.getSettings().guiCanvasPanelSize);

				// save new window dimensions
				Lttl.editor.getSettings().guiWindowSizeX = frame.getSize().width;
				Lttl.editor.getSettings().guiWindowSizeY = frame.getSize().height;
				lastFrameWidth = frame.getSize().width;
			}
		});

		frame.addComponentListener(new GuiComponentListener()
		{
			@Override
			public void componentMoved(ComponentEvent e)
			{
				Lttl.editor.getSettings().guiWindowLoc.set(
						frame.getLocation().x, frame.getLocation().y);
			}
		});

		// ** MAIN SPLIT PANE
		// set divider size
		mainSplitPane.setDividerSize(20);
		mainSplitPane.setUI(new BasicSplitPaneUI()
		{
			private boolean painted;

			public void paint(Graphics g, JComponent jc)
			{
				if (!painted)
				{
					painted = true;
				}
			}
		});

		// listen for when divider location changes and save value
		mainSplitPane.addPropertyChangeListener(
				JSplitPane.DIVIDER_LOCATION_PROPERTY,
				new PropertyChangeListener()
				{
					private boolean init = false;

					@Override
					public void propertyChange(PropertyChangeEvent evt)
					{
						// so doesn't fire on init
						if (!init)
						{
							init = true;
							return;
						}

						// keep the transform tree width fixed
						Integer diff = Lttl.editor.getSettings().guiCanvasPanelSize
								- mainSplitPane.getDividerLocation();
						Integer newDividerLocation = rightSplit
								.getDividerLocation() + diff;
						if (newDividerLocation > 100)
						{
							rightSplit.setDividerLocation(rightSplit
									.getDividerLocation() + diff);
						}

						Lttl.editor.getSettings().guiCanvasPanelSize = mainSplitPane
								.getDividerLocation();
					}
				});

		// ** RIGHT SPLIT PANE
		{
			rightSplit.setUI(new BasicSplitPaneUI()
			{
				public void paint(Graphics g, JComponent jc)
				{
				}
			});
			rightSplit.setDividerSize(4);
			rightSplit.setOneTouchExpandable(true);
			rightSplit
					.setDividerLocation(Lttl.editor.getSettings().guiRightPaneDividerLocation);
			rightSplit.addPropertyChangeListener("dividerLocation",
					new PropertyChangeListener()
					{
						private boolean init = false;

						@Override
						public void propertyChange(PropertyChangeEvent evt)
						{
							// so doesn't fire on init
							if (!init)
							{
								init = true;
								return;
							}
							Lttl.editor.getSettings().guiRightPaneDividerLocation = rightSplit
									.getDividerLocation();
						}
					});

			// TRANSFORM TREE
			{
				rightSplit
						.add(selectionController.getPanel(), JSplitPane.RIGHT);
				// JPanel propertiesPanel = new JPanel(new GridBagLayout());
				// rightSplit.add(propertiesPanel, JSplitPane.TOP);
				// propertiesPanel.setBorder(new CompoundBorder(BorderFactory
				// .createEmptyBorder(3, 0, 6, 6), BorderFactory
				// .createTitledBorder("Editor Properties")));
				// {
				// JScrollPane scroll = new JScrollPane();
				// propertiesPanel.add(scroll, new GridBagConstraints(0, 0, 1,
				// 1, 1, 1, GridBagConstraints.NORTH,
				// GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0,
				// 0));
				// scroll.setBorder(BorderFactory
				// .createEmptyBorder(0, 0, 0, 0));
				// {
				// editRowsPanel = new JPanel(new GridBagLayout());
				// scroll.setViewportView(editRowsPanel);
				// scroll.getVerticalScrollBar().setUnitIncrement(70);
				// }
				// }
			}

			// PROPERTIES
			{
				rightSplit.add(getPropertiesController().getPanel(),
						JSplitPane.LEFT);
				// JPanel propertiesPanel = new JPanel(new GridBagLayout());
				// rightSplit.add(propertiesPanel, JSplitPane.RIGHT);
				// propertiesPanel.setBorder(new CompoundBorder(BorderFactory
				// .createEmptyBorder(3, 0, 6, 6), BorderFactory
				// .createTitledBorder("Emitter Properties")));
				// {
				// JScrollPane scroll = new JScrollPane();
				// propertiesPanel.add(scroll, new GridBagConstraints(0, 0, 1,
				// 1, 1, 1, GridBagConstraints.NORTH,
				// GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0,
				// 0));
				// scroll.setBorder(BorderFactory
				// .createEmptyBorder(0, 0, 0, 0));
				// {
				// rowsPanel = new JPanel(new GridBagLayout());
				// scroll.setViewportView(rowsPanel);
				// scroll.getVerticalScrollBar().setUnitIncrement(70);
				// }
				// }
			}

		}

		// MENU BAR
		// create status bar controller
		initDependent();
	}

	public GuiSelectionController getSelectionController()
	{
		return selectionController;
	}

	GuiMenuBarController getMenuBarController()
	{
		return menuBarController;
	}

	GuiPropertiesController getPropertiesController()
	{
		return propertiesController;
	}

	GuiStatusBarController getStatusBarController()
	{
		return statusBarController;
	}

	boolean isInitialized()
	{
		return initialized;
	}

	/**
	 * Ran every frame to update values if being changed in runtime
	 */
	void update()
	{
		getPropertiesController().update();
		getStatusBarController().update();
		updateAnimEditors();
		drawGrids();
	}

	private void updateAnimEditors()
	{
		for (GuiAnimationEditor animEditor : animEditors)
		{
			animEditor.update();
		}
	}

	/**
	 * Sets the id for the copy component so you can add a copy of this component to another transform.
	 * 
	 * @param component
	 * @param isCut
	 */
	void setCopyComponent(LttlComponent component, boolean isCut)
	{
		if (component == null)
		{
			copyComponentId = -1;
		}
		else
		{
			copyComponentId = component.getId();
		}
		this.isCut = isCut;
	}

	/**
	 * Returns the latest id of the copy component.
	 */
	LttlComponent getCopyComponent()
	{
		return Lttl.scenes.findComponentByIdAllScenes(copyComponentId);
	}

	/**
	 * Sets a list of transforms to be saved and copied later (only ids, no reference will be saved)
	 * 
	 * @param list
	 */
	void setCopyTransforms(ArrayList<LttlTransform> list)
	{
		// convert to int array so no pointers
		copyTransformIds.clear();
		for (LttlTransform t : list)
		{
			copyTransformIds.add(t.getId());
		}
	}

	/**
	 * Sets a list of transforms to be moved later (only ids, not reference will be saved)
	 * 
	 * @param list
	 */
	void setMoveTransforms(ArrayList<LttlTransform> list)
	{
		// convert to int array so no pointers
		moveTransformIds.clear();
		for (LttlTransform t : list)
		{
			moveTransformIds.add(t.getId());
		}
	}

	/**
	 * Get a list of transforms that were copied
	 * 
	 * @return
	 */
	ArrayList<LttlTransform> getCopyTransforms()
	{
		ArrayList<LttlTransform> list = new ArrayList<LttlTransform>();

		for (int id : copyTransformIds.items)
		{
			LttlTransform t = (LttlTransform) Lttl.scenes
					.findComponentByIdAllScenes(id);

			if (t != null)
			{
				list.add(t);
			}
		}

		return list;
	}

	void clearCopyTransforms()
	{
		copyTransformIds.clear();
	}

	void clearMoveTransforms()
	{
		moveTransformIds.clear();
	}

	int getCopyTransformCount()
	{
		return copyTransformIds.size;
	}

	int getMoveTransformCount()
	{
		return moveTransformIds.size;
	}

	/**
	 * When a component changes enabled/disabled property this updates it in tree and in properties panel
	 * 
	 * @param transform
	 */
	void onComponentDisableEnable(LttlTransform transform)
	{
		// reload tree node to show change
		Lttl.editor.getGui().getSelectionController().reloadNode(transform);
		// check if this transform is in properties focus
		if (Lttl.editor.getGui().getPropertiesController()
				.getFocusTransformIds().contains(transform.getId()))
		{
			for (GuiFieldObject<?> gfo : Lttl.editor.getGui()
					.getPropertiesController().focusedGuiFieldObjects)
			{
				// check if it is a GuiComponentObject for a LttlComponent
				if (gfo.getClass() == GuiComponentComponent.class
						&& LttlComponent.class.isAssignableFrom(gfo.objectRef
								.getClass()))
				{
					((GuiComponentObject) gfo).updateComponentToggleButton();
				}
			}
		}
	}

	ArrayList<LttlTransform> getMoveTransforms()
	{
		ArrayList<LttlTransform> list = new ArrayList<LttlTransform>();

		for (int id : moveTransformIds.toArray())
		{
			LttlTransform t = (LttlTransform) Lttl.scenes
					.findComponentByIdAllScenes(id);

			if (t != null)
			{
				list.add(t);
			}
		}

		return list;
	}

	/**
	 * @return the latest captured component, may be null if none or is not loaded
	 */
	LttlComponent getCaptureComponent(Class<? extends LttlComponent> compClass)
	{
		LttlComponent capturedComponent = Lttl.scenes
				.findComponentByIdAllScenes(captureTransformId);

		// check null
		if (capturedComponent == null)
		{
			return capturedComponent;
		}
		// same type, yay!
		else if (compClass.isAssignableFrom(capturedComponent.getClass()))
		{
			return capturedComponent;
		}
		// if it's a transform, and thats not the desired type, check it's components for type
		else if (capturedComponent.getClass() == LttlTransform.class)
		{
			capturedComponent = ((LttlTransform) capturedComponent)
					.getComponent(compClass, true);
		}

		return capturedComponent;
	}

	void setCaptureComponent(LttlComponent component)
	{
		setCaptureComponent(component.getId());
	}

	void setCaptureComponent(int id)
	{
		captureTransformId = id;
	}

	short editTagBitDialog(short initialTagBit)
	{
		JPanel contentPane = new JPanel(new GridBagLayout());
		contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// create dialog
		final JDialog dialog = GuiHelper.createDialog("Edit Tags", 200, 500,
				true, false, ModalityType.APPLICATION_MODAL, contentPane);
		dialog.setResizable(false);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		final ArrayList<JToggleButton> buttons = new ArrayList<JToggleButton>();
		final JToggleButton noneToggle = new JToggleButton("None",
				LttlHelper.bitIsNone(initialTagBit));
		final JToggleButton allToggle = new JToggleButton("All",
				LttlHelper.bitIsAll(initialTagBit));
		allToggle.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (allToggle.isSelected())
				{
					noneToggle.setSelected(false);
					for (JToggleButton b : buttons)
					{
						b.setSelected(false);
					}
				}
				else
				{
					allToggle.setSelected(true);
				}
			}
		});
		contentPane.add(allToggle, gbc);

		noneToggle.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (noneToggle.isSelected())
				{
					allToggle.setSelected(false);
					for (JToggleButton b : buttons)
					{
						b.setSelected(false);
					}
				}
				else
				{
					noneToggle.setSelected(true);
				}
			}
		});
		gbc.gridx = 1;
		contentPane.add(noneToggle, gbc);

		gbc.gridx = 0;
		gbc.gridwidth = 2;
		gbc.gridy = GridBagConstraints.RELATIVE;

		for (int i = 0; i < 15; i++)
		{
			String name = Lttl.game.getSettings().getTagName(i);
			if (name.isEmpty())
			{
				name = " ";
			}
			final JToggleButton button = new JToggleButton(name);
			buttons.add(button);
			button.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					if (button.isSelected())
					{
						allToggle.setSelected(false);
						noneToggle.setSelected(false);
					}
					else
					{
						boolean atleastOneSelected = false;
						boolean atleastOneNotSelected = false;
						for (JToggleButton b : buttons)
						{
							if (b.isSelected())
							{
								atleastOneSelected = true;
							}
							else
							{
								atleastOneNotSelected = true;
							}
						}
						if (atleastOneSelected && !atleastOneNotSelected)
						{
							allToggle.setSelected(true);
						}
						else if (!atleastOneSelected && atleastOneNotSelected)
						{
							noneToggle.setSelected(true);
						}
					}
				}
			});

			// button should not be selected if tag is none or all
			if (LttlHelper.bitIsAll(initialTagBit)
					|| LttlHelper.bitIsNone(initialTagBit))
			{
				button.setSelected(false);
			}
			else
			{
				button.setSelected(LttlHelper.bitHasInt(initialTagBit, i));
			}

			JPanel panel = new JPanel(new GridBagLayout());
			JLabel label = new JLabel(i + "  ");
			label.setPreferredSize(new Dimension(20, 15));
			label.setHorizontalAlignment(SwingConstants.RIGHT);
			panel.add(label, GuiHelper.GetGridBagConstraintsFieldLabel());
			panel.add(button, GuiHelper.GetGridBagConstraintsFieldValue());

			contentPane.add(panel, gbc);
		}

		dialog.setVisible(true);

		// dialog closed, now calculate the bit
		if (allToggle.isSelected())
		{
			return -1;
		}
		else if (noneToggle.isSelected())
		{
			return 0;
		}
		else
		{
			short bit = 0;
			for (int i = 0; i < buttons.size(); i++)
			{
				JToggleButton button = buttons.get(i);
				if (button.isSelected())
				{
					bit = (short) LttlHelper.bitAddInt(bit, i);
				}
			}
			return bit;
		}
	}

	void addTransformDialog(final ArrayList<LttlTransform> parents,
			final LttlScene scene)
	{
		// create content pane
		JPanel contentPane = new JPanel(new GridBagLayout());
		contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// create dialog
		final JDialog dialog = GuiHelper.createDialog("Add Transform", 300,
				100, true, false, ModalityType.APPLICATION_MODAL, contentPane);
		dialog.setResizable(false);

		// create the form object that has the enum and transform name fields
		final AddObjectTypeContainer aotc = new AddObjectTypeContainer();

		// create gui field object from the aotc object
		final GuiComponentObject gfo = (GuiComponentObject) GuiFieldObject
				.GenerateGuiFieldObject(aotc);

		// disables undo states being made
		gfo.setAutoRegisterUndo(false);

		gfo.collapsableGroup.setCollapseState(false);
		gfo.collapsableGroup.getToggleButton().setVisible(false);
		// set focus
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				gfo.children.get(0).getPanel().getComponent(1).requestFocus();
			}
		});
		contentPane.add(gfo.getPanel(), gbc);

		class Functions
		{
			void submit()
			{
				// create transform
				if (parents != null)
				{
					getSelectionController().clearSelection();
					for (LttlTransform lt : parents)
					{
						LttlTransform newChild = aotc.create(lt);

						// if only one, then select the new transform
						if (parents.size() == 1)
						{
							getSelectionController().addSelection(newChild);
						}
					}
				}
				else if (scene != null)
				{
					LttlTransform newTransform = aotc.create(scene);
					// select the created transform
					getSelectionController().setSelection(newTransform);
				}
				dialog.dispose();
			}
		}
		final Functions f = new Functions();

		// enter submits
		contentPane.registerKeyboardAction(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				f.submit();
			}
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
				JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

		GuiLttlComboBox comboBox = ((GuiLttlComboBox) gfo.children.get(0)
				.getPanel().getComponent(1));
		comboBox.addLttlActionListener(new GuiLttlComboBoxListener()
		{
			@Override
			public void selectionSubmitted(GuiSelectOptionContainer gsoc)
			{
				f.submit();
			}

			@Override
			public void selectionChanged(GuiSelectOptionContainer gsoc)
			{
			}
		});

		dialog.setVisible(true);
	}

	@GuiShow
	class AddObjectTypeContainer
	{
		@GuiShow
		public ObjectType type = ObjectType.Standard;
		@GuiShow
		public String name = "";

		public LttlTransform create(LttlScene scene)
		{
			// moves it to current editor camera position
			LttlTransform newTransform = LttlObjectFactory.AddObjectNoCallback(
					type, scene, name);
			newTransform.position.set(Lttl.editor.getCamera().position);
			// callbacks
			ComponentHelper.callBackTransformTree(newTransform,
					ComponentCallBackType.onEditorCreate);
			ComponentHelper.callBackTransformTree(newTransform,
					ComponentCallBackType.onStart);
			return newTransform;
		}

		public LttlTransform create(LttlTransform parent)
		{
			LttlTransform newTransform = LttlObjectFactory.AddObjectNoCallback(
					type, parent.getScene(), name);
			newTransform.setParent(parent, false);
			// callbacks
			ComponentHelper.callBackTransformTree(newTransform,
					ComponentCallBackType.onEditorCreate);
			ComponentHelper.callBackTransformTree(newTransform,
					ComponentCallBackType.onStart);
			return newTransform;
		}
	}

	void setSceneNameDialog(final LttlScene scene)
	{
		// create content pane
		JPanel contentPane = new JPanel(new GridBagLayout());
		contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// get all transforms
		final JTextField textField = new JTextField();
		contentPane.add(textField, gbc);

		// create dialog
		final JDialog dialog = GuiHelper.createDialog("Set Scene Name", 300,
				70, true, false, ModalityType.APPLICATION_MODAL, contentPane);
		dialog.setResizable(false);
		textField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void removeUpdate(DocumentEvent e)
			{
				changeState();
			}

			@Override
			public void insertUpdate(DocumentEvent e)
			{
				changeState();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
			}

			private void changeState()
			{
				if (Lttl.scenes.sceneNameExists(textField.getText()))
				{
					textField.setForeground(java.awt.Color.RED);
				}
				else
				{
					textField.setForeground(java.awt.Color.BLACK);
				}
			}
		});
		textField.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (!Lttl.scenes.sceneNameExists(textField.getText()))
				{
					scene.setName(textField.getText());
					dialog.dispose();
				}
			}
		});

		dialog.setVisible(true);
	}

	void loadSceneDialog()
	{
		// create content pane
		JPanel contentPane = new JPanel(new GridBagLayout());
		contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// get all non loaded scenes
		ArrayList<GuiSelectOptionContainer> options = new ArrayList<GuiSelectOptionContainer>();
		for (Iterator<Entry<String, Integer>> it = Lttl.scenes.getWorldCore().sceneNameMap
				.entrySet().iterator(); it.hasNext();)
		{
			Entry<String, Integer> entry = it.next();
			if (!Lttl.scenes.isSceneLoaded(entry.getValue()))
			{
				options.add(new GuiSelectOptionContainer(entry.getValue(),
						entry.getKey()));
			}
		}

		// create select box
		final GuiLttlComboBox combo = new GuiLttlComboBox(options, null, true);
		contentPane.add(combo, gbc);

		// create dialog
		final JDialog dialog = GuiHelper.createDialog("Load Scene", 300, 70,
				true, false, ModalityType.APPLICATION_MODAL, contentPane);
		dialog.setResizable(false);

		combo.addLttlActionListener(new GuiLttlComboBoxListener()
		{
			@Override
			public void selectionSubmitted(GuiSelectOptionContainer gsoc)
			{
				Lttl.scenes.loadScene((Integer) gsoc.value);
				dialog.dispose();
			}
		});

		dialog.setVisible(true);
	}

	void deleteTransformsDialog(final ArrayList<LttlTransform> transforms)
	{
		if (GuiHelper.showOptionModal("Delete Transform(s)", "Delete: " + "<b>"
				+ LttlHelper.FormatListObjectStrings(transforms) + "</b>"
				+ ".  Including all components and children?") > 0) { return; }

		// check if there are any dependencies on the entire transform tree (this will pickup
		ArrayList<LttlComponent> needleComponents = new ArrayList<LttlComponent>();
		for (LttlTransform transform : transforms)
		{
			needleComponents.addAll(transform.getComponentsInTree());
		}

		int dependecyCount = ComponentHelper.checkDependencies(
				needleComponents, Lttl.scenes.getAllLoaded(true),
				FieldsMode.AllButIgnore);

		if (dependecyCount > 0)
		{
			if (GuiHelper
					.showOptionModal(
							"Component Reference Dependencies",
							"There are "
									+ dependecyCount
									+ " component references (may not be gui) in this or another scene (see console for details).  Would you like to continue deleting and hard remove the references?") > 0) { return; }
		}

		// perform delete
		LttlObjectHelper.RemoveDescendants(transforms);
		for (LttlTransform transform : transforms)
		{
			transform.destroyComp(true, false);
		}
	}

	void createSceneDialog()
	{
		String sceneName = "New Scene";
		int count = -1;
		while (Lttl.scenes.sceneNameExists(sceneName
				+ ((count > 0) ? count : "")))
		{
			count++;
		}
		sceneName = sceneName + ((count > 0) ? count : "");
		Lttl.editor.getGui().setSceneNameDialog(
				Lttl.scenes.createScene(sceneName));
	}

	void deleteSceneDialog()
	{
		// create content pane
		JPanel contentPane = new JPanel(new GridBagLayout());
		contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// get all scenes from sceneNameMap
		ArrayList<GuiSelectOptionContainer> options = new ArrayList<GuiSelectOptionContainer>();
		for (Iterator<Entry<String, Integer>> it = Lttl.scenes.getWorldCore().sceneNameMap
				.entrySet().iterator(); it.hasNext();)
		{
			Entry<String, Integer> entry = it.next();
			if (entry.getValue() != Lttl.scenes.WORLD_ID)
			{
				options.add(new GuiSelectOptionContainer(entry.getValue(),
						entry.getKey()));
			}
		}

		// create select box
		final GuiLttlComboBox combo = new GuiLttlComboBox(options, null, true);
		contentPane.add(combo, gbc);

		// create dialog
		final JDialog dialog = GuiHelper.createDialog("Delete Scene", 300, 90,
				true, false, ModalityType.APPLICATION_MODAL, contentPane);
		dialog.setResizable(false);

		JButton submitButton = new JButton("Delete");
		submitButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				GuiSelectOptionContainer gsoc = combo.getSelected();
				Lttl.scenes.deleteScene((Integer) gsoc.value);
				dialog.dispose();
			}
		});
		gbc.gridy = 1;
		contentPane.add(submitButton, gbc);

		dialog.setVisible(true);
	}

	JFrame getFrame()
	{
		return frame;
	}

	/**
	 * Returns all the component classes available.
	 * 
	 * @return
	 */
	public ArrayList<Class<? extends LttlComponent>> getComponentClasses()
	{
		return componentClasses;
	}

	private void checkClassPersistFormat(int clazzID, Class<?> clazz)
	{
		// must be negative
		if (clazzID > 0)
		{
			Lttl.Throw("Class " + clazz.getName()
					+ " does @Persist annotation needs to be negative.");
		}
	}

	private void checkPersistAnnotations()
	{
		// check Persist annotations are accurate
		IntArray persistIds = new IntArray(Lttl.editor.getGui()
				.getComponentClasses().size() * 4);

		// iterate through all the component classes
		for (Class<? extends LttlComponent> clazz : Lttl.editor.getGui()
				.getComponentClasses())
		{
			Persist ann = LttlObjectGraphCrawler
					.getPersistClassAnnotation(clazz);

			// check if has persist annotation
			if (ann == null)
			{
				Lttl.Throw("LttlComponent Class " + clazz.getName()
						+ " does not have the @Persist annotation.");
			}

			int clazzId = ann.value();
			// check class id format (negative)
			checkClassPersistFormat(clazzId, clazz);
			// check if class id is unique
			if (persistIds.contains(clazzId))
			{
				Lttl.Throw("There is a duplicate @Persist Id for " + clazzId
						+ " on class " + clazz.getName());
			}
			persistIds.add(clazzId);

			// check if class has been registered in classMap
			if (!LttlGameStarter.get().getClassMap().containsKey(clazzId))
			{
				Lttl.Throw("There is no entry in the ClassMap for " + clazzId
						+ " on class " + clazz.getName());
			}
			if (LttlGameStarter.get().getClassMap().get(clazzId) != clazz)
			{
				Lttl.Throw("The classes do not match for class id "
						+ clazzId
						+ " for "
						+ clazz.getName()
						+ " and with "
						+ LttlGameStarter.get().getClassMap().get(clazzId)
								.getName() + " which is in the classMap.");
			}
		}

		// now check if any of the non LttlComponent classes in the class map conflict with any of ther persist ids
		for (com.badlogic.gdx.utils.IntMap.Entry<Class<?>> e : LttlGameStarter
				.get().getClassMap().entries())
		{
			Class<?> clazz = e.value;
			// skip LttlComponents
			if (LttlComponent.class.isAssignableFrom(clazz))
			{
				continue;
			}

			int clazzId = e.key;
			// check class id format (negative)
			checkClassPersistFormat(clazzId, clazz);
			if (persistIds.contains(clazzId))
			{
				Lttl.Throw("There is a conflict with the persist id "
						+ e.key
						+ " in the classMap.  It was already found as a class or field.");
			}
			persistIds.add(e.key);
		}

		// now iterate through all the classes in the class map and check if all their fields are unique
		for (com.badlogic.gdx.utils.IntMap.Entry<Class<?>> e : LttlGameStarter
				.get().getClassMap().entries())
		{
			Class<?> clazz = e.value;

			Persist ann = LttlObjectGraphCrawler
					.getPersistClassAnnotation(clazz);

			// make sure each entry in class map has @Persist annotation
			if (ann == null)
			{
				Lttl.Throw("Class "
						+ clazz.getSimpleName()
						+ " is in the ClassMap but does not have a @Persist annotation.");
			}

			int clazzId = ann.value();

			// check all declared fields for this class are also unique and abide by parent class id format
			for (Field f : clazz.getDeclaredFields())
			{
				Persist fieldAnn = LttlObjectGraphCrawler
						.getPersistFieldAnnotation(f);

				// get the persist only fields
				if (fieldAnn != null)
				{
					int fieldId = fieldAnn.value();

					// make sure it's positive
					if (fieldId < 0)
					{
						Lttl.Throw("The @Persist ID " + fieldId + " on field "
								+ f.getName() + " (" + clazz.getSimpleName()
								+ ") can't be negative.");
					}

					// check fiedlId format relative to class Id
					String fieldIdString = fieldId + "";
					String startsWithString = (clazzId * -1) + "0";
					if (fieldIdString.length() <= startsWithString.length()
							|| !fieldIdString.startsWith(startsWithString))
					{
						Lttl.Throw("The @Persist ID "
								+ fieldId
								+ " on field "
								+ f.getName()
								+ " is not in the correct format with the class it is declared on "
								+ clazz.getSimpleName() + ".");
					}

					// check if fieldId already exists
					if (persistIds.contains(fieldId))
					{
						Lttl.Throw("There is a duplicate @Persist ID of "
								+ fieldId + " for class "
								+ clazz.getSimpleName() + " on field "
								+ f.getName());
					}
					persistIds.add(fieldId);
				}
			}
		}
	}

	/**
	 * Initiates the GUI that is not dependent on game being fully loaded first
	 */
	void initIndependent()
	{
		// reset tree
		if (selectionController != null)
		{
			selectionController.savePreviousSelectionIds();
		}
		selectionController = new GuiSelectionController();

		// reset properties panel
		propertiesController = new GuiPropertiesController();

		// clear copy/captures/moves
		copyTransformIds.clear();
		moveTransformIds.clear();
		captureTransformId = -1;
	}

	/**
	 * Initiates GUI that is dependent on the game being loaded to be initialized
	 */
	void initDependent()
	{
		// reset menubar
		menuBarController = new GuiMenuBarController();

		// reset statusbar
		statusBarController = new GuiStatusBarController();

		// set initial selection on load
		selectionController.initSelection();

		importSavedGFOs();
	}

	@SuppressWarnings("rawtypes")
	private void importSavedGFOs()
	{
		for (GuiFieldObject gfo : saveGFOs)
		{
			GuiFieldObject<?> highest = gfo.getHighestParent();

			Object container = null;

			// if a lttlcomponent, then replace with newly loaded lttlcomponent
			if (LttlComponent.class.isAssignableFrom(highest.getObjectClass()))
			{
				LttlComponent originalHost = (LttlComponent) highest.getValue();
				container = Lttl.scenes.findComponentByIdAllScenes(originalHost
						.getId());
				if (container == null)
				{
					Lttl.logNote("Import Saved GFOs: could not find the component.");
					continue;
				}
			}
			else if (highest.getObjectClass() == LttlGameSettings.class)
			{
				container = Lttl.game.getSettings();
			}
			else if (highest.getObjectClass() == LttlEditorSettings.class)
			{
				container = Lttl.editor.getSettings();
			}
			else if (highest.getObjectClass() == LttlCamera.class)
			{
				container = Lttl.game.getCamera();
			}
			else if (highest.getObjectClass() == LttlEditorCamera.class)
			{
				container = Lttl.editor.getCamera();
			}
			else
			{
				// don't know how to handle this
				Lttl.Throw(highest.getObjectClass().getSimpleName());
			}

			Lttl.Throw(container);

			// if the first time host has been replaced
			if (!highest.hostReplaced)
			{
				if (container != null)
				{
					highest.replaceHostTree(container);
				}
			}

			try
			{
				setSaved(gfo, container);
			}
			catch (IndexOutOfBoundsException e)
			{
				Lttl.logNote("Play Saving: Some error with "
						+ gfo.getTreeFieldName());
			}
		}

		// clear all and if any savedGFOs, refresh the properties controller
		if (saveGFOs.size() > 0)
		{
			getPropertiesController().draw(false);
		}
		saveGFOs.clear();
	}

	private void setSaved(GuiFieldObject<?> playGFOWithModifiedHost,
			Object container)
	{
		// this is a group
		if (playGFOWithModifiedHost.guiGroupName != null)
		{
			for (GuiFieldObject<?> child : playGFOWithModifiedHost.children)
			{
				// no child should need a container object, since it will be a field or list item and be okay with just
				// replacing entirely
				setSaved(child, null);
			}
			// iterate through any child groups too
			if (((GuiComponentObject) playGFOWithModifiedHost).guiGroupMap != null)
			{
				for (GuiComponentObject group : ((GuiComponentObject) playGFOWithModifiedHost).guiGroupMap
						.values())
				{
					setSaved(group, null);
				}
			}
			return;
		}
		// this is a root object
		else if (playGFOWithModifiedHost.hostObject == null)
		{
			// make a copy of the play time object into the container, most likely is a LttlComponent or some top level
			// object like game settings
			// this will copy LttlComponent references by reference even though they are from previous play mode, but
			// the id will be the same, so when do updateReferenceFromId below, it will search this game state and get
			// the accurate LttlComponents
			LttlCopier.copy(playGFOWithModifiedHost.objectRef,
					FieldsMode.Export, container);
			ComponentHelper.updateReferencesFromId(container);
		}
		else
		{
			// must be a field or list item

			// if it is LttlComponent reference, then no need to make a copy of it, just set it to the same value as the
			// gfo from the play mode, then it will update the references from the play mode components to the editor
			// mode components since the ids are the same
			if (LttlComponent.class.isAssignableFrom(playGFOWithModifiedHost
					.getObjectClass()))
			{
				playGFOWithModifiedHost
						.setValue(playGFOWithModifiedHost.objectRef);
				ComponentHelper
						.updateReferencesFromId(playGFOWithModifiedHost.hostObject);
			}
			// if is primative, then just make a copy of it and set value
			else if (LttlObjectGraphCrawler.isPrimative(playGFOWithModifiedHost
					.getObjectClass()))
			{
				Object newObject = LttlCopier.copy(
						playGFOWithModifiedHost.objectRef, FieldsMode.Export,
						null);
				playGFOWithModifiedHost.setValue(newObject);
			}
			else
			{
				// some non LttlCompponent object, make a copy into the container
				// LttlComponent references will be copied, not created, but they will be for play mode, but they'll
				// have accurate ids, so when updateReferences below it will update to editor mode components
				Object newObject = LttlCopier.copy(
						playGFOWithModifiedHost.objectRef, FieldsMode.Export,
						playGFOWithModifiedHost.getValue());
				if (playGFOWithModifiedHost.getValue() != newObject)
				{
					playGFOWithModifiedHost.setValue(newObject);
				}
				ComponentHelper
						.updateReferencesFromId(playGFOWithModifiedHost.hostObject);
			}
		}
		// process callbacks as if it was modified by user, this way it seems normal
		playGFOWithModifiedHost.onEditorValueChange();
	}

	private void drawGrids()
	{
		if (!Lttl.editor.getSettings().enableGrid) { return; }
		Rectangle cameraRect = Lttl.editor.getCamera().getViewportAABB();
		float top = cameraRect.y + cameraRect.height;
		float bottom = cameraRect.y;
		float left = cameraRect.x;
		float right = cameraRect.x + cameraRect.width;

		if (Lttl.editor.getCamera().zoom > .01f) // some bug whatever though
		{
			// vertical
			float x = Lttl.editor.getSettings().gridOffset.x;
			float x0 = x;
			boolean started = false;
			int count = 0;
			while (true)
			{
				tmpColor.set(Lttl.editor.getSettings().gridColor);
				if (count % 5 != 0)
				{
					tmpColor.a *= .3f;
				}

				boolean atleastOne = false;
				if (x >= left && x <= right)
				{
					Lttl.debug.drawLine(x, top, x, bottom, 0, tmpColor);
					started = true;
					atleastOne = true;
				}
				if (x0 != x && x0 >= left && x0 <= right)
				{
					Lttl.debug.drawLine(x0, top, x0, bottom, 0, tmpColor);
					started = true;
					atleastOne = true;
				}

				if (!atleastOne && started)
				{
					// not drawing anymore after started
					break;
				}

				// increment
				x += Lttl.editor.getSettings().gridStep;
				x0 -= Lttl.editor.getSettings().gridStep;
				count++;
			}

			// horizontal
			float y = Lttl.editor.getSettings().gridOffset.y;
			float y0 = y;
			started = false;
			count = 0;
			while (true)
			{
				tmpColor.set(Lttl.editor.getSettings().gridColor);
				if (count % 5 != 0)
				{
					tmpColor.a *= .5f;
				}

				boolean atleastOne = false;
				if (y >= bottom && y <= top)
				{
					Lttl.debug.drawLine(left, y, right, y, 0, tmpColor);
					started = true;
					atleastOne = true;
				}
				if (y0 != y && y0 >= bottom && y0 <= top)
				{
					Lttl.debug.drawLine(left, y0, right, y0, 0, tmpColor);
					started = true;
					atleastOne = true;
				}

				if (!atleastOne && started)
				{
					// not drawing anymore after started
					break;
				}

				// increment
				y += Lttl.editor.getSettings().gridStep;
				y0 -= Lttl.editor.getSettings().gridStep;
				count++;
			}
		}
	}

	/**
	 * Focus's swing input on canvas so commands like mousewheel work
	 */
	void focusCanvas()
	{
		lwjglCanvas.getCanvas().requestFocusInWindow();
	}

	ArrayList<GuiSelectOptionContainer> getComponentOptions()
	{
		return componentOptions;
	}

	/**
	 * Adds a menu item to the right click menu for this frame only, only useful if expecting the right click menu, akak
	 * check for isMouseReleased(1)
	 * 
	 * @param menuItem
	 */
	void addRightClickMenu(JMenuItem menuItem)
	{
		rightClickMenuItems.add(menuItem);
	}

	/**
	 * Disables the right click menu for this frame
	 */
	void disableRightClickMenu()
	{
		Lttl.editor.getInput().disableRightClickMenu();
	}

	public boolean isCut()
	{
		return isCut;
	}
}
