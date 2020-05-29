package com.lttlgames.editor;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.utils.Align;
import com.lttlgames.components.LttlGuiContainerComponent;
import com.lttlgames.editor.annotations.GuiButton;
import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.GuiCanNull;
import com.lttlgames.editor.annotations.GuiGroup;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.GuiReadOnly;
import com.lttlgames.editor.annotations.GuiShow;
import com.lttlgames.editor.annotations.GuiTextArea;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.graphics.LttlMeshFactory;
import com.lttlgames.helpers.LttlMath;

//17
/**
 * A font at a specific size is automatically cached as long as at least one component is using.
 */
@Persist(-909)
public class LttlFontGenerator extends LttlMeshGenerator
{
	final public static int SIZE_MIN = 12;
	/**
	 * limit is 120 because usually breaks because of textures are too big, more than reasonable
	 */
	final public static int SIZE_MAX = 120;

	@GuiCallback(
	{ "refreshFont", "updateMesh" })
	@Persist(90901)
	@GuiCanNull
	public String fontName = LttlFontManager.defaultFont;

	/**
	 * This is the LttlFont object that is given from the LttlFontManager.
	 */
	private LttlFont font;

	/**
	 * For line breaks, use "\n".
	 */
	@GuiCallback("updateMesh")
	@Persist(90903)
	@GuiCanNull
	@GuiTextArea
	public String text;

	/**
	 * This is the fontSize constant that is scaled based on screen resolution and other factors in Dynamic Settings.
	 */
	@GuiCallback(
	{ "refreshFont", "updateMesh" })
	@Persist(909013)
	@GuiMin(1)
	public int fontRenderSizeConstant = 12;

	/**
	 * the {@link #fontRenderSizeConstant} of the current {@link LttlFont} used for rendering based on Dynamic Settings
	 * and resolution. Call {@link #refreshFont()} to update this if screen, camera, or scale changed.
	 */
	@GuiShow
	@GuiReadOnly
	public int actualFontRenderSize = -1;

	/**
	 * Use this to scale the overall size.
	 */
	@GuiCallback("updateMesh")
	@Persist(90904)
	public float fontScale = 1;

	/**
	 * the {@link #actualFontRenderSize} will be modified to maintain clarity based on camera zoom
	 */
	@Persist(909015)
	@GuiGroup("Dynamic Settings")
	@GuiCallback(
	{ "refreshFont", "updateMesh" })
	public boolean cameraZoomDependent = true;
	/**
	 * the {@link #actualFontRenderSize} will be modified to maintain clarity based on transform's world scale (x).<br>
	 * If using a {@link LttlGuiContainerComponent} this won't be necessary if {@link #cameraZoomDependent} is enabled.
	 */
	@Persist(909016)
	@GuiGroup("Dynamic Settings")
	@GuiCallback(
	{ "refreshFont", "updateMesh" })
	public boolean scaleDependent = false;
	/**
	 * probably only worth enabling this for actual production since resizing happens a lot while in editor and making
	 * font textures is slow
	 */
	@Persist(909017)
	@GuiGroup("Dynamic Settings")
	public boolean autoRefreshFontOnResize = false;

	@GuiCallback("updateMesh")
	@Persist(90905)
	@GuiGroup("Setttings")
	public float spaceWidth = 10;

	@GuiCallback("updateMesh")
	@Persist(90906)
	@GuiGroup("Setttings")
	public float lineHeight = 60;

	@GuiCallback("updateMesh")
	@Persist(90907)
	@GuiGroup("Setttings")
	public float xStretch = 1;

	@GuiCallback("updateMesh")
	@Persist(90908)
	@GuiGroup("Setttings")
	public float yStretch = 1;

	@GuiCallback("updateMesh")
	@Persist(90909)
	@GuiGroup("Setttings")
	public int alignment = Align.left;

	@GuiCallback("updateMesh")
	@Persist(909011)
	@GuiGroup("Setttings")
	public boolean wrap = false;

	@GuiCallback("updateMesh")
	@Persist(909012)
	@GuiMin(0)
	@GuiGroup("Setttings")
	public float width = 100;

	@GuiCallback("updateMesh")
	@Persist(909010)
	@GuiGroup("Setttings")
	public boolean centerMesh = false;

	/**
	 * Not sure how to use this with my setup.
	 */
	@GuiCallback(
	{ "refreshFont", "updateMesh" })
	@Persist(909014)
	public boolean useIntegers = false;

	private float scaleFactor = 1;

	@Override
	public void onResize()
	{
		resize();
	}

	@Override
	public void onEditorResize()
	{
		resize();
	}

	private void resize()
	{
		if (autoRefreshFontOnResize)
		{
			refreshFont();
		}
	}

	@Override
	final public void onEditorStart()
	{
		start();
	}

	@Override
	final public void onStart()
	{
		start();
	}

	private void start()
	{
		// only refresh font on start if it has not been created yet, it may alread by created from an onResize, since
		// that is called before onStart
		if (font == null && fontName != null)
		{
			refreshFont();
		}

		// if text and font exist, update mesh
		if (text != null && font != null)
		{
			updateMesh();
		}
	}

	/**
	 * Sets the font name and then gets/updates the bitmap font for this rendere based on the fontName specified. This
	 * should only be done every so often since it uses a string look up.
	 * 
	 * @param fontName
	 */
	public void refreshFont(String fontName)
	{
		this.fontName = fontName;
		refreshFont();
	}

	/**
	 * Gets/updates the LttlFont based on the fontName and the fontSize that generates the BitmapFont used for this
	 * render. This should only be done every so often since it uses a string look up. actually render.
	 */
	@GuiButton
	public void refreshFont()
	{
		if (font != null)
		{
			font.stopUsing(this);
		}
		font = null;
		scaleFactor = 1;
		if (fontName != null && !fontName.isEmpty())
		{
			actualFontRenderSize = getActualFontRenderSize();
			font = Lttl.game.fontManager.getFont(fontName, actualFontRenderSize, this);

			// compensates the change in font render size of the font being used so the visual size stays about the same
			scaleFactor = 1 / (actualFontRenderSize / 12f);

		}
	}

	private int getActualFontRenderSize()
	{
		if (aaSettings == null) return fontRenderSizeConstant;
		return LttlMath
				.clamp(LttlMath
						.round(fontRenderSizeConstant
								/ Lttl.game.getSettings().getScaleFactor(
										Lttl.game.getSettings().getTargetCamera(),
										aaSettings.cameraZoomDependent, true, false)
								/ (aaSettings.scaleDependent ? t().getWorldScale(true).x
										: 1)),
						SIZE_MIN, SIZE_MAX);
	}

	/**
	 * Sets the text and updates updates mesh if text is different and bounding rect, also taking into consideration any
	 * other changes.
	 * 
	 * @param text
	 */
	public void updateText(String text)
	{
		boolean dif = !text.equals(this.text);
		this.text = text;
		if (dif) updateMesh();
	}

	/**
	 * Updates mesh and bounding rect with text and also taking into consideration any other changes. Also sets the
	 * texture on renderer.
	 */
	@Override
	public void updateMesh()
	{
		if (font == null)
		{
			if (fontName != null)
			{
				refreshFont();
				if (font != null)
				{
					updateMesh();
				}
			}
			return;
		}
		if (text == null) return;

		// always create the bitmap font since it prevents errors
		// tested- does not create memory leak
		BitmapFont bFont = font.generateBitmapFont();

		float actualScale = scaleFactor * fontScale;

		bFont.getData().setScale(actualScale * xStretch, actualScale * yStretch);
		// set space width
		bFont.getData().getGlyph(' ').xadvance = (int) (actualScale * spaceWidth);
		// lineheight
		bFont.getData().down = lineHeight * -actualScale * yStretch;

		// generate vertices for the mesh on the font object
		float x = alignment == Align.right ? -width
				: alignment == Align.center ? -width / 2 : 0;

		bFont.getCache().setText(text, x, 0, width, alignment, wrap);

		// create mesh using the font object's vertices and other parameters
		r().setMesh(
				LttlMeshFactory.GenerateFontMesh(r().getMesh(), bFont, centerMesh, text));

		r().getTex0().textureRegionName = ""; // this will prevent errors
		r().getTex0().setAR((AtlasRegion) bFont.getRegion());
	}

	@Override
	final public void updateMeshAA(float calculatedAA)
	{
		// do nothing
	}

	@Override
	public void onEditorDestroyComp()
	{
		processOnDestroyComp();
	}

	@Override
	public void onDestroyComp()
	{
		processOnDestroyComp();
	}

	private void processOnDestroyComp()
	{
		// tell the LttlFont you are not using it anymore
		if (font != null)
		{
			font.stopUsing(this);
		}
	}

}
