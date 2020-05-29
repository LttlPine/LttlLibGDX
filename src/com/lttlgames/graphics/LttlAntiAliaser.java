package com.lttlgames.graphics;

import java.util.ArrayList;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.ShortArray;
import com.lttlgames.editor.AASettings;
import com.lttlgames.editor.Lttl;
import com.lttlgames.exceptions.MultiplePolygonsException;
import com.lttlgames.helpers.LttlGeometryUtil;
import com.lttlgames.helpers.LttlGeometryUtil.PolygonContainer;
import com.lttlgames.helpers.LttlMath;
import com.lttlgames.helpers.LttlProfiler;
import com.lttlgames.helpers.Vector2Array;

public final class LttlAntiAliaser
{
	// prevent instantiation
	private LttlAntiAliaser()
	{
	}

	private static Vector2Array basePoints = new Vector2Array();
	private static Vector2Array aaPoints = new Vector2Array();
	private static Vector2Array vec2ContainerC = new Vector2Array();
	private static IntArray relationships = new IntArray();
	private static ShortArray indices = new ShortArray();
	private static Vector2 tmp = new Vector2();
	private static Vector2 aaPoint = new Vector2();
	private static PolygonContainer polyContainer;
	private static ArrayList<Vector2Array> holes = new ArrayList<Vector2Array>(
			0);

	/**
	 * Uses AA settings to determine which AA to do.
	 * 
	 * @param mesh
	 * @param width
	 * @param settings
	 */
	public static void AddAntiAliasingEither(LttlMesh mesh, float width,
			AASettings settings)
	{
		if (settings.useSimple)
		{
			AddAntiAliasingSimple(mesh, width, settings.simpleCleanup,
					settings.mitreRatioLimitOverride);
		}
		else
		{
			AddAntiAliasing(mesh, width, settings.mitreRatioLimitOverride);
		}
	}

	/**
	 * The mesh vertices need to be in continious order and have holes specified, if any.<br>
	 * Duplicates the original vertices, does not sew the AA vertices to the original.<br>
	 * Will fail if AA intersects itself, only happens if very close to touching.<br>
	 * A possible hack if animating a mesh and it crosses into itself is to always create a new mesh and when the new
	 * mesh doesn't have AA, just don't update it, so for a frame or two it will have the same mesh. <br>
	 * Note: Recommended to check for self intersection between holes and outer shell and each shell and hole with
	 * itself.
	 * 
	 * @param mesh
	 * @param width
	 * @param mitreRatioLimit
	 *            if greater than 0 overrides the global setting
	 * @throws MultiplePolygonsException
	 */
	public static void AddAntiAliasing(LttlMesh mesh, float width,
			float mitreRatioLimit)
	{
		mesh.clearAA();

		width = Math.abs(width);
		int aaIndex = mesh.getVertexCount();

		FloatArray verticesArray = mesh.getVerticesArray();
		ShortArray indicesArray = mesh.getIndicesArray();
		IntArray holeIndexArray = mesh.getHolesIndexArray();

		mesh.setAAVerticeIndex(aaIndex);
		mesh.setAAIndiceIndex(indicesArray.size);
		int baseStartIndex = 0;
		int endIndex = holeIndexArray.size > 0 ? holeIndexArray.get(0) - 1
				: aaIndex - 1;
		int holeIndex = 0;
		try
		{
			while (true)
			{
				int verticeCountStartingThisIteration = mesh.getVertexCount();

				LttlMesh.getVerticesPos(verticesArray, baseStartIndex,
						endIndex, basePoints);

				// if holeIndex > 0 then this is a hole, so actually do a shrink
				polyContainer = LttlGeometryUtil.offsetPolygon(basePoints,
						holeIndex > 0 ? -width : width, Joint.MITER, 1,
						mitreRatioLimit, polyContainer);
				if (polyContainer.getHoles().size() == 0)
				{
					aaPoints.set(polyContainer.getPoints());
				}
				else
				{
					throw new MultiplePolygonsException("holes");
				}

				// triangulate the new outerpoints which are the anti aliaising, can't sew the original mesh and the AA
				// together by connecting indices because they may not match up one to one, and it's just complicated
				if (aaPoints.size() > 2)
				{
					holes.clear();
					holes.add(basePoints);

					try
					{
						LttlTriangulator.DelaunayTriangulate(aaPoints, holes,
								vec2ContainerC, indices, null);
					}
					catch (RuntimeException e)
					{
						Lttl.logNote("AntiAliasing Failed because '"
								+ e.getMessage() + "'.");
						return;
					}

					int basePointSize = basePoints.size();
					int outerPointsSize = aaPoints.size();
					LttlMesh.ensureCapacity(verticesArray, aaPoints.size());

					// add all the outer points, which are the new aa points
					for (int i = 0, n = aaPoints.size(); i < n; i++)
					{
						// get aa point
						aaPoints.get(i, aaPoint);

						// a hack to get the mesh vertex that is most associated with this AA point, works for most part
						int indexPair = 0;
						float dstMin = Float.MAX_VALUE;
						for (int ii = 0; ii < basePointSize; ii++)
						{
							float dst = aaPoint.dst2(basePoints.getX(ii),
									basePoints.getY(ii));
							if (dst < dstMin)
							{
								indexPair = baseStartIndex + ii;
								dstMin = dst;
							}
						}

						// get real UV
						mesh.getUV(indexPair, tmp);
						mesh.addVertice(aaPoint, tmp, mesh.getColor(indexPair),
								0);
					}

					// add indices
					indicesArray.ensureCapacity(indices.size);
					for (int i = 0, n = indices.size; i < n; i++)
					{
						// the indices are relative to the past triangulation, they start from the outerpoints and then
						// go to the base points
						int ind = indices.get(i);

						// the indice index is referencing one of the base points
						if (ind >= outerPointsSize)
						{
							ind = ind - outerPointsSize + baseStartIndex;
						}
						else
						{
							// if the indice index is referencing one of the outer points then shift it to take into
							// account all the vertices
							ind += verticeCountStartingThisIteration;
						}
						indicesArray.add(ind);
					}
				}

				// prepare for next iteration
				baseStartIndex = endIndex + 1;
				holeIndex++;
				if (holeIndex > holeIndexArray.size)
				{
					break;
				}
				endIndex = holeIndex == holeIndexArray.size ? aaIndex - 1
						: holeIndexArray.get(holeIndex) - 1;
			}
		}
		catch (MultiplePolygonsException e)
		{
			if (e.getMessage().equals("holes"))
			{
				Lttl.logNote("AntiAliaser Failed: holes generated.");
			}
			else
			{
				Lttl.logNote("AntiAliaser Failed: multiple polygons generated.");
			}

			// clean up as if we didn't add any AA
			mesh.clearAA();
		}

		basePoints.clear();
		aaPoints.clear();
		vec2ContainerC.clear();
		holes.clear();
	}

	/**
	 * @param mesh
	 *            clears any AA values it has first
	 * @param width
	 * @param cleanup
	 *            if true, will try and cleanup any points that are self intersecting, not usually necessary
	 * @param mitreRatioLimit
	 *            if greater than, then overrides the global setting
	 */
	static public void AddAntiAliasingSimple(LttlMesh mesh, float width,
			boolean cleanup, float mitreRatioLimit)
	{
		// clear all AA
		mesh.clearAA();

		width = Math.abs(width);
		int originalVertexCount = mesh.getVertexCount();

		FloatArray verticesArray = mesh.getVerticesArray();
		ShortArray indicesArray = mesh.getIndicesArray();
		IntArray holeIndexArray = mesh.getHolesIndexArray();
		relationships.clear();

		mesh.setAAVerticeIndex(originalVertexCount);
		mesh.setAAIndiceIndex(indicesArray.size);
		int baseStartIndex = 0;
		int endIndex = holeIndexArray.size > 0 ? holeIndexArray.get(0) - 1
				: originalVertexCount - 1;
		int holeIndex = 0;

		while (true)
		{
			int verticeCountStartingThisIteration = mesh.getVertexCount();

			LttlMesh.getVerticesPos(verticesArray, baseStartIndex, endIndex,
					basePoints);

			// if holeIndex > 0 then this is a hole, so actually do a shrink, flip if clockwise
			float iWidth = holeIndex > 0 ? -width : width;
			LttlGeometryUtil.offsetPath(basePoints, true,
					basePoints.isCounterClockwise(), iWidth, mitreRatioLimit,
					cleanup, aaPoints, relationships);

			// ensure we can add all the new aa points without resize
			LttlMesh.ensureCapacity(verticesArray, aaPoints.size());

			// triangulate the new aa points
			for (int i = 0, n = aaPoints.size(); i < n; i++)
			{
				// define the index to the original point that this aaPoint is related to
				// offset since it could be a hole of some sort
				int relIndex = baseStartIndex + relationships.get(i);
				int relIndexNext = baseStartIndex
						+ LttlMath.loopIndex(relationships.get(i) + 1,
								basePoints.size());

				// get aa point
				aaPoints.get(i, aaPoint);

				// get real UV adn color form related
				mesh.getUV(relIndex, tmp);
				mesh.addVertice(aaPoint, tmp, mesh.getColor(relIndex), 0);

				// add indices
				int next = LttlMath.loopIndex(i + 1, n);
				indicesArray.add(verticeCountStartingThisIteration + i);
				indicesArray.add(relIndex);
				indicesArray.add(verticeCountStartingThisIteration + next);

				// check if this is the first aa point to a bevel corner, since a bevel corner has two aa points that
				// are related to same original point
				boolean isBevel = relationships.get(i) == relationships
						.get(next);
				if (!isBevel)
				{
					indicesArray.add(verticeCountStartingThisIteration + next);
					indicesArray.add(relIndex);
					indicesArray.add(relIndexNext);
				}
			}

			// prepare for next iteration
			baseStartIndex = endIndex + 1;
			holeIndex++;
			if (holeIndex > holeIndexArray.size)
			{
				break;
			}
			// if on last hole, get the rest of the original vertices
			endIndex = holeIndex == holeIndexArray.size ? originalVertexCount - 1
					: holeIndexArray.get(holeIndex) - 1;
		}

		relationships.clear();
		basePoints.clear();
		aaPoints.clear();

		LttlProfiler.aaUpdates.add();
	}
}
