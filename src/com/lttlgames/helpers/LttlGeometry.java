package com.lttlgames.helpers;

import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Intersector.MinimumTranslationVector;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.lttlgames.editor.Lttl;
import com.lttlgames.helpers.LttlGeometryUtil.PolygonContainer;

/**
 * Class with methods to do many geometry operations on these types only:<br>
 * points (2 floats or {@link Vector2})<br>
 * lines (4 floats or 2 {@link Vector2})<br>
 * line segments (4 floats or 2 {@link Vector2})<br>
 * circles ({@link Circle})<br>
 * rectangles ({@link Rectangle})<br>
 * polygons (float[] or {@link Vector2Array}). Use {@link LttlGeometryUtil} directly for polygon with holes (
 * {@link PolygonContainer})<br>
 * <br>
 * Definitions:<br>
 * Intersects- there is at least one common point on the perimeter of the two shapes.<br>
 * Contains- all of geometryB is contained inside geometryA and they do not intersect.<br>
 * Overlaps- the shapes intersect or one is contained in the other<br>
 * <br>
 * Note: For many complex operations that are using the same geometry may benefit in efficiency and less allocation by
 * interacting with {@link LttlGeometryUtil} and {@link Intersector} and {@link LttlMath} directly as needed.
 */
public class LttlGeometry
{
	static private float[] rectArray = new float[8];
	static private Vector2 tmp0 = new Vector2();
	static private Vector2 tmp1 = new Vector2();
	static private Vector2 tmp2 = new Vector2();
	static private Vector2 tmp3 = new Vector2();

	// TODO method for checking if a polygon is concave
	// TODO google for fast alternative for concave polygon intersection, otherwise could check each line segment, also
	// fast concave polygon contains
	// TODO make method called intersectPolygonsCheck which checks if at least one is concave, and if so, send is
	// through a different method, woudl check to see if makes a difference

	/* CONTAINS POINT */

	static public boolean ContainsPointInRectangle(Rectangle rectangle,
			float x, float y)
	{
		return rectangle.contains(x, y);
	}

	static public boolean ContainsPointInCircle(Circle circle, float x, float y)
	{
		return circle.contains(x, y);
	}

	/**
	 * Works for concave and convex polygons.
	 */
	static public boolean ContainsPointInPolygon(float[] polygon, float x,
			float y)
	{
		return Intersector.isPointInPolygon(polygon, 0, polygon.length, x, y);
	}

	/**
	 * @see #ContainsPointInPolygon(float[], float, float)
	 */
	static public boolean ContainsPointInPolygon(float[] polygon, Vector2 point)
	{
		return ContainsPointInPolygon(polygon, point.x, point.y);
	}

	/**
	 * @see #ContainsPointInPolygon(float[], float, float)
	 */
	static public boolean ContainsPointInPolygon(Vector2Array polygon, float x,
			float y)
	{
		return Intersector.isPointInPolygon(polygon.getFloatArray().items, 0,
				polygon.getFloatArray().size, x, y);
	}

	static public boolean ContainsRectangleInRectangle(Rectangle outer,
			Rectangle inner)
	{
		return outer.contains(inner);
	}

	static public boolean ContainsRectangleInCircle(Circle outer,
			Rectangle inner)
	{
		// if circle does not contain center, then it can't contain the entire rectangle
		if (!outer.contains(inner.getCenter(tmp0))) return false;

		// check if each corner of rectangle is outside of the circle or on the edge, by comparing square distance from
		// circle center
		LttlMath.GetRectFourCorners(inner, rectArray);
		float r2 = outer.radius * outer.radius;
		tmp0.set(outer.x, outer.y);
		for (int i = 0; i < 4; i++)
		{
			tmp0.set(outer.x, outer.y);
			if (tmp0.dst2(rectArray[i * 2], rectArray[i * 2 + 1]) >= r2) { return false; }
		}
		return true;
	}

	// static public boolean ContainsRectangleInPolygon(float[] outer,
	// Rectangle inner)
	// {
	// // check if all 4 corners are contained inside the polygon, if not then it can't contain the entire
	// LttlMath.GetRectFourCorners(inner, rectArray);
	// for (int i = 0; i < 4; i++)
	// {
	// if (!ContainsPointInPolygon(outer, rectArray[i * 2],
	// rectArray[i * 2 + 1])) { return false; }
	// }
	//
	// //TODO check that rectangle does not intersect the polygon
	// }

	/**
	 * @see #ContainsPointInPolygon(float[], float, float)
	 */
	static public boolean ContainsPointInPolygon(Vector2Array polygon,
			Vector2 point)
	{
		return ContainsPointInPolygon(polygon, point.x, point.y);
	}

	/* OVERLAPS */

	// TODO need to check if the proper CompareOperation is Overlap or Intersects
	static public boolean Overlaps(float[] polygonA, float[] polygonB)
	{
		return LttlGeometryUtil.compare(
				LttlGeometryUtil.createPolygon(polygonA),
				LttlGeometryUtil.createPolygon(polygonB),
				CompareOperation.OVERLAPS);
	}

	/**
	 * Checks if convex polygons overlap, for concave use {@link LttlGeometry#Overlaps(float[], float[])}.<br>
	 * If they do, optionally obtain a Minimum Translation Vector indicating the minimum magnitude vector required to
	 * push polygonA out of collision with polygonB.
	 * 
	 * @param polygonA
	 * @param polygonB
	 * @param mtv
	 *            A Minimum Translation Vector to fill in the case of a collision, or null (optional).
	 * @return if they overlap
	 */
	static public boolean OverlapsConvex(float[] polygonA, float[] polygonB,
			MinimumTranslationVector mtv)
	{
		return Intersector.overlapConvexPolygons(polygonA, 0, polygonA.length,
				polygonB, 0, polygonB.length, mtv);
	}

	/**
	 * @see #OverlapsConvex(float[], float[], MinimumTranslationVector)
	 */
	static public boolean OverlapsConvex(Vector2Array polygonA,
			Vector2Array polygonB, MinimumTranslationVector mtv)
	{
		return Intersector.overlapConvexPolygons(
				polygonA.getFloatArray().items, 0,
				polygonA.getFloatArray().size, polygonB.getFloatArray().items,
				0, polygonB.getFloatArray().size, mtv);
	}

	/**
	 * @see #OverlapsConvex(float[], float[], MinimumTranslationVector)
	 */
	static public boolean OverlapsConvex(float[] polygon, Rectangle rectangle,
			MinimumTranslationVector mtv)
	{
		return OverlapsConvex(polygon,
				LttlMath.GetRectFourCorners(rectangle, rectArray), mtv);
	}

	/**
	 * @see #overlapCirclePolygon(Circle, float[])
	 */
	public static boolean overlapCirclePolygon(Circle circle, float[] polygon,
			int offset, int count)
	{
		// First, check if circle center is contained by the polygon, easy solution
		if (ContainsPointInPolygon(polygon, circle.x, circle.y)) { return true; }

		// Second, check if any of the polygon's lines intesect the circle
		for (int i = 0; i < polygon.length; i += 2)
		{
			boolean result = false;
			if (i == 0)
			{
				// need to do something unique for first iteration (line segment that is from last to first)
				result = Intersector.intersectSegmentCircle(tmp0.set(
						polygon[polygon.length - 2],
						polygon[polygon.length - 1]), tmp1.set(polygon[i],
						polygon[i + 1]), tmp2.set(circle.x, circle.y),
						circle.radius * circle.radius);
			}
			else
			{
				result = Intersector.intersectSegmentCircle(
						tmp0.set(polygon[i - 2], polygon[i - 1]),
						tmp1.set(polygon[i], polygon[i + 1]),
						tmp2.set(circle.x, circle.y), circle.radius
								* circle.radius);
			}

			// early out if any intersection is found
			if (result) { return true; }
		}

		// no intersection found
		return false;
	}

	/**
	 * @see #overlapCirclePolygon(Circle, float[])
	 */
	public static boolean overlapCirclePolygon(Circle circle,
			Vector2Array polygon)
	{
		return overlapCirclePolygon(circle, polygon.getFloatArray().items, 0,
				polygon.getFloatArray().size);
	}

	/**
	 * TODO Google for fast solution that works for both concave and convex poolygons ideally<br>
	 * Returns if circle and polygon overlap.
	 * 
	 * @param circle
	 * @param polygon
	 * @return
	 */
	public static boolean overlapCirclePolygon(Circle circle, float[] polygon)
	{
		return overlapCirclePolygon(circle, polygon, 0, polygon.length);
	}

	/**
	 * Returns the nearest point on a path (open or closed)
	 * 
	 * @param path
	 * @param isClosed
	 * @param pos
	 * @param output
	 *            can be same as pos
	 * @return
	 */
	public static Vector2 GetNearestPointOnPath(Vector2Array path,
			boolean isClosed, Vector2 pos, Vector2 output)
	{
		// need atleast two points
		Lttl.Throw(path.size() <= 1);

		float minDst2 = Float.POSITIVE_INFINITY;
		float minX = 0, minY = 0;
		for (int i = 0, n = path.size() + (isClosed ? 0 : -1); i < n; i++)
		{
			int a = i;
			int b = LttlMath.loopIndex(i + 1, path.size());

			Intersector.nearestSegmentPoint(path.getX(a), path.getY(a),
					path.getX(b), path.getY(b), pos.x, pos.y, tmp0);
			float dst2 = pos.dst2(tmp0);
			if (dst2 < minDst2)
			{
				minDst2 = dst2;
				minX = tmp0.x;
				minY = tmp0.y;
			}
		}
		return output.set(minX, minY);
	}

	/**
	 * @see #intersectLinesDetail(Vector2, float, float, float, float, float, float, float, float)
	 */
	public static int intersectLinesDetail(Vector2 out, Vector2 l0p0,
			Vector2 l0p1, Vector2 l1p0, Vector2 l1p1)
	{
		return intersectLinesDetail(out, l0p0.x, l0p0.y, l0p1.x, l0p1.y,
				l1p0.x, l1p0.y, l1p1.x, l1p1.y);
	}

	/**
	 * Calculates the intersection point between two lines and returns some details. Without detail use
	 * {@link Intersector#intersectLines(Vector2, Vector2, Vector2, Vector2, Vector2)}, the speed is pretty similar I
	 * think.
	 * 
	 * @param out
	 *            The Vector2 that will be updated with the intersction point.
	 * @param l1p1X
	 *            Line 0, Point 0, X
	 * @param l1p1Y
	 *            Line 0, Point 0, Y
	 * @param l1p2X
	 *            Line 0, Point 1, X
	 * @param l1p2Y
	 *            Line 0, Point 1, Y
	 * @param l2p1X
	 *            Line 1, Point 0, X
	 * @param l2p1Y
	 *            Line 1, Point 0, Y
	 * @param l2p2X
	 *            Line 1, Point 1, X
	 * @param l2p2Y
	 *            Line 1, Point 1, Y
	 * @return int that describes the result [0 = lines are parallel] [1 = intersection lies inside both segments] [2 =
	 *         lines coincide] [3 = intersection lies outside segment 1] [4 = intersection lies outside segment 2] [5 =
	 *         intersection lies outside both segments]
	 */
	public static int intersectLinesDetail(Vector2 out, float l1p1X,
			float l1p1Y, float l1p2X, float l1p2Y, float l2p1X, float l2p1Y,
			float l2p2X, float l2p2Y)
	{
		double mua, mub;
		double denom, numera, numerb;
		double eps = 0.000000000001;

		denom = (l2p2Y - l2p1Y) * (l1p2X - l1p1X) - (l2p2X - l2p1X)
				* (l1p2Y - l1p1Y);
		numera = (l2p2X - l2p1X) * (l1p1Y - l2p1Y) - (l2p2Y - l2p1Y)
				* (l1p1X - l2p1X);
		numerb = (l1p2X - l1p1X) * (l1p1Y - l2p1Y) - (l1p2Y - l1p1Y)
				* (l1p1X - l2p1X);

		if ((-eps < numera && numera < eps) && (-eps < numerb && numerb < eps)
				&& (-eps < denom && denom < eps))
		{
			out.x = (l1p1X + l1p2X) * 0.5f;
			out.y = (l1p1Y + l1p2Y) * 0.5f;
			return 2; // meaning the lines coincide
		}

		if (-eps < denom && denom < eps)
		{
			out.x = 0;
			out.y = 0;
			return 0; // meaning lines are parallel
		}

		mua = numera / denom;
		mub = numerb / denom;
		out.x = (float) (l1p1X + mua * (l1p2X - l1p1X));
		out.y = (float) (l1p1Y + mua * (l1p2Y - l1p1Y));
		boolean out1 = mua < 0 || mua > 1;
		boolean out2 = mub < 0 || mub > 1;

		if (out1 & out2)
		{
			return 5; // the intersection lies outside both segments
		}
		else if (out1)
		{
			return 3; // the intersection lies outside segment 1
		}
		else if (out2)
		{
			return 4; // the intersection lies outside segment 2
		}
		else
		{
			return 1; // the intersection lies inside both segments
		}
	}

	/**
	 * Returns if the path self intersects; adjacent segments do not count as segments
	 */
	public static boolean intersectsSelf(Vector2Array path, boolean isClosed)
	{
		if (path.size() < 4) return false;

		int size = path.size();
		// always skip last two segments since everything has already been checked with them, and the second to last
		// segment can't intersect the last one because it's adjacent
		for (int i = 0, n = size - (isClosed ? 0 : 1) - 2; i < n; i++)
		{
			Vector2 seg0p0 = path.get(i, tmp0);
			Vector2 seg0p1 = path.get(LttlMath.loopIndex(i + 1, size), tmp1);

			// skip adjecent segments since they share a point and can't intersect for this purpose
			// don't let ii go beyond the path size, since the second segment should always be after the first
			for (int ii = i + 2; ii < size - (isClosed ? 0 : 1); ii++)
			{
				// skip if comparing first segment with last one, since they share a point, it'll always be true
				if (i == 0 && ii == size - 1) continue;

				Vector2 seg1p0 = path.get(ii, tmp2);
				Vector2 seg1p1 = path.get(
						LttlMath.loopIndex(ii + 1, path.size()), tmp3);

				if (Intersector.intersectSegments(seg0p0, seg0p1, seg1p0,
						seg1p1, null)) { return true; }
			}
		}

		return false;
	}
	/* OLD SHIT */
	//
	//
	// ReturnTmpV2Internal0();
	// ReturnTmpV2Internal1();
	// ReturnTmpV2Internal2();
	// return false;
	// }
	//
	// /**
	// * Returns if the circle overlaps the polygon, and optional computes the fast intersection.<br>
	// * Does not check bounding rect.
	// *
	// * @param c
	// * @param p
	// * @param fastIntersection
	// * (optional), if not null, computes intersection which is accurate only on initial collision
	// * @return
	// */
	// public static boolean overlapCirclePolygon(Circle c, Polygon p,
	// Vector2 fastIntersection)
	// {
	// return overlapCirclePolygonValues(c, p.getTransformedVertices(),
	// fastIntersection);
	// }
	//
	// public static boolean overlapCirclePolygonValues(Circle c, float[] values,
	// Vector2 fastIntersection)
	// {
	// LttlProfiler.collisionChecks.add();
	//
	// boolean overlap = false;
	//
	// // check all segments
	// Vector2 pStart = CheckoutTmpV2Internal0();
	// Vector2 pEnd = CheckoutTmpV2Internal1();
	//
	// for (int i = 0; i <= values.length - 2; i += 2)
	// {
	// // get polygon segment
	// if (i == values.length - 2)
	// {
	// // get segment from last to first
	// pStart.set(values[values.length - 2], values[values.length - 1]);
	// pEnd.set(values[0], values[1]);
	// }
	// else
	// {
	// pStart.set(values[i], values[i + 1]);
	// pEnd.set(values[i + 2], values[i + 3]);
	// }
	//
	// // check if segment intersects with circle
	// if (intersectionCircleSegment(c, pStart, pEnd, fastIntersection))
	// {
	// // fastIntsection should be set now
	// overlap = true;
	// break;
	// }
	// }
	//
	// // if tested all segments and still not overlapped, then check to see if maybe it is entirely contained
	// inside
	// // the polygon
	// if (!overlap)
	// {
	// if (Intersector
	// .isPointInPolygon(values, 0, values.length, c.x, c.y))
	// {
	// overlap = true;
	// if (fastIntersection != null)
	// {
	// // since the entire circle is inside the polygon, return the circle's center
	// fastIntersection.set(c.x, c.y);
	// }
	// }
	// }
	//
	// ReturnTmpV2Internal0();
	// ReturnTmpV2Internal1();
	//
	// return overlap;
	// }
	//
	// /**
	// * Checks if line segment intersects circle and optionally computes a fast intersection, which is only
	// accurate as
	// * the circle is intersecting segment at shape's edge.
	// *
	// * @param c
	// * @param start
	// * @param end
	// * @param fastIntersection
	// * (optional) can be null, otherwise if intersecting, will set it to the intersecting point (only
	// * accurate on intersection)
	// * @return
	// */
	// public static boolean intersectionCircleSegment(Circle c, Vector2 start,
	// Vector2 end, Vector2 fastIntersection)
	// {
	// Vector2 cCenter = CheckoutTmpV2Internal2();
	// if (Intersector.intersectSegmentCircle(start, end,
	// cCenter.set(c.x, c.y), c.radius * c.radius))
	// {
	// if (fastIntersection != null)
	// {
	// // if horizontal line
	// if (start.y == end.y)
	// {
	// fastIntersection.set(c.x, start.y);
	// }
	// // if vertical line
	// else if (start.x == end.x)
	// {
	// fastIntersection.set(start.x, c.y);
	// }
	// // else must be slanted
	// else
	// {
	// float oppositeReciprocalSlope = -(end.x - start.x)
	// / (end.y - start.y);
	// Intersector.intersectLines(start.x, start.y, end.x, end.y,
	// c.x, c.y, c.x + 1, c.y + oppositeReciprocalSlope,
	// fastIntersection);
	// }
	// }
	//
	// ReturnTmpV2Internal2();
	// return true;
	// }
	//
	// ReturnTmpV2Internal2();
	// return false;
	// }
	//
	// /**
	// * @param polygon
	// * @param rect
	// * @return
	// */
	// public static boolean overlapConvexPolygonRectangle(Polygon polygon, Rectangle rect)
	// {
	// return Intersector.overlapConvexPolygons(polygon.getVertices()
	// }
	//
	// /**
	// * Checks if rectangle overlaps polygon. (HACK) <br>
	// * Does not check bounding rect.<br>
	// * <br>
	// * Note: This checks polygon points being inside the rect, and the corners of rect being inside the polygon.
	// This
	// is
	// * best for finding initial intersect points. Not true intersection.
	// *
	// * @param r
	// * @param p
	// * @param fastIntersection
	// * (optional) will set to this to the first found fast intersection point, which is primarily accurate on
	// * initial collision, doesn't require any extra computation either way
	// * @return
	// */
	// public static boolean overlapRectanglePolygon(Rectangle r, Polygon p,
	// Vector2 fastIntersection)
	// {
	// LttlProfiler.collisionChecks.add();
	//
	// // check if any of the polygon's points are inside the rectangle
	// float[] polygonPoints = p.getTransformedVertices();
	// for (int i = 0; i < polygonPoints.length; i += 2)
	// {
	// if (r.contains(polygonPoints[i], polygonPoints[i + 1]))
	// {
	// if (fastIntersection != null)
	// {
	// fastIntersection
	// .set(polygonPoints[i], polygonPoints[i + 1]);
	// }
	// return true;
	// }
	// }
	//
	// // check if any of the four rectangle corners are inside the polygon
	// float[] rectPoints = LttlMath.GetRectFourCorners(r);
	// for (int i = 0; i < rectPoints.length; i += 2)
	// {
	// if (PolygonContains(p, rectPoints[i], rectPoints[i + 1]))
	// {
	// if (fastIntersection != null)
	// {
	// fastIntersection.set(rectPoints[i], rectPoints[i + 1]);
	// }
	// return true;
	// }
	// }
	// return false;
	// }
	//
	// /**
	// * Returns if the polygons are intersecting by checking if their points are inside each other. (HACK)<br>
	// * Does not check bounding rect.<br>
	// * This is not true collision detection, since it is only best for checking initial intersections.
	// *
	// * @param p0
	// * @param p1
	// * @param fastIntersection
	// * (optional) will set to this to the first found fast intersection point, which is primarily accurate on
	// * initial collision, extra compution is required if the polygons overlap
	// * @return
	// */
	// public static boolean overlapPolygons(Polygon p0, Polygon p1,
	// Vector2 fastIntersection)
	// {
	// LttlProfiler.collisionChecks.add();
	//
	// // OPTIMIZE not sure if this is a necssary step to check if they overlap first before checking individual
	// // points, but it might be faster than checking the contains point first
	// // THIS ALSO MAKE THIS METHOD CONFINED TO CONVEX ONLY
	// if (!Intersector.overlapConvexPolygons(p0, p1)) { return false; }
	//
	// if (overlapPolygonsInternal(p0, p1, fastIntersection)) { return true; }
	// if (overlapPolygonsInternal(p1, p0, fastIntersection)) { return true; }
	// return false;
	// }
	//
	// /**
	// * Only checks if p0 contains p1's points
	// *
	// * @param p0
	// * @param p1
	// * @param fastIntersection
	// * (optional) will set to this to the first found fast intersection point, which is primarily accurate on
	// * initial collision,
	// * @return
	// */
	// private static boolean overlapPolygonsInternal(Polygon p0, Polygon p1,
	// Vector2 fastIntersection)
	// {
	// // check first polygon
	// float[] polygonPoints = p1.getTransformedVertices();
	// for (int i = 0; i < polygonPoints.length; i += 2)
	// {
	// if (p0.contains(polygonPoints[i], polygonPoints[i + 1]))
	// {
	// if (fastIntersection != null)
	// {
	// fastIntersection
	// .set(polygonPoints[i], polygonPoints[i + 1]);
	// }
	// return true;
	// }
	// }
	// return false;
	// }
	//
	// /**
	// * Returns all the points of intersection. It is assumed that these are already intersecting.
	// *
	// * @param r
	// * @param p
	// * @return
	// */
	// public static ArrayList<Vector2> IntersectionsRectanglePolygon(Rectangle r,
	// Polygon p)
	// {
	// // iterate through segments of the polygon
	// float[] pValues = p.getTransformedVertices();
	// float[] rValues = GetRectFourCorners(r);
	//
	// return IntersectionsPolygonsValues(pValues, rValues);
	// }
	//
	// // OPTIMIZE I assume this could be optimized cause they are axis bound
	// /**
	// * Returns all the points of intersection. It is assumed that these are already intersecting.
	// *
	// * @param r0
	// * @param r1
	// * @return
	// */
	// public static ArrayList<Vector2> IntersectionsRectangles(Rectangle r0,
	// Rectangle r1)
	// {
	// // iterate through segments of the polygon
	// float[] r0Values = GetRectFourCorners(r0);
	// float[] r1Values = GetRectFourCorners(r1);
	//
	// return IntersectionsPolygonsValues(r0Values, r1Values);
	// }
	//
	// /**
	// * Returns all the points of intersection. It is assumed that these are already intersecting.
	// *
	// * @param p0
	// * @param p1
	// * @return
	// */
	// public static ArrayList<Vector2> IntersectionsPolygons(Polygon p0,
	// Polygon p1)
	// {
	// float[] p0Values = p0.getTransformedVertices();
	// float[] p1Values = p1.getTransformedVertices();
	//
	// return IntersectionsPolygonsValues(p0Values, p1Values);
	// }
	//
	// /**
	// * Returns all the points of intersection. It is assumed that these polygons are already intersecting.
	// *
	// * @param p0Values
	// * @param p1Values
	// * @return
	// */
	// public static ArrayList<Vector2> IntersectionsPolygonsValues(
	// float[] p0Values, float[] p1Values)
	// {
	// ArrayList<Vector2> list = new ArrayList<Vector2>();
	//
	// Vector2 p0Start = CheckoutTmpV2Internal0();
	// Vector2 p0End = CheckoutTmpV2Internal1();
	// Vector2 p1Start = CheckoutTmpV2Internal2();
	// Vector2 p1End = CheckoutTmpV2Internal3();
	// Vector2 intersection = CheckoutTmpV2Internal4();
	//
	// for (int i = 0; i <= p0Values.length - 2; i += 2)
	// {
	// // get polygon0 segment
	// if (i == p0Values.length - 2)
	// {
	// // get segment from last to first
	// p0Start.set(p0Values[p0Values.length - 2],
	// p0Values[p0Values.length - 1]);
	// p0End.set(p0Values[0], p0Values[1]);
	// }
	// else
	// {
	// p0Start.set(p0Values[i], p0Values[i + 1]);
	// p0End.set(p0Values[i + 2], p0Values[i + 3]);
	// }
	//
	// // iterate through all of polygon1's segments
	// for (int y = 0; y <= p1Values.length - 2; y += 2)
	// {
	// // get polygon1 segment
	// if (y == p1Values.length - 2)
	// {
	// // get segment from last to first
	// p1Start.set(p1Values[p1Values.length - 2],
	// p1Values[p1Values.length - 1]);
	// p1End.set(p1Values[0], p1Values[1]);
	// }
	// else
	// {
	// p1Start.set(p1Values[y], p1Values[y + 1]);
	// p1End.set(p1Values[y + 2], p1Values[y + 3]);
	// }
	//
	// intersection.set(Float.POSITIVE_INFINITY, 0);
	// Intersector.intersectSegments(p0Start, p0End, p1Start, p1End,
	// intersection);
	// if (intersection.x != Float.POSITIVE_INFINITY)
	// {
	// list.add(new Vector2(intersection));
	// }
	// }
	// }
	//
	// ReturnTmpV2Internal0();
	// ReturnTmpV2Internal1();
	// ReturnTmpV2Internal2();
	// ReturnTmpV2Internal3();
	// ReturnTmpV2Internal4();
	//
	// return list;
	// }
	//
	// /**
	// * Returns if the rectangles are intersecting by checking if their points are inside each other. (HACK). This
	// is
	// not
	// * true collision, but is best for finding the initial intersection.
	// *
	// * @param r0
	// * @param r1
	// * @param intersection
	// * will set this to first intersecting point found, does not require extra computation because this is
	// * the method of detecting collisions
	// * @return
	// */
	// public static boolean overlapRectangles(Rectangle r0, Rectangle r1,
	// Vector2 intersection)
	// {
	// LttlProfiler.collisionChecks.add();
	//
	// if (overlapRectanglesInternal(r0, r1, intersection)) { return true; }
	// if (overlapRectanglesInternal(r1, r0, intersection)) { return true; }
	// return false;
	// }
	//
	// /**
	// * Only checks if r0 contains r1's points
	// *
	// * @param r0
	// * @param r1
	// * @param intersection
	// * @return
	// */
	// private static boolean overlapRectanglesInternal(Rectangle r0,
	// Rectangle r1, Vector2 intersection)
	// {
	// float[] rectPoints = LttlMath.GetRectFourCorners(r1);
	// for (int i = 0; i < rectPoints.length; i += 2)
	// {
	// if (r0.contains(rectPoints[i], rectPoints[i + 1]))
	// {
	// if (intersection != null)
	// {
	// intersection.set(rectPoints[i], rectPoints[i + 1]);
	// }
	// return true;
	// }
	// }
	// return false;
	// }
	//
	// /**
	// * TODO not sure if this includes concave polygons, may just need to also do a line check if it does contain
	// it to
	// * to see if it is inside or not by drawing a line from this point in any direction beyond the other shape,
	// and if
	// * it hits an odd number of lines, than it's inside, if it's even, then it's outside
	// *
	// * @param polygon
	// * @param x
	// * @param y
	// * @return
	// */
	// public static boolean PolygonContains(Polygon polygon, float x, float y)
	// {
	// return polygon.contains(x, y);
	// }
}
