package com.lttlgames.components;

import com.lttlgames.editor.Lttl;
import com.lttlgames.editor.LttlMeshGenerator;
import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.GuiGroup;
import com.lttlgames.editor.annotations.GuiMax;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.graphics.LttlAntiAliaser;
import com.lttlgames.graphics.LttlMesh;
import com.lttlgames.graphics.LttlMeshFactory;
import com.lttlgames.helpers.LttlMath;

//9
@Persist(-90126)
public class LttlShapeGenerator extends LttlMeshGenerator
{
	@Persist(9012600)
	@GuiCallback("onGuiUpdateMesh")
	public float radiusX = 2;
	@Persist(9012601)
	@GuiCallback("onGuiUpdateMesh")
	public float radiusY = 2;
	@Persist(9012603)
	@GuiCallback("onGuiUpdateMesh")
	@GuiMin(3)
	public int sides = 3;
	@Persist(9012608)
	@GuiCallback("onGuiUpdateMesh")
	@GuiMin(-360)
	@GuiMax(360)
	public float degrees = 360;
	@Persist(9012609)
	@GuiCallback("onGuiUpdateMesh")
	public float degreesOffset = 0;
	/**
	 * just uses radiusX
	 */
	@Persist(9012602)
	@GuiCallback("onGuiUpdateMesh")
	public boolean isCircle = true;

	@Persist(9012605)
	@GuiGroup("Outline")
	@GuiCallback("onGuiUpdateMesh")
	public boolean isOutline = false;
	@Persist(9012606)
	@GuiGroup("Outline")
	@GuiCallback("onGuiUpdateMesh")
	public float outlineWidth = 1;
	@Persist(9012607)
	@GuiGroup("Outline")
	@GuiCallback("onGuiUpdateMesh")
	public boolean centered = false;

	@Persist(9012604)
	@GuiGroup("Shape")
	@GuiCallback("onGuiUpdateMesh")
	public boolean isRadialUV = false;

	@Override
	public void onEditorCreate()
	{
		radiusX = Lttl.game.getSettings().getWidthFactor() * 1;
		radiusY = Lttl.game.getSettings().getWidthFactor() * 1;
		super.onEditorCreate();
	}

	@Override
	public void updateMesh()
	{
		LttlMesh mesh = getNewMesh();

		if (isOutline)
		{
			LttlMeshFactory.GenerateShapeOutline(mesh, radiusX,
					isCircle ? radiusX : radiusY, sides, degrees,
					degreesOffset,
					LttlMath.MaxAbsValueWithSign(outlineWidth, .0001f),
					centered);
			updateMeshAA();
		}
		else
		{
			LttlMeshFactory.GenerateShapeFill(mesh, radiusX, isCircle ? radiusX
					: radiusY, sides, degrees, degreesOffset, isRadialUV,
					updateActualAA());
		}

		r().setMesh(mesh);
	}

	@Override
	public void updateMeshAA(float calculatedAA)
	{
		if (r().getMesh() == null) return;

		if (isOutline)
		{
			if (calculatedAA > 0)
			{
				LttlAntiAliaser.AddAntiAliasingEither(r().getMesh(),
						calculatedAA, aaSettings);
				r().setMesh(r().getMesh());
			}

		}
		else
		{
			// can't really seperate the AA from mesh too easily for elipses because they have a center point
			updateMesh();
		}
	}
}
