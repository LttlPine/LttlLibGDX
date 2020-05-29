package com.lttlgames.helpers;

import com.lttlgames.editor.annotations.Persist;

@Persist(-90102)
public class FloatRangeRandomTimeline extends RangeRandomTimeline
{
	@Persist(9010201)
	public FloatRangeRandom high = new FloatRangeRandom();
	@Persist(9010200)
	public FloatRangeRandom low = new FloatRangeRandom();

	/**
	 * Generates a new low value
	 * 
	 * @return
	 */
	public float newLow()
	{
		return low.newValue();
	}

	/**
	 * Generates a new high value (not taking relative into consideration)
	 * 
	 * @return
	 */
	public float newHigh()
	{
		return high.newValue();
	}

	/**
	 * Generates a new high value, taking into consideration {@link FloatRangeRandomTimeline#isRelative} by using the
	 * provided low value
	 * 
	 * @param low
	 * @return
	 */
	public float newHigh(float low)
	{
		return isRelative ? low + high.newValue() : high.newValue();
	}

	/**
	 * Returns the value at a specific timeline position (between 0-1) and scales it to the low and high values
	 * 
	 * @param low
	 * @param high
	 * @param lerp
	 * @return
	 */
	public float lerp(float low, float high, float lerp)
	{
		return LttlMath.Lerp(low, high, timeline.getValue(lerp));
	}

	/**
	 * Returns the value on the timeline at the percent
	 * 
	 * @param percent
	 * @return
	 */
	public float getValue(float percent)
	{
		return timeline.getValue(percent);
	}
}
