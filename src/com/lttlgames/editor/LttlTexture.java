package com.lttlgames.editor;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.lttlgames.editor.annotations.GuiButton;
import com.lttlgames.editor.annotations.Persist;

/**
 * NOTE: this needs to refresh() onStart
 * 
 * @author Josh
 */
@Persist(-9017)
public class LttlTexture extends LttlTextureBase
{
	private AtlasRegion refAtlasRegion;
	private boolean didWarning = false;

	// used for JSON creation, cause no params
	public LttlTexture()
	{
	}

	/**
	 * Sets the atlas region reference. This is useful if you want to set the texture based on another LttlTexture but
	 * keep them as seperate objects.
	 * 
	 * @param ar
	 */
	public void setAR(AtlasRegion ar)
	{
		this.refAtlasRegion = ar;
	}

	/**
	 * Sets texture name and refreshes
	 * 
	 * @param textureRegionName
	 * @return
	 */
	public AtlasRegion setAndRefresh(String textureRegionName)
	{
		this.textureRegionName = textureRegionName;
		return refresh();
	}

	/**
	 * Finds a texture with set name (from atlas or not), sets it for a quicker reference via get(), returns it.<br>
	 * You don't want to use this every frame since it is doing a string comparison.<br>
	 * <br>
	 * <b>Note:</b> This automatically is ran during staging, if the texture is null and but there is a region name.
	 * 
	 * @return
	 */
	@GuiButton
	public AtlasRegion refresh()
	{
		if (textureRegionName != null && !textureRegionName.isEmpty())
		{
			// search all loaded scenes
			for (LttlScene s : Lttl.scenes.getAllLoaded(true))
			{
				refAtlasRegion = s.getTextureManager().findAtlasRegion(
						textureRegionName, false);
				if (refAtlasRegion != null)
				{
					break;
				}
			}
			if (refAtlasRegion == null)
			{
				if (!didWarning)
				{
					didWarning = true;
					Lttl.logNote("Refreshing LttlTexture: AtlasRegion not found when searching for "
							+ textureRegionName);
				}
			}
			else
			{
				didWarning = false;
			}
		}
		else
		{
			refAtlasRegion = null;
		}
		return refAtlasRegion;
	}

	@GuiButton
	private void checkTexture()
	{
		if (getTex() == null)
		{
			Lttl.logNote("Check Texture: Null");
		}
		else
		{
			Lttl.logNote("Check Texture: Exists");
		}
	}

	/**
	 * Returns the atlas region based on last refresh, does not auto refresh.
	 * 
	 * @return
	 */
	public AtlasRegion getAR()
	{
		return refAtlasRegion;
	}

	/**
	 * Returns texture of the atlas region based on last refresh, does not auto refresh.
	 * 
	 * @return
	 */
	public Texture getTex()
	{
		if (refAtlasRegion != null)
		{
			return refAtlasRegion.getTexture();
		}
		else
		{
			return null;
		}
	}

	/**
	 * Compares if using same texture.
	 * 
	 * @param lt
	 * @return
	 */
	public boolean hasSameTexture(LttlTexture lt)
	{
		return getTex() == lt.getTex();
	}

	/**
	 * Nulls out the reference to the atlas region. Will need to do a refresh to obtain a new reference.
	 */
	@Override
	public void clearReference()
	{
		refAtlasRegion = null;
	}
}
