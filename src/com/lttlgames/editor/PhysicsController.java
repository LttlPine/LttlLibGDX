package com.lttlgames.editor;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.ChainShape;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactFilter;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.QueryCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.Pool;
import com.lttlgames.components.MouseJointSettings;
import com.lttlgames.components.interfaces.MouseListener;
import com.lttlgames.components.interfaces.PhysicsListener;
import com.lttlgames.editor.annotations.GuiButton;
import com.lttlgames.editor.annotations.GuiCallbackDescendants;
import com.lttlgames.editor.annotations.GuiGroup;
import com.lttlgames.editor.annotations.GuiMax;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.GuiReadOnly;
import com.lttlgames.editor.annotations.GuiShow;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.helpers.LttlClosure;
import com.lttlgames.helpers.LttlHelper;

//15
@Persist(-9092)
public class PhysicsController implements ContactListener
{
	static private float ZERO_STEP = .0000000000001f;

	@Persist(909202)
	public boolean enabled = true;
	@Persist(909207)
	@GuiMin(0)
	public float scaling = 1;

	@GuiCallbackDescendants("onGuiGravity")
	@Persist(909200)
	public Vector2 gravity = new Vector2(0, -10);
	@Persist(909201)
	public boolean doSleep = true;
	@GuiGroup("Step")
	@GuiMin(1)
	@Persist(909203)
	public int velocityIterations = 6;
	@GuiGroup("Step")
	@GuiMin(1)
	@Persist(909204)
	public int positionIterations = 2;
	/**
	 * This automatically runs {@link #stepZero()} if any bodies are found to be using
	 * {@link LttlPhysicsBody#transformToBody} on a frame where a step was not called automatically.<br>
	 * If this is disabled, bodies with {@link LttlPhysicsBody#transformToBody} will just update during the next real
	 * physics step, so only necessary if need to guarantee a collision result on the same frame that you moved the
	 * LttlTransform.
	 */
	@GuiGroup("Step")
	@Persist(9092013)
	public boolean enableZeroStep = false;

	/**
	 * default settings for mouse joints
	 */
	@Persist(9092015)
	public MouseJointSettings mouseJointSettings = new MouseJointSettings();

	@Persist(909205)
	@GuiGroup("Debug")
	public boolean debugDraw = true;
	@Persist(9092012)
	@GuiGroup("Debug")
	public boolean debugDrawPlay = false;
	/**
	 * helps seephysic debug draws by making everything seem lighter
	 */
	@Persist(9092010)
	@GuiGroup("Debug")
	@GuiMin(0)
	@GuiMax(1)
	public float fadeToWhite = 0;
	/**
	 * only checks body dimensions in editor, and if beyond range, gives error
	 */
	@Persist(909206)
	@GuiGroup("Debug")
	public boolean checkBodyDimensions = true;
	@Persist(909208)
	@GuiGroup("Debug")
	public boolean initInEditor = true;

	@Persist(9092014)
	@GuiGroup("Debug")
	public float angleLimitLength = 5;

	@Persist(909209)
	@GuiGroup(
	{ "Debug", "Color" })
	public Color centerOfMass = new Color(Color.ORANGE);

	@Persist(9092011)
	@GuiGroup("Stats")
	public boolean enableStats = false;
	@GuiGroup("Stats")
	@GuiShow
	@GuiReadOnly
	private int bodyCount = -1;
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
	private int contactCount = -1;

	private World world;
	private Box2DDebugRenderer debugRenderer;
	private Matrix4 debugMatrix;

	private IntArray mouseContainedIds = new IntArray(1);
	private IntArray mouseContainedIdsLast = new IntArray(1);
	/**
	 * holds all non zero steps
	 */
	private int stepCount = 0;
	private Array<LttlPhysicsFixture> fixtureCompsArray = new Array<LttlPhysicsFixture>(
			false, 2);
	private ArrayList<LttlClosure> queueEndContacts = new ArrayList<LttlClosure>(
			0);
	private Body staticBody;

	/**
	 * Used for when getting all bodies in physics world; internal use only
	 */
	Array<Body> bodiesContainerShared = new Array<Body>(0);
	Array<LttlClosure> afterStepClosures = new Array<LttlClosure>(true, 2);

	boolean shouldUpdateBodyToTransform = false;
	private boolean isStepping = false;
	/**
	 * if true, then when {@link #endContact(Contact)} is ran, it will queue it to be processed later. Prevents multiple
	 * calls to {@link LttlPhysicsBody#processBodyCollisionsUpdate()} by queue them until done with all callbacks or
	 * closures.<br>
	 * This is primarily useful when destroying a fixture/body that a body is touching from within a
	 * {@link PhysicsListener#onBodyExit(LttlPhysicsBody, LttlPhysicsBody)} callback.
	 */
	private boolean isQueueEndContacts = false;

	/**
	 * private, guarantees a world always exists
	 */
	private void destroyWorld()
	{
		if (world == null) return;

		Array<Body> bodies = new Array<Body>(world.getBodyCount());
		world.getBodies(bodies);

		for (Body b : bodies)
		{
			LttlPhysicsBody.getComp(b).destroyPhysics();
		}
	}

	@SuppressWarnings("unused")
	private void onGuiGravity()
	{
		if (world == null) return;
		world.setGravity(gravity);
	}

	@GuiButton
	public void initWorld()
	{
		destroyWorld();
		world = new World(gravity, doSleep);
		world.setContactListener(this);
		// need to set our own default contact filter because it allows a fixture with no tags to still collide with an
		// "all" tag, and a "none" mask to not collide with a fixture with no tags, even though same bit
		setContactFilter(getDefaultContactFilter());
		createStaticBody();
	}

	private void createStaticBody()
	{
		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyType.StaticBody;
		bodyDef.position.set(0, 0);
		staticBody = world.createBody(bodyDef);
		// doesn't need fixtures
	}

	/**
	 * Returns a static body (single instance) that can be used for attaching joints too.<br>
	 * <b>Do not modify.</b>
	 */
	public Body getStaticBody()
	{
		return staticBody;
	}

	public World getWorld()
	{
		return world;
	}

	void step(boolean isZero)
	{
		isStepping = true;
		if (isZero)
		{
			// using 0 as deltaTime isn't really reliable, so use super super low number
			getWorld().step(ZERO_STEP, 0, 0);
			// keep fixedAccumulator accurate, as if it was necessary
			Lttl.loop.fixedAccumulatorAdjustment(ZERO_STEP);
		}
		else
		{
			stepCount++;
			getWorld().step(Lttl.game.getSettings().fixedDeltaTime,
					velocityIterations, positionIterations);
		}
		isStepping = false;

		// process after step closures
		processAfterStepClosures();

		// bleh
		processQueuedEndContacts();
	}

	/**
	 * this is only necessary if you need to check for collisions when modifying the physics world bodies' transform
	 * (position and rotation) after this frame's {@link LttlComponent#onFixedUpdate()} has ran.
	 */
	public void stepZero()
	{
		if (Lttl.game.isPlaying() && Lttl.game.getPhysics().enabled)
		{
			// only update physics body transforms if necessary, if it is enabled and simulatedWithForces is disabled
			// because, can't imulate any forces with a 0 time step
			Lttl.game.getPhysics().getWorld()
					.getBodies(Lttl.game.getPhysics().bodiesContainerShared);
			boolean isChanged = false;
			for (Body b : Lttl.game.getPhysics().bodiesContainerShared)
			{
				LttlPhysicsBody bodyComp = LttlPhysicsBody.getComp(b);
				if (bodyComp.transformToBody && !bodyComp.simulateWithForces
						&& bodyComp.isEnabled())
				{
					if (bodyComp.transformToBody())
					{
						isChanged = true;
					}
				}
			}

			// only step if a physics body actually changed
			if (isChanged)
			{
				step(true);
			}
		}
	}

	private void processQueuedEndContacts()
	{
		// loop until queueEndContacts is empty, this is necessary for crazy situation as described below
		while (queueEndContacts.size() > 0)
		{
			// for crazy situation as described below
			isQueueEndContacts = true;

			ArrayList<LttlClosure> copy = new ArrayList<LttlClosure>(
					queueEndContacts);
			queueEndContacts.clear();

			// make it an arraylist, since maybe in some crazy situation, another body object would be destroyed from
			// one of these queue end contact closures, and we want to leave the original queueEndContacts Array
			// availabe to take in new ones
			for (LttlClosure closure : copy)
			{
				closure.run();
			}

			isQueueEndContacts = false;
		}
		queueEndContacts.clear();
	}

	/**
	 * this is done before {@link #processAfterStepOnBodyStayCallbacks()}, since bodies may be destroyed or inited here,
	 * which will ultimately affect the bodies that are touching
	 */
	private void processAfterStepClosures()
	{
		// if there are any destroys in here that end contacts (stop touching), queue them until all the
		// afterStepClosures have been ran
		isQueueEndContacts = true;
		for (LttlClosure closure : afterStepClosures)
		{
			closure.run();
		}
		afterStepClosures.clear();
		isQueueEndContacts = false;

		processQueuedEndContacts();
	}

	public void preDebugDraw()
	{
		if (fadeToWhite > 0)
		{
			Lttl.debug.setInstantDraw(true);
			Lttl.debug.drawPolygonFilled(Lttl.loop.getCurrentRenderingCamera()
					.getViewportPolygon(false), LttlHelper.tmpColor.set(1, 1,
					1, fadeToWhite));
			Lttl.debug.setInstantDraw(false);
		}
	}

	public void debugDraw()
	{
		if (debugRenderer == null)
		{
			debugRenderer = new Box2DDebugRenderer();
			debugMatrix = new Matrix4();
		}
		debugMatrix.set(Lttl.loop.getCurrentRenderingCamera().getWorldMatrix());
		debugMatrix.scale(1 / scaling, 1 / scaling, 1);
		debugRenderer.render(getWorld(), debugMatrix);
	}

	/**
	 * runs every frame after any steps run, not the step
	 */
	void update()
	{
		updateStats();
	}

	void updateStats()
	{
		if (!Lttl.game.inEditor() || !enableStats) return;

		bodyCount = getWorld().getBodyCount();
		fixtureCount = getWorld().getFixtureCount();
		jointCount = getWorld().getJointCount();
		contactCount = getWorld().getContactCount();
	}

	/**
	 * @see #getFixtureComponentsOverlap(float, float, Array)
	 */
	public Array<LttlPhysicsFixture> getFixtureComponentsOverlap(Vector2 point,
			Array<LttlPhysicsFixture> output)
	{
		return getFixtureComponentsOverlap(point.x, point.y, output);
	}

	/**
	 * Gets all the {@link LttlPhysicsFixture} that contain this point in no specific order.
	 */
	public Array<LttlPhysicsFixture> getFixtureComponentsOverlap(float x,
			float y, final Array<LttlPhysicsFixture> output)
	{
		return getFixtureComponentsOverlap(x, y, x, y, output);
	}

	// OPTIMIZE if this becomes slow with many fixtures, could try iterating only through body objects that have
	// mouseCallback enabled, might be slower since box2d probably implements some sort of tree, but if it was really
	// slow, I could also implement some tree, but rather not do any of this. Also this query does AABB overlap check
	// first, which is fast, then I test if the specific point (if it is a point) is inside the fixture.
	/**
	 * Gets all the {@link LttlPhysicsFixture} that overlap this AABB in no specific order.
	 */
	public Array<LttlPhysicsFixture> getFixtureComponentsOverlap(float x1,
			float y1, float x2, float y2, final Array<LttlPhysicsFixture> output)
	{
		output.clear();

		final boolean isPoint = x1 == x2 && y1 == y2;

		final float x1S = x1 * scaling;
		final float y1S = y1 * scaling;
		final float x2S = x2 * scaling;
		final float y2S = y2 * scaling;

		// checks
		world.QueryAABB(new QueryCallback()
		{
			@Override
			public boolean reportFixture(Fixture fixture)
			{
				// check if the fixture's fixture component already exists in the array, we only care about unique
				// fixture components not individual fixture pieces
				if (output.size > 0)
				{
					for (LttlPhysicsFixture fixtureComp : output)
					{
						// skip adding since already have this fixture component
						if (fixtureComp == fixture.getUserData()) return true;
					}
				}
				// if the AABB we are testing is really a point, then check if point is inside fixture
				if (isPoint)
				{
					if (!fixture.testPoint(x1S, y1S)) { return true; }
				}
				else
				{
					Lttl.Throw();
					// TODO need to check if AABB given actually overlaps the fixture, not just the fixture's AABB
				}

				// don't have it, so add it
				output.add((LttlPhysicsFixture) fixture.getUserData());
				return true;
			}
		}, x1S, y1S, x2S, y2S);

		return output;
	}

	/**
	 * Returns all the fixtures that overlap the world AABB in no specific order.
	 */
	public Array<Fixture> getFixturesOverlap(float x1, float y1, float x2,
			float y2, final Array<Fixture> output)
	{
		output.clear();

		x1 *= scaling;
		y1 *= scaling;
		x2 *= scaling;
		y2 *= scaling;
		world.QueryAABB(new QueryCallback()
		{

			@Override
			public boolean reportFixture(Fixture fixture)
			{
				output.add(fixture);
				return true;
			}
		}, x1, y1, x2, y2);

		return output;
	}

	/**
	 * Returns all the fixtures that overlap the world point in no specific order. This should be seldomly needed, since
	 * you rarely need to know which {@link Fixture}, only which {@link LttlPhysicsFixture}.<br>
	 * {@link #getFixtureComponentsOverlap(float, float, float, float, Array)} is faster, and if you need specifc
	 * fixtures for some reason you can just use {@link LttlPhysicsFixtureBodyBase#getFixtureContains(float, float)}.
	 */
	public Array<Fixture> getFixturesOverlap(float x, float y,
			final Array<Fixture> output)
	{
		output.clear();

		final float xS = x * scaling;
		final float yS = y * scaling;

		// fast way (i think) to get fixtures, but it can be inaccurate if lots of fixtures in same body
		world.QueryAABB(new QueryCallback()
		{
			@Override
			public boolean reportFixture(Fixture fixture)
			{
				// test point if actually in fixture, since the query just checks fixture's AABB with the point given
				if (!fixture.testPoint(xS, yS)) return true;

				output.add(fixture);
				return true;
			}
		}, xS, yS, xS, yS);

		return output;
	}

	/**
	 * @see #getFixturesOverlap(float, float)
	 */
	public Array<Fixture> getFixturesOverlap(Vector2 point,
			Array<Fixture> output)
	{
		return getFixturesOverlap(point.x, point.y, output);
	}

	/**
	 * check for mouse position in all fixtures, process callbacks on enabled bodies
	 */
	void processMouseContact()
	{
		mouseContainedIds.clear();
		getFixtureComponentsOverlap(Lttl.input.getMousePos(), fixtureCompsArray);

		// check for any mouse enters and/or contacts
		if (fixtureCompsArray.size == 1)
		{
			processMouseEnterContact(fixtureCompsArray.get(0), 0);
		}
		// multiple, sort based on z then process
		else if (fixtureCompsArray.size > 1)
		{
			fixtureCompsArray.sort();

			int i = 0;
			for (LttlPhysicsFixture fixtureComp : fixtureCompsArray)
			{
				processMouseEnterContact(fixtureComp, i++);
			}
		}

		// check for any mouse exits
		for (int i = 0, n = mouseContainedIds.size; i < n; i++)
		{
			mouseContainedIdsLast.removeValue(mouseContainedIds.get(i));
		}
		for (int i = 0, n = mouseContainedIdsLast.size; i < n; i++)
		{
			processMouseExit(mouseContainedIdsLast.get(i));
		}
		mouseContainedIdsLast.clear();
		mouseContainedIdsLast.addAll(mouseContainedIds);
	}

	private void processMouseExit(int id)
	{
		LttlComponent comp = Lttl.scenes.findComponentByIdAllScenes(id);
		// component was probably destroyed
		if (comp == null) return;

		if (comp instanceof LttlPhysicsBody)
		{
			LttlPhysicsBody bodyComp = ((LttlPhysicsBody) comp);
			bodyComp.containsMouseStart = false;
			bodyComp.containsMouseStartzIndex = -1;
			if (!bodyComp.callbackMouse) return;

			for (MouseListener listener : bodyComp.t().getComponentsInTree(
					MouseListener.class, true))
			{
				// skip if not enabled
				if (!((LttlComponent) listener).isEnabled()) continue;

				listener.onMouseExitBody(bodyComp);
			}
		}
		else if (comp instanceof LttlPhysicsFixture)
		{
			LttlPhysicsFixture fixtureComp = ((LttlPhysicsFixture) comp);
			fixtureComp.containsMouseStart = false;
			fixtureComp.containsMouseStartzIndex = -1;

			LttlPhysicsBody bodyComp = fixtureComp.getBodyComp();
			if (bodyComp == null || !bodyComp.callbackMouse) return;

			for (MouseListener listener : bodyComp.t().getComponentsInTree(
					MouseListener.class, true))
			{
				// skip if not enabled
				if (!((LttlComponent) listener).isEnabled()) continue;

				listener.onMouseExitFixture(fixtureComp);
			}
		}
		else
		{
			Lttl.Throw("Unexpected component class: "
					+ comp.getClass().getSimpleName());
		}
	}

	private void processMouseEnterContact(LttlPhysicsFixture fixtureComp,
			int zIndex)
	{
		LttlPhysicsBody bodyComp = fixtureComp.getBodyComp();
		boolean bodyCompOrig = bodyComp.containsMouseStart;
		bodyComp.containsMouseStart = true;
		bodyComp.containsMouseStartzIndex = zIndex;
		if (!mouseContainedIds.contains(bodyComp.getId()))
		{
			mouseContainedIds.add(bodyComp.getId());
		}

		boolean fixtureCompOrig = fixtureComp.containsMouseStart;
		fixtureComp.containsMouseStart = true;
		fixtureComp.containsMouseStartzIndex = zIndex;
		if (!mouseContainedIds.contains(fixtureComp.getId()))
		{
			mouseContainedIds.add(fixtureComp.getId());
		}

		// global callback

		// body component callback
		if (bodyComp.callbackMouse)
		{
			for (MouseListener listener : bodyComp.t().getComponentsInTree(
					MouseListener.class, true))
			{
				// skip if not enabled
				if (!((LttlComponent) listener).isEnabled()
						| ((LttlComponent) listener).isDestroyPending())
					continue;

				// mouse entered this frame
				if (!bodyCompOrig)
				{
					listener.onMouseEnterBody(bodyComp, fixtureComp, zIndex);
				}
				listener.onMouseContactBody(bodyComp, fixtureComp, zIndex);

				// mouse entered this frame
				if (!fixtureCompOrig)
				{
					listener.onMouseEnterFixture(fixtureComp, zIndex);
				}
				listener.onMouseContactFixture(fixtureComp, zIndex);
			}
		}
	}

	/**
	 * True if is in the middle of a physics step. This does not indicate if between step iterations (during the same
	 * frame).
	 * 
	 * @return
	 */
	public boolean isStepping()
	{
		return isStepping;
	}

	/* CONTACT LISTENER */

	/**
	 * Called First. This is called when two fixtures start touching, regardless if contact is enabled or disabled, must
	 * be allowed to collide in the {@link ContactFilter}. However, since this is before the preSolve, it does nothing
	 * except add it to the body component's contact list. The preSolve MAY modify the contact's enabled/disabled, and
	 * there is where callbacks will be done. This is called for sensors and non-sensors. This event can only occur
	 * inside the time step.<br>
	 * Contacts should always be touching.
	 */
	@Override
	public void beginContact(Contact contact)
	{
		Fixture fixtureA = contact.getFixtureA();
		Fixture fixtureB = contact.getFixtureB();

		processBeginContact(fixtureA, contact);
		processBeginContact(fixtureB, contact);

		// since because it's a sensor collision, it will never run preSolve, which is the normal place where body
		// collisions are updated, so if one is a sensor, the process body collisions now
		if (fixtureA.isSensor() || fixtureB.isSensor())
		{
			processUpdateBodyCollisions(fixtureA, contact, false);
			processUpdateBodyCollisions(fixtureB, contact, false);
		}
	}

	private void processBeginContact(Fixture fixture, Contact contact)
	{
		LttlPhysicsBody bodyComp = LttlPhysicsHelper.getBodyComp(fixture);
		if (bodyComp == null) return;

		bodyComp.touchingContacts.add(contact);
		bodyComp.touchingContactsDirty = true;

		// beginContact callback
		if (bodyComp.callbackContact)
		{
			ArrayList<PhysicsListener> listeners = bodyComp.t()
					.getComponentsInTree(PhysicsListener.class, true);

			for (PhysicsListener listener : listeners)
			{
				// skip if not enabled
				if (!((LttlComponent) listener).isEnabled()) continue;

				listener.beginContact(bodyComp, contact);
			}
		}
	}

	/**
	 * Called Second. This is called after collision detection of bodies that should have a collision response (bodies
	 * that are filtered/grouped to collide or have been allowed though the custom {@link ContactFilter}, and not
	 * sensors), but before collision resolution (how they are going to react physically from the collision).<br>
	 * This gives you a chance to disable the contact based on the current configuration. For example, you can implement
	 * a one-sided platform using this callback and calling {@link Contact#setEnabled(boolean)}. The contact will be
	 * re-enabled each time through collision processing, so you will need to disable the contact every time-step. The
	 * pre-solve event may be fired multiple times per time step per contact due to continuous collision detection.<br>
	 * The pre-solve event is also a good place to determine the point state and the approach velocity of collisions.<br>
	 * Pre-solve does not run if one of the fixtures is a sensor or the fixtures are filtered/grouped out of colliding,
	 * or have not been allowed though the custom {@link ContactFilter}. <br>
	 * This should only be used to disable collisions if you sometimes want the collision, if you never do, then use
	 * tags/group to filter it out completely.
	 */
	@Override
	public void preSolve(Contact contact, Manifold oldManifold)
	{
		// body component callbacks for preSolve
		// must try and update both preSolves first
		// mark if at least one of them actually had a preSolve callback that could have modified contact
		boolean atleastOneCallbackSolve = processPreSolve(
				contact.getFixtureA(), contact, oldManifold)
				| processPreSolve(contact.getFixtureB(), contact, oldManifold);

		// body component callbacks for update body collision
		processUpdateBodyCollisions(contact.getFixtureA(), contact,
				atleastOneCallbackSolve);
		processUpdateBodyCollisions(contact.getFixtureB(), contact,
				atleastOneCallbackSolve);
	}

	private void processUpdateBodyCollisions(Fixture fixture, Contact contact,
			boolean atleastOneCallbackSolve)
	{
		LttlPhysicsBody bodyComp = LttlPhysicsHelper.getBodyComp(fixture);
		if (bodyComp == null) return;

		// only need to update body collisions if doing body collision callbacks, and if either their is a chance of a
		// preSolve modifying the contact or their has been an additional contact that needs to be taken into
		// consideration.
		if (bodyComp.callbackBodyCollision
				&& (atleastOneCallbackSolve || bodyComp.touchingContactsDirty))
		{
			// enter/exit callbacks and update touching bodies
			bodyComp.processBodyCollisionsUpdate();
		}
	}

	/**
	 * @return if bodyComp has a callback for pre and post solve, no guarantee it changed anything
	 */
	private boolean processPreSolve(Fixture fixture, Contact contact,
			Manifold oldManifold)
	{
		final LttlPhysicsBody bodyComp = LttlPhysicsHelper.getBodyComp(fixture);
		if (bodyComp == null) return false;

		if (bodyComp.callbackSolve)
		{
			ArrayList<PhysicsListener> listeners = bodyComp.t()
					.getComponentsInTree(PhysicsListener.class, true);

			for (PhysicsListener listener : listeners)
			{
				// skip if not enabled
				if (!((LttlComponent) listener).isEnabled()) continue;

				listener.preSolve(bodyComp, contact, oldManifold);
			}

			return true;
		}
		return false;
	}

	/**
	 * Called Third. The post solve event is where you can gather collision impulse results. If you don’t care about the
	 * impulses, you should probably just implement the pre-solve event.
	 */
	@Override
	public void postSolve(Contact contact, ContactImpulse impulse)
	{
		// body component callbacks
		processPostSolve(contact.getFixtureA(), contact, impulse);
		processPostSolve(contact.getFixtureB(), contact, impulse);
	}

	private void processPostSolve(Fixture fixture, Contact contact,
			ContactImpulse impulse)
	{
		final LttlPhysicsBody bodyComp = LttlPhysicsHelper.getBodyComp(fixture);
		if (bodyComp == null) return;

		if (bodyComp.callbackSolve)
		{
			ArrayList<PhysicsListener> listeners = bodyComp.t()
					.getComponentsInTree(PhysicsListener.class, true);

			for (PhysicsListener listener : listeners)
			{
				// skip if not enabled
				if (!((LttlComponent) listener).isEnabled()) continue;

				listener.postSolve(bodyComp, contact, impulse);
			}
		}
	}

	/**
	 * Called Last. This is called when two fixtures stop touching regardless if enabled or disabled. This is called for
	 * sensors and non-sensors. This may be called when a body or fixture is destroyed, so this event can occur outside
	 * the time step. It can also occur when one of the fixtures/bodies goes to sleep or during a
	 * {@link Body#setTransform(Vector2, float)}.<br>
	 * Contacts appear to always be not touching unless it was destroyed, and in that case it will probably still be
	 * touching because contacts only exist if touching.
	 */
	@Override
	public void endContact(Contact contact)
	{
		processEndContact(contact.getFixtureA(), contact);
		processEndContact(contact.getFixtureB(), contact);
	}

	private void processEndContact(Fixture fixture, final Contact contact)
	{
		final LttlPhysicsBody bodyComp = LttlPhysicsHelper.getBodyComp(fixture);
		if (bodyComp == null) return;

		// skip callback if it is on a body component that is initialized and asleep since this should not count as not
		// touching, still remove contact though since contact is dead
		// also check if it is not a sensor, since sensors can be asleep and still have contacts
		if (bodyComp.isInit() && !fixture.isSensor()
				&& !bodyComp.getBody().isAwake())
		{
			bodyComp.touchingContacts.remove(contact);
			return;
		}

		// endContact callback
		if (bodyComp.callbackContact)
		{
			ArrayList<PhysicsListener> listeners = bodyComp.t()
					.getComponentsInTree(PhysicsListener.class, true);

			for (PhysicsListener listener : listeners)
			{
				// skip if not enabled
				if (!((LttlComponent) listener).isEnabled()) continue;

				listener.endContact(bodyComp, contact);
			}
		}

		// if doing body collision callbacks, then update touching bodies and do callbacks; doesn't matter if
		// contact is touching or not, since it most likely is not touching anymore since it is endContact
		if (bodyComp.callbackBodyCollision)
		{
			if (isQueueEndContacts)
			{
				// if is currently processing onBodyStay callbacks, then queue this to be ran after
				// all bodies finish processing callbacks.
				// this prevents a body's touching bodies list from being modified while it is iterating through them
				// for callbacks, or in very rare cases, calling back into the same processBodyCollisionUpdate method,
				// which will cause all sorts of problems
				queueEndContacts.add(new LttlClosure()
				{
					@Override
					public void run()
					{
						processEndContactCallbackBodyCollisions(bodyComp,
								contact);
					}
				});
			}
			else
			{
				processEndContactCallbackBodyCollisions(bodyComp, contact);
			}
		}
		else
		{
			bodyComp.touchingContacts.remove(contact);
		}
	}

	private void processEndContactCallbackBodyCollisions(
			LttlPhysicsBody bodyComp, Contact contact)
	{
		// always remove before processing
		// sometimes the contact will still be touching even when endContact() is called. This happens if the bodies
		// were touching but then one was destroyed. So just remove it every time to be consistent.
		bodyComp.touchingContacts.remove(contact);

		// enter/exit callbacks and update touching bodies
		bodyComp.processBodyCollisionsUpdate();
	}

	/**
	 * Allows physics world modifications to be done accurately after current step is completed, most helpful when
	 * called from {@link PhysicsListener}<br>
	 * Registers the closure to run in the order registered after the current step is completed.<br>
	 * Note: If not {@link #isStepping()}, which could be the case with
	 * {@link PhysicsListener#endContact(LttlPhysicsBody, Contact)}, then teh closure will run right away.
	 * 
	 * @param closure
	 *            Be sure to check {@link LttlPhysicsBase#isInit()} if the physics objects to be manipulated could have
	 *            been destroyed<br>
	 *            Should not contain the calculations with {@link Contact} or {@link ContactImpulse} or {@link Manifold}
	 *            , just the task with the resulting values.
	 */
	public void registerAfterStep(LttlClosure closure)
	{
		if (isStepping())
		{
			afterStepClosures.add(closure);
		}
		else
		{
			closure.run();
		}
	}

	/**
	 * Returns number of (non zero steps)
	 */
	public int getStepCount()
	{
		return stepCount;
	}

	/**
	 * Sets a custom {@link ContactFilter} to overwrite the default {@link #getDefaultContactFilter()}. Same positive
	 * group index always collide, same negative group index never collide. The {@link LttlTransform#getTagsBit()}
	 * defines the category bits.
	 * 
	 * @param listener
	 */
	public void setContactFilter(ContactFilter filter)
	{
		world.setContactFilter(filter);
	}

	public ContactFilter getDefaultContactFilter()
	{
		ContactFilter filter = new ContactFilter()
		{
			@Override
			public boolean shouldCollide(Fixture fixtureA, Fixture fixtureB)
			{
				// accessing filter data seems to be very fast, even if called over 10000 times
				// so no real optimization loss with this custom filter
				Filter a = fixtureA.getFilterData();
				Filter b = fixtureB.getFilterData();

				// if same groupIndex, then if positive, allow, if negative, don't allow, overrides any bit mask
				if (a.groupIndex == b.groupIndex && a.groupIndex != 0) { return a.groupIndex > 0; }

				// check if each fixture's bit mask allows the other fixtures tags
				return LttlHelper.bitMaskAllowTags(a.maskBits, b.categoryBits)
						&& LttlHelper.bitMaskAllowTags(b.maskBits,
								a.categoryBits);
			}
		};

		return filter;
	}

	/**
	 * This is true when a LttlPhysicsBody should update it's body to LttlTransform during the lateFixedUpdate
	 */
	public boolean shouldUpdateBodyToTransform()
	{
		return shouldUpdateBodyToTransform;
	}

	public boolean isInit()
	{
		return world != null;
	}

	/* SHARED RESOURCES */
	// this is necessary since they are connected to specific physics world instance and so they can only be called if
	// world has been created first
	final private Pool<PolygonShape> polygonShapePool = new Pool<PolygonShape>(
			0)
	{
		@Override
		protected PolygonShape newObject()
		{
			return new PolygonShape();
		}
	};
	private CircleShape circleShapeShared;
	private ChainShape chainShapeShared;
	private EdgeShape edgeShapeShared;

	public CircleShape getCircleShapeShared()
	{
		if (!isInit())
		{
			// this is called before physics world was created, probably something running in a Constructor or static
			Lttl.Throw();
			return null;
		}

		if (circleShapeShared == null)
		{
			circleShapeShared = new CircleShape();
		}
		return circleShapeShared;
	}

	public ChainShape getChainShapeShared()
	{
		if (!isInit())
		{
			// this is called before physics world was created, probably something running in a Constructor or static
			Lttl.Throw();
			return null;
		}

		if (chainShapeShared == null)
		{
			chainShapeShared = new ChainShape();
		}
		return chainShapeShared;
	}

	public EdgeShape getEdgeShapeShared()
	{
		if (!isInit())
		{
			// this is called before physics world was created, probably something running in a Constructor or static
			Lttl.Throw();
			return null;
		}

		if (edgeShapeShared == null)
		{
			edgeShapeShared = new EdgeShape();
		}
		return edgeShapeShared;
	}

	public Pool<PolygonShape> getPolygonShapePool()
	{
		if (!isInit())
		{
			// this is called before physics world was created, probably something running in a Constructor or static
			Lttl.Throw();
			return null;
		}

		return polygonShapePool;
	}

}
