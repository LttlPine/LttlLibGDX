package com.lttlgames.helpers;

import com.badlogic.gdx.math.Rectangle;
import com.lttlgames.editor.annotations.Persist;

/**
 * A rectangle wrapper that takes values that are as if the rectangle was centered. Then getRect() gies you the
 * Rectangle object
 * 
 * @author Josh
 */
@Persist(-90111)
public class LttlRectangle
{
	// center pos
	@Persist(9011100)
	public float x = 0;
	@Persist(9011101)
	public float y = 0;

	// dimensions
	@Persist(9011102)
	public float width = 0;
	@Persist(9011103)
	public float height = 0;

	private Rectangle rect;

	public Rectangle getRect()
	{
		if (rect == null)
		{
			rect = new Rectangle();
		}
		rect.setWidth(width).setHeight(height).setCenter(x, y);

		return rect;
	}
}
