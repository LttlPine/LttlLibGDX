package com.lttlgames.editor;

import com.badlogic.gdx.math.Vector2;

public abstract class UndoSetter
{
	public static final UndoSetter TransformPosition = new UndoSetter()
	{
		@Override
		public void set(LttlComponent comp, Object value)
		{
			((LttlTransform) comp).position.set((Vector2) value);
		}
	};
	public static final UndoSetter TransformScale = new UndoSetter()
	{
		@Override
		public void set(LttlComponent comp, Object value)
		{
			((LttlTransform) comp).scale.set((Vector2) value);
		}
	};
	public static final UndoSetter TransformRotation = new UndoSetter()
	{
		@Override
		public void set(LttlComponent comp, Object value)
		{
			((LttlTransform) comp).rotation = (Float) value;
		}
	};

	public abstract void set(LttlComponent comp, Object value);
}