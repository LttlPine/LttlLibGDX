package com.lttlgames.editor;

class GuiSelectOptionContainer
{
	public String display;
	public Object value;

	public GuiSelectOptionContainer(Object value, String display)
	{
		this.display = display;
		this.value = value;
	}

	public String toString()
	{
		return display;
	}
}
