package com.lttlgames.components;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.lttlgames.editor.LttlTexture;
import com.lttlgames.editor.annotations.GuiCanNull;
import com.lttlgames.editor.annotations.GuiGroup;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.graphics.LttlMesh;

//9
@Persist(-90120)
public class TextureFoldObject
{
	@Persist(9012001)
	public Vector2 top = new Vector2();
	@Persist(9012002)
	public Vector2 bottom = new Vector2();

	@Persist(9012000)
	public LttlTexture texture = new LttlTexture();

	@Persist(9012005)
	@GuiCanNull
	public CustomVertexObject custom;

	@Persist(9012004)
	@GuiGroup("Settings")
	@GuiMin(0)
	public int densifySteps = 0;

	@Persist(9012006)
	@GuiGroup("Settings")
	public float alpha = 1;
	@Persist(9012009)
	@GuiGroup("Settings")
	@GuiCanNull
	public Color color;

	@Persist(9012007)
	@GuiGroup("Seem Adjustment")
	@GuiCanNull
	public Vector2 topAdjustment;
	@Persist(9012008)
	@GuiGroup("Seem Adjustment")
	@GuiCanNull
	public Vector2 bottomAdjustment;

	@Persist(9012003)
	@GuiGroup("Settings")
	public boolean positionsRelative = false;

	/**
	 * exists for mesh generation only
	 */
	public LttlMesh mesh;

}
