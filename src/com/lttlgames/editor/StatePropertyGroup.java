package com.lttlgames.editor;

import com.lttlgames.editor.annotations.Persist;

/**
 * Often used for lists of objects with state properties
 * 
 * @author Josh
 */
@Persist(-9081)
public abstract class StatePropertyGroup
{
	/**
	 * optional, to use in a state's generatePropetyMap() to skip
	 */
	@Persist(908100)
	public boolean active = true;
}
