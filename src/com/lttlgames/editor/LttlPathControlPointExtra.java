package com.lttlgames.editor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.lttlgames.editor.annotations.GuiCanNull;
import com.lttlgames.editor.annotations.GuiMax;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.Persist;

/**
 * A class to provide optional info for a control point
 * 
 * @author Josh
 */
@Persist(-9079)
public class LttlPathControlPointExtra
{
	@Persist(907902)
	@GuiMax(1)
	@GuiMin(0)
	public float alpha = 1;
	@Persist(907903)
	@GuiCanNull
	public Color color;
	/**
	 * Experimental, really only works for sharp control points, but needs more sub divisions
	 */
	@Persist(907900)
	public Vector2 uv = new Vector2();
	@Persist(907901)
	public float lineWidth = -1;
}
