package com.lttlgames.editor;

import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ShortArray;
import com.lttlgames.editor.LoopManager.RenderView;
import com.lttlgames.editor.annotations.GuiGroup;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.helpers.LttlMath;
import com.lttlgames.helpers.Vector2Array;

//2
@Persist(-9012)
abstract public class LttlMultiRenderer extends LttlRenderer
{
	private static final Matrix3 staticTmpM3 = new Matrix3();
	private static final Matrix4 staticTmpM4 = new Matrix4();
	private static final Rectangle staticTmpRect = new Rectangle();
	private static final Vector2Array staticTempV2DebugArray = new Vector2Array(
			0);
	private static final Rectangle staticRectDebugArray = new Rectangle();

	/**
	 * This will check each draw to see if it should be drawn. This probably is inefficient, and instead you should make
	 * this false and use {@link LttlRenderer#checkInCameraView} with a {@link LttlRenderer#customBoundingRect} to
	 * check, but that will either render none or all particles, useful if particles are predictable.
	 */
	@Persist(901200)
	@GuiGroup("Render Check")
	public boolean checkEachMultiDraw = false;
	/**
	 * after and before each multi draw, should it callback post processing
	 */
	@Persist(901201)
	@GuiGroup("Settings")
	public boolean postProcessingCallbackEachDraw = false;
	/**
	 * If multi renders have any rotation, they will rotate their matrix using a lookup table instead of strict math.
	 */
	@Persist(901202)
	@GuiGroup("Settings")
	public boolean useLookupForRotation = true;
	/**
	 * this just helps in editor for seeing when the custom bounding is out of camera, it forces all particles to be
	 * marked as hidden since thats what actually happening in play mode<br>
	 * This should never be true when rendering play view because if it is true, it doesn't render the multi's
	 */
	boolean parentRendererNotDrawing = false;

	/**
	 * This is where you prepare your data/objects for rendering (and optionally update them) and call renderDraw()
	 * multiple times.
	 */
	abstract protected void render();

	/**
	 * Renders with the worldRenderMatrix (which includes camera matrix) and uses mesh's local vertices, if null, then
	 * just renders with mesh's world vertices, and it should have world vales<br>
	 * All multi renderer render draws must call this method.<br>
	 * This is where each draw call is checked for rendering (if enabled)
	 * 
	 * @param worldRenderMatrix
	 *            can be null
	 */
	final protected void renderDraw(Matrix4 worldRenderMatrix)
	{
		// check each particle
		if (checkEachMultiDraw)
		{
			if (worldRenderMatrix != null)
			{
				// don't have checkEachMultiDraw working for non preMultiplyMesh renders, you're stupid anyways for
				// doing this, so just don't. It kind of defies the whole purpose of a multirenderer
				Lttl.Throw();
			}

			Rectangle rect = staticTmpRect;

			// use world vertices and get boudning rect (already transformed)
			getMesh().getBoundingRect(true, false, rect);
			if (Lttl.loop.getRenderView() == RenderView.Editor)
			{
				if (parentRendererNotDrawing)
				{
					Lttl.loop.markNonRenders(rect);
				}
			}
			else if (!Lttl.loop.getCurrentRenderingCamera()
					.getViewportRotatedAABB().overlaps(rect))
			{
				Lttl.loop.markNonRenders(rect);
				return;
			}
		}
		else if (parentRendererNotDrawing)
		{
			Lttl.loop.markNonRenders(getMesh().getBoundingRect(true, false,
					staticTmpRect));
		}

		// DEBUG
		if (Lttl.game.inEditor()
				&& Lttl.loop.getCurrentRenderingCamera().isEditorCamera())
		{
			if (drawMeshOultine)
			{
				Lttl.debug.drawPolygonOutline(
						getMesh().getVerticesPosMain(true, staticTempV2DebugArray), 0,
						Lttl.editor.getSettings().colorMeshOultine);
			}
			if (drawMeshBoundingRectAxisAligned)
			{
				Lttl.debug.drawRectOutline(LttlMath.GetAABB(getMesh()
						.getVerticesPosMain(true, staticTempV2DebugArray),
						staticRectDebugArray), 0,
						Lttl.editor.getSettings().colorMeshBounding);
			}
			if (drawMeshBoundingRect)
			{
				Lttl.debug.drawPolygonOutline(
						getMesh().getVerticesPosMain(true, staticTempV2DebugArray), 0,
						Lttl.editor.getSettings().colorMeshBounding);
			}
			if (drawMeshTriangles)
			{
				ShortArray indices = getMesh().getIndicesArray();
				Vector2Array vertices = getMesh().getVerticesPosMain(true,
						staticTempV2DebugArray);
				for (int i = 0; i < indices.size / 3; i++)
				{
					Lttl.debug.drawLine(vertices.getX(indices.get(i * 3)),
							vertices.getY(indices.get(i * 3)),
							vertices.getX(indices.get(i * 3 + 1)),
							vertices.getY(indices.get(i * 3 + 1)), 0,
							Lttl.editor.getSettings().colorMeshOultine);
					Lttl.debug.drawLine(vertices.getX(indices.get(i * 3 + 1)),
							vertices.getY(indices.get(i * 3 + 1)),
							vertices.getX(indices.get(i * 3 + 2)),
							vertices.getY(indices.get(i * 3 + 2)), 0,
							Lttl.editor.getSettings().colorMeshOultine);
					Lttl.debug.drawLine(vertices.getX(indices.get(i * 3 + 2)),
							vertices.getY(indices.get(i * 3 + 2)),
							vertices.getX(indices.get(i * 3)),
							vertices.getY(indices.get(i * 3)), 0,
							Lttl.editor.getSettings().colorMeshOultine);
				}
			}
		}

		// RENDER, and check post processing callback, if editor, always render, do not post process check
		if (!postProcessingCallbackEachDraw
				|| Lttl.loop.getRenderView() == RenderView.Editor
				|| Lttl.loop.getProcessing() == null
				|| Lttl.loop.getProcessing().beforeMultiRenderDraw(t()))
		{
			Lttl.loop.renderRenderer(this, worldRenderMatrix);
		}
	}

	/**
	 * Returns the world render matrix using the precalcualted worldMatrix, this can be sent straight to graphics card.
	 * 
	 * @param posX
	 * @param posY
	 * @param sclX
	 * @param sclY
	 * @param originX
	 * @param originY
	 * @param rotation
	 * @param shearX
	 * @param shearY
	 * @param worldMatrix
	 *            already calcualted
	 * @param ouptutM4
	 *            where the rendererMatrix will be saved
	 * @param isGlobal
	 * @return
	 */
	final protected Matrix4 getRenderMatrix(float posX, float posY, float sclX,
			float sclY, float originX, float originY, float rotation,
			float shearX, float shearY, Matrix4 worldMatrix, Matrix4 ouptutM4,
			boolean isGlobal)
	{
		// populate staticTmpM3 with the local transform matrix
		getLocalTransformMatrix(posX, posY, sclX, sclY, originX, originY,
				rotation, shearX, shearY, staticTmpM3, isGlobal);

		// if different matrices, set the output as worldRenderMatrix, could be the same
		if (ouptutM4 != worldMatrix)
		{
			ouptutM4.set(worldMatrix);
		}

		// multiply the worldMatrix(camera matrix/worldRenderTransform) to the local transform
		// matrix/worldTransformMeshScale matrix
		ouptutM4.mul(staticTmpM4.set(staticTmpM3));
		return ouptutM4;
	}

	/**
	 * Returns the render matrix, this can be sent straight to graphics card. Generates the world and local matrix.
	 * 
	 * @return
	 */
	final protected Matrix4 getRenderMatrix(float posX, float posY, float sclX,
			float sclY, float originX, float originY, float rotation,
			float shearX, float shearY, Matrix4 ouptutM4, boolean isGlobal)
	{
		// populate ouptutM4 with the world matrix
		return getRenderMatrix(posX, posY, sclX, sclY, originX, originY,
				rotation, shearX, shearY, getWorldMatrix(ouptutM4, isGlobal),
				ouptutM4, isGlobal);
	}

	/**
	 * Returns the world matrix (camera, worldTransform, meshScale) that you multiply the local transform matrix by
	 * 
	 * @param ouptutM4
	 * @param isGlobal
	 * @return
	 */
	final protected Matrix4 getWorldMatrix(Matrix4 ouptutM4, boolean isGlobal)
	{
		// shouldn't be getting world matrix, should just be setting the mesh to world values
		Lttl.Throw(preMultiplyWorldMesh);

		if (isGlobal)
		{
			// just use the camera as matrix
			ouptutM4.set(Lttl.loop.getCurrentRenderingCamera().worldMatrix);
		}
		else
		{
			// if not global then use the already calculated worldRenderMatrix (includes camera matrix and
			// worldTransform)
			ouptutM4.set(worldRenderMatrix);
		}

		return ouptutM4;
	}

	/**
	 * Returns the local transform matrix for this MultiObject, takes into consideration if isGlobal
	 * 
	 * @param obj
	 * @return
	 */
	final protected Matrix3 getLocalTransformMatrix(float posX, float posY,
			float sclX, float sclY, float originX, float originY,
			float rotation, float shearX, float shearY, Matrix3 ouptut,
			boolean isGlobal)
	{
		// just create local transform
		return LttlMath.GenerateTransormMatrix(posX, posY, sclX, sclY, originX,
				originY, rotation, useLookupForRotation, shearX, shearY,
				(isGlobal) ? null : transform(), ouptut);
	}

}
