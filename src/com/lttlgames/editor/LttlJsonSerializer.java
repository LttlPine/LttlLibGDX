package com.lttlgames.editor;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.badlogic.gdx.utils.BooleanArray;
import com.badlogic.gdx.utils.CharArray;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.LongArray;
import com.lttlgames.editor.LttlObjectGraphCrawler.FieldsMode;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.editor.interfaces.Serializable;

//REMEMBER No nested or inner classes allowed to be persisted, will error out catastrophically otherwise.  There is no check in place, and frankly it's too confusing and not worth it.
public class LttlJsonSerializer
{
	private FieldsMode fieldsMode = null;
	private StringBuilder sb = null;

	private Object rootObject;

	/**
	 * Creates a LttlJsonSerializer object;
	 */
	public LttlJsonSerializer()
	{
	}

	/**
	 * Returns JSON and will get all persisted fields
	 * 
	 * @param object
	 * @return
	 */
	public String toJson(Object object)
	{
		return toJson(object, FieldsMode.Persisted);
	}

	/**
	 * Returns JSON of object
	 * 
	 * @param object
	 * @param fieldsMode
	 * @return
	 */
	public String toJson(Object object, FieldsMode fieldsMode)
	{
		this.fieldsMode = fieldsMode;
		sb = new StringBuilder();
		Class<?> type = object.getClass();
		if (type.isArray() || type == ArrayList.class || type == HashMap.class
				|| LttlObjectGraphCrawler.isPrimative(type))
		{
			System.out
					.println("Cannot JSON an Array, ArrayList, HashMap, or primative must be in an Object first!");
			return null;
		}
		rootObject = object;
		CheckInitialObject(object);
		getObjectJson(object, null, null);
		rootObject = null;
		return sb.toString();
	}

	private void getObjectJson(Object o, Class<?> knownClass)
	{
		getObjectJson(o, null, knownClass);
	}

	private void getObjectJson(Object o, ProcessedFieldType pft)
	{
		Class<?> knownClass = (pft != null) ? pft.getCurrentClass() : null;
		getObjectJson(o, pft, knownClass);
	}

	private void getObjectJson(Object o, ProcessedFieldType pft,
			Class<?> knownClass)
	{
		if (o == null)
		{
			sb.append("null");
			return;
		}

		Class<? extends Object> c = o.getClass();

		// check a bunch of stuff
		LttlObjectGraphCrawler.checkPersistedClassValidity(c);

		// before serializing
		if (o instanceof Serializable)
		{
			((Serializable) o).beforeSerialized();
		}

		sb.append("{");

		int outputtedFieldCount = 0;

		if (pft != null)
		{
			knownClass = pft.getCurrentClass();
		}

		// if the knownClass does not exist or if the current object's class does not equal the knownClass (usually the
		// component type of a list, map, or array or the type of a field, then print the class as if it was a field
		if (knownClass == null || c != knownClass)
		{
			sb.append("class:");
			sb.append(getClassJson(c));
			sb.append(",");
			outputtedFieldCount++;
		}

		// FIELDS (PUBLIC AND PRIVATES)
		// if ProcessedFieldType exists and has a parameter, then give that as the param type
		for (ProcessedFieldType pftI : LttlObjectGraphCrawler.getAllFields(c,
				fieldsMode, (pft != null) ? pft.getParam(0) : null))
		{
			Field f = pftI.getField();

			// check if it's private/protected so it can be accessed
			boolean isPrivate = false;
			if (LttlObjectGraphCrawler.isPrivateOrProtectedOrDefault(f))
			{
				isPrivate = true;
				f.setAccessible(true);
			}
			try
			{
				if (getFieldJson(pftI, o))
				{
					outputtedFieldCount++;
				}
			}
			catch (IllegalArgumentException | IllegalAccessException e)
			{
				e.printStackTrace();
			}

			// if private, return the accessibility to false
			if (isPrivate)
			{
				f.setAccessible(false);
			}

		}

		// remove last comma
		if (outputtedFieldCount > 0)
		{
			sb.deleteCharAt(sb.length() - 1);
		}

		sb.append("}");

		// after serializing
		if (o instanceof Serializable)
		{
			((Serializable) o).afterSerialized();
		}

		CompletedObject(o);
	}

	/**
	 * Returns a property formatted primative value
	 * 
	 * @param value
	 * @param valueType
	 * @return
	 */
	private void getPrimativeJson(Object value)
	{
		if (value == null)
		{
			sb.append("null");
			return;
		}

		String valueString = value.toString();
		if (value.getClass() == String.class)
		{
			// value = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
			// + "\"";
			valueString = "\"" + valueString.replace("\"", "\\\"") + "\"";
		}

		sb.append(valueString);
	}

	private void getArrayJson(Object arrayObject, Class<?> arrayClassDefined)
	{
		if (arrayObject == null)
		{
			sb.append("null");
			return;
		}

		sb.append("[");

		int outputtedFieldCount = 0;

		// checks a bunch of stuff, throws
		LttlObjectGraphCrawler.checkPersistedClassValidity(arrayObject
				.getClass());

		if (arrayObject.getClass() != arrayClassDefined)
		{
			// the instance of array component class does not equal the one on the field/pft, so print it out for the
			// deserializer
			sb.append("class:");
			sb.append(getClassJson(arrayObject.getClass()));
			sb.append(",");
			outputtedFieldCount++;
		}
		Class<?> knownComponentType = arrayClassDefined.getComponentType();

		// iterate through array
		for (int i = 0; i < Array.getLength(arrayObject); i++)
		{
			Object item = Array.get(arrayObject, i);

			// check custom skip
			if (!CheckListItem(item, 0)) continue;

			if (item == null)
			{
				sb.append("null");
			}
			else if (LttlObjectGraphCrawler.isPrimative(item.getClass()))
			{
				getPrimativeJson(item);
			}
			else if (item.getClass().isArray())
			{
				getArrayJson(item, knownComponentType);
			}
			else if (item.getClass() == HashMap.class
					|| item.getClass() == ArrayList.class)
			{
				Lttl.Throw("ERROR: Can't have a hashmap or arraylist as the component type of an array.");
			}
			else
			// its an object
			{
				getObjectJson(item, knownComponentType);
			}

			outputtedFieldCount++;
			sb.append(",");
		}

		// remove last comma
		if (outputtedFieldCount > 0)
		{
			removeLastChar();
		}

		sb.append("]");
		CompletedObject(arrayObject);
	}

	private void getArrayListJson(Object arrayListObject, ProcessedFieldType pft)
	{
		// check if the arrayListObject exists and it can become an ArrayList and is not empty
		@SuppressWarnings("unchecked")
		ArrayList<? extends Object> aL = (ArrayList<? extends Object>) arrayListObject;
		if (arrayListObject == null || aL == null)
		{
			sb.append("null");
			return;
		}

		// get the component processedFieldType
		if (pft.getParamCount() == 0)
		{
			// needs to have atleast one parameter
			Lttl.Throw();
		}
		ProcessedFieldType componentPft = pft.getParam(0);
		// component pft needs to exist
		Lttl.Throw(componentPft);

		sb.append("[");

		int outputtedFieldCount = 0;

		// iterate through array
		for (int i = 0; i < aL.size(); i++)
		{
			// check custom skip
			Object item = aL.get(i);
			if (!CheckListItem(item, 1)) continue;

			if (item == null)
			{
				sb.append("null");
			}
			else if (LttlObjectGraphCrawler.isPrimative(item.getClass()))
			{
				getPrimativeJson(item);
			}
			else if (item.getClass().isArray())
			{
				getArrayJson(item, componentPft.getCurrentClass());
			}
			else if (item.getClass() == ArrayList.class)
			{
				getArrayListJson(item, componentPft);
			}
			else if (item.getClass() == HashMap.class)
			{
				getHashMapJson(item, componentPft);
			}
			else
			{
				getObjectJson(item, componentPft);
			}

			outputtedFieldCount++;
			sb.append(",");
		}

		// remove last comma
		if (outputtedFieldCount > 0)
		{
			removeLastChar();
		}

		sb.append("]");
		CompletedObject(arrayListObject);
	}

	@SuppressWarnings("rawtypes")
	private void getHashMapJson(Object hashMapObject, ProcessedFieldType pft)
	{
		@SuppressWarnings("unchecked")
		HashMap<? extends Object, ? extends Object> hm = (HashMap<? extends Object, ? extends Object>) hashMapObject;
		if (hashMapObject == null || hm == null)
		{
			sb.append("null");
			return;
		}

		sb.append("[");
		int outputtedEntryCount = 0;

		// retrieve known classes
		ProcessedFieldType keyPft = pft.getParam(0);
		ProcessedFieldType valuePft = pft.getParam(1);

		// iterate through hashmap
		for (Iterator it = hm.entrySet().iterator(); it.hasNext();)
		{
			Map.Entry pairs = (Map.Entry) it.next();

			// check custom skip
			if (!CheckHashMapItem(pairs.getKey(), pairs.getValue()))
			{
				continue;
			}
			if (!CheckListItem(pairs.getKey(), 2)
					|| !CheckListItem(pairs.getValue(), 2))
			{
				continue;
			}

			sb.append("{");

			// smiple loop to go through key and then value item
			for (int i = 0; i < 2; i++)
			{
				Object item = null;
				ProcessedFieldType currentPft = null;
				if (i == 0)
				{
					// process key
					sb.append("key:");
					item = pairs.getKey();
					currentPft = keyPft;
				}
				else if (i == 1)
				{
					// process value
					sb.append(",value:");
					item = pairs.getValue();
					currentPft = valuePft;
				}

				// generate output for value/key
				if (item == null)
				{
					sb.append("null");
				}
				else if (LttlObjectGraphCrawler.isPrimative(item.getClass()))
				{
					getPrimativeJson(item);
				}
				else if (item.getClass().isArray())
				{
					getArrayJson(item, currentPft.getCurrentClass());
				}
				else if (item.getClass() == ArrayList.class)
				{
					getArrayListJson(item, currentPft);
				}
				else if (item.getClass() == HashMap.class)
				{
					getHashMapJson(item, currentPft);
				}
				else
				{
					getObjectJson(item, currentPft);
				}
			}

			outputtedEntryCount++;
			sb.append("},");
		}

		// remove last comma
		if (outputtedEntryCount > 0)
		{
			removeLastChar();
		}

		sb.append("]");
		CompletedObject(hashMapObject);
	}

	/**
	 * returns true if valid field to save
	 * 
	 * @param pft
	 * @param sourceObject
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	private boolean getFieldJson(ProcessedFieldType pft, Object sourceObject)
			throws IllegalArgumentException, IllegalAccessException
	{
		// ///CHECK IF SHOULD SKIP FIELD OR NOT/////
		// skip static and synthetic fields
		Field f = pft.getField();

		// run custom check
		if (!CheckField(sourceObject, pft)) return false;

		// ///PROCESSING FIELD/////

		// add name of field
		Persist persistAnnotation = LttlObjectGraphCrawler
				.getPersistFieldAnnotation(f);
		if (persistAnnotation != null)
		{
			// if it is a persisted field then use the id of the field
			sb.append(persistAnnotation.value());
		}
		else if (LttlObjectGraphCrawler.isLibraryPersistedClass(sourceObject
				.getClass()))
		{
			// if it is a GDX Array, shrink it's backing array, if not do nothing
			shrinkGdxArray(sourceObject);

			// if the object is a Gdx class that is allowed to be persisted, then use the field name
			sb.append(f.getName());
		}
		else
		{
			Lttl.Throw("Field " + f.getName()
					+ " does not have a Persist annotation and object "
					+ sourceObject.getClass()
					+ " is a gdx class to be persisted.");
		}
		sb.append(":");

		Object fieldObject = null;

		if ((fieldObject = f.get(sourceObject)) == null)
		{
			sb.append("null");
		}

		if (fieldObject != null)
		{
			if (LttlObjectGraphCrawler.isPrimative(fieldObject.getClass()))
			{
				getPrimativeJson(fieldObject);
			}
			else if (fieldObject.getClass().isArray())
			{
				getArrayJson(fieldObject, pft.getCurrentClass());
			}
			else if (fieldObject.getClass() == ArrayList.class)
			{
				getArrayListJson(fieldObject, pft);
			}
			else if (fieldObject.getClass() == HashMap.class)
			{
				getHashMapJson(fieldObject, pft);
			}
			else
			{
				getObjectJson(fieldObject, pft);
			}
		}

		sb.append(",");
		CompletedField(sourceObject, pft);
		return true;
	}

	private void removeLastChar()
	{
		sb.deleteCharAt(sb.length() - 1);
	}

	/**
	 * This is the initial object running in the JSON Serializer
	 * 
	 * @param object
	 * @return nothing
	 */
	public void CheckInitialObject(Object object)
	{
	}

	final public Object getRootObject()
	{
		return rootObject;
	}

	/**
	 * *This is an override method.<br>
	 * A field can be checked and the return boolean decides if it is exported or not
	 * 
	 * @param sourceObject
	 *            the object the field is on
	 * @param processedFieldType
	 * @return false to skip
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	public boolean CheckField(Object sourceObject,
			ProcessedFieldType processedFieldType)
			throws IllegalArgumentException, IllegalAccessException
	{
		return true;
	}

	/**
	 * *This is an override method.<br>
	 * Runs when a field is completed.
	 * 
	 * @param parentObject
	 *            the object the field is on
	 * @param processedFieldType
	 */
	public void CompletedField(Object parentObject,
			ProcessedFieldType processedFieldType)
	{

	}

	/**
	 * *This is an override method.<br>
	 * Runs when an Object (Array, ArrayList, or HashMap) is completed.
	 * 
	 * @param object
	 */
	public void CompletedObject(Object object)
	{

	}

	/**
	 * *This is an override method.<br>
	 * Decide if to skip an item in a list/map.<br>
	 * You need to be check if it is null too.
	 * 
	 * @param item
	 *            The current item in an Array, ArrayList, HashMaps, if key or value is skipped, it does not print the
	 *            entire entry, will not run value if key is denied first.
	 * @param listType
	 *            [0=Array, 1=ArrayList, 2=HashMap]
	 * @return false to skip
	 */
	public boolean CheckListItem(Object item, int listType)
	{
		return true;
	}

	/**
	 * *This is an override method.<br>
	 * Decide to skip an entire hashmap entry before processing each key/value individually.<br>
	 * You need to be check if it is null too.
	 * 
	 * @param key
	 * @param value
	 * @return false to skip
	 */
	public boolean CheckHashMapItem(Object key, Object value)
	{
		return true;
	}

	/**
	 * Returns the class convert to json (persist id or canonical), does not check if can be persisted
	 * (LttlCopier.checkClassToPersist())
	 * 
	 * @param clazz
	 * @return
	 */
	public static String getClassJson(Class<?> clazz)
	{
		Class<?> c = clazz;
		String arrayDimensions = null;
		if (clazz.isArray())
		{
			c = LttlObjectGraphCrawler.getBaseComponentType(clazz);
			arrayDimensions = "";
			for (int i = 0; i < LttlObjectGraphCrawler
					.getArrayDimensions(clazz); i++)
			{
				arrayDimensions += "[]";
			}
		}

		int classId = LttlGameStarter.get().getClassMap()
				.findKey(c, true, Integer.MIN_VALUE);
		if (LttlObjectGraphCrawler.isLibraryPersistedClass(c))
		{
			// it is a GdxClass/library/java class so use full class name
			return c.getCanonicalName()
					+ ((arrayDimensions != null) ? arrayDimensions : "");
		}
		else if (classId != Integer.MIN_VALUE)
		{
			// it is a Lttl class so use mapped class id
			return classId + ((arrayDimensions != null) ? arrayDimensions : "");
		}
		else
		{
			Lttl.Throw("Serializing: class ("
					+ c.getSimpleName()
					+ ") is persisted but not on ClassMap or not added to libraryPersistedClasses.");
			return null;
		}
	}

	private void shrinkGdxArray(Object possibleGdxArray)
	{
		if (possibleGdxArray.getClass() == IntArray.class)
		{
			((IntArray) possibleGdxArray).shrink();
		}
		else if (possibleGdxArray.getClass() == FloatArray.class)
		{
			((FloatArray) possibleGdxArray).shrink();
		}
		else if (possibleGdxArray.getClass() == BooleanArray.class)
		{
			((BooleanArray) possibleGdxArray).shrink();
		}
		else if (possibleGdxArray.getClass() == CharArray.class)
		{
			((CharArray) possibleGdxArray).shrink();
		}
		else if (possibleGdxArray.getClass() == LongArray.class)
		{
			((LongArray) possibleGdxArray).shrink();
		}
	}
}
