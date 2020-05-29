package com.lttlgames.editor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Pool.Poolable;

public class MultiObject implements Poolable
{
	public Vector2 position = new Vector2();
	public Vector2 scale = new Vector2(1, 1);
	public Vector2 shear = new Vector2(0, 0);
	public float rotation = 0;
	public Vector2 origin = new Vector2();
	public float alpha = 1;
	public Color color = new Color();

	@Override
	public void reset()
	{
		position.set(0, 0);
		scale.set(1, 1);
		shear.set(0, 0);
		origin.set(0, 0);
		alpha = 1;
		rotation = 0;
		color.set(Color.WHITE);
	}
}
