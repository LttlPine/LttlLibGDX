package com.lttlgames.editor;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ShortArray;
import com.lttlgames.editor.annotations.AnimateField;
import com.lttlgames.editor.annotations.ComponentLimitOne;
import com.lttlgames.editor.annotations.DoNotCopy;
import com.lttlgames.editor.annotations.DoNotExport;
import com.lttlgames.editor.annotations.GuiButton;
import com.lttlgames.editor.annotations.GuiCallbackDescendants;
import com.lttlgames.editor.annotations.GuiCanNull;
import com.lttlgames.editor.annotations.GuiGroup;
import com.lttlgames.editor.annotations.GuiHide;
import com.lttlgames.editor.annotations.GuiMax;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.IgnoreCrawl;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.graphics.LttlBlendMode;
import com.lttlgames.graphics.LttlMesh;
import com.lttlgames.graphics.LttlShader;
import com.lttlgames.graphics.LttlShaders;
import com.lttlgames.helpers.LttlMath;
import com.lttlgames.helpers.Vector2Array;
import com.lttlgames.tweenengine.Tween;
import com.lttlgames.tweenengine.TweenGetterSetter;

//26
@ComponentLimitOne
@Persist(-9015)
public class LttlRenderer extends LttlComponent implements LttlAnimated
{
	LttlMesh mesh;

	/**
	 * the shader used on this renderer
	 */
	@Persist(901501)
	public LttlShader shader = LttlShader.SimpleColorShader;

	/**
	 * The blend type this renderer will render with. [Default: ALPHA]
	 */
	@Persist(901502)
	public LttlBlendMode blendMode = LttlBlendMode.ALPHA;

	/**
	 * Stores the current LttlMeshGenerator, if one exists, only one allowed per transform. This is for quick reference,
	 * since it is called a lot in render loop
	 */
	@Persist(901503)
	@GuiHide
	@IgnoreCrawl
	@DoNotCopy
	@DoNotExport
	LttlMeshGenerator meshGenerator;

	/**
	 * This holds the value if the object is currently visible and if all it's parents are currently visible, if so,
	 * then it is true and renders.
	 */
	boolean shouldRender;

	@Persist(901504)
	@GuiMin(0)
	@GuiMax(1)
	@AnimateField(0)
	public float alpha = 1;

	/**
	 * Protected because don't want to use getWorldAlpha() in loopmanager, since it has to recursively do some stuff. In
	 * loopmanager's renderLoop() we can safely assume all world alpha are already update and accurate from stage().
	 */
	float worldAlpha;

	@Persist(901505)
	public boolean inheritAlpha = true;

	private float oldAlpha = 1;
	private boolean oldInheritAlpha = false;
	private boolean parentAlphaChanged = false;

	/**
	 * <b>No guarantee this is here<b><br>
	 * this includes camera matrix, and it's only created if renderer is not preWorldMultiplyVertices
	 */
	Matrix4 worldRenderMatrix;

	@Persist(901506)
	@GuiCallbackDescendants("onTextureChangeGui")
	protected LttlTexture texture0 = new LttlTexture();

	@Persist(901507)
	@GuiCallbackDescendants("onTextureChangeGui")
	protected LttlTexture texture1 = new LttlTexture();

	@Persist(9015026)
	@GuiGroup("Shader Uniforms")
	@AnimateField(4)
	@GuiCanNull
	public Color colorUniform;
	/**
	 * Used with specific transform UV shaders (texture repeat or scrolling), can animate too.
	 */
	@Persist(901508)
	@GuiGroup("Shader Uniforms")
	@AnimateField(1)
	@GuiCanNull
	public Vector2 uvOffsetShader;

	/**
	 * Used with specific transform UV shaders (texture scale), can animate too.
	 */
	@Persist(901509)
	@GuiGroup("Shader Uniforms")
	@AnimateField(2)
	@GuiCanNull
	public Vector2 uvScaleShader;

	/**
	 * This automatically gets set to the uniform "u_color" on the shader.
	 */
	@Persist(9015010)
	@AnimateField(3)
	public final Color color = new Color(Color.WHITE);

	/**
	 * Pre multiplies color by it's alpha value before rendering with shader.
	 */
	@Persist(9015012)
	public boolean premultiplyColor = false;

	/**
	 * auto updates mesh with color and world alpha when changed, this should be false if MultiRenderer since it'll be
	 * setting it anyway.
	 */
	@Persist(9015020)
	public boolean autoUpdateMeshColorAlpha = true;

	/**
	 * if true, when worldTransform changes, it then updates the mesh's worldVertices to reflect those changes. This way
	 * multiple meshes can be rendered with the same uniform matrix on the GPU.<br>
	 * If false, then it will update worldRenderMatrix when the worldTransform changes. Then the local vertices will be
	 * used when rendering, and a unique uniform matrix will be given to GPU, which must be drawn by itself (no
	 * batch)<br>
	 * The reason for making this false would be if you had a mesh that was moving a lot and had lots of vertices to
	 * update with world. Instead of updating the vertices each frame from the local values to the world values on the
	 * CPU, the local values would be sent to GPU with a matrix that includes the transform's worldTransform.
	 */
	@Persist(9015021)
	public boolean preMultiplyWorldMesh = true;
	/**
	 * Checks if the axis aligned transformed mesh bounding rect is in the the camera's axis aligned transformed rect
	 * before rendering. If multiRenderer, needs custom bounding rect.
	 */
	@Persist(9015022)
	@GuiGroup("Render Check")
	public boolean checkInCameraView = false;
	/**
	 * If set, this will be used instead of a mesh bounding rect to determine if should render or not, useful for things
	 * like particle emitters where you want to check the region and not each individual particle, this will act as a
	 * child of the transform.
	 */
	@Persist(9015023)
	@GuiGroup("Render Check")
	@GuiCanNull
	public Rectangle customBoundingRect;
	private Rectangle customBoundingRectTransformedAxisAligned;
	@Persist(9015024)
	@GuiGroup("Render Check")
	@GuiCanNull
	public boolean drawCustomBoundingRect = false;
	/**
	 * if true, will not render this transform or prepare it for render (aka update matrix, mesh, etc). Assumed to be
	 * used by post processing. <b>Be sure to run canRender() on the Renderer before drawing it, so it does not error
	 * out.</b>
	 */
	@Persist(9015025)
	@GuiGroup("Render Check")
	public boolean doNotRender = false;

	@Persist(9015014)
	@GuiGroup("Debug")
	public boolean drawMeshOultine = false;

	@Persist(9015015)
	@GuiGroup("Debug")
	public boolean drawMeshBoundingRectAxisAligned = false;

	@Persist(9015016)
	@GuiGroup("Debug")
	public boolean drawMeshBoundingRect = false;
	@Persist(9015019)
	@GuiGroup("Debug")
	public boolean drawMeshTriangles = false;

	private boolean noShaderWarning = false;
	private boolean noMeshWarning = false;
	private boolean generatorWarning = false;

	private Rectangle meshBoundingRectTransformedAxisAligned;
	private Vector2Array meshTransformedVertices;
	HashMap<String, Object> uniformsMap;

	/* RESET EACH FRAME - post processing stuff */
	/**
	 * this is set by post processing to prevent it from trying to render (only if it hasn't rendered yet)
	 */
	boolean isRenderConsumed = false;
	/**
	 * only set when successfully rendered in play view (naturally, not by post processing)
	 */
	boolean hasRendered = false;
	boolean hasUpdatedCanRender = false;
	private boolean canRender = false;

	@Override
	public void onStart()
	{
		refreshTextures();
	}

	@Override
	public void onEditorStart()
	{
		refreshTextures();
	}

	private void refreshTextures()
	{
		// auto refreshes the textures on start
		// this means all objects, even if not rendering on screen will get their textures
		getTex0().refresh(this);
		getTex1().refresh(this);
	}

	/**
	 * @param check
	 *            means it will check to see if there are any changes in the worldAlpha hiearchy, this only needs to
	 *            check once after a change has been made
	 * @return The current world alpha. If being inheritted from parent, the world alpha is only influenced by a
	 *         continous connection of parents with renderers. If there is a parent without a renderer, it stop
	 *         inheriting from any further parents.
	 */
	public float getWorldAlpha(boolean check)
	{
		if (check)
		{
			updateDownHiearchy();
		}
		return worldAlpha;
	}

	private void updateDownHiearchy()
	{
		if (this.transform().getParent() != null
				&& this.transform().getParent().renderer() != null
				&& this.transform().getParent().renderer().isEnabled())
		{
			this.transform().getParent().renderer().updateDownHiearchy();
		}
		updateAlpha(false);
	}

	/**
	 * Updates world alpha values and marks children to force update if a parent changes. This function is meant to be
	 * ran recursively down a tree.
	 * 
	 * @param forceUpdate
	 */
	void updateAlpha(boolean forceUpdate)
	{
		if (forceUpdate || inheritAlpha != oldInheritAlpha
				|| (parentAlphaChanged && inheritAlpha)
				|| alpha != oldAlpha)
		{
			updateAlphaValues();

			// flag chilcren for update so they know that their parent updated
			if (transform().children != null)
			{
				for (LttlTransform c : transform().children)
				{
					if (c.renderer != null)
					{
						c.renderer.parentAlphaChanged = true;
					}
				}
			}

			// reset
			oldAlpha = alpha;
			oldInheritAlpha = inheritAlpha;
			parentAlphaChanged = false;
		}
	}

	private void updateAlphaValues()
	{
		alpha = LttlMath.clamp(alpha, 0, 1);

		if (inheritAlpha && transform().getParent() != null
				&& transform().getParent().renderer() != null
				&& transform().getParent().renderer().isEnabled())
		{
			worldAlpha = alpha * transform().getParent().renderer().worldAlpha;
		}
		else
		{
			worldAlpha = alpha;
		}
	}

	/**
	 * Toggles (on or off) all renderers on all children. Does not toggle itself though.
	 * 
	 * @param toggle
	 */
	public void toggleAllChildrenRenderers(boolean toggle)
	{
		transform().toggleAllChildrenRenderers(toggle);
	}

	/**
	 * Sets the color value for this renderer
	 * 
	 * @param r
	 * @param g
	 * @param b
	 * @param a
	 * @return
	 */
	public void setColor(float r, float g, float b, float a)
	{
		this.color.set(r, g, b, a);

	}

	/**
	 * @return The color0 value for this object.
	 */
	public Color getColor()
	{
		return this.color;
	}

	// public Rectangle getWorldBoundingRect()
	// {
	// return worldBoundingRect;
	// }

	// public Rectangle getLocalBoundingRect()
	// {
	// return localBoundingRect;
	// }

	/**
	 * Sets or updates the uniform with 'name' with the value provided. <br>
	 * 
	 * @param name
	 *            Name of the uniform (ie. "u_time")
	 * @param value
	 */
	public void setUniform(String name, Object value)
	{
		// if map does not exist, create it
		if (uniformsMap == null)
		{
			uniformsMap = new HashMap<>();
		}
		uniformsMap.put(name, value);
	}

	/**
	 * Removes the requested uniform;
	 * 
	 * @param name
	 */
	public void removeUniform(String name)
	{
		// if map does not exist, create it
		if (uniformsMap == null) uniformsMap.remove(name);

	}

	/**
	 * Removes the requested uniform;
	 * 
	 * @param textLabel
	 */
	public void removeAllUniforms()
	{
		// if map does not exist, create it
		if (uniformsMap == null) uniformsMap.clear();
	}

	/**
	 * This sets any default uniforms, this is assumed that this shader type {@link LttlShader#hasDefaultUniforms()}
	 * 
	 * @param shaderObject
	 */
	void processDefaultUniforms(ShaderProgram shaderObject, LttlShader shaderType)
	{
		// assume (as of now) that any shader that uses default uniforms uses them all, otherwise would need to do
		// shaderObject.hasUniform("u_uvOffset")
		if (uvOffsetShader != null)
		{
			shaderObject.setUniformf("u_uvOffset", uvOffsetShader);
		}
		if (uvScaleShader != null)
		{
			shaderObject.setUniformf("u_uvScale", uvScaleShader);
		}
		if (colorUniform != null)
		{
			shaderObject.setUniformf(LttlShaders.UNIFORM_COLOR1, colorUniform);
		}
	}

	/**
	 * returns if it has any custom uniforms, does not check shader for them
	 * 
	 * @return
	 */
	boolean hasCustomUniforms()
	{
		return uniformsMap != null && uniformsMap.size() > 0;
	}

	/**
	 * Iterates through the Uniforms Map and adds them to the current shader.
	 * 
	 * @param shaderObject
	 */
	void processCustomUniforms(ShaderProgram shaderObject)
	{
		if (!hasCustomUniforms()) return;
		for (Map.Entry<String, Object> entry : uniformsMap.entrySet())
		{
			String name = entry.getKey();
			Object value = entry.getValue();
			if (!shaderObject.hasUniform(name))
			{
				Lttl.Throw("Shader: missing uniform:" + name + " on shader");
			}
			if (value.getClass() == Matrix3.class)
			{
				shaderObject.setUniformMatrix(name, (Matrix3) value);
			}
			else if (value.getClass() == Matrix4.class)
			{
				shaderObject.setUniformMatrix(name, (Matrix4) value);
			}
			else if (value.getClass() == Float.class)
			{
				shaderObject.setUniformf(name, (Float) value);
			}
			else if (value.getClass() == Integer.class)
			{
				shaderObject.setUniformi(name, (Integer) value);
			}
			else if (value.getClass() == Vector2.class)
			{
				shaderObject.setUniformf(name, (Vector2) value);
			}
			else if (value.getClass() == Vector3.class)
			{
				shaderObject.setUniformf(name, (Vector3) value);
			}
			else if (value.getClass() == Color.class)
			{
				shaderObject.setUniformf(name, (Color) value);
			}
			else
			{
				Lttl.Throw(
						"There was no setUniform() for " + name + ":" + value.toString());
			}
		}
	}

	// void updateWorldBoundingRect()
	// {
	// if (object.world.worldSettings.checkOnScreen && object.checkOnScreen)
	// {
	// LttlMath.TransformRectangle(localBoundingRect,
	// object.worldTransform, worldBoundingRect);
	// }
	// }

	// private void updateLocalBoundingRect()
	// {
	// if (object.world.worldSettings.checkOnScreen && object.checkOnScreen)
	// {
	// LttlGraphics.getMeshBoundingRectangle(mesh, localBoundingRect);
	// }
	// }

	// /**
	// * If changes are made to a mesh, this updates the mesh so it can render successfully. <br>
	// * (so far only really important for boundingRect)
	// */
	// public void updateMesh()
	// {
	// //updateLocalBoundingRect();
	// }

	/**
	 * Returns this renderer's mesh.<br>
	 * <br>
	 * Use LttlMeshGenerator to generate mesh
	 * 
	 * @return the mesh
	 */
	public LttlMesh getMesh()
	{
		return mesh;
	}

	/**
	 * @param mesh
	 *            the mesh to set
	 */
	public void setMesh(LttlMesh mesh)
	{
		if (mesh != null)
		{
			mesh.modified();
		}
		this.mesh = mesh;
		onMeshChange();
	}

	/**
	 * Sets this renderer's mesh to null which will force it be made again
	 */
	@GuiButton
	public void clearMesh()
	{
		setMesh(null);
	}

	/**
	 * The current Mesh Generator on this transform, only one allowed on transform (renderer requried). This helps avoid
	 * getComponent() calls since LttlMeshGenerator is called often in render loop
	 * 
	 * @return
	 */
	public LttlMeshGenerator generator()
	{
		return meshGenerator;
	}

	public LttlTexture getTex0()
	{
		return texture0;
	}

	public LttlTexture getTex1()
	{
		return texture1;
	}

	/**
	 * Returns a rectangle object (shared with this LttlRenderer instance) that contains the axis aligned bounding rect
	 * for this mesh, with all the transformations (rotation, scale, and position).<br>
	 * <br>
	 * <b>It is always axis aligned though.</b>
	 * 
	 * @param output
	 * @return
	 */
	public Rectangle getMeshBoundingRectTransformedAxisAligned()
	{
		if (meshBoundingRectTransformedAxisAligned == null)
		{
			meshBoundingRectTransformedAxisAligned = new Rectangle();
		}

		// marked for updated
		if (meshBoundingRectTransformedAxisAligned.width == 0
				&& meshBoundingRectTransformedAxisAligned.height == 0)
		{
			getMesh().getBoundingRectTransformedAxisAligned(
					t().getWorldRenderTransform(true),
					meshBoundingRectTransformedAxisAligned);
		}
		return meshBoundingRectTransformedAxisAligned;
	}

	/**
	 * Returns the transformed mesh's vertices without attributes (this takes into consideration if the mesh or world
	 * transform has updated, so it's cached)<br>
	 * <b>Do not modify these</b>
	 * 
	 * @param forceWorldMeshUpdate
	 *            is only necessary if this is before the mesh's world values were updated (before render loop) and it
	 *            only matters if it's a preMultiplyMesh
	 * @param includeHoles
	 * @param includeAA
	 *            if applicable, will also include holes
	 * @return
	 */
	public Vector2Array getMeshTransformedVertices(boolean forceWorldMeshUpdate,
			boolean includeHoles, boolean includeAA)
	{
		if (meshTransformedVertices == null)
		{
			meshTransformedVertices = new Vector2Array();
		}

		// only update if marked to
		int desiredSize = mesh.getVertextCount(true, includeHoles, includeAA);
		if (meshTransformedVertices.size() == 0
				|| meshTransformedVertices.size() != desiredSize)
		{
			if (getMesh().getWorldVerticesArray().size == 0 || forceWorldMeshUpdate)
			{
				getMesh().updateWorldVertices(t().worldRenderTransform);
			}
			if (includeAA)
			{
				getMesh().getVerticesPos(true, meshTransformedVertices);
			}
			else if (includeHoles)
			{
				getMesh().getVerticesPosNoAA(true, meshTransformedVertices);
			}
			else
			{
				getMesh().getVerticesPos(true, meshTransformedVertices);
			}
		}

		return meshTransformedVertices;
	}

	private void onMeshChange()
	{
		// can be null
		onWorldTransformChange();
	}

	void onWorldTransformChange()
	{
		// mark mesh world vertices to update when rendering, it is better to do that then update it here, since it
		// could be a multi renderer or this could be called multiple times a frame, so that just ensures one update is
		// done when it needs to be done
		if (r().getMesh() != null)
		{
			r().getMesh().getWorldVerticesArray().clear();
		}
		// mark the world custom bounding rect cache to be updated next time it's needed
		if (customBoundingRect != null
				&& customBoundingRectTransformedAxisAligned != null)
		{
			customBoundingRectTransformedAxisAligned.set(0, 0, 0, 0);
		}
		// mark for updating
		if (meshTransformedVertices != null)
		{
			meshTransformedVertices.clear();
		}
		if (meshBoundingRectTransformedAxisAligned != null)
		{
			meshBoundingRectTransformedAxisAligned.set(0, 0, 0, 0);
		}
	}

	/**
	 * Returns the custom bounding rect with world values/transformed that is axis aligned.<b>Cached</b>
	 * 
	 * @return
	 */
	private Rectangle getCustomBoundingRectTransformedAxisAligned()
	{
		Lttl.Throw(customBoundingRect == null);
		if (customBoundingRectTransformedAxisAligned == null)
		{
			customBoundingRectTransformedAxisAligned = new Rectangle();
		}
		// marked to be updated, which happened when world transform changed
		if (customBoundingRectTransformedAxisAligned.height == 0
				&& customBoundingRectTransformedAxisAligned.width == 0)
		{
			// only if it doesn't have a parent then transform the rectangle with the world matrix
			LttlMath.GetAABB(
					LttlMath.TransformRectangle(customBoundingRect,
							t().getWorldRenderTransform(true), null),
					customBoundingRectTransformedAxisAligned);
		}
		return customBoundingRectTransformedAxisAligned;
	}

	@Override
	public void debugDraw()
	{
		if (getMesh() == null) { return; }

		if (drawCustomBoundingRect && customBoundingRect != null)
		{
			float[] p = LttlMath.TransformRectangle(customBoundingRect,
					t().getWorldRenderTransform(true), null);
			Lttl.editor.getSettings().colorRenderCheck.a *= .5f;
			Lttl.debug.drawPolygonOutline(p, 0,
					Lttl.editor.getSettings().colorRenderCheck);
			Lttl.editor.getSettings().colorRenderCheck.a *= 2f;
			Lttl.debug.drawRectOutline(LttlMath.GetAABB(p, null), 0,
					Lttl.editor.getSettings().colorRenderCheck);
		}

		// already checked for mesh by this point, and staged, which means world values are updated
		if (drawMeshOultine)
		{
			Vector2Array points = getMeshTransformedVertices(false, true, false);
			if (getMesh().getHolesCount() > 0)
			{
				IntArray holes = getMesh().getHolesIndexArray();
				int start = 0;
				for (int i = 0, n = holes.size; i <= n; i++)
				{
					int end = i == n ? points.size() : holes.get(i);
					Lttl.debug.drawPolygonOutline(points.toArray(start, end - 1), 0,
							Lttl.editor.getSettings().colorMeshOultine);
					start = end;
				}
			}
			else
			{
				Lttl.debug.drawPolygonOutline(points, 0,
						Lttl.editor.getSettings().colorMeshOultine);
			}
		}
		if (drawMeshBoundingRectAxisAligned)
		{
			Lttl.debug.drawRectOutline(getMeshBoundingRectTransformedAxisAligned(), 0,
					Lttl.editor.getSettings().colorMeshBounding);
		}
		if (drawMeshBoundingRect)
		{
			Lttl.debug.drawPolygonOutline(getMeshBoundingRectTransformed(), 0,
					Lttl.editor.getSettings().colorMeshBounding);
		}
		if (drawMeshTriangles)
		{
			ShortArray indices = getMesh().getIndicesArray();
			Vector2Array vertices = getMeshTransformedVertices(true, true, true);
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

	/**
	 * Returns a float array of the transformed bounding rect of this mesh (includes scale, rotation, position). Should
	 * only be called if updates are assumed, since kinda expensive.<br>
	 * <br>
	 * If you want a rectangle (not rotated) and not float[], then use getMeshBoundingRectAxisAligned().
	 * 
	 * @return
	 */
	public float[] getMeshBoundingRectTransformed()
	{
		return getMesh().getBoundingRectTransformed(t().getWorldRenderTransform(true),
				new float[8]);
	}

	// *********************//
	// ******* TWEENS ******//
	// *********************//

	/**
	 * Tweens alpha to target value.
	 * 
	 * @param targetAlpha
	 * @param duration
	 * @return
	 */
	public Tween tweenAlphaTo(float targetAlpha, float duration)
	{
		final LttlRenderer thisThis = this;
		return Tween.to(this, new TweenGetterSetter()
		{
			@Override
			public void set(float[] values)
			{
				thisThis.alpha = values[0];
			}

			@Override
			public float[] get()
			{
				return new float[]
				{ thisThis.alpha };
			}
		}, duration).target(targetAlpha);
	}

	/**
	 * Tweens color0 to target. If want to tween individual color properties use Lttl.tween.tweenColor, and specify
	 * tweenType.
	 * 
	 * @param targetColor
	 * @param duration
	 * @return
	 */
	public Tween tweenColorTo(Color targetColor, float duration)
	{
		return tweenColorTo(targetColor.r, targetColor.g, targetColor.b, targetColor.a,
				duration);
	}

	/**
	 * Tweens color0 to target. If want to tween individual color properties use Lttl.tween.tweenColor, and specify
	 * tweenType.
	 * 
	 * @param targetR
	 * @param targetG
	 * @param targetB
	 * @param targetA
	 * @param duration
	 * @return
	 */
	public Tween tweenColorTo(float targetR, float targetG, float targetB, float targetA,
			float duration)
	{
		return Lttl.tween.tweenColorTo(this, getColor(), targetR, targetG, targetB,
				targetA, duration);
	}

	@SuppressWarnings("unused")
	private void onTextureChangeGui()
	{
		if (generator() == null) return;
		generator().updateMesh();
	}

	IntMap<TweenGetterSetter> cachedTweenGetterSetters = new IntMap<>(0);

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
					cachedTweenGetterSetters.put(animID, new TweenGetterSetter()
					{

						@Override
						public void set(float[] values)
						{
							alpha = values[0];
						}

						@Override
						public float[] get()
						{
							return new float[]
							{ alpha };
						}
					});
					break;
				}
			}
		}
		TweenGetterSetter result = cachedTweenGetterSetters.get(animID, null);
		return result;
	}

	/**
	 * Returns the rect used for checking if should render or not. This assumes a mesh exists.
	 * 
	 * @return
	 */
	Rectangle getRenderCheckRect()
	{
		if (customBoundingRect != null)
		{
			return getCustomBoundingRectTransformedAxisAligned();
		}
		else
		{
			return getMeshBoundingRectTransformedAxisAligned();
		}
	}

	/**
	 * Checks if transform is enabled (including parents), checks renderer exist and is enabled, has a shader and not
	 * invisible, and if it doesn't have a mesh or a mesh generator<br>
	 * only call during render loop (after stage), or it may not be accurate<br>
	 * Does not check {@link #doNotRender} because that is only checked during the noemal renderLoop, that way post
	 * processing can run canRender() without fail.
	 * 
	 * @return if can render
	 */
	public boolean canRender()
	{
		// check this before cached one because it does not set it's hasUpdatedCanRender to false unless it's enabled
		// this frame
		if (!t().enabledThisFrame) { return canRender = false; }

		// return cached canRender
		if (hasUpdatedCanRender) { return canRender; }

		/* update canRender */
		// transform not enabled this frame
		// renderer is not enabled or has a 0 world alpha
		if (!t().enabledThisFrame || !isEnabled()
				|| getWorldAlpha(false) == 0) { return canRender = false; }

		// if no shader, show error if first time
		if (shader == null)
		{
			if (!noShaderWarning)
			{
				noShaderWarning = true;
				Lttl.logNote("Rendering: the renderer on " + t().getName()
						+ " does not have a shader.");
			}
			return canRender = false;
		}

		// if no mesh and no generator to make it, show error if first time
		if (getMesh() == null && generator() == null)
		{
			if (!noMeshWarning)
			{
				noMeshWarning = true;
				Lttl.logNote("Rendering: the renderer on " + t().getName()
						+ " does not have a mesh or a generator.");
			}
			return canRender = false;
		}

		// reset warnings
		noShaderWarning = false;
		noMeshWarning = false;

		// Checking if should create mesh or update it if it's last update's zoom is different from the the
		// current camera's zoom
		if (generator() != null && generator().isEnabled())
		{
			// active generator exists

			// check if mesh exists
			if (getMesh() == null)
			{
				// maybe it has not been created yet (wasn't enabled or not visible on start, or was created after
				// program started)
				generator().updateMesh();

				// generator failed
				if (getMesh() == null)
				{
					if (!generatorWarning)
					{
						generatorWarning = true;
						Lttl.logNote("Renderering: renderer on " + t().getName()
								+ " does not have a mesh, even after generator attempted to create one.");
					}
					return canRender = false;
				}
				// mesh now exists at this point
			}
			// mesh exists and it has an active generator to possibly update if suppose to autoUpdate AA
			// if mesh exists and the camera is not already autoUpdatingMeshesOnZoom, then check to see if the
			// object's last update's zoom is different from the the current camera's zoom, if so, put camera into
			// autoUpdateMode. This is better than just updating the mesh now, since there could be a lot of these
			// and it would lag the game. Scenario: Object was disabled or invisible, but already had a mesh, and
			// now it is enabled and visible but the camera zoom has changed since it's initial mesh creation
			// The reason why we update these meshes over time is because it is just for slight AA fixes, while
			// above, the meshes don't even exist at all.
			else if (!Lttl.game.getSettings().getTargetCamera()
					.isAutoUpdatingMeshesOnZoom() && generator().aaSettings != null
					&& generator().aaSettings.cameraZoomDependent
					&& generator().aaSettings.autoUpdateOnCameraZoom
					&& generator().getCameraZoomOnLastUpdateMesh() != Lttl.game
							.getSettings().getTargetCamera().zoom)
			{
				Lttl.game.getSettings().getTargetCamera().autoUpdatingMeshesOnZoom = true;
				// camera won't start updating til next frame (STAGING)
			}
		}
		else if (getMesh() == null)
		{
			// no generator and no mesh
			return canRender = false;
		}

		// reset generator warning
		generatorWarning = false;

		// mark that has updated canRender this frame
		hasUpdatedCanRender = true;

		// finally
		return canRender = true;
	}

	@GuiButton
	void printMeshDetails()
	{
		if (getMesh() == null)
		{
			Lttl.logNote("Mesh Details: null");
		}
		else
		{
			Lttl.logNote(getMesh().toString());
		}
	}
}
