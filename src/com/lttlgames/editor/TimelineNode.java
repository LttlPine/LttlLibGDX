package com.lttlgames.editor;

import javax.swing.JSlider;

import com.lttlgames.editor.annotations.GuiDecimalPlaces;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.IgnoreCrawl;
import com.lttlgames.editor.annotations.Persist;

@Persist(-9065)
abstract class TimelineNode
{
	@Persist(906501)
	@GuiMin(0)
	@GuiDecimalPlaces(2)
	public float time;
	@Persist(906502)
	public boolean active = true;

	protected float undoValue;

	// editor use only
	@IgnoreCrawl
	JSlider slider;
	@IgnoreCrawl
	AnimGuiSequence sequenceGui;
}
