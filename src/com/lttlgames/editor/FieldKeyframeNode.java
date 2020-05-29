package com.lttlgames.editor;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;

import com.lttlgames.editor.annotations.GuiCanNull;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.tweenengine.TweenGetterSetter;

@Persist(-9090)
class FieldKeyframeNode extends KeyframeNode
{
	@Persist(909001)
	float value;

	@Persist(909003)
	@GuiCanNull
	String property;

	@Persist(909002)
	@GuiCanNull
	KeyframeOptions options = null;

	FieldKeyframeNode()
	{
	}

	@SuppressWarnings("rawtypes")
	void updateValue()
	{
		Object parentObject = sequenceGui.parentGui.getObject();
		if (parentObject != null)
		{
			if (parentObject.getClass() == ArrayList.class)
			{
				int index = sequenceGui.getAnimId();
				if (index >= ((ArrayList) parentObject).size())
				{
					Lttl.logNote("Animation Editor: Unable to get current value for item at index : "
							+ index + " becuase index is out of range.");
					return;
				}
				value = ((Number) ((ArrayList) parentObject).get(index))
						.floatValue();
			}
			else if (parentObject.getClass().isArray())
			{
				int index = sequenceGui.getAnimId();
				if (index >= Array.getLength(parentObject))
				{
					Lttl.logNote("Animation Editor: Unable to get current value for item at index : "
							+ index + " becuase index is out of range.");
					return;
				}
				value = Array.getFloat(parentObject, index);
			}
			else
			{
				try
				{
					// check if field is private/protected, if it is, make it accessible
					boolean isPrivate = false;
					Field f = sequenceGui.pft.getField();
					if (LttlObjectGraphCrawler.isPrivateOrProtectedOrDefault(f))
					{
						isPrivate = true;
						f.setAccessible(true);
					}

					try
					{
						value = sequenceGui.pft.getField().getFloat(
								parentObject);
					}
					catch (IllegalArgumentException e)
					{
						e.printStackTrace();
					}
					catch (IllegalAccessException e)
					{
						e.printStackTrace();
					}

					// set private/protected field's access back to what it was
					if (isPrivate)
					{
						f.setAccessible(false);
					}
				}
				catch (IllegalArgumentException e1)
				{
					e1.printStackTrace();
				}
			}
		}
		else
		{
			Lttl.logNote("Animation Editor: Unable to get current value for field: "
					+ sequenceGui.pft.getField().getName()
					+ " because source object is null.");
		}
	}

	/**
	 * get value, check if it's mapped
	 * 
	 * @param anim
	 * @param animComp
	 * @param fieldSeqTweenGetterSetter
	 * @return
	 */
	float getValue(AnimationObject anim, AnimatedComponent animComp,
			TweenGetterSetter fieldSeqTweenGetterSetter)
	{
		float returnValue = value;

		// check if it has a property mapped to it
		if (property != null && !property.isEmpty())
		{
			AnimationProperty prop = anim.getProperty(property);
			if (property != null)
			{
				// multiply node value by the property value
				// check if the actual has been set, if not, then use the original
				returnValue *= prop.valueAnim != Float.POSITIVE_INFINITY ? prop.valueAnim
						: prop.value;
			}
			else
			{
				Lttl.logNote("Animation Property: No property found for "
						+ property + " on animation " + anim.name
						+ " on animated component "
						+ animComp.target.toString());
			}
		}

		// if this animated component is set to be relative, than offset the keyframe value based on the
		// current value. It's assumed that the animation is starting at some state where a relative animation makes
		// sense.
		// this is necessary so you don't have to set each node to be relatvie
		if (!animComp.relativeState.isEmpty())
		{
			returnValue += fieldSeqTweenGetterSetter.get()[0];
		}

		return returnValue;
	}
}
