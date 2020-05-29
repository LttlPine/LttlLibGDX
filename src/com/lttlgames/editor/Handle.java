package com.lttlgames.editor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * @author Josh
 */
public abstract class Handle
{
	/**
	 * disabling means it won't interact with mouse and color will be a percent of normal opacity
	 */
	public boolean enabled = true;
	/**
	 * if not visible, does not draw the handle and nothing can interact with it
	 */
	public boolean visible = true;
	public Vector2 position = new Vector2();
	private float zPos = 0;
	public boolean fixedScale = false;
	final static float disabledOpacityFactor = .3f;
	/**
	 * This holds the state of the handle being pressed on, since it could be being dragged but appeared while dragging
	 * was happening.
	 */
	boolean isPressed = false;

	private boolean isRegistered = false;

	public boolean isDraggable = false;
	/**
	 * When dragging, handle position auto updates.
	 */
	public boolean isAutoUpdatePos = false;
	public boolean isHoverable = false;
	/**
	 * if isDraggable, when shift is held x or y axiss will be locked and reflected in the deltaX and deltaY parameters
	 * in onDrag()
	 */
	public boolean canLockAxis = false;

	public final Color fillColor;
	public final Color borderColor;
	public float borderWidth = 0;
	/**
	 * Is the mouse down on this handle
	 */
	boolean isDown = false;
	boolean isDragging = false;
	LockedAxis lockedAxis = LockedAxis.None;
	boolean wasDragged = false;

	/**
	 * Creates and auto registers a handle with given properites.
	 * 
	 * @param posX
	 * @param posY
	 * @param zPos
	 *            z order (relative to only other handles)
	 * @param fixedScale
	 *            scale will adjust based on camera zoom to maintain size
	 * @param rotation
	 *            if circle, irrelevant
	 * @param isDraggable
	 *            if isDraggable, position can auto update when dragging if isAutoUpdatePos is true, will also call
	 *            onDrag()
	 * @param isAutoUpdatePos
	 *            if isDraggable, the handle position will auto update
	 * @param canLockAxis
	 *            if isDraggable, when shift is held x or y axiss will be locked and reflected in the deltaX and deltaY
	 *            parameters in onDrag()
	 * @param isHoverable
	 *            if is hoverable will run callbacks onHoverEnter(), onHover(), and onHoverExit()
	 * @param fillColor
	 *            the fill color, if null, will not draw a fill
	 * @param borderColor
	 *            the border color, if null, will not draw a border
	 * @param borderWidth
	 *            if a any border, width
	 */
	Handle(float posX, float posY, float zPos, boolean fixedScale,
			boolean isDraggable, boolean isAutoUpdatePos, boolean canLockAxis,
			boolean isHoverable, Color fillColor, Color borderColor,
			float borderWidth)
	{
		this.position.set(posX, posY);
		this.zPos = zPos;
		this.fixedScale = fixedScale;
		this.isDraggable = isDraggable;
		this.canLockAxis = canLockAxis;
		this.isAutoUpdatePos = isAutoUpdatePos;
		this.isHoverable = isHoverable;
		if (fillColor != null)
		{
			this.fillColor = new Color(fillColor);
		}
		else
		{
			this.fillColor = null;
		}
		if (borderColor != null)
		{
			this.borderColor = new Color(borderColor);
		}
		else
		{
			this.borderColor = null;
		}
		this.borderWidth = borderWidth;

		register();
	}

	/**
	 * The mouse entered the handle (this is ambigious to the mouse's button state).
	 */
	public void onHoverEnter()
	{

	}

	/**
	 * The mouse is contained in the handle (this is ambigious to the mouse's button state).
	 */
	public void onHover()
	{

	}

	/**
	 * The mouse left the handle (this is ambigious to the mouse's button state).
	 */
	public void onHoverExit()
	{

	}

	/**
	 * The handles is being dragged (isDraggable must be true and it must have been pressed on, since it could have been
	 * made under mouse while mouse was already dragging).<br>
	 * If isAutoUpdatePos then the handle's position is updated before this callback.
	 * 
	 * @param deltaX
	 *            (change in position with possibly locked axis)
	 * @param deltaY
	 *            (change in position with possibly locked axis)
	 */
	public void onDrag(float deltaX, float deltaY)
	{

	}

	/**
	 * Mouse just went down on this handle. Check for which mouse button.
	 * 
	 * @param button
	 */
	public void onPressed()
	{

	}

	/**
	 * Mouse is currently down on the handle. Check for which mouse button.
	 * 
	 * @param button
	 */
	public void onDown()
	{

	}

	/**
	 * Mouse has been released from clicking on this handle. Check for which mouse button.
	 * 
	 * @param button
	 */
	public void onReleased()
	{

	}

	/**
	 * Runs every frame after all component updates and before any of the handle callbacks are checked. Good place to
	 * update position and rotation based on transforms.
	 */
	public void onUpdate()
	{

	}

	/**
	 * Removes handle from the HandleManager so it will not render or update anymore, but can register it again by
	 * running register().
	 */
	final public void unregister()
	{
		isRegistered = false;
		Lttl.editor.getHandleController().unregister(this);
	}

	final public void register()
	{
		if (!isRegistered)
		{
			isRegistered = true;
			Lttl.editor.getHandleController().register(this);
		}
	}

	/**
	 * Returns if this handle is currently registered.
	 * 
	 * @return
	 */
	final public boolean isRegistered()
	{
		return isRegistered;
	}

	/**
	 * Checks if the handle contains the editor mouse.
	 * 
	 * @return
	 */
	public abstract boolean containsMouse();

	/**
	 * Checks if the handle overlaps the rect.
	 * 
	 * @return
	 */
	public abstract boolean overlapsRectangle(Rectangle rect);

	public float getzPos()
	{
		return zPos;
	}

	/**
	 * Sets the z position for this handle.
	 * 
	 * @param zPos
	 */
	public void setzPos(float zPos)
	{
		this.zPos = zPos;
		Lttl.editor.getHandleController().setNeedToUpdateOrder(true);
	}

	/**
	 * Draws the handle.
	 */
	public abstract void draw();

	Color prepColor(Color c)
	{
		return (enabled) ? c : new Color(c.r, c.g, c.b, c.a
				* Handle.disabledOpacityFactor);
	}

	float getScaleFactor()
	{
		return (fixedScale) ? Lttl.debug.eF() : 1;
	}

	/**
	 * If is dragging and canLockAxis, then this will return the locked axis if any. The locked axis can not be
	 * redefined until mouse is released.
	 * 
	 * @return
	 */
	public LockedAxis getLockedAxis()
	{
		return lockedAxis;
	}

	/**
	 * If isDraggable, and is currently dragging.
	 * 
	 * @return
	 */
	public boolean isDragging()
	{
		return isDragging;
	}

	/**
	 * If did some dragging between pressed and released, this gets reset after onReleased() is called
	 * 
	 * @return
	 */
	public boolean wasDragged()
	{
		return wasDragged;
	}
}
