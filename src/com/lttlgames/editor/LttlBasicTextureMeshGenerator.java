package com.lttlgames.editor;

import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.lttlgames.components.LttlQuadGeneratorAbstract;
import com.lttlgames.editor.annotations.GuiButton;
import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.GuiGroup;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.graphics.LttlMesh;
import com.lttlgames.graphics.LttlMeshFactory;

//11
@Persist(-9095)
public class LttlBasicTextureMeshGenerator extends LttlQuadGeneratorAbstract
{
	/**
	 * This is the width (game units) that a mesh should be despite the texture aspect ratio or size <b>(only if
	 * isBasicTextureMesh)</b>. This should always be used when initially scaling since you usually want the transform
	 * scale (1,1) to actually be the starting size. If texture size changes, the width stays the same.
	 */
	@Persist(909500)
	@GuiCallback("onGuiUpdateMesh")
	@GuiMin(.001f)
	public float meshWidth = 10;

	@Persist(9095011)
	@GuiGroup("Offset Settings")
	@GuiCallback("onGuiUpdateMesh")
	public boolean relativeToMeshWidth = true;

	@Override
	protected void onGuiUpdateMesh()
	{
		updateMesh();
	}

	@Override
	public void updateMesh()
	{
		LttlTexture tex = r().getTex0();

		// if no atlas region, clears any mesh
		if (tex.getAR() == null)
		{
			r().setMesh(null);
			return;
		}
		float meshScale = getMeshScale(tex.getAR());

		// generate quad, skip AA
		LttlMesh mesh = LttlMeshFactory.GenerateQuad(r().getMesh(),
				tex.getAR(), meshScale * tex.getAR().getRegionWidth(),
				meshScale * tex.getAR().getRegionHeight(), 0);

		// set alpha and color
		mesh.setAlphaAll(r().getWorldAlpha(false));
		mesh.setColorAll(r().getColor().toFloatBits());

		// modify vertices (offset, alpha, colors, uvs, densify)
		float xF = 1;
		float yF = 1;
		if (relativeToMeshWidth)
		{
			xF = meshScale * tex.getAR().getRegionWidth();
			yF = meshScale * tex.getAR().getRegionHeight();
		}
		this.modifyVertices(mesh, xF, yF);

		r().setMesh(mesh);
		// skip AA
	}

	public float getMeshScale(AtlasRegion ar)
	{
		return meshWidth / ar.getRegionWidth();
	}

	/**
	 * Sets the mesh width to the pixel width of texture0's atlas region's width, this is not exact pixels, but is good
	 * way to make all your textures relative to each other in size
	 */
	@GuiButton
	public void setMeshWidthRelativeToPixelWidth()
	{
		if (r().getTex0().getAR() != null)
		{
			meshWidth = r().getTex0().getAR().getRegionWidth()
					* Lttl.game.getSettings().getWidthFactor() / 10f;
			updateMesh();
		}
	}
}
