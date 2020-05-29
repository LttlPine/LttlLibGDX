package com.me.game.helpers;

import com.lttlgames.editor.LttlClassMap;
import com.me.game.components.ProcessingHelper;

/**
 * The specific class map for this project's game
 * 
 * @author Josh
 */
public class ClassMap extends LttlClassMap
{
	public ClassMap()
	{
		super();
	}

	public ClassMap(String packageName)
	{
		super(packageName);
	}

	// NOTE
	// NOTE
	// NOTE
	// NOTE
	// NOTE
	// NOTE
	// NOTE
	/* MAKE SURE CLASS IS NOT BEING USED ANYWHERE IN GAME */
	// can search for Persist Id in the json or change the id to something else and see if you get errors
	// NOTE
	// NOTE
	// NOTE
	// NOTE
	// NOTE
	// NOTE
	// NOTE
	// NOTE
	{
		classMap.put(-1, ProcessingHelper.class);
	}
}
