package com.lttlgames.editor;

import java.util.ArrayList;
import java.util.HashMap;

import com.lttlgames.editor.LttlObjectGraphCrawler.FieldsMode;
import com.lttlgames.editor.annotations.ComponentRequired;
import com.lttlgames.editor.annotations.DoNotCopy;
import com.lttlgames.editor.annotations.DoNotExport;
import com.lttlgames.editor.annotations.GuiHide;
import com.lttlgames.editor.annotations.IgnoreCrawl;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.tweenengine.Timeline;
import com.lttlgames.tweenengine.Tween;
import com.lttlgames.tweenengine.TweenGetterSetter;

/**
 * @author Josh
 */
@Persist(-907)
public abstract class LttlComponent
{
	/**
	 * The unique ID for this component, even across scenes. Will need to be set via refractor.
	 */
	@Persist(90701)
	@GuiHide
	// @DoNotExport @see LttlCopier#isExportingFromMenuItem for reason why don't need this
	private int id = -1;

	static private HashMap<Class<? extends LttlComponent>, ArrayList<Class<? extends LttlComponent>>> cachedRequiredComponents = new HashMap<Class<? extends LttlComponent>, ArrayList<Class<? extends LttlComponent>>>();

	/**
	 * Starts enabled, if disabled, then the updates functions will not execute, but the onStart always does.<br>
	 * USE enable()
	 */
	@Persist(90702)
	@GuiHide
	@DoNotExport
	boolean isEnabled = true;

	/**
	 * The transform the component is on.
	 */
	@Persist(90703)
	@IgnoreCrawl
	@GuiHide
	@DoNotExport
	@DoNotCopy
	private LttlTransform transform;

	@Persist(90704)
	@GuiHide
	@DoNotExport
	boolean guiCollapsed = true;

	/**
	 * true if this component has been ran through it's destroyComp method
	 */
	boolean destroyCompProcessed = false;
	/**
	 * starts off as 0, so when onStart runs it will be accurate since it won't reset til first {@link LoopManager#loop} <br>
	 * 0 has to be the bit for none, -1 will match eveything
	 */
	int hasRanBit = 0;

	// FUNCTIONS//

	/**
	 * Runs once when the game has started or when the component is added (created) if the game is already started.<br>
	 * <b>Component does not have to be enabled.</b>
	 */
	public void onStart()
	{
	}

	/**
	 * see {@link #onStart()}
	 */
	public void onEditorStart()
	{
	}

	/**
	 * Runs once when component is globally enabled, when it was original globally disabled. (globally requires the host
	 * transform to be globally enabled). (on callback when component onStart or onDestroy)
	 */
	public void onEnable()
	{
	}

	/**
	 * see {@link #onEnable()}
	 */
	public void onEditorEnable()
	{
	}

	/**
	 * Runs once when component is globally disabled, when it was original globally enabled. (globally including when
	 * the host transform is globally disabled). (on callback when component onStart or onDestroy)
	 */
	public void onDisable()
	{
	}

	/**
	 * see {@link #onDisable()}
	 */
	public void onEditorDisable()
	{
	}

	/**
	 * See Execution Order of Event Functions doc
	 */
	public void onEarlyUpdate()
	{
	}

	/**
	 * see {@link #onEarlyUpdate()}
	 */
	public void onEditorEarlyUpdate()
	{
	}

	/**
	 * See Execution Order of Event Functions doc
	 */
	public void onUpdate()
	{
	}

	/**
	 * see {@link #onUpdate()}
	 */
	public void onEditorUpdate()
	{
	}

	/**
	 * See Execution Order of Event Functions doc
	 */
	public void onLateUpdate()
	{
	}

	/**
	 * see {@link #onLateUpdate()}
	 */
	public void onEditorLateUpdate()
	{
	}

	/**
	 * See Execution Order of Event Functions doc<br>
	 * This will be called each time {@link LttlGameSettings#fixedDeltaTime} is passed.<br>
	 * Not guaranteed to run every frame. It may be called once a frame, more than once a frame, or <b>NONE</b>.<br>
	 * Note: Never called if paused, which is probably okay
	 */
	public void onFixedUpdate()
	{
	}

	/**
	 * @see {@link #onFixedUpdate()}
	 */
	public void onEditorFixedUpdate()
	{
	}

	/**
	 * After physics step.
	 * 
	 * @see {@link #onFixedUpdate()}
	 */
	public void onLateFixedUpdate()
	{
	}

	/**
	 * @see {@link #onLateFixedUpdate()}
	 */
	public void onEditorLateFixedUpdate()
	{
	}

	/**
	 * Processed as soon as destroyed is called by user (immediate), does not wait til end of frame. Called when scene
	 * is unloaded too. This is good for cleaning up references and stuff.<br>
	 * If destroying a {@link LttlTransform} the order of the callbacks will be down the transform tree -
	 * {@link ComponentHelper#callBackTransformTree}. {@link LttlComponent#isDestroyPending()} will be accurate, and can
	 * be called on the host or parent transform to check if they are being destroyed too.<br>
	 * <b>Component does not have to be enabled.</b>
	 */
	public void onDestroyComp()
	{
	}

	/**
	 * see {@link #onDestroyComp()}
	 */
	public void onEditorDestroyComp()
	{
	}

	/**
	 * when the scene is saved, this runs (can only save in editor)<br>
	 * Useful for prepping any values before saving. (ie. convert IntMap to Hashmap)<br>
	 * <b>Component does not have to be enabled.</b>
	 */
	public void onSaveScene()
	{
	}

	/**
	 * This runs every frame after the late update regardless of mode (even paused) but only in editor. The component
	 * needs to be enabled. This is where you should place your debug draw calls. If you want to debug draw in play mode
	 * while not in editor. Your debug draw calls need to be in another function. Do not update anything in this
	 * function, since it runs when editor paused.
	 */
	public void debugDraw()
	{

	}

	/**
	 * Enables the component (may still be disabled because of host transform)
	 */
	final public void enable()
	{
		boolean origState = isEnabled();
		isEnabled = true;
		boolean newState = isEnabled();

		// updates gui
		if (Lttl.game.inEditor())
		{
			Lttl.editor.getGui().onComponentDisableEnable(transform());
		}

		// if different process callback, this way it only runs callback if there really is a change in enable state,
		// which includes the the host transform
		if (origState != newState)
		{
			ComponentHelper.processCallBack(this,
					ComponentCallBackType.onEnable);
		}
	}

	/**
	 * Disables the component.
	 */
	final public void disable()
	{
		boolean origState = isEnabled();
		isEnabled = false;
		boolean newState = isEnabled();

		// updates gui
		if (Lttl.game.inEditor())
		{
			Lttl.editor.getGui().onComponentDisableEnable(transform());
		}

		// if different process callback, this way it only runs callback if there really is a change in enable state,
		// which includes the the host transform
		if (origState != newState)
		{
			ComponentHelper.processCallBack(this,
					ComponentCallBackType.onDisable);
		}
	}

	/**
	 * Returns the Global enable state (if this component and it's host transform is Globally enabled)
	 * 
	 * @return
	 */
	public boolean isEnabled()
	{
		return t().isEnabled() && isEnabled;
	}

	/**
	 * Returns if this component is enabled, does not check transform
	 * 
	 * @return
	 */
	public boolean isEnabledSelf()
	{
		return isEnabled;
	}

	/**
	 * Checks if component is enabled and is allowed to be running IF the game happen to be paused. If not, this is thea
	 * same as isEnabled()
	 * 
	 * @return
	 */
	public boolean isPauseEnabled()
	{
		if (Lttl.game.isPaused())
		{
			if (transform().unPauseable)
			{
				return isEnabled();
			}
			else
			{
				return false;
			}
		}
		else
		{
			return isEnabled();
		}
	}

	/**
	 * Get the ID of this component
	 * 
	 * @return
	 */
	public int getId()
	{
		return id;
	}

	/**
	 * Returns the transform this component is on.
	 * 
	 * @return
	 */
	public LttlTransform transform()
	{
		return transform;
	}

	/**
	 * Returns the transform this component is on.
	 * 
	 * @return
	 */
	public LttlTransform t()
	{
		return transform;
	}

	/**
	 * @see #destroyComp(boolean, boolean)
	 */
	public boolean destroyComp()
	{
		return destroyComp(false, false);
	}

	/**
	 * Removes component from it's transform's component list and from the scene's component list and set's all it's
	 * refernces to null. Works for all components, including LttlTransforms. Does not destroy dependents automatically,
	 * will error out if there are dependents.</br> Notes: The component will be disabled, isDestroyPending() will be
	 * true, and end of frame it will be removed. Will not render this frame.<br>
	 * Also process the 'onDestroy' callbacks with no delay.
	 * 
	 * @param hard
	 *            if hard, then scans through all components loaded in all scenes and nulls references (
	 *            {@link FieldsMode#AllButIgnore}) [expensive].
	 * @param immediate
	 *            does not wait til end of frame, may mess stuff up, a dependent component who is being delete this
	 *            frame may have errors if it is still called and requires this component, so be careful, still wont
	 *            delete if has dependents that are not being delete this frame
	 * @return if destroy was successful
	 */
	public boolean destroyComp(boolean hard, boolean immediate)
	{
		return destroyComp(hard, immediate, true, true);
	}

	/**
	 * Internal destroy method that has a checkDependents and onDestroyCallback variable which are made false by
	 * LttlTransform since there is no need to check depdendents when everything is getting destroyed or do callbacks
	 * since they are all done as a tree callback when the root transform is called.
	 * 
	 * @param hard
	 * @param immediate
	 * @param checkDependents
	 * @param onDestroyCallback
	 * @return if destroy was successful
	 */
	final boolean destroyComp(boolean hard, boolean immediate,
			boolean checkDependents, boolean onDestroyCallback)
	{
		// early out if destroyComp() is called more than once on a component
		// already checked if LttlTransform is already being destroyed in it's own destroyComp() so no need to check
		// again since if it's a LttlTransform it will always be destroyPending
		// also don't use isDestroyPending(), instead use destroyPending variable directly since we want to know if this
		// component specifically has been ran through destroyComp() yet, isDestroyPending() checks the host and parent
		// transform too
		if (this.getClass() != LttlTransform.class && destroyCompProcessed)
			return false;

		if (checkDependents && hasDependents())
		{
			Lttl.logNote("Destroying Component: Can't destroy component "
					+ this.getClass().getName() + " on "
					+ this.transform().getName()
					+ " because it has dependents.");
			return false;
		}

		// set destroyed properties, want to be accurate when does callback
		destroyCompProcessed = true;

		// process onDestroy callbacks
		if (onDestroyCallback)
		{
			ComponentHelper.processCallBack(this,
					ComponentCallBackType.onDestroy);
		}

		// disables without callback
		isEnabled = false;

		if (Lttl.game.inEditor())
		{
			// redraw properties if this transform is in focus on properties, needs to be after destroyPending
			if (transform().getId() == Lttl.editor.getGui()
					.getPropertiesController().getFocusTransformId())
			{
				Lttl.editor.getGui().getPropertiesController().draw(false);
			}

			// remove all undo fields for this component, skip if scene is unloading, since that will do same thing
			if (!getScene().isPendingUnload())
			{
				Lttl.editor.getUndoManager().removeComponentUndos(getId());
			}
		}

		// immediately destroy component
		if (immediate)
		{
			executeDestroy();
			if (hard)
			{
				// immediately remove all references from global
				ComponentHelper.removeComponentReferencesGlobal(this);
			}
		}
		else
		{
			// queue for destroy
			Lttl.loop.compDestroyList.add(this);

			// add this component to queue of components to remove all game references, this will run during the stage
			// of the next frame right after components are processed to be destroyed
			if (hard)
			{
				Lttl.loop.compHardDestroyList.add(this);
			}
		}
		return true;
	}

	/**
	 * Is this component going to be officially destroyed (removed from scene) at the end of the update functions and
	 * before the render.<br>
	 * This is accurate (for the entire tree) the moment the component, host transform, or parent transform's
	 * {@link LttlComponent#destroyComp()} is called, even when called inside {@link LttlComponent#onDestroyComp()}.
	 * 
	 * @return
	 */
	public boolean isDestroyPending()
	{
		// check if this component has personally ran through it's destroyComp method or if it is on a transform or a
		// child of a transform that has
		return destroyCompProcessed || transform.isDestroyPendingAncestor();
	}

	/**
	 * Removes standard component referenes (Transform, Renderer, etc) and removes component from scene and host
	 * transform and destroys all tweens and timeline tweens with this component.
	 */
	void executeDestroy()
	{
		ComponentHelper.removeStandardComponentReferences(this);

		// clean up any tweens
		Lttl.tween.getManager().killHost(this);
		Lttl.tween.getManager().killTarget(this);

		// remove from scene and transform
		transform().components.remove(this);
		transform().getSceneCore().componentMap.remove(getId());
	}

	/**
	 * Checks if there is any other component requiring this component as defined in requiredComponents.
	 * 
	 * @return
	 */
	public boolean hasDependents()
	{
		// iterate through all the components on this transform
		for (LttlComponent component : transform().components)
		{
			// skip self
			if (component == this) continue;

			// skip checking if the component is already destroyed
			if (component.isDestroyPending()) continue;

			// check if the component has requiredComponents and one of them is this class (or this class'
			// super classes)
			for (Class<? extends LttlComponent> reqC : LttlComponent
					.getRequiredComponents(component.getClass()))
			{
				if (reqC.isAssignableFrom(this.getClass())) { return true; }
			}
		}
		return false;
	}

	/**
	 * Returns all the components that require this component
	 * 
	 * @return
	 */
	public ArrayList<LttlComponent> getDependents()
	{
		ArrayList<LttlComponent> dependents = new ArrayList<LttlComponent>();
		// iterate through all the components on this transform
		for (LttlComponent component : transform().components)
		{
			if (component == this) continue;
			if (component.isDestroyPending()) continue; // skip checking if the component is already destroyed

			for (Class<? extends LttlComponent> reqC : LttlComponent
					.getRequiredComponents(component.getClass()))
			{
				if (reqC.isAssignableFrom(this.getClass()))
				{
					dependents.add(component);
					break;
				}
			}
		}
		return dependents;
	}

	/**
	 * Checks if this component can safely be referenced (either in same scene or in world scene)
	 * 
	 * @param component
	 * @return
	 */
	public boolean isSafeReference(LttlComponent component)
	{
		int sceneId = component.transform().getSceneId();
		if (sceneId == Lttl.scenes.WORLD_ID) return true;
		if (sceneId == this.transform().getSceneId()) return true;
		return false;
	}

	/**
	 * Returns the scene core this component is in.
	 * 
	 * @return
	 */
	LttlSceneCore getSceneCore()
	{
		return t().getSceneCore();
	}

	/**
	 * Returns the scene this component is in.
	 * 
	 * @return
	 */
	public final LttlScene getScene()
	{
		return getSceneCore().getLttlScene();
	}

	/**
	 * Called whenever the screen is resized. This is highly useful for components that are based on the viewport
	 * dimensions. Includes first time game starts, which is before OnStart. If game is already started and this
	 * component is created, then it will not run onResize() automatically.
	 */
	public void onResize()
	{

	}

	/**
	 * Called whenever the screen is resized in the editor not in play mode.
	 */
	public void onEditorResize()
	{
	}

	/**
	 * Runs once before any other callback (for the entire life of the component) when the component is first created
	 * (in editor only). This is different that onEditorStart becuase Start runs every time the editor starts, this does
	 * not.<br>
	 * Often used to initialize variables.<br>
	 * Will not be called on copying or moving of components. Only on initial create.<br>
	 * Will only be called in play mode if added by the editor, not via script.<br>
	 * <b>Component does not have to be enabled.</b>
	 */
	public void onEditorCreate()
	{

	}

	/**
	 * Returns the renderer component (if any).
	 * 
	 * @return
	 */
	public LttlRenderer renderer()
	{
		return transform().renderer;
	}

	/**
	 * Returns the renderer component (if any).
	 * 
	 * @return
	 */
	public LttlRenderer r()
	{
		return transform().renderer;
	}

	int getSceneId()
	{
		return transform().getSceneId();
	}

	<T extends LttlComponent> T addComponentInternal(Class<T> componentType,
			boolean onStartCallBack)
	{
		if (!transform.canAddComponentType(componentType)) { return null; }

		T newComp = LttlObjectGraphCrawler.newInstance(componentType);

		addComponentShared(newComp);
		if (onStartCallBack)
		{
			ComponentHelper.processCallBack(newComp,
					ComponentCallBackType.onStart);
		}
		return newComp;
	}

	/**
	 * Checks if component can be added to transform. Then creates a new instance of it and add's it to transform. Do
	 * not use this to add transforms, use addChild()
	 * 
	 * @param componentType
	 * @return the new component, null if failed.
	 */
	public <T extends LttlComponent> T addComponent(Class<T> componentType)
	{
		return addComponentInternal(componentType, true);
	}

	/**
	 * Adds all the new components, order matters when their are dependencies.
	 * 
	 * @param components
	 */
	public void addComponents(Class<? extends LttlComponent>... components)
	{
		for (Class<? extends LttlComponent> c : components)
		{
			addComponent(c);
		}
	}

	/**
	 * Copies a componet entirely (including component references and abides by annotations). First it checks if it can
	 * be added. Component can be in any scene, if in another scene (not including world) then checks if has non safe
	 * references (not same scene or world) and makes reference null.
	 * 
	 * @param component
	 * @return the new component, null if failed.
	 */
	public <T extends LttlComponent> T addComponentCopy(T component)
	{
		Lttl.Throw(component);

		// check if can add this component
		if (!transform().canAddComponentType(component.getClass())) { return null; }

		// make a copy of component
		T newComp = LttlCopier.copy(component);
		Lttl.Throw(newComp);

		// remove non world and same scene references only if coming from different scene
		if (component.transform().getSceneId() != Lttl.scenes.WORLD_ID
				&& component.transform().getSceneId() != this.getSceneId())
		{
			ComponentHelper.removeNonSafeSceneReferences(component);
		}

		addComponentShared(newComp);
		ComponentHelper.processCallBack(newComp, ComponentCallBackType.onStart);
		return newComp;
	}

	/**
	 * Adds specified component to transform, assumes already checked if allowed to add component to transform. Sets up
	 * component and everything
	 * 
	 * @param newComp
	 */
	private void addComponentShared(LttlComponent newComp)
	{
		Lttl.Throw(newComp);

		// already checked if could add it, so no need to check again
		LttlSceneCore scene = transform().getSceneCore();
		ComponentHelper.setupNewComponent(newComp, transform(), scene);

		// redraw properties for this transform if it is in focus
		if (Lttl.game.inEditor())
		{
			if (Lttl.editor.getGui().getPropertiesController()
					.getFocusTransformId() == transform().getId())
			{
				Lttl.editor.getGui().getPropertiesController().draw(false);
			}
		}
	}

	/**
	 * Tween a float fields (cast to int, if needed) via a TweenGetterSetter (does not uses reflection).<br>
	 * Note: Color2 and Vector2 objects can be tweened from Lttl.tween
	 * 
	 * @param duration
	 * @param getterSetter
	 *            how to get and set the values, target object will be a final variable
	 * @param targetValues
	 * @return
	 */
	public Tween tweenTo(float duration, TweenGetterSetter getterSetter,
			float... targetValues)
	{
		return Lttl.tween.tweenTo(this, duration, getterSetter, targetValues);
	}

	/**
	 * A Timeline can be used to create complex animations made of sequences and parallel sets of Tweens.
	 * <p/>
	 * The following example will create an animation sequence composed of 5 parts:
	 * <p/>
	 * 1. First, opacity and scale are set to 0 (with Tween.set() calls).<br/>
	 * 2. Then, opacity and scale are animated in parallel.<br/>
	 * 3. Then, the animation is paused for 1s.<br/>
	 * 4. Then, position is animated to x=100.<br/>
	 * 5. Then, rotation is animated to 360°.
	 * <p/>
	 * This animation will be repeated 5 times, with a 500ms delay between each iteration: <br/>
	 * <br/>
	 * 
	 * <pre>
	 * {@code
	 * Timeline.createSequence()
	 *     .push(Tween.set(myObject, OPACITY).target(0))
	 *     .push(Tween.set(myObject, SCALE).target(0, 0))
	 *     .beginParallel()
	 *          .push(Tween.to(myObject, OPACITY, 0.5f).target(1).ease(Quad.INOUT))
	 *          .push(Tween.to(myObject, SCALE, 0.5f).target(1, 1).ease(Quad.INOUT))
	 *     .end()
	 *     .pushPause(1.0f)
	 *     .push(Tween.to(myObject, POSITION_X, 0.5f).target(100).ease(Quad.INOUT))
	 *     .push(Tween.to(myObject, ROTATION, 0.5f).target(360).ease(Quad.INOUT))
	 *     .repeat(5, 0.5f)
	 *     .start();
	 * }
	 * </pre>
	 */
	public Timeline tweenSequence()
	{
		return Lttl.tween.createSequence(this);
	}

	/**
	 * Creates a new timeline with a 'parallel' behavior. Its children will be triggered all at once.
	 */
	public Timeline tweenParallel()
	{
		return Lttl.tween.createParallel(this);
	}

	/**
	 * Kill all tweens associated with this component.
	 */
	public void tweenKillAll()
	{
		Lttl.tween.getManager().killHost(this);
	}

	/**
	 * Checks all the other scenes to see if any components are referencing this component. This is slow and
	 * inefficient.
	 * 
	 * @param searchOwnScene
	 *            should it search it's own scene too
	 * @param fieldsMode
	 * @return the number of dependencies in other scenes, 0 means none
	 */
	public int checkRefDependency(boolean searchOwnScene, FieldsMode fieldsMode)
	{
		ArrayList<LttlComponent> needleComponents = new ArrayList<LttlComponent>();
		needleComponents.add(this);
		ArrayList<LttlScene> scenes = new ArrayList<LttlScene>();
		for (LttlScene scene : Lttl.scenes.getAllLoaded(true))
		{
			if (searchOwnScene || scene != getScene())
			{
				scenes.add(scene);
			}
		}
		return ComponentHelper.checkDependencies(needleComponents, scenes,
				fieldsMode);
	}

	/**
	 * Returns all the required components for a LttlComponent class (caches)
	 * 
	 * @param clazz
	 * @return
	 */
	public static ArrayList<Class<? extends LttlComponent>> getRequiredComponents(
			Class<? extends LttlComponent> clazz)
	{
		// see if it is in cache
		if (cachedRequiredComponents.containsKey(clazz)) { return cachedRequiredComponents
				.get(clazz); }

		ArrayList<Class<? extends LttlComponent>> reqComps = new ArrayList<Class<? extends LttlComponent>>();
		Class<? extends LttlComponent> currentClass = clazz;

		// iterate through declared class and all super classes
		while (currentClass != LttlComponent.class)
		{
			// add each required components if don't exist yet
			ComponentRequired cr = null;
			if ((cr = currentClass.getAnnotation(ComponentRequired.class)) != null)
			{
				for (Class<? extends LttlComponent> c : cr.value())
				{
					if (!reqComps.contains(c))
					{
						reqComps.add(c);
					}
				}
			}

			// go to next super class
			currentClass = (Class<? extends LttlComponent>) currentClass
					.getSuperclass();
		}

		// save in cache
		cachedRequiredComponents.put(clazz, reqComps);

		return reqComps;
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "(" + getId() + ", "
				+ t().getName() + ")";
	}

	public String toStringPlusScene()
	{
		return getClass().getSimpleName() + "(" + getId() + ", "
				+ t().getName() + "-" + getScene().getName() + ")";
	}

	/**
	 * This returns if this component has ran the specified callback type
	 * 
	 * @return
	 */
	final public boolean hasRan(ComponentCallBackType callbackType)
	{
		return (hasRanBit & callbackType.getValue()) != 0;
	}

	public boolean isSelectedInEditor()
	{
		return Lttl.game.inEditor()
				&& Lttl.editor.getGui().getSelectionController()
						.isSelected(t());
	}

	public boolean isFocusedInEditor()
	{
		return Lttl.game.inEditor()
				&& Lttl.editor.getGui().getPropertiesController()
						.getFocusedTransformsCount() == 1
				&& Lttl.editor.getGui().getPropertiesController()
						.isFocused(t());
	}
}
