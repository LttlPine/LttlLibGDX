package com.lttlgames.editor;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.IntArray;
import com.lttlgames.editor.annotations.IgnoreCrawl;

public class LttlFont
{
	private String name;
	private int fontSize;
	// TODO can probably remove this annotation
	@IgnoreCrawl
	private BitmapFont bFont;
	private IntArray compsIdUsing = new IntArray(1);

	LttlFont(BitmapFont bFont, String name, int fontSize)
	{
		this.bFont = bFont;
		this.name = name;
		this.fontSize = fontSize;
	}

	public String getName()
	{
		return name;
	}

	public int getFontSize()
	{
		return fontSize;
	}

	public BitmapFont getBitmapFont()
	{
		return bFont;
	}

	public BitmapFont generateBitmapFont()
	{
		Texture tex = getBitmapFont().getRegion().getTexture();
		return new BitmapFont(getBitmapFont().getData(), new AtlasRegion(tex,
				0, 0, tex.getWidth(), tex.getHeight()), false);
	}

	/**
	 * Returns the number of objects using this font
	 * 
	 * @return
	 */
	public int getObjectUsingCount()
	{
		return compsIdUsing.size;
	}

	void startUsing(LttlComponent comp)
	{
		// if comp is null, then register it with an id of -1, so it will never be cleaned
		int id = comp != null ? comp.getId() : -1;
		if (!compsIdUsing.contains(id))
		{
			compsIdUsing.add(id);
		}
	}

	void stopUsing(LttlComponent comp)
	{
		// if no comp then don't clean any fonts
		if (comp == null) return;
		compsIdUsing.removeValue(comp.getId());
		Lttl.game.getFontManager().clean();
	}
}
