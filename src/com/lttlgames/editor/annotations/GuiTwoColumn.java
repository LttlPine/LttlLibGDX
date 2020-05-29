package com.lttlgames.editor.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Makes this class use two columns to display child fields.<br>
 * Can also be used on fields. If it's on a field, then this field will be drawn as if part of twoColumn
 * 
 * @author Josh
 */
@Target(value =
{ ElementType.FIELD, ElementType.TYPE })
@Retention(value = RetentionPolicy.RUNTIME)
public @interface GuiTwoColumn
{
}
