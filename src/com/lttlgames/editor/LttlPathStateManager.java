package com.lttlgames.editor;

import com.lttlgames.editor.annotations.Persist;

@Persist(-9083)
public class LttlPathStateManager extends
		LttlStateManager<LttlPath, LttlPathState>
{
	@Override
	public Class<LttlPath> getTargetClass()
	{
		return LttlPath.class;
	}

	@Override
	public Class<LttlPathState> getStateClass()
	{
		return LttlPathState.class;
	}
}