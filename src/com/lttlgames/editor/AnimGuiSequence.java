package com.lttlgames.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;

import com.badlogic.gdx.utils.IntArray;
import com.lttlgames.editor.annotations.IgnoreCrawl;
import com.lttlgames.helpers.LttlCallback;
import com.lttlgames.helpers.LttlHelper;
import com.lttlgames.helpers.LttlMath;

@IgnoreCrawl
public class AnimGuiSequence extends AnimGuiBase
{
	public AnimationSequence seq;

	private String level = "";

	public AnimGuiSequence(final GuiAnimationEditor editor,
			final AnimationSequence seq)
	{
		this(editor, seq, null, null);
	}

	public AnimGuiSequence(final GuiAnimationEditor editor,
			final AnimationSequence seq, ProcessedFieldType pft,
			final AnimGuiObject parentGui)
	{
		super(editor);

		this.pft = pft;
		this.seq = seq;
		this.parentGui = parentGui;
		group = new GuiLttlCollapsableGroup("", false, false, 0, false, true,
				false);

		group.getToggleButton().setFont(
				group.getToggleButton().getFont().deriveFont(Font.BOLD));

		updateLabel();

		if (parentGui != null)
		{
			level = parentGui.level + ">";
		}

		final JPopupMenu namePopup = new JPopupMenu();

		setIndexMenu(namePopup);

		final JMenuItem enableToggle = new JMenuItem("Disable");
		enableToggle.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				seq.active = !seq.active;
				editor.updateAnim();
				updateLabel();
				group.getToggleButton().repaint();
			}
		});
		namePopup.add(enableToggle);

		final JMenuItem copyAll = new JMenuItem("Copy All");
		copyAll.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				editor.capturedTimelineNodes = new ArrayList<TimelineNode>();
				editor.capturedTimelineNodes.addAll(seq.nodes);
				editor.capturedTimelineMultiSlider = null;
			}
		});
		namePopup.add(copyAll);

		final JMenuItem pasteAll = new JMenuItem("Paste");
		pasteAll.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				HashMap<AnimGuiSequence, ArrayList<TimelineNode>> undoValue = editor
						.getCurrentAnimSeqNodeMap(AnimGuiSequence.this);

				for (TimelineNode node : editor.capturedTimelineNodes)
				{
					TimelineNode timelineNodeCopy = LttlCopier.copy(node);
					addNode(timelineNodeCopy, false);
				}

				editor.registerUndoState(undoValue, "Paste Nodes");

				editor.refreshMasterSliderAndAllGroups(false);
				editor.updateAnim();
			}
		});
		namePopup.add(pasteAll);

		final JMenuItem clearMenuItem = new JMenuItem("Clear");
		clearMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				HashMap<AnimGuiSequence, ArrayList<TimelineNode>> undoValue = editor
						.getCurrentAnimSeqNodeMap(AnimGuiSequence.this);

				for (TimelineNode node : new ArrayList<TimelineNode>(seq.nodes))
				{
					removeNode(node, false, false);
				}

				editor.registerUndoState(undoValue, "Clear Nodes");

				editor.refreshMasterSliderAndAllGroups(false);
				editor.updateAnim();
			}
		});
		namePopup.add(clearMenuItem);

		final JMenuItem easeItem = new JMenuItem("Set All Eases To Default");
		easeItem.addActionListener(new ActionListener()
		{
			HashMap<AnimGuiSequence, ArrayList<TimelineNode>> undoValue = editor
					.getCurrentAnimSeqNodeMap(AnimGuiSequence.this);

			@Override
			public void actionPerformed(ActionEvent e)
			{
				for (TimelineNode node : seq.nodes)
				{
					if (KeyframeNode.class.isAssignableFrom(node.getClass()))
					{
						((KeyframeNode) node).easeType = editor.defaultEase;
					}
				}

				editor.registerUndoState(undoValue, "Set Node Eases To "
						+ editor.defaultEase.name());

				editor.updateAnim();
			}
		});
		namePopup.add(easeItem);

		JMenuItem renameMenuItem = new JMenuItem("Rename");
		renameMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				String newName = GuiHelper.showTextFieldModal(
						"Animation Sequence Name", "");
				// no name change, escaped
				if (newName == null) return;

				if (editor.anim.getStateSequenceNames().contains(newName))
				{
					Lttl.logNote("Can't rename sequence because name taken.");
					return;
				}
				seq.name = newName;
				updateLabel();
			}
		});
		namePopup.add(renameMenuItem);
		JMenu deleteMenu = new JMenu("Delete");
		JMenuItem confirmDeleteMenuItem = new JMenuItem("Confirm");
		confirmDeleteMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				editor.removeSequence(AnimGuiSequence.this);
				editor.refreshMasterSliderAndAllGroups(false);
				editor.updateAnim();
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
					pasteAll.setEnabled(editor.capturedTimelineNodes != null
							&& editor.capturedTimelineNodes.size() > 0
							&& editor.capturedTimelineMultiSlider == null);
					enableToggle.setText(seq.active ? "Disable" : "Enable");

					// show popup menu
					namePopup.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});

		updateLabel();

		GridBagConstraints gbc = new GridBagConstraints();

		/* CREATE TIMELINE */
		multiSlider = new GuiLttlMultiSlider(0, editor.sliderMax,
				new LttlCallback()
				{
					IntArray cachedShiftTimes;
					MouseAdapter mouseAdapter;
					int mouseXPos;

					@Override
					public void callback(int id, Object... values)
					{
						if (id == 0)
						{
							// right clicked on no node, show create TimelineNode menu
							final MouseEvent mouseEvent = (MouseEvent) values[0];

							JPopupMenu timelinePopupMenu = new JPopupMenu();
							JMenuItem addKeyframe = new JMenuItem(
									"Add Keyframe");
							addKeyframe.addActionListener(new ActionListener()
							{
								@Override
								public void actionPerformed(ActionEvent e)
								{
									HashMap<AnimGuiSequence, ArrayList<TimelineNode>> undoValue = editor
											.getCurrentAnimSeqNodeMap(AnimGuiSequence.this);

									KeyframeNode keyframeNode;
									if (isFieldSequence())
									{
										keyframeNode = new FieldKeyframeNode();
									}
									else
									{
										keyframeNode = new StateKeyframeNode();
									}
									keyframeNode.easeType = editor.defaultEase;
									addNode(keyframeNode, mouseEvent);

									// update value with current value if field keyframe
									if (isFieldSequence())
									{
										((FieldKeyframeNode) keyframeNode)
												.updateValue();
									}

									editor.registerUndoState(undoValue,
											"Add Keyframe");

									editor.refreshMasterSliderAndAllGroups(false);
									editor.updateAnim();
								}
							});
							timelinePopupMenu.add(addKeyframe);
							JMenuItem addHold = new JMenuItem("Add Hold");
							addHold.addActionListener(new ActionListener()
							{
								@Override
								public void actionPerformed(ActionEvent e)
								{
									HashMap<AnimGuiSequence, ArrayList<TimelineNode>> undoValue = editor
											.getCurrentAnimSeqNodeMap(AnimGuiSequence.this);

									addNode(new AnimationHoldNode(), mouseEvent);

									editor.registerUndoState(undoValue,
											"Add Hold");

									editor.refreshMasterSliderAndAllGroups(false);
									editor.updateAnim();
								}
							});
							timelinePopupMenu.add(addHold);
							JMenuItem addCallback = new JMenuItem(
									"Add Callback");
							addCallback.addActionListener(new ActionListener()
							{
								@Override
								public void actionPerformed(ActionEvent e)
								{
									HashMap<AnimGuiSequence, ArrayList<TimelineNode>> undoValue = editor
											.getCurrentAnimSeqNodeMap(AnimGuiSequence.this);

									addNode(new AnimationCallbackNode(),
											mouseEvent);

									editor.registerUndoState(undoValue,
											"Add Callback");

									editor.refreshMasterSliderAndAllGroups(false);
									editor.updateAnim();
								}
							});
							timelinePopupMenu.add(addCallback);

							JMenuItem pasteMenuItem = new JMenuItem("Paste");
							pasteMenuItem
									.addActionListener(new ActionListener()
									{
										@Override
										public void actionPerformed(
												ActionEvent e)
										{
											HashMap<AnimGuiSequence, ArrayList<TimelineNode>> undoValue = editor
													.getCurrentAnimSeqNodeMap(AnimGuiSequence.this);

											TimelineNode timelineNodeCopy = LttlCopier
													.copy(editor.capturedTimelineNodes
															.get(0));
											addNode(timelineNodeCopy,
													mouseEvent);

											editor.registerUndoState(undoValue,
													"Paste Node");

											editor.refreshMasterSliderAndAllGroups(false);
											editor.updateAnim();
										}
									});
							TimelineNode capturedTimelineNode = editor.capturedTimelineNodes == null
									|| editor.capturedTimelineNodes.size() == 0
									|| editor.capturedTimelineMultiSlider != null ? null
									: editor.capturedTimelineNodes.get(0);
							if (capturedTimelineNode == null)
							{
								pasteMenuItem.setEnabled(false);
							}
							else
							{
								if (isFieldSequence())
								{
									pasteMenuItem.setEnabled(capturedTimelineNode
											.getClass() != StateKeyframeNode.class);
								}
								else
								{
									pasteMenuItem.setEnabled(capturedTimelineNode
											.getClass() != FieldKeyframeNode.class);
								}
							}
							timelinePopupMenu.add(pasteMenuItem);

							timelinePopupMenu.show(mouseEvent.getComponent(),
									mouseEvent.getX(), mouseEvent.getY());
						}
						else if (id == 1 || id == 2)
						{
							// right clicked on node
							final MouseEvent mouseEvent = (MouseEvent) values[0];
							final JSlider clickedSlider = (JSlider) values[1];

							final TimelineNode clickedNode = findNodeBySlider(clickedSlider);

							if (clickedNode == null) { return; }

							String menuName = null;
							if (clickedNode.getClass() == StateKeyframeNode.class)
							{
								menuName = "KeyFrame("
										+ ((StateKeyframeNode) clickedNode).stateName
										+ ")";
							}
							else if (clickedNode.getClass() == FieldKeyframeNode.class)
							{
								menuName = "KeyFrame("
										+ ((FieldKeyframeNode) clickedNode).value
										+ ")";
							}
							else if (clickedNode.getClass() == AnimationCallbackNode.class)
							{
								menuName = "Callback("
										+ ((AnimationCallbackNode) clickedNode).callbackValue
										+ ")";
							}
							else
							{
								menuName = "Hold";
							}
							menuName += " - "
									+ new DecimalFormat("#.###")
											.format(clickedNode.time);

							GuiLttlLabeledPopupMenu popupMenu = new GuiLttlLabeledPopupMenu(
									menuName);

							final HashMap<AnimGuiSequence, ArrayList<TimelineNode>> undoValue = editor
									.getCurrentAnimSeqNodeMap(AnimGuiSequence.this);

							JMenuItem edit = new JMenuItem("Edit");
							edit.addActionListener(new ActionListener()
							{
								@Override
								public void actionPerformed(ActionEvent e)
								{
									JDialog dialog = editor
											.createEditNodeDialog(clickedNode,
													new LttlCallback()
													{
														@Override
														public void callback(
																int id,
																Object... value)
														{
															editor.registerUndoState(
																	undoValue,
																	"Edit Node");
															editor.formatSlider(
																	clickedSlider,
																	clickedNode);
															editor.refreshMasterSliderAndAllGroups(false);
															editor.updateAnim();
														}
													}, false);

									dialog.setVisible(true);
								}
							});
							popupMenu.add(edit);

							JMenuItem copy = new JMenuItem("Copy");
							copy.addActionListener(new ActionListener()
							{
								@Override
								public void actionPerformed(ActionEvent e)
								{
									editor.capturedTimelineNodes = new ArrayList<TimelineNode>();
									editor.capturedTimelineNodes
											.add(clickedNode);
									editor.capturedTimelineMultiSlider = null;
								}
							});
							popupMenu.add(copy);

							if (clickedNode.getClass() == FieldKeyframeNode.class)
							{
								JMenuItem update = new JMenuItem("Update");
								update.addActionListener(new ActionListener()
								{
									@Override
									public void actionPerformed(ActionEvent e)
									{
										final HashMap<AnimGuiSequence, ArrayList<TimelineNode>> undoValue = editor
												.getCurrentAnimSeqNodeMap(AnimGuiSequence.this);
										((FieldKeyframeNode) clickedNode)
												.updateValue();
										editor.registerUndoState(undoValue,
												"Update Node");
										editor.updateAnim();
									}
								});
								popupMenu.add(update);
							}

							if (clickedNode.getClass() == StateKeyframeNode.class)
							{
								final StateKeyframeNode skn = (StateKeyframeNode) clickedNode;
								JMenuItem activateToggle = new JMenuItem(
										skn.active ? "Deactivate" : "Activate");
								activateToggle
										.addActionListener(new ActionListener()
										{
											@Override
											public void actionPerformed(
													ActionEvent e)
											{
												final HashMap<AnimGuiSequence, ArrayList<TimelineNode>> undoValue = editor
														.getCurrentAnimSeqNodeMap(AnimGuiSequence.this);
												skn.active = !skn.active;
												editor.formatSlider(
														clickedSlider,
														clickedNode);
												editor.registerUndoState(
														undoValue,
														(skn.active ? "Deactivate"
																: "Activate")
																+ " Node");
												editor.updateAnim();
											}
										});
								popupMenu.add(activateToggle);
							}

							JMenu delete = new JMenu("Delete");
							JMenuItem deleteConfirm = new JMenuItem("Confirm");
							delete.add(deleteConfirm);
							deleteConfirm
									.addActionListener(new ActionListener()
									{
										@Override
										public void actionPerformed(
												ActionEvent e)
										{
											HashMap<AnimGuiSequence, ArrayList<TimelineNode>> undoValue = editor
													.getCurrentAnimSeqNodeMap(AnimGuiSequence.this);

											final TimelineNode node = findNodeBySlider(clickedSlider);
											removeNode(node);

											editor.registerUndoState(undoValue,
													"Delete Node");

											editor.refreshAllGroupSliders(false);
											editor.updateAnim();
										}
									});
							delete.setEnabled(AnimGuiSequence.this.seq.nodes
									.size() > 1);

							popupMenu.add(delete);

							if (id == 1)
							{
								popupMenu.show(mouseEvent.getComponent(),
										mouseEvent.getX(), mouseEvent.getY());
							}
							else
							{
								edit.doClick();
							}
						}
						else if (id == 3)
						{
							if (editor.focusedGroup == null)
							{
								// slider node dragging
								// set the time field on timeline nodes
								JSlider movedSlider = (JSlider) values[0];
								int tick = movedSlider.getValue();

								if (parentGui != null)
								{
									if (Lttl.editor.getInput().isShiftSwing()
											&& cachedShiftTimes != null
											&& cachedShiftTimes.size > 0)
									{
										int realTick = multiSlider
												.getValueForXPosition(mouseXPos);
										int closestTick = cachedShiftTimes
												.get(0);
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
										movedSlider.setValue(closestTick);
									}
								}

								final TimelineNode node = findNodeBySlider(movedSlider);
								if (node == null) return;

								float sliderTime = editor.tickToTime(tick);
								node.time = sliderTime;

								// don't update these if moving group slider, since that will call the updates
								if (editor.focusedGroup == null)
								{
									editor.updateCurrentTimeLabel(node.time);

									// now that node's time's have been updated
									editor.clampAndSetSeekTime();
									editor.setMinEditorDuration();
									editor.refreshSeekSlider();
									editor.updateAnim();

									// if field sequence, then refresh group slider
									if (!Lttl.editor.getInput().isShiftSwing())
									{
										editor.refreshMasterSliderAndAllGroups(false);
									}
								}
							}
						}
						else if (id == 4)
						{
							// begin node drag (save undoValue)
							JSlider dragSlider = (JSlider) values[0];
							int tick = dragSlider.getValue();
							final TimelineNode node = findNodeBySlider(dragSlider);
							if (node != null)
							{
								node.undoValue = node.time;
							}

							// only for sequences that are inside an ObjectAnimGUI (field)
							if (parentGui != null)
							{
								mouseAdapter = new MouseAdapter()
								{
									public void mouseDragged(MouseEvent e)
									{
										mouseXPos = e.getX();
									};
								};
								dragSlider.addMouseMotionListener(mouseAdapter);

								if (cachedShiftTimes == null)
								{
									cachedShiftTimes = new IntArray(false, 4);
								}

								// get the cachedShiftTimes right when Shift goes down for all sequences in
								// editor get all the node times for all sequences
								cachedShiftTimes.clear();
								editor.getAllNodeTimes(cachedShiftTimes);

								// remove all other times that are on this slider, since we don't want to
								// snap on top of keyframes on same slider, except keep itself, so it has an
								// origin to snap to
								for (Component slider : multiSlider
										.getComponents())
								{
									int siblingTick = ((JSlider) slider)
											.getValue();
									if (siblingTick != tick)
									{
										cachedShiftTimes
												.removeValue(siblingTick);
									}
								}
							}
						}
						else if (id == 5)
						{
							// end node drag (create undo)
							JSlider dragSlider = (JSlider) values[0];

							final TimelineNode node = findNodeBySlider(dragSlider);

							node.time = editor.tickToTime(dragSlider.getValue());
							dragSlider.setValue(editor.timeToTick(node.time));

							// if field sequence, then refresh group slider
							if (parentGui != null)
							{
								if (cachedShiftTimes != null)
								{
									cachedShiftTimes.clear();
								}
								dragSlider
										.removeMouseMotionListener(mouseAdapter);
								parentGui.refreshGroupSliderSelfAndAncestors();
							}

							if (node != null
									&& !LttlMath.isEqual(node.undoValue,
											node.time))
							{
								UndoState undoState = new UndoState(
										editor.manager.t().getName()
												+ " ("
												+ editor.manager.getClass()
														.getSimpleName()
												+ ") - Animations:"
												+ editor.anim.name + ":"
												+ seq.name
												+ ":Moved animation node",
										new UndoField(editor.manager.t(),
												node.undoValue, node.time,
												new UndoSetter()
												{
													@Override
													public void set(
															LttlComponent comp,
															Object value)
													{
														node.time = (float) value;
													}
												}));
								// save the undoState so we can remove all of them when animation window closes
								editor.undoStates.add(undoState);
								Lttl.editor.getUndoManager().registerUndoState(
										undoState);
							}
							editor.refreshMasterSliderAndAllGroups(false);
						}
					}
				});
		multiSlider.addMouseMotionListener(new MouseMotionAdapter()
		{
			@Override
			public void mouseMoved(MouseEvent e)
			{
				editor.updateCurrentTimeLabel(e);
			}
		});
		JPanel multiSliderPanel = new JPanel(new BorderLayout());
		multiSliderPanel.setPreferredSize(new Dimension(1, 20));
		multiSliderPanel.add(multiSlider);
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = 0;
		gbc.gridwidth = 1;
		group.getCollapsePanel().add(multiSliderPanel, gbc);

		/* NODES */
		// need to have at least one node, so create one
		if (seq.nodes.size() == 0)
		{
			if (isFieldSequence())
			{
				FieldKeyframeNode node = new FieldKeyframeNode();
				addNode(node, true);
				node.updateValue();
			}
			else
			{
				addNode(new StateKeyframeNode(), true);
			}
		}
		// add all exising nodes
		for (int i = 0; i < seq.nodes.size(); i++)
		{
			TimelineNode node = seq.nodes.get(i);

			JSlider s = null;
			if (i == 0)
			{
				// set first keyframe to main slider
				s = multiSlider.getSlider(0);
				s.setValue(editor.timeToTick(node.time));
			}
			else
			{
				s = multiSlider.addSlider(editor.timeToTick(node.time));
			}

			node.slider = s;
			node.sequenceGui = this;
			// post stuff
			editor.formatSlider(s, node);
		}
		editor.setMinEditorDuration();
	}

	public void updateLabel()
	{
		String label = level.isEmpty() ? "" : level + " ";
		if (isFieldSequence())
		{
			label += (isListItem() ? getAnimId() : pft.getField().getName())
					+ (seq.name.isEmpty() ? "" : "(" + seq.name + ")");
		}
		else
		{
			label += seq.name.isEmpty() ? "Unnamed" : seq.name;
		}
		group.setLabel(label);
		GuiHelper.SetFontColor(group.getToggleButton(), new Color(0, 0, 0,
				seq.active ? .5f : .24f));
	}

	/**
	 * Sets the node's time based on mouse event and then adds it<br>
	 * Note: It only works if mouse right clicked on the slider it's adding the node to
	 * 
	 * @param node
	 * @param e
	 */
	private void addNode(TimelineNode node, MouseEvent e)
	{
		int tick = multiSlider.getValueForXPosition(e.getX());
		node.time = editor.tickToTime(tick);

		addNode(node, true);
	}

	/**
	 * Adds a node using the nodes time, if first node, then doesn't create a new slider
	 * 
	 * @param node
	 * @param updateAnim
	 *            set this to false if you are adding a lot and want to update anim at end
	 */
	void addNode(TimelineNode node, boolean updateAnim)
	{
		// add to sequence
		seq.nodes.add(node);

		JSlider s;
		if (seq.nodes.size() == 1)
		{
			s = multiSlider.getSlider(0);
		}
		else
		{
			// create slider
			s = multiSlider.addSlider(editor.timeToTick(node.time));
		}

		// save the slider to the node
		node.slider = s;
		node.sequenceGui = this;

		// post stuff
		editor.formatSlider(s, node);
		editor.setMinEditorDuration();
		if (updateAnim)
		{
			editor.updateAnim();
		}
	}

	void removeNode(TimelineNode timelineNode)
	{
		removeNode(timelineNode, true, true);
	}

	/**
	 * @param timelineNode
	 * @param updateAnim
	 *            set this to false if you are removing a lot and want to update anim at end
	 */
	void removeNode(TimelineNode timelineNode, boolean refreshGroupSliders,
			boolean updateAnim)
	{
		if (timelineNode != null)
		{
			AnimGuiSequence.this.seq.nodes.remove(timelineNode);

			// now that node's time's have been updated
			editor.clampAndSetSeekTime();
			editor.setMinEditorDuration();
			editor.refreshSeekSlider();
			if (updateAnim)
			{
				editor.updateAnim();
			}

		}

		// multi slider handles always having one opaque
		AnimGuiSequence.this.multiSlider.removeSlider(timelineNode.slider);

		if (refreshGroupSliders)
		{
			editor.refreshMasterSliderAndAllGroups(false);
		}
	}

	/**
	 * returns null if none found
	 * 
	 * @param slider
	 * @return
	 */
	TimelineNode findNodeBySlider(JSlider slider)
	{
		for (TimelineNode node : seq.nodes)
		{
			if (node.slider == slider) { return node; }
		}
		return null;
	}

	boolean isFieldSequence()
	{
		return parentGui != null;
	}

	@Override
	public int getAnimId()
	{
		// needs to have a parent
		Lttl.Throw(parentGui);

		return LttlHelper.GetHashMapFirstKey(parentGui.animObject.sequences,
				seq, true);
	}

	@Override
	protected void listItemModifyIndex(int newIndex)
	{
		LttlHelper.RemoveHashMapValue(parentGui.animObject.sequences,
				AnimGuiSequence.this.seq, true, false);
		parentGui.animObject.sequences.put(newIndex, AnimGuiSequence.this.seq);
	}

	void makeVisible()
	{
		group.setCollapseState(false);
		if (parentGui != null)
		{
			parentGui.makeVisible();
		}
	}

	void getNodesTimeMap(HashMap<Float, ArrayList<TimelineNode>> sharedMap)
	{
		for (TimelineNode node : seq.nodes)
		{
			// create the arraylist for this time if any
			if (!sharedMap.containsKey(node.time))
			{
				sharedMap.put(node.time, new ArrayList<TimelineNode>());
			}
			sharedMap.get(node.time).add(node);
		}
	}
}
