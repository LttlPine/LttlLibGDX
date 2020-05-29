package com.lttlgames.tweenengine;

import java.util.ArrayList;

import com.lttlgames.editor.Lttl;
import com.lttlgames.editor.LttlComponent;
import com.lttlgames.editor.annotations.IgnoreCrawl;
import com.lttlgames.helpers.LttlMath;

@IgnoreCrawl
public abstract class BaseTween<T>
{
	// General
	private int step;
	private int repeatCnt;
	private boolean isIterationStep;
	private boolean isYoyo;
	private float targetPercentage;

	// Timings
	protected float delay;
	protected float duration;
	private float repeatDelay;
	private float currentTime;
	private float deltaTime;
	private boolean isStarted; // true when the object is started
	private boolean isInitialized; // true after the delay
	private boolean isFinished; // true when all repetitions are done
	private boolean isKilled; // true if kill() was called
	private boolean isPaused; // true if pause() was called

	// Misc
	protected Timeline parent;
	protected ArrayList<TweenCallback> callbacks = new ArrayList<TweenCallback>(
			0);
	private float stepCallbackTime;
	private Object userData;
	protected int hostCompId;
	protected boolean unPauseable;
	private long instanceId;
	boolean isAutoRemoveEnabled;
	private float speedMultiplier = 1;

	// -------------------------------------------------------------------------

	protected void reset()
	{
		clean();

		repeatCnt = 0;
		hostCompId = -1;
		isYoyo = isStarted = false;
		delay = duration = repeatDelay = 0;
		targetPercentage = 1;
		speedMultiplier = 1;
		callbacks.clear();
		userData = null;

		isAutoRemoveEnabled = true;

		// generate a new instance id
		instanceId = System.nanoTime();
	}

	// -------------------------------------------------------------------------
	// Public API
	// -------------------------------------------------------------------------

	/**
	 * Builds and validates the object. Only needed if you want to finalize a tween or timeline without starting it,
	 * since a call to ".start()" also calls this method.
	 * 
	 * @return The current object, for chaining instructions.
	 */
	abstract T build();

	/**
	 * Starts the object unmanaged. You will need to take care of its life-cycle. If you want the tween to be managed
	 * for you, use a {@link TweenManager}, or .start()<br>
	 * You can go straight to a specific point in time by giving it the time in the update() and if you want to go to
	 * another, just clean it first.
	 * 
	 * <pre>
	 * tween.startUnmanaged();
	 * tween.update(1.42f);
	 * tween.clean();
	 * tween.update(1.5f);
	 * </pre>
	 * 
	 * @return The current object, for chaining instructions.
	 */
	public T startUnmanaged()
	{
		if (isStarted())
		{
			Lttl.Throw("Tween already started/built.");
		}
		build();
		currentTime = 0;
		isStarted = true;
		return (T) this;
	}

	/**
	 * Adds to (default) tween manager and starts it. This should only be ran once.
	 * 
	 * @return
	 */
	final public T start()
	{
		Lttl.tween.getManager().add(this);
		return (T) this;
	}

	/**
	 * if the specific tween pertainting to the id specified is done/killed. This means that it not only has finished
	 * it's tween, but it is also done in any tween sequence it may have been in. This checks if it isKilled() or if
	 * it's unique id is different.<br>
	 * <br>
	 * The tween and the id should both be saved to be used with this function.
	 * 
	 * @param id
	 *            unique instance id for this tween (obtained with getId() on creation);
	 * @return
	 */
	public boolean isKilledUnique(long id)
	{
		// check to make sure the id is different, since if it was killed, it should have a different id
		return getId() != id || isKilled();
	}

	/**
	 * Returns the instance id of this tween/timeline. Use this to check to see if your tween is done or not, need to
	 * use ID since it can be reused from pool and look active again. Once you are done tracking tween it is best to
	 * NULL your reference out to free up memory.<br>
	 * <br>
	 * Alternatively you can save the id instead of the reference and use {@link TweenManager#getFromId(long)}
	 * 
	 * @return
	 */
	public long getId()
	{
		return instanceId;
	}

	/**
	 * Adds a delay to the tween or timeline.
	 * 
	 * @param delay
	 *            A duration.
	 * @return The current object, for chaining instructions.
	 */
	public T setDelay(float delay)
	{
		this.delay = delay;
		return (T) this;
	}

	/**
	 * Finishes the tween by updating to end in one giant leap, which kills it.
	 */
	public void killAndFinish()
	{
		if (isKilled()) return;
		update(Float.MAX_VALUE);
	}

	/**
	 * Kills the tween or timeline. If you are using a TweenManager, this object will be removed automatically.
	 */
	public void kill()
	{
		isKilled = true;
	}

	/**
	 * Frees the baseTween to the pool, it is assumed it is killed or finished.
	 */
	abstract void free();

	/**
	 * Pauses the tween or timeline. Further update calls won't have any effect. <br>
	 * <b>Note:</b> This can only be done if it is a Tween by itself or the highest timeline, since everthing is based
	 * on a fixed amount of time.
	 */
	public void pause()
	{
		if (!isStarted())
		{
			Lttl.Throw("Can only pause a started tween.");
		}
		if (this.parent != null)
		{
			Lttl.Throw("Can only pause top level tween.");
		}
		isPaused = true;
	}

	/**
	 * Resumes the tween or timeline. Has no effect is it was not already paused.
	 */
	public void resume()
	{
		isPaused = false;
	}

	/**
	 * Repeats (via Rewind) the tween or timeline for a given number of times.
	 * 
	 * @param count
	 *            The number of repetitions. For infinite repetition, use a negative number.
	 * @param delay
	 *            A delay between each iteration.
	 * @return The current tween or timeline, for chaining instructions.
	 */
	public T repeat(int count, float delay)
	{
		if (isStarted())
		{
			Lttl.Throw("You can't change the repetitions of a tween or timeline once it is started");
		}
		if (delay < 0)
		{
			Lttl.Throw("Repeat delay can't be negative");
		}
		repeatCnt = count;
		this.repeatDelay = delay;
		isYoyo = false;
		return (T) this;
	}

	/**
	 * Repeats the tween or timeline for a given number of times. Every two iterations, it will be played backwards.
	 * 
	 * @param count
	 *            The number of repetitions. For infinite repetition, use a negative number.
	 * @param delay
	 *            A delay before each repetition.
	 * @return The current tween or timeline, for chaining instructions.
	 */
	public T repeatYoyo(int count, float delay)
	{
		if (isStarted())
		{
			Lttl.Throw("You can't change the repetitions of a tween or timeline once it is started");
		}
		if (delay < 0)
		{
			Lttl.Throw("Repeat delay can't be negative");
		}
		repeatCnt = count;
		repeatDelay = delay;
		isYoyo = true;
		return (T) this;
	}

	/**
	 * Sets the <b>playback speed</b> of this tween by multiplying the incoming delta by this.<br>
	 * <b>This should only be done on highest/top-level tween.</b>
	 * 
	 * @param multiplier
	 * @return
	 */
	public T setSpeedMultiplier(float multiplier)
	{
		if (parent != null)
		{
			Lttl.Throw("Can't set speed on a tween with a parent.  Set the highest tween.");
		}
		speedMultiplier = multiplier;
		return (T) this;
	}

	/**
	 * adds the callback
	 * 
	 * @see TweenCallback
	 */
	public T addCallback(TweenCallback callback)
	{
		callbacks.add(callback);
		return (T) this;
	}

	/**
	 * Returns all the callbacks
	 * 
	 * @return
	 */
	public ArrayList<TweenCallback> getCallbacks()
	{
		return callbacks;
	}

	/**
	 * Attaches an object to this tween or timeline. It can be useful in order to retrieve some data from a CallBack.
	 * 
	 * @param data
	 *            Any kind of object.
	 * @return The current tween or timeline, for chaining instructions.
	 */
	public T setUserData(Object data)
	{
		userData = data;
		return (T) this;
	}

	// -------------------------------------------------------------------------
	// Getters
	// -------------------------------------------------------------------------

	public float getSpeed()
	{
		return speedMultiplier;
	}

	/**
	 * Gets the delay of the tween or timeline. Nothing will happen before this delay.
	 */
	public float getDelay()
	{
		return delay;
	}

	/**
	 * Gets the duration of a single iteration. (accurate after build())
	 */
	public float getDuration()
	{
		return duration;
	}

	/**
	 * Returns the complete duration, including initial delay and repetitions (accurate after build()). The formula is
	 * as follows:
	 * 
	 * <pre>
	 * fullDuration = delay + duration + (repeatDelay + duration) * repeatCnt
	 * </pre>
	 */
	public float getFullDuration()
	{
		if (repeatCnt < 0) return -1;
		return delay + duration + (repeatDelay + duration) * repeatCnt;
	}

	/**
	 * Gets the id of the current step. Values are as follows:<br/>
	 * <ul>
	 * <li>even numbers mean that an iteration is playing,<br/>
	 * <li>odd numbers mean that we are between two iterations,<br/>
	 * <li>-2 means that the initial delay has not ended,<br/>
	 * <li>-1 means that we are before the first iteration,<br/>
	 * <li>repeatCount*2 + 1 means that we are after the last iteration
	 */
	public int getStep()
	{
		return step;
	}

	/**
	 * Checks if this tween is after the last iteration.
	 * 
	 * @return
	 */
	public boolean isComplete()
	{
		return getStep() == getRepeatCount() * 2 + 1;
	}

	/**
	 * Gets the local time (for a single iteration, no delay or repeat delay, it also progresses the same if yoyoing or
	 * not on this tween)
	 */
	public float getCurrentTime()
	{
		return currentTime;
	}

	/**
	 * This is the current iteration value or progress of this Tween or Timeline (0-1). This could be forward or
	 * backward.
	 * 
	 * @return
	 */
	public float getIterationValue()
	{
		if (getDuration() <= 0) { return 1; }
		return getCurrentTime() / getDuration();
	}

	/**
	 * Gets the number of iterations that will be played.
	 */
	public int getRepeatCount()
	{
		return repeatCnt;
	}

	/**
	 * Gets the delay occuring between two iterations.
	 */
	public float getRepeatDelay()
	{
		return repeatDelay;
	}

	/**
	 * Gets the attached data, or null if none.
	 */
	public Object getUserData()
	{
		return userData;
	}

	/**
	 * Returns true if the tween or timeline has been started (doesn't mean it's managed).
	 */
	public boolean isStarted()
	{
		return isStarted;
	}

	/**
	 * Returns true if the tween or timeline has been initialized. Starting values for tweens are stored at
	 * initialization time. This initialization takes place right after the initial delay, if any.
	 */
	public boolean isInitialized()
	{
		return isInitialized;
	}

	/**
	 * Returns true if the tween is finished (i.e. if the tween has reached its end or has been killed). If you don't
	 * use a TweenManager, you may want to call {@link free()} to reuse the object later.<br>
	 * <br>
	 * This is not reliable if it is part of a repeating tween sequence.
	 */
	public boolean isFinished()
	{
		return isFinished || isKilled;
	}

	public boolean isKilled()
	{
		return isKilled;
	}

	/**
	 * Returns true if the repeat is to to play as yoyo and repeatCount > 0. Yoyo means that every other iterations, the
	 * animation will be played backwards.
	 */
	public boolean isRepeatYoyo()
	{
		return isYoyo && isRepeat();
	}

	/**
	 * Returns true if the repeat is to rewind and play again and repeatCount > 0.
	 */
	public boolean isRepeatRewind()
	{
		return isRepeat();
	}

	/**
	 * Returns true if set to repeat (checks repeat count, could be infinite)
	 */
	public boolean isRepeat()
	{
		return repeatCnt != 0;
	}

	/**
	 * Returns true if the tween or timeline is currently paused.
	 */
	public boolean isPaused()
	{
		return isPaused;
	}

	// -------------------------------------------------------------------------
	// Abstract API
	// -------------------------------------------------------------------------

	/**
	 * Tween: forces the end values via getter setter. Useful if killing tween and want it to finish or rever tween.
	 * This does not take into consideration if it is yoying or anything.
	 */
	protected abstract void forceStartValues();

	/**
	 * Tween: Forces the start values via getter setter. Useful if killing tween and want it to finish or rever tween.
	 * This does not take into consideration if it is yoying or anything.
	 */
	protected abstract void forceEndValues();

	protected abstract boolean containsTarget(Object target);

	protected abstract boolean containsHost(LttlComponent host);

	protected abstract BaseTween<?> searchForId(long id);

	// -------------------------------------------------------------------------
	// Protected API
	// -------------------------------------------------------------------------

	protected void initializeOverride()
	{
	}

	protected void relativeHelper()
	{
	}

	protected void prepNewIteration()
	{

	}

	protected void updateOverride(int step, int lastStep,
			boolean isIterationStep, float delta)
	{
	}

	protected void forceToStart()
	{
		currentTime = -delay;
		step = -1;
		isIterationStep = false;
		// This is weird, since it will never run
		if (isReverse(0))
		{
			forceEndValues();

			// HACK
			// these hacks callback if the tween is getting restarted, and this is crucial if it had a delay and it
			// returns to the start values before the delay, we call these callbacks so any updating can take affect
			if (getTreeDelay() > 0) callCallback(CallBack.STEP, 1);
		}
		else
		{
			forceStartValues();

			// HACK
			if (getTreeDelay() > 0) callCallback(CallBack.STEP, 0);
		}

	}

	protected void forceToEnd(float time)
	{
		currentTime = time - getFullDuration();
		step = repeatCnt * 2 + 1;
		isIterationStep = false;
		if (isReverse(repeatCnt * 2))
		{
			forceStartValues();
			// HACK
			if (getTreeDelay() > 0) callCallback(CallBack.STEP, 0);
		}
		else
		{
			forceEndValues();
			// HACK
			if (getTreeDelay() > 0) callCallback(CallBack.STEP, 1);
		}
	}

	protected void callCallback(CallBack type)
	{
		callCallback(type, -1);
	}

	protected void callCallback(CallBack type, float interpValue)
	{
		for (TweenCallback callback : callbacks)
		{
			if (callback == null) continue;

			callback.source = this;
			switch (type)
			{
				case BEGIN:
					callback.onBegin();
					break;
				case COMPLETE:
					callback.onComplete();
					break;
				case END:
					callback.onEnd();
					break;
				case START:
					callback.onStart();
					break;
				case STEP:
					// prevents more than one callback per frame
					if (stepCallbackTime < Lttl.game.getRawTime())
					{
						callback.onStep(interpValue);
						stepCallbackTime = Lttl.game.getRawTime();
					}
					break;
			}
		}
	}

	/**
	 * This gets the true reverse based on the direction of the parents.
	 * 
	 * @return
	 */
	protected boolean isReverseTrue(int step)
	{
		if (parent == null)
		{
			return isReverse(step);
		}
		else
		{
			int p = (parent.isReverseTrue(parent.getStep())) ? -1 : 1;
			int s = (isReverse(step)) ? -1 : 1;
			return p * s == -1;
		}
	}

	protected boolean isReverse(int step)
	{
		return isYoyo && LttlMath.abs(step % 4) == 2;
	}

	protected boolean isValid(int step)
	{
		return (step >= 0 && step <= repeatCnt * 2) || repeatCnt < 0;
	}

	protected void killHost(LttlComponent host)
	{
		if (containsHost(host)) kill();
	}

	protected void killTarget(Object target)
	{
		if (containsTarget(target)) kill();
	}

	// -------------------------------------------------------------------------
	// Update engine
	// -------------------------------------------------------------------------

	/**
	 * Updates the tween or timeline state. <b>You may want to use a TweenManager to update objects for you.</b> Slow
	 * motion, fast motion and backward play can be easily achieved by tweaking this delta time. Multiply it by -1 to
	 * play the animation backward, or by 0.5 to play it twice slower than its normal speed.<br>
	 * Note: if delta is 0 (with no delay), then it will not do anything, be sure to make it something, even if .000001
	 * 
	 * @param delta
	 *            A delta time between now and the last call.
	 */
	public void update(float delta)
	{
		if (!isStarted() || isPaused() || isKilled()) return;

		deltaTime = delta * speedMultiplier;

		// initializes once delay has been met
		if (!isInitialized)
		{
			initialize();
		}

		if (isInitialized)
		{
			testRelaunch();
			updateStep();
			testCompletion();
		}

		currentTime += deltaTime;
		deltaTime = 0;
	}

	/**
	 * This is used for tweens that are NOT managaged. It resets the tween/timeline back to before any updates were
	 * made.<br>
	 * <b>Do not use this when a tween is being managed.
	 */
	public void clean()
	{
		step = -2;
		isIterationStep = false;

		currentTime = deltaTime = stepCallbackTime = 0;
		isInitialized = isFinished = isKilled = isPaused = false;
	}

	private void initialize()
	{
		if (currentTime + deltaTime >= delay)
		{
			initializeOverride();
			prepNewIteration();
			isInitialized = true;
			isIterationStep = true;
			if (parent != null && speedMultiplier != 1)
			{
				Lttl.Throw("Only the highest top-level tween can have a speed mutliplier set.");
			}
			step = 0;
			deltaTime -= delay - currentTime;
			currentTime = 0;
			callCallback(CallBack.BEGIN);
			callCallback(CallBack.START);
		}
	}

	private void testRelaunch()
	{
		if (!isIterationStep && repeatCnt >= 0 && step < 0
				&& currentTime + deltaTime >= 0)
		{
			assert step == -1;
			isIterationStep = true;
			step = 0;
			float delta = 0 - currentTime;
			deltaTime -= delta;
			currentTime = 0;

			callCallback(CallBack.BEGIN);
			callCallback(CallBack.START);
			updateOverride(step, step - 1, isIterationStep, delta);

		}
		else if (!isIterationStep && repeatCnt >= 0 && step > repeatCnt * 2
				&& currentTime + deltaTime < 0)
		{
			assert step == repeatCnt * 2 + 1;
			isIterationStep = true;
			step = repeatCnt * 2;
			float delta = 0 - currentTime;
			deltaTime -= delta;
			currentTime = duration;

			// back begin
			callCallback(CallBack.COMPLETE);
			// back start
			callCallback(CallBack.END);
			updateOverride(step, step + 1, isIterationStep, delta);
		}
	}

	private void updateStep()
	{
		while (isValid(step))
		{
			// going backwards because in a yoyoing sequence
			if (!isIterationStep && currentTime + deltaTime <= 0)
			{
				isIterationStep = true;
				step -= 1;

				float delta = 0 - currentTime;
				deltaTime -= delta;
				currentTime = duration;

				relativeHelper();

				if (isReverse(step)) forceStartValues();
				else forceEndValues();

				prepNewIteration();

				// back end
				callCallback(CallBack.END);
				updateOverride(step, step + 1, isIterationStep, delta);
			}
			else if (!isIterationStep && currentTime + deltaTime >= repeatDelay)
			{
				isIterationStep = true;
				step += 1;

				float delta = repeatDelay - currentTime;
				deltaTime -= delta;
				currentTime = 0;

				relativeHelper();

				if (isReverse(step)) forceEndValues();
				else forceStartValues();

				prepNewIteration();

				// start
				callCallback(CallBack.START);
				updateOverride(step, step - 1, isIterationStep, delta);

			}
			else if (isIterationStep && currentTime + deltaTime < 0)
			{
				isIterationStep = false;
				step -= 1;

				float delta = 0 - currentTime;
				deltaTime -= delta;
				currentTime = 0;

				updateOverride(step, step + 1, isIterationStep, delta);
				// Step Callback
				// HACK
				endStepCallbackCheck(true, step - 1);
				// back end
				callCallback(CallBack.START);

				if (step < 0 && repeatCnt >= 0)
				{
					// back complete
					callCallback(CallBack.BEGIN);
				}
				else
				{
					currentTime = repeatDelay;
				}

			}
			else if (isIterationStep && currentTime + deltaTime > duration)
			{
				isIterationStep = false;
				step += 1;

				float delta = duration - currentTime;
				deltaTime -= delta;
				currentTime = duration;

				updateOverride(step, step - 1, isIterationStep, delta);
				// Step Callback
				// HACK
				endStepCallbackCheck(false, step - 1);
				callCallback(CallBack.END);

				if (step > repeatCnt * 2 && repeatCnt >= 0)
				{
					callCallback(CallBack.COMPLETE);
				}
				currentTime = 0;

			}
			else if (isIterationStep)
			{
				float delta = deltaTime;
				deltaTime -= delta;
				currentTime += delta;
				updateOverride(step, step, isIterationStep, delta);
				break;

			}
			else
			{
				float delta = deltaTime;
				deltaTime -= delta;
				currentTime += delta;
				break;
			}
		}
		// Step Callback
		// HACK
		if (isIterationStep && callbacks.size() > 0)
		{
			callCallback(CallBack.STEP,
					isReverse(getStep()) ? 1 - getIterationValue()
							: getIterationValue());
		}
	}

	private void testCompletion()
	{
		isFinished = repeatCnt >= 0 && (step > repeatCnt * 2 || step < 0);
	}

	enum CallBack
	{
		/**
		 * right after the delay (if any), and every time a timeline repeats it
		 */
		BEGIN, /**
		 * at each iteration beginning
		 */
		START, /**
		 * at each iteration ending, before the repeat delay
		 */
		END, /**
		 * at last END event, may happen multiple times if in a repeating timeline
		 */
		COMPLETE, /**
		 * Each iteration.
		 */
		STEP
	};

	/**
	 * This is automatically set based on the tween's hostComponent's transform's unpauseable setting at the time of the
	 * tweens creation (.to()). If this is a top-level tween/timeline then all children will update based on this, not
	 * their own unpauseable setting.
	 * 
	 * @return
	 */
	public boolean isUnPauseable()
	{
		return unPauseable;
	}

	protected void setup(LttlComponent hostComponent)
	{
		if (hostComponent == null)
		{
			this.hostCompId = -1;
			this.unPauseable = false;
		}
		else
		{
			this.unPauseable = hostComponent.t().unPauseable;
			this.hostCompId = hostComponent.getId();
		}
	}

	/**
	 * Searches scenes for host component.
	 * 
	 * @return null if not found
	 */
	protected LttlComponent getHostComponent()
	{
		if (hostCompId < 0) return null;
		return Lttl.scenes.findComponentByIdAllScenes(hostCompId);
	}

	public BaseTween<T> setAutoRemove(boolean value)
	{
		this.isAutoRemoveEnabled = value;
		return this;
	}

	/**
	 * @param back
	 *            if it is going backwards because of parent yoyo
	 * @param lastStep
	 */
	private void endStepCallbackCheck(boolean back, int lastStep)
	{
		// check if it is complete or before first iteration (because going in reverse) or if there is a repeat delay
		if (callbacks.size() > 0
				&& ((isRepeat() && getRepeatDelay() > 0) || isComplete() || getStep() == -1))
		{
			callCallback(CallBack.STEP, back ? isReverse(lastStep) ? 1 : 0
					: isReverse(lastStep) ? 0 : 1);
		}
	}

	float getTreeDelay()
	{
		Timeline p = parent;
		float delay = getDelay();
		while (p != null)
		{
			delay += LttlMath.max(0, p.getDelay());
			p = p.parent;
		}
		return delay;
	}

	/**
	 * Adjusts the target values so it tweens to to this percentage. This way you can tween to half way or w/e of target
	 * values. (must be between 0 and 1)<br>
	 * Can only be set before start/initialized.
	 * 
	 * @param perc
	 * @return
	 */
	public T setTargetPercentage(float perc)
	{
		if (isStarted())
		{
			Lttl.Throw("Can't change target percentage after tween has started.");
		}
		targetPercentage = LttlMath.Clamp01(perc);
		return (T) this;
	}

	public float getTargetPercentage()
	{
		return targetPercentage;
	}
}
