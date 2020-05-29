package com.lttlgames.components.interfaces;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.lttlgames.editor.LttlComponent;
import com.lttlgames.editor.LttlPhysicsBase;
import com.lttlgames.editor.LttlPhysicsBody;
import com.lttlgames.editor.LttlPhysicsFixture;
import com.lttlgames.editor.PhysicsController;

/**
 * Physics callbacks. Needs to be on the same transform or a descendant of a {@link LttlPhysicsBody}.<br>
 * All the callbacks will fire within a fixed update. That is why there is no callback that is intended to fire every
 * frame (ie. onBodyStay), since that logic should be saved for {@link LttlComponent#onUpdate()} (if want it every frame
 * regardless if did a step) or {@link LttlComponent#onFixedUpdate()} or {@link LttlComponent#onLateFixedUpdate()} (if
 * only want to do it each step)
 */
public interface PhysicsListener
{
	/**
	 * Right <b>BEFORE</b> the physics object is removed from the physics world, this is called, thus it is still
	 * {@link LttlPhysicsBase#isInit()}.<br>
	 * {@link LttlPhysicsBase#callbackDestroyAny} must be true.<br>
	 * If destroying {@link LttlPhysicsBody} then it will be called first, followed by {@link LttlPhysicsFixture}s, then
	 * {@link LttlPhysicsJoint}s.
	 * 
	 * @param object
	 *            can be {@link LttlPhysicsBody} or {@link LttlPhysicsFixture} or {@link LttlPhysicsJoint}
	 */
	public void onDestroyPhysics(LttlPhysicsBase object);

	/**
	 * Callback for when start touching a body (including sensors). There is an individual callback each otherBody that
	 * this body begins touching.<br>
	 * Requires {@link LttlPhysicsBody#callbackBodyCollision} to be enabled.<br>
	 * Note: Use {@link LttlPhysicsBody#getContacts(LttlPhysicsBody, com.badlogic.gdx.utils.Array)} to get contacts.<br>
	 * Note: This will always be called during the step ({@link PhysicsController#isStepping()}), which means any
	 * physics world modification (forces, destroy, init, etc) needs to be done using
	 * {@link PhysicsController#registerAfterStep(com.lttlgames.helpers.LttlClosure)}.
	 * 
	 * @param thisBody
	 * @param otherBody
	 */
	public void onBodyEnter(LttlPhysicsBody thisBody, LttlPhysicsBody otherBody);

	/**
	 * Same as below, except for when not touching a body anymore.<br>
	 * Note: This could be called during the step or after the step, if it pone of the bodies (or fixture) was
	 * destroyed.<br>
	 * Note: The {@link Contact} for these bodies has been removed because they are not touching anymore.<br>
	 * Note: thisBody and the otherBody are not guaranteed to be {@link LttlPhysicsBase#isInit()}, since one could have
	 * been destroyed (which may be why this callback was triggered), would be wise to check.
	 * 
	 * @see PhysicsListener#onBodyEnter(LttlPhysicsBody, LttlPhysicsBody)
	 */
	public void onBodyExit(LttlPhysicsBody thisBody, LttlPhysicsBody otherBody);

	/**
	 * Only contacts that are relevant to this body. Called before any onBody callbacks. Called before presolve, so
	 * don't know if it is going to be disabled or not.<br>
	 * Inlcudes collisions with sensors, which can be disabled in this method, not in
	 * {@link #preSolve(LttlPhysicsBody, Contact, Manifold)}.<br>
	 * Note: This will always be called during the step ({@link PhysicsController#isStepping()}), which means any
	 * physics world modification (forces, destroy, init, etc) needs to be done using
	 * {@link PhysicsController#registerAfterStep(com.lttlgames.helpers.LttlClosure)}.
	 * 
	 * @see PhysicsController#beginContact(Contact)
	 */
	public void beginContact(LttlPhysicsBody thisBody, Contact contact);

	/**
	 * @see PhysicsListener#beginContact(LttlPhysicsBody, Contact)
	 * @see PhysicsController#beginContact(Contact)
	 */
	public void endContact(LttlPhysicsBody thisBody, Contact contact);

	/**
	 * This is called after beginContact and before postSolve. All contacts in fixture are touching, but here you can
	 * disabled the contact before the collision response is calculated.<br>
	 * Contacts that have at least one fixture that is a sensor will not exist here, since they will never have a
	 * collision response. To disable a contact with a sensor, use {@link #beginContact(LttlPhysicsBody, Contact)} and
	 * check if it has a sensor.<br>
	 * <br>
	 * Only use this method to disable collisions that you want to sometimes happen. If you never want a collision to
	 * happen, then use group and tags to filter out the collision entirely.<br>
	 * Note: This will always be called during the step ({@link PhysicsController#isStepping()}), which means any
	 * physics world modification (forces, destroy, init, etc) needs to be done using
	 * {@link PhysicsController#registerAfterStep(com.lttlgames.helpers.LttlClosure)}.
	 * 
	 * @see PhysicsController#preSolve(Contact, Manifold)
	 */
	public void preSolve(LttlPhysicsBody thisBody, Contact contact,
			Manifold oldManifold);

	/**
	 * Note: This will always be called during the step ({@link PhysicsController#isStepping()}), which means any
	 * physics world modification (forces, destroy, init, etc) needs to be done using
	 * {@link PhysicsController#registerAfterStep(com.lttlgames.helpers.LttlClosure)}.
	 * 
	 * @see PhysicsController#postSolve(Contact, ContactImpulse)
	 */
	public void postSolve(LttlPhysicsBody thisBody, Contact contact,
			ContactImpulse impulse);
}
