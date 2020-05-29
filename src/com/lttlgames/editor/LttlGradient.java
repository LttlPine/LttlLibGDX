package com.lttlgames.editor;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.FloatArray;
import com.lttlgames.editor.annotations.DoCopy;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.helpers.LttlMath;

@Persist(-90101)
public class LttlGradient
{
	@Persist(9010100)
	private ArrayList<Color> colors = new ArrayList<Color>();
	@Persist(9010101)
	private FloatArray times = new FloatArray(1);
	{
		colors.add(new Color(1, .01f, .01f, 1));
		times.add(0);
	}
	@DoCopy
	int guiSelectedIndex = 0;

	/**
	 * Needs to be called whenever colors are added and they could be not in order or values out of range (0-1)
	 */
	void modified()
	{
		// TODO not necessary because almost always going to be modifying this in Gui/Editor, which doesn't make
		// mistakes, or put things out of order
	}

	public FloatArray getTimeline()
	{
		return times;
	}

	public ArrayList<Color> getColors()
	{
		return colors;
	}

	/**
	 * Returns the color value at the lerp position
	 * 
	 * @param percent
	 *            between 0 and 1, inclusive
	 * @param output
	 *            only the rgb values are changed
	 * @return
	 */
	public Color lerp(float percent, Color output)
	{
		// no colors
		Lttl.Throw(colors.size() < 1 || times.size < 1
				|| colors.size() != times.size);

		percent = LttlMath.Clamp01(percent);
		float last = 0;
		for (int i = 1, n = times.size; i < n; i++)
		{
			float current = times.get(i);
			if (percent >= last && percent < current)
			{
				Color a = colors.get(i - 1);
				Color b = colors.get(i);
				float p = (percent - last) / (current - last);
				output.r = LttlMath.Lerp(a.r, b.r, p);
				output.g = LttlMath.Lerp(a.g, b.g, p);
				output.b = LttlMath.Lerp(a.b, b.b, p);
				return output;
			}
			last = current;
		}
		// it is greater than or equal to the last pos
		return output.set(colors.get(colors.size() - 1));
	}

	public int size()
	{
		return times.size;
	}
}
