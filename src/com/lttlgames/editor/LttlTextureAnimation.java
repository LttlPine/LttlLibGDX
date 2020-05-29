package com.lttlgames.editor;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.lttlgames.editor.annotations.GuiButton;
import com.lttlgames.editor.annotations.Persist;

@Persist(-9018)
public class LttlTextureAnimation extends LttlTextureBase
{
	private ArrayList<AtlasRegion> refAtlasRegions;

	// used for JSON creation, cause no params
	public LttlTextureAnimation()
	{

	}

	/**
	 * Sets the atlas regions reference. This is useful if you want to set the texture based on another
	 * LttlTextureAniamtion but keep them as seperate objects.
	 * 
	 * @param ar
	 */
	public void setARs(ArrayList<AtlasRegion> ars)
	{
		this.refAtlasRegions = ars;
	}

	/**
	 * Finds textures with set name (from atlas) in all loaded scenes, sets it for a quicker reference via get(),
	 * returns it.<br>
	 * You don't want to use this too often since it can be slow.
	 * 
	 * @return
	 */
	@GuiButton
	public ArrayList<AtlasRegion> refresh()
	{
		if (textureRegionName != null && !textureRegionName.isEmpty())
		{
			// always search all loaded scenes
			for (LttlScene s : Lttl.scenes.getAllLoaded(true))
			{
				refAtlasRegions = s.getTextureManager().findAtlasRegions(
						textureRegionName, false);
				if (refAtlasRegions != null)
				{
					break;
				}
			}
			if (refAtlasRegions == null)
			{
				Lttl.logNote("Refreshing LttlTextureAnimation: AtlasRegions not found when searching for "
						+ textureRegionName);
			}
		}
		else
		{
			refAtlasRegions = null;
		}
		return refAtlasRegions;
	}

	/**
	 * Modifies the textures name then finds the texture with given name (from atlas), sets it for a quicker reference
	 * via get(), returns it.<br>
	 * You don't want to use this too often since it is slow.
	 * 
	 * @param textureRegionName
	 * @return
	 */
	@GuiButton
	public ArrayList<AtlasRegion> setTexture(String textureRegionName)
	{
		this.textureRegionName = textureRegionName;
		return refresh();
	}

	/**
	 * Returns the atlas regions based on last refresh.
	 * 
	 * @return
	 */
	public ArrayList<AtlasRegion> getARs()
	{
		// if null and there is a name, then try and obtain the region.
		if (refAtlasRegions == null)
		{
			refresh();
		}
		return refAtlasRegions;
	}

	/**
	 * Nulls out atlas region references. Must refresh().
	 */
	@Override
	public void clearReference()
	{
		refAtlasRegions = null;
	}
}
