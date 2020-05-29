package com.lttlgames.helpers;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.NumberUtils;
import com.lttlgames.editor.Lttl;
import com.lttlgames.editor.LttlObjectGraphCrawler;
import com.lttlgames.editor.LttlObjectGraphCrawler.FieldsMode;
import com.lttlgames.editor.ProcessedFieldType;

/**
 * Helper functions.
 */
public final class LttlHelper
{
	final static public Vector2 tmpV2 = new Vector2();
	final static public Color tmpColor = new Color();

	/* INTERNAL TEMP */
	private static IntArray intArrayTemp = new IntArray(0);

	public static <T> T[] CopyArray(T[] source)
	{
		return source.clone();
	}

	/** NOTE: ARRAYS **/
	/**
	 * @see #ArrayItemsSame(float[], float[], boolean)
	 */
	public static boolean ArrayItemsSame(Object[] a, Object[] b, boolean order,
			boolean identity)
	{
		if (a.length != b.length) { return false; }

		if (order)
		{
			for (int i = 0; i < a.length; i++)
			{
				if ((identity && a[i] != b[i])
						|| (!identity && !a[i].equals(b[i]))) { return false; }
			}
			return true;
		}
		else
		{
			intArrayTemp.clear();
			intArrayTemp.ensureCapacity(a.length);
			for (int i = 0; i < a.length; i++)
			{
				Object nA = a[i];
				boolean found = false;
				for (int ii = 0; ii < b.length; ii++)
				{
					if (intArrayTemp.contains(ii)) continue;

					if ((identity && nA == b[ii])
							|| (!identity && nA.equals(b[ii])))
					{
						intArrayTemp.add(ii);
						found = true;
						break;
					}
				}
				if (!found)
				{
					intArrayTemp.clear();
					return false;
				}
			}
			intArrayTemp.clear();
			return true;
		}
	}

	/**
	 * @see #ArrayItemsSame(float[], float[], boolean)
	 */
	public static boolean ArrayItemsSame(int[] a, int[] b, boolean order)
	{
		if (a.length != b.length) { return false; }

		if (order)
		{
			for (int i = 0; i < a.length; i++)
			{
				if (a[i] != b[i]) { return false; }
			}
			return true;
		}
		else
		{
			intArrayTemp.clear();
			intArrayTemp.ensureCapacity(a.length);
			for (int i = 0; i < a.length; i++)
			{
				int nA = a[i];
				boolean found = false;
				for (int ii = 0; ii < b.length; ii++)
				{
					if (intArrayTemp.contains(ii)) continue;

					if (nA == b[ii])
					{
						intArrayTemp.add(ii);
						found = true;
						break;
					}
				}
				if (!found)
				{
					intArrayTemp.clear();
					return false;
				}
			}
			intArrayTemp.clear();
			return true;
		}
	}

	/**
	 * Checks if the two arrays contain the same values (one to one).<br>
	 * Note: If Array A has duplicate values, it will check that they match to unique values in the B Array, not many to
	 * one.
	 * 
	 * @param a
	 * @param b
	 * @param order
	 * @return
	 */
	public static boolean ArrayItemsSame(float[] a, float[] b, boolean order)
	{
		if (a.length != b.length) { return false; }

		if (order)
		{
			for (int i = 0; i < a.length; i++)
			{
				if (a[i] != b[i]) { return false; }
			}
			return true;
		}
		else
		{
			// if order does not matter, then iterate through each value of A and see if you can find it in B, if you
			// can, then save the B index so you don't mach multiple A values to same B value
			intArrayTemp.clear();
			intArrayTemp.ensureCapacity(a.length);
			for (int i = 0; i < a.length; i++)
			{
				float nA = a[i];
				boolean found = false;
				for (int ii = 0; ii < b.length; ii++)
				{
					if (intArrayTemp.contains(ii)) continue;

					if (nA == b[ii])
					{
						intArrayTemp.add(ii);
						found = true;
						break;
					}
				}
				if (!found)
				{
					intArrayTemp.clear();
					return false;
				}
			}
			intArrayTemp.clear();
			return true;
		}
	}

	/**
	 * @see #ArrayItemsSame(float[], float[], boolean)
	 */
	public static boolean ArrayItemsSame(IntArray a, IntArray b, boolean order)
	{
		if (a.size != b.size) { return false; }

		if (order)
		{
			for (int i = 0; i < a.size; i++)
			{
				if (a.get(i) != b.get(i)) { return false; }
			}
			return true;
		}
		else
		{
			intArrayTemp.clear();
			intArrayTemp.ensureCapacity(a.size);
			for (int i = 0; i < a.size; i++)
			{
				int nA = a.get(i);
				boolean found = false;
				for (int ii = 0; ii < b.size; ii++)
				{
					if (intArrayTemp.contains(ii)) continue;

					if (nA == b.get(ii))
					{
						intArrayTemp.add(ii);
						found = true;
						break;
					}
				}
				if (!found)
				{
					intArrayTemp.clear();
					return false;
				}
			}
			intArrayTemp.clear();
			return true;
		}
	}

	/**
	 * @see #ArrayItemsSame(float[], float[], boolean)
	 */
	public static boolean ArrayItemsSame(FloatArray a, FloatArray b,
			boolean order)
	{
		if (a.size != b.size) { return false; }

		if (order)
		{
			for (int i = 0; i < a.size; i++)
			{
				if (a.get(i) != b.get(i)) { return false; }
			}
			return true;
		}
		else
		{
			intArrayTemp.clear();
			intArrayTemp.ensureCapacity(a.size);
			for (int i = 0; i < a.size; i++)
			{
				float nA = a.get(i);
				boolean found = false;
				for (int ii = 0; ii < b.size; ii++)
				{
					if (intArrayTemp.contains(ii)) continue;

					if (nA == b.get(ii))
					{
						intArrayTemp.add(ii);
						found = true;
						break;
					}
				}
				if (!found)
				{
					intArrayTemp.clear();
					return false;
				}
			}
			intArrayTemp.clear();
			return true;
		}
	}

	/**
	 * Use list.toArray(new TYPE[list.size()]) instead. This does not work with primatives, have to make their own
	 * function.
	 * 
	 * @param list
	 * @param clazz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Object> T[] ToArray(ArrayList<T> list,
			Class<T> clazz)
	{
		if (list == null) return null;

		T[] array = (T[]) Array.newInstance(clazz, list.size());
		list.toArray(array);
		return array;
	}

	/* NOTE: ARRAYLIST */
	/**
	 * Removes all duplicates
	 * 
	 * @param list
	 * @param identity
	 * @return
	 */
	public static <T> ArrayList<T> ArrayListRemoveDuplicates(ArrayList<T> list,
			boolean identity)
	{
		ArrayList<T> newList = new ArrayList<T>();
		for (T item : list)
		{
			if (!ArrayListContains(newList, item, identity))
			{
				newList.add(item);
			}
		}
		list.clear();
		list.addAll(newList);
		return list;
	}

	/**
	 * Searches an arraylist (by identity optionally)
	 * 
	 * @param list
	 * @param object
	 * @param identity
	 * @return
	 */
	public static <T> boolean ArrayListContains(ArrayList<T> list, T object,
			boolean identity)
	{
		if (identity)
		{
			for (T item : list)
			{
				if (item == object) { return true; }
			}
			return false;
		}
		else
		{
			return list.contains(object);
		}
	}

	/**
	 * @see #ArrayItemsSame(float[], float[], boolean)
	 */
	public static <T> boolean ArrayListItemsSame(ArrayList<T> a,
			ArrayList<T> b, boolean order, boolean identity)
	{
		if (a.size() != b.size()) { return false; }

		if (order)
		{
			for (int i = 0; i < a.size(); i++)
			{
				if ((identity && a.get(i) != b.get(i))
						|| (!identity && !a.get(i).equals(b.get(i)))) { return false; }
			}
			return true;
		}
		else
		{
			intArrayTemp.clear();
			intArrayTemp.ensureCapacity(a.size());
			for (int i = 0; i < a.size(); i++)
			{
				Object nA = a.get(i);
				boolean found = false;
				for (int ii = 0; ii < b.size(); ii++)
				{
					if (intArrayTemp.contains(ii)) continue;

					if ((identity && nA == b.get(ii))
							|| (!identity && nA.equals(b.get(ii))))
					{
						intArrayTemp.add(ii);
						found = true;
						break;
					}
				}
				if (!found)
				{
					intArrayTemp.clear();
					return false;
				}
			}
			intArrayTemp.clear();
			return true;
		}
	}

	public static <T> ArrayList<T> ConvertToArrayList(
			com.badlogic.gdx.utils.Array<T> array)
	{
		ArrayList<T> list = new ArrayList<T>();
		for (T t : array)
		{
			list.add(t);
		}

		return list;
	}

	public static <T> ArrayList<T> ConvertToArrayList(T[] array)
	{
		ArrayList<T> list = new ArrayList<T>();
		for (T t : array)
		{
			list.add(t);
		}

		return list;
	}

	public static short[] ConvertShortListToFixed(ArrayList<Short> list)
	{
		short[] fixed = new short[list.size()];
		for (int i = 0; i < fixed.length; i++)
		{
			fixed[i] = list.get(i);
		}
		return fixed;
	}

	public static int[] ConvertIntegerListToFixed(ArrayList<Integer> list)
	{
		int[] fixed = new int[list.size()];
		for (int i = 0; i < fixed.length; i++)
		{
			fixed[i] = list.get(i);
		}
		return fixed;
	}

	public static float[] ConvertFloatListToFixed(ArrayList<Float> list)
	{
		float[] fixed = new float[list.size()];
		for (int i = 0; i < fixed.length; i++)
		{
			fixed[i] = list.get(i);
		}
		return fixed;
	}

	/**
	 * Returns an ArrayList from a fixed array given the array.
	 * 
	 * @param array
	 * @return
	 */
	public static <T> ArrayList<T> ToList(T[] array)
	{
		if (array == null) return null;
		ArrayList<T> list = new ArrayList<T>();
		for (T obj : array)
		{
			list.add(obj);
		}

		return list;
	}

	/**
	 * Returns an ArrayList from a FloatArray.
	 * 
	 * @param array
	 * @return
	 */
	public static ArrayList<Float> ToList(FloatArray array)
	{
		if (array == null) return null;
		ArrayList<Float> list = new ArrayList<Float>();
		for (int i = 0, n = array.size; i < n; i++)
		{
			list.add(array.get(i));
		}
		return list;
	}

	public static Vector2[] ConvertFloatArrayToVector2Array(float[] floats)
	{
		Vector2[] points = new Vector2[floats.length / 2];
		for (int i = 0; i < floats.length; i += 2)
		{
			points[i / 2] = new Vector2(floats[i], floats[i + 1]);
		}
		return points;
	}

	public static float[] ConvertVector2ListToFloatArray(ArrayList<Vector2> list)
	{
		float[] values = new float[list.size() * 2];
		for (int i = 0; i < list.size(); i++)
		{
			values[i * 2] = list.get(i).x;
			values[i * 2 + 1] = list.get(i).y;
		}
		return values;
	}

	public static void addFloatsToList(ArrayList<Float> list, float... values)
	{
		for (int i = 0; i < values.length; i++)
		{
			list.add(values[i]);
		}
	}

	public static void addIntsToList(ArrayList<Integer> list, int... values)
	{
		for (int i = 0; i < values.length; i++)
		{
			list.add(values[i]);
		}
	}

	public static void addShortsToList(ArrayList<Short> list, int... values)
	{
		for (int i = 0; i < values.length; i++)
		{
			list.add((short) values[i]);
		}
	}

	public static <T> ArrayList<T> addArrayToList(ArrayList<T> list, T[] array)
	{
		for (T item : array)
		{
			list.add(item);
		}

		return list;
	}

	/**
	 * Adds only the items in the other list that are not already in the main list
	 * 
	 * @param main
	 * @param other
	 * @return
	 */
	public static <T> ArrayList<T> AddUniqueToList(ArrayList<T> main,
			ArrayList<T> other)
	{
		for (T o : other)
		{
			if (!main.contains(o))
			{
				main.add(o);
			}
		}
		return main;
	}

	@SuppressWarnings("rawtypes")
	public static void PrintHashMap(HashMap hm)
	{
		if (hm == null)
		{
			System.out.println("hashmap is null.");
			return;
		}
		if (hm.isEmpty())
		{
			System.out.println("hashmap is empty.");
			return;
		}

		Iterator iterator = hm.entrySet().iterator();
		while (iterator.hasNext())
		{
			Entry e = (Entry) iterator.next();

			String key = (String) e.getKey().toString();
			String value = e.getValue().toString();

			System.out.println("key: " + key + "  value: " + value);

			// iterator.remove();
		}
	}

	@SuppressWarnings("rawtypes")
	public static void PrintArrayList(ArrayList list)
	{
		for (int i = 0; i < list.size(); i++)
		{
			System.out.println(i + ": " + list.get(i));
		}
	}

	public static void PrintArray(Object[] array)
	{
		for (int i = 0; i < array.length; i++)
		{
			System.out.println(i + ": " + array[i] + " ["
					+ array[i].getClass().getCanonicalName() + "]");
		}
	}

	public static void PrintErrorTop(int size)
	{
		for (int i = 0; i < size; i++)
		{
			for (int y = 0; y <= i; y++)
			{
				System.out.print("!");
			}
			System.out.println();
		}
	}

	public static void PrintErrorBottom(int size)
	{
		for (int i = 0; i < size; i++)
		{
			for (int y = 0; y < size - i; y++)
			{
				System.out.print("!");
			}
			System.out.println();
		}
	}

	/**
	 * Returns the first the first key found with the given value.
	 * 
	 * @param hm
	 *            The hashmap you are searching in
	 * @param value
	 *            The value you are searching for
	 * @param identity
	 *            if true, will use ==, otherwise will use equals()
	 * @return null if not found
	 */
	public static <T, X> T GetHashMapFirstKey(HashMap<T, X> hm, X value,
			boolean identity)
	{
		if (hm == null) { return null; }
		for (Iterator<Entry<T, X>> it = hm.entrySet().iterator(); it.hasNext();)
		{
			Entry<T, X> entry = it.next();
			if (identity ? entry.getValue() == value : entry.getValue().equals(
					value)) { return entry.getKey(); }
		}
		return null;
	}

	/**
	 * removes the value from the hashmap
	 * 
	 * @param hm
	 * @param value
	 * @param identity
	 *            if true, will use ==, otherwise will use equals()
	 * @param checkMultiple
	 *            should it check for multiple values or just one
	 */
	@SuppressWarnings("rawtypes")
	public static void RemoveHashMapValue(HashMap<?, ?> hm, Object value,
			boolean identity, boolean checkMultiple)
	{
		for (Iterator<?> it = hm.entrySet().iterator(); it.hasNext();)
		{
			Entry entry = (Entry) it.next();
			if (identity ? entry.getValue() == value : entry.getValue().equals(
					value))
			{
				it.remove();
				if (!checkMultiple) { return; }
			}
		}
	}

	/**
	 * Adds an object to end of an array and returns the array. If not same type
	 * 
	 * @param sourceArray
	 * @param objects
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] AppendArray(T[] sourceArray, T... objects)
	{
		if (!sourceArray.getClass().getComponentType()
				.isAssignableFrom(objects.getClass().getComponentType())) { return null; }
		T[] newArray = (T[]) Array.newInstance(sourceArray.getClass()
				.getComponentType(), sourceArray.length + objects.length);
		int i = 0;
		for (T o : sourceArray)
		{
			newArray[i++] = o;
		}
		for (T o : objects)
		{
			newArray[i++] = o;
		}
		return newArray;
	}

	/**
	 * @see #ArrayPosition(int[], int)
	 */
	public static int ArrayPosition(Object[] haystackArray, Object needle,
			boolean identity)
	{
		for (int i = 0; i < haystackArray.length; i++)
		{
			if ((identity && haystackArray[i] == needle)
					|| (!identity && haystackArray[i].equals(needle))) { return i; }
		}
		return -1;
	}

	/**
	 * Returns the index of an item in an array
	 * 
	 * @param haystackArray
	 * @param needle
	 * @return -1 if none found
	 */
	public static int ArrayPosition(int[] haystackArray, int needle)
	{
		for (int i = 0; i < haystackArray.length; i++)
		{
			if (haystackArray[i] == needle) { return i; }
		}
		return -1;
	}

	/**
	 * @see #ArrayPosition(int[], int)
	 */
	public static int ArrayPosition(float[] haystackArray, float needle)
	{
		for (int i = 0; i < haystackArray.length; i++)
		{
			if (haystackArray[i] == needle) { return i; }
		}
		return -1;
	}

	/**
	 * @see #ArrayContains(int[], int)
	 */
	public static boolean ArrayContains(Object[] haystackArray, Object needle,
			boolean identity)
	{
		if (haystackArray.length == 0) return false;
		if (ArrayPosition(haystackArray, needle, identity) >= 0)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * Check if Array contains object
	 * 
	 * @param haystackArray
	 * @param needle
	 */
	public static boolean ArrayContains(int[] haystackArray, int needle)
	{
		if (haystackArray.length == 0) return false;
		if (ArrayPosition(haystackArray, needle) >= 0)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * @see #ArrayContains(int[], int)
	 */
	public static boolean ArrayContains(float[] haystackArray, float needle)
	{
		if (haystackArray.length == 0) return false;
		if (ArrayPosition(haystackArray, needle) >= 0)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	public static String readFile(String path, Charset encoding)
			throws IOException
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return encoding.decode(ByteBuffer.wrap(encoded)).toString();
	}

	public static void dumpStack(int level)
	{
		String stack = new Throwable().getStackTrace()[level + 1].toString();
		System.out.print(stack.substring(stack.indexOf("("), stack.length())
				+ " --- ");
	}

	public static final String indentString = "   ";
	private static boolean dumpLevelsEnabledSettings;
	private static int maxLevelSetting;
	private static final LttlObjectGraphCrawler dumpCrawler = new LttlObjectGraphCrawler()
	{
		int indent = 0;

		@Override
		public boolean onStart(Object root)
		{
			indent = 0;
			if (root == null) return true;
			Class<?> rootClass = root.getClass();
			if (rootClass != Class.class
					&& LttlObjectGraphCrawler.isIgnoreCrawlClass(rootClass))
			{
				System.out
						.println("Ignoring dump on root dump object with class "
								+ rootClass.getSimpleName());
				return false;
			}
			return true;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public boolean BeforeObject(Object o, ProcessedFieldType pft)
		{
			// handle nulls
			if (o == null)
			{
				System.out.println("null");
				return false;
			}
			// don't crawl classes, but do print out their simple name
			if (o.getClass() == Class.class)
			{
				System.out.println(((Class<?>) o).getSimpleName() + " (class)");
				return false;
			}

			// already checking if Class is IgnoreCrawl in BeforeField and OnStart, so no need to check it again here.

			Class<?> clazz = o.getClass();

			if (LttlObjectGraphCrawler.isPrimative(clazz))
			{
				System.out.println(o.toString());
				return false;
			}
			else if (clazz.isArray() || clazz == ArrayList.class
					|| clazz == HashMap.class)
			{
				// collection
				int size = 0;

				if (clazz.isArray())
				{
					size = Array.getLength(o);
				}
				else if (clazz == ArrayList.class)
				{
					size = ((ArrayList) o).size();
				}
				else
				{
					size = ((HashMap) o).size();
				}

				if (dumpLevelsEnabledSettings && indent + 1 > maxLevelSetting)
				{
					System.out.println(getIndent() + "[.." + size + "..]");
					return false;
				}
				if (size == 0)
				{
					System.out.println(getIndent() + "[]");
					return false;
				}

				System.out.println("");
				System.out.println(getIndent() + "[");
			}
			else if (Class.class.isAssignableFrom(o.getClass()))
			{
				System.out.println(o.toString());
				return false;
			}
			else
			{
				// object
				if (dumpLevelsEnabledSettings && indent + 1 > maxLevelSetting)
				{
					System.out.println(getIndent() + "{...}");
					return false;
				}
				System.out.println("");
				System.out.println(getIndent() + "{");
			}
			indent++;
			return true;
		}

		@Override
		public void AfterObject(Object o, ProcessedFieldType pft)
		{
			// end object
			Class<?> clazz = o.getClass();

			indent--;
			if (clazz.isArray() || clazz == ArrayList.class
					|| clazz == HashMap.class)
			{
				System.out.println(getIndent() + "]");
			}
			else
			{
				System.out.println(getIndent() + "}");
			}
		}

		@Override
		public boolean BeforeField(Object parentObject, ProcessedFieldType pft)
		{
			indent++;

			Field f = pft.getField();
			// skip if suppose to IgnoreCraw (field or type), example would be when dumping
			// Persisted fields, don't want to crawl something like LttlComponent.renderer
			if (LttlObjectGraphCrawler.isIgnoreCrawlField(f))
			{
				indent--;
				return false;
			}

			// print out field name
			System.out.print(getIndent() + f.getName() + ": ");

			return true;
		}

		@Override
		public void AfterField(Object parentObject, ProcessedFieldType pft)
		{
			indent--;
		}

		@Override
		public boolean BeforeArrayItem(Object array, Object o, int index)
		{
			indent++;

			// print out index
			System.out.print(getIndent() + index + ": ");

			return true;
		}

		@Override
		public void AfterArrayItem(Object array, Object o, int index)
		{
			indent--;
		}

		@Override
		@SuppressWarnings("rawtypes")
		public void BeforeHashMapKey(HashMap hashMap, Object key)
		{
			indent++;

			System.out.print(getIndent() + "key: ");
		}

		@Override
		@SuppressWarnings("rawtypes")
		public void BeforeHashMapValue(HashMap hashMap, Object key)
		{
			indent++;

			System.out.print(getIndent() + "value: ");
		}

		@Override
		@SuppressWarnings("rawtypes")
		public void AfterHashMapKey(HashMap hashMap, Object key)
		{
			indent--;
		}

		@Override
		@SuppressWarnings("rawtypes")
		public void AfterHashMapValue(HashMap hashMap, Object key)
		{
			indent--;
		}

		String getIndent()
		{
			// make indentString
			String s = "";
			for (int i = 0; i < indent; i++)
			{
				s += LttlHelper.indentString;
			}

			return s;
		}
	};

	public static void dump(Object object, final int maxLevel,
			FieldsMode fieldsMode)
	{
		// set static values
		maxLevelSetting = maxLevel;
		dumpLevelsEnabledSettings = maxLevel >= 0;

		// crawl
		dumpCrawler.crawl(object, -1, fieldsMode);
	}

	public static ArrayList<Vector2> OffsetVector2List(ArrayList<Vector2> list,
			float xOffset, float yOffset)
	{
		for (Vector2 v : list)
		{
			v.add(xOffset, yOffset);
		}
		return list;
	}

	public static float[] OffsetPositionArray(float[] array, float offsetX,
			float offsetY)
	{
		for (int i = 0; i < array.length; i += 2)
		{
			array[i] += offsetX;
			array[i + 1] += offsetY;
		}
		return array;
	}

	/**
	 * Convert camelCase to Title Case
	 * 
	 * @param camelCase
	 * @return
	 */
	public static String toTitleCase(String camelCase)
	{
		StringBuilder titleCase = new StringBuilder();

		boolean init = false;
		boolean prevUpperCase = false;

		for (char c : camelCase.toCharArray())
		{
			if (Character.isSpaceChar(c))
			{
				Lttl.Throw("Can't have spaces in camelCase");
			}

			// make first uppercase
			if (!init)
			{
				init = true;
				prevUpperCase = Character.isUpperCase(c);
				titleCase.append(Character.toUpperCase(c));
				continue;
			}

			if (!prevUpperCase && Character.isUpperCase(c))
			{
				titleCase.append(' ');
				titleCase.append(c);
			}
			else
			{
				titleCase.append(c);
			}

			prevUpperCase = Character.isUpperCase(c);
		}

		return titleCase.toString();
	}

	/**
	 * Moves the item at start to dest and returns self, offsetting the other items, not replacing
	 * 
	 * @param list
	 * @param start
	 * @param dest
	 * @return
	 */
	public static <T> ArrayList<T> MoveItemArrayList(ArrayList<T> list,
			int start, int dest)
	{
		list.add(dest, list.remove(start));
		return list;
	}

	public static <T> ArrayList<T> SwapItemArrayList(ArrayList<T> list,
			int startIndex, int destIndex)
	{
		Collections.swap(list, startIndex, destIndex);
		return list;
	}

	/**
	 * non case sensitive
	 * 
	 * @param stringA
	 * @param stringB
	 * @return -1 if stringA is before stringB, 0 if same, 1 if stringA is after stringB
	 */
	public static int SortCheckAlphaNumeric(String stringA, String stringB)
	{
		for (int i = 0; i < stringA.length(); i++)
		{
			// there are more chars in stringA than stringB beyond comparing
			if (i >= stringB.length()) { return 1; }

			Character a = Character.toLowerCase(stringA.charAt(i));
			Character b = Character.toLowerCase(stringB.charAt(i));
			if (a == b)
			{
				continue;
			}
			else if (a > b)
			{
				return 1;
			}
			else
			{
				return -1;
			}
		}
		if (stringB.length() > stringA.length()) { return -1; }
		return 0;
	}

	/**
	 * Sorts the elements in the list via their toString() method
	 * 
	 * @param list
	 * @return
	 */
	public static <T> ArrayList<T> ArrayListSortAlphaNumeric(ArrayList<T> list)
	{
		Collections.sort(list, new Comparator<T>()
		{
			@Override
			public int compare(T o1, T o2)
			{
				return LttlHelper.SortCheckAlphaNumeric(o1.toString(),
						o2.toString());
			}
		});
		return list;
	}

	public static int getNumberOfDecimalPlaces(float num)
	{
		String text = Float.toString(LttlMath.abs(num));
		int integerPlaces = text.indexOf('.');
		return text.length() - integerPlaces - 1;
	}

	/**
	 * Using the toString() formats a string from a list of objects. (ie. 'one, two, and three')
	 * 
	 * @param objects
	 * @return
	 */
	public static String FormatListObjectStrings(
			ArrayList<? extends Object> objects)
	{
		String text = "";
		for (int i = 0; i < objects.size(); i++)
		{
			if (i > 0)
			{
				if (objects.size() > 2)
				{
					text += ", ";
				}
				else
				{
					text += " ";
				}
				if (i == objects.size() - 1)
				{
					text += "and ";
				}
			}
			text += objects.get(i).toString();
		}
		return text;
	}

	public static String formatFloat(int numberOfDecimals, float num)
	{
		return String.format("%." + numberOfDecimals + "f", num);
	}

	/**
	 * Converts a string to an integer.
	 * 
	 * @param string
	 * @return
	 */
	public static int StringToByteSum(String string)
	{
		int sum = 0;
		for (Byte b : string.getBytes())
		{
			sum += b.intValue();
		}
		return sum;
	}

	/**
	 * Creates Color objects from integers.
	 * 
	 * @param r
	 * @param g
	 * @param b
	 * @param a
	 * @return
	 */
	public static com.badlogic.gdx.graphics.Color Color(int r, int g, int b,
			float a)
	{
		return new com.badlogic.gdx.graphics.Color(r / 255f, g / 255f,
				b / 255f, a);
	}

	/* FLOAT ARRAYS */
	public static float getFloatArrayElement(FloatArray array,
			int elementCount, int elementIndex, int index)
	{
		return array.get(index * elementCount + elementIndex);
	}

	public static int getFloatArraySize(FloatArray array, int elementCount)
	{
		return array.size / elementCount;
	}

	public static void sortFloatArrayByElement(FloatArray array,
			int elementCount, int comparingElementIndex, boolean ascending)
	{
		// create a static reference
		FloatArray reference = LttlMath.CheckoutTmpFloatArrayInternal();
		reference.addAll(array);

		array.clear();
		outer: for (int refI = 0, refN = getFloatArraySize(reference,
				elementCount); refI < refN; refI++)
		{
			// get the comparing values
			float refComparer = getFloatArrayElement(reference, elementCount,
					comparingElementIndex, refI);
			// the array is now being ordered based on ascending
			for (int arrI = 0, arrN = getFloatArraySize(array, elementCount); arrI < arrN; arrI++)
			{
				float arrayComparer = getFloatArrayElement(array, elementCount,
						comparingElementIndex, arrI);
				if (ascending ? refComparer < arrayComparer
						: refComparer > arrayComparer)
				{
					// add all the elements
					for (int eI = 0; eI < elementCount; eI++)
					{
						array.insert(
								arrI * elementCount + eI,
								eI == comparingElementIndex ? refComparer
										: getFloatArrayElement(reference,
												elementCount, eI, refI));
					}
					// skip to the next
					continue outer;
				}
			}
			// if not before any values, then add at end
			// add all the elements
			for (int eI = 0; eI < elementCount; eI++)
			{
				array.add(eI == comparingElementIndex ? refComparer
						: getFloatArrayElement(reference, elementCount, eI,
								refI));
			}
		}
		LttlMath.ReturnTmpFloatArrayInternal();
	}

	/**
	 * Returns i the two arrays have the same values (order)
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean FloatArrayEqual(FloatArray a, FloatArray b)
	{
		if (a == b) return true;
		if ((a == null && b != null) || (b == null && a != null)) return false;
		if (a.size != b.size) return false;

		for (int i = 0, n = a.size; i < n; i++)
		{
			if (a.get(i) != b.get(i)) return false;
		}
		return true;
	}

	/* COLOR */
	/**
	 * EXPERIMENTAL - don't think this works right
	 * 
	 * @param container
	 * @param bits
	 * @return
	 */
	public static Color ColorFromFloatBits(Color container, float bits)
	{
		return container.set(NumberUtils.floatToIntColor(bits));
	}

	/**
	 * Unescapes a string. src: http://stackoverflow.com/a/4298836/2234054<br>
	 * Alternatively can use ApacheCommons's StringEscapeUtils
	 * 
	 * @param oldstr
	 * @return
	 */
	public final static String unescape(String oldstr)
	{
		StringBuffer newstr = new StringBuffer(oldstr.length());

		boolean saw_backslash = false;

		for (int i = 0; i < oldstr.length(); i++)
		{
			int cp = oldstr.codePointAt(i);
			if (oldstr.codePointAt(i) > Character.MAX_VALUE)
			{
				i++;
			}

			if (!saw_backslash)
			{
				if (cp == '\\')
				{
					saw_backslash = true;
				}
				else
				{
					newstr.append(Character.toChars(cp));
				}
				continue; /* switch */
			}

			if (cp == '\\')
			{
				saw_backslash = false;
				newstr.append('\\');
				newstr.append('\\');
				continue; /* switch */
			}

			switch (cp)
			{

				case 'r':
					newstr.append('\r');
					break; /* switch */

				case 'n':
					newstr.append('\n');
					break; /* switch */

				case 'f':
					newstr.append('\f');
					break; /* switch */

				/* PASS a \b THROUGH!! */
				case 'b':
					newstr.append("\\b");
					break; /* switch */

				case 't':
					newstr.append('\t');
					break; /* switch */

				case 'a':
					newstr.append('\007');
					break; /* switch */

				case 'e':
					newstr.append('\033');
					break; /* switch */
				case 'c':
				{
					if (++i == oldstr.length())
					{
						Lttl.Throw("trailing \\c");
					}
					cp = oldstr.codePointAt(i);
					/*
					 * don't need to grok surrogates, as next line blows them up
					 */
					if (cp > 0x7f)
					{
						Lttl.Throw("expected ASCII after \\c");
					}
					newstr.append(Character.toChars(cp ^ 64));
					break; /* switch */
				}

				case '8':
				case '9':
					Lttl.Throw("illegal octal digit");
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
					--i;
				case '0':
				{
					if (i + 1 == oldstr.length())
					{
						newstr.append(Character.toChars(0));
						break; /* switch */
					}
					i++;
					int digits = 0;
					int j;
					for (j = 0; j <= 2; j++)
					{
						if (i + j == oldstr.length())
						{
							break;
						}
						int ch = oldstr.charAt(i + j);
						if (ch < '0' || ch > '7')
						{
							break; /* for */
						}
						digits++;
					}
					if (digits == 0)
					{
						--i;
						newstr.append('\0');
						break;
					}
					int value = 0;
					try
					{
						value = Integer.parseInt(
								oldstr.substring(i, i + digits), 8);
					}
					catch (NumberFormatException nfe)
					{
						Lttl.Throw("invalid octal value for \\0 escape");
					}
					newstr.append(Character.toChars(value));
					i += digits - 1;
					break;
				}

				case 'x':
				{
					if (i + 2 > oldstr.length())
					{
						Lttl.Throw("string too short for \\x escape");
					}
					i++;
					boolean saw_brace = false;
					if (oldstr.charAt(i) == '{')
					{
						i++;
						saw_brace = true;
					}
					int j;
					for (j = 0; j < 8; j++)
					{

						if (!saw_brace && j == 2)
						{
							break;
						}
						int ch = oldstr.charAt(i + j);
						if (ch > 127)
						{
							Lttl.Throw("illegal non-ASCII hex digit in \\x escape");
						}

						if (saw_brace && ch == '}')
						{
							break;
						}

						if (!((ch >= '0' && ch <= '9')
								|| (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F')))
						{
							Lttl.Throw(String
									.format("illegal hex digit #%d '%c' in \\x",
											ch, ch));
						}
					}
					if (j == 0)
					{
						Lttl.Throw("empty braces in \\x{} escape");
					}
					int value = 0;
					try
					{
						value = Integer
								.parseInt(oldstr.substring(i, i + j), 16);
					}
					catch (NumberFormatException nfe)
					{
						Lttl.Throw("invalid hex value for \\x escape");
					}
					newstr.append(Character.toChars(value));
					if (saw_brace)
					{
						j++;
					}
					i += j - 1;
					break;
				}

				case 'u':
				{
					if (i + 4 > oldstr.length())
					{
						Lttl.Throw("string too short for \\u escape");
					}
					i++;
					int j;
					for (j = 0; j < 4; j++)
					{
						if (oldstr.charAt(i + j) > 127)
						{
							Lttl.Throw("illegal non-ASCII hex digit in \\u escape");
						}
					}
					int value = 0;
					try
					{
						value = Integer
								.parseInt(oldstr.substring(i, i + j), 16);
					}
					catch (NumberFormatException nfe)
					{
						Lttl.Throw("invalid hex value for \\u escape");
					}
					newstr.append(Character.toChars(value));
					i += j - 1;
					break;
				}

				case 'U':
				{
					if (i + 8 > oldstr.length())
					{
						Lttl.Throw("string too short for \\U escape");
					}
					i++;
					int j;
					for (j = 0; j < 8; j++)
					{
						if (oldstr.charAt(i + j) > 127)
						{
							Lttl.Throw("illegal non-ASCII hex digit in \\U escape");
						}
					}
					int value = 0;
					try
					{
						value = Integer
								.parseInt(oldstr.substring(i, i + j), 16);
					}
					catch (NumberFormatException nfe)
					{
						Lttl.Throw("invalid hex value for \\U escape");
					}
					newstr.append(Character.toChars(value));
					i += j - 1;
					break; /* switch */
				}

				default:
					newstr.append('\\');
					newstr.append(Character.toChars(cp));
					break;

			}
			saw_backslash = false;
		}

		/* weird to leave one at the end */
		if (saw_backslash)
		{
			newstr.append('\\');
		}

		return newstr.toString();
	}

	/**
	 * Returns if the bit contains the Integer (not a bit)
	 * 
	 * @param bit
	 * @param i
	 * @return
	 */
	static public boolean bitHasInt(int bit, int i)
	{
		return (bit & (1 << i)) != 0;
	}

	/**
	 * Returns if bitA and bitB share at least one Integer
	 * 
	 * @param bitA
	 * @param bitB
	 * @return
	 */
	static public boolean bitCommon(int bitA, int bitB)
	{
		return (bitA & bitB) != 0;
	}

	/**
	 * Adds the Integer (0-31 for int bits and 0-15 for short bits) to the bit.
	 * 
	 * @param bit
	 * @param i
	 * @return
	 */
	static public int bitAddInt(int bit, int i)
	{
		return bit |= 1 << i;
	}

	static public int bitRemoveInt(int bit, int i)
	{
		// TODO needs fixed
		Lttl.Throw();
		return bit &= ~i;
	}

	static public boolean bitIsNone(int bit)
	{
		return bit == 0;
	}

	static public boolean bitIsAll(int bit)
	{
		return bit == -1;
	}

	/**
	 * Checks if the bit mask should allow the tags bit.<br>
	 * This allows tags with none to still be allowed when the bitMask is set to All and if both the mask and tags are
	 * none, it does not allow.
	 * 
	 * @param bitMask
	 * @param tagsBit
	 * @return
	 */
	static public boolean bitMaskAllowTags(short bitMask, short tagsBit)
	{
		// if bit mask is set to all, then any tags will always be allowed, this allows tags to be set to none and still
		// return true
		if (bitIsAll(bitMask)) return true;
		// if bit mask is none, then doesn't matter what the tags are, it won't be allowed
		else if (bitIsNone(bitMask)) return false;
		// if bit mask has at least one in common with the tags, return true
		else if (bitCommon(bitMask, tagsBit)) return true;
		// otherwise no in common, so return false
		else return false;
	}
}
