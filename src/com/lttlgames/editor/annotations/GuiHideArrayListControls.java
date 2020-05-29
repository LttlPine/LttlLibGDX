package com.lttlgames.editor.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Hides some of the ArrayList's controls.<br>
 * Default: everything is enabled.<br>
 * If canDelete is false, then can't clear
 * 
 * @author Josh
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GuiHideArrayListControls
{
	public boolean canAdd() default true;

	public boolean canDelete() default true;

	public boolean canClear() default true;

	public boolean canMove() default true;
}
