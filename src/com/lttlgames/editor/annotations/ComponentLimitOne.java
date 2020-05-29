package com.lttlgames.editor.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sets a Component class to only have a max of one instance per transform. This is overrided by any superclass who has
 * this setting.
 * 
 * @author Josh
 */
@Target(value = ElementType.TYPE)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface ComponentLimitOne
{

}
