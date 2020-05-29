package com.lttlgames.editor.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds some string data to a field, which can be used in any way needed by the GuiFieldObject.<br>
 * You can optionally specify and id value which will be required for GuiStringDataInherit<br>
 * ie. This sets the name for timeline, or if it's not on a timeline then it will set it for any timeline that has
 * GuiStringDataInherit(id)
 * 
 * @author Josh
 */
@Target(value = ElementType.FIELD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface GuiStringData
{
	public String value();

	public int id() default -1;
}