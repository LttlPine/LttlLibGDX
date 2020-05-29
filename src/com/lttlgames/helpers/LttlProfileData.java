package com.lttlgames.helpers;

import com.lttlgames.editor.Lttl;
import com.lttlgames.editor.annotations.GuiShow;
import com.lttlgames.editor.annotations.GuiReadOnly;

@GuiShow
public class LttlProfileData
{
	String name;
	float current = 0;
	@GuiShow
	@GuiReadOnly
	float peak = 0;

	public LttlProfileData(String name)
	{
		this.name = name;
	}

	/**
	 * Adds 1 to this data. <br>
	 * Note: this automatically checks if current world has 'showProfilerData' enabled.
	 */
	public void add()
	{
		add(1);
	}

	/**
	 * Adds the specified amount.
	 * 
	 * @param count
	 */
	public void add(int count)
	{
		if (!Lttl.game.inEditor() || Lttl.game.isSettingUp()
				|| !Lttl.editor.getSettings().showProfilerData) return;
		current += count;
	}
}
