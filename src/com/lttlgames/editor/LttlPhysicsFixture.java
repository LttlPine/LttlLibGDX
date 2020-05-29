package com.lttlgames.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.physics.box2d.Shape.Type;
import com.badlogic.gdx.utils.ShortArray;
import com.lttlgames.editor.LttlTransform.GuiTransformListener;
import com.lttlgames.editor.annotations.GuiButton;
import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.GuiGroup;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.GuiReadOnly;
import com.lttlgames.editor.annotations.GuiShow;
import com.lttlgames.editor.annotations.GuiTagBit;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.exceptions.MultiplePolygonsException;
import com.lttlgames.graphics.Joint;
import com.lttlgames.helpers.LttlGeometryUtil;
import com.lttlgames.helpers.LttlGeometryUtil.PolygonContainer;
import com.lttlgames.helpers.Vector2Array;

//8
@Persist(-90129)
public abstract class LttlPhysicsFixture extends LttlPhysicsFixtureBodyBase
		implements Comparable<LttlPhysicsFixture>
{
	/* STATIC */
	final private static FixtureDef fixtureDefShared = new FixtureDef();
	final static Vector2Array pointsShared = new Vector2Array();
	final static ShortArray indicesContainer = new ShortArray();
	private static PolygonContainer sharedPolyContainer;
	/**
	 * each shape represents a fixture that should be made, this is shared by each LttlPhysicsFixture, but only used to
	 * store the shapes, then is cleared once physics fixture has been made
	 */
	final static ArrayList<Shape> shapesListShared = new ArrayList<Shape>();
	final static Vector2 tmp = new Vector2();

	/* MEMBERS */

	@GuiGroup("Stats")
	@GuiShow
	@GuiReadOnly
	private int fixturesCount = -1;

	@Persist(9012900)
	@GuiGroup("Physics")
	@GuiCallback("onGuiChange")
	@GuiMin(0)
	public float friction = 0.2f;
	@Persist(9012901)
	@GuiGroup("Physics")
	@GuiCallback("onGuiChange")
	@GuiMin(0)
	public float restitution = .2f;
	@Persist(9012902)
	@GuiGroup("Physics")
	@GuiCallback("onGuiChange")
	@GuiMin(0)
	public float density = .2f;
	@Persist(9012903)
	@GuiGroup("Physics")
	@GuiCallback("onGuiChange")
	public boolean isSensor = false;
	/**
	 * If fixture is {@link LttlPhysicsMesh} or {@link LttlPhysicsPath} and it's concave or has holes
	 */
	@Persist(9012906)
	@GuiGroup("Shape")
	@GuiCallback("onGuiShape")
	public boolean triangulate = false;
	@Persist(9012904)
	@GuiGroup("Shape")
	@GuiCallback("onGuiShape")
	@GuiMin(0)
	public float radius = .01f;
	/**
	 * Shrinks the fixture by this amount (local units). Mostly used {@link LttlPhysicsMesh} and {@link LttlPhysicsPath}
	 * for when you want the fixture shape to be smalled than the render. Not used for {@link LttlPhysicsCircle}.
	 */
	@Persist(9012905)
	@GuiGroup("Shape")
	@GuiCallback("onGuiShape")
	@GuiMin(0)
	public float radiusBuffer = 0;

	/**
	 * Starts off as 0, which means o group, so does nothing.
	 * 
	 * @see Filter#groupIndex
	 */
	@GuiGroup("Filter")
	@Persist(9012907)
	public short groupIndex = 0;

	/**
	 * Default behavior: Only collides with fixtures that have a tag in this mask. Non-zero group index will always win.<br>
	 * Starts off as -1, which is all.
	 * 
	 * @see Filter#maskBits
	 */
	@GuiGroup("Filter")
	@Persist(9012908)
	@GuiTagBit
	public short tagMaskBits = -1;

	final private ArrayList<Fixture> fixtures = new ArrayList<Fixture>(1);
	private GuiTransformListener guiTransformListener;

	final void init(LttlPhysicsBody bodyComp)
	{
		// fixtures should be destroyed before initing a new one
		// NOT ALLOWED to init the same fixture on two bodies (dumb), just make a copy of fixture first
		Lttl.Throw(isInit());

		Body body = bodyComp.getBody();

		FixtureDef def = getFixtureDef(body.getPosition());
		// no shapes were generated
		if (shapesListShared.size() == 0)
		{
			Lttl.logNote("Generating Fixture: No shapes generated for "
					+ this.toString() + ".");
			return;
		}

		switch (body.getType())
		{
			case DynamicBody:
				// all fixtures in a dynamic body must have some density
				if (def.density <= 0)
				{
					Lttl.logError("Generating Fixture: Density for "
							+ this.toString()
							+ " must be greater than 0 because dynamic.");
				}
				break;
			case KinematicBody:
				break;
			case StaticBody:
				break;

		}

		// check if adding these fixtures will exceed the body fixture limit
		if (bodyComp.getFixtureCount() + shapesListShared.size() > LttlPhysicsBody.MAX_FIXTURES)
		{
			Lttl.logNote("Generating Fixture: max fixtures per body has been exceeded ("
					+ toString() + ").");
			return;
		}

		// create a fixture using the FixtureDef and each shape that is popuated in the shared shapes list
		for (Shape shape : shapesListShared)
		{
			def.shape = shape;
			addFixtureInternal(body, def);

			// free polygon shape from pool
			if (shape.getType() == Type.Polygon)
			{
				Lttl.game.getPhysics().getPolygonShapePool()
						.free((PolygonShape) shape);
			}
		}
		// clear shapes from list since all fixtures have been made
		shapesListShared.clear();

		// this should never happen, should always be at least one fixture by this point
		Lttl.Throw((fixturesCount = getFixtureCount()) == 0);

		// only add gui transform listener if there is at least one fixture and it's not the same tranform as the
		// body because there will already be a transform listener on the body
		// only add during editor while not playing, editor use onle
		if (!Lttl.game.isPlaying() && bodyComp.t() != t())
		{
			addGuiTransformListenerNonPlaying();
		}
	}

	private void addFixtureInternal(Body body, FixtureDef def)
	{
		Fixture fixture = body.createFixture(def);
		fixture.setUserData(this);
		fixtures.add(fixture);
	}

	/**
	 * There is no init() method for Fixtures because it can only be initialized through the creation of a body. So
	 * either run this method, or destroy and re init body.<br>
	 * This method can find the body that contains this fixture if it's in same tree.
	 */
	@GuiButton(order = 0)
	@Override
	public void destroyAndInit()
	{
		if (isInit())
		{
			getBodyComp().destroyAndInit();
		}
		else
		{
			// if not init, then try to find the body
			for (LttlPhysicsBody body : t().getHighestParent()
					.getComponentsInTree(LttlPhysicsBody.class, true))
			{
				if (body.fixtureComps.contains(this))
				{
					body.destroyAndInit();
					break;
				}
			}
		}
	}

	/**
	 * Facilitates changing fixture settings while game is playing via editor
	 */
	void onGuiChange()
	{
		if (!isInit()) return;

		for (Fixture f : fixtures)
		{
			if (f.getDensity() != density)
			{
				f.setDensity(density);
				f.getBody().resetMassData();
			}
			f.setFriction(friction);
			f.setRestitution(restitution);
			f.setSensor(isSensor);
		}

	}

	/**
	 * Facilitates changing radius setting while game is playing via editor, requires recreating the entire physics body
	 */
	final void onGuiShape()
	{
		// if in editor always destroy and init, if playing, then only if not init
		if (!isInit() && Lttl.game.isPlaying()) return;

		destroyAndInit();
	}

	private void addGuiTransformListenerNonPlaying()
	{
		// don't even worry about any of this if not in editor
		if (!Lttl.game.inEditor()) return;

		// if already exists, probably some error with destroying
		Lttl.Throw(guiTransformListener != null);

		// always add listener if in editor, even if playing, since physics could be enabled or not in runtime
		// when the scale, rotation, position, or origin changes on this fixture, recreate the parent body
		t().addGuiListener(
				guiTransformListener = new GuiTransformListener(true)
				{
					private void doo()
					{
						// needs to be initialized and physics can't be running (playing and enabled)
						if (!isInit()
								|| (Lttl.game.getPhysics().enabled && Lttl.game
										.isPlaying())) { return; }
						getBodyComp().destroyAndInit();
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

	/**
	 * Returns a shared {@link FixtreDef} with the settings of this {@link LttlPhysicsFixture} and no shape set, need to
	 * set them from {@link #shapesListShared}
	 * 
	 * @return
	 */
	final public FixtureDef getFixtureDef(Vector2 bodyOrigin)
	{
		fixtureDefShared.friction = friction;
		fixtureDefShared.restitution = restitution;
		fixtureDefShared.friction = friction;
		fixtureDefShared.density = density;
		fixtureDefShared.isSensor = isSensor;
		fixtureDefShared.shape = null;
		fixtureDefShared.filter.categoryBits = t().tagBit;
		fixtureDefShared.filter.groupIndex = groupIndex;
		fixtureDefShared.filter.maskBits = tagMaskBits;

		// always clear shapes list before generating new shapes, that way can tell if none were created
		shapesListShared.clear();
		getShapes(bodyOrigin);

		return fixtureDefShared;
	}

	/**
	 * if greater than 0, than this fixture has succseffully been initialized
	 */
	@Override
	public int getFixtureCount()
	{
		return fixtures.size();
	}

	public List<Fixture> getFixtures()
	{
		return Collections.unmodifiableList(fixtures);
	}

	/**
	 * Populates the {@link #shapesListShared}, which will always be empty when this method runs, to be used to create
	 * all the fixtures on this component.<br>
	 * The shape's points should be local to the body (not world).
	 * 
	 * @return
	 */
	public abstract void getShapes(Vector2 bodyOrigin);

	@Override
	public void destroyPhysics()
	{
		// if not initialized, then just skip destroying
		if (!isInit()) return;

		getBodyComp().processDestroyCallbacks(this);

		Body body = getBody();

		// need to clean up before destroying in physics world, so other callbacks will know it has been destroyed, and
		// so user loses all ability to manipulate the physics object, since once it is destroyed below, it's gone
		ArrayList<Fixture> fixturesSaved = new ArrayList<Fixture>(fixtures);
		cleanUp();

		for (Fixture f : fixturesSaved)
		{
			body.destroyFixture(f);
		}
	}

	@Override
	void cleanUp()
	{
		if (Lttl.game.inEditor() && guiTransformListener != null)
		{
			t().removeGuiListener(guiTransformListener);
			guiTransformListener = null;
		}
		fixtures.clear();
	}

	@Override
	public LttlPhysicsBody getBodyComp()
	{
		if (!isInit()) return null;
		Fixture f = getFixtures().get(0);
		return (LttlPhysicsBody) f.getBody().getUserData();
	}

	/**
	 * May return null if no fixtures created.
	 * 
	 * @return
	 */
	public Body getBody()
	{
		if (!isInit()) return null;
		Fixture f = getFixtures().get(0);
		return f.getBody();
	}

	@Override
	public void onEditorDestroyComp()
	{
		processOnDestroyComp();
	}

	@Override
	public void onDestroyComp()
	{
		processOnDestroyComp();
	}

	private void processOnDestroyComp()
	{
		// destroy fixture if it is initialized, and if so then tell body to destroy and reinit itself

		// if body component is destroyed first (probably because component was called to be destroyed and this
		// component is a child of it) the body component would have already destroyed the fixtures on this component,
		// so the fixtures count would be 0, meaning it is not initialized, thus skipping this method entirely
		if (!isInit()) return;

		// get the body component, which should exist because isInit()
		LttlPhysicsBody bodyComp = getBodyComp();

		// if the body component is pending to be destroyed, then these physics fixtures will be destroyed with it, so
		// no need to continue; this would only happen if this fixture component was actually above the body component
		// in terms of component order, since if body component's onDestroyComp() callback was called before this, this
		// fixture component would not be initialized
		if (bodyComp.isDestroyPending()) return;

		// remove this fixture component from the body component (because this component is being destroyed)
		bodyComp.fixtureComps.remove(this);

		// destroy this fixture physics object
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
		// this is never done when playing (so always will be in editor)
		if (!Lttl.game.isPlaying())
		{
			// if enabled or disabled will reinit physics body so it will not include this fixture, only works in editor
			// while not playing, if playing need to do this manually

			// if was just disabled, then try to recreate the parent body without this fixture
			if (!isEnabled())
			{
				if (isInit() && getBodyComp().getBody() != null)
				{
					getBodyComp().destroyAndInit();
				}
			}
			// if it was just enabled, can't create the fixture since no reference to the parent body, so look it up
			else
			{
				for (LttlPhysicsBody b : Lttl.scenes.findComponentsAllScenes(
						LttlPhysicsBody.class, true))
				{
					if (b.fixtureComps.contains(this))
					{
						b.destroyAndInit();
						// only one body can have this fixture
						break;
					}
				}
			}
		}
	}

	final void processRadiusBuffer(Vector2Array points)
	{
		if (radiusBuffer != 0)
		{
			try
			{
				sharedPolyContainer = LttlGeometryUtil.offsetPolygon(points,
						-radiusBuffer, Joint.MITER, 1, 0, sharedPolyContainer);
				if (sharedPolyContainer.getHoles().size() > 0)
				{
					Lttl.logNote("Processing Fixture Radius Buffer Failed: "
							+ sharedPolyContainer.getHoles().size()
							+ " holes found when applying buffer.");
				}
				else
				{
					// since the buffer was a success because no holes or multiple polygones, update points
					points.set(sharedPolyContainer.getPoints());
				}
			}
			catch (MultiplePolygonsException e)
			{
				Lttl.logNote("Processing Fixture Radius Buffer Failed: multiple polygons were generated when adding radius buffer.");
			}
		}
	}

	@Override
	public boolean isInit()
	{
		return fixtures.size() > 0;
	}

	@Override
	public Fixture getFixtureContains(float x, float y)
	{
		if (!isInit()) return null;

		x *= Lttl.game.getPhysics().scaling;
		y *= Lttl.game.getPhysics().scaling;

		for (Fixture f : fixtures)
		{
			if (f.testPoint(x, y)) { return f; }
		}
		return null;
	}

	/**
	 * Used in {@link PhysicsController#processMouseContact()}
	 */
	@Override
	public int compareTo(LttlPhysicsFixture o)
	{
		// no need to update any world values because this running at the beginning of the frame, before any
		// updates have ran
		float z1 = this.t().getWorldZPos(false);
		float z2 = o.t().getWorldZPos(false);
		if (z1 == z2) return 0;
		if (z1 < z2) return -1;
		return 1;
	}

	@Override
	public Rectangle getAABB()
	{
		if (!isInit()) return null;

		aabbTempArray.clear();
		for (Fixture f : fixtures)
		{
			LttlPhysicsHelper.getShapePoints(f.getShape(), aabbTempArray);
		}

		return aabbTempArray.getAABB(aabb);
	}
}
