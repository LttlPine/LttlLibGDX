package com.lttlgames.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.lttlgames.components.LttlAnimationManager;
import com.lttlgames.components.interfaces.AnimationCallback;
import com.lttlgames.editor.annotations.GuiButton;
import com.lttlgames.editor.annotations.GuiDecimalPlaces;
import com.lttlgames.editor.annotations.GuiHide;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.helpers.LttlHelper;
import com.lttlgames.helpers.LttlMath;
import com.lttlgames.helpers.LttlMutatableBoolean;
import com.lttlgames.tweenengine.BaseTween;
import com.lttlgames.tweenengine.RepeatTweenType;
import com.lttlgames.tweenengine.Timeline;
import com.lttlgames.tweenengine.TweenCallback;
import com.lttlgames.tweenengine.TweenGetterSetter;

//12
@Persist(-9064)
public class AnimationObject
{
	@Persist(906401)
	public String name = "";
	@Persist(906402)
	@GuiMin(0)
	public float delay = 0;
	@Persist(906403)
	public RepeatTweenType repeatType = RepeatTweenType.None;
	@Persist(906404)
	@GuiMin(-1)
	public int repeatCount = 0;
	@Persist(906405)
	@GuiMin(0)
	public float repeatDelay = 0;
	@Persist(906406)
	@GuiHide
	ArrayList<AnimationSequence> stateSequences = new ArrayList<AnimationSequence>();
	@Persist(906407)
	@GuiHide
	ArrayList<AnimatedComponent> animComps = new ArrayList<AnimatedComponent>();
	/**
	 * The amount of time in the editor for playing timeline nodes.
	 */
	@Persist(906408)
	@GuiHide
	@GuiMin(0)
	@GuiDecimalPlaces(2)
	float editorDuration = 5;
	@Persist(906409)
	@GuiHide
	RepeatTweenType editorRepeatType = RepeatTweenType.None;
	@Persist(9064012)
	@GuiHide
	ArrayList<AnimationProperty> properties = new ArrayList<AnimationProperty>(
			0);
	/**
	 * components to callback in addition to the component being modified
	 */
	@Persist(9064011)
	public ArrayList<LttlComponent> callbackComponents;

	private Timeline playingTimeline;

	/**
	 * Disables editor callback nodes (stepCallbacks are always enabled) when playing from editor.
	 */
	@Persist(9064010)
	boolean editorCallbacks = false;

	@SuppressWarnings("rawtypes")
	/**
	 * Internal use only, gui
	 */
	@GuiButton(order = 1)
	private void edit(GuiFieldObject source)
	{
		new GuiAnimationEditor(this, source);
	}

	public ArrayList<String> getStateSequenceNames()
	{
		ArrayList<String> names = new ArrayList<String>();
		for (AnimationSequence seq : stateSequences)
		{
			if (!names.contains(seq.name))
			{
				names.add(seq.name);
			}
		}
		return names;
	}

	/**
	 * Generates and returns the tween for this animation using the provided state managers.
	 * 
	 * @param targetTrees
	 * @param owner
	 *            the tween owner
	 * @return
	 */
	public Timeline getAnimationTween(ArrayList<LttlTransform> targetTrees,
			LttlComponent owner)
	{
		return getAnimationTween(targetTrees, owner, true, true);
	}

	/**
	 * internal use
	 * 
	 * @param targetTrees
	 *            can be null if field sequences only
	 * @param owner
	 *            the component that has the animation manager on it
	 * @param addRepeatAndDelayOptions
	 * @param addCallbacks
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	Timeline getAnimationTween(ArrayList<LttlTransform> targetTrees,
			LttlComponent owner, boolean addRepeatAndDelayOptions,
			boolean addCallbacks)
	{
		Timeline mainParallel = owner.tweenParallel();

		// create an empty list to be populated with state manager animation callbacks at the end, if needed
		final ArrayList<AnimationCallback> cachedStateManagerAnimationCallbacks = new ArrayList<AnimationCallback>();
		LttlMutatableBoolean atLeastOneCallbackStateManager = new LttlMutatableBoolean(
				false);

		if (targetTrees != null && targetTrees.size() > 0)
		{
			// get all state managers in target trees
			final ArrayList<LttlStateManager> stateManagers = LttlStateManager
					.getAllTargetStateManagers(targetTrees);

			// parallel
			// create the state animation sequences
			for (final AnimationSequence seq : stateSequences)
			{
				// skip unactive sequences
				if (!seq.active) continue;
				seq.sortNodes();
				Timeline sequenceParallel = owner.tweenParallel();
				ArrayList<String> sequenceStateNames = seq.getAllStateNames();

				// sequential
				for (final LttlStateManager<?, ?> lsm : stateManagers)
				{
					// skip disabled state managers
					if (!lsm.isEnabled()) continue;

					// check if this state manager has at least one state in this sequence
					boolean hasAtleastOneState = false;
					for (String sn : lsm.getAllStateNames())
					{
						if (sequenceStateNames.contains(sn))
						{
							hasAtleastOneState = true;
							break;
						}
					}
					if (!hasAtleastOneState)
					{
						// if no state's in this sequence could be found in this state manager, skip it
						continue;
					}

					addSequenceTween(seq, cachedStateManagerAnimationCallbacks,
							sequenceParallel, atLeastOneCallbackStateManager,
							addCallbacks, lsm, null, null, null);

					// add it to the main parallel
					mainParallel.push(sequenceParallel);
				}
			}
		}

		// create each animated component's Timeline
		for (final AnimatedComponent animComp : animComps)
		{
			if (animComp.target == null) continue;

			// create timeline parallel for each animated component
			Timeline animatedComponentParallel = animComp.target
					.tweenParallel();

			// add all of it's tweens, recusively
			// use the component as the parent object, and no pft is required
			animComp.addAllTweens(this, animComp.target, animComp.target, null,
					cachedStateManagerAnimationCallbacks,
					animatedComponentParallel, atLeastOneCallbackStateManager,
					addCallbacks, animComp);

			// add step callback to the entire parallel
			if (animComp.stepCallback
					&& AnimationCallback.class.isAssignableFrom(animComp.target
							.getClass()))
			{
				animatedComponentParallel.addCallback(new TweenCallback()
				{
					@Override
					public void onStep(float iterationPosition)
					{
						((AnimationCallback) animComp.target).onStep(name,
								iterationPosition);
					}
				});
			}

			// add it to the main parallel
			mainParallel.push(animatedComponentParallel);
		}

		// get animation callbacks components if we had at least one callback, we only do this if it's
		// necessary, and it only needs to be generated once
		if (atLeastOneCallbackStateManager.value)
		{
			cachedStateManagerAnimationCallbacks
					.addAll(getAllAnimationCallbackComponents(targetTrees));
		}

		// add repeat options
		if (addRepeatAndDelayOptions)
		{
			if (repeatType != RepeatTweenType.None && repeatCount != 0)
			{
				if (repeatType == RepeatTweenType.Rewind)
				{
					mainParallel.repeat(repeatCount,
							LttlMath.max(0, repeatDelay));
				}
				else
				{
					mainParallel.repeatYoyo(repeatCount,
							LttlMath.max(0, repeatDelay));
				}
			}
			if (delay > 0)
			{
				mainParallel.setDelay(delay);
			}
		}

		// add step callbacks to the entire animation for the other callback components
		if (callbackComponents != null && callbackComponents.size() > 0)
		{
			mainParallel.addCallback(new TweenCallback()
			{
				@Override
				public void onStep(float iterationPosition)
				{
					for (LttlComponent ac : callbackComponents)
					{
						if (ac != null
								&& AnimationCallback.class.isAssignableFrom(ac
										.getClass()))
						{
							((AnimationCallback) ac).onStep(name,
									iterationPosition);
						}
					}
				}
			});
		}

		return mainParallel;
	}

	/**
	 * @param seq
	 * @param cachedStateManagerAnimationCallbacks
	 * @param sequenceParallel
	 *            the timeline it adds it to
	 * @param atLeastOneCallbackStateManager
	 * @param addCallbacks
	 * @param lsm
	 *            optional, only for state sequences, can be null
	 * @param fieldSeqTweenGetterSetter
	 *            optional, only for field sequences, can be null
	 * @param fieldSeqHostComp
	 *            optional, only for field sequence, can be null
	 * @param animComp
	 *            optional, only for field sequence, can be null
	 */
	void addSequenceTween(
			final AnimationSequence seq,
			final ArrayList<AnimationCallback> cachedStateManagerAnimationCallbacks,
			final Timeline sequenceParallel,
			final LttlMutatableBoolean atLeastOneCallbackStateManager,
			boolean addCallbacks, final LttlStateManager<?, ?> lsm,
			final TweenGetterSetter fieldSeqTweenGetterSetter,
			final LttlComponent fieldSeqHostComp,
			final AnimatedComponent animComp)
	{
		// stay null unless need them
		Timeline keyFrameSequence = null;
		Timeline callbacksParallel = null;
		TimelineNode prevKeyframeOrHold = null;
		TimelineNode prevPrevKeyframeOrHold = null;

		LttlComponent hostComponent = (lsm == null) ? fieldSeqHostComp : lsm;

		for (int i = 0; i < seq.nodes.size(); i++)
		{
			final TimelineNode node = seq.nodes.get(i);
			if (i > 0 && seq.nodes.get(i - 1).time == node.time)
			{
				Lttl.logNote("Animation Warning: nodes with same time at "
						+ node.time);
			}

			// skip
			if (!node.active) continue;

			BaseTween<?> tween = null;

			if (KeyframeNode.class.isAssignableFrom(node.getClass())
					|| node.getClass() == AnimationHoldNode.class)
			{
				if (KeyframeNode.class.isAssignableFrom(node.getClass()))
				{
					/* KEYFRAME */
					final KeyframeNode kf = (KeyframeNode) node;

					// calculate duration for keyframe by comparing time between this keyframe and last
					// keyframe or node, if none, then uses it's own time if it is a set, then duration is always 0
					float duration = 0;
					if (!kf.set)
					{
						if (prevKeyframeOrHold == null)
						{
							duration = node.time;
						}
						else
						{
							duration = node.time - prevKeyframeOrHold.time;
						}
					}

					if (node.getClass() == StateKeyframeNode.class)
					{
						final StateKeyframeNode skf = (StateKeyframeNode) node;
						@SuppressWarnings("rawtypes")
						StateBase state = lsm.getState(skf.stateName);
						// skip if state is null or empty
						if (state == null) continue;
						tween = state.getTweenParallel(lsm.target, duration,
								kf.easeType);
						if (skf.targetPercentage != 1)
						{
							tween.setTargetPercentage(skf.targetPercentage);
						}
					}
					else if (node.getClass() == FieldKeyframeNode.class)
					{
						final FieldKeyframeNode fkf = (FieldKeyframeNode) node;

						tween = AnimatedObject.getSequence(this, animComp,
								fieldSeqHostComp, fkf,
								fieldSeqTweenGetterSetter, duration);
					}
					else
					{
						Lttl.Throw();
					}

					// if keyframe is a set, then create delay time between this keyframe and last (or hold)
					// if it exists, if not then use own time as delay
					if (kf.set)
					{
						float delayTime = kf.time;
						if (prevKeyframeOrHold != null)
						{
							delayTime = kf.time - prevKeyframeOrHold.time;
						}
						if (delayTime > 0)
						{
							tween.setDelay(delayTime);
						}
					}
					if (prevKeyframeOrHold != null
							&& prevKeyframeOrHold.getClass() == AnimationHoldNode.class)
					{
						// if the previous node was a hold, then add a delay for the time between the hold
						// and the previous node (if none, then just use time of delay)
						float delayTime = (prevPrevKeyframeOrHold == null) ? prevKeyframeOrHold.time
								: prevKeyframeOrHold.time
										- prevPrevKeyframeOrHold.time;
						if (delayTime > 0)
						{
							tween.setDelay(delayTime);
						}
					}
					if (addCallbacks && kf.callback)
					{
						/* ANIMATION CALLBACK */
						tween.addCallback(new TweenCallback()
						{
							@Override
							public void onEnd()
							{
								processOnCallback(
										atLeastOneCallbackStateManager,
										animComp, seq, kf.callbackValue,
										cachedStateManagerAnimationCallbacks);
							}
						});
					}

					// create sequence to put all keyframes in
					if (keyFrameSequence == null)
					{
						keyFrameSequence = hostComponent.tweenSequence();
					}
					keyFrameSequence.push(tween);
				}
				else if (node.getClass() == AnimationHoldNode.class)
				{
					/* Hold */
					final AnimationHoldNode hold = (AnimationHoldNode) node;
					if (addCallbacks && hold.callback)
					{
						/* ANIMATION CALLBACK */
						tween = Lttl.tween.mark(hostComponent).addCallback(
								new TweenCallback()
								{
									@Override
									public void onStart()
									{
										processOnCallback(
												atLeastOneCallbackStateManager,
												animComp,
												seq,
												((AnimationHoldNode) node).callbackValue,
												cachedStateManagerAnimationCallbacks);
									}
								});
						if (node.time > 0)
						{
							tween.setDelay(node.time);
						}

						// create parallel sequence to put all callbacks in
						if (callbacksParallel == null)
						{
							callbacksParallel = hostComponent.tweenParallel();
						}
						callbacksParallel.push(tween);
					}
				}

				// save previous nodes
				prevPrevKeyframeOrHold = prevKeyframeOrHold;
				prevKeyframeOrHold = node;
			}
			else if (addCallbacks
					&& node.getClass() == AnimationCallbackNode.class)
			{
				/* ANIMATION CALLBACK */
				tween = Lttl.tween.mark(hostComponent).addCallback(
						new TweenCallback()
						{
							@Override
							public void onStart()
							{
								processOnCallback(
										atLeastOneCallbackStateManager,
										animComp,
										seq,
										((AnimationCallbackNode) node).callbackValue,
										cachedStateManagerAnimationCallbacks);
							}
						});
				if (node.time > 0)
				{
					tween.setDelay(node.time);
				}

				// create parallel sequence to put all callbacks in
				if (callbacksParallel == null)
				{
					callbacksParallel = hostComponent.tweenParallel();
				}
				callbacksParallel.push(tween);
			}
		}

		// add sequences to the main sequence parallel
		if (keyFrameSequence != null)
		{
			sequenceParallel.push(keyFrameSequence);
		}
		if (callbacksParallel != null)
		{
			sequenceParallel.push(callbacksParallel);
		}
	}

	private void processOnCallback(
			LttlMutatableBoolean atLeastOneCallbackStateManager,
			AnimatedComponent animComp, AnimationSequence seq,
			String callbackValue,
			ArrayList<AnimationCallback> cachedStateManagerAnimationCallbacks)
	{
		// if animated component, then do callback on that
		if (animComp != null
				&& AnimationCallback.class.isAssignableFrom(animComp.target
						.getClass()))
		{
			((AnimationCallback) animComp).onCallback(
					AnimationObject.this.name, seq.name, callbackValue);
		}
		// else it must be a state sequence, so callback all the state manager animation
		// callbacks
		else
		{
			atLeastOneCallbackStateManager.value = true;
			for (AnimationCallback ac : cachedStateManagerAnimationCallbacks)
			{
				ac.onCallback(AnimationObject.this.name, seq.name,
						callbackValue);
			}
		}

		// callback the callbackcomponents (regardless if state or object animation)
		if (callbackComponents != null && callbackComponents.size() > 0)
		{
			for (LttlComponent ac : callbackComponents)
			{
				if (ac != null
						&& AnimationCallback.class.isAssignableFrom(ac
								.getClass()))
				{
					((AnimationCallback) ac).onCallback(
							AnimationObject.this.name, seq.name, callbackValue);
				}
			}
		}
	}

	public Timeline getAnimationTween(LttlAnimationManager manager)
	{
		return getAnimationTween(manager.stateTargetTrees, manager);
	}

	/**
	 * Retrieves all sequences (state and recusive animated components)
	 * 
	 * @return
	 */
	private ArrayList<AnimationSequence> getAllSequences()
	{
		ArrayList<AnimationSequence> all = new ArrayList<AnimationSequence>();
		all.addAll(stateSequences);
		for (AnimatedComponent animComp : animComps)
		{
			animComp.getAllSequences(all);
		}
		return all;
	}

	public float getLatestNodeTime()
	{
		float greatestTime = 0;
		for (AnimationSequence seq : getAllSequences())
		{
			float time = seq.getLatestNodeTime();
			if (time > greatestTime)
			{
				greatestTime = time;
			}
		}

		return greatestTime;
	}

	public float getLatestKeyframeCallbackTime()
	{
		float latestTime = 0;
		for (AnimationSequence seq : stateSequences)
		{
			float time = seq.getLatestKeyframeCallbackTime();
			if (time > latestTime)
			{
				latestTime = time;
			}
		}

		return latestTime;
	}

	/**
	 * internal use only
	 * 
	 * @param gfo
	 */
	@SuppressWarnings("rawtypes")
	@GuiButton(order = 2)
	private void play(GuiFieldObject gfo)
	{
		LttlAnimationManager manager = (LttlAnimationManager) gfo.getParent()
				.getParent().objectRef;
		Timeline timeline = getAnimationTween(manager.stateTargetTrees,
				manager, true, editorCallbacks);
		timeline.addCallback(new TweenCallback()
		{
			@Override
			public void onComplete()
			{
				playingTimeline = null;
			}
		});
		playingTimeline = timeline.start();
	}

	@GuiButton(order = 3)
	private void stop()
	{
		if (playingTimeline != null)
		{
			playingTimeline.kill();
		}
	}

	/**
	 * internal use only
	 * 
	 * @param gfo
	 */
	@SuppressWarnings("rawtypes")
	@GuiButton(order = 20)
	private void duplicate(GuiFieldObject gfo)
	{
		LttlAnimationManager manager = (LttlAnimationManager) gfo.getParent()
				.getParent().objectRef;
		AnimationObject newAO = LttlCopier.copy(this);
		newAO.name = this.name + "_copy";
		manager.animations.add(newAO);
	}

	public ArrayList<AnimationCallback> getAllAnimationCallbackComponents(
			ArrayList<LttlTransform> targetTrees)
	{
		ArrayList<AnimationCallback> list = new ArrayList<AnimationCallback>();
		for (LttlTransform lt : targetTrees)
		{
			LttlHelper.AddUniqueToList(list, lt.getHighestParent()
					.getComponentsInTree(AnimationCallback.class, true));
		}
		return list;
	}

	/**
	 * Sets the properties value to be used in the animation (does not override the orignal value which can be
	 * referenced at {@link AnimationProperty#getOriginalValue()}
	 * 
	 * @param name
	 * @param value
	 */
	public void setProperty(String name, float value)
	{
		AnimationProperty prop = getProperty(name);
		if (prop != null)
		{
			prop.setValueAnim(value);
		}
	}

	/**
	 * @param name
	 * @return null if none found
	 */
	public AnimationProperty getProperty(String name)
	{
		for (AnimationProperty prop : properties)
		{
			if (prop.name.equals(name)) { return prop; }
		}
		return null;
	}

	/**
	 * Get an unmodifiable list of properties.
	 * 
	 * @return
	 */
	public List<AnimationProperty> getProperties()
	{
		return Collections.unmodifiableList(properties);
	}
}
