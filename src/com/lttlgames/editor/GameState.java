package com.lttlgames.editor;

import com.lttlgames.editor.annotations.Persist;

@Persist(-904)
public enum GameState
{
	/**
	 * This is the initial load of the game before any UPDATE has ran
	 */
	SETTINGUP, /**
	 * start of each iteration, completed setting up
	 */
	STARTING, /**
	 * This includes all the component callbacks
	 */
	UPDATING, /**
	 * All updates to objects have already been made, now preparing to render
	 */
	STAGING, RENDERING;
}