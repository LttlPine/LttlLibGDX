package com.lttlgames.editor;

import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.exceptions.MultiplePolygonsException;
import com.lttlgames.graphics.Cap;
import com.lttlgames.graphics.Joint;
import com.lttlgames.helpers.LttlGeometryUtil;
import com.lttlgames.helpers.LttlGeometryUtil.PolygonContainer;
import com.lttlgames.helpers.LttlMath;
import com.lttlgames.helpers.LttlProfiler;
import com.lttlgames.helpers.Vector2Array;
import com.vividsolutions.jts.geom.Polygon;

//5
/**
 * Custom line widths should not be implemented since it's too complicated, and for borders you can use a path with a
 * cutout path which could start off as a duplicate path of original.
 */
@Persist(-9087)
public class LttlCustomLine extends LttlCustomShapeBase
{
	@Persist(908700)
	@GuiCallback("onGuiUpdateMesh")
	public float width = 1;

	@Persist(908701)
	@GuiCallback("onGuiUpdateMesh")
	public boolean singleSided = false;

	@Persist(908702)
	@GuiCallback("onGuiUpdateMesh")
	public float offset = 0;

	@Persist(908703)
	@GuiCallback("onGuiUpdateMesh")
	public Joint jointType = Joint.MITER;

	@Persist(908704)
	@GuiCallback("onGuiUpdateMesh")
	public Cap capType = Cap.NONE;

	/**
	 * if round joint or cap, this is number of segments, 0 uses default
	 */
	@Persist(908705)
	@GuiCallback("onGuiUpdateMesh")
	@GuiMin(0)
	public int roundSegments = 0;

	/**
	 * if greater than 0, will use this instead of {@link LttlGameSettings#miterRatioLimit}. This is for creating the
	 * line, not the AA.
	 */
	@Persist(908706)
	@GuiCallback("onGuiUpdateMesh")
	@GuiMin(0)
	public float mitreRatioLimitOverride = 0;

	@Override
	public void onEditorCreate()
	{
		super.onEditorCreate();
		width = 1 * Lttl.game.getSettings().getWidthFactor();
	}

	@Override
	public void updateMesh()
	{
		if (path == null)
		{
			r().setMesh(null);
			return;
		}

		// update the path if it has not been updated since modified
		path.updatePathIfNecessary();

		// create mesh
		LttlProfiler.meshUpdates.add();

		try
		{
			PolygonContainer polyCont;
			if (path.closed)
			{
				Vector2Array points = path.getPath();
				if (offset != 0)
				{
					polyCont = LttlGeometryUtil.offsetPolygon(path.getPath(),
							offset, Joint.MITER, roundSegments,
							mitreRatioLimitOverride, null);
					if (polyCont == null || polyCont.getHoles().size() > 0)
					{
						Lttl.logNote("Creating Line Mesh Failed");
						r().setMesh(null);
						return;
					}
					points = polyCont.getPoints();
				}

				float w = (singleSided ? width < 0 ? 0 : width : LttlMath
						.abs(width) / 2f);
				polyCont = LttlGeometryUtil.offsetPolygon(points, w, jointType,
						roundSegments, 0, null);

				w = (singleSided ? width < 0 ? width : 0
						: -LttlMath.abs(width) / 2f);
				PolygonContainer innerPolyCont = LttlGeometryUtil
						.offsetPolygon(points, w, jointType, roundSegments,
								mitreRatioLimitOverride, null);
				if (innerPolyCont == null || polyCont == null)
				{
					Lttl.logNote("Creating Line Mesh Failed: because closed path was self intersecting.");
					r().setMesh(null);
					return;
				}

				Polygon p = (Polygon) polyCont.getPolygon().difference(
						innerPolyCont.getPolygon());
				polyCont = polyCont.set(p);
			}
			else
			{
				if (offset != 0)
				{
					// NOTE currently offset does not work with open paths
				}

				polyCont = LttlGeometryUtil.bufferPath(path.getPath(),
						singleSided ? width : width / 2f, jointType,
						roundSegments, 0, capType, singleSided, null);
			}

			if (!polyCont.isValid())
			{
				r().setMesh(null);
			}
			else
			{
				finalizeMesh(getNewMesh(polyCont.getPointCount()), polyCont,
						false);
			}
		}
		catch (MultiplePolygonsException e)
		{
			Lttl.logNote("Generating Line Mesh Failed: multiple polygons created");
			r().setMesh(null);
		}
	}
}
