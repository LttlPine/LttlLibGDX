package com.lttlgames.editor;

import com.badlogic.gdx.graphics.Color;
import com.lttlgames.editor.annotations.Persist;

@Persist(-9058)
public class StatePropertyColor extends StateProperty<Color>
{
	public StatePropertyColor()
	{
		super();
	}

	public StatePropertyColor(boolean startActive)
	{
		super(startActive);
	}

	@Override
	float[] getTargetValues()
	{
		return new float[]
		{ value.r, value.g, value.b, value.a };
	}

	@Override
	Color getDefaultValue()
	{
		return new Color(Color.WHITE);
	}

	@Override
	void setTargetValues(float[] values)
	{
		value.r = values[0];
		value.g = values[1];
		value.b = values[2];
		value.a = values[3];
	}
}
