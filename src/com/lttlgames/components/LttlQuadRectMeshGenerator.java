package com.lttlgames.components;

import com.badlogic.gdx.math.Vector2;
import com.lttlgames.editor.Lttl;
import com.lttlgames.editor.annotations.GuiButton;
import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.GuiCallbackDescendants;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.graphics.LttlMesh;

//3
@Persist(-90138)
public class LttlQuadRectMeshGenerator extends LttlQuadGeneratorAbstract
{
	@Persist(9013800)
	@GuiCallback("onGuiUpdateMesh")
	@GuiMin(0)
	public float width = 0;

	@Persist(9013801)
	@GuiCallback("onGuiUpdateMesh")
	@GuiMin(0)
	public float height = 0;

	@Persist(9013802)
	@GuiCallbackDescendants("onGuiUpdateMesh")
	public Vector2 center = new Vector2();

	@Persist(9013803)
	@GuiCallback("onGuiUpdateMesh")
	public boolean isSquare = false;

	private static Vector2 tmpV2 = new Vector2();

	@GuiButton
	protected void reset()
	{
		width = height = 10f * Lttl.game.getSettings().getWidthFactor();

		updateMesh();
	}

	@Override
	public void onEditorCreate()
	{
		reset();
	}

	@Override
	public void updateMesh()
	{
		// obtain mesh
		LttlMesh mesh = getNewMesh(4);

		float halfWidth = .5f * width;
		float halfHeight = isSquare ? halfWidth : .5f * height;

		// add vertices
		// top Left
		tmpV2.set(-1 * halfWidth, halfHeight).add(center);
		mesh.addVertice(tmpV2, 0, 1, r().getColor().toFloatBits(), 1);

		// top right
		tmpV2.set(halfWidth, halfHeight).add(center);
		mesh.addVertice(tmpV2, 1, 1, r().getColor().toFloatBits(), 1);

		// bottom right
		tmpV2.set(halfWidth, -1 * halfHeight).add(center);
		mesh.addVertice(tmpV2, 1, 0, r().getColor().toFloatBits(), 1);

		// bottom Left
		tmpV2.set(-1 * halfWidth, -1 * halfHeight).add(center);
		mesh.addVertice(tmpV2, 0, 0, r().getColor().toFloatBits(), 1);

		// set indices
		mesh.addIndice(0);
		mesh.addIndice(1);
		mesh.addIndice(2);
		mesh.addIndice(2);
		mesh.addIndice(3);
		mesh.addIndice(0);

		// modify vertices (offset, alpha, colors, uvs)
		this.modifyVertices(mesh, 1, 1);

		if (uvMeshSettings != null)
		{
			mesh.transformUVs(uvMeshSettings);
		}

		// set mesh
		r().setMesh(mesh);
		updateMeshAA();
	}
}
