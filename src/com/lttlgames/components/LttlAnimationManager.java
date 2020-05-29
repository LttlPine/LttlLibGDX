package com.lttlgames.components;

import java.util.ArrayList;

import com.lttlgames.editor.AnimationObject;
import com.lttlgames.editor.GuiHelper;
import com.lttlgames.editor.Lttl;
import com.lttlgames.editor.LttlComponent;
import com.lttlgames.editor.LttlStateManager;
import com.lttlgames.editor.LttlTransform;
import com.lttlgames.editor.annotations.GuiButton;
import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.GuiHideArrayListControls;
import com.lttlgames.editor.annotations.GuiListItemNameField;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.tweenengine.Timeline;

@Persist(-9063)
public class LttlAnimationManager extends LttlComponent
{
	@Persist(906301)
	@GuiCallback("purgeDescendants")
	public ArrayList<LttlTransform> stateTargetTrees = new ArrayList<LttlTransform>();

	@GuiHideArrayListControls(canAdd = false)
	@GuiListItemNameField("name")
	@Persist(906302)
	public ArrayList<AnimationObject> animations = new ArrayList<AnimationObject>();

	@Override
	public void onEditorCreate()
	{
		stateTargetTrees.add(t());
	}

	/**
	 * Returns the animation with tha name
	 * 
	 * @param name
	 * @return null if none found
	 */
	public AnimationObject getAnimation(String name)
	{
		for (AnimationObject anim : animations)
		{
			if (anim.name.equals(name)) { return anim; }
		}
		return null;
	}

	/**
	 * Returns all the animation names
	 * 
	 * @return
	 */
	public ArrayList<String> getAnimationNames()
	{
		ArrayList<String> names = new ArrayList<String>();
		for (AnimationObject anim : animations)
		{
			if (!names.contains(anim.name))
			{
				names.add(anim.name);
			}
		}
		return names;
	}

	/**
	 * internal use only, gui
	 */
	@GuiButton
	private void createAnimation()
	{
		String animName = GuiHelper.showTextFieldModal("Create Animation", "");
		if (animName == null) { return; }
		if (getAnimationNames().contains(animName))
		{
			Lttl.logNote("No animation created because an animation already exists with that name on this Animation Manager.");
			return;
		}

		AnimationObject animObject = new AnimationObject();
		animObject.name = animName;
		animations.add(animObject);
	}

	/**
	 * Returns a list of all the state names for the targeted trees.
	 * 
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public ArrayList<String> getAllStateNames()
	{
		purgeDescendants();

		ArrayList<String> names = new ArrayList<String>();
		for (LttlTransform lt : stateTargetTrees)
		{
			for (LttlStateManager lsm : lt.getHighestParent()
					.getComponentsInTree(LttlStateManager.class, true))
			{
				lsm.getStateNames(names);
			}
		}

		return names;
	}

	private void purgeDescendants()
	{
		for (LttlTransform lt : stateTargetTrees)
		{
			if (lt == null) continue;
			for (LttlTransform lt2 : stateTargetTrees)
			{
				if (lt2 == lt) continue;
				if (lt.isDescendent(lt2))
				{
					Lttl.logNote("Removed " + lt.getName()
							+ " from targetTrees because it is a descendant.");
					stateTargetTrees.remove(lt);
					purgeDescendants();
					return;
				}
			}
		}
	}

	/**
	 * Returns the animation specified as a timeline tween. It still needs to be started.
	 * 
	 * @param animName
	 * @return null if no animation found
	 */
	public Timeline getAnimationTween(String animName)
	{
		AnimationObject anim = getAnimation(animName);
		if (anim != null) { return anim.getAnimationTween(this); }
		return null;
	}
}
