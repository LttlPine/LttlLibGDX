package com.lttlgames.helpers;

import com.lttlgames.editor.annotations.GuiTwoColumn;
import com.lttlgames.editor.annotations.Persist;

@GuiTwoColumn
@Persist(-90104)
public class IntRange
{
	@Persist(9010400)
	public int min;
	@Persist(9010401)
	public int max;

	public IntRange()
	{
	}

	public IntRange(int min, int max)
	{
		this.min = min;
		this.max = max;
	}

	/**
	 * returns a random value between min and max
	 * 
	 * @return
	 */
	public int newValue()
	{
		return LttlMath.random(min, max);
	}

	public int clamp(int value)
	{
		return LttlMath.clamp(value, min, max);
	}

	public int lerp(float value)
	{
		return (int) LttlMath.Lerp(min, max, value);
	}

	public int interp(float value, EaseType ease)
	{
		return (int) LttlMath.interp(min, max, value, ease);
	}

	public int interp(float value, LttlTimeline timeline)
	{
		return (int) timeline.getValue(value, min, max);
	}
}
