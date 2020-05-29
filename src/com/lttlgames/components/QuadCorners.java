package com.lttlgames.components;

import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.graphics.LttlMeshFactory;

@Persist(-90122)
public enum QuadCorners
{
	TopLeft(LttlMeshFactory.QuadTopLeft), TopRight(LttlMeshFactory.QuadTopRight), BottomRight(
			LttlMeshFactory.QuadBottomRight), BottomLeft(
			LttlMeshFactory.QuadBottomLeft);

	int value = 0;

	QuadCorners(int index)
	{
		this.value = index;
	}
}
