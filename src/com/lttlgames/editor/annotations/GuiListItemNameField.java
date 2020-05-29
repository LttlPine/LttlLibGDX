package com.lttlgames.editor.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a field on each item object that will be used to label it instead of the index. Can be a string or enum.<br>
 * This goes on the list field.
 * 
 * @author Josh
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GuiListItemNameField
{
	public String value();
}
