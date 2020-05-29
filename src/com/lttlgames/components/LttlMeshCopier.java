package com.lttlgames.components;

import com.lttlgames.editor.LttlMeshGenerator;
import com.lttlgames.editor.LttlRenderer;
import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.graphics.LttlMesh;

@Persist(-9035)
public final class LttlMeshCopier extends LttlMeshGenerator
{
	@Persist(903501)
	@GuiCallback("updateMesh")
	public LttlRenderer sourceRenderer;

	public void onUpdate()
	{
		updateMesh();
	}

	public void updateMesh()
	{
		if (sourceRenderer == null)
		{
			t().r().setMesh(null);
			return;
		}

		LttlMesh copy = sourceRenderer.getMesh();
		if (copy == null && sourceRenderer.generator() != null)
		{
			sourceRenderer.generator().updateMesh();
		}
		copy = sourceRenderer.getMesh();

		if (copy == null)
		{
			t().r().setMesh(null);
			return;
		}

		LttlMesh yours = getNewMesh(copy.getVertexCount());
		yours.set(copy);

		// forces refresh
		yours.getWorldVerticesArray().clear();

		t().r().setMesh(yours);
	}

	@Override
	public void updateMeshAA(float calculatedAA)
	{
		updateMesh();
	}
}
