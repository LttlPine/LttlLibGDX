package com.lttlgames.helpers;

import com.lttlgames.editor.Lttl;
import com.lttlgames.editor.annotations.GuiHide;
import com.lttlgames.editor.annotations.Persist;

@Persist(-9097)
public class LttlTimeline
{
	@Persist(909700)
	@GuiHide
	private Vector2Array points;

	/**
	 * Creates a LttlTimeline with two points that represent linear interpolation.
	 */
	public LttlTimeline()
	{
		points = new Vector2Array(0, 0, 1, 1);
	}

	public LttlTimeline(float... points)
	{
		this.points = new Vector2Array(points);
	}

	public LttlTimeline(Vector2Array points)
	{
		this.points = new Vector2Array(points);
	}

	/**
	 * Returns the timeline points, after modifying these, be sure to run {@link LttlTimeline#order}. x and y values
	 * should be between 0 and 1, inclusive.
	 * 
	 * @return
	 */
	public Vector2Array getPoints()
	{
		return points;
	}

	/**
	 * Needs to be called whenever points are added and they could be not in order or values out of range (0-1)
	 */
	public void modified()
	{
		// clamp to range
		for (int i = 0, n = points.size(); i < n; i++)
		{
			points.setX(i, LttlMath.Clamp01(points.getX(i)));
			points.setY(i, LttlMath.Clamp01(points.getY(i)));
		}
		// sort
		points.sortByX(true);
	}

	public float getValue(float percent, float start, float end)
	{
		return LttlMath.Lerp(start, end, getValue(percent));
	}

	/**
	 * Returns the y value (between 0 and 1, inclusive) of timeline at x (between 0 and 1, inclusive)
	 * 
	 * @param percent
	 *            between 0 and 1, inclusive
	 * @return
	 */
	public float getValue(float percent)
	{
		// no points
		Lttl.Throw(points.size() == 0);

		// quick return if only one point
		if (points.size() == 1) { return points.getFirstY(); }

		percent = LttlMath.Clamp01(percent);
		float lastX = 0;
		float lastY = points.getFirstY();
		for (int i = 0, n = points.size(); i < n; i++)
		{
			float currentX = points.getX(i);
			float currentY = points.getY(i);
			if (percent >= lastX && percent < currentX) { return LttlMath.Lerp(
					lastY, currentY, (percent - lastX) / (currentX - lastX)); }
			lastX = currentX;
			lastY = currentY;
		}
		// it is greater than or equal to the last X
		return points.getLastY();
	}

	public int size()
	{
		return points.size();
	}
}
