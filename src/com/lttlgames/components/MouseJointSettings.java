package com.lttlgames.components;

import com.badlogic.gdx.physics.box2d.joints.MouseJointDef;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.Persist;

@Persist(-9027)
public class MouseJointSettings
{
	/**
	 * @see MouseJointDef#dampingRatio
	 */
	@Persist(902700)
	@GuiMin(0)
	public float dampingRatio = .780f;

	/**
	 * @see MouseJointDef#frequencyHz
	 */
	@Persist(902701)
	public float frequencyHz = 7.6f;

	/**
	 * @see MouseJointDef#maxForce
	 */
	@Persist(902702)
	public float maxForceMultiplier = 96;

	/**
	 * should maxForce be relative to body's mass
	 */
	@Persist(902703)
	public boolean relativeMass = true;

	/**
	 * should maxForce be relative to gravity
	 */
	@Persist(902704)
	public boolean relativeGravity = true;

	@GuiMin(0)
	@Persist(902705)
	public float jointFriction = 0;

	@Persist(902706)
	public boolean autoStartWhenBodyClicked = true;

	/**
	 * will automatically set this body to be a bullet when dragging, will return to original setting after done
	 * dragging
	 */
	@Persist(902707)
	public boolean autoBullet = true;

	/**
	 * when dragging, will constrain the target position to the clipped viewport
	 */
	@Persist(902708)
	public boolean clippedViewportOnly = true;
}
