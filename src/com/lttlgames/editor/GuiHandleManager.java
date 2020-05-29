package com.lttlgames.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import com.lttlgames.helpers.LttlMath;

/**
 * @author Josh
 */
class GuiHandleManager
{
	private ArrayList<Handle> orderedHandles = new ArrayList<Handle>();
	private boolean needToUpdateOrder = false;
	/**
	 * This is the handle that the mouse is currently in contact with.
	 */
	private Handle contactedHandle = null;
	private Iterator<Handle> it = null;
	private boolean dontDrawThisFrame = false;

	void updateOrderedHandles()
	{
		// Sort the list based on z position
		Collections.sort(orderedHandles, new Comparator<Handle>()
		{
			@Override
			public int compare(Handle o1, Handle o2)
			{
				if (o1.getzPos() == o2.getzPos())
				{
					return 0;
				}
				else if (o1.getzPos() > o2.getzPos())
				{
					return 1;
				}
				else
				{
					return -1;
				}
			}
		});
	}

	/**
	 * Updates and checks handles. Does not draw.
	 * 
	 * @return true if consuming mouse
	 */
	boolean update()
	{
		boolean isConsumingMouse = false;

		// update the z order if necessary
		if (needToUpdateOrder)
		{
			updateOrderedHandles();
			needToUpdateOrder = false;
		}

		// if the mouse is down, and it's not being pressed (down this frame), and there is a contact handle, then don't
		// check other handles
		if (contactedHandle != null && Lttl.input.isEitherMouseDownAny()
				&& !Lttl.input.isEditorMousePressedAny())
		{
			contactedHandle.onHover();
			contactedHandle.onDown();

			// check drag if there is some mouse movement and if draggable and if pressed
			if (contactedHandle.isDraggable
					&& contactedHandle.isPressed
					&& (contactedHandle.isDragging || (LttlMath.abs(Lttl.input
							.getPixelDeltaX()) > 0 || LttlMath.abs(Lttl.input
							.getPixelDeltaY()) > 0)))
			{
				// if this is first frame that is dragging and shift is held, then check for lockedAxis
				if (!contactedHandle.isDragging
						&& Lttl.editor.getInput().isShiftEV())
				{
					if (LttlMath.abs(Lttl.input.getPixelDeltaX()) >= LttlMath
							.abs(Lttl.input.getPixelDeltaY()))
					{
						contactedHandle.lockedAxis = LockedAxis.X;
					}
					else
					{
						contactedHandle.lockedAxis = LockedAxis.Y;
					}
				}

				// process onDrag callback, check if there is a lockedAxis
				switch (contactedHandle.lockedAxis)
				{
					case None:
						float deltaX = Lttl.input.getEditorDeltaX();
						float deltaY = Lttl.input.getEditorDeltaY();
						if (Lttl.input.isEditorControl())
						{
							deltaX = Lttl.editor.getSettings().snapX(
									Lttl.input.getEditorX())
									- contactedHandle.position.x;
							deltaY = Lttl.editor.getSettings().snapY(
									Lttl.input.getEditorY())
									- contactedHandle.position.y;
						}
						if (contactedHandle.isAutoUpdatePos)
						{
							contactedHandle.position.add(deltaX, deltaY);
						}
						contactedHandle.onDrag(deltaX, deltaY);
						break;
					case X:
						if (contactedHandle.isAutoUpdatePos)
						{
							contactedHandle.position.add(
									Lttl.input.getEditorDeltaX(), 0);
						}
						contactedHandle.onDrag(Lttl.input.getEditorDeltaX(), 0);
						break;
					case Y:
						if (contactedHandle.isAutoUpdatePos)
						{
							contactedHandle.position.add(0,
									Lttl.input.getEditorDeltaY());
						}
						contactedHandle.onDrag(0, Lttl.input.getEditorDeltaY());
						break;

				}
				contactedHandle.isDragging = true;
				contactedHandle.wasDragged = true;
			}
		}
		else
		{
			// process release
			if (contactedHandle != null)
			{
				// reset drag state
				if (contactedHandle.isDragging)
				{
					contactedHandle.isDragging = false;
					contactedHandle.lockedAxis = LockedAxis.None;
				}

				// set released on mouse if mouse was released
				if (contactedHandle.isDown)
				{
					contactedHandle.isDown = false;
					contactedHandle.isPressed = false;
					contactedHandle.onReleased();
					contactedHandle.wasDragged = false;
				}
			}

			boolean contact = false;
			// check which handles have a mouse contained in it
			// set iterator just in case a handle is unregistered while iterating
			it = orderedHandles.iterator();
			while (it.hasNext())
			{
				Handle h = it.next();
				// if the handle is enabled and not hidden and the mouse is down or the handle is hoverable check if it
				// contains the mouse position
				if (h.visible && h.enabled
						&& (h.isHoverable || Lttl.input.isEditorMouseDownAny()))
				{
					// check is handle contains mouse
					if (h.containsMouse())
					{
						if (h.isHoverable)
						{
							// check if different handle than last frame
							if (contactedHandle != h)
							{
								// if exists, exit previous handle
								if (contactedHandle != null)
								{
									contactedHandle.onHoverExit();
								}
								h.onHoverEnter();
							}
							h.onHover();
						}

						// if mouse was pressed this frame
						if (Lttl.input.isEditorMousePressedAny())
						{
							// mouseStartPixelPosition.set(Gdx.input.getX(),
							// Gdx.input.getY());
							h.isPressed = true;
							h.onPressed();
						}

						// if mouse is down, also set isDown so we can know when it is released
						if (Lttl.input.isEditorMouseDownAny())
						{
							h.isDown = true;
							h.onDown();
						}

						// don't need to keep on checking, can only be focused on one handle at a time, and because list
						// is ordered it will get closest one
						contactedHandle = h;
						contact = true;
						break;
					}
				}
			}
			// clear iterator
			it = null;

			// sets no handle as contacted if no contact was found
			if (!contact)
			{
				// if no contact was made, but last frame a contact handle was set, then...
				if (contactedHandle != null)
				{
					// set exit for hover
					if (contactedHandle.isHoverable)
					{
						contactedHandle.onHoverExit();
					}

					// set released on mouse if mouse was released
					if (contactedHandle.isDown)
					{
						contactedHandle.isDown = false;
						contactedHandle.onReleased();
						contactedHandle.wasDragged = false;
					}
					isConsumingMouse = true;
				}
				contactedHandle = null;
			}
		}
		if (contactedHandle != null)
		{
			isConsumingMouse = true;
		}

		// process each handles OnUpdate()
		it = orderedHandles.iterator();
		while (it.hasNext())
		{
			it.next().onUpdate();
		}
		it = null;

		return isConsumingMouse;
	}

	/**
	 * Draws the handles.
	 */
	void drawHandles()
	{
		if (!dontDrawThisFrame && Lttl.editor.getSettings().enableHandles)
		{
			// draw in reverse order so they are drawn in correct z pos
			for (int i = orderedHandles.size() - 1; i >= 0; i--)
			{
				if (!orderedHandles.get(i).visible) continue;
				orderedHandles.get(i).draw();
			}
		}
		// reset
		dontDrawThisFrame = false;
	}

	void register(Handle handle)
	{
		orderedHandles.add(handle);
		needToUpdateOrder = true;
	}

	void unregister(Handle handle)
	{
		if (it != null)
		{
			it.remove();
		}
		else
		{
			orderedHandles.remove(handle);
		}
	}

	void setNeedToUpdateOrder(boolean needToUpdateOrder)
	{
		this.needToUpdateOrder = needToUpdateOrder;
	}

	/**
	 * Get the handle currently in contact with the mouse.
	 * 
	 * @return
	 */
	public Handle getContactedCollider()
	{
		return contactedHandle;
	}

	/**
	 * The handles will not draw this frame.
	 */
	public void dontDrawThisFrame()
	{
		dontDrawThisFrame = true;
	}

	/**
	 * Returns if the handles should draw this frame.
	 * 
	 * @return
	 */
	public boolean isDrawingThisFrame()
	{
		return !dontDrawThisFrame;
	}

	public int getHandleCount()
	{
		return orderedHandles.size();
	}
}
