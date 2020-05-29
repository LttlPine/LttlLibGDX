package com.lttlgames.helpers;

import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.GuiToggleOnly;
import com.lttlgames.editor.annotations.GuiTwoColumn;
import com.lttlgames.editor.annotations.Persist;

@Persist(-9099)
public class FloatRangeRandom
{
	@Persist(909900)
	@GuiTwoColumn
	public float base;
	@Persist(909901)
	@GuiMin(0)
	@GuiTwoColumn
	public float random;
	@Persist(909902)
	@GuiToggleOnly
	public boolean randomSign = false;

	/**
	 * This is the lowest value possible, not including randomSign
	 * 
	 * @return
	 */
	public float getLowest()
	{
		return base - random;
	}

	/**
	 * This is the highest value possible, not including randomSign
	 * 
	 * @return
	 */
	public float getHighest()
	{
		return base + random;
	}

	public float newValue()
	{
		return (randomSign ? LttlMath.RandomSign() : 1)
				* (base + LttlMath.random(-random, random));
	}
}
