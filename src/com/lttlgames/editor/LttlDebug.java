package com.lttlgames.editor;

import java.util.Comparator;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.lttlgames.editor.LoopManager.RenderView;
import com.lttlgames.graphics.Cap;
import com.lttlgames.graphics.Joint;
import com.lttlgames.graphics.LttlBlendMode;
import com.lttlgames.graphics.LttlMesh;
import com.lttlgames.graphics.LttlMeshFactory;
import com.lttlgames.graphics.LttlShader;
import com.lttlgames.helpers.LttlGeometryUtil.PolygonContainer;
import com.lttlgames.helpers.LttlMath;
import com.lttlgames.helpers.Vector2Array;

public class LttlDebug
{
	private static final float aaFactor = .2f;
	private Matrix3 transform = new Matrix3();
	public final Color tmpColor = new Color();
	private BitmapFont bFont;
	private LttlMesh tempMesh;
	private static final LttlBlendMode defaultBlendMode = LttlBlendMode.ALPHA;
	private boolean instantDraw = false;
	private int zIndex = 0;

	public static final float RADIUS_SMALL = .4f;
	public static final float RADIUS_MEDIUM = .5f;
	public static final float RADIUS_LARGE = 2;

	public static final float WIDTH_SMALL = 0;
	public static final float WIDTH_MEDIUM = .07f;
	// public static final float WIDTH_LARGE = ??

	private Array<DebugShape> shapes = new Array<>();
	private Pool<Rect> rectPool = new Pool<LttlDebug.Rect>(0)
	{
		@Override
		protected Rect newObject()
		{
			return new Rect();
		}

	};
	private Pool<Polygon> polygonPool = new Pool<LttlDebug.Polygon>(0)
	{
		@Override
		protected Polygon newObject()
		{
			return new Polygon();
		}

	};
	private Pool<Text> textPool = new Pool<LttlDebug.Text>(0)
	{
		@Override
		protected Text newObject()
		{
			return new Text();
		}

	};
	private Pool<Elipse> ellipsePool = new Pool<LttlDebug.Elipse>(0)
	{
		@Override
		protected Elipse newObject()
		{
			return new Elipse();
		}

	};
	private Pool<PrettyLine> prettyLinePool = new Pool<LttlDebug.PrettyLine>(0)
	{
		@Override
		protected PrettyLine newObject()
		{
			return new PrettyLine();
		}

	};
	private Pool<Donut> donutPool = new Pool<LttlDebug.Donut>(0)
	{
		@Override
		protected Donut newObject()
		{
			return new Donut();
		}

	};

	private LttlModeOption currentMode = LttlModeOption.Editor;

	private Vector2 tmp0 = new Vector2();

	public void drawText(float x, float y, int fontSize, float fontScale, String text,
			Color color)
	{
		if (!canAdd()) return;
		if (text == null || text.isEmpty()) return;

		add(textPool.obtain().set(x, y, fontSize, fontScale, text, false, 0, Align.left,
				true, color));
	}

	public void drawText(float x, float y, int fontSize, float fontScale, String text,
			boolean wrap, float width, int alignment, boolean centerMesh, Color color)
	{
		if (!canAdd()) return;
		if (text == null || text.isEmpty()) return;

		add(textPool.obtain().set(x, y, fontSize, fontScale, text, wrap, width, alignment,
				centerMesh, color));
	}

	public void drawCircle(Circle c, Color color)
	{
		drawCircle(c.x, c.y, c.radius, color);
	}

	public void drawCircle(float x, float y, float radius, Color color)
	{
		if (!canAdd()) return;

		add(ellipsePool.obtain().set(x, y, radius, color));
	}

	public void drawCircle(Vector2 position, float radius, Color color)
	{
		drawCircle(position.x, position.y, radius, color);
	}

	public void drawElipse(float x, float y, float width, float height, Color color)
	{
		if (!canAdd()) return;

		add(ellipsePool.obtain().set(x, y, width, height, color));
	}

	/**
	 * @param rect
	 * @param rotation
	 * @param color
	 */
	public void drawRect(Rectangle rect, float rotation, Color color)
	{
		drawRect(rect.x + rect.width / 2, rect.y + rect.height / 2, rect.width,
				rect.height, rotation, color);
	}

	/**
	 * @param x
	 *            center position
	 * @param y
	 *            center position
	 * @param sizeX
	 * @param sizeY
	 * @param rotation
	 * @param color
	 */
	public void drawRect(float x, float y, float sizeX, float sizeY, float rotation,
			Color color)
	{
		if (!canAdd()) return;

		add(rectPool.obtain().reset(x, y, sizeX, sizeY, rotation, color));
	}

	private void drawRectLine(float x, float y, float sizeX, float sizeY, float rotation,
			boolean isHorizontal, Color color)
	{
		if (!canAdd()) return;

		add(rectPool.obtain().reset(x, y, sizeX, sizeY, rotation, true, isHorizontal,
				color));
	}

	/**
	 * @param position
	 *            center
	 * @param sizeX
	 * @param sizeY
	 * @param color
	 */
	public void drawRect(Vector2 position, float sizeX, float sizeY, float rotation,
			Color color)
	{
		if (!canAdd()) return;

		drawRect(position.x, position.y, sizeX, sizeY, rotation, color);
	}

	public void drawLine(Vector2 start, Vector2 end, float width, Color color)
	{
		drawLine(start.x, start.y, end.x, end.y, width, color);
	}

	public void drawLine(float startX, float startY, float endX, float endY, float width,
			Color color)
	{
		if (!canAdd()) return;

		// makes sure atleast one pixel wide
		width = LttlMath.max(
				LttlMath.abs(getModeCamera().getUnitsPerPixelXZoomed() * .4f), width);

		// draw a non rotated line
		if (startX == endX)
		{
			// vertical line
			float length = LttlMath.abs(startY - endY);
			drawRectLine(startX, (startY + endY) / 2, width, length, 0, false, color);
		}
		else if (startY == endY)
		{
			// horizontal line
			float length = LttlMath.abs(endX - startX);
			drawRectLine((startX + endX) / 2, startY, length, width, 0, true, color);
		}
		else
		{
			// slanted line (rect is horizontal though)
			// position is midpoint between the points
			float length = tmp0.set(startX, startY).dst(endX, endY);
			drawRectLine((startX + endX) / 2, (startY + endY) / 2, length, width,
					LttlMath.GetAngleBetweenPoints(startX, startY, endX, endY, true),
					true, color);
		}
	}

	public void drawLines(float[] points, float width, boolean connected, Color color)
	{
		drawLines(new Vector2Array(points), width, connected, color);
	}

	public void drawLines(Vector2Array points, float width, boolean connected,
			Color color)
	{
		if (points.size() < 2)
		{
			Lttl.Throw("Need at least 2 points to draw lines.");
		}

		// Skip last point unless connected
		for (int i = 0; i < points.size() - 1; i++)
		{
			drawLine(points.getX(i), points.getY(i), points.getX(i + 1),
					points.getY(i + 1), width, color);
		}

		if (connected)
		{
			drawLine(points.getLastX(), points.getLastY(), points.getFirstX(),
					points.getFirstY(), width, color);
		}
	}

	public void drawPrettyLine(float startX, float startY, float endX, float endY,
			float width, Cap capStart, Cap capEnd, Color color)
	{
		if (!canAdd()) return;

		add(prettyLinePool.obtain().set(new Vector2Array(startX, startY, endX, endY),
				width, Joint.BEVEL, capStart, capEnd, false, color));
	}

	public void drawPrettyLine(Vector2 startPoint, Vector2 endPoint, float width,
			Cap capStart, Cap capEnd, Color color)
	{
		drawPrettyLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y, width,
				capStart, capEnd, color);
	}

	public void drawPrettyLinesClosed(Vector2Array points, float width, Joint cornerType,
			boolean isClosed, Color color)
	{
		if (!canAdd()) return;

		// need at least two points
		Lttl.Throw(points.size() < 2);
		add(prettyLinePool.obtain().set(points, width, cornerType, Cap.NONE, Cap.NONE,
				true, color));
	}

	public void drawPrettyLinesOpen(Vector2Array points, float width, Joint cornerType,
			Cap capStart, Cap capEnd, Color color)
	{
		if (!canAdd()) return;

		// need at least two points
		Lttl.Throw(points.size() < 2);
		add(prettyLinePool.obtain().set(points, width, cornerType, capStart, capEnd,
				false, color));
	}

	/**
	 * @see #drawElipseOutline(float, float, float, float, Color)
	 */
	public void drawCircleOutline(float centerX, float centerY, float radius, Color color)
	{
		drawCircleOutline(centerX, centerY, radius, -1, color);
	}

	/**
	 * @see #drawElipseOutline(float, float, float, float, float, Color)
	 */
	public void drawCircleOutline(float centerX, float centerY, float radius,
			float borderWidth, Color color)
	{
		if (!canAdd()) return;

		add(donutPool.obtain().set(centerX, centerY, radius, borderWidth, color));
	}

	/**
	 * Draws ellipse, uses standard border width (% of radius)
	 */
	public void drawElipseOutline(float centerX, float centerY, float width, float height,
			Color color)
	{
		drawElipseOutline(centerX, centerY, width, height, -1, color);
	}

	/**
	 * if borderWidth is 0 or less, then uses standard border width (% of radius), if not has to make a new mesh every
	 * frame
	 */
	public void drawElipseOutline(float centerX, float centerY, float width, float height,
			float borderWidth, Color color)
	{
		if (!canAdd()) return;

		add(donutPool.obtain().set(centerX, centerY, width, height, borderWidth, color));
	}

	/**
	 * @see #drawElipseOutline(float, float, float, float, Color)
	 */
	public void drawElipseFilledOutline(float centerX, float centerY, float width,
			float height, Color fillColor, Color borderColor)
	{
		drawElipseFilledOutline(centerX, centerY, width, height, fillColor, -1,
				borderColor);
	}

	/**
	 * @see #drawElipseOutline(float, float, float, float, float, Color)
	 */
	public void drawElipseFilledOutline(float centerX, float centerY, float width,
			float height, Color fillColor, float borderWidth, Color borderColor)
	{
		if (!canAdd()) return;

		drawElipseOutline(centerX, centerY, width, height, borderWidth, borderColor);
		drawElipse(centerX, centerY, width, height, fillColor);
	}

	/**
	 * @see #drawElipseOutline(float, float, float, float, Color)
	 */
	public void drawCircleFilledOutline(float centerX, float centerY, float radius,
			Color fillColor, Color borderColor)
	{
		drawCircleFilledOutline(centerX, centerY, radius, fillColor, -1, borderColor);
	}

	/**
	 * @see #drawElipseOutline(float, float, float, float, float, Color)
	 */
	public void drawCircleFilledOutline(float centerX, float centerY, float radius,
			Color fillColor, float borderWidth, Color borderColor)
	{
		if (!canAdd()) return;

		drawCircleOutline(centerX, centerY, radius, borderColor);
		drawCircle(centerX, centerY, radius, fillColor);
	}

	public void drawCross(float x, float y, float size, float lineWidth, Color color)
	{
		if (!canAdd()) return;

		drawLine(x, y + lineWidth / 2, x, y + size, lineWidth, color);
		drawLine(x, y - lineWidth / 2, x, y - size, lineWidth, color);
		drawLine(x - size, y, x + size, y, lineWidth, color);
	}

	public void drawCrosshairs(float x, float y, float radius, Color color)
	{
		if (!canAdd()) return;

		drawCircleOutline(x, y, radius, color);
		drawCross(x, y, radius, radius * .02f, color);
	}

	public void drawCrosshairsFilled(float x, float y, float radius, Color fillColor,
			float lineWidth, Color borderColor)
	{
		if (!canAdd()) return;

		drawCrosshairs(x, y, radius, borderColor);
		drawCircle(x, y, radius, fillColor);
	}

	public void drawRectOutline(Rectangle rect, float width, Color color)
	{
		drawRectOutline(rect.x + rect.width / 2, rect.y + rect.height / 2, rect.width,
				rect.height, width, color);
	}

	/**
	 * @param centerX
	 * @param centerY
	 * @param sizeX
	 * @param sizeY
	 * @param borderWidth
	 * @param color
	 */
	public void drawRectOutline(float centerX, float centerY, float sizeX, float sizeY,
			float borderWidth, Color color)
	{
		if (!canAdd()) return;

		Vector2Array values = new Vector2Array(centerX - sizeX / 2, centerY - sizeY / 2,
				centerX - sizeX / 2, centerY + sizeY / 2, centerX + sizeX / 2,
				centerY + sizeY / 2, centerX + sizeX / 2, centerY - sizeY / 2);

		drawLines(values, borderWidth, true, color);
	}

	public void drawPrettyRectOutline(float x, float y, float sizeX, float sizeY,
			float borderWidth, Joint cornerType, Color color)
	{
		if (!canAdd()) return;

		Vector2Array points = new Vector2Array(4);
		points.add(x - sizeX / 2, y - sizeY / 2);
		points.add(x - sizeX / 2, y + sizeY / 2);
		points.add(x + sizeX / 2, y + sizeY / 2);
		points.add(x + sizeX / 2, y - sizeY / 2);

		drawPrettyLinesClosed(points, borderWidth, cornerType, true, color);
	}

	public void drawRectFilledOutline(Rectangle rect, float rotation, Color fillColor,
			float borderWidth, Color borderColor)
	{
		drawRectFilledOutline(rect.x, rect.y, rect.width, rect.height, rotation,
				fillColor, borderWidth, borderColor);
	}

	public void drawRectFilledOutline(float x, float y, float sizeX, float sizeY,
			float rotation, Color fillColor, float borderWidth, Color borderColor)
	{
		if (!canAdd()) return;

		drawRectOutline(x + sizeX / 2, y + sizeY / 2, sizeX, sizeY, borderWidth,
				borderColor);
		drawRect(x + sizeX / 2, y + sizeY / 2, sizeX, sizeY, rotation, fillColor);
	}

	public void drawPolygonFilled(PolygonContainer polyCont, Color color)
	{
		if (!canAdd()) return;

		if (polyCont.isValid())
		{
			add(polygonPool.obtain().set(polyCont, color));
		}
	}

	public void drawPolygonFilled(Vector2Array points, Color color)
	{
		drawPolygonFilled(new PolygonContainer(points), color);
	}

	public void drawPolygonFilled(float[] points, Color color)
	{
		drawPolygonFilled(new PolygonContainer(points), color);
	}

	/**
	 * @param points
	 * @param fillColor
	 *            if null, will not draw fill
	 * @param borderWidth
	 *            if 0 or less will not draw border
	 * @param borderColor
	 *            if null, will not draw border
	 */
	public void drawPolygonFilledOutline(float[] points, Color fillColor,
			float borderWidth, Color borderColor)
	{
		drawPolygonFilledOutline(new Vector2Array(points), fillColor, borderWidth,
				borderColor);
	}

	/**
	 * @param points
	 * @param fillColor
	 *            if null, will not draw fill
	 * @param borderWidth
	 *            if 0 or less will not draw border
	 * @param borderColor
	 *            if null, will not draw border
	 */
	public void drawPolygonFilledOutline(PolygonContainer polyCont, Color fillColor,
			float borderWidth, Color borderColor)
	{
		if (!canAdd()) return;

		drawPolygonFilled(polyCont, fillColor);
		drawPolygonOutline(polyCont, borderWidth, borderColor);
	}

	/**
	 * @param points
	 * @param fillColor
	 *            if null, will not draw fill
	 * @param borderWidth
	 *            if 0 or less will not draw border
	 * @param borderColor
	 *            if null, will not draw border
	 */
	public void drawPolygonFilledOutline(Vector2Array points, Color fillColor,
			float borderWidth, Color borderColor)
	{
		drawPolygonFilledOutline(new PolygonContainer(points), fillColor, borderWidth,
				borderColor);
	}

	public void drawPolygonOutline(float[] points, float width, Color color)
	{
		drawPolygonOutline(new Vector2Array(points), width, color);
	}

	public void drawPolygonOutline(Vector2Array points, float width, Color color)
	{
		if (!canAdd()) return;

		drawLines(points, width, true, color);
	}

	public void drawPolygonOutline(PolygonContainer polyCont, float width, Color color)
	{
		if (!canAdd()) return;

		if (width > 0)
		{
			drawLines(polyCont.getPoints(), width, true, color);
			for (Vector2Array hole : polyCont.getHoles())
			{
				drawLines(hole, width, true, color);
			}
		}

	}

	private void renderText(Text t)
	{
		// create object transform
		transform.idt();
		transform.trn(t.x, t.y);

		if (bFont == null)
		{
			// create standard font to use for debug
			bFont = Lttl.game.fontManager
					.getFont(LttlFontManager.defaultFont, t.fontSize, null)
					.generateBitmapFont();
		}

		float fontScale = t.fontScale;
		float spaceWidth = 10;
		float lineHeight = 60;

		bFont.getData().setScale(fontScale, fontScale);
		bFont.getData().getGlyph(' ').xadvance = (int) (fontScale * spaceWidth);
		bFont.getData().down = lineHeight * -fontScale * 1;

		float x = t.alignment == Align.right ? -t.width
				: t.alignment == Align.center ? -t.width / 2 : 0;

		bFont.getCache().setText(t.text, x, 0, t.width, t.alignment, t.wrap);

		// create mesh using the font object's vertices and other parameters
		tempMesh =
				LttlMeshFactory.GenerateFontMesh(tempMesh, bFont, t.centerMesh, t.text);
		tempMesh.updateColorAlpha(tmpColor.set(t.r, t.g, t.b, t.a), t.a);
		tempMesh.updateWorldVertices(transform);

		// actually render
		Lttl.loop.renderDebug(tempMesh, defaultBlendMode, LttlShader.TextureShader,
				bFont.getRegion().getTexture());
	}

	private void renderElipse(Elipse e)
	{
		// create object transform
		transform.idt();
		transform.scale(e.width, e.height);
		transform.trn(e.x, e.y);

		LttlMesh mesh = LttlMeshFactory.GetCircleMesh();
		mesh.updateColorAlpha(tmpColor.set(e.r, e.g, e.b, e.a), e.a);
		mesh.updateWorldVertices(transform);

		// actually render
		Lttl.loop.renderDebug(mesh, defaultBlendMode, LttlShader.SimpleColorShader, null);
	}

	private void renderDonut(Donut d)
	{
		// create object transform
		transform.idt();
		if (d.lineWidth == -1) transform.scale(d.width, d.height);
		transform.trn(d.x, d.y);

		// generate mesh
		LttlMesh mesh = d.lineWidth <= 0 ? LttlMeshFactory.GetDonutMesh()
				: LttlMeshFactory.GenerateShapeOutline(tempMesh, d.width, d.height, 50,
						360, 0, d.lineWidth, true);
		mesh.updateColorAlpha(tmpColor.set(d.r, d.g, d.b, d.a), d.a);
		mesh.updateWorldVertices(transform);

		// actually render
		Lttl.loop.renderDebug(mesh, defaultBlendMode, LttlShader.SimpleColorShader, null);
	}

	private void renderRect(Rect r)
	{
		// create object transform
		transform.idt();
		if (r.rotation != 0)
		{
			transform.rotate(r.rotation);
		}
		transform.scale(r.sizeX, r.sizeY);
		transform.trn(r.x, r.y);
		if (r.rotation == 0 && r.isLine && !r.isHorizontal)
		{
			transform.rotate(90);
		}

		LttlMesh mesh;
		if (r.isLine)
		{
			mesh = LttlMeshFactory.GetLineQuadMesh();
		}
		else
		{
			mesh = LttlMeshFactory.GetQuadMeshShared();
		}

		mesh.updateColorAlpha(tmpColor.set(r.r, r.g, r.b, r.a), r.a);
		mesh.updateWorldVertices(transform);

		// actually render
		Lttl.loop.renderDebug(mesh, defaultBlendMode, LttlShader.SimpleColorShader, null);
	}

	private void renderPrettyLine(PrettyLine l)
	{
		// actually render (adds AA based on editor view zoom and line width)
		// OPTIMIZE creating new meshes every frame is bad, but unavoidable
		int capCornerSteps = 15;
		tempMesh = LttlMeshFactory.GenerateLine(tempMesh, l.points, l.cornerType,
				l.capStart, l.capEnd, capCornerSteps, l.width, 0, l.isClosed,
				aaFactor * eF());
		tempMesh.updateColorAlpha(tmpColor.set(l.r, l.g, l.b, l.a), l.a);

		// already in world transform
		tempMesh.getWorldVerticesArray().clear();
		tempMesh.getWorldVerticesArray().addAll(tempMesh.getVerticesArray());

		// actually render
		Lttl.loop.renderDebug(tempMesh, defaultBlendMode, LttlShader.SimpleColorShader,
				null);

	}

	private void renderPolygon(Polygon p)
	{
		// actually render
		// OPTIMIZE creating new meshes every frame is bad, but unavoidable
		tempMesh = LttlMeshFactory.GeneratePolygon(tempMesh, p.polyCont, false);
		tempMesh.updateColorAlpha(tmpColor.set(p.r, p.g, p.b, p.a), p.a);

		// already in world transform
		tempMesh.getWorldVerticesArray().clear();
		tempMesh.getWorldVerticesArray().addAll(tempMesh.getVerticesArray());

		// actually render
		Lttl.loop.renderDebug(tempMesh, defaultBlendMode, LttlShader.SimpleColorShader,
				null);
	}

	private class Text extends DebugShape
	{
		float x;
		float y;
		String text;
		int fontSize;
		float fontScale;
		float width;
		boolean wrap;
		int alignment;
		boolean centerMesh;

		Text set(float x, float y, int fontSize, float fontScale, String text,
				boolean wrap, float width, int alignment, boolean centerMesh, Color color)
		{
			super.reset();

			this.x = x;
			this.y = y;
			this.text = text;
			this.fontSize = fontSize;
			this.fontScale = fontScale;
			this.wrap = wrap;
			this.width = width;
			this.alignment = alignment;
			this.centerMesh = centerMesh;

			if (color == null) return this;
			this.r = color.r;
			this.g = color.g;
			this.b = color.b;
			this.a = color.a;

			return this;
		}
	}

	private class Elipse extends DebugShape
	{
		float x;
		float y;
		float height;
		float width;

		Elipse set(float x, float y, float width, float height, Color color)
		{
			this.x = x;
			this.y = y;
			this.height = height;
			this.width = width;

			if (color == null) return this;
			this.r = color.r;
			this.g = color.g;
			this.b = color.b;
			this.a = color.a;

			return this;
		}

		Elipse set(float x, float y, float radius, Color color)
		{
			radius = getDefaultRadius(radius);
			return set(x, y, radius, radius, color);
		}
	}

	private class Donut extends DebugShape
	{
		float x;
		float y;
		float width;
		float height;
		float lineWidth;

		Donut set(float x, float y, float width, float height, float lineWidth,
				Color color)
		{
			super.reset();

			this.x = x;
			this.y = y;
			this.lineWidth = lineWidth;
			this.width = width;
			this.height = height;

			if (color == null) return this;
			this.r = color.r;
			this.g = color.g;
			this.b = color.b;
			this.a = color.a;

			return this;
		}

		Donut set(float x, float y, float radius, float lineWidth, Color color)
		{
			return set(x, y, radius, radius, lineWidth, color);
		}
	}

	private class Rect extends DebugShape
	{
		float x;
		float y;
		float sizeX;
		float sizeY;
		float rotation = 0;
		boolean isLine = false;
		boolean isHorizontal = true;

		Rect reset(float x, float y, float sizeX, float sizeY, float rotation,
				Color color)
		{
			return reset(x, y, sizeX, sizeY, rotation, false, false, color);
		}

		Rect reset(float x, float y, float sizeX, float sizeY, float rotation,
				boolean isLine, boolean isHorizontal, Color color)
		{
			super.reset();

			this.isLine = isLine;
			this.isHorizontal = isHorizontal;
			this.rotation = rotation;
			this.x = x;
			this.y = y;
			this.sizeX = sizeX;
			this.sizeY = sizeY;

			if (color == null) return this;
			this.r = color.r;
			this.g = color.g;
			this.b = color.b;
			this.a = color.a;

			return this;
		}
	}

	private class Polygon extends DebugShape
	{
		PolygonContainer polyCont;

		Polygon set(PolygonContainer polyCont, Color color)
		{
			super.reset();

			this.polyCont = polyCont;

			if (color == null) return this;
			this.r = color.r;
			this.g = color.g;
			this.b = color.b;
			this.a = color.a;

			return this;
		}
	}

	private class PrettyLine extends DebugShape
	{
		Vector2Array points;
		Joint cornerType;
		Cap capStart;
		Cap capEnd;
		boolean isClosed;
		float width;

		PrettyLine set(Vector2Array points, float width, Joint cornerType, Cap capStart,
				Cap capEnd, boolean isClosed, Color color)
		{
			super.reset();

			this.width = width;
			this.cornerType = cornerType;
			this.capStart = capStart;
			this.capEnd = capEnd;
			this.isClosed = isClosed;
			this.points = points;

			if (color == null) return this;
			this.r = color.r;
			this.g = color.g;
			this.b = color.b;
			this.a = color.a;

			return this;
		}
	}

	private class DebugShape
	{
		LttlModeOption mode;
		float r = 0;
		float g = 0;
		float b = 0;
		float a = 1;
		int zIndex = 0;

		void reset()
		{
			r = 0;
			g = 0;
			b = 0;
			a = 1;
			zIndex = 0;
		}
	}

	void debugRender()
	{
		// sort by zIndex
		shapes.sort(new Comparator<DebugShape>()
		{
			@Override
			public int compare(DebugShape o1, DebugShape o2)
			{
				if (o1.zIndex < o2.zIndex)
				{
					return 1;
				}
				else if (o1.zIndex > o2.zIndex)
				{
					return -1;
				}
				else
				{
					return 0;
				}
			}
		});

		// editor camera
		if (Lttl.loop.getRenderView() == RenderView.Editor)
		{
			// only render if in editor or both mode
			for (DebugShape ds : shapes)
			{
				if (ds.mode == LttlModeOption.Play) continue;
				render(ds);
			}
		}
		else
		{
			// play camera
			if (Lttl.game.getSettings().drawEditorDebugInPlayMode)
			{
				// draw editor debug in play mode so render every mode
				for (DebugShape ds : shapes)
				{
					render(ds);
				}
			}
			else
			{
				// only render if in play or both mode
				for (DebugShape ds : shapes)
				{
					if (ds.mode == LttlModeOption.Editor) continue;
					render(ds);
				}
			}
		}
	}

	private void render(DebugShape ds)
	{
		Class<? extends DebugShape> c = ds.getClass();

		if (c == Donut.class) renderDonut((Donut) ds);
		else if (c == Rect.class) renderRect((Rect) ds);
		else if (c == Elipse.class) renderElipse((Elipse) ds);
		else if (c == Polygon.class) renderPolygon((Polygon) ds);
		else if (c == PrettyLine.class) renderPrettyLine((PrettyLine) ds);
		else if (c == Text.class) renderText((Text) ds);
	}

	/**
	 * This should be ran after both cameras do their debugDraw
	 */
	void reset()
	{
		// release the hounds
		for (DebugShape s : shapes)
		{
			if (s instanceof Rect)
			{
				rectPool.free((Rect) s);
			}
			else if (s instanceof Polygon)
			{
				polygonPool.free((Polygon) s);
			}
			else if (s instanceof Elipse)
			{
				ellipsePool.free((Elipse) s);
			}
			else if (s instanceof Donut)
			{
				donutPool.free((Donut) s);
			}
			else if (s instanceof Text)
			{
				textPool.free((Text) s);
			}
			else if (s instanceof PrettyLine)
			{
				prettyLinePool.free((PrettyLine) s);
			}
		}

		// clear shapes list
		shapes.clear();

		// reset mode
		currentMode = LttlModeOption.Editor;
		zIndex = 0;
		instantDraw = false;
	}

	/**
	 * This is an early out for so don't need to even process any debug calls, since things like trigonometry are
	 * expensive.
	 * 
	 * @return
	 */
	private boolean canAdd()
	{
		// if in playMode, then always allow this debug
		if (currentMode == LttlModeOption.Play || currentMode == LttlModeOption.Both
				|| Lttl.game.inEditor())
		{
			return true;
		}
		// if playing not in editor don't add any of the debugs, unless drawEditorDebugsInPlayMode is enabled
		else if (!Lttl.game.inEditor()
				&& Lttl.game.getSettings().drawEditorDebugInPlayMode) { return true; }
		return false;

	}

	/**
	 * Debug draw will be drawn right away on the current camera rendering, does not force a flush though. This is
	 * really only useful during rendering loop or post processing.
	 */
	public void setInstantDraw(boolean instantDraw)
	{
		this.instantDraw = instantDraw;
	}

	/**
	 * @see #setInstantDraw(boolean)
	 */
	public boolean getInstantDraw()
	{
		return instantDraw;
	}

	/**
	 * The following shapes rendered will use this zIndex. Lower indexes means rendered later (on top).
	 */
	public void setZIndex(int zIndex)
	{
		this.zIndex = zIndex;
	}

	public int getZIndex()
	{
		return zIndex;
	}

	public LttlModeOption getMode()
	{
		return currentMode;
	}

	public void setMode(LttlModeOption mode)
	{
		currentMode = mode;
	}

	/**
	 * Used to add all shapes to same list so they can always be rendered in order of call
	 * 
	 * @param ds
	 */
	private void add(DebugShape ds)
	{
		if (instantDraw)
		{
			render(ds);
		}
		else
		{
			ds.mode = currentMode;
			ds.zIndex = zIndex;
			shapes.add(ds);
		}
	}

	/**
	 * The editor camera zoom factor. Used as a facor to be multipled to debug shape's sizes, so they maintain size even
	 * when zoom and editor view changes.<br>
	 * Also includes the WidthFactor.
	 * 
	 * @return
	 */
	public float eF()
	{
		return 1 / Lttl.editor.getCamera().zoom
				/ LttlMath.EpsilonClamp(Lttl.editor.getSettings().editorViewRatio)
				* Lttl.game.getSettings().getWidthFactor();
	}

	/**
	 * The play camera zoom factor. Used as a facor to be multipled to debug shape's sizes, so they maintain size even
	 * when zoom changes.<br>
	 * Also includes the WidthFactor.
	 * 
	 * @return
	 */
	public float pF()
	{
		return 1 / Lttl.game.getCamera().zoom
				/ LttlMath.EpsilonClamp(1 - Lttl.editor.getSettings().editorViewRatio)
				* Lttl.game.getSettings().getWidthFactor();
	}

	/**
	 * Returns the mode relative factor {@link #eF()} or {@link #pF()}. If {@link LttlModeOption#Both} then
	 * {@link #eF()}
	 */
	public float mF()
	{
		if (!Lttl.game.inEditor()) { return pF(); }
		switch (currentMode)
		{
			case Both:
			case Editor:
				return eF();
			case Play:
				return pF();
		}
		return pF();
	}

	/**
	 * Returns the camera of the current mode.
	 */
	public LttlCamera getModeCamera()
	{
		if (!Lttl.game.inEditor()) { return Lttl.game.getCamera(); }
		switch (currentMode)
		{
			case Both:
			case Editor:
				return Lttl.editor.getCamera();
			case Play:
				return Lttl.game.getCamera();
		}
		return Lttl.game.getCamera();

	}

	public int getShapesCount()
	{
		return shapes.size;
	}

	private float getDefaultRadius(float user)
	{
		return user < 0 ? Lttl.debug.eF() * LttlDebug.RADIUS_SMALL : user;
	}
}
