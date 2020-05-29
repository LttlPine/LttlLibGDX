package com.lttlgames.helpers;

import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.GuiToggleOnly;
import com.lttlgames.editor.annotations.GuiTwoColumn;
import com.lttlgames.editor.annotations.Persist;

@Persist(-90100)
public class IntRangeRandom
{
	@Persist(9010000)
	@GuiTwoColumn
	public int base;
	@Persist(9010001)
	@GuiTwoColumn
	@GuiMin(0)
	public int random;
	@Persist(9010002)
	@GuiToggleOnly
	public boolean randomSign = false;

	/**
	 * This is the lowest value possible, not including randomSign
	 * 
	 * @return
	 */
	public int getLowest()
	{
		return base - random;
	}

	/**
	 * This is the highest value possible, not including randomSign
	 * 
	 * @return
	 */
	public int getHighest()
	{
		return base + random;
	}

	public int newValue()
	{
		return (randomSign ? LttlMath.RandomSign() : 1)
				* (base + LttlMath.random(-random, random));
	}
}
