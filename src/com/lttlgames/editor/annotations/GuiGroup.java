package com.lttlgames.editor.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Organizes fields into a group.<br>
 * Nested groups can occur if use {"Outter Group","Inner Group"}
 * 
 * @author Josh
 */
@Target(value = ElementType.FIELD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface GuiGroup
{
	public String[] value();

	public int order() default -1;
}
