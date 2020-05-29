package com.lttlgames.editor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;

public class GuiComponentSelectTextureAnimation extends
		GuiComponentSelectString
{
	GuiComponentSelectTextureAnimation(ProcessedFieldType pft,
			Object hostObject, int index, GuiComponentObject parent)
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
			for (Iterator<Entry<String, ArrayList<AtlasRegion>>> it = ls
					.getTextureManager().getAtlasAnimationTexturesHashmap()
					.entrySet().iterator(); it.hasNext();)
			{
				Entry<String, ArrayList<AtlasRegion>> set = it.next();
				ArrayList<AtlasRegion> list = set.getValue();
				optionContainers.add(new GuiSelectOptionContainer(set.getKey(),
						set.getKey()
								+ (list.size() > 0 ? " ["
										+ ls.getTextureManager().getTextureId(
												list.get(0)) + "]" : "")));
			}
		}
	}
}
