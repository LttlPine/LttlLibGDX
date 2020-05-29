package com.lttlgames.components.interfaces;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.lttlgames.editor.LttlPhysicsBody;
import com.lttlgames.editor.LttlPhysicsFixture;
import com.lttlgames.editor.LttlPhysicsFixtureBodyBase;

/**
 * Callbacks if enabled {@link LttlPhysicsBody#callbackMouse}. Will only callback on components on the
 * {@link LttlPhysicsBody} or descendants.<br>
 * All mouse contained at start data can be captured via {@link LttlPhysicsFixtureBodyBase#containsMouseStart()} and
 * {@link LttlPhysicsFixtureBodyBase#getContainsMouseStartZindex()} without any callbacks or extra computation. This way
 * you can have a component that monitors all mouse contacts.
 */
public interface MouseListener
{
	/**
	 * Mouse entered this body this frame.
	 * 
	 * @param bodyComp
	 * @param fixtureComp
	 *            the fixture component that contains the mouse
	 * @param zIndex
	 *            the order of the callbacks in z space. 0 means it is the first callback and the most in foreground.
	 *            Same z spaces will return with different indexes.
	 */
	public void onMouseEnterBody(LttlPhysicsBody bodyComp,
			LttlPhysicsFixture fixtureComp, int zIndex);

	/**
	 * Mouse exited this body this frame.
	 */
	public void onMouseExitBody(LttlPhysicsBody bodyComp);

	/**
	 * The mouse is contained in this body this frame, specifically the given fixture. This will be called back
	 * immediately after {@link #onMouseEnterBody(LttlPhysicsBody, Fixture, int)}.
	 * 
	 * @param bodyComp
	 * @param fixtureComp
	 *            the fixture component that contains the mouse
	 * @param zIndex
	 *            the order of the callbacks in the body's z space. 0 means it is the first callback and the most in
	 *            foreground. Same z spaces will return with different indexes.
	 */
	public void onMouseContactBody(LttlPhysicsBody bodyComp,
			LttlPhysicsFixture fixtureComp, int zIndex);

	/**
	 * Mouse entered this fixture component this frame.<br>
	 * There is no callback for specific {@link Fixture} changes, just changes in state of the
	 * {@link LttlPhysicsFixture}.
	 * 
	 * @param fixtureComp
	 * @param zIndex
	 *            the order of the callbacks in z space. 0 means it is the first callback and the most in foreground.
	 *            Same z spaces will return with different indexes.
	 */
	public void onMouseEnterFixture(LttlPhysicsFixture fixtureComp, int zIndex);

	/**
	 * Mouse exited this fixture component this frame.<br>
	 * There is no callback for specific {@link Fixture} changes, just changes in state of the
	 * {@link LttlPhysicsFixture}.
	 * 
	 * @param fixtureComp
	 *            the fixture component the mouse exited
	 */
	public void onMouseExitFixture(LttlPhysicsFixture fixtureComp);

	/**
	 * The mouse is contained in this fixture component this frame. This will be called back immediately after
	 * {@link #onMouseEnterFixture(LttlPhysicsFixture, Fixture, int)}.<br>
	 * There is no callback for specific {@link Fixture} changes, just changes in state of the
	 * {@link LttlPhysicsFixture}.
	 * 
	 * @param fixtureComp
	 * @param zIndex
	 *            the order of the callbacks in z space. 0 means it is the first callback and the most in foreground.
	 *            Same z spaces will return with different indexes.
	 */
	public void onMouseContactFixture(LttlPhysicsFixture fixtureComp, int zIndex);
}
