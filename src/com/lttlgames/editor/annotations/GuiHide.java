package com.lttlgames.editor.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Flags a field or class to not be shown in the GUI. All fields and classes are hidden unless @Persist annotation is
 * present or @GuiHide
 * 
 * @Persist, so this is mainly used for public fields.
 * @author Josh
 */
@Target(value =
{ ElementType.FIELD, ElementType.TYPE })
@Retention(value = RetentionPolicy.RUNTIME)
public @interface GuiHide
{

}
