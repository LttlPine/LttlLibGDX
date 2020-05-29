package com.lttlgames.helpers;

import com.lttlgames.editor.annotations.GuiStringDataInherit;
import com.lttlgames.editor.annotations.GuiToggleOnly;
import com.lttlgames.editor.annotations.GuiTwoColumn;
import com.lttlgames.editor.annotations.Persist;

@Persist(-90107)
public abstract class RangeRandomTimeline
{
	@Persist(9010700)
	@GuiStringDataInherit(0)
	public LttlTimeline timeline = new LttlTimeline();
	/**
	 * optional, mainly for gui
	 */
	@GuiTwoColumn
	@Persist(9010702)
	@GuiToggleOnly
	public boolean isActive = true;
	/**
	 * is high defined by always adding on top of low or are they independent values. This value is not forced to be
	 * used since can do {@link FloatRangeRandomTimeline#newHigh()}
	 */
	@GuiTwoColumn
	@Persist(9010701)
	@GuiToggleOnly
	public boolean isRelative = false;
}
