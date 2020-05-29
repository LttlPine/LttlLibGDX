package com.lttlgames.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

/**
 * @author Josh
 */
public class LttlFontManager
{
	private static final String defaultChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890\"!`?'.,;:()[]{}<>|/@\\^$-%+=#_&~*";
	public static final String defaultFont = "Default";

	/**
	 * Automaticaluu sets folders, loads fonts.
	 */
	LttlFontManager()
	{
		setupFolders();
		loadFontNames();
	}

	private HashMap<String, ArrayList<LttlFont>> fontMap = new HashMap<String, ArrayList<LttlFont>>();
	FileHandle fontDirInternal;
	FileHandle fontDirRelative;

	/**
	 * Checks folders and creates them.
	 */
	private void setupFolders()
	{
		fontDirInternal = LttlResourceManager
				.getFileHandleInternalAssets("resources/fonts");

		if (Lttl.game.inEditor())
		{
			// used for making the directory (only in editor)
			fontDirRelative = LttlResourceManager.getFileHandle(
					"resources/fonts", FileType.Absolute, true);
			fontDirRelative.mkdirs();
		}
	}

	/**
	 * loads the font names and preps
	 */
	private void loadFontNames()
	{
		// convert each font file to font
		for (FileHandle fh : fontDirInternal.list())
		{
			// only search for .ttf files
			if (fh.extension().equals("ttf"))
			{
				String name = fh.nameWithoutExtension();
				if (!fontMap.keySet().contains(name))
				{
					fontMap.put(name, new ArrayList<LttlFont>());
				}
			}
		}
		// add default font
		if (!fontMap.keySet().contains(defaultFont))
		{
			fontMap.put(defaultFont, new ArrayList<LttlFont>());
		}
	}

	/**
	 * Retrieves the shared font object from cache or creates a new one.
	 * 
	 * @param name
	 * @param fontSize
	 * @param comp
	 *            the requesting component, null will register to no component, so it will never be cleaned (debug use
	 *            really)
	 * @return null if not found
	 */
	LttlFont getFont(String name, int fontSize, LttlComponent comp)
	{
		/* Check to see if font at this fontSize already exists*/
		// get this fonts sizes list
		ArrayList<LttlFont> list = fontMap.get(name);
		// if no font by that name, return null
		if (list == null)
		{
			Lttl.logNote("No font by name of '" + name + "' could be found.");
			return null;
		}
		// iterate through each font and check for sizes
		for (LttlFont font : list)
		{
			// a font already exists at this size, set that it is being used by this component and return it
			if (font.getFontSize() == fontSize)
			{
				font.startUsing(comp);
				return font;
			}
		}

		/* Need to create a new font */
		LttlFont newFont = creatFont(name, fontSize);
		newFont.startUsing(comp);

		// add to fonts list
		list.add(newFont);

		return newFont;
	}

	public ArrayList<String> getFontNames()
	{
		ArrayList<String> list = new ArrayList<String>();
		for (String s : fontMap.keySet())
		{
			list.add(s);
		}
		return list;
	}

	/**
	 * Returns all the fonts and sizes that are created.
	 * 
	 * @return
	 */
	public ArrayList<LttlFont> getAllCachedFonts()
	{
		ArrayList<LttlFont> all = new ArrayList<LttlFont>();
		for (ArrayList<LttlFont> list : fontMap.values())
		{
			for (LttlFont font : list)
			{
				all.add(font);
			}
		}
		return all;
	}

	/**
	 * Returns all sizes created for a specific font.
	 * 
	 * @return
	 */
	public ArrayList<LttlFont> getCachedFonts(String fontName)
	{
		ArrayList<LttlFont> list = fontMap.get(fontName);
		return (list == null) ? new ArrayList<LttlFont>()
				: new ArrayList<LttlFont>(list);
	}

	/**
	 * |
	 * 
	 * @param fontName
	 * @param size
	 * @return
	 */
	private LttlFont creatFont(String fontName, int size)
	{
		if (!fontMap.keySet().contains(fontName)) { return null; }

		FreeTypeFontGenerator generator;
		if (fontName.equals(defaultFont))
		{
			// TODO use LttlResourceManager to get class path assets files
			generator = new FreeTypeFontGenerator(
					LttlResourceManager
							.getFileHandleClass(defaultFont + ".ttf"));
		}
		else
		{
			generator = new FreeTypeFontGenerator(
					fontDirInternal.child(fontName + ".ttf"));
		}
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = size;
		parameter.characters = defaultChars;
		parameter.packer = null;
		parameter.flip = false;
		parameter.genMipMaps = false;
		parameter.minFilter = TextureFilter.Linear;
		parameter.magFilter = TextureFilter.Linear;
		BitmapFont bFont = generator.generateFont(parameter);
		generator.dispose();
		return new LttlFont(bFont, fontName, size);
	}

	/**
	 * Removes all LttlFonts not being used and disposes their textures. Automatically called when a LttlFontRenderer is
	 * destroyed.
	 */
	public void clean()
	{
		for (ArrayList<LttlFont> list : fontMap.values())
		{
			for (Iterator<LttlFont> it = list.iterator(); it.hasNext();)
			{
				LttlFont font = it.next();
				if (font.getObjectUsingCount() == 0)
				{
					// prevents memory leak
					font.getBitmapFont().dispose();
					it.remove();
				}
			}
		}
	}

}
