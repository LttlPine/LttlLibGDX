package com.lttlgames.editor;

import com.badlogic.gdx.math.Rectangle;
import com.lttlgames.components.interfaces.AnimationCallback;
import com.lttlgames.editor.annotations.ComponentLimitOne;
import com.lttlgames.editor.annotations.ComponentRequired;
import com.lttlgames.editor.annotations.GuiButton;
import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.GuiCallbackDescendants;
import com.lttlgames.editor.annotations.GuiCanNull;
import com.lttlgames.editor.annotations.GuiGroup;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.graphics.LttlMesh;
import com.lttlgames.helpers.LttlMath;

@ComponentLimitOne
@ComponentRequired(LttlRenderer.class)
@Persist(-9011)
abstract public class LttlMeshGenerator extends LttlComponent implements
		AnimationCallback
{
	private float cameraZoomOnLastUpdateMesh = 1;

	// starts as null, because rarely used
	@Persist(901101)
	@GuiCanNull
	@GuiCallbackDescendants("onGuiUpdateMesh")
	@GuiCallback("onGuiUpdateMesh")
	public AASettings aaSettings = null;
	// starts as null, because rarely used
	@Persist(901102)
	@GuiCanNull
	@GuiCallbackDescendants("onGuiUpdateMesh")
	@GuiCallback("onGuiUpdateMesh")
	public UVMeshSettings uvMeshSettings = null;

	private float actualAA;

	@Persist(901103)
	@GuiGroup("Debug")
	public boolean debugUpdateMeshEveryFrame = false;
	@Persist(901104)
	@GuiGroup("Debug")
	public boolean printMeshDetails = false;

	@Override
	public void onEditorCreate()
	{
		// create mesh when initially created in editor, since another mesh already may be set, which means an
		// updateMesh won't automatically run unless gui is changed.
		updateMesh();
	}

	@Override
	public void onEditorUpdate()
	{
		if (printMeshDetails)
		{
			r().printMeshDetails();
		}
	}

	@Override
	public void onUpdate()
	{
		if (Lttl.game.inEditor() && printMeshDetails)
		{
			r().printMeshDetails();
		}
	}

	// * CALL BACK *//

	/**
	 * <b> Be sure to call renderer().setMesh(mesh, hasAA) always at end</b><br>
	 * This is where you either update or make a new mesh. You need to set the mesh on the renderer manually though, or
	 * get the mesh and modify it and then set it. The mesh could be a reference from another renderer.<br>
	 * This is called when no mesh exists when renderering, when screen resizes, when camera zoom changes (if set in
	 * AASettings), and anytime manually (like when scale changes, etc).<br>
	 * The LttlProfiler.meshUpdates.add() call should be in this function.<br>
	 * There is no need to ever call this unless you manually made a change. The mesh will not be created until it is
	 * needed (enabled through hiearchy and visible) in the render loop. This way there is no waste of resources. Any
	 * objects that start enabled, will have their meshes created when rendered first time by calling back this function<br>
	 * You do not need to update the color and alpha vertex attributes here, if render is set to autoUpdate them when
	 * rendering if any changes. <br>
	 * <br>
	 * AA: Whenever you enable an entire LttlTransform (even parents) or LttlRenderer or LttlMeshGenerator, it is good
	 * to manually call updateMesh, especially if the camera has changed or any AA settings have been changed, since
	 * disabled meshes are usually skipped for updating. You can always compare getActualAA() to updateAA() and if
	 * different, you should definitely updateMesh(). If not it should automatically update in the render loop by
	 * checking values.
	 */
	@GuiButton
	public abstract void updateMesh();

	/**
	 * This is called when only the meshes' AA should be updated AND a mesh already exists. This should be more
	 * efficient then recreating whole mesh.<b>YOU NEED TO {@link LttlRenderer#setMesh(LttlMesh)} or will not be marked
	 * as modified.</b><br>
	 * This is usually called by camera zoom change or manually for when a an object's zoom is changed.
	 * 
	 * @param calculatedAA
	 *            this already calculated and can be used without calling {@link #updateActualAA()}, since it is already
	 *            calculated prior to this callback, however if you call this, you can just calculate it yourself or use
	 *            getActualAA() to get the last calculated one.
	 */
	public abstract void updateMeshAA(float calculatedAA);

	/**
	 * Updates actual AA and then runs updates mesh's AA>
	 */
	public final void updateMeshAA()
	{
		updateMeshAA(updateActualAA());
	}

	// LttlProfiler.meshUpdates.add();
	// do mesh modifications/creation here
	// ...
	// set mesh
	// this.transform().renderer().setMesh(mesh);

	/**
	 * Calculates the AA value and stores it (getActualAA()) taking into consideration scale, useGlobal, camera zoom,
	 * and screen size. This should be called when doing an updateMesh. It will check if AA is even enabled.
	 * 
	 * @return the actual AA amount
	 */
	public float updateActualAA()
	{
		return updateActualAA(true);
	}

	/**
	 * User should not have access to this.
	 * 
	 * @param checkWorld
	 * @return
	 */
	float updateActualAA(boolean checkWorld)
	{
		if (aaSettings == null)
		{
			actualAA = 0;
			return actualAA;
		}

		float actual = aaSettings.customAA;

		// process global
		if (aaSettings.useGlobal)
		{
			actual = Lttl.game.getSettings().globalAA;
		}

		// early out if actual AA is 0 (AKA disabled)
		if (actual == 0)
		{
			actualAA = actual;
			return actualAA;
		}

		// process scale (only x)
		if (aaSettings.scaleDependent)
		{
			actual = actual / LttlMath.abs(transform().getWorldScale(true).x);
		}

		actual *= Lttl.game.getSettings().getScaleFactor(
				aaSettings.cameraZoomDependent, true);

		cameraZoomOnLastUpdateMesh = Lttl.game.getSettings().getTargetCamera().zoom;

		actualAA = actual;
		return actualAA;
	}

	/**
	 * Returns the last calculated actual AA. The ActualAA is often calculated automatically whenever update mesh
	 * callbacks are made, since it is needed to determine if AA is even enabled, so use this value instead of running
	 * updateActualAA() again.<br>
	 * However, if not running in a callback that updatesActualAA(), then do it yourself.
	 * 
	 * @return
	 */
	public float getActualAA()
	{
		return actualAA;
	}

	/**
	 * The camera zoom amount that this object last got updated to. This changes when updateActualAA() is ran, which is
	 * ran whenever the mesh updates.
	 * 
	 * @return
	 */
	public float getCameraZoomOnLastUpdateMesh()
	{
		return cameraZoomOnLastUpdateMesh;
	}

	/**
	 * Called when any settings are modified in GUI that should trigger an update mesh
	 */
	protected void onGuiUpdateMesh()
	{
		updateMesh();
	}

	/**
	 * Gets a new mesh, if one already exists on renderer clears it returns it
	 * 
	 * @return
	 */
	protected LttlMesh getNewMesh()
	{
		return LttlMesh.getNew(r().getMesh());
	}

	/**
	 * Gets a new mesh, if one already exists on renderer clears it returns it with initial vertice capacity
	 * 
	 * @param initialVertexCapacity
	 * @return
	 */
	protected LttlMesh getNewMesh(int initialVertexCapacity)
	{
		return LttlMesh.getNew(r().getMesh(), initialVertexCapacity);
	}

	@GuiButton
	private void setStaticUvDimensions()
	{
		if (uvMeshSettings == null)
		{
			uvMeshSettings = new UVMeshSettings();
		}

		updateMesh();
		Rectangle range = r().getMesh().getBoundingRect(false, false, null);
		uvMeshSettings.minDim.x = range.x;
		uvMeshSettings.minDim.y = range.y;
		uvMeshSettings.maxDim.x = range.x + range.width;
		uvMeshSettings.maxDim.y = range.y + range.height;
		onGuiUpdateMesh();
	}

	@Override
	public void onCallback(String animName, String seqName, String value)
	{
	}

	@Override
	public void onStep(String name, float iterationPosition)
	{
		updateMesh();
	}

	@GuiButton
	private void printMeshDetails()
	{
		if (r().getMesh() == null)
		{
			Lttl.logNote("Mesh Details: null");
		}
		else
		{
			Lttl.logNote(r().getMesh().toString());
		}
	}
}
