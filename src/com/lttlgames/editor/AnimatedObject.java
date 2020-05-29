package com.lttlgames.editor;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntArray;
import com.lttlgames.components.interfaces.AnimationCallback;
import com.lttlgames.editor.LttlObjectGraphCrawler.FieldsMode;
import com.lttlgames.editor.annotations.AnimateField;
import com.lttlgames.editor.annotations.GuiHide;
import com.lttlgames.editor.annotations.IgnoreCrawl;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.helpers.LttlMath;
import com.lttlgames.helpers.LttlMutatableBoolean;
import com.lttlgames.tweenengine.RepeatTweenType;
import com.lttlgames.tweenengine.Timeline;
import com.lttlgames.tweenengine.Tween;
import com.lttlgames.tweenengine.TweenGetterSetter;

@Persist(-9088)
@GuiHide
public class AnimatedObject
{

	/**
	 * holds the animation sequence as the value, with the animation field id as the key. These are only fields that are
	 * floats that are directly on the object.
	 */
	@Persist(908800)
	public HashMap<Integer, AnimationSequence> sequences = new HashMap<Integer, AnimationSequence>();

	/**
	 * These are fields that are objects that are animated too, the key being the animatedField ID
	 */
	@Persist(908801)
	public HashMap<Integer, AnimatedObject> children = new HashMap<Integer, AnimatedObject>();
	@IgnoreCrawl
	private HashMap<Integer, ProcessedFieldType> fieldsMapCache;

	/**
	 * Generates the animation fields map (but only once)
	 * 
	 * @param clazz
	 * @param pftParams
	 * @return
	 */
	HashMap<Integer, ProcessedFieldType> getFieldsMap(Class<?> clazz,
			ProcessedFieldType... pftParams)
	{
		if (fieldsMapCache == null)
		{
			fieldsMapCache = new HashMap<Integer, ProcessedFieldType>(0);
			for (ProcessedFieldType pft : LttlObjectGraphCrawler.getAllFields(
					clazz, FieldsMode.AllButIgnore, pftParams))
			{
				// special cases
				if (clazz == Vector2.class)
				{
					if (pft.getField().getName().equals("x"))
					{
						fieldsMapCache.put(0, pft);
					}
					else if (pft.getField().getName().equals("y"))
					{
						fieldsMapCache.put(1, pft);
					}
				}
				else if (clazz == Color.class)
				{
					if (pft.getField().getName().equals("r"))
					{
						fieldsMapCache.put(0, pft);
					}
					else if (pft.getField().getName().equals("g"))
					{
						fieldsMapCache.put(1, pft);
					}
					else if (pft.getField().getName().equals("b"))
					{
						fieldsMapCache.put(2, pft);
					}
					else if (pft.getField().getName().equals("a"))
					{
						fieldsMapCache.put(3, pft);
					}
				}
				else if (clazz == IntArray.class || clazz == FloatArray.class)
				{
					if (pft.getField().getName().equals("items"))
					{
						fieldsMapCache.put(0, pft);
					}
				}
				else
				{
					AnimateField ann = pft.getField().getAnnotation(
							AnimateField.class);
					if (ann == null) continue;
					// duplicate Animation Field ID
					Lttl.Throw(fieldsMapCache.containsKey(ann.value()));
					fieldsMapCache.put(ann.value(), pft);
				}
			}
		}
		return fieldsMapCache;
	}

	/**
	 * does not add any kind of delay
	 * 
	 * @param anim
	 *            TODO
	 * @param animComp
	 *            TODO
	 * @param hostComponent
	 * @param fkf
	 * @param getterSetter
	 * @param duration
	 * @return
	 */
	static Tween getSequence(AnimationObject anim, AnimatedComponent animComp,
			final LttlComponent hostComponent, final FieldKeyframeNode fkf,
			final TweenGetterSetter getterSetter, float duration)
	{
		// repeat
		float realRepeatDelay = 0;
		if (fkf.options != null && fkf.options.repeatCount > 0
				&& fkf.options.repeatType != RepeatTweenType.None)
		{
			// clamp to 0+
			realRepeatDelay = LttlMath.max(0, fkf.options.repeatDelay);
			// clamp repeat delay to the biggest repeat delay possible based on duration and repeat count
			realRepeatDelay = LttlMath.min(realRepeatDelay, duration
					/ fkf.options.repeatCount);
			// calculate real duration based on what time is left after the realRepeatDelay
			duration = (duration - (realRepeatDelay * fkf.options.repeatCount))
					/ (fkf.options.repeatCount + 1);
			// if specified duration, then clamp it to realDuration
			if (duration >= 0)
			{
				duration = LttlMath.min(duration, duration);
			}
		}

		// create tween
		Tween tween = hostComponent.tweenTo(duration, getterSetter,
				fkf.getValue(anim, animComp, getterSetter)).setEase(
				fkf.easeType);

		// options
		if (fkf.options != null)
		{
			if (fkf.options.repeatCount > 0
					&& fkf.options.repeatType != RepeatTweenType.None)
			{
				if (fkf.options.repeatType == RepeatTweenType.Rewind)
				{
					tween.repeat(fkf.options.repeatCount, realRepeatDelay);
				}
				else if (fkf.options.repeatType == RepeatTweenType.YoYo)
				{
					tween.repeatYoyo(fkf.options.repeatCount, realRepeatDelay);
				}
			}
			if (fkf.options.addNoise)
			{
				tween.addNoise(fkf.options.noiseOptions,
						fkf.options.generateNewNoiseEachIteration);
			}
			if (fkf.options.isRelative)
			{
				tween.setRelative();
			}
			if (fkf.options.addShake)
			{
				tween.addShake(fkf.options.shakeRangeBottom,
						fkf.options.shakeRangeTop, fkf.options.shakeUpdateRate,
						fkf.options.shakeType);
			}
		}

		return tween;
	}

	/**
	 * Recusively gets all the animated field sequences on itself and all of it's children and adds them to the
	 * sequenceParallel
	 * 
	 * @param animationObject
	 * @param hostComponent
	 * @param parentObject
	 * @param pft
	 * @param cachedStateManagerAnimationCallbacks
	 * @param sequenceParallel
	 * @param atLeastOneCallbackStateManager
	 * @param addCallbacks
	 * @param animComp
	 */
	@SuppressWarnings("rawtypes")
	void addAllTweens(AnimationObject animationObject,
			LttlComponent hostComponent, Object parentObject,
			ProcessedFieldType pft,
			ArrayList<AnimationCallback> cachedStateManagerAnimationCallbacks,
			Timeline sequenceParallel,
			LttlMutatableBoolean atLeastOneCallbackStateManager,
			boolean addCallbacks, AnimatedComponent animComp)
	{
		if (parentObject == null)
		{
			// skips null objects, very rare
			Lttl.logNote("Unable to animate object because null ["
					+ pft.getField().getName() + "]");
			return;
		}

		// add any of this animated objects fields/sequences
		for (Entry<Integer, AnimationSequence> e : sequences.entrySet())
		{
			int animID = e.getKey();
			AnimationSequence seq = e.getValue();

			if (!seq.active) continue;

			seq.sortNodes();
			TweenGetterSetter fieldSeqTweenGetterSetter = null;

			// special cases where there is no LttlAnimated implementation because they are GDX or Java classes
			if (parentObject.getClass() == Vector2.class)
			{
				fieldSeqTweenGetterSetter = TweenGetterSetter.getVector2(
						(Vector2) parentObject, animID);
			}
			else if (parentObject.getClass() == Color.class)
			{
				fieldSeqTweenGetterSetter = TweenGetterSetter.getColor(
						(Color) parentObject, animID);
			}
			else if (parentObject.getClass() == ArrayList.class)
			{
				// skip all that are out of range
				if (animID >= ((ArrayList) parentObject).size())
				{
					Lttl.logNote("Animation: Skipping index " + animID
							+ " because out of range.");
					continue;
				}

				if (LttlObjectGraphCrawler.isIntegerLikePrimative(pft.getParam(
						0).getCurrentClass()))
				{
					fieldSeqTweenGetterSetter = TweenGetterSetter
							.getArrayListInteger((ArrayList) parentObject,
									animID);
				}
				else if (LttlObjectGraphCrawler.isFloatLikePrimative(pft
						.getParam(0).getCurrentClass()))
				{
					fieldSeqTweenGetterSetter = TweenGetterSetter
							.getArrayListFloat((ArrayList) parentObject, animID);
				}
				else
				{
					// unknown primative type in arraylist
					Lttl.Throw();
				}
			}
			else if (parentObject.getClass().isArray())
			{
				// skip all that are out of range
				if (animID >= Array.getLength(parentObject))
				{
					Lttl.logNote("Animation: Skipping index " + animID
							+ " because out of range.");
					continue;
				}

				if (LttlObjectGraphCrawler.isIntegerLikePrimative(pft
						.getCurrentClass().getComponentType()))
				{
					fieldSeqTweenGetterSetter = TweenGetterSetter
							.getArrayInteger(parentObject, animID);
				}
				else if (LttlObjectGraphCrawler.isFloatLikePrimative(pft
						.getCurrentClass().getComponentType()))
				{
					fieldSeqTweenGetterSetter = TweenGetterSetter
							.getArrayFloat(parentObject, animID);
				}
				else
				{
					// unknown primative type in array
					Lttl.Throw();
				}
			}
			else
			{
				// The object that has the animated fields (these sequences) does not implement LttlAnimated
				Lttl.Throw(!LttlAnimated.class.isAssignableFrom(parentObject
						.getClass()));
				LttlAnimated lttlAnimated = (LttlAnimated) parentObject;
				fieldSeqTweenGetterSetter = lttlAnimated
						.getTweenGetterSetter(animID);

				// If null, it's probably because the AnimID was given incorrectly or the TweenGetterSetter was not
				// created, or it is not properly calling super class's getTweenGetterSetter()
				Lttl.Throw(fieldSeqTweenGetterSetter == null,
						"Animation Field: " + animID
								+ " could not be found on "
								+ parentObject.getClass().getSimpleName());
			}
			Lttl.Throw(fieldSeqTweenGetterSetter);

			// add sequence
			animationObject.addSequenceTween(seq,
					cachedStateManagerAnimationCallbacks, sequenceParallel,
					atLeastOneCallbackStateManager, addCallbacks, null,
					fieldSeqTweenGetterSetter, hostComponent, animComp);
		}

		// recursive through children
		for (Entry<Integer, AnimatedObject> e : children.entrySet())
		{
			int animID = e.getKey();
			AnimatedObject childAnimObject = e.getValue();
			ProcessedFieldType childPft = null;
			if (parentObject.getClass() == ArrayList.class)
			{
				// skip all that are out of range
				if (animID >= ((ArrayList) parentObject).size())
				{
					Lttl.logNote("Animation: Skipping index " + animID
							+ " because out of range.");
					continue;
				}

				childPft = pft.getParam(0);
			}
			else if (parentObject.getClass().isArray())
			{
				// skip all that are out of range
				if (animID >= Array.getLength(parentObject))
				{
					Lttl.logNote("Animation: Skipping index " + animID
							+ " because out of range.");
					continue;
				}

				childPft = new ProcessedFieldType(pft.getCurrentClass()
						.getComponentType());
			}
			else
			{
				childPft = getFieldsMap(
						parentObject.getClass(),
						pft == null ? (ProcessedFieldType[]) null : pft
								.getParams()).get(animID);
			}

			// skup this animated object since it is not an actual field on this class, probably deleted the field after
			// making the animation the animation editor cleans this up when it loads
			if (childPft == null) continue;

			Object childObject = null;
			// OPTIMIZE could cache the childObject and store on the animatedObject so if it exists, don't need to use
			// reflection again, but this allows it to change, between aniamtions, which is pretty much unnecessary
			if (parentObject.getClass() == ArrayList.class)
			{
				if (animID >= ((ArrayList) parentObject).size()) { return; }
				childObject = ((ArrayList) parentObject).get(animID);
			}
			else if (parentObject.getClass().isArray())
			{
				if (animID >= Array.getLength(parentObject)) { return; }
				childObject = Array.get(parentObject, animID);
			}
			else
			{
				try
				{
					childObject = childPft.getField().get(parentObject);
				}
				catch (IllegalArgumentException | IllegalAccessException e1)
				{
					Lttl.Throw();
				}
			}
			childAnimObject.addAllTweens(animationObject, hostComponent,
					childObject, childPft,
					cachedStateManagerAnimationCallbacks, sequenceParallel,
					atLeastOneCallbackStateManager, addCallbacks, animComp);
		}
	}

	ArrayList<AnimationSequence> getAllSequences(
			ArrayList<AnimationSequence> container)
	{
		container.addAll(sequences.values());
		for (AnimatedObject child : children.values())
		{
			child.getAllSequences(container);
		}
		return container;
	}
}
