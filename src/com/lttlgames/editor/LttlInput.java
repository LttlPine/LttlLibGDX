package com.lttlgames.editor;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.IntArray;
import com.lttlgames.helpers.LttlMath;

/**
 * 
 */
public final class LttlInput extends InputAdapter
{
	LttlInput()
	{
		// if in play mode than start focus in play mode
		if (Lttl.game.isPlaying())
		{
			isMouseFocusPlayViewport = true;
			isMouseFocusEditorViewport = false;
		}
		else
		{
			isMouseFocusPlayViewport = false;
			isMouseFocusEditorViewport = true;
		}

		setInputProcessor();
	}

	/* FINAL */
	public final float DoubleClickTime = .5f;

	private Vector2 tmp0 = new Vector2();
	private Vector2 tmpDelta = new Vector2();

	// play mode
	private Vector2 playActiveViewportMousePos = new Vector2();
	private Vector2 playActiveClippedViewportMousePos = new Vector2();
	private Vector2 playActiveViewportMouseDelta = new Vector2();
	private Vector2 playActiveClippedViewportMouseDelta = new Vector2();
	private Vector2 lastActiveViewportMousePos;
	private Vector2 lastActiveClippedViewportMousePos;

	// editor mode
	private Vector2 editorActiveViewportMousePos = new Vector2();
	private Vector2 editorActiveViewportMouseDelta = new Vector2();
	private Vector2 lastEditorActiveViewportMousePos;

	// both modes
	private boolean isMouseDown0 = false;
	private boolean isMouseReleased0 = false;
	private boolean isMousePressed0 = false;
	private boolean isMouseDown1 = false;
	private boolean isMouseReleased1 = false;
	private boolean isMousePressed1 = false;
	private boolean isMouseDown2 = false;
	private boolean isMouseReleased2 = false;
	private boolean isMousePressed2 = false;

	// times
	private float mousePressedTime0 = -1;
	private float mouseReleasedTime0 = -1;
	private float lastMousePressedTime0 = -1;
	private float lastMouseReleasedTime0 = -1;
	private float mousePressedTime1 = -1;
	private float mouseReleasedTime1 = -1;
	private float lastMousePressedTime1 = -1;
	private float lastMouseReleasedTime1 = -1;

	private boolean isMouseInPlayViewport = false;
	private boolean isMouseInPlayClippedViewport = false;
	private boolean isMouseInEditorViewport = false;
	private boolean isMouseFocusPlayViewport = false;
	private boolean isMouseFocusEditorViewport = false;

	private IntArray downLastFrame = new IntArray(false, 16);
	private IntArray downThisFrame = new IntArray(false, 16);
	private IntArray pressedThisFrame = new IntArray(false, 16);
	private IntArray releasedThisFrame = new IntArray(false, 16);

	private IntArray downKeys = new IntArray(false, 16);

	private void setInputProcessor()
	{
		Gdx.input.setInputProcessor(new InputProcessor()
		{
			// REALTIME (actually I think it is just before Gdx's render()) processing of input
			// below is where the input is defined per frame

			// TODO when doing android use these touch methods
			@Override
			public boolean touchUp(int screenX, int screenY, int pointer,
					int button)
			{
				return false;
			}

			@Override
			public boolean touchDragged(int screenX, int screenY, int pointer)
			{
				return false;
			}

			@Override
			public boolean touchDown(int screenX, int screenY, int pointer,
					int button)
			{
				return false;
			}

			@Override
			public boolean scrolled(int amount)
			{
				if (Lttl.game.inEditor())
				{
					Lttl.editor.getInput().mouseWheelMoved(amount);
				}
				return false;
			}

			@Override
			public boolean mouseMoved(int screenX, int screenY)
			{
				return false;
			}

			@Override
			public boolean keyUp(int keycode)
			{
				downKeys.removeValue(keycode);
				return false;
			}

			@Override
			public boolean keyTyped(char character)
			{
				return false;
			}

			@Override
			public boolean keyDown(int keycode)
			{
				if (!downKeys.contains(keycode))
				{
					downKeys.add(keycode);
				}
				return false;
			}
		});
	}

	void update()
	{
		// do include clipping space
		isMouseInPlayViewport = isMouseInCameraViewport(Lttl.game.getCamera(),
				true);
		isMouseInPlayClippedViewport = isMouseInCameraViewport(
				Lttl.game.getCamera(), false);
		isMouseInEditorViewport = (Lttl.game.inEditor()) ? isMouseInCameraViewport(
				Lttl.editor.getCamera(), true) : false;

		// PLAY MOUSE
		if (isMouseActivePlayViewport())
		{
			// POSITION
			// only update positions and deltas if it is active, since if not, then position will either be the last
			// active position or (0,0) if never active

			// get mouse position clipped
			playActiveViewportMousePos.set(getMousePosCurrent(false));

			// get mouse position clipped
			playActiveClippedViewportMousePos.set(getMousePosCurrent(true));

			// DELTAS
			// clipped deltas
			if (lastActiveViewportMousePos != null)
			{
				// skip if null, since no delta if no last active pos
				playActiveViewportMouseDelta.set(playActiveViewportMousePos)
						.sub(lastActiveViewportMousePos);
			}
			else
			{
				lastActiveViewportMousePos = new Vector2();
				playActiveViewportMouseDelta.setZero();
			}
			lastActiveViewportMousePos.set(playActiveViewportMousePos);

			// clipped deltas
			if (lastActiveClippedViewportMousePos != null)
			{
				// skip if null, since no delta if no last active pos
				playActiveClippedViewportMouseDelta.set(
						playActiveClippedViewportMousePos).sub(
						lastActiveClippedViewportMousePos);
			}
			else
			{
				lastActiveClippedViewportMousePos = new Vector2();
				playActiveClippedViewportMouseDelta.setZero();
			}
			lastActiveClippedViewportMousePos
					.set(playActiveClippedViewportMousePos);
		}
		else
		{
			// clear deltas because not active and active mouse positions will remain whatever they were last
			playActiveViewportMouseDelta.setZero();
			playActiveClippedViewportMouseDelta.setZero();
		}

		// EDITOR MOUSE
		if (Lttl.game.inEditor())
		{
			if (isMouseActiveEditorViewport())
			{
				// POSITION
				editorActiveViewportMousePos.set(getEditorMousePosCurrent());

				// DELTAS
				if (lastEditorActiveViewportMousePos != null)
				{
					// skip if null, since no delta if no last active pos
					editorActiveViewportMouseDelta.set(
							editorActiveViewportMousePos).sub(
							lastEditorActiveViewportMousePos);
				}
				else
				{
					lastEditorActiveViewportMousePos = new Vector2();
					editorActiveViewportMouseDelta.setZero();
				}
				lastEditorActiveViewportMousePos
						.set(editorActiveViewportMousePos);
			}
			else
			{
				editorActiveViewportMouseDelta.setZero();
			}
		}

		// MOUSE PRESS STATE
		// reset one time events
		isMousePressed0 = false;
		isMousePressed1 = false;
		isMousePressed2 = false;
		isMouseReleased0 = false;
		isMouseReleased1 = false;
		isMouseReleased2 = false;

		// if not in editor, always assume the focus is in play viewport and don't check when pressed
		if (!Lttl.game.inEditor())
		{
			isMouseFocusPlayViewport = true;
			isMouseFocusEditorViewport = false;
		}

		// check if mouse buttons are pressed
		if (Gdx.input.isButtonPressed(Buttons.LEFT))
		{
			// if the mouse was not pressed, then set one time down
			if (!isMouseDown0)
			{
				isMousePressed0 = true;
				lastMousePressedTime0 = mousePressedTime0;
				mousePressedTime0 = Lttl.game.getRawTime();
				if (Lttl.game.inEditor())
				{
					updateFocusOnClick();
				}
			}
			isMouseDown0 = true;
		}
		else
		{
			// if mouse was down, but now is not
			if (isMouseDown0)
			{
				isMouseReleased0 = true;
				lastMouseReleasedTime0 = mouseReleasedTime0;
				mouseReleasedTime0 = Lttl.game.getRawTime();
			}
			isMouseDown0 = false;
		}

		if (Gdx.input.isButtonPressed(Buttons.RIGHT))
		{
			if (!isMouseDown1)
			{
				isMousePressed1 = true;
				lastMousePressedTime1 = mousePressedTime1;
				mousePressedTime1 = Lttl.game.getRawTime();
				if (Lttl.game.inEditor())
				{
					updateFocusOnClick();
				}
			}
			isMouseDown1 = true;
		}
		else
		{
			// if mouse was down, but now is not
			if (isMouseDown1)
			{
				isMouseReleased1 = true;
				lastMouseReleasedTime1 = mouseReleasedTime1;
				mouseReleasedTime1 = Lttl.game.getRawTime();
			}
			isMouseDown1 = false;
		}

		if (Gdx.input.isButtonPressed(Buttons.MIDDLE))
		{
			if (!isMouseDown2)
			{
				isMousePressed2 = true;
				if (Lttl.game.inEditor())
				{
					updateFocusOnClick();
				}
			}
			isMouseDown2 = true;
		}
		else
		{
			// if mouse was down, but now is not
			if (isMouseDown2)
			{
				isMouseReleased2 = true;
			}
			isMouseDown2 = false;
		}

		// KEYBOARD
		if (Gdx.app.getType() == ApplicationType.Desktop)
		{
			// clear all up and down keys from last frame
			downLastFrame.clear();
			downLastFrame.addAll(downThisFrame);
			downThisFrame.clear();
			pressedThisFrame.clear();
			releasedThisFrame.clear();

			// check for new down keys
			for (int key : downKeys.items)
			{
				if (Gdx.input.isKeyPressed(key))
				{
					if (downLastFrame.contains(key))
					{
						downLastFrame.removeValue(key);
					}
					else
					{
						pressedThisFrame.add(key);
					}
					downThisFrame.add(key);
				}
			}

			// add any keys that were not pressed down this frame (but were pressed down last frame) to the up state
			releasedThisFrame.addAll(downLastFrame);
		}
	}

	private void updateFocusOnClick()
	{
		if (isMouseInPlayViewport())
		{
			isMouseFocusPlayViewport = true;
			isMouseFocusEditorViewport = false;
		}
		else if (isMouseInEditorViewport())
		{
			isMouseFocusPlayViewport = false;
			isMouseFocusEditorViewport = true;
		}
		else
		{
			isMouseFocusPlayViewport = false;
			isMouseFocusEditorViewport = false;
		}
	}

	/**
	 * Constrained to the clipped play viewport.
	 * 
	 * @see #getX(boolean)
	 */
	public float getX()
	{
		return getX(true);
	}

	/**
	 * Constrained to the clipped play viewport.
	 * 
	 * @see #getY(boolean)
	 */
	public float getY()
	{
		return getY(true);
	}

	/**
	 * Returns the active play viewport x position of mouse in game units.
	 * 
	 * @param constrainToClippedArea
	 *            if true, will constrain the mouse position to the clipped play viewport area.
	 * @return
	 */
	public float getX(boolean constrainToClippedArea)
	{
		return (constrainToClippedArea) ? playActiveClippedViewportMousePos.x
				: playActiveViewportMousePos.x;
	}

	/**
	 * Returns the active play viewport y position of mouse in game units.
	 * 
	 * @param constrainToClippedArea
	 *            if true, will constrain the mouse position to the clipped play viewport area.
	 * @return
	 */
	public float getY(boolean constrainToClippedArea)
	{
		return (constrainToClippedArea) ? playActiveClippedViewportMousePos.y
				: playActiveViewportMousePos.y;
	}

	/**
	 * Returns the delta of the mouse in the play viewport. This is not constrained to the clipped play area.<br>
	 * Will be 0 if play viewport is not active.<br>
	 * Returns the change in the y position of mouse in game units since last frame, not relative to camera position,
	 * but relative to the screen position.
	 * 
	 * @return a shared vector2, this should be used right away since the values may change
	 */
	public Vector2 getDelta()
	{
		return tmpDelta.set(playActiveViewportMouseDelta);
	}

	/**
	 * @see #getDelta()
	 */
	public float getDeltaX()
	{
		return playActiveViewportMouseDelta.x;
	}

	/**
	 * @see #getDelta()
	 */
	public float getDeltaY()
	{
		return playActiveViewportMouseDelta.y;
	}

	/**
	 * Returns the mouse delta. This is constrained to the clipped play area.<br>
	 * Will be 0 if play viewport is not active.<br>
	 * Returns the change in the y position of mouse in game units since last frame, not relative to camera position,
	 * but relative to the screen position.
	 * 
	 * @return a shared vector2, this should be used right away since the values may change
	 */
	public Vector2 getDeltaClipped()
	{
		return tmpDelta.set(playActiveClippedViewportMouseDelta);
	}

	/**
	 * @see #getDeltaClipped()
	 */
	public float getDeltaXClipped()
	{
		return playActiveClippedViewportMouseDelta.x;
	}

	/**
	 * @see #getDeltaClipped()
	 */
	public float getDeltaYClipped()
	{
		return playActiveClippedViewportMouseDelta.y;
	}

	/**
	 * Returns if the left mouse button is down (continous).
	 * 
	 * @return
	 */
	public boolean isMouseDown()
	{
		return isMouseDown(0);
	}

	/**
	 * Returns if the mouse button is down (continous).<br>
	 * [0-left, 1-right, 2-left]
	 * 
	 * @return
	 */
	public boolean isMouseDown(int button)
	{
		if (isMouseActivePlayViewport())
		{
			if (button == 0)
			{
				return isMouseDown0;
			}
			else if (button == 1)
			{
				return isMouseDown1;
			}
			else if (button == 2) { return isMouseDown2; }
		}
		return false;
	}

	/**
	 * Returns if the left mouse button is down in the editor (continous).
	 * 
	 * @return
	 */
	public boolean isEditorMouseDown()
	{
		return isEditorMouseDown(0);
	}

	/**
	 * Returns if the mouse button is down in the editor (continous).<br>
	 * [0-left, 1-right, 2-left]
	 * 
	 * @return
	 */
	public boolean isEditorMouseDown(int button)
	{
		if (isMouseActiveEditorViewport())
		{
			if (button == 0)
			{
				return isMouseDown0;
			}
			else if (button == 1)
			{
				return isMouseDown1;
			}
			else if (button == 2) { return isMouseDown2; }
		}
		return false;
	}

	/**
	 * Returns if the mouse button is down (continous) in play or editor. [left = 0, right = 1, middle = 2]
	 * 
	 * @return
	 */
	public boolean isEitherMouseDown(int button)
	{
		if (button == 0)
		{
			return isMouseDown0;
		}
		else if (button == 1)
		{
			return isMouseDown1;
		}
		else if (button == 2) { return isMouseDown2; }
		return false;
	}

	/**
	 * Returns if the left mouse button was released this frame (one time).
	 * 
	 * @param button
	 * @return
	 */
	public boolean isMouseReleased()
	{
		return isMouseReleased(0);
	}

	/**
	 * Returns if this mouse button was released this frame (one time).<br>
	 * [0-left, 1-right, 2-left]
	 * 
	 * @param button
	 * @return
	 */
	public boolean isMouseReleased(int button)
	{
		if (isMouseFocusPlayViewport())
		{
			if (button == 0)
			{
				return isMouseReleased0;
			}
			else if (button == 1)
			{
				return isMouseReleased1;
			}
			else if (button == 2) { return isMouseReleased2; }
		}
		return false;
	}

	/**
	 * Returns if the left mouse button was released this frame in the editor viewport (one time).
	 * 
	 * @return
	 */
	public boolean isEditorMouseReleased()
	{
		return isEditorMouseReleased(0);
	}

	/**
	 * Returns if this mouse button was released this frame in the editor viewport (one time).<br>
	 * [0-left, 1-right, 2-left]
	 * 
	 * @param button
	 * @return
	 */
	public boolean isEditorMouseReleased(int button)
	{
		if (isMouseFocusEditorViewport())
		{
			if (button == 0)
			{
				return isMouseReleased0;
			}
			else if (button == 1)
			{
				return isMouseReleased1;
			}
			else if (button == 2) { return isMouseReleased2; }
		}
		return false;
	}

	/**
	 * Returns if the mouse button is released in play or editor. [left = 0, right = 1, middle = 2]
	 * 
	 * @return
	 */
	public boolean isEitherMouseReleased(int button)
	{
		if (button == 0)
		{
			return isMouseReleased0;
		}
		else if (button == 1)
		{
			return isMouseReleased1;
		}
		else if (button == 2) { return isMouseReleased2; }
		return false;
	}

	/**
	 * Returns if the left mouse button was pressed this frame (one time).
	 * 
	 * @param button
	 * @return
	 */
	public boolean isMousePressed()
	{
		return isMousePressed(0);
	}

	/**
	 * Returns if this mouse button was pressed this frame (one time).<br>
	 * [0-left, 1-right, 2-left]
	 * 
	 * @param button
	 * @return
	 */
	public boolean isMousePressed(int button)
	{
		if (isMouseInPlayViewport())
		{
			if (button == 0)
			{
				return isMousePressed0;
			}
			else if (button == 1)
			{
				return isMousePressed1;
			}
			else if (button == 2) { return isMousePressed2; }
		}
		return false;
	}

	/**
	 * Returns if the left mouse button was pressed down this frame in the editor viewport (one time).
	 * 
	 * @return
	 */
	public boolean isEditorMousePressed()
	{
		return isEditorMousePressed(0);
	}

	/**
	 * Returns if this mouse button was pressed down this frame in the editor viewport (one time).<br>
	 * [0-left, 1-right, 2-left]
	 * 
	 * @param button
	 * @return
	 */
	public boolean isEditorMousePressed(int button)
	{
		if (isMouseInEditorViewport())
		{
			if (button == 0)
			{
				return isMousePressed0;
			}
			else if (button == 1)
			{
				return isMousePressed1;
			}
			else if (button == 2) { return isMousePressed2; }
		}
		return false;
	}

	/**
	 * Returns if the mouse button is pressed (down this frame) in play or editor.<br>
	 * [0-left, 1-right, 2-left]
	 * 
	 * @return
	 */
	public boolean isEitherMousePressed(int button)
	{
		if (button == 0)
		{
			return isMousePressed0;
		}
		else if (button == 1)
		{
			return isMousePressed1;
		}
		else if (button == 2) { return isMousePressed2; }
		return false;
	}

	/**
	 * Returns the active editor viewport x position of mouse in game units.
	 * 
	 * @return
	 */
	public float getEditorX()
	{
		return editorActiveViewportMousePos.x;
	}

	/**
	 * Returns the active editor viewport y position of mouse in game units.
	 * 
	 * @return
	 */
	public float getEditorY()
	{
		return editorActiveViewportMousePos.y;
	}

	/**
	 * Returns the change in the mouse position in game units since last frame when in the editor viewport, not relative
	 * to camera position, but relative to the screen position.<br>
	 * Will be 0,0 if editor viewport is not active.
	 * 
	 * @return a shared vector2, this should be used right away since the values may change
	 */
	public Vector2 getEditorDelta()
	{
		return tmpDelta.set(editorActiveViewportMouseDelta);
	}

	/**
	 * @see #getEditorDelta()
	 */
	public float getEditorDeltaX()
	{
		return editorActiveViewportMouseDelta.x;
	}

	/**
	 * @see #getEditorDelta()
	 */
	public float getEditorDeltaY()
	{
		return editorActiveViewportMouseDelta.y;
	}

	private boolean isMouseInCameraViewport(LttlCamera camera,
			boolean shouldIncludeClippingArea)
	{
		// check x
		if (Gdx.input.getX() < ((shouldIncludeClippingArea) ? 0 : camera
				.getClippingPixelWidth())
				|| Gdx.input.getX() > ((shouldIncludeClippingArea) ? camera
						.getRawPixelWidth() : camera.getClippingPixelWidth()
						+ camera.getViewportPixelWidthStatic()))
		{
			return false;
		}
		// check y
		else
		{
			// in editor, requires taking into consideration that the viewports are both in the gdx screen
			if (Lttl.game.inEditor())
			{
				// play camera
				if (camera.isPlayCamera())
				{
					if (Gdx.input.getY() < Lttl.editor.getCamera()
							.getRawPixelHeight()
							+ ((shouldIncludeClippingArea) ? 0 : camera
									.getClippingPixelHeight())
							|| Gdx.input.getY() > Lttl.editor.getCamera()
									.getRawPixelHeight()
									+ ((shouldIncludeClippingArea) ? camera
											.getRawPixelHeight()
											: camera.getClippingPixelHeight()
													+ camera.getViewportPixelHeightStatic())) { return false; }
				}
				// editor camera, no clipping
				else if (Gdx.input.getY() < 0
						|| Gdx.input.getY() > camera.getRawPixelHeight()) { return false; }
			}
			// not in editor, can assume it's always play camera and no editor camera
			else if (Gdx.input.getY() < ((shouldIncludeClippingArea) ? 0
					: camera.getClippingPixelHeight())
					|| Gdx.input.getY() > ((shouldIncludeClippingArea) ? camera
							.getRawPixelHeight() : camera
							.getClippingPixelHeight()
							+ camera.getViewportPixelHeightStatic())) { return false; }
		}
		// must be contained in viewport
		return true;
	}

	/**
	 * Returns if the current mouse position is inside the playing viewport (including clipping). Will be false if out
	 * of application window.
	 * 
	 * @return
	 */
	public boolean isMouseInPlayViewport()
	{
		return isMouseInPlayViewport;
	}

	/**
	 * Returns if the current mouse position is inside the playing viewport (excluding clipping). Will be false if out
	 * of application window.
	 * 
	 * @return
	 */
	public boolean isMouseInPlayClippedViewport()
	{
		return isMouseInPlayClippedViewport;
	}

	/**
	 * Returns if the current mouse position is inside the editor viewport. Will be false if out of application window.
	 * 
	 * @return
	 */
	public boolean isMouseInEditorViewport()
	{
		return isMouseInEditorViewport;
	}

	/**
	 * Returns if last mouse press was in the play viewport, mostly used when polling for key events.
	 * 
	 * @return
	 */
	public boolean isMouseFocusPlayViewport()
	{
		return isMouseFocusPlayViewport;
	}

	/**
	 * Returns if last mouse press was in the editor viewport, mostly used when polling for key events.
	 * 
	 * @return
	 */
	public boolean isMouseFocusEditorViewport()
	{
		return isMouseFocusEditorViewport;
	}

	/**
	 * Returns if mouse is down(any) and last mouse press was in the editor viewport or if not dragging then is it in
	 * the editor viewport Should the mouse be considered active in editor viewport?<br>
	 * Returns if mouse is down(any) and if last mouse press was in the editor viewport (could be in play viewport or
	 * outside application) or if not dragging then is it in the editor viewport last, actual mouse could be outside
	 * application, but GDX doesn't have the position since not dragging.<br>
	 * <br>
	 * If mouse is let go while dragging outside application, then no viewport will be active.
	 * 
	 * @return
	 */
	public boolean isMouseActiveEditorViewport()
	{
		if (isEitherMouseDownAny())
		{
			return isMouseFocusEditorViewport;
		}
		else
		{
			return isMouseInEditorViewport;
		}
	}

	/**
	 * Should the mouse be considered active in play viewport?<br>
	 * Returns if mouse is down(any) and if last mouse press was in the play viewport (could be in editor viewport or
	 * outside application) or if not dragging then is it in the play viewport last (includes clipping area), actual
	 * mouse could be outside application, but GDX doesn't have the position since not dragging.<br>
	 * If mouse is let go while dragging outside application, then no viewport will be active.<br>
	 * If need to know if it's not in clipping area, then do {@link #isMouseInPlayClippedViewport()}.
	 * 
	 * @return
	 */
	public boolean isMouseActivePlayViewport()
	{
		if (isEitherMouseDownAny())
		{
			return isMouseFocusPlayViewport();
		}
		else
		{
			return isMouseInPlayViewport();
		}
	}

	/**
	 * Returns if this key was pressed down this frame.
	 * 
	 * @param key
	 * @return
	 */
	public boolean isKeyPressed(int key)
	{
		if (isMouseFocusPlayViewport() && pressedThisFrame.size > 0)
		{
			if (key == Keys.ANY_KEY) return true;
			return pressedThisFrame.contains(key);
		}
		else
		{
			return false;
		}
	}

	/**
	 * Returns if any of the keys were pressed down this frame.
	 * 
	 * @param keys
	 * @return
	 */
	public boolean isAnyKeyPressed(int... keys)
	{
		for (int i = 0; i < keys.length; i++)
		{
			if (isKeyPressed(keys[i])) { return true; }
		}
		return false;
	}

	/**
	 * Returns if this key was pressed down this frame in editor.
	 * 
	 * @param key
	 * @return
	 */
	public boolean isEditorKeyPressed(int key)
	{
		if (isMouseFocusEditorViewport() && pressedThisFrame.size > 0)
		{
			if (key == Keys.ANY_KEY) return true;
			return pressedThisFrame.contains(key);
		}
		else
		{
			return false;
		}
	}

	/**
	 * Returns if any of the keys were pressed down this frame in the editor.
	 * 
	 * @param keys
	 * @return
	 */
	public boolean isEditorAnyKeyPressed(int... keys)
	{
		for (int i = 0; i < keys.length; i++)
		{
			if (isEditorKeyPressed(keys[i])) { return true; }
		}
		return false;
	}

	/**
	 * Returns if this key was released this frame.
	 * 
	 * @param key
	 * @return
	 */
	public boolean isKeyReleased(int key)
	{
		if (isMouseFocusPlayViewport() && releasedThisFrame.size > 0)
		{
			if (key == Keys.ANY_KEY) return true;
			return releasedThisFrame.contains(key);
		}
		else
		{
			return false;
		}
	}

	/**
	 * Returns if any of the keys were released this frame.
	 * 
	 * @param keys
	 * @return
	 */
	public boolean isAnyKeyReleased(int... keys)
	{
		for (int i = 0; i < keys.length; i++)
		{
			if (isKeyReleased(keys[i])) { return true; }
		}
		return false;
	}

	/**
	 * Returns if this key was pressed down this frame in editor.
	 * 
	 * @param key
	 * @return
	 */
	public boolean isEditorKeyReleased(int key)
	{
		if (isMouseFocusEditorViewport() && releasedThisFrame.size > 0)
		{
			if (key == Keys.ANY_KEY) return true;
			return releasedThisFrame.contains(key);
		}
		else
		{
			return false;
		}
	}

	/**
	 * Returns if any of the keys were released this frame in the editor.
	 * 
	 * @param keys
	 * @return
	 */
	public boolean isEditorAnyKeyReleased(int... keys)
	{
		for (int i = 0; i < keys.length; i++)
		{
			if (isEditorKeyReleased(keys[i])) { return true; }
		}
		return false;
	}

	/**
	 * Returns if this key is being pressed down.
	 * 
	 * @param key
	 * @return
	 */
	public boolean isKeyDown(int key)
	{
		if (isMouseFocusPlayViewport() && downThisFrame.size > 0)
		{
			if (key == Keys.ANY_KEY) return true;
			return downThisFrame.contains(key);
		}
		else
		{
			return false;
		}
	}

	/**
	 * Returns if any of the keys are down this frame.
	 * 
	 * @param keys
	 * @return
	 */
	public boolean isAnyKeyDown(int... keys)
	{
		for (int i = 0; i < keys.length; i++)
		{
			if (isKeyDown(keys[i])) { return true; }
		}
		return false;
	}

	/**
	 * Returns if this key is being pressed down in the editor.
	 * 
	 * @param key
	 * @return
	 */
	public boolean isEditorKeyDown(int key)
	{
		if (isMouseFocusEditorViewport() && downThisFrame.size > 0)
		{
			if (key == Keys.ANY_KEY) return true;
			return downThisFrame.contains(key);
		}
		else
		{
			return false;
		}
	}

	public boolean isRawKeyDown(int key)
	{
		return Gdx.input.isKeyPressed(key);
	}

	/**
	 * Returns if any of the keys are down this frame in the editor.
	 * 
	 * @param keys
	 * @return
	 */
	public boolean isEditorAnyKeyDown(int... keys)
	{
		for (int i = 0; i < keys.length; i++)
		{
			if (isEditorKeyDown(keys[i])) { return true; }
		}
		return false;
	}

	/**
	 * This is the the active mouse position in the editor viewport based on the camera's transformation that started
	 * this frame.
	 * 
	 * @return a shared vector2, this should be used right away since the values may change
	 */
	public Vector2 getEditorMousePos()
	{
		return tmp0.set(getEditorX(), getEditorY());
	}

	/**
	 * Gets the current editor mouse position with camera's current transform changes (position, zoom, etc)<br>
	 * Note: This does not check if the mouse is in editor viewort or active in editor viewport use other methods to
	 * check that first.
	 * 
	 * @return a shared vector2, this should be used right away since the values may change.
	 */
	public Vector2 getEditorMousePosCurrent()
	{
		Lttl.editor.getCamera().unProjectPoint(Gdx.input.getX(),
				Gdx.input.getY(), tmp0);
		return tmp0;
	}

	/**
	 * This will be constrained to the clipped viewport area.
	 * 
	 * @see LttlInput#getMousePos(boolean)
	 */
	public Vector2 getMousePos()
	{
		return getMousePos(true);
	}

	/**
	 * This is the the active mouse position in the play viewport based on the camera's transformation that started this
	 * frame.
	 * 
	 * @param constrainToClippedArea
	 *            if true, will constrain the mouse position to the clipped viewport area
	 * @return a shared vector2, this should be used right away since the values may change
	 */
	public Vector2 getMousePos(boolean constrainToClippedArea)
	{
		return tmp0.set(getX(constrainToClippedArea),
				getY(constrainToClippedArea));
	}

	/**
	 * Gets the current play mouse position with camera's current transform changes (position, zoom, etc)<br>
	 * Note: This does not check if the mouse is in play viewort or active in play viewport use other methods to check
	 * that first.
	 * 
	 * @param constrainToClippedArea
	 *            if true, will constrain the mouse to the clipped play viewport
	 * @return a shared vector2, this should be used right away since the values may change
	 */
	public Vector2 getMousePosCurrent(boolean constrainToClippedArea)
	{
		Lttl.game.getCamera().unProjectPoint(
				constrainToClippedArea ? LttlMath.clamp(Gdx.input.getX(),
						Lttl.game.getCamera().getClippingPixelWidth(),
						Lttl.game.getCamera().getClippingPixelWidth()
								+ Lttl.game.getCamera()
										.getViewportPixelWidthStatic())
						: Gdx.input.getX(),
				constrainToClippedArea ? LttlMath.clamp(Gdx.input.getY(),
						(Lttl.game.inEditor() ? Lttl.editor.getCamera()
								.getRawPixelHeight() : 0)
								+ Lttl.game.getCamera()
										.getClippingPixelHeight(),
						(Lttl.game.inEditor() ? Lttl.editor.getCamera()
								.getRawPixelHeight() : 0)
								+ Lttl.game.getCamera()
										.getClippingPixelHeight()
								+ Lttl.game.getCamera()
										.getViewportPixelHeightStatic())
						: Gdx.input.getY(), tmp0);
		return tmp0;
	}

	/**
	 * Bottom left is 0, includes both editor and play view (use getPixelY() or getEditorPixelY() instead)
	 * 
	 * @return
	 */
	public float getRawPixelX()
	{
		return Gdx.input.getX();
	}

	/**
	 * Bottom left is 0, includes both editor and play view (use getPixelY() or getEditorPixelY() instead)
	 * 
	 * @return
	 */
	public float getRawPixelY()
	{
		return Gdx.graphics.getHeight() - Gdx.input.getY();
	}

	/**
	 * This is the pixel y position for the play view (not constrained)
	 * 
	 * @return
	 */
	public float getPixelY()
	{
		return getRawPixelY();
	}

	/**
	 * This is the pixel y position for the editor view (not constrained)
	 * 
	 * @return
	 */
	public float getEditorPixelY()
	{
		return getRawPixelY()
				- (Gdx.graphics.getHeight() * (1 - Lttl.editor.getSettings().editorViewRatio));
	}

	/**
	 * This is the pixel x position for the play view (not constrained)
	 * 
	 * @return
	 */
	public float getPixelX()
	{
		return getRawPixelX();
	}

	/**
	 * This is the pixel x position for the editor view (not constrained)
	 * 
	 * @return
	 */
	public float getEditorPixelX()
	{
		return getRawPixelX();
	}

	/**
	 * The change in pixels this frame. (Gdx.input.getDeltaX()), regardless of if editor or play viewport.
	 * 
	 * @return
	 */
	public int getPixelDeltaX()
	{
		return Gdx.input.getDeltaX();
	}

	/**
	 * The change in pixels this frame. (Gdx.input.getDeltaY() but fixed orientation), regardless of if editor or play
	 * viewport.
	 * 
	 * @return
	 */
	public int getPixelDeltaY()
	{
		return -Gdx.input.getDeltaY();
	}

	/**
	 * Is shift being pressed in editor view.
	 * 
	 * @return
	 */
	public boolean isEditorShift()
	{
		return Lttl.input.isEditorKeyDown(Keys.SHIFT_LEFT)
				|| Lttl.input.isEditorKeyDown(Keys.SHIFT_RIGHT);
	}

	/**
	 * Is alt is being pressed in editor view. <br>
	 * Note: Using Alt must be used with another modifier or will lose focus on frame.
	 * 
	 * @return
	 */
	public boolean isEditorAlt()
	{
		return Lttl.input.isEditorKeyDown(Keys.ALT_LEFT)
				|| Lttl.input.isEditorKeyDown(Keys.ALT_RIGHT);
	}

	/**
	 * Is alt is being pressed in play view. <br>
	 * Note: Using Alt must be used with another modifier or will lose focus on frame.
	 * 
	 * @return
	 */
	public boolean isAlt()
	{
		return Lttl.input.isKeyDown(Keys.ALT_LEFT)
				|| Lttl.input.isKeyDown(Keys.ALT_RIGHT);
	}

	/**
	 * Is shift being pressed in game view.
	 * 
	 * @return
	 */
	public boolean isShift()
	{
		return Lttl.input.isKeyDown(Keys.SHIFT_LEFT)
				|| Lttl.input.isKeyDown(Keys.SHIFT_RIGHT);
	}

	/**
	 * Is Control being pressed in editor view.
	 * 
	 * @return
	 */
	public boolean isEditorControl()
	{
		return Lttl.input.isEditorKeyDown(Keys.CONTROL_LEFT)
				|| Lttl.input.isEditorKeyDown(Keys.CONTROL_RIGHT);
	}

	/**
	 * Is control being pressed in game view.
	 * 
	 * @return
	 */
	public boolean isControl()
	{
		return Lttl.input.isKeyDown(Keys.CONTROL_LEFT)
				|| Lttl.input.isKeyDown(Keys.CONTROL_RIGHT);
	}

	public boolean isEditorMouseDownAny()
	{
		return Lttl.input.isEditorMouseDown(0)
				|| Lttl.input.isEditorMouseDown(1)
				|| Lttl.input.isEditorMouseDown(2);
	}

	public boolean isEditorMousePressedAny()
	{
		return Lttl.input.isEditorMousePressed(0)
				|| Lttl.input.isEditorMousePressed(1)
				|| Lttl.input.isEditorMousePressed(2);
	}

	public boolean isEditorMouseReleasedAny()
	{
		return Lttl.input.isEditorMouseReleased(0)
				|| Lttl.input.isEditorMouseReleased(1)
				|| Lttl.input.isEditorMouseReleased(2);
	}

	public boolean isMouseDownAny()
	{
		return Lttl.input.isMouseDown(0) || Lttl.input.isMouseDown(1)
				|| Lttl.input.isMouseDown(2);
	}

	public boolean isMousePressedAny()
	{
		return Lttl.input.isMousePressed(0) || Lttl.input.isMousePressed(1)
				|| Lttl.input.isMousePressed(2);
	}

	public boolean isMouseReleasedAny()
	{
		return Lttl.input.isMouseReleased(0) || Lttl.input.isMouseReleased(1)
				|| Lttl.input.isMouseReleased(2);
	}

	public boolean isEitherMouseDownAny()
	{
		return isMouseDown0 || isMouseDown1 || isMouseDown2;
	}

	public boolean isEitherMousePressedAny()
	{
		return isMousePressed0 || isMousePressed1 || isMousePressed2;
	}

	public boolean isEitherMouseReleasedAny()
	{
		return isMouseReleased0 || isMouseReleased1 || isMouseReleased2;
	}

	/**
	 * Returns the last mouse pressed time (rawTime) (-1 if none)
	 * 
	 * @return
	 */
	public float getLastMousePressedTime0()
	{
		return lastMousePressedTime0;
	}

	/**
	 * Returns the last mouse pressed time (rawTime) (-1 if none)
	 * 
	 * @return
	 */
	public float getLastMousePressedTime1()
	{
		return lastMousePressedTime1;
	}

	/**
	 * Returns the last mouse released time (rawTime) (-1 if none)
	 * 
	 * @return
	 */
	public float getLastMouseReleasedTime0()
	{
		return lastMouseReleasedTime0;
	}

	/**
	 * Returns the last mouse released time (rawTime) (-1 if none)
	 * 
	 * @return
	 */
	public float getLastMouseReleasedTime1()
	{
		return lastMouseReleasedTime1;
	}
}
