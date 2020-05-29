package com.lttlgames.editor;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.ChainShape;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Joint;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.lttlgames.helpers.Vector2Array;

public class LttlPhysicsHelper
{
	static private Vector2 tmp = new Vector2();

	/**
	 * Returns the {@link LttlPhysicsFixture} component on this fixture, if any.
	 */
	public static LttlPhysicsFixture getFixtureComp(Fixture fixture)
	{
		return fixture.getUserData() instanceof LttlPhysicsFixture ? (LttlPhysicsFixture) fixture
				.getUserData() : null;
	}

	/**
	 * Returns the {@link LttlPhysicsBody} component on this body, if any.
	 * 
	 * @return null if none
	 */
	public static LttlPhysicsBody getBodyComp(Body body)
	{
		return body.getUserData() instanceof LttlPhysicsBody ? (LttlPhysicsBody) body
				.getUserData() : null;
	}

	/**
	 * Uses the fixture's physics body object, not the fixture component, to get the LttlPhysicsBody component. This way
	 * if the fixture does not have a component or has not had it set, it still will find the body.
	 * 
	 * @see #getBodyComp(Body)
	 */
	public static LttlPhysicsBody getBodyComp(Fixture fixture)
	{
		return getBodyComp(fixture.getBody());
	}

	/**
	 * @see #getBodyComp(Body)
	 */
	public static LttlPhysicsBody getBodyCompA(Joint joint)
	{
		return getBodyComp(joint.getBodyA());
	}

	/**
	 * @see #getBodyComp(Body)
	 */
	public static LttlPhysicsBody getBodyCompB(Joint joint)
	{
		return getBodyComp(joint.getBodyB());
	}

	/**
	 * Returns the {@link LttlPhysicsJointBase} component on this joint, if any.
	 * 
	 * @return null if none
	 */
	public static LttlPhysicsJointBase<?> getJointComp(Joint joint)
	{
		return joint.getUserData() instanceof LttlPhysicsJointBase ? (LttlPhysicsJointBase<?>) joint
				.getUserData() : null;
	}

	/**
	 * Returns the other {@link LttlPhysicsBody} component that is not thisBodyComp based on the contact's fixtures, if
	 * the fixtures has one.
	 * 
	 * @param thisBodyComp
	 * @param contact
	 * @return null if thisBodyComp is not found or the other fixture does not have a physics body component associated
	 *         with it
	 */
	public static LttlPhysicsBody getOtherBodyComp(
			LttlPhysicsBody thisBodyComp, Contact contact)
	{
		switch (getIndexFixture(contact, thisBodyComp))
		{
			case 0:
				return getBodyComp(contact.getFixtureB());
			case 1:
				return getBodyComp(contact.getFixtureA());
			default:
				return null;
		}
	}

	/**
	 * Returns the index if the contact contains a fixture on the body.
	 * 
	 * @param haystackContact
	 * @param needleBody
	 * @return 0 [A], 1 [B], -1 [not found]
	 */
	public static int getIndexFixture(Contact haystackContact,
			LttlPhysicsBody needleBody)
	{
		if (haystackContact.getFixtureA().getBody() == needleBody.getBody())
		{
			return 0;
		}
		else if (haystackContact.getFixtureB().getBody() == needleBody
				.getBody()) { return 1; }
		return -1;
	}

	/**
	 * Gets the points involved in this shape (for circles it uses AABB) and adds to the Vector2Array given
	 */
	public static void getShapePoints(Shape shape, Vector2Array arrayToAddTo)
	{
		switch (shape.getType())
		{
			case Chain:
			{
				ChainShape s = (ChainShape) shape;
				for (int i = 0, n = s.getVertexCount(); i < n; i++)
				{
					s.getVertex(i, tmp);
					arrayToAddTo.add(tmp);
				}
				break;
			}
			case Circle:
			{
				CircleShape s = (CircleShape) shape;
				Vector2 pos = s.getPosition();
				float r = s.getRadius();
				arrayToAddTo.add(pos.x + r, pos.y + r);
				arrayToAddTo.add(pos.x + r, pos.y - r);
				arrayToAddTo.add(pos.x - r, pos.y - r);
				arrayToAddTo.add(pos.x - r, pos.y + r);
				break;
			}
			case Edge:
			{
				EdgeShape s = (EdgeShape) shape;
				s.getVertex0(tmp);
				arrayToAddTo.add(tmp);
				s.getVertex1(tmp);
				arrayToAddTo.add(tmp);
				s.getVertex2(tmp);
				arrayToAddTo.add(tmp);
				s.getVertex3(tmp);
				arrayToAddTo.add(tmp);
				break;
			}
			case Polygon:
			{
				PolygonShape s = (PolygonShape) shape;
				for (int i = 0, n = s.getVertexCount(); i < n; i++)
				{
					s.getVertex(i, tmp);
					arrayToAddTo.add(tmp);
				}
				break;
			}
		}
	}

	/**
	 * Calculates force.
	 * 
	 * @param forceMultiplier
	 * @param isRelativeMass
	 * @param isRelativeGravity
	 * @param body
	 *            if null, will skip isRelativeMass and isRelativeGravity
	 * @return
	 */
	public static float getForce(float forceMultiplier, boolean isRelativeMass,
			boolean isRelativeGravity, Body body)
	{
		if (isRelativeMass && body != null)
		{
			forceMultiplier *= body.getMass();
		}
		if (isRelativeGravity && body != null)
		{
			forceMultiplier *= body.getGravityScale()
					* Lttl.game.getPhysics().gravity.len();
		}
		return forceMultiplier;
	}

	/**
	 * Get the reaction torque on otherBody in N*m (Newtons*mass?).<br>
	 * You can use reaction torque to break joints or trigger other game events. This function may do some computations,
	 * so don't call it if you don't need the result.<br>
	 * It will be different after each step, so to get every change best to use it udirng
	 * {@link LttlComponent#onLateFixedUpdate()}.
	 */
	static public float getReactionTorque(Joint joint)
	{
		return joint
				.getReactionTorque(1 / Lttl.game.getSettings().fixedDeltaTime);
	}

	/**
	 * Get the reaction force on otherBody at the joint anchor in Newtons.<br>
	 * You can use reaction force to break joints or trigger other game events. This function may do some computations,
	 * so don't call it if you don't need the result.<br>
	 * It will be different after each step, so to get every change best to use it udirng
	 * {@link LttlComponent#onLateFixedUpdate()}.
	 * 
	 * @return a shared vector2 for this joint
	 */
	static public Vector2 getReactionForce(Joint joint)
	{
		return joint
				.getReactionForce(1 / Lttl.game.getSettings().fixedDeltaTime);
	}
}
