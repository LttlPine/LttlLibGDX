package com.lttlgames.helpers;

import com.badlogic.gdx.Gdx;
import com.lttlgames.editor.Lttl;
import com.lttlgames.editor.annotations.GuiButton;
import com.lttlgames.editor.annotations.GuiShow;

@GuiShow
public final class LttlProfiler
{
	private LttlProfiler()
	{
		// Exists only to defeat instantiation.
	}

	static LttlProfiler singleton = new LttlProfiler();

	@GuiShow
	public static LttlProfileData meshUpdates = new LttlProfileData(
			"Mesh Updates");
	@GuiShow
	public static LttlProfileData meshObjects = new LttlProfileData(
			"Mesh Objects Created");
	@GuiShow
	public static LttlProfileData aaUpdates = new LttlProfileData("AA Updates");
	@GuiShow
	public static LttlProfileData triangulations = new LttlProfileData(
			"Triangulations");
	@GuiShow
	public static LttlProfileData transformLocalUpdates = new LttlProfileData(
			"Transform Local Updates");
	@GuiShow
	public static LttlProfileData transformWorldUpdates = new LttlProfileData(
			"Transform World Updates");
	@GuiShow
	public static LttlProfileData renderedMeshes = new LttlProfileData(
			"Rendered Meshes");
	@GuiShow
	public static LttlProfileData renderedVertices = new LttlProfileData(
			"Rendered Vertices");
	@GuiShow
	public static LttlProfileData renderedTriangles = new LttlProfileData(
			"Rendered Triangles");
	@GuiShow
	public static LttlProfileData batchCount = new LttlProfileData(
			"Render Batches");
	@GuiShow
	private static LttlProfileData javaHeap = new LttlProfileData("Java Heap");
	@GuiShow
	public static LttlProfileData enabledTransforms = new LttlProfileData(
			"Enabled LttlTransforms");
	@GuiShow
	public static LttlProfileData enabledComponents = new LttlProfileData(
			"Enabled LttlComponents");
	@GuiShow
	public static LttlProfileData checkBoundingRectangles = new LttlProfileData(
			"Bounding Rectangle Collision Checks");
	/**
	 * These are the number of searches (per scene) for a texture. Should be 0 except for load.
	 */
	@GuiShow
	public static LttlProfileData textureRefreshes = new LttlProfileData(
			"Texture Refreshes");
	@GuiShow
	public static LttlProfileData pathUpdates = new LttlProfileData(
			"Path Updates");

	private static LttlProfileData[] allLttlProfileDatas =
	{ enabledTransforms, enabledComponents, meshUpdates, meshObjects,
			triangulations, aaUpdates, transformLocalUpdates,
			transformWorldUpdates, renderedMeshes, renderedVertices,
			renderedTriangles, batchCount, checkBoundingRectangles,
			textureRefreshes, pathUpdates, javaHeap };

	/**
	 * Calculates peaks, prints to screen, and clears for next frame<br>
	 * Note: this automatically checks if current world has 'showProfilerData' enabled.
	 */
	public static void run()
	{
		if (!Lttl.editor.getSettings().showProfilerData) return;

		javaHeap.current = Gdx.app.getJavaHeap() / 1000000f;

		String current = "";
		String peak = "";

		Lttl.logNote("Profiler: ***** Starting *****");

		for (LttlProfileData p : allLttlProfileDatas)
		{
			// set peak
			p.peak = LttlMath.max(p.peak, p.current);

			// prep format
			current = ((p.current == (int) p.current) ? String
					.valueOf((int) p.current) : p.current) + "";
			peak = ((p.peak == (int) p.peak) ? String.valueOf((int) p.peak)
					: p.peak) + "";

			// display data
			Lttl.logNote("Profiler - " + p.name + ": " + current + "[" + peak
					+ "]");

			// clear for next frame
			p.current = 0;
		}
	}

	@GuiButton
	public static void toggle()
	{
		Lttl.editor.getSettings().showProfilerData = !Lttl.editor.getSettings().showProfilerData;
	}

	@GuiButton
	public static void resetPeaks()
	{
		for (LttlProfileData p : allLttlProfileDatas)
		{
			p.peak = 0;
		}
	}

	public static LttlProfiler get()
	{
		return singleton;
	}
}
