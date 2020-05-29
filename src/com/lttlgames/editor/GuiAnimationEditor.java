package com.lttlgames.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;

import com.badlogic.gdx.utils.IntArray;
import com.lttlgames.components.LttlAnimationManager;
import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.GuiCallbackDescendants;
import com.lttlgames.editor.annotations.GuiDecimalPlaces;
import com.lttlgames.editor.annotations.GuiHideLabel;
import com.lttlgames.editor.annotations.GuiListItemNameField;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.GuiShow;
import com.lttlgames.editor.annotations.GuiTwoColumn;
import com.lttlgames.helpers.EaseType;
import com.lttlgames.helpers.LttlCallback;
import com.lttlgames.helpers.LttlHelper;
import com.lttlgames.helpers.LttlMath;
import com.lttlgames.helpers.LttlMutatableFloat;
import com.lttlgames.tweenengine.RepeatTweenType;
import com.lttlgames.tweenengine.Timeline;
import com.lttlgames.tweenengine.TweenCallback;

public class GuiAnimationEditor
{
	static float maxZoomFactor = 4;

	private static final GridBagConstraints gbcSequence = new GridBagConstraints(
			0, GridBagConstraints.RELATIVE, 1, 1, 1, 0,
			GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(
					0, 0, 0, 0), 0, 0);
	Object focusedGroup = null;
	private boolean isPlaying = false;
	AnimationObject anim;
	private JScrollPane vertScrollPane;
	private JScrollPane horizScrollPane;
	private GuiLttlSlider zoomSlider;
	private JPanel horizPanel;
	JPanel mainPanel;
	private JPanel vertPanel;
	private JPanel stateSeqPanel;
	JPanel compPanel;
	private JPanel seekPanel;
	private JPanel bottomPanel;
	private JSlider seekSlider;
	private JLabel currentCursorTimeLabel;
	ArrayList<AnimGuiSequence> animStatesGUI = new ArrayList<AnimGuiSequence>();
	ArrayList<AnimGuiObject> animCompsGUI = new ArrayList<AnimGuiObject>();
	int sliderMax = 100000;
	LttlAnimationManager manager;
	private GuiComponentNumber<Float> editorDurationGFO;
	ArrayList<UndoState> undoStates = new ArrayList<UndoState>();
	@GuiDecimalPlaces(2)
	private float seekTime = 0;
	private GuiComponentNumber<Float> seekTimeGFO;
	private boolean seekTimeUpdating = false;
	private boolean seekSliderUpdating = false;
	private JButton playStopButton;
	@GuiMin(0)
	private float speedMultiplier = 1;
	@GuiHideLabel
	EaseType defaultEase = EaseType.QuadInOut;
	private Timeline cachedAnim;
	private Timeline cachedPlayAnim;
	private boolean skipCachedAnimUpdate = false;
	private JToggleButton liveUpdateToggle;
	ArrayList<TimelineNode> capturedTimelineNodes;
	GuiLttlMultiSlider capturedTimelineMultiSlider;
	private GuiLttlInputListener spaceKeyListener;
	private GuiLttlMultiSlider masterSlider;
	private JDialog propertyDialog;
	private JDialog mainDialog;

	@SuppressWarnings("rawtypes")
	public GuiAnimationEditor(final AnimationObject anim,
			GuiFieldObject animationObjectGFO)
	{
		Lttl.editor.getGui().animEditors.add(this);
		this.anim = anim;
		manager = (LttlAnimationManager) animationObjectGFO.getParent()
				.getParent().objectRef;
		updateCachedAnim();

		mainPanel = new JPanel(new GridBagLayout());
		mainPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		vertPanel = new JPanel(new GridBagLayout());
		vertScrollPane = new JScrollPane();
		vertScrollPane.setViewportView(vertPanel);
		vertScrollPane.setBorder(null);
		vertScrollPane.getViewport().addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent e)
			{
				if (vertScrollPane.getVerticalScrollBar().isVisible())
				{
					seekPanel.setBorder(BorderFactory
							.createEmptyBorder(0, 0, 0, vertScrollPane
									.getVerticalScrollBar().getWidth()));
				}
				else
				{
					seekPanel.setBorder(null);
				}
			}
		});

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = 1;
		gbc.weightx = 0;
		gbc.fill = GridBagConstraints.NONE;

		// get fields
		Field editorDurationField = null;
		Field editorRepeatTypeField = null;
		Field speedMultiplierField = null;
		Field defaultEaseField = null;
		try
		{
			editorDurationField = anim.getClass().getDeclaredField(
					"editorDuration");
			editorRepeatTypeField = anim.getClass().getDeclaredField(
					"editorRepeatType");
			speedMultiplierField = GuiAnimationEditor.class
					.getDeclaredField("speedMultiplier");
			defaultEaseField = GuiAnimationEditor.class
					.getDeclaredField("defaultEase");
		}
		catch (NoSuchFieldException e1)
		{
			e1.printStackTrace();
		}
		catch (SecurityException e1)
		{
			e1.printStackTrace();
		}

		/* TOP PANEL */
		// LEFT
		JPanel topPanelLeft = new JPanel(new FlowLayout(FlowLayout.LEFT));
		// Jump To Start Button
		final JButton jumpToStartButton = new JButton("<");
		jumpToStartButton.setToolTipText("Ctrl + Space");
		jumpToStartButton.setFocusable(false);
		jumpToStartButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (isPlaying)
				{
					playStopButton.doClick();
				}
				else
				{
					seekSlider.setValue(0);
					updateAnim();
				}
			}
		});
		topPanelLeft.add(jumpToStartButton);

		// Play/Pause Button
		final JButton playStopButton = new JButton("Play");
		playStopButton.setToolTipText("Space");
		this.playStopButton = playStopButton;
		playStopButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (isPlaying)
				{
					playStopButton.setText("Play");
					isPlaying = false;
					cachedPlayAnim = null;
					return;
				}

				isPlaying = true;
				playStopButton.setText("Stop");
				updateAnim();
			}
		});
		topPanelLeft.add(playStopButton);

		Lttl.editor.getInput().addListener(
				spaceKeyListener = new GuiLttlInputListener()
				{
					@Override
					public void onKeyReleased(KeyEvent e)
					{
					}

					@Override
					public void onKeyPressed(KeyEvent e)
					{
						if (e.getKeyCode() == KeyEvent.VK_SPACE)
						{
							if (e.isControlDown())
							{
								jumpToStartButton.doClick();
							}
							else if (!playStopButton.isFocusOwner())
							{
								playStopButton.doClick();
							}
						}
					}
				});

		// add repeat select
		GuiComponentEnum repeatTypeGFO = new GuiComponentEnum(
				new ProcessedFieldType(editorRepeatTypeField), anim, -1, null);
		repeatTypeGFO.addChangeListener(new GuiLttlChangeListener()
		{
			@Override
			void onChange(int changeId)
			{
				if (isPlaying)
				{
					playStopButton.doClick();
					playStopButton.doClick();
				}
			}
		});
		repeatTypeGFO.setAutoRegisterUndo(false);
		repeatTypeGFO.getPanel().remove(repeatTypeGFO.getLabel());
		repeatTypeGFO.getPanel().setPreferredSize(
				new Dimension(85, GuiHelper.defaultFieldDimension.height));
		topPanelLeft.add(repeatTypeGFO.getPanel());

		// add speed multiplier and scale button
		final GuiComponentNumber<Number> speedMultiplierGFO = new GuiComponentNumber<Number>(
				new ProcessedFieldType(speedMultiplierField), this, -1, null);
		speedMultiplierGFO.addChangeListener(new GuiLttlChangeListener()
		{
			@Override
			void onChange(int changeId)
			{
				if (cachedPlayAnim != null)
				{
					cachedPlayAnim.setSpeedMultiplier(speedMultiplier);
				}
			}
		});
		speedMultiplierGFO.setAutoRegisterUndo(false);
		speedMultiplierGFO.getPanel().setPreferredSize(
				new Dimension(165, GuiHelper.defaultFieldDimension.height));
		topPanelLeft.add(speedMultiplierGFO.getPanel());

		JButton scaleTimeButton = new JButton("Scale Times");
		scaleTimeButton.setFocusable(false);
		scaleTimeButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				anim.editorDuration /= speedMultiplier;
				editorDurationGFO.updatePrimativeValue();
				for (AnimGuiSequence aGUI : getAllSequenceAnimGUIs())
				{
					for (TimelineNode node : aGUI.seq.nodes)
					{
						node.time /= speedMultiplier;
					}
				}
				speedMultiplierGFO.setValue(1);
				speedMultiplierGFO.updatePrimativeValue();
				updateEditorDuration();
				if (isPlaying)
				{
					playStopButton.doClick();
					playStopButton.doClick();
				}
			}
		});
		topPanelLeft.add(scaleTimeButton);

		// Add State Sequence Button
		JButton addStateSequenceButton = new JButton("Add State");
		addStateSequenceButton.setFocusable(false);
		addStateSequenceButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				AnimationSequence seq = new AnimationSequence();
				anim.stateSequences.add(seq);
				AnimGuiSequence ag = new AnimGuiSequence(
						GuiAnimationEditor.this, seq);
				animStatesGUI.add(ag);
				stateSeqPanel.add(ag.group.getPanel(), gbcSequence);
				stateSeqPanel.revalidate();
				stateSeqPanel.repaint();
			}
		});
		topPanelLeft.add(addStateSequenceButton);

		// Add Component Sequence Button
		JButton addCompSequenceButton = new JButton("Add Component");
		addCompSequenceButton.setFocusable(false);
		addCompSequenceButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				AnimatedComponent animComp = new AnimatedComponent();
				anim.animComps.add(animComp);
				AnimGuiObject og = new AnimGuiObject(GuiAnimationEditor.this,
						null, animComp, null);
				animCompsGUI.add(og);
				compPanel.add(og.group.getPanel(), gbcSequence);
				compPanel.revalidate();
				compPanel.repaint();
			}
		});
		topPanelLeft.add(addCompSequenceButton);

		// add callbacks in editor toggle
		final JToggleButton editorCallbacks = new JToggleButton("Callbacks");
		editorCallbacks.setFocusable(false);
		editorCallbacks.setToolTipText("Toggles callback nodes for editor.");
		editorCallbacks.setSelected(anim.editorCallbacks);
		editorCallbacks.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				anim.editorCallbacks = editorCallbacks.isSelected();
				if (isPlaying)
				{
					playStopButton.doClick();
					playStopButton.doClick();
				}
			}
		});
		topPanelLeft.add(editorCallbacks);

		// live update button
		liveUpdateToggle = new JToggleButton("Live");
		liveUpdateToggle.setFocusable(false);
		liveUpdateToggle.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				updateAnim();
			}
		});
		liveUpdateToggle
				.setToolTipText("Changes to states will reflect here live.  Can also just tap this to refresh cached animation.  Callbacks disabled.  Does break shake update rate.");
		topPanelLeft.add(liveUpdateToggle);

		// Add Properties Button
		JButton viewPropertiesButton = new JButton("Properties");
		viewPropertiesButton.setFocusable(false);
		viewPropertiesButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (propertyDialog == null)
				{
					createPropertyDialog().setVisible(true);
				}
			}
		});
		topPanelLeft.add(viewPropertiesButton);

		// add default ease
		final GuiComponentEnum defaultEaseGFO = new GuiComponentEnum(
				new ProcessedFieldType(defaultEaseField), this, -1, null);
		defaultEaseGFO.setAutoRegisterUndo(false);
		defaultEaseGFO.getPanel().setPreferredSize(
				new Dimension(90, GuiHelper.defaultFieldDimension.height));
		defaultEaseGFO.combo.textBox.setToolTipText("Default Ease");
		topPanelLeft.add(defaultEaseGFO.getPanel());

		// RIGHT
		JPanel topPanelRight = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		// Editor Duration Field
		final GuiComponentNumber<Float> editorDurationGFO = new GuiComponentNumber<Float>(
				new ProcessedFieldType(editorDurationField), this.anim, -1,
				null);
		this.editorDurationGFO = editorDurationGFO;
		setMinEditorDuration();
		editorDurationGFO.addChangeListener(new GuiLttlChangeListener()
		{
			@Override
			void onChange(int changeId)
			{
				updateEditorDuration();
			}
		});
		editorDurationGFO.getPanel().setPreferredSize(
				new Dimension(165, GuiHelper.defaultFieldDimension.height));
		topPanelRight.add(editorDurationGFO.getPanel());

		// Trim Button
		JButton trimButton = new JButton("Trim");
		trimButton.setFocusable(false);
		trimButton.setToolTipText("Trims the excess time past the last node.");
		trimButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				editorDurationGFO.setValue(anim.getLatestNodeTime());
				editorDurationGFO.updatePrimativeValue();
				updateEditorDuration();
			}
		});
		topPanelRight.add(trimButton, gbc);

		currentCursorTimeLabel = new JLabel();

		gbc.gridy = 0;
		gbc.gridx = 0;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.WEST;
		mainPanel.add(topPanelLeft, gbc);
		gbc.gridx = 1;
		gbc.anchor = GridBagConstraints.EAST;
		mainPanel.add(currentCursorTimeLabel, gbc);
		gbc.gridx = 2;
		gbc.anchor = GridBagConstraints.EAST;
		mainPanel.add(topPanelRight, gbc);

		/* MASTER SLIDER */
		masterSlider = new GuiLttlMultiSlider(0, sliderMax, new LttlCallback()
		{
			ArrayList<TimelineNode> focusedNodes;
			IntArray cachedShiftTimes;
			MouseAdapter mouseAdapter;
			int mouseXPos;

			float undoGroupTime = 0;

			private ArrayList<AnimGuiSequence> allFieldSeqsGui;
			private ArrayList<AnimGuiSequence> allSeqsGui;

			private void updateSeqsLists()
			{
				allFieldSeqsGui = getAllFieldSequenceAnimGUIs();
				allSeqsGui = new ArrayList<AnimGuiSequence>();
				allSeqsGui.addAll(animStatesGUI);
				allSeqsGui.addAll(allFieldSeqsGui);
			}

			@Override
			public void callback(int id, Object... values)
			{
				if (id == 0)
				{
					// right clicked on no node, show create master nodes
					updateSeqsLists();

					final MouseEvent mouseEvent = (MouseEvent) values[0];
					final float time = tickToTime(masterSlider
							.getValueForXPosition(mouseEvent.getX()));
					JPopupMenu popupMenu = new JPopupMenu();

					// adds keyframe to all field sequences, not states
					JMenuItem addFieldKeyframe = new JMenuItem("Add Keyframe");
					addFieldKeyframe.setEnabled(allFieldSeqsGui.size() > 0);
					addFieldKeyframe.addActionListener(new ActionListener()
					{
						@Override
						public void actionPerformed(ActionEvent e)
						{
							HashMap<AnimGuiSequence, ArrayList<TimelineNode>> undoValue = getAllCurrentAnimSeqNodeMap();

							for (AnimGuiSequence sg : allFieldSeqsGui)
							{
								FieldKeyframeNode fk = new FieldKeyframeNode();
								fk.easeType = defaultEase;
								fk.time = time;
								sg.addNode(fk, true);
								fk.updateValue();
							}

							registerUndoState(undoValue, "Add Keyframes");

							refreshMasterSliderAndAllGroups(false);
							updateAnim();
						}
					});
					popupMenu.add(addFieldKeyframe);

					JMenuItem addHold = new JMenuItem("Add Hold");
					addHold.setEnabled(allFieldSeqsGui.size() > 0);
					addHold.addActionListener(new ActionListener()
					{
						@Override
						public void actionPerformed(ActionEvent e)
						{
							for (AnimGuiSequence sg : allFieldSeqsGui)
							{
								AnimationHoldNode ah = new AnimationHoldNode();
								ah.time = time;
								sg.addNode(ah, true);
							}
							refreshMasterSliderAndAllGroups(false);
						}
					});
					popupMenu.add(addHold);

					JMenuItem pasteMenuItem = new JMenuItem("Paste");
					pasteMenuItem.addActionListener(new ActionListener()
					{
						@Override
						public void actionPerformed(ActionEvent e)
						{
							HashMap<AnimGuiSequence, ArrayList<TimelineNode>> undoValue = getAllCurrentAnimSeqNodeMap();

							for (TimelineNode node : capturedTimelineNodes)
							{
								TimelineNode timelineNodeCopy = LttlCopier
										.copy(node);
								timelineNodeCopy.time = time;
								node.sequenceGui.addNode(timelineNodeCopy,
										false);
							}

							registerUndoState(undoValue, "Paste Nodes");

							refreshMasterSliderAndAllGroups(false);
							updateAnim();
						}
					});
					pasteMenuItem.setEnabled(capturedTimelineNodes != null
							&& capturedTimelineNodes.size() > 0
							&& capturedTimelineMultiSlider == GuiAnimationEditor.this.masterSlider);
					popupMenu.add(pasteMenuItem);

					popupMenu.show(mouseEvent.getComponent(),
							mouseEvent.getX(), mouseEvent.getY());
				}
				else if (id == 1)
				{
					// right clicked on node
					updateSeqsLists();

					final MouseEvent mouseEvent = (MouseEvent) values[0];
					final JSlider clickedSlider = (JSlider) values[1];
					final float time = tickToTime(clickedSlider.getValue());

					final ArrayList<TimelineNode> nodes = getAllNodesAtTime(
							time, allSeqsGui);

					GuiLttlLabeledPopupMenu popupMenu = new GuiLttlLabeledPopupMenu(
							"Group Node ["
									+ nodes.size()
									+ "]"
									+ " - "
									+ new DecimalFormat("#.###")
											.format(tickToTime(clickedSlider
													.getValue())));

					JMenuItem batchEditMenuItem = new JMenuItem("Batch Edit");
					batchEditMenuItem.addActionListener(new ActionListener()
					{
						@Override
						public void actionPerformed(ActionEvent e)
						{
							createDummyNodeDialog(time, nodes);
						}
					});
					popupMenu.add(batchEditMenuItem);

					JMenuItem copyMenuItem = new JMenuItem("Copy");
					copyMenuItem.addActionListener(new ActionListener()
					{
						@Override
						public void actionPerformed(ActionEvent e)
						{
							capturedTimelineNodes = getAllNodesAtTime(
									tickToTime(clickedSlider.getValue()),
									allSeqsGui);
							capturedTimelineMultiSlider = masterSlider;
						}
					});
					popupMenu.add(copyMenuItem);

					JMenuItem updateMenuItem = new JMenuItem("Update");
					updateMenuItem.addActionListener(new ActionListener()
					{
						@Override
						public void actionPerformed(ActionEvent e)
						{
							for (TimelineNode node : nodes)
							{
								if (node.getClass() == FieldKeyframeNode.class)
								{
									((FieldKeyframeNode) node).updateValue();
								}
							}
							updateAnim();
						}
					});
					popupMenu.add(updateMenuItem);

					JMenuItem isolateMenuItem = new JMenuItem("Isolate");
					isolateMenuItem.addActionListener(new ActionListener()
					{
						@Override
						public void actionPerformed(ActionEvent e)
						{
							// collapse everything in editor
							collapseAll();

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

					JMenu deleteMenu = new JMenu("Delete All (" + nodes.size()
							+ ")");
					JMenuItem confirmDeleteMenuItem = new JMenuItem("Confirm");
					confirmDeleteMenuItem
							.addActionListener(new ActionListener()
							{
								@Override
								public void actionPerformed(ActionEvent e)
								{
									HashMap<AnimGuiSequence, ArrayList<TimelineNode>> undoValue = getAllCurrentAnimSeqNodeMap();

									// deleting a timelinenode
									int unableToDeleteCount = 0;
									for (TimelineNode node : nodes)
									{
										// can't delete the last node of a sequence
										if (node.sequenceGui.seq.nodes.size() > 1)
										{
											node.sequenceGui.removeNode(node,
													false, false);
										}
										else
										{
											unableToDeleteCount++;
										}
									}

									registerUndoState(undoValue, "Delete Nodes");

									refreshMasterSliderAndAllGroups(false);
									updateAnim();

									if (unableToDeleteCount > 0)
									{
										GuiHelper
												.showAlert(
														mainPanel,
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
					updateSeqsLists();

					// begin node drag
					JSlider clickedSlider = (JSlider) values[0];

					// get start time of this slider
					float time = tickToTime(clickedSlider.getValue());

					// save undo time
					undoGroupTime = time;

					// grab all child nodes that are at this time
					focusedNodes = getAllNodesAtTime(
							tickToTime(clickedSlider.getValue()), allSeqsGui);

					if (cachedShiftTimes == null)
					{
						cachedShiftTimes = new IntArray(false, 4);
					}
					// get all the node times for all sequences
					cachedShiftTimes.clear();
					getAllNodeTimes(cachedShiftTimes);

					focusedGroup = GuiAnimationEditor.this;

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
						float time = tickToTime(tick);

						if (Lttl.editor.getInput().isShiftSwing()
								&& cachedShiftTimes != null
								&& cachedShiftTimes.size > 0)
						{
							int realTick = masterSlider
									.getValueForXPosition(mouseXPos);
							int closestTick = cachedShiftTimes.get(0);
							for (int i = 1; i < cachedShiftTimes.size; i++)
							{
								if (LttlMath.abs(realTick
										- cachedShiftTimes.get(i)) < LttlMath
										.abs(realTick - closestTick))
								{
									closestTick = cachedShiftTimes.get(i);
								}
							}
							tick = closestTick;
							time = tickToTime(closestTick);
						}

						// update all the node with new time
						for (TimelineNode node : focusedNodes)
						{
							node.time = time;
							node.slider.setValue(tick);
						}
						refreshAllGroupSliders(true);

						updateCurrentTimeLabel(time);

						// now that node's time's have been updated
						clampAndSetSeekTime();
						setMinEditorDuration();
						refreshSeekSlider();
						updateAnim();
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

					// if master node changed time position
					float time = tickToTime(dragSlider.getValue());
					if (!LttlMath.isEqual(undoGroupTime, time))
					{
						final ArrayList<TimelineNode> undoNodes = focusedNodes;
						UndoState undoState = new UndoState(manager.t()
								.getName()
								+ " ("
								+ manager.getClass().getSimpleName()
								+ ") - Animations:"
								+ anim.name
								+ ":Moved master animation node",
								new UndoField(manager.t(), undoGroupTime, time,
										new UndoSetter()
										{
											@Override
											public void set(LttlComponent comp,
													Object value)
											{
												for (TimelineNode node : undoNodes)
												{
													node.time = (float) value;
												}
											}
										}));
						// save the undoState so we can remove all of them when animation window closes
						undoStates.add(undoState);
						Lttl.editor.getUndoManager().registerUndoState(
								undoState);
					}

					focusedNodes = null;
					focusedGroup = null;

					refreshMasterSliderAndAllGroups(false);
				}
			}
		});
		JPanel multiSliderPanel = new JPanel(new BorderLayout());
		multiSliderPanel.setPreferredSize(new Dimension(1, 20));
		multiSliderPanel.add(masterSlider);
		formatGroupSlider(masterSlider.getSlider(0));
		gbc.gridx = 0;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.weightx = 1;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(5, 0, 0, 0);
		vertPanel.add(multiSliderPanel, gbc);
		gbc.insets = new Insets(0, 0, 0, 0);

		/* STATE SEQUENCES */
		boolean collapseState = (GuiAnimationEditor.this.anim.stateSequences
				.size() + GuiAnimationEditor.this.anim.animComps.size()) > 1;
		stateSeqPanel = new JPanel(new GridBagLayout());
		for (final AnimationSequence seq : anim.stateSequences)
		{
			AnimGuiSequence ag = new AnimGuiSequence(this, seq);
			animStatesGUI.add(ag);
			ag.group.setCollapseState(collapseState);
			stateSeqPanel.add(ag.group.getPanel(), gbcSequence);
		}
		gbc.gridx = 0;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.weightx = 1;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.WEST;
		vertPanel.add(stateSeqPanel, gbc);

		/* COMPONENTS */
		compPanel = new JPanel(new GridBagLayout());
		for (final AnimatedComponent ac : anim.animComps)
		{
			AnimGuiObject ag = new AnimGuiObject(this, null, ac, null);
			animCompsGUI.add(ag);
			ag.group.setCollapseState(collapseState);
			compPanel.add(ag.group.getPanel(), gbcSequence);
		}
		gbc.gridx = 0;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.weightx = 1;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.WEST;
		vertPanel.add(compPanel, gbc);

		vertPanel.add(new JPanel(), new GridBagConstraints(0, -1, 1, 1, 1, 1,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 0, 0, 0), 0, 0));

		horizPanel = new JPanel(new GridBagLayout());

		gbc.gridx = 0;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(0, 0, 0, 0);
		horizPanel.add(vertScrollPane, gbc);

		/* SEEK SLIDER */
		seekPanel = new JPanel(new GridBagLayout());
		seekSlider = new JSlider(0, sliderMax, 0);
		seekGoTo();
		addTickMarks(seekSlider);
		seekSlider.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent e)
			{
				// set seekTime field to seek slider's value
				float sliderTime = tickToTime(seekSlider.getValue());
				float greatest = anim.getLatestNodeTime();
				if (sliderTime > greatest)
				{
					seekSlider.setValue(timeToTick(greatest));
					return;
				}

				// prevents loop betweens seekTime and seekSlider
				if (seekTimeUpdating)
				{
					seekTimeUpdating = false;
					return;
				}

				seekSliderUpdating = true;

				seekTimeGFO.setValue(sliderTime);
				seekTimeGFO.updatePrimativeValue();
				seekTimeGFO.processChangeCallback(2);
			}
		});
		seekSlider.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				seekTimeGFO.setUndoValue(seekTimeGFO.getValue());
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				// create seekTimer undo if values different
				seekTimeGFO.registerUndo();
			}
		});
		seekSlider.addMouseMotionListener(new MouseMotionAdapter()
		{
			@Override
			public void mouseMoved(MouseEvent e)
			{
				updateCurrentTimeLabel(e);
			}

			@Override
			public void mouseDragged(MouseEvent e)
			{
				mouseMoved(e);
			}
		});
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.WEST;
		seekPanel.add(seekSlider, gbc);

		gbc.gridx = 0;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.weightx = 1;
		gbc.gridwidth = 1;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(0, 0, 0, 0);
		horizPanel.add(seekPanel, gbc);

		horizScrollPane = new JScrollPane(horizPanel);
		horizScrollPane
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

		gbc.gridx = 0;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.gridwidth = 3;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.anchor = GridBagConstraints.NORTH;
		mainPanel.add(horizScrollPane, gbc);

		bottomPanel = new JPanel(new GridBagLayout());

		/* Zoom Slider */
		// ratio slider
		zoomSlider = new GuiLttlSlider(SwingConstants.HORIZONTAL, .01f, 1,
				maxZoomFactor, 1, false);
		zoomSlider.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent e)
			{
				updateVertScroll();
			}
		});

		// FILLER
		gbc.gridx = 0;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.CENTER;
		bottomPanel.add(new JLabel(), gbc);

		// add zoom slider
		gbc.gridx = 1;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(0, 0, 0, 20);
		bottomPanel.add(zoomSlider, gbc);
		gbc.insets = new Insets(0, 0, 0, 0);

		// Seek Time Field
		Field seekTimeField = null;
		try
		{
			seekTimeField = this.getClass().getDeclaredField("seekTime");
		}
		catch (NoSuchFieldException e1)
		{
			e1.printStackTrace();
		}
		catch (SecurityException e1)
		{
			e1.printStackTrace();
		}
		Lttl.Throw(seekTimeField);
		final GuiComponentNumber<Float> seekTimeGFO = new GuiComponentNumber<Float>(
				new ProcessedFieldType(seekTimeField), this, -1, null);
		seekTimeGFO.minMut = new LttlMutatableFloat();
		seekTimeGFO.minMut.value = 0;
		seekTimeGFO.maxMut = new LttlMutatableFloat();
		seekTimeGFO.maxMut.value = anim.editorDuration;
		this.seekTimeGFO = seekTimeGFO;
		seekTimeGFO.addChangeListener(new GuiLttlChangeListener()
		{
			void onChange(int changeId)
			{
				if (!isPlaying)
				{
					seekGoTo();
				}

				if (seekSliderUpdating)
				{
					seekSliderUpdating = false;
					return;
				}
				seekTimeUpdating = true;
				seekSlider.setValue(timeToTick(seekTime));
			}
		});
		seekTimeGFO.getPanel().setPreferredSize(
				new Dimension(180, GuiHelper.defaultFieldDimension.height));

		gbc.gridx = 2;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.EAST;
		bottomPanel.add(seekTimeGFO.getPanel(), gbc);

		gbc.gridx = 0;
		gbc.gridy = GridBagConstraints.RELATIVE;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.gridwidth = 3;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.WEST;
		mainPanel.add(bottomPanel, gbc);

		/* FIRST UPDATES */
		updateEditorDuration();
		refreshMasterSliderAndAllGroups(false);

		// CHECK FOR CHANGED VALUES
		// need to iterate through all current values and check if they are different from true values, this way
		// undo/redo will work along if any values are changed outside the animator editor, which really isn't possible
		final Timer timer = new Timer(200, new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (liveUpdateToggle.isSelected() && !isPlaying)
				{
					updateCachedAnim();
					seekGoTo();
				}

				// check nodes
				boolean change = false;
				for (AnimGuiSequence agui : getAllSequenceAnimGUIs())
				{
					for (TimelineNode node : agui.seq.nodes)
					{
						if (!LttlMath.isEqual(node.time,
								tickToTime(node.slider.getValue())))
						{
							change = true;
							if (node.time > anim.editorDuration)
							{
								node.time = anim.editorDuration;
							}
							skipCachedAnimUpdate = true;
							node.slider.setValue(timeToTick(node.time));
						}
					}
				}
				if (change)
				{
					refreshMasterSliderAndAllGroups(false);

					clampAndSetSeekTime();
					setMinEditorDuration();
					updateEditorDuration();
					refreshSeekSlider();
					updateCachedAnim();
					seekGoTo();
				}

			}
		});
		timer.start();

		final JDialog dialog = GuiHelper.createDialog("Animation Editor",
				(int) Lttl.editor.getSettings().animationWindowSize.x,
				(int) Lttl.editor.getSettings().animationWindowSize.y, false,
				true, ModalityType.MODELESS, mainPanel, new LttlCallback()
				{
					@Override
					public void callback(int id, Object... objects)
					{
						if (id == 1 || id == 2)
						{
							// exiting
							timer.stop();
							// unregister undo states since aniamtion window is closing
							Lttl.editor.getUndoManager().unregisterUndoStates(
									undoStates);
							seekTimeGFO.unregisterAllUndoStates();
							editorDurationGFO.unregisterAllUndoStates();
							Lttl.editor.getGui().animEditors
									.remove(GuiAnimationEditor.this);
							Lttl.editor.getInput().removeListener(
									spaceKeyListener);
						}
					}
				});
		dialog.setLocation(
				(int) Lttl.editor.getSettings().animationWindowLocation.x,
				(int) Lttl.editor.getSettings().animationWindowLocation.y);
		dialog.addComponentListener(new ComponentListener()
		{

			@Override
			public void componentShown(ComponentEvent e)
			{
			}

			@Override
			public void componentResized(ComponentEvent e)
			{
				// constrain to min
				if (dialog.getSize().width < 1210)
				{
					dialog.setSize(1210, dialog.getSize().height);
				}
				if (dialog.getSize().height < 175)
				{
					dialog.setSize(dialog.getSize().width, 175);
				}

				updateVertScroll();
				Lttl.editor.getSettings().animationWindowSize.set(
						dialog.getSize().width, dialog.getSize().height);
			}

			@Override
			public void componentMoved(ComponentEvent e)
			{
				Lttl.editor.getSettings().animationWindowLocation.set(
						dialog.getLocation().x, dialog.getLocation().y);
			}

			@Override
			public void componentHidden(ComponentEvent e)
			{
			}
		});
		mainDialog = dialog;
		dialog.setVisible(true);
	}

	void refreshGroupSlider(GuiLttlMultiSlider multiSlider,
			HashMap<Float, ArrayList<TimelineNode>> nodeTimeMap)
	{
		JSlider s = null;
		int count = 0;
		for (Float time : nodeTimeMap.keySet())
		{
			if (count == 0)
			{
				// set first keyframe to main slider
				s = multiSlider.getSlider(0);
				s.setValue(timeToTick(time));
			}
			else
			{
				if (count < multiSlider.getSliderCount())
				{
					s = multiSlider.getSlider(count);
					multiSlider.setValue(s, timeToTick(time));
				}
				else
				{
					s = multiSlider.addSlider(timeToTick(time));
				}
			}
			formatGroupSlider(s);

			count++;
		}

		while (multiSlider.getSliderCount() > 1
				&& multiSlider.getSliderCount() > nodeTimeMap.size())
		{
			multiSlider.removeSlider(nodeTimeMap.size());
		}
	}

	void refreshMasterSlider()
	{
		refreshGroupSlider(masterSlider,
				getAllNodesTimeMap(getAllSequenceAnimGUIs()));
	}

	void refreshAllGroupSliders(boolean onlyVisible)
	{
		for (AnimGuiObject animCompGui : animCompsGUI)
		{
			animCompGui.refreshGroupSliderSelfAndDescendants(onlyVisible);
		}
	}

	void refreshMasterSliderAndAllGroups(boolean onlyVisible)
	{
		refreshGroupSlider(masterSlider,
				getAllNodesTimeMap(getAllSequenceAnimGUIs()));
		for (AnimGuiObject animCompGui : animCompsGUI)
		{
			animCompGui.refreshGroupSliderSelfAndDescendants(onlyVisible);
		}
	}

	private void updateVertScroll()
	{
		vertScrollPane.setPreferredSize(new Dimension((int) (zoomSlider
				.getValueFloat() * (horizScrollPane.getWidth() - 50)),
				horizScrollPane.getHeight() - seekPanel.getHeight() * 2));
		vertScrollPane.revalidate();
		vertScrollPane.repaint();
		horizScrollPane.revalidate();
		horizScrollPane.repaint();
	}

	int timeToTick(float time)
	{
		return LttlMath.round(time / anim.editorDuration * sliderMax);
	}

	float tickToTime(int tick)
	{
		return (tick / (sliderMax * 1f)) * anim.editorDuration;
	}

	static Color holdColor = new Color(1, 0, 0, .5f);
	static Color callbackColor = new Color(1, 20 / 255f, 147 / 255f, .5f);
	static Color keyframeColor = new Color(29 / 255f, 228 / 255f, 0, .5f);
	static Color keyframeSetColor = new Color(29 / 255f, 143 / 255f, 0, .5f);
	static Color keyframeErrorColor = new Color(1, .5f, 0, .5f);
	static Color groupNode = new Color(0, 0, .7f, .5f);

	void formatGroupSlider(JSlider slider)
	{
		slider.setUI(new BasicSliderUI(slider)
		{
			public void paintThumb(Graphics g)
			{
				g.setColor(groupNode);
				g.fillPolygon(
						new int[]
						{ thumbRect.x, thumbRect.x + thumbRect.width,
								thumbRect.x + thumbRect.width, thumbRect.x,
								thumbRect.x }, new int[]
						{ thumbRect.y, thumbRect.y, thumbRect.y + 15,
								thumbRect.y + 15, thumbRect.y }, 5);
			}
		});
		addTickMarks(slider);
	}

	/**
	 * Set color
	 * 
	 * @param slider
	 * @param node
	 */
	void formatSlider(JSlider slider, TimelineNode node)
	{
		Class<? extends TimelineNode> nodeClass = node.getClass();
		Color c = null;
		if (KeyframeNode.class.isAssignableFrom(nodeClass))
		{
			KeyframeNode kf = (KeyframeNode) node;
			if (kf.set)
			{
				c = keyframeSetColor;
			}
			else
			{
				c = keyframeColor;
			}
			if (nodeClass == StateKeyframeNode.class)
			{
				StateKeyframeNode skf = (StateKeyframeNode) node;
				if (skf.stateName.isEmpty()
						|| !manager.getAllStateNames().contains(skf.stateName))
				{
					c = keyframeErrorColor;
				}
			}
		}
		else if (nodeClass == AnimationCallbackNode.class)
		{
			c = callbackColor;
		}
		else if (nodeClass == AnimationHoldNode.class)
		{
			c = holdColor;
		}
		final Color fColor = new Color(c.getRed(), c.getGreen(), c.getBlue(),
				LttlMath.floor(c.getAlpha() * ((node.active) ? 1 : .3f)));

		slider.setUI(new BasicSliderUI(slider)
		{
			public void paintThumb(Graphics g)
			{
				g.setColor(fColor);
				g.fillPolygon(
						new int[]
						{ thumbRect.x, thumbRect.x + thumbRect.width,
								thumbRect.x + thumbRect.width, thumbRect.x,
								thumbRect.x }, new int[]
						{ thumbRect.y, thumbRect.y, thumbRect.y + 15,
								thumbRect.y + 15, thumbRect.y }, 5);
			}
		});
		addTickMarks(slider);
	}

	private void addTickMarks(JSlider slider)
	{
		if (slider == null) return;

		int tick = LttlMath.round(sliderMax / anim.editorDuration);
		int minorTick = LttlMath.round(tick / 10f);
		slider.setPaintTicks(true);
		slider.setMajorTickSpacing(tick);
		slider.setMinorTickSpacing(minorTick);
	}

	/**
	 * updates timeleine nodes, ticks, and seek slider
	 */
	private void updateEditorDuration()
	{
		// shift all keyframes to scale based on new editorDuration
		ArrayList<AnimGuiSequence> list = getAllSequenceAnimGUIs();
		for (AnimGuiSequence aGUI : list)
		{
			for (TimelineNode node : aGUI.seq.nodes)
			{
				skipCachedAnimUpdate = true;
				node.slider.setValue(timeToTick(node.time));
			}
		}

		// adjust 1 second ticks
		for (AnimGuiSequence aGUI : list)
		{
			for (TimelineNode node : aGUI.seq.nodes)
			{
				skipCachedAnimUpdate = true;
				formatSlider(node.slider, node);
			}
		}

		// adjust seekSlider so it stays in same place
		refreshSeekSlider();
		addTickMarks(seekSlider);
	}

	private ArrayList<AnimGuiSequence> getAllFieldSequenceAnimGUIs()
	{
		ArrayList<AnimGuiSequence> list = new ArrayList<AnimGuiSequence>();
		for (AnimGuiObject ac : animCompsGUI)
		{
			ac.getAllSequencesGUIs(list);
		}
		return list;
	}

	/**
	 * Returns all the AnimGuiSequences (states and fields)
	 * 
	 * @return
	 */
	private ArrayList<AnimGuiSequence> getAllSequenceAnimGUIs()
	{
		ArrayList<AnimGuiSequence> allSequenceAnimGUIs = new ArrayList<AnimGuiSequence>();
		for (AnimGuiObject animCompGUI : animCompsGUI)
		{
			animCompGUI.getAllChildSequenceAnimGUIs(allSequenceAnimGUIs);
		}
		allSequenceAnimGUIs.addAll(animStatesGUI);
		return allSequenceAnimGUIs;
	}

	/**
	 * refreshes, but does not animate
	 */
	void refreshSeekSlider()
	{
		if (seekSlider == null) return;

		seekSlider.setValue(timeToTick(seekTime));
	}

	void setMinEditorDuration()
	{
		if (editorDurationGFO == null) return;

		if (editorDurationGFO.minMut == null)
			editorDurationGFO.minMut = new LttlMutatableFloat();
		editorDurationGFO.minMut.value = anim.getLatestNodeTime();
	}

	/**
	 * returns true if clamped and set value of seekTime field
	 * 
	 * @return
	 */
	void clampAndSetSeekTime()
	{
		if (seekTimeGFO == null) return;

		float greatestTime = anim.getLatestNodeTime();

		// clamping time to last node and show the result in the spinner
		if (seekTimeGFO.maxMut == null)
			seekTimeGFO.maxMut = new LttlMutatableFloat();
		seekTimeGFO.maxMut.value = greatestTime;
		if (seekTime > greatestTime)
		{
			seekTimeGFO.setValue(greatestTime);
			seekTimeGFO.updatePrimativeValue();
		}
	}

	/**
	 * sets the values for where the seek time is for all active sequences
	 */
	void seekGoTo()
	{
		seekGoTo(this.seekTime);
	}

	/**
	 * sets the values for whatever time given
	 */
	private void seekGoTo(float seekTime)
	{
		cachedAnim.clean();
		// fixes bug about going to 0.0
		cachedAnim.update(getSafeSeekTime());
	}

	void updateCurrentTimeLabel(MouseEvent e)
	{
		BasicSliderUI bsUI = (BasicSliderUI) ((JSlider) e.getSource()).getUI();
		updateCurrentTimeLabel(tickToTime(bsUI.valueForXPosition(e.getX())));
	}

	void updateCurrentTimeLabel(float time)
	{
		currentCursorTimeLabel.setText(String.format("%.3f", time));
	}

	private void updateCachedAnim()
	{
		if (skipCachedAnimUpdate)
		{
			skipCachedAnimUpdate = false;
			return;
		}
		cachedAnim = getPlayAnim(false).startUnmanaged();
	}

	/**
	 * Returns the animation (not started) for playing in editor,
	 * 
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private Timeline getPlayAnim(boolean callbackNodes)
	{
		// go through all the aniamated components that have a target and a relative state, and set it to that state for
		// taht component
		for (AnimatedComponent animComp : anim.animComps)
		{
			if (animComp.target != null && !animComp.relativeState.isEmpty())
			{
				for (LttlStateManager lsm : animComp.target.t().getComponents(
						LttlStateManager.class, true))
				{
					if (lsm.getTargetClass().isAssignableFrom(
							animComp.target.getClass()))
					{
						StateBase state = lsm.getState(animComp.relativeState);
						if (state != null)
						{
							state.goTo(animComp.target);
						}
						else
						{
							Lttl.logNote("Aniamtion Editor - Relative State: could not find state "
									+ animComp.relativeState
									+ " on component "
									+ animComp.target.toString());
						}
					}
				}
			}
		}

		return anim.getAnimationTween(manager.stateTargetTrees, manager, false,
				callbackNodes);
	}

	void update()
	{
		if (isPlaying && cachedPlayAnim != null)
		{
			if (liveUpdateToggle.isSelected())
			{
				// increment time
				float newTime = cachedPlayAnim.getCurrentTime()
						/ speedMultiplier
						+ Lttl.game
								.getDeltaTime(Lttl.game.getSettings().animationDeltaTimeType);
				// refresh animation
				updatePlayAnim(false);

				cachedPlayAnim.update(newTime);
			}
			else
			{
				cachedPlayAnim.update(Lttl.game.getDeltaTime(Lttl.game
						.getSettings().animationDeltaTimeType));
			}
		}
	}

	private void updatePlayAnim(boolean editorCallbacks)
	{
		cachedPlayAnim = getPlayAnim(editorCallbacks);
		cachedPlayAnim.addCallback(new TweenCallback()
		{
			@Override
			public void onStep(float iterationPosition)
			{
				// set slider
				seekSlider.setValue(timeToTick(iterationPosition
						* cachedPlayAnim.getDuration()));
			}

			@Override
			public void onComplete()
			{
				playStopButton.doClick();
			}
		});
		if (anim.editorRepeatType == RepeatTweenType.Rewind)
		{
			cachedPlayAnim.repeat(-1, 0);
		}
		else if (anim.editorRepeatType == RepeatTweenType.YoYo)
		{
			cachedPlayAnim.repeatYoyo(-1, 0);
		}
		cachedPlayAnim.setSpeedMultiplier(speedMultiplier);
		cachedPlayAnim.startUnmanaged();

		// prevents infinite loop of 0 duration animation
		if (cachedPlayAnim.getDuration() <= 0 && isPlaying)
		{
			playStopButton.doClick();
		}
	}

	/**
	 * universal method to update animation and go to seek position
	 */
	void updateAnim()
	{
		if (isPlaying)
		{
			if (liveUpdateToggle.isSelected())
			{
				// disable editor callbacks because it would call them every frame
				updatePlayAnim(false);
			}
			else
			{
				updatePlayAnim(anim.editorCallbacks);
			}

			// will be null if there is nothing tweening
			if (cachedPlayAnim != null)
			{
				cachedPlayAnim.update(getSafeSeekTime() / speedMultiplier);
			}
		}
		else
		{
			updateCachedAnim();
			cachedAnim.update(getSafeSeekTime());
		}
	}

	private float getSafeSeekTime()
	{
		return LttlMath.max(.00000000001f, seekTime);
	}

	void removeSequence(final AnimGuiSequence sg)
	{
		if (!sg.isFieldSequence())
		{
			/* STATE */
			// remove from animation object
			anim.stateSequences.remove(sg.seq);

			// remove from GUI
			animStatesGUI.remove(sg);
			stateSeqPanel.remove(sg.group.getPanel());
			stateSeqPanel.revalidate();
			stateSeqPanel.repaint();
		}
		else
		{
			/* FIELD */
			// remove from animation object
			LttlHelper.RemoveHashMapValue(sg.parentGui.animObject.sequences,
					sg.seq, true, false);

			// remove from GUI
			sg.parentGui.sequenceAnimGUIs.remove(sg);
			sg.parentGui.group.getCollapsePanel().remove(sg.group.getPanel());
			sg.parentGui.group.getPanel().revalidate();
			sg.parentGui.group.getPanel().repaint();
		}
		updateAnim();
	}

	void collapseAll()
	{
		for (AnimGuiSequence sg : animStatesGUI)
		{
			sg.group.setCollapseState(true);
		}
		// collapse all animated components
		for (AnimGuiObject og : animCompsGUI)
		{
			og.collapseAll();
		}
	}

	/**
	 * Assumes all group sliders are up to date (refreshed)
	 * 
	 * @param containerArray
	 */
	void getAllNodeTimes(IntArray containerArray)
	{
		for (AnimGuiBase base : getAllBases())
		{
			for (Component slider : base.multiSlider.getComponents())
			{
				int it = ((JSlider) slider).getValue();
				if (!containerArray.contains(it))
				{
					containerArray.add(it);
				}
			}
		}
	}

	ArrayList<AnimGuiBase> getAllBases()
	{
		ArrayList<AnimGuiBase> bases = new ArrayList<AnimGuiBase>();
		bases.addAll(animCompsGUI);
		bases.addAll(animStatesGUI);
		return bases;
	}

	/**
	 * @param allSeqGui
	 *            give it all the sequenceAnimGuis getAllSequenceAnimGUIs()
	 * @return
	 */
	private HashMap<Float, ArrayList<TimelineNode>> getAllNodesTimeMap(
			ArrayList<AnimGuiSequence> allSeqGui)
	{
		HashMap<Float, ArrayList<TimelineNode>> sharedMap = new HashMap<Float, ArrayList<TimelineNode>>();
		for (AnimGuiSequence seqGui : allSeqGui)
		{
			seqGui.getNodesTimeMap(sharedMap);
		}
		return sharedMap;
	}

	/**
	 * @param time
	 * @param allSeqGui
	 *            give it all the sequenceAnimGuis getAllSequenceAnimGUIs()
	 * @return
	 */
	private ArrayList<TimelineNode> getAllNodesAtTime(float time,
			ArrayList<AnimGuiSequence> allSeqGui)
	{
		return getAllNodesTimeMap(allSeqGui).get(time);
	}

	/**
	 * Still need to call dialog.setVisible(true)
	 */
	JDialog createPropertyDialog()
	{
		JPanel mainPanel = new JPanel(new GridBagLayout());
		JPanel contentPanel = new JPanel(new GridBagLayout());
		JScrollPane scrollPane = new JScrollPane(contentPanel);

		GridBagConstraints scrollGBC = new GridBagConstraints();
		scrollGBC.weightx = 1;
		scrollGBC.weighty = 1;
		scrollGBC.fill = GridBagConstraints.BOTH;
		scrollGBC.anchor = GridBagConstraints.NORTH;
		mainPanel.add(scrollPane, scrollGBC);
		propertyDialog = GuiHelper.createDialog("Properties", 300, 250, true,
				true, ModalityType.MODELESS, mainPanel, new LttlCallback()
				{

					@Override
					public void callback(int id, Object... objects)
					{
						if (id == 1 || id == 2)
						{
							propertyDialog = null;
						}
					}
				});
		propertyDialog.setResizable(true);
		GuiComponentObject gfo = (GuiComponentObject) GuiFieldObject
				.GenerateGuiFieldObject(new Object()
				{
					/* WOAH - this actually works, lol.  Super ninja coding skills! */
					@GuiShow
					@GuiCallbackDescendants("callback")
					@GuiCallback("callback")
					@GuiListItemNameField("name")
					public ArrayList<AnimationProperty> properties = anim.properties;

					@SuppressWarnings("unused")
					public void callback()
					{
						updateAnim();
					}
				});
		gfo.setAutoRegisterUndo(true);
		gfo.collapsableGroup.setCollapseState(false);
		gfo.collapsableGroup.getToggleButton().setVisible(false);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		contentPanel.add(gfo.getPanel(), gbc);

		// add filler
		contentPanel.add(new JPanel(), new GridBagConstraints(0,
				GridBagConstraints.RELATIVE, 1, 1, 1, 1,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 0, 0, 0), 0, 0));

		return propertyDialog;
	}

	/**
	 * Still need to call dialog.setVisible(true)
	 * 
	 * @param node
	 * @param isBatch
	 * @param callbackOnClosed
	 *            can be null, called back on close
	 * @return
	 */
	JDialog createEditNodeDialog(Object node,
			final LttlCallback callbackOnClosed, boolean isBatch)
	{
		JPanel mainPanel = new JPanel(new GridBagLayout());
		JPanel contentPanel = new JPanel(new GridBagLayout());
		JScrollPane scrollPane = new JScrollPane(contentPanel);

		GridBagConstraints scrollGBC = new GridBagConstraints();
		scrollGBC.weightx = 1;
		scrollGBC.weighty = 1;
		scrollGBC.fill = GridBagConstraints.BOTH;
		scrollGBC.anchor = GridBagConstraints.NORTH;
		mainPanel.add(scrollPane, scrollGBC);
		final JDialog dialog = GuiHelper.createDialog(
				isBatch ? "Batch Edit Node" : "Edit Node", 300, 250, true,
				true, ModalityType.MODELESS, mainPanel, new LttlCallback()
				{
					@Override
					public void callback(int id, Object... objects)
					{
						if (id != 1 && id != 2) return;

						if (callbackOnClosed != null)
						{
							callbackOnClosed.callback(0, (Object[]) null);
						}
					}
				});
		dialog.setResizable(true);
		GuiComponentObject gfo = (GuiComponentObject) GuiFieldObject
				.GenerateGuiFieldObject(node);
		gfo.setAutoRegisterUndo(false);
		gfo.collapsableGroup.setCollapseState(false);
		gfo.collapsableGroup.getToggleButton().setVisible(false);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// if state keyframe then add a string selector for states
		if (node.getClass() == StateKeyframeNode.class)
		{
			final StateKeyframeNode keyframe = (StateKeyframeNode) node;
			ArrayList<String> names = manager.getAllStateNames();
			names.add(0, "");
			final JComboBox<String> stateNamesCombo = new JComboBox<String>(
					names.toArray(new String[names.size()]));

			if (names.contains(keyframe.stateName))
			{
				stateNamesCombo.setSelectedItem(keyframe.stateName);
			}

			stateNamesCombo.addItemListener(new ItemListener()
			{
				@Override
				public void itemStateChanged(ItemEvent e)
				{
					keyframe.stateName = (String) stateNamesCombo
							.getSelectedItem();
				}
			});

			JPanel panel = new JPanel(new GridBagLayout());
			panel.add(new JLabel("State: "), gbc);
			gbc.gridx = 1;
			panel.add(stateNamesCombo, gbc);

			gbc.gridx = 0;
			contentPanel.add(panel, gbc);
			gbc.gridy = 1;
		}
		contentPanel.add(gfo.getPanel(), gbc);

		// add filler
		contentPanel.add(new JPanel(), new GridBagConstraints(0,
				GridBagConstraints.RELATIVE, 1, 1, 1, 1,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 0, 0, 0), 0, 0));

		return dialog;
	}

	@GuiTwoColumn
	@GuiShow
	private class DummyNode
	{
		@GuiShow
		@GuiMin(0)
		float time;
		@GuiShow
		@GuiHideLabel
		boolean saveTime;
		@GuiShow
		float value;
		@GuiShow
		@GuiHideLabel
		boolean saveValue;
		@GuiShow
		String property;
		@GuiShow
		@GuiHideLabel
		boolean saveMappedPropery;
		@GuiShow
		boolean set = false;
		@GuiShow
		@GuiHideLabel
		boolean saveSet;
		@GuiShow
		EaseType easeType = EaseType.QuadInOut;
		@GuiShow
		@GuiHideLabel
		boolean saveEaseType;
		@GuiShow
		boolean active = true;
		@GuiShow
		@GuiHideLabel
		boolean saveActive;

		public DummyNode(float time)
		{
			this.time = time;
		}
	}

	void createDummyNodeDialog(final float time,
			final ArrayList<TimelineNode> nodes)
	{
		final DummyNode dum = new DummyNode(time);

		final HashMap<AnimGuiSequence, ArrayList<TimelineNode>> undoValue = getAllCurrentAnimSeqNodeMap();

		createEditNodeDialog(dum, new LttlCallback()
		{
			@Override
			public void callback(int id, Object... value)
			{
				for (TimelineNode node : nodes)
				{
					if (dum.saveTime)
					{
						node.time = dum.time;
					}
					if (dum.saveActive)
					{
						node.active = dum.active;
					}

					if (KeyframeNode.class.isAssignableFrom(node.getClass()))
					{
						KeyframeNode kf = (KeyframeNode) node;
						if (dum.saveEaseType)
						{
							kf.easeType = dum.easeType;
						}
						if (dum.saveSet)
						{
							kf.set = dum.set;
						}
					}

					if (FieldKeyframeNode.class.isAssignableFrom(node
							.getClass()))
					{
						FieldKeyframeNode fkf = (FieldKeyframeNode) node;
						if (dum.saveValue)
						{
							fkf.value = dum.value;
						}
						if (dum.saveMappedPropery)
						{
							fkf.property = dum.property;
						}
					}
				}
				registerUndoState(undoValue, "Batch Edit");
				refreshMasterSliderAndAllGroups(false);
				updateAnim();
			}
		}, true).setVisible(true);
	}

	void registerUndoState(
			HashMap<AnimGuiSequence, ArrayList<TimelineNode>> undoValue,
			String description)
	{
		UndoState undoState = new UndoState(manager.t().getName() + " ("
				+ manager.getClass().getSimpleName() + ") - Animations:"
				+ anim.name + ": " + (description == null ? "" : description),
				new UndoField(manager.t(), undoValue,
						getAllCurrentAnimSeqNodeMap(), new UndoSetter()
						{
							@Override
							public void set(LttlComponent comp, Object value)
							{
								HashMap<AnimGuiSequence, ArrayList<TimelineNode>> map = (HashMap<AnimGuiSequence, ArrayList<TimelineNode>>) value;
								for (AnimGuiSequence ags : getAllSequenceAnimGUIs())
								{
									if (map.containsKey(ags))
									{
										// remove all current nodes
										// need to make new list because they are getting removed from actual list
										for (TimelineNode node : new ArrayList<TimelineNode>(
												ags.seq.nodes))
										{
											ags.removeNode(node, false, false);
										}
										// add all undo state nodes
										for (TimelineNode node : map.get(ags))
										{
											ags.addNode(node, false);
										}
									}
								}
								refreshMasterSliderAndAllGroups(false);
								updateAnim();
							}
						}));
		undoStates.add(undoState);
		Lttl.editor.getUndoManager().registerUndoState(undoState);
	}

	/**
	 * get the current state for just one anim gui sequence
	 * 
	 * @param animGuiSeq
	 * @return
	 */
	HashMap<AnimGuiSequence, ArrayList<TimelineNode>> getCurrentAnimSeqNodeMap(
			AnimGuiSequence animGuiSeq)
	{
		HashMap<AnimGuiSequence, ArrayList<TimelineNode>> map = new HashMap<AnimGuiSequence, ArrayList<TimelineNode>>();
		map.put(animGuiSeq, LttlCopier.copy(animGuiSeq.seq.nodes));
		return map;
	}

	HashMap<AnimGuiSequence, ArrayList<TimelineNode>> getAllCurrentAnimSeqNodeMap()
	{
		HashMap<AnimGuiSequence, ArrayList<TimelineNode>> map = new HashMap<AnimGuiSequence, ArrayList<TimelineNode>>();
		for (AnimGuiSequence ags : getAllSequenceAnimGUIs())
		{
			map.put(ags, LttlCopier.copy(ags.seq.nodes));
		}

		return map;
	}
}
