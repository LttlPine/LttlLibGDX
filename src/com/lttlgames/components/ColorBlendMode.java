package com.lttlgames.components;

import com.badlogic.gdx.graphics.Color;
import com.lttlgames.editor.annotations.Persist;

@Persist(-90106)
public enum ColorBlendMode
{
	None, Additive, Subtractive, Multiply;

	/**
	 * When subtractive, a-b
	 * 
	 * @param blend
	 * @param a
	 *            stores value in this
	 * @param b
	 */
	public static Color blend(ColorBlendMode blend, Color a, Color b)
	{
		switch (blend)
		{
			case Additive:
				a.add(b);
				break;
			case Multiply:
				a.mul(b);
				break;
			case None:
				break;
			case Subtractive:
				a.sub(b);
				break;
			default:
				break;

		}
		return a;
	}
}
