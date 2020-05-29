package com.lttlgames.editor;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.lttlgames.editor.interfaces.Deserializable;
import com.lttlgames.helpers.LttlMutatableBoolean;
import com.lttlgames.helpers.LttlMututatableObject;

public class LttlJsonDeserializer
{
	private LttlJsonDeserializer()
	{
	}

	/**
	 * used for not setting a field because the json does not match the field. Usually happens when changing the field
	 * ids.
	 */
	private static LttlMutatableBoolean typeMismatch = new LttlMutatableBoolean(
			false);

	private static Helper helper;

	/**
	 * Returns object (no need for cast)
	 * 
	 * @param json
	 * @param c
	 * @param compRefsList
	 *            optional, if not null, wll populate with fields and objects and ids which can be used to populate all
	 *            component references once deserialize is done
	 * @param container
	 *            can be null
	 * @return
	 */
	public static <T> T deserialize(String json, Class<T> c,
			ArrayList<ComponentRef> compRefsList, Object container)
	{
		helper = new Helper(compRefsList);
		T result = (T) preProcess(json, c, null, container);
		helper = null;
		return result;
	}

	private static Object preProcess(String json, Class<?> c,
			ProcessedFieldType pft, Object container)
	{
		if ((c == null && pft == null) || json == null || json.isEmpty())
		{
			Lttl.logNote("JSON String or class was null or empty when running preProcess. Returning null");
			return null;
		}

		if (pft != null)
		{
			c = pft.getCurrentClass();
		}

		// if value is null then return null
		if (json.equals("null")) { return null; }

		// check which parser to put it through
		if (LttlObjectGraphCrawler.isPrimative(c))
		{
			return deserializePrimative(json, c);
		}
		else if (c == HashMap.class)
		{
			return deserializeHashMap(parseHashMap(json), pft,
					(HashMap<Object, Object>) container);
		}
		else if (c == ArrayList.class)
		{
			return deserializeArrayList(parseList(json), pft,
					(ArrayList<Object>) container);
		}
		else if (c.isArray())
		{
			return deserializeArray(parseList(json), c);
		}
		else
		{
			// it must be an object
			// if no bracket, then actually assume it is a primitive on an Object field.
			if (json.indexOf("{") == -1)
			{
				// return deserializePrimative(json, null);
				typeMismatch.value = true;
				Lttl.logNote("Deserializing: Unexpected primative.");
				return 0;
			}
			return deserializeObject(parseObject(json), c, pft, container);
		}
	}

	private static Object deserializeArray(ArrayList<String> parsedList,
			Class<?> c)
	{
		if (parsedList == null) return null;

		if (parsedList.size() > 0)
		{
			// deserialize the class from json
			if (parsedList.get(0).indexOf("class:") == 0)
			{
				// calculate number of dimensions
				int bracketIndex = parsedList.get(0).indexOf("[]");
				int dimensions = parsedList.get(0).length() - bracketIndex / 2;

				// get the base class
				Class<?> baseClass = c = LttlObjectGraphCrawler
						.getClassByNameOrId(parsedList.get(0).substring(0,
								bracketIndex));

				// generate the array class with dimensions
				c = LttlObjectGraphCrawler.getArrayClass(baseClass, dimensions);
				parsedList.remove(0);
			}
		}

		Class<?> compType = c.getComponentType();
		final Object newArray = Array.newInstance(compType, parsedList.size());

		// iterate through the ArrayList of JSON and add the items to the created Array
		for (int i = 0; i < parsedList.size(); i++)
		{
			if (shouldDoComponentRef(compType))
			{
				// set as default null
				Array.set(newArray, i, null);

				// then later one it will be replaced with component refs
				final int finalIndex = i;
				helper.compRefsList.add(new ComponentRef(parsedList.get(i))
				{
					@Override
					void set(LttlComponent component)
					{
						Array.set(newArray, finalIndex, component);
					}
				});
			}
			else
			{
				Array.set(newArray, i,
						preProcess(parsedList.get(i), compType, null, null));
			}
		}

		return newArray;
	}

	private static ArrayList<Object> deserializeArrayList(
			ArrayList<String> parsedList, ProcessedFieldType pft,
			ArrayList<Object> container)
	{
		if (parsedList == null) return null;

		final ArrayList<Object> newList;
		if (container == null)
		{
			newList = new ArrayList<Object>(parsedList.size());
		}
		else
		{
			container.clear();
			container.ensureCapacity(parsedList.size());
			newList = container;
		}

		// iterate through the parsed list and populale the new list
		int index = -1;
		for (String json : parsedList)
		{
			index++;
			if (shouldDoComponentRef(pft.getParam(0).getCurrentClass()))
			{
				// add default value as null
				newList.add(null);

				// then later one it will be replaced with component refs
				final int finalIndex = index;
				helper.compRefsList.add(new ComponentRef(json)
				{
					@Override
					void set(LttlComponent component)
					{
						newList.set(finalIndex, component);
					}
				});
			}
			else
			{
				// add normal object
				newList.add(preProcess(json, null, pft.getParam(0), null));
			}
		}

		return newList;
	}

	private static HashMap<Object, Object> deserializeHashMap(
			HashMap<String, String> hm, ProcessedFieldType pft,
			HashMap<Object, Object> container)
	{
		if (hm == null) return null;

		HashMap<Object, Object> newHM = null;
		if (container == null)
		{
			new HashMap<Object, Object>(hm.size());
		}
		else
		{
			container.clear();
			newHM = container;
		}
		final HashMap<Object, Object> finalNewHM = newHM;

		// iterate through the hashmap and create the key and value object and add them to the new hashmap
		for (Iterator<Entry<String, String>> it = hm.entrySet().iterator(); it
				.hasNext();)
		{
			Map.Entry<String, String> pairs = (Map.Entry<String, String>) it
					.next();

			// skip class field if any, shouldnt be
			if (pairs.getKey().equals("class"))
			{
				continue;
			}

			// get key object
			if (shouldDoComponentRef(pft.getParam(0).getCurrentClass()))
			{
				// if the key is a LttlComponent, need to not "put" anything in map til after all desializing is done
				// since we want no null values as keys

				// need to check if value is also a LttlComponent, if it is, it's callback needs to be added before the
				// key callback so when the key callback runs it has the value already
				final LttlMututatableObject valueObject = new LttlMututatableObject();
				if (shouldDoComponentRef(pft.getParam(1).getCurrentClass()))
				{
					helper.compRefsList.add(new ComponentRef(pairs.getKey())
					{
						@Override
						void set(LttlComponent component)
						{
							valueObject.value = component;
						}
					});
				}
				else
				{
					// normal way
					valueObject.value = preProcess(pairs.getValue(), null,
							pft.getParam(1), null);
				}

				helper.compRefsList.add(new ComponentRef(pairs.getKey())
				{
					@Override
					void set(LttlComponent component)
					{
						if (component == null)
						{
							Lttl.logNote("WARNING: a hashmap had a null key, which is probably unintended.");
						}
						finalNewHM.put(component, valueObject.value);
					}
				});
			}
			else
			{
				// key is normal object
				final Object keyObject = preProcess(pairs.getKey(), null,
						pft.getParam(0), null);
				if (keyObject == null)
				{
					Lttl.logNote("WARNING: a hashmap had a null key, which is probably unintended.");
				}

				// get value object
				if (shouldDoComponentRef(pft.getParam(1).getCurrentClass()))
				{
					helper.compRefsList.add(new ComponentRef(pairs.getKey())
					{
						@Override
						void set(LttlComponent component)
						{
							// add entry to hashmap
							finalNewHM.put(keyObject, component);
						}
					});
				}
				else
				{
					// normal way
					Object valueObject = preProcess(pairs.getValue(), null,
							pft.getParam(1), null);
					newHM.put(keyObject, valueObject);
				}
			}
		}

		return newHM;
	}

	@SuppressWarnings(
	{ "unchecked", "rawtypes" })
	private static Object deserializePrimative(String json, Class<?> c)
	{
		if (c == Boolean.class || c.getName().equals("boolean"))
		{
			return Boolean.parseBoolean(json);
		}
		else if (c == Integer.class || c.getName().equals("int"))
		{
			try
			{
				return Integer.parseInt(json);

			}
			catch (NumberFormatException e)
			{
				Lttl.logNote("Deserializing: Can't parse to int: '" + json
						+ "'");
				typeMismatch.value = true;
				return 0;
			}
		}
		else if (c == Float.class || c.getName().equals("float"))
		{
			try
			{
				return Float.parseFloat(json);
			}
			catch (NumberFormatException e)
			{
				Lttl.logNote("Deserializing: Can't parse to float: '" + json
						+ "'");
				typeMismatch.value = true;
				return 0;
			}
		}
		else if (c == String.class || c.getName().equals("String"))
		{
			if (json.equals("null"))
			{
				return null;
			}
			else
			{
				// return the string without the begininng and ending quotes and replace all escaped double quotes with
				// just double quotes
				return json.substring(1, json.length() - 1).replace("\\\"",
						"\"");
			}

		}
		else if (c.isEnum())
		{
			if (json.equals("null")) { return null; }

			try
			{
				return Enum.valueOf((Class<Enum>) c, json);
			}
			catch (IllegalArgumentException e)
			{
				Lttl.logNote("Deserializing: No Enum with value of '" + json
						+ "'" + " in class: " + c.getCanonicalName());
				typeMismatch.value = true;
				return null;
			}
		}
		else if (c == Double.class || c.getName().equals("double"))
		{
			try
			{
				return Double.parseDouble(json);
			}
			catch (NumberFormatException e)
			{
				Lttl.logNote("Deserializing: Can't parse to double: '" + json
						+ "'");
				typeMismatch.value = true;
				return 0;
			}
		}
		else if (c == Long.class || c.getName().equals("long"))
		{
			try
			{
				return Long.parseLong(json);
			}
			catch (NumberFormatException e)
			{
				Lttl.logNote("Deserializing: Can't parse to long: '" + json
						+ "'");
				typeMismatch.value = true;
				return 0;
			}
		}
		else if (c == Short.class || c.getName().equals("short"))
		{
			try
			{
				return Short.parseShort(json);
			}
			catch (NumberFormatException e)
			{
				Lttl.logNote("Deserializing: Can't parse to short: '" + json
						+ "'");
				typeMismatch.value = true;
				return 0;
			}
		}
		else if (c == Byte.class || c.getName().equals("byte"))
		{
			try
			{
				return Byte.parseByte(json);
			}
			catch (NumberFormatException e)
			{
				Lttl.logNote("Deserializing: Can't parse to byte: '" + json
						+ "'");
				typeMismatch.value = true;
				return 0;
			}
		}
		return null;
	}

	/**
	 * @param hm
	 * @param c
	 * @param pft
	 * @param container
	 *            can be null, will create new instance
	 * @return
	 */
	private static Object deserializeObject(HashMap<String, String> hm,
			Class<?> c, ProcessedFieldType pft, Object container)
	{
		if (hm == null) return null;

		if (pft != null)
		{
			c = pft.getCurrentClass();
		}

		// check if class field exists and try and get that class whether it is a name or id
		if (hm.containsKey("class"))
		{
			c = LttlObjectGraphCrawler.getClassByNameOrId(hm.get("class"));
			hm.remove("class");
		}

		Object object = null;
		if (container == null)
		{
			// obtain a new instance of the class
			object = LttlObjectGraphCrawler.newInstance(c);
		}
		else
		{
			if (!c.isAssignableFrom(container.getClass()))
			{
				Lttl.Throw("The given container object "
						+ container.getClass().getSimpleName()
						+ " does not match the class " + c.getSimpleName());
			}
			object = container;
		}
		final Object finalObject = object;

		// entering first LttlComponent
		if (LttlComponent.class.isAssignableFrom(c)
				&& helper.primaryComp == null)
		{
			helper.primaryComp = (LttlComponent) object;
		}

		// iterate through rest of hash map fields
		for (Iterator<Entry<String, String>> it = hm.entrySet().iterator(); it
				.hasNext();)
		{
			Map.Entry<String, String> pairs = (Map.Entry<String, String>) it
					.next();

			// get field
			final ProcessedFieldType pftI = LttlObjectGraphCrawler
					.getFieldByNameOrId(pairs.getKey(), c,
							(pft != null) ? pft.getParam(0) : null);

			boolean isSceneCompMapField = false;
			if (LttlSceneCore.class.isAssignableFrom(c)
					&& pftI.getField() != null
					&& LttlObjectGraphCrawler.isFieldComponentMap(pftI
							.getField()))
			{
				isSceneCompMapField = true;
				helper.inSceneCompMap = true;
			}

			// did we get the field?
			if (pftI == null || pftI.getField() == null)
			{
				Lttl.logNote("Deserializing: Skipping field " + pairs.getKey()
						+ " on class " + c.getName() + "... losing data.");
				continue;
			}

			// special case for if it is a LttlComponent that should be referenced
			if (shouldDoComponentRef(pftI.getCurrentClass()))
			{
				// don't set anything from default
				// then later on it will be replaced with component refs
				helper.compRefsList.add(new ComponentRef(pairs.getValue())
				{
					@Override
					void set(LttlComponent component)
					{
						// check if field is private/protected, if it is, make it accessible
						final boolean isPrivate;
						Field f = pftI.getField();
						if (LttlObjectGraphCrawler
								.isPrivateOrProtectedOrDefault(f))
						{
							isPrivate = true;
							f.setAccessible(true);
						}
						else
						{
							isPrivate = false;
						}

						try
						{
							f.set(finalObject, component);
						}
						catch (IllegalArgumentException
								| IllegalAccessException e)
						{
							e.printStackTrace();
						}

						// set private/protected field's access back to what it was
						if (isPrivate)
						{
							f.setAccessible(false);
						}
					}
				});

				// go straight to next field
				continue;
			}

			/* NORMAL DESERIALIZE */

			// check if field is private/protected, if it is, make it accessible
			final boolean isPrivate;
			final Field f = pftI.getField();
			if (LttlObjectGraphCrawler.isPrivateOrProtectedOrDefault(f))
			{
				isPrivate = true;
				f.setAccessible(true);
			}
			else
			{
				isPrivate = false;
			}

			// get the object of the field, this way can utilize the intial objects created, helps garbage collector and
			// final fields
			Object fieldObjectContainer = null;
			try
			{
				fieldObjectContainer = f.get(object);
			}
			catch (IllegalArgumentException | IllegalAccessException e1)
			{
				e1.printStackTrace();
			}

			// set the object to the field
			try
			{
				Object fieldObject = null;

				typeMismatch.value = false;
				fieldObject = preProcess(pairs.getValue(),
						pftI.getCurrentClass(), pftI, fieldObjectContainer);

				if (!typeMismatch.value)
				{
					// only set the field if the fieldObject's container object is different than the created object
					// the created object should be the same as the fieldObjectContainer if it was not null and not
					// primative this way finals should work and won't have a huge spike in garbage collecting
					if (fieldObjectContainer != fieldObject)
					{
						try
						{
							f.set(object, fieldObject);
						}
						catch (IllegalArgumentException e)
						{
							Lttl.logNote("Deserializing: Skipping field "
									+ pairs.getKey()
									+ " on class "
									+ c.getName()
									+ ".  Probably because a field type was changed.");
						}
					}
				}
				else
				{
					Lttl.logNote("Deserializing: Skipping field "
							+ pairs.getKey() + " on class " + c.getName());
				}
			}
			catch (IllegalArgumentException | IllegalAccessException e)
			{
				e.printStackTrace();
			}

			// set private/protected field's access back to what it was
			if (isPrivate)
			{
				f.setAccessible(false);
			}

			// if was in scene component map field, and now leaving, then set to false
			if (isSceneCompMapField)
			{
				helper.inSceneCompMap = false;
			}
		}

		// finished deserializing object
		if (object instanceof Deserializable)
		{
			((Deserializable) object).afterDeserialized();
		}

		// reset primaryComp since leaving
		if (helper.primaryComp != null && helper.primaryComp == object)
		{
			helper.primaryComp = null;
		}

		return object;
	}

	private static HashMap<String, String> parseObject(String json)
	{
		HashMap<String, String> hm = new HashMap<String, String>();

		// iterate through the whole string, skipping first and last chars
		StringBuilder fieldName = new StringBuilder();
		StringBuilder fieldValue = new StringBuilder();
		boolean searchingFieldName = true;
		boolean inString = false;
		int bracketLevel = 0;
		for (int i = 1; i < json.length() - 1; i++)
		{
			if (searchingFieldName)
			{
				if (json.charAt(i) == ':')
				{
					searchingFieldName = false;
					continue;
				}
				else
				{
					fieldName.append(json.charAt(i));
					continue;
				}
			}
			else
			{
				if (json.charAt(i) == '"' && json.charAt(i - 1) != '\\')
				{
					inString = !inString;
				}
				if (!inString)
				{
					// check if bracket level changed
					if ((json.charAt(i) == '{' || json.charAt(i) == '['))
					{
						bracketLevel++;
					}
					else if ((json.charAt(i) == '}' || json.charAt(i) == ']'))
					{
						bracketLevel--;
					}

					if (bracketLevel == 0
							&& (json.charAt(i) == ',' || i == json.length() - 2))
					{
						// if this is the last char then be sure to add it
						if (i == json.length() - 2)
						{
							fieldValue.append(json.charAt(i));
						}

						searchingFieldName = true;
						hm.put(fieldName.toString(), fieldValue.toString());

						// reset stringbuilders
						fieldName.delete(0, fieldName.length());
						fieldValue.delete(0, fieldValue.length());
						continue;
					}
				}
				fieldValue.append(json.charAt(i));
			}
		}
		return hm;
	}

	private static ArrayList<String> parseList(String json)
	{
		ArrayList<String> list = new ArrayList<String>();

		// iterate through the whole string, skipping first and last chars
		StringBuilder itemValue = new StringBuilder();
		boolean inString = false;
		int bracketLevel = 0;
		for (int i = 1; i < json.length() - 1; i++)
		{
			if (json.charAt(i) == '"' && json.charAt(i - 1) != '\\')
			{
				inString = !inString;
			}
			if (!inString)
			{
				if ((json.charAt(i) == '{' || json.charAt(i) == '['))
				{
					bracketLevel++;
				}
				else if ((json.charAt(i) == '}' || json.charAt(i) == ']'))
				{
					bracketLevel--;
				}

				if (bracketLevel == 0
						&& (json.charAt(i) == ',' || i == json.length() - 2))
				{
					// if this is the last char then be sure to add it
					if (i == json.length() - 2)
					{
						itemValue.append(json.charAt(i));
					}

					list.add(itemValue.toString());

					// reset stringbuilders
					itemValue.delete(0, itemValue.length());
					continue;
				}
			}
			itemValue.append(json.charAt(i));
		}
		return list;
	}

	private static HashMap<String, String> parseHashMap(String json)
	{
		HashMap<String, String> hm = new HashMap<String, String>();

		// iterate through the whole string, skipping first and last chars
		StringBuilder keyValue = new StringBuilder();
		StringBuilder valueValue = new StringBuilder();
		boolean inString = false;
		int retrievingLevel = -2; // are we getting nothing the key or the value
		int bracketLevel = 0;
		for (int i = 1; i < json.length() - 1; i++)
		{
			if (json.charAt(i) == '"' && json.charAt(i - 1) != '\\')
			{
				inString = !inString;
			}
			if (!inString)
			{
				if ((json.charAt(i) == '{' || json.charAt(i) == '['))
				{
					bracketLevel++;
				}
				else if ((json.charAt(i) == '}' || json.charAt(i) == ']'))
				{
					bracketLevel--;
					if (bracketLevel == 0) // finished an entry, so add it to map
					{
						hm.put(keyValue.toString(), valueValue.toString());
						retrievingLevel = -2; // reset retrieving level

						// reset stringbuilders
						keyValue.delete(0, keyValue.length());
						valueValue.delete(0, valueValue.length());
						continue;
					}
				}

				if (bracketLevel == 1)
				{
					if (json.charAt(i) == ':')
					{
						if (retrievingLevel == -2)
						{
							retrievingLevel = 0;
							continue;
						}
						else if (retrievingLevel == -1)
						{
							retrievingLevel = 1;
							continue;
						}
					}
					else if (json.charAt(i) == ',')
					{
						retrievingLevel = -1;
						continue;
					}
				}
			}
			if (bracketLevel > 0)
			{
				if (retrievingLevel == 0)
				{
					keyValue.append(json.charAt(i));
				}
				else if (retrievingLevel == 1)
				{
					valueValue.append(json.charAt(i));
				}
			}
		}
		return hm;
	}

	/**
	 * Checks if should save this object as a component reference which will be set later, and for now be set as null or
	 * entirely skipped in the case of a hashmap. Expects all LttlComponents inside another LttlComponent should only
	 * have an id.
	 */
	private static boolean shouldDoComponentRef(Class<?> c)
	{
		return LttlComponent.class.isAssignableFrom(c)
				&& helper.compRefsList != null
				// if outside scene component map, then always try and make references, if inside the scene component
				// map, then the first component will have a new instance created, the deeper ones will be by reference
				&& ((helper.inSceneCompMap && helper.primaryComp != null) || !helper.inSceneCompMap);
	}

	static class Helper
	{
		/**
		 * Can't be unordered array because things like hashmaps require order
		 */
		public ArrayList<ComponentRef> compRefsList;
		/**
		 * Keeps track if inside a LttlComponent already, if not, then deserializes the LttlComponent by creating a new
		 * instance
		 */
		public LttlComponent primaryComp;
		public boolean inSceneCompMap = false;

		public Helper(ArrayList<ComponentRef> compRefsList)
		{
			this.compRefsList = compRefsList;
		}
	}

	static abstract class ComponentRef
	{
		public int id;
		boolean isNull;

		public ComponentRef(String json)
		{
			// if null do nothing
			if (json.equals("null"))
			{
				isNull = true;
				return;
			}

			// get id
			HashMap<String, String> hm = parseObject(json);
			String stringId = hm.get(90701 + "");
			if (stringId == null)
			{
				// must have changed the persist id from a primative to a component ref
				// just set to null
				isNull = true;
				return;
			}
			id = Integer.parseInt(stringId);
		}

		abstract void set(LttlComponent component);

		public void set(LttlSceneCore scene)
		{
			if (isNull)
			{
				set((LttlComponent) null);
				return;
			}

			if (id > Lttl.scenes.getWorldCore().getLastComponentId())
			{
				Lttl.Throw("Inconsistency with component ids.  Loading component id "
						+ id + " which is out of range for component ids.");
			}

			// if null with post a silent error
			LttlComponent component = ComponentHelper.getComponentReference(id,
					scene);
			if (component == null) { return; }

			set(component);
		}
	}
}
