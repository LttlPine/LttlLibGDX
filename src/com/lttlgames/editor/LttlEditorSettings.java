package com.lttlgames.editor;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.lttlgames.editor.annotations.GuiButton;
import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.GuiGroup;
import com.lttlgames.editor.annotations.GuiHide;
import com.lttlgames.editor.annotations.GuiMax;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.GuiToolTip;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.helpers.LttlCameraTransformState;
import com.lttlgames.helpers.LttlHelper;
import com.lttlgames.helpers.LttlMath;
import com.lttlgames.helpers.LttlProfiler;

//63
/**
 * These will only be accessible if in editor.
 */
@Persist(-908)
public class LttlEditorSettings
{
	@GuiGroup("Quick Variables")
	@Persist(908060)
	public float quickFloat = 0;
	@GuiGroup("Quick Variables")
	@Persist(908061)
	public int quickInt = 0;
	@GuiGroup("Quick Variables")
	@Persist(908062)
	public boolean quickBoolean = false;

	@GuiMin(0)
	@Persist(90800)
	public float spinnerStepSize = .01f;

	/**
	 * How much the mouse wheel zooms
	 */
	@GuiMin(0)
	@GuiMax(1)
	@GuiGroup("Zoom")
	@Persist(90801)
	public float zoomSpeed = .05f;

	/**
	 * Ctrl + Shift reset zoom to this.
	 */
	@GuiMin(0)
	@GuiGroup("Zoom")
	@Persist(90802)
	public float defaultZoom = 1f;

	/**
	 * blends AA between cameras, which ever is bigger, if false, uses the {@link #aaPixelWidthThreshold}
	 */
	@GuiCallback("refreshCameraDimensions")
	@Persist(908056)
	@GuiGroup("AA")
	public boolean blendViewAA = true;
	/**
	 * if {@link #blendViewAA} is false, then uses this to define what play view width to switch to editor
	 */
	@GuiCallback("refreshCameraDimensions")
	@Persist(908055)
	@GuiGroup("AA")
	@GuiMin(30)
	public int aaPixelWidthThreshold = 200;

	/**
	 * The split between the editor and play views. (0 means 100% play view)
	 */
	@GuiGroup("View Ratio")
	@GuiCallback("onGuiEditorViewRatio")
	@GuiMin(0)
	@GuiMax(1)
	@Persist(90803)
	public float editorViewRatio = .5f;
	@Persist(908058)
	@GuiHide
	private float lastEditorViewRatio = .5f;

	/**
	 * if true, when editor view changes, the editor camera's zoom will compensate
	 */
	@GuiGroup("View Ratio")
	@Persist(908057)
	public boolean lockEditorZoom = true;

	@GuiGroup("Camera View")
	@Persist(908038)
	public boolean cameraHandles = false;

	@GuiCallback("refreshCameraDimensions")
	@GuiGroup("Camera View")
	@GuiMin(0)
	@Persist(90804)
	public float defaultAspectRatio = 1.6f;

	/**
	 * Should the play view clamp to the default aspect ratio, will not when playing out of editor.
	 */
	@GuiCallback("refreshCameraDimensions")
	@GuiGroup("Camera View")
	@Persist(90805)
	public boolean clampToDefaultAspectRatio = true;

	/**
	 * This will only be recording data if it is enabled. If you want peaks from start, be sure to have this enabled
	 * when starting.
	 */
	@Persist(90806)
	public boolean showProfilerData = false;
	@GuiToolTip("Checks if textures in folders have changed")
	@Persist(90807)
	public boolean autoRefreshTextureResources = true;

	@GuiGroup("Colors")
	@Persist(908054)
	public Color backgroundColor = new Color(Color.WHITE);
	@GuiGroup("Colors")
	@Persist(90808)
	public Color colorMeshOultine = new Color(Color.GREEN);
	@Persist(90809)
	@GuiGroup("Colors")
	public Color colorMeshBounding = new Color(Color.ORANGE);
	/**
	 * this is used for camera axis aligned, custom bounding rects, when marking what's not drawing in play view
	 */
	@GuiGroup("Colors")
	@Persist(908048)
	public Color colorRenderCheck = new Color(Color.PURPLE);
	@GuiGroup("Colors")
	@Persist(908051)
	public Color colorCameraOutline = new Color(Color.GREEN);
	@GuiGroup("Colors")
	@Persist(908052)
	public Color colorCameraRatioGuides = new Color(Color.RED);
	@Persist(908010)
	@GuiGroup("Colors")
	public Color colorCollider = new Color(Color.RED);
	@Persist(908011)
	@GuiGroup("Colors")
	public Color colorColliderBounding = new Color(Color.YELLOW);
	@Persist(908016)
	@GuiGroup("Colors")
	public Color colorMultiSelect = new Color(Color.BLUE);
	@Persist(908034)
	@GuiGroup("Colors")
	public Color colorZoomBoxRect = new Color(Color.GREEN);
	@Persist(908017)
	@GuiGroup("Colors")
	public Color colorSelect = new Color(Color.BLUE);
	@Persist(908031)
	@GuiGroup("Colors")
	public Color colorRuleOfThirds = new Color(0.5f, 0.5f, 0.5f, .7f);
	@Persist(908018)
	@GuiGroup("Colors")
	public Color handlePosColor = new Color(Color.BLUE);
	@Persist(908019)
	@GuiGroup("Colors")
	public Color handleRotColor = new Color(Color.YELLOW);
	@Persist(908020)
	@GuiGroup("Colors")
	public Color handleSclColor = new Color(Color.GREEN);
	@Persist(908033)
	@GuiGroup("Colors")
	public Color highlightColor = new Color(Color.GREEN);
	@Persist(908043)
	@GuiGroup("Colors")
	@GuiCallback("updateLeftPanelBackgroundColor")
	public Color playModeBackground = LttlHelper.Color(147, 224, 147, 1);
	@GuiGroup("Colors")
	@GuiCallback("updateLeftPanelBackgroundColor")
	public Color pauseModeBackground = LttlHelper.Color(214, 141, 134, 1);
	@Persist(908042)
	@GuiGroup("Colors")
	public Color drawMousePositionColor = new Color(Color.YELLOW);
	@Persist(908037)
	@GuiGroup("Handles")
	public boolean enableHandles = true;
	@Persist(908021)
	@GuiGroup("Handles")
	public float handleSize = 2;
	@Persist(908022)
	@GuiGroup("Handles")
	public float handleScaleSmoothness = 20;
	/**
	 * The greater the number the more smooth the scaling is when you are decreasing scale
	 */
	@Persist(908047)
	@GuiGroup("Handles")
	public float handleScaleDecreasingSmoothness = 3;
	@Persist(908023)
	@GuiGroup("Handles")
	public float handleRotationSmoothness = 2;
	@GuiGroup("Grid")
	@Persist(908024)
	public boolean enableGrid = false;
	@GuiGroup("Grid")
	@Persist(908025)
	public float gridStep = 10;
	@GuiGroup("Grid")
	@Persist(908026)
	public Vector2 gridOffset = new Vector2();
	@GuiGroup("Grid")
	@Persist(908027)
	public Color gridColor = new Color(Color.GREEN);
	/**
	 * Draws an outline around the current viewport of camera.
	 */
	@Persist(908028)
	@GuiGroup("Camera View")
	public boolean drawCameraOutline = false;
	/**
	 * Draws the axis aligned rotated rect for the camera. This is used for checking if a mesh should be rendered.
	 */
	@Persist(908049)
	@GuiGroup("Render Check")
	public boolean drawCameraAxisAlignedRect = false;
	/**
	 * marks all the renders that are visible in editor view that are not sent to play view
	 */
	@Persist(908053)
	@GuiGroup("Render Check")
	public boolean markNonRenders = false;
	/**
	 * Shows the min and max aspect ratios on camera.
	 */
	@Persist(908029)
	@GuiGroup("Camera View")
	public boolean drawAspectRatioGuides = false;
	@Persist(908030)
	@GuiGroup("Camera View")
	public boolean drawRuleOfThirds = false;
	@Persist(908059)
	@GuiGroup("Camera View")
	public LttlModeOption drawRuleOfThirdsMode = LttlModeOption.Play;
	@Persist(908039)
	@GuiHide
	ArrayList<LttlCameraTransformState> savedEditorCameraStates = new ArrayList<LttlCameraTransformState>(
			12);
	{
		for (int i = 0; i < 12; i++)
		{
			savedEditorCameraStates.add(null);
		}
	}
	@Persist(908032)
	public float arrowKeyStepFactor = .05f;
	@Persist(908035)
	public boolean showSelectionOutline = true;
	@Persist(908036)
	public boolean showTransformHiearchy = false;

	// **** GUI SETTINGS (hidden) ****//
	@GuiHide
	@Persist(908012)
	int guiCanvasPanelSize = 1200;
	@GuiHide
	@Persist(908013)
	int guiWindowSizeX = 1680;
	@GuiHide
	@Persist(908014)
	int guiWindowSizeY = 700;
	@GuiHide
	@Persist(908063)
	public Vector2 guiWindowLoc = new Vector2(0, 0);
	@GuiHide
	@Persist(908015)
	int guiRightPaneDividerLocation = 300;
	@GuiHide
	@Persist(908040)
	int[] initialSelectedTransforms;
	@Persist(908041)
	public boolean drawMousePosition = false;
	@Persist(908044)
	@GuiHide
	public Vector2 animationWindowLocation = new Vector2(0, 0);
	@Persist(908045)
	@GuiHide
	public Vector2 animationWindowSize = new Vector2(1400, 500);
	@Persist(908046)
	public LttlPathEditorSettings pathSettings = new LttlPathEditorSettings();

	@GuiButton
	public void resetProfilePeaks()
	{
		LttlProfiler.resetPeaks();
	}

	void onGuiEditorViewRatio()
	{
		if (editorViewRatio != 0)
		{
			if (lockEditorZoom)
			{
				Lttl.editor.getCamera().zoom *= lastEditorViewRatio
						/ editorViewRatio;
			}
			lastEditorViewRatio = editorViewRatio;
		}
		refreshCameraDimensions();
	}

	private void refreshCameraDimensions()
	{
		Lttl.game.resize();
	}

	@SuppressWarnings("unused")
	private void updateLeftPanelBackgroundColor()
	{
		Lttl.editor.getGui().getStatusBarController()
				.updateLeftPanelBackgroundColor();
	}

	/**
	 * Converts the x position to the nearest grid line.
	 * 
	 * @param x
	 * @return
	 */
	public float snapX(float x)
	{
		x -= gridOffset.x;
		int steps = LttlMath.floorPositive(x / gridStep);
		if (LttlMath.abs(x) % gridStep < gridStep / 2)
		{
			return gridOffset.x + steps * gridStep;
		}
		else
		{
			return gridOffset.x + (steps + ((x < 0) ? -1 : 1)) * gridStep;
		}
	}

	/**
	 * Converts the y position to the nearest grid line.
	 * 
	 * @param y
	 * @return
	 */
	public float snapY(float y)
	{
		y -= gridOffset.y;
		int steps = LttlMath.floorPositive(y / gridStep);
		if (LttlMath.abs(y) % gridStep < gridStep / 2)
		{
			return gridOffset.y + steps * gridStep;
		}
		else
		{
			return gridOffset.y + (steps + ((y < 0) ? -1 : 1)) * gridStep;
		}
	}
}
