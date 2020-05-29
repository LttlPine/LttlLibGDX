package com.lttlgames.editor;

import com.lttlgames.editor.annotations.GuiButton;
import com.lttlgames.editor.annotations.GuiCanNull;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.helpers.EaseType;
import com.lttlgames.helpers.LttlMath;
import com.lttlgames.tweenengine.RepeatTweenType;
import com.lttlgames.tweenengine.Tween;
import com.lttlgames.tweenengine.TweenGetterSetter;

@Persist(-9054)
public abstract class StateProperty<T>
{
	/**
	 * The saved state's value.<br>
	 * Note: This should not be a pointer to anything but this StateProperty.
	 */
	@Persist(905400)
	public T value;

	/**
	 * Does this property animate when creating a tween or when using goTo()
	 */
	@Persist(905401)
	public boolean active = true;
	/**
	 * The individual delay for this property that adds on to any animation delay
	 */
	@GuiMin(0)
	@Persist(905404)
	public float delay = 0;
	/**
	 * The duration for this state property. If negative, then the duration is that of the animation.
	 */
	@Persist(905405)
	@GuiMin(-1)
	public float duration = -1;
	@Persist(905406)
	@GuiCanNull
	public StatePropertyOptions options;

	public StateProperty()
	{
		this(true);
	}

	public StateProperty(boolean startActive)
	{
		// on creation set default value
		value = getDefaultValue();
	}

	/**
	 * Genereates a tween object for this state property taking into consideration the state property's own easeType,
	 * duration, and delay, and the properties of the animation's easetype and duration.
	 * 
	 * @param component
	 * @param getterSetter
	 * @param animationDuration
	 * @param animationEaseType
	 * @return
	 */
	public Tween getTween(LttlComponent component,
			final TweenGetterSetter getterSetter, float animationDuration,
			EaseType animationEaseType)
	{
		// value can't be null
		Lttl.Throw(value);

		// clip delay to animation duration
		float realDelay = LttlMath.min(delay, animationDuration);

		// calculate duration
		float realDuration = 0;
		if (duration < 0)
		{
			// if less than 0 then it same as animation duration
			realDuration = animationDuration - realDelay;
		}
		else
		{
			// use specified duration clipped to animationDuration - realDelay
			realDuration = LttlMath
					.min(animationDuration - realDelay, duration);
		}

		// repeat
		float realRepeatDelay = 0;
		if (options != null && options.repeatCount > 0
				&& options.repeatType != RepeatTweenType.None)
		{
			// clamp to 0+
			realRepeatDelay = LttlMath.max(0, options.repeatDelay);
			// clamp repeat delay to the biggest repeat delay possible based on duration and repeat count
			realRepeatDelay = LttlMath.min(realRepeatDelay, realDuration
					/ options.repeatCount);
			// calculate real duration based on what time is left after the realRepeatDelay
			realDuration = (realDuration - (realRepeatDelay * options.repeatCount))
					/ (options.repeatCount + 1);
			// if specified duration, then clamp it to realDuration
			if (duration >= 0)
			{
				realDuration = LttlMath.min(realDuration, duration);
			}
		}

		// create tween
		Tween tween = component
				.tweenTo(realDuration, getterSetter, getTargetValues())
				.setDelay(realDelay)
				.setEase(
						isEaseTypeOverride() ? options.easeType
								: animationEaseType);

		// options
		if (options != null)
		{
			if (options.repeatCount > 0
					&& options.repeatType != RepeatTweenType.None)
			{
				if (options.repeatType == RepeatTweenType.Rewind)
				{
					tween.repeat(options.repeatCount, realRepeatDelay);
				}
				else if (options.repeatType == RepeatTweenType.YoYo)
				{
					tween.repeatYoyo(options.repeatCount, realRepeatDelay);
				}
			}
			if (options.addNoise)
			{
				tween.addNoise(options.noiseOptions,
						options.generateNewNoiseEachIteration);
			}
			if (options.isRelative)
			{
				tween.setRelative();
			}
			if (options.addShake)
			{
				tween.addShake(options.shakeRangeBottom, options.shakeRangeTop,
						options.shakeUpdateRate, options.shakeType);
			}
		}

		return tween;
	}

	/**
	 * Converts the value to a float array to be used with tweening system.<br>
	 * Meant to be overridden to access the values from the value object.
	 * 
	 * @return
	 */
	abstract float[] getTargetValues();

	/**
	 * Sets the values to the property variable.
	 */
	abstract void setTargetValues(float[] values);

	/**
	 * This sets the default (starting) value for this property. Create a new object.
	 * 
	 * @return
	 */
	abstract T getDefaultValue();

	@GuiButton(tooltip = "Sets value to default.")
	public void resetValue()
	{
		value = getDefaultValue();
	}

	public boolean isEaseTypeOverride()
	{
		return options != null && options.easeTypeOverride;
	}

	public boolean isRelative()
	{
		return options != null && options.isRelative;
	}

	public boolean isRepeat()
	{
		return options != null && options.repeatCount > 0
				&& options.repeatType != RepeatTweenType.None;
	}

	public boolean isRepeatYoyo()
	{
		return options != null && options.repeatCount > 0
				&& options.repeatType == RepeatTweenType.YoYo;
	}

	public boolean isRepeatYoyoOdd()
	{
		return isRepeatYoyo() && options.repeatCount % 2 == 1;
	}

	public boolean isRepeatRewind()
	{
		return options != null && options.repeatCount > 0
				&& options.repeatType == RepeatTweenType.Rewind;
	}

	/**
	 * editor use only
	 */
	@SuppressWarnings("rawtypes")
	@GuiButton(order = 0)
	private void update(GuiFieldObject source)
	{
		// try to find the state manager and state
		LttlStateManager manager = (LttlStateManager) source
				.getAncestorByClass(LttlStateManager.class, true).objectRef;
		StateBase state = (StateBase) source.getAncestorByClass(
				StateBase.class, true).objectRef;

		state.updateInternal(manager.target, this);
	}
}
