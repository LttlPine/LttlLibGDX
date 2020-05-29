package com.lttlgames.editor;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.utils.ShortArray;
import com.lttlgames.editor.annotations.ComponentRequired;
import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.GuiGroup;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.graphics.LttlMesh;
import com.lttlgames.graphics.LttlTriangulator;

@ComponentRequired(LttlMeshGenerator.class)
@Persist(-90130)
public class LttlPhysicsMesh extends LttlPhysicsFixture
{
	@Persist(9013000)
	@GuiGroup("Shape")
	@GuiCallback("onGuiShape")
	public boolean inheritOriginRenderMesh = true;

	/**
	 * if mesh has holes, should they be taken into consideration?
	 */
	@GuiGroup("Shape")
	@GuiCallback("onGuiShape")
	@Persist(9013001)
	public boolean useMeshHoles = false;

	@Override
	public void getShapes(Vector2 bodyOrigin)
	{
		LttlMesh mesh = t().renderer().getMesh();
		if (mesh == null && t().renderer().generator().isEnabled())
		{
			t().renderer().generator().updateMesh();
			mesh = t().renderer().getMesh();
		}
		if (mesh == null)
		{
			Lttl.logNote("Unable to generate mesh on " + t().getName() + ".");
			return;
		}

		if (mesh.getVertexCount() < 3)
		{
			Lttl.logNote("Unable to create physics shape from mesh on "
					+ t().getName()
					+ " because number of vertices is less than 3.");
			return;
		}

		// get mesh points, this way don't have to mess up mesh
		if (useMeshHoles)
		{
			// should include holes, if any
			mesh.getVerticesPosNoAA(false, pointsShared);
		}
		else
		{
			mesh.getVerticesPosMain(false, pointsShared);
			// radius buffer only set for non hole polygons
			processRadiusBuffer(pointsShared);
		}

		if (inheritOriginRenderMesh)
		{
			pointsShared.mulAll(t().getWorldRenderTransform(true));
		}
		else
		{
			pointsShared.mulAll(t().getWorldTransform(true));
		}

		// scale the vertices, and make the shape relative to body's position
		pointsShared.sclAll(Lttl.game.getPhysics().scaling).offsetAll(
				-bodyOrigin.x, -bodyOrigin.y);

		// OPTIMIZE even though breaking everything down into triangles may seem inefficient since convex hulls up to 8
		// points are allowed, it is reliable and simple, and unless there are peformance issues, probably best to just
		// keep it this way

		// populate shared shapes list, each one representing a fixtures
		// check if using triangulation or force triangulation if vertices>8
		if (triangulate || pointsShared.size() > 8)
		{
			// create a shape of each triangle,
			ShortArray indices;

			// if it has holes, and you don't want them, then will need to retriangulate
			if (mesh.getHolesCount() > 0 && !useMeshHoles)
			{
				indicesContainer.clear();
				LttlTriangulator.Triangulate(pointsShared, indicesContainer);
				indices = indicesContainer;
			}
			else
			{
				// getting holes, pretty much using the mesh exactly how it is
				indices = mesh.getIndicesArray();
			}

			// limit is so there is no IndexOutOfBoundsException because the shape may have AA and we may be skipping
			// holes
			for (int i = 0, n = mesh.hasAA() ? mesh.getAAIndiceIndex()
					: indices.size; i < n; i += 3)
			{
				int index = indices.get(i);
				float[] pts = new float[6];
				pts[0] = pointsShared.getX(index);
				pts[1] = pointsShared.getY(index);
				index = indices.get(i + 1);
				pts[2] = pointsShared.getX(index);
				pts[3] = pointsShared.getY(index);
				index = indices.get(i + 2);
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
			// creates a shape for the entire mesh
			PolygonShape shape = Lttl.game.getPhysics().getPolygonShapePool()
					.obtain();
			shape.set(pointsShared.toArray());
			shape.setRadius(radius);
			shapesListShared.add(shape);
		}
	}
}
