package com.lttlgames.editor.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.lttlgames.editor.LttlComponent;

/**
 * Sets the required components that must be on the transform for this component to be allowed. This is combined with
 * the required components of super classes too. (ie @ComponentRequired(class) or @ComponentRequired({class0,class1}) )
 * 
 * @author Josh
 */
@Target(value = ElementType.TYPE)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface ComponentRequired
{
	public Class<? extends LttlComponent>[] value();
}
