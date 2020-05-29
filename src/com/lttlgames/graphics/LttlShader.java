package com.lttlgames.graphics;

import com.lttlgames.editor.annotations.Persist;

@Persist(-9038)
public enum LttlShader
{
	/**
	 * 
	 */
	SimpleColorShader,
	/**
	 * Used if you want to multiply the colors, takes alpha into consideration to dull the multiply amount. Meant to be
	 * used with {@link LttlBlendMode#MULTIPLY}
	 */
	SimpleColorMultiplyShader,
	/**
	 * 
	 */
	ColorTextureMultipliedShader,
	/**
	 * 
	 */
	ColorGTextureMultipliedShader,
	/**
	 * 
	 */
	GradientShader(true),
	/**
	 * 
	 */
	GaussianBlurHShader,
	/**
	 * 
	 */
	GaussianBlurVShader,
	/**
	 * 
	 */
	TextureGMaskShader,
	/**
	 * 
	 */
	TextureSelfOtherGMaskShader,
	/**
	 * 
	 */
	TextureSelfGMaskShader,
	/**
	 * Renders textures and multiplies color.
	 */
	TexturesShader,
	/**
	 * Renders a texture and multiplies color.
	 */
	TextureShader,
	/**
	 * Renders a texture and adds the color (multiplied by alpha);
	 */
	TextureAdditiveColorShader,
	/**
	 * 
	 */
	// CycleColorFadeShader,
	/**
	 * 
	 */
	VertexColorShader,
	/**
	 * Use uvOffset and uvScale on renderer()
	 */
	TextureTransformUVShader(true),
	/**
	 * adds color to texture, Use uvOffset and uvScale on renderer()
	 */
	TextureAdditiveColorTransformUVShader(true);

	private boolean hasDefaultUniforms;

	private LttlShader()
	{
		this.hasDefaultUniforms = false;
	}

	private LttlShader(boolean hasDefaultUniforms)
	{
		this.hasDefaultUniforms = hasDefaultUniforms;
	}

	public boolean hasDefaultUniforms()
	{
		return hasDefaultUniforms;
	}
}