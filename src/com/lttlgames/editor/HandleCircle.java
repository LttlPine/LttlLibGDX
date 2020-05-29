package com.lttlgames.editor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class HandleCircle extends Handle
{
	private Rectangle boundingRect = new Rectangle();
	private Circle circle = new Circle();
	public float radius = 0;

	/**
	 * Creates and auto registers a circle handle.
	 * 
	 * @param position
	 *            position of handle (center)
	 * @param zPos
	 *            z order (relative to only other handles)
	 * @param radius
	 * @param fixedRadius
	 *            the radius will adjust based on camera zoom to maintain size
	 * @param isDraggable
	 *            if isDraggable, position can auto update when dragging if isAutoUpdatePos is true, will also call
	 *            onDrag()
	 * @param isAutoUpdatePos
	 *            if isDraggable, the handle position will auto update
	 * @param canLockAxis
	 *            if isDraggable, when shift is held x or y axiss will be locked and reflected in the deltaX and deltaY
	 *            parameters in onDrag(), use getLockedAxis() to check
	 * @param isHoverable
	 *            if is hoverable will run callbacks onHoverEnter(), onHover(), and onHoverExit()
	 * @param fillColor
	 *            the fill color, if null, will not draw a fill
	 * @param borderColor
	 *            the border color, if null, will not draw a border
	 */
	public HandleCircle(Vector2 position, float zPos, float radius,
			boolean fixedRadius, boolean isDraggable, boolean isAutoUpdatePos,
			boolean canLockAxis, boolean isHoverable, Color fillColor,
			Color borderColor)
	{
		this(position.x, position.y, zPos, radius, fixedRadius, isDraggable,
				isAutoUpdatePos, canLockAxis, isHoverable, fillColor,
				borderColor);
	}

	/**
	 * Creates and auto registers a circle handle.
	 * 
	 * @param posX
	 * @param posY
	 * @param zPos
	 *            z order (relative to only other handles)
	 * @param radius
	 * @param fixedRadius
	 *            the radius will adjust based on camera zoom to maintain size
	 * @param isDraggable
	 *            if isDraggable, position can auto update when dragging if isAutoUpdatePos is true, will also call
	 *            onDrag()
	 * @param isAutoUpdatePos
	 *            if isDraggable, the handle position will auto update
	 * @param canLockAxis
	 *            if isDraggable, when shift is held x or y axiss will be locked and reflected in the deltaX and deltaY
	 *            parameters in onDrag(), use getLockedAxis() to check
	 * @param isHoverable
	 *            if is hoverable will run callbacks onHoverEnter(), onHover(), and onHoverExit()
	 * @param fillColor
	 *            the fill color, if null, will not draw a fill
	 * @param borderColor
	 *            the border color, if null, will not draw a border
	 */
	public HandleCircle(float posX, float posY, float zPos, float radius,
			boolean fixedRadius, boolean isDraggable, boolean isAutoUpdatePos,
			boolean canLockAxis, boolean isHoverable, Color fillColor,
			Color borderColor)
	{
		super(posX, posY, zPos, fixedRadius, isDraggable, isAutoUpdatePos,
				canLockAxis, isHoverable, fillColor, borderColor, 0);
		this.radius = radius;
	}

	@Override
	final public boolean containsMouse()
	{
		float radiusAdj = getScaleFactor() * radius;
		// update bounding rect and circle shapes
		boundingRect.set(position.x - radiusAdj, position.y - radiusAdj,
				radiusAdj * 2, radiusAdj * 2);
		circle.set(position.x, position.y, radiusAdj);

		return boundingRect.contains(Lttl.input.getEditorMousePos())
				&& circle.contains(Lttl.input.getEditorMousePos());
	}

	@Override
	public void draw()
	{
		float scaleFactor = getScaleFactor();
		if (fillColor != null)
		{
			if (borderColor != null)
			{
				Lttl.debug.drawCircleFilledOutline(position.x, position.y,
						radius * scaleFactor, prepColor(fillColor),
						prepColor(borderColor));
			}
			else
			{
				Lttl.debug.drawCircle(position, radius * scaleFactor,
						prepColor(fillColor));
			}
		}
		else if (borderColor != null)
		{
			Lttl.debug.drawCircleOutline(position.x, position.y, radius
					* scaleFactor, prepColor(borderColor));
		}
		else
		{
			Lttl.Throw();
		}
	}

	@Override
	public boolean overlapsRectangle(Rectangle rect)
	{
		return Intersector.overlaps(circle, rect);
	}
}
