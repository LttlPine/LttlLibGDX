package com.lttlgames.editor;

import com.lttlgames.tweenengine.TweenGetterSetter;

/**
 * <b>This is only necessary for classes that have animated primatives.</b>
 */
public interface LttlAnimated
{
	/**
	 * Returns the TweenGetterSetter for only the int and float animated fields.<br>
	 * <br>
	 * Highly recommended to create an IntMap to cache the tween getterSetters in.</b>
	 * 
	 * <pre>
	 * IntMap&lt;TweenGetterSetter&gt; cachedTweenGetterSetters = new IntMap&lt;TweenGetterSetter&gt;(
	 * 		0);
	 * </pre>
	 * 
	 * The whole point of this is to not have to do reflection for the animation aka every frame.<br>
	 * Note: Be sure to call a super classes method.
	 * 
	 * @param animID
	 * @return
	 */
	TweenGetterSetter getTweenGetterSetter(int animID);
}
