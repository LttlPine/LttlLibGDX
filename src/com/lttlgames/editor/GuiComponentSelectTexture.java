package com.lttlgames.editor;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;

public class GuiComponentSelectTexture extends GuiComponentSelectString
{
	GuiComponentSelectTexture(ProcessedFieldType pft, Object hostObject,
			int index, GuiComponentObject parent)
	{
		super(pft, hostObject, index, parent);
	}

	@Override
	void loadOptions()
	{
		optionContainers = new ArrayList<GuiSelectOptionContainer>();
		optionContainers.add(new GuiSelectOptionContainer("", "")); // add blank
		for (LttlScene ls : Lttl.scenes.getAllLoaded(true))
		{
			for (AtlasRegion ar : ls.getTextureManager().getAllTextures(false))
			{
				optionContainers
						.add(new GuiSelectOptionContainer(ar.name, ar.name
								+ " ["
								+ ls.getTextureManager().getTextureId(ar) + "]"));
			}
		}
	}
}
