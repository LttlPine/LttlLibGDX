package com.lttlgames.tweenengine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.lttlgames.editor.Lttl;
import com.lttlgames.editor.LttlComponent;

/**
 * A TweenManager updates all your tweens and timelines at once. Its main interest is that it handles the tween/timeline
 * life-cycles for you, as well as the pooling constraints (if object pooling is enabled).
 * <p/>
 * Just give it a bunch of tweens or timelines and call update() periodically, you don't need to care for anything else!
 * Relax and enjoy your animations.
 * 
 * @see Tween
 * @see Timeline
 * @author Aurelien Ribon | http://www.aurelienribon.com/
 */
public class TweenManager
{
	// -------------------------------------------------------------------------
	// Public API
	// -------------------------------------------------------------------------

	private final ArrayList<BaseTween<?>> objects = new ArrayList<BaseTween<?>>(
			20);
	private boolean isPaused = false;

	/**
	 * Adds a tween or timeline to the manager, does not start it.
	 * 
	 * @return The manager, for instruction chaining.
	 */
	public TweenManager add(BaseTween<?> object)
	{
		if (objects.contains(object))
		{
			Lttl.Throw("This tween object is already on manager or did not get removed properly when free to pool.");
		}
		objects.add(object);
		object.startUnmanaged();
		return this;
	}

	/**
	 * Finds a BaseTween object (Tween or Timeline) from an id, returning null means none exists, probably finished.<br>
	 * This is untested, and could be slow.
	 * 
	 * @param id
	 *            this id should should have been saved from the tween object you are looking for by using getId()
	 * @return
	 */
	public BaseTween<?> getFromId(long id)
	{
		for (BaseTween<?> bt : objects)
		{
			BaseTween<?> result = bt.searchForId(id);
			if (result != null) { return result; }
		}
		return null;
	}

	/**
	 * Returns true if the manager contains any valid interpolation associated to the given target object.
	 */
	public boolean containsTarget(Object target)
	{
		for (int i = 0, n = objects.size(); i < n; i++)
		{
			BaseTween<?> obj = objects.get(i);
			if (obj.containsTarget(target)) return true;
		}
		return false;
	}

	/**
	 * Kills every managed tweens and timelines.
	 */
	public void killAll()
	{
		for (int i = 0, n = objects.size(); i < n; i++)
		{
			BaseTween<?> obj = objects.get(i);
			obj.kill();
		}
	}

	/**
	 * Kills every tweens associated to the given target. Will also kill every timelines containing a tween associated
	 * to the given target.
	 */
	public void killTarget(Object target)
	{
		for (int i = 0, n = objects.size(); i < n; i++)
		{
			BaseTween<?> obj = objects.get(i);
			if (obj.isKilled()) continue;
			obj.killTarget(target);
		}
	}

	/**
	 * Kills every tweens associated with the host component. Will also kill every timelines containing a tween
	 * associated to the given host component.
	 */
	public void killHost(LttlComponent host)
	{
		for (int i = 0, n = objects.size(); i < n; i++)
		{
			BaseTween<?> obj = objects.get(i);
			if (obj.isKilled()) continue;
			obj.killHost(host);
		}
	}

	/**
	 * Increases the minimum capacity of the manager. Defaults to 20.
	 */
	public void ensureCapacity(int minCapacity)
	{
		objects.ensureCapacity(minCapacity);
	}

	/**
	 * Pauses the manager. Further update calls won't have any effect.
	 */
	public void pause()
	{
		isPaused = true;
	}

	/**
	 * Resumes the manager, if paused.
	 */
	public void resume()
	{
		isPaused = false;
	}

	/**
	 * Updates every tweens with a delta time ang handles the tween life-cycles automatically. If a tween is finished,
	 * it will be removed from the manager. The delta time represents the elapsed time between now and the last update
	 * call. Each tween or timeline manages its local time, and adds this delta to its local time to update itself.
	 */
	public void update(float delta)
	{
		// only iterates through the top level base tweens
		for (int i = objects.size() - 1; i >= 0; i--)
		{
			BaseTween<?> obj = objects.get(i);
			if (obj.isFinished() && obj.isAutoRemoveEnabled)
			{
				objects.remove(i);
				obj.free();
			}
		}

		if (!isPaused) // if tween manager itself is not paused
		{
			if (delta >= 0)
			{
				for (int i = 0, n = objects.size(); i < n; i++)
				{
					// if game paused don't update tweens on a transform that is pauseable
					// NOTE this is only for top-level tweens, it is assumed all children are also unpauseable
					if (Lttl.game.isPaused() && !objects.get(i).unPauseable)
					{
						continue;
					}
					objects.get(i).update(delta);
				}
			}
			else
			{
				for (int i = objects.size() - 1; i >= 0; i--)
				{
					if (Lttl.game.isPaused() && !objects.get(i).unPauseable)
					{
						continue;
					}
					objects.get(i).update(delta);
				}
			}
		}
	}

	/**
	 * Gets the number of managed objects. An object may be a tween or a timeline. Note that a timeline only counts for
	 * 1 object, since it manages its children itself.
	 * <p/>
	 * To get the count of running tweens, see {@link #getRunningTweensCount()}.
	 */
	public int size()
	{
		return objects.size();
	}

	/**
	 * Gets the number of running tweens. This number includes the tweens located inside timelines (and nested
	 * timelines).
	 * <p/>
	 * <b>Provided for debug purpose only.</b>
	 */
	public int getRunningTweensCount()
	{
		return getTweensCount(objects);
	}

	/**
	 * Gets the number of running timelines. This number includes the timelines nested inside other timelines.
	 * <p/>
	 * <b>Provided for debug purpose only.</b>
	 */
	public int getRunningTimelinesCount()
	{
		return getTimelinesCount(objects);
	}

	/**
	 * Gets an immutable list of every managed object.
	 * <p/>
	 * <b>Provided for debug purpose only.</b>
	 */
	public List<BaseTween<?>> getObjects()
	{
		return Collections.unmodifiableList(objects);
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private static int getTweensCount(List<BaseTween<?>> objs)
	{
		int cnt = 0;
		for (int i = 0, n = objs.size(); i < n; i++)
		{
			BaseTween<?> obj = objs.get(i);
			if (obj instanceof Tween) cnt += 1;
			else cnt += getTweensCount(((Timeline) obj).getChildren());
		}
		return cnt;
	}

	private static int getTimelinesCount(List<BaseTween<?>> objs)
	{
		int cnt = 0;
		for (int i = 0, n = objs.size(); i < n; i++)
		{
			BaseTween<?> obj = objs.get(i);
			if (obj instanceof Timeline)
			{
				cnt += 1 + getTimelinesCount(((Timeline) obj).getChildren());
			}
		}
		return cnt;
	}
}
