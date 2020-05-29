package com.lttlgames.editor;

import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.helpers.EaseType;

//02
@Persist(-9070)
public class StatePropertyOptions extends KeyframeOptions
{
	/**
	 * if true, automatically updates when state updates. This is good to disable if you are doing isRelative() or a
	 * repeat yoyo that returns to original value
	 */
	@Persist(907001)
	public boolean autoUpdate = true;
	/**
	 * If true, means the ease type will be used instead of the keyframe's ease type.
	 */
	@Persist(907002)
	public boolean easeTypeOverride = false;
	@Persist(907003)
	public EaseType easeType = EaseType.QuadInOut;
}
