package com.lttlgames.editor;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;
import com.lttlgames.editor.LoopManager.RenderType;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.graphics.LttlBlendMode;
import com.lttlgames.graphics.LttlMesh;
import com.lttlgames.graphics.LttlMeshFactory;
import com.lttlgames.graphics.LttlShader;
import com.lttlgames.helpers.EaseType;
import com.lttlgames.helpers.LttlCameraTransformState;
import com.lttlgames.helpers.LttlGeometry;
import com.lttlgames.helpers.LttlMath;
import com.lttlgames.helpers.LttlObjectHelper;
import com.lttlgames.tweenengine.Timeline;
import com.lttlgames.tweenengine.Tween;
import com.lttlgames.tweenengine.TweenGetterSetter;

//4
@Persist(-905)
public class LttlCamera
{
	@Persist(90501)
	public Vector2 position = new Vector2(0, 0);
	private Vector2 lastPosition = new Vector2(Float.POSITIVE_INFINITY,
			Float.POSITIVE_INFINITY);
	@Persist(90502)
	public float rotation = 0;
	private float lastRotation = Float.POSITIVE_INFINITY; // forces a change on start
	/**
	 * Must be greater than 0.
	 */
	@Persist(90503)
	public float zoom = 1;
	private float lastZoom = Float.POSITIVE_INFINITY;
	private float lastAAZoom = 1;
	/**
	 * This holds the real camera, no clipping area.
	 */
	OrthographicCamera orthoCamera;
	/**
	 * Non transformed rect (no zoom, rotation, or position change), this is the camera viewport with clipping (if
	 * editor camera, then it's same as orthoCamera since there is no clipping)
	 */
	final Rectangle staticVirtualCameraRect = new Rectangle();
	/**
	 * Representing the bounding rect of the transformedCameraPolygon (rotated camera)
	 */
	final Rectangle boundingRectRotatedAxisAligned = new Rectangle();
	/**
	 * Rectangle representing the camera's position and zoom
	 */
	final Rectangle transformedRectangle = new Rectangle();
	final float[] viewportPolygon = new float[8];
	private Vector2 lastVector2ArrayUpdatePosition = new Vector2(
			Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
	private float lastVector2ArrayUpdateRotation = Float.POSITIVE_INFINITY;
	private float lastVector2ArrayUpdateZoom = Float.POSITIVE_INFINITY;
	final Matrix4 worldMatrix = new Matrix4();
	final Matrix4 clippingMatrix = new Matrix4();
	private float[] minAspectRatioOutline = new float[8];
	private float[] maxAspectRatioOutline = new float[8];
	private float actualAspectRatio;
	private float useableAspectRatio;

	float viewportPixelWidth;
	float viewportPixelHeight;

	boolean autoUpdatingMeshesOnZoom = false;
	private int autoUpdateStartIndex = 0;
	private Matrix3 tmp = new Matrix3();

	public OrthographicCamera getOrthoCamera()
	{
		return orthoCamera;
	}

	/**
	 * Checks if the position, rotation, or zoom of the camera has changed since last camera.update(), which is ran
	 * every frame or manually.
	 * 
	 * @return
	 */
	public boolean isChanged()
	{
		if (lastPosition.x != position.x || lastPosition.y != position.y
				|| lastRotation != rotation || lastZoom != zoom) { return true; }
		return false;
	}

	/**
	 * Works with isChanged() to clear values so can detect change next frame.
	 */
	void setLastValues()
	{
		lastPosition.set(position);
		lastRotation = rotation;
		lastZoom = zoom;
	}

	Matrix4 projection = new Matrix4();
	Matrix4 view = new Matrix4();
	Matrix4 combined = new Matrix4();

	/**
	 * This is called once all LttlTransforms have been updated and staged.<br>
	 * updates the projection and view matrices based on any transformations done this frame. if zoom changed, then
	 * update meshes (AA), if play camera
	 */
	void update()
	{
		// generate real matrix
		orthoCamera.position.set(position.x, position.y, 0);
		// reset rotation
		orthoCamera.direction.set(0, 0, -1);
		orthoCamera.up.set(0, 1, 0);
		orthoCamera.update();

		orthoCamera.position.set(position.x, position.y, 0);
		orthoCamera.rotate(rotation, 0, 0, 1);
		orthoCamera.zoom = 1 / zoom;
		orthoCamera.update();

		// add rotation and set zoom
		worldMatrix.set(orthoCamera.combined);

		if (this == Lttl.game.getSettings().getTargetCamera())
		{
			// AA updating
			// auto update meshes (AA) on camera zoom if it passes threshold
			if (LttlMath.abs(zoom - lastAAZoom) >= Lttl.game.getSettings().zoomThreshold)
			{
				// starts or continues
				autoUpdatingMeshesOnZoom = true;
				lastAAZoom = zoom;
			}
			// keep on updating meshes, even when camera zoom stopped changing, since you need to make sure all objects
			// get updated
			if (autoUpdatingMeshesOnZoom)
			{
				updateMeshes();
			}
		}

		setLastValues();
	}

	public Matrix4 getWorldMatrix()
	{
		return worldMatrix;
	}

	// OPTIMIZE maybe check if they are even visible before updating them, then when become visible you update them,
	// like need a start rending method, so when start rendering it can update then only
	private void updateMeshes()
	{
		// quick out
		if (Lttl.loop.transformsOrdered.size() == 0)
		{
			autoUpdatingMeshesOnZoom = false;
			autoUpdateStartIndex = 0; // reset
		}

		int updateCount = 0;
		int iterationCount = 0;
		float targetZoom = Lttl.game.getSettings().getTargetCamera().zoom;
		int index = autoUpdateStartIndex - 1;

		while (true)
		{
			index = LttlMath.loopIndex(index + 1,
					Lttl.loop.transformsOrdered.size());

			iterationCount++;
			if (iterationCount > Lttl.loop.transformsOrdered.size())
			{
				// went through whole list
				// if no updates then stop updating
				if (updateCount == 0)
				{
					autoUpdatingMeshesOnZoom = false;
					autoUpdateStartIndex = 0; // reset
				}
				return;
			}

			// break out if reached limit
			if (updateCount >= Lttl.game.getSettings().maxAAMeshUpdatesPerFrameOnZoom)
			{
				// sets the index to start at next frame, will keep on updating
				autoUpdateStartIndex = index;
				return;
			}

			// check if should update this transform's mesh generator
			LttlTransform lt = Lttl.loop.transformsOrdered.get(index);

			// is transform enabled
			if (!lt.enabledThisFrame) continue;
			// does it have an enabled renderer that currently has a mesh (this way not creating meshes that aren't
			// there already)
			if (lt.renderer() == null || !lt.renderer().isEnabled()
					|| lt.renderer().getMesh() == null) continue;
			// does it have an enabled mesh generator
			if (lt.renderer.generator() == null
					|| !lt.renderer.generator().isEnabled()) continue;
			// has this object already been updated at this zoom amount, if so skip it
			if (lt.renderer.generator().getCameraZoomOnLastUpdateMesh() == targetZoom)
				continue;
			// check if has any AA Settings, if camera zoom dependent, autoupdate enabled and if it's actual AA is not 0
			if (lt.renderer.generator().aaSettings == null
					|| !lt.renderer.generator().aaSettings.cameraZoomDependent
					|| !lt.renderer.generator().aaSettings.autoUpdateOnCameraZoom
					// don't need to check world because this is ran after all transforms have been updated and staged
					|| lt.renderer.generator().updateActualAA(false) == 0)
				continue;

			// the AA was updated in prior conditional statement
			lt.renderer.generator().updateMeshAA(
					lt.renderer.generator().getActualAA());

			// keep a count of updates so can know if all are updated and can stop checking in the following frames
			updateCount++;
		}
	}

	/**
	 * Is the camera currently updating meshes because zoom changed
	 * 
	 * @return
	 */
	public boolean isAutoUpdatingMeshesOnZoom()
	{
		return autoUpdatingMeshesOnZoom;
	}

	void onResize()
	{
		if (orthoCamera == null)
		{
			orthoCamera = new OrthographicCamera();
		}

		// gets aspectRatio based on the new screen size
		actualAspectRatio = getRawPixelWidth() / getRawPixelHeight();

		// only use a ratio between the min and max (inclusive) for camera, also maybe use defaultAspectRatio
		useableAspectRatio = (Lttl.game.inEditor() && Lttl.editor.getSettings().clampToDefaultAspectRatio) ? Lttl.editor
				.getSettings().defaultAspectRatio : LttlMath.clamp(
				actualAspectRatio, Lttl.game.getSettings().minAspectRatio,
				Lttl.game.getSettings().maxAspectRatio);

		// calculate viewportHeight based on the useableAspectRatio (in game units)
		float viewportWidth;
		float viewportHeight;
		viewportWidth = Lttl.game.getSettings().gameUnitsAcrossScreenWidth;
		viewportHeight = Lttl.game.getSettings().gameUnitsAcrossScreenWidth
				/ useableAspectRatio;

		// calculate the orthoCamera size in game units, it will always be bigger or the same size as viewport width and
		// height
		if (actualAspectRatio > useableAspectRatio)
		{
			// too wide, set height as viewport, and calculate longer width
			orthoCamera.viewportHeight = viewportHeight;
			orthoCamera.viewportWidth = viewportHeight * actualAspectRatio;
		}
		else if (actualAspectRatio < useableAspectRatio)
		{
			// too tall, set width as viewport, and calculate tall height
			orthoCamera.viewportHeight = viewportWidth / actualAspectRatio;
			orthoCamera.viewportWidth = viewportWidth;
		}
		else
		{
			orthoCamera.viewportWidth = viewportWidth;
			orthoCamera.viewportHeight = viewportHeight;
		}

		if (isPlayCamera())
		{
			// if play camera virtual viewport is always fixed to the aspect ratio, this allows clipping
			staticVirtualCameraRect.setWidth(viewportWidth)
					.setHeight(viewportHeight).setCenter(0, 0);
		}
		else
		{
			// if editor camera, there is no clipping, so use ortho
			staticVirtualCameraRect.setWidth(orthoCamera.viewportWidth)
					.setHeight(orthoCamera.viewportHeight).setCenter(0, 0);
		}

		// update viewport pixel dimensions
		viewportPixelHeight = getRawPixelHeight()
				* (staticVirtualCameraRect.height / orthoCamera.viewportHeight);
		viewportPixelWidth = getRawPixelWidth()
				* (staticVirtualCameraRect.width / orthoCamera.viewportWidth);
	}

	/**
	 * Returns the static viewport height in game units with zoom as 1 (excludes any clipping area, use orthocamera for
	 * that)
	 * 
	 * @return
	 */
	public float getViewportHeightStatic()
	{
		return staticVirtualCameraRect.height;
	}

	/**
	 * Returns the static viewport width in game units with zoom as 1 (excludes any clipping area, use orthocamera for
	 * that).
	 * 
	 * @return
	 */
	public float getViewportWidthStatic()
	{
		return staticVirtualCameraRect.width;
	}

	/**
	 * Returns the viewport height in game units affected by zoom (excludes any clipping areat, use orthocamera for
	 * that)
	 * 
	 * @return
	 */
	public float getViewportHeight()
	{
		return getViewportAABB().height;
	}

	/**
	 * Returns the viewport width in game units affected by zoom (excludes any clipping area, use orthocamera for that).
	 * 
	 * @return
	 */
	public float getViewportWidth()
	{
		return getViewportAABB().width;
	}

	/**
	 * Only in editor.
	 */
	void debugDraw()
	{
		if (!Lttl.game.inEditor()) return;

		if (Lttl.editor.getSettings().drawAspectRatioGuides)
		{
			// getRect() already has zoom factored into it
			LttlMath.TransformRectangle(
					getViewportAABB(),
					rotation,
					1,
					(useableAspectRatio / Lttl.game.getSettings().minAspectRatio),
					minAspectRatioOutline);

			LttlMath.TransformRectangle(
					getViewportAABB(),
					rotation,
					1,
					(useableAspectRatio / Lttl.game.getSettings().maxAspectRatio),
					maxAspectRatioOutline);

			Lttl.debug.drawPolygonOutline(minAspectRatioOutline,
					LttlDebug.WIDTH_SMALL * Lttl.debug.eF(),
					Lttl.editor.getSettings().colorCameraRatioGuides);
			Lttl.debug.drawPolygonOutline(maxAspectRatioOutline,
					LttlDebug.WIDTH_SMALL * Lttl.debug.eF(),
					Lttl.editor.getSettings().colorCameraRatioGuides);
		}

		if (Lttl.editor.getSettings().drawCameraOutline)
		{
			Lttl.debug.drawPolygonOutline(getViewportPolygon(false),
					LttlDebug.WIDTH_SMALL * Lttl.debug.eF(),
					Lttl.editor.getSettings().colorCameraOutline);
		}

		if (Lttl.editor.getSettings().drawCameraAxisAlignedRect)
		{
			Lttl.debug.drawRectOutline(getViewportRotatedAABB(), 0,
					Lttl.editor.getSettings().colorRenderCheck);
		}

		if (Lttl.editor.getSettings().drawRuleOfThirds)
		{
			Vector2 a = new Vector2();
			Vector2 b = new Vector2();

			LttlModeOption mode = Lttl.debug.getMode();
			Lttl.debug.setMode(Lttl.editor.getSettings().drawRuleOfThirdsMode);

			// vertical lines
			Lttl.debug.drawLine(unProjectPercentage(1 / 3f, 1, a),
					unProjectPercentage(1 / 3f, 0, b), LttlDebug.WIDTH_SMALL
							* Lttl.debug.mF(),
					Lttl.editor.getSettings().colorRuleOfThirds);
			Lttl.debug.drawLine(unProjectPercentage(2 / 3f, 1, a),
					unProjectPercentage(2 / 3f, 0, b), LttlDebug.WIDTH_SMALL
							* Lttl.debug.mF(),
					Lttl.editor.getSettings().colorRuleOfThirds);

			// horizontal lines
			Lttl.debug.drawLine(unProjectPercentage(0, 1 / 3f, a),
					unProjectPercentage(1, 1 / 3f, b), LttlDebug.WIDTH_SMALL
							* Lttl.debug.mF(),
					Lttl.editor.getSettings().colorRuleOfThirds);
			Lttl.debug.drawLine(unProjectPercentage(0, 2 / 3f, a),
					unProjectPercentage(1, 2 / 3f, b), LttlDebug.WIDTH_SMALL
							* Lttl.debug.mF(),
					Lttl.editor.getSettings().colorRuleOfThirds);

			Lttl.debug.setMode(mode);
		}
	}

	// OPTIMIZE cache the clipping mesh (combine ther vertices and indices) and only make it on resize, and if game
	// settings change them call resize or delete the meshes, should still take 1 batch still
	void drawNonAspectRatioBorders()
	{
		// only draws if necessary, if you can't see them, then they aren't drawing, since it takes two batches, this is
		// nice
		if (actualAspectRatio != useableAspectRatio)
		{
			// clip edges if aspect ratio is not allowed.
			if (actualAspectRatio < useableAspectRatio)
			{
				// clipping at top and bottom
				clippingMatrix
						.idt()
						.translate(
								0,
								((getClippingPixelHeight() / 2) + (getViewportPixelHeightStatic() / 2))
										/ (getRawPixelHeight() / 2), 0)
						.scale(2,
								getClippingPixelHeight()
										/ (getRawPixelHeight() / 2), 0);
				LttlMesh mesh = LttlMeshFactory.GetQuadMeshShared();
				mesh.updateColorAlpha(Lttl.game.getSettings().clippingColor, 1);
				Lttl.loop.renderDraw(RenderType.Other, LttlBlendMode.NONE,
						LttlShader.SimpleColorShader, null, null,
						mesh.getVerticesArray(), mesh.getIndicesArray(),
						clippingMatrix, null);

				clippingMatrix
						.idt()
						.translate(
								0,
								-((getClippingPixelHeight() / 2) + (getViewportPixelHeightStatic() / 2))
										/ (getRawPixelHeight() / 2), 0)
						.scale(2,
								getClippingPixelHeight()
										/ (getRawPixelHeight() / 2), 0);
				Lttl.loop.renderDraw(RenderType.Other, LttlBlendMode.NONE,
						LttlShader.SimpleColorShader, null, null,
						mesh.getVerticesArray(), mesh.getIndicesArray(),
						clippingMatrix, null);
			}
			else
			{
				// clipping at left and right
				clippingMatrix
						.idt()
						.translate(
								((getClippingPixelWidth() / 2) + (getViewportPixelWidthStatic() / 2))
										/ (getRawPixelWidth() / 2), 0, 0)
						.scale(getClippingPixelWidth()
								/ (getRawPixelWidth() / 2), 2, 0);
				LttlMesh mesh = LttlMeshFactory.GetQuadMeshShared();
				mesh.updateColorAlpha(Lttl.game.getSettings().clippingColor, 1);
				Lttl.loop.renderDraw(RenderType.Other, LttlBlendMode.NONE,
						LttlShader.SimpleColorShader, null, null,
						mesh.getVerticesArray(), mesh.getIndicesArray(),
						clippingMatrix, null);

				clippingMatrix
						.idt()
						.translate(
								-((getClippingPixelWidth() / 2) + (getViewportPixelWidthStatic() / 2))
										/ (getRawPixelWidth() / 2), 0, 0)
						.scale(getClippingPixelWidth()
								/ (getRawPixelWidth() / 2), 2, 0);
				Lttl.loop.renderDraw(RenderType.Other, LttlBlendMode.NONE,
						LttlShader.SimpleColorShader, null, null,
						mesh.getVerticesArray(), mesh.getIndicesArray(),
						clippingMatrix, null);
			}
		}
	}

	/**
	 * Returns the pixel height of this camera's viewport (excluding clipping area), static meaning no zoom
	 * 
	 * @return
	 */
	public float getViewportPixelHeightStatic()
	{
		return viewportPixelHeight;
	}

	/**
	 * Returns the pixel width of this camera's viewport (excluding clipping area), static meaning no zoom
	 * 
	 * @return
	 */
	public float getViewportPixelWidthStatic()
	{
		return viewportPixelWidth;
	}

	/**
	 * The screen height (including any clipping area) in pixels for this view. NOTE: This is unique to editor and play
	 * viewport.
	 * 
	 * @return
	 */
	public float getRawPixelHeight()
	{
		float height = (float) Gdx.graphics.getHeight();

		if (Lttl.game.inEditor())
		{
			height *= (isEditorCamera()) ? Lttl.editor.getSettings().editorViewRatio
					: 1 - Lttl.editor.getSettings().editorViewRatio;
			return height;
		}
		else
		{
			return height;
		}
	}

	/**
	 * The screen width (including any clipping area) in pixels.
	 * 
	 * @return
	 */
	public float getRawPixelWidth()
	{
		return (float) Gdx.graphics.getWidth();
	}

	/**
	 * Returns the bounding rect for the camera, with any rotations and zoom, but it is axis aligned. Useful especially
	 * when camera is rotating. DO NOT MODIFY THIS!<br>
	 * If you want the true transformed rectangle with rotation then use {@link #getViewportPolygon(boolean)}.
	 * 
	 * @return
	 */
	public Rectangle getViewportRotatedAABB()
	{
		if (rotation != 0)
		{
			// update rotated rect if necessary, then update the axis aligned bounding rect from it
			return LttlMath.GetAABB(getViewportPolygon(true),
					boundingRectRotatedAxisAligned);
		}
		else
		{
			// if no rotation, just return the transformed rectangle
			return getViewportAABB();
		}
	}

	/**
	 * Returns the transformed rectangle representing this camera's viewport (zoom and position) excluding clipping.<br>
	 * <b>THIS DOES NOT INCLUDE ROTATION</b> instead use {@link #getViewportPolygon(boolean)} or
	 * {@link #getViewportRotatedAABB()}<br>
	 * DO NOT MODIFY THIS!
	 * 
	 * @return
	 */
	public Rectangle getViewportAABB()
	{
		transformedRectangle.setHeight(staticVirtualCameraRect.height / zoom);
		transformedRectangle.setWidth(staticVirtualCameraRect.width / zoom);
		// need to set center after setHeight and setWidth
		transformedRectangle.setCenter(position.x, position.y);
		return transformedRectangle;
	}

	/**
	 * Returns the Vector2Array of this camera's viewport, even if not rotated. If not rotated, may be more efficient to
	 * use {@link #getViewportAABB()}.
	 * 
	 * @paramc check, if true will only update Vector2Array if position, rotation, or zoom changed
	 * @return
	 */
	public float[] getViewportPolygon(boolean check)
	{
		// checks if anything has changed since last polygon generation
		if (!check
				|| (!lastVector2ArrayUpdatePosition.equals(position)
						|| lastVector2ArrayUpdateRotation != rotation || lastVector2ArrayUpdateZoom != zoom))
		{
			// update the polygon
			// getRect() already has zoom factored into it
			LttlMath.TransformRectangle(getViewportAABB(), rotation, 1, 1,
					viewportPolygon);

			// save last update values
			lastVector2ArrayUpdatePosition.set(position);
			lastVector2ArrayUpdateRotation = rotation;
			lastVector2ArrayUpdateZoom = zoom;
		}
		return viewportPolygon;
	}

	/**
	 * This is just one side.
	 */
	public float getClippingPixelWidth()
	{
		return (this.getRawPixelWidth() - this.getViewportPixelWidthStatic()) / 2;
	}

	/**
	 * This is just one side.
	 */
	public float getClippingPixelHeight()
	{
		return (this.getRawPixelHeight() - this.getViewportPixelHeightStatic()) / 2;
	}

	/**
	 * Returns the none rotated or scaled camera x position in game units. Assuming the position is actually in the
	 * viewport. This is good for GUI.
	 * 
	 * @param screenX
	 * @return
	 */
	public float unProjectLinearX(float screenX)
	{
		return (((screenX - getClippingPixelWidth() - this
				.getViewportPixelWidthStatic() / 2) / this
				.getViewportPixelWidthStatic()) * this.getViewportWidthStatic())
				+ position.x;
	}

	/**
	 * Returns the non rotated or scaled camera mouse y position in game units. Assuming the position is actually in the
	 * viewport.
	 * 
	 * @param screenY
	 * @return
	 */
	public float unProjectLinearY(float screenY)
	{
		return (((screenY
				- getClippingPixelHeight()
				- ((!Lttl.game.inEditor() || isEditorCamera() || Lttl.editor
						.getSettings().editorViewRatio == 0) ? 0 : Lttl.editor
						.getCamera().getViewportPixelHeightStatic()) - (this
				.getViewportPixelHeightStatic() / 2)) / this
				.getViewportPixelHeightStatic())
				* this.getViewportHeightStatic() * -1)
				+ position.y;
	}

	/**
	 * Converts screen position to world in game units. Mostly used with Lttl.input.getX() or getY()
	 * 
	 * @param screenX
	 * @param screenY
	 * @param result
	 *            provide a vector2 container to retrieve the result in
	 * @return
	 */
	public Vector2 unProjectPoint(float screenX, float screenY, Vector2 result)
	{
		// quick return if no rotation or zoom changes
		if (rotation == 0 && zoom == 1) { return result.set(
				unProjectLinearX(screenX), unProjectLinearY(screenY)); }

		// othwerise calculate rotation and zoom with matrices
		tmp.idt();
		if (rotation != 0)
		{
			tmp.rotate(rotation);
		}

		// get the game units without the camera position and then apply a scale
		tmp.translate((unProjectLinearX(screenX) - position.x) / zoom,
				(unProjectLinearY(screenY) - position.y) / zoom);

		// add camera game position offset
		tmp.getTranslation(result);
		result.add(position.x, position.y);

		// othwerise calculate rotation and zoom with matrices
		return result;
	}

	/**
	 * Returns the position in the camera based on the percentage (across the camera), includes rotation and zoom
	 * 
	 * @param percentageX
	 *            0 is left, 1 is right and takes into consideration clipping amount
	 * @param percentageY
	 *            0 is bottom, 1 is top and takes into consideration clipping amount
	 * @return
	 */
	public Vector2 unProjectPercentage(float percentageX, float percentageY,
			Vector2 result)
	{
		result.x = LttlMath.Lerp(getLeft(), getRight(), percentageX);
		result.y = LttlMath.Lerp(getBottom(), getTop(), percentageY);

		if (rotation != 0)
		{
			LttlMath.RotateAroundPoint(result, position, rotation, result);
		}

		return result;
	}

	/**
	 * Returns the ratio of game units per pixel X (assumes zoom is 1)
	 * 
	 * @return
	 */
	public float getUnitsPerPixelX()
	{
		return getViewportWidthStatic() / getViewportPixelWidthStatic();
	}

	/**
	 * Returns the ratio of game units per pixel X including zoom
	 * 
	 * @return
	 */
	public float getUnitsPerPixelXZoomed()
	{
		return getUnitsPerPixelX() / zoom;
	}

	/**
	 * Returns the ratio of game units per pixel y (assumes zoom is 1)
	 * 
	 * @return
	 */
	public float getUnitsPerPixelY()
	{
		return getViewportHeightStatic() / getViewportPixelHeightStatic();
	}

	/**
	 * Returns the ratio of game units per pixel y including zoom
	 * 
	 * @return
	 */
	public float getUnitsPerPixelYZoomed()
	{
		return getUnitsPerPixelY() / zoom;
	}

	/**
	 * Moves and zooms camera to look at given transform adjusting zoom to make sure to include mesh and center
	 * position.
	 * 
	 * @param transform
	 * @param ease
	 * @param duration
	 *            if 0, no tween will be created
	 * @return the tween TimeLine created
	 */
	public Timeline lookAt(LttlTransform transform, EaseType ease,
			float duration)
	{
		ArrayList<LttlTransform> list = new ArrayList<LttlTransform>();
		list.add(transform);
		return lookAt(list, ease, duration);
	}

	/**
	 * Moves and zooms camera to look at all given transforms adjusting zoom to make sure to include all meshes and
	 * center positions.
	 * 
	 * @param pointsShared
	 * @param ease
	 * @param duration
	 *            if 0, no tween will be created
	 * @return the tween TimeLine created
	 */
	public Timeline lookAt(ArrayList<LttlTransform> transforms, EaseType ease,
			float duration)
	{
		if (transforms.size() == 0) return null;

		FloatArray points = new FloatArray(transforms.size() * 6);

		// add each transform's mesh bounding rect (including position) points
		for (LttlTransform lt : transforms)
		{
			lt.updateWorldValuesTree();
			points.addAll(lt.getSelectionBoundingRectPointsTree(true));
		}
		return lookAt(points.toArray(), ease, duration, .4f);
	}

	/**
	 * Moves and zooms camera to look at all given transforms adjusting zoom to make sure to include all meshes and
	 * center positions.
	 * 
	 * @param points
	 * @param ease
	 * @param duration
	 *            if 0, no tween will be created
	 * @param zoomBufferPercentage
	 *            how much of extra zoom will there be
	 * @return the tween TimeLine created
	 */
	public Timeline lookAt(float[] points, EaseType ease, float duration,
			float zoomBufferPercentage)
	{
		return lookAtInternal(points, ease, duration, zoomBufferPercentage,
				zoom);
	}

	Timeline lookAtInternal(float[] points, EaseType ease, float duration,
			float zoomBufferPercentage, float startZoom)
	{
		Vector2 center = new Vector2();
		float calcZoom = zoom;

		// if only one point
		if (points.length == 2)
		{
			// define center as itself
			center.set(points[0], points[1]);
			// define new zoom the previous zoom, this is only used with GUI's Find Transform to make it return to it's
			// original zoom if it is just going to a point
			calcZoom = startZoom;
		}
		if (points.length >= 4)
		{
			// must have at least two points
			// calculate bounding rect that includes all points
			Rectangle groupBoundingRect = LttlMath.GetAABB(points, null);

			// only calculate zoom if there is some difference in the points (not all same point)
			if (groupBoundingRect.width != 0 || groupBoundingRect.height != 0)
			{
				// calculate zoom
				if (this.rotation != 0)
				{
					// rotate bounding group rect to counter camera rotation and then calculate zoom as if there is no
					// rotation
					LttlMath.GetAABB(LttlMath.TransformRectangle(
							groupBoundingRect, this.rotation, 1, 1, null),
							groupBoundingRect);
				}
				groupBoundingRect.getCenter(center);
				float xFactor = groupBoundingRect.width
						/ getOrthoCamera().viewportWidth;
				float yFactor = groupBoundingRect.height
						/ getOrthoCamera().viewportHeight;
				// get reciprocal because zoom number is backwards
				calcZoom = 1 / LttlMath.max(xFactor, yFactor);
				calcZoom -= calcZoom * zoomBufferPercentage;
			}
			else
			{
				groupBoundingRect.getCenter(center);
				calcZoom = startZoom;
			}
		}

		// set action
		if (duration <= 0)
		{
			// instant
			this.zoom = calcZoom;
			this.position.set(center);
			return null;
		}
		else
		{
			// Tween
			return tweenParallel()
					.push(tweenPosTo(center, duration).setEase(ease))
					.push(tweenZoomTo(calcZoom, duration).setEase(ease))
					.start();
		}
	}

	/**
	 * Moves camera to transform position (updates transform's world positions).
	 * 
	 * @param transform
	 * @param ease
	 * @param duration
	 *            if 0, no tween will be created
	 * @return the tween created
	 */
	public Tween goTo(LttlTransform transform, EaseType ease, float duration)
	{
		return tweenPosTo(transform.getWorldPosition(true), duration).setEase(
				ease).start();

	}

	/**
	 * Moves camera to center of transform positions (updates their world positions).
	 * 
	 * @param transforms
	 * @param ease
	 * @param duration
	 *            if 0, no tween will be created
	 * @return the tween created
	 */
	public Tween goTo(ArrayList<LttlTransform> transforms, EaseType ease,
			float duration)
	{
		if (transforms.size() == 0) return null;

		Vector2 center = new Vector2();

		// calculate center
		if (transforms.size() == 1)
		{
			// single transform position
			center.set(transforms.get(0).getWorldPosition(true));
		}
		else
		{
			// atleast two transforms
			LttlObjectHelper.GetCenterOfTransforms(transforms, center);
		}

		// set action
		if (duration <= 0)
		{
			// instant
			this.position.set(center);
			return null;
		}
		else
		{
			// Tween
			return tweenPosTo(center, duration).setEase(ease).start();
		}
	}

	// *********************//
	// ******* TWEENS ******//
	// *********************//

	/**
	 * Tweens position of camera to target.
	 * 
	 * @param targetPos
	 * @param duration
	 * @return
	 */
	public Tween tweenPosTo(Vector2 targetPos, float duration)
	{
		return tweenPosTo(targetPos.x, targetPos.y, duration);
	}

	/**
	 * Tweens position of camera to target.
	 * 
	 * @param targetX
	 * @param targetY
	 * @param duration
	 * @return
	 */
	public Tween tweenPosTo(float targetX, float targetY, float duration)
	{
		return Lttl.tween.tweenVector2To(null, position, targetX, targetY,
				duration);
	}

	/**
	 * Creates a parallel timeline with two tweens (one for each position property). You can use Timeline.getChildren()
	 * to modify them further, or write it yourself.
	 * 
	 * @param targetX
	 * @param easeX
	 * @param targetY
	 * @param easeY
	 * @param duration
	 * @return
	 */
	public Timeline tweenPosArcTo(float targetX, EaseType easeX, float targetY,
			EaseType easeY, float duration)
	{
		return tweenParallel().push(
				Lttl.tween.tweenVector2PropTo(null, this.position, 0, targetX,
						duration)).push(
				Lttl.tween.tweenVector2PropTo(null, this.position, 1, targetY,
						duration));
	}

	/**
	 * Tweens rotation of camera to target.
	 * 
	 * @param targetRotation
	 * @param duration
	 * @return
	 */
	public Tween tweenRotTo(float targetRotation, float duration)
	{
		final LttlCamera thisThis = this;
		return Tween.to(null, new TweenGetterSetter()
		{
			@Override
			public void set(float[] values)
			{
				thisThis.rotation = values[0];
			}

			@Override
			public float[] get()
			{
				return new float[]
				{ thisThis.rotation };
			}
		}, duration).target(targetRotation);
	}

	/**
	 * Tweens zoom of camera to target.
	 * 
	 * @param targetZoom
	 * @param duration
	 * @return
	 */
	public Tween tweenZoomTo(float targetZoom, float duration)
	{
		final LttlCamera thisThis = this;
		return Tween.to(null, new TweenGetterSetter()
		{
			@Override
			public void set(float[] values)
			{
				thisThis.zoom = values[0];
			}

			@Override
			public float[] get()
			{
				return new float[]
				{ thisThis.zoom };
			}
		}, duration).target(targetZoom);
	}

	/**
	 * A Timeline can be used to create complex animations made of sequences and parallel sets of Tweens. Use push() to
	 * add twe
	 * <p/>
	 * The following example will create an animation sequence composed of 5 parts:
	 * <p/>
	 * 1. First, opacity and scale are set to 0 (with Tween.set() calls).<br/>
	 * 2. Then, opacity and scale are animated in parallel.<br/>
	 * 3. Then, the animation is paused for 1s.<br/>
	 * 4. Then, position is animated to x=100.<br/>
	 * 5. Then, rotation is animated to 360°.
	 * <p/>
	 * This animation will be repeated 5 times, with a 500ms delay between each iteration: <br/>
	 * <br/>
	 * 
	 * <pre>
	 * {@code
	 * Timeline.createSequence()
	 *     .push(Tween.set(myObject, OPACITY).target(0))
	 *     .push(Tween.set(myObject, SCALE).target(0, 0))
	 *     .beginParallel()
	 *          .push(Tween.to(myObject, OPACITY, 0.5f).target(1).ease(Quad.INOUT))
	 *          .push(Tween.to(myObject, SCALE, 0.5f).target(1, 1).ease(Quad.INOUT))
	 *     .end()
	 *     .pushPause(1.0f)
	 *     .push(Tween.to(myObject, POSITION_X, 0.5f).target(100).ease(Quad.INOUT))
	 *     .push(Tween.to(myObject, ROTATION, 0.5f).target(360).ease(Quad.INOUT))
	 *     .repeat(5, 0.5f)
	 *     .start();
	 * }
	 * </pre>
	 */
	public Timeline tweenSequence()
	{
		return Lttl.tween.createSequence(null);
	}

	/**
	 * Creates a new timeline with a 'parallel' behavior. Its children will be triggered all at once. Use "push" to add
	 * Tween objects.
	 */
	public Timeline tweenParallel()
	{
		return Lttl.tween.createParallel(null);
	}

	/**
	 * Kill all tweens with this camera as the target.
	 */
	public void tweenKillAll()
	{
		Lttl.tween.getManager().killTarget(this);
	}

	public boolean isPlayCamera()
	{
		return getClass() == LttlCamera.class;
	}

	boolean isEditorCamera()
	{
		return getClass() == LttlEditorCamera.class;
	}

	/**
	 * returns the top unrotated game position (includes zoom, excludes clipping area)
	 */
	public float getTop()
	{
		return Lttl.game.getCamera().position.y
				+ Lttl.game.getCamera().getViewportHeight() / 2;
	}

	/**
	 * returns the bottom unrotated game position (includes zoom, excludes clipping area)
	 */
	public float getBottom()
	{
		return Lttl.game.getCamera().position.y
				- Lttl.game.getCamera().getViewportHeight() / 2;
	}

	/**
	 * returns the left unrotated game position (includes zoom, excludes clipping area)
	 */
	public float getLeft()
	{
		return Lttl.game.getCamera().position.x
				- Lttl.game.getCamera().getViewportWidth() / 2;
	}

	/**
	 * returns the right unrotated game position (includes zoom, excludes clipping area)
	 */
	public float getRight()
	{
		return Lttl.game.getCamera().position.x
				+ Lttl.game.getCamera().getViewportWidth() / 2;
	}

	/**
	 * Returns the transform saved state for this camera.
	 * 
	 * @return
	 */
	public LttlCameraTransformState getTransformState()
	{
		return LttlCameraTransformState.getTransformState(this);
	}

	/**
	 * Sets the GL Viewport.
	 */
	protected void setViewport()
	{
		if (!Lttl.game.inEditor())
		{
			Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(),
					Gdx.graphics.getHeight());
		}
		else
		{
			if (isEditorCamera())
			{
				Gdx.gl.glViewport(0, LttlMath.round(Lttl.game.getCamera()
						.getRawPixelHeight()), LttlMath.round(Lttl.editor
						.getCamera().getRawPixelWidth()), LttlMath
						.round(Lttl.editor.getCamera().getRawPixelHeight()));
			}
			else
			{
				Gdx.gl.glViewport(0, 0, LttlMath.round(Lttl.game.getCamera()
						.getRawPixelWidth()), LttlMath.round(Lttl.game
						.getCamera().getRawPixelHeight()));
			}
		}
	}

	public float getAspectRatio()
	{
		return useableAspectRatio;
	}

	/**
	 * Returns if the rectangle is overlapping the camera's viewport, if camera is rotated, this will be checking the
	 * {@link #getViewportPolygon(boolean)} polygon, which may be a more expensive operation, instead could use
	 * {@link #overlapAABB(Rectangle)}.
	 */
	public boolean overlap(Rectangle rect)
	{
		if (rotation != 0)
		{
			// update polygon and bounding rect if necessary
			return LttlGeometry.OverlapsConvex(viewportPolygon, rect, null);
		}
		else
		{
			// if no rotation, just use the transformed rectangle
			return getViewportAABB().overlaps(rect);
		}
	}

	/**
	 * Returns if the rectangle is overlapping the camera's viewport. If camera is rotated, then it will be using the
	 * camera's {@link #getViewportRotatedAABB()}.
	 * 
	 * @param rect
	 * @return
	 */
	public boolean overlapAABB(Rectangle rect)
	{
		if (rotation != 0)
		{
			// update polygon and bounding rect if necessary
			return getViewportRotatedAABB().overlaps(rect);
		}
		else
		{
			// if no rotation, just use the transformed rectangle
			return getViewportAABB().overlaps(rect);
		}
	}

	public boolean overlap(Circle circle)
	{
		return Intersector.overlaps(circle, getViewportRotatedAABB());
	}
}
