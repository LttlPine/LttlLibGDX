package com.lttlgames.editor;

import com.lttlgames.editor.annotations.Persist;

@Persist(-902)
public enum AnimationType
{
	/**
	 * Plays forward and stops at end.
	 */
	NORMAL(0), REVERSED(1), LOOP(2), LOOP_REVERSED(3), LOOP_PINGPONG(4), LOOP_RANDOM(
			5);

	int value = 0;

	AnimationType(int value)
	{
		this.value = value;
	}
}
