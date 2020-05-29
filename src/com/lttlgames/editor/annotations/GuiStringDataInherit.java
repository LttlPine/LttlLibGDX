package com.lttlgames.editor.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify the id for a GuiStringData value to inherit from some ancestor
 * 
 * @author Josh
 */
@Target(value = ElementType.FIELD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface GuiStringDataInherit
{
	public int value();
}
