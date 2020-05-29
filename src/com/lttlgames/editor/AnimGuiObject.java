package com.lttlgames.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;

import com.badlogic.gdx.utils.IntArray;
import com.lttlgames.editor.annotations.IgnoreCrawl;
import com.lttlgames.helpers.LttlCallback;
import com.lttlgames.helpers.LttlHelper;
import com.lttlgames.helpers.LttlMath;

@IgnoreCrawl
public class AnimGuiObject extends AnimGuiBase
{

	/**
	 * The actual anim object that is persisted on the AnimationObject
	 */
	public AnimatedObject animObject;
	public ArrayList<AnimGuiSequence> sequenceAnimGUIs = new ArrayList<AnimGuiSequence>();
	public ArrayList<AnimGuiObject> children = new ArrayList<AnimGuiObject>();
	String level = "";
	private float undoGroupTime;
	public Class<?> targetClass;

	public AnimGuiObject(final GuiAnimationEditor editor,
			AnimGuiObject parentGui, AnimatedObject animObject,
			ProcessedFieldType pft)
	{
		super(editor);
		this.pft = pft;
		this.animObject = animObject;
		this.parentGui = parentGui;

		updateTargetClass();

		if (parentGui != null)
		{
			level = parentGui.level + ">";
		}

		group = new GuiLttlCollapsableGroup("", false, false, 0, false, true,
				true);

		updateLabel();

		group.getToggleButton().setFont(
				group.getToggleButton().getFont().deriveFont(Font.BOLD));
		GuiHelper
				.SetFontColor(group.getToggleButton(), new Color(0, 0, 0, .5f));

		final JPopupMenu namePopup = new JPopupMenu();

		setIndexMenu(namePopup);

		final JMenuItem addMenuItem = new JMenuItem();
		final JMenuItem addAll = new JMenuItem("Add All");

		if (pft != null
				&& (pft.getCurrentClass() == ArrayList.class || pft
						.getCurrentClass().isArray()))
		{
			addMenuItem.setText("Add Item");
			namePopup.add(addMenuItem);

			addMenuItem.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					createFieldSequence(-1, getListPft(), false);
				}
			});
		}
		else
		{
			addMenuItem.setText("Add Field");
			namePopup.add(addMenuItem);

			addMenuItem.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					// get options
					final HashMap<Integer, ProcessedFieldType> fieldMap = getSharedFieldMap(true);
					final ArrayList<String> options = new ArrayList<String>();
					for (Entry<Integer, ProcessedFieldType> ee : fieldMap
							.entrySet())
					{
						options.add(ee.getValue().getField().getName());
					}

					String choice = GuiHelper.showComboboxModal(
							"Add Field Sequence", "", false, true, true,
							options.toArray(new String[options.size()]));
					if (choice != null)
					{
						for (Entry<Integer, ProcessedFieldType> ee : fieldMap
								.entrySet())
						{
							if (ee.getValue().getField().getName()
									.equals(choice))
							{
								createFieldSequence(ee.getKey(), ee.getValue(),
										false);
								break;
							}
						}
					}
				}
			});

			namePopup.add(addAll);
			addAll.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					addAll();
				}
			});
		}

		final JMenuItem activateAll = new JMenuItem("Activate All");
		activateAll.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				activateAll(false);
				editor.updateAnim();
			}
		});
		namePopup.add(activateAll);

		final JMenuItem deactivateAll = new JMenuItem("Deactivate All");
		deactivateAll.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				activateAll(true);
				editor.updateAnim();
			}
		});
		namePopup.add(deactivateAll);

		JMenu deleteMenu = new JMenu("Delete");
		JMenuItem confirmDeleteMenuItem = new JMenuItem("Confirm");
		confirmDeleteMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (AnimGuiObject.this.parentGui != null)
				{
					// remove from animation object
					LttlHelper.RemoveHashMapValue(
							AnimGuiObject.this.parentGui.animObject.children,
							AnimGuiObject.this.animObject, true, false);

					// remove from GUI
					AnimGuiObject.this.parentGui.children
							.remove(AnimGuiObject.this);
					AnimGuiObject.this.parentGui.group.getCollapsePanel()
							.remove(AnimGuiObject.this.group.getPanel());
					AnimGuiObject.this.parentGui.group.getPanel().revalidate();
					AnimGuiObject.this.parentGui.group.getPanel().repaint();
				}
				else
				{
					// remove from animation object
					editor.anim.animComps.remove(AnimGuiObject.this.animObject);

					// remove from GUI
					editor.animCompsGUI.remove(AnimGuiObject.this);
					editor.compPanel.remove(AnimGuiObject.this.group.getPanel());
					editor.compPanel.revalidate();
					editor.compPanel.repaint();
				}

				// update
				editor.updateAnim();
				editor.refreshMasterSliderAndAllGroups(false);
			}
		});
		deleteMenu.add(confirmDeleteMenuItem);
		namePopup.add(deleteMenu);

		// create popup menu listener
		group.getToggleButton().addMouseListener(new MouseAdapter()
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
				if (e.isPopupTrigger())
				{
					// show popup menu
					namePopup.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});

		final JPanel top = new JPanel(new GridBagLayout());
		final GridBagConstraints topGB = new GridBagConstraints();
		topGB.fill = GridBagConstraints.HORIZONTAL;
		topGB.anchor = GridBagConstraints.WEST;
		topGB.gridx = 0;
		topGB.gridy = 0;
		topGB.gridheight = 1;
		topGB.gridwidth = 1;
		topGB.weightx = 1;

		// create the target field, but only if it is an animated component
		if (animObject.getClass() == AnimatedComponent.class)
		{
			addMenuItem.setEnabled(addMenuItem.isEnabled()
					&& ((AnimatedComponent) animObject).target != null);
			addAll.setEnabled(addMenuItem.isEnabled());
			AnimatedComponent animComp = (AnimatedComponent) animObject;
			// getting the target field on the AnimatedComponent
			final GuiComponentComponentRef targetGFO = new GuiComponentComponentRef(
					LttlObjectGraphCrawler.getFieldByNameOrId("908900",
							AnimatedComponent.class,
							(ProcessedFieldType[]) null), animComp, -1, null);
			targetGFO.setAutoRegisterUndo(false);
			targetGFO.getPanel().setPreferredSize(new Dimension(200, 20));
			targetGFO.componentRefLabel.setMargin(new Insets(-3, 0, 0, 0));

			final GuiComponentBoolean stepCallbackGFO = new GuiComponentBoolean(
					LttlObjectGraphCrawler.getFieldByNameOrId("908901",
							AnimatedComponent.class,
							(ProcessedFieldType[]) null), animComp, -1, null);
			stepCallbackGFO.setAutoRegisterUndo(false);
			stepCallbackGFO.toggle.setFocusable(false);

			final GuiComponentString relativeStateGFO = new GuiComponentString(
					LttlObjectGraphCrawler.getFieldByNameOrId("908902",
							AnimatedComponent.class,
							(ProcessedFieldType[]) null), animComp, -1, null);
			relativeStateGFO.setAutoRegisterUndo(false);
			relativeStateGFO.getPanel()
					.setPreferredSize(new Dimension(200, 20));
			relativeStateGFO.addChangeListener(new GuiLttlChangeListener()
			{
				@Override
				void onChange(int changeId)
				{
					editor.updateAnim();
				}
			});

			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = GridBagConstraints.RELATIVE;
			gbc.gridy = GridBagConstraints.RELATIVE;
			gbc.weightx = 1;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.gridwidth = 1;
			// spacer
			group.getHeaderExtraPanel().add(new JLabel(), gbc);
			gbc.weightx = 0;
			gbc.fill = GridBagConstraints.NONE;
			group.getHeaderExtraPanel().add(targetGFO.getPanel(), gbc);
			group.getHeaderExtraPanel().add(stepCallbackGFO.getPanel(), gbc);
			group.getHeaderExtraPanel().add(relativeStateGFO.getPanel(), gbc);

			// filler
			gbc.weightx = 1;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			top.add(new JLabel(), gbc);
			targetGFO.addChangeListener(new GuiLttlChangeListener()
			{
				@Override
				void onChange(int changeId)
				{
					LttlComponent target = ((AnimatedComponent) AnimGuiObject.this.animObject).target;
					// if the new target is not null and assignable from the previous target class keep the animation
					// nodes
					if (!(target != null
							&& AnimGuiObject.this.targetClass != null && AnimGuiObject.this.targetClass
							.isAssignableFrom(target.getClass())))
					{
						updateTargetClass();
						addMenuItem.setEnabled(target != null);
						addAll.setEnabled(addMenuItem.isEnabled());

						// clear all animated object data
						AnimGuiObject.this.animObject.sequences.clear();
						AnimGuiObject.this.animObject.children.clear();

						// clear all gui
						group.getCollapsePanel().removeAll();
						sequenceAnimGUIs.clear();
						children.clear();
						refreshGroupSlider();

						// except for top portion
						group.getCollapsePanel().add(top, topGB);
					}

					AnimGuiObject.this.updateLabel();

					group.getPanel().revalidate();
					group.getPanel().repaint();

					AnimGuiObject.this.editor.updateAnim();
				}
			});
		}

		// create object group timeline
		this.multiSlider = new GuiLttlMultiSlider(0, editor.sliderMax,
				new LttlCallback()
				{
					ArrayList<TimelineNode> focusedNodes;
					IntArray cachedShiftTimes;
					MouseAdapter mouseAdapter;
					int mouseXPos;

					@Override
					public void callback(int id, Object... values)
					{
						if (id == 0)
						{
							// right clicked on no node, show create group nodes
							final ArrayList<AnimGuiSequence> allChildSequenceAnimGUIs = getAllChildSequenceAnimGUIs(new ArrayList<AnimGuiSequence>());

							final MouseEvent mouseEvent = (MouseEvent) values[0];
							final float time = editor.tickToTime(multiSlider
									.getValueForXPosition(mouseEvent.getX()));
							JPopupMenu popupMenu = new JPopupMenu();

							JMenuItem addKeyframe = new JMenuItem(
									"Add Keyframe");
							addKeyframe.addActionListener(new ActionListener()
							{
								@Override
								public void actionPerformed(ActionEvent e)
								{
									HashMap<AnimGuiSequence, ArrayList<TimelineNode>> undoValue = editor
											.getAllCurrentAnimSeqNodeMap();
									for (AnimGuiSequence sg : allChildSequenceAnimGUIs)
									{
										FieldKeyframeNode fk = new FieldKeyframeNode();
										fk.easeType = editor.defaultEase;
										fk.time = time;
										sg.addNode(fk, true);
										fk.updateValue();
									}
									editor.registerUndoState(undoValue,
											"Batch Edit");
									editor.refreshMasterSliderAndAllGroups(false);
								}
							});
							popupMenu.add(addKeyframe);

							JMenuItem addHold = new JMenuItem("Add Hold");
							addHold.addActionListener(new ActionListener()
							{
								@Override
								public void actionPerformed(ActionEvent e)
								{
									for (AnimGuiSequence sg : allChildSequenceAnimGUIs)
									{
										AnimationHoldNode ah = new AnimationHoldNode();
										ah.time = time;
										sg.addNode(ah, true);
									}
									editor.refreshMasterSliderAndAllGroups(false);
								}
							});
							popupMenu.add(addHold);

							JMenuItem pasteMenuItem = new JMenuItem("Paste");
							pasteMenuItem
									.addActionListener(new ActionListener()
									{
										@Override
										public void actionPerformed(
												ActionEvent e)
										{
											HashMap<AnimGuiSequence, ArrayList<TimelineNode>> undoValue = editor
													.getAllCurrentAnimSeqNodeMap();

											for (TimelineNode node : editor.capturedTimelineNodes)
											{
												TimelineNode timelineNodeCopy = LttlCopier
														.copy(node);
												timelineNodeCopy.time = time;
												node.sequenceGui
														.addNode(
																timelineNodeCopy,
																false);
											}

											editor.registerUndoState(undoValue,
													"Paste Nodes");

											editor.refreshMasterSliderAndAllGroups(false);
											editor.updateAnim();
										}
									});
							pasteMenuItem.setEnabled(editor.capturedTimelineNodes != null
									&& editor.capturedTimelineNodes.size() > 0
									&& editor.capturedTimelineMultiSlider == multiSlider);
							popupMenu.add(pasteMenuItem);

							popupMenu.show(mouseEvent.getComponent(),
									mouseEvent.getX(), mouseEvent.getY());
						}
						else if (id == 1)
						{
							// right clicked on node
							final MouseEvent mouseEvent = (MouseEvent) values[0];
							final JSlider clickedSlider = (JSlider) values[1];
							final float time = editor.tickToTime(clickedSlider
									.getValue());

							final ArrayList<TimelineNode> nodes = getNodesAtTime(time);

							GuiLttlLabeledPopupMenu popupMenu = new GuiLttlLabeledPopupMenu(
									"Group Node ["
											+ nodes.size()
											+ "]"
											+ " - "
											+ new DecimalFormat("#.###").format(editor
													.tickToTime(clickedSlider
															.getValue())));

							JMenuItem batchEditMenuItem = new JMenuItem(
									"Batch Edit");
							batchEditMenuItem
									.addActionListener(new ActionListener()
									{
										@Override
										public void actionPerformed(
												ActionEvent e)
										{
											editor.createDummyNodeDialog(time,
													nodes);
										}
									});
							popupMenu.add(batchEditMenuItem);

							JMenuItem copyMenuItem = new JMenuItem("Copy");
							copyMenuItem.addActionListener(new ActionListener()
							{
								@Override
								public void actionPerformed(ActionEvent e)
								{
									editor.capturedTimelineNodes = getNodesAtTime(editor
											.tickToTime(clickedSlider
													.getValue()));
									editor.capturedTimelineMultiSlider = multiSlider;
								}
							});
							popupMenu.add(copyMenuItem);

							JMenuItem updateMenuItem = new JMenuItem("Update");
							updateMenuItem
									.addActionListener(new ActionListener()
									{
										@Override
										public void actionPerformed(
												ActionEvent e)
										{
											for (TimelineNode node : nodes)
											{
												if (node.getClass() == FieldKeyframeNode.class)
												{
													((FieldKeyframeNode) node)
															.updateValue();
												}
											}
											editor.updateAnim();
										}
									});
							popupMenu.add(updateMenuItem);

							JMenuItem isolateMenuItem = new JMenuItem("Isolate");
							isolateMenuItem
									.addActionListener(new ActionListener()
									{
										@Override
										public void actionPerformed(
												ActionEvent e)
										{
											// collapse everything in editor
											editor.collapseAll();

											// make visible certain ones
											for (TimelineNode node : nodes)
											{
												node.sequenceGui.makeVisible();
											}
										}
									});
							popupMenu.add(isolateMenuItem);

							JMenuItem showMenuItem = new JMenuItem("Show");
							showMenuItem.addActionListener(new ActionListener()
							{
								@Override
								public void actionPerformed(ActionEvent e)
								{
									// make visible certain ones
									for (TimelineNode node : nodes)
									{
										node.sequenceGui.makeVisible();
									}
								}
							});
							popupMenu.add(showMenuItem);

							JMenu deleteMenu = new JMenu("Delete All ("
									+ nodes.size() + ")");
							JMenuItem confirmDeleteMenuItem = new JMenuItem(
									"Confirm");
							confirmDeleteMenuItem
									.addActionListener(new ActionListener()
									{
										@Override
										public void actionPerformed(
												ActionEvent e)
										{
											HashMap<AnimGuiSequence, ArrayList<TimelineNode>> undoValue = editor
													.getAllCurrentAnimSeqNodeMap();

											// deleting a timelinenode
											int unableToDeleteCount = 0;
											for (TimelineNode node : nodes)
											{
												// can't delete the last node of a sequence
												if (node.sequenceGui.seq.nodes
														.size() > 1)
												{
													// don't refresh yet
													node.sequenceGui
															.removeNode(node,
																	false,
																	false);
												}
												else
												{
													unableToDeleteCount++;
												}
											}

											editor.registerUndoState(undoValue,
													"Delete Nodes");

											// now refresh since all deleted
											editor.refreshMasterSliderAndAllGroups(false);
											editor.updateAnim();
											if (unableToDeleteCount > 0)
											{
												GuiHelper
														.showAlert(
																editor.mainPanel,
																"Unable to delete",
																unableToDeleteCount
																		+ " node"
																		+ ((unableToDeleteCount > 1) ? "s"
																				: "")
																		+ " could not be deleted because sequence has only one node.",
																JOptionPane.WARNING_MESSAGE);
											}
										}
									});
							deleteMenu.add(confirmDeleteMenuItem);
							popupMenu.add(deleteMenu);

							popupMenu.show(mouseEvent.getComponent(),
									mouseEvent.getX(), mouseEvent.getY());
						}
						else if (id == 4)
						{
							// begin node drag
							JSlider clickedSlider = (JSlider) values[0];

							// get start time of this slider
							float time = editor.tickToTime(clickedSlider
									.getValue());

							// save undo time
							undoGroupTime = time;

							// grab all child the nodes that are at this time
							focusedNodes = getNodesAtTime(time);

							if (cachedShiftTimes == null)
							{
								cachedShiftTimes = new IntArray(false, 4);
							}
							// get all the node times for all sequences
							cachedShiftTimes.clear();
							editor.getAllNodeTimes(cachedShiftTimes);

							editor.focusedGroup = AnimGuiObject.this;

							mouseAdapter = new MouseAdapter()
							{
								public void mouseDragged(MouseEvent e)
								{
									mouseXPos = e.getX();
								}
							};
							clickedSlider.addMouseMotionListener(mouseAdapter);
						}
						else if (id == 3)
						{
							// dragging node

							if (focusedNodes != null)
							{
								final JSlider dragSlider = (JSlider) values[0];
								int tick = dragSlider.getValue();
								float time = editor.tickToTime(tick);

								if (Lttl.editor.getInput().isShiftSwing()
										&& cachedShiftTimes != null
										&& cachedShiftTimes.size > 0)
								{
									int realTick = multiSlider
											.getValueForXPosition(mouseXPos);
									int closestTick = cachedShiftTimes.get(0);
									for (int i = 1; i < cachedShiftTimes.size; i++)
									{
										if (LttlMath.abs(realTick
												- cachedShiftTimes.get(i)) < LttlMath
												.abs(realTick - closestTick))
										{
											closestTick = cachedShiftTimes
													.get(i);
										}
									}
									tick = closestTick;
									time = editor.tickToTime(closestTick);
								}

								// update all the node with new time
								for (TimelineNode node : focusedNodes)
								{
									node.time = time;
									node.slider.setValue(tick);
								}

								editor.refreshMasterSliderAndAllGroups(true);

								editor.updateCurrentTimeLabel(time);

								// now that node's time's have been updated
								editor.clampAndSetSeekTime();
								editor.setMinEditorDuration();
								editor.refreshSeekSlider();
								editor.updateAnim();
							}
						}
						else if (id == 5)
						{
							if (cachedShiftTimes != null)
							{
								cachedShiftTimes.clear();
							}

							JSlider dragSlider = (JSlider) values[0];
							dragSlider.removeMouseMotionListener(mouseAdapter);

							// if group node changed time position
							float time = editor.tickToTime(dragSlider
									.getValue());
							if (!LttlMath.isEqual(undoGroupTime, time))
							{
								final ArrayList<TimelineNode> undoNodes = focusedNodes;
								UndoState undoState = new UndoState(
										editor.manager.t().getName()
												+ " ("
												+ editor.manager.getClass()
														.getSimpleName()
												+ ") - Animations:"
												+ editor.anim.name
												+ ":Moved group animation node",
										new UndoField(editor.manager.t(),
												undoGroupTime, time,
												new UndoSetter()
												{
													@Override
													public void set(
															LttlComponent comp,
															Object value)
													{
														for (TimelineNode node : undoNodes)
														{
															node.time = (float) value;
														}
													}
												}));
								// save the undoState so we can remove all of them when animation window closes
								editor.undoStates.add(undoState);
								Lttl.editor.getUndoManager().registerUndoState(
										undoState);
							}

							focusedNodes = null;
							editor.focusedGroup = null;
							editor.refreshMasterSliderAndAllGroups(false);
						}
					}
				});
		JPanel multiSliderPanel = new JPanel(new BorderLayout());
		multiSliderPanel.setPreferredSize(new Dimension(1, 20));
		multiSliderPanel.add(multiSlider);
		editor.formatGroupSlider(multiSlider.getSlider(0));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridwidth = 2;
		top.add(multiSliderPanel, gbc);

		// add top to the collapse panel
		group.getCollapsePanel().add(top, topGB);

		// create child sequences and animation objects if any
		// only do this if it is not a component, or if it is a component, it must have a defined target
		if (parentGui != null
				|| ((AnimatedComponent) animObject).target != null)
		{
			HashMap<Integer, ProcessedFieldType> fieldMap = getSharedFieldMap(false);

			// check if there are any extra objects or sequences
			// if there is one, it will remove it and throw a warning
			// this usually happens if you remove a field or change an animation id.
			// add the sequences and objects already created
			for (Iterator<Entry<Integer, AnimationSequence>> it = animObject.sequences
					.entrySet().iterator(); it.hasNext();)
			{
				Entry<Integer, AnimationSequence> e = it.next();
				if (!isList() && doesntExist(fieldMap, e.getKey()))
				{
					it.remove();
					continue;
				}
				addToGuiField(e.getValue(),
						isList() ? getListPft() : fieldMap.get(e.getKey()));
			}
			for (Iterator<Entry<Integer, AnimatedObject>> it = animObject.children
					.entrySet().iterator(); it.hasNext();)
			{
				Entry<Integer, AnimatedObject> e = it.next();
				if (!isList() && doesntExist(fieldMap, e.getKey()))
				{
					it.remove();
					continue;
				}
				addToGuiField(e.getValue(),
						isList() ? getListPft() : fieldMap.get(e.getKey()));
			}
		}

		refreshGroupSlider();
	}

	public void updateTargetClass()
	{
		if (animObject.getClass() == AnimatedComponent.class
				&& ((AnimatedComponent) animObject).target != null)
		{
			targetClass = ((AnimatedComponent) animObject).target.getClass();
		}
	}

	private ProcessedFieldType getListPft()
	{
		if (pft.getCurrentClass() == ArrayList.class)
		{
			return AnimGuiObject.this.pft.getParam(0);
		}
		else
		{
			return new ProcessedFieldType(AnimGuiObject.this.pft
					.getCurrentClass().getComponentType());
		}
	}

	private boolean doesntExist(HashMap<Integer, ProcessedFieldType> fieldMap,
			int animID)
	{
		if (!fieldMap.containsKey(animID))
		{
			Lttl.logNote("Animation Import: Losing data because the animation ID "
					+ animID
					+ " does not exist on class "
					+ (pft != null ? pft.getCurrentClass().getSimpleName()
							: ((AnimatedComponent) animObject).target
									.getClass()));
			return true;
		}
		return false;
	}

	ArrayList<TimelineNode> getNodesAtTime(float time)
	{
		HashMap<Float, ArrayList<TimelineNode>> nodesMap = getNodesTimeMap();
		// if size == 0, then it must be the first node with no keyframes yet, there always has
		// to be at least one group node, regardless if it really doesn't relate to any
		// keyframes
		return nodesMap.size() == 0 ? new ArrayList<TimelineNode>() : nodesMap
				.get(time);

	}

	HashMap<Float, ArrayList<TimelineNode>> getNodesTimeMap()
	{
		return getNodesTimeMap(new HashMap<Float, ArrayList<TimelineNode>>());
	}

	/**
	 * Gets all nodes (self and children) and places in a map with the time as the key
	 * 
	 * @param sharedMap
	 * @return
	 */
	HashMap<Float, ArrayList<TimelineNode>> getNodesTimeMap(
			HashMap<Float, ArrayList<TimelineNode>> sharedMap)
	{
		// add all sequences' nodes
		for (AnimGuiSequence seqGui : sequenceAnimGUIs)
		{
			seqGui.getNodesTimeMap(sharedMap);
		}

		// all children objects
		for (AnimGuiObject child : this.children)
		{
			child.getNodesTimeMap(sharedMap);
		}

		return sharedMap;
	}

	/**
	 * Refreshes descendants
	 */
	void refreshGroupSliderSelfAndDescendants(boolean onlyVisible)
	{
		if (onlyVisible && group.isCollapsed()) return;

		// skip since it is already updating from dragging
		if (AnimGuiObject.this != editor.focusedGroup)
		{
			refreshGroupSlider();
		}
		for (AnimGuiObject child : children)
		{
			child.refreshGroupSliderSelfAndDescendants(onlyVisible);
		}
	}

	/**
	 * if no parent, will return self
	 * 
	 * @return
	 */
	AnimGuiObject getHighestAncestor()
	{
		if (parentGui == null)
		{
			return this;
		}
		else
		{
			return parentGui.getHighestAncestor();
		}
	}

	/**
	 * Refreshes self and ancestors
	 */
	void refreshGroupSliderSelfAndAncestors()
	{
		refreshGroupSlider();
		if (parentGui != null)
		{
			parentGui.refreshGroupSliderSelfAndAncestors();
		}
	}

	void refreshGroupSlider()
	{
		editor.refreshGroupSlider(multiSlider, getNodesTimeMap());
	}

	private HashMap<Integer, ProcessedFieldType> getSharedFieldMap(
			boolean onlyUnused)
	{
		HashMap<Integer, ProcessedFieldType> map;
		// check if component (root)
		if (animObject.getClass() == AnimatedComponent.class)
		{
			AnimatedComponent animComp = (AnimatedComponent) AnimGuiObject.this.animObject;
			// can assume target exists
			map = AnimGuiObject.this.animObject.getFieldsMap(
					animComp.target.getClass(), (ProcessedFieldType[]) null);
		}
		else
		{
			map = AnimGuiObject.this.animObject.getFieldsMap(
					AnimGuiObject.this.pft.getCurrentClass(),
					AnimGuiObject.this.pft.getParams());
		}

		if (onlyUnused)
		{
			HashMap<Integer, ProcessedFieldType> unusedMap = new HashMap<Integer, ProcessedFieldType>();
			for (Entry<Integer, ProcessedFieldType> ee : map.entrySet())
			{
				if (!AnimGuiObject.this.animObject.sequences.containsKey(ee
						.getKey())
						&& !AnimGuiObject.this.animObject.children
								.containsKey(ee.getKey()))
				{
					unusedMap.put(ee.getKey(), ee.getValue());
				}
			}
			return unusedMap;
		}
		else
		{
			return map;
		}
	}

	private void createFieldSequence(int animFieldId, ProcessedFieldType pft,
			boolean addAll)
	{
		if (LttlObjectGraphCrawler.isPrimative(pft.getCurrentClass()))
		{
			// create a sequence
			AnimationSequence newSeq = new AnimationSequence();

			// generates the animFieldId based on the next available index, since these ids represent the index for
			// an ArrayList
			if (isList())
			{
				animFieldId = getNextAvailableIndexInList(AnimGuiObject.this.animObject.sequences);
			}

			AnimGuiObject.this.animObject.sequences.put(animFieldId, newSeq);

			// add to GUI
			AnimGuiSequence sg = addToGuiField(newSeq, pft);

			if (addAll)
			{
				// start as collapsed
				sg.group.setCollapseState(true);
			}
		}
		else
		{
			// create another animated object
			AnimatedObject newAo = new AnimatedObject();

			// generates the animFieldId based on the next available index, since these ids represent the index for
			// an ArrayList
			if (isList())
			{
				animFieldId = getNextAvailableIndexInList(AnimGuiObject.this.animObject.children);
			}

			AnimGuiObject.this.animObject.children.put(animFieldId, newAo);

			// add to GUI
			AnimGuiObject og = addToGuiField(newAo, pft);

			if (addAll)
			{
				// start as collapsed
				og.group.setCollapseState(true);
				og.addAll();
			}
		}
	}

	private int getNextAvailableIndexInList(HashMap<Integer, ?> map)
	{
		int index = 0;
		while (true)
		{
			if (!map.containsKey(index)) { return index; }
			index++;
		}
	}

	private void addAll()
	{
		for (Entry<Integer, ProcessedFieldType> ee : getSharedFieldMap(true)
				.entrySet())
		{
			createFieldSequence(ee.getKey(), ee.getValue(), true);
		}
	}

	private AnimGuiSequence addToGuiField(AnimationSequence as,
			ProcessedFieldType pft)
	{
		AnimGuiSequence newSeqGUI = new AnimGuiSequence(editor, as, pft,
				AnimGuiObject.this);
		newSeqGUI.group.setCollapseState(true);
		AnimGuiObject.this.sequenceAnimGUIs.add(newSeqGUI);
		addToGuiField(newSeqGUI.group.getPanel());
		return newSeqGUI;
	}

	private AnimGuiObject addToGuiField(AnimatedObject ao,
			ProcessedFieldType pft)
	{
		AnimGuiObject newAoGUI = null;
		newAoGUI = new AnimGuiObject(editor, AnimGuiObject.this, ao, pft);
		newAoGUI.group.setCollapseState(true);
		AnimGuiObject.this.children.add(newAoGUI);
		addToGuiField(newAoGUI.group.getPanel());
		return newAoGUI;
	}

	private void addToGuiField(JPanel panel)
	{
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		gbc.gridx = 0;
		gbc.gridy = GridBagConstraints.RELATIVE;
		AnimGuiObject.this.group.getCollapsePanel().add(panel, gbc);
		AnimGuiObject.this.group.getPanel().revalidate();
		AnimGuiObject.this.group.getPanel().repaint();
	}

	/**
	 * Gets all the sequences on this object and all of it's children and adds to the provided list
	 * 
	 * @param list
	 * @return
	 */
	public ArrayList<AnimGuiSequence> getAllSequencesGUIs(
			ArrayList<AnimGuiSequence> list)
	{
		list.addAll(sequenceAnimGUIs);
		for (AnimGuiObject child : children)
		{
			child.getAllSequencesGUIs(list);
		}
		return list;
	}

	private void activateAll(boolean deactivate)
	{
		for (AnimGuiSequence s : sequenceAnimGUIs)
		{
			s.seq.active = !deactivate;
			s.updateLabel();
		}
		for (AnimGuiObject child : children)
		{
			child.activateAll(deactivate);
		}
	}

	void collapseAll()
	{
		group.setCollapseState(true);
		for (AnimGuiSequence sg : sequenceAnimGUIs)
		{
			sg.group.setCollapseState(true);
		}
		for (AnimGuiObject child : children)
		{
			child.collapseAll();
		}
	}

	void makeVisible()
	{
		group.setCollapseState(false);
		if (parentGui != null)
		{
			parentGui.makeVisible();
		}
	}

	private String getAnimationCompLabel()
	{
		return ((AnimatedComponent) animObject).target != null ? ((AnimatedComponent) animObject).target
				.getClass().getSimpleName()
				+ " ["
				+ ((AnimatedComponent) animObject).target.t().getName() + "]"
				: "Undefined";
	}

	ArrayList<AnimGuiSequence> getAllChildSequenceAnimGUIs(
			ArrayList<AnimGuiSequence> list)
	{
		// add direct sequences
		for (AnimGuiSequence sg : sequenceAnimGUIs)
		{
			list.add(sg);
		}

		// add children
		for (AnimGuiObject child : children)
		{
			child.getAllChildSequenceAnimGUIs(list);
		}
		return list;
	}

	@Override
	void updateCollapseMap(HashMap<GuiLttlCollapsableGroup, Boolean> map)
	{
		super.updateCollapseMap(map);

		for (AnimGuiSequence sg : sequenceAnimGUIs)
		{
			sg.updateCollapseMap(map);
		}

		for (AnimGuiObject child : children)
		{
			child.updateCollapseMap(map);
		}
	}

	@SuppressWarnings("rawtypes")
	public Object getObject()
	{
		if (animObject.getClass() == AnimatedComponent.class)
		{
			return ((AnimatedComponent) animObject).target;
		}
		else
		{
			Object parentObj = parentGui.getObject();

			if (parentObj == null) { return null; }

			Object selfObj = null;
			if (parentObj.getClass() == ArrayList.class)
			{
				int index = getAnimId();
				if (index < ((ArrayList) parentObj).size())
				{
					selfObj = ((ArrayList) parentObj).get(index);
				}
				else
				{
					selfObj = null;
				}
			}
			else if (parentObj.getClass().isArray())
			{
				int index = getAnimId();
				if (index < Array.getLength(parentObj))
				{
					selfObj = Array.get(parentObj, index);
				}
				else
				{
					selfObj = null;
				}
			}
			else
			{
				try
				{
					Field f = pft.getField();
					boolean accessible = true;
					if (!f.isAccessible())
					{
						accessible = false;
						f.setAccessible(true);
					}

					selfObj = pft.getField().get(parentObj);
					if (!accessible)
					{
						f.setAccessible(false);
					}
				}
				catch (IllegalArgumentException | IllegalAccessException e)
				{
					e.printStackTrace();
				}
			}

			return selfObj;
		}
	}

	@Override
	public int getAnimId()
	{
		// needs to have a parent
		Lttl.Throw(parentGui);

		return LttlHelper.GetHashMapFirstKey(parentGui.animObject.children,
				animObject, true);
	}

	@Override
	public void updateLabel()
	{
		if (parentGui == null)
		{
			group.setLabel(getAnimationCompLabel());
		}
		else
		{
			// can't have a component reference field by animated
			Lttl.Throw(LttlComponent.class.isAssignableFrom(pft
					.getCurrentClass()));
			group.setLabel(level + " "
					+ (isListItem() ? getAnimId() : pft.getField().getName()));
		}
	}

	@Override
	protected void listItemModifyIndex(int newIndex)
	{
		LttlHelper.RemoveHashMapValue(
				AnimGuiObject.this.parentGui.animObject.children,
				AnimGuiObject.this.animObject, true, false);
		AnimGuiObject.this.parentGui.animObject.children.put(newIndex,
				AnimGuiObject.this.animObject);
	}
}
