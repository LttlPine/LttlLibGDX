package com.lttlgames.editor.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <b>DO NOT USE AS OF NOW, not recommended to use as of now since anything that would need to be copied by refernce
 * should just be replaced after the copy, this insures that the user knows what is being copied and what isn't
 * LttlComponents are the only thing that is copied by reference by default</b> When copying any object, all fields with
 * objects will be created as new objects, with the exception of LttlComponents which are always copied by reference no
 * matter what. However, this annotation allows a field to be copied by reference and not create a new
 * object/array/list/map, etc.<br>
 * <b>NOTE: A field using CopyByReference that is persisted will throw an error, so only put it on non persisted fields.
 * This overrides @IgnoreCrawl.</b>
 * 
 * @author Josh
 */
@Target(value = ElementType.FIELD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface DoCopyByReference
{
}
