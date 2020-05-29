package com.lttlgames.editor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

import com.lttlgames.editor.LttlObjectGraphCrawler.FieldsMode;
import com.lttlgames.helpers.LttlHelper;

/**
 * Quick access to global objects.
 * 
 * @author Josh
 */
public class Lttl
{
	private Lttl()
	{
		// Exists only to defeat instantiation.
	}

	public static LttlGame game;
	/**
	 * Scenes Manager
	 */
	public static ScenesManager scenes;

	public static final LttlDebug debug = new LttlDebug();
	public static LttlInput input;
	public static LttlTween tween;
	public static LttlEditor editor;

	/**
	 * The loop manager (details on renderering)
	 */
	public static LoopManager loop;

	// THROWS//

	/**
	 * Throws null pointer exception.
	 */
	public static void Throw()
	{
		Throw(new RuntimeException());
	}

	private static int throwAfterCallsCount = 0;

	/**
	 * throws after so many calls, good for finding out why something is being called like every frame when it shouldn't
	 * be
	 * 
	 * @param count
	 *            (like 200)
	 */
	public static void ThrowAfterCalls(int count)
	{
		throwAfterCallsCount++;
		if (throwAfterCallsCount == count)
		{
			Throw();
		}
	}

	/**
	 * Throws the provided exception and saves all open scenes before throwing.
	 * 
	 * @param exception
	 */
	public static void Throw(RuntimeException exception)
	{
		// TODO NEED TO UNCOMMENT THIS if I want this functionality enabled
		// Lttl.scenes.saveAllOpenScenes();
		throw exception;
	}

	/**
	 * Throws the a runtime exception with message and saves all open scenes before throwing.
	 * 
	 * @param exception
	 */
	public static void Throw(String message)
	{
		Throw(new RuntimeException(message));
	}

	/**
	 * Checks if the condition provided is true, and if so, throws.
	 * 
	 * @param bool
	 */
	public static void Throw(boolean bool)
	{
		if (bool) Throw();
	}

	/**
	 * Checks if the condition provided is true, and if so, throws with given message.
	 * 
	 * @param bool
	 * @param message
	 */
	public static void Throw(boolean bool, String message)
	{
		if (bool)
		{
			Throw(message);
		}
	}

	/**
	 * Checks if objects are null, and if so throws a null pointer and saves all open scenes before throwing.
	 * 
	 * @param objects
	 */
	public static void Throw(Object... objects)
	{
		for (Object o : objects)
		{
			if (o == null) Throw();
		}
	}

	static private int dumpCount = 0;

	/**
	 * Dumps incremental integers.
	 */
	public static void dump()
	{
		LttlHelper.dumpStack(1);
		System.out.println(dumpCount++);
	}

	/**
	 * Dumps the object's graph. Includes all levels and all fields, regardless if persisted or not.
	 * 
	 * @param object
	 *            the object to dump
	 * @return object dumping
	 */
	public static <T> T dump(T o)
	{
		LttlHelper.dumpStack(1);
		LttlHelper.dump(o, -1, FieldsMode.AllButIgnore);
		return o;
	}

	public static void logNote(String message)
	{
		if (LttlGameStarter.get().getLogLevel() < 0
				|| LttlGameStarter.get().getLogLevel() == 2) return;
		LttlHelper.dumpStack(1);
		System.out.println(message);
	}

	public static void logError(String message)
	{
		if (LttlGameStarter.get().getLogLevel() < 0
				|| LttlGameStarter.get().getLogLevel() == 1) return;
		LttlHelper.dumpStack(1);
		System.out.println("***" + message);
	}

	/**
	 * Dumps the object's graph.
	 * 
	 * @param object
	 *            the object to dump
	 * @param maxLevel
	 *            how many levels should be dumped (-1 is all)
	 * @return object dumping
	 */
	public static <T> T dump(T object, final int maxLevel)
	{
		LttlHelper.dumpStack(1);
		LttlHelper.dump(object, maxLevel, FieldsMode.AllButIgnore);
		return object;
	}

	/**
	 * Dumps the object's graph.
	 * 
	 * @param object
	 *            the object to dump
	 * @param maxLevel
	 *            how many levels should be dumped (-1 is all)
	 * @param fieldsMode
	 * @return object dumping
	 */
	public static <T> T dump(T object, final int maxLevel, FieldsMode fieldsMode)
	{
		LttlHelper.dumpStack(1);
		LttlHelper.dump(object, maxLevel, fieldsMode);
		return object;
	}

	/**
	 * Dumps the object's graph and dies. Includes all levels and all fields, regardless if persisted or not.
	 * 
	 * @param o
	 *            object to dump
	 * @return
	 */
	public static void ddump(Object o)
	{
		ddump(o, -1, FieldsMode.AllButIgnore);
	}

	/**
	 * Dumps the object's graph and dies.
	 * 
	 * @param object
	 *            the object to dump
	 * @param maxLevel
	 *            how many levels should be dumped (-1 is all)
	 * @param fieldMode
	 * @return object dumping
	 */
	public static void ddump(Object object, final int maxLevel,
			FieldsMode fieldMode)
	{
		LttlHelper.dump(object, maxLevel, fieldMode);
		Lttl.Throw();
	}

	/**
	 * Dumps several object's graph. Includes all levels and all fields, regardless if persisted or not.
	 * 
	 * @param objs
	 *            the objects to dump
	 */
	public static void dump(Object... objs)
	{
		LttlHelper.dumpStack(1);
		LttlHelper.dump(objs, -1, FieldsMode.AllButIgnore);
	}

	/**
	 * Prints all the objects' toString() (including collections).
	 * 
	 * @param objs
	 */
	public static void dumpToString(Object... objs)
	{
		LttlHelper.dumpStack(1);
		System.out.println("");
		for (Object obj : objs)
		{
			if (Collection.class.isAssignableFrom(obj.getClass()))
			{
				System.out.println("[");
				Collection<?> c = (Collection<?>) obj;
				for (Object o : c)
				{
					System.out.println(LttlHelper.indentString + o.toString());
				}
				System.out.println("]");
			}
			else if (obj.getClass().isArray())
			{
				Object[] a = (Object[]) obj;
				System.out.println("[");
				for (Object o : a)
				{
					System.out.println(LttlHelper.indentString + o.toString());
				}
				System.out.println("]");
			}
			else if (obj.getClass() == HashMap.class)
			{
				HashMap<?, ?> h = (HashMap<?, ?>) obj;
				System.out.println("[");
				for (Entry<?, ?> e : h.entrySet())
				{
					System.out.println(LttlHelper.indentString
							+ e.getKey().toString() + " | "
							+ e.getValue().toString());
				}
				System.out.println("]");
			}
			else
			{
				System.out.println(obj.toString());
			}
		}
	}

	/**
	 * Dumps several object's graph.
	 * 
	 * @param maxLevel
	 *            how many levels should be dumped (-1 is all)
	 * @param fieldMode
	 * @param objs
	 *            the objects to dump
	 */
	public static void dump(final int maxLevel, FieldsMode fieldMode,
			Object... objs)
	{
		LttlHelper.dumpStack(1);
		LttlHelper.dump(objs, maxLevel, fieldMode);
	}

	/**
	 * does not save scenes or print stack trace
	 */
	public static void die()
	{
		throw new RuntimeException("die");
	}

	/**
	 * Logs a note error if the object is null, that way you can know something is up without throwing. It returns the
	 * boolean so it can be used inline.
	 * 
	 * @param objectToCheckIfNull
	 * @return true if null
	 */
	public static boolean quiet(Object objectToCheckIfNull)
	{
		if (objectToCheckIfNull == null)
		{
			if (!(LttlGameStarter.get().getLogLevel() < 0 || LttlGameStarter
					.get().getLogLevel() == 2))
			{
				LttlHelper.dumpStack(1);
				System.out.println("Silent Error");
			}
			return true;
		}
		return false;
	}

	/**
	 * Logs a note error if the condition is true, that way you can know something is up without throwing. It returns
	 * the condition so it can be used inline.
	 * 
	 * @param condition
	 * @return the condition result
	 */
	public static boolean quiet(boolean condition)
	{
		if (condition
				&& !(LttlGameStarter.get().getLogLevel() < 0 || LttlGameStarter
						.get().getLogLevel() == 2))
		{
			LttlHelper.dumpStack(1);
			System.out.println("Silent Error");
		}
		return condition;

	}

	public static boolean debugTrigger = false;
}
