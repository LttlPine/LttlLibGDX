package com.lttlgames.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.GuiCallbackDescendants;
import com.lttlgames.editor.annotations.GuiGroup;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.exceptions.MultiplePolygonsException;
import com.lttlgames.helpers.LttlGeometryUtil;
import com.lttlgames.helpers.LttlGeometryUtil.PolygonContainer;
import com.lttlgames.helpers.LttlProfiler;
import com.lttlgames.helpers.Vector2Array;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.TopologyException;

//1
@Persist(-9084)
public class LttlCustomShape extends LttlCustomShapeBase
{
	@Persist(908400)
	@GuiCallback("onGuiCutouts")
	@GuiCallbackDescendants("onGuiCutouts")
	private ArrayList<LttlPath> cutouts = new ArrayList<LttlPath>(0);

	/**
	 * in addition to any cutouts provided, it will detect holes from self intersections.
	 */
	@Persist(908401)
	@GuiGroup("Settings")
	@GuiCallback("onGuiUpdateMesh")
	public boolean autoDetectHoles = false;

	/**
	 * only used in editor,caches the cutouts, so if they are changed from editor GUI, we'll be able to tell what
	 * changed and manage listeners
	 */
	private ArrayList<LttlPath> cutoutsEditorCache;

	@Override
	public void updateMesh()
	{
		// check if main path is valid
		if (!isPathValid(path)) { return; }
		pathWarning = false;

		// update the paths if they have not been updated since modified
		path.updatePathIfNecessary();
		ArrayList<Vector2Array> cutoutsList = null;
		if (cutouts.size() > 0)
		{
			cutoutsList = new ArrayList<Vector2Array>(cutouts.size());
			for (LttlPath cut : cutouts)
			{
				if (cut == null || !cut.closed || cut.controlPoints.size() < 3)
					continue;
				cut.updatePathIfNecessary();
				cutoutsList.add(cut.getPath());
			}
		}

		// create a polygon container from path
		PolygonContainer polyCont = new PolygonContainer(path.getPath());
		// auto detect holes before processing cutouts
		if (autoDetectHoles)
		{
			try
			{
				polyCont = LttlGeometryUtil.simplifyPolygon(
						polyCont.getPolygon(), polyCont);
			}
			catch (MultiplePolygonsException e)
			{
				Lttl.logNote("Update Mesh failed on " + toString()
						+ " mesh split into multiple seperate polygons.");
				r().setMesh(null);
				return;
			}
		}

		// do we need to do any geometry calculations for cutouts
		if (cutoutsList != null && cutoutsList.size() > 0)
		{
			try
			{
				Geometry g = polyCont.getPolygon().difference(
						LttlGeometryUtil.polygonUnionToGeometry(cutoutsList));
				if (g instanceof Polygon)
				{
					polyCont.set((Polygon) g);
				}
				else
				{
					// can not have multiple polygons
					if (g instanceof MultiPolygon)
					{
						Lttl.logNote("Update Mesh failed on "
								+ toString()
								+ " because with cutouts it produced more than 1 mesh.");
					}
					else
					{
						Lttl.logNote("Update Mesh failed on " + toString()
								+ " because of some Geometry error.");
					}
					r().setMesh(null);
					return;
				}
			}
			catch (TopologyException e)
			{
				Lttl.logNote("Processing Custom Shape Cutouts Failed: "
						+ e.getMessage());
			}
		}

		// create mesh
		LttlProfiler.meshUpdates.add();

		finalizeMesh(getNewMesh(polyCont.getPointCount()), polyCont,
				false);
	}

	@Override
	protected void initModifiedListeners()
	{
		super.initModifiedListeners();
		for (LttlPath cut : cutouts)
		{
			if (cut == null) continue;
			cut.addModifiedListener(this);
		}
		if (Lttl.game.inEditor())
		{
			cutoutsEditorCache = new ArrayList<LttlPath>(cutouts);
		}
	}

	@SuppressWarnings("unused")
	private void onGuiCutouts()
	{
		// the cutouts arraylist changed in editor, remove all listners from old cached cutout paths
		if (cutoutsEditorCache != null)
		{
			for (LttlPath cut : cutoutsEditorCache)
			{
				if (cut == null) continue;
				cut.removeModifiedListener(this);
			}
			// readd all listeners, this will not add to the main path since it already has one
			// this will also update the cutoutsEditorCache with the new cutouts paths
			initModifiedListeners();
		}

		updateMesh();
	}

	/**
	 * Returns an unmodified list of cutouts. To add and remove use {@link #addCutout(LttlPath)} and
	 * {@link #removeCutout(LttlPath)}, this properly manages listeners.
	 * 
	 * @return
	 */
	public List<LttlPath> getCutouts()
	{
		return Collections.unmodifiableList(cutouts);
	}

	/**
	 * Adds a cutout path and manages listeners.
	 * 
	 * @param cutout
	 */
	public void addCutout(LttlPath cutout)
	{
		cutouts.add(cutout);
		cutout.removeModifiedListener(this);
	}

	/**
	 * Removes a cutout path and manages listeners.
	 * 
	 * @param cutout
	 */
	public void removeCutout(LttlPath cutout)
	{
		cutouts.remove(cutout);
		cutout.addModifiedListener(this);
	}

	@Override
	protected void processOnDestroyComp()
	{
		super.processOnDestroyComp();
		for (LttlPath path : cutouts)
		{
			if (path == null) continue;
			path.removeModifiedListener(this);
		}
	}
}
