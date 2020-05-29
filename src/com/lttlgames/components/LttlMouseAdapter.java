package com.lttlgames.components;

import com.lttlgames.components.interfaces.MouseListener;
import com.lttlgames.editor.LttlComponent;
import com.lttlgames.editor.LttlPhysicsBody;
import com.lttlgames.editor.LttlPhysicsFixture;
import com.lttlgames.editor.annotations.GuiHideComponentList;
import com.lttlgames.editor.annotations.Persist;

@Persist(-90139)
@GuiHideComponentList
public class LttlMouseAdapter extends LttlComponent implements MouseListener
{
	@Override
	public void onMouseEnterBody(LttlPhysicsBody bodyComp,
			LttlPhysicsFixture fixtureComp, int zIndex)
	{
	}

	@Override
	public void onMouseExitBody(LttlPhysicsBody bodyComp)
	{
	}

	@Override
	public void onMouseContactBody(LttlPhysicsBody bodyComp,
			LttlPhysicsFixture fixtureComp, int zIndex)
	{
	}

	@Override
	public void onMouseEnterFixture(LttlPhysicsFixture fixtureComp, int zIndex)
	{
	}

	@Override
	public void onMouseExitFixture(LttlPhysicsFixture fixtureComp)
	{
	}

	@Override
	public void onMouseContactFixture(LttlPhysicsFixture fixtureComp, int zIndex)
	{
	}

}
