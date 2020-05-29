package com.lttlgames.components;

import java.util.ArrayList;

import com.badlogic.gdx.math.Vector2;
import com.lttlgames.editor.Handle;
import com.lttlgames.editor.HandleRect;
import com.lttlgames.editor.Lttl;
import com.lttlgames.editor.annotations.AnimateField;
import com.lttlgames.editor.annotations.GuiButton;
import com.lttlgames.editor.annotations.GuiCallbackDescendants;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.graphics.LttlMesh;
import com.lttlgames.helpers.LttlCallback;
import com.lttlgames.tweenengine.TweenGetterSetter;

@Persist(-90109)
public class LttlQuadMeshGenerator extends LttlQuadGeneratorAbstract
{
	@Persist(9010900)
	@GuiCallbackDescendants("onGuiUpdateMesh")
	@AnimateField(0)
	public LttlQuadPoints points = new LttlQuadPoints();

	/* EDITOR */
	private boolean isEditing = false;
	private ArrayList<Handle> handles;

	@GuiButton
	protected void reset()
	{
		float halfSize = .5f * (!Lttl.game.isSettingUp() ? Lttl.game
				.getSettings().getWidthFactor() : 1);
		points.topLeft.set(-1 * halfSize, halfSize);
		points.topRight.set(halfSize, halfSize);
		points.bottomRight.set(halfSize, -1 * halfSize);
		points.bottomLeft.set(-1 * halfSize, -1 * halfSize);

		updateMesh();
	}

	@Override
	public void onEditorCreate()
	{
		reset();
	}

	@Override
	public void updateMesh()
	{
		// obtain mesh
		LttlMesh mesh = getNewMesh(4);

		// add vertices
		// top Left
		mesh.addVertice(points.topLeft.x, points.topLeft.y, 0, 1, r()
				.getColor().toFloatBits(), 1);
		// top right
		mesh.addVertice(points.topRight.x, points.topRight.y, 1, 1, r()
				.getColor().toFloatBits(), 1);
		// bottom right
		mesh.addVertice(points.bottomRight.x, points.bottomRight.y, 1, 0, r()
				.getColor().toFloatBits(), 1);
		// bottom Left
		mesh.addVertice(points.bottomLeft.x, points.bottomLeft.y, 0, 0, r()
				.getColor().toFloatBits(), 1);

		// set indices
		mesh.addIndice(0);
		mesh.addIndice(1);
		mesh.addIndice(2);
		mesh.addIndice(2);
		mesh.addIndice(3);
		mesh.addIndice(0);

		// modify vertices (offset, alpha, colors, uvs)
		this.modifyVertices(mesh, 1, 1);

		// set mesh
		r().setMesh(mesh);
		updateMeshAA();
	}

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
			handles = new ArrayList<Handle>(4);
		}
		handles.add(createHandle(points.topLeft));
		handles.add(createHandle(points.topRight));
		handles.add(createHandle(points.bottomLeft));
		handles.add(createHandle(points.bottomRight));
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
	public TweenGetterSetter getTweenGetterSetter(int animID)
	{
		return super.getTweenGetterSetter(animID);
	}
}
