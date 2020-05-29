package com.lttlgames.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.ChainShape;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.JointEdge;
import com.badlogic.gdx.physics.box2d.MassData;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Transform;
import com.badlogic.gdx.utils.Array;
import com.lttlgames.components.interfaces.PhysicsListener;
import com.lttlgames.editor.LttlTransform.GuiTransformListener;
import com.lttlgames.editor.annotations.ComponentLimitOne;
import com.lttlgames.editor.annotations.GuiButton;
import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.GuiCallbackDescendants;
import com.lttlgames.editor.annotations.GuiGroup;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.GuiReadOnly;
import com.lttlgames.editor.annotations.GuiShow;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.helpers.LttlMath;
import com.lttlgames.helpers.Vector2Array;

//21
@ComponentLimitOne
@Persist(-90128)
public class LttlPhysicsBody extends LttlPhysicsFixtureBodyBase
{
	/**
	 * any higher than this and the JNI crashes
	 */
	public final static int MAX_FIXTURES = 68;

	private Body body;

	@GuiCallback("onGuiChange")
	@Persist(9012800)
	public BodyDef.BodyType bodyType = BodyType.DynamicBody;

	@GuiGroup("Stats")
	@GuiShow
	@GuiReadOnly
	private int fixtureCount = -1;
	@GuiGroup("Stats")
	@GuiShow
	@GuiReadOnly
	private int jointCount = -1;
	@GuiGroup("Stats")
	@GuiShow
	@GuiReadOnly
	private float width = -1;
	@GuiGroup("Stats")
	@GuiShow
	@GuiReadOnly
	private float height = -1;
	@GuiGroup("Stats")
	@GuiShow
	@GuiReadOnly
	private boolean isAwakeStat = false;
	@GuiGroup("Stats")
	@GuiShow
	@GuiReadOnly
	private Vector2 linearVelocityStat;
	@GuiGroup("Stats")
	@GuiShow
	@GuiReadOnly
	private float angularVelocityStat = 0;

	@GuiGroup("General")
	@Persist(90128012)
	public boolean initActive = true;
	@GuiGroup("General")
	@GuiCallback("onGuiChange")
	@Persist(9012801)
	public boolean allowSleep = true;
	@GuiGroup("General")
	@Persist(9012802)
	public boolean initAwake = true;
	@GuiGroup("General")
	@Persist(9012803)
	public boolean initOnStart = true;

	// DYNAMIC ONLY
	@GuiGroup("Dynamic")
	@GuiCallback("onGuiChange")
	@Persist(9012804)
	public boolean fixedRotation = false;
	@GuiGroup("Dynamic")
	@GuiCallback("onGuiChange")
	@GuiMin(0)
	@Persist(9012805)
	public float angularDamping = 0;
	@GuiGroup("Dynamic")
	@GuiCallback("onGuiChange")
	@GuiMin(0)
	@Persist(9012806)
	public float linearDamping = 0;
	@GuiGroup("Dynamic")
	@GuiCallback("onGuiChange")
	@Persist(9012807)
	/**
	 * only necessary if fast moving and intersecting with dynamic bodies.
	 */
	public boolean isBullet = false;
	@GuiGroup("Dynamic")
	@GuiCallback("onGuiChange")
	@Persist(9012808)
	public float gravityScale = 1;

	// DYNAMIC AND KINEMATIC
	/**
	 * if kinematic, this is a constant velocity<br>
	 * if dynamic, it's the starting velocity
	 */
	@GuiGroup("Dynamic and Kinematic")
	@GuiCallback("onGuiChange")
	@Persist(9012809)
	public float angularVelocity = 0;
	/**
	 * if kinematic, this is a constant velocity<br>
	 * if dynamic, it's the starting velocity
	 */
	@GuiGroup("Dynamic and Kinematic")
	@GuiCallbackDescendants("onGuiChange")
	@Persist(90128010)
	public Vector2 linearVelocity = new Vector2();

	/**
	 * Before physics steps, the physics body is updated with the values from the LttlTransform. If
	 * {@link #simulateWithForces} is false, then it will appear in the physics simulation as if the object disappeared
	 * and reappeared at a new location.<br>
	 * Advice: it may be good to make the density high if it is Dynamic, otherwise it may give away from the position.<br>
	 * Note: Gravity Scaling should be set to 0 in almost all cases.<br>
	 * Note: If body is getting stuck in other bodies, try enabling {@link #simulateWithForces}.
	 */
	@GuiGroup("Transform Update Settings")
	@Persist(90128019)
	public boolean transformToBody = false;
	/**
	 * Requires {@link #transformToBody} to be true.<br>
	 * When the physics body's values are updated with the LttlTransform's values it will use
	 * {@link Body#setLinearVelocity(Vector2)} and {@link Body#setAngularVelocity(float)} to reach the transform state.
	 * {@link #linearDamping} and {@link #angularDamping} and {@link #gravityScale} should be 0 for precise results.
	 * This will simulate collisions along the way.
	 */
	@GuiGroup("Transform Update Settings")
	@Persist(90128020)
	public boolean simulateWithForces = false;
	/**
	 * after all physics steps have completed, sets the LttlTransform from the physics body object
	 */
	@GuiGroup("Transform Update Settings")
	@Persist(90128021)
	public boolean bodyToTransform = true;

	/**
	 * any mouse contact
	 */
	@GuiGroup("Callbacks")
	@Persist(90128014)
	public boolean callbackMouse = false;

	/**
	 * Enables enter and exit callbacks when colliding with a body. It also enables {@link #getTouchingBodies()} to
	 * update and cached during each physics step, instead of only when {@link #getTouchingBodies()} is called.
	 * 
	 * @see PhysicsListener#onBodyEnter(LttlPhysicsBody, LttlPhysicsBody)
	 * @see PhysicsListener#onBodyExit(LttlPhysicsBody, LttlPhysicsBody)
	 */
	@GuiGroup("Callbacks")
	@Persist(90128016)
	public boolean callbackBodyCollision = false;

	/**
	 * enables callbacks for begin and end contacts
	 */
	@GuiGroup("Callbacks")
	@Persist(90128017)
	public boolean callbackContact = false;

	/**
	 * enables callbacks for preSolve and postSolve
	 */
	@GuiGroup("Callbacks")
	@Persist(90128018)
	public boolean callbackSolve = false;

	/**
	 * any physics object is destroyed (body, fixture, or joint), it will do a callback before it is destroyed
	 */
	@GuiGroup("Callbacks")
	@Persist(90128015)
	public boolean callbackDestroyAny = false;

	/**
	 * fixtures all need to be a descendant of the body transform, since it is the only transform that is updated from
	 * physics
	 */
	@GuiCallback("onGuiChangeFixtures")
	@GuiCallbackDescendants("onGuiChangeFixtures")
	@Persist(90128011)
	final ArrayList<LttlPhysicsFixture> fixtureComps = new ArrayList<LttlPhysicsFixture>(
			0);

	@GuiGroup("Debug")
	@Persist(90128013)
	public boolean drawCenterOfMass = false;

	final protected static BodyDef bodyDef = new BodyDef();
	final static private Vector2 tmp0 = new Vector2();
	final static private Vector2 tmp1 = new Vector2();
	private GuiTransformListener guiTransformListener;
	private boolean lastUpdateTransformsAwakeState = false;
	/**
	 * holds all the contacts related to this body, all should be touching
	 */
	ArrayList<Contact> touchingContacts = new ArrayList<Contact>(1);
	boolean touchingContactsDirty = false;
	private ArrayList<LttlPhysicsBody> touchingBodyComps = new ArrayList<LttlPhysicsBody>(
			1);

	/* STATIC */
	static private Array<LttlPhysicsBase> physicsBaseList = new Array<LttlPhysicsBase>(
			false, 2);
	static private ArrayList<LttlPhysicsBody> touchingBodyCompsCache = new ArrayList<LttlPhysicsBody>(
			1);
	static private ArrayList<LttlPhysicsBody> touchingBodyCompsStart = new ArrayList<LttlPhysicsBody>(
			1);

	/**
	 * Populates {@link #fixtureComps} with all the {@link LttlPhysicsFixture} components on this transform and any
	 * descendants.
	 */
	@GuiButton
	private void populateFixtures()
	{
		fixtureComps.clear();

		fixtureComps.addAll(t().getComponentsInTree(LttlPhysicsFixture.class,
				true));

		destroyAndInit();
	}

	@Override
	public void onStart()
	{
		if (isEnabled() && initOnStart)
		{
			init();
		}
	}

	@Override
	public void onEditorStart()
	{
		if (isEnabled() && Lttl.game.getPhysics().initInEditor)
		{
			init();
		}
	}

	private void addGuiTransformListenerNonPlaying()
	{
		// don't even worry about any of this if not in editor
		if (!Lttl.game.inEditor()) return;

		// if already exists, probably some error with destroying
		Lttl.Throw(guiTransformListener != null);

		// always add listener if in editor, even if playing, since physics could be enabled or not in runtime
		// when the Physics Body listener is called back, it always destroy and inits everything (body, joints, and
		// fixtures), so the listeners on fixtures will be removed and not called back
		t().addGuiListener(
				guiTransformListener = new GuiTransformListener(true)
				{
					private void doo()
					{
						// don't allow transform to change physics body when playing with physics enabled or not
						// initialized yet
						if (!isInit()
								|| (Lttl.game.getPhysics().enabled && Lttl.game
										.isPlaying())) return;

						// Just always destroy and init because it ensures joints and fixtures are correct, especially
						// when there are physics bodies as descendants
						destroyAndInit();
					}

					@Override
					public void onScale(boolean handle,
							LttlTransform sourceTransform)
					{
						doo();
					}

					@Override
					public void onRotation(boolean handle,
							LttlTransform sourceTransform)
					{
						doo();
					}

					@Override
					public void onPosition(boolean handle,
							LttlTransform ancestorChange)
					{
						doo();
					}

					@Override
					public void onOrigin(LttlTransform sourceTransform)
					{
						doo();
					}
				});
	}

	public void destroyAndInit()
	{
		if (isInit())
		{
			// set the joints to reinit in the earlyUpdate
			for (JointEdge j : body.getJointList())
			{
				LttlPhysicsJointBase<?> jointComp = LttlPhysicsHelper
						.getJointComp(j.joint);
				if (jointComp == null) continue;
				jointComp.initNextEarlyUpdate();
			}
		}
		destroyPhysics();
		init();
	}

	/**
	 * This allows adding and removing fixtures automatically during editor while playing
	 */
	@SuppressWarnings("unused")
	private void onGuiChangeFixtures()
	{
		if (!isInit()) return;
		destroyAndInit();
	}

	/**
	 * This allows chaning settings while game is playing through the editor
	 */
	@SuppressWarnings("unused")
	private void onGuiChange()
	{
		updateSettingsOnBody();
	}

	/**
	 * all settings on body will be updated on actual physics body, if initialized<br>
	 * it seems to be more efficient to not check if the values are different.
	 */
	public void updateSettingsOnBody()
	{
		if (!isInit()) return;

		// general
		// if (getBody().getType() != bodyType)
		getBody().setType(bodyType);
		// if (getBody().isSleepingAllowed() != allowSleep)
		getBody().setSleepingAllowed(allowSleep);

		// DYNAMIC
		if (bodyType == BodyType.DynamicBody)
		{
			// if (getBody().getAngularDamping() != angularDamping)
			getBody().setAngularDamping(angularDamping);
			// if (getBody().getLinearDamping() != linearDamping)
			getBody().setLinearDamping(linearDamping);
			// if (getBody().isBullet() != isBullet)
			getBody().setBullet(isBullet);
			// if (getBody().isFixedRotation() != fixedRotation)
			getBody().setFixedRotation(fixedRotation);
			// if (getBody().getGravityScale() != gravityScale)
			getBody().setGravityScale(gravityScale);
			// if (getBody().getAngularVelocity() != angularVelocity)
			getBody().setAngularVelocity(angularVelocity);
			// if (!getBody().getLinearVelocity().equals(linearVelocity)) should use epsilon
			getBody().setLinearVelocity(linearVelocity);
		}
		else
		{
			// non dynamic defaults
			// if (getBody().getAngularDamping() != 0)
			getBody().setAngularDamping(0);
			// if (getBody().getLinearDamping() != 0)
			getBody().setLinearDamping(0);
			// if (getBody().isBullet() != false)
			getBody().setBullet(false);
			// if (getBody().isFixedRotation() != false)
			getBody().setFixedRotation(false);
			// if (getBody().getGravityScale() != 1)
			getBody().setGravityScale(1);
		}

		// KINEMATIC or DYNAMIC
		if (bodyType == BodyType.KinematicBody
				|| bodyType == BodyType.DynamicBody)
		{
			// if (getBody().getAngularVelocity() != angularVelocity)
			getBody().setAngularVelocity(angularVelocity);
			// if (!getBody().getLinearVelocity().equals(linearVelocity))
			getBody().setLinearVelocity(linearVelocity);
		}
		// STATIC
		else
		{
			// static defaults
			// if (getBody().getAngularVelocity() != 0)
			getBody().setAngularVelocity(0);
			// if (!getBody().getLinearVelocity().equals(Vector2.Zero))
			getBody().setLinearVelocity(0, 0);
		}
	}

	/**
	 * Initializes the physics body. If {@link PhysicsController#isStepping()} will add to queue to init at end of
	 * current step and after any queued destroys have been processed.<br>
	 * Will throw error if already initialized, instead call {@link #destroyAndInit()}.
	 */
	@GuiButton(order = 2)
	public void init()
	{
		// body should be destroyed before initing a new one
		Lttl.Throw(isInit());

		// check if there are any fixtures
		if (fixtureComps.size() == 0)
		{
			Lttl.logNote("Could not init LttlPhysicsBody " + toString()
					+ " because no fixtures set.");
			return;
		}

		addGuiTransformListenerNonPlaying();

		t().updateWorldValues();

		// ALL
		bodyDef.active = initActive;
		bodyDef.type = bodyType;
		bodyDef.allowSleep = allowSleep;
		bodyDef.awake = initAwake;
		bodyDef.angle = LttlMath.degreesToRadians * t().getWorldRotation(false);
		bodyDef.position.set(t().getWorldPosition(false));
		bodyDef.position.x *= Lttl.game.getPhysics().scaling;
		bodyDef.position.y *= Lttl.game.getPhysics().scaling;

		// DYNAMIC
		if (bodyType == BodyType.DynamicBody)
		{
			// user specified
			bodyDef.angularDamping = angularDamping;
			bodyDef.linearDamping = linearDamping;
			bodyDef.bullet = isBullet;
			bodyDef.fixedRotation = fixedRotation;
			bodyDef.gravityScale = gravityScale;
		}
		else
		{
			// defaults
			bodyDef.angularDamping = 0;
			bodyDef.linearDamping = 0;
			bodyDef.bullet = false;
			bodyDef.fixedRotation = false;
			bodyDef.gravityScale = 1;
		}

		// KINEMATIC or DYNAMIC
		if (bodyType == BodyType.DynamicBody
				|| bodyType == BodyType.KinematicBody)
		{
			// user specified
			bodyDef.angularVelocity = angularVelocity;
			bodyDef.linearVelocity.set(linearVelocity);
		}
		else
		{
			// defaults
			bodyDef.angularVelocity = 0;
			bodyDef.linearVelocity.set(0, 0);
		}

		// create body
		body = Lttl.game.getPhysics().getWorld().createBody(bodyDef);

		// link component to physics body
		body.setUserData(this);

		// add fixtures

		// force body to have a rotation of 0, so when adding fixtures, it does not include this rotation
		float originalWorldRotation = t().getWorldRotation(true);
		if (originalWorldRotation != 0)
		{
			t().setWorldRotation(0);
			t().updateWorldValuesTree();
		}
		int fixCompsAdded = 0;
		for (LttlPhysicsFixture fixComp : fixtureComps)
		{
			if (fixComp == null || !fixComp.isEnabled()) continue;
			fixComp.init(this);
			if (fixComp.getFixtureCount() == 0)
			{
				// revert rotation since ending early
				if (originalWorldRotation != 0)
				{
					t().rotation = originalWorldRotation;
					t().updateWorldValuesTree();
				}
				Lttl.logNote("Unable to create LttlPhysicsBody "
						+ toString()
						+ " because one ore more enabled LttlPhsyicsFixtures failed to be created with any fixtures.");
				destroyPhysics();
				return;
			}
			fixCompsAdded++;
		}
		if (fixCompsAdded == 0)
		{
			Lttl.logNote("Unable to create LttlPhysicsBody "
					+ toString()
					+ " because no LttlPhysicsFixtures were added, some may be disabled.");
			destroyPhysics();
			return;
		}
		// revert rotation since done
		if (originalWorldRotation != 0)
		{
			t().setWorldRotation(originalWorldRotation);
			t().updateWorldValuesTree();
		}

		fixtureCount = body.getFixtureList().size;

		// if in editor calculate the width of the physics body
		if (Lttl.game.inEditor() && Lttl.game.getPhysics().checkBodyDimensions)
		{
			Rectangle rect = getAABB(new Rectangle());
			width = rect.width;
			height = rect.height;

			float max = bodyType == BodyType.StaticBody ? 50 : 10;
			if (!LttlMath.isBetween(width, .1f, max)
					|| !LttlMath.isBetween(height, .1f, max))
			{
				Lttl.logNote(toString() + " width and/or height (" + width
						+ ", " + height + ") is out of range (.1 - " + max
						+ "), consider scaling.");
			}
		}
	}

	@Override
	public void debugDraw()
	{
		if (isInit() && drawCenterOfMass)
		{
			MassData md = getBody().getMassData();
			Vector2 v = new Vector2(md.center).rotateRad(getBody().getAngle())
					.add(getBody().getPosition())
					.scl(1 / Lttl.game.getPhysics().scaling);
			Lttl.debug.drawCircle(v, LttlDebug.RADIUS_SMALL * Lttl.debug.eF(),
					Lttl.game.getPhysics().centerOfMass);
		}
	}

	/**
	 * calculates the width and height of the body (not rotated)
	 */
	private Rectangle getAABB(Rectangle rect)
	{
		// TODO is there a better way to get a AABB for a body or the dimensions of a body, google

		Vector2Array array = new Vector2Array();

		for (Fixture f : body.getFixtureList())
		{
			switch (f.getType())
			{
				case Chain:
				{
					ChainShape shape = (ChainShape) f.getShape();
					// TODO
					Lttl.Throw();
				}
					break;
				case Circle:
				{
					CircleShape shape = (CircleShape) f.getShape();
					Vector2 pos = shape.getPosition();
					float radius = shape.getRadius();
					array.add(pos.x - radius, pos.y - radius);
					array.add(pos.x - radius, pos.y + radius);
					array.add(pos.x + radius, pos.y + radius);
					array.add(pos.x + radius, pos.y - radius);
				}
					break;
				case Edge:
				{
					EdgeShape shape = (EdgeShape) f.getShape();
					// TODO
					Lttl.Throw();
				}
					break;
				case Polygon:
				{
					PolygonShape shape = (PolygonShape) f.getShape();
					for (int i = 0, n = shape.getVertexCount(); i < n; i++)
					{
						shape.getVertex(i, tmp0);
						array.add(tmp0);
					}
				}
					break;
			}
		}

		return LttlMath.getRect(rect, array.toArray());
	}

	@GuiButton
	private void activateToggle()
	{
		if (!isInit()) return;
		body.setActive(!body.isActive());
	}

	@Override
	public void destroyPhysics()
	{
		if (!isInit()) return;

		// get listeners once
		ArrayList<PhysicsListener> listeners = t().getComponentsInTree(
				PhysicsListener.class, true);

		processDestroyCallbacks(this, listeners);

		// EFFICIENCY
		// does not run destroyPhysics on each fixture and joint because it's more efficient to just delete the
		// entire body, but still need to clean them up and process onDestroyPhysics callbacks for each

		// clean up and process onDestroyPhysics callbacks
		// fixtures
		physicsBaseList.clear();
		for (Fixture f : body.getFixtureList())
		{
			// clean up each fixtureComp only once, since there could be multiple fixtures all pointing to the same
			// fixture component
			LttlPhysicsFixture fixtureComp = LttlPhysicsHelper
					.getFixtureComp(f);
			if (fixtureComp == null) continue;
			if (!physicsBaseList.contains(fixtureComp, true))
			{
				// callbacks
				processDestroyCallbacks(fixtureComp, listeners);

				// make the fixture comp appear to be not initialized, even though it has not technically been destroyed
				// from the physics world yet, will when physics body gets destroyed below
				// clearn up after callbacks, so the callbacks can still access the physics object
				fixtureComp.cleanUp();

				// prevent duplicate processing
				physicsBaseList.add(fixtureComp);
			}
		}
		physicsBaseList.clear();

		// clean up and process onDestroyPhysics callbacks
		// joints
		for (JointEdge j : body.getJointList())
		{
			LttlPhysicsJointBase<?> jointComp = LttlPhysicsHelper
					.getJointComp(j.joint);
			if (jointComp == null) continue;

			// process callbacks on this body and the other body since it will be destroyed from that body automatically
			// when it's destroyed here
			processDestroyCallbacks(jointComp, listeners);
			LttlPhysicsHelper.getBodyComp(j.other).processDestroyCallbacks(
					jointComp, listeners);

			jointComp.cleanUp();
		}

		// need to clean up before destroying in physics world, so other callbacks will know it has been destroyed, and
		// so user loses all ability to manipulate the physics object, since once it is destroyed below, it's gone
		Body bodySaved = body;
		cleanUp();

		// destroy body on physics world, which destroys the physics objects of fixtures and joints too
		Lttl.game.getPhysics().getWorld().destroyBody(bodySaved);
	}

	@Override
	void cleanUp()
	{
		if (Lttl.game.inEditor() && guiTransformListener != null)
		{
			t().removeGuiListener(guiTransformListener);
			guiTransformListener = null;
		}
		body = null;
	}

	/**
	 * Returns the physics body so you can modify it's properties.<br>
	 * <b>SHOULD ONLY BE USED AFTER BODY CREATION</b>, since it is more expensive to use the body's set methods
	 * 
	 * @return
	 */
	public Body getBody()
	{
		return body;
	}

	/**
	 * Should never need to call, since after all the steps, it is automatically called on all bodies that have
	 * {@link #bodyToTransform} enabled.<br>
	 * Update's the transform (position and rotation) based on the physics body (automatically done after physics
	 * finishes stepping)
	 */
	public void bodyToTransform()
	{
		// when called from LoopManager, this component and it's transform can assumed to be enabled

		// don't update if it's not active
		if (!isInit() || !getBody().isActive()) return;

		// check if awake changed and is awake
		// if was sleeping last update and sleeping this update, probably safe to not update transforms
		boolean currentAwakeState = getBody().isAwake();
		if (!lastUpdateTransformsAwakeState && !currentAwakeState) return;
		lastUpdateTransformsAwakeState = currentAwakeState;

		// update THAT transform
		t().setWorldPosition(getBodyWorldPosition(tmp0));
		t().setWorldRotation(getBodyWorldRotation());
	}

	public Vector2 getBodyWorldPosition(Vector2 container)
	{
		Vector2 pos = getBody().getPosition();
		return container.set(pos.x / Lttl.game.getPhysics().scaling, pos.y
				/ Lttl.game.getPhysics().scaling);
	}

	public float getBodyWorldRotation()
	{
		return LttlMath.radiansToDegrees * getBody().getAngle();
	}

	/**
	 * This should never be needed to call since if a {@link #transformToBody} is enabled, then it will automatically do
	 * this method before each step.<br>
	 * Update the body's transform to the transform component (position and rotation) only if there is a change, does
	 * not apply any forces or anything. Always runs {@link LttlTransform#updateWorldValues()}.
	 * 
	 * @return if change was found
	 */
	public boolean transformToBody()
	{
		if (!isInit()) return false;

		// update world values
		t().updateWorldValues();

		// calculate new physics transform values
		float newX = t().getWorldPosition(false).x
				* Lttl.game.getPhysics().scaling;
		float newY = t().getWorldPosition(false).y
				* Lttl.game.getPhysics().scaling;
		float newRot = LttlMath.degreesToRadians * t().getWorldRotation(false);

		// get current physics transform values and check for change
		Transform transform = getBody().getTransform();
		Vector2 pos = transform.getPosition();
		if (!LttlMath.isEqual(pos.x, newX) || !LttlMath.isEqual(pos.y, newY)
				|| !LttlMath.isEqual(transform.getRotation(), newRot))
		{
			// change found, so set transform and return true
			getBody().setTransform(newX, newY, newRot);
			// need to clear angular and linear velocity otherwise it'll keep on moving
			getBody().setLinearVelocity(0, 0);
			getBody().setAngularVelocity(0);
			if (getBody().getGravityScale() > 0)
			{
				Lttl.logNote("Gravity exists on a physics body that is transformToBody, best to set gravity scale to 1. "
						+ toString());
			}
			return true;
		}

		// no changes found, do nothing
		return false;
	}

	@Override
	public void onEditorDestroyComp()
	{
		destroyPhysics();
	}

	@Override
	public void onDestroyComp()
	{
		destroyPhysics();
	}

	@Override
	public void onEditorEnable()
	{
		processEditorEnableChange();
	}

	@Override
	public void onEditorDisable()
	{
		processEditorEnableChange();
	}

	private void processEditorEnableChange()
	{
		// in editor while not playing, if playing need to do it manually
		if (!Lttl.game.isPlaying())
		{
			// if the body was just enabled (body should not exist yet, so create it)
			if (isEnabled())
			{
				init();
			}
			// if the body was just disabled and a body exists, then destroy it
			else if (isInit())
			{
				destroyPhysics();
			}
		}
	}

	@GuiButton
	private void awake()
	{
		if (!isInit()) return;

		getBody().setAwake(true);
	}

	@Override
	public boolean isInit()
	{
		return body != null;
	}

	@Override
	public Fixture getFixtureContains(float x, float y)
	{
		if (!isInit()) return null;

		x *= Lttl.game.getPhysics().scaling;
		y *= Lttl.game.getPhysics().scaling;

		// best to use the actual physics list not the component list since it could have been modified since physics
		// objects were initially created
		for (Fixture f : body.getFixtureList())
		{
			if (f.testPoint(x, y)) { return f; }
		}
		return null;
	}

	@Override
	public int getFixtureCount()
	{
		return getBody().getFixtureList().size;
	}

	/* STATIC METHODS */

	/**
	 * Get the {@link LttlPhysicsBody} for this {@link Body}
	 */
	public static LttlPhysicsBody getComp(Body body)
	{
		return LttlPhysicsHelper.getBodyComp(body);
	}

	/**
	 * Get the {@link LttlPhysicsBody} for this {@link Fixture}
	 */
	public static LttlPhysicsBody getComp(Fixture fixture)
	{
		return LttlPhysicsHelper.getBodyComp(fixture);
	}

	/**
	 * checks if should do a destroy callback (if playing and {@link #callbackDestroyAny} is true and then calls back on
	 * this body component's tree
	 * 
	 * @param base
	 *            the object being destroyed
	 * @param listeners
	 */
	final void processDestroyCallbacks(LttlPhysicsBase base,
			ArrayList<PhysicsListener> listeners)
	{
		// check if should do a destroy callback
		if (Lttl.game.isPlaying() && callbackDestroyAny)
		{
			// callbacks for when physics object is destroyed, called back before the physics object is removed from the
			// physics world
			for (PhysicsListener listener : listeners)
			{
				listener.onDestroyPhysics(base);
			}
		}
	}

	/**
	 * @see #processDestroyCallbacks(LttlPhysicsBase, ArrayList)
	 */
	final void processDestroyCallbacks(LttlPhysicsBase base)
	{
		processDestroyCallbacks(base,
				t().getComponentsInTree(PhysicsListener.class, true));
	}

	@Override
	public LttlPhysicsBody getBodyComp()
	{
		return this;
	}

	/**
	 * Gets all the touching and enabled contacts that are related to this body component and the otherBodyComp.
	 * 
	 * @param otherBodyComp
	 * @param outputContacts
	 */
	public void getContacts(LttlPhysicsBody otherBodyComp,
			Array<Contact> outputContacts)
	{
		outputContacts.clear();
		for (Contact c : touchingContacts)
		{
			// if contact is disabled, or not related to the otherBodyComp, then don't add it
			// TODO not sure if necessary to check if contact isEnabled, since it may always be enabled
			// !c.isEnabled()
			if (LttlPhysicsHelper.getIndexFixture(c, otherBodyComp) == -1)
				continue;

			outputContacts.add(c);
		}
	}

	/**
	 * Gets all the contacts that are related to this body component regardless without checking if touching or enabled.
	 * (no allocation). May not return contacts this body component or one it was contacting was just destroyed.
	 * 
	 * @return
	 */
	public List<Contact> getContacts()
	{
		return Collections.unmodifiableList(touchingContacts);
	}

	/**
	 * populates the {@link #touchingBodyComps} based on {@link #touchingContacts}, and does any enter or exit callbacks
	 * if {@link #callbackBodyCollision} is true.
	 */
	void processBodyCollisionsUpdate()
	{
		// need to cache the original touching body components to know which are new touches, and which bodies have
		// stopped touching, only necessary if doing callback body collisiosn
		if (callbackBodyCollision)
		{
			touchingBodyCompsCache.clear();
			touchingBodyCompsCache.addAll(touchingBodyComps);
			touchingBodyCompsStart.clear();
		}

		// go through contacts to find which bodies are touching
		touchingBodyComps.clear();
		// all contacts are touching
		for (Contact c : touchingContacts)
		{
			// skip contact if not enabled (probably because of a preSolve callback)
			if (!c.isEnabled()) continue;

			// get the other body component involved in this contact
			LttlPhysicsBody otherBodyComp = LttlPhysicsHelper.getOtherBodyComp(
					this, c);

			// if no other body component could be found, it's probably because either this or the other component
			// is destroyed
			if (otherBodyComp == null) continue;

			// if this is the first contact found that is touching this other body component
			// otherwise it will skip duplicate contacts that are for the same other body component
			if (!touchingBodyComps.contains(otherBodyComp))
			{
				// then add it to touchingBodyComponents list, preventing duplicates, and populating touching body
				// comps list
				touchingBodyComps.add(otherBodyComp);

				// if doing callback collisions, then remove from cached list and populate the start list
				if (callbackBodyCollision)
				{
					// if it was already touching, then just remove it from the cache
					boolean wasTouching = touchingBodyCompsCache
							.remove(otherBodyComp);

					if (!wasTouching)
					{
						// if it wasn't touching before, but now is, then it is the first time, so add to start list
						// to callback after done populating all touching body components
						touchingBodyCompsStart.add(otherBodyComp);
					}
				}
			}
		}

		// process callback on body collision if at least one of the lists (start or end touching) are greater than 0
		if (callbackBodyCollision
				&& (touchingBodyCompsStart.size() > 0 || touchingBodyCompsCache
						.size() > 0))
		{
			ArrayList<PhysicsListener> listeners = t().getComponentsInTree(
					PhysicsListener.class, true);

			// callback onBodyEnter
			for (LttlPhysicsBody startTouchingBodyComp : touchingBodyCompsStart)
			{
				for (PhysicsListener listener : listeners)
				{
					// skip if not enabled
					if (!((LttlComponent) listener).isEnabled()) continue;

					listener.onBodyEnter(this, startTouchingBodyComp);
				}
			}
			touchingBodyCompsStart.clear();

			// callback onBodyExit
			for (LttlPhysicsBody stopTouchingBodyComp : touchingBodyCompsCache)
			{
				for (PhysicsListener listener : listeners)
				{
					// skip if not enabled
					if (!((LttlComponent) listener).isEnabled()) continue;

					listener.onBodyExit(this, stopTouchingBodyComp);
				}
			}
			touchingBodyCompsCache.clear();
		}

		touchingContactsDirty = false;
	}

	/**
	 * Returns a list of all the bodies that are touching this body.<br>
	 * IF {@link #callbackBodyCollision} is enabled, then will return unmodifiable list that will be updated
	 * automatically during each step. This is most efficient if going to be calling this method often, since it checks
	 * for changes as they happen and caches the result.<br>
	 * IF {@link #callbackBodyCollision} is disabled, then will return a new list (allocation) that will be updated each
	 * time this method is called with all the touching bodies, not cached.
	 * 
	 * @return
	 */
	public List<LttlPhysicsBody> getTouchingBodies()
	{
		if (callbackBodyCollision)
		{
			return Collections.unmodifiableList(touchingBodyComps);
		}
		else
		{
			// update touching body comps, but won't do any callbacks
			processBodyCollisionsUpdate();

			// make into another list, the reason is because if a LttlPhysicsBody is destroyed when it was touching,
			// then it will not automatically remove from the touching list, unlike how it would if
			// callbackBodyCollision
			// was enabled. So this allows touchingBodyComps to stay clean

			// since updating touching bodies each step could be kind of ineficient for a world with lots of physics
			// bodies, I want to keep the auto touching bodies update functionality of callbackBodyCollision seperate,
			// so it can be disabled for some cases

			ArrayList<LttlPhysicsBody> otherList = new ArrayList<LttlPhysicsBody>(
					touchingBodyComps);
			touchingBodyComps.clear();
			return otherList;
		}
	}

	public boolean isTouching(LttlPhysicsBody otherBodyComp)
	{
		return touchingBodyComps.contains(otherBodyComp);
	}

	@Override
	public Rectangle getAABB()
	{
		if (!isInit()) return null;

		aabbTempArray.clear();
		for (Fixture f : body.getFixtureList())
		{
			LttlPhysicsHelper.getShapePoints(f.getShape(), aabbTempArray);
		}

		return aabbTempArray.getAABB(aabb);
	}

	@Override
	public void onFixedUpdate()
	{
		if (transformToBody && Lttl.game.getPhysics().enabled && isInit())
		{
			if (simulateWithForces && getBody().isActive())
			{
				t().updateWorldValues();

				// position
				// check if physics body and LttlTransform's position is different
				if (!LttlMath.isEqual(getBodyWorldPosition(tmp0), t()
						.getWorldPosition(false)))
				{
					// calculate linear velocity needed to get to position in the next step
					tmp1.set(t().getWorldPosition(false)).sub(tmp0)
							.scl(Lttl.game.getPhysics().scaling)
							.scl(1 / Lttl.game.getSettings().fixedDeltaTime);
				}
				else
				{
					// hault any velocity
					tmp1.setZero();
				}
				getBody().setLinearVelocity(tmp1);

				// rotation
				float angularVelocity;
				float worldBodyRot = getBodyWorldRotation();
				if (!LttlMath
						.isEqual(worldBodyRot, t().getWorldRotation(false)))
				{
					angularVelocity = (t().getWorldRotation(false) - worldBodyRot)
							* LttlMath.degreesToRadians
							* (1 / Lttl.game.getSettings().fixedDeltaTime);
				}
				else
				{
					angularVelocity = 0;
				}
				getBody().setAngularVelocity(angularVelocity);
			}
			else
			{
				transformToBody();
			}
		}
	}

	@Override
	public void onLateFixedUpdate()
	{
		// update stats
		if (Lttl.game.inEditor() && isInit() && t().isFocusedInEditor())
		{
			if (linearVelocityStat == null) linearVelocityStat = new Vector2();
			linearVelocityStat.set(getBody().getLinearVelocity());
			angularVelocityStat = getBody().getAngularVelocity();
			isAwakeStat = getBody().isAwake();
			jointCount = getBody().getJointList().size;
		}

		if (Lttl.game.getPhysics().shouldUpdateBodyToTransform() && isInit()
				&& bodyToTransform && isEnabled())
		{
			bodyToTransform();
		}
	}

	@GuiButton(order = 99)
	private void setAllFixturesSensor()
	{
		for (LttlPhysicsFixture fc : fixtureComps)
		{
			fc.isSensor = true;
		}
	}

	@GuiButton(order = 100)
	private void setAllFixturesNotSensor()
	{
		for (LttlPhysicsFixture fc : fixtureComps)
		{
			fc.isSensor = false;
		}
	}
}
