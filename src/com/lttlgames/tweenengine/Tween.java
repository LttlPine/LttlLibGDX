package com.lttlgames.tweenengine;

import com.lttlgames.editor.EaseMode;
import com.lttlgames.editor.Lttl;
import com.lttlgames.editor.LttlComponent;
import com.lttlgames.editor.NoiseOptions;
import com.lttlgames.helpers.EaseType;
import com.lttlgames.helpers.LttlMath;
import com.lttlgames.helpers.LttlNoise;

public final class Tween extends BaseTween<Tween>
{
	private static float epsilon = 0.00000000001f;

	// -------------------------------------------------------------------------
	// Static -- pool
	// -------------------------------------------------------------------------

	private static final Pool.Callback<Tween> poolCallback = new Pool.Callback<Tween>()
	{
		@Override
		public void onPool(Tween obj)
		{
			obj.reset();
		}

		@Override
		public void onUnPool(Tween obj)
		{
			obj.reset();
		}
	};

	private static final Pool<Tween> pool = new Pool<Tween>(20, poolCallback)
	{
		@Override
		protected Tween create()
		{
			return new Tween();
		}
	};

	/**
	 * Used for debug purpose. Gets the current number of objects that are waiting in the Tween pool.
	 */
	public static int getPoolSize()
	{
		return pool.size();
	}

	/**
	 * Increases the minimum capacity of the pool. Capacity defaults to 20.
	 */
	public static void ensurePoolCapacity(int minCapacity)
	{
		pool.ensureCapacity(minCapacity);
	}

	// -------------------------------------------------------------------------
	// Static -- factories
	// -------------------------------------------------------------------------

	/**
	 * Uses TweenGetterSetter.
	 * 
	 * @param hostComponent
	 *            when this component is destroyed, it forces this Timeline to also be destroyed, also caches if
	 *            component is pauseable, can be null if not on a component
	 * @param getterSetter
	 *            how to get and set the values
	 * @param duration
	 * @return
	 */
	public static Tween to(LttlComponent hostComponent,
			TweenGetterSetter getterSetter, float duration)
	{
		Tween tween = pool.get();
		tween.setup(hostComponent, getterSetter, duration);
		return tween;
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
	public static Tween mark(LttlComponent hostComponent)
	{
		Tween tween = pool.get();
		tween.setup(hostComponent, null, 0);
		return tween;
	}

	// -------------------------------------------------------------------------
	// Attributes
	// -------------------------------------------------------------------------

	// Main
	private EaseType ease;
	private float[] easeParams;

	// General
	private boolean isFrom;
	private boolean isRelative;
	private int combinedAttrsCnt;
	private float speed;

	// Values
	private float[] targetValues;
	private float[] startValues;
	private float[] endValues;

	// Buffers
	private float[] accessorBuffer;
	float[] targetArray;

	private TweenGetterSetter getterSetter;

	// Noise
	private boolean addNoise;
	private boolean generateNewNoisePerIteration;
	private int ns_lod;
	private float ns_stepSizePerSecond;
	private float ns_startRange;
	private float ns_endRange;
	private boolean ns_smooth;
	private boolean ns_fadeIn;
	private boolean ns_fadeOut;
	private float[] ns_startStep;

	// Shake
	private boolean addShake;
	private float[] sh_values;
	private float sh_rangeBottom;
	private float sh_rangeTop;
	private float sh_rateTime;
	private float sh_waitTime;
	private EaseMode sh_easeMode;

	// -------------------------------------------------------------------------
	// Setup
	// -------------------------------------------------------------------------

	private Tween()
	{
		reset();
	}

	@Override
	protected void reset()
	{
		super.reset();

		getterSetter = null;
		ease = EaseType.QuadInOut;
		easeParams = null;

		isFrom = isRelative = false;
		combinedAttrsCnt = -1;

		targetArray = startValues = endValues = targetValues = accessorBuffer = null;

		speed = -1;

		// noise defaults
		addNoise = false;
		generateNewNoisePerIteration = false;
		ns_lod = 2;
		ns_stepSizePerSecond = .006f;
		ns_smooth = false;
		ns_startRange = -1;
		ns_endRange = 1;
		ns_fadeIn = ns_fadeOut = true;
		ns_startStep = null;

		// shake reset
		addShake = false;
		sh_values = null;
		sh_rangeBottom = sh_rangeTop = sh_rateTime = sh_waitTime = 0;
		sh_easeMode = EaseMode.InOut;
	}

	private void setup(LttlComponent hostComponent,
			TweenGetterSetter getterSetter, float duration)
	{
		setDuration(duration);

		setup(hostComponent);
		this.getterSetter = getterSetter;
		this.duration = duration;
	}

	// -------------------------------------------------------------------------
	// Public API
	// -------------------------------------------------------------------------

	/**
	 * Sets the easing equation of the tween. Default equation is easeInOutQuad
	 * 
	 * @return The current tween, for chaining instructions.
	 */
	public Tween setEase(EaseType ease)
	{
		this.ease = ease;
		return this;
	}

	/**
	 * Sets optional parameters for ease equations. See EaseType for list of parameters.
	 * 
	 * @param easeParams
	 * @return
	 */
	public Tween setEaseParams(float... easeParams)
	{
		this.easeParams = easeParams;
		return this;
	}

	// /**
	// * Forces the tween to use the TweenAccessor registered with the given target class. Useful if you want to use a
	// * specific accessor associated to an interface, for instance.
	// *
	// * @param targetClass
	// * A class registered with an accessor.
	// * @return The current tween, for chaining instructions.
	// */
	// public Tween cast(Class<?> targetClass)
	// {
	// if (isStarted())
	// throw new RuntimeException(
	// "You can't cast the target of a tween once it is started");
	// this.targetClass = targetClass;
	// return this;
	// }

	/**
	 * Sets the target (or relative target) values of the interpolation. The interpolation will run from the <b>values
	 * at start time (after the delay, if any)</b> to these target values.
	 * <p/>
	 * To sum-up:<br/>
	 * - start values: values at start time, after delay<br/>
	 * - end values: params
	 * 
	 * @param targetValues
	 *            The target values of the interpolation.
	 * @return The current tween, for chaining instructions.
	 */
	public Tween target(float... targetValues)
	{
		this.targetValues = new float[targetValues.length];

		System.arraycopy(targetValues, 0, this.targetValues, 0,
				targetValues.length);
		return this;
	}

	/**
	 * Sets this tween as relative. When target values are set they will be relative to the start values, defined after
	 * the delay, if any. This will be cumulative if repeated (even inside sequences that are repeating).<br>
	 * Note: This will often break if it is inside a repeating sequence that is inside any yoyo sequence.
	 * 
	 * @return The current tween, for chaining instructions.
	 */
	public Tween setRelative()
	{
		if (isStarted())
		{
			Lttl.Throw("Can't change duration after tween has started.");
		}
		isRelative = true;
		return this;
	}

	/**
	 * Sets interpolation as reversed (target to start). The ending values are retrieved automatically after the delay
	 * (if any). <br/>
	 * <br/>
	 * <b>You need to set the starting values of the interpolation by using one of the target() methods</b>. The
	 * interpolation will run from the starting values to these target values.
	 * 
	 * @return
	 */
	public Tween setFrom()
	{
		if (isStarted())
		{
			Lttl.Throw("Can't change duration after tween has started.");
		}
		isFrom = true;
		return this;
	}

	// -------------------------------------------------------------------------
	// Getters
	// -------------------------------------------------------------------------

	/**
	 * Returns if this tween is set to be relative.
	 * 
	 * @return
	 */
	public boolean isRelative()
	{
		return isRelative;
	}

	/**
	 * Returns if this tween is set to be from.
	 * 
	 * @return
	 */
	public boolean isFrom()
	{
		return isFrom;
	}

	/**
	 * Gets the new values used when setting them on to the target object.
	 * 
	 * @return
	 */
	public float[] getNewValues()
	{
		return accessorBuffer;
	}

	/**
	 * Gets the easing equation.
	 */
	public EaseType getEase()
	{
		return ease;
	}

	/**
	 * Gets the target values. The returned buffer is as long as the maximum allowed combined values. Therefore, you're
	 * surely not interested in all its content. Use {@link #getCombinedTweenCount()} to get the number of interesting
	 * slots.
	 */
	public float[] getTargetValues()
	{
		return targetValues;
	}

	/**
	 * Gets the number of combined animations.
	 */
	public int getCombinedAttributesCount()
	{
		return combinedAttrsCnt;
	}

	// -------------------------------------------------------------------------
	// Overrides
	// -------------------------------------------------------------------------
	@Override
	public Tween build()
	{
		if (getterSetter == null) return this;

		// create buffer, even though these aren't the exact start values
		accessorBuffer = getterSetter.get();

		if (targetValues.length != accessorBuffer.length)
		{
			Lttl.Throw("The number of target values is not the same as the getterSetter is getting.");
		}

		// define combinedAttrsCnt
		combinedAttrsCnt = accessorBuffer.length;

		return this;
	}

	@Override
	void free()
	{
		pool.free(this);
	}

	@Override
	protected void initializeOverride()
	{
		// getter setter may be null if it's a mark
		if (getterSetter == null) return;

		// get start values
		startValues = getterSetter.get();
		endValues = new float[combinedAttrsCnt];

		// modify the target values if end percentage is not 1
		if (getTargetPercentage() != 1)
		{
			if (isRelative())
			{
				for (int i = 0; i < targetValues.length; i++)
				{
					targetValues[i] *= getTargetPercentage();
				}
			}
			else
			{
				for (int i = 0; i < targetValues.length; i++)
				{
					targetValues[i] = startValues[i]
							+ ((targetValues[i] - startValues[i]) * getTargetPercentage());
				}
			}
		}

		// override duration if speed is set
		if (speed > 0)
		{
			// already started, so set duration directly by using the first target values
			// use target values because the end values have not been set and because the end values may not reflect the
			// aniamtion amount
			duration = LttlMath.abs(startValues[0] - targetValues[0]) / speed;
			if (LttlMath.isEaseActionWithMagnitude(ease))
			{
				duration *= 2;
			}
		}

		// set end values
		for (int i = 0; i < combinedAttrsCnt; i++)
		{
			if (LttlMath.isEaseActionWithMagnitude(ease))
			{
				endValues[i] = startValues[i];
			}
			else
			{
				endValues[i] = targetValues[i]
						+ (isRelative ? startValues[i] : 0);

				// HACK
				// this allows tweens to have a duration of 0
				// since we don't need a start value, this prevents a bug
				// this is necessary, since if you have a tween with 0 duration starting a sequence, but it has a delay,
				// then if it gets repeated, it will go to the start value before the delay, which is usually undesired.
				// 0 duration tweens are meant to be used to set the start of animation.
				if (getDuration() == 0)
				{
					startValues[i] = endValues[i];
				}

				if (isFrom)
				{
					float tmp = startValues[i];
					startValues[i] = endValues[i];
					endValues[i] = tmp;
				}
			}
		}
	}

	@Override
	protected void relativeHelper()
	{
		if (isRelative & !isRepeatYoyo())
		{
			for (int i = 0; i < combinedAttrsCnt; i++)
			{
				endValues[i] += (isReverseTrue(getStep()) ? -1 : 1)
						* targetValues[i];
				startValues[i] += (isReverseTrue(getStep()) ? -1 : 1)
						* targetValues[i];
			}
		}
	}

	@Override
	protected void updateOverride(int step, int lastStep,
			boolean isIterationStep, float delta)
	{
		if (getterSetter == null) return;

		// Case iteration end has been reached

		if (!isIterationStep && step > lastStep)
		{
			getterSetter.set(isReverse(lastStep) ? startValues : endValues);
			return;
		}

		if (!isIterationStep && step < lastStep)
		{
			getterSetter.set(isReverse(lastStep) ? endValues : startValues);
			return;
		}

		// Validation

		assert isIterationStep;
		assert getCurrentTime() >= 0;
		assert getCurrentTime() <= duration;

		// Case duration equals zero
		if (duration < epsilon && delta > -epsilon)
		{
			getterSetter.set(isReverse(step) ? endValues : startValues);
			return;
		}

		if (duration < epsilon && delta < epsilon)
		{
			getterSetter.set(isReverse(step) ? startValues : endValues);
			return;
		}

		// Normal behavior
		float time = isReverse(step) ? duration - getCurrentTime()
				: getCurrentTime();
		float t = time / duration;

		// generate interpolation value (0-1)
		// decide if need just one shared interpolation or unique ones (ie. shake)
		if (LttlMath.isEaseUnique(ease))
		{
			if (targetArray == null)
			{
				targetArray = new float[combinedAttrsCnt];
			}
			for (int i = 0; i < targetArray.length; i++)
			{
				targetArray[i] = LttlMath.interp(t, ease, easeParams);
			}
		}
		else
		{
			// generate 1 interpolation
			t = LttlMath.interp(t, ease, easeParams);
		}

		// apply interpolation
		for (int i = 0; i < combinedAttrsCnt; i++)
		{
			if (LttlMath.isEaseActionWithMagnitude(ease))
			{
				// if magnitude, then use target as the magnitude
				accessorBuffer[i] = startValues[i]
						+ ((targetArray == null) ? t : targetArray[i])
						* targetValues[i];
			}
			else
			{
				accessorBuffer[i] = startValues[i]
						+ ((targetArray == null) ? t : targetArray[i])
						* (endValues[i] - startValues[i]);
			}
		}

		// add noise
		if (addNoise)
		{
			float ns_t = (targetArray == null) ? t : targetArray[0];
			float fadeFactor = 1;
			if (ns_fadeIn || ns_fadeOut)
			{
				if (ns_fadeIn && ns_fadeOut)
				{
					fadeFactor = LttlMath.interpBoomerang(ns_t,
							EaseType.QuadOut);
				}
				else if (ns_fadeIn)
				{
					fadeFactor = LttlMath.interp(ns_t, EaseType.QuadOut);
				}
				else if (ns_fadeOut)
				{
					fadeFactor = LttlMath.interp(1 - ns_t, EaseType.QuadOut);
				}
			}

			LttlNoise.staticNoise.setDetail(ns_lod).setSmooth(ns_smooth);
			float ns_currentStep = ns_stepSizePerSecond * (t * getDuration());
			for (int i = 0; i < combinedAttrsCnt; i++)
			{
				accessorBuffer[i] += fadeFactor
						* LttlNoise.staticNoise.noise1D(ns_startStep[i]
								+ ns_currentStep, ns_startRange, ns_endRange);
			}
		}

		// add shake
		if (addShake)
		{
			if (sh_values == null)
			{
				sh_values = new float[combinedAttrsCnt];
			}

			// calculate the shake interpolation
			float sh_t = 0;
			switch (sh_easeMode)
			{
				case In:
					sh_t = t;
					break;
				case InOut:
					sh_t = LttlMath.interpBoomerang(t, EaseType.QuadInOut);
					break;
				case Out:
					sh_t = 1 - t;
					break;
				case Fixed:
					sh_t = 1;
					break;
			}

			// generate new shake values if time has passed
			sh_waitTime += LttlMath.abs(delta); // needs to be abs because could be going backwards
			if (sh_waitTime >= sh_rateTime)
			{
				sh_waitTime = 0;
				for (int i = 0; i < accessorBuffer.length; i++)
				{
					sh_values[i] = LttlMath.actionShakeFixed(sh_rangeBottom,
							sh_rangeTop);
				}
			}

			// set the new or old shake values and use the shake interpolation
			for (int i = 0; i < accessorBuffer.length; i++)
			{
				accessorBuffer[i] += sh_values[i] * sh_t;
			}
		}

		getterSetter.set(accessorBuffer);
	}

	// -------------------------------------------------------------------------
	// BaseTween impl.
	// -------------------------------------------------------------------------

	@Override
	public void forceStartValues()
	{
		if (getterSetter == null) return;
		getterSetter.set(startValues);
	}

	@Override
	public void forceEndValues()
	{
		if (getterSetter == null) return;
		getterSetter.set(endValues);
	}

	@Override
	protected boolean containsTarget(Object target)
	{
		return target != null && getterSetter.getTarget() == target;
	}

	@Override
	protected boolean containsHost(LttlComponent host)
	{
		return host.getId() == hostCompId;
	}

	@Override
	protected BaseTween<?> searchForId(long id)
	{
		// check self
		if (getId() == id) { return this; }
		return null;
	}

	// -------------------------------------------------------------------------
	// Extras
	// -------------------------------------------------------------------------

	/**
	 * Overrides the set duration by taking the difference from first target's start and end value, divided by the
	 * speed. This will give a constant rate of change with different durations.<br>
	 * If 0 or less, will ignore speed.
	 * 
	 * @param speed
	 * @return
	 */
	public Tween setSpeed(float speed)
	{
		if (isStarted())
		{
			Lttl.Throw("Can't change speed after tween has started.");
		}
		this.speed = speed;

		return this;
	}

	public float getSpeed()
	{
		return speed;
	}

	/**
	 * This sets the duration value on the tween. Helpful if you want to modify a tween's duration before starting it,
	 * but it was created somewhere else.
	 * 
	 * @param duration
	 * @return
	 */
	public Tween setDuration(float duration)
	{
		if (isStarted())
		{
			Lttl.Throw("Can't change duration after tween has started.");
		}
		if (duration < 0)
		{
			Lttl.Throw("Duration can't be negative");
		}
		this.duration = duration;

		return this;
	}

	public Tween addNoise(NoiseOptions options,
			boolean generateNewNoisePerIteration)
	{
		return addNoise(options.stepSizePerSecond, options.lod,
				options.startRange, options.endRange, options.smooth,
				options.fadeIn, options.fadeOut, generateNewNoisePerIteration);
	}

	/**
	 * Adds noise to tween.<br>
	 * <br>
	 * Noise is always additive, so you can actually do a tween with no change in target values but still add noise.
	 * 
	 * @param ns_stepSizePerSecond
	 *            .1f
	 * @param lod
	 *            6
	 * @param startRange
	 * @param endRange
	 * @param smooth
	 * @param fadeIn
	 * @param fadeOut
	 * @param generateNewNoisePerIteration
	 *            if true, each repeat iteration will have new noise seed
	 * @return self for chaining
	 */
	public Tween addNoise(float ns_stepSizePerSecond, int lod,
			float startRange, float endRange, boolean smooth, boolean fadeIn,
			boolean fadeOut, boolean generateNewNoisePerIteration)
	{
		if (isStarted())
		{
			Lttl.Throw("Can't add noise after tween has started.");
		}
		addNoise = true;
		this.generateNewNoisePerIteration = generateNewNoisePerIteration;
		this.ns_endRange = endRange;
		this.ns_startRange = startRange;
		this.ns_stepSizePerSecond = ns_stepSizePerSecond;
		this.ns_lod = lod;
		this.ns_smooth = smooth;
		this.ns_fadeIn = fadeIn;
		this.ns_fadeOut = fadeOut;
		return this;
	}

	private void setupNoise()
	{
		// define the start steps only on first iteration, or each iteration if specified
		if (addNoise && (!isInitialized() || generateNewNoisePerIteration))
		{
			// generate a random step starts for each because using same noise
			ns_startStep = new float[combinedAttrsCnt];
			for (int i = 0; i < ns_startStep.length; i++)
			{
				// NOTE may want to make this bigger if getting similar noise
				ns_startStep[i] = LttlMath.random(100f);
			}
		}
	}

	/**
	 * Adds additive shake after interpolation. Each target value will have a unique shake.
	 * 
	 * @param rangeBottom
	 *            minimum shake
	 * @param rangeTop
	 *            max shake
	 * @param rateTime
	 *            how much time between new shake amounts, if more than one frame, then no new shake value will be set,
	 *            this gives a less flashy, unrealistic shake
	 * @param easeMode
	 *            how the shake is added over the life of tween (fading)
	 * @return
	 */
	public Tween addShake(float rangeBottom, float rangeTop, float rateTime,
			EaseMode easeMode)
	{
		addShake = true;
		sh_rangeBottom = rangeBottom;
		sh_rangeTop = rangeTop;
		sh_rateTime = rateTime;
		sh_easeMode = easeMode;
		return this;
	}

	@Override
	protected void prepNewIteration()
	{
		setupNoise();
	}

	/**
	 * Returns the getterSetter so you can retrieve current values or set them.
	 * 
	 * <pre>
	 * getGetterSetter().get();
	 * getGetterSetter().set(1);
	 * </pre>
	 * 
	 * @return
	 */
	public TweenGetterSetter getGetterSetter()
	{
		return getterSetter;
	}
}
