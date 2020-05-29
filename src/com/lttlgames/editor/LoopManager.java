package com.lttlgames.editor;

import java.util.ArrayList;
import java.util.Iterator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;
import com.lttlgames.graphics.LttlBlendMode;
import com.lttlgames.graphics.LttlMesh;
import com.lttlgames.graphics.LttlShader;
import com.lttlgames.graphics.LttlShaders;
import com.lttlgames.helpers.LttlMath;
import com.lttlgames.helpers.LttlProfiler;

public class LoopManager
{
	private LttlProcessing processing;
	private Color tmpColor = new Color();

	LoopManager()
	{
		// initial stuff
		Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
		Gdx.gl.glDepthMask(false);
		// Gdx.gl.glEnable(GL20.GL_TEXTURE_2D);
		Gdx.gl.glEnable(GL20.GL_BLEND);
	}

	/**
	 * iterated through when drawing all transforms (z order)
	 */
	ArrayList<LttlTransform> transformsOrdered = new ArrayList<LttlTransform>();

	/**
	 * The components to be destroyed this frame.
	 */
	ArrayList<LttlComponent> compDestroyList = new ArrayList<LttlComponent>();
	/**
	 * Stores the components that are suppose to be hard destroyed (remove all references in scenes), that way they can
	 * be delete all the same time instead of running it for each component
	 */
	ArrayList<LttlComponent> compHardDestroyList = new ArrayList<LttlComponent>();

	/**
	 * The currently binded texture;
	 */
	private Texture currentTexture0 = null;
	/**
	 * The currently binded texture1;
	 */
	private Texture currentTexture1 = null;

	boolean stepOneFrame = false;
	boolean isEditorPaused = false;

	private int lastSceneIdRefreshed = -1;
	private float sceneRefreshElapsed = 0;
	private final float sceneResourceCheckInterval = 2f;
	int resizeCount = 0;

	// Rendering stuff
	private Mesh renderMesh;
	private int batchNum;
	private int batchRenderCount;
	private ShaderProgram currentShader;
	private LttlBlendMode currentBlendMode;
	private boolean shaderHasCameraMatrix = false;
	private LttlRenderer lastRenderer;
	private LttlMesh batchMesh;
	private int totalFrameRenderCount = 0;
	private int frameRenderPeak = 0;
	private RenderView renderView = RenderView.Play;

	enum RenderView
	{
		Play, Editor
	}

	void createMesh()
	{
		int size = Lttl.game.getSettings().maxTriangleBatch;
		// if (size > 10920) { throw new IllegalArgumentException(
		// "Can't have more than 10920 triangles per batch: " + size); }

		if (renderMesh != null)
		{
			renderMesh.dispose();
		}
		renderMesh = new Mesh(false, size, size * 3, LttlMesh.VERTEX_ATTRIBUTES);

		if (batchMesh == null)
		{
			batchMesh = new LttlMesh(size / 3);
		}
		else
		{
			batchMesh.clear();
			batchMesh.ensureCapacity(size / 3);
		}
	}

	/**
	 * Runs everytime before rendering the game, clearing the game with background color.
	 */
	private void clearBuffer()
	{
		Color bgColor = Lttl.game.getSettings().backgroundColor;
		Gdx.gl.glClearColor(bgColor.r, bgColor.g, bgColor.b, bgColor.a);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
	}

	/**
	 * Executes the components' early update, update, late update, and finally renders scenes.
	 */
	protected final void loop()
	{
		try
		{
			// always process engine input before any updates
			// needs to be done even if paused because this also processes editor input
			Lttl.input.update();

			// update editor
			if (Lttl.game.inEditor() && Lttl.editor.getGui().isInitialized())
			{
				Lttl.editor.update();
			}

			// EDITOR PAUSED
			// don't update anything, unless stepOneFrame is true, then pretend not paused
			if (Lttl.game.inEditor() && Lttl.editor.isPaused() && !stepOneFrame)
			{
				stage();
				// still want to debug draw when paused
				functionsGroupDebugDraw();
				// render (didn't update anything though)
				renderLoop();
				printProfileData();
				return;
			}

			// update game time is not paused and update frame count
			Lttl.game.rawframeCount++;
			// update raw game time
			Lttl.game.rawGameTime += Gdx.graphics.getRawDeltaTime();

			if (!Lttl.game.isPaused())
			{
				Lttl.game.gameTime += Lttl.game.getRawDeltaTime();
				Lttl.game.frameCount++;
			}

			// clear all components' has ran bits
			clearHasRanBits();

			/* UPDATES */
			updates();

			/* STAGE */
			stage();

			/* RENDER */
			renderLoop();

			// REPAUSES IF ONLY PLAYING ONE FRAME
			if (Lttl.game.inEditor() && Lttl.editor.isPaused() && stepOneFrame)
			{
				stepOneFrame = false;
			}
			printProfileData();
		}
		catch (KillLoopException k)
		{
		}
	}

	private void updates()
	{
		// set game state (if first update, then it is setting state from SettingUp)
		Lttl.game.state = GameState.UPDATING;

		// The order of update calls is down the transform hiearchy and on each transform's components (based on their
		// order, which can be modified in editor)

		/* Functions Group: Input */
		functionsGroupInput();

		/* Functions Group: Animation */
		functionsGroupAnimation();

		/* Functions Group: Physics/Fixed */
		functionsGroupPhysicsFixed();

		/* Functions Group: Core Logic */
		functionsGroupCoreLogic();

		/* Functions Group: Dependent Logic */
		functionsGroupDependentLogic();

		/* Functions Group: Debug Draw */
		functionsGroupDebugDraw();
	}

	private void functionsGroupInput()
	{
		// process mouse callbacks before early update
		if (Lttl.game.isPlaying())
		{
			Lttl.game.getPhysics().processMouseContact();
		}
		Lttl.scenes.callBackScenes(ComponentCallBackType.OnEarlyUpdate);
	}

	private void functionsGroupAnimation()
	{
		Lttl.tween
				.getManager()
				.update(Lttl.game.getDeltaTime(Lttl.game.getSettings().animationDeltaTimeType));
	}

	private void functionsGroupPhysicsFixed()
	{
		// add the delta time to the fixed left over
		// limit the deltaTime, so if it spikes on really slow devices or on load, prevents a runaway train effect
		Lttl.game.fixedAccumulator += LttlMath.min(Lttl.game.getDeltaTime(),
				Lttl.game.getSettings().fixedDeltaTimeMax);

		if (Lttl.game.fixedAccumulator >= Lttl.game.getSettings().fixedDeltaTime)
		{
			// guaranteed at least one step
			while (Lttl.game.fixedAccumulator >= Lttl.game.getSettings().fixedDeltaTime)
			{
				Lttl.game.fixedFrameCount++;
				fixedAccumulatorAdjustment(Lttl.game.getSettings().fixedDeltaTime);

				Lttl.scenes.callBackScenes(ComponentCallBackType.onFixedUpdate);

				// step physics after running this iteration's onFixedUpdates if physics is enabled and isPlaying
				if (Lttl.game.isPlaying() && Lttl.game.getPhysics().enabled)
				{
					Lttl.game.getPhysics().step(false);

					// update the LttlTransforms with the physics body's transform since this is the last step. The
					// bodyToTransform update needs to be done in onLateFixedUpdate because the bodies need to update in
					// hiearchial order, since a body could have a child body inside of it, and the parent body needs to
					// be updated first this variable tells the bodies to update
					if (Lttl.game.fixedAccumulator < Lttl.game.getSettings().fixedDeltaTime)
					{
						Lttl.game.getPhysics().shouldUpdateBodyToTransform = true;
					}
				}

				Lttl.scenes
						.callBackScenes(ComponentCallBackType.onLateFixedUpdate);

				// always set to false afterward
				Lttl.game.getPhysics().shouldUpdateBodyToTransform = false;
			}

			// clearForces() is automatically done after each step(), if necessary, could disable autoClearForces and do
			// it here after all steps have ran
			// Lttl.game.getPhysics().getWorld().clearForces();
		}
		else
		{
			// if no step, then try doing a zero step if there are any forced physics body transform changes
			if (Lttl.game.getPhysics().enableZeroStep && Lttl.game.isPlaying()
					&& Lttl.game.getPhysics().enabled)
			{
				Lttl.game.getPhysics().stepZero();
			}
		}

		Lttl.game.getPhysics().update();
	}

	void fixedAccumulatorAdjustment(float adjustment)
	{
		Lttl.game.fixedAccumulator -= adjustment;
		Lttl.game.gameTimeFixed += adjustment;
	}

	private void functionsGroupCoreLogic()
	{
		Lttl.scenes.callBackScenes(ComponentCallBackType.onUpdate);
	}

	private void functionsGroupDependentLogic()
	{
		Lttl.scenes.callBackScenes(ComponentCallBackType.onLateUpdate);
	}

	private void functionsGroupDebugDraw()
	{
		if (Lttl.game.inEditor())
		{
			Lttl.scenes.callBackScenes(ComponentCallBackType.DebugDraw);
		}

		// draw camera guides to debug
		Lttl.game.getCamera().debugDraw();
	}

	/**
	 * Prepares the transform for rendering by computing their world matrix and updating the world Z position and
	 * checking if it changed and then updates z ordering list if necessary. This works in a hiearchical manner, meaning
	 * every child's parents are updated with the current world values before their values are computred from their
	 * parents.
	 */
	private void stage()
	{
		// set game state
		Lttl.game.state = GameState.STAGING;

		// check for modified resources (just textures)
		processSceneResourceRefreshes();

		// it is crucial that these 3 functions run in this order because processDestroyed() removes them from the scene
		// but needs the scene to still be loaded into game, then processUnloaded() removes scene from the game, and
		// finally processHardDestroyed() iterates through all the loaded scenes, which now does not include the
		// unloaded scenes and destroyed components
		processDestroyed();
		processUnloaded();
		processHardDestroyed();

		// stage all the transforms and play camera
		// it is crucial that the camera gets staged before the transforms, since they need the camera worldMatrix
		Lttl.game.getCamera().update();

		for (LttlSceneCore ls : Lttl.scenes.getScenesAndWorld())
		{
			for (LttlTransform lt : ls.transformHiearchy)
			{
				stageTransform(lt);
			}
		}
	}

	private void stageTransform(LttlTransform lt)
	{
		lt.enabledThisFrame = true; // all transforms start as enabled, then determine if not enabled this frame
		if (!lt.isEnabledSelf())
		{
			disableEntireHiearachy(lt);
			return; // don't compute any of the staging stuff for this object and all its children
		}

		if (lt.r() != null)
		{
			// reset post processing stuff
			lt.r().isRenderConsumed = false;
			lt.r().hasRendered = false;
			lt.r().hasUpdatedCanRender = false;

			if (lt.r().isEnabled())
			{
				// update Alpha
				lt.r().updateAlpha(false);

				// check if should force a mesh update with MeshGenerator
				if (lt.r().generator() != null
						&& lt.r().generator().isEnabled()
						&& lt.r().generator().debugUpdateMeshEveryFrame)
				{
					Lttl.logNote("Forcing Mesh Update: on " + lt.getName());
					lt.r().generator().updateMesh();
				}

				// update textures if they are null but have a regionName
				if (lt.r().getTex0().getTex() == null)
				{
					lt.r().getTex0().refresh();
				}
				if (lt.r().getTex1().getTex() == null)
				{
					lt.r().getTex1().refresh();
				}
			}
		}

		// update (local and world) transform and world values
		lt.updateTransforms(false);

		// check if world z position changed, should be up to date since the updateTransforms() ran above
		checkTransformZindex(lt);

		// stage any children too
		if (lt.children != null && lt.children.size() > 0)
		{
			for (int i = 0; i < lt.children.size(); i++)
			{
				stageTransform(lt.children.get(i));
			}
		}
	}

	/**
	 * Makes this object and all it's children disabled for this frame.<br>
	 * Sets enabledThisFrame to false.
	 * 
	 * @param o
	 */
	private void disableEntireHiearachy(LttlTransform o)
	{
		o.enabledThisFrame = false;

		// stage any children too
		if (o.children != null && o.children.size() > 0)
		{
			for (int i = 0; i < o.children.size(); i++)
			{
				disableEntireHiearachy(o.children.get(i));
			}
		}
	}

	private int max;

	private void renderLoop()
	{
		// set game state
		Lttl.game.state = GameState.RENDERING;

		clearBuffer();

		// reset render stats
		batchNum = 0;
		batchRenderCount = 0;
		totalFrameRenderCount = 0;

		max = Lttl.game.getSettings().maxTriangleBatch = LttlMath.max(
				Lttl.game.getSettings().maxTriangleBatch, 1);

		// create renderMesh and batchMesh
		if (renderMesh == null)
		{
			createMesh();
		}

		// should already be clear
		batchMesh.clear();

		// force texture rebind, prevents errors when making textures and stuff, like LttlFontRenderer, and when
		// importing texture atlas
		clearBindedTextures();

		// draw editor debug stuff and add handles, this way it renders like any other debug, but it is on top
		if (Lttl.game.inEditor())
		{
			Lttl.debug.setZIndex(Integer.MIN_VALUE);
			Lttl.editor.debugDraw();
			Lttl.editor.getHandleController().drawHandles();
			Lttl.debug.setZIndex(0);
		}

		// render play view
		if (!Lttl.game.inEditor()
				|| (Lttl.game.inEditor() && Lttl.editor.getSettings().editorViewRatio != 1))
		{
			// set view
			renderView = RenderView.Play;

			// render
			setRenderViewport();
			renderView();

			// now that play view is finished rendering, callback post
			if (processing != null)
			{
				processing.onComplete();
			}

			renderDebug();

			// add clips for non aspect ratio screens (after debugDraw to crop out any debug draw)
			Lttl.game.getCamera().drawNonAspectRatioBorders();

			// flush and clean
			cleanUpRenderingView("Finished");
		}

		if (Lttl.game.inEditor()
				&& Lttl.editor.getSettings().editorViewRatio != 0)
		{
			// set view
			renderView = RenderView.Editor;

			// stage editor camera (play already staged in stage method)
			Lttl.editor.getCamera().update();

			// set render viewport
			setRenderViewport();

			// draw editor background color, instant so it's not considered normal debug
			Lttl.debug.setInstantDraw(true);
			Lttl.debug.drawRect(Lttl.editor.getCamera().getViewportAABB(), 0,
					Lttl.editor.getSettings().backgroundColor);
			Lttl.debug.setInstantDraw(false);

			// render
			renderView();

			// draw mouse position
			if (Lttl.editor.getSettings().drawMousePosition)
			{
				// draw unclipped mouse position
				Lttl.debug.drawCircle(Lttl.input.getMousePos(false),
						LttlDebug.RADIUS_MEDIUM * Lttl.debug.eF(),
						Lttl.editor.getSettings().drawMousePositionColor);
				// draw clipped mouse position
				Lttl.debug.drawCircle(Lttl.input.getMousePos(true),
						LttlDebug.RADIUS_MEDIUM * Lttl.debug.eF(),
						Lttl.editor.getSettings().drawMousePositionColor);
			}

			renderDebug();

			// flush and clean
			cleanUpRenderingView("Finished");

			// reset back to play view
			renderView = RenderView.Play;
		}

		// reset debug
		Lttl.debug.reset();

		// save peak
		frameRenderPeak = LttlMath.max(frameRenderPeak, totalFrameRenderCount);
	}

	private void renderDebug()
	{
		// render debug
		Lttl.debug.debugRender();

		// render the physics debug draw
		renderPhysicDebug();
	}

	/**
	 * after main debug render, renders the actual physics debug
	 */
	private void renderPhysicDebug()
	{
		if (Lttl.game.getPhysics().debugDraw && Lttl.game.getPhysics().enabled)
		{
			if (getCurrentRenderingCamera().isPlayCamera()
					&& !Lttl.game.getPhysics().debugDrawPlay) return;

			// this draws the solid over physics
			Lttl.game.getPhysics().preDebugDraw();

			// need to close rendering view which does a flush and clears all the rendering state variable, since the
			// physics debug draw will modify it's own render state (ie. shaders, etc)
			cleanUpRenderingView("Pre Physics Debug Draw");

			// render physics debug
			Lttl.game.getPhysics().debugDraw();
		}
	}

	/**
	 * does a flush and clears/closes all the rendering state variables (ie. currentShader, blendMode, textures), so
	 * when starts rendering again it will start off with a clean slate.
	 * 
	 * @param flushReason
	 */
	private void cleanUpRenderingView(String flushReason)
	{
		// flush any left
		if (batchMesh.getVertexCount() > 0)
		{
			flush(RenderType.Other, null, flushReason);
		}

		// finished renderering all Objects, close shader if it exists
		if (currentShader != null)
		{
			currentShader.end();
			currentShader = null;
		}
		currentBlendMode = null;
		shaderHasCameraMatrix = false;
		lastRenderer = null;
		clearBindedTextures();
	}

	private void setRenderViewport()
	{
		// set the viewport if in editor
		if (Lttl.game.inEditor())
		{
			getCurrentRenderingCamera().setViewport();
		}
	}

	private void renderView()
	{
		// now that everything is ready for rendering callback post process, but only if rendering play view
		// this needs to happen after setViewport above
		if (processing != null && getRenderView() == RenderView.Play)
		{
			if (!processing.init)
			{
				processing.onInit();
				processing.init = true;
			}
			processing.onStart();
		}

		// iterate through the objects in order of their z position
		for (LttlTransform lt : transformsOrdered)
		{
			if (lt.r() == null || lt.r().doNotRender || !lt.r().canRender())
				continue;

			// no post callbacks for editor
			renderTransform(lt);
		}
	}

	/**
	 * This is a shortcut straight to render this transform that is intended to be called by post processing.<br>
	 * <b>There are no checks. Be sure you check if transform has a renderer and if it {@link LttlRenderer#canRender()}
	 * before sending it to this. </b>
	 * 
	 * @param lt
	 */
	void renderFromPostProcessing(LttlTransform lt)
	{
		LttlRenderer r = lt.r();

		if (LttlMultiRenderer.class.isAssignableFrom(r.getClass()))
		{
			((LttlMultiRenderer) r).render();
		}
		else
		{
			if (r.preMultiplyWorldMesh)
			{
				renderRenderer(lt.r(), null);
			}
			else
			{
				renderRenderer(lt.r(), lt.r().worldRenderMatrix);
			}
		}
	}

	private void renderTransform(LttlTransform lt)
	{
		// assumes renderer exists
		LttlRenderer r = lt.r();

		Rectangle rect;
		if (LttlMultiRenderer.class.isAssignableFrom(r.getClass()))
		{
			// multi renderer stuff
			LttlMultiRenderer mr = (LttlMultiRenderer) r;

			// check if in camera view and if it has a custom bounding rect defined, since it can't check with the mesh
			// bounding rect since it's not accurate for all the multi renders
			if (getRenderView() == RenderView.Play)
			{
				// reset the check if parent is not rendering at the beginning of each play view render
				mr.parentRendererNotDrawing = false;
			}
			if (r.checkInCameraView
					&& r.customBoundingRect != null
					&& !getCurrentRenderingCamera().getViewportRotatedAABB()
							.overlaps(rect = r.getRenderCheckRect()))
			{
				if (getRenderView() == RenderView.Play)
				{
					// if in play view mark that the parent is not drawing and draw the mark
					mr.parentRendererNotDrawing = true;
					markNonRenders(rect);
				}
				return;
			}

			// updates the world matrix that will be used in the shader (relative to camera transform)
			if (!lt.r().preMultiplyWorldMesh)
			{
				lt.updateWorldRenderMatrix();
			}

			if (checkPostProcessing(lt))
			{
				((LttlMultiRenderer) r).render();
			}
			else return;
		}
		else
		{
			// normal renderer stuff

			// check if in camera view and if it is not in view return
			if (r.checkInCameraView
					&& !getCurrentRenderingCamera().getViewportRotatedAABB()
							.overlaps(rect = r.getRenderCheckRect()))
			{
				markNonRenders(rect);
				return;
			}

			// update the local mesh's color and alpha vertex attributes (if changed)
			boolean colorAlphaChanged = false;
			if (r.autoUpdateMeshColorAlpha)
			{
				Color c = lt.r().premultiplyColor ? tmpColor.set(lt.r().color)
						.premultiplyAlpha() : lt.r().color;
				colorAlphaChanged = r.getMesh().updateColorAlpha(c,
						r.getWorldAlpha(false));
			}

			// update the world vertices array if it's empty, this is probably because the world transform changed or it
			// has never updated
			if (lt.r().preMultiplyWorldMesh)
			{
				if (lt.r().getMesh().getWorldVerticesArray().size == 0)
				{
					lt.r().getMesh()
							.updateWorldVertices(lt.worldRenderTransform);
				}
				else
				{
					// if didn't update worldVerticesArray, then also update it's color and alpha values, because it
					// didn't get the new ones updated above
					if (colorAlphaChanged)
					{
						Color c = lt.r().premultiplyColor ? tmpColor.set(
								lt.r().color).premultiplyAlpha() : lt.r().color;
						lt.r()
								.getMesh()
								.updateColorAlphaWorld(c,
										lt.r().getWorldAlpha(false));
					}
				}

				if (checkPostProcessing(lt))
				{
					// draw with just mesh since it has world values
					renderRenderer(lt.r(), null);
				}
				else return;
			}
			else
			{
				// updates the world matrix that will be used in the shader (includes camera transform)
				lt.updateWorldRenderMatrix();

				if (checkPostProcessing(lt))
				{
					// draw with worldRenderMatrix, mesh should be local values
					renderRenderer(lt.r(), lt.r().worldRenderMatrix);
				}
				else return;
			}
		}

		// successful render, post processs callback
		if (processing != null && renderView == RenderView.Play)
		{
			// callback post
			processing.afterTransform(lt);
		}
	}

	/**
	 * meant to be called right before {@link #renderDraw()}
	 * 
	 * @param lt
	 * @return
	 */
	private boolean checkPostProcessing(LttlTransform lt)
	{
		// post processing callback
		if (renderView == RenderView.Editor || processing == null
				|| (!lt.r().isRenderConsumed && processing.beforeTransform(lt)))
		{
			// mark as rendered
			lt.r().hasRendered = true;
			return true;
		}
		return false;
	}

	void markNonRenders(Rectangle rect)
	{
		if (Lttl.game.inEditor() && Lttl.editor.getSettings().markNonRenders)
		{
			// only draw mark non renders if this is in play view
			Lttl.debug.tmpColor.set(Lttl.editor.getSettings().colorRenderCheck).a *= .3f;
			Lttl.debug.drawRectFilledOutline(rect, 0, Lttl.debug.tmpColor, 0,
					Lttl.editor.getSettings().colorRenderCheck);
		}
	}

	void flush(RenderType type, LttlRenderer renderer, String reason)
	{
		// check if an empty batch
		if (batchMesh.getVertexCount() == 0) return;

		// check if flush is too big
		if (batchMesh.getIndiceCount() > max * 3
				|| batchMesh.getVerticesArray().size > max
						* LttlMesh.VERTICE_ATTRIBUTE_COUNT)
		{
			Lttl.logError("Rendering: A mesh was added to batch that was itself greater than the maxes from "
					+ (renderer == null ? "unknown" : renderer.t().getName()
							+ ".  Removing mesh."));
			batchMesh.clear();
			return;
		}

		batchNum++;

		printBatchData(type, renderer, reason);

		totalFrameRenderCount += batchRenderCount;
		batchRenderCount = 0;

		/* RENDER */
		// set vertices and indices
		renderMesh.setVertices(batchMesh.getVerticesArray().items, 0,
				batchMesh.getVerticesArray().size);
		renderMesh.setIndices(batchMesh.getIndicesArray().items, 0,
				batchMesh.getIndicesArray().size);
		// actually render (epicness!)
		renderMesh.render(currentShader, GL20.GL_TRIANGLES, 0,
				batchMesh.getIndicesArray().size);

		// log batch and mesh
		if (getRenderView() == RenderView.Play)
		{
			LttlMesh.profileMesh(renderMesh);
			LttlProfiler.batchCount.add();
		}

		batchMesh.clear();
	}

	private void printBatchData(RenderType type, LttlRenderer renderer,
			String reason)
	{
		if (Lttl.game.getSettings().showBatchData
				&& getRenderView() == RenderView.Play && reason != null)
		{
			Lttl.logNote(batchNum
					+ ": ("
					+ type.name()
					+ ") "
					+ reason
					+ " ["
					+ (lastRenderer != null ? lastRenderer.t().getName()
							: "none")
					+ " => "
					+ (renderer != null ? renderer.t().getName()
							: "unspecified") + "]" + " RenderCount: "
					+ batchRenderCount + " Triangles: "
					+ batchMesh.getTriangleCount());
		}
	}

	private void addToRenderBatch(FloatArray vertices, ShortArray indices)
	{
		int indexOffset = LttlMesh.getVertexCount(batchMesh.getVerticesArray());
		batchMesh.getVerticesArray().addAll(vertices);
		ShortArray batchIndicesArray = batchMesh.getIndicesArray();
		batchIndicesArray.ensureCapacity(indices.size);
		for (int i = 0; i < indices.size; i++)
		{
			batchIndicesArray.add(indexOffset + indices.get(i));
		}
	}

	public enum RenderType
	{
		Renderer, Debug, Other, Post
	}

	void renderDebug(LttlMesh mesh, LttlBlendMode blendMode, LttlShader shader,
			Texture tex0)
	{
		renderDraw(RenderType.Debug, blendMode, shader, tex0, null,
				mesh.getWorldVerticesArray(), mesh.getIndicesArray(), null,
				null);
	}

	void renderRenderer(LttlRenderer renderer, Matrix4 worldRenderMatrix)
	{
		renderDraw(RenderType.Renderer, renderer.blendMode, renderer.shader,
				renderer.getTex0().getTex(), renderer.getTex1().getTex(),
				worldRenderMatrix == null ? renderer.getMesh()
						.getWorldVerticesArray() : renderer.getMesh()
						.getVerticesArray(), renderer.getMesh()
						.getIndicesArray(), worldRenderMatrix, renderer);
	}

	/**
	 * @param renderer
	 * @param worldRenderMatrix
	 *            can be null if mesh has world values, if has worldRenderMatrix will always flush
	 */
	void renderDraw(RenderType type, LttlBlendMode blendMode,
			LttlShader shaderType, Texture tex0, Texture tex1,
			FloatArray vertices, ShortArray indices, Matrix4 worldRenderMatrix,
			LttlRenderer renderer)
	{
		batchRenderCount++;

		boolean uniqueUniforms = false;

		// manage current binded texture0
		if (tex0 != null && tex0 != currentTexture0)
		{
			// first time a texture is used, no need to flush becomes no one is using the previous texture
			if (currentTexture0 != null)
			{
				flush(type, renderer, "Texture0 Change");
			}
			currentTexture0 = tex0;
			currentTexture0.bind(0);
		}

		// manage current binded texture1
		if (tex1 != null && tex1 != currentTexture1)
		{
			if (currentTexture1 != null)
			{
				flush(type, renderer, "Texture1 Change");
			}
			currentTexture1 = tex1;
			currentTexture1.bind(1);
		}

		// *** SHADER ***//
		// check if it is using multiple textures, so can choose which shader to give it
		ShaderProgram nextShader;
		nextShader = LttlShaders.getShader(shaderType, tex0 != null
				&& tex1 != null && tex0 != tex1);

		// check if shader was found
		Lttl.Throw(nextShader);

		// manage current shader
		if (currentShader != nextShader)
		{
			// close current shader
			// currentShader will be null first time running, so don't close
			if (currentShader != null)
			{
				flush(type, renderer, "Shader Change");
				currentShader.end();
			}
			currentShader = nextShader;
			// clear the current blend mode because it's a new shader and needs to be initally set
			currentBlendMode = null;
			currentShader.begin();

			// if using the camera matrix then make sure it's set
			if (worldRenderMatrix == null)
			{
				currentShader.setUniformMatrix(
						LttlShaders.UNIFORM_WORLD_MATRIX,
						getCurrentRenderingCamera().getWorldMatrix());
				shaderHasCameraMatrix = true;
			}
			else
			{
				shaderHasCameraMatrix = false;
			}
		}

		// manage blending
		if (blendMode != currentBlendMode)
		{
			// whenever shader changes it will be null, which means it would have already flushed
			if (currentBlendMode != null)
			{
				flush(type, renderer, "Blend Mode Change");
			}
			currentBlendMode = blendMode;
			LttlShaders.blend(currentBlendMode);
		}

		// manage shader uniforms
		// can't ever compare shader uniform objects, like two renderDraws can use same shader with uniforms and maybe
		// even the same object, but the value in that object can be assumed to be the same, so always batch if have
		// unique uniforms
		if (worldRenderMatrix != null)
		{
			// it is using a unique render matrix, so flush first
			flush(type, renderer, "Uses Unique Render Matrix");
			uniqueUniforms = true;
			currentShader.setUniformMatrix(LttlShaders.UNIFORM_WORLD_MATRIX,
					worldRenderMatrix);
			// need to set this because even though using unique render matrix, it could be same shader it needs to know
			// if it needs to reset the camera matrix
			shaderHasCameraMatrix = false;
		}
		else
		{
			if (!shaderHasCameraMatrix)
			{
				// don't need to flush here because any time it would need to set camera matrix not when shader is
				// changed would be if the last render used a unique matrix, which always flushes
				currentShader.setUniformMatrix(
						LttlShaders.UNIFORM_WORLD_MATRIX,
						getCurrentRenderingCamera().getWorldMatrix());
				shaderHasCameraMatrix = true;
			}
		}

		// LttlRenderer specific
		if (renderer != null)
		{
			// currentShader is guaranteed to be the type of shaderType, since it was changed above
			if (shaderType.hasDefaultUniforms())
			{
				flush(type, renderer, "Uses Default Uniforms");
				uniqueUniforms = true;
				renderer.processDefaultUniforms(currentShader, shaderType);
			}
			if (renderer.hasCustomUniforms())
			{
				flush(type, renderer, "Uses Custom Uniforms");
				uniqueUniforms = true;
				renderer.processCustomUniforms(currentShader);
			}
		}

		// check if rendering this mesh would go beyond the max triangle batch, if so flush whats on and continue
		if (batchMesh.getIndiceCount() + indices.size > max * 3
				|| batchMesh.getVerticesArray().size + vertices.size > max
						* LttlMesh.VERTICE_ATTRIBUTE_COUNT)
		{
			flush(type, renderer, "Max Triangle Batch Reached");
		}

		/* ADD TO BATCH */
		// we have rendered anything if the shader changed, including uniforms and blend mode, or a texture
		// needed to be binded
		// now add this renderer's mesh to the batchMesh
		batchMesh.add(vertices, indices);

		// if there were any unique uniforms then flush it now, because can't batch more than one with unique uniforms
		if (uniqueUniforms)
		{
			flush(type, renderer, "Uses Unique Uniforms");
		}

		// save last rendere for data
		lastRenderer = renderer;
	}

	private void checkTransformZindex(LttlTransform lt)
	{
		// check if z position has changed since last frame
		if (lt.lastF_worldZpos != lt.worldZpos)
		{
			updateTransformZindex(lt);
		}
		lt.lastF_worldZpos = lt.worldZpos;
	}

	/**
	 * Adds/updates the transformsOrdered (by z world pos) list. Required on scene load and transform creation.
	 * 
	 * @param lt
	 */
	void updateTransformZindex(LttlTransform lt)
	{
		transformsOrdered.remove(lt);
		for (int i = 0; i < transformsOrdered.size(); i++)
		{
			// z positions have been updated already
			if (lt.worldZpos > transformsOrdered.get(i).worldZpos)
			{
				transformsOrdered.add(i, lt);
				return;
			}
		}
		transformsOrdered.add(lt); // add at end
	}

	/**
	 * Actually unloads a scene from game.
	 */
	private void processUnloaded()
	{
		for (Iterator<LttlSceneCore> it = Lttl.scenes.loadedScenes.iterator(); it
				.hasNext();)
		{
			LttlSceneCore scene = it.next();
			if (scene.isPendingUnload)
			{
				it.remove();
				Lttl.scenes.executeUnloadScene(scene.getLttlScene());
			}
		}
	}

	/**
	 * Manages destroying components and transforms. Purpose is to not mess up order of objects when iterating during
	 * updates loop.
	 */
	private void processDestroyed()
	{
		for (LttlComponent lc : compDestroyList)
		{
			lc.executeDestroy();
		}
		compDestroyList.clear();
	}

	/**
	 * Destroys all components to be hard destoyed at once.
	 */
	private void processHardDestroyed()
	{
		if (compHardDestroyList.size() <= 0) return;

		ComponentHelper.removeComponentReferencesGlobal(compHardDestroyList);
		compHardDestroyList.clear();
	}

	/**
	 * Checks if textures in folders have changed (only in editor)
	 */
	private void processSceneResourceRefreshes()
	{
		if (Lttl.game.inEditor()
				&& Lttl.editor.getSettings().autoRefreshTextureResources)
		{
			sceneRefreshElapsed += Lttl.game.getRawDeltaTime();
			if (sceneRefreshElapsed >= sceneResourceCheckInterval)
			{
				ArrayList<LttlSceneCore> allScenesAndWorld = Lttl.scenes
						.getScenesAndWorld();
				sceneRefreshElapsed = 0;
				int nextIndex = lastSceneIdRefreshed + 1;
				if (nextIndex > allScenesAndWorld.size() - 1)
				{
					nextIndex = 0;
				}
				lastSceneIdRefreshed = nextIndex;

				LttlSceneCore refreshScene = allScenesAndWorld.get(nextIndex);

				// refresh resources
				if (refreshScene.getLttlScene().getTextureManager()
						.loadAndBuildTextures(true, true))
				{
					// refresh references in all scenes
					LttlTextureManager.refreshAllReferences();
				}
			}
		}
	}

	/**
	 * Runs whenever screen resizes (including once at start)
	 */
	void onResize()
	{
		resizeCount += 1;

		if (processing != null && resizeCount > 1)
		{
			processing.onResize();
		}

		// check if this is at least the first non initial resize
		ComponentHelper.callBackAllScenes(ComponentCallBackType.onResize);

		// even though this runs at start of game, it won't do anything really first time since it only updates meshes
		// that already exist, which is none at start, meshes are created manually via scripts or in the render loop (if
		// enabled, etc)
		updateAllMeshesAA();
	}

	/**
	 * Updates all meshes that already exist and that have AA enabled
	 */
	void updateAllMeshesAA()
	{
		// iterate through all scenes transform trees
		for (LttlSceneCore ls : Lttl.scenes.getScenesAndWorld())
		{
			for (LttlTransform lt : ls.transformHiearchy)
			{
				// not sure if the transforms have been staged yet because this is usually called by onResize()
				lt.updateWorldValuesTree();
				updateMeshAATree(lt);
			}
		}
	}

	private void updateMeshAATree(LttlTransform lt)
	{
		// only update if mesh already exists, since non existing meshes will be created fresh in renderLoop
		// and update regardless if it's visible or enabled. Resize updates are rare, and if an object is disabled or
		// whatever, we should still update since if it is enabled it will have relatively accurate AA settings and mesh
		if (lt.r() != null && lt.r().getMesh() != null
				&& lt.r().generator() != null
				&& lt.r().generator().updateActualAA(false) != 0)
		{
			lt.r().generator().updateMeshAA(lt.r().generator().getActualAA());
		}

		// update any children too
		if (lt.children != null && lt.children.size() > 0)
		{
			for (int i = 0; i < lt.children.size(); i++)
			{
				updateMeshAATree(lt.children.get(i));
			}
		}
	}

	private void printProfileData()
	{
		if (Lttl.game.inEditor() && Lttl.editor.getSettings().showProfilerData)
		{
			LttlProfiler.run();
		}
	}

	public RenderView getRenderView()
	{
		return renderView;
	}

	/**
	 * Returns the current camera rendering
	 */
	public LttlCamera getCurrentRenderingCamera()
	{
		return renderView == RenderView.Editor ? Lttl.editor.getCamera()
				: Lttl.game.getCamera();
	}

	private void clearBindedTextures()
	{
		currentTexture0 = null;
		currentTexture1 = null;
	}

	Texture getCurrentTexture0()
	{
		return currentTexture0;
	}

	void setCurrentTexture0(Texture currentTexture0)
	{
		this.currentTexture0 = currentTexture0;
	}

	Texture getCurrentTexture1()
	{
		return currentTexture1;
	}

	void setCurrentTexture1(Texture currentTexture1)
	{
		this.currentTexture1 = currentTexture1;
	}

	/**
	 * Returns the highest number of renders done in a frame.
	 * 
	 * @return
	 */
	public int getRenderPeak()
	{
		return frameRenderPeak;
	}

	public void resetRenderPeak()
	{
		frameRenderPeak = 0;
	}

	private void clearHasRanBits()
	{
		for (LttlSceneCore ls : Lttl.scenes.getScenesAndWorld())
		{
			for (LttlComponent lc : ls.componentMap.values())
			{
				// note: to remove a specific bit do this hasRanBit &= ~callbackType.getValue();
				// but here we are clearing all
				lc.hasRanBit = 0;
			}
		}
	}

	/**
	 * returns processing object
	 * 
	 * @return
	 */
	public LttlProcessing getProcessing()
	{
		return processing;
	}

	/**
	 * needed to be created after world is loaded to be accurate and allow post processing to find components
	 */
	void createPostProcessingObject()
	{
		// post should always be null
		Lttl.Throw(processing != null);

		if (LttlGameStarter.get().getPostProcessingClass() != null)
		{
			processing = LttlObjectGraphCrawler.newInstance(LttlGameStarter
					.get().getPostProcessingClass());
		}
	}

	/**
	 * disposes resources, should be called whenever editor reloads
	 */
	void dispose()
	{
		if (processing != null)
		{
			processing.dispose();
		}
		if (renderMesh != null)
		{
			renderMesh.dispose();
		}
	}
}
