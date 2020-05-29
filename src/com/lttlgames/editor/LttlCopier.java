package com.lttlgames.editor;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.lttlgames.editor.LttlJsonDeserializer.ComponentRef;
import com.lttlgames.editor.LttlObjectGraphCrawler.FieldsMode;
import com.lttlgames.editor.annotations.DoCopyByReference;

public final class LttlCopier
{
	public static class Helper
	{
		public FieldsMode fieldsModes = null;

		public boolean rootObjectIsComponent = false;
		public Object rootObject = null;

		public Helper(FieldsMode fieldsModes, Object startingObject)
		{
			if (startingObject != null
					&& LttlComponent.class.isAssignableFrom(startingObject
							.getClass()))
			{
				rootObjectIsComponent = true;
			}
			this.rootObject = startingObject;
			this.fieldsModes = fieldsModes;
		}
	}

	private LttlCopier()
	{
		// Exists only to defeat instantiation.
	}

	/**
	 * when true, will not get the id for th root object if its a lttlcomponent, since we don't want to overwrite other
	 * components with it when we import, BUT we still want to get fields that referencelttlcomponents' ids, so can't
	 * block them all together with a DoNotExport annotation
	 */
	private static boolean isExportingFromMenuItem = false;

	/**
	 * In addition to serializing the requested fields, normally it will only serialize one layer deep of LttlComponents
	 * and only save it's id field. However, if serializing inside the {@link LttlSceneCore#componentMap}, then it will
	 * get two LttlComponent layers deep. The first component will have all it's requested fields serialized, the next
	 * layer will only have the id serialized, and any deeper components will be skipped.<br>
	 * If a LttlComponent is the root object, then it will not count itself as the first component layer, thus all it's
	 * requested fields will be serialized.
	 */
	private static final LttlJsonSerializer jsonSerializer = new LttlJsonSerializer()
	{
		private Object primaryComp = null;
		private Object secondaryComp = null;
		/**
		 * if not null, then inside component map so go two deep
		 */
		private Object componentMap = null;

		public boolean CheckListItem(Object item, int listType)
		{
			if (item == null) { return true; }

			if (item instanceof LttlComponent)
			{
				// always go into at least one comp to either get full or get just id
				if (primaryComp == null)
				{
					primaryComp = item;
					return true;
				}
				// sometimes go into a second comp, but only if in component map, will be getting just id
				else if (componentMap != null && secondaryComp == null)
				{
					secondaryComp = item;
					return true;
				}
				else
				{
					// skip it because it is too deep
					return false;
				}
			}
			return true;
		}

		@Override
		public boolean CheckField(Object sourceObject,
				ProcessedFieldType processedFieldType)
				throws IllegalArgumentException, IllegalAccessException
		{
			Class<?> type = processedFieldType.getCurrentClass();
			Field field = processedFieldType.getField();

			if (LttlComponent.class.isAssignableFrom(type))
			{
				if (primaryComp == null)
				{
					// entering a new component
					// always enter if first component, may be getting full component or just id
					primaryComp = field.get(sourceObject);
					return true;
				}
				else
				{
					// only check if in secondary component if in component map, otherwise don't go any deeper
					// check if already in not secondary comp
					if (componentMap != null && secondaryComp == null)
					{
						// entering secondary component
						secondaryComp = field.get(sourceObject);
						return true;
					}
					else
					{
						// trying to enter a third deep component, skip entirely, all we want is the id of the
						// second component
						return false;
					}
				}
			}
			else if (secondaryComp != null
					|| (primaryComp != null && componentMap == null))
			{
				// only save the id field
				if (LttlObjectGraphCrawler.isComponentIdField(field))
				{
					return true;
				}
				else
				{
					return false;
				}
			}

			// if is exporting and the exporting object is a LttlComponent and this is that object then skip the id
			// field. PrimaryComp will not be defined since it is the rootObject not one found in a
			// field. This is necessary since we want component ids to be exportable for referenced Lttlcomponents, but
			// just don't want to export the id of it's the root object since it may overwrite the component when
			// importing
			if (isExportingFromMenuItem
					&& getRootObject() instanceof LttlComponent
					&& sourceObject == getRootObject()
					&& LttlObjectGraphCrawler.isComponentIdField(field)) { return false; }

			// if field is LttlSceneCore's componentMapField the save component map object so it will do two deep of
			// LttlComponents
			if (LttlObjectGraphCrawler.isFieldComponentMap(field))
			{
				componentMap = field.get(sourceObject);
			}

			return true;
		}

		public void CompletedObject(Object object)
		{
			if (object == secondaryComp)
			{
				secondaryComp = null;
			}
			if (object == primaryComp)
			{
				secondaryComp = null;
				primaryComp = null;
			}
			if (object == componentMap)
			{
				componentMap = null;
			}
		}
	};

	/**
	 * Converts all object's persisted fields to json.
	 * 
	 * @see #jsonSerializer
	 * @param object
	 * @return
	 */
	public static String toJson(Object object)
	{
		return jsonSerializer.toJson(object);
	}

	/**
	 * *Will limit any field that extends LttlComponent to only save the ID
	 * 
	 * @param object
	 * @param fieldsMode
	 * @return
	 */
	public static String toJson(Object object, FieldsMode fieldsMode)
	{
		return toJson(object, fieldsMode, false);
	}

	/**
	 * *Will limit any field that extends LttlComponent to only save the ID.<br>
	 * If is exporting and the exporting (root) object is a LttlComponent, will not export it's id, but will export ids
	 * of any components it is referencing.
	 * 
	 * @param object
	 * @param fieldsMode
	 * @param isExportingFromMenuItem
	 * @return
	 */
	public static String toJson(Object object, FieldsMode fieldsMode,
			boolean isExportingFromMenuItem)
	{
		LttlCopier.isExportingFromMenuItem = isExportingFromMenuItem;
		return jsonSerializer.toJson(object, fieldsMode);
	}

	/**
	 * Deseralizes.
	 * 
	 * @param json
	 * @param jsonClass
	 * @param compRefsList
	 *            optional, if not null then will populate with fields and object and the component id that needs to be
	 *            set to that field, all LttlComponent reference fields will be null
	 * 
	 *            <pre>
	 * for (ComponentRef cr : compRefsList)
	 * {
	 * 	cr.set(scene);
	 * }
	 * </pre>
	 * @param container
	 *            optional, can be null
	 * @return
	 */
	public static <T extends Object> T fromJson(String json,
			Class<T> jsonClass, ArrayList<ComponentRef> compRefsList,
			Object container)
	{
		return LttlJsonDeserializer.deserialize(json, jsonClass, compRefsList,
				container);
	}

	/**
	 * Returns a copy of an object (hashmap, array, arraylist), abiding by annotation rules and saving all
	 * LttlComponents (extends too) by reference, any other object will be recreated if it has a value. If the root
	 * object is a LttlComponent then it will get references one layer deep.<br>
	 * <br>
	 * Note: Only the fields that should be copied ( @Persist, and @DoCopy if not persisted, and not @DoNotCopy if
	 * persisted). This will also check for @DoCopyByReference fields.
	 * 
	 * @param object
	 * @return
	 */
	public static <T> T copy(T object)
	{
		return copy(object, null);
	}

	/**
	 * Copies an object into a container object.
	 * 
	 * @param object
	 * @param container
	 * @return
	 */
	public static <T> T copy(T object, T container)
	{
		return copy(object, FieldsMode.Copy, container);
	}

	/**
	 * Returns a copy of an object (hashmap, array, arraylist), LttlComponents will be copied by reference along with
	 * any other @DoCopyByReference fields.
	 * 
	 * @param object
	 *            the object to copy
	 * @param fieldsModes
	 * @param container
	 *            can be null
	 * @return
	 */
	public static <T> T copy(T object, FieldsMode fieldsModes, T container)
	{
		return copy(object, new Helper(fieldsModes, object), null, container);
	}

	private static <T> T copy(T object, Helper h, ProcessedFieldType pft,
			T container)
	{
		if (object == null) return null;

		Class<?> c = object.getClass();

		if (LttlObjectGraphCrawler.isPrimative(c))
		{
			return object;
		}
		else if (c.isArray())
		{
			return copyArray(object, h);
		}
		else if (c == ArrayList.class)
		{
			return copyArrayList(object, h, pft, container);
		}
		else if (c == HashMap.class)
		{
			return copyHashMap(object, h, pft, container);
		}
		else
		{
			return copyObject(object, h, pft, container);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> T copyObject(T object, Helper h, ProcessedFieldType pft,
			Object container)
	{
		if (object == null) return null;

		// only save LttlComponents as references if the original object copying is not a LttlComponent or if it is,
		// this object is not that original object copying
		if (LttlComponent.class.isAssignableFrom(object.getClass()))
		{
			if ((h.rootObjectIsComponent && object != h.rootObject)
					|| !h.rootObjectIsComponent) { return object; }
		}

		Object newObject = null;
		// only create a new object if no container
		if (container == null)
		{
			newObject = LttlObjectGraphCrawler.newInstance(object.getClass());
			Lttl.Throw(newObject);
		}
		else
		{
			if (!object.getClass().isAssignableFrom(container.getClass()))
			{
				Lttl.Throw("The given container object "
						+ container.getClass().getSimpleName()
						+ " does not match the class "
						+ object.getClass().getSimpleName());
			}
			newObject = container;
		}

		// FIELDS (PUBLIC AND PRIVATES)
		for (ProcessedFieldType pftI : LttlObjectGraphCrawler.getAllFields(
				object.getClass(), h.fieldsModes,
				(pft != null) ? pft.getParam() : null))
		{
			Field f = pftI.getField();

			// check if it's private so it can be accessed
			boolean isPrivate = false;
			if (LttlObjectGraphCrawler.isPrivateOrProtectedOrDefault(f))
			{
				isPrivate = true;
				f.setAccessible(true);
			}

			// get the object of the field from the "newObject" or container, this way can utilize the intial objects
			// created, helps garbage collector and final fields
			Object fieldObjectContainer = null;
			try
			{
				fieldObjectContainer = f.get(newObject);
			}
			catch (IllegalArgumentException | IllegalAccessException e1)
			{
				e1.printStackTrace();
			}

			try
			{
				Object fieldObject = null;

				// check if should copy this field's object by reference if it is a LttlComponent or based on
				// annotations
				if (LttlComponent.class.isAssignableFrom(f.getType())
						|| f.isAnnotationPresent(DoCopyByReference.class))
				{
					// copyByReference fields can't be persisted because of the nature of preserving unique data
					fieldObject = f.get(object);
				}

				// no reference was made so create it and use the fieldObejct's container if any
				if (fieldObject == null)
				{
					fieldObject = copy(f.get(object), h, pftI,
							fieldObjectContainer);
				}

				// only set the field if the fieldObject's container object is different than the created object
				// the created object should be the same as the fieldObjectContainer if it was not null and not
				// primative this way finals should work and won't have a huge spike in garbage collecting
				if (fieldObjectContainer != fieldObject)
				{
					f.set(newObject, fieldObject);
				}
			}
			catch (IllegalArgumentException e)
			{
				e.printStackTrace();
			}
			catch (IllegalAccessException e)
			{
				e.printStackTrace();
			}

			// if private return the accessibility to false
			if (isPrivate)
			{
				f.setAccessible(false);
			}

		}

		return (T) newObject;
	}

	@SuppressWarnings("unchecked")
	private static <T> T copyArray(T arrayObject, Helper h)
	{
		if (arrayObject == null) return null;

		Object newArray = Array.newInstance(arrayObject.getClass()
				.getComponentType(), Array.getLength(arrayObject));
		for (int i = 0; i < Array.getLength(arrayObject); i++)
		{
			Array.set(newArray, i,
					copy(Array.get(arrayObject, i), h, null, null));
		}
		return (T) newArray;
	}

	@SuppressWarnings(
	{ "rawtypes", "unchecked" })
	private static <T> T copyArrayList(T listObject, Helper h,
			ProcessedFieldType pft, T container)
	{
		if (listObject == null) return null;

		ArrayList castedList = (ArrayList) listObject;

		if (pft != null && pft.getParamCount() < 1)
		{
			Lttl.Throw("Should be at least one parameter.");
		}

		ArrayList newList = null;
		if (container == null)
		{
			newList = new ArrayList();
		}
		else
		{
			((ArrayList) container).clear();
			((ArrayList) container).ensureCapacity(castedList.size());
			newList = (ArrayList) container;
		}

		for (int i = 0; i < castedList.size(); i++)
		{
			newList.add(copy(castedList.get(i), h,
					(pft != null) ? pft.getParam() : null, null));
		}
		return (T) newList;
	}

	@SuppressWarnings(
	{ "rawtypes", "unchecked" })
	private static <T> T copyHashMap(T hashMapObject, Helper h,
			ProcessedFieldType pft, T container)
	{
		if (hashMapObject == null) return null;

		HashMap castedHashMap = (HashMap) hashMapObject;

		if (pft != null && pft.getParamCount() < 2)
		{
			Lttl.Throw("Should be at least two parameters.");
		}

		HashMap newHM = null;
		if (container == null)
		{
			newHM = new HashMap();
		}
		else
		{
			((HashMap) container).clear();
			newHM = (HashMap) container;
		}
		for (Iterator it = castedHashMap.entrySet().iterator(); it.hasNext();)
		{
			Map.Entry pairs = (Map.Entry) it.next();

			newHM.put(
					copy(pairs.getKey(), h, (pft != null) ? pft.getParam(0)
							: null, null),
					copy(pairs.getValue(), h, (pft != null) ? pft.getParam(1)
							: null, null));
		}
		return (T) newHM;
	}
}
