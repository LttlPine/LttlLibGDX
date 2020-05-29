package com.lttlgames.editor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.lttlgames.tweenengine.BaseTween;
import com.lttlgames.tweenengine.Timeline;
import com.lttlgames.tweenengine.Tween;
import com.lttlgames.tweenengine.TweenCallback;
import com.lttlgames.tweenengine.TweenGetterSetter;
import com.lttlgames.tweenengine.TweenManager;

public final class LttlTween
{
	private TweenManager tweenManager = new TweenManager();

	public LttlTween()
	{
		Lttl.tween = this;
	}

	public TweenManager getManager()
	{
		return tweenManager;
	}

	/**
	 * A normal tween that animates a single float value from 0 to 1, which can be retrieved from the callback's
	 * getSource() casted to a Tween then using getGetterSetter().get()<br>
	 * Can use it as just a delay with 0 duration, or whatever, but it will callback on each step if you need the value.
	 * 
	 * <pre>
	 * timer(hostComponent, callback, duration).delay(1).repeat(10, 1000).start();
	 * </pre>
	 * 
	 * @param hostComponent
	 *            when this component is destroyed, it forces this Timeline to also be destroyed, also caches if
	 *            component is pauseable, can be null if not on a component
	 * @param callback
	 * @param duration
	 * @return
	 */
	public Tween timer(LttlComponent hostComponent, TweenCallback callback,
			float duration)
	{
		return tweenFloat(hostComponent, 0, 1, duration, callback);
	}

	/**
	 * Convenience method to create an empty tween. Such object is only useful when placed inside animation sequences
	 * (see {@link Timeline}), in which it may act as a beacon, so you can set a callback on it in order to trigger some
	 * action at the right moment.
	 * 
	 * @param hostComponent
	 *            when this component is destroyed, it forces this Timeline to also be destroyed, also caches if
	 *            component is pauseable, can be null if not on a component
	 * @return The generated Tween.
	 * @see Timeline
	 */
	public Tween mark(LttlComponent hostComponent)
	{
		return Tween.mark(hostComponent);
	}

	/**
	 * A Timeline can be used to create complex animations made of sequences and parallel sets of Tweens. Use push() to
	 * add twe
	 * <p/>
	 * The following example will create an animation sequence composed of 5 parts:
	 * <p/>
	 * 1. First, opacity and scale are set to 0 (with Tween.set() calls).<br/>
	 * 2. Then, opacity and scale are animated in parallel.<br/>
	 * 3. Then, the animation is paused for 1s.<br/>
	 * 4. Then, position is animated to x=100.<br/>
	 * 5. Then, rotation is animated to 360°.
	 * <p/>
	 * This animation will be repeated 5 times, with a 500ms delay between each iteration: <br/>
	 * <br/>
	 * 
	 * <pre>
	 * {@code
	 * Timeline.createSequence()
	 *     .push(Tween.set(myObject, OPACITY).target(0))
	 *     .push(Tween.set(myObject, SCALE).target(0, 0))
	 *     .beginParallel()
	 *          .push(Tween.to(myObject, OPACITY, 0.5f).target(1).ease(Quad.INOUT))
	 *          .push(Tween.to(myObject, SCALE, 0.5f).target(1, 1).ease(Quad.INOUT))
	 *     .end()
	 *     .pushPause(1.0f)
	 *     .push(Tween.to(myObject, POSITION_X, 0.5f).target(100).ease(Quad.INOUT))
	 *     .push(Tween.to(myObject, ROTATION, 0.5f).target(360).ease(Quad.INOUT))
	 *     .repeat(5, 0.5f)
	 *     .start();
	 * }
	 * </pre>
	 * 
	 * @param hostComponent
	 *            when this component is destroyed, it forces this Timeline to also be destroyed, also caches if
	 *            component is pauseable, can be null if not on a component
	 * @see Tween
	 * @see TweenManager
	 * @see TweenCallback
	 * @author Aurelien Ribon | http://www.aurelienribon.com/
	 */
	public Timeline createSequence(LttlComponent hostComponent)
	{
		return Timeline.createSequence(hostComponent);
	}

	/**
	 * Creates a new timeline with a 'parallel' behavior. Its children will be triggered all at once. Use push() to add
	 * tweens.
	 * 
	 * @param hostComponent
	 *            when this component is destroyed, it forces this Timeline to also be destroyed, also caches if
	 *            component is pauseable, can be null if not on a component
	 */
	public Timeline createParallel(LttlComponent hostComponent)
	{
		return Timeline.createParallel(hostComponent);
	}

	/**
	 * Creates a tween where the value can be obtained via callback (onStep()) and cast source to Tween and use
	 * getNewValues()[0]..
	 * 
	 * <pre>
	 * {@code
	 * public void onStep()
	 * {
	 *  lt.position.x = ((Tween) getSource()).getNewValues()[0];
	 * }
	 * </pre>
	 * 
	 * @param hostComponent
	 *            when this component is destroyed, it forces this Timeline to also be destroyed, also caches if
	 *            component is pauseable, can be null if not on a component
	 * @param start
	 * @param target
	 * @param duration
	 * @param callback
	 * @return
	 */
	public Tween tweenFloat(LttlComponent hostComponent, float start,
			float target, float duration, TweenCallback callback)
	{
		return Tween
				.to(hostComponent, TweenGetterSetter.getFloat(start), duration)
				.target(target).addCallback(callback);
	}

	/**
	 * Creates a tween where the value can be obtained via callback (onStep()) and cast source to Tween and use
	 * getNewValues()[0].
	 * 
	 * <pre>
	 * {@code
	 * public void onStep()
	 * {
	 *  lt.position.x = ((Tween) getSource()).getNewValues()[0];
	 * }
	 * </pre>
	 * 
	 * @param hostComponent
	 *            when this component is destroyed, it forces this Timeline to also be destroyed, also caches if
	 *            component is pauseable, can be null if not on a component
	 * @param start
	 * @param target
	 * @param duration
	 * @param callback
	 * @return
	 */
	public Tween tweenInteger(LttlComponent hostComponent, int start,
			int target, float duration, TweenCallback callback)
	{
		return Tween
				.to(hostComponent, TweenGetterSetter.getInteger(start),
						duration).target(target).addCallback(callback);
	}

	/**
	 * Tween a color object
	 * 
	 * @param hostComponent
	 *            when this component is destroyed, it forces this Timeline to also be destroyed, also caches if
	 *            component is pauseable, can be null if not on a component
	 * @param tweenedColor
	 * @param r
	 * @param g
	 * @param b
	 * @param a
	 * @param duration
	 * @return
	 */
	public Tween tweenColorTo(LttlComponent hostComponent, Color tweenedColor,
			float r, float g, float b, float a, float duration)
	{
		return Tween.to(hostComponent,
				TweenGetterSetter.getColor(tweenedColor), duration).target(r,
				g, b, a);
	}

	/**
	 * Tweens a color object's property independently. This is useful for doing parallel sequences to edit properties
	 * with different eases on the same color object.
	 * 
	 * @param hostComponent
	 *            when this component is destroyed, it forces this Timeline to also be destroyed, also caches if
	 *            component is pauseable, can be null if not on a component
	 * @param tweenedColor
	 * @param property
	 *            [0=r, 1=g, 2=b, 3=a]
	 * @param targetValue
	 * @param duration
	 * @return
	 */
	public Tween tweenColorPropTo(LttlComponent hostComponent,
			Color tweenedColor, int property, float targetValue, float duration)
	{
		if (property < 0 || property > 3)
		{
			Lttl.Throw("Property out of range.");
		}
		return Tween.to(hostComponent,
				TweenGetterSetter.getColor(tweenedColor, property), duration)
				.target(targetValue);
	}

	/**
	 * @param hostComponent
	 *            when this component is destroyed, it forces this Timeline to also be destroyed, also caches if
	 *            component is pauseable, can be null if not on a component
	 * @param tweenedColor
	 * @param targetColor
	 * @param duration
	 * @return
	 */
	public Tween tweenColorTo(LttlComponent hostComponent, Color tweenedColor,
			Color targetColor, float duration)
	{
		return tweenColorTo(hostComponent, tweenedColor, targetColor.r,
				targetColor.g, targetColor.b, targetColor.a, duration);
	}

	/**
	 * @param hostComponent
	 *            when this component is destroyed, it forces this tween to also be destroyed
	 * @param tweenedVector
	 * @param targetX
	 * @param targetY
	 * @param duration
	 * @return
	 */
	public Tween tweenVector2To(LttlComponent hostComponent,
			Vector2 tweenedVector, float targetX, float targetY, float duration)
	{
		return Tween.to(hostComponent,
				TweenGetterSetter.getVector2(tweenedVector), duration).target(
				targetX, targetY);
	}

	/**
	 * @param hostComponent
	 *            when this component is destroyed, it forces this Timeline to also be destroyed, also caches if
	 *            component is pauseable, can be null if not on a component
	 * @param tweenedVector
	 * @param targetVector
	 * @param duration
	 * @return
	 */
	public Tween tweenVector2To(LttlComponent hostComponent,
			Vector2 tweenedVector, Vector2 targetVector, float duration)
	{
		return tweenVector2To(hostComponent, tweenedVector, targetVector.x,
				targetVector.y, duration)
				.target(targetVector.x, targetVector.y);
	}

	/**
	 * Tweens a vector2 object's property independently. This is useful for doing parallel sequences to edit properties
	 * with different eases on the same vector2 object.
	 * 
	 * @param hostComponent
	 *            when this component is destroyed, it forces this Timeline to also be destroyed, also caches if
	 *            component is pauseable, can be null if not on a component
	 * @param tweenedVector
	 * @param property
	 *            [0=x, 1=y]
	 * @param targetValue
	 * @param duration
	 * @return
	 */
	public Tween tweenVector2PropTo(LttlComponent hostComponent,
			Vector2 tweenedVector, int property, float targetValue,
			float duration)
	{
		if (property < 0 || property > 1)
		{
			Lttl.Throw("Property out of range.");
		}
		return Tween
				.to(hostComponent,
						TweenGetterSetter.getVector2(tweenedVector, property),
						duration).target(targetValue);
	}

	/**
	 * Tween a float fields (cast to int, if needed) via a TweenGetterSetter (no uses reflection)
	 * 
	 * @param hostComponent
	 *            when this component is destroyed, it forces this Timeline to also be destroyed, also caches if
	 *            component is pauseable, can be null if not on a component
	 * @param duration
	 * @param getterSetter
	 *            how to get and set the value
	 * @param targetValues
	 * @return
	 */
	public Tween tweenTo(LttlComponent hostComponent, float duration,
			TweenGetterSetter getterSetter, float... targetValues)
	{
		return Tween.to(hostComponent, getterSetter, duration).target(
				targetValues);
	}

	/**
	 * Finds a BaseTween object (Tween or Timeline) from an id, returning null means none exists, probably finished.<br>
	 * This is untested, and could be slow if lots of tweens.
	 * 
	 * @param id
	 *            this id should should have been saved from the tween object you are looking for by using getId()
	 * @return null if none could be found
	 */
	public BaseTween<?> getFromId(long id)
	{
		return getManager().getFromId(id);
	}

	/**
	 * Checks if the tween created with the unique instance id provided is killed.<br>
	 * Alternately you could use isKilledUnique() on the Tween or Timeline, but you need to save the instance of the
	 * tween/timeline and it's unique instance id.
	 * 
	 * @param id
	 * @return
	 */
	public boolean isTweenKilled(long id)
	{
		BaseTween<?> baseTween = getFromId(id);
		if (baseTween == null)
		{
			return false;
		}
		else
		{
			return !baseTween.isKilled();
		}
	}
}
