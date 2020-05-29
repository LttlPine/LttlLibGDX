package com.lttlgames.editor;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntMap;
import com.lttlgames.editor.LttlObjectGraphCrawler.FieldsMode;
import com.lttlgames.editor.annotations.AnimateField;
import com.lttlgames.editor.annotations.ComponentLimitOne;
import com.lttlgames.editor.annotations.DoNotCopy;
import com.lttlgames.editor.annotations.DoNotExport;
import com.lttlgames.editor.annotations.GuiAutoExpand;
import com.lttlgames.editor.annotations.GuiButton;
import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.GuiCallbackDescendants;
import com.lttlgames.editor.annotations.GuiGroup;
import com.lttlgames.editor.annotations.GuiHide;
import com.lttlgames.editor.annotations.GuiHideComponentList;
import com.lttlgames.editor.annotations.GuiShow;
import com.lttlgames.editor.annotations.IgnoreCrawl;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.editor.interfaces.AlternateSelectionBounds;
import com.lttlgames.helpers.EaseType;
import com.lttlgames.helpers.LttlCallback;
import com.lttlgames.helpers.LttlGeometryUtil.PolygonContainer;
import com.lttlgames.helpers.LttlHelper;
import com.lttlgames.helpers.LttlMath;
import com.lttlgames.helpers.LttlProfiler;
import com.lttlgames.tweenengine.Timeline;
import com.lttlgames.tweenengine.Tween;
import com.lttlgames.tweenengine.TweenGetterSetter;

//17
@ComponentLimitOne
@GuiHideComponentList
@Persist(-9020)
public final class LttlTransform extends LttlComponent implements LttlAnimated
{
	/**
	 * bit that holds all tags for this transform
	 */
	@Persist(9020017)
	@GuiHide
	short tagBit = 0;

	/**
	 * This is the name of the game object, helps with looking up game objects
	 */
	@GuiCallback("guiUpdateTreeName")
	@Persist(902001)
	@DoNotExport
	private String name;

	private LttlScene sceneRef;

	/**
	 * Reference to commonly used renderer component
	 */
	@Persist(902002)
	@GuiHide
	@IgnoreCrawl
	@DoNotCopy
	@DoNotExport
	LttlRenderer renderer;

	@Persist(902003)
	@GuiHide
	@IgnoreCrawl
	@DoNotCopy
	@DoNotExport
	LttlTransform parentTransform;
	private int lastParentId = -1;

	@Persist(902004)
	@GuiHide
	@IgnoreCrawl
	@DoNotCopy
	@DoNotExport
	final ArrayList<LttlTransform> children = new ArrayList<LttlTransform>();

	@Persist(902005)
	@GuiHide
	@IgnoreCrawl
	@DoNotCopy
	@DoNotExport
	final ArrayList<LttlComponent> components = new ArrayList<LttlComponent>();

	@Persist(902006)
	@GuiHide
	@DoNotCopy
	@DoNotExport
	private int sceneId = -1;

	@GuiAutoExpand
	@Persist(902009)
	@AnimateField(0)
	@GuiCallbackDescendants("onGuiPosition")
	public final Vector2 position = new Vector2();

	@Persist(9020010)
	@AnimateField(1)
	@GuiCallback("onGuiZ")
	public float zPos = 0;

	@Persist(9020011)
	@AnimateField(2)
	@GuiCallbackDescendants("onGuiScale")
	public final Vector2 scale = new Vector2(1, 1);

	@Persist(9020012)
	@AnimateField(3)
	@GuiCallback("onGuiRotation")
	public float rotation = 0;

	/**
	 * Used to set the origin of the mesh that is rendering
	 */
	@Persist(9020013)
	@AnimateField(4)
	@GuiCallbackDescendants("onGuiOrigin")
	public final Vector2 originRenderMesh = new Vector2();

	@Persist(9020014)
	public boolean drawOrigin = false;

	@Persist(9020016)
	@AnimateField(5)
	@GuiCallbackDescendants("onGuiShear")
	public final Vector2 shear = new Vector2(0, 0);

	private ArrayList<GuiTransformListener> guiListeners;
	private AlternateSelectionBounds altSelectionBounds;

	private int modifiedId = 0;

	private Matrix3 localTransform = new Matrix3();
	/**
	 * represents the local render transform, includes origin
	 */
	private Matrix3 localRenderTransform = new Matrix3();
	final Matrix3 worldTransform = new Matrix3();
	/**
	 * used for when matrix needs to be temporarily used as mathematical container
	 */
	private static final Matrix3 sharedMatrix = new Matrix3();
	/**
	 * this should only be used when getting the position of something rendering on this transform, aka mesh positions,
	 * otherwise worldTransform is what is used to find the position of a child, worldRenderTransform has the origin
	 * already baked into it, so the mesh is as if it was a child and offsetted when it renders
	 */
	final Matrix3 worldRenderTransform = new Matrix3();
	// holds the conversion from matrix3 to matrix4 to be used with worldMatrix
	Matrix4 worldTransform4 = new Matrix4();

	private final static Vector2 tmpV2 = new Vector2();

	@GuiShow
	@GuiGroup("World")
	@GuiCallbackDescendants("guiSetWorldPosition")
	Vector2 worldPosition = new Vector2(0, 0);
	Vector2 worldRenderPosition = new Vector2(0, 0);
	@GuiShow
	@GuiGroup("World")
	@GuiCallbackDescendants("guiSetWorldScale")
	Vector2 worldScale = new Vector2(1, 1);
	@GuiShow
	@GuiGroup("World")
	@GuiCallbackDescendants("guiSetWorldShear")
	Vector2 worldShear = new Vector2(0, 0);
	@GuiShow
	@GuiGroup("World")
	@GuiCallbackDescendants("guiSetWorldRotation")
	float worldRotation;
	@GuiShow
	@GuiGroup("World")
	@GuiCallback("guiSetWorldZpos")
	float worldZpos = 0;
	float lastF_worldZpos = Float.POSITIVE_INFINITY; // force change

	@Persist(9020015)
	public SelectionOptions selectionOptions = new SelectionOptions();
	/**
	 * If true, then when game is in pause mode, this transform and it's components will still get update and other
	 * callbacks. This is independent from hiearchy, so children can be unpauseable while the parent is paused.
	 */
	@Persist(902007)
	public boolean unPauseable = false;

	{
		// default open
		guiCollapsed = false;
	}

	@SuppressWarnings("unused")
	private void guiSetWorldPosition()
	{
		setWorldPosition(worldPosition);
	}

	@SuppressWarnings("unused")
	private void guiSetWorldScale()
	{
		setWorldScale(worldScale);
	}

	@SuppressWarnings("unused")
	private void guiSetWorldShear()
	{
		setWorldShear(worldShear);
	}

	@SuppressWarnings("unused")
	private void guiSetWorldZpos()
	{
		setWorldzPos(worldZpos);
	}

	@SuppressWarnings("unused")
	private void guiSetWorldRotation()
	{
		setWorldRotation(worldRotation);
	}

	/**
	 * The enable that takes into consideration parents being disabled.
	 */
	boolean enabledThisFrame = true;

	private Vector2 oldPosition = new Vector2(Float.POSITIVE_INFINITY,
			Float.POSITIVE_INFINITY);
	private Vector2 oldScale = new Vector2(Float.POSITIVE_INFINITY,
			Float.POSITIVE_INFINITY);
	private Vector2 oldOrigin = new Vector2(Float.POSITIVE_INFINITY,
			Float.POSITIVE_INFINITY);
	private Vector2 oldShear = new Vector2(Float.POSITIVE_INFINITY,
			Float.POSITIVE_INFINITY);
	private float oldRotation = Float.POSITIVE_INFINITY;
	private float oldzPos = Float.POSITIVE_INFINITY;

	boolean parentTransformChanged = true; // starts true to force a refresh on start

	// GETTERS and SETTERS//
	public String getName()
	{
		return name;
	}

	/**
	 * Sets the name of the transform.
	 * 
	 * @param name
	 * @return itself for chaining.
	 */
	public LttlTransform setName(String name)
	{
		this.name = name;

		if (Lttl.game.inEditor())
		{
			guiUpdateTreeName();
		}

		return this;
	}

	private void guiUpdateTreeName()
	{
		// updates the node label in the tree
		Lttl.editor.getGui().getSelectionController().reloadNode(this);
		// updates the label on the LttlTransform component in the properties panel
		ArrayList<GuiFieldObject<?>> focused = Lttl.editor.getGui()
				.getPropertiesController().focusedGuiFieldObjects;
		if (focused != null && focused.size() > 0)
		{
			focused.get(0).updateLabel();
		}
	}

	/**
	 * Toggles (on or off) all renderers on all children. Does not toggle itself though.
	 * 
	 * @param toggle
	 */
	public void toggleAllChildrenRenderers(boolean toggle)
	{
		for (LttlTransform lt : this.children)
		{
			if (lt.renderer != null)
			{
				if (toggle)
				{
					lt.renderer.enable();
				}
				else
				{
					lt.renderer.disable();
				}
			}
			lt.toggleAllChildrenRenderers(toggle);
		}
	}

	/**
	 * @see #destroyComp(boolean, boolean)
	 */
	@Override
	public boolean destroyComp()
	{
		return destroyComp(false, false);
	}

	/**
	 * Destroys all children and their components and then destroys this transform's components and self. Removing all
	 * from scene.<br>
	 * As for the {@link LttlComponent#onDestroyComp()} callback, that is called at very start before anything changes,
	 * and it is called from this transform and down the tree.
	 * 
	 * @see LttlComponent#destroyComp(boolean, boolean)
	 */
	@Override
	public boolean destroyComp(boolean hard, boolean immediate)
	{
		return destroyTransformInternal(hard, immediate, true);
	}

	/**
	 * @param hard
	 * @param immediate
	 * @param isRootDestroyTransform
	 *            is this the root transform that called {@link #destroyComp()}
	 * @return
	 */
	private boolean destroyTransformInternal(boolean hard, boolean immediate,
			boolean isRootDestroyTransform)
	{
		// early out if called more than once
		if (destroyCompProcessed) return false;

		// set early (instead of in super.destroyComp()) because gui needs it defined now, should not hurt anything
		// set destroyed properties, want to be accurate when does callback
		destroyCompProcessed = true;

		// only do onDestroy tree callback if this is the root transform
		if (isRootDestroyTransform)
		{
			// process onDestroy callbacks for entire tree
			// ignore if pending to be destroyed
			ComponentHelper.callBackTransformTree(this,
					ComponentCallBackType.onDestroy);
		}
		isEnabled = false;

		// if hard is true when destroying immediately, then process everything as if hard was false, so it doesn't
		// individually process removeAllGameReferences, which is super expensive, instead add each one to a static
		// array, which is added
		boolean hardImmediate = false;
		if (hard && immediate)
		{
			hardImmediate = true;
			ComponentHelper.componentsToHardDestroyImmediately.clear();
			hard = false;
		}

		// destroy all children
		// iterate through and destroy children and remove each manually
		for (LttlTransform lt : getChildren())
		{
			// don't need to add to componentsToHardDestroyImmediately because it'll be added when it is actually
			// destroyed (lower)
			// set it as non root transform because it is a child
			lt.destroyTransformInternal(hard, immediate, false);
		}

		// remove this transform from transform tree (after children)
		if (Lttl.game.inEditor())
		{
			Lttl.editor.getGui().getSelectionController()
					.removeTransform(this, this.getSceneCore());
		}

		// remove self from parent, if parent is not being destroyed too
		if (getParent() != null && !getParent().destroyCompProcessed)
		{
			setParent(null, false);
		}

		// destroy all components
		// iterate through components (make copy of list because they could be modifying it)
		for (LttlComponent lc : new ArrayList<LttlComponent>(components))
		{
			if (immediate)
			{
				ComponentHelper.componentsToHardDestroyImmediately.add(lc);
			}
			// make sure not to do callbacks, since already called when root transform's destroyComp was called
			lc.destroyComp(hard, immediate, false, false);
		}

		// then destroy self
		if (immediate)
		{
			ComponentHelper.componentsToHardDestroyImmediately.add(this);
		}

		// LIST CLEAN UP
		// remove from LoopManager's z ordered list (preventing any rendering)
		Lttl.loop.transformsOrdered.remove(this);
		// remove from it's scene's transform hierarchy (top level) if it is there
		getSceneCore().transformHiearchy.remove(this);

		boolean result = super.destroyComp(hard, immediate, false, false);

		// now that all children, components, and self have been destroyed, and if this was originally suppose to be a
		// hard destroy immediately, then remove all the references at once
		if (hardImmediate)
		{
			ComponentHelper
					.removeComponentReferencesGlobal(ComponentHelper.componentsToHardDestroyImmediately);

			ComponentHelper.componentsToHardDestroyImmediately.clear();
		}

		return result;
	}

	/**
	 * Updates all the world values on this transform and all of it's parents. Use this before calling any of the
	 * getWorld...() functions if you don't want to call check on them individidually.
	 */
	public void updateWorldValues()
	{
		updateUpHiearchyTransform();
	}

	/**
	 * Updates all the world values on this transform and all it's descendents. It's good to call this on the
	 * highestParent() if you want everything in the tree to be updated.
	 */
	public void updateWorldValuesTree()
	{
		updateWorldValues();
		updateDownHiearchyTransform();
	}

	/**
	 * Updates down a tree, assumes the parent is fully updated
	 */
	private void updateDownHiearchyTransform()
	{
		// updates each child and down it's hierachy
		for (LttlTransform lt : children)
		{
			lt.updateTransforms(false);
			lt.updateDownHiearchyTransform();
		}
	}

	/**
	 * @return The current world position vector, which is based on origin.<br>
	 *         <b>(THIS IS SHARED AND SHOULD BE READ ONLY)</b>
	 * @param check
	 *            if true, will check all parent transforms and self for any changes. It is best to only do check if you
	 *            know the world values have changed. Running a getWorld..() function with check true, will check and
	 *            update all world values. It is good to run a updateWorldValues() and then use all getWorld...() with
	 *            check false.
	 */
	public Vector2 getWorldPosition(boolean check)
	{
		if (check)
		{
			updateWorldValues();
		}
		return worldPosition;
	}

	/**
	 * @return The current world render position vectorm this is where the mesh will render, adds origin as part of
	 *         position
	 * @param check
	 *            if true, will check all parent transforms and self for any changes. It is best to only do check if you
	 *            know the world values have changed. Running a getWorld..() function with check true, will check and
	 *            update all world values. It is good to run a updateWorldValues() and then use all getWOrld...() with
	 *            check false.
	 */
	public Vector2 getWorldRenderPosition(boolean check)
	{
		if (check)
		{
			updateWorldValues();
		}
		return worldRenderPosition;
	}

	/**
	 * @return The current world scale vector. (THIS IS SHARED AND SHOULD BE READ ONLY)
	 * @param check
	 *            if true, will check all parent transforms and self for any changes. It is best to only do check if you
	 *            know the world values have changed. Running a getWorld..() function with check true, will check and
	 *            update all world values. It is good to run a updateWorldValues() and then use all getWOrld...() with
	 *            check false.
	 */
	public Vector2 getWorldScale(boolean check)
	{
		if (check)
		{
			updateWorldValues();
		}
		return worldScale;
	}

	/**
	 * @return The current world shear vector. (THIS IS SHARED AND SHOULD BE READ ONLY)
	 * @param check
	 *            if true, will check all parent transforms and self for any changes. It is best to only do check if you
	 *            know the world values have changed. Running a getWorld..() function with check true, will check and
	 *            update all world values. It is good to run a updateWorldValues() and then use all getWOrld...() with
	 *            check false.
	 */
	public Vector2 getWorldShear(boolean check)
	{
		if (check)
		{
			updateWorldValues();
		}
		return worldShear;
	}

	/**
	 * @return The current world rotation;
	 * @param check
	 *            if true, will check all parent transforms and self for any changes. It is best to only do check if you
	 *            know the world values have changed. Running a getWorld..() function with check true, will check and
	 *            update all world values. It is good to run a updateWorldValues() and then use all getWOrld...() with
	 *            check false.
	 */
	public float getWorldRotation(boolean check)
	{
		if (check)
		{
			updateWorldValues();
		}
		return worldRotation;
	}

	/**
	 * @return The current world z position. <br>
	 *         world z position does not take scale into any affect.
	 * @param check
	 *            if true, will check all parent transforms and self for any changes. It is best to only do check if you
	 *            know the world values have changed. Running a getWorld..() function with check true, will check and
	 *            update all world values. It is good to run a updateWorldValues() and then use all getWOrld...() with
	 *            check false.
	 */
	public float getWorldZPos(boolean check)
	{
		if (check)
		{
			updateWorldValues();
		}
		return worldZpos;
	}

	/**
	 * Set's an object's local position to match the desired world position based on the parent's current position
	 * 
	 * @param x
	 * @param y
	 */
	public void setWorldPosition(float x, float y)
	{
		// need to use a tmp vector2, because if updating world values, need accurate position vector
		position.set(worldToLocalPosition(tmpV2.set(x, y), true));
	}

	/**
	 * Set's an object's local position to match the desired world position based on the parent's current position
	 * 
	 * @param worldPosition
	 */
	public void setWorldPosition(Vector2 worldPosition)
	{
		setWorldPosition(worldPosition.x, worldPosition.y);
	}

	/**
	 * Set's an object's local origin to match the render relative given position, optionally maintains position
	 * 
	 * @param x
	 * @param y
	 * @param maintainPosition
	 *            of self and children
	 */
	public void setOriginFromRender(float x, float y, boolean maintainPosition)
	{
		setWorldOrigin(renderToWorldPosition(new Vector2(x, y), true),
				maintainPosition);
	}

	/**
	 * Set's an object's origin by using local position given, optionally maintains position
	 * 
	 * @param x
	 * @param y
	 * @param maintainPosition
	 *            of self and children
	 */
	public void setOrigin(float x, float y, boolean maintainPosition)
	{
		if (!maintainPosition)
		{
			originRenderMesh.set(x, y);
		}
		else
		{
			setWorldOrigin(localToWorldPosition(new Vector2(x, y), true), true);
		}
	}

	/**
	 * Set's an object's local origin to match the desired world position, takes into consideration of rotation.
	 * 
	 * @param x
	 *            world
	 * @param y
	 *            world
	 * @param maintainPosition
	 *            if true, will modify position to keep the object in same visual position, it will also offset the
	 *            children so they stay in same position as well
	 */
	public void setWorldOrigin(float x, float y, boolean maintainPosition)
	{
		Vector2 tmp1 = new Vector2(x, y);

		updateWorldValues();

		Vector2 original = null;
		if (maintainPosition)
		{
			original = new Vector2(getWorldRenderPosition(false));
		}

		originRenderMesh.set(worldToRenderPosition(tmp1, false));

		if (maintainPosition)
		{
			// changed world render position
			tmp1.set(original.sub(getWorldRenderPosition(true)));

			// compensate by moving parent's position
			setWorldPosition(original.set(tmp1).add(getWorldPosition(true)));

			// maintain child positions
			for (LttlTransform child : getChildren())
			{
				child.setWorldPosition(original.set(
						child.getWorldPosition(true)).sub(tmp1));
			}
		}
	}

	/**
	 * Set's an object's local origin to match the desired world origin position.
	 * 
	 * @param p_worldOrigin
	 */
	public void setWorldOrigin(Vector2 p_worldOrigin, boolean maintainPosition)
	{
		setWorldOrigin(p_worldOrigin.x, p_worldOrigin.y, maintainPosition);
	}

	public void setWorldzPos(float p_worldZPos)
	{
		zPos = worldToLocalZPos(p_worldZPos, true);
	}

	/**
	 * Set's world rotation despite the parent's rotation.
	 * 
	 * @param p_worldRotation
	 */
	public void setWorldRotation(float p_worldRotation)
	{
		rotation = worldToLocalRotation(p_worldRotation, true);
	}

	/**
	 * Set's the world scale of the object despite the scale of the parent.
	 * 
	 * @param x
	 * @param y
	 */
	public void setWorldScale(float x, float y)
	{
		worldToLocalScale(scale.set(x, y), true);
	}

	/**
	 * Set's the world shear of the object despite the shear of the parent.
	 * 
	 * @param p_worldShear
	 */
	public void setWorldShear(Vector2 p_worldShear)
	{
		setWorldShear(p_worldShear.x, p_worldShear.y);
	}

	/**
	 * Set's the world shear of the object despite the shear of the parent.
	 * 
	 * @param x
	 * @param y
	 */
	public void setWorldShear(float x, float y)
	{
		worldToLocalShear(shear.set(x, y), true);
	}

	/**
	 * Set's the world scale of the object despite the scale of the parent.
	 * 
	 * @param p_worldScale
	 */
	public void setWorldScale(Vector2 p_worldScale)
	{
		setWorldScale(p_worldScale.x, p_worldScale.y);
	}

	/**
	 * Refreshes the object's transform (rotation, position, zPosition, alpha, and scale), to produce accurate world
	 * values 1) It is called by the main loop's stage function in a hiearchy order.<br>
	 * 2) It is called by when a get function is called for a transform value. This will also be called with the highest
	 * most parent first.
	 * 
	 * @param forceUpdate
	 *            if true, forces an update of local and world transforms regardless of change. This is mostly used on
	 *            transform creation first time.
	 */
	void updateTransforms(boolean forceUpdate)
	{
		// only update local transform if local changes were made or if it is suppose to be
		// ignoringCameraTransformations and camera has changed
		boolean localTransformChange = false;
		if (forceUpdate || !position.equals(oldPosition)
				|| rotation != oldRotation
				|| !scale.equals(oldScale)
				|| !shear.equals(oldShear)
				|| zPos != oldzPos
				|| !originRenderMesh.equals(oldOrigin)
				|| (getParent() == null && lastParentId != -1)
				|| (getParent() != null && lastParentId != getParent().getId())
				// if parentTransformChanged, only need to update if this transform has some rotation or shear, since
				// those are the only parts that is dependent on the parent transform
				|| (getParent() != null && parentTransformChanged && (rotation != 0 || !shear
						.equals(LttlMath.Zero))))
		{
			localTransformChange = true;
			updateLocalTransform();
			cleanseLocalTransformChanges();
		}

		// update world transform if local changes were made or if parent transform changed
		if (forceUpdate || parentTransformChanged || localTransformChange)
		{

			LttlProfiler.transformWorldUpdates.add();

			modifiedId++;
			updateWorldTransform();
			updateWorldValuesInternal();

			// mark all children to force them to update their transform because the parent's world transform did
			// these LttlTransform's updateTransforms() will be called by LoopManager, this just makes sure they update
			// too
			if (children != null)
			{
				for (LttlTransform c : children)
				{
					c.parentTransformChanged = true;
				}
			}
		}

		// reset
		parentTransformChanged = false;
	}

	/**
	 * This forces all parents to check for transform updates and through hiearchy update, allowing this transform to
	 * have the most accurate world values
	 */
	void updateUpHiearchyTransform()
	{
		// update parent first
		if (getParent() != null)
		{
			getParent().updateUpHiearchyTransform();
		}
		updateTransforms(false);
	}

	private void updateLocalTransform()
	{
		// it's assumed that parent world transforms and values have been updated by this point

		LttlProfiler.transformLocalUpdates.add();

		// generate local render transform, which includes origin (if any)
		LttlMath.GenerateTransormMatrix(position, scale, originRenderMesh,
				rotation, false, shear, getParent(), localRenderTransform);

		// if origin offset exists, then generate a new transform matrix for localTransform with origin at zero
		// otherwise use the localRenderTransform because they are the same
		if (originRenderMesh.x != 0 || originRenderMesh.y != 0)
		{
			LttlMath.GenerateTransormMatrix(position, scale, Vector2.Zero,
					rotation, false, shear, getParent(), localTransform);
		}
		else
		{
			localTransform.set(localRenderTransform);
		}
	}

	/**
	 * Resets local values so they do not show any changes
	 */
	private void cleanseLocalTransformChanges()
	{
		oldPosition.set(position);
		oldScale.set(scale);
		oldShear.set(shear);
		oldOrigin.set(originRenderMesh);
		oldRotation = rotation;
		oldzPos = zPos;
		lastParentId = (getParent() == null) ? -1 : getParent().getId();
	}

	/**
	 * only ran when there is a change
	 */
	private void updateWorldTransform()
	{
		if (getParent() != null)
		{
			worldTransform.set(getParent().worldTransform);
			worldTransform.mul(localTransform);

			worldRenderTransform.set(getParent().worldTransform);
			worldRenderTransform.mul(localRenderTransform);
		}
		else
		{
			worldTransform.set(localTransform);
			worldRenderTransform.set(localRenderTransform);
		}

		if (r() != null)
		{
			r().onWorldTransformChange();
		}
	}

	/**
	 * Returns THE world transform, do not modify this.
	 * 
	 * @param check
	 *            if true, will check all parent transforms and self for any changes. It is best to only do check if you
	 *            know the world values have changed. Running a getWorld..() function with check true, will check and
	 *            update all world values. It is good to run a updateWorldValues() and then use all getWOrld...() with
	 *            check false.
	 * @return
	 */
	public Matrix3 getWorldTransform(boolean check)
	{
		if (check)
		{
			updateUpHiearchyTransform();
		}
		return worldTransform;
	}

	public Matrix3 getWorldRenderTransform(boolean check)
	{
		if (check)
		{
			updateUpHiearchyTransform();
		}
		return worldRenderTransform;
	}

	/**
	 * updates world matrix based on camera
	 */
	public void updateWorldRenderMatrix()
	{
		if (r().worldRenderMatrix == null)
		{
			r().worldRenderMatrix = new Matrix4();
		}

		renderer().worldRenderMatrix.set(
				Lttl.loop.getCurrentRenderingCamera().worldMatrix).mul(
				worldTransform4.set(worldRenderTransform));
	}

	private void updateWorldValuesInternal()
	{
		// assumed at this point that all parent world values and transforms are updated

		// calculate world scale, rotation, and z position from parent because it's faster than using transform
		// (matrices)
		if (getParent() != null)
		{
			// if has parent

			// scale
			worldScale.set(scale);
			worldScale.scl(getParent().worldScale);

			// shear
			worldShear.set(shear);
			worldShear.add(getParent().worldShear);

			// rotation
			worldRotation = rotation + getParent().worldRotation;

			// z position
			worldZpos = zPos + getParent().worldZpos;
		}
		else
		{
			// no parent

			// scale
			worldScale.set(scale);

			// shear
			worldShear.set(shear);

			// rotation
			worldRotation = rotation;

			// z position
			worldZpos = zPos;
		}

		// position (derive from world transform matrices)
		worldTransform.getTranslation(worldPosition);
		worldRenderTransform.getTranslation(worldRenderPosition);
	}

	// //PARENTS & CHILDREN////

	/**
	 * Returns the direct parent, will return null if no parent
	 * 
	 * @return
	 */
	public LttlTransform getParent()
	{
		return parentTransform;
	}

	/**
	 * @return the highest parent
	 */
	public LttlTransform getHighestParent()
	{
		if (getParent() != null && getParent().children != null)
		{
			return getParent().getHighestParent();
		}
		else
		{
			return this;
		}
	}

	/**
	 * Checks to see if this transform is an ancestor (parent or greater) of the given transform
	 * 
	 * @param potentialDescendent
	 * @return
	 */
	public boolean isAncestor(LttlTransform potentialDescendent)
	{
		return potentialDescendent.isDescendent(this);
	}

	/**
	 * Checks to see if this transform is a descendent (child or grandchild, etc) of the given transform
	 * 
	 * @param potentialAncestor
	 * @return
	 */
	public boolean isDescendent(LttlTransform potentialAncestor)
	{
		return checkAncestor(potentialAncestor);
	}

	private boolean checkAncestor(LttlTransform potentialAncestor)
	{
		if (getParent() == null) { return false; }
		if (this == potentialAncestor) { return false; }

		if (getParent() == potentialAncestor)
		{
			return true;
		}
		else
		{
			return getParent().checkAncestor(potentialAncestor);
		}
	}

	/**
	 * Returns an unmodifieable list of all of this transform's children transforms.
	 * 
	 * @return
	 */
	public List<LttlTransform> getChildren()
	{
		return Collections.unmodifiableList(children);
	}

	/**
	 * Returns the first child with the specified name. Will return null if no child with that name.<br>
	 * NOTE: It would be good to save this as a reference since this is an an expensive search.<br>
	 * <b>DO NOT RUN EVERY FRAME!</b>
	 * 
	 * @param name
	 * @return
	 */
	public LttlTransform getChild(String name)
	{
		if (children != null)
		{
			for (LttlTransform lc : children)
			{
				if (lc.name.equals(name)) { return lc; }
			}
		}
		return null;
	}

	/**
	 * Sets the current parent of this transform. Null will set it to no parent. Parents must be in the same scene.
	 * 
	 * @param parent
	 *            the transform of desired parent, use null to set it as no parent (top-level transform).
	 * @param maintainWorldValues
	 *            if true, the child's world position, rotation, scale will be maintained, otherwise it's local values
	 *            will remain the same
	 */
	public void setParent(LttlTransform parent, boolean maintainWorldValues)
	{
		// some checks
		if (parent == this)
		{
			Lttl.Throw("Can't set parent to self.");
			return;
		}
		else if (parent == this.getParent())
		{
			// same parent
			return;
		}
		else if (parent != null && !this.isInSameScene(parent))
		{
			Lttl.Throw("Can't set parent from another scene.  Solution: copy to same scene first.");
			return;
		}

		// if parent is a descendent then need to make the descendent a child of this transform's parent
		if (parent != null && parent.isDescendent(this))
		{
			parent.setParent(this.getParent(), true);
		}

		// update world values and capture them if maintainng world values
		Vector2 origWorldPos = null;
		Vector2 origWorldScale = null;
		float origWorldRot = 0;
		float origWorldZpos = 0;
		if (maintainWorldValues)
		{
			updateWorldValues();
			origWorldPos = new Vector2(getWorldPosition(false));
			origWorldScale = new Vector2(getWorldScale(false));
			origWorldRot = getWorldRotation(false);
			origWorldZpos = getWorldZPos(false);
		}

		// get scene reference
		LttlSceneCore scene = getSceneCore();

		// remove it from current parent (if any)
		if (this.getParent() != null)
		{
			this.getParent().children.remove(this);
		}
		else
		// it must not have a parent and be a top-level transform, so remove from scene's transformHiearchy
		{
			scene.transformHiearchy.remove(this);
		}

		// add transform to the new parent
		if (parent != null)
		{
			parent.children.add(this);
		}
		// if the new parent none, then add it to the scene's top-leve transformHiearchy
		else
		{
			scene.transformHiearchy.add(this);
		}

		// remove transform from GUI tree, now that it has been removed from parent or scene hiearchy
		// dont do this if it is being destroyed, since it was already removed
		if (Lttl.game.inEditor() && !destroyCompProcessed)
		{
			Lttl.editor.getGui().getSelectionController()
					.removeTransform(this, scene);
		}

		// define parent
		this.parentTransform = parent;

		// Add transform to GUI based on new parent
		if (Lttl.game.inEditor())
		{
			Lttl.editor.getGui().getSelectionController()
					.addTransform(this, scene);
		}

		if (maintainWorldValues)
		{
			// set world values to the originals defined above
			worldToLocalPosition(position.set(origWorldPos), false);
			worldToLocalScale(scale.set(origWorldScale), false);
			rotation = worldToLocalRotation(origWorldRot, false);
			zPos = worldToLocalZPos(origWorldZpos, false);
		}
	}

	/**
	 * Sets another transform as a child of this transform, if it already has a parent, then removes itself from it
	 * first.
	 * 
	 * @param child
	 * @param maintainWorldValues
	 *            if true, the child's world position, rotation, scale will be maintained
	 * @return the child
	 */
	public LttlTransform setChild(LttlTransform child,
			boolean maintainWorldValues)
	{
		child.setParent(this, maintainWorldValues);
		return child;
	}

	/**
	 * Generates a new transform in scene and then adds it as a child to this transform.
	 */
	public LttlTransform addNewChild()
	{
		return addNewChild("");
	}

	/**
	 * Generates a new transform in scene and then adds it as a child to this transform.
	 * 
	 * @param name
	 */
	public LttlTransform addNewChild(String name)
	{
		LttlTransform newLt = getScene().addNewTransform(name);
		newLt.setParent(this, false);
		return newLt;
	}

	/**
	 * Makes a copy of transform and then adds it as a child. Optionally can maintain any references that were made
	 * between components in the source tree. Maintains child's world position.
	 * 
	 * @param transform
	 * @param matchLocalTreeReferences
	 * @return
	 */
	public LttlTransform addTransformCopyAsChild(LttlTransform transform,
			boolean matchLocalTreeReferences)
	{
		return this.setChild(transform.duplicate(matchLocalTreeReferences),
				true);
	}

	/**
	 * Creates a full object based on ObjectType and adds as child.
	 * 
	 * @param type
	 * @param name
	 * @return
	 */
	public LttlTransform addTransformTypeAsChild(ObjectType type, String name)
	{
		LttlTransform transform = LttlObjectFactory.AddObject(type, getScene(),
				name);
		transform.setParent(this, false);
		return transform;
	}

	/**
	 * Checks if transform is in same scene.
	 * 
	 * @param transform
	 * @return
	 */
	public boolean isInSameScene(LttlTransform transform)
	{
		if (transform == null) Lttl.Throw();

		return transform.getSceneId() == this.getSceneId();
	}

	@Override
	LttlSceneCore getSceneCore()
	{
		if (sceneRef == null)
		{
			sceneRef = Lttl.scenes.get(getSceneId());
		}
		return sceneRef.getRef();
	}

	/**
	 * Creates a copy of the transform tree (children and components, recursively) at the same hiearchy level (same
	 * parent). All same scene and world references will be maintained.
	 * 
	 * @param matchLocalTreeReferences
	 *            if true, if there are references of components within the copying transform hierarchy, then it will
	 *            maintain those after the copy
	 * @return the new transform
	 */
	public LttlTransform duplicate(boolean matchLocalTreeReferences)
	{
		LttlTransform newTransform = getScene().addTransformCopy(this, true,
				matchLocalTreeReferences);
		newTransform.setParent(this.getParent(), true);
		return newTransform;
	}

	/**
	 * Finds first child (and optionally deeper descendants) of this transform with this name. This is expensive and
	 * result should be saved.
	 * 
	 * @param name
	 * @param firstDescendants
	 *            true means it only searches it's direct children (one level)
	 * @return
	 */
	public LttlTransform findChild(String name, boolean firstDescendants)
	{
		return getScene().findTransform(name, this, firstDescendants);
	}

	/**
	 * Finds all the children (and optionally deeper descendants) on this transform with this name. This is expensive
	 * and result should be saved.
	 * 
	 * @param name
	 * @param firstDescendants
	 *            true means it only searches it's direct children (one level)
	 * @return
	 */
	public ArrayList<LttlTransform> findChildren(String name,
			boolean firstDescendants)
	{
		return getScene().findTransforms(name, this, firstDescendants);
	}

	/**
	 * Searches up the ancestor hiearchy and tries to find one with specified name.
	 * 
	 * @param name
	 * @return
	 */
	public LttlTransform findAncestor(String name)
	{
		if (getParent() != null)
		{
			if (getParent().getName().equals(name)) { return getParent(); }
			return getParent().findAncestor(name);
		}
		return null;
	}

	int getSceneId()
	{
		return sceneId;
	}

	/**
	 * Converts the given world position vector so it is relative to this transform's local and updates the values in
	 * the given vector.<br>
	 * This is NOT the same local as a child, for that use worldtoChildPosition()
	 * 
	 * @param worldPos
	 * @param check
	 *            should update world values if there are any changes
	 * @return worldPos with new values
	 */
	public Vector2 worldToLocalPosition(Vector2 worldPos, boolean check)
	{
		if (check)
		{
			updateWorldValues();
		}

		if (getParent() != null)
		{
			worldPos.mul(sharedMatrix.set(getParent().worldTransform).inv());
		}
		// if no parent, then local and world positions are same

		return worldPos;
	}

	/**
	 * Converts the given world position vector so it is relative to the worldRenderTransform (includes origin) and
	 * updates the values in the given vector.
	 * 
	 * @param worldPos
	 * @param check
	 *            should update world values if there are any changes
	 * @return worldPos with new values
	 */
	public Vector2 worldToRenderPosition(Vector2 worldPos, boolean check)
	{
		if (check)
		{
			updateWorldValues();
		}

		return worldPos.mul(sharedMatrix.set(worldRenderTransform).inv());
	}

	/**
	 * Converts the given world position vector so it is relative to a child's transform and updates the values in the
	 * given vector.
	 * 
	 * @param worldPos
	 * @param check
	 *            should update world values if there are any changes
	 * @return worldPos with new values
	 */
	public Vector2 worldToChildPosition(Vector2 worldPos, boolean check)
	{
		if (check)
		{
			updateWorldValues();
		}

		return worldPos.mul(sharedMatrix.set(worldTransform).inv());
	}

	/**
	 * Converts a position as if it was a child of this transform. (0,0) is the same position as this LttlTransform
	 * 
	 * @param childPos
	 *            vector to convert to world position and update values of
	 * @param check
	 *            should if there are any changes to transform tree
	 * @return childPos with new values
	 */
	public Vector2 childToWorldPosition(Vector2 childPos, boolean check)
	{
		if (check)
		{
			updateWorldValues();
		}

		return childPos.mul(worldTransform);
	}

	/**
	 * Converts the given render relative position vector so it is relative to the world and updates the values in the
	 * given vector.<br>
	 * This is good if you want to know know the positions of anything that uses the worldRenderTransform (meshes and
	 * colliders) - takes into consideration (scale, position, rotation, and origin)
	 * 
	 * @param renderPos
	 * @param check
	 *            should update world values if there are any changes
	 * @return worldPos with new values
	 */
	public Vector2 renderToWorldPosition(Vector2 renderPos, boolean check)
	{
		if (check)
		{
			updateWorldValues();
		}

		return renderPos.mul(worldRenderTransform);
	}

	// UNTESTED never had a need for it
	/**
	 * Converts the given render relative (uses renderTransform, which includes origin) position vector so it is
	 * relative to this transform's local and updates the values in the given vector.<br>
	 * 
	 * @param renderPos
	 * @param check
	 *            should update world values if there are any changes
	 * @return worldPos with new values
	 */
	public Vector2 renderToLocalPosition(Vector2 renderPos, boolean check)
	{
		if (check)
		{
			updateWorldValues();
		}

		return renderPos.mul(localRenderTransform).add(position);
	}

	// UNTESTED never had a need for it
	/**
	 * Converts the given local position vector so it is relative to the render local and updates the values in the
	 * given vector.<br>
	 * 
	 * @param localPos
	 * @param check
	 *            should update world values if there are any changes
	 * @return worldPos with new values
	 */
	public Vector2 localToChildPosition(Vector2 localPos, boolean check)
	{
		if (check)
		{
			updateWorldValues();
		}

		return localPos.mul(sharedMatrix.set(localRenderTransform).inv());
	}

	// UNTESTED never had a need for it
	/**
	 * Converts the local position vector so it is relative to the world and updates the values in the vector.
	 * 
	 * @param localPos
	 *            vector to convert and update values of
	 * @param check
	 *            should update world values if there are any changes
	 * @return localPos with new values
	 */
	public Vector2 localToWorldPosition(Vector2 localPos, boolean check)
	{
		if (check)
		{
			updateWorldValues();
		}

		// if has parent then convert localPos based on parent transform
		if (getParent() != null)
		{
			localPos.mul(getParent().worldTransform);
		}

		// if no parent, then local and world positions are same
		// then add result to the world position of this transform
		localPos.add(getWorldPosition(false));

		return localPos;
	}

	/**
	 * Converts a world scale vector so it is relative to this transform's local and updates the values in the given
	 * vector.
	 * 
	 * @param p_worldScale
	 * @param check
	 *            should update world values if there are any changes
	 * @return worldScale with new values
	 */
	public Vector2 worldToLocalScale(Vector2 p_worldScale, boolean check)
	{
		if (getParent() != null)
		{
			Vector2 v = getParent().getWorldScale(check);
			p_worldScale.scl(1 / v.x, 1 / v.y);
		}
		return p_worldScale;
	}

	/**
	 * Converts a world shear vector so it is relative to this transform's local and updates the values in the given
	 * vector.
	 * 
	 * @param p_worldShear
	 * @param check
	 *            should update world values if there are any changes
	 * @return worldScale with new values
	 */
	public Vector2 worldToLocalShear(Vector2 p_worldShear, boolean check)
	{
		if (getParent() != null)
		{
			Vector2 v = getParent().getWorldShear(check);
			p_worldShear.sub(v);
		}
		return p_worldShear;
	}

	/**
	 * Converts a local scale vector so it is relative to the world and updates the values in the given vector.
	 * 
	 * @param localScale
	 *            vector to convert and update the values of
	 * @param check
	 *            should update world values if there are any changes
	 * @return localScale with new values
	 */
	public Vector2 localToWorldScale(Vector2 localScale, boolean check)
	{
		if (getParent() != null)
		{
			localScale.scl(getParent().getWorldScale(check));
		}
		return localScale;
	}

	/**
	 * Converts world rotation so it is relative to this transform's local.
	 * 
	 * @param worldRot
	 * @param check
	 * @return
	 */
	public float worldToLocalRotation(float worldRot, boolean check)
	{
		if (getParent() != null)
		{
			worldRot = worldRot - getParent().getWorldRotation(check);
		}

		return worldRot;
	}

	/**
	 * Converts local rotation so it is relative to the world.
	 * 
	 * @param localRot
	 * @param check
	 * @return
	 */
	public float localToWorldRotation(float localRot, boolean check)
	{
		if (getParent() != null)
		{
			localRot = localRot + getParent().getWorldRotation(check);
		}

		return localRot;
	}

	/**
	 * Converts world z position so it is relative to this transform's local.
	 * 
	 * @param worldZPos
	 * @param check
	 * @return
	 */
	public float worldToLocalZPos(float worldZPos, boolean check)
	{
		if (getParent() != null)
		{
			worldZPos = worldZPos - getParent().getWorldZPos(check);
		}

		return worldZPos;
	}

	/**
	 * Converts local z position so it is relative to the world
	 * 
	 * @param localZPos
	 * @param check
	 * @return
	 */
	public float localToWorldZPos(float localZPos, boolean check)
	{
		if (getParent() != null)
		{
			localZPos = localZPos + getParent().getWorldZPos(check);
		}

		return localZPos;
	}

	// *********************//
	// ******* TWEENS ******//
	// *********************//

	/**
	 * Tweens position of transform to target.
	 * 
	 * @param targetPos
	 * @param duration
	 * @return
	 */
	public Tween tweenPosTo(Vector2 targetPos, float duration)
	{
		return tweenPosTo(targetPos.x, targetPos.y, duration);
	}

	/**
	 * Tweens position of transform to target.
	 * 
	 * @param targetX
	 * @param targetY
	 * @param duration
	 * @return
	 */
	public Tween tweenPosTo(float targetX, float targetY, float duration)
	{
		return Lttl.tween.tweenVector2To(this, position, targetX, targetY,
				duration);
	}

	/**
	 * Creates a parallel timeline with two tweens (one for each position property). You can use Timeline.getChildren()
	 * to modify them further, or write it yourself.
	 * 
	 * @param targetX
	 * @param easeX
	 * @param targetY
	 * @param easeY
	 * @param duration
	 * @return
	 */
	public Timeline tweenPosArcTo(float targetX, EaseType easeX, float targetY,
			EaseType easeY, float duration)
	{
		return tweenParallel().push(
				Lttl.tween.tweenVector2PropTo(this, this.position, 0, targetX,
						duration)).push(
				Lttl.tween.tweenVector2PropTo(this, this.position, 1, targetY,
						duration));
	}

	/**
	 * Tweens scale of transform to target.
	 * 
	 * @param targetScale
	 * @param duration
	 * @return
	 */
	public Tween tweenScaleTo(Vector2 targetScale, float duration)
	{
		return tweenScaleTo(targetScale.x, targetScale.y, duration);
	}

	/**
	 * Tweens scale of transform to target.
	 * 
	 * @param targetX
	 * @param targetY
	 * @param duration
	 * @return
	 */
	public Tween tweenScaleTo(float targetX, float targetY, float duration)
	{
		return Lttl.tween.tweenVector2To(this, scale, targetX, targetY,
				duration);
	}

	/**
	 * Tweens origin of transform to target.
	 * 
	 * @param targetOrigin
	 * @param duration
	 * @return
	 */
	public Tween tweenOriginTo(Vector2 targetOrigin, float duration)
	{
		return tweenOriginTo(targetOrigin.x, targetOrigin.y, duration);
	}

	/**
	 * Tweens origin of transform to target.
	 * 
	 * @param targetX
	 * @param targetY
	 * @param duration
	 * @return
	 */
	public Tween tweenOriginTo(float targetX, float targetY, float duration)
	{
		return Lttl.tween.tweenVector2To(this, originRenderMesh, targetX,
				targetY, duration);
	}

	/**
	 * Tweens rotation of transform to target.
	 * 
	 * @param targetRotation
	 * @param duration
	 * @return
	 */
	public Tween tweenRotTo(float targetRotation, float duration)
	{
		return Tween.to(this, new TweenGetterSetter()
		{
			@Override
			public void set(float[] values)
			{
				LttlTransform.this.rotation = values[0];
			}

			@Override
			public float[] get()
			{
				return new float[]
				{ LttlTransform.this.rotation };
			}
		}, duration).target(targetRotation);
	}

	/**
	 * Tweens Zpos of transform to target.
	 * 
	 * @param targetZPos
	 * @param duration
	 * @return
	 */
	public Tween tweenZPosTo(float targetZPos, float duration)
	{
		return Tween.to(this, new TweenGetterSetter()
		{
			@Override
			public void set(float[] values)
			{
				LttlTransform.this.zPos = values[0];
			}

			@Override
			public float[] get()
			{
				return new float[]
				{ LttlTransform.this.zPos };
			}
		}, duration).target(targetZPos);
	}

	@Override
	public void debugDraw()
	{
		if (drawOrigin)
		{
			Lttl.debug.drawCircle(getWorldPosition(true),
					LttlDebug.RADIUS_SMALL * Lttl.debug.eF(), Color.GREEN);
		}
	}

	/**
	 * Checks if a class can be added to this transform
	 * 
	 * @param clazz
	 * @return
	 */
	public boolean canAddComponentType(Class<? extends LttlComponent> clazz)
	{
		// a LttlTransform can not add another LttlTransform
		if (clazz == LttlTransform.class)
		{
			Lttl.logNote("Adding Component: Can't add another transform on "
					+ getName() + ".");
			return false;
		}

		if (Modifier.isAbstract(clazz.getModifiers()))
		{
			Lttl.logNote("Adding Component: Can't add abstract component class "
					+ clazz.getSimpleName());
			return false;
		}

		// check if there is any limitOne on this class or any supers
		Class<? extends LttlComponent> currentClass = clazz;
		while (currentClass != LttlComponent.class)
		{
			// check if limit one annotation is present
			if (currentClass.isAnnotationPresent(ComponentLimitOne.class))
			{
				// check if the transform already has one of these components, including sub classes
				if (getComponent(currentClass, true) != null)
				{
					Lttl.logNote("Adding Component: Can't add "
							+ clazz.getName()
							+ " component on "
							+ getName()
							+ " because there is a limit of one on super class "
							+ currentClass.getName());
					return false;
				}
			}

			// go to next super class
			currentClass = (Class<? extends LttlComponent>) currentClass
					.getSuperclass();
		}

		// lastly check required components
		ArrayList<Class<? extends LttlComponent>> requiredComponents = LttlComponent
				.getRequiredComponents(clazz);
		if (requiredComponents.size() == 0) { return true; }
		for (int i = 0; i < requiredComponents.size(); i++)
		{
			if (getComponent(requiredComponents.get(i), true) == null)
			{
				Lttl.logNote("Adding Component: Can't add "
						+ clazz.getSimpleName() + " on " + getName()
						+ " component because missing at least component "
						+ requiredComponents.get(i).getSimpleName());
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns all the points that make up the selection polygon bounding rect for this transform and all it's
	 * decendents. Be sure all world positions are updated when running this, may need to run updateWorldValuesTree()
	 * before hand.
	 * 
	 * @param includePositionPoint
	 *            if true, then each transform will also add it's transform position regardless if it has a mesh or not
	 * @return
	 */
	public float[] getSelectionBoundingRectPointsTree(
			boolean includePositionPoint)
	{
		// create an array with initial capacity to fit all the children and self into it (4 points each), or 5 if
		// includePositionPoint
		FloatArray array = new FloatArray(8 * (children.size() + 1)
				+ ((includePositionPoint) ? 2 * (children.size() + 1) : 0));

		// add position
		if (includePositionPoint)
		{
			array.add(getWorldPosition(true).x);
			array.add(getWorldPosition(false).y);
		}

		// add self bounding rect points
		float[] transformedBoundingRect = getSelectionTransformedBoundingRect();
		if (transformedBoundingRect != null)
		{
			array.addAll(transformedBoundingRect);
		}

		// add children bounding rect points
		for (LttlTransform lt : children)
		{
			array.addAll(lt
					.getSelectionBoundingRectPointsTree(includePositionPoint));
		}

		return array.toArray();
	}

	@GuiButton(order = 0)
	private void editTags()
	{
		tagBit = Lttl.editor.getGui().editTagBitDialog(tagBit);
		Lttl.editor.getGui().getPropertiesController().draw(false);
	}

	@GuiButton
	private void setOrigin()
	{
		Lttl.editor.getInput().setVector2FromEditorClick(new LttlCallback()
		{
			@Override
			public void callback(int id, Object... objects)
			{
				LttlTransform.this.setWorldOrigin((Vector2) objects[0], true);
			}
		});
	}

	@GuiButton(name = "Left", group = "Origin,Top")
	private void setOriginMeshTopLeft()
	{
		setOriginShared(1);
	}

	@GuiButton(name = "Center", group = "Origin,Top")
	private void setOriginMeshTopCenter()
	{
		setOriginShared(2);
	}

	@GuiButton(name = "Right", group = "Origin,Top")
	private void setOriginMeshTopRight()
	{
		setOriginShared(3);
	}

	@GuiButton(name = "Left", group = "Origin,Middle")
	private void setOriginMeshMiddleLeft()
	{
		setOriginShared(8);
	}

	@GuiButton(name = "Center", group = "Origin,Middle")
	private void setOriginMeshCenter()
	{
		setOriginShared(0);
	}

	@GuiButton(name = "Right", group = "Origin,Middle")
	private void setOriginMeshMiddleRight()
	{
		setOriginShared(4);
	}

	@GuiButton(name = "Left", group = "Origin,Bottom")
	private void setOriginMeshBottomLeft()
	{
		setOriginShared(7);
	}

	@GuiButton(name = "Center", group = "Origin,Bottom")
	private void setOriginMeshBottomCenter()
	{
		setOriginShared(6);
	}

	@GuiButton(name = "Right", group = "Origin,Bottom")
	private void setOriginMeshBottomRight()
	{
		setOriginShared(5);
	}

	private void setOriginShared(int mode)
	{
		if (r() == null || r().getMesh() == null)
		{
			Lttl.logNote("Set Origin: Need renderer and mesh.");
			return;
		}

		Rectangle rect = r().getMesh().getBoundingRectCached();
		float x = 0;
		float y = 0;
		switch (mode)
		{
		// center
			case 0:
				x = rect.x + rect.width / 2f;
				y = rect.y + rect.height / 2f;
				break;
			// top left
			case 1:
				x = rect.x;
				y = rect.y + rect.height;
				break;
			// top center
			case 2:
				x = rect.x + rect.width / 2f;
				y = rect.y + rect.height;
				break;
			// top right
			case 3:
				x = rect.x + rect.width;
				y = rect.y + rect.height;
				break;
			// middle right
			case 4:
				x = rect.x + rect.width;
				y = rect.y + rect.height / 2f;
				break;
			// bottom right
			case 5:
				x = rect.x + rect.width;
				y = rect.y;
				break;
			// bottom center
			case 6:
				x = rect.x + rect.width / 2f;
				y = rect.y;
				break;
			// bottom left
			case 7:
				x = rect.x;
				y = rect.y;
				break;
			// middle left
			case 8:
				x = rect.x;
				y = rect.y + rect.height / 2f;
				break;
		}

		// offset origin
		setOriginFromRender(x, y, true);
	}

	/**
	 * Returns the number of meshes in this transform tree (self and descendants)
	 * 
	 * @return
	 */
	public int getMeshCountInTree()
	{
		int count = 0;

		// check self
		if (this.r() != null && this.r().getMesh() != null)
		{
			count++;
		}

		// check chdilren
		for (LttlTransform lt : children)
		{
			count += lt.getMeshCountInTree();
		}

		return count;
	}

	/**
	 * Checks if self or any descendants (including components) have any references in other scenes
	 * 
	 * @param searchOwnScene
	 *            should search it's own scene too
	 * @param fieldsMode
	 * @return the number of references, 0 is none
	 */
	public int checkRefDependencyTree(boolean searchOwnScene,
			FieldsMode fieldsMode)
	{
		ArrayList<LttlScene> scenes = new ArrayList<LttlScene>();
		for (LttlScene scene : Lttl.scenes.getAllLoaded(true))
		{
			if (searchOwnScene || scene != getScene())
			{
				scenes.add(scene);
			}
		}
		return ComponentHelper.checkDependencies(getComponentsInTree(), scenes,
				fieldsMode);
	}

	/**
	 * updates mesh if has generator and aa settings enabled, this for when change the scale it automatically updates AA
	 */
	void onHandleScaleDrag()
	{
		if (r() != null && r().getMesh() != null && r().generator() != null
				&& r().generator().isEnabled()
				&& r().generator().aaSettings != null)
		{
			r().generator().updateMeshAA();
		}
		for (LttlTransform child : children)
		{
			child.onHandleScaleDrag();
		}
	}

	IntMap<TweenGetterSetter> cachedTweenGetterSetters = new IntMap<TweenGetterSetter>(
			0);

	@Override
	public TweenGetterSetter getTweenGetterSetter(int animID)
	{
		// only creates the ones it needs
		if (!cachedTweenGetterSetters.containsKey(animID))
		{
			switch (animID)
			{
				case 1:
				{
					cachedTweenGetterSetters.put(animID,
							new TweenGetterSetter()
							{

								@Override
								public void set(float[] values)
								{
									zPos = values[0];
								}

								@Override
								public float[] get()
								{
									return new float[]
									{ zPos };
								}
							});
					break;
				}
				case 3:
				{
					cachedTweenGetterSetters.put(animID,
							new TweenGetterSetter()
							{

								@Override
								public void set(float[] values)
								{
									rotation = values[0];
								}

								@Override
								public float[] get()
								{
									return new float[]
									{ rotation };
								}
							});
					break;
				}
			}
		}
		TweenGetterSetter result = cachedTweenGetterSetters.get(animID, null);
		return result;
	}

	/**
	 * Modifies the order of your children, which ultimately affects the order they are updated
	 * 
	 * @param start
	 *            child at index to move
	 * @param dest
	 *            destination for child
	 */
	public void moveChildOrder(int start, int dest)
	{
		LttlHelper.MoveItemArrayList(children, start, dest);
	}

	/**
	 * Modifies the order of your components, which ultimately affects the order they are updated
	 * 
	 * @param start
	 *            component at index to move
	 * @param dest
	 *            destination for component
	 */
	public void moveComponentOrder(int start, int dest)
	{
		LttlHelper.MoveItemArrayList(components, start, dest);
	}

	/**
	 * Returns the id of the last updateWorldValues (when a change is found). This can be used to poll this transform to
	 * see if it's world values have changed.
	 * 
	 * @return
	 */
	public int getModifiedId()
	{
		return modifiedId;
	}

	/**
	 * editor and handles
	 * 
	 * @param listener
	 */
	public void addGuiListener(GuiTransformListener listener)
	{
		if (guiListeners == null)
		{
			guiListeners = new ArrayList<LttlTransform.GuiTransformListener>(1);
		}
		guiListeners.add(listener);
	}

	public void removeGuiListener(GuiTransformListener listener)
	{
		if (guiListeners == null) return;
		guiListeners.remove(listener);
	}

	/**
	 * Can be registered to a LttlTransform to receive callback when essential variables change.
	 */
	@IgnoreCrawl
	public static abstract class GuiTransformListener
	{
		boolean checkAncestorTransforms = false;

		final void process(LttlTransformChangeType changeType, boolean handle,
				LttlTransform sourceTransform)
		{
			switch (changeType)
			{
				case Origin:
					onOrigin(sourceTransform);
					break;
				case Position:
					onPosition(handle, sourceTransform);
					break;
				case Rotation:
					onRotation(handle, sourceTransform);
					break;
				case Scale:
					onScale(handle, sourceTransform);
					break;
				case Shear:
					onShear(sourceTransform);
					break;
				case Z:
					onZ(sourceTransform);
					break;
			}
			onAny(handle, sourceTransform);
		}

		/**
		 * @param checkAncestorTransforms
		 *            if true, will also callback when any parent changes
		 */
		public GuiTransformListener(boolean checkAncestorTransforms)
		{
			this.checkAncestorTransforms = checkAncestorTransforms;
		}

		public void onPosition(boolean handle, LttlTransform sourceTransform)
		{
		};

		public void onRotation(boolean handle, LttlTransform sourceTransform)
		{
		};

		public void onScale(boolean handle, LttlTransform sourceTransform)
		{
		};

		public void onOrigin(LttlTransform sourceTransform)
		{
		};

		public void onShear(LttlTransform sourceTransform)
		{
		};

		public void onZ(LttlTransform sourceTransform)
		{
		};

		public void onAny(boolean handle, LttlTransform sourceTransform)
		{
		};
	}

	void onGuiChange(LttlTransformChangeType changeType, boolean handle,
			LttlTransform sourceTransform)
	{
		if (guiListeners != null)
		{
			for (GuiTransformListener g : guiListeners)
			{
				// if sourceTransform does not equal this transform, then must have been called back form some ancestor
				// change, then only callback listeners that check ancestor transforms
				// if sourceTransform does equal this tranform, then always process
				if (sourceTransform == this
						|| (sourceTransform != this && g.checkAncestorTransforms))
				{
					g.process(changeType, handle, sourceTransform);
				}
			}
		}
		for (LttlTransform child : children)
		{
			// call on each child, pass on the source transform
			child.onGuiChange(changeType, handle, sourceTransform);
		}
	}

	@SuppressWarnings("unused")
	private void onGuiPosition()
	{
		onGuiChange(LttlTransformChangeType.Position, false, this);
	}

	@SuppressWarnings("unused")
	private void onGuiRotation()
	{
		onGuiChange(LttlTransformChangeType.Rotation, false, this);
	}

	@SuppressWarnings("unused")
	private void onGuiOrigin()
	{
		onGuiChange(LttlTransformChangeType.Origin, false, this);
	}

	@SuppressWarnings("unused")
	private void onGuiScale()
	{
		onGuiChange(LttlTransformChangeType.Scale, false, this);
	}

	@SuppressWarnings("unused")
	private void onGuiShear()
	{
		onGuiChange(LttlTransformChangeType.Shear, false, this);
	}

	@SuppressWarnings("unused")
	private void onGuiZ()
	{
		onGuiChange(LttlTransformChangeType.Z, false, this);
	}

	@Override
	public void onDisable()
	{
		processEnableDisableCallback(ComponentCallBackType.onDisable);
	}

	@Override
	public void onEditorDisable()
	{
		processEnableDisableCallback(ComponentCallBackType.onDisable);
	}

	@Override
	public void onEnable()
	{
		processEnableDisableCallback(ComponentCallBackType.onEnable);
	}

	@Override
	public void onEditorEnable()
	{
		processEnableDisableCallback(ComponentCallBackType.onEnable);
	}

	private void processEnableDisableCallback(ComponentCallBackType type)
	{
		for (LttlComponent comp : components)
		{
			// if it is enabled on the component level, then it mirrors this transform, so callback the same
			if (comp.isEnabledSelf())
			{
				ComponentHelper.processCallBack(comp, type);
			}

		}
		for (LttlComponent child : children)
		{
			if (child.isEnabledSelf())
			{
				ComponentHelper.processCallBack(child, type);
			}

		}
	}

	/**
	 * Returns the Global enabled state of this transform by also checking it's parents
	 */
	@Override
	public boolean isEnabled()
	{
		if (getParent() == null)
		{
			return isEnabled;
		}
		else
		{
			return isEnabled && getParent().isEnabled();
		}
	}

	/**
	 * Returns if this transform or any ancestor transform is destroy pending, thus meaning this transform is also
	 * destroy pending
	 * 
	 * @return
	 */
	boolean isDestroyPendingAncestor()
	{
		// if this transform is destroy pending, then stop going up tree and return true
		if (destroyCompProcessed) { return true; }

		// if not destroy pending, then check for parent

		// if no parent then return false
		if (parentTransform == null) return false;

		// there is a parent, so keep going up tree
		return parentTransform.isDestroyPendingAncestor();
	}

	/* GET COMPONENTS */

	/**
	 * Returns first component of that class including subclasses.<br>
	 * ie. 'getComponent(LttlRenderer.class);'
	 * 
	 * @param componentClass
	 *            the class of the component
	 * @return
	 */
	public <T> T getComponent(Class<T> componentClass)
	{
		return getComponent(componentClass, true);
	}

	/**
	 * Returns first component of that type.<br>
	 * ie. 'getComponent(LttlRenderer.class);'
	 * 
	 * @param componentClass
	 *            the class of the component
	 * @param includeSubClasses
	 *            should sub classes also be found and returned, or just the exact class
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> T getComponent(Class<T> componentClass, boolean includeSubClasses)
	{
		// check transform (no subclasses because final)
		if (LttlTransform.class == componentClass) { return (T) t(); }

		// iterates through this transform's components
		for (LttlComponent c : transform().components)
		{
			// checks if it is the class specified or an extended class
			if (includeSubClasses)
			{
				if (componentClass.isAssignableFrom(c.getClass())) { return (T) c; }
			}
			else
			{
				if (c.getClass() == componentClass) { return (T) c; }
			}
		}
		return null;
	}

	/**
	 * Returns first component of the class on the transform tree going down (self and descendants).
	 * 
	 * @param componentClass
	 * @param includeSubClasses
	 * @return null if none
	 */
	public <T> T getComponentInTree(Class<T> componentClass,
			boolean includeSubClasses)
	{
		T needle;

		// check self
		needle = getComponent(componentClass, includeSubClasses);

		// if found, then return result
		if (needle != null) return needle;

		// check each child
		for (LttlTransform child : children)
		{
			needle = child.getComponentInTreeUp(componentClass,
					includeSubClasses);

			// if needle is found, then break out before going to next child
			if (needle != null) break;
		}

		return needle;
	}

	/**
	 * Returns an unmodifieable list of all of this components on this component's transform (does not include transform
	 * - {@link LttlTransform}).
	 * 
	 * @return
	 */
	public List<LttlComponent> getComponents()
	{
		return Collections.unmodifiableList(components);
	}

	/**
	 * Returns all the components of that type.<br>
	 * ie. 'getComponent(LttlRenderer.class);'
	 * 
	 * @param componentClass
	 *            the class of the component
	 * @param subClasses
	 *            should extendedClasses also be found and returned, or just the exact class
	 * @return An ArrayList of components even if it is empty
	 */
	@SuppressWarnings("unchecked")
	public <T> ArrayList<T> getComponents(Class<T> componentClass,
			boolean subClasses)
	{
		return getComponents(componentClass, subClasses, new ArrayList<T>());
	}

	/**
	 * @see LttlComponent#getComponents(Class, boolean)
	 * @param componentClass
	 * @param includeSubClasses
	 * @param containerList
	 *            will not clear, just adds components to it
	 */
	public <T> ArrayList<T> getComponents(Class<T> componentClass,
			boolean includeSubClasses, ArrayList<T> containerList)
	{
		// check transform (no subclasses because final)
		if (LttlTransform.class == componentClass)
		{
			// if it is, then cast and add it
			containerList.add((T) t());
		}

		// iterates through this transform's components
		for (LttlComponent c : transform().components)
		{
			// checks if it is the class specified or a subclass
			if (includeSubClasses)
			{
				if (componentClass.isAssignableFrom(c.getClass()))
				{
					containerList.add((T) c);
				}
			}
			else
			{
				if (c.getClass() == componentClass)
				{
					// if it is, then cast and add it
					containerList.add((T) c);
				}
			}
		}
		return containerList;
	}

	/**
	 * Returns all the components in this transform tree going down (itself and below) that are of the specified class
	 * (optionally inlclude subclasses)
	 * 
	 * @param componentClass
	 * @param includeSubClasses
	 */
	public <T> ArrayList<T> getComponentsInTree(Class<T> componentClass,
			boolean includeSubClasses)
	{
		return getComponentsInTree(componentClass, includeSubClasses,
				new ArrayList<T>());
	}

	/**
	 * @see LttlComponent#getComponentsInTree(Class, boolean)
	 * @param componentClass
	 * @param includeSubClasses
	 * @param listToAddCompsTo
	 *            will not clear, just add components to it
	 */
	public <T> ArrayList<T> getComponentsInTree(Class<T> componentClass,
			boolean includeSubClasses, ArrayList<T> listToAddCompsTo)
	{
		// get self
		getComponents(componentClass, includeSubClasses, listToAddCompsTo);

		// get all children trees
		for (LttlTransform child : children)
		{
			child.getComponentsInTree(componentClass, includeSubClasses,
					listToAddCompsTo);
		}

		return listToAddCompsTo;
	}

	/**
	 * Returns all the components in this transform tree going down (itself and below)
	 * 
	 * @return
	 */
	public ArrayList<LttlComponent> getComponentsInTree()
	{
		ArrayList<LttlComponent> list = new ArrayList<LttlComponent>();

		// get self
		list.add(this);
		list.addAll(components);

		// get all children trees
		for (LttlTransform child : children)
		{
			list.addAll(child.getComponentsInTree());
		}

		return list;
	}

	/**
	 * Returns all the components in this transform tree going up (itself and all ancestors)
	 * 
	 * @return
	 */
	public ArrayList<LttlComponent> getComponentsInTreeUp()
	{
		return getComponentsInTreeUp(new ArrayList<LttlComponent>());
	}

	/**
	 * @see LttlComponent#getComponentsInTreeUp()
	 * @param listToAddCompsTo
	 */
	public ArrayList<LttlComponent> getComponentsInTreeUp(
			ArrayList<LttlComponent> listToAddCompsTo)
	{
		// get self
		listToAddCompsTo.addAll(getComponents());

		// get parent (recursive)
		if (getParent() != null)
		{
			getParent().getComponentsInTreeUp(listToAddCompsTo);
		}

		return listToAddCompsTo;
	}

	/**
	 * Returns all the components in this transform tree going up (itself and all ancestors) that are of the specified
	 * class (optionally inlclude subclasses)
	 * 
	 * @param componentClass
	 * @param includeSubClasses
	 * @param listToAddCompsTo
	 *            will not clear, just add components to it
	 */
	public <T> ArrayList<T> getComponentsInTreeUp(Class<T> componentClass,
			boolean includeSubClasses, ArrayList<T> listToAddCompsTo)
	{
		// get self
		getComponents(componentClass, includeSubClasses, listToAddCompsTo);

		// get parent (recursive)
		if (getParent() != null)
		{
			getParent().getComponentsInTreeUp(componentClass,
					includeSubClasses, listToAddCompsTo);
		}

		return listToAddCompsTo;
	}

	/**
	 * Returns first component of the class on the transform tree going up (self and ancestors).
	 * 
	 * @param componentClass
	 * @param includeSubClasses
	 * @return null if none
	 */
	public <T> T getComponentInTreeUp(Class<T> componentClass,
			boolean includeSubClasses)
	{
		T needle;

		// check self
		needle = getComponent(componentClass, includeSubClasses);

		// if found, then return result
		if (needle != null) return needle;

		// if not found, then check if has parent to check, if not, return the null, if it does, then check parent.
		if (getParent() != null)
		{
			needle = getParent().getComponentInTreeUp(componentClass,
					includeSubClasses);
		}

		return needle;
	}

	/**
	 * Finds all the components of the specified class on all child (or all descendants).
	 * 
	 * @param componentClass
	 * @param includeSubClasses
	 * @param firstDescendants
	 *            true means it only searches it's direct children (one level)
	 * @param listToAddCompsTo
	 *            will not clear, just add components to it
	 * @return
	 */
	public <T> ArrayList<T> getComponentsChildren(Class<T> componentClass,
			boolean includeSubClasses, boolean firstDescendants,
			ArrayList<T> listToAddCompsTo)
	{
		for (LttlTransform child : children)
		{
			// check child for component
			child.getComponents(componentClass, includeSubClasses,
					listToAddCompsTo);

			// if check deeper descendants, then do that for this child
			if (!firstDescendants)
			{
				child.getComponentsChildren(componentClass, includeSubClasses,
						firstDescendants, listToAddCompsTo);
			}
		}

		return listToAddCompsTo;
	}

	/**
	 * Finds the first component of the specified class on a child (or any descendant).
	 * 
	 * @param componentClass
	 * @param includeSubClasses
	 * @param firstDescendants
	 *            true means it only searches it's direct children (one level)
	 * @return null if none found
	 */
	public <T> T getComponentChildren(Class<T> componentClass,
			boolean includeSubClasses, boolean firstDescendants)
	{
		for (LttlTransform child : children)
		{
			// check child for component
			T needle = child.getComponent(componentClass, includeSubClasses);

			// if found component, then return
			if (needle != null) return needle;

			// if check deeper descendants, then do that for this child
			if (!firstDescendants)
			{
				needle = child.getComponentChildren(componentClass,
						includeSubClasses, firstDescendants);

				// if found component, then return
				if (needle != null) return needle;
			}
		}

		// none found
		return null;
	}

	public String toStringTransform()
	{
		return getName() + " (" + getId() + ", " + getScene().getName() + ")";
	}

	/**
	 * Returns all the tags on this transform
	 */
	public short getTagsBit()
	{
		return tagBit;
	}

	/**
	 * Returns if this transform has this tag.
	 * 
	 * @param tagIndex
	 * @return
	 */
	public boolean hasTag(short tagIndex)
	{
		return (tagBit & tagIndex) != 0;
	}

	/**
	 * Note: It is more efficient to create a class of constants for your tag indexes. ie. Tags.ENEMY
	 * 
	 * @see #hasTag(int)
	 */
	public boolean hasTag(String tagName)
	{
		short tagIndex = Lttl.game.getSettings().getTagIndex(tagName);
		return hasTag(tagIndex);
	}

	public enum LttlTransformChangeType
	{
		Position, Scale, Rotation, Origin, Z, Shear
	}

	/**
	 * Returns the AABB that includes this transform and all of it's descendents' {@link #getSelectionPolygon()}.
	 * Transform position points are not taken into consideration.
	 * 
	 * @param output
	 *            if null, creates one
	 * @param includePositionPoint
	 *            if true, then each transform will also add it's transform position regardless if it has a mesh or not
	 * @return may return a rect with 0 width and height if not enough points to make rect
	 */
	public Rectangle getSelectionAABBTree(Rectangle output,
			boolean includePositionPoint)
	{
		return LttlMath.GenerateBoundingRect(
				getSelectionBoundingRectPointsTree(includePositionPoint),
				output);
	}

	/**
	 * Returns the transformed bounding rect
	 * 
	 * @returnc can be null if none
	 */
	public float[] getSelectionTransformedBoundingRect()
	{
		if (altSelectionBounds != null)
		{
			// don't want to multiply by world transform matrix since uses may differ
			return altSelectionBounds.getSelectionBoundingRectTransformed();
		}
		else if (r() != null && r().getMesh() != null)
		{
			return r().getMeshBoundingRectTransformed();
		}
		else
		{
			return null;
		}
	}

	/**
	 * The transformed bounding rect that is axis aligned used for selection, will be in world units.
	 * 
	 * @return can be null if none
	 */
	public Rectangle getSelectionBoundingRectTransformedAxisAligned()
	{
		if (altSelectionBounds != null)
		{
			// don't want to multiply by world transform matrix since uses may differ
			return altSelectionBounds.getSelectionAABB();
		}
		else if (r() != null && r().getMesh() != null)
		{
			return r().getMeshBoundingRectTransformedAxisAligned();
		}
		else
		{
			return null;
		}
	}

	/**
	 * The Polygon used for selection, will be in world units.
	 * 
	 * @return can be null if none
	 */
	public PolygonContainer getSelectionPolygon()
	{
		if (altSelectionBounds != null)
		{
			return altSelectionBounds.getSelectionPolygon();
		}
		else if (r() != null && r().getMesh() != null)
		{
			return r().getMesh().generatePolygon(true);
		}
		else
		{
			return null;
		}
	}

	/**
	 * Set to null to remove. Uses this alternate aabb and polygon to check selection. Good for transforms that have no
	 * need for a renderer or mesh.
	 */
	public void setAlternateSelectionBounds(AlternateSelectionBounds alt)
	{
		if (!Lttl.game.inEditor()) return;
		this.altSelectionBounds = alt;
	}

	/**
	 * Moves the LttlTransform the specified world units in the direction of world rotation (0 rotation is to the
	 * right).
	 * 
	 * @param worldUnits
	 * @param lookup
	 *            should use lookup table for rotating
	 */
	public void moveWorld(float worldUnits, boolean lookup)
	{
		updateWorldValues();
		float v_worldRotation = getWorldRotation(false) % 360;
		Vector2 v_worldPos = getWorldPosition(false);
		if (v_worldRotation == 0)
		{
			setWorldPosition(v_worldPos.x + worldUnits, v_worldPos.y);
		}
		else
		{
			tmpV2.set(worldUnits, 0);
			LttlMath.Rotate(tmpV2, v_worldRotation, lookup);
			setWorldPosition(v_worldPos.x + tmpV2.x, v_worldPos.y + tmpV2.y);
		}
	}

	/**
	 * Moves the LttlTransform the specified local units in the direction of local rotation (0 rotation is to the
	 * right).
	 * 
	 * @param localUnits
	 * @param lookup
	 *            should use lookup table for rotating
	 */
	public void moveLocal(float localUnits, boolean lookup)
	{
		float v_rotation = rotation % 360;
		if (v_rotation == 0)
		{
			position.x += localUnits;
		}
		else
		{
			tmpV2.set(localUnits, 0);
			LttlMath.Rotate(tmpV2, v_rotation, lookup);
			position.add(tmpV2);
		}
	}
}
