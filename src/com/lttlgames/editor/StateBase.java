package com.lttlgames.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import com.lttlgames.components.interfaces.AnimationCallback;
import com.lttlgames.editor.LttlObjectGraphCrawler.FieldsMode;
import com.lttlgames.editor.annotations.GuiButton;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.helpers.EaseType;
import com.lttlgames.helpers.LttlMath;
import com.lttlgames.tweenengine.Timeline;
import com.lttlgames.tweenengine.Tween;
import com.lttlgames.tweenengine.TweenCallback;
import com.lttlgames.tweenengine.TweenGetterSetter;

/**
 * Creates a state for a specific type of LttlComponent
 * 
 * @author Josh
 * @param <T>
 */
@Persist(-9052)
public abstract class StateBase<T extends LttlComponent>
{
	@Persist(905200)
	public String name;

	/**
	 * If true, will do a step callback on the target component if it implements AnimationCallback while animating to
	 * this state.<br>
	 * Setting the stepCallback will be for the entire state timeline, not the individual properties, so it will
	 * callback for each interpolation, even if some properties start or end at different times.
	 */
	@Persist(905201)
	public boolean stepCallback = false;

	private ArrayList<StateProperty<?>> statePropertiesCached;

	/**
	 * Generates the parallel timeline tween for this state for the provided component. The individual state properties
	 * may be turned off to be animated, have their own duration, delay, easetype, and other options.
	 * 
	 * @param component
	 * @param duration
	 * @param easeType
	 * @return
	 */
	public Timeline getTweenParallel(final T component, float duration,
			EaseType easeType)
	{
		// create timeline
		Timeline timeline = Lttl.tween.createParallel(component);

		// add all tweens
		timeline.push(getAllTweens(component, duration, easeType));

		// add step callback
		if (stepCallback
				&& AnimationCallback.class.isAssignableFrom(component
						.getClass()))
		{
			timeline.addCallback(new TweenCallback()
			{
				@Override
				public void onStep(float iterationPosition)
				{
					((AnimationCallback) component).onStep(name,
							iterationPosition);
				}
			});
		}

		return timeline;
	}

	/**
	 * Returns a list of all the animatable tweens for the component specified. This way you can manually add callbacks
	 * and other specifications to them and add them to their own timeline sequence.
	 * 
	 * @param component
	 *            the component taht it affects
	 * @param duration
	 * @param easeType
	 * @return
	 */
	public ArrayList<Tween> getAllTweens(T component, float duration,
			EaseType easeType)
	{
		ArrayList<Tween> list = new ArrayList<Tween>();

		// get all the state properties with their getter and setters
		HashMap<StateProperty<?>, TweenGetterSetter> map = generatePropetyMap(
				component, getNewMap());
		for (Entry<StateProperty<?>, TweenGetterSetter> e : map.entrySet())
		{
			StateProperty<?> prop = e.getKey();
			TweenGetterSetter getSet = e.getValue();

			// check if should animate
			if (prop.active)
			{
				list.add(prop.getTween(component, getSet, duration, easeType));
			}
		}

		return list;
	}

	/**
	 * Immediately changes the animated properties to this state for the specified component. No tween.<br>
	 * Note: Skips any properties that relative or repeat yoyo with odd repeat count
	 * 
	 * @param component
	 */
	public void goTo(T component)
	{
		HashMap<StateProperty<?>, TweenGetterSetter> map = generatePropetyMap(
				component, getNewMap());
		for (Entry<StateProperty<?>, TweenGetterSetter> e : map.entrySet())
		{
			StateProperty<?> prop = e.getKey();
			TweenGetterSetter getSet = e.getValue();

			// check if should animate
			if (prop.active)
			{
				if (prop.isRepeatYoyoOdd() || prop.isRelative()) continue;
				getSet.set(prop.getTargetValues());
			}
		}
		if (AnimationCallback.class.isAssignableFrom(component.getClass()))
		{
			((AnimationCallback) component).onStep(name, 1);
		}
	}

	/**
	 * Used internally only for GUI
	 * 
	 * @param component
	 */
	void goToTween(final T component)
	{
		HashMap<StateProperty<?>, TweenGetterSetter> map = generatePropetyMap(
				component, getNewMap());

		Timeline parallel = component.tweenParallel();
		for (Entry<StateProperty<?>, TweenGetterSetter> e : map.entrySet())
		{
			StateProperty<?> prop = e.getKey();
			TweenGetterSetter getSet = e.getValue();

			// check if should animate
			if (prop.active && !(prop.isRepeatYoyoOdd() || prop.isRelative()))
			{
				parallel.push(component.tweenTo(1, getSet,
						prop.getTargetValues()));
			}
		}
		parallel.addCallback(new TweenCallback()
		{
			@Override
			public void onStep(float iterationPosition)
			{
				if (AnimationCallback.class.isAssignableFrom(component
						.getClass()))
				{
					((AnimationCallback) component).onStep(name,
							iterationPosition);
				}
			}
		});
		parallel.start();
	}

	/**
	 * Generates a tween for the single specified property for this component regardless if it is active or not. The
	 * property's duration, delay, and easetype override will still take priority.<br>
	 * Still need to call start() on tween.
	 * 
	 * @param component
	 * @param prop
	 * @param duration
	 * @param easeType
	 * @return
	 */
	public Tween getTween(T component, StateProperty<?> prop, float duration,
			EaseType easeType)
	{
		Lttl.Throw(prop);
		HashMap<StateProperty<?>, TweenGetterSetter> map = generatePropetyMap(
				component, getNewMap());
		TweenGetterSetter getSet = map.get(prop);
		if (getSet == null)
		{
			Lttl.Throw("No GetterSetter for the given property.");
		}
		return prop.getTween(component, getSet, duration, easeType);
	}

	/**
	 * Returns a hashmap of all the state properties and their TweenGetterSetter. This is set manually on each state
	 * type.<br>
	 * This can be used on multiple subClasses. Just be sure to call the super at the start.
	 * 
	 * @param component
	 * @param map
	 * @return
	 */
	protected abstract HashMap<StateProperty<?>, TweenGetterSetter> generatePropetyMap(
			T component, HashMap<StateProperty<?>, TweenGetterSetter> map);

	/**
	 * Returns a hashmap of all the state properties and their TweenGetterSetter.
	 * 
	 * @param component
	 * @return
	 */
	public HashMap<StateProperty<?>, TweenGetterSetter> generatePropertyMap(
			T component)
	{
		return generatePropetyMap(component, getNewMap());
	}

	/**
	 * internal use only, used for updating all properties or just one
	 * 
	 * @param component
	 * @param onlyProp
	 */
	void updateInternal(T component, StateProperty<?> onlyProp)
	{
		if (component == null) return;

		// get all the state properties with their getter and setters
		HashMap<StateProperty<?>, TweenGetterSetter> map = generatePropetyMap(
				component, getNewMap());

		// update each property
		for (Entry<StateProperty<?>, TweenGetterSetter> e : map.entrySet())
		{
			StateProperty<?> prop = e.getKey();
			TweenGetterSetter getSet = e.getValue();

			if (onlyProp != null && prop != onlyProp) continue;

			// check if should not autoupdate
			if (onlyProp == null && prop.options != null
					&& prop.options.autoUpdate)
			{
				continue;
			}

			// set the state's property values from the current values
			prop.setTargetValues(getSet.get());
		}
	}

	/**
	 * Updates the state's fields to the current values of component specified.
	 * 
	 * @param component
	 */
	public void update(T component)
	{
		beforeUpdate(component);
		updateInternal(component, null);
	}

	/**
	 * editor use only
	 */
	@SuppressWarnings("rawtypes")
	@GuiButton(order = 1)
	private void updateState(GuiFieldObject source)
	{
		LttlStateManager manager = ((LttlStateManager) source.parent.parent.objectRef);
		update((T) manager.target);
	}

	/**
	 * Editor only.
	 * 
	 * @param source
	 */
	@SuppressWarnings("rawtypes")
	@GuiButton(order = 5)
	private void renameState(GuiFieldObject source)
	{
		String stateName = GuiHelper.showTextFieldModal("Rename State", "");
		if (stateName == null) { return; }

		LttlStateManager manager = ((LttlStateManager) source.parent.parent.objectRef);
		if (manager.getAllStateNames().contains(stateName))
		{
			Lttl.logNote("No state created because a state already exists with that name on this State Manager.");
			return;
		}

		this.name = stateName;
	}

	/**
	 * editor use only
	 */
	@SuppressWarnings("rawtypes")
	@GuiButton(tooltip = "Updates all the states in the tree with this name.", order = 2)
	private void updateAllStatesInTree(GuiFieldObject source)
	{
		LttlStateManager manager = ((LttlStateManager) source.parent.parent.objectRef);
		manager.updateAllStatesInTree(name);
	}

	/**
	 * editor use only
	 */
	@SuppressWarnings("rawtypes")
	@GuiButton(tooltip = "Renames all the states in the tree with this name.", order = 6)
	private void renameAllStatesInTree(GuiFieldObject source)
	{
		String newName = GuiHelper.showTextFieldModal("Rename All States", "");
		if (newName == null) { return; }

		LttlStateManager manager = ((LttlStateManager) source
				.getAncestorByClass(LttlStateManager.class, true).objectRef);
		manager.renameAllStatesInTree(name, newName);
	}

	/**
	 * editor use only
	 */
	@SuppressWarnings("rawtypes")
	@GuiButton(order = 3)
	private void goToState(GuiFieldObject source)
	{
		LttlStateManager manager = ((LttlStateManager) source
				.getAncestorByClass(LttlStateManager.class, true).objectRef);
		goToTween((T) manager.target);
	}

	/**
	 * editor use only
	 */
	@SuppressWarnings("rawtypes")
	@GuiButton(order = 4)
	private void goToAllStatesInTree(GuiFieldObject source)
	{
		LttlStateManager manager = ((LttlStateManager) source
				.getAncestorByClass(LttlStateManager.class, true).objectRef);
		manager.goToTweenAllStatesInTree(name);
	}

	private HashMap<StateProperty<?>, TweenGetterSetter> getNewMap()
	{
		return new HashMap<StateProperty<?>, TweenGetterSetter>();
	}

	/**
	 * <b>Does not take into consideration property level delays or durations</b><br>
	 * sets the values between two states.<br>
	 * if a value is active on fromState but not toState (this), then it is set to fromState. If the toState has an
	 * active field that fromState does not, then it will just take the current value and ease to that.<br>
	 * Note: Skips any properties that relative or repeat yoyo with odd repeat count
	 * 
	 * @param fromState
	 * @param ease
	 * @param value
	 */
	public void easeFromState(StateBase<T> fromState, T component,
			EaseType ease, float value)
	{
		// set all values to from state
		fromState.goTo(component);

		// ease properties from current to this state
		easeFromCurrent(component, ease, value);
	}

	/**
	 * <b>Does not take into consideration property level delays or durations</b><br>
	 * sets the values between current state and this state.<br>
	 * Note: Skips any properties that relative or repeat yoyo with odd repeat count
	 * 
	 * @param ease
	 * @param value
	 */
	public void easeFromCurrent(T component, EaseType ease, float value)
	{
		// ease properties from current to this state
		HashMap<StateProperty<?>, TweenGetterSetter> map = generatePropetyMap(
				component, getNewMap());
		for (Entry<StateProperty<?>, TweenGetterSetter> e : map.entrySet())
		{
			StateProperty<?> prop = e.getKey();
			TweenGetterSetter getSet = e.getValue();

			// check if should animate
			if (prop.active && !(prop.isRepeatYoyoOdd() || prop.isRelative()))
			{
				float[] currentValues = getSet.get();
				float[] targetValues = prop.getTargetValues();
				for (int i = 0; i < targetValues.length; i++)
				{
					// ease values
					targetValues[i] = LttlMath.interp(currentValues[i],
							targetValues[i], value, ease);
				}
				// set values
				getSet.set(targetValues);
			}
		}
	}

	/**
	 * Runs before state is updated or created for first time. Used to prep anything before the state is populated.<br>
	 * Can just be left empty.
	 */
	protected abstract void beforeUpdate(T comp);

	public ArrayList<StateProperty<?>> getStateProperties()
	{
		// one time generate
		if (statePropertiesCached == null)
		{
			statePropertiesCached = new ArrayList<StateProperty<?>>();
			for (ProcessedFieldType f : LttlObjectGraphCrawler.getAllFields(
					getClass(), FieldsMode.AllButIgnore))
			{
				if (StateProperty.class.isAssignableFrom(f.getCurrentClass()))
				{
					try
					{
						statePropertiesCached.add((StateProperty<?>) f
								.getField().get(this));
					}
					catch (IllegalArgumentException | IllegalAccessException e)
					{
						e.printStackTrace();
					}
				}
			}
		}
		return statePropertiesCached;
	}

	@GuiButton(order = 7)
	private void deactivateAll()
	{
		for (StateProperty<?> sp : getStateProperties())
		{
			sp.active = false;
		}
	}

	@GuiButton(order = 8)
	private void activateAll()
	{
		for (StateProperty<?> sp : getStateProperties())
		{
			sp.active = true;
		}
	}
}
