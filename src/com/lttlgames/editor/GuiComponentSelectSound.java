package com.lttlgames.editor;

import java.util.ArrayList;

public class GuiComponentSelectSound extends GuiComponentSelectString
{
	GuiComponentSelectSound(ProcessedFieldType pft, Object hostObject,
			int index, GuiComponentObject parent)
	{
		super(pft, hostObject, index, parent);
	}

	@Override
	void loadOptions()
	{
		optionContainers = new ArrayList<GuiSelectOptionContainer>();
		optionContainers.add(new GuiSelectOptionContainer("", "")); // add blank
		for (LttlScene scene : Lttl.scenes.getAllLoaded(true))
		{
			for (String s : scene.getAudioManager().getSoundNames())
			{
				optionContainers.add(new GuiSelectOptionContainer(s, s + " ["
						+ scene.getId() + "]"));
			}
		}
	}
}
