package com.lttlgames.editor.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Flags a field or class to be shown in the GUI. Adding @GuiShow annotation does not force serialization. The @Persist
 * annotation must still be used. @GuiShow can be used on non persisted fields and class that you want to be allowed to
 * be in GUI.<br>
 * If the variable is static, it can not be persisted and needs to have a @GuiShow on it to work.
 * 
 * @author Josh
 */
@Target(value =
{ ElementType.FIELD, ElementType.TYPE })
@Retention(value = RetentionPolicy.RUNTIME)
public @interface GuiShow
{

}
