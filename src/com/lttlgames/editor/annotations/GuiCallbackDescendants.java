package com.lttlgames.editor.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When a field contained inside this object or arraylist, etc (at any level) is modified, it will callback this method
 * or methods (ie {"method1","method2"}) in the order specified on the object containg this field. It will invoke the
 * first method it finds by that name with no params.<br>
 * If this is on an ArrayList field, then it will not callback for arraylist modifications or if the object is nulled
 * out, you need to use GuiCallback for that.
 * 
 * @author Josh
 */
@Target(value = ElementType.FIELD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface GuiCallbackDescendants
{
	public String[] value();
}
