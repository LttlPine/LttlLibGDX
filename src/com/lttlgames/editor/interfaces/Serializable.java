package com.lttlgames.editor.interfaces;

/**
 * Allows any object to have a callback right before it is serialized, allowing it to prepare data.
 */
public interface Serializable
{
	public void beforeSerialized();

	public void afterSerialized();
}
