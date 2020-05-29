package com.lttlgames.editor;

import com.lttlgames.editor.annotations.Persist;

@Persist(-90125)
public class AnimationProperty
{
	/**
	 * Can only be changed in the editor, this way it can be reference when modifying it
	 */
	@Persist(9012500)
	float value;
	@Persist(9012501)
	String name = "";

	/**
	 * this value is actually used in the animation, if it's not {@link Float#POSITIVE_INFINITY}
	 */
	float valueAnim = Float.POSITIVE_INFINITY;

	/**
	 * This is the value that is set in the Animation Editor, then you can {@link AnimationProperty#setValue()} to a
	 * percentage of the original value or a totally different number.
	 * 
	 * @return
	 */
	public float getOriginalValue()
	{
		return value;
	}

	/**
	 * Sets the value that will be used in the next animation creation. Can set to {@link Float#POSITIVE_INFINITY} to
	 * use the original value.
	 * 
	 * @param p_value
	 */
	public void setValueAnim(float p_value)
	{
		valueAnim = p_value;
	}

	/**
	 * Sets the vaule to animate as a multiple of the original value (ie. .2, 1.4, 3)
	 * 
	 * @param multiple
	 */
	public void setMultiple(float multiple)
	{
		valueAnim = value * multiple;
	}

	/**
	 * Gets the value that will be used in the next animation creation
	 * 
	 * @return
	 */
	public float getValueAnim()
	{
		return valueAnim;
	}
}
