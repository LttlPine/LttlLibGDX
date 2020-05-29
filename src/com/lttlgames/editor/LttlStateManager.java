package com.lttlgames.editor;

import java.util.ArrayList;

import com.lttlgames.editor.annotations.GuiButton;
import com.lttlgames.editor.annotations.GuiHideArrayListControls;
import com.lttlgames.editor.annotations.GuiListItemNameField;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.helpers.EaseType;
import com.lttlgames.helpers.LttlHelper;
import com.lttlgames.tweenengine.Timeline;

@Persist(-9059)
public abstract class LttlStateManager<T extends LttlComponent, X extends StateBase<T>>
		extends LttlComponent
{
	@Persist(905901)
	public T target;

	@GuiListItemNameField("name")
	@GuiHideArrayListControls(canAdd = false)
	@Persist(905902)
	public ArrayList<X> states = new ArrayList<>();

	/**
	 * Defines the component class for this state manager.
	 * 
	 * @return
	 */
	public abstract Class<T> getTargetClass();

	/**
	 * Defines the state class for this state manager.
	 * 
	 * @return
	 */
	public abstract Class<X> getStateClass();

	@Override
	public void onEditorCreate()
	{
		// see if you can find a component on this transform that fits the target component class exactly, if not, then
		// looks for sub class
		if (getTargetClass() == LttlTransform.class)
		{
			target = (T) t();
		}
		else
		{
			target = t().getComponent(getTargetClass(), false);
			if (target == null)
			{
				target = t().getComponent(getTargetClass(), true);
			}
		}
	}

	/**
	 * Only meant to be used in editor GUI
	 * 
	 * @param source
	 */
	@GuiButton
	private void createState()
	{
		ArrayList<String> names = getAllStateNamesTree();
		String stateName = GuiHelper.showComboboxModal("Create State", "", true, false,
				false, names.toArray(new String[names.size()]));
		if (stateName == null) { return; }
		if (getAllStateNames().contains(stateName))
		{
			Lttl.logNote(
					"No state created because a state already exists with that name on this State Manager.");
			return;
		}

		X newState = LttlObjectGraphCrawler.newInstance(getStateClass());
		newState.name = stateName;

		// if target component exists, update the new state to this component's values
		if (target != null)
		{
			newState.update(target);
		}

		states.add(newState);
	}

	/**
	 * Creates a new state on all of the state managers on this tree.
	 */
	@GuiButton
	@SuppressWarnings("rawtypes")
	private void createStateAll()
	{
		ArrayList<String> names = getAllStateNamesTree();

		String stateName = GuiHelper.showTextFieldModal("Create State (All)",
				"New state name for all state managers in tree.");
		if (stateName == null) { return; }
		if (names.contains(stateName))
		{
			Lttl.logNote("No states created because state '" + stateName
					+ "' already exists with that name on at least one State Manager on this tree.");
			return;
		}

		// create the new state for each manager with the relatedStateName
		for (LttlStateManager manager : getAllStateManagersInTree())
		{
			StateBase newState = (StateBase) LttlObjectGraphCrawler
					.newInstance(manager.getStateClass());
			newState.name = stateName;

			// if target component exists, update the new state to this component's values
			if (manager.target != null)
			{
				newState.update(manager.target);
			}

			manager.states.add(newState);
		}
	}

	/**
	 * Creates a new state on all of the state managers on this tree that have the selected state. Good for creating new
	 * states on all the managers when creating a group of states.
	 */
	@GuiButton
	@SuppressWarnings("rawtypes")
	private void createStateAllSimilar()
	{
		ArrayList<String> names = getAllStateNamesTree();
		String relatedStateName =
				GuiHelper.showComboboxModal("Create State (All) - Dialog 1",
						"All the LttlStateManagers with the state selected below will have a state created (next dialog).",
						false, true, true, names.toArray(new String[names.size()]));
		if (relatedStateName == null) { return; }

		String stateName = GuiHelper.showTextFieldModal("Create State (All) - Dialog 2",
				"New state name for all state managers that have state '"
						+ relatedStateName + "'.");
		if (stateName == null) { return; }
		if (names.contains(stateName))
		{
			Lttl.logNote("No states created because state '" + stateName
					+ "' already exists with that name on at least one State Manager on this tree.");
			return;
		}

		// create the new state for each manager with the relatedStateName
		for (LttlStateManager manager : getAllStateManagersInTree())
		{
			if (manager.getAllStateNames().contains(relatedStateName))
			{
				StateBase newState = (StateBase) LttlObjectGraphCrawler
						.newInstance(manager.getStateClass());
				newState.name = stateName;

				// if target component exists, update the new state to this component's values
				if (manager.target != null)
				{
					newState.update(manager.target);
				}

				manager.states.add(newState);
			}
		}
	}

	/**
	 * Returns all the unique state names on all the state managers (all types) on the entire transform tree (above and
	 * below)
	 * 
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public ArrayList<String> getAllStateNamesTree()
	{
		ArrayList<String> list = new ArrayList<>();
		for (LttlStateManager manager : getAllStateManagersInTree())
		{
			manager.getStateNames(list);
		}

		return list;
	}

	/**
	 * doesn't add duplicates
	 * 
	 * @param list
	 * @return
	 */
	public ArrayList<String> getStateNames(ArrayList<String> list)
	{
		if (list == null)
		{
			list = new ArrayList<>();
		}
		if (states != null)
		{
			for (X state : states)
			{
				if (!list.contains(state.name))
				{
					list.add(state.name);
				}
			}
		}

		return list;
	}

	/**
	 * Returns all the unqiue state names for the states for this state manager.
	 * 
	 * @return
	 */
	public ArrayList<String> getAllStateNames()
	{
		return getStateNames(null);
	}

	/**
	 * Returns all the states in this tree with the specified name.
	 * 
	 * @param stateName
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public ArrayList<StateBase> getAllStatesInTree(String stateName)
	{
		ArrayList<StateBase> states = new ArrayList<>();
		for (LttlStateManager manager : getAllStateManagersInTree())
		{
			if (manager.states == null) continue;
			for (Object state : manager.states)
			{
				StateBase stateBase = (StateBase) state;
				if (stateBase.name.equals(stateName))
				{
					states.add(stateBase);
				}
			}
		}
		return states;
	}

	/**
	 * Returns all the State Manager components in this transform tree (above and below)
	 * 
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public ArrayList<LttlStateManager> getAllStateManagersInTree()
	{
		return t().getHighestParent().getComponentsInTree(LttlStateManager.class, true);
	}

	public void updateState(String stateName)
	{
		X state = getState(stateName);
		if (state != null && target != null)
		{
			state.update(target);
		}
	}

	@SuppressWarnings("rawtypes")
	public Timeline getAllTweensInTree(String stateName, float duration,
			EaseType easeType)
	{
		Timeline parallel = tweenParallel();
		for (LttlStateManager manager : getAllStateManagersInTree())
		{
			StateBase state = manager.getState(stateName);
			if (state != null && manager.target != null)
			{
				parallel.push(state.getAllTweens(manager.target, duration, easeType));
			}
		}

		return parallel;
	}

	/**
	 * Get a state on this manager by name.
	 * 
	 * @param stateName
	 * @return null if none found
	 */
	public X getState(String stateName)
	{
		for (X state : states)
		{
			if (state.name.equals(stateName)) { return state; }
		}
		return null;
	}

	/**
	 * Updates all the states in this tree for state name specified.
	 * 
	 * @param stateName
	 */
	@SuppressWarnings("rawtypes")
	public void updateAllStatesInTree(String stateName)
	{
		int count = 0;
		for (LttlStateManager manager : getAllStateManagersInTree())
		{
			StateBase state = manager.getState(stateName);
			if (state != null && manager.target != null)
			{
				count++;
				state.update(manager.target);
			}
		}
		Lttl.logNote("Updated " + count
				+ " states in "
				+ t().getHighestParent().getName()
				+ " tree.");
	}

	/**
	 * renames all the states in this tree from state name specified to the new names
	 * 
	 * @param stateName
	 */
	@SuppressWarnings("rawtypes")
	public void renameAllStatesInTree(String stateName, String newName)
	{
		int count = 0;

		if (getAllStateNamesTree().contains(newName))
		{
			Lttl.logNote("Can't rename all states to " + newName
					+ " because it already exists on "
					+ t().getHighestParent().getName()
					+ " tree's managers.");
			return;
		}

		for (LttlStateManager manager : getAllStateManagersInTree())
		{
			StateBase state = manager.getState(stateName);
			if (state != null)
			{
				count++;
				state.name = newName;
			}
		}
		Lttl.logNote("Renamed " + count
				+ " states in "
				+ t().getHighestParent().getName()
				+ " tree.");
	}

	/**
	 * Immedieately has all states in tree with state names go to their values (if active)
	 * 
	 * @param stateName
	 */
	@SuppressWarnings("rawtypes")
	public void goToAllStatesInTree(String stateName)
	{
		for (LttlStateManager manager : getAllStateManagersInTree())
		{
			StateBase state = manager.getState(stateName);
			if (state != null && manager.target != null)
			{
				state.goTo(manager.target);
			}
		}
	}

	/**
	 * Internal use.
	 */
	@GuiButton
	private void gotoAllStatesInTree()
	{
		ArrayList<String> names = getAllStateNamesTree();
		String stateName = GuiHelper.showComboboxModal("Go To State (All)", "", false,
				true, true, names.toArray(new String[names.size()]));
		if (stateName == null) return;
		goToTweenAllStatesInTree(stateName);
	}

	/**
	 * Internal use for GUI.
	 * 
	 * @param stateName
	 */
	@SuppressWarnings("rawtypes")
	void goToTweenAllStatesInTree(String stateName)
	{
		for (LttlStateManager manager : getAllStateManagersInTree())
		{
			StateBase state = manager.getState(stateName);
			if (state != null && manager.target != null)
			{
				state.goToTween(manager.target);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	public static ArrayList<LttlStateManager> getAllTargetStateManagers(
			ArrayList<LttlTransform> targetTrees)
	{
		ArrayList<LttlStateManager> list = new ArrayList<>();
		for (LttlTransform t : targetTrees)
		{
			if (t == null) continue;
			LttlHelper.AddUniqueToList(list, t.getHighestParent()
					.getComponentsInTree(LttlStateManager.class, true));
		}
		return list;
	}
}
