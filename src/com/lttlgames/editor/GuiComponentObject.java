package com.lttlgames.editor;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.lttlgames.editor.LttlObjectGraphCrawler.FieldsMode;
import com.lttlgames.editor.annotations.GuiAutoExpand;
import com.lttlgames.editor.annotations.GuiButton;
import com.lttlgames.editor.annotations.GuiGroup;
import com.lttlgames.editor.annotations.GuiReadOnly;
import com.lttlgames.editor.annotations.GuiTagBit;
import com.lttlgames.editor.annotations.GuiTwoColumn;
import com.lttlgames.helpers.LttlCallback;
import com.lttlgames.helpers.LttlClosure;
import com.lttlgames.helpers.LttlHelper;
import com.lttlgames.helpers.LttlTimeline;

public class GuiComponentObject extends GuiFieldObject<Object>
{
	private static final GridBagConstraints gbc = new GridBagConstraints(0, -1,
			1, 1, 1, 0, GridBagConstraints.CENTER,
			GridBagConstraints.HORIZONTAL, new Insets(0, 2, 0, 0), 0, 0);
	protected static final GridBagConstraints gbcStatic = new GridBagConstraints(
			0, -1, 1, 1, 1, 0, GridBagConstraints.CENTER,
			GridBagConstraints.HORIZONTAL, new Insets(0, 2, 0, 0), 0, 0);

	// do not set to anything, since these will be initialized after the draw method, and overwriten, they will be set
	// accordingly during the draw()
	GuiLttlCollapsableGroup collapsableGroup;
	HashMap<String, GuiComponentObject> guiGroupMap;
	boolean twoColumnObject;

	GuiComponentObject(ProcessedFieldType pft, Object hostObject, int index,
			GuiComponentObject parent)
	{
		super(pft, hostObject, index, parent);
	}

	/**
	 * Used when creating a root object (no parent)
	 * 
	 * @param object
	 * @param guiGroupName
	 *            should be null, unless this is a GuiComponentObject for a group of gui fields (organization purposes)
	 */
	GuiComponentObject(Object object, String guiGroupName)
	{
		super(null, object, guiGroupName);
	}

	/**
	 * Used when creating a group object
	 * 
	 * @param parent
	 * @param object
	 * @param guiGroupName
	 *            should be null, unless this is a GuiComponentObject for a group of gui fields (organization purposes)
	 */
	GuiComponentObject(GuiComponentObject parent, Object object,
			String guiGroupName)
	{
		super(parent, object, guiGroupName);
	}

	@Override
	void draw()
	{
		guiGroupMap = null;
		if (checkDrawNull()) return;

		drawComponentFrameworkShared();
		// don't draw fields if gui group
		if (!isGuiGroup())
		{
			drawComponentFieldsShared();
		}
		drawComponentObjectShared();

		// disable popup menu if read only
		if (getAnnotationOnAncestorsAndSelf(GuiReadOnly.class) == null)
		{
			createPopupMenu();
		}
	}

	void createPopupMenu()
	{
		super.createPopupMenu(collapsableGroup.getToggleButton());

		// skip adding any popup menu items if it's a group
		if (!isGuiGroup())
		{
			// add any menu items for library classes
			addLibraryMenuItems();

			// add method menu items
			addMethodMenuItems(popup);

			// add export/import menu options
			addExportImportMenuItem();

			// add set to null menu option
			addSetToNullMenuItem();
		}
	}

	private void addLibraryMenuItems()
	{
		if (getObjectClass() == Vector2.class)
		{
			final Vector2 vector = (Vector2) objectRef;

			final LttlClosure before = new LttlClosure()
			{
				@Override
				public void run()
				{
					if (shouldAutoUndo())
					{
						// save undoValue
						undoValue = new Vector2(vector);
					}
				}
			};
			final LttlClosure after = new LttlClosure()
			{
				@Override
				public void run()
				{
					if (shouldAutoUndo())
					{
						// create undo
						registerUndo(new UndoState(GuiComponentObject.this,
								new Vector2(vector)));
					}

					onEditorValueChange();
				}
			};

			// set from click
			JMenu set = new JMenu("Set From Click");
			JMenuItem setAsWorld = new JMenuItem("World");
			set.add(setAsWorld);
			setAsWorld.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					Lttl.editor.getInput().setVector2FromEditorClick(
							new LttlCallback()
							{
								@Override
								public void callback(int id, Object... objects)
								{
									before.run();

									vector.set((Vector2) objects[0]);

									after.run();
								}
							});
				}
			});
			JMenuItem setAsLocal = new JMenuItem("Local");
			set.add(setAsLocal);
			setAsLocal.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					Lttl.editor.getInput().setVector2FromEditorClick(
							new LttlCallback()
							{
								@Override
								public void callback(int id, Object... objects)
								{
									before.run();

									vector.set((Vector2) objects[0]);
									LttlComponent parentComponent = getParentComponent();
									if (Lttl.quiet(parentComponent)) { return; }
									parentComponent.t().worldToLocalPosition(
											vector, true);

									after.run();
								}
							});
				}
			});
			JMenuItem setAsChild = new JMenuItem("Child");
			set.add(setAsChild);
			setAsChild.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					Lttl.editor.getInput().setVector2FromEditorClick(
							new LttlCallback()
							{
								@Override
								public void callback(int id, Object... objects)
								{
									before.run();

									vector.set((Vector2) objects[0]);
									LttlComponent parentComponent = getParentComponent();
									if (Lttl.quiet(parentComponent)) { return; }
									parentComponent.t().worldToChildPosition(
											vector, true);

									after.run();
								}
							});
				}
			});
			JMenuItem setAsRender = new JMenuItem("Render");
			set.add(setAsRender);
			setAsRender.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					Lttl.editor.getInput().setVector2FromEditorClick(
							new LttlCallback()
							{
								@Override
								public void callback(int id, Object... objects)
								{
									before.run();

									vector.set((Vector2) objects[0]);
									LttlComponent parentComponent = getParentComponent();
									if (Lttl.quiet(parentComponent)) { return; }
									parentComponent.t().worldToRenderPosition(
											vector, true);

									after.run();
								}
							});
				}
			});
			popup.add(set);

			// zeros
			JMenuItem setZeros = new JMenuItem("Set To Zeros");
			popup.add(setZeros);
			setZeros.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					before.run();
					vector.set(0, 0);
					after.run();
				}
			});

			// ones
			JMenuItem setOnes = new JMenuItem("Set To Ones");
			popup.add(setOnes);
			setOnes.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					before.run();
					vector.set(1, 1);
					after.run();
				}
			});

			popup.add(new JSeparator());
		}
	}

	protected void drawComponentFieldsShared()
	{
		// check if this object should be two columns
		if (objectRef.getClass().isAnnotationPresent(GuiTwoColumn.class)
				|| GuiHelper.GetTwoColumnObjectClasses().contains(
						objectRef.getClass()))
		{
			twoColumnObject = true;
		}

		// reset gbcs
		int gridyCount = 0;
		int currentGridY = 0;
		int gridX = 0;

		// add each field object
		for (ProcessedFieldType pft : LttlObjectGraphCrawler.getAllFields(
				objectRef.getClass(), FieldsMode.GUI, this.pft.getParam(0)))
		{
			GuiFieldObject<?> child = null;

			/* CREATE CHILD GFO */
			/* Special Object Cases */
			// check if a string select (gets any string field inside these objects)
			if (objectRef.getClass() == LttlTexture.class
					&& pft.getCurrentClass() == String.class)
			{
				child = new GuiComponentSelectTexture(pft, objectRef, -1, this);
			}
			else if (objectRef.getClass() == LttlTextureAnimation.class
					&& pft.getCurrentClass() == String.class)
			{
				child = new GuiComponentSelectTextureAnimation(pft, objectRef,
						-1, this);
			}
			else if (objectRef.getClass() == LttlSound.class
					&& pft.getCurrentClass() == String.class)
			{
				child = new GuiComponentSelectSound(pft, objectRef, -1, this);
			}
			else if (objectRef.getClass() == LttlMusic.class
					&& pft.getCurrentClass() == String.class)
			{
				child = new GuiComponentSelectMusic(pft, objectRef, -1, this);
			}
			else if (objectRef.getClass() == LttlFontGenerator.class
					&& pft.getField().getName().equals("fontName"))
			// specifies unique field to use
			{
				child = new GuiComponentSelectFont(pft, objectRef, -1, this);
			}
			else
			{
				// normal
				child = generateChildGuiFieldObject(pft, -1);
			}
			if (child == null) continue;

			/* CREATE GUI GROUP */
			// check if field is in a group (if this is not already a gui component object for a specific gui group)
			GuiComponentObject group = this;
			if (pft.getField().isAnnotationPresent(GuiGroup.class))
			{
				GuiGroup annotation = pft.getField().getAnnotation(
						GuiGroup.class);
				// iterate through all the groups in the array (nested)
				for (String s : annotation.value())
				{
					if (group.guiGroupMap == null)
					{
						group.guiGroupMap = new HashMap<String, GuiComponentObject>();
					}
					// check if the current level group has this inner group already
					if (group.guiGroupMap.containsKey(s))
					{
						group = group.guiGroupMap.get(s);
					}
					else
					{
						// create the inner group on the current group because it's new and save to map
						GuiComponentObject newGroup = new GuiComponentObject(
								this, objectRef, s);
						group.collapsableGroup.getCollapsePanel().add(
								newGroup.getPanel(), gbcStatic);
						// save it in this group's map
						group.guiGroupMap.put(s, newGroup);
						newGroup.parentGroup = group;
						group = newGroup;
					}
				}
			}

			boolean singleTwoColumn = !twoColumnObject
					&& pft.getField() != null
					&& pft.getField().isAnnotationPresent(GuiTwoColumn.class);

			if (twoColumnObject || singleTwoColumn)
			{
				gbc.gridwidth = 1;
				if (gridX == 0)
				{
					// left
					gbc.gridx = 0;
					gbc.gridy = gridyCount;
					currentGridY = gridyCount;
					gridyCount++;
					gridX++;
				}
				else
				{
					// right
					gbc.gridx = 1;
					gbc.gridy = currentGridY;
					gridX = 0;
				}
				group.collapsableGroup.getCollapsePanel().add(child.getPanel(),
						gbc);
			}
			else
			{
				gbc.gridx = 0;
				gbc.gridy = gridyCount;
				gbc.gridwidth = 2;
				group.collapsableGroup.getCollapsePanel().add(child.getPanel(),
						gbc);
				gridyCount++;
			}

			// if child is being added to a group, add to it's child list, this is really only used for if the group is
			// a savedGFO and is being imported, need to know which GFOs to import
			if (group != this)
			{
				child.parentGroup = group;
				group.children.add(child);
			}
			// always add the children to this GuiComponentObject, even if it's in a group
			// this helps with managing updates and things like that
			// all child parents will be THIS not it's group
			children.add(child);
		}
	}

	/**
	 * clears all children and clears main panel
	 */
	protected void drawComponentFrameworkShared()
	{
		// reset
		children.clear();
		getPanel().removeAll();

		// setup panel
		getPanel().setLayout(new GridBagLayout());

		// create custom collapse panel
		collapsableGroup = new GuiLttlCollapsableGroup(getLabelText(), true,
				true);

		if (Lttl.game.isPlayingEditor() && isExportable())
		{
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 99;
			collapsableGroup.getHeaderPanel().add(getSaveToggle(), gbc);
		}

		collapsableGroup.addCollapseListener(new LttlCallback()
		{
			@Override
			public void callback(int id, Object... objects)
			{
				collapsableGroup.label = getLabelText();
			}
		});

		// add collapse panel
		getPanel().add(
				collapsableGroup.getPanel(),
				new GridBagConstraints(0, 0, 1, 1, 1, 0,
						GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
						new Insets(0, 0, 0, 0), 0, 0));
	}

	protected void drawComponentObjectShared()
	{
		if (getField() != null
				&& getField().isAnnotationPresent(GuiAutoExpand.class))
		{
			collapsableGroup.setCollapseState(false);
		}

		// add listener for when state changes, check if gui change as soon as uncollapses
		collapsableGroup.getToggleButton().addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				// left click
				if (e.getButton() == MouseEvent.BUTTON1)
				{
					// not sure why this is like this
					boolean collapsed = !collapsableGroup.isCollapsed();

					GuiFieldObject<?> highest = getHighestParent();
					if (GuiComponentObject.class.isAssignableFrom(highest
							.getClass())
							&& Lttl.editor.getInput().isControlSwing())
					{
						if (parent == null)
						{
							Lttl.editor.getGui().getPropertiesController()
									.collapseAll();
						}
						((GuiComponentObject) highest).collapseAllDecendants();
						forceVisible();
						collapsed = false;
					}
					else
					{
						collapsed = !collapsed;
					}

					collapsableGroup.setCollapseState(collapsed);
				}
			}
		});

		setFieldLabelToolTip();

		GuiHelper.SetFontSize(collapsableGroup.getToggleButton(),
				GuiHelper.fieldFontSize);
	}

	protected void addMethodMenuItems(final JPopupMenu popup)
	{
		boolean atleastOne = false;

		HashMap<String, JMenu> groupMap = new HashMap<String, JMenu>();

		// get all GuiButton valid methods
		ArrayList<Method> methods = new ArrayList<Method>();
		for (Method m : LttlObjectGraphCrawler.getAllMethods(objectRef
				.getClass()))
		{
			// check for annotation and no parameters
			final Class<?>[] paramTypes = m.getParameterTypes();
			if (m.isAnnotationPresent(GuiButton.class))
			{
				if (paramTypes.length == 0
						|| (paramTypes.length == 1 && paramTypes[0] == GuiFieldObject.class))
				{
					methods.add(m);
				}
				else
				{
					Lttl.Throw("GuiButton: parameters error on method '"
							+ m.getName() + "' on class + "
							+ objectRef.getClass().getSimpleName());
				}
				GuiButton gb = m.getAnnotation(GuiButton.class);
				if (!gb.group().isEmpty() && !groupMap.containsKey(gb.group()))
				{
					JComponent prevGroup = popup;
					String sKey = "";
					for (String s : gb.group().split(","))
					{
						sKey += s + ",";
						JComponent group = groupMap.get(sKey);
						if (group == null)
						{
							group = new JMenu(s);
							prevGroup.add(group);
							groupMap.put(sKey, (JMenu) group);
						}
						prevGroup = group;
					}
				}
			}

		}

		// sort the methods by their order numbers, and if none, then alphabetical
		Collections.sort(methods, new Comparator<Method>()
		{
			@Override
			public int compare(Method o1, Method o2)
			{
				GuiButton gb1 = o1.getAnnotation(GuiButton.class);
				GuiButton gb2 = o2.getAnnotation(GuiButton.class);
				if (gb1.order() == gb2.order())
				{
					// if both undefined order, then sort by alphabetical
					if (gb1.order() == Integer.MAX_VALUE) { return LttlHelper
							.SortCheckAlphaNumeric(o1.getName(), o2.getName()); }
					return 0;
				}
				else if (gb1.order() > gb2.order()) { return 1; }
				return -1;
			};
		});

		for (final Method m : methods)
		{
			GuiButton gb = m.getAnnotation(GuiButton.class);

			// create menu item with method name
			JMenuItem menuItem = new JMenuItem(!gb.name().isEmpty() ? gb.name()
					: LttlHelper.toTitleCase(m.getName()));
			final Class<?>[] paramTypes = m.getParameterTypes();
			menuItem.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					try
					{
						boolean inAccessible = false;
						if (!m.isAccessible())
						{
							inAccessible = true;
							m.setAccessible(true);
						}
						if (paramTypes.length == 1)
						{
							m.invoke(objectRef, GuiComponentObject.this);
						}
						else
						{
							m.invoke(objectRef);
						}
						if (inAccessible)
						{
							m.setAccessible(false);
						}
					}
					catch (IllegalAccessException | IllegalArgumentException
							| InvocationTargetException e1)
					{
						e1.printStackTrace();
					}
				}
			});

			// set tooltip (if any)
			if (!gb.tooltip().isEmpty())
			{
				menuItem.setToolTipText(gb.tooltip());
			}
			// enable or disable button based on mode
			menuItem.setEnabled(gb.mode() == 0
					|| (gb.mode() == 1 && !Lttl.game.isPlaying())
					|| (gb.mode() == 2 && !Lttl.game.isPlaying()));

			// add to menu
			atleastOne = true;

			// add to group if specified
			if (!gb.group().isEmpty())
			{
				String sKey = "";
				for (String s : gb.group().split(","))
				{
					sKey += s + ",";
				}
				JMenu group = groupMap.get(sKey);
				group.add(menuItem);
			}
			else
			{
				// add to popup
				popup.add(menuItem);
			}
		}

		if (atleastOne)
		{
			popup.add(new JSeparator());
		}
	}

	void updateComponentToggleButton()
	{
		LttlComponent component = (LttlComponent) objectRef;
		GuiHelper.SetFontColor(collapsableGroup.getToggleButton(),
				new java.awt.Color(component.isEnabledSelf() ? 0 : 1, 0, 0,
						component.isEnabledSelf() ? .65f : .5f));
		collapsableGroup.getToggleButton().repaint();
	}

	// have to override because it is a toggleButton not a JLabel
	@Override
	void updateLabel()
	{
		collapsableGroup.setLabel(getLabelText());
	}

	/**
	 * Generates a child GUI Field object.
	 * 
	 * @param pft
	 * @param index
	 *            if list
	 * @return
	 */
	protected GuiFieldObject<?> generateChildGuiFieldObject(
			ProcessedFieldType pft, int index)
	{
		Class<?> type = pft.getCurrentClass();

		// DECIDE BASED ON TYPE
		if (LttlObjectGraphCrawler.isPrimative(type))
		{
			if (type == String.class)
			{
				return new GuiComponentString(pft, objectRef, index, this);
			}
			else if (type == int.class || type == Integer.class)
			{
				if (pft.getField() != null
						&& pft.getField().isAnnotationPresent(GuiTagBit.class))
				{
					return new GuiComponentTagBit<Integer>(pft, objectRef,
							index, this);
				}
				else
				{
					return new GuiComponentNumber<Integer>(pft, objectRef,
							index, this);
				}
			}
			else if (type == short.class || type == Short.class)
			{
				if (pft.getField() != null
						&& pft.getField().isAnnotationPresent(GuiTagBit.class))
				{
					return new GuiComponentTagBit<Short>(pft, objectRef, index,
							this);
				}
				else
				{
					return new GuiComponentNumber<Short>(pft, objectRef, index,
							this);
				}
			}
			else if (type == long.class || type == Long.class)
			{
				return new GuiComponentNumber<Long>(pft, objectRef, index, this);
			}
			else if (type == byte.class || type == Byte.class)
			{
				return new GuiComponentNumber<Byte>(pft, objectRef, index, this);
			}
			else if (type == boolean.class || type == Boolean.class)
			{
				return new GuiComponentBoolean(pft, objectRef, index, this);
			}
			else if (type == float.class || type == Float.class)
			{
				return new GuiComponentNumber<Float>(pft, objectRef, index,
						this);
			}
			else if (type == double.class || type == Double.class)
			{
				return new GuiComponentNumber<Double>(pft, objectRef, index,
						this);
			}
			else if (type.isEnum())
			{
				return new GuiComponentEnum(pft, objectRef, index, this);
			}
			else
			{
				Lttl.Throw("Field " + pft.getField().getName()
						+ " is not an allowed primative type.");
			}
		}
		else if (type == ArrayList.class)
		{
			return new GuiComponentArrayList(pft, objectRef, index, this);
		}
		else if (type == Color.class)
		{
			return new GuiComponentColor(pft, objectRef, index, this);
		}
		else if (LttlComponent.class.isAssignableFrom(type)
				|| type.isInterface())
		{
			return new GuiComponentComponentRef(pft, objectRef, index, this);
		}
		else if (type == LttlTimeline.class)
		{
			return new GuiComponentTimeline(pft, objectRef, index, this);
		}
		else if (type == LttlGradient.class)
		{
			return new GuiComponentGradient(pft, objectRef, index, this);
		}
		else
		{
			return new GuiComponentObject(pft, objectRef, index, this);
		}
		return null;
	}

	void collapseAllDecendants()
	{
		// only collapse children if not a gui group, because then they would be collapsed once in real GFO and once in
		// guiGroup
		if (!isGuiGroup())
		{
			for (GuiFieldObject<?> child : children)
			{
				if (!GuiComponentObject.class
						.isAssignableFrom(child.getClass())) continue;

				GuiComponentObject gco = (GuiComponentObject) child;
				// collapse all children
				gco.collapseAllDecendants();
				// collapse self
				// check if it has a collapse group, might not if null
				if (gco.collapsableGroup != null)
				{
					gco.collapsableGroup.setCollapseState(true);
				}
			}
		}

		// collapse all inner groups
		if (guiGroupMap != null)
		{
			for (GuiComponentObject innerGroup : guiGroupMap.values())
			{
				// collapse inner groups
				innerGroup.collapseAllDecendants();

				// collapse self
				innerGroup.collapsableGroup.setCollapseState(true);
			}
		}
	}

	final boolean isGuiGroup()
	{
		return guiGroupName != null;
	}
}
