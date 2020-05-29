package com.lttlgames.editor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.badlogic.gdx.math.Vector2;
import com.lttlgames.editor.LttlObjectGraphCrawler.FieldsMode;
import com.lttlgames.editor.annotations.IgnoreCrawl;
import com.lttlgames.helpers.LttlHelper;

/**
 * Very hand class since it allows a scene to be passed around like an object, since there is only one instance of it
 * with a reference to the real LttlSceneCore, the reference can easily be destroyed in one place and the scene is free
 * to be garbage dumped.
 * 
 * @author Josh
 */
@IgnoreCrawl
public final class LttlScene
{
	private LttlSceneCore ref;

	// prevent public creation
	LttlScene(LttlSceneCore sceneRefeference)
	{
		this.ref = sceneRefeference;
	}

	/**
	 * Returns the name of this scene
	 * 
	 * @return
	 */
	public String getName()
	{
		return ref.getName();
	}

	/**
	 * Sets the scene's name
	 * 
	 * @param name
	 * @return True if was successful
	 */
	public boolean setName(String name)
	{
		return ref.setName(name);
	}

	/**
	 * Returns the id of this scene.
	 * 
	 * @return
	 */
	public int getId()
	{
		return ref.getId();
	}

	// TODO make some huge scene that you can tell is in memory base on stats, and then unload it and see of memory is
	// reduced
	/**
	 * Queues the specified scene to have all of it's components destroyed and removed from game before next frame
	 * starts. If in editor and not playing, then it will temporary save the scene and save the world after it's
	 * unloaded.<br>
	 * <b>All functions that create transforms will be disabled and return null because scene is going to be
	 * destroyed.</b>
	 * 
	 * @param hard
	 *            should the components in this scene have their references destroyed hard
	 */
	public void unload(boolean hard)
	{
		unload(hard, false);
	}

	void unload(boolean hard, boolean isDeleting)
	{
		// scene is unloading protection
		if (getRef().isPendingUnload)
		{
			pendingUnloadError();
			return;
		}

		if (getRef() == Lttl.scenes.getWorldCore())
		{
			Lttl.Throw("Can't unload world scene.");
		}

		Lttl.logNote("Unloading Scene: getName() + [" + getId() + "]"
				+ ((hard) ? " [hard]" : ""));

		// remove this scene's undos (only if in editor)
		if (Lttl.game.inEditor())
		{
			// clear the selection if it consisted of any transform in that scene or that scene
			if (Lttl.editor.getGui().getSelectionController().isSceneSelected())
			{
				if (Lttl.editor.getGui().getSelectionController()
						.getSelectedScene() == this)
				{
					Lttl.editor.getGui().getSelectionController()
							.clearSelection();
				}
			}
			else
			{
				for (LttlTransform lt : Lttl.editor.getGui()
						.getSelectionController().getSelectedTransforms())
				{
					if (lt.getScene() == this)
					{
						Lttl.editor.getGui().getSelectionController()
								.clearSelection();
						break;
					}
				}
			}
			Lttl.editor.getUndoManager().removeScenesUndos(getId());
		}

		// no need to save temporary state if deleting because if you save it will be deleted, if you don't save, it
		// will just revert to original loaded in version of scene
		if (!isDeleting && Lttl.game.inEditor() && !Lttl.game.isPlaying())
		{
			// temporary save scene at the state it is before destroying all components or messing with anything that
			// has to do with the unload, this way if there is a save, we still keep the changes.
			Lttl.scenes.temporarySaveScene(getRef());
		}
		getRef().isPendingUnload = true;

		if (Lttl.game.inEditor())
		{
			Lttl.editor.getGui().getSelectionController().removeSceneTree(this);
		}

		// iterate through a COPY of the top level transforms and destroy them
		for (LttlTransform lt : new ArrayList<LttlTransform>(
				getRef().transformHiearchy))
		{
			lt.destroyComp(hard, false);
		}
	}

	/**
	 * Returns if the scene is pending to be unloaded next frame.
	 * 
	 * @return
	 */
	public boolean isPendingUnload()
	{
		return getRef().isPendingUnload;
	}

	LttlTransform addNewTransformNoCallback(String name)
	{
		// scene is unloading protection
		if (getRef().isPendingUnload)
		{
			pendingUnloadError();
			return null;
		}

		LttlTransform newTransform = addTransformShared(new LttlTransform());

		// set name
		newTransform.setName(name);

		// run initial calculations
		ComponentHelper.initialPrepTree(newTransform);

		return newTransform;
	}

	/**
	 * Adds a new transform to scene with name.
	 * 
	 * @param name
	 * @return the new transform
	 */
	public LttlTransform addNewTransform(String name)
	{
		LttlTransform newTransform = addNewTransformNoCallback(name);

		// process onstart callback
		ComponentHelper.callBackTransformTree(newTransform,
				ComponentCallBackType.onStart);

		return newTransform;
	}

	/**
	 * Adds a new transform to scene.
	 * 
	 * @return the new transform
	 */
	public LttlTransform addNewTransform()
	{
		return addNewTransform("");
	}

	private LttlTransform addTransformShared(LttlTransform transform)
	{
		// setup new component
		ComponentHelper.setupNewComponent(transform, transform, getRef());

		// add tranform to scene
		ComponentHelper.addTransformToScene(transform, getRef());

		return transform;
	}

	/**
	 * @param sourceTransform
	 * @param map
	 * @param tree
	 *            should this do the whole transform tree recursively
	 * @return
	 */
	private LttlTransform addTransformCopyShared(LttlTransform sourceTransform,
			HashMap<LttlComponent, LttlComponent> map, boolean tree)
	{
		Lttl.Throw(sourceTransform);

		// TRANSFORM
		// create a copy of source transform
		LttlTransform copyTransform = LttlCopier.copy(sourceTransform);
		Lttl.Throw(copyTransform);

		// clear some of copy transform's properties
		ComponentHelper.resetTransform(copyTransform);

		// add copy of transform to map
		if (map != null)
		{
			map.put(sourceTransform, copyTransform);
		}

		copyTransform = addTransformShared(copyTransform);

		// COMPONENTS
		// create a copy for source component list so can delete them as using, this way we can keep on iterating
		// through the list of components to copy til they are all copied, we have to skipover ones that require other
		// components, but we don't know which
		ArrayList<LttlComponent> compsToCopy = new ArrayList<LttlComponent>(
				sourceTransform.components);

		LttlSceneCore scene = copyTransform.getSceneCore();
		while (compsToCopy.size() > 0)
		{
			for (Iterator<LttlComponent> it = compsToCopy.iterator(); it
					.hasNext();)
			{
				LttlComponent lc = it.next();
				if (copyTransform.canAddComponentType(lc.getClass()))
				{
					// make a copy of component
					LttlComponent newComp = LttlCopier.copy(lc);
					Lttl.Throw(newComp);

					// setup and add to transform
					ComponentHelper.setupNewComponent(newComp, copyTransform,
							scene);

					// add component to map for referencing later
					if (map != null)
					{
						map.put(lc, newComp);
					}

					it.remove();
				}
				else
				{
					// if can't be added, then skip and add it next time around, probably has dependents
					continue;
				}
			}
		}

		// CHILDREN
		if (tree)
		{
			for (LttlTransform sourceChild : sourceTransform.children)
			{
				addTransformCopyShared(sourceChild, map, tree).setParent(
						copyTransform, false);
			}
		}

		return copyTransform;
	}

	/**
	 * Adds a copy of this transform not including any of the children to the scene specified. Copies components too.<br>
	 * Note: only copy adn persisted fields and does not match local tree references.
	 * 
	 * @param sourceTransform
	 * @return
	 */
	public LttlTransform addTransformSingleCopy(LttlTransform sourceTransform)
	{
		// scene is unloading protection
		if (getRef().isPendingUnload)
		{
			pendingUnloadError();
			return null;
		}

		return addTransformSingleCopy(sourceTransform, false);
	}

	/**
	 * Adds a copy of this transform not including any of the children to the scene specified. Copies components too.
	 * 
	 * @param sourceTransform
	 * @param matchLocalTreeReferences
	 *            if true, if there are references of components within the copying transform hierarchy, then it will
	 *            maintain those after the copy
	 * @return
	 */
	public LttlTransform addTransformSingleCopy(LttlTransform sourceTransform,
			boolean matchLocalTreeReferences)
	{
		// scene is unloading protection
		if (getRef().isPendingUnload)
		{
			pendingUnloadError();
			return null;
		}

		if (Lttl.game.inEditor())
		{
			// disable adding things to GUI just in case it's a big add
			Lttl.editor.getGui().disableGuiRefresh = true;
		}

		LttlTransform copyTransform = addTransformCopyShared(sourceTransform,
				null, matchLocalTreeReferences);

		// add '_copy' to new top level transform
		copyTransform.setName(sourceTransform.getName() + "_copy");

		// only remove possible non safe references if the source transform is not in the world scene and it's not
		// in
		// the same scene as destination (new) transform
		if (sourceTransform.getSceneId() != Lttl.scenes.WORLD_ID
				&& sourceTransform.getSceneId() != copyTransform.getSceneId())
		{
			ComponentHelper.removeNonSafeSceneReferencesTree(copyTransform);
		}

		// run initial calculations
		ComponentHelper.initialPrepTree(copyTransform);

		if (Lttl.game.inEditor())
		{
			// now add it to GUI
			Lttl.editor.getGui().disableGuiRefresh = false;
			Lttl.editor.getGui().getSelectionController()
					.addTransform(copyTransform, copyTransform.getSceneCore());
		}

		// process onstart callback for entire transforma and all components
		ComponentHelper.callBackTransformTree(copyTransform,
				ComponentCallBackType.onStart);

		return copyTransform;
	}

	/**
	 * Adds a copy of this transform including all of the children to the scene specified. Copies components too. Does
	 * maintain all local tree references that were made between components in the source tree (this can be disabled in
	 * other overloaded function). Maintains world values. Nulls out any component references that are not in world or
	 * same scene.
	 * 
	 * @param sourceTransform
	 * @return
	 */
	public LttlTransform addTransformCopy(LttlTransform sourceTransform)
	{
		return addTransformCopy(sourceTransform, true, true);
	}

	// TODO MAINTAIN WORLD VALUES OPTION?
	/**
	 * Adds a copy of this transform including all of the children to the scene specified. Copies components too. Nulls
	 * out any component references that are not in world or same scene.
	 * 
	 * @param sourceTransform
	 * @param maintainWorldValues
	 *            if true, the child's world position, rotation, scale will be maintained
	 * @param matchLocalTreeReferences
	 *            if true, if there are references of components within the copying transform hierarchy, then it will
	 *            maintain those after the copy
	 * @return
	 */
	public LttlTransform addTransformCopy(LttlTransform sourceTransform,
			boolean maintainWorldValues, boolean matchLocalTreeReferences)
	{
		// scene is unloading protection
		if (getRef().isPendingUnload)
		{
			pendingUnloadError();
			return null;
		}

		// update world values and capture them if maintainng world values
		Vector2 origWorldPos = null;
		Vector2 origWorldScale = null;
		float origWorldRot = 0;
		float origWorldZpos = 0;
		if (maintainWorldValues)
		{
			sourceTransform.updateWorldValues();
			origWorldPos = new Vector2(sourceTransform.getWorldPosition(false));
			origWorldScale = new Vector2(sourceTransform.getWorldScale(false));
			origWorldRot = sourceTransform.getWorldRotation(false);
			origWorldZpos = sourceTransform.getWorldZPos(false);
		}

		if (Lttl.game.inEditor())
		{
			// disable adding things to GUI just in case it's a big add
			Lttl.editor.getGui().disableGuiRefresh = true;
		}

		LttlTransform copyTransform = addTransformCopyNoCallBack(
				sourceTransform, this, matchLocalTreeReferences);

		// add '_copy' to new top level transform
		if (sourceTransform.getName().endsWith("_copy"))
		{
			copyTransform.setName(sourceTransform.getName());
		}
		else
		{
			copyTransform.setName(sourceTransform.getName() + "_copy");
		}

		if (maintainWorldValues)
		{
			// set world values to the originals defined above
			copyTransform.worldToLocalPosition(
					copyTransform.position.set(origWorldPos), false);
			copyTransform.worldToLocalScale(
					copyTransform.scale.set(origWorldScale), false);
			copyTransform.rotation = copyTransform.worldToLocalRotation(
					origWorldRot, false);
			copyTransform.zPos = copyTransform.worldToLocalZPos(origWorldZpos,
					false);
		}

		if (Lttl.game.inEditor())
		{
			// now add it to GUI
			Lttl.editor.getGui().disableGuiRefresh = false;
			Lttl.editor.getGui().getSelectionController()
					.addTransform(copyTransform, copyTransform.getSceneCore());
		}

		// process onstart callback for entire transform and all components adn children
		ComponentHelper.callBackTransformTree(copyTransform,
				ComponentCallBackType.onStart);

		return copyTransform;
	}

	/**
	 * Moves a transform tree (maintains local tree references) from one scene to another. This is simply a copy and a
	 * hard destroy (removes all references). This should really only be used in the editor since in game this type of
	 * move should be avoided for orginzation purposes.
	 * 
	 * @param LttlTransform
	 *            lt transform to move
	 * @param destinationScene
	 * @return the new transform (moved)
	 */
	public LttlTransform moveTransform(LttlTransform lt,
			LttlScene destinationScene)
	{
		// scene is unloading protection
		if (getRef().isPendingUnload)
		{
			pendingUnloadError();
			return null;
		}

		Lttl.Throw(destinationScene);
		if (getRef() == destinationScene.getRef())
		{
			Lttl.Throw("Can't move transform into same scene.");
		}

		if (Lttl.game.inEditor())
		{
			// disable adding things to GUI just in case it's a big add
			Lttl.editor.getGui().disableGuiRefresh = true;
		}

		LttlTransform newTransform = addTransformCopyNoCallBack(lt,
				destinationScene, true);

		if (Lttl.game.inEditor())
		{
			// now add it to GUI
			Lttl.editor.getGui().disableGuiRefresh = false;
			Lttl.editor.getGui().getSelectionController()
					.addTransform(newTransform, newTransform.getSceneCore());
		}

		lt.destroyComp(true, false);
		return newTransform;
	}

	/**
	 * Does everything for adding a copy of a transform, but without the callback, which helps for when moving transform
	 * tree to new scene, it doesn't call onStart again.
	 * 
	 * @param sourceTransform
	 * @param destinationScene
	 * @param matchLocalTreeReferences
	 *            if true, if there are references of components within the copying transform hierarchy, then it will
	 *            maintain those after the copy
	 * @return
	 */
	private LttlTransform addTransformCopyNoCallBack(
			final LttlTransform sourceTransform, LttlScene destinationScene,
			boolean matchLocalTreeReferences)
	{
		// this is a map of the old components with their new counterparts
		HashMap<LttlComponent, LttlComponent> map = (matchLocalTreeReferences) ? new HashMap<LttlComponent, LttlComponent>()
				: null;

		LttlTransform copyTransform = destinationScene.addTransformCopyShared(
				sourceTransform, map, true);

		// OPTIMIZE may make this the default if it's not too expensive
		// update component references so they are setup like the source transform's tree
		if (matchLocalTreeReferences)
		{
			ComponentHelper.matchReferencesCopyTree(copyTransform, map);
		}

		// only remove possible non safe references if the source transform is not in the world scene and it's not
		// in the same scene as destination (new) transform. This must be done after the prior
		// 'matchReferencesCopyTree()' since it needs the source's references to map them to the new components.
		if (sourceTransform.getSceneId() != Lttl.scenes.WORLD_ID
				&& sourceTransform.getSceneId() != copyTransform.getSceneId())
		{
			ComponentHelper.removeNonSafeSceneReferencesTree(copyTransform);
		}

		// run initial calculations
		ComponentHelper.initialPrepTree(copyTransform);

		return copyTransform;
	}

	/**
	 * Creates a copy of the scene (maintaining all local tree references). Can't be ran while playing.
	 * 
	 * @param name
	 * @return new scene
	 */
	LttlScene copyScene(String name)
	{
		// scene is unloading protection
		if (getRef().isPendingUnload)
		{
			pendingUnloadError();
			return null;
		}

		if (Lttl.game.inEditor())
		{
			// disable adding things to GUI just in case it's a big add
			Lttl.editor.getGui().disableGuiRefresh = true;
		}

		// create new scene
		LttlSceneCore newScene = Lttl.scenes.createScene(name).getRef();

		// make a copy of each top level transform
		for (LttlTransform lt : getRef().transformHiearchy)
		{
			addTransformCopyNoCallBack(lt, newScene.getLttlScene(), true);
		}

		if (Lttl.game.inEditor())
		{
			// now add it to GUI
			Lttl.editor.getGui().disableGuiRefresh = false;
			Lttl.editor.getGui().getSelectionController()
					.addSceneTree(newScene.getLttlScene());
		}

		// process onstart callback for all new transforms (and their children and components)
		for (LttlTransform lt : newScene.transformHiearchy)
		{
			ComponentHelper.callBackTransformTree(lt,
					ComponentCallBackType.onStart);
		}
		return newScene.getLttlScene();
	}

	LttlSceneCore getRef()
	{
		return ref;
	}

	/**
	 * Removes the reference to the LttlSceneCore. This is done right before unloading
	 */
	void nullRef()
	{
		ref = null;
	}

	private void pendingUnloadError()
	{
		Lttl.logNote("Unloading Scene: Scene " + getRef().getId()
				+ " is unloading, function not allowed.");
	}

	/**
	 * Returns the first transform found with this name in this scene.<br>
	 * <b>DO NOT RUN EVERY FRAME!</b>
	 * 
	 * @param name
	 * @return found transform or null if none found
	 */
	public LttlTransform findTransform(String name)
	{
		return findTransform(name, null, false);
	}

	/**
	 * Returns the first transform found with this name on a parent. This is expensive and a reference should be saved.<br>
	 * <b>DO NOT RUN EVERY FRAME!</b>
	 * 
	 * @param name
	 * @param parent
	 *            if null, will search all scenes, otherwise will search from this parent down
	 * @param firstDescendents
	 *            should only search the first generation of descendents or all
	 * @return
	 */
	LttlTransform findTransform(String name, LttlTransform parent,
			boolean firstDescendents)
	{
		ArrayList<LttlTransform> result = findTransformsInternal(name, parent,
				true, firstDescendents, null);
		return (result.size() > 0) ? result.get(0) : null;
	}

	/**
	 * Returns all the transforms found with this name in this scene.<br>
	 * <b>DO NOT RUN EVERY FRAME!</b>
	 * 
	 * @param name
	 * @return
	 */
	public ArrayList<LttlTransform> findTransforms(String name)
	{
		return findTransforms(name, null, false);
	}

	/**
	 * Returns all the transforms found with this name on the parent specified.<br>
	 * <b>DO NOT RUN EVERY FRAME!</b>
	 * 
	 * @param name
	 * @param parent
	 *            if null, will search all scenes, otherwise will search from this parent down
	 * @param firstDescendents
	 *            should only search the first generation of descendents or all
	 * @return
	 */
	ArrayList<LttlTransform> findTransforms(String name, LttlTransform parent,
			boolean firstDescendents)
	{
		return findTransformsInternal(name, parent, false, firstDescendents,
				null);
	}

	private ArrayList<LttlTransform> findTransformsInternal(String name,
			LttlTransform parent, boolean limitOne, boolean firstDescendents,
			ArrayList<LttlTransform> finds)
	{
		// create initial finds arrayList if just started
		if (finds == null)
		{
			finds = new ArrayList<LttlTransform>(0);
		}

		// iterate through all
		for (LttlTransform lt : (parent == null) ? getRef().transformHiearchy
				: parent.children)
		{
			// stop looking if only looking for one
			if (limitOne && finds.size() > 0) return finds;

			// check name
			if (name.equals(lt.getName()))
			{
				finds.add(lt);
			}

			// if not just searching firstDescendants then search children of this transform (recursively)
			if (!firstDescendents)
			{
				findTransformsInternal(name, lt, limitOne, firstDescendents,
						finds);
			}
		}
		return finds;
	}

	/**
	 * Returns the first component found with this class in this scene.
	 * 
	 * @param theClass
	 * @param subClasses
	 * @return
	 */
	public <T> T findComponent(Class<T> theClass, boolean subClasses)
	{
		for (LttlComponent lc : getRef().componentMap.values())
		{
			if (lc.getClass() == theClass
					|| (subClasses && theClass.isAssignableFrom(lc.getClass()))) { return (T) lc; }
		}
		return null;
	}

	/**
	 * Returns all the components found with this class in this scene.
	 * 
	 * @param theClass
	 * @param subClasses
	 * @param containerList
	 *            adds the components to this list, does not clear beforehand
	 * @return
	 */
	public <T> ArrayList<T> findComponents(Class<T> theClass,
			boolean subClasses, ArrayList<T> containerList)
	{
		for (LttlComponent lc : getRef().componentMap.values())
		{
			if (lc.getClass() == theClass
					|| (subClasses && theClass.isAssignableFrom(lc.getClass())))
			{
				containerList.add((T) lc);
			}
		}

		return containerList;
	}

	/**
	 * @see #findComponents(Class, boolean, ArrayList)
	 */
	public <T> ArrayList<T> findComponents(Class<T> theClass, boolean subClasses)
	{
		return findComponents(theClass, subClasses, new ArrayList<T>());
	}

	/**
	 * Returns an unmodifiable list of top level transforms in this scene (no parents)
	 * 
	 * @return
	 */
	public List<LttlTransform> getTopLevelTransforms()
	{
		return Collections.unmodifiableList(getRef().transformHiearchy);
	}

	/**
	 * Swaps the order of top level transforms, which ultimately affects the order they are updated
	 * 
	 * @param start
	 * @param dest
	 */
	public void moveTopLevelTransformsOrder(int start, int dest)
	{
		LttlHelper.MoveItemArrayList(getRef().transformHiearchy, start, dest);
	}

	/**
	 * Returns the scene's texture manager.
	 * 
	 * @return
	 */
	public LttlTextureManager getTextureManager()
	{
		return getRef().textureManager;
	}

	/**
	 * Returns the scene's audio manager (sounds and music).
	 * 
	 * @return
	 */
	public LttlAudioManager getAudioManager()
	{
		return getRef().audioManager;
	}

	/**
	 * Returns an unmodifiable collection of all the components in this scene in no specific order.
	 * 
	 * @return
	 */
	public Collection<LttlComponent> getAllComponents()
	{
		return Collections.unmodifiableCollection(getRef().componentMap
				.values());
	}

	/**
	 * Creates a full object based on ObjectType.
	 * 
	 * @param type
	 * @param name
	 * @return
	 */
	public LttlTransform addTransformType(ObjectType type, String name)
	{
		return LttlObjectFactory.AddObject(type, this, name);
	}

	/**
	 * Searches scene for the component with specified id and returns it.
	 * 
	 * @param id
	 * @return null if none found
	 */
	public LttlComponent findComponentById(int id)
	{
		return ref.componentMap.get(id);
	}

	/**
	 * Returns the components that are being referenced in other scenes.<br>
	 * <br>
	 * NOTE: expensive and slow.
	 * 
	 * @return
	 */
	public ArrayList<LttlComponent> getDependencies()
	{
		final ArrayList<LttlComponent> needleComponents = new ArrayList<LttlComponent>(
				getRef().componentMap.values());
		ArrayList<LttlComponent> haystackComponents = new ArrayList<LttlComponent>();
		for (LttlScene scene : Lttl.scenes.getAllLoaded(true))
		{
			if (scene != this)
			{
				haystackComponents.addAll(scene.getRef().componentMap.values());
			}
		}

		final ArrayList<LttlComponent> dependencyList = new ArrayList<LttlComponent>();
		new LttlComponentCrawler()
		{
			public Object set(Object o)
			{
				if (needleComponents.contains(o))
				{
					dependencyList.add((LttlComponent) o);
				}
				return o;
			}

			public Object setHashMapEntry(Object key, Object value)
			{
				if (needleComponents.contains(key))
				{
					dependencyList.add((LttlComponent) key);
				}
				else if (needleComponents.contains(value))
				{
					dependencyList.add((LttlComponent) value);
				}
				return value;
			}
		}.crawl(haystackComponents, FieldsMode.AllButIgnore, 1);

		return dependencyList;
	}

	@Override
	public String toString()
	{
		return getName() + "[" + getId() + "]";
	}
}
