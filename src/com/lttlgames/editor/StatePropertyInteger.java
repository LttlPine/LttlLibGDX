package com.lttlgames.editor;

import com.lttlgames.editor.annotations.Persist;

@Persist(-9056)
public class StatePropertyInteger extends StateProperty<Integer>
{
	public StatePropertyInteger()
	{
		super();
	}

	public StatePropertyInteger(boolean startActive)
	{
		super(startActive);
	}

	@Override
	float[] getTargetValues()
	{
		return new float[]
		{ value };
	}

	@Override
	Integer getDefaultValue()
	{
		return 0;
	}

	@Override
	void setTargetValues(float[] values)
	{
		value = (int) values[0];
	}
}