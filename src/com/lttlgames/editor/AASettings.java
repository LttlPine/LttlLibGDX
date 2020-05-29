package com.lttlgames.editor;

import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.Persist;

//8
@Persist(-901)
public class AASettings
{
	/**
	 * uses the globalAA value stored on WorldSettings instead of a custom one.
	 */
	@Persist(90101)
	public boolean useGlobal = true;
	/**
	 * Set this to 0 to disable AA
	 */
	@Persist(90102)
	@GuiMin(0)
	public float customAA = 0;
	/**
	 * The AA factor will be relative to how much the object is scaled. You will still have to call updateMesh() on the
	 * LttlObject after you modify scale. It uses the x scale to determine the AA amount for the entire shape.
	 */
	@Persist(90103)
	public boolean scaleDependent = true;
	/**
	 * The AA factor will be relative to the amount of camera zoom.
	 */
	@Persist(90104)
	public boolean cameraZoomDependent = true;
	/**
	 * Auto update this mesh and it's AA when camera's zoom changes (cameraZoomDependent must be true too).
	 */
	@Persist(90105)
	public boolean autoUpdateOnCameraZoom = true;

	/**
	 * less precise but good for most simple meshes
	 */
	@Persist(90107)
	public boolean useSimple = true;

	/**
	 * When AA is generated with {@link #useSimple} it will cleanup the points to try and prevent self intersections.
	 * Mostly useful when something is shrinking and large AA or very thin parts.
	 */
	@Persist(90106)
	public boolean simpleCleanup = false;

	/**
	 * if greater than 0, will use this instead of {@link LttlGameSettings#miterRatioLimit}
	 */
	@Persist(90108)
	@GuiMin(0)
	public float mitreRatioLimitOverride = 0;

	/**
	 * if shape is counterclockwise, make this true
	 */
	// @Persist(90106)
	// public boolean counterClockwise = false;
}