package com.lttlgames.editor;

import com.badlogic.gdx.math.Vector2;
import com.lttlgames.editor.annotations.GuiGroup;
import com.lttlgames.editor.annotations.Persist;

/**
 * These settings are only used for generating meshes with the LttlMeshGenerator. If you want to animate UV values, then
 * modify the values on the LttlRenderer and use a shader that accepts modified uvs.
 * 
 * @author Josh
 */
@Persist(-9025)
public class UVMeshSettings
{
	@Persist(902501)
	public Vector2 offset = new Vector2();

	@Persist(902502)
	public Vector2 scale = new Vector2(1, 1);

	@Persist(902503)
	public float angle = 0;

	/**
	 * Clamps the generated mesh UV to the size based on the min and max. This is helpful for things like gradients on
	 * shapes that change size a lot.<br>
	 * NOTE: use LttlMeshGenerator.setStaticUvDimensions()
	 */
	@GuiGroup("Static UV Dimensions")
	@Persist(902505)
	public boolean staticUvSize = false;
	@GuiGroup("Static UV Dimensions")
	@Persist(902506)
	public Vector2 minDim = new Vector2();
	@GuiGroup("Static UV Dimensions")
	@Persist(902507)
	public Vector2 maxDim = new Vector2();
}
