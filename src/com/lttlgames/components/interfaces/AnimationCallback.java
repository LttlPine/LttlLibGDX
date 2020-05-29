package com.lttlgames.components.interfaces;

import com.lttlgames.editor.AnimatedComponent;
import com.lttlgames.editor.AnimationObject;
import com.lttlgames.editor.StateBase;

public interface AnimationCallback
{
	/**
	 * This is the standard callback used in the Animation Editor. It is called at the time of the callback node.<br>
	 * If a callback is at the edge of a yoyoing sequence, it will be called twice.
	 * 
	 * <pre>
	 * count++;
	 * if (count % 2 == 0)
	 * {
	 * 	t().getParent().tweenPosTo(10, 50, 1).ease(EaseType.Punch).start();
	 * }
	 * </pre>
	 * 
	 * @param animName
	 *            animation name
	 * @param seqName
	 *            sequence name
	 * @param value
	 */
	public void onCallback(String animName, String seqName, String value);

	/**
	 * This is called each time a new value is set for interpolation and {@link StateBase#stepCallback} or
	 * {@link AnimatedComponent#stepCallback} is set.<br>
	 * It will callback once an inteprolation for the entire component or state animation, not individual nodes or
	 * sequences.<br>
	 * If this is not the main component instead it's in {@link AnimationObject#callbackComponents}, then it will be
	 * called for each step for the entire animation, not individual sequences (state or animated object).<br>
	 * <b>This is guaranteed not to be called more than once per frame.</b><br>
	 * This will not fire at all for timer() or mark() <br>
	 * This may call in editor, so can do a check for that if it matters.
	 * 
	 * @param name
	 *            will be the AnimationName animating an object, if animating a state, it will be state name (don't know
	 *            the animation object when tweening a state, I think it's good to keep it seperate, since state's can
	 *            be used indepdently)
	 * @param iterationPosition
	 *            this is the value (0-1) for the current iteration. This is not 100% reliable.
	 */
	public void onStep(String name, float iterationPosition);
}
