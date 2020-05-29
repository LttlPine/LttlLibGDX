package com.lttlgames.tweenengine;

import com.lttlgames.editor.annotations.Persist;

@Persist(-9068)
public enum RepeatTweenType
{
	/**
	 * No looping
	 */
	None, /**
	 * Starts from beginning
	 */
	Rewind, /**
	 * Goes backwards and forwards (each direction is one iteration)
	 */
	YoYo
}
