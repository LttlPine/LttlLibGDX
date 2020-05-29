package com.lttlgames.editor.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When the editor GUI is modified it will callback this method or methods (ie {"method1","method2"}) in the order
 * specified on the object with the field. Be sure a method exists without parameters. It will invoke the first method
 * it finds by that name with no params. It will search itself and any super classes.<br>
 * This includes modifications (order, add, delete) to ArrayLists.
 * 
 * @author Josh
 */
@Target(value = ElementType.FIELD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface GuiCallback
{
	public String[] value();
}
