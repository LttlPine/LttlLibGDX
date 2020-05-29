package com.lttlgames.editor.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field to be animated, must have a unique id with super and sub classes.<br>
 * If field is an object, it's class must implement LttlAnimated (exceptions: Vector2, Color, IntArray, and FloatArray)
 * 
 * @author Josh
 */
@Target(value = ElementType.FIELD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface AnimateField
{
	public int value();
}
