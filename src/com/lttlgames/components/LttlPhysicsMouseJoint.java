package com.lttlgames.components;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.joints.MouseJoint;
import com.badlogic.gdx.physics.box2d.joints.MouseJointDef;
import com.lttlgames.editor.Lttl;
import com.lttlgames.editor.LttlPhysicsBody;
import com.lttlgames.editor.LttlPhysicsFixture;
import com.lttlgames.editor.LttlPhysicsHelper;
import com.lttlgames.editor.PhysicsController;
import com.lttlgames.editor.annotations.ComponentRequired;
import com.lttlgames.editor.annotations.GuiCanNull;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.helpers.LttlMath;

@Persist(-906)
@ComponentRequired(LttlPhysicsBody.class)
public class LttlPhysicsMouseJoint extends LttlMouseAdapter
{
	static private MouseJointDef mouseJointDef = new MouseJointDef();
	static private Vector2 tmp = new Vector2();

	/**
	 * if null, then uses {@link PhysicsController#mouseJointSettings}.
	 */
	@GuiCanNull
	@Persist(90600)
	public MouseJointSettings settings;

	private boolean originalBullet;
	private boolean wasAutoSetup = false;

	private MouseJoint mouseJoint;
	private Body bodyB;

	@Override
	public void onEditorCreate()
	{
		// set mouse callback on body automatically
		t().getComponent(LttlPhysicsBody.class, true).callbackMouse = true;
	}

	@Override
	public void onMouseContactBody(LttlPhysicsBody bodyComp,
			LttlPhysicsFixture fixtureComp, int zIndex)
	{
		if (!getMouseJointSettings().autoStartWhenBodyClicked) return;

		// if already dragging, then don't care about making a new mouse joint
		if (isMouseDragging()) return;

		// if not this transform, then skip
		if (bodyComp.t() != t()) return;

		// check if should create the mouse joint
		// skip if not closest object or if mouse was not pressed this frame
		if (zIndex > 0 || !Lttl.input.isMousePressed()) return;

		// create mouse joint at mouse position
		wasAutoSetup = true;
		Vector2 mousePos = Lttl.input
				.getMousePos(getMouseJointSettings().clippedViewportOnly);
		initInternal(bodyComp.getBody(), mousePos.x, mousePos.y);
	}

	private MouseJointSettings getMouseJointSettings()
	{
		return settings == null ? Lttl.game.getPhysics().mouseJointSettings
				: settings;
	}

	/**
	 * @see #init(float, float)
	 */
	public void init(Vector2 worldAnchorPos)
	{
		init(worldAnchorPos.x, worldAnchorPos.y);
	}

	/**
	 * Sets up the mouse dragging at the anchor point provided (in world game units, not physics units). Must not be
	 * {@link #isMouseDragging()}, so check before hand and {{@link #destroyPhysics()} if is.
	 * 
	 * @param worldAnchorPosX
	 * @param worldAnchorPosY
	 */
	public void init(float worldAnchorPosX, float worldAnchorPosY)
	{
		LttlPhysicsBody bodyComp = getBodyComp();
		if (bodyComp == null || !bodyComp.isInit()) return;
		initInternal(bodyComp.getBody(), worldAnchorPosX, worldAnchorPosY);
	}

	private void initInternal(Body body, float worldAnchorPosX,
			float worldAnchorPosY)
	{
		// can't setup while dragging, destroy first
		Lttl.Throw(isMouseDragging());

		// must be dynamic body
		Lttl.Throw(body.getType() != BodyType.DynamicBody);

		MouseJointSettings settingsActual = getMouseJointSettings();

		mouseJointDef.bodyA = Lttl.game.getPhysics().getStaticBody();
		mouseJointDef.bodyB = bodyB = body;
		mouseJointDef.dampingRatio = settingsActual.dampingRatio;
		mouseJointDef.frequencyHz = settingsActual.frequencyHz;
		mouseJointDef.maxForce = LttlPhysicsHelper.getForce(
				settingsActual.maxForceMultiplier, settingsActual.relativeMass,
				settingsActual.relativeGravity, bodyB);
		mouseJointDef.target.set(worldAnchorPosX, worldAnchorPosY).scl(
				Lttl.game.getPhysics().scaling);
		mouseJoint = (MouseJoint) Lttl.game.getPhysics().getWorld()
				.createJoint(mouseJointDef);

		if (settingsActual.autoBullet)
		{
			originalBullet = body.isBullet();
			if (!originalBullet)
			{
				body.setBullet(true);
			}
		}
	}

	@Override
	public void onEarlyUpdate()
	{
		// Before each physics step, if mouse joint exists, check if should destrou mouse jouint if mouse released, if
		// not then set target to mouse position
		if (!isMouseDragging()) return;

		// check if body is destroyed, if so then it can be assumed that the mouse joint is destroyed too
		if (!bodyCompInit())
		{
			// just need to clean up reference
			cleanUp();
			return;
		}

		// if mouse is released, then destroy mouse joint and clean up
		if (Lttl.input.isMouseReleased() && wasAutoSetup)
		{
			destroyMouseJoint();
			return;
		}

		MouseJointSettings settingsActual = getMouseJointSettings();

		// set target to mouse position
		tmp.set(Lttl.input.getMousePos(settingsActual.clippedViewportOnly))
				.scl(Lttl.game.getPhysics().scaling);
		mouseJoint.setTarget(tmp);

		// joint friction
		float angularVelocity = 0;
		if (settingsActual.jointFriction > 0
				&& !LttlMath.isZero(angularVelocity = bodyB
						.getAngularVelocity()))
		{
			bodyB.setAngularVelocity(LttlMath.sign(angularVelocity)
					* LttlMath.max(
							0,
							LttlMath.abs(angularVelocity)
									- (settingsActual.jointFriction * Lttl.game
											.getSettings().fixedDeltaTime)));
		}
	}

	private boolean bodyCompInit()
	{
		return LttlPhysicsHelper.getBodyComp(bodyB) != null
				&& LttlPhysicsHelper.getBodyComp(bodyB).isInit();
	}

	/**
	 * Will destoy mouse joint and stop dragging. This will be called automatically if
	 * {@link MouseJointSettings#autoStartWhenBodyClicked}.
	 */
	public void destroyMouseJoint()
	{
		if (!isMouseDragging()) return;

		Lttl.game.getPhysics().getWorld().destroyJoint(mouseJoint);
		if (getMouseJointSettings().autoBullet && !originalBullet
				&& bodyCompInit())
		{
			bodyB.setBullet(false);
		}
		cleanUp();
	}

	/**
	 * clean up references after the mouse joint is destroyed
	 */
	private void cleanUp()
	{
		wasAutoSetup = false;
		bodyB = null;
		mouseJoint = null;
	}

	/**
	 * Returns if there is a mouse joint on this physics body, not percisely if the mouse is moving as well.
	 */
	public boolean isMouseDragging()
	{
		return mouseJoint != null;
	}

	public MouseJoint getMouseJoint()
	{
		return mouseJoint;
	}

	private LttlPhysicsBody getBodyComp()
	{
		return t().getComponent(LttlPhysicsBody.class, true);
	}
}
