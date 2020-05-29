package com.lttlgames.components;

import com.lttlgames.editor.LttlTexture;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.graphics.LttlMesh;

@Persist(-9046)
public class LttlParticleEmitterTexture
{
	@Persist(904600)
	public LttlTexture texture;

	@Persist(904601)
	@GuiMin(0)
	public float randomWeight = 1;

	/* cached stuff */
	/**
	 * this holds the mesh that renders, only need to get this once
	 */
	public LttlMesh mesh;

	public void clearCache()
	{
		mesh.clear();
	}
}
