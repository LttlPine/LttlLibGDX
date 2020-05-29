package com.lttlgames.editor;

import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;
import com.lttlgames.helpers.EaseType;
import com.lttlgames.helpers.LttlCallback;
import com.lttlgames.helpers.LttlCameraTransformState;
import com.lttlgames.helpers.LttlHelper;
import com.lttlgames.helpers.LttlMututatableObject;
import com.lttlgames.helpers.LttlObjectHelper;
import com.lttlgames.tweenengine.BaseTween;

class GuiMenuBarController
{
	private JLabel undoStatusLabel;
	private static JMenuBar menuBar;
	private ArrayList<GuiLttlMenuHotkeyItem> items = new ArrayList<GuiMenuBarController.GuiLttlMenuHotkeyItem>();

	public GuiMenuBarController()
	{
		items.clear();
		init();
		build();
	}

	/**
	 * The initial menu items and hotkeys registers
	 */
	private void init()
	{
		/*** FILE ***/
		// RELOAD EDITOR
		register(new GuiLttlMenuHotkeyItem("Reload", "File", new Hotkey(true,
				true, false, Keys.R))
		{
			@Override
			boolean validate()
			{
				return true;
			}

			@Override
			void action(boolean triggeredFromHotkey)
			{
				Lttl.logNote("Reloading...");
				Lttl.scenes.deleteAllTempScenes();
				Lttl.game.stopPlayingInEditorMode(false);

				// if trigered by hotkey, then it could be in the loop, so throw. Don't want to throw when triggered by
				// menu click because it will throw when not in loop.
				if (triggeredFromHotkey) { throw new KillLoopException(); }
			}
		});

		// SAVE
		register(new GuiLttlMenuHotkeyItem("Save", "File", new Hotkey(false,
				true, false, Keys.S))
		{
			{
				inPlayModeToo = true;
			}

			@Override
			boolean validate()
			{
				return !Lttl.game.isPlaying();
			}

			@Override
			void action(boolean clickedInMenu)
			{
				Lttl.scenes.saveAllScenes();
				Lttl.editor.getGui().getStatusBarController().saved();
			}
		});

		// SAVE AND QUIT
		register(new GuiLttlMenuHotkeyItem("Save and Quit", "File", new Hotkey(
				true, true, false, Keys.S))
		{
			@Override
			boolean validate()
			{
				return !Lttl.game.isPlaying();
			}

			@Override
			void action(boolean clickedInMenu)
			{
				Lttl.scenes.saveAllScenes();
				Lttl.editor.getGui().getFrame().dispose();
			}
		});

		// QUIT
		register(new GuiLttlMenuHotkeyItem("Quit", "File", new Hotkey(false,
				true, false, Keys.Q))
		{
			@Override
			boolean validate()
			{
				return true;
			}

			@Override
			void action(boolean clickedInMenu)
			{
				Lttl.editor.getGui().getFrame().dispose();
			}
		});

		/*** EDIT ***/
		// UNDO
		register(new GuiLttlMenuHotkeyItem("Undo", "Edit", new Hotkey(false,
				true, false, Keys.Z))
		{
			@Override
			boolean validate()
			{
				if (Lttl.editor.getUndoManager().canUndo())
				{
					menuItem.setText("Undo "
							+ Lttl.editor.getUndoManager().getUndoDescription());
					return true;
				}
				else
				{
					menuItem.setText("Undo");
					return false;
				}
			}

			@Override
			void action(boolean clickedInMenu)
			{
				Lttl.editor.getUndoManager().undo();
			}
		});

		// REDO
		register(new GuiLttlMenuHotkeyItem("Redo", "Edit", new Hotkey(true,
				true, false, Keys.Z))
		{
			@Override
			boolean validate()
			{
				if (Lttl.editor.getUndoManager().canRedo())
				{
					menuItem.setText("Redo "
							+ Lttl.editor.getUndoManager().getRedoDescription());
					return true;
				}
				else
				{
					menuItem.setText("Redo");
					return false;
				}
			}

			@Override
			void action(boolean clickedInMenu)
			{
				Lttl.editor.getUndoManager().redo();
			}
		});

		// Copies selected transforms
		register(new GuiLttlMenuHotkeyItem("Copy", "Edit", new Hotkey(false,
				true, false, Keys.C))
		{
			@Override
			boolean validate()
			{
				return Lttl.editor.getGui().getSelectionController()
						.getSelectedTransformCount() > 0;
			}

			@Override
			void action(boolean clickedInMenu)
			{
				Lttl.editor.getGui().setCopyTransforms(
						Lttl.editor.getGui().getSelectionController()
								.getSelectedTransforms());
			}
		});

		// Moves a transform
		register(new GuiLttlMenuHotkeyItem("Cut/Move", "Edit", new Hotkey(
				false, true, false, Keys.X))
		{
			@Override
			boolean validate()
			{
				return Lttl.editor.getGui().getSelectionController()
						.getSelectedTransformCount() > 0;
			}

			@Override
			void action(boolean clickedInMenu)
			{
				// set as move transforms
				Lttl.editor.getGui().setMoveTransforms(
						Lttl.editor.getGui().getSelectionController()
								.getSelectedTransforms());

				// clear copy transforms so on Ctrl+V it will do the place not the paste
				Lttl.editor.getGui().clearCopyTransforms();
			}
		});

		// Paste or Places a transform (based on if it was copied or cut)
		register(new GuiLttlMenuHotkeyItem("Paste/Place", "Edit", new Hotkey(
				false, true, false, Keys.V))
		{
			@Override
			boolean validate()
			{
				if (Lttl.editor.getGui().getCopyTransforms().size() > 0)
				{
					menuItem.setText("Paste");
					return true;
				}
				else if (Lttl.editor.getGui().getMoveTransforms().size() > 0
						&& Lttl.editor.getGui().getSelectionController()
								.getSelectedTransformCount() == 1)
				{
					menuItem.setText("Place");
					return true;
				}
				else
				{
					menuItem.setText("Paste/Place");
					return false;
				}
			}

			@Override
			void action(boolean clickedInMenu)
			{
				LttlScene selectedScene = Lttl.editor.getGui()
						.getSelectionController().getSelectedScene();
				ArrayList<LttlTransform> selectedTransforms = Lttl.editor
						.getGui().getSelectionController()
						.getSelectedTransforms();

				if (selectedScene != null || selectedTransforms.size() > 0)
				{
					// if there is some copied transforms then paste
					if (Lttl.editor.getGui().getCopyTransformCount() > 0)
					{
						// paste to scene
						if (selectedScene != null)
						{
							for (LttlTransform lt : Lttl.editor.getGui()
									.getCopyTransforms())
							{
								selectedScene.addTransformCopy(lt, true, true);
							}
						}
						else
						{
							// paste to transform(s)
							// check if can process paste based on same scenes
							for (LttlTransform transform : selectedTransforms)
							{
								for (LttlTransform lt : Lttl.editor.getGui()
										.getCopyTransforms())
								{
									if (transform.getScene() != lt.getScene())
									{
										JOptionPane
												.showMessageDialog(
														null,
														"Can't copy "
																+ lt.getName()
																+ " to "
																+ transform
																		.getName()
																+ " in another scene.  Copy the transform(s) to the other scene first.  No changes made.",
														"Copy Failed - Different Scenes",
														JOptionPane.ERROR_MESSAGE);
										return;
									}
								}
							}
							// process paste
							LttlObjectHelper.copyTransformsToTransforms(
									selectedTransforms, Lttl.editor.getGui()
											.getCopyTransforms(), true, true);
						}
					}
					// there is only move transforms, so it's a place not a paste
					else if (Lttl.editor.getGui().getMoveTransformCount() > 0)
					{
						// move to scene
						if (selectedScene != null)
						{
							for (LttlTransform transform : Lttl.editor.getGui()
									.getMoveTransforms())
							{
								if (transform.getScene() == selectedScene)
								{
									// it's already in this scene, just remove any parents
									transform.setParent(null, true);
								}
								else
								{
									// need to move/copy it to this scene
									transform.getScene().moveTransform(
											transform, selectedScene);
								}
							}
						}
						else if (selectedTransforms.size() == 1)
						{
							// move to transform
							LttlTransform target = Lttl.editor.getGui()
									.getSelectionController()
									.getSelectedTransform();
							ArrayList<LttlTransform> movers = Lttl.editor
									.getGui().getMoveTransforms();
							if (movers.contains(target))
							{
								JOptionPane
										.showMessageDialog(
												null,
												"Can't move transform '"
														+ target.getName()
														+ "' to self. No changes made.",
												"Error",
												JOptionPane.ERROR_MESSAGE);
								return;
							}
							for (LttlTransform mover : movers)
							{
								if (!mover.isInSameScene(target))
								{
									JOptionPane
											.showMessageDialog(
													null,
													"Can't move transform '"
															+ mover.getName()
															+ "' to '"
															+ target.getName()
															+ "' becase they're in different scenes.  Copy/Move to scene first. No changes made.",
													"Error",
													JOptionPane.ERROR_MESSAGE);
									return;
								}
							}
							for (LttlTransform mover : movers)
							{
								mover.setParent(target, true);
							}
						}

						// clear the moveTransformIds
						Lttl.editor.getGui().moveTransformIds.clear();
					}
				}
			}
		});

		register(new GuiLttlMenuHotkeyItem("Capture Selected", "Edit",
				new Hotkey(true, true, false, Keys.C))
		{
			@Override
			boolean validate()
			{
				return Lttl.editor.getGui().getSelectionController()
						.getSelectedTransformCount() == 1;
			}

			@Override
			void action(boolean clickedInMenu)
			{
				Lttl.editor.getGui().setCaptureComponent(
						Lttl.editor.getGui().getSelectionController()
								.getSelectedTransform());
			}
		});

		// Deselect
		register(new GuiLttlMenuHotkeyItem("Deselect", "Edit", new Hotkey(
				false, true, false, Keys.D))
		{
			@Override
			boolean validate()
			{
				return Lttl.editor.getGui().getSelectionController()
						.getSelectedTransformCount() > 0;
			}

			@Override
			void action(boolean clickedInMenu)
			{
				Lttl.editor.getGui().getSelectionController().clearSelection();
			}
		});

		/*** VIEW ***/
		// Transform Hierarchy
		register(new GuiLttlMenuHotkeyItem("Transform Hiearchy", "View",
				new Hotkey(false, true, false, Keys.H))
		{
			@Override
			boolean validate()
			{
				menuItem.setText((Lttl.editor.getSettings().showTransformHiearchy ? "Hide"
						: "Show")
						+ " Transform Hiearchy");
				return true;
			}

			@Override
			void action(boolean clickedInMenu)
			{
				Lttl.editor.getSettings().showTransformHiearchy = !Lttl.editor
						.getSettings().showTransformHiearchy;
			}
		});

		// Selection Outline
		register(new GuiLttlMenuHotkeyItem("Selection Outline", "View",
				new Hotkey(false, true, false, Keys.B))
		{
			@Override
			boolean validate()
			{
				menuItem.setText((Lttl.editor.getSettings().showSelectionOutline ? "Hide"
						: "Show")
						+ " Selection Outline");
				return true;
			}

			@Override
			void action(boolean clickedInMenu)
			{
				Lttl.editor.getSettings().showSelectionOutline = !Lttl.editor
						.getSettings().showSelectionOutline;
			}
		});

		// Toggle Play Mouse Position
		register(new GuiLttlMenuHotkeyItem("Mouse Position", "View", null)
		{
			@Override
			boolean validate()
			{
				menuItem.setText((Lttl.editor.getSettings().drawMousePosition ? "Hide"
						: "Show")
						+ " Mouse Position");
				return true;
			}

			@Override
			void action(boolean clickedInMenu)
			{
				Lttl.editor.getSettings().drawMousePosition = !Lttl.editor
						.getSettings().drawMousePosition;
			}
		});

		// Editor Camera Zoom Lock
		register(new GuiLttlMenuHotkeyItem("", "View", new Hotkey(true, true,
				false, Keys.L))
		{
			@Override
			boolean validate()
			{
				menuItem.setText((Lttl.editor.getSettings().lockEditorZoom ? "Unlock"
						: "Lock")
						+ " Editor Camera Zoom");
				return true;
			}

			@Override
			void action(boolean clickedInMenu)
			{
				Lttl.editor.getSettings().lockEditorZoom = !Lttl.editor
						.getSettings().lockEditorZoom;
			}
		});

		// Toggle Grid
		register(new GuiLttlMenuHotkeyItem("Show Grid", "View", new Hotkey(
				true, true, false, Keys.G))
		{
			@Override
			boolean validate()
			{
				menuItem.setText((Lttl.editor.getSettings().enableGrid ? "Hide"
						: "Show") + " Grid");
				return true;
			}

			@Override
			void action(boolean clickedInMenu)
			{
				Lttl.editor.getSettings().enableGrid = !Lttl.editor
						.getSettings().enableGrid;
			}
		});

		// Offset grid with selected transform
		register(new GuiLttlMenuHotkeyItem("Offset Grid with Selection",
				"View", new Hotkey(true, true, true, Keys.G))
		{
			@Override
			boolean validate()
			{
				return Lttl.editor.getGui().getSelectionController()
						.getSelectedTransformCount() == 1;
			}

			@Override
			void action(boolean clickedInMenu)
			{
				Lttl.editor.getSettings().gridOffset.set(Lttl.editor.getGui()
						.getSelectionController().getSelectedTransform()
						.getWorldPosition(true));
			}
		});
		// Rest Grid Offset to (0,0)
		register(new GuiLttlMenuHotkeyItem("Reset Grid Offset", "View", null)
		{
			@Override
			boolean validate()
			{
				return !Lttl.editor.getSettings().gridOffset
						.equals(Vector2.Zero);
			}

			@Override
			void action(boolean clickedInMenu)
			{
				Lttl.editor.getSettings().gridOffset.set(0, 0);
			}
		});

		/*** HANDLES ***/
		// Position Handles
		register(new GuiLttlMenuHotkeyItem("Enable Position Handles",
				"Handles", new Hotkey(false, false, false, Keys.NUM_1))
		{
			@Override
			void action(boolean clickedInMenu)
			{
				Lttl.editor.getGui().getStatusBarController().handlePosButton
						.doClick();
			}
		});

		// Scale Handles
		register(new GuiLttlMenuHotkeyItem("Enable Scale Handles", "Handles",
				new Hotkey(false, false, false, Keys.NUM_2))
		{
			@Override
			void action(boolean clickedInMenu)
			{
				Lttl.editor.getGui().getStatusBarController().handleSclButton
						.doClick();
			}
		});

		// Rotation Handles
		register(new GuiLttlMenuHotkeyItem("Enable Rotation Handles",
				"Handles", new Hotkey(false, false, false, Keys.NUM_3))
		{
			@Override
			void action(boolean clickedInMenu)
			{
				Lttl.editor.getGui().getStatusBarController().handleRotButton
						.doClick();
			}
		});

		// Camera Handles
		register(new GuiLttlMenuHotkeyItem("Camera Handles", "Handles", null)
		{
			@Override
			boolean validate()
			{
				menuItem.setText((Lttl.editor.getSettings().cameraHandles ? "Disable"
						: "Enable")
						+ " Camera Handles");
				return true;
			}

			@Override
			void action(boolean clickedInMenu)
			{
				Lttl.editor.getSettings().cameraHandles = !Lttl.editor
						.getSettings().cameraHandles;
			}
		});

		// Toggle Handles
		register(new GuiLttlMenuHotkeyItem("Handles", "Handles", new Hotkey(
				true, true, false, Keys.H))
		{
			@Override
			boolean validate()
			{
				menuItem.setText((Lttl.editor.getSettings().enableHandles ? "Disable"
						: "Enable")
						+ " Handles");
				return true;
			}

			@Override
			void action(boolean clickedInMenu)
			{
				Lttl.editor.getSettings().enableHandles = !Lttl.editor
						.getSettings().enableHandles;
			}
		});

		/*** OBJECTS ***/
		// FIND TRANSFORM
		register(new GuiLttlMenuHotkeyItem("Find Transform", "Objects",
				new Hotkey(true, true, false, Keys.F))
		{
			@Override
			void action(boolean clickedInMenu)
			{
				findComponentDialog(false, CameraAction.Nothing, true, "Find",
						LttlTransform.class, null);
			}
		});

		// FIND and LOOK TRANSFORM
		register(new GuiLttlMenuHotkeyItem("Find + Look Transform", "Objects",
				new Hotkey(false, true, false, Keys.F))
		{
			@Override
			void action(boolean clickedInMenu)
			{
				findComponentDialog(true, CameraAction.LookAt, true,
						"Find and Look", LttlTransform.class, null);
			}
		});

		// Go To Transform
		register(new GuiLttlMenuHotkeyItem("Go To Transform", "Objects",
				new Hotkey(false, true, false, Keys.G))
		{
			@Override
			void action(boolean clickedInMenu)
			{
				findComponentDialog(true, CameraAction.GoTo, false, "Go To",
						LttlTransform.class, null);
			}
		});

		// Look At Transform
		register(new GuiLttlMenuHotkeyItem("Look At Transform", "Objects",
				new Hotkey(false, true, false, Keys.L))
		{
			@Override
			void action(boolean clickedInMenu)
			{
				findComponentDialog(true, CameraAction.LookAt, false,
						"Look At", LttlTransform.class, null);
			}
		});

		// Go To Transform (selection)
		register(new GuiLttlMenuHotkeyItem("Go To Selected Transform(s)",
				"Objects", new Hotkey(true, true, false, Keys.G))
		{
			@Override
			boolean validate()
			{
				return Lttl.editor.getGui().getSelectionController()
						.getSelectedTransformCount() > 0;
			}

			@Override
			void action(boolean clickedInMenu)
			{
				Lttl.editor.getCamera().goTo(
						Lttl.editor.getGui().getSelectionController()
								.getSelectedTransforms(), EaseType.QuadOut, 1f);
			}
		});

		// Look At Transform (selection)
		register(new GuiLttlMenuHotkeyItem("Look At Selected Transform(s)",
				"Objects", new Hotkey(true, true, false, Keys.L))
		{
			@Override
			boolean validate()
			{
				return Lttl.editor.getGui().getSelectionController()
						.getSelectedTransformCount() > 0;
			}

			@Override
			void action(boolean clickedInMenu)
			{
				Lttl.editor.getCamera().lookAt(
						Lttl.editor.getGui().getSelectionController()
								.getSelectedTransforms(), EaseType.QuadOut, 1f);
			}
		});

		// Previous Transform
		register(new GuiLttlMenuHotkeyItem("Previous Transform", "Objects",
				new Hotkey(false, true, false, Keys.COMMA))
		{
			@Override
			boolean validate()
			{
				return Lttl.editor.getGui().getStatusBarController().transformPrev
						.isEnabled();
			}

			@Override
			void action(boolean clickedInMenu)
			{
				Lttl.editor.getGui().getStatusBarController().transformPrev
						.doClick();
			}
		});

		// Forward(next) Transform
		register(new GuiLttlMenuHotkeyItem("Next Transform", "Objects",
				new Hotkey(false, true, false, Keys.PERIOD))
		{
			@Override
			boolean validate()
			{
				return Lttl.editor.getGui().getStatusBarController().transformNext
						.isEnabled();
			}

			@Override
			void action(boolean clickedInMenu)
			{
				Lttl.editor.getGui().getStatusBarController().transformNext
						.doClick();
			}
		});

		// Find Component
		register(new GuiLttlMenuHotkeyItem("Find Component", "Objects", null)
		{
			@Override
			void action(boolean clickedInMenu)
			{
				findComponentDialog();
			}
		});

		// Add Transform to World
		register(new GuiLttlMenuHotkeyItem("Add Transform To World", "Objects",
				new Hotkey(true, true, false, Keys.T))
		{
			@Override
			void action(boolean clickedInMenu)
			{
				Lttl.editor.getGui().addTransformDialog(null,
						Lttl.scenes.getWorld());
			}
		});

		// Add Transform to Selections
		register(new GuiLttlMenuHotkeyItem("Add Transform To Selection",
				"Objects", new Hotkey(false, true, false, Keys.T))
		{
			@Override
			boolean validate()
			{
				return Lttl.editor.getGui().getSelectionController()
						.getSelectedTransformCount() > 0
						|| Lttl.editor.getGui().getSelectionController()
								.isSceneSelected();
			}

			@Override
			void action(boolean clickedInMenu)
			{
				if (Lttl.editor.getGui().getSelectionController()
						.isSceneSelected())
				{
					Lttl.editor.getGui().addTransformDialog(
							null,
							Lttl.editor.getGui().getSelectionController()
									.getSelectedScene());
				}
				else
				{
					Lttl.editor.getGui().addTransformDialog(
							Lttl.editor.getGui().getSelectionController()
									.getSelectedTransforms(), null);
				}
			}
		});

		// Add Component
		register(new GuiLttlMenuHotkeyItem("Add Component to Selection",
				"Objects", new Hotkey(false, true, false, Keys.Y))
		{
			@Override
			boolean validate()
			{
				return Lttl.editor.getGui().getSelectionController()
						.getSelectedTransformCount() > 0;
			}

			@Override
			void action(boolean clickedInMenu)
			{
				addComponentDialog(Lttl.editor.getGui()
						.getSelectionController().getSelectedTransforms());
			}
		});

		// Add Component
		register(new GuiLttlMenuHotkeyItem("Duplicate Selection", "Objects",
				new Hotkey(true, true, false, Keys.D))
		{
			@Override
			boolean validate()
			{
				return Lttl.editor.getGui().getSelectionController()
						.getSelectedTransformCount() > 0;
			}

			@Override
			void action(boolean clickedInMenu)
			{
				ArrayList<LttlTransform> newSelection = new ArrayList<LttlTransform>();
				// create duplicates
				for (LttlTransform lt : Lttl.editor.getGui()
						.getSelectionController().getSelectedTransforms())
				{
					LttlTransform duplicate = lt.duplicate(true);
					newSelection.add(duplicate);
				}

				// set selection to the duplicates
				Lttl.editor.getGui().getSelectionController()
						.setSelection(newSelection);
			}
		});

		// Delete Transforms
		register(new GuiLttlMenuHotkeyItem("Delete Transforms", "Objects",
				new Hotkey(false, false, false, Keys.FORWARD_DEL))
		{
			@Override
			boolean validate()
			{
				return Lttl.editor.getGui().getSelectionController()
						.getSelectedTransformCount() > 0;
			}

			@Override
			void action(boolean clickedInMenu)
			{
				Lttl.editor.getGui().deleteTransformsDialog(
						Lttl.editor.getGui().getSelectionController()
								.getSelectedTransforms());
			}
		});

		/*** Modes ***/
		// Play/Stop
		register(new GuiLttlMenuHotkeyItem("Play/Stop", "Modes", new Hotkey(
				false, true, false, Keys.O))
		{
			@Override
			boolean validate()
			{
				menuItem.setText(Lttl.game.isPlaying() ? "Stop" : "Play");
				return true;
			}

			@Override
			void action(boolean clickedInMenu)
			{
				Lttl.editor.getGui().getStatusBarController().playStopButton
						.doClick();
			}
		});

		// Pause/Unpause
		register(new GuiLttlMenuHotkeyItem("Pause", "Modes", new Hotkey(false,
				true, false, Keys.P))
		{
			@Override
			boolean validate()
			{
				if (!Lttl.game.isPlaying())
				{
					menuItem.setText("Pause");
					return false;
				}
				menuItem.setText(Lttl.game.isPaused() ? "Unpause" : "Pause");
				return true;
			}

			@Override
			void action(boolean clickedInMenu)
			{
				Lttl.editor.getGui().getStatusBarController().pauseButton
						.doClick();
			}
		});

		// Step
		register(new GuiLttlMenuHotkeyItem("Step", "Modes", new Hotkey(false,
				true, false, Keys.LEFT_BRACKET))
		{
			@Override
			boolean validate()
			{
				return Lttl.game.isPlaying();
			}

			@Override
			void action(boolean clickedInMenu)
			{
				Lttl.editor.getGui().getStatusBarController().stepButton
						.doClick();
			}
		});

		/*** Scenes ***/
		// Load Scene
		register(new GuiLttlMenuHotkeyItem("Load", "Scenes", null)
		{
			@Override
			boolean validate()
			{
				return !Lttl.scenes.areAllScenesLoaded();
			}

			@Override
			void action(boolean clickedInMenu)
			{
				Lttl.editor.getGui().loadSceneDialog();
			}
		});

		// Create Scene
		register(new GuiLttlMenuHotkeyItem("Create", "Scenes", null)
		{
			@Override
			boolean validate()
			{
				return !Lttl.game.isPlaying();
			}

			@Override
			void action(boolean clickedInMenu)
			{
				Lttl.editor.getGui().createSceneDialog();
			}
		});

		// Delete Scene
		register(new GuiLttlMenuHotkeyItem("Delete", "Scenes", null)
		{
			@Override
			boolean validate()
			{
				return !Lttl.game.isPlaying()
						&& Lttl.scenes.getSceneCount() > 0;
			}

			@Override
			void action(boolean clickedInMenu)
			{
				Lttl.editor.getGui().deleteSceneDialog();
			}
		});

		// Reload Textures Scene
		register(new GuiLttlMenuHotkeyItem("Refresh Textures", "Scenes", null)
		{
			@Override
			boolean validate()
			{
				return true;
			}

			@Override
			void action(boolean clickedInMenu)
			{
				for (LttlScene scene : Lttl.scenes.getAllLoaded(true))
				{
					scene.getTextureManager()
							.loadAndBuildTextures(false, false);
					LttlTextureManager.refreshAllReferences();
				}
			}
		});

		/*** HIDDEN COMMANDS ***/

		// save and process camera state (function keys)
		int[] fKeys = new int[]
		{ Keys.F1, Keys.F2, Keys.F3, Keys.F4, Keys.F5, Keys.F6, Keys.F7,
				Keys.F8, Keys.F9, Keys.F10, Keys.F11, Keys.F12 };
		for (int i = 0; i < fKeys.length; i++)
		{
			int key = fKeys[i];
			final int fi = i;

			// save camera state
			register(new GuiLttlMenuHotkeyItem(null, null, new Hotkey(false,
					true, false, key))
			{
				@Override
				boolean validate()
				{
					return true;
				}

				@Override
				void action(boolean clickedInMenu)
				{
					Lttl.editor.getSettings().savedEditorCameraStates.set(fi,
							Lttl.editor.getCamera().getTransformState());
				}
			});
			// tween to camera state
			register(new GuiLttlMenuHotkeyItem(null, null, new Hotkey(false,
					false, false, key))
			{
				@Override
				boolean validate()
				{
					return Lttl.editor.getSettings().savedEditorCameraStates
							.get(fi) != null;
				}

				@Override
				void action(boolean clickedInMenu)
				{
					LttlCamera c = Lttl.editor.getCamera();
					LttlCameraTransformState state = Lttl.editor.getSettings().savedEditorCameraStates
							.get(fi);
					Lttl.tween
							.createParallel(null)
							.push(c.tweenPosTo(state.position, 1).setEase(
									EaseType.QuadOut))
							.push(c.tweenZoomTo(state.zoom, 1).setEase(
									EaseType.QuadOut)).start();
				}
			});
		}

	}

	/**
	 * Removes a menu item from top menu bar.<br>
	 * Note: calling build() will show changes in menu bar
	 * 
	 * @param menuItem
	 */
	public void unregister(GuiLttlMenuHotkeyItem menuItem)
	{
		items.remove(menuItem);
	}

	/**
	 * Registers a menu item to the to menu bar. It will stay there until you unregister it via a reference to the
	 * GuiLttlMenuItem.<br>
	 * Note: This will overwrite any other menu item with same hotkey or name<br>
	 * Note: calling build() will show changes in menu bar
	 * 
	 * @param menuItem
	 */
	public void register(GuiLttlMenuHotkeyItem menuItem)
	{
		// clear any conflicts
		for (Iterator<GuiLttlMenuHotkeyItem> it = items.iterator(); it
				.hasNext();)
		{
			GuiLttlMenuHotkeyItem glmi = it.next();
			if (glmi.name != null
					&& menuItem.name != null
					&& (glmi.name.equals(menuItem.name) && glmi.folderPath
							.equals(menuItem.folderPath))
					|| (glmi.hotkey != null && menuItem.hotkey != null && glmi.hotkey
							.isSame(menuItem.hotkey)))
			{
				it.remove();
			}
		}

		// add to menu list
		items.add(menuItem);
	}

	/**
	 * Finds or creates the menu for the menu item with the given name
	 * 
	 * @param folderPath
	 * @return null if menubar
	 */
	private JMenu getMenu(String folderPath)
	{
		if (folderPath.isEmpty()) { return null; }
		String[] folders = folderPath.split("/");

		JComponent current = menuBar;
		// iterate through all folder names except for last which is the file name
		for (int i = 0; i < folders.length; i++)
		{
			current = getMenu(current, folders[i]);
		}
		return (JMenu) current;
	}

	/**
	 * Finds or creates a menu on the parent menu specified with given name
	 * 
	 * @param parent
	 * @param folderName
	 * @return
	 */
	private JMenu getMenu(JComponent parent, String folderName)
	{
		// get components based on if it's menu or bar
		Component[] comps;
		if (parent instanceof JMenu)
		{
			comps = ((JMenu) parent).getMenuComponents();
		}
		else
		{
			comps = parent.getComponents();
		}

		// iterate through menu's inner menus looking for a menu with name
		for (Component c : comps)
		{
			if (c.getClass() == JMenu.class)
			{
				JMenu m = (JMenu) c;
				// check if name is the same
				if (m.getText().equals(folderName)) { return m; }
			}
		}

		// no menu found, so make it
		JMenu menu = new JMenu(folderName);
		parent.add(menu);
		return menu;
	}

	void build()
	{
		menuBar = new JMenuBar();

		menuBar.setLayout(new GridBagLayout());
		Lttl.editor.getGui().getFrame().setJMenuBar(menuBar);
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.NONE;
		gbc.gridx = GridBagConstraints.RELATIVE;
		gbc.gridwidth = 1;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.weightx = 0;

		// iterate through all menu items
		for (final GuiLttlMenuHotkeyItem mi : items)
		{
			// check if suppose to show in menu bar
			if (mi.name == null)
			{
				continue;
			}

			// create menu item
			final JMenuItem menuItem = new JMenuItem(mi.name);
			menuItem.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					mi.action(false);
				}
			});
			// set menuItem
			mi.menuItem = menuItem;
			if (mi.hotkey != null)
			{
				menuItem.setToolTipText(mi.hotkey.toString());
			}

			// get menu
			JMenu menu = getMenu(mi.folderPath);
			if (menu == null)
			{
				// on menubar
				menuBar.add(menuItem, gbc);

				// validate when enter menubar
				menuBar.addMouseListener(new MouseAdapter()
				{
					@Override
					public void mouseEntered(MouseEvent e)
					{
						menuItem.setEnabled(mi.validate());
					}
				});
			}
			else
			{
				// on a JMenu
				menu.add(menuItem);
				menu.addChangeListener(new ChangeListener()
				{
					@Override
					public void stateChanged(ChangeEvent e)
					{
						menuItem.setEnabled(mi.validate());
					}
				});
			}
		}

		// UNDO STATUS LABEL
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		JPanel undoStatusPanel = new JPanel(new GridBagLayout());
		undoStatusLabel = new JLabel("");
		undoStatusPanel.setOpaque(false);
		undoStatusPanel.add(undoStatusLabel, new GridBagConstraints(0, 0, 1, 1,
				1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets(0, 0, 0, 0), 0, 0));
		menuBar.add(undoStatusPanel, gbc);
	}

	/**** DIALOGS ****/
	void addComponentDialog(LttlTransform transform)
	{
		ArrayList<LttlTransform> list = new ArrayList<LttlTransform>();
		list.add(transform);
		addComponentDialog(list);
	}

	void addComponentDialog(final ArrayList<LttlTransform> transforms)
	{
		// create content pane
		JPanel contentPane = new JPanel(new GridBagLayout());
		contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// create select box
		final GuiLttlComboBox combo = new GuiLttlComboBox(Lttl.editor.getGui()
				.getComponentOptions(), null, true);
		contentPane.add(combo, gbc);

		// // push everything up
		// gbc.weighty = 1;
		// gbc.fill = GridBagConstraints.VERTICAL;
		// contentPane.add(new JPanel(), gbc);

		// create dialog
		final JDialog dialog = GuiHelper.createDialog("Add Component", 300, 70,
				true, false, ModalityType.APPLICATION_MODAL, contentPane);
		dialog.setResizable(false);

		combo.addLttlActionListener(new GuiLttlComboBoxListener()
		{
			@Override
			public void selectionSubmitted(GuiSelectOptionContainer gsoc)
			{
				Class<? extends LttlComponent> compClass = (Class<? extends LttlComponent>) gsoc.value;

				for (LttlTransform lt : transforms)
				{
					if (!lt.canAddComponentType(compClass))
					{
						JOptionPane.showMessageDialog(
								null,
								((Object) compClass.getSimpleName()
										+ " cannot be added to transform '" + lt
											.getName()) + ".  No changes.",
								"Failed To Add Component",
								JOptionPane.ERROR_MESSAGE);
						dialog.dispose();
						return;
					}
				}

				// add component
				for (LttlTransform lt : transforms)
				{
					LttlComponent newComp = lt.addComponentInternal(compClass,
							false);
					ComponentHelper.processCallBack(newComp,
							ComponentCallBackType.onEditorCreate);
					ComponentHelper.processCallBack(newComp,
							ComponentCallBackType.onStart);
				}

				// close dialog
				dialog.dispose();
			}

			@Override
			public void selectionChanged(GuiSelectOptionContainer gsoc)
			{
			}
		});

		dialog.setVisible(true);
	}

	enum CameraAction
	{
		LookAt, GoTo, Nothing;
	}

	void findComponentDialog()
	{
		// create content pane
		final JPanel contentPane = new JPanel(new GridBagLayout());
		contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// create component select box
		// duplicate component options and concat the number of the components in game
		ArrayList<GuiSelectOptionContainer> options = new ArrayList<GuiSelectOptionContainer>(
				Lttl.editor.getGui().getComponentOptions());
		for (GuiSelectOptionContainer g : options)
		{
			int count = Lttl.scenes.findComponentsAllScenes((Class<?>) g.value,
					true).size();
			g.display += " (" + count + ")";
		}
		final GuiLttlComboBox comboComp = new GuiLttlComboBox(options, null,
				true);
		contentPane.add(comboComp, gbc);

		// create transform select box
		final GuiLttlComboBox comboTransformStart = new GuiLttlComboBox(
				new ArrayList<GuiSelectOptionContainer>(), null, true);
		gbc.gridy = 1;
		contentPane.add(comboTransformStart, gbc);

		// create dialog
		final JDialog dialog = GuiHelper.createDialog(
				"Find Transform with Component", 300, 100, true, false,
				ModalityType.APPLICATION_MODAL, contentPane);
		dialog.setResizable(false);

		comboComp.addLttlActionListener(new GuiLttlComboBoxListener()
		{
			GuiLttlComboBox comboTransform = comboTransformStart;

			@Override
			public void selectionSubmitted(GuiSelectOptionContainer gsoc)
			{
				Class<? extends LttlComponent> compClass = (Class<? extends LttlComponent>) gsoc.value;

				// get all transforms with that component
				ArrayList<LttlTransform> transforms = new ArrayList<LttlTransform>();
				for (LttlComponent comp : Lttl.scenes.findComponentsAllScenes(
						compClass, true))
				{
					if (!transforms.contains(comp.t()))
					{
						transforms.add(comp.t());
					}
				}

				// create options
				ArrayList<GuiSelectOptionContainer> options = new ArrayList<GuiSelectOptionContainer>();
				for (LttlTransform t : transforms)
				{
					options.add(new GuiSelectOptionContainer(t.getId(), t
							.toStringTransform()));
				}

				// remove old
				contentPane.remove(comboTransform);
				comboTransform = new GuiLttlComboBox(options, null, true);

				comboTransform
						.addLttlActionListener(new GuiLttlComboBoxListener()
						{
							@Override
							public void selectionSubmitted(
									GuiSelectOptionContainer gsoc)
							{
								LttlTransform transform = (LttlTransform) Lttl.scenes
										.findComponentByIdAllScenes((Integer) gsoc.value);
								Lttl.editor.getGui().getSelectionController()
										.setSelection(transform);

								// close dialog
								dialog.dispose();
							}
						});

				contentPane.add(comboTransform, gbc);

			}
		});

		dialog.setVisible(true);
	}

	/**
	 * Creates a dialog for finding a component of a specific class in all the scenes and returns the result. The result
	 * can be null if exited/escaped.
	 * 
	 * @param live
	 *            will lookAt the component while selecting in list
	 * @param cameraAction
	 *            after submitting, what does camera do
	 * @param autoSelect
	 *            will select it after submission
	 * @param title
	 * @param compClass
	 * @param priorityTransform
	 *            components on this tranform will be at top of list with an asterisk and already selected, optional
	 * @return
	 */
	<T> T findComponentDialog(final boolean live,
			final CameraAction cameraAction, final boolean autoSelect,
			String title, Class<T> compClass, LttlTransform priorityTransform)
	{
		// create content pane
		JPanel contentPane = new JPanel(new GridBagLayout());
		contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = 1;
		gbc.gridx = 0;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		// get all components of this class including subclasses
		ArrayList<GuiSelectOptionContainer> options = new ArrayList<GuiSelectOptionContainer>();
		ArrayList<GuiSelectOptionContainer> priorityOptions = new ArrayList<GuiSelectOptionContainer>();
		for (T c : Lttl.scenes.findComponentsAllScenes(compClass, true))
		{
			LttlComponent comp = (LttlComponent) c;
			GuiSelectOptionContainer g = new GuiSelectOptionContainer(
					comp.getId(), comp.t().toStringTransform());
			if (priorityTransform != null && comp.t() == priorityTransform)
			{
				g.display = "*" + g.display;
				priorityOptions.add(g);
			}
			else
			{
				options.add(g);
			}
		}
		LttlHelper.ArrayListSortAlphaNumeric(options);
		options.addAll(0, priorityOptions);

		// create select box
		final GuiLttlComboBox combo = new GuiLttlComboBox(options, null, false);
		contentPane.add(combo, gbc);

		final LttlMututatableObject selected = new LttlMututatableObject(null);
		final float startZoom = Lttl.editor.getCamera().zoom;

		// create dialog
		final JDialog dialog = GuiHelper.createDialog(
				title + " " + compClass.getSimpleName(), 300, 70, true, true,
				ModalityType.APPLICATION_MODAL, contentPane,
				live ? new LttlCallback()
				{
					Vector2 startCamPos = new Vector2();

					@Override
					public void callback(int id, Object... objects)
					{
						switch (id)
						{
							case 0:
								// save initial camera pos
								startCamPos.set(Lttl.editor.getCamera().position);
								break;
							case 1:
							case 2:
								if (selected.value == null)
								{
									// no selection was submitted, so return to original camera state
									Lttl.editor.getCamera()
											.tweenZoomTo(startZoom, 1).start();
									Lttl.editor.getCamera()
											.tweenPosTo(startCamPos, 1).start();
								}
								break;
							default:
								break;
						}

					}
				}
						: null);
		dialog.setResizable(false);

		final LttlCallback action = new LttlCallback()
		{
			BaseTween<?> timeline = null;
			long uniqueId = -1;

			@Override
			public void callback(int id, Object... objects)
			{
				LttlTransform lt = ((LttlComponent) objects[0]).t();

				if (timeline != null && !timeline.isKilledUnique(uniqueId))
				{
					timeline.kill();
				}

				switch (cameraAction)
				{
					case GoTo:
						timeline = Lttl.editor.getCamera().goTo(lt,
								EaseType.QuadOut, 1f);
						break;
					case Nothing:
						break;
					case LookAt:
						lt.updateWorldValuesTree();
						timeline = Lttl.editor.getCamera().lookAtInternal(
								lt.getSelectionBoundingRectPointsTree(true),
								EaseType.QuadOut, 1f, .4f, startZoom);
						break;

				}

				if (timeline != null)
				{
					uniqueId = timeline.getId();
				}
			}
		};

		combo.addLttlActionListener(new GuiLttlComboBoxListener()
		{
			@Override
			public void selectionSubmitted(GuiSelectOptionContainer gsoc)
			{
				selected.value = (T) Lttl.scenes
						.findComponentByIdAllScenes((Integer) gsoc.value);

				LttlTransform lt = ((LttlComponent) selected.value).t();

				if (autoSelect && selected.value != null)
				{
					// submitted
					Lttl.editor.getGui().getSelectionController()
							.setSelection(lt);
				}
				action.callback(-1, lt);

				// close dialog
				dialog.dispose();
			}

			@Override
			public void selectionChanged(GuiSelectOptionContainer gsoc)
			{
				if (!live) return;

				LttlComponent lt = Lttl.scenes
						.findComponentByIdAllScenes((Integer) gsoc.value);
				action.callback(-1, lt);
			}
		});

		dialog.setVisible(true);
		return (T) selected.value;
	}

	public JLabel getUndoStatusLabel()
	{
		return undoStatusLabel;
	}

	public JMenuBar getMenuBar()
	{
		return menuBar;
	}

	abstract class GuiLttlMenuHotkeyItem
	{
		public String name;
		public String folderPath;
		public Hotkey hotkey;
		public JMenuItem menuItem;
		/**
		 * really just for save hotkey
		 */
		public boolean inPlayModeToo = false;

		/**
		 * Creates a menu item without a hotkey.
		 * 
		 * @param name
		 *            menu item name (required)
		 * @param folderPath
		 *            folder Path for the menu item (ie. Tools/Movement), can leave blank/null
		 */
		public GuiLttlMenuHotkeyItem(String name, String folderPath)
		{
			this(name, folderPath, null);
		}

		public GuiLttlMenuHotkeyItem(Hotkey hotkey)
		{
			this(null, null, hotkey);
		}

		/**
		 * @param name
		 *            menu item name (if null, will not show in menu bar) (can't have same name as other menu item, will
		 *            overwrite it)
		 * @param folderPath
		 *            folder Path for the menu item (ie. Tools/Movement), can leave blank/null
		 * @param hotkey
		 *            (optional can leave null) (hotkey will overwrite any other hotkey/menu item with same hotkey)
		 */
		public GuiLttlMenuHotkeyItem(String name, String folderPath,
				Hotkey hotkey)
		{
			this.name = name;
			this.folderPath = folderPath;
			if (this.folderPath == null)
			{
				this.folderPath = "";
			}
			this.hotkey = hotkey;
		}

		/**
		 * If return true, enabled. You can access the JMenuItem to change text via this.menuItem
		 * 
		 * @return
		 */
		boolean validate()
		{
			return true;
		}

		/**
		 * What will happen if valid/enabled and clicked and/or when the hotkey is pressed
		 * 
		 * @param triggeredFromHotkey
		 *            true if this action was triggered by hotkey, alternatively, could have been triggered by clicking
		 *            menu item
		 * @return
		 */
		abstract void action(boolean triggeredFromHotkey);
	}

	class Hotkey
	{
		public boolean shift;
		public boolean alt;
		public boolean control;
		public int key;

		/**
		 * Creates a hotkey object
		 * 
		 * @param shift
		 * @param control
		 * @param alt
		 * @param key
		 *            the Keys integer, -1 if no key
		 */
		public Hotkey(boolean shift, boolean control, boolean alt, int key)
		{
			this.shift = shift;
			this.control = control;
			this.alt = alt;
			this.key = key;
		}

		boolean isSame(Hotkey other)
		{
			return other.shift == this.shift && other.control == this.control
					&& other.alt == this.alt && other.key == this.key;
		}

		@Override
		public String toString()
		{
			ArrayList<String> strings = new ArrayList<String>();
			if (control) strings.add("Ctrl");
			if (shift) strings.add("Shift");
			if (alt) strings.add("Alt");
			if (key > -1)
			{
				strings.add(Keys.toString(key));
			}
			String result = "";
			for (int i = 0; i < strings.size(); i++)
			{
				result += strings.get(i)
						+ ((i == strings.size() - 1) ? "" : " + ");
			}
			return result;

		}
	}

	/**
	 * Returns the menu items/hotkeys list (need to check to see if each has a hotkey object first)
	 * 
	 * @return
	 */
	public ArrayList<GuiLttlMenuHotkeyItem> getItems()
	{
		return items;
	}
}
