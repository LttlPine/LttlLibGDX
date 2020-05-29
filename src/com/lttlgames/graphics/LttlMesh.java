package com.lttlgames.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.ShortArray;
import com.lttlgames.editor.Lttl;
import com.lttlgames.editor.UVMeshSettings;
import com.lttlgames.editor.annotations.IgnoreCrawl;
import com.lttlgames.helpers.LttlGeometryUtil.PolygonContainer;
import com.lttlgames.helpers.LttlMath;
import com.lttlgames.helpers.LttlProfiler;
import com.lttlgames.helpers.Vector2Array;

@IgnoreCrawl
public class LttlMesh
{
	/* STATICS */
	public static final VertexAttribute[] VERTEX_ATTRIBUTES = new VertexAttribute[]
	{
			new VertexAttribute(Usage.Position, 2, LttlShaders.ATTR_POS),
			new VertexAttribute(Usage.TextureCoordinates, 2,
					LttlShaders.ATTR_TEXCOORD),
			new VertexAttribute(Usage.ColorPacked, 4, LttlShaders.ATTR_COLOR),
			new VertexAttribute(Usage.Generic, 1, LttlShaders.ATTR_ALPHA) };
	public static final int ATTRIBUTE_INDEX_POSX = 0;
	public static final int ATTRIBUTE_INDEX_POSY = 1;
	public static final int ATTRIBUTE_INDEX_UVX = 2;
	public static final int ATTRIBUTE_INDEX_UVY = 3;
	public static final int ATTRIBUTE_INDEX_COLOR = 4;
	public static final int ATTRIBUTE_INDEX_ALPHA = 5;
	// NOTE changes to vertex attributes should also be done in LttlMesh.addVertice
	public static final int VERTICE_ATTRIBUTE_COUNT = 6;

	/* TEMP */
	private static final Vector2 tmp = new Vector2();
	private static final Vector2Array tmpV2Array = new Vector2Array();
	private static final Rectangle tmpRect = new Rectangle();

	/* MEMBERS */
	/**
	 * This holds the value for where the aa vertices start
	 */
	private int aaVerticeIndex = -1;
	/**
	 * This holds the value for where the aa indices start
	 */
	private int aaIndiceIndex = -1;
	private final IntArray holesIndexArray = new IntArray(0);
	private FloatArray verticesArray;
	private FloatArray worldVerticesArray = new FloatArray();
	private ShortArray indicesArray;
	private float lastMeshUpdateWorldAlpha = Float.NEGATIVE_INFINITY;
	private Color lastMeshUpdateColor;
	private boolean needToUpdateBoundingRect = true;
	private Rectangle boundingRectCached;
	public int updateId = 0;

	/**
	 * Returns the id of the last time this mesh was set (usually when updated). This can be used to know when the mesh
	 * has been updated.
	 * 
	 * @return
	 */
	public int getUpdateId()
	{
		return updateId;
	}

	/**
	 * Creates an array with a capacity of 4.
	 */
	public LttlMesh()
	{
		this(4);
	}

	public LttlMesh(FloatArray vertices, ShortArray indices)
	{
		LttlProfiler.meshObjects.add();
		verticesArray = new FloatArray(vertices);
		indicesArray = new ShortArray(indices);
	}

	public LttlMesh(int verticeCapacity)
	{
		LttlProfiler.meshObjects.add();
		verticesArray = new FloatArray(verticeCapacity
				* LttlMesh.VERTICE_ATTRIBUTE_COUNT);
		indicesArray = new ShortArray(verticeCapacity * 3);
	}

	/**
	 * Makes a copy of the source VerticeArray
	 * 
	 * @param source
	 */
	public LttlMesh(LttlMesh source)
	{
		verticesArray = new FloatArray(source.verticesArray);
	}

	public void addIndices(int... indices)
	{
		indicesArray.ensureCapacity(indices.length);
		for (int i = 0; i < indices.length; i++)
		{
			indicesArray.add(indices[i]);
		}
	}

	public void addIndice(int i)
	{
		indicesArray.add(i);
	}

	public void add(LttlMesh mesh)
	{
		add(mesh.getVerticesArray(), mesh.getIndicesArray());
	}

	public void add(FloatArray vertices, ShortArray indices)
	{
		int indexOffset = this.getVertexCount();
		this.verticesArray.addAll(vertices);
		this.indicesArray.ensureCapacity(indices.size);
		for (int i = 0; i < indices.size; i++)
		{
			this.indicesArray.add(indexOffset + indices.get(i));
		}
	}

	/**
	 * @param x
	 * @param y
	 * @param u
	 * @param v
	 * @param color
	 * @param alpha
	 */
	public void addVertice(float x, float y, float u, float v, float color,
			float alpha)
	{
		addVertice(verticesArray, x, y, u, v, color, alpha);
	}

	public void addVertice(Vector2 pos, Vector2 uv, float color, float alpha)
	{
		addVertice(verticesArray, pos, uv, color, alpha);
	}

	public void addVertice(Vector2 pos, float u, float v, float color,
			float alpha)
	{
		addVertice(verticesArray, pos, u, v, color, alpha);
	}

	public void set(int index, Vector2 point, float u, float v, float color,
			float alpha)
	{
		set(index, point.x, point.y, u, v, color, alpha);
	}

	public void set(int index, float x, float y, float u, float v, float color,
			float alpha)
	{
		set(verticesArray, index, x, y, u, v, color, alpha);
	}

	public void set(int index, int attribute, float value)
	{
		set(verticesArray, index, attribute, value);
	}

	public void set(LttlMesh mesh)
	{
		clear();
		verticesArray.addAll(mesh.verticesArray);
		worldVerticesArray.addAll(mesh.worldVerticesArray);
		indicesArray.addAll(mesh.indicesArray);
		holesIndexArray.addAll(mesh.holesIndexArray);
		aaVerticeIndex = mesh.aaVerticeIndex;
		aaIndiceIndex = mesh.aaIndiceIndex;
	}

	public float get(int index, int attribute)
	{
		return get(verticesArray, index, attribute);
	}

	public void offset(float x, float y)
	{
		for (int i = 0, n = getVertexCount(); i < n; i++)
		{
			setX(i, getX(i) + x);
			setY(i, getY(i) + x);
		}
	}

	public int getVertexCount()
	{
		return getVertexCount(verticesArray);
	}

	public int getVertextCount(boolean includeMain, boolean includeHoles,
			boolean includeAA)
	{
		int size = 0;

		// so can assume each is valid if icnluding
		includeHoles = includeHoles && getHolesCount() > 0;
		includeAA = includeAA && hasAA();

		// none
		if (!(includeMain || includeHoles || includeAA)) { return 0; }

		// all
		if (includeMain && includeHoles && includeAA) { return getVertexCount(); }

		int startHoles = getHolesCount() > 0 ? getHolesIndexArray().get(0) : -1;

		if (includeMain)
		{
			if (startHoles != -1)
			{
				size += startHoles;
			}
			else if (aaVerticeIndex != -1)
			{
				size += aaVerticeIndex;
			}
			else
			{
				size += getVertexCount();
				return size;
			}
		}
		if (includeHoles)
		{
			if (aaVerticeIndex != -1)
			{
				size += aaVerticeIndex - startHoles;
			}
			else
			{
				size += getVertexCount() - startHoles;
				return size;
			}
		}
		if (includeAA)
		{
			size += getVertexCount() - aaVerticeIndex;
		}

		return size;
	}

	/* GETTERS */
	public Vector2 getPos(int index, Vector2 result)
	{
		return result.set(getX(index), getY(index));
	}

	public Vector2 getUV(int index, Vector2 result)
	{
		return result.set(getU(index), getV(index));
	}

	public float getX(int index)
	{
		return get(index, LttlMesh.ATTRIBUTE_INDEX_POSX);
	}

	public float getY(int index)
	{
		return get(index, LttlMesh.ATTRIBUTE_INDEX_POSY);
	}

	public float getU(int index)
	{
		return get(index, LttlMesh.ATTRIBUTE_INDEX_UVX);
	}

	public float getV(int index)
	{
		return get(index, LttlMesh.ATTRIBUTE_INDEX_UVY);
	}

	public float getAlpha(int index)
	{
		return get(index, LttlMesh.ATTRIBUTE_INDEX_ALPHA);
	}

	public float getColor(int index)
	{
		return get(index, LttlMesh.ATTRIBUTE_INDEX_COLOR);
	}

	/* SETTERS */
	public void setX(int index, float value)
	{
		setX(verticesArray, index, value);
	}

	public void setY(int index, float value)
	{
		setY(verticesArray, index, value);
	}

	public void setPos(int index, float x, float y)
	{
		setPos(verticesArray, index, x, y);
	}

	public void offsetPos(int index, float x, float y)
	{
		offsetPos(verticesArray, index, x, y);
	}

	public void setU(int index, float value)
	{
		setU(verticesArray, index, value);
	}

	public void setV(int index, float value)
	{
		setV(verticesArray, index, value);
	}

	public void setUV(int index, float x, float y)
	{
		setUV(verticesArray, index, x, y);
	}

	public void setAlpha(int index, float value)
	{
		setAlpha(verticesArray, index, value);
	}

	public void setAlphaAll(float value)
	{
		setAlphaAll(verticesArray, value, aaVerticeIndex);
	}

	public void setColor(int index, float value)
	{
		setColor(verticesArray, index, value);
	}

	public void setColorAll(float value)
	{
		setColorAll(verticesArray, value);
	}

	/**
	 * Ensures the capacty of the local vertices and indices array
	 * 
	 * @param verticeCapacity
	 */
	public void ensureCapacity(int verticeCapacity)
	{
		Lttl.Throw(verticeCapacity < 3);
		ensureCapacity(verticesArray, verticeCapacity);
		indicesArray.ensureCapacity((verticeCapacity - 2) * 3);
	}

	/**
	 * Tries to update the local verticesArray's alpha and color, Called by loop manager before rendering if the
	 * renderer is set to auto update color and alpha<br>
	 * Only updates if values since last mesh update are different.
	 * 
	 * @param color
	 * @param alpha
	 * @return if found change and updated
	 */
	public boolean updateColorAlpha(Color color, float alpha)
	{
		return updateColorAlpha(color, alpha, true);
	}

	/**
	 * Tries to update the local verticesArray's alpha and color, Called by loop manager before rendering if the
	 * renderer is set to auto update color and alpha<br>
	 * Only updates if values since last mesh update are different.
	 * 
	 * @param color
	 * @param alpha
	 * @param check
	 * @return if found change and updated
	 */
	public boolean updateColorAlpha(Color color, float alpha, boolean check)
	{
		boolean changed = false;

		if (!check || !color.equals(lastMeshUpdateColor))
		{
			changed = true;
			setColorAll(verticesArray, color.toFloatBits());
		}
		if (!check || lastMeshUpdateWorldAlpha != alpha)
		{
			changed = true;
			setAlphaAll(verticesArray, alpha, aaVerticeIndex);
		}

		// save last values
		if (lastMeshUpdateColor == null)
		{
			lastMeshUpdateColor = new Color();
		}
		lastMeshUpdateColor.set(color);
		lastMeshUpdateWorldAlpha = alpha;

		return changed;
	}

	/**
	 * updates the color and alpha on the world vertices, does not check
	 * 
	 * @param color
	 * @param alpha
	 * @param world
	 */
	public void updateColorAlphaWorld(Color color, float alpha)
	{
		setColorAll(worldVerticesArray, color.toFloatBits());
		setAlphaAll(worldVerticesArray, alpha, aaVerticeIndex);
	}

	public void updateWorldVertices(Matrix3 worldRenderTransform)
	{
		worldVerticesArray.clear();
		worldVerticesArray.addAll(verticesArray);
		for (int i = 0, n = getVertexCount(); i < n; i++)
		{
			tmp.set(get(verticesArray, i, LttlMesh.ATTRIBUTE_INDEX_POSX),
					get(verticesArray, i, LttlMesh.ATTRIBUTE_INDEX_POSY));
			tmp.mul(worldRenderTransform);
			set(worldVerticesArray, i, LttlMesh.ATTRIBUTE_INDEX_POSX, tmp.x);
			set(worldVerticesArray, i, LttlMesh.ATTRIBUTE_INDEX_POSY, tmp.y);
		}
	}

	/**
	 * Includes AA vertices, if {@link #hasAA()} then use {@link #getAAVerticeIndex()}
	 */
	public FloatArray getVerticesArray()
	{
		return verticesArray;
	}

	/**
	 * Includes AA indices, if {@link #hasAA()} then use {@link #getAAIndiceIndex()}
	 */
	public ShortArray getIndicesArray()
	{
		return indicesArray;
	}

	public FloatArray getWorldVerticesArray()
	{
		return worldVerticesArray;
	}

	/**
	 * Gets all vertices (holes and AA)<br>
	 * see {@link #getVerticesPos(FloatArray, int, int, Vector2Array)}
	 */
	public Vector2Array getVerticesPos(boolean world, Vector2Array container)
	{
		return getVerticesPos(world, 0, getVertexCount() - 1, container);
	}

	/**
	 * see {@link #getVerticesPos(FloatArray, int, int, Vector2Array)}
	 */
	public Vector2Array getVerticesPos(boolean world, int startIndex,
			int endIndex, Vector2Array container)
	{
		return getVerticesPos(world ? getWorldVerticesArray()
				: getVerticesArray(), startIndex, endIndex, container);
	}

	/**
	 * Returns the position vertices (excluding holes and AA) of a mesh without attributes
	 * 
	 * @param world
	 *            world values, may not be generated
	 * @param container
	 * @return
	 */
	public Vector2Array getVerticesPosMain(boolean world, Vector2Array container)
	{
		int endIndex = getVertexCount() - 1;
		if (holesIndexArray.size > 0)
		{
			endIndex = holesIndexArray.get(0) - 1;
		}
		else if (hasAA())
		{
			endIndex = aaVerticeIndex - 1;
		}
		return getVerticesPos(world, 0, endIndex, container);
	}

	/**
	 * Returns the position vertices (excluding AA), may include holes, of a mesh without attributes
	 * 
	 * @param world
	 *            world values, may not be generated
	 * @param container
	 * @return
	 */
	public Vector2Array getVerticesPosNoAA(boolean world, Vector2Array container)
	{
		int endIndex = getVertexCount() - 1;
		if (hasAA())
		{
			endIndex = aaVerticeIndex - 1;
		}
		return getVerticesPos(world, 0, endIndex, container);
	}

	/**
	 * Generates the bounding rect, useful for just knowing the dimensions of the mesh.
	 * 
	 * @param world
	 *            world vertices, may not be generated
	 * @param includeAA
	 *            if applicable
	 * @param output
	 *            (if null return new Rectangle object)
	 * @return
	 */
	public Rectangle getBoundingRect(boolean world, boolean includeAA,
			Rectangle output)
	{
		return LttlMath.GetAABB(getVerticesPosMain(world, tmpV2Array), output);
	}

	/**
	 * Returns the local bounding rect for this mesh since the last modification. If called and no modifications, will
	 * return the cached rect from last calculation.<br>
	 * Note: Does not includeAA points
	 * 
	 * @return
	 */
	public Rectangle getBoundingRectCached()
	{
		if (boundingRectCached == null)
		{
			boundingRectCached = new Rectangle();
		}
		if (needToUpdateBoundingRect)
		{
			if (getVertexCount() == 0)
			{
				Lttl.logNote("Mesh: no vertices to create bounding rect.");
				return boundingRectCached;
			}
			else
			{
				LttlMath.GetAABB(getVerticesPosMain(false, tmpV2Array),
						boundingRectCached);
				needToUpdateBoundingRect = false;
			}
		}
		return boundingRectCached;
	}

	/**
	 * Returns/updates the rectangle to be an AABB of {@link #getBoundingRectTransformed(Matrix3)}.
	 * 
	 * @param transformMatrix
	 * @param output
	 * @return
	 */
	public Rectangle getBoundingRectTransformedAxisAligned(
			Matrix3 transformMatrix, Rectangle output)
	{
		return LttlMath.GetAABB(
				getBoundingRectTransformed(transformMatrix, null), output);
	}

	/**
	 * Returns a float array
	 * 
	 * @param transformMatrix
	 * @param container
	 *            if null will create a new float array, needs to be a size of 8
	 * @return
	 */
	public float[] getBoundingRectTransformed(Matrix3 transformMatrix,
			float[] container)
	{
		return LttlMath.TransformRectangle(
				tmpRect.set(getBoundingRectCached()), transformMatrix,
				container);
	}

	/**
	 * <b>If animating the mesh UVS, use LttlRenderer's Shader UVs.</b> <br>
	 * Checks if transformingUVs is even necessary.
	 * 
	 * @param angle
	 * @param scale
	 * @param offset
	 */
	public void transformUVs(float angle, float scaleX, float scaleY,
			float offsetX, float offsetY)
	{
		transformUVs(verticesArray, angle, scaleX, scaleY, offsetX, offsetY);
	}

	public void transformUVs(UVMeshSettings settings)
	{
		transformUVs(verticesArray, settings);
	}

	public void centerVertices()
	{
		centerVertices(verticesArray);
	}

	public void clear()
	{
		lastMeshUpdateColor = null;
		lastMeshUpdateWorldAlpha = Float.NEGATIVE_INFINITY;
		verticesArray.clear();
		indicesArray.clear();
		worldVerticesArray.clear();
		holesIndexArray.clear();
		aaVerticeIndex = -1;
		aaIndiceIndex = -1;
	}

	public int getIndiceCount()
	{
		return indicesArray.size;
	}

	public int getTriangleCount()
	{
		return indicesArray.size / 3;
	}

	/* STATIC METHODS */

	public static void addVertice(FloatArray verticesArray, Vector2 pos,
			Vector2 uv, float color, float alpha)
	{
		addVertice(verticesArray, pos, uv.x, uv.y, color, alpha);
	}

	public static void addVertice(FloatArray verticesArray, Vector2 pos,
			float u, float v, float color, float alpha)
	{
		addVertice(verticesArray, pos.x, pos.y, u, v, color, alpha);
	}

	public static Vector2 getUV(FloatArray vertices, int index, Vector2 result)
	{
		return result.set(getU(vertices, index), getV(vertices, index));
	}

	public static Vector2 getPos(FloatArray vertices, int index, Vector2 result)
	{
		return result.set(getX(vertices, index), getY(vertices, index));
	}

	public static float getX(FloatArray vertices, int index)
	{
		return get(vertices, index, LttlMesh.ATTRIBUTE_INDEX_POSX);
	}

	public static float getY(FloatArray vertices, int index)
	{
		return get(vertices, index, LttlMesh.ATTRIBUTE_INDEX_POSY);
	}

	public static float getU(FloatArray vertices, int index)
	{
		return get(vertices, index, LttlMesh.ATTRIBUTE_INDEX_UVX);
	}

	public static float getV(FloatArray vertices, int index)
	{
		return get(vertices, index, LttlMesh.ATTRIBUTE_INDEX_UVY);
	}

	public static float getAlpha(FloatArray vertices, int index)
	{
		return get(vertices, index, LttlMesh.ATTRIBUTE_INDEX_ALPHA);
	}

	public static float getColor(FloatArray vertices, int index)
	{
		return get(vertices, index, LttlMesh.ATTRIBUTE_INDEX_COLOR);
	}

	public static void setX(FloatArray vertices, int index, float value)
	{
		set(vertices, index, LttlMesh.ATTRIBUTE_INDEX_POSX, value);
	}

	public static void setY(FloatArray vertices, int index, float value)
	{
		set(vertices, index, LttlMesh.ATTRIBUTE_INDEX_POSY, value);
	}

	public static void setPos(FloatArray vertices, int index, float x, float y)
	{
		setX(vertices, index, x);
		setY(vertices, index, y);
	}

	public static void offsetPos(FloatArray vertices, int index, float x,
			float y)
	{
		setPos(vertices, index, getX(vertices, index) + x,
				getY(vertices, index) + y);
	}

	public static void setU(FloatArray vertices, int index, float value)
	{
		set(vertices, index, LttlMesh.ATTRIBUTE_INDEX_UVX, value);
	}

	public static void setV(FloatArray vertices, int index, float value)
	{
		set(vertices, index, LttlMesh.ATTRIBUTE_INDEX_UVY, value);
	}

	public static void setUV(FloatArray vertices, int index, float x, float y)
	{
		setU(vertices, index, x);
		setV(vertices, index, y);
	}

	public static void set(FloatArray vertices, int index, float x, float y,
			float u, float v, float color, float alpha)
	{
		setX(vertices, index, x);
		setY(vertices, index, y);
		setU(vertices, index, u);
		setV(vertices, index, v);
		setColor(vertices, index, color);
		setAlpha(vertices, index, alpha);
	}

	public static void addVertice(FloatArray vertices, float x, float y,
			float u, float v, float color, float alpha)
	{
		vertices.ensureCapacity(LttlMesh.VERTICE_ATTRIBUTE_COUNT);
		vertices.add(x);
		vertices.add(y);
		vertices.add(u);
		vertices.add(v);
		vertices.add(color);
		vertices.add(alpha);
	}

	public static void setAlpha(FloatArray vertices, int index, float value)
	{
		set(vertices, index, LttlMesh.ATTRIBUTE_INDEX_ALPHA, value);
	}

	/**
	 * @param vertices
	 * @param value
	 * @param aaIndex
	 *            -1 if no AA
	 */
	public static void setAlphaAll(FloatArray vertices, float value, int aaIndex)
	{
		for (int i = 0, n = aaIndex < 0 ? getVertexCount(vertices) : aaIndex; i < n; i++)
		{
			setAlpha(vertices, i, value);
		}
	}

	public static void setColor(FloatArray vertices, int index, float value)
	{
		set(vertices, index, LttlMesh.ATTRIBUTE_INDEX_COLOR, value);
	}

	public static void setColorAll(FloatArray vertices, float value)
	{
		for (int i = 0, n = getVertexCount(vertices); i < n; i++)
		{
			setColor(vertices, i, value);
		}
	}

	public static int getVertexCount(FloatArray vertices)
	{
		return vertices.size / LttlMesh.VERTICE_ATTRIBUTE_COUNT;
	}

	public static float get(FloatArray vertices, int index, int attribute)
	{
		return vertices.get(index * LttlMesh.VERTICE_ATTRIBUTE_COUNT
				+ attribute);
	}

	public static void set(FloatArray vertices, int index, int attribute,
			float value)
	{
		vertices.set(index * LttlMesh.VERTICE_ATTRIBUTE_COUNT + attribute,
				value);
	}

	public static void centerVertices(FloatArray vertices)
	{
		// get max and min
		float maxX = Float.NEGATIVE_INFINITY;
		float maxY = Float.NEGATIVE_INFINITY;
		float minX = Float.POSITIVE_INFINITY;
		float minY = Float.POSITIVE_INFINITY;
		for (int i = 0, n = getVertexCount(vertices); i < n; i++)
		{
			float x = getX(vertices, i);
			float y = getY(vertices, i);

			maxX = LttlMath.max(maxX, x);
			maxY = LttlMath.max(maxY, y);
			minX = LttlMath.min(minX, x);
			minY = LttlMath.min(minY, y);
		}

		float halfWidth = (maxX - minX) / 2;
		float halfHeight = (maxY - minY) / 2;

		// iterate back through and shift vertices to center
		for (int i = 0, n = getVertexCount(vertices); i < n; i++)
		{
			float x = getX(vertices, i);
			float y = getY(vertices, i);

			// set x and y
			setPos(vertices, i, x - minX - halfWidth, y - minY - halfHeight);
		}
	}

	public static void transformUVs(FloatArray vertices, UVMeshSettings settings)
	{
		transformUVs(vertices, settings.angle, settings.scale.x,
				settings.scale.y, settings.offset.x, settings.offset.y);
	}

	/**
	 * <b>If animating the mesh UVS, use LttlRenderer's Shader UVs.</b> <br>
	 * Checks if transformingUVs is even necessary.
	 * 
	 * @param angle
	 * @param scale
	 * @param offset
	 */
	public static void transformUVs(FloatArray vertices, float angle,
			float scaleX, float scaleY, float offsetX, float offsetY)
	{
		// EARLY OUT
		if (angle == 0 && scaleX == 1 && scaleY == 1 && offsetX == 0
				&& offsetY == 0) { return; }

		float minX = Float.POSITIVE_INFINITY;
		float minY = Float.POSITIVE_INFINITY;
		float maxX = Float.NEGATIVE_INFINITY;
		float maxY = Float.NEGATIVE_INFINITY;

		// apply scale and offset and caclulate max and mins
		for (int i = 0, n = getVertexCount(vertices); i < n; i++)
		{
			float x = (getU(vertices, i) + offsetX) * scaleX;
			float y = (getV(vertices, i) + offsetY) * scaleY;

			if (angle != 0)
			{
				minX = LttlMath.min(minX, x);
				maxX = LttlMath.max(maxX, x);
				minY = LttlMath.min(minY, y);
				maxY = LttlMath.max(maxY, y);
			}

			setUV(vertices, i, x, y);
		}

		// rotate points around center UV
		if (angle != 0)
		{
			float centerX = minX + ((maxX - minX) / 2);
			float centerY = minY + ((maxY - minY) / 2);

			for (int i = 0, n = getVertexCount(vertices); i < n; i++)
			{
				LttlMath.RotateAroundPoint(getU(vertices, i),
						getV(vertices, i), centerX, centerY, angle, tmp);
				setUV(vertices, i, tmp.x, tmp.y);
			}
		}

	}

	public static LttlMesh getNew(LttlMesh mesh, FloatArray vertices,
			ShortArray indices)
	{
		if (mesh == null)
		{
			mesh = new LttlMesh(vertices, indices);
		}
		else
		{
			mesh.clear();
			mesh.getVerticesArray().addAll(vertices);
			mesh.getIndicesArray().addAll(indices);
		}
		return mesh;
	}

	public static LttlMesh getNew(LttlMesh mesh, int initialVerticeCapacity)
	{
		if (mesh == null)
		{
			mesh = new LttlMesh(initialVerticeCapacity);
		}
		else
		{
			mesh.clear();
			mesh.ensureCapacity(initialVerticeCapacity);
		}
		return mesh;
	}

	public static LttlMesh getNew(LttlMesh mesh)
	{
		if (mesh == null)
		{
			mesh = new LttlMesh(4);
		}
		else
		{
			mesh.clear();
			mesh.ensureCapacity(4);
		}
		return mesh;
	}

	/**
	 * Returns an array of the vertice positions within given range.
	 * 
	 * @param vertices
	 * @param startIndex
	 *            in terms of vertices, not vertice components
	 * @param endIndex
	 *            inclusive
	 * @param container
	 * @return
	 */
	public static Vector2Array getVerticesPos(FloatArray vertices,
			int startIndex, int endIndex, Vector2Array container)
	{
		container.clear();
		container.ensureCapacity(endIndex - startIndex + 1);

		for (int i = startIndex; i <= endIndex; i++)
		{
			container.add(getX(vertices, i), getY(vertices, i));
		}

		return container;
	}

	/**
	 * Returns an array of the vertice positions.
	 * 
	 * @param vertices
	 * @param container
	 * @return
	 */
	public static Vector2Array getVerticesPos(FloatArray vertices,
			Vector2Array container)
	{
		return getVerticesPos(vertices, 0, getVertexCount(vertices) - 1,
				container);
	}

	/**
	 * Adds 'renderedMeshes', 'rendereredTriangles', and 'rendereredVertices' to LttlProfiler for this mesh.<br>
	 * Call this for each mesh that is rendered.
	 * 
	 * @param mesh
	 */
	public static void profileMesh(Mesh mesh)
	{
		LttlProfiler.renderedMeshes.add();

		// reminder: this includes AA points
		LttlProfiler.renderedTriangles.add(mesh.getNumIndices() / 3);
		LttlProfiler.renderedVertices.add(mesh.getNumVertices());
	}

	public static void ensureCapacity(FloatArray vertices, int verticeCapacity)
	{
		vertices.ensureCapacity(verticeCapacity
				* LttlMesh.VERTICE_ATTRIBUTE_COUNT);
	}

	/**
	 * Should be called whenever a mesh has been modified.<br>
	 * 1. updated Id<br>
	 * 2. clear the last updated color and alpha values, forcing them to update next render.<br>
	 * 3. Force the bounding rect to be generated on next call
	 */
	public void modified()
	{
		updateId++;

		// reset some modified states
		needToUpdateBoundingRect = true;
		lastMeshUpdateWorldAlpha = Float.NEGATIVE_INFINITY;
		lastMeshUpdateColor = null;
	}

	/**
	 * Does the mesh using AA
	 * 
	 * @return
	 */
	public boolean hasAA()
	{
		return aaVerticeIndex > 0;
	}

	/**
	 * This is the index that the AA vertices start, after main mesh vertices and holes. -1 means no AA, use
	 * {@link #hasAA()} before.
	 */
	public int getAAVerticeIndex()
	{
		return aaVerticeIndex;
	}

	/**
	 * @see #getAAVerticeIndex()
	 */
	public void setAAVerticeIndex(int index)
	{
		aaVerticeIndex = index;
	}

	/**
	 * This is the index that the AA indices start at. -1 means no AA, use {@link #hasAA()} before.
	 */
	public int getAAIndiceIndex()
	{
		return aaIndiceIndex;
	}

	/**
	 * @see #getAAIndiceIndex()
	 */
	public void setAAIndiceIndex(int index)
	{
		aaIndiceIndex = index;
	}

	/**
	 * Returns the number of holes in this mesh
	 * 
	 * @return
	 */
	public int getHolesCount()
	{
		return holesIndexArray.size;
	}

	/**
	 * Returns an array of each hole's start index in this mesh. The length of the hole's vertices can be to the next
	 * hole or to the end of the vertices.<br>
	 * Holes are after main mesh vertices and before AA vertices.<br>
	 * This is useful for when you only want the non holes points, like when calculating a bounding rect.
	 * 
	 * @return
	 */
	public IntArray getHolesIndexArray()
	{
		return holesIndexArray;
	}

	@Override
	public String toString()
	{
		return "Mesh Details: vertices[" + getVertexCount() + "] indices["
				+ getIndiceCount() + "]";
	}

	/**
	 * Creates a polygon.
	 * 
	 * @return null if no related vertices
	 */
	public PolygonContainer generatePolygon(boolean world)
	{
		if ((world && worldVerticesArray.size == 0)
				|| (!world && verticesArray.size == 0)) return null;

		return new PolygonContainer(this, world);
	}

	public void clearAA()
	{
		if (aaVerticeIndex > -1)
		{
			if (aaVerticeIndex * LttlMesh.VERTICE_ATTRIBUTE_COUNT < verticesArray.size)
			{
				verticesArray.removeRange(aaVerticeIndex
						* LttlMesh.VERTICE_ATTRIBUTE_COUNT,
						verticesArray.size - 1);
			}
		}
		if (aaIndiceIndex > -1)
		{
			if (aaIndiceIndex < indicesArray.size)
			{
				indicesArray.removeRange(aaIndiceIndex, indicesArray.size - 1);
			}
		}
		aaVerticeIndex = -1;
		aaIndiceIndex = -1;
	}
}
