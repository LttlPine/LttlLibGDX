package com.lttlgames.editor;

import com.lttlgames.editor.annotations.GuiHide;
import com.lttlgames.editor.annotations.GuiMax;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.Persist;

@Persist(-9066)
class StateKeyframeNode extends KeyframeNode
{
	@Persist(906601)
	@GuiHide
	String stateName = "";
	@Persist(906602)
	@GuiMin(0)
	@GuiMax(1)
	float targetPercentage = 1;
}
