package com.lttlgames.editor;

import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.graphics.LttlMesh;
import com.lttlgames.graphics.LttlMeshFactory;
import com.lttlgames.helpers.LttlGeometryUtil.PolygonContainer;
import com.lttlgames.helpers.LttlProfiler;
import com.lttlgames.helpers.Vector2Array;

@Persist(-9086)
public class LttlCustomLandscape extends LttlCustomShapeBase
{
	@Persist(908600)
	@GuiCallback("onGuiUpdateMesh")
	public float height = 100;
	/**
	 * the landscape bottom is 0 and the top is 1
	 */
	@Persist(908601)
	@GuiCallback("onGuiUpdateMesh")
	public boolean fixedUV = true;

	/**
	 * Does not do the normal triangulation, instead makes it so each path point has a counter point, only need linear
	 * trianfulation if you want triangles to all be evenly spaced out
	 */
	@Persist(908602)
	@GuiCallback("onGuiUpdateMesh")
	public boolean linearTriangulation = true;

	/**
	 * improves performance by releasing memory after mesh created
	 */
	@Persist(908603)
	public boolean isStatic = true;

	/* Temp */
	private Vector2Array vectorArrayTemp;

	@Override
	public void updateMesh()
	{
		if (path == null || path.closed)
		{
			if (path != null && path.closed)
			{
				Lttl.logNote("Path must be not closed to create landscape mesh.");
			}
			r().setMesh(null);
			return;
		}

		// update the path if it has not been updated since modified
		path.updatePathIfNecessary();

		LttlMesh mesh = getNewMesh(path.getPath().size());

		// create mesh
		LttlProfiler.meshUpdates.add();

		if (vectorArrayTemp == null)
		{
			vectorArrayTemp = new Vector2Array();
		}

		// get path
		vectorArrayTemp.clear();
		vectorArrayTemp.addAll(path.getPath());

		float y = path.getPath().getMinMax()[1] - height;
		if (linearTriangulation)
		{
			// vertices, add all bottom points
			vectorArrayTemp.ensureCapacity(path.getPath().size());
			for (int i = path.getPath().size() - 1; i >= 0; i--)
			{
				vectorArrayTemp.add(path.getPath().getX(i), y);
			}
			mesh.getVerticesArray().ensureCapacity(path.getPath().size() * 2);
			LttlMeshFactory.GenerateVertices(vectorArrayTemp,
					mesh.getVerticesArray());
			if (fixedUV)
			{
				fixUV(r().getMesh());
			}

			// indices
			mesh.getIndicesArray().ensureCapacity(
					(path.getPath().size() * 2 - 2) * 3);
			int e = vectorArrayTemp.size() - 1;
			for (int i = 0, n = path.getPath().size(); i < n - 1; i++)
			{
				mesh.getIndicesArray().add(i);
				mesh.getIndicesArray().add(i + 1);
				mesh.getIndicesArray().add(e - i);
				mesh.getIndicesArray().add(e - i);
				mesh.getIndicesArray().add(i + 1);
				mesh.getIndicesArray().add(e - i - 1);
			}

			finalizeMesh(mesh, (Vector2Array) null, true);
		}
		else
		{
			// add the two bottom points
			vectorArrayTemp.add(path.getPath().getLastX(), y);
			vectorArrayTemp.add(path.getPath().getFirstX(), y);

			if (fixedUV)
			{
				// will add AA later
				r().setMesh(
						LttlMeshFactory.GeneratePolygon(r().getMesh(), new PolygonContainer(
								vectorArrayTemp),
								useDelaunay));
				fixUV(r().getMesh());

				finalizeMesh(r().getMesh(), (Vector2Array) null, true);
			}
			else
			{
				finalizeMesh(r().getMesh(), vectorArrayTemp, false);
			}
		}

		vectorArrayTemp.clear();
		if (isStatic)
		{
			vectorArrayTemp = null;
		}
	}

	private void fixUV(LttlMesh mesh)
	{
		for (int i = 0, n = mesh.getVertexCount(); i < n; i++)
		{
			mesh.setY(i, ((i < path.getPath().size()) ? 1 : 0));
		}
	}
}
