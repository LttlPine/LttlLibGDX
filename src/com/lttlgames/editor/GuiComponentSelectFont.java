package com.lttlgames.editor;

import java.util.ArrayList;

public class GuiComponentSelectFont extends GuiComponentSelectString
{
	GuiComponentSelectFont(ProcessedFieldType pft, Object hostObject,
			int index, GuiComponentObject parent)
	{
		super(pft, hostObject, index, parent);
	}

	@Override
	void loadOptions()
	{
		optionContainers = new ArrayList<GuiSelectOptionContainer>();
		for (String s : Lttl.game.getFontManager().getFontNames())
		{
			optionContainers.add(new GuiSelectOptionContainer(s, s));
		}
	}
}
