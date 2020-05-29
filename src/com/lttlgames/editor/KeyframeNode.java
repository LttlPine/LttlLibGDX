package com.lttlgames.editor;

import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.helpers.EaseType;

@Persist(-9091)
abstract class KeyframeNode extends TimelineNode
{
	/**
	 * No interpolation, immediatly goes to value after previous node
	 */
	@Persist(909100)
	boolean set = false;

	/**
	 * State: This is the default ease type applied to all state properties, unless they override it.<br>
	 * Field: This is the ease type used for this keyframe.
	 */
	@Persist(909101)
	EaseType easeType = EaseType.QuadInOut;

	/**
	 * this callback when this keyframe starts it's tween
	 */
	@Persist(909102)
	boolean callback = false;

	@Persist(909103)
	String callbackValue = "";
}
