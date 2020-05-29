package com.lttlgames.editor.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Can null this field or class out via right click menu.<br>
 * You can provide a callback that will run and if it returns true, it will procede with setting it to null. This is
 * helpful for preparing your program to handle the null value and checking to see if it can be nulled now. The method
 * should be on the class that has the field if the annotation is on the field, or the callback should be on the class
 * of the object the field represents.
 * 
 * @author Josh
 */
@Target(value =
{ ElementType.FIELD, ElementType.TYPE })
@Retention(value = RetentionPolicy.RUNTIME)
public @interface GuiCanNull
{
	String value() default "";
}
