package com.lttlgames.editor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import com.lttlgames.editor.LttlObjectGraphCrawler.FieldsMode;
import com.lttlgames.helpers.LttlMutatableInt;
import com.lttlgames.helpers.LttlProfiler;

/**
 * Stores a bunch of static methods and properites to help handle the life of a component.
 * 
 * @author Josh
 */
final class ComponentHelper
{
	static final Field componentIdField = LttlObjectGraphCrawler
			.getFieldByNameOrId(90701 + "", LttlComponent.class).getField();
	static final Field sceneIdField = LttlObjectGraphCrawler
			.getFieldByNameOrId(902006 + "", LttlTransform.class).getField();
	static final Field transformField = LttlObjectGraphCrawler
			.getFieldByNameOrId(90703 + "", LttlComponent.class).getField();

	static ArrayList<LttlComponent> componentsToHardDestroyImmediately = new ArrayList<LttlComponent>();

	/**
	 * Sets the unique id for the component using refractoring
	 * 
	 * @param component
	 */
	static private void setComponentId(LttlComponent component)
	{
		// Set component's unique id via reflector
		try
		{
			componentIdField.setAccessible(true);
			componentIdField.setInt(component, Lttl.game.getWorldCore()
					.nextComponentId());
			componentIdField.setAccessible(false);
		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
		}
		catch (SecurityException e1)
		{
			e1.printStackTrace();
		}
	}

	/**
	 * Sets the transform reference field for the component using refractoring
	 * 
	 * @param component
	 */
	static private void setTransform(LttlComponent component,
			LttlTransform transform)
	{
		try
		{
			transformField.setAccessible(true);
			transformField.set(component, transform);
			transformField.setAccessible(false);
		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
		}
		catch (SecurityException e1)
		{
			e1.printStackTrace();
		}
	}

	/**
	 * Sets the scene id on the LttlTransform using refractoring.
	 * 
	 * @param lt
	 * @param sceneId
	 */
	static private void setSceneId(LttlTransform lt, int sceneId)
	{
		try
		{
			sceneIdField.setAccessible(true);
			sceneIdField.setInt(lt, sceneId);
			sceneIdField.setAccessible(false);
		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
		}
		catch (SecurityException e1)
		{
			e1.printStackTrace();
		}
	}

	/**
	 * Crawls through a LttlComponent and when finds another LttlComponent it uses it's ID to find it in the scene (if
	 * not searches other scenes) and saves the reference.<br>
	 * Should be used on JSON load of a scene if not using component refs list, whenever a component with component
	 * references is being copied to a new scene.
	 * 
	 * @param comp
	 *            the LttlComponent to search and update references in
	 * @param ls
	 *            the LttlSceneCore that this object is in, can be null if you don't know
	 */
	static void updateReferencesFromId(LttlComponent comp,
			final LttlSceneCore ls)
	{
		new LttlComponentCrawler()
		{
			// can assume each object given in parameters is a (secondary) LttlComponent with just an ID property that
			// is accurate
			public Object set(Object o)
			{
				return getComponentReference(((LttlComponent) o).getId(), ls);
			}

			public Object setHashMapEntry(Object key, Object value)
			{
				return getComponentReference(((LttlComponent) value).getId(),
						ls);
			}
		}.crawl(comp, FieldsMode.Persisted, 1);
	}

	/**
	 * Crawls through non LttlComponent object and when finds a LttlComponent object it uses it's ID to find it in the
	 * scene and saves the reference.
	 * 
	 * @param object
	 *            the object to search and update references in
	 */
	static void updateReferencesFromId(Object object)
	{
		if (LttlComponent.class.isAssignableFrom(object.getClass()))
		{
			updateReferencesFromId((LttlComponent) object, null);
			return;
		}

		new LttlComponentCrawler()
		{
			public Object set(Object o)
			{
				return getComponentReference(((LttlComponent) o).getId(), null);
			}

			public Object setHashMapEntry(Object key, Object value)
			{
				return getComponentReference(((LttlComponent) value).getId(),
						null);
			}
		}.crawl(object, FieldsMode.Persisted, 0);
	}

	/**
	 * Updates an entire tree's (transform, children, and components, recursively) component references to match the
	 * source of the copy. This is ran after addTransformCopy() so if there were references of components inside the
	 * tree it will maintain them even though all the components are new.
	 * 
	 * @param lt
	 * @param map
	 */
	static void matchReferencesCopyTree(LttlTransform lt,
			final HashMap<LttlComponent, LttlComponent> map)
	{
		// self, I don't think this is necessary to do on the transform
		// updateReferencesCopy(lt, map);

		// components
		for (LttlComponent lc : lt.components)
		{
			updateReferencesCopy(lc, map);
		}

		// children
		for (LttlTransform ltc : lt.children)
		{
			matchReferencesCopyTree(ltc, map);
		}
	}

	/**
	 * Updates a components references to match the source of the copy. This is ran after addTransformCopy() so if there
	 * were references of components inside the tree it will maintain them even though all the components are new.<br>
	 * It also removes non safe references (in other scenes), so removeNonSafeReferenes() does not need to run after
	 * this.
	 * 
	 * @param lc
	 * @param map
	 *            keys are source components and the values are the new components
	 */
	static void updateReferencesCopy(final LttlComponent lc,
			final HashMap<LttlComponent, LttlComponent> map)
	{
		new LttlComponentCrawler()
		{
			// can assume each object given in parameters is a (secondary) LttlComponent
			public Object set(Object o)
			{
				if (map.containsKey(o)) { return map.get(o); }
				if (!lc.isSafeReference((LttlComponent) o)) { return null; }
				return o;
			}

			public Object setHashMapEntry(Object key, Object value)
			{
				if (map.containsKey(value)) { return map.get(value); }
				if (!lc.isSafeReference((LttlComponent) value)) { return null; }
				return value;
			}
		}.crawl(lc, FieldsMode.Copy, 1);
	}

	/**
	 * Crawls through the component and looks for any references to components that are not in the scene specified or
	 * the world and nulls them (follows annotation rules, since non persisted fields would have been lost in the copy).
	 * This does not link any componentID to the real references. This is strictly cleanup.<br>
	 * Runs when a component is copied possibly from another scene.
	 * 
	 * @param component
	 */
	static void removeNonSafeSceneReferences(final LttlComponent component)
	{
		removeNonSafeSceneReferences(component, FieldsMode.AllButIgnore);
	}

	/**
	 * Crawls through the component and looks for any references to components that are not in the scene specified or
	 * the world and nulls them (follows annotation rules, since non persisted fields would have been lost in the copy).
	 * This does not link any componentID to the real references. This is strictly cleanup.<br>
	 * Runs when a component is copied possibly from another scene.
	 * 
	 * @param component
	 * @param fieldsMode
	 */
	static void removeNonSafeSceneReferences(final LttlComponent component,
			FieldsMode fieldsMode)
	{
		new LttlComponentCrawler()
		{
			// can assume each object given in parameters is a (secondary) LttlComponent with just an ID property that
			// is accurate
			public Object set(Object o)
			{
				if (!component.isSafeReference((LttlComponent) o)) { return null; }
				return o;
			}

			public Object setHashMapEntry(Object key, Object value)
			{
				if (!component.isSafeReference((LttlComponent) value)) { return null; }
				return value;
			}

		}.crawl(component, fieldsMode, 1);
	}

	/**
	 * Crawls through the transform tree (children and components) and looks for any references to components that are
	 * not in the scene specified or the world and nulls them (follows annotation rules, since non persisted fields
	 * would have been lost in the copy). This does not link any componentID to the real references. This is strictly
	 * cleanup.<br>
	 * Runs when a component is copied possibly from another scene.
	 * 
	 * @param lt
	 */
	static void removeNonSafeSceneReferencesTree(LttlTransform lt)
	{
		removeNonSafeSceneReferencesTree(lt, FieldsMode.AllButIgnore);
	}

	/**
	 * Crawls through the transform tree (children and components) and looks for any references to components that are
	 * not in the scene specified or the world and nulls them (follows annotation rules, since non persisted fields
	 * would have been lost in the copy). This does not link any componentID to the real references. This is strictly
	 * cleanup.<br>
	 * Runs when a component is copied possibly from another scene.
	 * 
	 * @param lt
	 * @param fieldsMode
	 */
	static void removeNonSafeSceneReferencesTree(LttlTransform lt,
			FieldsMode fieldsMode)
	{
		// self
		removeNonSafeSceneReferences(lt, fieldsMode);

		// components
		for (LttlComponent lc : lt.components)
		{
			removeNonSafeSceneReferences(lc, fieldsMode);
		}

		// children
		for (LttlTransform ltc : lt.children)
		{
			removeNonSafeSceneReferencesTree(ltc, fieldsMode);
		}
	}

	/**
	 * Looks inside the scene specified and the world for a component id. If not found, searches all other loaded
	 * scenes.
	 * 
	 * @param compId
	 * @param scene
	 *            can be null if you don't know
	 * @return
	 */
	static LttlComponent getComponentReference(int compId, LttlSceneCore scene)
	{
		// try to find the LttlComponent in the scene's comp map
		LttlComponent ro = null;
		if (scene != null)
		{
			ro = scene.componentMap.get(compId);
		}

		// if didn't find it and this scene is not the world or the scene was not specified, then check inside the world
		if (ro == null
				&& (scene == null || scene.getId() != Lttl.scenes.WORLD_ID))
		{
			ro = Lttl.game.getWorldCore().componentMap.get(compId);
		}

		// search all other scenes
		if (ro == null)
		{
			// skip world scene
			for (LttlScene iScene : Lttl.scenes.getAllLoaded(false))
			{
				// skip scene already checked, if scene was specified
				if (scene != null && iScene.getId() == scene.getId())
				{
					continue;
				}

				// if found, then break
				if ((ro = iScene.getRef().componentMap.get(compId)) != null)
				{
					break;
				}
			}
		}

		if (ro == null)
		{
			Lttl.logNote("Loading Scene "
					+ ((scene == null) ? "UNSPECIFIED" : scene.getName() + " ["
							+ scene.getId() + "]")
					+ ": No component found for id: "
					+ compId
					+ " in this scene or any loaded scenes.  Setting to null.  To fix this, load dependent scene first.");
		}

		// return LttlComponent reference or null and send silent error
		return ro;
	}

	/**
	 * This sets up an entire transform hiearchy tree by updating the parent down to the lowest child and adding all of
	 * them to the loopmanager. NOTE: transform should already be children or parents by now, and if no parent, it
	 * should have been added to top level transform hiearchy already.<br>
	 * <br>
	 * This should be ran whenever a LttlTransform is added to a scene (alway without a parent first), or when top level
	 * transforms are loaded with a scene via JSON, or when a LttlTransform Tree is copied (either same or another
	 * scene)
	 * 
	 * @param transform
	 *            the highest level transform (should not have a parent)
	 */
	static void initialPrepTree(LttlTransform transform)
	{
		// it's key to update parents first since it will give the children accurate world transforms
		transform.updateTransforms(true);

		// add to z ordered list
		Lttl.loop.updateTransformZindex(transform);

		// update alpha
		if (transform.renderer() != null)
		{
			transform.renderer().updateAlpha(true);
		}

		// now update children
		for (LttlTransform child : transform.children)
		{
			initialPrepTree(child);
		}
	}

	/**
	 * Callback on all scenes and all the transform trees.
	 * 
	 * @param methodType
	 */
	static void callBackAllScenes(ComponentCallBackType methodType)
	{
		for (LttlScene scene : Lttl.scenes.getAllLoaded(true))
		{
			callBackScene(scene, methodType);
		}
	}

	/**
	 * Callback on specified scene and all of it's tranfsform trees.
	 * 
	 * @param scene
	 * @param methodType
	 */
	static void callBackScene(LttlScene scene, ComponentCallBackType methodType)
	{
		for (LttlTransform lt : scene.getTopLevelTransforms())
		{
			callBackTransformTree(lt, methodType);
		}
	}

	/**
	 * Callback on transform, then all of it's components, and then the same for it's children (in that order).
	 * 
	 * @param lt
	 * @param methodType
	 * @return if component passed the enabledCheck and the destroyPendingCheck. if true, callback may not have ran if
	 *         game was paused
	 */
	static boolean callBackTransformTree(LttlTransform lt,
			ComponentCallBackType methodType)
	{
		// process callback for transform
		boolean transformPassCheck = processCallBack(lt, methodType);

		// if the LttlTransform did not callback, because it is not enabled and this callback requires it to be enabled,
		// then all of it's children and components will not be enabled either, so skip. If this transform failed
		// because it is checking destroy pending and it is destroy pending, then all of it's children and components
		// will also be destroy pending, so skip.
		if (!transformPassCheck) { return false; }

		// callback all it's components too
		for (LttlComponent lc : lt.components)
		{
			processCallBack(lc, methodType);
		}

		// process callbacks for all children transforms too
		for (LttlTransform ltc : lt.children)
		{
			callBackTransformTree(ltc, methodType);
		}

		return true;
	}

	/**
	 * Processes any callback and takes into consideraion if in editor or not and if paused. Checks if destroy pending
	 * (optional) and if the callback type requires the component to be enabled
	 * {@link ComponentCallBackType#isCheckEnabled()}.
	 * 
	 * @param lc
	 *            component
	 * @param methodType
	 * @return if component passed the enabledCheck and the destroyPendingCheck. if true, callback may not have ran if
	 *         game was paused
	 */
	static boolean processCallBack(LttlComponent lc,
			ComponentCallBackType methodType)
	{
		/**
		 * The checks need to be done here, so even when processCallBack is called not from processCallBackOnTree, it
		 * will still be accurate
		 */

		// skip callback if needs to be enabled and is not
		if (methodType.isCheckEnabled() && !lc.isEnabled()) return false;

		// skip callback if not allowed to be destroy pending
		if (methodType.isCheckDestroyPending() && lc.isDestroyPending())
			return false;

		// debug draw callback if in editor regardless if paused
		if (Lttl.game.inEditor()
				&& methodType == ComponentCallBackType.DebugDraw)
		{
			lc.debugDraw();
		}

		// do not do callback if paused and transform is not unPauseable
		if (Lttl.game.isPaused() && !lc.transform().unPauseable) { return true; }

		switch (methodType)
		{
			case onStart:
				if (!Lttl.game.isPlaying())
				{
					lc.onEditorStart();
				}
				else
				{
					lc.onStart();
				}
				break;
			case OnEarlyUpdate:
				if (!Lttl.game.isPlaying())
				{
					lc.onEditorEarlyUpdate();
				}
				else
				{
					lc.onEarlyUpdate();
				}
				break;
			case onFixedUpdate:
				if (!Lttl.game.isPlaying())
				{
					lc.onEditorFixedUpdate();
				}
				else
				{
					lc.onFixedUpdate();
				}
				break;
			case onLateFixedUpdate:
				if (!Lttl.game.isPlaying())
				{
					lc.onEditorLateFixedUpdate();
				}
				else
				{
					lc.onLateFixedUpdate();
				}
				break;
			case onUpdate:
				if (!Lttl.game.isPlaying())
				{
					lc.onEditorUpdate();
				}
				else
				{
					lc.onUpdate();
				}
				if (lc.getClass() == LttlTransform.class)
				{
					LttlProfiler.enabledTransforms.add();
				}
				LttlProfiler.enabledComponents.add();
				break;
			case onLateUpdate:
				if (!Lttl.game.isPlaying())
				{
					lc.onEditorLateUpdate();
				}
				else
				{
					lc.onLateUpdate();
				}
				break;
			case onDestroy:
				if (!Lttl.game.isPlaying())
				{
					lc.onEditorDestroyComp();
				}
				else
				{
					lc.onDestroyComp();
				}
				break;
			case onDisable:
				if (!Lttl.game.isPlaying())
				{
					lc.onEditorDisable();
				}
				else
				{
					lc.onDisable();
				}
				break;
			case onEnable:
				if (!Lttl.game.isPlaying())
				{
					lc.onEditorEnable();
				}
				else
				{
					lc.onEnable();
				}
				break;
			case onResize:
				if (!Lttl.game.isPlaying())
				{
					lc.onEditorResize();
				}
				else
				{
					lc.onResize();
				}
				break;
			case onSaveScene:
				lc.onSaveScene();
				break;
			case onEditorCreate:
				lc.onEditorCreate();
				break;
			case DebugDraw:
				break;
		}
		lc.hasRanBit |= methodType.getValue();

		return true;
	}

	/**
	 * Sets the transform property, adds component to transform's component list (only if new component is not
	 * LttlTransform), sets the unique componentID, set's any component specific references, and adds to scene's
	 * component map
	 * 
	 * @param component
	 * @param transform
	 * @param scene
	 */
	static void setupNewComponent(LttlComponent component,
			LttlTransform transform, LttlSceneCore scene)
	{
		setTransform(component, transform);
		setComponentId(component);
		setComponentReferences(component, transform);

		// add to transform's component list
		if (component.getClass() != LttlTransform.class)
		{
			transform.components.add(component);
		}

		// add to scene's componentMap
		scene.componentMap.put(component.getId(), component);
	}

	/**
	 * removes specific component references, called when component is destroyed<br>
	 * OPTIMIZE Not sure why this is necessary since it's being destroyed and references will be lost anyways, just
	 * proper I guess
	 * 
	 * @param component
	 */
	static void removeStandardComponentReferences(LttlComponent component)
	{
		Class<?> compClass = component.getClass();

		if (compClass == LttlTransform.class) return;
		if (LttlRenderer.class.isAssignableFrom(compClass))
		{
			if (component.transform() != null)
			{
				component.transform().renderer = null;
				return;
			}
		}
		if (LttlMeshGenerator.class.isAssignableFrom(compClass))
		{
			if (component.transform() != null
					&& component.transform().renderer != null)
			{
				component.transform().renderer.meshGenerator = null;
				return;
			}
		}
	}

	/**
	 * Sets specific component references, called on component creation
	 * 
	 * @param component
	 * @param transform
	 */
	static void setComponentReferences(LttlComponent component,
			LttlTransform transform)
	{
		Class<?> compClass = component.getClass();

		if (compClass == LttlTransform.class) return;
		if (LttlRenderer.class.isAssignableFrom(compClass))
		{
			if (transform != null)
			{
				transform.renderer = (LttlRenderer) component;
				return;
			}
		}
		if (LttlMeshGenerator.class.isAssignableFrom(compClass))
		{
			if (transform != null && transform.renderer != null)
			{
				transform.renderer.meshGenerator = (LttlMeshGenerator) component;
				return;
			}
		}
	}

	/**
	 * Clears a transforms children, components, parentTransform and renderer
	 * 
	 * @param transform
	 * @return
	 */
	static LttlTransform resetTransform(LttlTransform transform)
	{
		transform.children.clear();
		transform.components.clear();
		transform.parentTransform = null;
		transform.renderer = null;

		return transform;
	}

	/**
	 * Sets the transform's sceneID, adds to scene's top-level transform hiearchy
	 * 
	 * @param transform
	 * @param scene
	 */
	static void addTransformToScene(LttlTransform transform, LttlSceneCore scene)
	{
		setSceneId(transform, scene.getId());

		// add to top level transform hierarchy
		transform.parentTransform = null;
		scene.transformHiearchy.add(transform);

		if (Lttl.game.inEditor())
		{
			Lttl.editor.getGui().getSelectionController()
					.addTransform(transform, scene);
		}
	}

	/**
	 * Removes all references for all the component in the entire game.<br>
	 * Searches through all scenes and their components and in all their fields (ignoring annotations) and when it finds
	 * one of the specified components it sets it's value to null. If it finds a key with the component in a hashmap, it
	 * deletes the whole entry, and if it finds just the value in a hashmap, it sets it to null too. <br>
	 * <b>WARNING: this is probably very slow and has the chance of getting trapped recursively.</b>
	 * 
	 * @param components
	 */
	static void removeComponentReferencesGlobal(LttlComponent component)
	{
		ArrayList<LttlComponent> components = new ArrayList<LttlComponent>();
		components.add(component);
		removeComponentReferencesGlobal(components);
	}

	/**
	 * Removes all references for all the provided components in the entire game.<br>
	 * Searches through all scenes and their components and in all their fields and when it finds one of the specified
	 * components it sets it's value to null. If it finds a key with the component in a hashmap, it deletes the whole
	 * entry, and if it finds just the value in a hashmap, it sets it to null. <br>
	 * <b>WARNING: this is probably very slow and has the chance of getting trapped recursively.</b>
	 * 
	 * @param components
	 */
	static void removeComponentReferencesGlobal(
			final ArrayList<LttlComponent> components)
	{
		new LttlComponentCrawler()
		{
			public Object set(Object o)
			{
				if (components.contains(o))
				{
					Lttl.logNote("Hard Destroy: Found reference of "
							+ ((LttlComponent) o).toString() + " on "
							+ getCurrentComponent().toString()
							+ " and nulled it.");
					return null;
				}
				return o;
			}

			public Object setHashMapEntry(Object key, Object value)
			{
				if (components.contains(key))
				{
					Lttl.logNote("Hard Destroy: Found reference of "
							+ ((LttlComponent) value).toString()
							+ " as key in hashmap on "
							+ getCurrentComponent().toString()
							+ " and remove whole entry.");
					return "delete";
				}
				else if (components.contains(value))
				{
					// only value matches, so keep entry but set value to null
					Lttl.logNote("Hard Destroy: Found reference of "
							+ ((LttlComponent) value).toString()
							+ " as value in hashmap on "
							+ getCurrentComponent().toString()
							+ " and nulled out the value.");
					return null;
				}
				return value;
			}
		}.crawl(Lttl.scenes.findAllComponents(), FieldsMode.AllButIgnore, 1);
	}

	/**
	 * Crawls through all of the specified scene's components looking for any property (field) with theClass
	 * 
	 * @param scene
	 *            haystack
	 * @param theClass
	 *            needle
	 * @param getExtends
	 *            should include classes that extend theClass
	 * @param deep
	 *            should it search for theClass inside other objects, or just stop at the first. It will search arrays
	 *            until it gets to an object, and if deep is false, it wills till return that object, but not go
	 *            further.
	 * @param fieldsMode
	 * @return an array of the specified objects with no duplicates.
	 */
	static <T> ArrayList<T> getScenesComponentProperties(LttlSceneCore scene,
			final Class<T> theClass, final boolean getExtends, boolean deep,
			FieldsMode fieldsMode)
	{
		ArrayList<T> sharedList = new ArrayList<T>();
		for (Iterator<Entry<Integer, LttlComponent>> it = scene.componentMap
				.entrySet().iterator(); it.hasNext();)
		{
			Entry<Integer, LttlComponent> pair = it.next();
			sharedList = getComponentProperties(pair.getValue(), theClass,
					getExtends, deep, fieldsMode, sharedList);
		}

		return sharedList;
	}

	/**
	 * Crawls through the component looking for any property (field) with theClass
	 * 
	 * @param component
	 *            haystack
	 * @param theClass
	 *            needle
	 * @param getExtends
	 *            should include classes that extend theClass
	 * @param deep
	 *            should it search for theClass inside other objects, or just stop at the first. It will search arrays
	 *            until it gets to an object, and if deep is false, it wills till return that object, but not go
	 *            further.
	 * @param fieldsMode
	 * @return an array of the specified objects with no duplicates.
	 */
	static <T> ArrayList<T> getComponentProperties(LttlComponent component,
			final Class<T> theClass, final boolean getExtends, boolean deep,
			FieldsMode fieldsMode)
	{
		return getComponentProperties(component, theClass, getExtends, deep,
				fieldsMode, null);
	}

	private static <T> ArrayList<T> getComponentProperties(
			LttlComponent component, final Class<T> theClass,
			final boolean getExtends, boolean deep, FieldsMode fieldsMode,
			ArrayList<T> sharedList)
	{
		final ArrayList<T> list = (sharedList == null) ? new ArrayList<T>()
				: sharedList;

		LttlComponentPropertyCrawler cpc = new LttlComponentPropertyCrawler()
		{
			public Object set(Object o)
			{
				return checkObject(o);
			}

			public Object setHashMapEntry(Object key, Object value)
			{
				checkObject(key);
				return checkObject(value);
			}

			private Object checkObject(Object o)
			{
				if (o.getClass() == theClass
						|| (getExtends && theClass.isAssignableFrom(o
								.getClass())))
				{
					T cO = (T) o;
					if (!list.contains(cO))
					{
						list.add((T) cO);
					}
				}
				return o;
			}
		};
		cpc.crawl(component, fieldsMode, deep);
		return list;
	}

	/**
	 * Returns the number of references of needleComponents in the components fields in the specified scenes.<br>
	 * Skips all LttlTransforms because they can't hold references to components, and because want to avoid checking
	 * children and component lists
	 * 
	 * @param needleComponents
	 *            will not crawl any of these components, even if they are in a scene to be crawled
	 * @param scenes
	 * @param fieldsMode
	 * @return the number of references, 0 is none
	 */
	public static int checkDependencies(
			final ArrayList<LttlComponent> needleComponents,
			final ArrayList<LttlScene> scenes, FieldsMode fieldsMode)
	{
		ArrayList<LttlComponent> haystackComponents = new ArrayList<LttlComponent>();
		for (LttlScene scene : scenes)
		{
			for (LttlComponent comp : scene.getRef().componentMap.values())
			{
				haystackComponents.add(comp);
			}
		}

		// don't crawl the needle components
		for (LttlComponent comp : needleComponents)
		{
			haystackComponents.remove(comp);
		}

		final LttlMutatableInt dependencyCount = new LttlMutatableInt();
		new LttlComponentCrawler()
		{
			private void dependencyAlert(Object o)
			{
				Lttl.logNote("Dependency Found: on "
						+ getCurrentComponent().t().getName() + "["
						+ getCurrentComponent().getClass().getSimpleName()
						+ "] of " + ((LttlComponent) o).t().getName() + "["
						+ o.getClass().getSimpleName() + "]");
			}

			public Object set(Object o)
			{
				if (needleComponents.contains(o))
				{
					dependencyAlert(o);
					dependencyCount.value++;
				}
				return o;
			}
		}.crawl(haystackComponents, fieldsMode, 1);

		return dependencyCount.value;
	}
}
