package com.lttlgames.editor;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import com.lttlgames.editor.LttlObjectGraphCrawler.FieldsMode;

public class LttlComponentCrawler
{
	/**
	 * These are for storing objects out of scope that you want to use inside ComponentCrawler
	 */
	public Object[] objects;
	private ArrayList<LttlComponent> currentComponents = new ArrayList<LttlComponent>(
			0);

	/**
	 * @param objects
	 *            for storing objects out of scope that you want to use inside ComponentCrawler, use by
	 *            this.objects[0..]
	 */
	public LttlComponentCrawler(Object... objects)
	{
		this.objects = objects;
	}

	public LttlComponentCrawler()
	{
	}

	/**
	 * Crawls the primary components (first), calls them back, and calls back but doesn't crawl secondary components.
	 * 
	 * @param object
	 *            object to crawl
	 * @param fieldsMode
	 * @param componentsDeep
	 *            Only components who are this deep/inside that many other components will be called back<br>
	 *            if 0, will callback the first level of components, if the primary object crawling is a LttlComponent,
	 *            then it will callback nothing.<br>
	 *            if 1, will crawl one level of components, and callback any components in them. The first level of
	 *            components will not be called.
	 */
	public void crawl(Object object, FieldsMode fieldsMode,
			final int componentsDeep)
	{
		currentComponents.clear();

		new LttlObjectGraphCrawler()
		{
			@SuppressWarnings("rawtypes")
			HashMap<Object, HashMap> entriesToDelete = new HashMap<Object, HashMap>();

			@SuppressWarnings(
			{ "unchecked", "rawtypes" })
			public boolean BeforeHashMapEntry(HashMap hashMap, Object key,
					Object value)
			{
				// all map items are expected to be within the callbackLevel because it is checked in BeforeObject

				if (value == null) { return false; }
				if (LttlComponent.class.isAssignableFrom(value.getClass()))
				{
					Object response = setHashMapEntry(key, value);
					if (response.equals("delete"))
					{
						entriesToDelete.put(key, hashMap);
					}
					else if (response != value)
					{
						hashMap.put(key, response);
					}
				}
				return true;
			}

			@SuppressWarnings("unchecked")
			public boolean BeforeArrayItem(Object array, Object o, int index)
			{
				// all array items are expected to be within the callbackLevel because it is checked in BeforeObject

				if (o == null) { return false; }
				if (LttlComponent.class.isAssignableFrom(o.getClass()))
				{
					Object response = set(o);
					if (response != o)
					{
						if (array.getClass() == ArrayList.class)
						{
							((ArrayList<Object>) array).set(index, response);
						}
						else
						// it must be an array
						{
							Array.set(array, index, response);
						}
					}
				}
				return true;
			}

			public boolean BeforeField(Object parentObject,
					ProcessedFieldType pft)
			{
				// all fields are expected to be within the callbackLevel because it is checked in BeforeObject

				Field f = pft.getField();

				if (LttlComponent.class.isAssignableFrom(pft.getCurrentClass()))
				{
					// get the object from field
					Object o = null;
					try
					{
						o = f.get(parentObject);
					}
					catch (IllegalArgumentException | IllegalAccessException e1)
					{
						e1.printStackTrace();
					}

					if (o == null) { return false; }

					// get the response from the Anonymous class, the returned value will be used to set the object for
					// field
					Object response = set(o);

					// only set the response object if it is different from the original
					if (response != o)
					{
						boolean isPrivate = false;
						if (LttlObjectGraphCrawler
								.isPrivateOrProtectedOrDefault(f))
						{
							isPrivate = true;
							f.setAccessible(true);
						}

						try
						{
							f.set(parentObject, response);

							if (isPrivate)
							{
								f.setAccessible(false);
							}
							return false;
						}
						catch (IllegalArgumentException e)
						{
							e.printStackTrace();
						}
						catch (IllegalAccessException e)
						{
							e.printStackTrace();
						}

						if (isPrivate)
						{
							f.setAccessible(false);
						}
					}

					// crawl this component, if already at the callbackLevel it will not crawl, checks below in
					// BeforeObject()
					return true;
				}
				// skip Arrays that have root component types that are primative
				// don't need to be iterating through like 200 float values looking for a LttlComponent
				else if (pft.getCurrentClass().isArray()
						&& LttlObjectGraphCrawler
								.isPrimative(LttlObjectGraphCrawler
										.getComponentTypeRoot(pft
												.getCurrentClass()))) { return false; }

				// crawl it because it's not a LttlComponent or an array
				return true;
			};

			public boolean BeforeObject(Object o, ProcessedFieldType pft)
			{
				if (o == null) return false;

				// do not crawl this object if already on the callback level since any components found deeper
				// will not be called back anyways
				if (currentComponents.size() > componentsDeep) { return false; }

				if (LttlComponent.class.isAssignableFrom(o.getClass()))
				{
					// if going into a LttlComponent add to end
					currentComponents.add((LttlComponent) o);
				}
				return true;
			}

			public void AfterObject(Object o, ProcessedFieldType pft)
			{
				if (LttlComponent.class.isAssignableFrom(o.getClass()))
				{
					// leaving LttlComponent, remove last
					currentComponents.remove(currentComponents.size() - 1);
				}
			}

			// delete any entries from hashmaps, don't delete while iterating through them
			@SuppressWarnings("rawtypes")
			public void onCompletion()
			{
				for (Iterator<Entry<Object, HashMap>> it = entriesToDelete
						.entrySet().iterator(); it.hasNext();)
				{
					Entry<Object, HashMap> pair = it.next();
					pair.getValue().remove(pair.getKey());
				}

				currentComponents.clear();
			}

		}.crawl(object, -1, fieldsMode);
	}

	/**
	 * Gives you a LttlComponent object and what you return affects how it is saved (via fields, lists, arrays, or HMs).
	 * Return the object to preserve it, or return any other object to change it (null, etc). Works for fields, lists,
	 * and arrays.<br>
	 * Automatically skips null objects.
	 * 
	 * @param object
	 * @return
	 */
	public Object set(Object object)
	{
		return object;
	}

	/**
	 * Gives you a LttlComponent object and what you return affects how it is saved (via fields, lists, arrays, or HMs).
	 * Return the value to preserve it, or return any other object to change the object at the key, or return with
	 * 'delete' to remove the entire entry.<br>
	 * Automatically skips null values.<br>
	 * <br>
	 * **Can only set the value object, not the key.
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	public Object setHashMapEntry(Object key, Object value)
	{
		return value;
	}

	/**
	 * Returns the current component it is crawling through
	 * 
	 * @return null if in none
	 */
	final public LttlComponent getCurrentComponent()
	{
		return currentComponents.size() == 0 ? null : currentComponents
				.get(currentComponents.size() - 1);
	}
}
