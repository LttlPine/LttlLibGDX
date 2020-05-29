package com.lttlgames.helpers;

import com.badlogic.gdx.math.Bezier;
import com.badlogic.gdx.math.Vector;
import com.badlogic.gdx.math.Vector2;

public class LttlBezier extends Bezier<Vector2>
{
	/*Temp*/
	final static private Vector2 p0 = new Vector2();
	final static private Vector2 p1 = new Vector2();
	final static private Vector2 p2 = new Vector2();
	final static private Vector2 p3 = new Vector2();
	final static private Vector2 tmp = new Vector2();

	/**
	 * Cubic Bezier curve
	 * 
	 * @param out
	 *            The {@link Vector} to set to the result.
	 * @param t
	 *            The location (ranging 0..1) on the curve.
	 * @param p0
	 *            The first bezier point.
	 * @param p1
	 *            The second bezier point.
	 * @param p2
	 *            The third bezier point.
	 * @param p3
	 *            The fourth bezier point.
	 * @param tmpV2
	 *            A temporary vector to be used by the calculation.
	 * @return The value specified by out for chaining
	 */
	public static Vector2 cubic(final Vector2 out, final float t, float p0x,
			float p0y, float p1x, float p1y, float p2x, float p2y, float p3x,
			float p3y)
	{
		return cubic(out, t, p0.set(p0x, p0y), p1.set(p1x, p1y),
				p2.set(p2x, p2y), p3.set(p3x, p3y), tmp);
	}

	public static Vector2 cubic(Vector2Array points, final float t,
			final Vector2 out)
	{
		return cubic(out, t, points.get(0, p0), points.get(1, p1),
				points.get(2, p2), points.get(3, p3), tmp);
	}
}
