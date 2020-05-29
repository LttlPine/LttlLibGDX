package com.lttlgames.editor;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.Color;
import com.lttlgames.editor.annotations.GuiButton;
import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.GuiGroup;
import com.lttlgames.editor.annotations.GuiHideArrayListControls;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.GuiToolTip;
import com.lttlgames.editor.annotations.Persist;

//21
@Persist(-9010)
public class LttlGameSettings
{
	LttlGameSettings()
	{
	}

	/**
	 * can't have more than 15 tags, since that is max a short bit can hold, needs to be short because {@link Filter} is
	 * a short.
	 */
	@Persist(9010020)
	@GuiHideArrayListControls(canAdd = false, canClear = false, canDelete = false, canMove = false)
	ArrayList<String> tags = new ArrayList<String>(15);
	{
		// initial tags
		for (int i = 0; i < 15; i++)
		{
			tags.add("");
		}
	}

	@Persist(901001)
	public Color backgroundColor = Color.WHITE.cpy();

	@GuiCallback("refreshCameraDimensions")
	@GuiMin(0)
	@Persist(901002)
	public float minAspectRatio = 1.33f; // 4:3

	@GuiCallback("refreshCameraDimensions")
	@GuiMin(0)
	@Persist(901003)
	public float maxAspectRatio = 1.78f; // 16:9

	@Persist(901004)
	public Color clippingColor = Color.BLACK.cpy();

	@Persist(901005)
	public boolean drawEditorDebugInPlayMode = false;

	@Persist(901006)
	public int collisionQuadTreeMaxColliders = 20;

	@Persist(901007)
	public int collisionQuadTreeMaxLevels = -1;

	/**
	 * All objects with 'useGlobal' for anti aliasing enabled will use this value for AA.
	 */
	@GuiGroup("AA")
	@Persist(901008)
	@GuiMin(0)
	@GuiCallback("updateAllMeshes")
	public float globalAA = .16f;

	/**
	 * The max number of meshes using AA to update (they need to have a LttlMeshGenerator) in a frame when camera zoom
	 * cahnges. If 0, then is disabled.
	 */
	@Persist(9010013)
	@GuiToolTip("0 means disabled")
	public int maxAAMeshUpdatesPerFrameOnZoom = 10;

	/**
	 * zoom needs to change by this much to start updating meshes.
	 */
	@GuiGroup("AA")
	@Persist(9010021)
	@GuiMin(0)
	public float zoomThreshold = .1f;

	/**
	 * This is the screen width of the editor (primarily for creating a standard for scaling AA on other sized screens).
	 * This actually does not be accurate to the real editor width, just a constant,
	 */
	@GuiCallback("refreshCameraDimensions")
	@Persist(901009)
	public int developmentPixelWidth = 1000;

	@GuiCallback("refreshCameraDimensions")
	@Persist(9010010)
	public int gameUnitsAcrossScreenWidth = 100;

	/**
	 * Limits the number of triangles on a single batch.
	 */
	@Persist(9010011)
	@GuiMin(1)
	// @GuiMax(10920)
	@GuiCallback("guiChangeMaxTriangleBatch")
	@GuiGroup("Rendering")
	public int maxTriangleBatch = 10000;
	@GuiGroup("Rendering")
	@Persist(9010017)
	public boolean showBatchData = false;

	/**
	 * A more reliable alternative to deltaTime. Use for {@link LttlComponent#onFixedUpdate()} and
	 * {@link PhysicsController#step()} and optionally for {@link LttlGameSettings#animationDeltaTimeType}
	 */
	@Persist(9010012)
	@GuiMin(.00001f)
	public float fixedDeltaTime = .0167f;

	/**
	 * limit the deltaTime when iterating over the accumulated fixed delta time, so if it spikes on really slow devices
	 * or on load, prevents a runaway train effect
	 */
	@Persist(9010018)
	public float fixedDeltaTimeMax = .25f;
	/**
	 * the delta time to be used for stepping animation tweens, to keep them accurate to their real time should use
	 * {@link DeltaTimeType#Raw}.
	 */
	@Persist(9010016)
	public DeltaTimeType animationDeltaTimeType = DeltaTimeType.Raw;
	/**
	 * Good for time manipulation, if animations are based of of these: Lttl.game.getDeltaTime(),
	 * Lttl.game.getRawDeltaTime(), or Lttl.game.getFixedDeltaTime().
	 */
	@Persist(9010014)
	public float timeFactor = 1;

	/**
	 * This limits the distance of the mitre point when expanding a path or polygon. The ratio is the mitre length /
	 * abs(expand length)
	 */
	@Persist(9010019)
	@GuiGroup("Geometry")
	@GuiCallback("updateAllMeshes")
	@GuiMin(0)
	public float miterRatioLimit = 5;

	@GuiButton
	public void setCurrentDevelopmentPixelWidthAA()
	{
		developmentPixelWidth = (int) Lttl.game.getCamera()
				.getViewportPixelWidthStatic();
	}

	@SuppressWarnings("unused")
	private void refreshCameraDimensions()
	{
		Lttl.game.resize();
	}

	@SuppressWarnings("unused")
	private void updateAllMeshes()
	{
		Lttl.loop.updateAllMeshesAA();
	}

	@SuppressWarnings("unused")
	private void guiChangeMaxTriangleBatch()
	{
		Lttl.loop.createMesh();
	}

	/**
	 * Returns whichever camera is bigger from {@link LttlEditorSettings#editorViewRatio} or
	 * {@link LttlEditorSettings#aaPixelWidthThreshold} If not in editor, then always returns play camera.
	 */
	public LttlCamera getTargetCamera()
	{
		LttlCamera camera;
		// if in editor, choose which camera to use for calculating AA
		if (Lttl.game.inEditor())
		{
			// when more of the screen is the editor view, it uses the editor camera to generate the AA
			if (Lttl.editor.getSettings().blendViewAA)
			{
				camera = Lttl.editor.getSettings().editorViewRatio > .5f ? Lttl.editor
						.getCamera() : Lttl.game.getCamera();
			}
			else
			{
				// when the play camera width is less than threshold, it switches to editor
				camera = Lttl.game.getCamera().getViewportPixelWidthStatic() < Lttl.editor
						.getSettings().aaPixelWidthThreshold ? Lttl.editor
						.getCamera() : Lttl.game.getCamera();
			}
		}
		else
		{
			camera = Lttl.game.getCamera();
		}
		return camera;
	}

	/**
	 * Returns a simple scale factor that maintains with changes to camera zoom and screen width of whichever camera is
	 * larger
	 * 
	 * @see LttlGameSettings#getScaleFactor(LttlCamera, boolean, boolean, boolean)
	 */
	public float getScaleFactor(boolean cameraZoom, boolean screenWidth)
	{
		return getScaleFactor(getTargetCamera(), cameraZoom, screenWidth, false);
	}

	/**
	 * Returns a factor that combines all the options bellow.
	 * 
	 * @see LttlGameSettings#getScreenWidthFactor()
	 * @see LttlGameSettings#getWidthFactor()
	 * @param camera
	 *            used with cameraZoom and screenWidth
	 * @param cameraZoom
	 * @param screenWidth
	 * @param widthFactor
	 * @return
	 */
	public float getScaleFactor(LttlCamera camera, boolean cameraZoom,
			boolean screenWidth, boolean widthFactor)
	{
		float f = 1;

		// process camera zoom
		if (cameraZoom)
		{
			f /= camera.zoom;
		}

		// process screen dimensions (only width)
		if (screenWidth)
		{
			f /= getScreenWidthFactor(camera);
		}

		if (widthFactor)
		{
			f *= getWidthFactor();
		}

		return f;
	}

	/**
	 * Uses bigger camera.
	 * 
	 * @see LttlGameSettings#getScreenWidthFactor(LttlCamera)
	 */
	public float getScreenWidthFactor()
	{
		return getScreenWidthFactor(getTargetCamera());
	}

	/**
	 * Returns a factor relative to the current screen pixel width and the development pixel width. This is most useful
	 * for standardizing values between different screen sizes
	 */
	public float getScreenWidthFactor(LttlCamera camera)
	{
		return camera.getViewportPixelWidthStatic()
				/ Lttl.game.getSettings().developmentPixelWidth;
	}

	/**
	 * Returns a factor that is relative to the number of units across screen. This is most useful for standardizing
	 * sizes between game projects (ie. editor stuff like handles)
	 * 
	 * @return
	 */
	public float getWidthFactor()
	{
		return gameUnitsAcrossScreenWidth / 100f;
	}

	/**
	 * Returns the tag name from the tag index (0-15)
	 */
	public String getTagName(int tagIndex)
	{
		return tags.get(tagIndex);
	}

	public short getTagIndex(String tagName)
	{
		return (short) tags.indexOf(tagName);
	}

	/**
	 * Returns the user specified value if greater than 0, otherwise returns default value
	 */
	public float getMiterRatioLimit(float userSpecified)
	{
		return userSpecified <= 0 ? miterRatioLimit : userSpecified;
	}
}
