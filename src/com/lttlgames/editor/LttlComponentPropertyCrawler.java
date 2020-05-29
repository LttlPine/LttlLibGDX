package com.lttlgames.editor;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import com.lttlgames.editor.LttlObjectGraphCrawler.FieldsMode;

/**
 * This crawls through a single component efficiently, does not crawl into any deeper component references, does return
 * them though as properties.
 * 
 * @author Josh
 */
public class LttlComponentPropertyCrawler
{
	/**
	 * These are for storing objects out of scope that you want to use inside ComponentCrawler
	 */
	public Object[] objects;

	/**
	 * This is the main component that the crawler is crawling
	 */
	private LttlComponent mainComponent = null;

	/**
	 * @param objects
	 *            for storing objects out of scope that you want to use inside ComponentCrawler, use by
	 *            this.objects[0..]
	 */
	public LttlComponentPropertyCrawler(Object... objects)
	{
		this.objects = objects;
	}

	public LttlComponentPropertyCrawler()
	{
	}

	/**
	 * Crawls the specified LttlComponent object's properties (inside other objects, hashmaps, lists, and arrays). It
	 * will never crawl into any LttlComponent references, but will call them back to be modified. You can modify,
	 * delete, or just obtain the properties.
	 * 
	 * @param object
	 *            object to crawl
	 * @param fieldsMode
	 * @param deep
	 *            if false, will only search the properties directly on the component. searches in the properties inside
	 *            other prop objects (will look in an array or list or hashmap, but not search the fields of any more
	 *            objects than the first layer) Lot faster and less likely to get caught in recursive loop.
	 */
	public void crawl(LttlComponent object, FieldsMode fieldsMode,
			final boolean deep)
	{
		new LttlObjectGraphCrawler()
		{
			@SuppressWarnings("rawtypes")
			HashMap<Object, HashMap> entriesToDelete = new HashMap<Object, HashMap>();

			@Override
			public boolean onStart(Object root)
			{
				mainComponent = (LttlComponent) root;
				return true;
			}

			@Override
			@SuppressWarnings(
			{ "unchecked", "rawtypes" })
			public boolean BeforeHashMapEntry(HashMap hashMap, Object key,
					Object value)
			{
				if (value == null) return false;

				Object response = setHashMapEntry(key, value);
				if (response.equals("delete"))
				{
					entriesToDelete.put(key, hashMap);
				}
				else if (response != value)
				{
					hashMap.put(key, response);
				}

				// don't crawl LttlComponent references, just callback the value and key above
				if (LttlComponent.class.isAssignableFrom(value.getClass())
						|| LttlComponent.class.isAssignableFrom(key.getClass())) { return false; }

				return true;
			}

			@Override
			@SuppressWarnings("unchecked")
			public boolean BeforeArrayItem(Object array, Object o, int index)
			{
				if (o == null) return false;

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

				// don't crawl LttlComponent references, just callback the object above
				if (LttlComponent.class.isAssignableFrom(o.getClass())) { return false; }

				return true;
			}

			public boolean BeforeField(Object parentObject,
					ProcessedFieldType pft)
			{
				Field f = pft.getField();

				Object o = null;
				try
				{
					o = f.get(parentObject);
				}
				catch (IllegalArgumentException e1)
				{
					e1.printStackTrace();
				}
				catch (IllegalAccessException e1)
				{
					e1.printStackTrace();
				}

				if (o == null) { return false; }

				Object response = set(o);
				if (response != o)
				{
					boolean isPrivate = false;
					if (LttlObjectGraphCrawler.isPrivateOrProtectedOrDefault(f))
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

				// don't crawl LttlComponent references, just callback them as did above
				if (LttlComponent.class.isAssignableFrom(f.getType())) { return false; }

				return true;
			};

			private boolean initial = false;

			@Override
			public boolean BeforeObject(Object o, ProcessedFieldType pft)
			{
				// make sure we at least crawl the first object since it has to be a lttlcomponent
				if (!initial)
				{
					initial = true;
					return true;
				}

				// if no deep skip this object
				if (!deep)
				{
					return false;
				}
				else
				{
					return true;
				}
			}

			// delete any entries from hashmaps, don't delete while iterating through them
			@Override
			@SuppressWarnings("rawtypes")
			public void onCompletion()
			{
				for (Iterator<Entry<Object, HashMap>> it = entriesToDelete
						.entrySet().iterator(); it.hasNext();)
				{
					Entry<Object, HashMap> pair = it.next();
					pair.getValue().remove(pair.getKey());
				}

				mainComponent = null;
			}

		}.crawl(object, -1, fieldsMode);
	}

	/**
	 * Gives you an object and what you return affects how it is saved (via fields, lists, arrays, or HMs). Return the
	 * object (with altering it) to preserve it, or return any other object to change it (null, etc). Works for fields,
	 * lists, and arrays.<br>
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
	 * Gives you a object and what you return affects how it is saved (via fields, lists, arrays, or HMs). Return the
	 * value (with altering it) to preserve it, or return any other object to change the object at the key, or return
	 * with 'delete' to remove the entire entry.<br>
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
	 * Returns the main component crawling
	 * 
	 * @return
	 */
	public LttlComponent getMainComponent()
	{
		return mainComponent;
	}
}
