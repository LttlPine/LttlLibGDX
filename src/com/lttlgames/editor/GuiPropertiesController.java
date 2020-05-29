package com.lttlgames.editor;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.border.CompoundBorder;

import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.TimeUtils;
import com.lttlgames.helpers.LttlProfiler;

class GuiPropertiesController
{
	enum PropertiesState
	{
		NOTHING, TRANSFORM, TRANSFORMS, SCENE
	}

	private PropertiesState state = PropertiesState.NOTHING;
	private static JPanel panel;
	private JScrollPane scrollPane;
	private JPanel innerPanel;
	private long updateIntervalMillis = 100;
	private long nextUpdateTime = 0;
	private int lockedSceneId = -1;
	private int lockedTransformId = -1;

	private static MouseListener panelMouseListener;
	private int focusSceneId;
	private IntArray focusTransformIds = new IntArray();
	{
		resetIds();
	}
	/**
	 * Primarily these are the Components to a transform, used for updating editor gui values in runtime
	 */
	ArrayList<GuiFieldObject<?>> focusedGuiFieldObjects = new ArrayList<GuiFieldObject<?>>();

	GuiPropertiesController()
	{
		// only create panel if first time
		if (panel == null)
		{
			panel = new JPanel(new GridBagLayout());
			panel.setBorder(new CompoundBorder(BorderFactory.createEmptyBorder(
					3, 0, 6, 6), BorderFactory.createTitledBorder("Properties")));
		}
		panel.removeAll();

		// PROPERTIES PANEL POPUP MENU
		final JPopupMenu propertiesPopup = new JPopupMenu();
		final JMenuItem collapseAll = new JMenuItem("Collapse All");
		collapseAll.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				collapseAll();
			}

		});
		propertiesPopup.add(collapseAll);

		final JMenuItem lockToggle = new JMenuItem();
		lockToggle.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// if locked
				if (lockedSceneId != -1 || lockedTransformId != -1)
				{
					// unlock
					lockedSceneId = -1;
					lockedTransformId = -1;
					// redraw
					draw(false);
				}
				// unlocked
				else
				{
					// lock by saving focus ids
					lockedSceneId = getFocusSceneId();
					lockedTransformId = getFocusTransformId();
				}
			}
		});
		propertiesPopup.add(lockToggle);
		panel.removeMouseListener(panelMouseListener);
		panelMouseListener = new MouseAdapter()
		{
			public void mousePressed(MouseEvent e)
			{
				maybeShowPopup(e);
			}

			public void mouseReleased(MouseEvent e)
			{
				maybeShowPopup(e);
			}

			private void maybeShowPopup(MouseEvent e)
			{
				if (!e.isPopupTrigger()) return;
				// disable ability to lock if no selection or multi
				if ((getFocusTransformIds().size > 1 || getFocusTransformIds().size == 0)
						&& getFocusSceneId() == -1)
				{
					lockToggle.setEnabled(false);
				}
				lockToggle
						.setText((lockedSceneId != -1 || lockedTransformId != -1) ? "Unlock"
								: "Lock");

				collapseAll.setEnabled(false);
				for (GuiFieldObject<?> gfo : focusedGuiFieldObjects)
				{
					if (GuiComponentObject.class.isAssignableFrom(gfo
							.getClass()))
					{
						GuiComponentObject gco = (GuiComponentObject) gfo;
						if (!gco.collapsableGroup.isCollapsed())
						{
							collapseAll.setEnabled(true);
							break;
						}
					}
				}

				// show popup menu
				propertiesPopup.show(e.getComponent(), e.getX(), e.getY());
			}
		};
		panel.addMouseListener(panelMouseListener);

		scrollPane = new JScrollPane();
		panel.add(scrollPane, new GridBagConstraints(0, 0, 1, 1, 1, 1,
				GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets(
						-5, 0, 0, 0), 0, 0));
		scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		{
			innerPanel = new JPanel(new GridBagLayout());
			scrollPane.setViewportView(innerPanel);
			scrollPane.getVerticalScrollBar().setUnitIncrement(70);
		}

		// SETUP SCROLL PANE POPUP MENU
		final JPopupMenu scrollPanePopup = new JPopupMenu();

		// Add Component Button
		final JMenuItem addComponentMenuItem = new JMenuItem("Add Component");
		addComponentMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Lttl.editor.getGui().getMenuBarController()
						.addComponentDialog(getFocusedTransforms());
			}
		});
		scrollPanePopup.add(addComponentMenuItem);

		// Paste Component Button
		final JMenuItem pasteComponentButton = new JMenuItem("Paste Component");
		pasteComponentButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Lttl.editor.getGui().getSelectionController()
						.pasteComponent(getFocusedTransforms());
			}
		});
		scrollPanePopup.add(pasteComponentButton);

		MouseListener scrollPaneMouseListener = new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				action(e);
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				action(e);
			}

			private void action(MouseEvent e)
			{
				if (!e.isPopupTrigger()) return;

				// show popup menu only if focused on a single transform
				if (getFocusTransformId() != -1)
				{
					pasteComponentButton.setEnabled(Lttl.editor.getGui()
							.getCopyComponent() != null);
					scrollPanePopup.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		};
		scrollPane.addMouseListener(scrollPaneMouseListener);
	}

	/**
	 * collapse all focused top level gui field objects
	 */
	void collapseAll()
	{
		for (GuiFieldObject<?> gfo : focusedGuiFieldObjects)
		{
			// they all should be GuiComponentObjects, but jsut in case
			if (!GuiComponentObject.class.isAssignableFrom(gfo.getClass()))
				continue;

			GuiComponentObject gco = (GuiComponentObject) gfo;

			// only do anything if not collapsed already
			if (!gco.collapsableGroup.isCollapsed())
			{
				// if it's actually a GuiComponentComponent (ie. transform), then also save the value the guiCollapse
				// value
				if (GuiComponentComponent.class
						.isAssignableFrom(gfo.getClass()))
				{
					LttlComponent comp = (LttlComponent) gfo.getValue();
					if (comp != null)
					{
						comp.guiCollapsed = true;
					}
				}
				gco.collapsableGroup.setCollapseState(true);
			}
		}
	}

	JPanel getPanel()
	{
		return panel;
	}

	private JViewport getScrollViewport()
	{
		return scrollPane.getViewport();
	}

	private void drawSingleTransform(LttlTransform transform)
	{
		if (transform.isDestroyPending())
		{
			drawNothing();
			return;
		}

		state = PropertiesState.TRANSFORM;
		clear();
		focusTransformIds.add(transform.getId());

		// build
		// if no components, make transform start expanded or if it is just renderer
		List<LttlComponent> comps = transform.getComponents();
		if (comps.size() == 0 || (comps.size() == 1 && transform.r() != null))
		{
			transform.guiCollapsed = false;
		}
		GuiFieldObject<?> transformGFO = GuiFieldObject
				.GenerateGuiFieldObject(transform);
		focusedGuiFieldObjects.add(transformGFO);
		innerPanelAdd(transformGFO.getPanel());
		for (LttlComponent comp : comps)
		{
			if (comp.isDestroyPending()) continue;

			GuiFieldObject<?> compGFO = GuiFieldObject
					.GenerateGuiFieldObject(comp);

			focusedGuiFieldObjects.add(compGFO);

			innerPanelAdd(compGFO.getPanel());
		}
		// fills in so top aligned
		innerPanelEnd();

		// refresh
		getPanel().revalidate();
		getPanel().repaint();
	}

	private void innerPanelAdd(JPanel panel)
	{
		innerPanel.add(panel, new GridBagConstraints(0, -1, 1, 1, 1, 0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 0, 0, 0), 0, 0));
	}

	private void innerPanelEnd()
	{
		innerPanel.add(new JPanel(), new GridBagConstraints(0, -1, 1, 1, 1, 1,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 0, 0, 0), 0, 0));
	}

	private void drawMultipleTransforms(ArrayList<LttlTransform> transforms)
	{
		state = PropertiesState.TRANSFORMS;
		clear();
		for (LttlTransform lt : transforms)
		{
			focusTransformIds.add(lt.getId());
		}

		// render

		getPanel().revalidate();
		getPanel().repaint();
	}

	private void drawSingleScene(LttlScene scene)
	{
		state = PropertiesState.SCENE;
		clear();
		focusSceneId = scene.getId();

		// render the scene
		if (focusSceneId == Lttl.scenes.WORLD_ID)
		{
			drawWorldScene();
		}
		else
		{
			drawNormalScene(scene);
		}

		getPanel().revalidate();
		getPanel().repaint();
	}

	private void drawWorldScene()
	{
		// SETTINGS
		GuiFieldObject<?> settingsGFO = GuiFieldObject
				.GenerateGuiFieldObject(Lttl.game.getSettings());
		focusedGuiFieldObjects.add(settingsGFO);
		innerPanelAdd(settingsGFO.getPanel());

		// PHYSICS SETTINGS
		GuiFieldObject<?> physicsGFO = GuiFieldObject
				.GenerateGuiFieldObject(Lttl.game.getPhysics());
		focusedGuiFieldObjects.add(physicsGFO);
		innerPanelAdd(physicsGFO.getPanel());

		// PLAY CAMERA
		GuiFieldObject<?> cameraPlayGFO = GuiFieldObject
				.GenerateGuiFieldObject(Lttl.game.getCamera());
		focusedGuiFieldObjects.add(cameraPlayGFO);
		innerPanelAdd(cameraPlayGFO.getPanel());

		// EDITOR CAMERA
		GuiFieldObject<?> cameraEditorGFO = GuiFieldObject
				.GenerateGuiFieldObject(Lttl.editor.getCamera());
		focusedGuiFieldObjects.add(cameraEditorGFO);
		innerPanelAdd(cameraEditorGFO.getPanel());

		// EDITOR SETTINGS
		GuiFieldObject<?> editorSettingsGFO = GuiFieldObject
				.GenerateGuiFieldObject(Lttl.editor.getSettings());
		focusedGuiFieldObjects.add(editorSettingsGFO);
		innerPanelAdd(editorSettingsGFO.getPanel());

		// PROFILER DATA
		GuiFieldObject<?> profilerDataGFO = GuiFieldObject
				.GenerateGuiFieldObject(LttlProfiler.get());
		focusedGuiFieldObjects.add(profilerDataGFO);
		innerPanelAdd(profilerDataGFO.getPanel());

		// fills in so top aligned
		innerPanelEnd();
	}

	private void drawNormalScene(LttlScene scene)
	{

	}

	private void drawNothing()
	{
		state = PropertiesState.NOTHING;
		clear();

		getPanel().revalidate();
		getPanel().repaint();
	}

	/**
	 * @return the focused scene id, -1 if a scene is not in focus
	 */
	int getFocusSceneId()
	{
		return focusSceneId;
	}

	/**
	 * @return all the transform ids that are selected, will be empty if no selected transforms
	 */
	IntArray getFocusTransformIds()
	{
		return focusTransformIds;
	}

	/**
	 * @return focused LttlTransform id, if more than one selected then returns first, if none, returns -1
	 */
	public int getFocusTransformId()
	{
		if (getFocusTransformIds().size >= 1) { return getFocusTransformIds()
				.get(0); }
		return -1;
	}

	public int getFocusedTransformsCount()
	{
		return getFocusTransformIds().size;
	}

	public boolean isFocused(LttlTransform t)
	{
		IntArray ids = getFocusTransformIds();
		for (int i = 0; i < ids.size; i++)
		{
			if (ids.get(i) == t.getId()) { return true; }
		}
		return false;
	}

	ArrayList<LttlTransform> getFocusedTransforms()
	{
		ArrayList<LttlTransform> list = new ArrayList<LttlTransform>();
		IntArray ids = getFocusTransformIds();
		for (int i = 0; i < ids.size; i++)
		{
			list.add((LttlTransform) Lttl.scenes.findComponentByIdAllScenes(ids
					.get(i)));
		}
		return list;
	}

	PropertiesState getState()
	{
		return state;
	}

	/**
	 * Clears the scroll panel and resets the ids
	 */
	void clear()
	{
		resetIds();
		focusedGuiFieldObjects.clear();
		innerPanel.removeAll();
	}

	private void resetIds()
	{
		focusSceneId = -1;
		focusTransformIds.clear();
	}

	void update()
	{
		if (TimeUtils.millis() < nextUpdateTime) return;
		nextUpdateTime = TimeUtils.millis() + updateIntervalMillis;

		for (GuiFieldObject<?> gfo : focusedGuiFieldObjects)
		{
			if (gfo.getClass() == GuiComponentComponent.class
					&& ((GuiComponentComponent) gfo).collapsableGroup
							.getCollapsePanel().isCollapsed())
			{
				// skip checking for changes in closed components
				continue;
			}
			gfo.checkNonGuiChanged();
		}
	}

	/**
	 * draws the property panel based on curren selection and locked settings
	 * 
	 * @param isSelectionChange
	 *            this means it was called because selection was changed, if this is false, it's usually some sort of
	 *            reresh
	 */
	void draw(boolean isSelectionChange)
	{
		// check if locked
		if (lockedTransformId != -1)
		{
			LttlTransform lockedLT = (LttlTransform) Lttl.scenes
					.findComponentByIdAllScenes(lockedTransformId);
			if (lockedLT == null)
			{
				// unlock becuase no transform found (could be unloaded or deleted)
				lockedTransformId = -1;
				// continue normal drawng
			}
			else
			{
				// draw single transform, but only if it wasn't a selection change since we don't want changing
				// selection to keep on redrawing the properties when they are locked
				if (!isSelectionChange)
				{
					drawSingleTransform(lockedLT);
				}
				return;
			}
		}
		else if (lockedSceneId != -1)
		{
			LttlScene lockedScene = Lttl.scenes.get(lockedSceneId);
			if (lockedScene == null)
			{
				// unlock becuase no scene found (could be unloaded or deleted)
				lockedSceneId = -1;
				// continue normal drawng
			}
			else
			{
				if (!isSelectionChange)
				{
					drawSingleScene(lockedScene);
				}
				return;
			}
		}

		// unlocked drawing
		// don't draw properties controller til finished select boxing
		if (Lttl.editor.getGui().getSelectionController().selectBoxRectangle == null
				&& Lttl.editor.getGui().getSelectionController().pressedOnNonSelected == null)
		{
			int selectedTransforms = Lttl.editor.getGui()
					.getSelectionController().getSelectedTransformCount();
			if (selectedTransforms > 0)
			{
				if (selectedTransforms == 1)
				{
					drawSingleTransform(Lttl.editor.getGui()
							.getSelectionController().getSelectedTransform());
				}
				else
				{
					drawMultipleTransforms(Lttl.editor.getGui()
							.getSelectionController().getSelectedTransforms());
				}
			}
			else if (Lttl.editor.getGui().getSelectionController()
					.isSceneSelected())
			{
				drawSingleScene(Lttl.editor.getGui().getSelectionController()
						.getSelectedScene());
			}
			else
			{
				drawNothing();
			}
		}
		else
		{
			drawNothing();
		}
	}

	/**
	 * Finds and returns the GuiFieldObject for a component that is on the single selected transform (focused)
	 * 
	 * @param comp
	 * @return null if none could be found
	 */
	GuiFieldObject<?> findComponentGFO(LttlComponent comp)
	{
		for (GuiFieldObject<?> gfo : focusedGuiFieldObjects)
		{
			if (comp == gfo.objectRef) { return gfo; }
		}
		return null;
	}
}
