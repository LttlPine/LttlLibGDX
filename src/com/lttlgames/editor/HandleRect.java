package com.lttlgames.editor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.lttlgames.helpers.LttlCallback;
import com.lttlgames.helpers.LttlGeometry;
import com.lttlgames.helpers.LttlMath;

public class HandleRect extends Handle
{
	private Rectangle aabb = new Rectangle();
	private float[] rotatedRectanglePolygon = null;
	private Vector2 tmpPoint = new Vector2();
	public Vector2 scale = new Vector2(1, 1);
	public float rotation = 0;

	/**
	 * Creates and auto registers a circle handle.
	 * 
	 * @param position
	 *            initial position of handle (center)
	 * @param zPos
	 *            initial z order (relative to only other handles)
	 * @param scale
	 *            initial scale
	 * @param fixedScale
	 *            scale will adjust based on camera zoom to maintain size
	 * @param rotation
	 *            initial rotation
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
	 * @param borderWidth
	 *            if any border, width
	 * @param borderColor
	 *            the border color, if null, will not draw a border
	 */
	public HandleRect(Vector2 position, float zPos, Vector2 scale,
			boolean fixedScale, float rotation, boolean isDraggable,
			boolean isAutoUpdatePos, boolean canLockAxis, boolean isHoverable,
			Color fillColor, float borderWidth, Color borderColor)
	{
		this(position.x, position.y, zPos, scale.x, scale.y, fixedScale,
				rotation, isDraggable, isAutoUpdatePos, canLockAxis,
				isHoverable, fillColor, borderWidth, borderColor);
	}

	/**
	 * Creates and auto registers a circle handle.
	 * 
	 * @param posX
	 * @param posY
	 * @param zPos
	 *            initial z order (relative to only other handles)
	 * @param scaleX
	 * @param scaleY
	 * @param fixedScale
	 *            scale will adjust based on camera zoom to maintain size
	 * @param rotation
	 *            initial rotation
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
	 * @param borderWidth
	 *            if any border, width
	 * @param borderColor
	 *            the border color, if null, will not draw a border
	 */
	public HandleRect(float posX, float posY, float zPos, float scaleX,
			float scaleY, boolean fixedScale, float rotation,
			boolean isDraggable, boolean isAutoUpdatePos, boolean canLockAxis,
			boolean isHoverable, Color fillColor, float borderWidth,
			Color borderColor)
	{
		super(posX, posY, zPos, fixedScale, isDraggable, isAutoUpdatePos,
				canLockAxis, isHoverable, fillColor, borderColor, borderWidth);
		this.rotation = rotation;
		this.scale.set(scaleX, scaleY);
	}

	@Override
	public boolean containsMouse()
	{
		// adjust scale
		scale.scl(getScaleFactor());

		// UPDATE STUFF!!!
		if (rotation == 0)
		{
			// generate bounding rect, taking into consideration the scale
			aabb.x = position.x - (scale.x / 2);
			aabb.y = position.y - (scale.y / 2);
			aabb.width = scale.x;
			aabb.height = scale.y;
		}
		else
		{
			// CALCULATE ROTATED RECTANGLE

			// compute the 4 corners as if there was no offset, or scale, or world position, then add
			// transformedPosition to it
			// TOP LEFT //
			tmpPoint.set(-(scale.x / 2), (scale.y / 2));
			LttlMath.TransformPoint(rotation, LttlMath.One, tmpPoint, tmpPoint);
			rotatedRectanglePolygon[0] = tmpPoint.x + position.x;
			rotatedRectanglePolygon[1] = tmpPoint.y + position.y;

			// TOP RIGHT //
			tmpPoint.set((scale.x / 2), (scale.y / 2));
			LttlMath.TransformPoint(rotation, LttlMath.One, tmpPoint, tmpPoint);
			rotatedRectanglePolygon[2] = tmpPoint.x + position.x;
			rotatedRectanglePolygon[3] = tmpPoint.y + position.y;

			// BOTTOM RIGHT //
			tmpPoint.set((scale.x / 2), -(scale.y / 2));
			LttlMath.TransformPoint(rotation, LttlMath.One, tmpPoint, tmpPoint);
			rotatedRectanglePolygon[4] = tmpPoint.x + position.x;
			rotatedRectanglePolygon[5] = tmpPoint.y + position.y;

			// BOTTOM LEFT //
			tmpPoint.set(-(scale.x / 2), -(scale.y / 2));
			LttlMath.TransformPoint(rotation, LttlMath.One, tmpPoint, tmpPoint);
			rotatedRectanglePolygon[6] = tmpPoint.x + position.x;
			rotatedRectanglePolygon[7] = tmpPoint.y + position.y;

			// calculate the bounding rect (based on rotated rect)
			LttlMath.GetAABB(rotatedRectanglePolygon, aabb);

		}
		// revert scale
		scale.scl(1 / getScaleFactor());

		// Check mouse contained
		if (aabb.contains(Lttl.input.getEditorMousePos()))
		{
			if (rotation == 0)
			{
				return true;
			}
			else
			{
				return LttlGeometry.ContainsPointInPolygon(rotatedRectanglePolygon,
						Lttl.input.getEditorX(), Lttl.input.getEditorY());
			}
		}
		return false;
	}

	@Override
	public void draw()
	{
		// adjust scale
		scale.scl(getScaleFactor());
		if (fillColor != null)
		{
			if (borderColor != null)
			{
				Lttl.debug.drawRectFilledOutline(position.x, position.y,
						scale.x, scale.y, rotation, prepColor(fillColor),
						borderWidth, prepColor(borderColor));
			}
			else
			{
				Lttl.debug.drawRect(position, scale.x, scale.y, rotation,
						prepColor(fillColor));
			}
		}
		else if (borderColor != null)
		{
			Lttl.debug.drawRectOutline(position.x, position.y, scale.x,
					scale.y, borderWidth, prepColor(borderColor));
		}
		else
		{
			Lttl.Throw();
		}
		// revert scale
		scale.scl(1 / getScaleFactor());
	}

	@Override
	public boolean overlapsRectangle(Rectangle rect)
	{
		if (rotatedRectanglePolygon != null)
		{
			return LttlGeometry.OverlapsConvex(rotatedRectanglePolygon,
					LttlMath.GetRectFourCorners(rect), null);
		}
		else
		{
			return rect.overlaps(aabb);
		}
	}

	/**
	 * This generates a handle rect in the world position of the childPosToUpdate vector2 and when modified, updates the
	 * childPosToUpdate vector2 with the child position (converts from world)
	 * 
	 * @param childPosToUpdate
	 * @param transform
	 * @param undo
	 *            is undo enabled
	 * @param callback
	 *            [0 = on drag], [1 = on undo/redo], no objects are returned, {@link #childPosToUpdate} already updated<br>
	 *            Can be null.
	 * @return
	 */
	public static HandleRect GenerateTransformRenderHandle(
			final Vector2 childPosToUpdate, final LttlTransform transform,
			final boolean undo, final LttlCallback callback)
	{
		final Vector2 tmpV2 = new Vector2();

		transform.renderToWorldPosition(tmpV2.set(childPosToUpdate), false);
		HandleRect handle = new HandleRect(tmpV2.x, tmpV2.y, 0,
				Lttl.editor.getSettings().handleSize,
				Lttl.editor.getSettings().handleSize, true, 0, true, true,
				true, false, Lttl.editor.getSettings().colorCollider, 0, null)
		{
			private Vector2 undoObject = null;

			private Vector2 getCurrentChildPos(Vector2 v)
			{
				if (v == null)
				{
					v = new Vector2();
				}
				transform.worldToRenderPosition(v.set(position), false);
				return v;
			}

			@Override
			public void onPressed()
			{
				if (undo && Lttl.input.isEditorMousePressed())
				{
					// save undoObject
					undoObject = getCurrentChildPos(null);
				}
			}

			@Override
			public void onReleased()
			{
				if (undo && Lttl.input.isEditorMouseReleased() && wasDragged())
				{
					registerUndoState();
				}
			}

			@Override
			public void onDrag(float deltaX, float deltaY)
			{
				getCurrentChildPos(childPosToUpdate);
				if (callback != null)
				{
					callback.callback(0);
				}
			}

			@Override
			public void onUpdate()
			{
				super.onUpdate();
				transform.renderToWorldPosition(position.set(childPosToUpdate),
						false);

			}

			private void registerUndoState()
			{
				Lttl.editor.getUndoManager().registerUndoState(
						new UndoState("Modified Vector2 Handle", new UndoField(
								transform, undoObject,
								getCurrentChildPos(null), new UndoSetter()
								{
									@Override
									public void set(LttlComponent comp,
											Object value)
									{
										childPosToUpdate.set((Vector2) value);
										if (callback != null)
										{
											callback.callback(1);
										}
									}
								})));
			}
		};
		return handle;
	}
}
