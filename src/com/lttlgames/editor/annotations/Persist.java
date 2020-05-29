package com.lttlgames.editor.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//NOTE this will break when there are Persist IDs that exceed 9 digits (ie. 901234099)
// possible solution is convert int to long and use long literal when rarely needed (L)
//NOTE the number of game specific classes is limited to 900
/**
 * Flags a field or class to be persisted. The value is the id for the class or field.<br>
 * If the variable is static, it can not be persisted.<br>
 * Persisted fields will also be copied unless noted with @DoNotCopy
 * 
 * @author Josh
 */
@Target(value =
{ ElementType.FIELD, ElementType.TYPE })
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Persist
{
	public int value();
}
