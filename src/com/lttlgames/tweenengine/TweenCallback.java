package com.lttlgames.tweenengine;

import com.lttlgames.editor.Lttl;

/**
 * The callbacks are based on the original positions. So for example, start is at the start of the iteration (first
 * step), but if it is being yoyoed (itself or via a parent timeline), the start will still be called at same position,
 * even though it may appear at the end cronologically. This way events will always be at same PLACE in animation.<br>
 * Note: When yoyoing, the end,start,begin,complete callbacks may be called twice in a row if they are at the edge of
 * the yoyoing sequence, just keep track of that if doing something you only want to happen once, like a sfx.<br>
 * Note: Direction is ambiguous when these are called (no way to tell if yoyoing), so may need to keep track of that
 * yourself. However, the onStep iterationPosition is directionally accurate and will decrease if yoyoing.
 * 
 * <pre>
 * count++;
 * if (count % 2 == 0)
 * {
 * 	t().getParent().tweenPosTo(10, 50, 1).ease(EaseType.Punch).start();
 * }
 * </pre>
 * 
 * @author Josh
 */
public abstract class TweenCallback
{
	BaseTween<?> source;

	/**
	 * Useful to get the tween source (cast to Tween, to get current value).
	 * 
	 * @return
	 */
	final public BaseTween<?> getSource()
	{
		return source;
	}

	/**
	 * Each interpolation (new values set). <b>This is guaranteed not to be called more than once per frame.</b><br>
	 * The getter setter can be retrieved from source if it's a tween to get the curren values or set new ones.<br>
	 * This is also called when a sequence returns to start values, and the inner tween had a delay.
	 * 
	 * <pre>
	 * ((Tween) getSource()).getGetterSetter().get();
	 * </pre>
	 * 
	 * @param iterationPosition
	 *            this is the value (0-1) for the current iteration. This is pretty reliable value.
	 */
	public void onStep(float iterationPosition)
	{
	}

	/**
	 * right after the delay (if any)
	 * 
	 * @param source
	 */
	public void onBegin()
	{
	}

	/**
	 * at the beginning of the original iteration<br>
	 * Will be called twice in a row if yoyoing.
	 * 
	 * @param source
	 */
	public void onStart()
	{
	}

	/**
	 * at the end of the original iteration
	 * 
	 * @param source
	 */
	public void onEnd()
	{
	}

	/**
	 * at last END event
	 * 
	 * @param source
	 */
	public void onComplete()
	{
	}

	public static TweenCallback getDebug()
	{
		return new TweenCallback()
		{
			@Override
			public void onBegin()
			{
				Lttl.dump("begin");
			}

			@Override
			public void onStep(float interpValue)
			{
				Lttl.dump("step " + interpValue);
			}

			@Override
			public void onStart()
			{
				Lttl.dump("start");
			}

			@Override
			public void onEnd()
			{
				Lttl.dump("end");
			}

			@Override
			public void onComplete()
			{
				Lttl.dump("complete");
			}
		};
	}
}
