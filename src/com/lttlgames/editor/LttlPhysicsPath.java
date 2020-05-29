package com.lttlgames.editor;

import java.util.ArrayList;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.GuiCallbackDescendants;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.graphics.GeometryOperation;
import com.lttlgames.graphics.LttlTriangulator;
import com.lttlgames.helpers.LttlGeometryUtil;
import com.lttlgames.helpers.LttlGeometryUtil.PolygonContainer;
import com.lttlgames.helpers.Vector2Array;

//1
@Persist(-90131)
public class LttlPhysicsPath extends LttlPhysicsFixture
{
	@Persist(9013100)
	@GuiCallback("onGuiShape")
	public LttlPath path;
	@Persist(9013101)
	@GuiCallback("onGuiShape")
	@GuiCallbackDescendants("onGuiShape")
	public ArrayList<LttlPath> cutouts = new ArrayList<LttlPath>(0);
	/**
	 * if greater than 0, it overrides the paths' tSamples
	 */
	@Persist(9013102)
	@GuiMin(0)
	@GuiCallback("onGuiShape")
	public int tSamples = 0;

	private boolean pathWarning = false;

	@Override
	public void getShapes(Vector2 bodyOrigin)
	{
		// check if main path is valid
		if (!isPathValid(path)) { return; }
		pathWarning = false;

		// Get main path
		if (!getPathArray(path, pointsShared)) return;
		processRadiusBuffer(pointsShared);

		// Get cutout paths
		ArrayList<Vector2Array> cutoutsList = null;
		if (cutouts.size() > 0)
		{
			radiusBuffer *= -1;
			cutoutsList = new ArrayList<Vector2Array>(cutouts.size());
			for (LttlPath cut : cutouts)
			{
				if (cut == null || !cut.closed || cut.controlPoints.size() < 3)
					continue;
				Vector2Array array = new Vector2Array(0);
				if (!getPathArray(cut, array)) continue;
				processRadiusBuffer(array);
				cutoutsList.add(array);
			}
			radiusBuffer *= -1;
		}

		PolygonContainer polyCont;
		// do we need to do any geometry calculations
		if (cutoutsList != null && cutoutsList.size() > 0)
		{
			ArrayList<PolygonContainer> list = LttlGeometryUtil
					.polygonOperation(GeometryOperation.DIFFERENCE,
							pointsShared, cutoutsList);

			if (list == null || list.size() == 0)
			{
				Lttl.logNote("Create Fixture Shape Error: failed on "
						+ toString() + " because of some Geometry error.");
				return;
			}

			// can not have multiple polygons
			if (list.size() > 1)
			{
				Lttl.logNote("Create Fixture Shape Error: "
						+ toString()
						+ " because with cutouts it produced more than 1 polygon.");
				return;
			}
			polyCont = list.get(0);
		}
		else
		{
			polyCont = new PolygonContainer(pointsShared);
		}

		// OPTIMIZE even though breaking everything down into triangles may seem inefficient since convex hulls up to 8
		// points are allowed, it is reliable and simple, and unless there are peformance issues, probably best to just
		// keep it this way

		// triangulate if has any holes, too many points
		if (triangulate || polyCont.getHoles().size() > 0
				|| polyCont.getPoints().size() > 8)
		{
			LttlTriangulator.TriangulateEither(polyCont, pointsShared,
					indicesContainer, null, false);

			pointsShared.mulAll(t().getWorldTransform(true))
					.sclAll(Lttl.game.getPhysics().scaling)
					.offsetAll(-bodyOrigin.x, -bodyOrigin.y);

			for (int i = 0, n = indicesContainer.size; i < n; i += 3)
			{
				float[] pts = new float[6];
				int index = indicesContainer.get(i);
				pts[0] = pointsShared.getX(index);
				pts[1] = pointsShared.getY(index);
				index = indicesContainer.get(i + 1);
				pts[2] = pointsShared.getX(index);
				pts[3] = pointsShared.getY(index);
				index = indicesContainer.get(i + 2);
				pts[4] = pointsShared.getX(index);
				pts[5] = pointsShared.getY(index);

				PolygonShape shape = Lttl.game.getPhysics()
						.getPolygonShapePool().obtain();
				shape.set(pts);
				shape.setRadius(radius);
				shapesListShared.add(shape);
			}
		}
		else
		{
			// creates a shape from just a normal path
			PolygonShape shape = Lttl.game.getPhysics().getPolygonShapePool()
					.obtain();

			pointsShared.mulAll(t().getWorldTransform(true))
					.sclAll(Lttl.game.getPhysics().scaling)
					.offsetAll(-bodyOrigin.x, -bodyOrigin.y);

			shape.set(pointsShared.toArray());
			shape.setRadius(radius);
			shapesListShared.add(shape);
		}
	}

	/**
	 * @param p
	 * @param container
	 * @return if it was successful
	 */
	private boolean getPathArray(LttlPath p, Vector2Array container)
	{
		// use own tSamples
		if (tSamples > 0)
		{
			p.generatePath(tSamples, container, null);
		}
		else
		{
			// using path's default values
			// update the path if they have not been updated since modified
			p.updatePathIfNecessary();
			container.clear();
			container.addAll(p.getPath());
		}
		if (container.size() < 3)
		{
			Lttl.logNote("Unable to create physics shape from path on "
					+ toString() + " because number of points are less than 3.");
			return false;
		}
		return true;
	}

	@Override
	public void onEditorCreate()
	{
		// see if you can find a LttlPath component on this transform
		path = t().getComponent(LttlPath.class, true);
	}

	/**
	 * checks if path is not closed or has less than 3 points
	 */
	private boolean isPathValid(LttlPath p)
	{
		if (p == null || !p.closed)
		{
			if (p != null && (!p.closed || p.controlPoints.size() < 3)
					&& !pathWarning)
			{
				Lttl.logNote("Path or Cutout Path on " + toString()
						+ " must be closed and have more than 3 points..");
				pathWarning = true;
			}
			return false;
		}
		return true;
	}
}
