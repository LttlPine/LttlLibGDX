package com.lttlgames.components;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.IntMap;
import com.lttlgames.editor.Lttl;
import com.lttlgames.editor.LttlAnimated;
import com.lttlgames.editor.LttlBasicTextureMeshGenerator;
import com.lttlgames.editor.LttlMeshGenerator;
import com.lttlgames.editor.annotations.AnimateField;
import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.GuiCallbackDescendants;
import com.lttlgames.editor.annotations.GuiCanNull;
import com.lttlgames.editor.annotations.GuiGroup;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.graphics.LttlAntiAliaser;
import com.lttlgames.graphics.LttlMesh;
import com.lttlgames.graphics.LttlMeshFactory;
import com.lttlgames.tweenengine.TweenGetterSetter;

@Persist(-90114)
public abstract class LttlQuadGeneratorAbstract extends LttlMeshGenerator
		implements LttlAnimated
{
	@Persist(9011400)
	@GuiCallback("onGuiUpdateMesh")
	@GuiCallbackDescendants("onGuiUpdateMesh")
	@GuiCanNull
	@AnimateField(0)
	public CustomVertexObject custom;

	@Persist(9011401)
	@GuiCallback("onGuiUpdateMesh")
	@GuiCallbackDescendants("onGuiUpdateMesh")
	@GuiCanNull
	@AnimateField(1)
	public LttlQuadPoints offset;

	@Persist(9011402)
	@GuiMin(0)
	@GuiGroup("Densify Settings")
	@GuiCallback("onGuiUpdateMesh")
	@AnimateField(2)
	public int densifySteps = 0;

	@Persist(9011403)
	@GuiGroup("Vertex Locks")
	@GuiCallback("onGuiUpdateMesh")
	@GuiCallbackDescendants("onGuiUpdateMesh")
	@GuiCanNull
	public VertexLock topLeftLock;

	@Persist(9011404)
	@GuiGroup("Vertex Locks")
	@GuiCallback("onGuiUpdateMesh")
	@GuiCallbackDescendants("onGuiUpdateMesh")
	@GuiCanNull
	public VertexLock topRightLock;

	@Persist(9011405)
	@GuiGroup("Vertex Locks")
	@GuiCallback("onGuiUpdateMesh")
	@GuiCallbackDescendants("onGuiUpdateMesh")
	@GuiCanNull
	public VertexLock bottomRightLock;

	@Persist(9011406)
	@GuiGroup("Vertex Locks")
	@GuiCallback("onGuiUpdateMesh")
	@GuiCallbackDescendants("onGuiUpdateMesh")
	@GuiCanNull
	public VertexLock bottomLeftLock;

	private static Vector2 tmp = new Vector2();

	/**
	 * Modifies the vertices (alpha, color, uv, offsets, densify), before AA
	 * 
	 * @param mesh
	 * @param offsetXfactor
	 *            only relevant to {@link LttlBasicTextureMeshGenerator}, otherwise put 1
	 * @param offsetYfactor
	 *            only relevant to {@link LttlBasicTextureMeshGenerator}, otherwise put 1
	 */
	final protected void modifyVertices(LttlMesh mesh, float offsetXfactor,
			float offsetYfactor)
	{
		if (custom != null)
		{
			// modify vertices (alpha, color, and UV)
			if (custom.modifyVertices(mesh, r().getColor(),
					r().getWorldAlpha(false))
					&& r().autoUpdateMeshColorAlpha)
			{
				Lttl.logNote("Mesh Generator: Using custom vertex colors and/or alphas with autoUpdateMeshColorAlpha enabled.");
			}
		}

		// vertex locks
		processVertexLock(mesh, topLeftLock, QuadCorners.TopLeft);
		processVertexLock(mesh, topRightLock, QuadCorners.TopRight);
		processVertexLock(mesh, bottomRightLock, QuadCorners.BottomRight);
		processVertexLock(mesh, bottomLeftLock, QuadCorners.BottomLeft);

		// offset (do this after any vertex locks)
		if (offset != null)
		{
			offset.offset(mesh, offsetXfactor, offsetYfactor);
		}

		// densify
		if (densifySteps > 0)
		{
			LttlMeshFactory.DensifyQuad(mesh, densifySteps, true,
					custom != null ? custom.getCustomColors(r().getColor())
							: null);
		}
	}

	private void processVertexLock(LttlMesh mesh, VertexLock vLock,
			QuadCorners affected)
	{
		if (vLock != null && vLock.target != null
				&& vLock.target.r().getMesh() != null)
		{
			vLock.getPos(t(), tmp);
			// TODO adjustment relative to meshScale?
			tmp.add(vLock.adjustment);
			mesh.setPos(affected.value, tmp.x, tmp.y);
		}
	}

	@Override
	public void updateMeshAA(float calculatedAA)
	{
		// AA
		if (calculatedAA > 0)
		{
			LttlAntiAliaser.AddAntiAliasingEither(r().getMesh(), calculatedAA,
					aaSettings);
			r().setMesh(r().getMesh());
		}
	}

	// TODO
	final protected void processUvSettings()
	{
		Lttl.Throw();
		// uvMeshSettings.
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
				case 2:
				{
					cachedTweenGetterSetters.put(animID,
							new TweenGetterSetter()
							{

								@Override
								public void set(float[] values)
								{
									densifySteps = (int) values[0];
								}

								@Override
								public float[] get()
								{
									return new float[]
									{ densifySteps };
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
