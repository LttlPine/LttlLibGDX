package com.lttlgames.components;

import com.badlogic.gdx.math.Vector2;
import com.lttlgames.editor.Lttl;
import com.lttlgames.editor.LttlBasicTextureMeshGenerator;
import com.lttlgames.editor.annotations.AnimateField;
import com.lttlgames.editor.annotations.GuiButton;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.graphics.LttlMesh;
import com.lttlgames.graphics.LttlMeshFactory;

@Persist(-90116)
public class LttlQuadPoints
{
	@Persist(9011600)
	@AnimateField(0)
	public Vector2 topLeft = new Vector2();
	@Persist(9011601)
	@AnimateField(1)
	public Vector2 topRight = new Vector2();
	@Persist(9011602)
	@AnimateField(2)
	public Vector2 bottomRight = new Vector2();
	@Persist(9011603)
	@AnimateField(3)
	public Vector2 bottomLeft = new Vector2();

	/**
	 * @param mesh
	 * @param offsetXfactor
	 *            only relevant to {@link LttlBasicTextureMeshGenerator}, otherwise put 1
	 * @param offsetYfactor
	 *            only relevant to {@link LttlBasicTextureMeshGenerator}, otherwise put 1
	 * @throws meshVertexCount
	 *             !=4
	 */
	public void offset(LttlMesh mesh, float offsetXfactor, float offsetYfactor)
	{
		Lttl.Throw(mesh.getVertexCount() != 4);

		// top left
		mesh.offsetPos(LttlMeshFactory.QuadTopLeft, topLeft.x * offsetXfactor,
				topLeft.y * offsetYfactor);
		// top right
		mesh.offsetPos(LttlMeshFactory.QuadTopRight,
				topRight.x * offsetXfactor, topRight.y * offsetYfactor);
		// bottom right
		mesh.offsetPos(LttlMeshFactory.QuadBottomRight, bottomRight.x
				* offsetXfactor, bottomRight.y * offsetYfactor);
		// bottom left
		mesh.offsetPos(LttlMeshFactory.QuadBottomLeft, bottomLeft.x
				* offsetXfactor, bottomLeft.y * offsetYfactor);
	}

	@GuiButton
	public void reset()
	{
		topLeft.set(0, 0);
		topRight.set(0, 0);
		bottomRight.set(0, 0);
		bottomLeft.set(0, 0);
	}
}
