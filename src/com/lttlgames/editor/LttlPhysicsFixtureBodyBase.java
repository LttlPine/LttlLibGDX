package com.lttlgames.editor;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.helpers.Vector2Array;

@Persist(-90127)
public abstract class LttlPhysicsFixtureBodyBase extends LttlPhysicsBase
{
	boolean containsMouseStart = false;
	int containsMouseStartzIndex = -1;

	static protected Vector2Array aabbTempArray = new Vector2Array();
	protected Rectangle aabb;

	/**
	 * Returns if the mouse was contained in this physics component at the start of this frame, which is what was
	 * rendered at the end of the previous frame.<br>
	 * This does not require any extra computation. Does not require {@link LttlPhysicsBody#callbackMouse}.<br>
	 * This is guaranteed to be a valid replacement of {@link #containsMouse()} if called in
	 * {@link LttlComponent#onEarlyUpdate()}. This value only changes at the start of each frame.
	 */
	public final boolean containsMouseStart()
	{
		return containsMouseStart;
	}

	/**
	 * if {@link #containsMouseStart()}, then this will return the z index. 0 means it was the closest to camera when
	 * mouse was checked at start of frame.<br>
	 * Same z spaces will return with different indexes.
	 * 
	 * @return -1 if mouse was not contained at start
	 */
	public final int getContainsMouseStartZindex()
	{
		return containsMouseStartzIndex;
	}

	/**
	 * Returns if the mouse is contained in this physics component.
	 */
	public boolean containsMouse()
	{
		return contains(Lttl.input.getMousePos());
	}

	/**
	 * see {@link #contains(float, float)}
	 */
	public boolean contains(Vector2 point)
	{
		return contains(point.x, point.y);
	}

	/**
	 * Returns if has a fixture that contains this world point
	 */
	public boolean contains(float x, float y)
	{
		return getFixtureContains(x, y) != null;
	}

	/**
	 * Returns the first fixture that overlaps the given point. Null if none.
	 */
	public abstract Fixture getFixtureContains(float x, float y);

	public abstract int getFixtureCount();

	/**
	 * Calculates (each time ran) and returns the AABB for this physics object.
	 * 
	 * @return AABB rectangle or null if not {@link #isInit()}
	 */
	public abstract Rectangle getAABB();
}
