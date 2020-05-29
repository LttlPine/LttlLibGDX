package com.lttlgames.components;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.utils.Array;
import com.lttlgames.editor.Lttl;
import com.lttlgames.editor.LttlPhysicsBody;
import com.lttlgames.editor.annotations.Persist;

@Persist(-9029)
public class LttlDemoComponent extends LttlPhysicsAdapter
{
	Array<Contact> contacts = new Array<Contact>();

	LttlPhysicsBody thisBody;

	LttlPhysicsBody otherBody;

	@Override
	public void onBodyEnter(final LttlPhysicsBody thisBody,
			final LttlPhysicsBody otherBody)
	{
		this.thisBody = thisBody;
		this.otherBody = otherBody;
		Lttl.dump(debugText() + thisBody.toString() + " start touching "
				+ otherBody.toString());
	}

	@Override
	public void onBodyExit(final LttlPhysicsBody thisBody,
			final LttlPhysicsBody otherBody)
	{
		Lttl.dump(debugText() + thisBody.toString() + " stop touching "
				+ otherBody.toString());
	}

	private String debugText()
	{
		return Lttl.game.getPhysics().getStepCount() + " "
				+ Lttl.game.getPhysics().isStepping() + ": ";
	}

	int count = 0;

	@Override
	public void preSolve(LttlPhysicsBody thisBody, Contact contact,
			Manifold oldManifold)
	{
	}

	@Override
	public void onUpdate()
	{
	}

	@Override
	public void beginContact(LttlPhysicsBody thisBody, Contact contact)
	{
	}
}