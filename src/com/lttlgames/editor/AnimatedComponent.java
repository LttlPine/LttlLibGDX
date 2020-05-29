package com.lttlgames.editor;

import com.lttlgames.editor.annotations.GuiHide;
import com.lttlgames.editor.annotations.GuiToggleOnly;
import com.lttlgames.editor.annotations.Persist;

@Persist(-9089)
@GuiHide
public class AnimatedComponent extends AnimatedObject
{
	@Persist(908900)
	public LttlComponent target;

	/**
	 * If true, will do a step callback on the target component if it implements AnimationCallback.<br>
	 * Setting the stepCallback will be for the entire component's timeline, not the individual fields, so it will
	 * callback for each interpolation, even if some properties start or end at different times.
	 */
	@Persist(908901)
	@GuiToggleOnly
	public boolean stepCallback = false;

	/**
	 * aniamtion editor use only<br>
	 * if not empty, the component's sequence values will be relative to this state.<br>
	 * Will only goTo state when in animation editor when creating the animation. Otherwise it is espected that the
	 * components will be in proper starting values before running animation. <b>This offsets all the sequence keyframe
	 * values to the current values when the tween is made.</b><br>
	 * This is necessary so you don't have to set each node to be relative, and allows an entire sequence/component to
	 * be relative, which is not possible without using a state to force the values to a starting point, otherwise each
	 * aniamtion run would be cumulative
	 */
	@Persist(908902)
	public String relativeState = "";
}
