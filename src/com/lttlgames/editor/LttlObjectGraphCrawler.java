package com.lttlgames.editor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Joint;
import com.badlogic.gdx.physics.box2d.JointDef;
import com.badlogic.gdx.physics.box2d.JointEdge;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array.ArrayIterable;
import com.badlogic.gdx.utils.BooleanArray;
import com.badlogic.gdx.utils.CharArray;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.LongArray;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Predicate;
import com.lttlgames.editor.annotations.DoCopy;
import com.lttlgames.editor.annotations.DoCopyByReference;
import com.lttlgames.editor.annotations.DoExport;
import com.lttlgames.editor.annotations.DoNotCopy;
import com.lttlgames.editor.annotations.DoNotExport;
import com.lttlgames.editor.annotations.GuiHide;
import com.lttlgames.editor.annotations.GuiShow;
import com.lttlgames.editor.annotations.IgnoreCrawl;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.helpers.LttlHelper;
import com.vividsolutions.jts.geom.Geometry;

/**
 * @author Josh
 */
public class LttlObjectGraphCrawler
{
	private FieldsMode fieldsMode = FieldsMode.All;
	private int maxTiers = -1; // -1 is all
	private int currentTier = 0;
	private static ObjectMap<FieldsMode, ObjectMap<Class<?>, ArrayList<ProcessedFieldType>>> fieldsCacheMap = new ObjectMap<LttlObjectGraphCrawler.FieldsMode, ObjectMap<Class<?>, ArrayList<ProcessedFieldType>>>(
			3);
	private static ObjectMap<Field, Persist> persisFieldtMap = new ObjectMap<Field, Persist>();
	private static ObjectMap<Class<?>, Persist> persisClasstMap = new ObjectMap<Class<?>, Persist>();

	/**
	 * These classes are GDX (and Java) classes that can't have the @Persisted Annotation on them, but they should still
	 * be persisted. Their public variables will be persisted with their field names. Also these will automatically show
	 * up in GUI unless the field they are on has the GuiHide annotation
	 */
	private static final ArrayList<Class<?>> libraryPersistedClasses = new ArrayList<Class<?>>();
	static
	{
		libraryPersistedClasses.add(Vector2.class);
		libraryPersistedClasses.add(Color.class);
		libraryPersistedClasses.add(Float.class);
		libraryPersistedClasses.add(Integer.class);
		libraryPersistedClasses.add(ArrayList.class);
		libraryPersistedClasses.add(HashMap.class);
		libraryPersistedClasses.add(IntArray.class);
		libraryPersistedClasses.add(FloatArray.class);
		libraryPersistedClasses.add(BooleanArray.class);
		libraryPersistedClasses.add(CharArray.class);
		libraryPersistedClasses.add(LongArray.class);
		libraryPersistedClasses.add(Rectangle.class);
	}
	/**
	 * These are classes (and their subclasses) that should be ignored in crawl, that way they don't need a @IgnoreCrawl
	 * annotation
	 */
	private static final ArrayList<Class<?>> libraryIgnoreCrawlClasses = new ArrayList<Class<?>>();
	static
	{
		// java
		// FIXME maybe
		// libraryIgnoreCrawlClasses.add(JComponent.class);
		libraryIgnoreCrawlClasses.add(Class.class);

		// Gdx
		libraryIgnoreCrawlClasses.add(OrthographicCamera.class);
		libraryIgnoreCrawlClasses.add(BitmapFont.class);
		libraryIgnoreCrawlClasses.add(Matrix3.class);
		libraryIgnoreCrawlClasses.add(Matrix4.class);
		libraryIgnoreCrawlClasses.add(AtlasRegion.class);
		libraryIgnoreCrawlClasses.add(Texture.class);
		libraryIgnoreCrawlClasses.add(Pixmap.class);
		libraryIgnoreCrawlClasses.add(Pool.class);

		// Gdx.box2d
		libraryIgnoreCrawlClasses.add(Body.class);
		libraryIgnoreCrawlClasses.add(BodyDef.class);
		libraryIgnoreCrawlClasses.add(Shape.class);
		libraryIgnoreCrawlClasses.add(Joint.class);
		libraryIgnoreCrawlClasses.add(JointDef.class);
		libraryIgnoreCrawlClasses.add(JointEdge.class);
		libraryIgnoreCrawlClasses.add(Fixture.class);
		libraryIgnoreCrawlClasses.add(FixtureDef.class);
		libraryIgnoreCrawlClasses.add(World.class);

		// Gdx.Array
		libraryIgnoreCrawlClasses.add(ArrayIterable.class);
		libraryIgnoreCrawlClasses.add(Predicate.PredicateIterable.class);

		// Gdx.IntMap
		libraryIgnoreCrawlClasses.add(IntMap.Entries.class);
		libraryIgnoreCrawlClasses.add(IntMap.Values.class);
		libraryIgnoreCrawlClasses.add(IntMap.Keys.class);

		// Ignoring these because don't need to be looking in them for references since they are only primative, if you
		// want to dump() them use their items array, they will still be persisted though
		libraryIgnoreCrawlClasses.add(IntArray.class);
		libraryIgnoreCrawlClasses.add(FloatArray.class);
		libraryIgnoreCrawlClasses.add(BooleanArray.class);
		libraryIgnoreCrawlClasses.add(CharArray.class);
		libraryIgnoreCrawlClasses.add(LongArray.class);

		// jts
		libraryIgnoreCrawlClasses.add(Geometry.class);
	}

	/**
	 * This should never be called directly, should just use {@link #isIgnoreCrawlClass(Class)}
	 * 
	 * @param clazz
	 * @return
	 */
	static private boolean isLibraryIgnoreCrawlClass(Class<?> clazz)
	{
		for (Class<?> c : libraryIgnoreCrawlClasses)
		{
			if (c.isAssignableFrom(clazz)) { return true; }
		}
		return false;
	}

	private static Field modifiersField;
	static
	{
		try
		{
			modifiersField = Field.class.getDeclaredField("modifiers");
			modifiersField.setAccessible(true);
		}
		catch (NoSuchFieldException e)
		{
			e.printStackTrace();
		}
		catch (SecurityException e)
		{
			e.printStackTrace();
		}
	}

	public Object[] objects;

	/**
	 * @param objects
	 *            Pass in some objects that you want to test for, since the context is inside the ObjectGraphCrawler.
	 */
	public LttlObjectGraphCrawler(Object... objects)
	{
		this.objects = objects;
	}

	/**
	 * Crawls object's persisted fields through all tiers
	 * 
	 * @param o
	 */
	public void crawl(Object o)
	{
		if (!onStart(o)) return;
		crawl(o, -1, FieldsMode.Persisted);
		onCompletion();
	}

	/**
	 * Crawls object skipping all static fields with the specifications in the parameteres
	 * 
	 * @param o
	 *            object to crawl (Object, Array, ArrayList, or HashMap)
	 * @param maxTiers
	 *            how many tiers deep to go on the object graph [-1 = all] (ie. 1 would be all the fields of the
	 *            baseobject, but none of those fields' fields or items if an array, so nothing less than 1
	 *            realistically)
	 * @param fieldsMode
	 */
	public void crawl(Object o, int maxTiers, FieldsMode fieldsMode)
	{
		if (!onStart(o)) return;
		currentTier = 0;
		this.fieldsMode = fieldsMode;
		this.maxTiers = maxTiers;
		preProcess(o, null);
	}

	private void crawlObject(Object o, ProcessedFieldType pft)
	{
		// FIELDS (PUBLIC AND PRIVATES)
		Class<?> c = o.getClass();
		for (ProcessedFieldType pftI : LttlObjectGraphCrawler.getAllFields(c,
				fieldsMode, (pft != null) ? pft.getParam() : null))
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
				// crawl the field
				crawlField(pftI, o);
			}
			catch (IllegalArgumentException e)
			{
				e.printStackTrace();
			}
			catch (IllegalAccessException e)
			{
				e.printStackTrace();
			}

			// if private, return the accessibility to false
			if (isPrivate)
			{
				f.setAccessible(false);
			}
		}
	}

	private void crawlArray(Object o)
	{
		// iterate through array
		for (int i = 0; i < Array.getLength(o); i++)
		{
			Object item = Array.get(o, i);
			// check custom skip
			if (!BeforeArrayItem(o, item, i)) continue;

			preProcess(item, null);

			AfterArrayItem(o, item, i);
		}
	}

	private void crawlArrayList(Object o, ProcessedFieldType pft)
	{
		// check if the o exists and it can become an ArrayList and is not empty
		@SuppressWarnings("unchecked")
		ArrayList<? extends Object> aL = (ArrayList<? extends Object>) o;
		if (o == null || aL == null) return;

		if (pft != null && pft.getParamCount() < 1)
		{
			Lttl.Throw("Should be at least one parameter.");
		}

		// iterate through array
		for (int i = 0; i < aL.size(); i++)
		{
			Object item = aL.get(i);
			// check custom skip
			if (!BeforeArrayItem(o, item, i)) continue;

			preProcess(item, (pft != null) ? pft.getParam(0) : null);

			AfterArrayItem(o, item, i);
		}
	}

	@SuppressWarnings("rawtypes")
	private void crawlHashMap(Object o, ProcessedFieldType pft)
	{
		@SuppressWarnings("unchecked")
		HashMap<? extends Object, ? extends Object> hm = (HashMap<? extends Object, ? extends Object>) o;
		if (o == null || hm == null) return;

		if (pft != null && pft.getParamCount() < 2)
		{
			Lttl.Throw("Should be at least two parameter.");
		}

		// iterate through hashmap
		for (Iterator it = hm.entrySet().iterator(); it.hasNext();)
		{
			Map.Entry pairs = (Map.Entry) it.next();

			// check custom skip
			if (!BeforeHashMapEntry(hm, pairs.getKey(), pairs.getValue()))
			{
				continue;
			}

			for (int i = 0; i < 2; i++)
			{
				ProcessedFieldType currentPft;
				Object currentObject = null;
				if (i == 0)
				{
					// process key
					currentObject = pairs.getKey();
					currentPft = (pft != null) ? pft.getParam(0) : null;
					BeforeHashMapKey(hm, currentObject);
				}
				else if (i == 1)
				{
					// process value
					currentObject = pairs.getValue();
					currentPft = (pft != null) ? pft.getParam(1) : null;
					BeforeHashMapValue(hm, currentObject);
				}
				else
				{
					break;
				}

				preProcess(currentObject, currentPft);

				if (i == 0)
				{
					AfterHashMapKey(hm, currentObject);
				}
				else if (i == 1)
				{
					AfterHashMapValue(hm, currentObject);
				}

			}
			AfterHashMapEntry(hm, pairs.getKey(), pairs.getValue());
		}
	}

	private void crawlField(ProcessedFieldType pft, Object o)
			throws IllegalArgumentException, IllegalAccessException
	{
		// ///CHECK IF SHOULD SKIP FIELD OR NOT/////
		// skip static variables
		Field f = pft.getField();
		// run custom check
		if (!BeforeField(o, pft)) return;
		preProcess(f.get(o), pft);
		AfterField(o, pft);
	}

	// pft may be null because it could be crawling through the root object, which could be a list or hashmap
	private void preProcess(Object o, ProcessedFieldType pft)
	{
		// check tiers
		currentTier++;
		if (maxTiers >= 0 && currentTier > maxTiers)
		{
			currentTier--;
			return;
		}

		if (!BeforeObject(o, pft))
		{
			currentTier--;
			return;
		}

		// skip nulls
		if (o == null)
		{
			currentTier--;
			return;
		}

		Class<?> objectClass = o.getClass();
		if (isPrimative(objectClass))
		{
			currentTier--;
			return;
		}
		else if (objectClass.isArray() || objectClass == Array.class)
		{
			crawlArray(o);
		}
		else if (objectClass == ArrayList.class)
		{
			crawlArrayList(o, pft);
		}
		else if (objectClass == HashMap.class)
		{
			crawlHashMap(o, pft);
		}
		else
		{
			crawlObject(o, pft);
		}

		AfterObject(o, pft);

		// reverting tiers
		currentTier--;
	}

	/**
	 * Checks if class specified is primative or not (includes enum and Boxed primatives)
	 * 
	 * @param type
	 * @return
	 */
	public static boolean isPrimative(Class<?> type)
	{
		if (type.isPrimitive() || type == String.class || type == Boolean.class
				|| type == boolean.class || isIntegerLikePrimative(type)
				|| isFloatLikePrimative(type) || type == Character.class
				|| type == char.class || type.isEnum())
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * Check if type is int, long, short, or byte
	 * 
	 * @param type
	 * @return
	 */
	public static boolean isIntegerLikePrimative(Class<?> type)
	{
		return type == Integer.class || type == int.class
				|| type == Short.class || type == short.class
				|| type == Long.class || type == long.class
				|| type == Byte.class || type == byte.class;
	}

	/**
	 * Check if type is float or double
	 * 
	 * @param type
	 * @return
	 */
	public static boolean isFloatLikePrimative(Class<?> type)
	{
		return type == Float.class || type == float.class
				|| type == Double.class || type == double.class;
	}

	public enum FieldsMode
	{
		/**
		 * Returns all fields regardless of any annotations. (never used)
		 */
		All,
		/**
		 * Returns all fields (except for {@link IgnoreCrawl}) (ideal for skipping LttlTransform's children and
		 * components, renderer, etc)
		 */
		AllButIgnore, /**
		 * Returns the fields that should be copied: {@link Persist} and {@link DoCopy}, and not
		 * {@link DoNotCopy} if persisted). This will also check for {@link DoCopyByReference} fields.<br>
		 * Does not check for {@link IgnoreCrawl} on the field.
		 */
		Copy,
		/**
		 * Returns all fields that are persisted ({@link Persist}<br>
		 * Does not check for {@link IgnoreCrawl} on the field because there are cases where something is Persisted but
		 * also IgnoreCrawl (ie. renderer and FloatArray)
		 */
		Persisted,
		/**
		 * Will return only the fields that should show in GUI ({@link Persist}, not {@link GuiHide}, {@link GuiShow} if
		 * not persisted, and static variables if {@link GuiShow} and not persisted)<br>
		 * Does not check for {@link IgnoreCrawl} on the field.
		 */
		GUI,
		/**
		 * All persisted fields that do not have {@link DoNotExport} or non persisted fields that do have {@link Export}<br>
		 * Does not check for {@link IgnoreCrawl} on the field.
		 */
		Export
	}

	// OPTIMIZE could run this on all LttlClassMap classes when game starts, but probbaly not necessary since it doesn't
	// lag badly when doing getAllFields every so often
	/**
	 * Simply returns an unmodified list of fields
	 * 
	 * @param c
	 * @param fieldsMode
	 * @param paramPfts
	 *            this is all the ProcessedFieldTypes of the param types on Class c
	 * @return
	 */
	public static List<ProcessedFieldType> getAllFields(Class<?> c,
			FieldsMode fieldsMode, ProcessedFieldType... paramPfts)
	{
		ArrayList<ProcessedFieldType> fields;

		// check if can get cached fields (only cache root classes with non params objects)
		if (paramPfts == null || paramPfts.length == 0 || paramPfts[0] == null)
		{
			ObjectMap<Class<?>, ArrayList<ProcessedFieldType>> fieldModeMap = fieldsCacheMap
					.get(fieldsMode);
			if (fieldModeMap == null)
			{
				// create and add field mode map since it hasn't been created yet
				fieldModeMap = new ObjectMap<Class<?>, ArrayList<ProcessedFieldType>>();
				fieldsCacheMap.put(fieldsMode, fieldModeMap);
			}
			fields = fieldModeMap.get(c);
			if (fields == null)
			{
				// create and add field list since it hasn't been created yet
				fields = new ArrayList<ProcessedFieldType>();
				fieldModeMap.put(c, fields);

				// don't return it yet, since it's new, need to populate it with fields first
			}
			else
			{
				// it has a cache
				return Collections.unmodifiableList(fields);
			}
		}
		else
		{
			fields = new ArrayList<ProcessedFieldType>();
		}

		// Lttl.dump("Generating Class Field Map for " + fieldsMode.toString()
		// + " - " + c.getSimpleName());

		// need to cycle through super classes because when using getDeclaredFields() it only returns current classes
		// fields
		Class<?> currentClass = c;
		while (currentClass != Object.class)
		{
			// this gets all of the currentClass's fields (private and public), instead of of the normal .getFields()
			// which gets all public fields on all super classes too
			for (Field f : currentClass.getDeclaredFields())
			{
				// skip statics (with a specific check for if it is GUI mode, it can be static as long as it has GuiShow
				// since GUI mode can show statics)
				// also skip synthetics and transients
				if ((fieldsMode == FieldsMode.GUI && isStatic(f) && !f
						.isAnnotationPresent(GuiShow.class))
						|| (fieldsMode != FieldsMode.GUI && isStatic(f))
						|| f.isSynthetic()
						|| Modifier.isTransient(f.getModifiers()))
				{
					continue;
				}

				ProcessedFieldType pft = new ProcessedFieldType(f, paramPfts);

				// only add it without checking if it's persisted or if it's copied if getting all the fields
				// only check if ignoreCrawl field if ALLButIgnore, because sometimes it is persisted but also ignored
				switch (fieldsMode)
				{
					case All:
						fields.add(pft);
						break;
					case AllButIgnore:
						if (!isIgnoreCrawlField(f))
						{
							fields.add(pft);
						}
						break;
					case Copy:
						if (isFieldCopied(pft))
						{
							fields.add(pft);
						}
						break;
					case Persisted:
						if (isFieldPersisted(pft))
						{
							fields.add(pft);
						}
						break;
					case GUI:
						if (GuiHelper.isFieldDrawnGui(pft))
						{
							fields.add(pft);
						}
						break;
					case Export:
						if (isFieldExported(pft))
						{
							fields.add(pft);
						}
						break;
					default:
						break;
				}
			}

			// GET THE PARAM TYPE FOR SUPER CLASS OF CURRENT CLASS SO NEXT CLASS WILL HAVE IT
			paramPfts = getParamTypesOfSuperClass(currentClass, paramPfts);

			// go to next super class
			currentClass = currentClass.getSuperclass();
		}
		return Collections.unmodifiableList(fields);
	}

	/**
	 * Returns if the field has {@link IgnoreCrawl} or if it's type class is meant to be ignored.
	 * 
	 * @param field
	 * @return
	 */
	public static boolean isIgnoreCrawlField(Field field)
	{
		return field.isAnnotationPresent(IgnoreCrawl.class)
				|| isIgnoreCrawlClass(field.getType());
	}

	/**
	 * Returns if this class is meant to be ignored (checks self and super classes for {@link IgnoreCrawl}, also checks
	 * if {@link #isLibraryIgnoreCrawlClass(Class)}
	 * 
	 * @param clazz
	 * @return
	 */
	public static boolean isIgnoreCrawlClass(Class<?> clazz)
	{
		return isAnnotationPresentOnClassTree(clazz, IgnoreCrawl.class)
				|| isLibraryIgnoreCrawlClass(clazz);
	}

	/**
	 * Simply returns unmodified list of fields (no param types passed)
	 * 
	 * @param c
	 *            the class
	 * @param fieldsMode
	 * @return
	 */
	public static List<ProcessedFieldType> getAllFields(Class<?> c,
			FieldsMode fieldsMode)
	{
		return getAllFields(c, fieldsMode, (ProcessedFieldType[]) null);
	}

	public static boolean isFieldExported(ProcessedFieldType processedFieldType)
	{
		return (LttlObjectGraphCrawler.isFieldPersisted(processedFieldType) && !processedFieldType
				.getField().isAnnotationPresent(DoNotExport.class))
				|| processedFieldType.getField().isAnnotationPresent(
						DoExport.class);
	}

	/**
	 * If field would be persisted into json (has persist annotation, or is on a libraryPersistedClass and is public and
	 * the field type is of a class that can be persisted)
	 * 
	 * @param processedFieldType
	 * @return
	 */
	public static boolean isFieldPersisted(ProcessedFieldType processedFieldType)
	{
		boolean isFieldTypePersisted = isClassPersisted(processedFieldType
				.getCurrentClass());
		boolean isPersistPresent = getPersistFieldAnnotation(processedFieldType
				.getField()) != null;

		if (!isFieldTypePersisted && isPersistPresent)
		{
			Lttl.Throw("Field Type: "
					+ processedFieldType.getCurrentClass().getName()
					+ " can't be persisted."
					+ (processedFieldType.getCurrentClass() == Object.class ? " Be more descriptive with the param type arguments (not <?>)."
							: "")
					+ " It's on field '"
					+ processedFieldType.getField().getName()
					+ "' ("
					+ processedFieldType.getField().getDeclaringClass()
							.getName() + ")"
					+ " which has a persist annotation.");
		}
		return isFieldTypePersisted
				&& (isPersistPresent || (isLibraryPersistedClass(processedFieldType
						.getField().getDeclaringClass()) && !LttlObjectGraphCrawler
						.isPrivateOrProtectedOrDefault(processedFieldType
								.getField())));
	}

	/**
	 * Checks if this field is one that should be copied (all persisted field that don't have @DoNotCopy or any field
	 * with @DoCopy or @DoCopyByReference
	 * 
	 * @param processedFieldType
	 * @return
	 */
	public static boolean isFieldCopied(ProcessedFieldType processedFieldType)
	{
		Field field = processedFieldType.getField();
		if (field.isAnnotationPresent(DoNotCopy.class)) return false;

		boolean isPersisted = isFieldPersisted(processedFieldType);

		if (field.isAnnotationPresent(DoCopyByReference.class))
		{
			if (isPersisted)
			{
				Lttl.Throw("Field "
						+ field.getName()
						+ " on class "
						+ field.getDeclaringClass().getSimpleName()
						+ " is persisted but also a CopyByReference which is not allowed.");
				return false;
			}
			else
			{
				return true;
			}
		}
		else
		{
			return field.isAnnotationPresent(DoCopy.class) || isPersisted;
		}
	}

	/**
	 * If class persisted into json
	 * 
	 * @param clazz
	 *            that the field is on
	 * @return
	 */
	public static boolean isClassPersisted(Class<?> clazz)
	{
		// can't really hurt, if any object is saved, it will just use it's class
		// all interfaces that are in GUI will be assumed to be LttlComponent interfaces
		if (clazz.isInterface()) return true;

		// if array then get the componentType class and check if it is persisted, makes sure to get the component type
		if (clazz.isArray()) { return isClassPersisted(clazz.getComponentType()); }

		boolean isPrimative = isPrimative(clazz);
		if (isPrimative || getPersistClassAnnotation(clazz) != null
				|| isLibraryPersistedClass(clazz))
		{
			// if the class is suppose to be persisted (and not a primative), then run it through this check
			// which throws errors so we know what is going on
			if (!isPrimative)
			{
				checkPersistedClassValidity(clazz);
			}
			return true;
		}
		return false;
	}

	public static boolean isLibraryPersistedClass(Class<?> clazz)
	{
		return libraryPersistedClasses.contains(clazz);
	}

	/**
	 * Checks if the field's modifiers are default (package-private), private, or protected.
	 * 
	 * @param f
	 * @return
	 */
	public static boolean isPrivateOrProtectedOrDefault(Field f)
	{
		int m = f.getModifiers();
		return (isDefaultModifer(m) || Modifier.isPrivate(m) || Modifier
				.isProtected(m));
	}

	public static boolean isDefaultModifer(int modifer)
	{
		// 0 is default
		// 16 is default final
		return modifer == 0 || modifer == 16;
	}

	public static boolean isStatic(Field f)
	{
		return Modifier.isStatic(f.getModifiers());
	}

	// OVERIDE METHODS TO CUSTOM BEHAVIOUR
	/**
	 * Runs every time before crawling an object, could be an Object, Priamtive, Array, ArrayList, or HashMap.<br>
	 * Can return false to skip crawling this object.<br>
	 * You need to be check if it is null too.
	 * 
	 * @param o
	 * @param pft
	 *            The paramterized types object, which can be used to get the type of a param type object or list or
	 *            hashmap, check if null first
	 * @return
	 */
	public boolean BeforeObject(Object o, ProcessedFieldType pft)
	{
		return true;
	}

	/**
	 * Runs every time after crawling an object, could be an Object, Primative, Array, ArrayList, or HashMap.<br>
	 * 
	 * @param o
	 * @param pft
	 *            The paramterized types object, which can be used to get the param types of a generic object or list or
	 *            hashmap, check if null first
	 */
	public void AfterObject(Object o, ProcessedFieldType pft)
	{
	}

	/**
	 * Runs every time before crawling a field.<br>
	 * Can return false to skip crawling this field.
	 * 
	 * @param parentObject
	 * @param f
	 * @return
	 */
	public boolean BeforeField(Object parentObject, ProcessedFieldType pft)
	{
		return true;
	}

	/**
	 * Runs every time after crawling a field.
	 * 
	 * @param parentObject
	 * @param f
	 */
	public void AfterField(Object parentObject, ProcessedFieldType pft)
	{
	}

	/**
	 * Runs every time before crawling an Array or ArrayList item.<br>
	 * Can return false to skip crawling this item.<br>
	 * You need to be check if it is null too.
	 * 
	 * @param array
	 *            the Array or ArrayList object, may need to cast
	 * @param o
	 *            the object in the Array/ArrayList
	 * @param index
	 *            the index of the object in the Array/ArrayList
	 * @return
	 */
	public boolean BeforeArrayItem(Object array, Object o, int index)
	{
		return true;
	}

	/**
	 * Runs every time after crawling an Array or ArrayList item.
	 * 
	 * @param array
	 *            the Array or ArrayList object, will need to cast
	 * @param o
	 *            the object in the Array/ArrayList
	 * @param index
	 *            the index of the object in the Array/ArrayList
	 */
	public void AfterArrayItem(Object array, Object o, int index)
	{
	}

	/**
	 * Runs every time before crawling a HashMap entry's (key and valeu).<br>
	 * Can return false to skip crawling these items.<br>
	 * You need to check if it is null too.<br>
	 * <br>
	 * Note: The crawler will crawl the key and then the value and they have their own Before and After callbacks for
	 * labeling purposes
	 * 
	 * @param hashMap
	 *            the HashMap object
	 * @param key
	 * @param value
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public boolean BeforeHashMapEntry(HashMap hashMap, Object key, Object value)
	{
		return true;
	}

	/**
	 * Runs before processing the key
	 * 
	 * @param hashMap
	 * @param key
	 */
	@SuppressWarnings("rawtypes")
	public void BeforeHashMapKey(HashMap hashMap, Object key)
	{
	}

	/**
	 * Runs before processing the value
	 * 
	 * @param hashMap
	 * @param Value
	 */
	@SuppressWarnings("rawtypes")
	public void BeforeHashMapValue(HashMap hashMap, Object Value)
	{
	}

	/**
	 * Runs after processing the key
	 * 
	 * @param hashMap
	 * @param key
	 */
	@SuppressWarnings("rawtypes")
	public void AfterHashMapKey(HashMap hashMap, Object key)
	{
	}

	/**
	 * Runs after processing the value
	 * 
	 * @param hashMap
	 * @param Value
	 */
	@SuppressWarnings("rawtypes")
	public void AfterHashMapValue(HashMap hashMap, Object Value)
	{
	}

	/**
	 * Runs every time after crawling a HashMap entry's (key and value).
	 * 
	 * @param hashMap
	 *            the HashMap object, will need to cast
	 * @param key
	 * @param value
	 */
	public void AfterHashMapEntry(Object hashMap, Object key, Object value)
	{
	}

	/**
	 * Runs after the crawl finishes.
	 */
	public void onCompletion()
	{

	}

	/**
	 * Runs before crawl. If returns false, then stops crawl.
	 * 
	 * @param root
	 *            the object that is being crawled
	 * @return
	 */
	public boolean onStart(Object root)
	{
		return true;
	}

	/**
	 * Checks if this class or any super classes have this annotation
	 * 
	 * @param clazz
	 * @param annotationClass
	 * @return
	 */
	public static boolean isAnnotationPresentOnClassTree(Class<?> clazz,
			Class<? extends Annotation> annotationClass)
	{
		Class<?> currentClass = clazz;

		while (currentClass != null && currentClass != Object.class)
		{
			if (currentClass.isAnnotationPresent(annotationClass)) { return true; }
			currentClass = currentClass.getSuperclass();
		}
		return false;
	}

	public static int getArrayDimensions(Class<?> arrayClass)
	{
		if (!arrayClass.isArray())
		{
			Lttl.Throw(arrayClass.getName() + " is not an array.");
		}

		int dimensions = 1;
		// iterate through the component type to see how many dimensions there are
		Class<?> current = arrayClass;
		while ((current = current.getComponentType()).isArray())
		{
			dimensions++;
		}
		return dimensions;
	}

	/**
	 * Gets the base component type of an array (ie. Color[][][])
	 * 
	 * @param array
	 * @return
	 */
	public static Class<?> getBaseComponentType(Class<?> arrayClass)
	{
		if (!arrayClass.isArray())
		{
			Lttl.Throw(arrayClass.getName() + " is not an array.");
		}

		Class<?> baseComponentType = arrayClass.getComponentType();
		while (baseComponentType.isArray())
		{
			baseComponentType = baseComponentType.getComponentType();
		}

		return baseComponentType;
	}

	/**
	 * Checks if the class is allowed to be persisted and return the persisted class id, if applicable
	 * 
	 * @param clazz
	 * @return the classId (if it is not a persisted class, it will return -1 if it is a GDX/JAVA/libraryPersistedClass)
	 */
	static int checkPersistedClassValidity(Class<?> clazz)
	{
		// if array class, just check the base
		if (clazz.isArray())
		{
			clazz = LttlObjectGraphCrawler.getBaseComponentType(clazz);
		}

		// check if primitive
		if (isPrimative(clazz)) { return -1; }

		boolean isLibraryClass = isLibraryPersistedClass(clazz);
		if (isLibraryClass) return -1;

		Persist ann = getPersistClassAnnotation(clazz);

		if (ann == null && !isLibraryClass)
		{
			Lttl.Throw("Trying to persist an object whose class "
					+ clazz.getSimpleName()
					+ " does not have the Persist annotation and is not a library class to be persisted.");
		}

		if (!isLibraryClass
				&& ann != null
				&& LttlGameStarter.get().getClassMap().get(ann.value(), null) != clazz)
		{
			Lttl.Throw("No entry was found in the ClassMap for this persisted class "
					+ clazz.getSimpleName()
					+ " with a persist id of "
					+ ann.value());
		}

		if (clazz.getEnclosingClass() != null)
		{
			Lttl.Throw("Can't persist inner class " + clazz.getSimpleName());
		}

		return ann.value();
	}

	/**
	 * Retrieves the desired Method (may be inaccessible), by looking in current class and super classes
	 * 
	 * @param clazz
	 * @param methodName
	 * @param paramClasses
	 *            the classes to be in param in order, if null, doesn't check parameters, just gets first by name
	 * @return will return null if not found
	 */
	public static Method getMethodAnywhere(Class<?> clazz, String methodName,
			Class<?>... paramClasses)
	{
		// get method
		Method m = null;
		Class<?> currentClass = clazz;

		// loop through all classes/supers until all the way down to Object class
		while (currentClass != Object.class)
		{
			try
			{
				if (paramClasses == null)
				{
					m = currentClass.getDeclaredMethod(methodName);
				}
				else
				{
					m = currentClass
							.getDeclaredMethod(methodName, paramClasses);
				}

				// if found break out
				if (m != null)
				{
					break;
				}
			}
			catch (NoSuchMethodException e1)
			{
				// ignore this exception
			}
			catch (SecurityException e1)
			{
				e1.printStackTrace();
			}
			// go to next super class
			currentClass = currentClass.getSuperclass();
		}

		return m;
	}

	public static ProcessedFieldType getField(Class<?> clazz, String fieldName)
	{
		for (ProcessedFieldType p : LttlObjectGraphCrawler.getAllFields(clazz,
				FieldsMode.All))
		{
			if (p.getField().getName().equals(fieldName)) { return p; }
		}
		return null;
	}

	/**
	 * Gets all the methods on this class and super classes<br>
	 * Note: this will return overriden and super methods. overriden one
	 * 
	 * @param clazz
	 * @return
	 */
	public static ArrayList<Method> getAllMethods(Class<?> clazz)
	{
		// get methods
		ArrayList<Method> list = new ArrayList<Method>();
		Class<?> currentClass = clazz;

		// loop through all classes/supers until all the way down to Object class
		while (currentClass != Object.class)
		{
			LttlHelper.addArrayToList(list, currentClass.getDeclaredMethods());

			// go to next super class
			currentClass = currentClass.getSuperclass();
		}

		return list;
	}

	public static Persist getPersistClassAnnotation(Class<?> clazz)
	{
		Persist persist = persisClasstMap.get(clazz, null);
		if (persist == null)
		{
			persist = clazz.getAnnotation(Persist.class);
			persisClasstMap.put(clazz, persist);
		}

		return persist;
	}

	public static Persist getPersistFieldAnnotation(Field field)
	{
		Persist persist = persisFieldtMap.get(field, null);
		if (persist == null)
		{
			persist = field.getAnnotation(Persist.class);
			persisFieldtMap.put(field, persist);
		}

		return persist;
	}

	/**
	 * Gets the processedClass object for the field on a clazz (any any super classes) based on it's name or id
	 * 
	 * @param nameOrId
	 * @param clazz
	 * @param paramPfts
	 *            all the param types (procesedfieldtypes) for clazz, if any
	 * @return the ProcessedClass obejct of field if found, or null if not found
	 */
	public static ProcessedFieldType getFieldByNameOrId(String nameOrId,
			Class<?> clazz, ProcessedFieldType... paramPfts)
	{
		// check if the field is using an id
		int fieldId = -1;
		try
		{
			fieldId = Integer.parseInt(nameOrId);
		}
		catch (NumberFormatException e)
		{
		}

		ProcessedFieldType foundPft = null;
		for (ProcessedFieldType pft : getAllFields(clazz, FieldsMode.Persisted,
				paramPfts))
		{
			// if it is using an id, then iterate through all the declared fields for the current class and check
			// their annotations
			if (fieldId != -1)
			{
				Persist annotation = getPersistFieldAnnotation(pft.getField());
				if (annotation != null && fieldId == annotation.value())
				{
					foundPft = pft;
					break;
				}
			}
			else if (pft.getField().getName().equals(nameOrId))
			{
				foundPft = pft;
				break;
			}
		}

		// log it if it could never be found
		if (foundPft == null)
		{
			Lttl.logNote("Deserializing: Could not find field " + nameOrId
					+ " on class " + clazz.getName());
			return null;
		}
		else
		{
			return foundPft;
		}
	}

	/**
	 * Gets the class based on the id (mapped) or the name
	 * 
	 * @param nameOrId
	 * @return found class, should NEVER be null, will always do a throw
	 * @throws RuntimeException
	 *             if the class cannot be found via Id or name<br>
	 *             We always throw when a class cannot be found since if it does not exist anymore, it should have been
	 *             fully removed from the game and a json generated without any instances of it
	 */
	static Class<?> getClassByNameOrId(String nameOrId)
	{
		Class<?> resultClass = null;
		try
		{
			// try and parse it into an int, if you can then get the class from map
			int classId = Integer.parseInt(nameOrId);
			resultClass = LttlGameStarter.get().getClassMap().get(classId);
			if (resultClass == null)
			{
				Lttl.Throw("Deserialize: Could not find the mapped class with id "
						+ classId
						+ " Be sure all class components and objects are removed from the game before deleting a class.");
			}
		}
		catch (NumberFormatException e)
		{
			try
			{
				resultClass = Class.forName(nameOrId);
			}
			catch (ClassNotFoundException e1)
			{
				Lttl.logNote("Deserialize: Class specified in JSON ["
						+ nameOrId
						+ "] could not be found.  Be sure all class components and objects are removed from the game before deleting a class.");
			}
		}

		return resultClass;
	}

	/**
	 * Returns all the param types of a super class from a subclass perspective, if any.
	 * 
	 * @param subClassparamTypesProcessed
	 *            This is the subClass's param types, if it has any
	 * @param subClass
	 *            the sub class whose super you want to get the param types of
	 * @return super's param types, empty array if none
	 */
	private static ProcessedFieldType[] getParamTypesOfSuperClass(
			Class<?> subClass,
			ProcessedFieldType... subClassparamTypesProcessed)
	{
		// creates a ProcessedFieldType for the super class, so the currentClass for it will the superclass, we are
		// checking to see if it has any parameters
		ProcessedFieldType pft = new ProcessedFieldType(
				subClass.getGenericSuperclass(), subClassparamTypesProcessed);

		return pft.getParams();
	}

	/**
	 * Returns the base component type of the arrayClass, necessary if it is multidemensional
	 * 
	 * @param arrayClass
	 * @return
	 */
	public static Class<?> getComponentTypeRoot(Class<?> arrayClass)
	{
		if (!arrayClass.isArray()) return null;

		// iterate through the component type to see how many dimensions there are
		Class<?> current = arrayClass;
		while ((current = current.getComponentType()).isArray())
		{
		}
		return current;
	}

	public static Class<?> getArrayClass(Class<?> arrayClass, int dimensions)
	{
		return Array.newInstance(arrayClass, new int[dimensions]).getClass();
	}

	/**
	 * Returns a new instance of the clazz specified by using 'new' statement or looking for a public or private
	 * constructor with no parameters.<br>
	 * Includes boxed primatives workaround by giving them a default value of 0.<br>
	 * Does not work for arrays.
	 * 
	 * @param clazz
	 * @return a non null object
	 * @throws if
	 *             can't create an object
	 */
	public static <T> T newInstance(Class<T> clazz)
	{
		T object = null;

		// try newInstance method, may not exist with no parameters or may not be accessible or may be primative, but
		// this often works, and is quick, just as fast as call newInstance() withot this helper method
		try
		{
			object = clazz.newInstance();
		}
		catch (InstantiationException e1)
		{
			// no new Instance with no params or it could be primative

			// primative case
			if (isPrimative(clazz))
			{
				// Don't need to check for String, because it has an empty constructor and would have been created above

				// only need to check for Boxed primatives
				// 0 is default
				if (clazz == Integer.class)
				{
					object = (T) new Integer(0);
				}
				else if (clazz == Long.class)
				{
					object = (T) new Long(0);
				}
				else if (clazz == Short.class)
				{
					object = (T) new Short((short) 0);
				}
				else if (clazz == Byte.class)
				{
					object = (T) new Byte((byte) 0);
				}
				else if (clazz == Float.class)
				{
					object = (T) new Float(0);
				}
				else if (clazz == Double.class)
				{
					object = (T) new Double(0);
				}

				if (object == null)
				{
					// this should never happen, but just in case something weird happens
					Lttl.Throw("Could not create new instance of boxed primative class "
							+ clazz.getName());
				}
				return object;
			}
		}
		catch (IllegalAccessException e1)
		{
			// incaccesible case
			try
			{
				Constructor<T> constr = clazz.getDeclaredConstructor();
				boolean accessible = constr.isAccessible();
				if (!accessible)
				{
					constr.setAccessible(true);
				}
				object = constr.newInstance();
				if (!accessible)
				{
					constr.setAccessible(false);
				}
			}
			catch (NoSuchMethodException | SecurityException
					| InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException e)
			{
				// shouldn't happen because it's in IllegalAccessException, which means it found the constructor, just
				// can't access it
				e.printStackTrace();
			}
		}
		if (object != null) return object;

		Lttl.Throw("Could not find a constructor with no parameters for class "
				+ clazz.getName());

		return object;
	}

	/**
	 * Checks if the specified class can generate a 'new instance' via the newInstance() (any accessible level
	 * constructors with no params).
	 * 
	 * @param clazz
	 * @return
	 */
	public static boolean canNewInstance(Class<?> clazz)
	{
		Constructor<?> constr = null;
		try
		{
			constr = clazz.getDeclaredConstructor();
		}
		catch (NoSuchMethodException | SecurityException e)
		{
		}
		return constr != null;
	}

	/**
	 * checks if teh given field has anything to do with the class. Does the field have that class as the field type or
	 * is any of it's parameters have that field type, is it an array with that class as teh component type, also checks
	 * subclass
	 * 
	 * @param pft
	 * @param clazz
	 * @return
	 */
	public static boolean hasAnythingToDoWithClass(ProcessedFieldType pft,
			Class<?> clazz)
	{
		// check if lttlcomponent field
		if (clazz.isAssignableFrom(pft.getCurrentClass()))
		{
			return true;
		}
		// if array, check it's component type
		else if (pft.getCurrentClass().isArray())
		{
			if (clazz.isAssignableFrom(getComponentTypeRoot(pft
					.getCurrentClass()))) { return true; }
		}
		// if it has parameters, check if any of them have anything to do with this class
		else if (pft.getParamCount() > 0)
		{
			for (ProcessedFieldType child : pft.getParams())
			{
				if (hasAnythingToDoWithClass(child, clazz)) { return true; }
			}
		}

		// guess not
		return false;
	}

	static boolean isComponentIdField(Field field)
	{
		Persist p = LttlObjectGraphCrawler.getPersistFieldAnnotation(field);
		return p != null && p.value() == 90701;
	}

	static boolean isFieldComponentMap(Field field)
	{
		Persist p = LttlObjectGraphCrawler.getPersistFieldAnnotation(field);
		return p != null && p.value() == 904403;
	}
}
