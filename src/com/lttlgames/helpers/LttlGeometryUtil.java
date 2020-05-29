package com.lttlgames.helpers;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.IntArray;
import com.lttlgames.editor.Lttl;
import com.lttlgames.editor.LttlDebug;
import com.lttlgames.editor.LttlGameSettings;
import com.lttlgames.exceptions.MultiplePolygonsException;
import com.lttlgames.graphics.Cap;
import com.lttlgames.graphics.GeometryOperation;
import com.lttlgames.graphics.Joint;
import com.lttlgames.graphics.LttlMesh;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.buffer.BufferOp;
import com.vividsolutions.jts.operation.buffer.BufferParameters;

/**
 * Utility class that includes workhorse methods for complex geometry operations.
 */
public class LttlGeometryUtil
{
	// JTS Topology
	private static GeometryFactory geometryFactory;

	private static Vector2Array tmpArray1 = new Vector2Array(0);
	private static Vector2Array tmpArray2 = new Vector2Array(0);
	private static IntArray tmpIntArray = new IntArray(0);
	private static Vector2 temp = new Vector2();
	private static Vector2 temp2 = new Vector2();
	private static Vector2 temp3 = new Vector2();
	private static Vector2 temp4 = new Vector2();
	private static Vector2 offsetPrevLineStartPoint = new Vector2();
	private static Vector2 offsetPrevLineEndPoint = new Vector2();
	private static Vector2 offsetCurrentLineStartPoint = new Vector2();
	private static Vector2 offsetCurrentLineEndPoint = new Vector2();
	private static Vector2 prevLineVectorNor = new Vector2();
	private static Vector2 currentLineVectorNor = new Vector2();
	private static Vector2 intersectionPoint = new Vector2();

	/**
	 * @param points
	 * @return
	 * @throw lessThan3Points
	 */
	static public boolean isCounterClockwise(Vector2Array points)
	{
		// http://stackoverflow.com/questions/1165647/how-to-determine-if-a-list-of-polygon-points-are-in-clockwise-order

		// not enough points
		Lttl.Throw(points.size() < 3);

		// this sum is twice the area
		float twoArea = 0;
		for (int i = 0, n = points.size(); i < n; i++)
		{
			// define index
			int a = i;
			int b = LttlMath.loopIndex(i + 1, n);

			twoArea += (points.getX(b) - points.getX(a))
					* (points.getY(b) + points.getY(a));
		}

		return twoArea < 0;
	}

	static public GeometryFactory getGeometryFactory()
	{
		if (geometryFactory == null)
		{
			geometryFactory = new GeometryFactory();
		}
		return geometryFactory;
	}

	/**
	 * Buffer's an (open) path on one or both sides.<br>
	 * Self intersections are allowed.<br>
	 * *This might not be that efficient for generating a line with width every frame, especially if it has no end cap
	 * or special joint.
	 * 
	 * @param source
	 * @param amount
	 *            can't be 0
	 * @param jointType
	 * @param roundSegments
	 *            if round joint or cap, this is number of segments, 0 uses default
	 * @param mitreRatioLimit
	 *            Limits the length of mitre point. If 0 or less will automatically use
	 *            {@link LttlGameSettings#miterRatioLimit}
	 * @param capType
	 *            caps are disabled if singleSided
	 * @param singleSided
	 *            if single sided, then it will buffer on the side of the amount
	 * @param container
	 *            can be null to create a new one, otherwise will replace it with result and return it
	 * @return a polygon container which may include holes
	 */
	static public PolygonContainer bufferPath(Vector2Array source,
			float amount, Joint jointType, int roundSegments,
			float mitreRatioLimit, Cap capType, boolean singleSided,
			PolygonContainer container) throws MultiplePolygonsException
	{
		Lttl.Throw(source.size() < 2);
		Lttl.Throw(amount == 0);

		Geometry g = createLineString(source);

		BufferParameters bp = new BufferParameters(LttlMath.abs(roundSegments),
				capType.getValue(), jointType.getValue(), Lttl.game
						.getSettings().getMiterRatioLimit(mitreRatioLimit));
		bp.setSingleSided(singleSided);

		// do buffer
		amount = singleSided ? amount : LttlMath.abs(amount);
		g = BufferOp.bufferOp(g, amount, bp);
		return bufferResultShared(g, container);
	}

	/**
	 * Used to simplify a polygon if it self intersects and/or has holes that are not specified.
	 * 
	 * @param polygon
	 * @param container
	 *            can be null to create a new one, otherwise will replace it with result and return it
	 * @return
	 * @throws MultiplePolygonsException
	 */
	static public PolygonContainer simplifyPolygon(Polygon polygon,
			PolygonContainer container) throws MultiplePolygonsException
	{
		return offsetPolygon(polygon, 0, Joint.MITER, 0, 0, container);
	}

	/**
	 * @see #offsetPolygon(Polygon, float, Joint, int, float, PolygonContainer)
	 */
	static public PolygonContainer offsetPolygon(Vector2Array polygon,
			float amount, Joint jointType, int roundSegments,
			float mitreRatioLimit, PolygonContainer container)
			throws MultiplePolygonsException
	{
		return offsetPolygon(createPolygon(polygon), amount, jointType,
				roundSegments, mitreRatioLimit, container);
	}

	/**
	 * Expands or implodes a polygon.<br>
	 * If using {@link Joint#BEVEL} or {@link Joint#ROUND} or the mitre limit is reached, there will be additional
	 * points.<br>
	 * Holes can be generated.<br>
	 * Can be used with 0 buffer amount to simplify self intersecting polygons and identify holes
	 * {@link #simplifyPolygon}
	 * 
	 * @param polygon
	 * @param amount
	 * @param jointType
	 * @param roundSegments
	 *            if round joint or cap, this is number of segments, 0 uses default
	 * @param mitreRatioLimit
	 *            Limits the length of mitre point. If 0 or less will automatically use
	 *            {@link LttlGameSettings#miterRatioLimit}
	 * @param container
	 *            can be null to create a new one, otherwise will replace it with result and return it
	 * @return
	 * @throws MultiplePolygonsException
	 *             if multiple polygons
	 */
	static public PolygonContainer offsetPolygon(Polygon polygon, float amount,
			Joint jointType, int roundSegments, float mitreRatioLimit,
			PolygonContainer container) throws MultiplePolygonsException
	{
		BufferParameters bp = new BufferParameters(LttlMath.abs(roundSegments),
				BufferParameters.CAP_FLAT, jointType.getValue(), Lttl.game
						.getSettings().getMiterRatioLimit(mitreRatioLimit));
		bp.setSingleSided(true);

		// do buffer
		Geometry g = BufferOp.bufferOp(polygon, amount, bp);
		return bufferResultShared(g, container);
	}

	private static PolygonContainer bufferResultShared(Geometry g,
			PolygonContainer container) throws MultiplePolygonsException
	{
		if (g instanceof Polygon)
		{
			if (container == null) { return new PolygonContainer((Polygon) g); }
			return container.set((Polygon) g);
		}
		else if (g instanceof MultiPolygon)
		{
			new MultiplePolygonsException(
					"BufferPolygon: created multiple polygons");
		}
		else
		{
			new MultiplePolygonsException("BufferPolygon: unexpected type ["
					+ g.getClass().getSimpleName() + "]");
		}
		return container;
	}

	private static boolean offsetPathDebug = false;
	private static OffsetPathObject opo;

	/**
	 * Offsets the path. This allows offsetting paths that are open and gives relationship data, which
	 * {@link #offsetPolygon} does not. Also does not use an external library, and is pretty fast.<br>
	 * Can allow self intersection too, but will get irregular results if the offset amount is high and neighboring
	 * lines are very close together.
	 * 
	 * @param source
	 *            needs at least 2 points, 3 points if isClosed, may break if same consecutive points
	 * @param isClosed
	 * @param isCCW
	 *            if this is set accurately (esp. for closed paths), a positive offset amount will grow the path. It can
	 *            either be set to a fixed value (always false, this is good for open paths) or use
	 *            {@link #isCounterClockwise(Vector2Array)} to always be accurate.
	 * @param amount
	 *            can't be 0
	 * @param amount
	 *            <b>Can't be 0</b>
	 * @param miterRatioLimit
	 *            Limits the length of mitre point. If 0 or less will automatically use
	 *            {@link LttlGameSettings#miterRatioLimit}
	 * @param cleanup
	 *            if true, will do a check at the end and (try to) clean up any self intersections
	 * @param offsettedPointsContainer
	 *            can be same as the source, all the new offsetted points will go in here
	 * @param relationshipContainer
	 *            (optional) if null, the indexes correspond to each new point and the value is the index in the point
	 *            array that the new point was expanded from. This allows for triangulation later.
	 * @throws RuntimeException
	 *             not enough points or amount is 0
	 */
	static public void offsetPath(Vector2Array source, boolean isClosed,
			boolean isCCW, float amount, float miterRatioLimit,
			boolean cleanup, Vector2Array offsettedPointsContainer,
			IntArray relationshipContainer)
	{
		// not enough points (if closed needs at least 3, if open needs at least 2
		Lttl.Throw(source.size() < (isClosed ? 3 : 2));
		Lttl.Throw(amount == 0);

		// save all settings
		float miterDstLimit = LttlMath.abs(amount)
				* Lttl.game.getSettings().getMiterRatioLimit(miterRatioLimit);
		opo = new OffsetPathObject(source, isClosed, isCCW, amount,
				miterDstLimit);

		// reset temp variables
		temp.set(0, 0);
		temp2.set(0, 0);
		offsetPrevLineStartPoint.set(0, 0);
		offsetPrevLineEndPoint.set(0, 0);
		offsetCurrentLineStartPoint.set(0, 0);
		offsetCurrentLineEndPoint.set(0, 0);
		prevLineVectorNor.set(0, 0);
		currentLineVectorNor.set(0, 0);
		intersectionPoint.set(0, 0);

		// ensure capacities and clear
		tmpArray1.clear();
		tmpArray1.ensureCapacity(source.size());

		// since is closed, init values for last edge (don't add any points) so loop will work smoothly
		if (isClosed)
		{
			offsetPathProcessPoint(source.size() - 1, true);
		}

		/** LOOP **/
		// iterate through each point
		for (int i = 0, n = source.size(); i < n; i++)
		{
			offsetPathProcessPoint(i, false);
		}

		if (cleanup)
		{
			offsetPathCleanup(source, isClosed);
		}

		if (relationshipContainer != null)
		{
			relationshipContainer.clear();
			relationshipContainer.addAll(tmpIntArray);
		}
		offsettedPointsContainer.set(tmpArray1);

		tmpArray1.clear();
		tmpIntArray.clear();
		opo = null;
	}

	private static class OffsetPathObject
	{
		final Vector2Array source;
		final boolean isClosed;
		final boolean isCCW;
		final float amount;
		final float miterDstLimit;

		public OffsetPathObject(Vector2Array source, boolean isClosed,
				boolean isCCW, float amount, float miterDstLimit)
		{
			this.source = source;
			this.isClosed = isClosed;
			this.isCCW = isCCW;
			this.amount = amount;
			this.miterDstLimit = miterDstLimit;
		}
	}

	private static void offsetPathCleanup(Vector2Array source, boolean isClosed)
	{
		// rarely works

		// in brief, it checks it's neighbor to see if the lines have an intersection point that is in the same
		// direction as their current aa point, if so, then it uses the intersection point if it is shorter than the
		// current inrersection point
		// not perfect by any means, but does prevent some crazy stuff from happening, which is already rare
		for (int i = 0, n = isClosed ? source.size() : source.size() - 1; i < n; i++)
		{
			int next = LttlMath.loopIndex(i + 1, tmpArray1.size());

			// skip if bevel
			if (tmpIntArray.get(i) == tmpIntArray.get(next)) continue;

			if (Intersector
					.intersectLines(tmpArray1.get(i, temp),
							source.get(tmpIntArray.get(i), temp2),
							tmpArray1.get(next, temp3),
							source.get(tmpIntArray.get(next), temp4),
							intersectionPoint))
			{
				if (tmpArray1
						.get(i, temp3)
						.sub(source.get(tmpIntArray.get(i), temp2))
						.hasSameDirection(
								temp.set(intersectionPoint).sub(temp2)))
				{
					if (source.get(tmpIntArray.get(i), temp2).dst2(
							intersectionPoint) < temp2.dst2(tmpArray1.get(i,
							temp)))
					{
						tmpArray1.set(i, intersectionPoint);
					}
				}

				if (tmpArray1
						.get(next, temp3)
						.sub(source.get(tmpIntArray.get(next), temp2))
						.hasSameDirection(
								temp.set(intersectionPoint).sub(temp2)))
				{
					if (source.get(tmpIntArray.get(next), temp2).dst2(
							intersectionPoint) < temp2.dst2(tmpArray1.get(next,
							temp)))
					{
						tmpArray1.set(next, intersectionPoint);
					}
				}
			}
		}
	}

	/**
	 * @param currentIndex
	 *            this is the point that is currently being offset into a miter or bevel
	 */
	static private void offsetPathProcessPoint(int currentIndex, boolean init)
	{
		int nextIndex = LttlMath.loopIndex(currentIndex + 1, opo.source.size());

		// use last iteration's nextLineVector for this line's current, saves calculations
		prevLineVectorNor.set(currentLineVectorNor);

		// grab previously calculated offset to set as prev
		offsetPrevLineStartPoint.set(offsetCurrentLineStartPoint);
		offsetPrevLineEndPoint.set(offsetCurrentLineEndPoint);

		// if not closed and last point, then don't check for intersection, just use previous offsetted point
		if (!opo.isClosed && currentIndex == opo.source.size() - 1)
		{
			// add offsetted point
			offsetPathAddPoint(offsetPrevLineEndPoint, currentIndex);
			return;
		}

		// calculate current line vector (the slope between current point and the next)
		currentLineVectorNor.set(opo.source.get(nextIndex, temp))
				.sub(opo.source.get(currentIndex, temp)).nor();

		// flip if CCW
		if (opo.isCCW)
		{
			currentLineVectorNor.scl(-1);
		}

		// calculate the offset vector (rotates slope counter clockwise 90 degrees and gives length of amount)
		temp2.set(currentLineVectorNor).rotate90(1).scl(opo.amount);

		// calculate the offsetted current line points
		offsetCurrentLineStartPoint.set(opo.source.get(currentIndex, temp))
				.add(temp2);
		offsetCurrentLineEndPoint.set(opo.source.get(nextIndex, temp)).add(
				temp2);

		// if just initing values, then don't add a point
		if (init) return;

		if (offsetPathDebug)
		{
			Lttl.debug.drawCircle(offsetCurrentLineStartPoint,
					LttlDebug.RADIUS_SMALL * Lttl.debug.eF(), new Color(0, 0,
							0, .3f));
			Lttl.debug.drawCircle(offsetCurrentLineEndPoint,
					LttlDebug.RADIUS_SMALL * Lttl.debug.eF(), new Color(1, 0,
							0, .3f));
		}

		// if not closed and first point, then don't check for intersection, just use offsetted point
		if (!opo.isClosed && currentIndex == 0)
		{
			// add offsetted point
			offsetPathAddPoint(offsetCurrentLineStartPoint, currentIndex);
			return;
		}

		float direction = LttlMath.GetAngleRelative(currentLineVectorNor,
				prevLineVectorNor, true) * (opo.isCCW ? -1 : 1);

		if (LttlMath.isZero(direction))
		{
			// if same direction then just use current index's offset start position
			if (offsetPathDebug)
			{
				// cyan = same line point
				Lttl.debug.drawCircle(offsetCurrentLineStartPoint,
						LttlDebug.RADIUS_SMALL * Lttl.debug.eF(), new Color(0,
								1, 1, .3f));
			}
			offsetPathAddPoint(offsetCurrentLineStartPoint, currentIndex);
			return;
		}

		// find intersection point of currentLine and nextLine
		if (!Intersector.intersectLines(offsetPrevLineStartPoint,
				offsetPrevLineEndPoint, offsetCurrentLineStartPoint,
				offsetCurrentLineEndPoint, intersectionPoint))
		{
			// lines are parallel (rare)
			// set intersection point as being beyond the mitre limit, so later (below) a bevel will be created
			intersectionPoint.set(temp.set(currentLineVectorNor).scl(
					opo.miterDstLimit * 2));
		}
		else if (direction < 0)
		{
			// try to just use the intersection of the segments, this prevents very close points creating a huge line
			if (Intersector.intersectSegments(offsetPrevLineStartPoint,
					offsetPrevLineEndPoint, offsetCurrentLineStartPoint,
					offsetCurrentLineEndPoint, temp4))
			{
				intersectionPoint.set(temp4);
			}
			else
			{
				// takes the prev and nexy point and finds the point between them that would exist if the amount was the
				// greatest before intersecting
				opo.source
						.get(LttlMath.loopIndex(currentIndex - 1,
								opo.source.size()), temp2);
				opo.source.get(nextIndex, temp);
				Intersector.intersectLines(temp2, temp3.set(prevLineVectorNor)
						.rotate90(1).add(temp2), temp,
						temp4.set(currentLineVectorNor).rotate90(1).add(temp),
						intersectionPoint);
			}

			// always use intersection point since turning inside
			if (offsetPathDebug)
			{
				// blue = inside intersection
				Lttl.debug.drawCircle(intersectionPoint, LttlDebug.RADIUS_SMALL
						* Lttl.debug.eF(), new Color(0, 0, 1, .3f));
			}
			offsetPathAddPoint(intersectionPoint, currentIndex);
			return;
		}

		// checking if mitreDstLimit is exceeded
		// find squared distance from the corner to the intersection
		float dst2 = opo.source.get(currentIndex, temp).dst2(intersectionPoint);
		if (dst2 > LttlMath.pow2(opo.miterDstLimit))
		{
			// mitre limit has been exceeded
			// calcualte the vector/slope of the corner to the intersection point
			temp2.set(intersectionPoint)
					.sub(opo.source.get(currentIndex, temp)).nor();
			// calculate the bevel midpoint, which is offset from the corner towards the intersection point
			opo.source.get(currentIndex, temp3).add(
					temp.set(temp2).scl(opo.miterDstLimit));
			temp2.rotate90(1).scl(10);

			// get the intersection point between a line that goes through the bevel midpoint and is
			// perpendicular to the line that is from the corner to the intersection and the the offset current
			// line
			// idk why this would not intersect
			Lttl.Throw(!Intersector.intersectLines(temp3.x + temp2.x, temp3.y
					+ temp2.y, temp3.x - temp2.x, temp3.y - temp2.y,
					offsetPrevLineStartPoint.x, offsetPrevLineStartPoint.y,
					offsetPrevLineEndPoint.x, offsetPrevLineEndPoint.y,
					intersectionPoint));

			// the intersection point is the first bevel point
			if (offsetPathDebug)
			{
				// orange = first bevel point
				Lttl.debug.drawCircle(intersectionPoint, LttlDebug.RADIUS_SMALL
						* Lttl.debug.eF(), new Color(1, 0.78f, 0, .4f));
			}
			offsetPathAddPoint(intersectionPoint, currentIndex);

			// get the amount from bevel midpoint to the first bevel point, so can derive the difference, then
			// subtract that amount from the midpoint to get the second bevel point
			temp3.sub(intersectionPoint.sub(temp3));
			if (offsetPathDebug)
			{
				// purple = first bevel point
				Lttl.debug.drawCircle(temp3, LttlDebug.RADIUS_SMALL
						* Lttl.debug.eF(), new Color(0.5f, 0, 0.5f, .4f));
			}
			offsetPathAddPoint(temp3, currentIndex);
		}
		else
		{
			// add intersection point since miter distance does note exceed limit
			if (offsetPathDebug)
			{
				// green = outside intersection point
				Lttl.debug.drawCircle(intersectionPoint, LttlDebug.RADIUS_SMALL
						* Lttl.debug.eF(), new Color(0, 1, 0, .3f));
			}
			offsetPathAddPoint(intersectionPoint, currentIndex);
		}
	}

	static private void offsetPathAddPoint(Vector2 point, int relationshipIndex)
	{
		tmpArray1.add(point);
		tmpIntArray.add(relationshipIndex);
	}

	static private Coordinate[] convertToCoorindates(Vector2Array points,
			int start, int end, boolean isClosed)
	{
		return convertToCoorindates(points.getFloatArray().items, start, end,
				isClosed);
	}

	/**
	 * @param points
	 * @param start
	 *            each index is considered in pairs, not actual float array indexes
	 * @param end
	 * @param isClosed
	 * @return
	 */
	static private Coordinate[] convertToCoorindates(float[] points, int start,
			int end, boolean isClosed)
	{
		// coords must be in counter clockwise order or it will mess up
		Coordinate[] coords = new Coordinate[(end - start + 1)
				+ (isClosed ? 1 : 0)];

		// transfer the source polygon to a Coordinate array
		for (int i = start, n = end; i <= n; i++)
		{
			Coordinate coord = new Coordinate(points[i * 2], points[i * 2 + 1]);
			coords[i - start] = coord;
		}
		// if closed need to loop
		if (isClosed && points.length > 0)
		{
			coords[coords.length - 1] = coords[0];
		}

		return coords;
	}

	/**
	 * @see {@link #convertToCoorindates(Vector2Array, int, int, boolean)}
	 */
	static private Coordinate[] convertToCoorindates(Vector2Array points,
			boolean isClosed)
	{
		Lttl.Throw(points.size() == 0);
		return convertToCoorindates(points, 0, points.size() - 1, isClosed);
	}

	static private Coordinate[] convertToCoorindates(float[] points,
			boolean isClosed)
	{
		Lttl.Throw(points.length < 2);
		return convertToCoorindates(points, 0, (points.length / 2) - 1,
				isClosed);
	}

	static public Vector2Array convertToVector2Array(Coordinate[] coords,
			boolean includeLast, Vector2Array container)
	{
		int n = includeLast ? coords.length : coords.length - 1;
		container.clear();
		container.ensureCapacity(n);
		for (int i = 0; i < n; i++)
		{
			container.add((float) coords[i].x, (float) coords[i].y);
		}

		return container;
	}

	/**
	 * does not union
	 * 
	 * @param polygons
	 * @return
	 */
	static public MultiPolygon createMultiPolygon(
			ArrayList<Vector2Array> polygons)
	{
		GeometryFactory gf = getGeometryFactory();
		Polygon[] ps = new Polygon[polygons.size()];
		for (int i = 0, n = ps.length; i < n; i++)
		{
			ps[i] = createPolygon(polygons.get(i));
		}
		return gf.createMultiPolygon(ps);
	}

	static public LinearRing createLinearRing(Vector2Array points)
	{
		// linear rings need at least 3 points
		Lttl.Throw(points.size() < 3);

		Coordinate[] coords = convertToCoorindates(points, true);

		// create linear ring from coords
		GeometryFactory gf = getGeometryFactory();
		return gf.createLinearRing(coords);
	}

	static public Point createPoint(Vector2 point)
	{
		return createPoint(point.x, point.y);
	}

	static public Point createPoint(float x, float y)
	{
		return getGeometryFactory().createPoint(new Coordinate(x, y));
	}

	/**
	 * Creates a Polygon from the LttlMesh, including any holes and not any AA vertices. Need to make sure world or
	 * local vertices on mesh have been populated.
	 */
	static public Polygon createPolygon(LttlMesh mesh, boolean world)
	{
		LinearRing shell = null;
		LinearRing[] holes = null;

		mesh.getVerticesPosNoAA(world, tmpArray1);
		if (mesh.getHolesCount() > 0)
		{
			holes = new LinearRing[mesh.getHolesCount()];
			IntArray holesIndex = mesh.getHolesIndexArray();
			int start = 0;
			for (int i = 0, n = holesIndex.size; i <= n; i++)
			{
				int end = (i == n ? tmpArray1.size() : holesIndex.get(i)) - 1;
				mesh.getVerticesPos(world, start, end, tmpArray2);
				LinearRing ring = createLinearRing(tmpArray2);
				if (i == 0)
				{
					shell = ring;
				}
				else
				{
					holes[i - 1] = ring;
				}
				start = end + 1;
			}
		}
		else
		{
			shell = createLinearRing(tmpArray1);
		}

		return getGeometryFactory().createPolygon(shell, holes);
	}

	static public Polygon createPolygon(PolygonContainer polyCont)
	{
		return createPolygon(polyCont.points, polyCont.holes);
	}

	static public Polygon createPolygon(Vector2Array points,
			ArrayList<Vector2Array> holes)
	{
		LinearRing shell = createLinearRing(points);
		LinearRing[] holesArray = null;

		if (holes.size() > 0)
		{
			holesArray = new LinearRing[holes.size()];
			int count = -1;
			for (Vector2Array hole : holes)
			{
				count++;
				holesArray[count] = createLinearRing(hole);
			}
		}

		return getGeometryFactory().createPolygon(shell, holesArray);
	}

	static public Polygon createPolygon(float[] points)
	{
		// polygons need at least 3 points
		Lttl.Throw(points);
		Lttl.Throw(points.length < 6);

		Coordinate[] coords = convertToCoorindates(points, true);

		return getGeometryFactory().createPolygon(coords);
	}

	static public Polygon createPolygon(Vector2Array points)
	{
		// polygons need at least 3 points
		Lttl.Throw(points);
		Lttl.Throw(points.size() < 3);

		Coordinate[] coords = convertToCoorindates(points, true);

		GeometryFactory gf = getGeometryFactory();
		return gf.createPolygon(coords);
	}

	static public Polygon createPolygon(Rectangle rect)
	{
		Coordinate[] coords = new Coordinate[5];

		coords[0] = new Coordinate(rect.x, rect.y);
		coords[1] = new Coordinate(rect.x + rect.width, rect.y);
		coords[2] = new Coordinate(rect.x + rect.width, rect.y + rect.height);
		coords[3] = new Coordinate(rect.x, rect.y + rect.height);
		coords[4] = coords[0];

		GeometryFactory gf = getGeometryFactory();
		return gf.createPolygon(coords);
	}

	static public LineString createLineString(Vector2Array points)
	{
		// linear rings need at least 2 points
		Lttl.Throw(points.size() < 2);

		Coordinate[] coords = convertToCoorindates(points, false);

		// create linear string from coords
		GeometryFactory gf = getGeometryFactory();
		return gf.createLineString(coords);
	}

	/**
	 * Converts a polygon to a {@link PolygonContainer}
	 * 
	 * @param p
	 * @return
	 */
	static public PolygonContainer toPolygonContainer(Polygon p)
	{
		return new PolygonContainer(p);
	}

	/**
	 * Converts a {@link MultiPolygon} to an ArrayList of{@link PolygonContainer}, does not union.
	 * 
	 * @param mp
	 * @return
	 */
	static public ArrayList<PolygonContainer> toPolygonContainer(MultiPolygon mp)
	{
		int n = mp.getNumGeometries();
		ArrayList<PolygonContainer> list = new ArrayList<PolygonContainer>(n);
		for (int i = 0; i < n; i++)
		{
			list.add(toPolygonContainer((Polygon) mp.getGeometryN(i)));
		}

		return list;
	}

	/**
	 * Converts a {@link Polygon} or {@link MultiPolygon} to an ArrayList of {@link PolygonContainer}
	 * 
	 * @param polygonOrMultiPolygon
	 * @return
	 */
	public static ArrayList<PolygonContainer> toPolygonContainers(
			Geometry polygonOrMultiPolygon)
	{
		// prepare result
		if (polygonOrMultiPolygon.getClass() == MultiPolygon.class)
		{
			return toPolygonContainer((MultiPolygon) polygonOrMultiPolygon);
		}
		else if (polygonOrMultiPolygon.getClass() == Polygon.class)
		{
			ArrayList<PolygonContainer> list = new ArrayList<PolygonContainer>(
					1);
			list.add(toPolygonContainer((Polygon) polygonOrMultiPolygon));
			return list;
		}

		// werid case
		Lttl.Throw();
		return null;
	}

	// TODO
	static public void getVector2Array(LinearRing g)
	{
		Lttl.Throw();
	}

	// TODO
	static public void getVector2Array(LineString g)
	{
		Lttl.Throw();
	}

	/**
	 * An efficient way to union lots of polygons Returns a {@link Polygon} or {@link MultiPolygon} since some polygons
	 * may not be overlapping.
	 * 
	 * @param operation
	 * @param polygons
	 * @return
	 */
	static public Geometry polygonUnionToGeometry(
			ArrayList<Vector2Array> polygons)
	{
		if (polygons.size() == 1) { return createPolygon(polygons.get(0)); }
		MultiPolygon multiPolygon = createMultiPolygon(polygons);

		// OPTIMIZE could also use this, which may be faster and robust, especially if there are large overlaps
		// CascadedPolygonUnion.union(collection of polygons)
		return multiPolygon.buffer(0);
	}

	/**
	 * An efficient way to union lots of polygons Returns an ArrayList of {@link PolygonContainer}
	 * 
	 * @param polygons
	 * @return
	 */
	static public ArrayList<PolygonContainer> polygonUnion(
			ArrayList<Vector2Array> polygons)
	{
		return toPolygonContainers(polygonUnionToGeometry(polygons));
	}

	/**
	 * OPTIMIZE if this is slow for certain use cases, there are probably more efficient alternatives (ie. for a UNION
	 * make a list for each group of polygons that are touching, they get convex hull for each)<br>
	 * <br>
	 * <br>
	 * Each polygon list will be unionzied first, then the batch operation will be applied.
	 * 
	 * @param operation
	 * @param polygonA
	 *            with {@link GeometryOperations#DIFFERENCE} polygonsB is subtracted from polygonsA
	 * @param polygonsB
	 * @return null if it failed for some reason
	 */
	static public ArrayList<PolygonContainer> polygonOperation(
			GeometryOperation operation, Vector2Array polygonA,
			ArrayList<Vector2Array> polygonsB)
	{
		ArrayList<Vector2Array> polygonListA = new ArrayList<Vector2Array>(1);
		polygonListA.add(polygonA);

		return polygonOperation(operation, polygonListA, polygonsB);
	}

	/**
	 * see {@link #polygonOperation(GeometryOperation, Vector2Array, ArrayList)}
	 */
	static public ArrayList<PolygonContainer> polygonOperation(
			GeometryOperation operation, ArrayList<Vector2Array> polygonsA,
			ArrayList<Vector2Array> polygonsB)
	{
		Geometry result = null;

		try
		{
			// if Union operation, do it once by combing all polygons
			if (operation == GeometryOperation.UNION)
			{
				ArrayList<Vector2Array> combined = new ArrayList<Vector2Array>(
						polygonsA);
				combined.addAll(polygonsB);
				result = polygonUnionToGeometry(combined);
			}
			else
			{
				// union each group of polygons
				Geometry unionPolygonA = polygonUnionToGeometry(polygonsA);
				Geometry unionPolygonB = polygonUnionToGeometry(polygonsB);

				// process operation
				switch (operation)
				{
					case DIFFERENCE:
						result = unionPolygonA.difference(unionPolygonB);
						break;
					case INTERSECTION:
						result = unionPolygonA.intersection(unionPolygonB);
						break;
					case SYM_DIFFERENCE:
						result = unionPolygonA.symDifference(unionPolygonB);
						break;
					case UNION:
						// done above to prevent a double union operation
						break;
				}
			}
		}
		catch (RuntimeException e)
		{
			Lttl.logNote("Polygon Operation Failed: " + e.getMessage());
			return null;
		}

		return toPolygonContainers(result);
	}

	// OPTIMIZE could have the ability to create an use the actual Vector2Arrays given to save on garbage collection
	/**
	 * Holds polygon points and any holes, generates the values on creation.
	 */
	public static class PolygonContainer
	{
		private Polygon polygon;
		private Vector2Array points = new Vector2Array(0);
		private ArrayList<Vector2Array> holes = new ArrayList<Vector2Array>(0);

		public PolygonContainer()
		{

		}

		public PolygonContainer(LttlMesh mesh, boolean world)
		{
			set(mesh, world);
		}

		public PolygonContainer set(LttlMesh mesh, boolean world)
		{
			return set(createPolygon(mesh, world));
		}

		/**
		 * Creates a {@link PolygonContainer} from a {@link Polygon} and calculates any holes.
		 * 
		 * @param polygon
		 */
		public PolygonContainer(Polygon polygon)
		{
			set(polygon);
		}

		public PolygonContainer set(Polygon polygon)
		{
			clear();
			this.polygon = polygon;
			init();
			return this;
		}

		/**
		 * Creates a {@link PolygonContainer} from an array of points (copies points into a new array), no holes or
		 * polygon
		 * 
		 * @param points
		 */
		public PolygonContainer(Vector2Array points)
		{
			set(points);
		}

		public PolygonContainer set(Vector2Array points)
		{
			clear();
			this.points.addAll(points);
			return this;
		}

		public PolygonContainer(float[] points)
		{
			set(points);
		}

		public PolygonContainer set(float[] points)
		{
			clear();
			this.points.addAll(points);
			return this;
		}

		/**
		 * Only use this if you know for sure that the holes do not touch each other or the edge or are outside points
		 * 
		 * @param points
		 * @param holes
		 */
		public PolygonContainer(Vector2Array points, Vector2Array... holes)
		{
			this.points = new Vector2Array(points);
			for (int i = 0; i < holes.length; i++)
			{
				this.holes.add(holes[i]);
			}
		}

		public PolygonContainer set(Vector2Array points, Vector2Array... holes)
		{
			clear();
			this.points.addAll(points);
			for (int i = 0; i < holes.length; i++)
			{
				this.holes.add(new Vector2Array(holes[i]));
			}
			return this;
		}

		private void clear()
		{
			this.polygon = null;
			this.points.clear();
			this.holes.clear();
		}

		public Polygon getPolygon()
		{
			if (polygon == null)
			{
				polygon = LttlGeometryUtil.createPolygon(this);
			}
			return polygon;
		}

		public Vector2Array getPoints()
		{
			return points;
		}

		/**
		 * None of these holes should overlap, and all should be contained inside {@link #getPoints()}
		 * 
		 * @return
		 */
		public ArrayList<Vector2Array> getHoles()
		{
			return holes;
		}

		private void init()
		{
			points = convertToVector2Array(polygon.getExteriorRing()
					.getCoordinates(), false, points);

			for (int i = 0, n = polygon.getNumInteriorRing(); i < n; i++)
			{
				holes.add(convertToVector2Array(polygon.getInteriorRingN(i)
						.getCoordinates(), false, new Vector2Array(0)));
			}
		}

		public int getPointCount()
		{
			int count = points.size();
			for (Vector2Array hole : holes)
			{
				count += hole.size();
			}
			return count;
		}

		/**
		 * Checks if there are enough points in outer polygon and each hole.
		 */
		public boolean isValid()
		{
			if (points.size() < 3) return false;
			for (Vector2Array hole : holes)
			{
				if (hole.size() < 3) return false;
			}
			return true;
		}

		public void mul(Matrix3 matrix)
		{
			points.mulAll(matrix);
			for (Vector2Array hole : holes)
			{
				hole.mulAll(matrix);
			}

			// clear since it's not accurate anymore, but will be recreated when getPolygon() is ran
			polygon = null;
		}

		public void debugDrawOutline(Color outerColor, Color holeColor,
				float width)
		{
			if (!isValid()) return;

			Lttl.debug.drawLines(points, width, true, outerColor);
			for (Vector2Array hole : holes)
			{
				Lttl.debug.drawLines(hole, width, true, holeColor);
			}
		}
	}

	/**
	 * ie. GeometryA contains GeometryB
	 * 
	 * @param geometryA
	 * @param geometryB
	 * @param compareType
	 * @return
	 */
	public static boolean compare(Geometry geometryA, Geometry geometryB,
			CompareOperation compareType)
	{
		switch (compareType)
		{
			case CONTAINS:
				return geometryA.contains(geometryB);
			case CROSSES:
				return geometryA.crosses(geometryB);
			case DISJOINT:
				return geometryA.disjoint(geometryB);
			case EQUALS_TOPO:
				return geometryA.equalsTopo(geometryB);
			case EQUALS_EXACT:
				return geometryA.equalsExact(geometryB);
			case INTERSECTS:
				return geometryA.intersects(geometryB);
			case OVERLAPS:
				return geometryA.overlaps(geometryB);
			case TOUCHES:
				return geometryA.touches(geometryB);
			default:
				Lttl.Throw();
				break;

		}
		return false;
	}

	/**
	 * @see LttlGeometryUtil#compare(Geometry, Geometry, CompareOperation)
	 */
	public static boolean compare(Vector2Array polygonA, Vector2Array polygonB,
			CompareOperation compareType)
	{
		return compare(createPolygon(polygonA), createPolygon(polygonB),
				compareType);
	}
}
