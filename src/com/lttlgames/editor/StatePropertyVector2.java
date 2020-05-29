package com.lttlgames.editor;

import com.badlogic.gdx.math.Vector2;
import com.lttlgames.editor.annotations.Persist;

@Persist(-9057)
public class StatePropertyVector2 extends StateProperty<Vector2>
{
	public StatePropertyVector2()
	{
		super();
	}

	public StatePropertyVector2(boolean startActive)
	{
		super(startActive);
	}

	@Override
	float[] getTargetValues()
	{
		return new float[]
		{ value.x, value.y };
	}

	@Override
	void setTargetValues(float[] values)
	{
		value.x = values[0];
		value.y = values[1];
	}

	@Override
	Vector2 getDefaultValue()
	{
		return new Vector2();
	}
}
