package com.lttlgames.components;

import com.lttlgames.editor.LttlRenderer;
import com.lttlgames.editor.LttlRendererState;
import com.lttlgames.editor.LttlStateManager;
import com.lttlgames.editor.annotations.Persist;

@Persist(-9062)
public class LttlRendererStateManager extends
		LttlStateManager<LttlRenderer, LttlRendererState>
{

	@Override
	public Class<LttlRenderer> getTargetClass()
	{
		return LttlRenderer.class;
	}

	@Override
	public Class<LttlRendererState> getStateClass()
	{
		return LttlRendererState.class;
	}
}
