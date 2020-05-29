package com.lttlgames.components;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.lttlgames.editor.Handle;
import com.lttlgames.editor.HandleRect;
import com.lttlgames.editor.Lttl;
import com.lttlgames.editor.LttlMeshGenerator;
import com.lttlgames.editor.annotations.GuiButton;
import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.GuiCallbackDescendants;
import com.lttlgames.editor.annotations.GuiCanNull;
import com.lttlgames.editor.annotations.GuiGroup;
import com.lttlgames.editor.annotations.GuiMax;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.graphics.LttlAntiAliaser;
import com.lttlgames.graphics.LttlMesh;
import com.lttlgames.helpers.LttlCallback;

@Persist(-90112)
public class LttlTriangleGenerator extends LttlMeshGenerator
{
	// points
	@Persist(9011200)
	@GuiGroup("Points")
	@GuiCallbackDescendants("updateMesh")
	public Vector2 point0 = new Vector2();
	@Persist(9011201)
	@GuiGroup("Points")
	@GuiCallbackDescendants("updateMesh")
	public Vector2 point1 = new Vector2();
	@Persist(9011202)
	@GuiGroup("Points")
	@GuiCallbackDescendants("updateMesh")
	public Vector2 point2 = new Vector2();

	// colors
	// NOTE need to disable autoUpdateColorAlpha and may need to enable forceMeshUpdate to auto update color and stuff
	@Persist(9011203)
	@GuiGroup("Colors")
	@GuiCallback("updateMesh")
	@GuiCallbackDescendants("updateMesh")
	@GuiCanNull
	public Color color0;
	@Persist(9011204)
	@GuiGroup("Colors")
	@GuiCallback("updateMesh")
	@GuiCallbackDescendants("updateMesh")
	@GuiCanNull
	public Color color1;
	@Persist(9011205)
	@GuiGroup("Colors")
	@GuiCallback("updateMesh")
	@GuiCallbackDescendants("updateMesh")
	@GuiCanNull
	public Color color2;

	// alphas
	@Persist(9011206)
	@GuiGroup("Alphas")
	@GuiCallback("updateMesh")
	@GuiMin(0)
	@GuiMax(1)
	public float alpha0 = 1;
	@Persist(9011207)
	@GuiGroup("Alphas")
	@GuiCallback("updateMesh")
	@GuiMin(0)
	@GuiMax(1)
	public float alpha1 = 1;
	@Persist(9011208)
	@GuiGroup("Alphas")
	@GuiCallback("updateMesh")
	@GuiMin(0)
	@GuiMax(1)
	public float alpha2 = 1;

	// UVs
	@Persist(9011209)
	@GuiGroup("UVs")
	@GuiCallback("updateMesh")
	@GuiCallbackDescendants("updateMesh")
	@GuiCanNull
	public Vector2 uv0;
	@Persist(90112010)
	@GuiGroup("UVs")
	@GuiCallback("updateMesh")
	@GuiCallbackDescendants("updateMesh")
	@GuiCanNull
	public Vector2 uv1;
	@Persist(90112011)
	@GuiGroup("UVs")
	@GuiCallback("updateMesh")
	@GuiCallbackDescendants("updateMesh")
	@GuiCanNull
	public Vector2 uv2;

	@Override
	public void onEditorCreate()
	{
		super.onEditorCreate();
		reset();
	}

	@GuiButton
	protected void reset()
	{
		float width = 2 * Lttl.game.getSettings().getWidthFactor() / 2f;
		point0.set(-width, -width);
		point1.set(0, width);
		point2.set(width, -width);
		updateMesh();
	}

	/**
	 * How we know if it is in edit mode or not
	 */
	private boolean isEditing = false;
	protected ArrayList<Handle> handles;

	/**
	 * Toggles edit mode for this path
	 */
	@GuiButton(order = 1)
	private void editToggle()
	{
		// if already editing, stop
		if (isEditing)
		{
			Lttl.editor.getGui().getSelectionController().unlockSelection();
			editorStop();
			return;
		}
		editorStart();
	}

	/**
	 * Toggles edit mode for this path
	 */
	@GuiButton(order = 0)
	private void editToggleLock()
	{
		// if already editing, stop
		if (isEditing)
		{
			Lttl.editor.getGui().getSelectionController().unlockSelection();
			editorStop();
			return;
		}
		Lttl.editor.getGui().getSelectionController().lockSelection();
		editorStart();
	}

	protected void editorStart()
	{
		isEditing = true;

		if (handles == null)
		{
			handles = new ArrayList<Handle>(3);
		}
		handles.add(createHandle(point0));
		handles.add(createHandle(point1));
		handles.add(createHandle(point2));
		Lttl.logNote("Started editing mesh.");
	}

	private void editorStop()
	{
		isEditing = false;

		// unregister all handles
		for (Handle h : handles)
		{
			h.unregister();
		}
		handles.clear();

		Lttl.logNote("Stopped editing mesh.");
	}

	protected HandleRect createHandle(Vector2 v)
	{
		return HandleRect.GenerateTransformRenderHandle(v, t(), true,
				new LttlCallback()
				{
					@Override
					public void callback(int id, Object... value)
					{
						updateMesh();
					}
				});
	}

	@Override
	public void updateMesh()
	{
		// obtain mesh
		LttlMesh mesh = getNewMesh(3);

		// add vertices
		addVertice(mesh, point0, uv0, color0, alpha0);
		addVertice(mesh, point1, uv1, color1, alpha1);
		addVertice(mesh, point2, uv2, color2, alpha2);

		// set indices
		mesh.addIndice(0);
		mesh.addIndice(1);
		mesh.addIndice(2);

		// set mesh
		r().setMesh(mesh);
		updateMeshAA();
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

	protected void addVertice(LttlMesh mesh, Vector2 pos, Vector2 uv,
			Color color, float alpha)
	{
		if ((color != null || alpha != 1) && r().autoUpdateMeshColorAlpha)
		{
			Lttl.logNote("Mesh Generator: Using custom vertex colors and/or alphas with autoUpdateMeshColorAlpha enabled.");
		}
		mesh.addVertice(pos.x, pos.y, uv != null ? uv.x : 1, uv != null ? uv.y
				: 1,
				color != null ? color.toFloatBits() : r().color.toFloatBits(),
				r().getWorldAlpha(false) * alpha);
	}

}
