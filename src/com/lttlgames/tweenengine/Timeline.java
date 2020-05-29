package com.lttlgames.tweenengine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.lttlgames.editor.LttlComponent;
import com.lttlgames.helpers.LttlMath;

public final class Timeline extends BaseTween<Timeline>
{
	// -------------------------------------------------------------------------
	// Static -- pool
	// -------------------------------------------------------------------------

	private static final Pool.Callback<Timeline> poolCallback = new Pool.Callback<Timeline>()
	{
		@Override
		public void onPool(Timeline obj)
		{
			obj.reset();
		}

		@Override
		public void onUnPool(Timeline obj)
		{
			obj.reset();
		}
	};

	static final Pool<Timeline> pool = new Pool<Timeline>(10, poolCallback)
	{
		@Override
		protected Timeline create()
		{
			return new Timeline();
		}
	};

	/**
	 * Used for debug purpose. Gets the current number of empty timelines that are waiting in the Timeline pool.
	 */
	public static int getPoolSize()
	{
		return pool.size();
	}

	/**
	 * Increases the minimum capacity of the pool. Capacity defaults to 10.
	 */
	public static void ensurePoolCapacity(int minCapacity)
	{
		pool.ensureCapacity(minCapacity);
	}

	// -------------------------------------------------------------------------
	// Static -- factories
	// -------------------------------------------------------------------------

	/**
	 * Creates a new timeline with a 'sequence' behavior. Its children will be delayed so that they are triggered one
	 * after the other.
	 * 
	 * @param hostComponent
	 *            when this component is destroyed, it forces this Timeline to also be destroyed
	 */
	public static Timeline createSequence(LttlComponent hostComponent)
	{
		Timeline tl = pool.get();
		tl.setup(Modes.SEQUENCE, hostComponent);
		return tl;
	}

	/**
	 * Creates a new timeline with a 'parallel' behavior. Its children will be triggered all at once.
	 * 
	 * @param hostComponent
	 *            when this component is destroyed, it forces this Timeline to also be destroyed
	 */
	public static Timeline createParallel(LttlComponent hostComponent)
	{
		Timeline tl = pool.get();
		tl.setup(Modes.PARALLEL, hostComponent);
		return tl;
	}

	// -------------------------------------------------------------------------
	// Attributes
	// -------------------------------------------------------------------------

	private enum Modes
	{
		SEQUENCE, PARALLEL
	}

	private final List<BaseTween<?>> children = new ArrayList<BaseTween<?>>(10);
	private Timeline current;
	private Modes mode;
	private boolean isBuilt;

	// -------------------------------------------------------------------------
	// Setup
	// -------------------------------------------------------------------------

	private Timeline()
	{
		reset();
	}

	@Override
	protected void reset()
	{
		super.reset();

		children.clear();
		parent = current = null;

		isBuilt = false;
	}

	private void setup(Modes mode, LttlComponent hostComponent)
	{
		this.mode = mode;
		this.current = this;
		setup(hostComponent);
	}

	@Override
	public void clean()
	{
		super.clean();
		for (BaseTween<?> child : children)
		{
			child.clean();
		}
	}

	// -------------------------------------------------------------------------
	// Public API
	// -------------------------------------------------------------------------

	/**
	 * Adds a Tween/Timeline to the timeline.
	 * 
	 * @param baseTween
	 * @return the current timeline for chaining
	 */
	public Timeline push(BaseTween<?> baseTween)
	{
		if (isBuilt)
			throw new RuntimeException(
					"You can't push anything to a timeline once it is started");
		if (baseTween.getClass() == Tween.class)
		{
			push((Tween) baseTween);
		}
		else if (baseTween.getClass() == Timeline.class)
		{
			push((Timeline) baseTween);
		}
		return this;
	}

	/**
	 * Adds a Tween to the timeline.
	 * 
	 * @return The current timeline, for chaining instructions.
	 */
	public Timeline push(Tween tween)
	{
		if (isBuilt)
			throw new RuntimeException(
					"You can't push anything to a timeline once it is started");
		tween.parent = current;
		current.children.add(tween);
		return this;
	}

	/**
	 * Nests a Timeline in the current one.
	 * 
	 * @return The current timeline, for chaining instructions.
	 */
	public Timeline push(Timeline timeline)
	{
		if (isBuilt)
			throw new RuntimeException(
					"You can't push anything to a timeline once it is started");
		if (timeline.current != timeline)
			throw new RuntimeException(
					"You forgot to call a few 'end()' statements in your pushed timeline");
		timeline.parent = current;
		current.children.add(timeline);
		return this;
	}

	/**
	 * Adds all the Tweens in the list to the timeline.
	 * 
	 * @param tweenList
	 * @return
	 */
	public Timeline push(ArrayList<Tween> tweenList)
	{
		for (Tween tween : tweenList)
		{
			push(tween);
		}
		return this;
	}

	/**
	 * Adds a pause to the timeline. The pause may be negative if you want to overlap the preceding and following
	 * children.
	 * 
	 * @param time
	 *            A positive or negative duration.
	 * @return The current timeline, for chaining instructions.
	 */
	public Timeline pushPause(float time)
	{
		if (isBuilt)
			throw new RuntimeException(
					"You can't push anything to a timeline once it is started");
		current.children.add(Tween.mark(getHostComponent()).setDelay(time));
		return this;
	}

	/**
	 * Starts a nested timeline with a 'sequence' behavior. Don't forget to call {@link end()} to close this nested
	 * timeline.
	 * 
	 * @return The current timeline, for chaining instructions.
	 */
	public Timeline beginNestedSequence()
	{
		if (isBuilt)
			throw new RuntimeException(
					"You can't push anything to a timeline once it is started");
		Timeline tl = pool.get();
		tl.parent = current;
		tl.mode = Modes.SEQUENCE;
		current.children.add(tl);
		current = tl;
		return this;
	}

	/**
	 * Starts a nested timeline with a 'parallel' behavior. Don't forget to call {@link end()} to close this nested
	 * timeline.
	 * 
	 * @return The current timeline, for chaining instructions.
	 */
	public Timeline beginNestedParallel()
	{
		if (isBuilt)
			throw new RuntimeException(
					"You can't push anything to a timeline once it is started");
		Timeline tl = pool.get();
		tl.parent = current;
		tl.mode = Modes.PARALLEL;
		current.children.add(tl);
		current = tl;
		return this;
	}

	/**
	 * Closes the last nested timeline.
	 * 
	 * @return The current timeline, for chaining instructions.
	 */
	public Timeline end()
	{
		if (isBuilt)
			throw new RuntimeException(
					"You can't push anything to a timeline once it is started");
		if (current == this) throw new RuntimeException("Nothing to end...");
		current = (Timeline) current.parent;
		return this;
	}

	/**
	 * Gets a list of the timeline children. If the timeline is started, the list will be immutable.
	 */
	public List<BaseTween<?>> getChildren()
	{
		if (isBuilt) return Collections.unmodifiableList(current.children);
		else return current.children;
	}

	// -------------------------------------------------------------------------
	// Overrides
	// -------------------------------------------------------------------------

	@Override
	public Timeline build()
	{
		if (isBuilt) return this;

		duration = 0;

		for (int i = 0; i < children.size(); i++)
		{
			BaseTween<?> obj = children.get(i);

			if (obj.getRepeatCount() < 0)
				throw new RuntimeException(
						"You can't push an object with infinite repetitions in a timeline");
			obj.build();

			switch (mode)
			{
				case SEQUENCE:
					float tDelay = duration;
					duration += obj.getFullDuration();
					obj.delay += tDelay;
					break;

				case PARALLEL:
					duration = LttlMath.max(duration, obj.getFullDuration());
					break;
			}
		}

		isBuilt = true;
		return this;
	}

	@Override
	public Timeline startUnmanaged()
	{
		super.startUnmanaged();

		for (int i = 0; i < children.size(); i++)
		{
			BaseTween<?> obj = children.get(i);
			obj.startUnmanaged();
		}

		return this;
	}

	@Override
	public void free()
	{
		for (int i = children.size() - 1; i >= 0; i--)
		{
			BaseTween<?> obj = children.remove(i);
			obj.free();
		}

		pool.free(this);
	}

	@Override
	protected void updateOverride(int step, int lastStep,
			boolean isIterationStep, float delta)
	{
		if (!isIterationStep && step > lastStep)
		{
			assert delta >= 0;
			float dt = isReverse(lastStep) ? -delta - 1 : delta + 1;
			for (int i = 0, n = children.size(); i < n; i++)
				children.get(i).update(dt);
			return;
		}

		if (!isIterationStep && step < lastStep)
		{
			assert delta <= 0;
			float dt = isReverse(lastStep) ? delta + 1 : -delta - 1;
			for (int i = children.size() - 1; i >= 0; i--)
				children.get(i).update(dt);
			return;
		}

		assert isIterationStep;

		if (step > lastStep)
		{
			if (isReverse(step))
			{
				forceEndValues();
				for (int i = 0, n = children.size(); i < n; i++)
					children.get(i).update(delta);
			}
			else
			{
				forceStartValues();
				for (int i = 0, n = children.size(); i < n; i++)
					children.get(i).update(delta);
			}

		}
		else if (step < lastStep)
		{
			if (isReverse(step))
			{
				forceStartValues();
				for (int i = children.size() - 1; i >= 0; i--)
					children.get(i).update(delta);
			}
			else
			{
				forceEndValues();
				for (int i = children.size() - 1; i >= 0; i--)
					children.get(i).update(delta);
			}

		}
		else
		{
			float dt = isReverse(step) ? -delta : delta;
			if (delta >= 0) for (int i = 0, n = children.size(); i < n; i++)
				children.get(i).update(dt);
			else for (int i = children.size() - 1; i >= 0; i--)
				children.get(i).update(dt);
		}
	}

	// -------------------------------------------------------------------------
	// BaseTween impl.
	// -------------------------------------------------------------------------
	@Override
	protected void forceStartValues()
	{
		for (int i = children.size() - 1; i >= 0; i--)
		{
			BaseTween<?> obj = children.get(i);
			obj.forceToStart();
		}
	}

	@Override
	protected void forceEndValues()
	{
		for (int i = 0, n = children.size(); i < n; i++)
		{
			BaseTween<?> obj = children.get(i);
			obj.forceToEnd(duration);
		}
	}

	@Override
	protected boolean containsHost(LttlComponent host)
	{
		for (int i = 0, n = children.size(); i < n; i++)
		{
			BaseTween<?> obj = children.get(i);
			if (obj.containsHost(host)) return true;
		}
		return false;
	}

	@Override
	protected boolean containsTarget(Object target)
	{
		for (int i = 0, n = children.size(); i < n; i++)
		{
			BaseTween<?> obj = children.get(i);
			if (obj.containsTarget(target)) return true;
		}
		return false;
	}

	@Override
	protected BaseTween<?> searchForId(long id)
	{
		// check self
		if (getId() == id) { return this; }

		// check children
		for (BaseTween<?> child : children)
		{
			BaseTween<?> result = child.searchForId(id);
			if (result != null) { return result; }
		}
		return null;
	}

	@Override
	protected void initializeOverride()
	{
		// make sure all children have same target percentage
		if (getTargetPercentage() != 1)
		{
			for (BaseTween<?> child : children)
			{
				child.setTargetPercentage(getTargetPercentage());
			}
		}
	}
}
