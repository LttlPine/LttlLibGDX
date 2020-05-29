package com.lttlgames.components;

import com.lttlgames.editor.Lttl;
import com.lttlgames.editor.LttlComponent;
import com.lttlgames.editor.LttlRenderer;
import com.lttlgames.editor.annotations.ComponentRequired;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.helpers.FloatRangeRandom;

@ComponentRequired(LttlRenderer.class)
@Persist(-90110)
public class LttlUvOffsetRandomizer extends LttlComponent
{
	@Persist(9011000)
	public FloatRangeRandom uvX = new FloatRangeRandom();
	@Persist(9011001)
	public FloatRangeRandom uvY = new FloatRangeRandom();
	@Persist(9011002)
	@GuiMin(0)
	public float interval = .5f;
	@Persist(9011003)
	public boolean runInEditor = true;

	private float duration = 0;

	@Override
	public void onEditorUpdate()
	{
		if (runInEditor) update();
	}

	@Override
	public void onUpdate()
	{
		update();
	}

	private void update()
	{
		interval = Math.abs(interval);
		duration += Lttl.game.getDeltaTime();
		if (duration > interval)
		{
			duration = duration % interval;
			r().uvOffsetShader.x = uvX.newValue();
			r().uvOffsetShader.y = uvY.newValue();
		}
	}
}
