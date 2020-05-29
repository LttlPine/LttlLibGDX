package com.lttlgames.graphics;

import java.util.ArrayList;
import java.util.List;

import org.poly2tri.Poly2Tri;
import org.poly2tri.geometry.polygon.Polygon;
import org.poly2tri.geometry.polygon.PolygonPoint;
import org.poly2tri.triangulation.TriangulationPoint;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.ShortArray;
import com.lttlgames.editor.Lttl;
import com.lttlgames.helpers.LttlGeometryUtil.PolygonContainer;
import com.lttlgames.helpers.LttlMath;
import com.lttlgames.helpers.LttlProfiler;
import com.lttlgames.helpers.Vector2Array;

public final class LttlTriangulator
{
	private LttlTriangulator()
	{

	}

	/**
	 * Decides if should triangulate using
	 * {@link #DelaunayTriangulate(Vector2Array, ArrayList, Vector2Array, ShortArray, IntArray)} or
	 * {@link #Triangulate(Vector2Array, ShortArray)} based on if there are any holes or forcing delaunay.
	 * 
	 * @param polyCont
	 * @param pointsContainer
	 * @param indicesContainer
	 * @param holesIndexArray
	 *            can be null
	 * @param delaunay
	 *            if false, but has holes, will do it anyway
	 */
	public static void TriangulateEither(PolygonContainer polyCont,
			Vector2Array pointsContainer, ShortArray indicesContainer,
			IntArray holesIndexArray, boolean delaunay)
	{
		if (delaunay || polyCont.getHoles().size() > 0)
		{
			try
			{
				LttlTriangulator.DelaunayTriangulate(polyCont, pointsContainer,
						indicesContainer, holesIndexArray);
			}
			catch (RuntimeException e)
			{
				Lttl.logNote("Delaunay Triangulation Failed because '"
						+ e.getMessage() + "'.");
				return;
			}
		}
		else
		{
			pointsContainer.clear();
			pointsContainer.addAll(polyCont.getPoints());
			LttlTriangulator.Triangulate(pointsContainer, indicesContainer);
		}
	}

	/**
	 * Combines a list of two paths with equal number of points in each, and sews them together, optionally can treat
	 * them as if they are closed, making a donut.
	 * 
	 * @see LttlTriangulator#TriangulateColumn(int, ShortArray, boolean, boolean)
	 */
	public static void TriangulateColumn(Vector2Array points,
			ShortArray indicesContainer, boolean closed, boolean sameDirection)
	{
		// must have at least 2 poins in each side
		Lttl.Throw(points.size() < 4);
		TriangulateColumn(points.size(), indicesContainer, closed,
				sameDirection);
	}

	/**
	 * Generates indices for a mesh that has a column structure where each vertice has a unique pair to triangulate
	 * with.<br>
	 * It is assumes the first "side" of vertices are first, then all the vertices for the second "side" are afterward.
	 * 
	 * @param numPoints
	 * @param indicesContainer
	 * @param sameDirection
	 *            are the vertices on each "side" going in same direction
	 * @param closed
	 *            should it make the column into a donut
	 */
	public static void TriangulateColumn(int numPoints,
			ShortArray indicesContainer, boolean closed, boolean sameDirection)
	{
		// must be even
		Lttl.Throw(numPoints % 2 != 0);

		int half = numPoints / 2;
		int full = numPoints - 1;
		int n = numPoints / 2;
		for (int i = 0; i < n - (closed ? 0 : 1); i++)
		{
			int plusOne = i + 1 == n ? 0 : i + 1;

			indicesContainer.add(i);
			indicesContainer.add(sameDirection ? i + half : full - i);
			indicesContainer.add(plusOne);

			indicesContainer.add(sameDirection ? i + half : full - i);
			indicesContainer.add(sameDirection ? plusOne + half : full
					- plusOne);
			indicesContainer.add(plusOne);
		}
	}

	/**
	 * This is only meant for cases where you are very sure the points and holes are valid. see
	 * {@link #DelaunayTriangulate(PolygonContainer, Vector2Array, ShortArray, IntArray)}
	 * 
	 * @param points
	 * @param holes
	 *            hole paths can't be overlapping or touching edge of main points or outside of it
	 * @param pointsContainer
	 * @param indicesContainer
	 * @param holeIndexes
	 *            can be null
	 */
	public static void DelaunayTriangulate(Vector2Array points,
			ArrayList<Vector2Array> holes, Vector2Array pointsContainer,
			ShortArray indicesContainer, IntArray holeIndexes)
	{
		LttlProfiler.triangulations.add();

		// create main polygon
		Polygon main = toPolygon(points);
		ArrayList<TriangulationPoint> allPoints = new ArrayList<TriangulationPoint>(
				main.getPoints());

		// create and add any holes polygon
		if (holeIndexes != null)
		{
			holeIndexes.clear();
		}
		if (holes.size() > 0)
		{
			for (Vector2Array hole : holes)
			{
				Polygon hp = toPolygon(hole);
				if (holeIndexes != null)
				{
					holeIndexes.add(allPoints.size());
				}
				allPoints.addAll(hp.getPoints());
				main.addHole(hp);
			}
		}

		// triangulate
		Poly2Tri.triangulate(main);

		// populate vertices
		pointsContainer.clear();
		pointsContainer.ensureCapacity(allPoints.size());
		for (TriangulationPoint p : allPoints)
		{
			pointsContainer.add(p.getXf(), p.getYf());
		}

		// populate indices
		List<DelaunayTriangle> triangles = main.getTriangles();
		indicesContainer.clear();
		indicesContainer.ensureCapacity(triangles.size() * 3);
		for (DelaunayTriangle tri : main.getTriangles())
		{
			indicesContainer.add(allPoints.indexOf(tri.points[0]));
			indicesContainer.add(allPoints.indexOf(tri.points[1]));
			indicesContainer.add(allPoints.indexOf(tri.points[2]));
		}
	}

	/**
	 * Good for Polygons with holes, more uniform triangles. <br>
	 * <br>
	 * Benchmark: Compared to {@link #Triangulate} the speeds seem to be slower with polygons with a lower vertice count
	 * than the normal triangulator, while it is as fast as normal Triangulator if polygon has a high vertice count.
	 * Efficiency is probably do to the heavier object creation and geometry calculations.
	 * 
	 * @param source
	 * @param pointsContainer
	 * @param indicesContainer
	 * @param holeIndexes
	 *            can be null
	 */
	public static void DelaunayTriangulate(PolygonContainer source,
			Vector2Array pointsContainer, ShortArray indicesContainer,
			IntArray holeIndexes)
	{
		DelaunayTriangulate(source.getPoints(), source.getHoles(),
				pointsContainer, indicesContainer, holeIndexes);
	}

	private static Polygon toPolygon(Vector2Array points)
	{
		PolygonPoint[] array = new PolygonPoint[points.size()];
		for (int i = 0, n = points.size(); i < n; i++)
		{
			// OPTIMIZE PolygonPoint pool
			array[i] = new PolygonPoint(points.getX(i), points.getY(i));
		}
		return new Polygon(array);
	}

	// temp variables
	private static IntArray intArrayTemp = new IntArray();
	private static Vector2 v2SnipAtemp = new Vector2();
	private static Vector2 v2SnipBtemp = new Vector2();
	private static Vector2 v2SnipCtemp = new Vector2();
	private static Vector2 v2SnipPtemp = new Vector2();

	/**
	 * Good for fast triangulation, does not allow holes. <br>
	 * <br>
	 * Benchmark:Faster than {@link #DelaunayTriangulate(PolygonContainer, Vector2Array, ShortArray)} with lower
	 * vertices. About the same speed for very large amounts of vertices.
	 * 
	 * @param points
	 * @return sharedIndicesArray
	 */
	public static void Triangulate(final Vector2Array points,
			ShortArray indicesContainer)
	{
		LttlProfiler.triangulations.add();

		indicesContainer.clear();
		indicesContainer
				.ensureCapacity(LttlMath.max((points.size() - 2) * 3, 3));

		int n = points.size();
		if (n < 3)
		{
			indicesContainer.add(0);
			indicesContainer.add(1);
			indicesContainer.add(2);
			return;
		}

		IntArray V = intArrayTemp;
		V.clear(); // sets size to 0
		V.ensureCapacity(n);

		if (Area(points) > 0)
		{
			for (int v = 0; v < n; v++)
				V.add(v);
		}
		else
		{
			for (int v = 0; v < n; v++)
				V.add((n - 1) - v);
		}

		int nv = n;
		int count = 2 * nv;
		for (int v = nv - 1; nv > 2;)
		{
			if ((count--) <= 0) return;

			int u = v;
			if (nv <= u) u = 0;
			v = u + 1;
			if (nv <= v) v = 0;
			int w = v + 1;
			if (nv <= w) w = 0;

			if (Snip(u, v, w, nv, V, points))
			{
				int a, b, c, s, t;
				a = V.get(u);
				b = V.get(v);
				c = V.get(w);
				indicesContainer.add(a);
				indicesContainer.add(b);
				indicesContainer.add(c);
				for (s = v, t = v + 1; t < nv; s++, t++)
					V.set(s, V.get(t));
				nv--;
				count = 2 * nv;
			}
		}

		indicesContainer.reverse();
	}

	private static float Area(Vector2Array points)
	{
		int n = points.size();
		float A = 0.0f;
		for (int p = n - 1, q = 0; q < n; p = q++)
		{
			A += points.getX(p) * points.getY(q) - points.getX(q)
					* points.getY(p);
		}
		return (A * 0.5f);
	}

	private static boolean Snip(int u, int v, int w, int n, IntArray V,
			Vector2Array points)
	{
		int p;
		Vector2 A = points.get(V.get(u), v2SnipAtemp);
		Vector2 B = points.get(V.get(v), v2SnipBtemp);
		Vector2 C = points.get(V.get(w), v2SnipCtemp);
		if (LttlMath.EPSILON > (((B.x - A.x) * (C.y - A.y)) - ((B.y - A.y) * (C.x - A.x))))
			return false;
		for (p = 0; p < n; p++)
		{
			if ((p == u) || (p == v) || (p == w)) continue;
			Vector2 P = points.get(V.get(p), v2SnipPtemp);
			if (InsideTriangle(A, B, C, P)) return false;
		}
		return true;
	}

	private static boolean InsideTriangle(Vector2 A, Vector2 B, Vector2 C,
			Vector2 P)
	{
		float ax, ay, bx, by, cx, cy, apx, apy, bpx, bpy, cpx, cpy;
		float cCROSSap, bCROSScp, aCROSSbp;

		ax = C.x - B.x;
		ay = C.y - B.y;
		bx = A.x - C.x;
		by = A.y - C.y;
		cx = B.x - A.x;
		cy = B.y - A.y;
		apx = P.x - A.x;
		apy = P.y - A.y;
		bpx = P.x - B.x;
		bpy = P.y - B.y;
		cpx = P.x - C.x;
		cpy = P.y - C.y;

		aCROSSbp = ax * bpy - ay * bpx;
		cCROSSap = cx * apy - cy * apx;
		bCROSScp = bx * cpy - by * cpx;

		return ((aCROSSbp >= 0.0f) && (bCROSScp >= 0.0f) && (cCROSSap >= 0.0f));
	}
}