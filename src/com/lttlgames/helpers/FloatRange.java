package com.lttlgames.helpers;

import com.lttlgames.editor.annotations.GuiTwoColumn;
import com.lttlgames.editor.annotations.Persist;

@GuiTwoColumn
@Persist(-9096)
public class FloatRange
{
	@Persist(909600)
	public float min;
	@Persist(909601)
	public float max;

	public FloatRange()
	{
	}

	public FloatRange(float min, float max)
	{
		this.min = min;
		this.max = max;
	}

	/**
	 * returns a random value between min and max
	 * 
	 * @return
	 */
	public float newValue()
	{
		return LttlMath.random(min, max);
	}

	public boolean isBetween(float value)
	{
		return value > min && value < max;
	}

	public float clamp(float value)
	{
		return LttlMath.clamp(value, min, max);
	}

	public float lerp(float value)
	{
		return LttlMath.Lerp(min, max, value);
	}

	public float interp(float value, EaseType ease)
	{
		return LttlMath.interp(min, max, value, ease);
	}

	public float interp(float value, LttlTimeline timeline)
	{
		return timeline.getValue(value, min, max);
	}
}
