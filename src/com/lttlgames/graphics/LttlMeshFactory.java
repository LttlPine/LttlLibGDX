package com.lttlgames.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;
import com.lttlgames.components.QuadCorners;
import com.lttlgames.editor.Lttl;
import com.lttlgames.helpers.LttlGeometryUtil.PolygonContainer;
import com.lttlgames.helpers.LttlMath;
import com.lttlgames.helpers.Vector2Array;

public class LttlMeshFactory
{
	// index of each verteex of quad
	public static int QuadTopLeft = 0;
	public static int QuadTopRight = 1;
	public static int QuadBottomRight = 2;
	public static int QuadBottomLeft = 3;

	private static LttlMesh quadMesh;
	private static LttlMesh lineQuadMesh;
	private static LttlMesh circleMesh;
	private static LttlMesh donutMesh;
	private static FloatArray tmpFloatArray = new FloatArray();
	private static Vector2Array tmpVector2Array = new Vector2Array(0);
	private static Vector2Array tmpVector2Array2 = new Vector2Array(0);

	private static Vector2 tmpV2 = new Vector2();
	private static Color tmpColor = new Color();

	/**
	 * Used for testing scripts, not meant to be used in editor,shapes need to be at (0,0) and not rotated or scaled
	 */
	// static boolean drawProgDebugEnabled = false;

	/**
	 * This changes how many steps their are in a circle/arc based on the radius requested, the smaller the more steps.
	 * This specifies the min distance between two points on a circle.
	 */
	private static float circleStepConstant = 1.02f;

	/**
	 * Updates the mesh given (creates if null) based on the {@link PolygonContainer} and desired aaWidth
	 * 
	 * @param mesh
	 * @param polyCont
	 * @param delaunay
	 *            if {@link PolygonContainer} contains holes, even if false will use
	 *            {@link LttlTriangulator#DelaunayTriangulate(PolygonContainer, Vector2Array, ShortArray)}
	 * @return
	 */
	public static LttlMesh GeneratePolygon(LttlMesh mesh,
			PolygonContainer polyCont, boolean delaunay)
	{
		// check if more than 2 point
		Lttl.Throw(polyCont.getPoints().size() < 3);

		mesh = LttlMesh.getNew(mesh, polyCont.getPointCount());

		LttlTriangulator.TriangulateEither(polyCont, tmpVector2Array,
				mesh.getIndicesArray(), mesh.getHolesIndexArray(), delaunay);
		GenerateVertices(tmpVector2Array, mesh.getVerticesArray());

		// create new mesh
		return mesh;
	}

	/**
	 * sets the verticesContainer with the points Vector2Array, calculates UV
	 * 
	 * @param points
	 * @param verticesContainer
	 */
	public static void GenerateVertices(Vector2Array points,
			FloatArray verticesContainer)
	{
		// update dimensions (used in UVs)
		float minX = Float.POSITIVE_INFINITY;
		float minY = Float.POSITIVE_INFINITY;
		float maxX = Float.NEGATIVE_INFINITY;
		float maxY = Float.NEGATIVE_INFINITY;

		for (int i = 0; i < points.size(); i++)
		{
			float x = points.getX(i);
			float y = points.getY(i);
			if (x < minX) minX = x;
			if (x > maxX) maxX = x;

			if (y < minY) minY = y;
			if (y > maxY) maxY = y;
		}

		verticesContainer.clear();
		verticesContainer.ensureCapacity(points.size()
				* LttlMesh.VERTICE_ATTRIBUTE_COUNT);

		// populate vertices array with floats
		for (int i = 0; i < points.size(); i++)
		{
			float x = points.getX(i);
			float y = points.getY(i);
			LttlMesh.addVertice(verticesContainer, x, y, (x - minX)
					/ (maxX - minX), (y - minY) / (maxY - minY), 0, 1);
		}
	}

	/**
	 * Creates a line mesh with list of line widths, but will try to update provided mesh if possible.
	 * 
	 * @param mesh
	 *            mesh to try and update, if null will create new one
	 * @param points
	 * @param cornerType
	 * @param capTypeStart
	 * @param capTypeEnd
	 * @param capCornerSteps
	 *            the number of steps used to create caps and corners
	 * @param lineWidths
	 * @param closed
	 * @param aaWidth
	 * @return
	 */
	public static LttlMesh GenerateLine(LttlMesh mesh, Vector2Array points,
			Joint cornerType, Cap capTypeStart, Cap capTypeEnd,
			int capCornerSteps, FloatArray lineWidths, float offset,
			boolean closed, float aaWidth)
	{
		Lttl.Throw();
		return new LttlMesh();

		// TODO can use, but does not allow line widths or self intersection of path or widths
		// LttlGeometryUtil.bufferPath(pathPointsArray, debugDrawArray,
		// width / 2f, Joint.ROUND, 0, Cap.ROUND, false);

		// return GenerateLineInternal(mesh, points, cornerType, capTypeStart,
		// capTypeEnd, capCornerSteps, 0, lineWidths, offset, closed,
		// aaWidth);
	}

	/**
	 * Creates a line mesh, but will try to update provided mesh if possible.
	 * 
	 * @param mesh
	 *            mesh to try and update, if null will create new one
	 * @param points
	 * @param cornerType
	 * @param capTypeStart
	 * @param capTypeEnd
	 * @param capCornerSteps
	 *            the number of steps to use when creating caps and corners
	 * @param width
	 * @param closed
	 * @param aaWidth
	 * @return
	 */
	public static LttlMesh GenerateLine(LttlMesh mesh, Vector2Array points,
			Joint cornerType, Cap capTypeStart, Cap capTypeEnd,
			int capCornerSteps, float width, float offset, boolean closed,
			float aaWidth)
	{
		Lttl.Throw();
		return new LttlMesh();

		// TODO can use, but does not allow line widths or self intersection of path or widths
		// LttlGeometryUtil.bufferPath(pathPointsArray, debugDrawArray,
		// width / 2f, Joint.ROUND, 0, Cap.ROUND, false);

		// return GenerateLineInternal(mesh, points, cornerType, capTypeStart,
		// capTypeEnd, capCornerSteps, width, null, offset, closed,
		// aaWidth);
	}

	// private static LttlMesh GenerateLineInternal(LttlMesh mesh,
	// Vector2Array points, Joint cornerType, Cap capTypeStart,
	// Cap capTypeEnd, int capCornerSteps, float width,
	// FloatArray lineWidths, float offset, boolean closed, float aaWidth)
	// {
	// // check if more than 1 point
	// Lttl.Throw(points.size() < 2);
	//
	// // check if has only 2 points (no corners)
	// if (points.size() == 2)
	// {
	// return generateLineTwoPoints(mesh, points, cornerType,
	// capTypeStart, capTypeEnd, width, lineWidths, offset,
	// aaWidth, capCornerSteps);
	// }
	// else
	// {
	// return generateLineManyPoints(mesh, points, cornerType,
	// capTypeStart, capTypeEnd, capCornerSteps, width,
	// lineWidths, offset, closed, aaWidth);
	// }
	// }
	//
	// /* SHARED VARIABLES */
	// private static final Vector2 temp0 = new Vector2();
	// private static final IntArray orderedVertices1Shared = new IntArray();
	// private static final ShortArray indicesArrayShared = new ShortArray();
	// private static final FloatArray verticesArrayShared = new FloatArray();
	// private static final Vector2Array offsettedPointsShared = new Vector2Array();
	// // generate line many points variables
	// private static final Vector2 temp1 = new Vector2();
	// private static final Vector2 temp2 = new Vector2();
	// private static final Vector2 currentLineStartPoint0 = new Vector2();
	// private static final Vector2 currentLineStartPoint1 = new Vector2();
	// private static final Vector2 currentLineEndPoint0 = new Vector2();
	// private static final Vector2 currentLineEndPoint1 = new Vector2();
	// private static final Vector2 nextLineStartPoint0 = new Vector2();
	// private static final Vector2 nextLineStartPoint1 = new Vector2();
	// private static final Vector2 nextLineEndPoint0 = new Vector2();
	// private static final Vector2 nextLineEndPoint1 = new Vector2();
	// private static final Vector2 currentLineVector = new Vector2();
	// private static final Vector2 nextLineVector = new Vector2();
	// private static final Vector2 intersectionPoint = new Vector2();
	// private static final Vector2 iCurrentVector = new Vector2();
	// private static final Vector2 iNextVector = new Vector2();
	// private static final Vector2 tempCapVector = new Vector2();

	// private static LttlMesh generateLineTwoPoints(LttlMesh mesh,
	// Vector2Array points, Joint cornerType, Cap capTypeStart,
	// Cap capTypeEnd, float width, FloatArray lineWidths, float offset,
	// float aaWidth, int capCornerSteps)
	// {
	// // generate line //
	//
	// // clear and init temporary variables
	// temp0.set(0, 0);
	// currentLineVector.set(0, 0);
	// verticesArrayShared.clear();
	// orderedVertices0Shared.clear();
	// orderedVertices1Shared.clear();
	// indicesArrayShared.clear();
	//
	// verticesArrayShared
	// .ensureCapacity(4 * LttlMesh.VERTICE_ATTRIBUTE_COUNT);
	//
	// points.get(1, currentLineVector).sub(points.getX(0), points.getY(0))
	// .nor();
	//
	// if (offset != 0)
	// {
	// // preserve the points array
	// offsettedPointsShared.clear();
	// offsettedPointsShared.addAll(points);
	// points = Offset(offsettedPointsShared, offset);
	// }
	//
	// float lWidth = ((lineWidths != null) ? lineWidths.get(0) : width) / 2f;
	// points.get(0, temp0).add(-currentLineVector.y * lWidth,
	// currentLineVector.x * lWidth);
	// mesh.addVertice(temp0, 1, 1, 0, 1);
	//
	// lWidth = ((lineWidths != null) ? lineWidths.get(1) : width) / 2f;
	// points.get(1, temp0).add(-currentLineVector.y * lWidth,
	// currentLineVector.x * lWidth);
	// orderedVertices0Shared.addAll(0, 1);
	// mesh.addVertice(temp0, 1, 1, 0, 1);
	//
	// points.get(1, temp0).add(currentLineVector.y * lWidth,
	// -currentLineVector.x * lWidth);
	// mesh.addVertice(temp0, 1, 1, 0, 1);
	//
	// lWidth = ((lineWidths != null) ? lineWidths.get(0) : width) / 2f;
	// points.get(0, temp0).add(currentLineVector.y * lWidth,
	// -currentLineVector.x * lWidth);
	// mesh.addVertice(temp0, 1, 1, 0, 1);
	//
	// orderedVertices1Shared.addAll(2, 3);
	//
	// indicesArrayShared.addAll(new short[]
	// { 0, 1, 2, 3, 0, 2 });
	//
	// // generate caps //
	// if (capTypeStart == Cap.ROUND)
	// {
	// // begin cap
	// // OPTIMIZE use LttlMath.Angle() and lookup table, lttlmath Angle is clockwise, not counter
	// // clockwise so may need to do some adjusting
	// addArcEnd(verticesArrayShared, indicesArrayShared,
	// orderedVertices1Shared, points.getX(0), points.getY(0),
	// temp0.set(-currentLineVector.y, currentLineVector.x)
	// .angle(), 180, width / 2f, 0, 3, capCornerSteps);
	// }
	// if (capTypeEnd == Cap.ROUND)
	// {
	// // end cap
	// addArcEnd(verticesArrayShared, indicesArrayShared,
	// orderedVertices0Shared, points.getX(1), points.getY(1),
	// temp0.set(currentLineVector.y, -currentLineVector.x)
	// .angle(), 180, width / 2f, 2, 1, capCornerSteps);
	// }
	//
	// orderedVertices0Shared.addAll(orderedVertices1Shared);
	//
	// if (aaWidth > 0)
	// {
	// LttlAntiAliaser.AddAntiAliasing(mesh, verticesArrayShared,
	// indicesArrayShared, orderedVertices0Shared, aaWidth, false);
	// }
	//
	// return getFreshMesh(mesh, verticesArrayShared, indicesArrayShared);
	// }
	//
	// private static LttlMesh generateLineManyPoints(LttlMesh mesh,
	// Vector2Array points, Joint cornerType, Cap capTypeStart,
	// Cap capTypeEnd, int capCornerSteps, float width,
	// FloatArray lineWidths, float offset, boolean closed, float aaWidth)
	// {
	// // offset line //
	// if (offset != 0)
	// {
	// // preserve the given points array
	// offsettedPointsShared.clear();
	// offsettedPointsShared.addAll(points);
	// points = Offset(offsettedPointsShared, offset);
	// }
	// // clear and init shared variables
	// temp0.set(0, 0);
	// temp1.set(0, 0);
	// temp2.set(0, 0);
	// currentLineStartPoint0.set(0, 0);
	// currentLineStartPoint1.set(0, 0);
	// currentLineEndPoint0.set(0, 0);
	// currentLineEndPoint1.set(0, 0);
	// nextLineStartPoint0.set(0, 0);
	// nextLineStartPoint1.set(0, 0);
	// nextLineEndPoint0.set(0, 0);
	// nextLineEndPoint1.set(0, 0);
	// currentLineVector.set(0, 0);
	// nextLineVector.set(0, 0);
	// intersectionPoint.set(0, 0);
	// iCurrentVector.set(0, 0);
	// iNextVector.set(0, 0);
	// // saves indice of previous line segment, to recall later, i think this really just the last vertice index
	// int prevIndice0;
	// int prevIndice1;
	//
	// float lWidth;
	//
	// // just an estiamte for ensuring capacity
	// verticesArrayShared.clear();
	// verticesArrayShared.ensureCapacity(points.size() * 2
	// * LttlMesh.VERTICE_ATTRIBUTE_COUNT);
	//
	// indicesArrayShared.clear();
	// indicesArrayShared.ensureCapacity((points.size() - 1) * 6);
	//
	// orderedVertices0Shared.clear();
	// orderedVertices0Shared.ensureCapacity(points.size() * 2);
	//
	// orderedVertices1Shared.clear();
	// orderedVertices1Shared.ensureCapacity(points.size() * 2);
	//
	// // ** generate line **//
	// if (closed)
	// {
	// currentLineVector.set(points.getSharedFirst())
	// .sub(points.getSharedLast()).nor();
	// nextLineVector.set(currentLineVector);
	//
	// lWidth = ((lineWidths != null) ? lineWidths
	// .get(lineWidths.size - 1) : width) / 2f;
	// currentLineStartPoint0.set(points.getSharedLast())
	// .add(-currentLineVector.y * lWidth,
	// currentLineVector.x * lWidth);
	// nextLineStartPoint0.set(currentLineStartPoint0);
	// mesh.addVertice(currentLineStartPoint0, 1,
	// 1, 0, 1);
	//
	// // if (drawProgDebugEnabled)
	// // Lttl.debug.drawCircle(currentLineStartPoint0, 5, new Color(0,
	// // 0, 0, .5f));
	//
	// prevIndice0 = (verticesArrayShared.size / LttlMesh.VERTICE_ATTRIBUTE_COUNT) - 1;
	// currentLineStartPoint1.set(points.getSharedLast())
	// .add(currentLineVector.y * lWidth,
	// -currentLineVector.x * lWidth);
	// mesh.addVertice(currentLineStartPoint1, 1,
	// 1, 0, 1);
	//
	// // if (drawProgDebugEnabled)
	// // Lttl.debug.drawCircle(currentLineStartPoint1, 5, new Color(1,
	// // 0, 0, .5f));
	//
	// lWidth = ((lineWidths != null) ? lineWidths.first() : width) / 2f;
	// nextLineStartPoint1.set(currentLineStartPoint1);
	// prevIndice1 = (verticesArrayShared.size / LttlMesh.VERTICE_ATTRIBUTE_COUNT) - 1;
	//
	// currentLineEndPoint0.set(points.getSharedFirst())
	// .add(-currentLineVector.y * lWidth,
	// currentLineVector.x * lWidth);
	// nextLineEndPoint0.set(currentLineEndPoint0);
	// currentLineEndPoint1.set(points.getSharedFirst())
	// .add(currentLineVector.y * lWidth,
	// -currentLineVector.x * lWidth);
	// nextLineEndPoint1.set(currentLineEndPoint1);
	//
	// if (cornerType == Joint.MITER)
	// {
	// orderedVertices0Shared.add(0);
	// orderedVertices1Shared.add(1);
	// }
	// else if (cornerType == Joint.BEVEL)
	// {
	// nextLineVector.set(points.getSharedLast())
	// .sub(points.getShared(points.size() - 2)).nor();
	// boolean bendLeft = true;
	// if (temp2.set(nextLineVector).crs(currentLineVector) > 0)
	// {
	// bendLeft = false;
	// }
	// if (bendLeft)
	// {
	// orderedVertices0Shared.add(0);
	// }
	// else
	// {
	// orderedVertices1Shared.add(1);
	// }
	// nextLineVector.set(currentLineVector);
	//
	// }
	// }
	// else
	// {
	// // add first point
	// // only need to derive the currentLineVector if it's the first point
	// currentLineVector.set(points.getShared(1))
	// .sub(points.getSharedFirst()).nor();
	// nextLineVector.set(currentLineVector);
	//
	// lWidth = ((lineWidths != null) ? lineWidths.first() : width) / 2f;
	// // calculate the currentLine start points (and prep for first iteration)
	// currentLineStartPoint0.set(points.getSharedFirst())
	// .add(-currentLineVector.y * lWidth,
	// currentLineVector.x * lWidth);
	// nextLineStartPoint0.set(currentLineStartPoint0);
	// mesh.addVertice(currentLineStartPoint0, 1,
	// 1, 0, 1);
	//
	// orderedVertices0Shared.add(0);
	// // if (drawProgDebugEnabled)
	// // Lttl.debug.drawCircle(currentLineStartPoint0, 5, new Color(0,
	// // 0, 0, .5f));
	//
	// prevIndice0 = (verticesArrayShared.size / LttlMesh.VERTICE_ATTRIBUTE_COUNT) - 1;
	// currentLineStartPoint1.set(points.getSharedFirst())
	// .add(currentLineVector.y * lWidth,
	// -currentLineVector.x * lWidth);
	// mesh.addVertice(currentLineStartPoint1, 1,
	// 1, 0, 1);
	//
	// orderedVertices1Shared.add(1);
	// // if (drawProgDebugEnabled)
	// // Lttl.debug.drawCircle(currentLineStartPoint1, 5, new Color(1,
	// // 0, 0, .5f));
	//
	// nextLineStartPoint1.set(currentLineStartPoint1);
	// prevIndice1 = (verticesArrayShared.size / LttlMesh.VERTICE_ATTRIBUTE_COUNT) - 1;
	//
	// lWidth = ((lineWidths != null) ? lineWidths.get(1) : width) / 2f;
	// // calculate the currentLine end points (and prep for first iteration)
	// currentLineEndPoint0.set(points.getShared(1))
	// .add(-currentLineVector.y * lWidth,
	// currentLineVector.x * lWidth);
	// nextLineEndPoint0.set(currentLineEndPoint0);
	// currentLineEndPoint1.set(points.getShared(1))
	// .add(currentLineVector.y * lWidth,
	// -currentLineVector.x * lWidth);
	// nextLineEndPoint1.set(currentLineEndPoint1);
	// }
	//
	// // iterate through all the points
	// for (int i = (closed) ? 0 : 1; i < points.size() - ((closed) ? 0 : 1); i++)
	// {
	// // use last iteration's nextLineVector for this line's current
	// currentLineVector.set(nextLineVector);
	//
	// int nextIndex = i + 1;
	// if (i == points.size() - 1)
	// {
	// nextIndex = 0;
	// }
	//
	// // update this iteration's vector2, so only have to get once
	// points.get(i, iCurrentVector);
	// points.get(nextIndex, iNextVector);
	//
	// // calculate next line vector
	// nextLineVector.set(iNextVector).sub(iCurrentVector).nor();
	//
	// // grab previously calculated currentLine points
	// currentLineStartPoint0.set(nextLineStartPoint0);
	// currentLineStartPoint1.set(nextLineStartPoint1);
	// currentLineEndPoint0.set(nextLineEndPoint0);
	// currentLineEndPoint1.set(nextLineEndPoint1);
	//
	// // calculate the nextLine start points
	// lWidth = ((lineWidths != null) ? lineWidths.get(i) : width) / 2f;
	// nextLineStartPoint0.set(iCurrentVector).add(
	// -nextLineVector.y * lWidth, nextLineVector.x * lWidth);
	// nextLineStartPoint1.set(iCurrentVector).add(
	// nextLineVector.y * lWidth, -nextLineVector.x * lWidth);
	//
	// // calculate the nextLine end points
	// lWidth = ((lineWidths != null) ? lineWidths.get(nextIndex) : width) / 2f;
	// nextLineEndPoint0.set(iNextVector).add(-nextLineVector.y * lWidth,
	// nextLineVector.x * lWidth);
	// nextLineEndPoint1.set(iNextVector).add(nextLineVector.y * lWidth,
	// -nextLineVector.x * lWidth);
	//
	// // figure out which side is the bend and which is the intersect
	// boolean bendLeft = true;
	// if (temp2.set(currentLineVector).crs(nextLineVector) > 0)
	// {
	// bendLeft = false;
	// }
	//
	// // get insersection point
	// if (bendLeft)
	// {
	// LttlMath.LineIntersection(intersectionPoint,
	// currentLineStartPoint1.x, currentLineStartPoint1.y,
	// currentLineEndPoint1.x, currentLineEndPoint1.y,
	// nextLineStartPoint1.x, nextLineStartPoint1.y,
	// nextLineEndPoint1.x, nextLineEndPoint1.y);
	// }
	// else
	// {
	// LttlMath.LineIntersection(intersectionPoint,
	// currentLineStartPoint0.x, currentLineStartPoint0.y,
	// currentLineEndPoint0.x, currentLineEndPoint0.y,
	// nextLineStartPoint0.x, nextLineStartPoint0.y,
	// nextLineEndPoint0.x, nextLineEndPoint0.y);
	// }
	//
	// // add end point vertices and indices
	// int currentIndice0 = 0;
	// int currentIndice1 = 0;
	// if (cornerType == Joint.ROUND || cornerType == Joint.BEVEL)
	// {
	// if (bendLeft)
	// {
	// LttlMesh.addVertice(verticesArrayShared,
	// currentLineEndPoint0, 1, 1, 0, 1);
	// currentIndice0 = verticesArrayShared.size
	// / LttlMesh.VERTICE_ATTRIBUTE_COUNT - 1;
	//
	// orderedVertices0Shared.add(currentIndice0);
	// // if (drawProgDebugEnabled)
	// // Lttl.debug.drawCircle(currentLineEndPoint0, 5,
	// // new Color(0, 0, 0, .5f));
	//
	// mesh.addVertice(intersectionPoint,
	// 1, 1, 0, 1);
	// currentIndice1 = verticesArrayShared.size
	// / LttlMesh.VERTICE_ATTRIBUTE_COUNT - 1;
	//
	// orderedVertices1Shared.add(currentIndice1);
	// // if (drawProgDebugEnabled)
	// // Lttl.debug.drawCircle(intersectionPoint, 5, new Color(
	// // 1, 0, 0, .5f));
	// }
	// else
	// {
	// mesh.addVertice(intersectionPoint,
	// 1, 1, 0, 1);
	// currentIndice0 = verticesArrayShared.size
	// / LttlMesh.VERTICE_ATTRIBUTE_COUNT - 1;
	//
	// orderedVertices0Shared.add(currentIndice0);
	// // if (drawProgDebugEnabled)
	// // Lttl.debug.drawCircle(intersectionPoint, 5, new Color(
	// // 0, 0, 0, .5f));
	//
	// LttlMesh.addVertice(verticesArrayShared,
	// currentLineEndPoint1, 1, 1, 0, 1);
	// currentIndice1 = verticesArrayShared.size
	// / LttlMesh.VERTICE_ATTRIBUTE_COUNT - 1;
	//
	// orderedVertices1Shared.add(currentIndice1);
	// // if (drawProgDebugEnabled)
	// // Lttl.debug.drawCircle(currentLineEndPoint1, 5,
	// // new Color(1, 0, 0, .5f));
	// }
	// }
	// else if (cornerType == Joint.MITER)
	// {
	// if (bendLeft)
	// {
	// LttlMath.LineIntersection(temp1, currentLineStartPoint0.x,
	// currentLineStartPoint0.y, currentLineEndPoint0.x,
	// currentLineEndPoint0.y, nextLineStartPoint0.x,
	// nextLineStartPoint0.y, nextLineEndPoint0.x,
	// nextLineEndPoint0.y);
	//
	// if (closed && i == points.size() - 1)
	// {
	// verticesArrayShared.set(0, temp1.x);
	// verticesArrayShared.set(1, temp1.y);
	// verticesArrayShared.set(
	// LttlMesh.VERTICE_ATTRIBUTE_COUNT + 0,
	// intersectionPoint.x);
	// verticesArrayShared.set(
	// LttlMesh.VERTICE_ATTRIBUTE_COUNT + 1,
	// intersectionPoint.y);
	// currentIndice0 = 0;
	// currentIndice1 = 1;
	// }
	// else
	// {
	// mesh.addVertice(temp1, 1, 1,
	// 0, 1);
	// currentIndice0 = verticesArrayShared.size
	// / LttlMesh.VERTICE_ATTRIBUTE_COUNT - 1;
	//
	// orderedVertices0Shared.add(currentIndice0);
	// // if (drawProgDebugEnabled)
	// // Lttl.debug.drawCircle(temp1, 5, new Color(0, 0, 0,
	// // .5f));
	//
	// LttlMesh.addVertice(verticesArrayShared,
	// intersectionPoint, 1, 1, 0, 1);
	// currentIndice1 = verticesArrayShared.size
	// / LttlMesh.VERTICE_ATTRIBUTE_COUNT - 1;
	//
	// orderedVertices1Shared.add(currentIndice1);
	// // if (drawProgDebugEnabled)
	// // Lttl.debug.drawCircle(intersectionPoint, 5,
	// // new Color(1, 0, 0, .5f));
	// }
	// }
	// else
	// {
	// LttlMath.LineIntersection(temp1, currentLineStartPoint1.x,
	// currentLineStartPoint1.y, currentLineEndPoint1.x,
	// currentLineEndPoint1.y, nextLineStartPoint1.x,
	// nextLineStartPoint1.y, nextLineEndPoint1.x,
	// nextLineEndPoint1.y);
	// if (closed && i == points.size() - 1)
	// {
	// verticesArrayShared.set(0, intersectionPoint.x);
	// verticesArrayShared.set(1, intersectionPoint.y);
	// verticesArrayShared.set(
	// LttlMesh.VERTICE_ATTRIBUTE_COUNT + 0, temp1.x);
	// verticesArrayShared.set(
	// LttlMesh.VERTICE_ATTRIBUTE_COUNT + 1, temp1.y);
	// currentIndice0 = 0;
	// currentIndice1 = 1;
	// }
	// else
	// {
	// LttlMesh.addVertice(verticesArrayShared,
	// intersectionPoint, 1, 1, 0, 1);
	// currentIndice0 = verticesArrayShared.size
	// / LttlMesh.VERTICE_ATTRIBUTE_COUNT - 1;
	//
	// orderedVertices0Shared.add(currentIndice0);
	// // if (drawProgDebugEnabled)
	// // Lttl.debug.drawCircle(intersectionPoint, 5,
	// // new Color(0, 0, 0, .5f));
	//
	// mesh.addVertice(temp1, 1, 1,
	// 0, 1);
	// currentIndice1 = verticesArrayShared.size
	// / LttlMesh.VERTICE_ATTRIBUTE_COUNT - 1;
	//
	// orderedVertices1Shared.add(currentIndice1);
	// // if (drawProgDebugEnabled)
	// // Lttl.debug.drawCircle(temp1, 5, new Color(1, 0, 0,
	// // .5f));
	// }
	// }
	// }
	//
	// indicesArrayShared.ensureCapacity(6);
	// indicesArrayShared.add(prevIndice0);
	// indicesArrayShared.add(currentIndice0);
	// indicesArrayShared.add(currentIndice1);
	// indicesArrayShared.add(prevIndice1);
	// indicesArrayShared.add(prevIndice0);
	// indicesArrayShared.add(currentIndice1);
	//
	// if (cornerType == Joint.MITER)
	// {
	// prevIndice0 = currentIndice0;
	// prevIndice1 = currentIndice1;
	// }
	//
	// // add next line segments vertices
	// int nextIndice0 = 0;
	// int nextIndice1 = 0;
	// if (cornerType == Joint.ROUND)
	// {
	// if (bendLeft)
	// {
	// if (closed && i == points.size() - 1)
	// {
	// verticesArrayShared.set(0, nextLineStartPoint0.x);
	// verticesArrayShared.set(1, nextLineStartPoint0.y);
	// verticesArrayShared.set(
	// LttlMesh.VERTICE_ATTRIBUTE_COUNT + 0,
	// intersectionPoint.x);
	// verticesArrayShared.set(
	// LttlMesh.VERTICE_ATTRIBUTE_COUNT + 1,
	// intersectionPoint.y);
	// nextIndice0 = 0;
	// nextIndice1 = 1;
	// }
	// else
	// {
	// LttlMesh.addVertice(verticesArrayShared,
	// nextLineStartPoint0, 1, 1, 0, 1);
	//
	// nextIndice0 = verticesArrayShared.size
	// / LttlMesh.VERTICE_ATTRIBUTE_COUNT - 1;
	//
	// // if (drawProgDebugEnabled)
	// // Lttl.debug.drawCircle(nextLineStartPoint0, 5,
	// // new Color(0, 0, 0, .5f));
	//
	// nextIndice1 = currentIndice1;
	// }
	// prevIndice0 = nextIndice0;
	// prevIndice1 = nextIndice1;
	// }
	// else
	// {
	// if (closed && i == points.size() - 1)
	// {
	// verticesArrayShared.set(0, intersectionPoint.x);
	// verticesArrayShared.set(1, intersectionPoint.y);
	// verticesArrayShared.set(
	// LttlMesh.VERTICE_ATTRIBUTE_COUNT + 0,
	// nextLineStartPoint1.x);
	// verticesArrayShared.set(
	// LttlMesh.VERTICE_ATTRIBUTE_COUNT + 1,
	// nextLineStartPoint1.y);
	// nextIndice0 = 0;
	// nextIndice1 = 1;
	// }
	// else
	// {
	// nextIndice0 = currentIndice0;
	// LttlMesh.addVertice(verticesArrayShared,
	// nextLineStartPoint1, 1, 1, 0, 1);
	// nextIndice1 = verticesArrayShared.size
	// / LttlMesh.VERTICE_ATTRIBUTE_COUNT - 1;
	//
	// // if (drawProgDebugEnabled)
	// // Lttl.debug.drawCircle(nextLineStartPoint1, 5,
	// // new Color(1, 0, 0, .5f));
	// }
	// prevIndice0 = nextIndice0;
	// prevIndice1 = nextIndice1;
	// }
	// }
	//
	// if (cornerType == Joint.BEVEL)
	// {
	// indicesArrayShared.ensureCapacity(3);
	// if (bendLeft)
	// {
	// if (closed && i == points.size() - 1)
	// {
	// verticesArrayShared.set(0, nextLineStartPoint0.x);
	// verticesArrayShared.set(1, nextLineStartPoint0.y);
	// verticesArrayShared.set(
	// LttlMesh.VERTICE_ATTRIBUTE_COUNT + 0,
	// intersectionPoint.x);
	// verticesArrayShared.set(
	// LttlMesh.VERTICE_ATTRIBUTE_COUNT + 1,
	// intersectionPoint.y);
	// nextIndice0 = 0;
	// nextIndice1 = 1;
	// indicesArrayShared.add(currentIndice0);
	// indicesArrayShared.add((nextIndice0));
	// indicesArrayShared.add((nextIndice1));
	// }
	// else
	// {
	// LttlMesh.addVertice(verticesArrayShared,
	// nextLineStartPoint0, 1, 1, 0, 1);
	// nextIndice0 = verticesArrayShared.size
	// / LttlMesh.VERTICE_ATTRIBUTE_COUNT - 1;
	//
	// orderedVertices0Shared.add(nextIndice0);
	// // if (drawProgDebugEnabled)
	// // Lttl.debug.drawCircle(nextLineStartPoint0, 5,
	// // new Color(0, 0, 0, .5f));
	//
	// nextIndice1 = currentIndice1;
	// indicesArrayShared.add(currentIndice0);
	// indicesArrayShared.add((nextIndice0));
	// indicesArrayShared.add((nextIndice1));
	// }
	//
	// }
	// else
	// {
	// if (closed && i == points.size() - 1)
	// {
	// verticesArrayShared.set(0, intersectionPoint.x);
	// verticesArrayShared.set(1, intersectionPoint.y);
	// verticesArrayShared.set(
	// LttlMesh.VERTICE_ATTRIBUTE_COUNT + 0,
	// nextLineStartPoint1.x);
	// verticesArrayShared.set(
	// LttlMesh.VERTICE_ATTRIBUTE_COUNT + 1,
	// nextLineStartPoint1.y);
	// nextIndice0 = 0;
	// nextIndice1 = 1;
	// }
	// else
	// {
	// nextIndice0 = currentIndice0;
	// LttlMesh.addVertice(verticesArrayShared,
	// nextLineStartPoint1, 1, 1, 0, 1);
	// nextIndice1 = verticesArrayShared.size
	// / LttlMesh.VERTICE_ATTRIBUTE_COUNT - 1;
	//
	// orderedVertices1Shared.add(nextIndice1);
	// // if (drawProgDebugEnabled)
	// // Lttl.debug.drawCircle(nextLineStartPoint1, 5,
	// // new Color(1, 0, 0, .5f));
	//
	// }
	// indicesArrayShared.add(currentIndice1);
	// indicesArrayShared.add((nextIndice0));
	// indicesArrayShared.add((nextIndice1));
	//
	// }
	//
	// prevIndice0 = nextIndice0;
	// prevIndice1 = nextIndice1;
	// }
	//
	// if (cornerType == Joint.ROUND)
	// {
	// // arc//
	// if (bendLeft)
	// {
	// float interp = temp1.set(nextLineStartPoint0).dst(
	// currentLineEndPoint0)
	// / width;
	// interp = LttlMath.easeInCirc(0, 1, interp);
	//
	// // get midpoint
	// temp0.set(currentLineEndPoint0).add(nextLineStartPoint0)
	// .scl(.5f);
	// temp1.set(temp0).sub(intersectionPoint).nor();
	//
	// temp0.add(temp1.scl(width / 2 * -(1 - interp)));
	//
	// // get angles
	// // OPTIMIZE use LttlMath.Angle() and lookup table, lttlmath Angle is clockwise, not counter
	// // clockwise so may need to do some adjusting
	// temp1.set(nextLineStartPoint0).sub(temp0);
	// float temp1Angle = temp1.angle();
	// temp2.set(currentLineEndPoint0).sub(temp0);
	// float temp2Angle = temp2.angle();
	//
	// addArcCorner(
	// verticesArrayShared,
	// indicesArrayShared,
	// orderedVertices0Shared,
	// orderedVertices1Shared,
	// temp0.x,
	// temp0.y,
	// currentIndice1,
	// LttlMath.ConstrainDegrees360(temp1Angle),
	// LttlMath.ConstrainDegrees360(temp2Angle
	// - temp1Angle), width / 2, capCornerSteps,
	// nextIndice0, currentIndice0, true);
	//
	// orderedVertices0Shared.add(nextIndice0);
	// }
	// else
	// {
	// float interp = temp1.set(currentLineEndPoint1).dst(
	// nextLineStartPoint1)
	// / width;
	// interp = LttlMath.easeInCirc(0, 1, interp);
	//
	// // get midpoint
	// temp0.set(currentLineEndPoint1).add(nextLineStartPoint1)
	// .scl(.5f);
	// temp1.set(temp0).sub(intersectionPoint).nor();
	//
	// temp0.add(temp1.scl(width / 2 * -(1 - interp)));
	//
	// // get angles
	// temp1.set(currentLineEndPoint1).sub(temp0);
	// float temp1Angle = temp1.angle();
	// temp2.set(nextLineStartPoint1).sub(temp0);
	// float temp2Angle = temp2.angle();
	//
	// addArcCorner(
	// verticesArrayShared,
	// indicesArrayShared,
	// orderedVertices1Shared,
	// orderedVertices0Shared,
	// temp0.x,
	// temp0.y,
	// currentIndice0,
	// LttlMath.ConstrainDegrees360(temp1Angle),
	// LttlMath.ConstrainDegrees360(temp2Angle
	// - temp1Angle), width / 2, capCornerSteps,
	// currentIndice1, nextIndice1, false);
	//
	// orderedVertices1Shared.add(nextIndice1);
	// }
	// }
	//
	// }
	//
	// if (!closed)
	// {
	// // add last point
	// int currentIndice0;
	// int currentIndice1;
	// mesh.addVertice(nextLineEndPoint0, 1, 1,
	// 0, 1);
	// currentIndice0 = verticesArrayShared.size
	// / LttlMesh.VERTICE_ATTRIBUTE_COUNT - 1;
	//
	// orderedVertices0Shared.add(currentIndice0);
	// // if (drawProgDebugEnabled)
	// // Lttl.debug.drawCircle(nextLineEndPoint0, 5, new Color(0, 0, 0,
	// // .5f));
	//
	// mesh.addVertice(nextLineEndPoint1, 1, 1,
	// 0, 1);
	// currentIndice1 = verticesArrayShared.size
	// / LttlMesh.VERTICE_ATTRIBUTE_COUNT - 1;
	//
	// orderedVertices1Shared.add(currentIndice1);
	// // if (drawProgDebugEnabled)
	// // Lttl.debug.drawCircle(nextLineEndPoint1, 5, new Color(1, 0, 0,
	// // .5f));
	//
	// indicesArrayShared.ensureCapacity(6);
	// indicesArrayShared.add(prevIndice0);
	// indicesArrayShared.add(currentIndice0);
	// indicesArrayShared.add(currentIndice1);
	// indicesArrayShared.add(prevIndice1);
	// indicesArrayShared.add(prevIndice0);
	// indicesArrayShared.add(currentIndice1);
	//
	// orderedVertices1Shared.reverse();
	//
	// // generate caps //
	// if (capTypeEnd == Cap.ROUND)
	// {
	// // end cap
	// currentLineVector.set(points.getSharedLast())
	// .sub(points.getShared(points.size() - 2)).nor();
	// addArcEnd(
	// verticesArrayShared,
	// indicesArrayShared,
	// orderedVertices0Shared,
	// points.getLastX(),
	// points.getLastY(),
	// tempCapVector.set(currentLineVector.y,
	// -currentLineVector.x).angle(), 180, width / 2f,
	// currentIndice1, currentIndice0, capCornerSteps);
	// }
	// if (capTypeStart == Cap.ROUND)
	// {
	// // begin cap
	// currentLineVector.set(points.getShared(1))
	// .sub(points.getSharedFirst()).nor();
	// addArcEnd(
	// verticesArrayShared,
	// indicesArrayShared,
	// orderedVertices1Shared,
	// points.getFirstX(),
	// points.getFirstY(),
	// tempCapVector.set(-currentLineVector.y,
	// currentLineVector.x).angle(), 180, width / 2f,
	// 0, 1, capCornerSteps);
	// }
	//
	// orderedVertices0Shared.addAll(orderedVertices1Shared);
	// // LttlLog.logArrayShort(orderedVertices0Temp);
	//
	// if (aaWidth > 0)
	// {
	// LttlAntiAliaser.AddAntiAliasing(mesh, verticesArrayShared,
	// indicesArrayShared, orderedVertices0Shared, aaWidth,
	// false);
	// }
	// }
	// else
	// {
	// // if (drawProgDebugEnabled)
	// // {
	// // for (int s = 0; s < orderedVertices0Shared.size; s++)
	// // {
	// // Lttl.debug.drawCircle(
	// // getVertexPos(orderedVertices0Shared.get(s),
	// // verticesArrayShared, temp0), 2, new Color(
	// // 1, 1, 1, .3f));
	// // }
	// // }
	//
	// if (aaWidth > 0)
	// {
	// // LttlLog.logArrayShort(orderedVertices0Temp);
	// // orderedVertices0Temp.remove(orderedVertices0Temp.size - 1);
	// LttlAntiAliaser.AddAntiAliasing(mesh, verticesArrayShared,
	// indicesArrayShared, orderedVertices0Shared, aaWidth,
	// false);
	//
	// orderedVertices1Shared.reverse();
	// LttlAntiAliaser.AddAntiAliasing(mesh, verticesArrayShared,
	// indicesArrayShared, orderedVertices1Shared, aaWidth,
	// false);
	// }
	//
	// // if (drawProgDebugEnabled)
	// // {
	// // for (int s = 0; s < orderedVertices1Shared.size; s++)
	// // {
	// // Lttl.debug.drawCircle(
	// // getVertexPos(orderedVertices1Shared.get(s),
	// // verticesArrayShared, temp0), 2, new Color(
	// // 0, 0, 0, .3f));
	// // }
	// // }
	//
	// }
	//
	// return getFreshMesh(mesh, verticesArrayShared, indicesArrayShared);
	// }
	//
	// private static void addArcCorner(FloatArray verticesArray,
	// ShortArray indicesArray, IntArray orderedVerticesArc,
	// IntArray orderedVerticesOrigin, float centerX, float centerY,
	// int originIndice, float startAngle, float arcAngle, float radius,
	// int capCornerSteps, int beginningIndice, int endIndice,
	// boolean reverse)
	// {
	// float stepAngle = LttlMath.abs(arcAngle) / capCornerSteps;
	//
	// int startVerticesCount = verticesArray.size
	// / LttlMesh.VERTICE_ATTRIBUTE_COUNT;
	// if (capCornerSteps > 2)
	// {
	// verticesArray.ensureCapacity((capCornerSteps - 1)
	// * LttlMesh.VERTICE_ATTRIBUTE_COUNT);
	//
	// float a = 0;
	// for (float i = 0; i < capCornerSteps - 1; i++)
	// {
	// a += stepAngle;
	// float x = LttlMath
	// .cos((LttlMath.sign(arcAngle)
	// * LttlMath.clamp(a, 0, LttlMath.abs(arcAngle)) + startAngle)
	// * LttlMath.degreesToRadians);
	// float y = LttlMath
	// .sin((LttlMath.sign(arcAngle)
	// * LttlMath.clamp(a, 0, LttlMath.abs(arcAngle)) + startAngle)
	// * LttlMath.degreesToRadians);
	//
	// x = centerX + (x * radius);
	// y = centerY + (y * radius);
	//
	// LttlMesh.addVertice(verticesArray, x, y, 1, 1, 0, 1);
	//
	// // if (drawProgDebugEnabled)
	// // Lttl.debug.drawCircle(x, y, 1, new Color(0, 1, 0, .5f));
	// }
	//
	// indicesArray.ensureCapacity((capCornerSteps - 1) * 3);
	// for (int i = -1; i < capCornerSteps - 1; i++)
	// {
	// indicesArray.add(originIndice);
	// if (i == -1)
	// {
	// indicesArray.add(beginningIndice);
	// }
	// else
	// {
	// indicesArray.add(startVerticesCount + i);
	// }
	// if (i == capCornerSteps - 2)
	// {
	// indicesArray.add(endIndice);
	// }
	// else
	// {
	// indicesArray.add(startVerticesCount + i + 1);
	// }
	// }
	//
	// orderedVerticesArc.ensureCapacity((capCornerSteps - 1) * 3);
	// for (int i = 0; i < capCornerSteps - 1; i++)
	// {
	// if (reverse)
	// {
	// orderedVerticesArc.add(startVerticesCount + capCornerSteps
	// - 2 - i);
	// }
	// else
	// {
	// orderedVerticesArc.add(startVerticesCount + i);
	// }
	// }
	// }
	// else
	// {
	// indicesArray.ensureCapacity(3);
	// indicesArray.add(originIndice);
	// indicesArray.add(beginningIndice);
	// indicesArray.add(endIndice);
	// }
	//
	// }
	//
	// private static void addArcEnd(FloatArray verticesArray,
	// ShortArray indicesArray, IntArray orderedVertices, float centerX,
	// float centerY, float startAngle, float arcAngle, float radius,
	// int beginningIndice, int endIndice, int steps)
	// {
	// float stepAngle = LttlMath.abs(arcAngle) / steps;
	//
	// int startVerticesCount = verticesArray.size
	// / LttlMesh.VERTICE_ATTRIBUTE_COUNT;
	//
	// if (steps < 3) return;
	//
	// float a = 0;
	//
	// verticesArray.ensureCapacity((steps - 1)
	// * LttlMesh.VERTICE_ATTRIBUTE_COUNT);
	// orderedVertices.ensureCapacity(steps - 1);
	//
	// for (float i = 0; i < steps - 1; i++)
	// {
	// a += stepAngle;
	// float x = LttlMath
	// .cos((LttlMath.sign(arcAngle)
	// * LttlMath.clamp(a, 0, LttlMath.abs(arcAngle)) + startAngle)
	// * LttlMath.degreesToRadians);
	// float y = LttlMath
	// .sin((LttlMath.sign(arcAngle)
	// * LttlMath.clamp(a, 0, LttlMath.abs(arcAngle)) + startAngle)
	// * LttlMath.degreesToRadians);
	//
	// x = centerX + (x * radius);
	// y = centerY + (y * radius);
	//
	// LttlMesh.addVertice(verticesArray, x, y, 1, 1, 0, 1);
	//
	// // if (drawProgDebugEnabled)
	// // Lttl.debug.drawCircle(x, y, 1, new Color(0, 1, 0, .5f * i
	// // / (steps - 1) + .2f));
	//
	// orderedVertices.add((int) (startVerticesCount + (steps - 2) - i));
	// }
	//
	// indicesArray.ensureCapacity((steps - 1) * 3);
	// for (int i = 0; i < steps - 1; i++)
	// {
	// indicesArray.add((beginningIndice));
	// indicesArray.add((startVerticesCount + i));
	// if (i == steps - 2)
	// {
	// indicesArray.add((endIndice));
	// }
	// else
	// {
	// indicesArray.add((startVerticesCount + i + 1));
	// }
	// }
	// }

	/**
	 * Creates extra vertices by interpolating between vertices, including color, alpha, position, and uv
	 * 
	 * @param mesh
	 *            needs to have exactly 4 vertices (in order: top left, top right, bottom right, bottom left), before AA
	 * @param steps
	 *            number of steps to interpolate between vertices, 1 is same thing
	 * @param vertical
	 * @param colors
	 *            optionally give the 4 colors, one for each vertice (top left, top right, bottom right, bottom left),
	 *            can be null, will just use current vertice colors
	 * @return
	 */
	public static void DensifyQuad(LttlMesh mesh, int steps, boolean vertical,
			Color[] colors)
	{
		DensifyQuad(mesh.getVerticesArray(), mesh.getIndicesArray(), steps,
				vertical, colors);
	}

	/**
	 * Creates extra vertices by interpolating between vertices, including color, alpha, position, and uv
	 * 
	 * @param vertices
	 *            needs to have exactly 4 vertices, before AA
	 * @param indices
	 * @param steps
	 *            number of steps to interpolate between vertices, 0 is same thing and will do nothing
	 * @param vertical
	 * @param colors
	 *            optionally give the 4 colors, one for each vertice (top left,top right, bottom right, bottom left),
	 *            can be null, will just use current vertice colors
	 * @return
	 */
	public static void DensifyQuad(FloatArray vertices, ShortArray indices,
			int steps, boolean vertical, Color[] colors)
	{
		// early out if not changing anything
		if (steps < 1) return;
		// add one to steps, since that was what is was programmed for
		steps++;

		// TODO implement horizontal
		Lttl.Throw(!vertical);

		// needs to have exactly 4 vertices
		Lttl.Throw(LttlMesh.getVertexCount(vertices) != 4);
		Lttl.Throw(colors != null && colors.length != 4);

		// save initial vertices
		tmpFloatArray.clear();
		tmpFloatArray.addAll(vertices);

		// clear everything in mesh
		vertices.clear();
		indices.clear();
		int cap = 2 + steps * 2;
		vertices.ensureCapacity(cap * LttlMesh.VERTICE_ATTRIBUTE_COUNT);
		indices.ensureCapacity((cap - 2) * 3);

		// add vertices (from top left to top right to bottom right to bottom left, this way it can still have AA added)
		// top
		int a = 0;
		int b = 1;
		while (true)
		{
			float xA = LttlMesh.getX(tmpFloatArray, a);
			float xB = LttlMesh.getX(tmpFloatArray, b);
			float yA = LttlMesh.getY(tmpFloatArray, a);
			float yB = LttlMesh.getY(tmpFloatArray, b);
			float uA = LttlMesh.getU(tmpFloatArray, a);
			float uB = LttlMesh.getU(tmpFloatArray, b);
			float vA = LttlMesh.getV(tmpFloatArray, a);
			float vB = LttlMesh.getV(tmpFloatArray, b);
			Color colorA = colors == null ? null : colors[a];
			Color colorB = colors == null ? null : colors[b];
			// use colorBit if colors is null, just use same color for everything
			float colorBit = LttlMesh.getColor(tmpFloatArray, a);
			float alphaA = LttlMesh.getAlpha(tmpFloatArray, a);
			float alphaB = LttlMesh.getAlpha(tmpFloatArray, b);
			for (int i = 0; i <= steps; i++)
			{
				float percent = i / (float) steps;
				LttlMesh.addVertice(
						vertices,
						LttlMath.Lerp(xA, xB, percent),
						LttlMath.Lerp(yA, yB, percent),
						LttlMath.Lerp(uA, uB, percent),
						LttlMath.Lerp(vA, vB, percent),
						colorA == null ? colorBit : colorA == colorB ? colorA
								.toFloatBits() : tmpColor.set(colorA)
								.lerp(colorB, percent).toFloatBits(), LttlMath
								.Lerp(alphaA, alphaB, percent));
			}

			// run through bottom
			if (a == 2) break;
			a = 2;
			b = 3;
		}

		// add indices
		int size = LttlMesh.getVertexCount(vertices);
		for (int i = 0; i < steps; i++)
		{
			indices.add(i);
			indices.add(i + 1);
			indices.add(size - 1 - i);

			indices.add(i + 1);
			indices.add(size - 2 - i);
			indices.add(size - 1 - i);
		}
	}

	/**
	 * Returns the index of one of the corners of the original undesified quad mesh, AA is okay
	 * 
	 * @param desiredIndex
	 * @param densifySteps
	 * @return
	 */
	public static int getDensifiedQuadIndex(QuadCorners corner, int densifySteps)
	{
		switch (corner)
		{
			case TopLeft:
				return 0;
			case TopRight:
				return 1 + densifySteps;
			case BottomRight:
				return 1 + densifySteps + 1;
			case BottomLeft:
			default:
				return 1 + densifySteps + 1 + densifySteps + 1;
		}
	}

	/**
	 * Updates or generates a square/rect/quad mesh.<br>
	 * Vertice order: topLeft, top right, bottom right, bottom left
	 * 
	 * @param mesh
	 *            null for new mesh always, or provide mesh and will update it
	 * @param size
	 * @param aaWidth
	 *            (0 = none)
	 * @return
	 */
	public static LttlMesh GenerateQuad(LttlMesh mesh, float size, float aaWidth)
	{
		return GenerateQuad(mesh, size, size, aaWidth, 0, 1, 1, 0, 0);
	}

	/**
	 * Vertice order: topLeft, top right, bottom right, bottom left
	 * 
	 * @param mesh
	 * @param sizeX
	 * @param sizeY
	 * @param aaWidth
	 * @return
	 */
	public static LttlMesh GenerateQuad(LttlMesh mesh, float sizeX,
			float sizeY, float aaWidth)
	{
		return GenerateQuad(mesh, sizeX, sizeY, aaWidth, 0, 1, 1, 0, 0);
	}

	/**
	 * Generates a (1x1) quad with the UV dimensions of the atlas region. This is meant to be used with meshWidth.<br>
	 * Vertice order: topLeft, top right, bottom right, bottom left
	 * 
	 * @param mesh
	 *            (if null, will create new mesh)
	 * @param ar
	 * @param aaWidth
	 *            (optional) 0 means no aa width
	 * @return
	 */
	public static LttlMesh GenerateQuad(LttlMesh mesh, AtlasRegion ar,
			float sizeX, float sizeY, float aaWidth)
	{
		mesh = LttlMesh.getNew(mesh, 4);
		ShortArray indices = mesh.getIndicesArray();

		/* DO NOT CHANGE THIS ORDER */
		// top left
		mesh.addVertice(-sizeX / 2, sizeY / 2, ar.getU(), ar.getV2(), 0, 1);
		// top right
		mesh.addVertice(sizeX / 2, sizeY / 2, ar.getU2(), ar.getV2(), 0, 1);
		// bottom right
		mesh.addVertice(sizeX / 2, -sizeY / 2, ar.getU2(), ar.getV(), 0, 1);
		// bottom left
		mesh.addVertice(-sizeX / 2, -sizeY / 2, ar.getU(), ar.getV(), 0, 1);

		indices.addAll(new short[]
		{ 0, 1, 2, 2, 3, 0 });

		if (aaWidth > 0)
		{
			LttlAntiAliaser.AddAntiAliasingSimple(mesh, aaWidth, false, 0);
		}

		return mesh;
	}

	/**
	 * Vertice order: topLeft, top right, bottom right, bottom left
	 */
	public static LttlMesh GenerateQuad(LttlMesh mesh, float sizeX,
			float sizeY, float aaWidth, float angle, float scaleX,
			float scaleY, float offsetX, float offsetY)
	{
		mesh = LttlMesh.getNew(mesh, 4);
		FloatArray vertices = mesh.getVerticesArray();
		ShortArray indices = mesh.getIndicesArray();

		/* DO NOT CHANGE THIS ORDER */
		// top left
		mesh.addVertice(-sizeX / 2, sizeY / 2, 0, 1, 0, 1);
		// top right
		mesh.addVertice(sizeX / 2, sizeY / 2, 1, 1, 0, 1);
		// bottom right
		mesh.addVertice(sizeX / 2, -sizeY / 2, 1, 0, 0, 1);
		// bottom left
		mesh.addVertice(-sizeX / 2, -sizeY / 2, 0, 0, 0, 1);

		indices.addAll(new short[]
		{ 0, 1, 2, 2, 3, 0 });

		// transform UVS (will check if necessary in method)
		LttlMesh.transformUVs(vertices, angle, scaleX, scaleY, offsetX, offsetY);

		if (aaWidth > 0)
		{
			LttlAntiAliaser.AddAntiAliasingSimple(mesh, aaWidth, false, 0);
		}

		return mesh;
	}

	public static LttlMesh GenerateLineQuad(LttlMesh mesh, float aaWidth)
	{
		mesh = LttlMesh.getNew(mesh, 4);
		ShortArray indices = mesh.getIndicesArray();

		float sizeX = 1;
		float sizeY = 1;

		mesh.addVertice(-sizeX / 2, -sizeY / 2, 0, 0, 0, 1);
		mesh.addVertice(-sizeX / 2, sizeY / 2, 0, 1, 0, 1);
		mesh.addVertice(sizeX / 2, sizeY / 2, 1, 1, 0, 1);
		mesh.addVertice(sizeX / 2, -sizeY / 2, 1, 0, 0, 1);

		indices.addAll(new short[]
		{ 0, 1, 2, 2, 3, 0 });

		mesh.setAAIndiceIndex(indices.size);
		mesh.setAAVerticeIndex(mesh.getVertexCount());

		if (aaWidth > 0)
		{
			mesh.addVertice(-sizeX / 2, (-sizeY / 2) - aaWidth, 0, 0, 0, 0);
			mesh.addVertice(-sizeX / 2, (sizeY / 2) + aaWidth, 0, 1, 0, 0);
			mesh.addVertice(sizeX / 2, (sizeY / 2) + aaWidth, 1, 1, 0, 0);
			mesh.addVertice(sizeX / 2, (-sizeY / 2) - aaWidth, 1, 0, 0, 0);
			indices.addAll(new short[]
			{ 1, 5, 2, 2, 5, 6, 4, 3, 7, 0, 3, 4 });
		}

		return mesh;
	}

	/**
	 * Creates a circle mesh, tries to update provided if all possible.
	 * 
	 * @param mesh
	 *            tries to updates this mesh, if null always creates new mesh
	 * @param steps
	 * @param radius
	 * @param uvRadial
	 *            are the uv coordinates based on a circle (center 0,0) or quad, if uvRadial is enabled, then allows for
	 *            radial gradients
	 * @param aaWidth
	 *            (0 = none)
	 * @return
	 */
	public static LttlMesh GenerateCircle(LttlMesh mesh, int steps,
			float radius, boolean uvRadial, float aaWidth)
	{
		return GenerateShapeFill(mesh, radius, radius, steps, 360, 0, uvRadial,
				aaWidth);
	}

	/**
	 * Creates a shape mesh with the given number of sides. (circles mostly)
	 * 
	 * @param mesh
	 *            tries to updates this mesh, if null always creates new mesh
	 * @param radiusX
	 * @param radiusY
	 * @param sides
	 * @param degrees
	 *            between -360 and 360
	 * @param degreesOffset
	 *            default 0
	 * @param uvRadial
	 *            are the uv coordinates based on a circle (center 0,0) or quad, if uvRadial is enabled, then allows for
	 *            radial gradients
	 * @param aaWidth
	 *            (0 = none)
	 * @param aaSettings
	 * @return
	 */
	public static LttlMesh GenerateShapeFill(LttlMesh mesh, float radiusX,
			float radiusY, int sides, float degrees, float degreesOffset,
			boolean uvRadial, float aaWidth)
	{
		if (sides < 3) return null;
		degrees = LttlMath.clamp(degrees, -360, 360);
		boolean is360 = LttlMath.abs(degrees) == 360;

		mesh = LttlMesh.getNew(mesh, sides);
		mesh.ensureCapacity(sides + 1);
		ShortArray indices = mesh.getIndicesArray();

		// will return
		LttlMath.GenerateShapePoints(radiusX, radiusY, sides, degrees,
				degreesOffset, tmpVector2Array);

		if (is360)
		{
			// remove last because it is same as first
			tmpVector2Array.remove(tmpVector2Array.size() - 1);
		}
		else
		{
			// if not 360, then add center point in the beginning so it can be used in AA
			// add center point
			if (uvRadial)
			{
				// if radial, center's uv's are 0,0
				mesh.addVertice(0, 0, 0, 0, 1, 1);
			}
			else
			{
				// if not radial, center's uvs are in the middle
				mesh.addVertice(0, 0, .5f, .5f, 1, 1);
			}
		}

		// add all perimeter points
		float minX = -radiusX;
		float spanX = 2 * radiusX;
		float minY = -radiusY;
		float spanY = 2 * radiusY;
		for (int i = 0, n = tmpVector2Array.size(); i < n; i++)
		{
			float x = tmpVector2Array.getX(i);
			float y = tmpVector2Array.getY(i);

			// caluclate uv coords
			float uv1 = 0;
			float uv2 = 0;
			if (uvRadial)
			{
				uv1 = 1;
				uv2 = 1;
			}
			else
			{
				uv1 = (x - minX) / spanX;
				uv2 = (y - minY) / spanY;
			}

			mesh.addVertice(x, y, uv1, uv2, 1, 1);
		}

		// create indices
		// if is360, use -1 for center for now, will add center point after AA
		int adj360 = is360 ? -1 : 0;
		for (int i = 0; i < sides - (is360 ? 1 : 0); i++)
		{
			indices.add(adj360);
			indices.add((i + 1 + adj360));
			indices.add((i + 2 + adj360));
		}

		// connect the last one to the beginning since it is full circle
		if (is360)
		{
			// create last indice
			indices.add(-1);
			indices.add((sides - 1));
			indices.add((0));
		}

		int beforeAAIndiceSize = indices.size;

		// apply Anti Aliasing
		if (aaWidth > 0)
		{
			LttlAntiAliaser.AddAntiAliasingSimple(mesh, aaWidth, false, 0);
		}

		if (is360)
		{
			// add center point at end now, since AA has been applied
			if (uvRadial)
			{
				// if radial, center's uv's are 0,0
				mesh.addVertice(0, 0, 0, 0, 1, 1);
			}
			else
			{
				// if not radial, center's uvs are in the middle
				mesh.addVertice(0, 0, .5f, .5f, 1, 1);
			}

			// replace all -1 with the center point index
			short centerIndex = (short) (mesh.getVertexCount() - 1);
			for (int i = 0, n = beforeAAIndiceSize; i < n; i++)
			{
				if (indices.get(i) == -1)
				{
					indices.set(i, centerIndex);
				}
			}
		}

		return mesh;
	}

	/**
	 * @param mesh
	 * @param radiusX
	 * @param radiusY
	 * @param sides
	 * @param degrees
	 *            between -360 and 360
	 * @param degreesOffset
	 *            default 0
	 * @param width
	 *            can't equal 0
	 * @param centered
	 *            is the width around the center
	 * @return
	 */
	public static LttlMesh GenerateShapeOutline(LttlMesh mesh, float radiusX,
			float radiusY, int sides, float degrees, float degreesOffset,
			float width, boolean centered)
	{
		Lttl.Throw(sides < 3);
		Lttl.Throw(width == 0);

		degrees = LttlMath.clamp(degrees, -360, 360);
		boolean is360 = LttlMath.abs(degrees) == 360;

		mesh = LttlMesh.getNew(mesh, sides * 2);

		LttlMath.GenerateShapePoints(radiusX, radiusY, sides, degrees,
				degreesOffset, tmpVector2Array);

		if (is360)
		{
			// remove last because it is same as first
			tmpVector2Array.removeLast();
		}

		tmpVector2Array2.clear();
		tmpVector2Array2.addAll(tmpVector2Array);
		if (centered)
		{
			float xScale = (width / 2f) / radiusX;
			float yScale = (width / 2f) / radiusY;
			tmpVector2Array.sclAll(1 + xScale, 1 + yScale);
			tmpVector2Array2.sclAll(1 - xScale, 1 - yScale);
		}
		else
		{
			float xScale = width / radiusX;
			float yScale = width / radiusY;
			tmpVector2Array2.sclAll(1 + xScale, 1 + yScale);
		}

		Vector2Array outer = centered || width < 0 ? tmpVector2Array
				: tmpVector2Array2;
		Vector2Array inner = centered || width < 0 ? tmpVector2Array2
				: tmpVector2Array;

		int holeIndex = outer.size();
		if (is360)
		{
			// if 360 then set teh inner as a hole so AA knows how to handle it
			outer.addAll(inner);
			mesh.getHolesIndexArray().clear();
			mesh.getHolesIndexArray().add(holeIndex);
		}
		else
		{
			// if not 360, then reverse the inner points so the AA can treat it as a continuous path
			outer.addAll(inner.reverse());
		}

		GenerateVertices(outer, mesh.getVerticesArray());

		LttlTriangulator.TriangulateColumn(outer, mesh.getIndicesArray(),
				is360, is360);

		return mesh;
	}

	/**
	 * Generates a mesh based on the bitmap font provided (should already be setup with text and other options).
	 * 
	 * @param mesh
	 *            mesh to save to, if null creates new mesh
	 * @param font
	 * @param center
	 *            centers mesh instead of top left corner
	 * @param text
	 *            used for calculating max number of vertices
	 * @return
	 */
	public static LttlMesh GenerateFontMesh(LttlMesh mesh, BitmapFont font,
			boolean center, String text)
	{
		if (text == null || text.isEmpty()) { return null; }

		mesh = LttlMesh.getNew(mesh, text.length() * 4
				* LttlMesh.VERTICE_ATTRIBUTE_COUNT);

		// getfont vertices
		float[] fvs = font.getCache().getVertices();

		// generate the mesh vertices (x,y,color[skip],u,v)
		// bottom left, top left, top right, bottom right

		for (int i = 0; i < text.length() * 20; i += 5)
		{
			mesh.addVertice(fvs[i], fvs[i + 1], fvs[i + 3], fvs[i + 4], 1, 1);
		}

		// center vertices
		if (center)
		{
			mesh.centerVertices();
		}

		return TriangulateQuadGroups(mesh, false, true, 0);
	}

	/**
	 * Returns a simple quad (1x1) LttlMesh that is shared with no AA.
	 * 
	 * @return
	 */
	public static LttlMesh GetQuadMeshShared()
	{
		if (quadMesh == null)
		{
			quadMesh = LttlMeshFactory.GenerateQuad(null, 1, 0);
		}
		return quadMesh;
	}

	/**
	 * Returns a simple quad (1x1) LttlMesh with AA on top and bottom edges only that is shared.
	 * 
	 * @return
	 */
	public static LttlMesh GetLineQuadMesh()
	{
		if (lineQuadMesh == null)
		{
			lineQuadMesh = LttlMeshFactory.GenerateLineQuad(null, 1.2f);
		}
		return lineQuadMesh;
	}

	/**
	 * Returns a circle mesh with a radius of 1 and 50 steps and no AA. LttlMesh that is shared and generated once.
	 * 
	 * @return
	 */
	public static LttlMesh GetCircleMesh()
	{
		if (circleMesh == null)
		{
			circleMesh = LttlMeshFactory.GenerateCircle(null, 50, 1, false, 0);
		}
		return circleMesh;
	}

	/**
	 * Returns a donut mesh (with a 1 radius) and a fixed width of 7% of radius and 50 steps LttlMesh that is shared.
	 * 
	 * @return
	 */
	public static LttlMesh GetDonutMesh()
	{
		if (donutMesh == null)
		{
			donutMesh = LttlMeshFactory.GenerateShapeOutline(null, 1, 1, 50,
					360, 0, .07f, true);
		}
		return donutMesh;
	}

	/**
	 * offsets the points based on slope
	 * 
	 * @param points
	 * @param offset
	 * @return
	 */
	public static Vector2Array Offset(Vector2Array points, float offset)
	{
		if (points.size() < 2) return points;

		Vector2 currentLineVector = null;

		for (int i = 0; i < points.size(); i++)
		{
			if (i != points.size() - 1)
			{
				// only update currentLineVector if it's not the last point, depends on next vertice
				currentLineVector = points.get(i + 1, tmpV2)
						.sub(points.getX(i), points.getY(i)).nor();
			}
			points.offset(i, -currentLineVector.y * offset, currentLineVector.x
					* offset);
		}
		return points;
	}

	/**
	 * Creates the mesh
	 * 
	 * @param mesh
	 *            has all vertices and no indices (cleared array)
	 * @param indicesConnected
	 *            if this is true, the triangles will share sides
	 * @param groupByQuads
	 *            if indices are not connected it creates individual quads that are not connected
	 * @param aaWidth
	 *            if 0, no aa
	 * @return
	 */
	public static LttlMesh TriangulateQuadGroups(LttlMesh mesh,
			boolean indicesConnected, boolean groupByQuad, float aaWidth)
	{
		mesh.getIndicesArray().clear();

		int verticeCount = mesh.getVertexCount();
		boolean first = true;
		if (indicesConnected)
		{
			if (verticeCount < 3)
			{
				Lttl.Throw("Number of vertices need to be at least 3.");
			}
			for (int i = 0; i < verticeCount; i++)
			{
				if (i < 2) continue;

				if (first)
				{
					// triangle 1
					mesh.addIndice(i - 2);
					mesh.addIndice(i - 1);
					mesh.addIndice(i);
					first = false;
				}
				else
				{
					mesh.addIndice(i - 1);
					mesh.addIndice(i - 3);
					mesh.addIndice(i);
					first = true;
				}
			}
		}
		else if (groupByQuad)
		{
			if (verticeCount % 4 != 0)
			{
				Lttl.Throw("Number of vertices need to be divisable by 4.");
			}
			for (int i = 0; i < verticeCount; i += 4)
			{
				// quad
				mesh.addIndice(i);
				mesh.addIndice(i + 1);
				mesh.addIndice(i + 2);
				mesh.addIndice(i + 2);
				mesh.addIndice(i);
				mesh.addIndice(i + 3);
			}
		}
		else
		{
			if (verticeCount % 3 != 0)
			{
				Lttl.Throw("Number of vertices need to be divisable by 3.");
			}

			for (int i = 0; i < verticeCount; i += 3)
			{
				mesh.addIndice(i);
				mesh.addIndice(i + 1);
				mesh.addIndice(i + 2);
			}
		}

		if (aaWidth > 0)
		{
			LttlAntiAliaser.AddAntiAliasingSimple(mesh, aaWidth, false, 0);
		}

		return mesh;
	}
}
