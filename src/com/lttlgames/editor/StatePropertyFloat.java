package com.lttlgames.editor;

import com.lttlgames.editor.annotations.Persist;

@Persist(-9055)
public class StatePropertyFloat extends StateProperty<Float>
{
	public StatePropertyFloat()
	{
		super();
	}

	public StatePropertyFloat(boolean startActive)
	{
		super(startActive);
	}

	float[] getTargetValues()
	{
		return new float[]
		{ value };
	}

	@Override
	Float getDefaultValue()
	{
		return 0f;
	}

	@Override
	void setTargetValues(float[] values)
	{
		value = values[0];
	}
}
