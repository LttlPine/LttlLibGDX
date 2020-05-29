package com.lttlgames.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.lttlgames.editor.Lttl;

/**
 * @author Josh
 */
public final class LttlShaders
{
	public static final String ATTR_COLOR = ShaderProgram.COLOR_ATTRIBUTE;
	public static final String ATTR_POS = ShaderProgram.POSITION_ATTRIBUTE;
	public static final String ATTR_TEXCOORD = ShaderProgram.TEXCOORD_ATTRIBUTE;
	public static final String ATTR_ALPHA = "a_alpha";

	public static final String UNIFORM_WORLD_MATRIX = "u_worldView";
	public static final String UNIFORM_COLOR1 = "u_color1";

	private LttlShaders()
	{
		// Exists only to defeat instantiation.
	}

	/**
	 * Enables/Disables blending and sets the blending function RIGHT NOW.
	 * 
	 * @param blend
	 */
	public static void blend(LttlBlendMode blend)
	{
		switch (blend)
		{
			case NONE:
				Gdx.gl.glDisable(GL20.GL_BLEND);
				break;
			case ALPHA:
				Gdx.gl.glEnable(GL20.GL_BLEND);
				Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA,
						GL20.GL_ONE_MINUS_SRC_ALPHA);
				break;
			case ONE_MULTIPLY:
				Gdx.gl.glEnable(GL20.GL_BLEND);
				Gdx.gl.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
				break;
			case MULTIPLY:
				Gdx.gl.glEnable(GL20.GL_BLEND);
				Gdx.gl.glBlendFunc(GL20.GL_ZERO, GL20.GL_SRC_COLOR);
				break;
			case ADDITIVE:
				// idk
				Gdx.gl.glEnable(GL20.GL_BLEND);
				Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
				break;
			case DST_ALPHA_MASK:
				Gdx.gl.glEnable(GL20.GL_BLEND);
				Gdx.gl.glBlendFunc(GL20.GL_DST_ALPHA, GL20.GL_ZERO);
				break;
			case DST_INVERSE_ALPHA_MASK:
				Gdx.gl.glEnable(GL20.GL_BLEND);
				Gdx.gl.glBlendFunc(GL20.GL_ONE_MINUS_DST_ALPHA, GL20.GL_ONE);
				break;
			case REPLACE:
				Gdx.gl.glEnable(GL20.GL_BLEND);
				Gdx.gl.glBlendFunc(GL20.GL_ONE, GL20.GL_ZERO);
				break;
			default:
				Gdx.gl.glDisable(GL20.GL_BLEND);
				break;
			case SCREEN:
				// src: http://stackoverflow.com/questions/818230/photoshop-blending-mode-to-opengl-es-without-shaders
				Gdx.gl.glEnable(GL20.GL_BLEND);
				Gdx.gl.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_COLOR);
				break;
		}
	}

	// @formatter:off
	private static String ANDROID_FIX = "#ifdef GL_ES\n"
			+ "precision mediump float;\n" + "#endif\n";
	//TODO can VERTEX_ATTRIBUTE_POSITION be a vec2 or vec4
	private static String VERT_ATTRIBUTES_UNIFORMS_NO_TEXCOORD =
			"attribute vec4 " + LttlShaders.ATTR_POS + ";"
			+ "attribute vec4 " + LttlShaders.ATTR_COLOR + ";"
			+ "attribute float " + LttlShaders.ATTR_ALPHA + ";"
			+ "uniform mat4 " + LttlShaders.UNIFORM_WORLD_MATRIX + ";";
	private static String VERT_ATTRIBUTE_UNIFORM =
			"attribute vec2 " + LttlShaders.ATTR_TEXCOORD + ";"
			+ VERT_ATTRIBUTES_UNIFORMS_NO_TEXCOORD;
	private static String VERT_DEFAULT_VARYING = 
			"varying vec4 v_color;"
			+ "varying vec2 v_tex0Coord;";
	private static String VERT_DEFAULT_MAIN_START =
			"void main()"
			+ "{"
			+ "v_color = " + LttlShaders.ATTR_COLOR + ";"
			+ "v_color.a = " + LttlShaders.ATTR_ALPHA + ";"
			+ "gl_Position = " + UNIFORM_WORLD_MATRIX + " * " + LttlShaders.ATTR_POS + ";"
			+ "v_tex0Coord = " + LttlShaders.ATTR_TEXCOORD  + ";";
	// @formatter:on

	/**
	 * Loads and compiles the shader exiting on any errors.
	 * 
	 * @param vert
	 * @param frag
	 * @return The requested shader as a ShaderProgram.
	 */
	private static ShaderProgram load(String vert, String frag)
	{
		ShaderProgram shader = new ShaderProgram(vert, frag);
		if (!shader.isCompiled())
		{
			Lttl.logNote("ShaderTest: " + shader.getLog());
			Lttl.logNote("ShaderTest: " + vert);
			Lttl.logNote("ShaderTest: " + frag);
			Lttl.Throw();
		}
		return shader;
	}

	/**
	 * Returns the instance of the specified shader (assumes single texture)
	 * 
	 * @param shader
	 * @return
	 */
	public static ShaderProgram getShader(LttlShader shader)
	{
		return getShader(shader, false);
	}

	/**
	 * Returns an instance of the specified shader
	 * 
	 * @param shader
	 * @param multipleTextures
	 *            means the shader that is being used should assume it is using two different textures
	 * @return
	 */
	public static ShaderProgram getShader(LttlShader shader,
			boolean multipleTextures)
	{
		switch (shader)
		{
			case SimpleColorShader:
			{
				return getSimpleColorShader();
			}
			case SimpleColorMultiplyShader:
			{
				return getSimpleColorMultiplyShader();
			}
			case ColorTextureMultipliedShader:
			{
				return getColorTextureMultipliedShader();
			}
			case ColorGTextureMultipliedShader:
			{
				return getColorGTextureMultipliedShader();
			}
			case GradientShader:
			{
				return getGradientShader();
			}
			case GaussianBlurHShader:
			{
				return getGaussianBlurHShader();
			}
			case GaussianBlurVShader:
			{
				return getGaussianBlurVShader();
			}
			case TextureGMaskShader:
			{
				if (multipleTextures) return getTextureGMaskDifShader();
				else return getTextureGMaskShader();
			}
			case TextureSelfOtherGMaskShader:
			{
				if (multipleTextures) return getTextureSelfOtherGMaskDifShader();
				else return getTextureSelfOtherGMaskShader();
			}
			case TextureSelfGMaskShader:
			{
				return getTextureSelfGMaskShader();
			}
			case TexturesShader:
			{
				if (multipleTextures) return getTexturesDifShader();
				else return getTexturesShader();
			}
			case TextureShader:
			{
				return getTextureShader();
			}
			case TextureAdditiveColorShader:
			{
				return getTextureAdditiveColorShader();
			}
			// case CycleColorFadeShader:
			// {
			// return getCycleColorFadeShader();
			// }
			case VertexColorShader:
			{
				return getVertexColorShader();
			}
			case TextureTransformUVShader:
			{
				return getTextureTransformUVShader();
			}
			case TextureAdditiveColorTransformUVShader:
			{
				return getTextureAdditiveColorTransformUV();
			}
			default:
			{
				Lttl.Throw("No shader found.");
				return null;
			}
		}
	}

	// @formatter:on

	/**
	 * Makes mesh all one color (includes transparency) and AA.
	 */
	private static ShaderProgram getSimpleColorShader()
	{
		if (simpleColorShader == null)
		{
			simpleColorShader = load(simpleColorShaderVert,
					simpleColorShaderFrag);
		}
		return simpleColorShader;
	}

	private static ShaderProgram simpleColorShader;
	// @formatter:off
	private static String simpleColorShaderVert = VERT_ATTRIBUTES_UNIFORMS_NO_TEXCOORD
			+ "varying vec4 v_color;"
			+ "void main()"
			+ "{"
			+ "v_color = " + LttlShaders.ATTR_COLOR + ";"
			+ "v_color.a = " + LttlShaders.ATTR_ALPHA + ";"
			+ "gl_Position = u_worldView * a_position;"
			+ "}";
	private static String simpleColorShaderFrag = ANDROID_FIX
			+ "varying vec4 v_color;"
			+ "void main()"
			+ "{"
			+ "gl_FragColor = v_color;"
			+ "}";

	// @formatter:on

	/**
	 * Makes mesh all one color (uses alpha to dull the multiply amount) and AA
	 */
	private static ShaderProgram getSimpleColorMultiplyShader()
	{
		if (simpleColorMultiplyShader == null)
		{
			simpleColorMultiplyShader = load(simpleColorMultiplyShaderVert,
					simpleColorMultiplyShaderFrag);
		}
		return simpleColorMultiplyShader;
	}

	private static ShaderProgram simpleColorMultiplyShader;
	// @formatter:off
	private static String simpleColorMultiplyShaderVert = VERT_ATTRIBUTES_UNIFORMS_NO_TEXCOORD
			+ "varying vec4 v_color;"
			+ "void main()"
			+ "{"
			+ "v_color = " + LttlShaders.ATTR_COLOR + ";"
			+ "v_color.r += 1 - " +  LttlShaders.ATTR_ALPHA + ";"
			+ "v_color.g += 1 - " +  LttlShaders.ATTR_ALPHA + ";"
			+ "v_color.b += 1 - " +  LttlShaders.ATTR_ALPHA + ";"
			+ "gl_Position = u_worldView * a_position;"
			+ "}";
	private static String simpleColorMultiplyShaderFrag = ANDROID_FIX
			+ "varying vec4 v_color;"
			+ "void main()"
			+ "{"
			+ "gl_FragColor = v_color;"
			+ "}";

	// @formatter:on

	/**
	 * Renders a solid color with an atlas region texture multipled to it (the texture's alpha is ignored) [uses
	 * u_texAlpha].
	 */
	private static ShaderProgram getColorTextureMultipliedShader()
	{
		if (colorTextureMultipliedShader == null)
		{
			colorTextureMultipliedShader = load(
					colorTextureMultipliedShaderVert,
					colorTextureMultipliedShaderFrag);
		}
		return colorTextureMultipliedShader;
	}

	private static ShaderProgram colorTextureMultipliedShader;
	// @formatter:off
	private static String colorTextureMultipliedShaderVert = "attribute vec4 a_position;"
			+ "attribute vec2 a_texCoord;"
			+ "varying vec2 v_texCoord;"
			+ "attribute float a_alpha;"
			+ "uniform mat4 u_worldView;"
			+ "uniform vec4 u_color;"
			+ "varying float v_texAlpha;"
			+ "uniform float u_alpha;"
			+ "varying vec4 v_color;"
			+ "varying vec2 v_tex0Coord;"
			+ "void main() {"
			+ "v_color = vec4(u_color.rgb, u_alpha * a_alpha);"
			+ "v_texCoord = a_texCoord;"
			+ "v_texAlpha = a_color.a;"
			+ "gl_Position = u_worldView * a_position;"
			+ "v_tex0Coord = a_texCoord;"
			+ "}";
	private static String colorTextureMultipliedShaderFrag = ANDROID_FIX
			+ "varying vec4 v_color;"
			+ "varying vec2 v_tex0Coord;"
			+ "varying float v_texAlpha;"
			+ "uniform sampler2D u_texture0;"
			+ "void main() {"
			+ "vec4 texel = texture2D(u_texture0, v_tex0Coord);"
			+ "texel.r = mix(texel.r, 1, 1 - v_texAlpha);"
			+ "gl_FragColor = vec4(v_color.r * texel.r, v_color.g * texel.g, v_color.b * texel.b, v_color.a);"
			+ "}";

	// @formatter:on
	/**
	 * Renders a solid color with an atlas region multipled to it (works only with grayscale [uses r value] textures
	 * with no alpha) [uses u_texAlpha].
	 */
	private static ShaderProgram getColorGTextureMultipliedShader()
	{
		if (colorGTextureMultipliedShader == null)
		{
			colorGTextureMultipliedShader = load(
					colorGTextureMultipliedShaderVert,
					colorGTextureMultipliedShaderFrag);
		}
		return colorGTextureMultipliedShader;
	}

	private static ShaderProgram colorGTextureMultipliedShader;
	// @formatter:off
	private static String colorGTextureMultipliedShaderVert = "attribute vec4 a_position;"
			+ "attribute vec2 a_texCoord;"
			+ "varying vec2 v_texCoord;"
			+ "attribute float a_alpha;"
			+ "uniform mat4 u_worldView;"
			+ "uniform vec4 u_color;"
			+ "uniform float u_texAlpha = 1;"
			+ "varying float v_texAlpha;"
			+ "uniform float u_alpha;"
			+ "varying vec4 v_color;"
			+ "varying vec2 v_tex0Coord;"
			+ "void main() {"
			+ "v_color = vec4(u_color.rgb, u_alpha * a_alpha);"
			+ "v_texCoord = a_texCoord;"
			+ "v_texAlpha = u_texAlpha;"
			+ "gl_Position = u_worldView * a_position;"
			+ "v_tex0Coord = a_texCoord;"
			+ "}";
	private static String colorGTextureMultipliedShaderFrag = ANDROID_FIX
			+ "varying vec4 v_color;"
			+ "varying vec2 v_tex0Coord;"
			+ "varying float v_texAlpha;"
			+ "uniform sampler2D u_texture0;"
			+ "void main() {"
			+ "vec4 texel = texture2D(u_texture0, v_tex0Coord);"
			+ "texel.r = mix(texel.r, 1, 1 - v_texAlpha);"
			+ "gl_FragColor = vec4(v_color.r * texel.r, v_color.g * texel.r, v_color.b * texel.r, v_color.a);"
			+ "}";

	// @formatter:on

	/**
	 * Makes linear or radial gradient based on the mesh's UV y positions (includes transparency) and AA. <br>
	 * Requires vec4 color uniform property [u_color] and [u_color1].
	 */
	private static ShaderProgram getGradientShader()
	{
		if (gradientShader == null)
		{
			gradientShader = load(gradientShaderVert, gradientShaderFrag);
		}
		return gradientShader;
	}

	private static ShaderProgram gradientShader;
	// @formatter:off
	private static String gradientShaderVert = VERT_ATTRIBUTE_UNIFORM
			+ "varying vec4 v_color;"
			+ "uniform vec4 " + UNIFORM_COLOR1 + ";"
			+ "void main()"
			+ "{"
			+ "v_color = mix(" + ATTR_COLOR + ", " + UNIFORM_COLOR1 + ", clamp("+ ATTR_TEXCOORD + ".y, 0.0, 1.0));"
//			+ "v_color.a = " + LttlShaders.ATTR_ALPHA + ";"
			+ "gl_Position =  u_worldView * a_position;"
			+ "}";
	private static String gradientShaderFrag = simpleColorShaderFrag;

	// @formatter:on

	/**
	 * Blends colors based on each vertex color which was defined as a vertex attribute. <br>
	 * Requires vec4 color attribute [a_color].
	 */
	private static ShaderProgram getVertexColorShader()
	{
		if (vertexColorShader == null)
		{
			vertexColorShader = load(vertexColorShaderVert,
					vertexColorShaderFrag);
		}
		return vertexColorShader;
	}

	private static ShaderProgram vertexColorShader;
	// @formatter:off
	private static String vertexColorShaderVert = "attribute vec4 a_position;"
			+ "attribute vec4 a_color;"
			+ "attribute float a_alpha;"
			+ "uniform mat4 u_worldView;"
			+ "uniform vec4 u_color;"
			+ "uniform float u_alpha;"
			+ "varying vec4 v_color;"
			+ "void main()"
			+ "{"
			// this may be a better way to do it, not tested, cause it needs to take into account vertect colors
			// + "v_color = vec4(a_color.r * u_color.r, a_color.g * u_color.g, a_color.b * u_color.b, a_color.a * u_alpha * a_alpha);"
			+ "v_color = u_color;"
			+ "v_color.a *= u_alpha * a_alpha;"
			+ "gl_Position =  u_worldView * a_position;"
			+ "}";
	private static String vertexColorShaderFrag = ANDROID_FIX
			+ "varying vec4 v_color;"
			+ "void main()"
			+ "{"
			+ "gl_FragColor = v_color;"
			+ "}";

	// @formatter:on

	/**
	 * Cycles through colors based on a cycling factor (frankly, I am not sure how it works). <br>
	 * Requires texCoord attribute [a_texCoord] and cycling factor (-1 to 1) [u_cycleFactor].
	 */
	private static ShaderProgram getCycleColorFadeShader()
	{
		if (cycleColorFadeShader == null)
		{
			cycleColorFadeShader = load(cycleColorFadeShaderVert,
					cycleColorFadeShaderFrag);
		}
		return cycleColorFadeShader;
	}

	private static ShaderProgram cycleColorFadeShader;
	// @formatter:off
	private static String cycleColorFadeShaderVert = "attribute vec4 a_position;"
			+ "attribute vec2 a_texCoord;"
			+ "attribute float a_alpha;"
			+ "uniform mat4 u_worldView;"
			+ "uniform float u_cycleFactor;"
			+ "uniform vec4 u_color;"
			+ "uniform float u_alpha;"
			+ "varying vec4 v_color;"
			+ "void main()"
			+ "{"
			+ "v_color = vec4(u_color.r * a_texCoord, u_color.g * (0.5+0.5*u_cycleFactor),u_color.b * 1.0, u_alpha * a_alpha);"
			+ "gl_Position =  u_worldView * a_position;"
			+ "}";
	private static String cycleColorFadeShaderFrag = ANDROID_FIX
			+ "varying vec4 v_color;"
			+ "void main()"
			+ "{"
			+ "gl_FragColor = v_color;"
			+ "}";

	// @formatter:on

	/**
	 * Renders atlasRegion0.
	 */
	private static ShaderProgram getTextureAdditiveColorShader()
	{
		if (textureAdditiveColorShader == null)
		{
			textureAdditiveColorShader = load(textureAdditiveColorShaderVert,
					textureAdditiveColorShaderFrag);
		}
		return textureAdditiveColorShader;
	}

	private static ShaderProgram textureAdditiveColorShader;
	// @formatter:off
	private static String textureAdditiveColorShaderVert = "attribute vec4 a_position;"
			+ "attribute vec2 a_texCoord;"
			+ "attribute float a_alpha;"
			+ "uniform mat4 u_worldView;"
			+ "uniform vec4 u_color;"
			+ "uniform float u_alpha;"
			+ "varying vec4 v_color;"
			+ "varying vec2 v_tex0Coord;"
			+ "void main() {"
			+ "v_color = u_color * u_color.a;" //premultiply this color by alpha, reducing it's amount to be added
			+ "v_color.a = u_alpha;" //override the color's alpha with the alpha of the renderer
			+ "gl_Position = u_worldView * a_position;"
			+ "v_tex0Coord = a_texCoord;"
			+ "}";
	private static String textureAdditiveColorShaderFrag = ANDROID_FIX
			+ "varying vec4 v_color;"
			+ "uniform sampler2D u_texture0;"
			+ "varying vec2 v_tex0Coord;"
			+ "void main() {"
			+ "gl_FragColor = v_color + texture2D(u_texture0, v_tex0Coord);"
			+ "}";

	// @formatter:on

	/**
	 * Renders atlasRegion0.
	 */
	private static ShaderProgram getTextureShader()
	{
		if (textureShader == null)
		{
			textureShader = load(textureShaderVert, textureShaderFrag);
		}
		return textureShader;
	}

	private static ShaderProgram textureShader;
	// @formatter:off
	private static String textureShaderVert = VERT_ATTRIBUTE_UNIFORM
			+ VERT_DEFAULT_VARYING
			+ VERT_DEFAULT_MAIN_START
			+ "}";
	private static String textureShaderFrag = ANDROID_FIX
			+ "varying vec4 v_color;"
			+ "uniform sampler2D u_texture0;"
			+ "varying vec2 v_tex0Coord;"
			+ "void main() {"
			+ "gl_FragColor = v_color * texture2D(u_texture0, v_tex0Coord);"
			+ "}";

	// @formatter:on

	private static ShaderProgram getTextureTransformUVShader()
	{
		if (textureTransformUVShader == null)
		{
			textureTransformUVShader = load(textureTransformUVShaderVert,
					textureTransformUVShaderFrag);
		}
		return textureTransformUVShader;
	}

	private static ShaderProgram textureTransformUVShader;
	// @formatter:off
	private static String textureTransformUVShaderVert = VERT_ATTRIBUTE_UNIFORM
			+ VERT_DEFAULT_VARYING
			+ "uniform vec2 u_uvScale;"
			+ "uniform vec2 u_uvOffset;"
			+ "void main()"
			+ "{"
			+ "v_color = " + LttlShaders.ATTR_COLOR + ";"
			+ "v_color.a = " + LttlShaders.ATTR_ALPHA + ";"
			+ "gl_Position = " + UNIFORM_WORLD_MATRIX + " * " + LttlShaders.ATTR_POS + ";"
			+ "v_tex0Coord = " + LttlShaders.ATTR_TEXCOORD + " * u_uvScale + u_uvOffset;"
			+ "}";
	private static String textureTransformUVShaderFrag = textureShaderFrag;

	// @formatter:on

	private static ShaderProgram getTextureAdditiveColorTransformUV()
	{
		if (textureAdditiveColorTransformUV == null)
		{
			textureAdditiveColorTransformUV = load(
					textureAdditiveColorTransformUVVert,
					textureAdditiveColorTransformUVFrag);
		}
		return textureAdditiveColorTransformUV;
	}

	private static ShaderProgram textureAdditiveColorTransformUV;
	// @formatter:off
	private static String textureAdditiveColorTransformUVVert = "attribute vec4 a_position;"
			+ "attribute vec2 a_texCoord;"
			+ "attribute float a_alpha;"
			+ "uniform mat4 u_worldView;"
			+ "uniform vec4 u_color;"
			+ "uniform float u_alpha;"
			+ "uniform vec2 u_uvOffset;"
			+ "uniform vec2 u_uvScale;"
			+ "varying vec4 v_color;"
			+ "varying vec2 v_tex0Coord;"
			+ "void main() {"
			+ "v_color = u_color * u_color.a;" //premultiply this color by alpha, reducing it's amount to be added
			+ "v_color.a = u_alpha;" //override the color's alpha with the alpha of the renderer
			+ "gl_Position = u_worldView * a_position;"
			+ "v_tex0Coord = (v_tex0Coord * u_uvScale) + u_uvOffset;"
			+ "}";
	private static String textureAdditiveColorTransformUVFrag = ANDROID_FIX
			+ "varying vec4 v_color;"
			+ "uniform sampler2D u_texture0;"
			+ "varying vec2 v_tex0Coord;"
			+ "void main() {"
			+ "gl_FragColor = v_color + texture2D(u_texture0, v_tex0Coord);"
			+ "}";

	// @formatter:on

	/**
	 * Renders atlasRegion0 and atlasRegion1 on the same texture via multiply.
	 */
	private static ShaderProgram getTexturesShader()
	{
		if (texturesShader == null)
		{
			texturesShader = load(texturesShaderVert, texturesShaderFrag);
		}
		return texturesShader;
	}

	private static ShaderProgram texturesShader;
	// @formatter:off
	private static String texturesShaderVert = "attribute vec4 a_position;"
			+ "attribute vec2 a_texCoord;"
			+ "attribute float a_alpha;"
			+ "uniform mat4 u_worldView;"
			+ "uniform vec4 u_color;"
			+ "uniform float u_alpha;"
			+ "varying vec4 v_color;"
			+ "varying vec2 v_tex0Coord;"
			+ "uniform vec2 u_tex1uv;"
			+ "uniform vec2 u_tex1uv2;"
			+ "varying vec2 v_tex1Coord;"
			+ "void main() {"
			+ "v_color = u_color;"
			+ "v_color.a = 1;"
			+ "gl_Position = u_worldView * a_position;"
			+ "v_tex0Coord = a_texCoord;"
			+ "v_tex1Coord = vec2(((u_tex1uv2.x-u_tex1uv.x) * a_texCoord.x) + u_tex1uv.x,  ((u_tex1uv2.y-u_tex1uv.y) * a_texCoord.y) + u_tex1uv.y);"
			+ "}";
	private static String texturesShaderFrag = ANDROID_FIX
			+ "varying vec4 v_color;"
			+ "uniform sampler2D u_texture0;"
			+ "varying vec2 v_tex0Coord;"
			+ "varying vec2 v_tex1Coord;"
			+ "void main() {"
			+ "gl_FragColor = v_color * texture2D(u_texture0, v_tex0Coord) * texture2D(u_texture0, v_tex1Coord);"
			+ "}";

	// @formatter:on

	/**
	 * Renders atlasRegion0 and atlasRegion1 on different textures via multiply.
	 */
	private static ShaderProgram getTexturesDifShader()
	{
		if (texturesDifShader == null)
		{
			texturesDifShader = load(texturesDifShaderVert,
					texturesDifShaderFrag);
		}
		return texturesDifShader;
	}

	private static ShaderProgram texturesDifShader;
	// @formatter:off
	private static String texturesDifShaderVert = "attribute vec4 a_position;"
			+ "attribute vec2 a_texCoord;"
			+ "attribute float a_alpha;"
			+ "uniform mat4 u_worldView;"
			+ "uniform vec4 u_color;"
			+ "uniform float u_alpha;"
			+ "varying vec4 v_color;"
			+ "varying vec2 v_tex0Coord;"
			+ "uniform vec2 u_tex1uv;"
			+ "uniform vec2 u_tex1uv2;"
			+ "varying vec2 v_tex1Coord;"
			+ "void main() {"
			+ "v_color = u_color;"
			+ "v_color.a *= u_alpha * a_alpha;"
			+ "gl_Position = u_worldView * a_position;"
			+ "v_tex0Coord = a_texCoord;"
			+ "v_tex1Coord = vec2(((u_tex1uv2.x-u_tex1uv.x) * a_texCoord.x) + u_tex1uv.x,  ((u_tex1uv2.y-u_tex1uv.y) * a_texCoord.y) + u_tex1uv.y);"
			+ "}";
	private static String texturesDifShaderFrag = ANDROID_FIX
			+ "varying vec4 v_color;"
			+ "uniform sampler2D u_texture0;"
			+ "uniform sampler2D u_texture1;"
			+ "varying vec2 v_tex0Coord;"
			+ "varying vec2 v_tex1Coord;"
			+ "void main() {"
			+ "gl_FragColor = v_color * texture2D(u_texture0, v_tex0Coord) * texture2D(u_texture1, v_tex1Coord);"
			+ "}";

	// @formatter:on

	/**
	 * Renders atlasRegion0 and uses itself as a grayscale mask (r value for alpha).
	 */
	private static ShaderProgram getTextureSelfGMaskShader()
	{
		if (textureSelfGMaskShader == null)
		{
			textureSelfGMaskShader = load(textureSelfGMaskShaderVert,
					textureSelfGMaskShaderFrag);
		}
		return textureSelfGMaskShader;
	}

	private static ShaderProgram textureSelfGMaskShader;
	// @formatter:off
	private static String textureSelfGMaskShaderVert = "attribute vec4 a_position;"
			+ "attribute vec2 a_texCoord;"
			+ "attribute float a_alpha;"
			+ "uniform mat4 u_worldView;"
			+ "uniform vec4 u_color;"
			+ "uniform float u_alpha;"
			+ "varying vec4 v_color;"
			+ "varying vec2 v_tex0Coord;"
			+ "void main() {"
			+ "v_color = u_color;"
			+ "v_color.a = u_alpha * a_alpha;"
			+ "gl_Position = u_worldView * a_position;"
			+ "v_tex0Coord = a_texCoord;"
			+ "}";
	private static String textureSelfGMaskShaderFrag = ANDROID_FIX
			+ "varying vec4 v_color;"
			+ "uniform sampler2D u_texture0;"
			+ "varying vec2 v_tex0Coord;"
			+ "void main() {"
			+ "vec4 texel0 = texture2D(u_texture0, v_tex0Coord);"
			+ "gl_FragColor = vec4(v_color.rgb, v_color.a * texel0.r * texel0.a);"
			+ "}";

	// @formatter:on

	/**
	 * Renders atlasRegion0 and uses itself and atlasRegion1 as a grayscale mask on the same texture.
	 */
	private static ShaderProgram getTextureSelfOtherGMaskShader()
	{
		if (textureSelfOtherGMaskShader == null)
		{
			textureSelfOtherGMaskShader = load(textureSelfOtherGMaskShaderVert,
					textureSelfOtherGMaskShaderFrag);
		}
		return textureSelfOtherGMaskShader;
	}

	private static ShaderProgram textureSelfOtherGMaskShader;
	// @formatter:off
	private static String textureSelfOtherGMaskShaderVert = "attribute vec4 a_position;"
			+ "attribute vec2 a_texCoord;"
			+ "attribute float a_alpha;"
			+ "uniform mat4 u_worldView;"
			+ "uniform vec4 u_color;"
			+ "uniform float u_alpha;"
			+ "varying vec4 v_color;"
			+ "varying vec2 v_tex0Coord;"
			+ "uniform vec2 u_tex1uv;"
			+ "uniform vec2 u_tex1uv2;"
			+ "varying vec2 v_tex1Coord;"
			+ "void main() {"
			+ "v_color = u_color;"
			+ "v_color.a = u_alpha * a_alpha;"
			+ "gl_Position = u_worldView * a_position;"
			+ "v_tex0Coord = a_texCoord;"
			+ "v_tex1Coord = vec2(((u_tex1uv2.x-u_tex1uv.x) * a_texCoord.x) + u_tex1uv.x,  ((u_tex1uv2.y-u_tex1uv.y) * a_texCoord.y) + u_tex1uv.y);"
			+ "}";
	private static String textureSelfOtherGMaskShaderFrag = ANDROID_FIX
			+ "varying vec4 v_color;"
			+ "uniform sampler2D u_texture0;"
			+ "varying vec2 v_tex0Coord;"
			+ "varying vec2 v_tex1Coord;"
			+ "void main() {"
			+ "vec4 texel0 = texture2D(u_texture0, v_tex0Coord);"
			+ "vec4 texel1 = texture2D(u_texture0, v_tex1Coord);"
			+ "gl_FragColor = vec4(v_color.rgb, v_color.a * texel0.r * texel0.a * texel1.r * texel1.a);"
			+ "}";

	// @formatter:on

	/**
	 * Renders atlasRegion0 and uses itself and atlasRegion1 as a grayscale mask on the different textures.
	 */
	private static ShaderProgram getTextureSelfOtherGMaskDifShader()
	{
		if (textureSelfOtherGMaskDifShader == null)
		{
			textureSelfOtherGMaskDifShader = load(
					textureSelfOtherGMaskDifShaderVert,
					textureSelfOtherGMaskDifShaderFrag);
		}
		return textureSelfOtherGMaskDifShader;
	}

	private static ShaderProgram textureSelfOtherGMaskDifShader;
	// @formatter:off
	private static String textureSelfOtherGMaskDifShaderVert = "attribute vec4 a_position;"
			+ "attribute vec2 a_texCoord;"
			+ "attribute float a_alpha;"
			+ "uniform mat4 u_worldView;"
			+ "uniform vec4 u_color;"
			+ "uniform float u_alpha;"
			+ "varying vec4 v_color;"
			+ "varying vec2 v_tex0Coord;"
			+ "uniform vec2 u_tex1uv;"
			+ "uniform vec2 u_tex1uv2;"
			+ "varying vec2 v_tex1Coord;"
			+ "void main() {"
			+ "v_color = u_color;"
			+ "v_color.a = u_alpha * a_alpha;"
			+ "gl_Position = u_worldView * a_position;"
			+ "v_tex0Coord = a_texCoord;"
			+ "v_tex1Coord = vec2(((u_tex1uv2.x-u_tex1uv.x) * a_texCoord.x) + u_tex1uv.x,  ((u_tex1uv2.y-u_tex1uv.y) * a_texCoord.y) + u_tex1uv.y);"
			+ "}";
	private static String textureSelfOtherGMaskDifShaderFrag = ANDROID_FIX
			+ "varying vec4 v_color;"
			+ "uniform sampler2D u_texture0;"
			+ "uniform sampler2D u_texture1;"
			+ "varying vec2 v_tex0Coord;"
			+ "varying vec2 v_tex1Coord;"
			+ "void main() {"
			+ "vec4 texel0 = texture2D(u_texture0, v_tex0Coord);"
			+ "vec4 texel1 = texture2D(u_texture1, v_tex1Coord);"
			+ "gl_FragColor = vec4(v_color.rgb, v_color.a * texel0.r * texel0.a * texel1.r * texel1.a);"
			+ "}";

	// @formatter:on

	/**
	 * Renders atlasRegion0 and uses atlasRegion1 as a grayscale mask on the same texture.
	 */
	private static ShaderProgram getTextureGMaskShader()
	{
		if (textureGMaskShader == null)
		{
			textureGMaskShader = load(textureGMaskShaderVert,
					textureGMaskShaderFrag);
		}
		return textureGMaskShader;
	}

	private static ShaderProgram textureGMaskShader;
	// @formatter:off
	private static String textureGMaskShaderVert = "attribute vec4 a_position;"
			+ "attribute vec2 a_texCoord;"
			+ "attribute float a_alpha;"
			+ "uniform mat4 u_worldView;"
			+ "uniform vec4 u_color;"
			+ "uniform float u_alpha;"
			+ "varying vec4 v_color;"
			+ "varying vec2 v_tex0Coord;"
			+ "uniform vec2 u_tex1uv;"
			+ "uniform vec2 u_tex1uv2;"
			+ "varying vec2 v_tex1Coord;"
			+ "void main() {"
			+ "v_color = u_color;"
			+ "v_color.a = u_alpha * a_alpha;"
			+ "gl_Position = u_worldView * a_position;"
			+ "v_tex0Coord = a_texCoord;"
			+ "v_tex1Coord = vec2(((u_tex1uv2.x-u_tex1uv.x) * a_texCoord.x) + u_tex1uv.x,  ((u_tex1uv2.y-u_tex1uv.y) * a_texCoord.y) + u_tex1uv.y);"
			+ "}";
	private static String textureGMaskShaderFrag = ANDROID_FIX
			+ "varying vec4 v_color;"
			+ "uniform sampler2D u_texture0;"
			+ "varying vec2 v_tex0Coord;"
			+ "varying vec2 v_tex1Coord;"
			+ "void main() {"
			+ "vec4 texel0 = texture2D(u_texture0, v_tex0Coord);"
			+ "vec4 texel1 = texture2D(u_texture0, v_tex1Coord);"
			+ "gl_FragColor = vec4(v_color.rgb * texel0.rgb, v_color.a * texel0.a * texel1.r * texel1.a);"
			+ "}";

	// @formatter:on

	/**
	 * Renders atlasRegion0 and uses atlasRegion1 as a grayscale mask on different textures.
	 */
	private static ShaderProgram getTextureGMaskDifShader()
	{
		if (textureGMaskDifShader == null)
		{
			textureGMaskDifShader = load(textureGMaskDifShaderVert,
					textureGMaskDifShaderFrag);
		}
		return textureGMaskDifShader;
	}

	private static ShaderProgram textureGMaskDifShader;
	// @formatter:off
	private static String textureGMaskDifShaderVert = "attribute vec4 a_position;"
			+ "attribute vec2 a_texCoord;"
			+ "attribute float a_alpha;"
			+ "uniform mat4 u_worldView;"
			+ "uniform vec4 u_color;"
			+ "uniform float u_alpha;"
			+ "varying vec4 v_color;"
			+ "varying vec2 v_tex0Coord;"
			+ "uniform vec2 u_tex1uv;"
			+ "uniform vec2 u_tex1uv2;"
			+ "varying vec2 v_tex1Coord;"
			+ "void main() {"
			+ "v_color = u_color;"
			+ "v_color.a = u_alpha * a_alpha;"
			+ "gl_Position = u_worldView * a_position;"
			+ "v_tex0Coord = a_texCoord;"
			+ "v_tex1Coord = vec2(((u_tex1uv2.x-u_tex1uv.x) * a_texCoord.x) + u_tex1uv.x,  ((u_tex1uv2.y-u_tex1uv.y) * a_texCoord.y) + u_tex1uv.y);"
			+ "}";
	private static String textureGMaskDifShaderFrag = ANDROID_FIX
			+ "varying vec4 v_color;"
			+ "uniform sampler2D u_texture0;"
			+ "uniform sampler2D u_texture1;"
			+ "varying vec2 v_tex0Coord;"
			+ "varying vec2 v_tex1Coord;"
			+ "void main() {"
			+ "vec4 texel0 = texture2D(u_texture0, v_tex0Coord);"
			+ "vec4 texel1 = texture2D(u_texture1, v_tex1Coord);"
			+ "gl_FragColor = vec4(v_color.rgb * texel0.rgb, v_color.a * texel0.a * texel1.r * texel1.a);"
			+ "}";

	// @formatter:on

	/**** POST PROCESSING ****/
	private static ShaderProgram getGaussianBlurHShader()
	{
		if (gaussianBlurHShader == null)
		{
			gaussianBlurHShader = load(gaussianBlurHShaderVert,
					gaussianBlurShaderFrag);
		}
		return gaussianBlurHShader;
	}

	private static ShaderProgram gaussianBlurHShader;
	// @formatter:off
	private static String gaussianBlurHShaderVert = "attribute vec4 a_position;"
			+ "attribute vec2 a_texCoord;"
			+ "varying vec2 v_texCoord;"
			+ "uniform mat4 u_worldView;"
			+ "varying vec2 v_blurTexCoords[14];"
			+ "void main()"
			+ "{"
			+ "v_texCoord = a_texCoord;"
			+ "gl_Position =  u_worldView * a_position;"
			+ "v_blurTexCoords[0] = v_texCoord + vec2(-0.028, 0.0);"
			+ "v_blurTexCoords[1] = v_texCoord + vec2(-0.024, 0.0);"
			+ "v_blurTexCoords[2] = v_texCoord + vec2(-0.020, 0.0);"
			+ "v_blurTexCoords[3] = v_texCoord + vec2(-0.016, 0.0);"
			+ "v_blurTexCoords[4] = v_texCoord + vec2(-0.012, 0.0);"
			+ "v_blurTexCoords[5] = v_texCoord + vec2(-0.008, 0.0);"
			+ "v_blurTexCoords[6] = v_texCoord + vec2(-0.004, 0.0);"
			+ "v_blurTexCoords[7] = v_texCoord + vec2( 0.004, 0.0);"
			+ "v_blurTexCoords[8] = v_texCoord + vec2( 0.008, 0.0);"
			+ "v_blurTexCoords[9] = v_texCoord + vec2( 0.012, 0.0);"
			+ "v_blurTexCoords[10] = v_texCoord + vec2( 0.016, 0.0);"
			+ "v_blurTexCoords[11] = v_texCoord + vec2( 0.020, 0.0);"
			+ "v_blurTexCoords[12] = v_texCoord + vec2( 0.024, 0.0);"
			+ "v_blurTexCoords[13] = v_texCoord + vec2( 0.028, 0.0);"
			+ "}";

	// @formatter:on

	private static ShaderProgram getGaussianBlurVShader()
	{
		if (gaussianBlurVShader == null)
		{
			gaussianBlurVShader = load(gaussianBlurVShaderVert,
					gaussianBlurShaderFrag);
		}
		return gaussianBlurVShader;
	}

	private static ShaderProgram gaussianBlurVShader;
	// @formatter:off
	private static String gaussianBlurVShaderVert = "attribute vec4 a_position;"
			+ "attribute vec2 a_texCoord;"
			+ "varying vec2 v_texCoord;"
			+ "uniform mat4 u_worldView;"
			+ "varying vec2 v_blurTexCoords[14];"
			+ "void main()"
			+ "{"
			+ "v_texCoord = a_texCoord;"
			+ "gl_Position =  u_worldView * a_position;"
			+ "v_blurTexCoords[0] = v_texCoord + vec2(0.0, -0.028);"
			+ "v_blurTexCoords[1] = v_texCoord + vec2(0.0, -0.024);"
			+ "v_blurTexCoords[2] = v_texCoord + vec2(0.0, -0.020);"
			+ "v_blurTexCoords[3] = v_texCoord + vec2(0.0, -0.016);"
			+ "v_blurTexCoords[4] = v_texCoord + vec2(0.0, -0.012);"
			+ "v_blurTexCoords[5] = v_texCoord + vec2(0.0, -0.008);"
			+ "v_blurTexCoords[6] = v_texCoord + vec2(0.0, -0.004);"
			+ "v_blurTexCoords[7] = v_texCoord + vec2(0.0,  0.004);"
			+ "v_blurTexCoords[8] = v_texCoord + vec2(0.0,  0.008);"
			+ "v_blurTexCoords[9] = v_texCoord + vec2(0.0,  0.012);"
			+ "v_blurTexCoords[10] = v_texCoord + vec2(0.0,  0.016);"
			+ "v_blurTexCoords[11] = v_texCoord + vec2(0.0,  0.020);"
			+ "v_blurTexCoords[12] = v_texCoord + vec2(0.0,  0.024);"
			+ "v_blurTexCoords[13] = v_texCoord + vec2(0.0,  0.028);"
			+ "}";

	private static String gaussianBlurShaderFrag = ANDROID_FIX
			+ "varying vec2 v_texCoord;"
			+ "uniform sampler2D u_texture0;"
			+ "varying vec2 v_blurTexCoords[14];"
			+ "void main()"
			+ "{"
			+ "gl_FragColor = vec4(0.0);"
			+ "gl_FragColor += texture2D(u_texture0, v_blurTexCoords[0])*0.0044299121055113265;"
			+ "gl_FragColor += texture2D(u_texture0, v_blurTexCoords[1])*0.00895781211794;"
			+ "gl_FragColor += texture2D(u_texture0, v_blurTexCoords[2])*0.0215963866053;"
			+ "gl_FragColor += texture2D(u_texture0, v_blurTexCoords[3])*0.0443683338718;"
			+ "gl_FragColor += texture2D(u_texture0, v_blurTexCoords[4])*0.0776744219933;"
			+ "gl_FragColor += texture2D(u_texture0, v_blurTexCoords[5])*0.115876621105;"
			+ "gl_FragColor += texture2D(u_texture0, v_blurTexCoords[6])*0.147308056121;"
			+ "gl_FragColor += texture2D(u_texture0, v_texCoord)*0.159576912161;"
			+ "gl_FragColor += texture2D(u_texture0, v_blurTexCoords[7])*0.147308056121;"
			+ "gl_FragColor += texture2D(u_texture0, v_blurTexCoords[8])*0.115876621105;"
			+ "gl_FragColor += texture2D(u_texture0, v_blurTexCoords[9])*0.0776744219933;"
			+ "gl_FragColor += texture2D(u_texture0, v_blurTexCoords[10])*0.0443683338718;"
			+ "gl_FragColor += texture2D(u_texture0, v_blurTexCoords[11])*0.0215963866053;"
			+ "gl_FragColor += texture2D(u_texture0, v_blurTexCoords[12])*0.00895781211794;"
			+ "gl_FragColor += texture2D(u_texture0, v_blurTexCoords[13])*0.0044299121055113265;"
			+ "}";
	// @formatter:off
	
	

	// ** OBSOLETE SHADERS **//
	
	

	// @formatter:on
	// /**
	// * Renders a texture using attribute [a_texCoord] for texCoords.
	// */
	// private static ShaderProgram getTextureShader()
	// {
	// if (textureShader == null)
	// {
	// textureShader = load(textureShaderVert, textureShaderFrag);
	// }
	// return textureShader;
	// }
	//
	// private static ShaderProgram textureShader;
//	// @formatter:off
//	private static String textureShaderVert = "attribute vec4 a_position;"
//			+ "attribute vec2 a_texCoord;"
//			+ "varying vec2 v_texCoord;"
//			+ "attribute float a_alpha;"
//			+ "uniform mat4 u_worldView;"
//			+ "uniform vec4 u_color;"
//			+ "uniform float u_alpha;"
//			+ "varying vec4 v_color;"
//			+ "void main() {"
//			+ "v_color = vec4(u_color.rgb, u_alpha * a_alpha);"
//			+ "v_texCoord = a_texCoord;"
//			+ "gl_Position = u_worldView * a_position;"
//			+ "}";
//	private static String textureShaderFrag = androidFix
//			+ "varying vec4 v_color;"
//			+ "varying vec2 v_texCoord;"
//			+ "uniform sampler2D u_texture0;"
//			+ "void main() {"
//			+ "gl_FragColor = v_color * texture2D(u_texture0, v_texCoord);"
//			+ "}";
//
//	// @formatter:on
	//
	// /**
	// * Renders a texture using attribute [a_texCoord] for texCoords.
	// */
	// private static ShaderProgram getMultiTextureShader()
	// {
	// if (multiTextureShader == null)
	// {
	// multiTextureShader = load(multiTextureShaderVert,
	// multiTextureShaderFrag);
	// }
	// return multiTextureShader;
	// }
	//
	// private static ShaderProgram multiTextureShader;
//	// @formatter:off
//	private static String multiTextureShaderVert = "attribute vec4 a_position;"
//			+ "attribute vec2 a_texCoord;"
//			+ "varying vec2 v_texCoord;"
//			+ "attribute float a_alpha;"
//			+ "uniform mat4 u_worldView;"
//			+ "uniform vec4 u_color;"
//			+ "uniform float u_alpha;"
//			+ "varying float v_alpha;"
//			+ "varying vec4 v_color;"
//			+ "void main() {"
//			+ "v_color = vec4(u_color.rgb, u_alpha * a_alpha);"
//			+ "v_texCoord = a_texCoord;"
//			+ "gl_Position = u_worldView * a_position;"
//			+ "}";
//	private static String multiTextureShaderFrag = androidFix
//			+ "varying vec4 v_color;"
//			+ "varying vec2 v_texCoord;"
//			+ "uniform sampler2D u_texture0;"
//			+ "uniform sampler2D u_texture1;"
//			+ "void main() {"
//			+ "gl_FragColor = v_color * texture2D(u_texture0, v_texCoord) * texture2D(u_texture1, v_texCoord);"
//			+ "}";
//	// @formatter:on
	// /**
	// * Renders a solid color with a texture multipled to it (works only with grayscale [uses r value] textures with no
	// * alpha) [uses u_texAlpha].
	// */
	// private static ShaderProgram getColorTextureMultipliedShader()
	// {
	// if (colorTextureMultipliedShader == null)
	// {
	// colorTextureMultipliedShader = load(
	// colorTextureMultipliedShaderVert,
	// colorTextureMultipliedShaderFrag);
	// }
	// return colorTextureMultipliedShader;
	// }
	//
	// private static ShaderProgram colorTextureMultipliedShader;
//	// @formatter:off
//	private static String colorTextureMultipliedShaderVert = "attribute vec4 a_position;"
//			+ "attribute vec2 a_texCoord;"
//			+ "varying vec2 v_texCoord;"
//			+ "attribute float a_alpha;"
//			+ "uniform mat4 u_worldView;"
//			+ "uniform vec4 u_color;"
//			+ "uniform float u_texAlpha = 1;"
//			+ "varying float v_texAlpha;"
//			+ "uniform float u_alpha;"
//			+ "varying vec4 v_color;"
//			+ "void main() {"
//			+ "v_color = vec4(u_color.rgb, u_alpha * a_alpha);"
//			+ "v_texCoord = a_texCoord;"
//			+ "v_texAlpha = u_texAlpha;"
//			+ "gl_Position = u_worldView * a_position;"
//			+ "}";
//	private static String colorTextureMultipliedShaderFrag = androidFix
//			+ "varying vec4 v_color;"
//			+ "varying vec2 v_texCoord;"
//			+ "varying float v_texAlpha;"
//			+ "uniform sampler2D u_texture0;"
//			+ "void main() {"
//			+ "vec4 texel = texture2D(u_texture0, v_texCoord);"
//			+ "texel.r = mix(texel.r, 1, 1 - v_texAlpha);"
//			+ "gl_FragColor = vec4(v_color.r * texel.r, v_color.g * texel.r, v_color.b * texel.r, v_color.a);"
//			+ "}";

}
