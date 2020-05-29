package com.lttlgames.editor.interfaces;

/**
 * Allows any object to have a callback as soon as it is deserialized.
 */
public interface Deserializable
{
	/**
	 * Called after this object was finished being deserialized, guaranteeing that all fields have been properly set.
	 * With the exception of any LttlComponent reference fields, they will be null.
	 */
	public void afterDeserialized();
}
