package com.lttlgames.tweenengine;

import java.lang.reflect.Array;
import java.util.ArrayList;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;

public abstract class TweenGetterSetter
{
	public abstract void set(float[] values);

	public abstract float[] get();

	/**
	 * Optional to override. Returns the target object that the tween is applied to. This is helpful when searching for
	 * a tween with a specific target object.
	 * 
	 * @return
	 */
	public Object getTarget()
	{
		return null;
	}

	/**
	 * Generates a TweenGetterSetter for any Vector2 pointer
	 * 
	 * @param pointer
	 * @return
	 */
	public static TweenGetterSetter getVector2(final Vector2 pointer)
	{
		return new TweenGetterSetter()
		{
			@Override
			public Object getTarget()
			{
				return pointer;
			}

			@Override
			public void set(float[] values)
			{
				pointer.set(values[0], values[1]);
			}

			@Override
			public float[] get()
			{
				return new float[]
				{ pointer.x, pointer.y };
			}
		};
	}

	/**
	 * Generates a TweenGetterSetter for any Vector2 pointer for a single property
	 * 
	 * @param pointer
	 * @param property
	 *            [0=x, 1=y]
	 * @return
	 */
	public static TweenGetterSetter getVector2(final Vector2 pointer,
			final int property)
	{
		return new TweenGetterSetter()
		{
			@Override
			public Object getTarget()
			{
				return pointer;
			}

			@Override
			public void set(float[] values)
			{
				switch (property)
				{
					case 0:
						pointer.x = values[0];
						break;
					case 1:
						pointer.y = values[0];
						break;
				}
			}

			@Override
			public float[] get()
			{
				switch (property)
				{
					case 0:
						return new float[]
						{ pointer.x };
					case 1:
						return new float[]
						{ pointer.y };
				}
				return null;
			}
		};
	}

	/**
	 * Generates a TweenGetterSetter for any Color pointer
	 * 
	 * @param pointer
	 * @return
	 */
	public static TweenGetterSetter getColor(final Color pointer)
	{
		return new TweenGetterSetter()
		{
			@Override
			public Object getTarget()
			{
				return pointer;
			}

			@Override
			public void set(float[] values)
			{
				pointer.set(values[0], values[1], values[2], values[3]);
			}

			@Override
			public float[] get()
			{
				return new float[]
				{ pointer.r, pointer.g, pointer.b, pointer.a };
			}
		};
	}

	/**
	 * Generates a TweenGetterSetter for any Color pointer for a single property
	 * 
	 * @param pointer
	 * @param property
	 *            [0=r, 1=g, 2=b, 3=a]
	 * @return
	 */
	public static TweenGetterSetter getColor(final Color pointer,
			final int property)
	{
		return new TweenGetterSetter()
		{
			@Override
			public Object getTarget()
			{
				return pointer;
			}

			@Override
			public void set(float[] values)
			{
				switch (property)
				{
					case 0:
						pointer.r = values[0];
						break;
					case 1:
						pointer.g = values[0];
						break;
					case 2:
						pointer.b = values[0];
						break;
					case 3:
						pointer.a = values[0];
						break;
				}
			}

			@Override
			public float[] get()
			{
				switch (property)
				{
					case 0:
						return new float[]
						{ pointer.r };
					case 1:
						return new float[]
						{ pointer.g };
					case 2:
						return new float[]
						{ pointer.b };
					case 3:
						return new float[]
						{ pointer.a };
				}
				return null;
			}
		};
	}

	public static TweenGetterSetter getFloat(final float startValue)
	{
		return new TweenGetterSetter()
		{
			float value = startValue;

			@Override
			public void set(float[] values)
			{
				value = values[0];
			}

			@Override
			public float[] get()
			{
				return new float[]
				{ value };
			}
		};
	}

	public static TweenGetterSetter getInteger(final int startValue)
	{
		return new TweenGetterSetter()
		{
			int value = startValue;

			@Override
			public void set(float[] values)
			{
				// rounding down happens here
				value = (int) values[0];
			}

			@Override
			public float[] get()
			{
				return new float[]
				{ value };
			}
		};
	}

	public static TweenGetterSetter getArrayListFloat(
			final ArrayList<Float> list, final int index)
	{
		return new TweenGetterSetter()
		{
			@Override
			public void set(float[] values)
			{

				list.set(index, values[0]);
			}

			@Override
			public float[] get()
			{
				return new float[]
				{ list.get(index) };
			}
		};
	}

	public static TweenGetterSetter getArrayListInteger(
			final ArrayList<Integer> list, final int index)
	{
		return new TweenGetterSetter()
		{
			@Override
			public void set(float[] values)
			{
				// rounding down happens here
				list.set(index, (int) values[0]);
			}

			@Override
			public float[] get()
			{
				return new float[]
				{ list.get(index) };
			}
		};
	}

	public static TweenGetterSetter getArrayFloat(final Object array,
			final int index)
	{
		return new TweenGetterSetter()
		{
			@Override
			public void set(float[] values)
			{
				Array.setFloat(array, index, values[0]);
			}

			@Override
			public float[] get()
			{
				return new float[]
				{ Array.getFloat(array, index) };
			}
		};
	}

	public static TweenGetterSetter getArrayInteger(final Object array,
			final int index)
	{
		return new TweenGetterSetter()
		{
			@Override
			public void set(float[] values)
			{
				Array.setInt(array, index, (int) values[0]);
			}

			@Override
			public float[] get()
			{
				return new float[]
				{ Array.getInt(array, index) };
			}
		};
	}
}
