package com.lttlgames.components;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.lttlgames.components.interfaces.PhysicsListener;
import com.lttlgames.editor.LttlComponent;
import com.lttlgames.editor.LttlPhysicsBase;
import com.lttlgames.editor.LttlPhysicsBody;
import com.lttlgames.editor.annotations.GuiHideComponentList;
import com.lttlgames.editor.annotations.Persist;

@Persist(-90137)
@GuiHideComponentList
public abstract class LttlPhysicsAdapter extends LttlComponent implements
		PhysicsListener
{
	@Override
	public void onDestroyPhysics(LttlPhysicsBase object)
	{
	}

	@Override
	public void onBodyEnter(LttlPhysicsBody thisBody, LttlPhysicsBody otherBody)
	{
	}

	@Override
	public void onBodyExit(LttlPhysicsBody thisBody, LttlPhysicsBody otherBody)
	{
	}

	@Override
	public void beginContact(LttlPhysicsBody thisBody, Contact contact)
	{
	}

	@Override
	public void endContact(LttlPhysicsBody thisBody, Contact contact)
	{
	}

	@Override
	public void postSolve(LttlPhysicsBody thisBody, Contact contact,
			ContactImpulse impulse)
	{
	}

	@Override
	public void preSolve(LttlPhysicsBody thisBody, Contact contact,
			Manifold oldManifold)
	{
	}
}
