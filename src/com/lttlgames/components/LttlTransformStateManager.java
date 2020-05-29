package com.lttlgames.components;

import com.lttlgames.editor.LttlStateManager;
import com.lttlgames.editor.LttlTransform;
import com.lttlgames.editor.LttlTransformState;
import com.lttlgames.editor.annotations.Persist;

@Persist(-9060)
public class LttlTransformStateManager extends
		LttlStateManager<LttlTransform, LttlTransformState>
{
	@Override
	public Class<LttlTransform> getTargetClass()
	{
		return LttlTransform.class;
	}

	@Override
	public Class<LttlTransformState> getStateClass()
	{
		return LttlTransformState.class;
	}
}
