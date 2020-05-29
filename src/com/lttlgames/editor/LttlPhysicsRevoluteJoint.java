package com.lttlgames.editor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.JointDef;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJoint;
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef;
import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.GuiGroup;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.helpers.LttlHelper;
import com.lttlgames.helpers.LttlMath;

//12
/**
 * @see RevoluteJoint
 */
@Persist(-90141)
public class LttlPhysicsRevoluteJoint extends
		LttlPhysicsJointBase<RevoluteJoint>
{
	/* STATIC */
	private static RevoluteJointDef revoluteJointDefShared = new RevoluteJointDef();

	/* MEMBER */

	/**
	 * @see RevoluteJointDef#enableLimit
	 */
	@GuiCallback("onGuiChange")
	@GuiGroup("Angle Limit")
	@Persist(9014101)
	public boolean enableLimit = false;

	@GuiCallback("destroyAndInit")
	@GuiGroup("Angle Limit")
	@Persist(90141010)
	public boolean relativeLimit = true;

	/**
	 * @see RevoluteJointDef#lowerAngle
	 */
	@GuiCallback("onGuiChange")
	@GuiGroup("Angle Limit")
	@Persist(9014102)
	public float lowerAngle = 0;

	/**
	 * @see RevoluteJointDef#upperAngle
	 */
	@GuiCallback("onGuiChange")
	@GuiGroup("Angle Limit")
	@Persist(9014103)
	public float upperAngle = 0;

	@GuiGroup("Angle Limit")
	@Persist(9014108)
	public boolean drawLimits = true;

	/**
	 * used to make the otherBody angle line to be visually more aligned
	 */
	@GuiCallback("onGuiChange")
	@GuiGroup("Angle Limit")
	@Persist(9014109)
	public float angleOffsetDraw = 0;

	/**
	 * Can be enabled with a motorSpeed of 0 and maxMotorTorque something low to act as joint friction.
	 * 
	 * @see RevoluteJointDef#enableMotor
	 */
	@GuiCallback("onGuiChange")
	@GuiGroup("Motor")
	@Persist(9014104)
	public boolean enableMotor = false;

	/**
	 * Usually in degrees per second.
	 * 
	 * @see RevoluteJointDef#motorSpeed
	 */
	@GuiCallback("onGuiChange")
	@GuiGroup("Motor")
	@Persist(9014105)
	public float motorSpeed = 0;

	/**
	 * @see RevoluteJointDef#maxMotorTorque
	 */
	@GuiCallback("onGuiChange")
	@GuiGroup("Motor")
	@Persist(9014106)
	public float maxMotorTorque = 0;

	/**
	 * if true, the maxMotorTorque will be relative to the mass of the otherBody
	 */
	@GuiCallback("onGuiChange")
	@GuiGroup("Motor")
	@Persist(90141011)
	public boolean relativeMass = false;

	/**
	 * if true, the maxMotorTorque will be relative to the gravity
	 */
	@GuiCallback("onGuiChange")
	@GuiGroup("Motor")
	@Persist(90141012)
	public boolean relativeGravity = false;

	/**
	 * If true, uses the origin of the transform for the anchor position
	 */
	@GuiCallback("onGuiChange")
	@Persist(9014100)
	public boolean useOrigin = true;

	@GuiGroup("Debug")
	@Persist(9014107)
	public boolean drawAnchor = true;

	@Override
	protected void initInternal(LttlPhysicsBody bodyCompA,
			LttlPhysicsBody bodyCompB)
	{
		processJointDefinition(revoluteJointDefShared);
		revoluteJointDefShared.initialize(
				bodyCompA.getBody(),
				bodyCompB.getBody(),
				tmp.set(getWorldAnchorPoint()).scl(
						Lttl.game.getPhysics().scaling));
		if (!relativeLimit)
		{
			revoluteJointDefShared.referenceAngle = 0;
		}
		joint = (RevoluteJoint) Lttl.game.getPhysics().getWorld()
				.createJoint(revoluteJointDefShared);
		joint.setUserData(this);
	}

	@Override
	protected void processJointDefinition(JointDef def)
	{
		super.processJointDefinition(def);

		RevoluteJointDef revJointDef = (RevoluteJointDef) def;
		revJointDef.enableLimit = enableLimit;
		revJointDef.lowerAngle = lowerAngle * LttlMath.degreesToRadians;
		revJointDef.upperAngle = LttlMath.max(upperAngle
				* LttlMath.degreesToRadians, revJointDef.lowerAngle);

		revJointDef.enableMotor = enableMotor;
		revJointDef.motorSpeed = motorSpeed * LttlMath.degreesToRadians;
		revJointDef.maxMotorTorque = calcMaxMotorTorque();
	}

	@Override
	public void updateSettingsOnJoint()
	{
		RevoluteJoint joint = getJoint();

		joint.enableLimit(enableLimit);
		joint.setLimits(
				lowerAngle * LttlMath.degreesToRadians,
				LttlMath.max(upperAngle * LttlMath.degreesToRadians, lowerAngle
						* LttlMath.degreesToRadians));

		joint.enableMotor(enableMotor);
		joint.setMotorSpeed(motorSpeed * LttlMath.degreesToRadians);
		joint.setMaxMotorTorque(calcMaxMotorTorque());
	}

	private float calcMaxMotorTorque()
	{
		return LttlPhysicsHelper.getForce(maxMotorTorque, relativeMass,
				relativeGravity,
				(bodyB != null && bodyB.isInit()) ? bodyB.getBody() : null);
	}

	/**
	 * Use this over {@link RevoluteJoint#setMotorSpeed(float)} since it converts to radians for you.
	 * 
	 * @param degreesPerSecond
	 */
	public void setMotorSpeedDegrees(float degreesPerSecond)
	{
		RevoluteJoint joint = getJoint();
		joint.setMotorSpeed(degreesPerSecond * LttlMath.degreesToRadians);
	}

	@Override
	public void debugDraw()
	{
		if (drawAnchor)
		{
			Lttl.debug.drawCircle(getWorldAnchorPoint(), LttlDebug.RADIUS_SMALL
					* Lttl.debug.eF(), LttlHelper.tmpColor.set(0, 0, 0, .8f));
		}

		if (drawLimits && enableLimit
				&& ((relativeLimit && isInit()) || !relativeLimit))
		{
			// get world position anchor point (tmp)
			tmp.set(getWorldAnchorPoint());
			Vector2 lower = new Vector2(Lttl.game.getPhysics().angleLimitLength
					* Lttl.game.getSettings().getWidthFactor(), 0);
			Vector2 upper = new Vector2(lower);
			Vector2 other = new Vector2(lower);

			float base = t().getWorldRotation(true)
					+ (relativeLimit ? joint.getReferenceAngle()
							* LttlMath.radiansToDegrees : 0);

			lower.rotate(base + lowerAngle + angleOffsetDraw).add(tmp);
			upper.rotate(base + upperAngle + angleOffsetDraw).add(tmp);
			Lttl.debug.drawLine(tmp, lower, LttlDebug.WIDTH_MEDIUM, Color.BLUE);
			Lttl.debug
					.drawLine(tmp, upper, LttlDebug.WIDTH_MEDIUM, Color.GREEN);

			if (bodyB != null)
			{
				other.rotate(bodyB.t().getWorldRotation(true) + angleOffsetDraw)
						.add(tmp);
				Lttl.debug.drawLine(tmp, other, LttlDebug.WIDTH_MEDIUM,
						Color.ORANGE);
			}
		}
	}

	/**
	 * Vector2 is shared and should not be modified.
	 */
	private Vector2 getWorldAnchorPoint()
	{
		return useOrigin ? t().getWorldRenderPosition(true) : t()
				.getWorldPosition(true);
	}

	/**
	 * @see LttlPhysicsHelper#getReactionTorque(com.badlogic.gdx.physics.box2d.Joint)
	 */
	public float getReactionTorque()
	{
		return joint
				.getReactionTorque(1 / Lttl.game.getSettings().fixedDeltaTime);
	}

	/**
	 * @see LttlPhysicsHelper#getReactionForce(com.badlogic.gdx.physics.box2d.Joint)
	 */
	public Vector2 getReactionForce()
	{
		return joint
				.getReactionForce(1 / Lttl.game.getSettings().fixedDeltaTime);
	}
}
