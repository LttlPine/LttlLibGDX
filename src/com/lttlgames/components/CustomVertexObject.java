package com.lttlgames.components;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.IntMap;
import com.lttlgames.editor.LttlAnimated;
import com.lttlgames.editor.annotations.AnimateField;
import com.lttlgames.editor.annotations.GuiCanNull;
import com.lttlgames.editor.annotations.GuiGroup;
import com.lttlgames.editor.annotations.GuiMax;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.graphics.LttlMesh;
import com.lttlgames.graphics.LttlMeshFactory;
import com.lttlgames.helpers.LttlMath;
import com.lttlgames.tweenengine.TweenGetterSetter;

@Persist(-90119)
public class CustomVertexObject implements LttlAnimated
{
	// alphas
	@Persist(9011900)
	@GuiGroup("Alphas")
	@GuiMin(0)
	@GuiMax(1)
	@AnimateField(0)
	public float alphaTopLeft = 1;
	@Persist(9011901)
	@GuiGroup("Alphas")
	@GuiMin(0)
	@GuiMax(1)
	@AnimateField(1)
	public float alphaTopRight = 1;
	@Persist(9011902)
	@GuiGroup("Alphas")
	@GuiMin(0)
	@GuiMax(1)
	@AnimateField(2)
	public float alphaBottomRight = 1;
	@Persist(9011903)
	@GuiGroup("Alphas")
	@GuiMin(0)
	@GuiMax(1)
	@AnimateField(3)
	public float alphaBottomLeft = 1;

	// colors
	@Persist(9011905)
	@GuiGroup("Colors")
	@GuiCanNull
	@AnimateField(4)
	public Color colorTopLeft;
	@Persist(9011906)
	@GuiGroup("Colors")
	@GuiCanNull
	@AnimateField(5)
	public Color colorTopRight;
	@Persist(9011907)
	@GuiGroup("Colors")
	@GuiCanNull
	@AnimateField(6)
	public Color colorBottomRight;
	@Persist(9011908)
	@GuiGroup("Colors")
	@GuiCanNull
	@AnimateField(7)
	public Color colorBottomLeft;

	// uvs
	/**
	 * if true, will scale the UVs based on the uv value specified (ie .3 will be 1/3 of the origin UV), otherwise will
	 * just set the UVs to the values specified
	 */
	@Persist(90119009)
	@GuiGroup("UVs")
	@AnimateField(8)
	public boolean uvRelative = true;
	@Persist(90119010)
	@GuiGroup("UVs")
	@GuiCanNull
	@AnimateField(8)
	public Vector2 uvTopLeft;
	@Persist(90119011)
	@GuiGroup("UVs")
	@GuiCanNull
	@AnimateField(9)
	public Vector2 uvTopRight;
	@Persist(90119012)
	@GuiGroup("UVs")
	@GuiCanNull
	@AnimateField(10)
	public Vector2 uvBottomRight;
	@Persist(90119013)
	@GuiGroup("UVs")
	@GuiCanNull
	@AnimateField(11)
	public Vector2 uvBottomLeft;

	/**
	 * Modifies the vertices (alpha, color, uv), only sets alpha and color if necessary, when does, it sets them all
	 * thou
	 * 
	 * @param mesh
	 * @param defaultColor
	 * @param worldAlpha
	 * @return if modified alpha and color of vertices, not guaranteed
	 */
	public boolean modifyVertices(LttlMesh mesh, Color defaultColor,
			float worldAlpha)
	{
		boolean modifiedAlphaColor = false;

		// custom alphas and colors
		if (alphaTopLeft != 1 || alphaTopRight != 1 || alphaBottomRight != 1
				|| alphaBottomLeft != 1 || colorTopLeft != null
				|| colorTopRight != null || colorBottomLeft != null
				|| colorBottomRight != null)
		{
			mesh.setAlpha(LttlMeshFactory.QuadTopLeft, alphaTopLeft
					* worldAlpha);
			mesh.setColor(LttlMeshFactory.QuadTopLeft,
					colorTopLeft == null ? defaultColor.toFloatBits()
							: colorTopLeft.toFloatBits());
			mesh.setAlpha(LttlMeshFactory.QuadTopRight, alphaTopRight
					* worldAlpha);
			mesh.setColor(LttlMeshFactory.QuadTopRight,
					colorTopRight == null ? defaultColor.toFloatBits()
							: colorTopRight.toFloatBits());
			mesh.setAlpha(LttlMeshFactory.QuadBottomRight, alphaBottomRight
					* worldAlpha);
			mesh.setColor(LttlMeshFactory.QuadBottomRight,
					colorBottomRight == null ? defaultColor.toFloatBits()
							: colorBottomRight.toFloatBits());
			mesh.setAlpha(LttlMeshFactory.QuadBottomLeft, alphaBottomLeft
					* worldAlpha);
			mesh.setColor(LttlMeshFactory.QuadBottomLeft,
					colorBottomLeft == null ? defaultColor.toFloatBits()
							: colorBottomLeft.toFloatBits());
			modifiedAlphaColor = true;
		}

		// UNTESTED
		// custom UVs
		if (uvTopLeft != null || uvBottomLeft != null || uvBottomRight != null
				|| uvTopRight != null)
		{
			float u = 0;
			float v = 0;
			float u2 = 1;
			float v2 = 1;
			if (uvRelative)
			{
				// bottom left
				u = mesh.getU(LttlMeshFactory.QuadBottomLeft);
				v = mesh.getV(LttlMeshFactory.QuadBottomLeft);

				// top right
				u2 = mesh.getU(LttlMeshFactory.QuadTopRight);
				v2 = mesh.getV(LttlMeshFactory.QuadTopRight);
			}

			if (uvTopLeft != null)
			{
				mesh.setUV(LttlMeshFactory.QuadTopLeft,
						LttlMath.Lerp(u, u2, uvTopLeft.x),
						LttlMath.Lerp(v, v2, uvTopLeft.y));
			}
			if (uvTopRight != null)
			{
				mesh.setUV(LttlMeshFactory.QuadTopRight,
						LttlMath.Lerp(u, u2, uvTopRight.x),
						LttlMath.Lerp(v, v2, uvTopRight.y));
			}
			if (uvBottomRight != null)
			{
				mesh.setUV(LttlMeshFactory.QuadBottomRight,
						LttlMath.Lerp(u, u2, uvBottomRight.x),
						LttlMath.Lerp(v, v2, uvBottomRight.y));
			}
			if (uvBottomLeft != null)
			{
				mesh.setUV(LttlMeshFactory.QuadBottomLeft,
						LttlMath.Lerp(u, u2, uvBottomLeft.x),
						LttlMath.Lerp(v, v2, uvBottomLeft.y));
			}
		}

		return modifiedAlphaColor;
	}

	/**
	 * @param defaultColor
	 * @return null means no custom colors, otherwise will return all custom colors in order of topLeft, topRight,
	 *         bottomRight, bottomLeft, and if no custom color, then uses defaultColor
	 */
	public Color[] getCustomColors(Color defaultColor)
	{
		if (colorTopLeft != null || colorTopRight != null
				|| colorBottomLeft != null || colorBottomRight != null)
		{
			return new Color[]
			{ colorTopLeft == null ? defaultColor : colorTopLeft,
					colorTopRight == null ? defaultColor : colorTopRight,
					colorBottomRight == null ? defaultColor : colorBottomRight,
					colorBottomLeft == null ? defaultColor : colorBottomLeft };
		}
		else
		{
			return null;
		}
	}

	IntMap<TweenGetterSetter> cachedTweenGetterSetters = new IntMap<TweenGetterSetter>(
			0);

	@Override
	public TweenGetterSetter getTweenGetterSetter(int animID)
	{
		// only creates the ones it needs
		if (!cachedTweenGetterSetters.containsKey(animID))
		{
			switch (animID)
			{
				case 0:
				{
					cachedTweenGetterSetters.put(animID,
							new TweenGetterSetter()
							{

								@Override
								public void set(float[] values)
								{
									alphaTopLeft = values[0];
								}

								@Override
								public float[] get()
								{
									return new float[]
									{ alphaTopLeft };
								}
							});
					break;
				}
				case 1:
				{
					cachedTweenGetterSetters.put(animID,
							new TweenGetterSetter()
							{

								@Override
								public void set(float[] values)
								{
									alphaTopRight = values[0];
								}

								@Override
								public float[] get()
								{
									return new float[]
									{ alphaTopRight };
								}
							});
					break;
				}
				case 2:
				{
					cachedTweenGetterSetters.put(animID,
							new TweenGetterSetter()
							{

								@Override
								public void set(float[] values)
								{
									alphaBottomRight = values[0];
								}

								@Override
								public float[] get()
								{
									return new float[]
									{ alphaBottomRight };
								}
							});
					break;
				}
				case 3:
				{
					cachedTweenGetterSetters.put(animID,
							new TweenGetterSetter()
							{

								@Override
								public void set(float[] values)
								{
									alphaBottomLeft = values[0];
								}

								@Override
								public float[] get()
								{
									return new float[]
									{ alphaBottomLeft };
								}
							});
					break;
				}
			}
		}
		TweenGetterSetter result = cachedTweenGetterSetters.get(animID, null);
		return result;
	}
}
