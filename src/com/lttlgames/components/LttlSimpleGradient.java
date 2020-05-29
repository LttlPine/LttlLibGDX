package com.lttlgames.components;

import com.badlogic.gdx.utils.ShortArray;
import com.lttlgames.editor.LttlMeshGenerator;
import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.GuiMax;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.graphics.LttlMesh;
import com.lttlgames.graphics.LttlTriangulator;

@Persist(-90144)
public class LttlSimpleGradient extends LttlMeshGenerator
{
	/**
	 * this is the amount of space that will be used for just top color
	 */
	@GuiCallback("onGuiUpdateMesh")
	@Persist(9014400)
	@GuiMin(0)
	@GuiMax(1)
	public float topSpace = 0;

	/**
	 * this is the amount of space that will be used for just bottom color
	 */
	@GuiCallback("onGuiUpdateMesh")
	@Persist(9014401)
	@GuiMin(0)
	@GuiMax(1)
	public float bottomSpace = 0;

	@Override
	public void updateMesh()
	{
		LttlMesh mesh = LttlMesh.getNew(r().getMesh(), 4);
		ShortArray indices = mesh.getIndicesArray();

		float sizeX = 1;
		float sizeY = 1;
		float left = -sizeX / 2;
		float bottom = -sizeX / 2;

		/* RIGHT */
		// top right
		mesh.addVertice(left + sizeX, bottom + sizeY, 1, 1, 0, 1);
		// top space right
		if (topSpace > 0)
		{
			mesh.addVertice(left + sizeX, bottom + (sizeY * (1 - topSpace)), 1,
					1, 0, 1);
		}
		// bottom space right
		if (bottomSpace > 0)
		{
			mesh.addVertice(left + sizeX, bottom + (sizeY * (bottomSpace)), 1,
					0, 0, 1);
		}
		// bottom right
		mesh.addVertice(left + sizeX, bottom, 1, 0, 0, 1);

		/* LEFT */
		// bottom left
		mesh.addVertice(left, bottom, 0, 0, 0, 1);
		// bottom space left
		if (bottomSpace > 0)
		{
			mesh.addVertice(left, bottom + (sizeY * bottomSpace), 0, 0, 0, 1);
		}
		// top space left
		if (topSpace > 0)
		{
			mesh.addVertice(left, bottom + (sizeY * (1 - topSpace)), 0, 1, 0, 1);
		}
		// top left
		mesh.addVertice(left, bottom + sizeY, 0, 1, 0, 1);

		// triangulate
		LttlTriangulator.TriangulateColumn(mesh.getVertexCount(), indices,
				false, false);

		// transform UVS (will check if necessary in method)
		if (uvMeshSettings != null)
		{
			mesh.transformUVs(uvMeshSettings);
		}

		// NOTE no AA

		r().setMesh(mesh);
	}

	@Override
	public void updateMeshAA(float calculatedAA)
	{
		// do nothing, no need for AA
	}

}
