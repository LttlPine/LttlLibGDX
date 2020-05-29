package com.lttlgames.editor.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Flags a method to become a button in GUI. This way the method can be ran via a button in editor. It can be public or
 * private. It can have one parameter, as long as it's a GuiFieldObject.<br>
 * <b>tooltip</b> default:""<br>
 * <b>mode</b> default:0 [0=any,1=not playing,2=play only]
 * 
 * @author Josh
 */
@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface GuiButton
{
	public String tooltip() default "";

	public int order() default Integer.MAX_VALUE;

	/**
	 * 0=any,1=not playing,2=play only
	 * 
	 * @return
	 */
	public int mode() default 0;

	/**
	 * Groups a series of guiButtons together, seperate by commas for nested groups<br>
	 * ie. "Outer,Inner"
	 * 
	 * @return
	 */
	public String group() default "";

	public String name() default "";
}
