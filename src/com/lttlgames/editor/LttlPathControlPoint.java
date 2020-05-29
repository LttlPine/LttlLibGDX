package com.lttlgames.editor;

import com.badlogic.gdx.math.Vector2;
import com.lttlgames.editor.annotations.AnimateField;
import com.lttlgames.editor.annotations.GuiButton;
import com.lttlgames.editor.annotations.GuiCanNull;
import com.lttlgames.editor.annotations.Persist;

//05
@Persist(-9076)
public class LttlPathControlPoint
{
	@Persist(907600)
	public LttlPathControlPointType type = LttlPathControlPointType.Proximity;
	/**
	 * relative position of control point (relative to world scale, position, and rotation of tranform)
	 */
	@Persist(907601)
	@AnimateField(0)
	public final Vector2 pos = new Vector2();
	/**
	 * position of left handle relative to control point's position
	 */
	@Persist(907602)
	@AnimateField(1)
	public final Vector2 leftPos = new Vector2();
	/**
	 * position of righ handle relative to control point's position
	 */
	@Persist(907603)
	@AnimateField(2)
	public final Vector2 rightPos = new Vector2();
	/**
	 * are the left and right handles locked with eachother (creating smooth corner)<br>
	 * Editor use only
	 */
	@Persist(907604)
	boolean handlesLocked = true;
	/**
	 * Optional data for a control point.
	 */
	@Persist(907605)
	@GuiCanNull
	public LttlPathControlPointExtra extra = null;

	/* EDITOR USE ONLY */
	Vector2 originalScalePosition;
	Handle mainHandle;
	Handle leftHandle;
	Handle rightHandle;

	// prevent instantiation without resetting handles
	@SuppressWarnings("unused")
	private LttlPathControlPoint()
	{
	}

	public LttlPathControlPoint(LttlPathControlPoint controlPoint)
	{
		this(controlPoint.pos, controlPoint.leftPos, controlPoint.rightPos,
				controlPoint.handlesLocked, controlPoint.type);
	}

	public LttlPathControlPoint(float posX, float posY)
	{
		pos.set(posX, posY);
		resetHandles();
	}

	public LttlPathControlPoint(Vector2 p_main)
	{
		this(p_main.x, p_main.y);
	}

	public LttlPathControlPoint(Vector2 main, Vector2 leftHandle,
			Vector2 rightHandle, boolean handlesLocked,
			LttlPathControlPointType type)
	{
		this.pos.set(main);
		this.leftPos.set(leftHandle);
		this.rightPos.set(rightHandle);
		this.type = type;
		this.handlesLocked = handlesLocked;
	}

	/**
	 * forces the right handle to be locked from left, maintaining length
	 */
	public void lockFromLeft()
	{
		float len = rightPos.len();
		rightPos.set(leftPos).nor().scl(-len);
	}

	void lockFromLeftInternal(float deltaLength)
	{
		float len = rightPos.len();
		rightPos.set(leftPos).nor().scl(-len - deltaLength);
	}

	/**
	 * forces the left handle to be locked from right, maintaining length
	 */
	public void lockFromRight()
	{
		float len = leftPos.len();
		leftPos.set(rightPos).nor().scl(-len);
	}

	void lockFromRightInternal(float deltaLength)
	{
		float len = leftPos.len();
		leftPos.set(rightPos).nor().scl(-len - deltaLength);
	}

	@SuppressWarnings("rawtypes")
	@GuiButton
	private void resetHandles(GuiFieldObject gfo)
	{
		resetHandles();
		((LttlPath) gfo.getAncestorByClass(LttlPath.class, true).objectRef)
				.guiControlPointsUpdated();
	}

	void resetHandles()
	{
		leftPos.set(-Lttl.game.getSettings().getWidthFactor() * 1, 0);
		rightPos.set(Lttl.game.getSettings().getWidthFactor() * 1, 0);
		handlesLocked = true;
	}
}
