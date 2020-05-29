package com.lttlgames.editor.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.lttlgames.editor.LttlObjectGraphCrawler.FieldsMode;
import com.lttlgames.helpers.LttlHelper;

/**
 * Ignores this field or class (including subclasses) when doing {@link LttlHelper#dump()} skips when getting fields for
 * {@link FieldsMode#AllButIgnore}, and for all other {@link FieldsMode} types it checks to see if the field type does
 * not have IgnoreCrawl, does not check field for annotation
 * 
 * @author Josh
 */
@Target(value =
{ ElementType.FIELD, ElementType.TYPE })
@Retention(value = RetentionPolicy.RUNTIME)
public @interface IgnoreCrawl
{

}
