package com.lttlgames.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;
import com.lttlgames.editor.LoopManager.RenderType;
import com.lttlgames.graphics.LttlBlendMode;
import com.lttlgames.graphics.LttlMesh;
import com.lttlgames.graphics.LttlMeshFactory;
import com.lttlgames.graphics.LttlShader;

/**
 * This is given in the LttlGameStarter and is used for handing the processing before, while, after the renders.<br>
 * <b>NOTES:</b><br>
 * 1) It may be wise to create a component that holds some properties (component references, floats, booleans, etc) that
 * {@link #onStart()} you can get and then use the entire time.<br>
 * 2) This object is created
 * 
 * @author Josh
 */
public abstract class LttlProcessing
{
	private FrameBuffer currentFBO = null;
	private LttlMesh quad;
	// http://www.java2s.com/Open-Source/Android/Game/libgdx/com/badlogic/gdx/tests/FrameBufferTest.java.htm

	// I don't know why this has to be 2, but it does
	protected static final Matrix4 fullScreenMatrix = new Matrix4()
			.setToScaling(2f, 2f, 1);
	boolean init = false;

	/**
	 * called once before first {@link #onStart()}<br>
	 * This is the first place you can create frame buffers<br>
	 * NOTE: This is a good place to find a helper component to get values from, since world scene is fully loaded by
	 * here
	 */
	protected void onInit()
	{
	}

	/**
	 * called when screen resizes, <b>will NOT call on initial load/resize</b> though.
	 */
	protected void onResize()
	{

	}

	/**
	 * Called before rendering play view.
	 */
	protected void onStart()
	{

	}

	/**
	 * Called after rendering play view, debug and clipping still renders after this.
	 */
	protected void onComplete()
	{
	}

	/**
	 * Before a transform renders. This transform is expected to render, has a renderer, mesh, shader, doNotRender is
	 * false, is not consumed, and if checking, is in camera view.<br>
	 * This even calls for multi renders as whole.
	 * 
	 * @param lt
	 * @return if should render or not
	 */
	protected boolean beforeTransform(LttlTransform lt)
	{
		return true;
	}

	/**
	 * After a transform renders, will only callback if it actually rendered.
	 * 
	 * @param lt
	 */
	protected void afterTransform(LttlTransform lt)
	{
	}

	/**
	 * Before a multi renderer's draw. This is only done if multi renderer has post processing callbacks enabled.
	 * 
	 * @param lt
	 * @return if should render or not
	 */
	protected boolean beforeMultiRenderDraw(LttlTransform lt)
	{
		return true;
	}

	/**
	 * After a multi renderer's draw. This is only done if multi renderer has post processing callbacks enabled.
	 * 
	 * @param lt
	 */
	protected void afterMultiRenderDraw(LttlTransform lt)
	{
	}

	/**
	 * Disposes all the Frame Buffers that were used.
	 */
	protected abstract void dispose();

	/**
	 * clears the current buffer with color
	 */
	final protected void clear(Color color)
	{
		Gdx.gl.glClearColor(color.r, color.g, color.b, color.a);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
	}

	// UNTESTED
	// http://gamedev.stackexchange.com/questions/8568/opengl-es-clearing-the-alpha-of-the-framebufferobject
	/**
	 * clears the current buffer to transparent
	 */
	final protected void clearTransparent()
	{
		// This ensures that only alpha will be effected
		Gdx.gl.glColorMask(false, false, false, true);
		// last is the alpha value to which you need to clear
		Gdx.gl.glClearColor(0, 0, 0, 0);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
	}

	/**
	 * Sets the current frame buffer, this is what things will render on to. Automatically checks to see if different
	 * fbo then currently set, and if so, flushes, ends current fbo, adn begins new.<br>
	 * If newFBO is already set, then does nothing
	 * 
	 * @param newFBO
	 *            the new FrameBuffer to be set, null sets to default
	 */
	final protected void setFrameBuffer(FrameBuffer newFBO)
	{
		if (newFBO != currentFBO)
		{
			flush();
			// end previous fbo
			if (currentFBO != null)
			{
				// could be default, which means there is nothing to end
				currentFBO.end();
				// set back to default viewport
				Lttl.game.getCamera().setViewport();
			}
			// start new fbo
			if (newFBO != null)
			{
				newFBO.begin();
			}
			else
			{
				// if setting to default, then set default viewport
				Lttl.game.getCamera().setViewport();
			}
			currentFBO = newFBO;
		}
	}

	/**
	 * Returns the current frame buffer, null means default
	 * 
	 * @return
	 */
	final protected FrameBuffer getCurrentFBO()
	{
		return currentFBO;
	}

	final protected void flush()
	{
		Lttl.loop.flush(RenderType.Post, null, null);
	}

	/**
	 * Set's the specified FBO texture to the main buffer with {@link LttlShader#TextureShader} and blend mode.<br>
	 * {@link LttlBlendMode#ONE_MULTIPLY} usually works best, when you want the background of what is already on the
	 * main buffer to show.
	 * 
	 * @param fbo
	 * @param blendMode
	 */
	final protected void renderDrawFullScreen(FrameBuffer fbo,
			LttlBlendMode blendMode)
	{
		renderDrawFullScreen(fbo.getColorBufferTexture(), null, blendMode,
				Color.WHITE, 1, LttlShader.TextureShader);
	}

	final protected void renderDrawFullScreen(Texture tex0, Texture tex1,
			LttlBlendMode blendMode, Color color, float alpha, LttlShader shader)
	{
		renderDraw(tex0, tex1, blendMode, color, alpha, shader,
				fullScreenMatrix);
	}

	final protected void renderDraw(Texture tex0, Texture tex1,
			LttlBlendMode blendMode, Color color, float alpha,
			LttlShader shader, Matrix4 renderMatrix)
	{
		LttlMesh mesh = getQuad(color, alpha);
		renderDraw(blendMode, shader, tex0, tex1, mesh.getVerticesArray(),
				mesh.getIndicesArray(), renderMatrix);
	}

	final protected void renderDraw(LttlBlendMode blendMode, LttlShader shader,
			Texture tex0, Texture tex1, FloatArray vertices,
			ShortArray indices, Matrix4 renderMatrix)
	{
		Lttl.loop.renderDraw(RenderType.Post, blendMode, shader, tex0, tex1,
				vertices, indices, renderMatrix, null);
	}

	/**
	 * Creates a FBO with the given pixel width, calculates the height based on the exact screen dimensions. (taking
	 * into account if in editor)<br>
	 * Requires Gdx to be fully loaded, so do it {@link #onStart()}
	 * 
	 * @param width
	 * @return
	 */
	final protected FrameBuffer createFBO(int width)
	{
		int height = (int) (width * Lttl.game.getCamera().getRawPixelHeight() / Lttl.game
				.getCamera().getRawPixelWidth());
		return createFBO(width, height);
	}

	/**
	 * Creates a FBO a factor of the screen dimensions, 1 being the same size as screen.<br>
	 * Requires Gdx to be fully loaded, so do it {@link #onStart()}
	 * 
	 * @param scale
	 * @return
	 */
	final protected FrameBuffer createFBO(float scale)
	{
		int width = (int) (Lttl.game.getCamera().getRawPixelWidth() * scale);
		int height = (int) (Lttl.game.getCamera().getRawPixelHeight() * scale);
		return createFBO(width, height);
	}

	final protected FrameBuffer createFBO(int width, int height)
	{
		return new FrameBuffer(Format.RGBA8888, width, height, false);
	}

	/**
	 * Requires game to be fully loaded, so do it {@link #onStart()}
	 * 
	 * @return
	 */
	final protected FrameBuffer createFullCameraFBO()
	{
		return createFBO((int) Lttl.game.getCamera()
				.getViewportPixelWidthStatic(), (int) Lttl.game.getCamera()
				.getViewportPixelHeightStatic());
	}

	/**
	 * @param srcFBO
	 * @param dstFBO
	 *            can be the same as src or default
	 * @param blend
	 */
	protected void gaussianBlur(FrameBuffer srcFBO, FrameBuffer dstFBO,
			LttlBlendMode blend)
	{
		setFrameBuffer(srcFBO);

		// first pass on to self
		renderDraw(srcFBO.getColorBufferTexture(), null, LttlBlendMode.NONE,
				Color.WHITE, 1, LttlShader.GaussianBlurHShader,
				fullScreenMatrix);
		// will auto flush because unique uniform

		setFrameBuffer(dstFBO);
		renderDraw(srcFBO.getColorBufferTexture(), null, blend, Color.WHITE, 1,
				LttlShader.GaussianBlurVShader, fullScreenMatrix);
		// will auto flush because unique uniform
	}

	/**
	 * Adds a solid color rectangle over the whole screen for the current FBO.
	 * 
	 * @param color
	 * @param alpha
	 * @param blend
	 *            (ie. {@link LttlBlendMode#MULTIPLY} and {@link LttlBlendMode#ADDITIVE} are cool)
	 */
	protected void tint(Color color, float alpha, LttlBlendMode blend)
	{
		renderDrawFullScreen(null, null, blend, color, alpha,
				LttlShader.SimpleColorShader);
	}

	protected LttlMesh getQuad(Color color, float alpha)
	{
		if (quad == null)
		{
			quad = LttlMeshFactory.GenerateQuad(quad, 1, 0);
		}
		quad.setAlphaAll(alpha);
		quad.setColorAll(color.toFloatBits());
		return quad;
	}

	/**
	 * Renders the transform. This does not mark a renderer as hasRenderer, and can be ran any number of times. The
	 * transform is not checked to see if it can renderer, <b>make sure it has a renderer and canRender()</b>
	 * 
	 * @param lt
	 */
	protected void renderTransform(LttlTransform lt)
	{
		Lttl.loop.renderFromPostProcessing(lt);
	}

	final protected void setMeshQuiet(LttlMesh mesh, LttlRenderer renderer)
	{
		renderer.mesh = mesh;
	}
}
