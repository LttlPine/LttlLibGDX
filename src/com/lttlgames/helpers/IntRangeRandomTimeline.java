package com.lttlgames.helpers;

import com.lttlgames.editor.annotations.Persist;

@Persist(-90103)
public class IntRangeRandomTimeline extends RangeRandomTimeline
{
	@Persist(9010301)
	public IntRangeRandom high = new IntRangeRandom();
	@Persist(9010300)
	public IntRangeRandom low = new IntRangeRandom();

	/**
	 * Generates a new low value
	 * 
	 * @return
	 */
	public int newLow()
	{
		return low.newValue();
	}

	/**
	 * Generates a new high value (not taking relative into consideration)
	 * 
	 * @return
	 */
	public int newHigh()
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
	public int newHigh(float low)
	{
		return (int) (isRelative ? low + high.newValue() : high.newValue());
	}

	/**
	 * Returns the int value at a specific timeline position (between 0-1) and scales it to the low and high values
	 * 
	 * @param low
	 * @param high
	 * @param percent
	 * @return
	 */
	public int lerp(float low, float high, float percent)
	{
		return (int) LttlMath.Lerp(low, high, timeline.getValue(percent));
	}

	/**
	 * Returns the float value at a specific timeline position (between 0-1) and scales it to the low and high values
	 * 
	 * @param low
	 * @param high
	 * @param percent
	 * @return
	 */
	public float lerpFloat(float low, float high, float percent)
	{
		return LttlMath.Lerp(low, high, timeline.getValue(percent));
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
