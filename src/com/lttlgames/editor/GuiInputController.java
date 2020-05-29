package com.lttlgames.editor;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.text.JTextComponent;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.IntMap;
import com.lttlgames.editor.GuiMenuBarController.GuiLttlMenuHotkeyItem;
import com.lttlgames.helpers.EaseType;
import com.lttlgames.helpers.LttlCallback;
import com.lttlgames.helpers.LttlHelper;
import com.lttlgames.helpers.LttlMath;

public class GuiInputController
{
	/**
	 * The minimum amount of distance in pixels that's needed before drag is implemented
	 */
	static final private float minDeltaPixelDistanceDrag = 4;
	static private KeyEventDispatcher keyEventDispatcher;
	private boolean isDragging = false;

	private Vector2 mouseDownPositionPixel = new Vector2();
	private Vector2 mouseDownPosition = new Vector2();
	private Vector2 cameraStartPosition = new Vector2();
	private boolean disableRightClickMenu = false;

	private KeyEvent lastKeyEvent = null;

	private ArrayList<GuiLttlInputListener> listeners = new ArrayList<GuiLttlInputListener>();

	/**
	 * If dragging, descibes if axis is locked and how.
	 */
	private LockedAxis lockedAxis = LockedAxis.None;
	private IntMap<Integer> keyMap = new IntMap<Integer>();
	{
		keyMap.put(-1, -1);
		keyMap.put(Keys.NUM_0, KeyEvent.VK_0);
		keyMap.put(Keys.NUM_1, KeyEvent.VK_1);
		keyMap.put(Keys.NUM_2, KeyEvent.VK_2);
		keyMap.put(Keys.NUM_3, KeyEvent.VK_3);
		keyMap.put(Keys.NUM_4, KeyEvent.VK_4);
		keyMap.put(Keys.NUM_5, KeyEvent.VK_5);
		keyMap.put(Keys.NUM_6, KeyEvent.VK_6);
		keyMap.put(Keys.NUM_7, KeyEvent.VK_7);
		keyMap.put(Keys.NUM_8, KeyEvent.VK_8);
		keyMap.put(Keys.NUM_9, KeyEvent.VK_9);
		keyMap.put(Keys.A, KeyEvent.VK_A);
		keyMap.put(Keys.S, KeyEvent.VK_S);
		keyMap.put(Keys.D, KeyEvent.VK_D);
		keyMap.put(Keys.F, KeyEvent.VK_F);
		keyMap.put(Keys.G, KeyEvent.VK_G);
		keyMap.put(Keys.H, KeyEvent.VK_H);
		keyMap.put(Keys.J, KeyEvent.VK_J);
		keyMap.put(Keys.K, KeyEvent.VK_K);
		keyMap.put(Keys.L, KeyEvent.VK_L);
		keyMap.put(Keys.Z, KeyEvent.VK_Z);
		keyMap.put(Keys.X, KeyEvent.VK_X);
		keyMap.put(Keys.C, KeyEvent.VK_C);
		keyMap.put(Keys.V, KeyEvent.VK_V);
		keyMap.put(Keys.B, KeyEvent.VK_B);
		keyMap.put(Keys.N, KeyEvent.VK_N);
		keyMap.put(Keys.M, KeyEvent.VK_M);
		keyMap.put(Keys.Q, KeyEvent.VK_Q);
		keyMap.put(Keys.W, KeyEvent.VK_W);
		keyMap.put(Keys.E, KeyEvent.VK_E);
		keyMap.put(Keys.R, KeyEvent.VK_R);
		keyMap.put(Keys.T, KeyEvent.VK_T);
		keyMap.put(Keys.Y, KeyEvent.VK_Y);
		keyMap.put(Keys.U, KeyEvent.VK_U);
		keyMap.put(Keys.I, KeyEvent.VK_I);
		keyMap.put(Keys.O, KeyEvent.VK_O);
		keyMap.put(Keys.P, KeyEvent.VK_P);
		keyMap.put(Keys.ESCAPE, KeyEvent.VK_ESCAPE);
		keyMap.put(Keys.FORWARD_DEL, KeyEvent.VK_DELETE);
		keyMap.put(Keys.COMMA, KeyEvent.VK_COMMA);
		keyMap.put(Keys.PERIOD, KeyEvent.VK_PERIOD);
		keyMap.put(Keys.LEFT_BRACKET, KeyEvent.VK_BRACELEFT);
		keyMap.put(Keys.RIGHT_BRACKET, KeyEvent.VK_BRACERIGHT);
		keyMap.put(Keys.ENTER, KeyEvent.VK_ENTER);
		keyMap.put(Keys.F1, KeyEvent.VK_F1);
		keyMap.put(Keys.SHIFT_LEFT, KeyEvent.VK_SHIFT);
		keyMap.put(Keys.SPACE, KeyEvent.VK_SPACE);
	}
	private int[] textFieldKeys = new int[]
	{ KeyEvent.VK_A, KeyEvent.VK_C, KeyEvent.VK_V, KeyEvent.VK_X };

	private LttlCallback setVector2FromEditorClickCallback;

	private PressedType pressedType = PressedType.OnNothing;

	enum PressedType
	{
		OnSelectedTransform, OnNonSelectedTransform, OnNothing, ZoomRect
	};

	public GuiInputController()
	{
		// remove if already created a dispatched (coudl have been reset/reload)
		KeyboardFocusManager.getCurrentKeyboardFocusManager()
				.removeKeyEventDispatcher(keyEventDispatcher);

		// create new dispatcher
		keyEventDispatcher = new KeyEventDispatcher()
		{
			@Override
			public boolean dispatchKeyEvent(KeyEvent e)
			{
				lastKeyEvent = e;
				if (e.getID() == KeyEvent.KEY_PRESSED)
				{
					// skip checking hotkeys for text input
					if (JTextComponent.class.isAssignableFrom(e.getSource()
							.getClass())
							&& ((e.isControlDown() && LttlHelper.ArrayContains(
									textFieldKeys, e.getKeyCode())) || !e
									.isControlDown())) { return false; }
					keyPressed(e);
				}
				return false;
			}
		};
		KeyboardFocusManager.getCurrentKeyboardFocusManager()
				.addKeyEventDispatcher(keyEventDispatcher);
	}

	static final private float middleClickPanSpeedDivisor = 10;

	void update()
	{
		// check hotkeys with editor input, if a hotkey was pressed, then don't update editor gui because the hotkey
		// consumed the input
		if (!checkHotkeys())
		{
			updateEditorGui();
		}
	}

	private boolean checkHotkeys()
	{
		for (GuiLttlMenuHotkeyItem mi : Lttl.editor.getGui()
				.getMenuBarController().getItems())
		{
			if (mi.hotkey == null)
			{
				continue;
			}

			if (mi.inPlayModeToo)
			{
				// check hotkey matches
				if (Lttl.input.isKeyPressed(mi.hotkey.key)
						&& Lttl.input.isAlt() == mi.hotkey.alt
						&& Lttl.input.isShift() == mi.hotkey.shift
						&& Lttl.input.isControl() == mi.hotkey.control)
				{
					if (mi.validate())
					{
						mi.action(true);
					}
					return true;
				}
			}

			// check hotkey matches
			if ((Lttl.input.isEditorKeyPressed(mi.hotkey.key)
					&& Lttl.input.isEditorAlt() == mi.hotkey.alt
					&& Lttl.input.isEditorShift() == mi.hotkey.shift && Lttl.input
					.isEditorControl() == mi.hotkey.control)
					|| (mi.inPlayModeToo
							&& Lttl.input.isKeyPressed(mi.hotkey.key)
							&& Lttl.input.isAlt() == mi.hotkey.alt
							&& Lttl.input.isShift() == mi.hotkey.shift && Lttl.input
							.isControl() == mi.hotkey.control))
			{
				if (mi.validate())
				{
					mi.action(true);
				}
				return true;
			}
		}
		return false;
	}

	private void updateEditorGui()
	{
		// Set origin function (hault until origin set via mouse released)
		if (setVector2FromEditorClickCallback != null)
		{
			// draw crosshairs
			Lttl.debug.drawCrosshairs(Lttl.input.getEditorX(),
					Lttl.input.getEditorY(), LttlDebug.RADIUS_LARGE
							* Lttl.debug.eF(),
					Lttl.debug.tmpColor.set(0, 0, 0, .7f));

			// origin point made
			if (Lttl.input.isEditorMouseReleased())
			{
				setVector2FromEditorClickCallback.callback(-1, new Vector2(
						Lttl.input.getEditorMousePos()));
				setVector2FromEditorClickCallback = null;
			}
			Lttl.editor.getHandleController().dontDrawThisFrame();
			return;
		}

		// if dragging then process multi select or position drag or zoom rect
		if (isDragging)
		{
			if (Lttl.input.isEitherMouseDown(0)
					|| Lttl.input.isEitherMouseReleased(0))
			{
				// left mouse position drag
				Lttl.editor.getGui().getSelectionController()
						.processLeftClickDrag(pressedType, mouseDownPosition);
			}
			else if (Lttl.input.isEitherMouseDown(1))
			{
				// right mouse pan camera
				if (lockedAxis == LockedAxis.None || lockedAxis == LockedAxis.X)
				{
					Lttl.editor.getCamera().position.x = cameraStartPosition.x
							+ ((mouseDownPositionPixel.x - Lttl.input
									.getEditorPixelX()) * Lttl.editor
									.getCamera().getUnitsPerPixelXZoomed());
				}
				if (lockedAxis == LockedAxis.None || lockedAxis == LockedAxis.Y)
				{
					Lttl.editor.getCamera().position.y = cameraStartPosition.y
							+ ((mouseDownPositionPixel.y - Lttl.input
									.getEditorPixelY()) * Lttl.editor
									.getCamera().getUnitsPerPixelXZoomed());
				}
			}
			else if (Lttl.input.isEitherMouseDown(2))
			{
				// middle mouse button pan
				if (lockedAxis == LockedAxis.None || lockedAxis == LockedAxis.X)
				{
					Lttl.editor.getCamera().position.x -= (mouseDownPositionPixel.x - Lttl.input
							.getEditorPixelX())
							* Lttl.editor.getCamera().getUnitsPerPixelXZoomed()
							/ middleClickPanSpeedDivisor;
				}
				if (lockedAxis == LockedAxis.None || lockedAxis == LockedAxis.Y)
				{
					Lttl.editor.getCamera().position.y -= (mouseDownPositionPixel.y - Lttl.input
							.getEditorPixelY())
							* Lttl.editor.getCamera().getUnitsPerPixelXZoomed()
							/ middleClickPanSpeedDivisor;
				}
			}

			// check if finished dragging
			if (Lttl.input.isEitherMouseReleasedAny())
			{
				lockedAxis = LockedAxis.None;
				isDragging = false;
				pressedType = PressedType.OnNothing;
				if (Lttl.editor.getGui().getSelectionController().pressedOnNonSelected != null)
				{
					Lttl.editor.getGui().getSelectionController().pressedOnNonSelected = null;
					Lttl.editor.getGui().getSelectionController()
							.valueChanged(null);
				}
			}

			return;
		}

		// Middle mouse button released
		// reset to default zoom (must come after isDragging if statement, because it may be a drag)
		if (Lttl.input.isEditorMouseReleased(2))
		{
			// if control is held, set as default zoom
			if (isControlEV())
			{
				Lttl.editor.getSettings().defaultZoom = Lttl.editor.getCamera().zoom;
			}
			else if (Lttl.editor.getCamera().zoom != Lttl.editor.getSettings().defaultZoom)
			{
				Lttl.editor
						.getCamera()
						.tweenZoomTo(Lttl.editor.getSettings().defaultZoom, .4f)
						.start();
			}
		}

		// update handles
		boolean activeHandles = Lttl.editor.getHandleController().update();

		// if right click, show right click menu
		if (Lttl.input.isEditorMouseReleased(1))
		{
			if (disableRightClickMenu)
			{
				disableRightClickMenu = false;
			}
			else
			{
				Lttl.editor.getGui().getSelectionController()
						.createRightClickMenu();
			}
		}

		// hault if there are active handles
		if (activeHandles) return;

		// left or right pressed (check what state the left press is)
		if (Lttl.input.isEditorMousePressedAny())
		{
			// remove editor menu when left click
			if (Lttl.editor.getGui().getSelectionController().editorPopupMenu != null)
			{
				Lttl.editor.getGui().getSelectionController().editorPopupMenu
						.setVisible(false);
			}
			GuiHelper.hideMenuBar(Lttl.editor.getGui().getMenuBarController()
					.getMenuBar());

			// save to calcualte drag distance minimum
			mouseDownPositionPixel.set(Lttl.input.getEditorPixelX(),
					Lttl.input.getEditorPixelY());
			mouseDownPosition.set(Lttl.input.getEditorX(),
					Lttl.input.getEditorY());

			if (Lttl.input.isEditorMousePressed(0))
			{
				// zoom rect
				if (isControlEV() && isShiftEV())
				{
					pressedType = PressedType.ZoomRect;
				}
				else
				{
					// get current selected transforms
					ArrayList<LttlTransform> selectedTransforms = Lttl.editor
							.getGui().getSelectionController()
							.getSelectedTransforms();

					// single select
					pressedType = PressedType.OnNothing;
					if (selectedTransforms.size() == 1
							&& Lttl.editor
									.getGui()
									.getSelectionController()
									.checkSelectTransformsAtPosition(false,
											null)
									.contains(selectedTransforms.get(0)))
					{
						// check mouse was pressed in a single selection
						pressedType = PressedType.OnSelectedTransform;
					}
					else if (!Lttl.editor.getGui().getSelectionController()
							.isSelectionLocked())
					{
						// check if mouse was pressed on a transform (not selected)
						ArrayList<LttlTransform> clickedTransforms = Lttl.editor
								.getGui().getSelectionController()
								.checkSelectTransformsAtPosition(true, null);
						if (clickedTransforms.size() > 0)
						{
							pressedType = PressedType.OnNonSelectedTransform;
							Lttl.editor.getGui().getSelectionController().pressedOnNonSelected = clickedTransforms
									.get(0);
						}
					}
				}
			}
			else if (Lttl.input.isEditorMousePressed(1))
			{
				cameraStartPosition.set(Lttl.editor.getCamera().position);
			}
		}

		// any mouse button is down, check if it is dragging (but not frame that it whent down) (if dragging, this will
		// only run at beginning)
		if (!Lttl.input.isEditorMousePressedAny()
				&& Lttl.input.isEditorMouseDownAny())
		{
			// check if dragging
			if (mouseDownPositionPixel.dst(Lttl.input.getEditorPixelX(),
					Lttl.input.getEditorPixelY()) >= minDeltaPixelDistanceDrag)
			{
				if (Lttl.editor.getInput().isShiftEV())
				{
					if (LttlMath.abs(Lttl.input.getPixelDeltaX()) >= LttlMath
							.abs(Lttl.input.getPixelDeltaY()))
					{
						lockedAxis = LockedAxis.X;
					}
					else
					{
						lockedAxis = LockedAxis.Y;
					}
				}
				else
				{
					lockedAxis = LockedAxis.None;
				}
				isDragging = true;
				if (Lttl.input.isEditorMouseDown(0))
				{
					Lttl.editor
							.getGui()
							.getSelectionController()
							.processLeftClickDrag(pressedType,
									mouseDownPosition);
				}
				return;
			}
		}

		// left or right released (not from drag)
		if (Lttl.input.isEditorMouseReleased(0)
				|| Lttl.input.isEditorMouseReleased(1))
		{
			Lttl.editor.getGui().getSelectionController().pressedOnNonSelected = null;

			// if left click then check if in mesh
			if (Lttl.input.isEditorMouseReleased(0)
					&& !Lttl.editor.getGui().getSelectionController()
							.isSelectionLocked())
			{
				// check if mouse position was in a mesh
				ArrayList<LttlTransform> clickedTransforms = Lttl.editor
						.getGui().getSelectionController()
						.checkSelectTransformsAtPosition(true, null);

				if (clickedTransforms.size() > 0)
				{
					Lttl.editor.getGui().getSelectionController()
							.selectionWithModifiers(clickedTransforms.get(0));

					// check if double click
					if (Lttl.game.getTime()
							- Lttl.input.getLastMousePressedTime0() < Lttl.input.DoubleClickTime)
					{
						Lttl.editor.getCamera().lookAt(
								clickedTransforms.get(0), EaseType.QuadOut, 1f);
					}
					return;
				}
				else
				{
					// deselect
					Lttl.editor.getGui().getSelectionController()
							.clearSelection();
					return;
				}
			}
		}
	}

	void mouseWheelMoved(int amount)
	{
		if (!Lttl.input.isMouseInEditorViewport()) { return; }

		float zoomSpeed = Lttl.editor.getSettings().zoomSpeed;
		if (isShiftEV())
		{
			// shift zooms 4 times faster
			zoomSpeed *= 4;
		}
		else if (isControlEV() && !isAltEV())
		{
			// control zooms 4 times slower
			zoomSpeed /= 4;
		}

		Vector2 mouseOffset = new Vector2(Lttl.input.getEditorMousePos())
				.sub(Lttl.editor.getCamera().position);

		float zoomMultiplier = (amount < 0) ? 1 + zoomSpeed : 1 - zoomSpeed;

		Lttl.editor.getCamera().zoom *= zoomMultiplier;

		// holding down control and shift zooms camera relative to mouse position
		if (isControlEV() && isAltEV())
		{
			Vector2 newMouseOffset = new Vector2(
					Lttl.input.getEditorMousePosCurrent()).sub(Lttl.editor
					.getCamera().position);
			Lttl.editor.getCamera().position.sub(newMouseOffset
					.sub(mouseOffset));
		}
	}

	/**
	 * Check hotkeys
	 * 
	 * @param e
	 */
	private void keyPressed(KeyEvent e)
	{
		for (GuiLttlMenuHotkeyItem mi : Lttl.editor.getGui()
				.getMenuBarController().getItems())
		{
			if (mi.hotkey == null)
			{
				continue;
			}
			// check hotkey matches
			if (getKeyCodeFromKey(mi.hotkey.key) == e.getKeyCode()
					&& e.isAltDown() == mi.hotkey.alt
					&& e.isShiftDown() == mi.hotkey.shift
					&& e.isControlDown() == mi.hotkey.control)
			{
				if (mi.validate())
				{
					try
					{
						mi.action(true);
					}
					catch (KillLoopException k)
					{

					}
				}
				return;
			}
		}
		processCallbacks(e);
	}

	private void processCallbacks(KeyEvent e)
	{
		for (GuiLttlInputListener listener : listeners)
		{
			if (e.getID() == KeyEvent.KEY_PRESSED)
			{
				listener.onKeyPressed(e);
			}
			else if (e.getID() == KeyEvent.KEY_RELEASED)
			{
				listener.onKeyReleased(e);
			}
		}
	}

	/**
	 * Editor view not gui.
	 * 
	 * @return
	 */
	public boolean isControlEV()
	{
		return Lttl.input.isRawKeyDown(Keys.CONTROL_LEFT)
				|| Lttl.input.isRawKeyDown(Keys.CONTROL_RIGHT);
	}

	/**
	 * Editor view not gui.
	 * 
	 * @return
	 */
	public boolean isAltEV()
	{
		return Lttl.input.isRawKeyDown(Keys.ALT_LEFT)
				|| Lttl.input.isRawKeyDown(Keys.ALT_RIGHT);
	}

	/**
	 * Editor view not gui.
	 * 
	 * @return
	 */
	public boolean isShiftEV()
	{
		return Lttl.input.isRawKeyDown(Keys.SHIFT_LEFT)
				|| Lttl.input.isRawKeyDown(Keys.SHIFT_RIGHT);
	}

	/**
	 * Experimental
	 * 
	 * @return
	 */
	public boolean isShiftSwing()
	{
		return lastKeyEvent != null && lastKeyEvent.isShiftDown();
	}

	/**
	 * Experimental
	 * 
	 * @return
	 */
	public boolean isControlSwing()
	{
		return lastKeyEvent != null && lastKeyEvent.isControlDown();
	}

	/**
	 * Experimental
	 * 
	 * @return
	 */
	public boolean isAltSwing()
	{
		return lastKeyEvent != null && lastKeyEvent.isAltDown();
	}

	/**
	 * The next click in the editor will call the callback given with a World Position Vector2 as the first object.
	 * 
	 * @param callback
	 */
	public void setVector2FromEditorClick(LttlCallback callback)
	{
		setVector2FromEditorClickCallback = callback;
	}

	public LockedAxis getLockedAxis()
	{
		return lockedAxis;
	}

	int getKeyCodeFromKey(int key)
	{
		if (!keyMap.containsKey(key)) { return -2; }
		return keyMap.get(key);
	}

	/**
	 * Disables the right click menu for this frame
	 */
	void disableRightClickMenu()
	{
		disableRightClickMenu = true;
	}

	void addListener(GuiLttlInputListener listener)
	{
		listeners.add(listener);
	}

	void removeListener(GuiLttlInputListener listener)
	{
		listeners.remove(listener);
	}
}
