package com.lttlgames.components;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Vector2;
import com.lttlgames.editor.Lttl;
import com.lttlgames.editor.LttlBasicTextureMeshGenerator;
import com.lttlgames.editor.LttlTexture;
import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.GuiCallbackDescendants;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.graphics.LttlAntiAliaser;
import com.lttlgames.graphics.LttlMesh;
import com.lttlgames.graphics.LttlMeshFactory;

@Persist(-90121)
public class LttlTrifoldMeshGenerator extends LttlBasicTextureMeshGenerator
{
	@Persist(9012100)
	@GuiCallback("onGuiListChange")
	@GuiCallbackDescendants("onGuiUpdateMesh")
	private ArrayList<TextureFoldObject> left = new ArrayList<TextureFoldObject>();
	@Persist(9012101)
	@GuiCallback("onGuiListChange")
	@GuiCallbackDescendants("onGuiUpdateMesh")
	private ArrayList<TextureFoldObject> right = new ArrayList<TextureFoldObject>();

	private LttlMesh centerMesh = new LttlMesh();

	public static Vector2 prevTop = new Vector2();
	public static Vector2 prevBottom = new Vector2();

	@SuppressWarnings("unused")
	private void onGuiListChange()
	{
		refreshObjects();
	}

	@Override
	public void onStart()
	{
		super.onStart();
		refreshObjects();
	}

	@Override
	public void onEditorStart()
	{
		super.onEditorStart();
		refreshObjects();
	}

	final public void refreshObjects()
	{
		for (TextureFoldObject tfo : left)
		{
			tfo.texture.refresh();
		}
		for (TextureFoldObject tfo : right)
		{
			tfo.texture.refresh();
		}
	}

	@Override
	public void updateMesh()
	{
		// create initial mesh
		super.updateMesh();
		// check if updating mesh was successful
		if (r().getMesh() == null) return;

		// save the generated mesh into the centerMesh
		centerMesh.set(r().getMesh());

		updateActualAA();

		// generate left folds
		if (left.size() > 0)
		{
			processSide(true, getActualAA());
		}
		// generate right folds
		if (right.size() > 0)
		{
			processSide(false, getActualAA());
		}

		combineMeshes();

		r().setMesh(r().getMesh());
	}

	@Override
	public void updateMeshAA(float calculatedAA)
	{
		// if just need to update AA, just recreate whole mesh
		updateMesh();
	}

	/**
	 * combines lefts, center, and rights meshes together into the renderer's main mesh, the vertices are not in order,
	 * so AA ust be added on each mesh before combining
	 */
	private void combineMeshes()
	{
		LttlMesh mesh = r().getMesh();
		mesh.clear();

		for (TextureFoldObject tfo : left)
		{
			if (tfo.mesh != null)
			{
				mesh.add(tfo.mesh);
			}
		}
		mesh.add(centerMesh);
		for (TextureFoldObject tfo : right)
		{
			if (tfo.mesh != null)
			{
				mesh.add(tfo.mesh);
			}
		}
	}

	/**
	 * @param tfo
	 * @param centerTex
	 * @param isLeftSide
	 * @param aaWidth
	 * @param worldAlpha
	 * @param meshScale
	 * @param prevTop
	 *            sets to new values of edge of fold
	 * @param prevBottom
	 *            sets to new values of edge of fold
	 * @param rendererColor
	 * @return
	 */
	public static void GenerateFoldMesh(TextureFoldObject tfo,
			LttlTexture centerTex, boolean isLeftSide, float aaWidth,
			float worldAlpha, float meshScale, Vector2 prevTop,
			Vector2 prevBottom, Color rendererColor)
	{

		QuadCorners topNext = isLeftSide ? QuadCorners.TopLeft
				: QuadCorners.TopRight;
		QuadCorners bottomNext = isLeftSide ? QuadCorners.BottomLeft
				: QuadCorners.BottomRight;
		QuadCorners topPrev = isLeftSide ? QuadCorners.TopRight
				: QuadCorners.TopLeft;
		QuadCorners bottomPrev = isLeftSide ? QuadCorners.BottomRight
				: QuadCorners.BottomLeft;

		// make sure has atlas region, otherwise skip
		AtlasRegion ar = tfo.texture.getAR();
		if (ar == null)
		{
			tfo.mesh = null;
			return;
		}

		// check if same texture
		if (ar.getTexture() != centerTex.getTex())
		{
			Lttl.logNote("LttlTrifoldGenerator: texture region " + ar.name
					+ " is not on the same texture as base texture region "
					+ centerTex.textureRegionName);
			tfo.mesh = null;
			return;
		}

		// create mesh from atlas regions
		tfo.mesh = LttlMeshFactory.GenerateQuad(tfo.mesh, ar, 1, 1, 0);

		// set previous points
		tfo.mesh.setPos(topPrev.value, prevTop.x
				+ (tfo.topAdjustment == null ? 0
						: (tfo.topAdjustment.x * meshScale)), prevTop.y
				+ (tfo.topAdjustment == null ? 0
						: (tfo.topAdjustment.y * meshScale)));
		tfo.mesh.setPos(bottomPrev.value, prevBottom.x
				+ (tfo.bottomAdjustment == null ? 0
						: (tfo.bottomAdjustment.x * meshScale)), prevBottom.y
				+ (tfo.bottomAdjustment == null ? 0
						: (tfo.bottomAdjustment.y * meshScale)));

		// save as new previous points
		if (tfo.positionsRelative)
		{
			prevTop.add(tfo.top);
			prevBottom.add(tfo.bottom);
		}
		else
		{
			prevTop.set(tfo.top);
			prevBottom.set(tfo.bottom);
		}

		// set new points (left)
		tfo.mesh.setPos(topNext.value, prevTop.x, prevTop.y);
		tfo.mesh.setPos(bottomNext.value, prevBottom.x, prevBottom.y);

		Color defaultColor = tfo.color == null ? rendererColor : tfo.color;

		// set alpha and color
		tfo.mesh.setAlphaAll(worldAlpha * tfo.alpha);
		tfo.mesh.setColorAll(defaultColor.toFloatBits());

		// apply customs
		if (tfo.custom != null)
		{
			tfo.custom.modifyVertices(tfo.mesh, defaultColor, worldAlpha
					* tfo.alpha);
		}

		// densify
		if (tfo.densifySteps > 0)
		{
			LttlMeshFactory.DensifyQuad(
					tfo.mesh,
					tfo.densifySteps,
					true,
					tfo.custom != null ? tfo.custom
							.getCustomColors(defaultColor) : null);
		}

		// add AA
		if (aaWidth > 0)
		{
			LttlAntiAliaser.AddAntiAliasingSimple(tfo.mesh, aaWidth, false, 0);
		}
	}

	private void processSide(boolean isLeftSide, float aaWidth)
	{
		QuadCorners topNext = isLeftSide ? QuadCorners.TopLeft
				: QuadCorners.TopRight;
		QuadCorners bottomNext = isLeftSide ? QuadCorners.BottomLeft
				: QuadCorners.BottomRight;

		float worldAlpha = r().getWorldAlpha(false);
		float meshScale = getMeshScale(r().getTex0().getAR());

		// get starting previous points from center mesh
		centerMesh.getPos(
				LttlMeshFactory.getDensifiedQuadIndex(topNext, densifySteps),
				prevTop);
		centerMesh
				.getPos(LttlMeshFactory.getDensifiedQuadIndex(bottomNext,
						densifySteps), prevBottom);

		LttlTexture centerTex = r().getTex0();
		for (TextureFoldObject tfo : isLeftSide ? left : right)
		{
			GenerateFoldMesh(tfo, centerTex, isLeftSide, aaWidth, worldAlpha,
					meshScale, prevTop, prevBottom, r().getColor());
		}
	}
}
