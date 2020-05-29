package com.lttlgames.editor;

import java.util.HashMap;

import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.tweenengine.TweenGetterSetter;

//4
@Persist(-9053)
public class LttlTransformState extends StateBase<LttlTransform>
{
	/* Properties */
	@Persist(905300)
	public StatePropertyFloat positionX = new StatePropertyFloat(true);
	@Persist(905301)
	public StatePropertyFloat positionY = new StatePropertyFloat(true);
	@Persist(905302)
	public StatePropertyFloat scaleX = new StatePropertyFloat(true);
	@Persist(905303)
	public StatePropertyFloat scaleY = new StatePropertyFloat(true);
	@Persist(905304)
	public StatePropertyFloat rotation = new StatePropertyFloat(true);
	@Persist(905305)
	public StatePropertyFloat zPos = new StatePropertyFloat(false);

	@Override
	protected HashMap<StateProperty<?>, TweenGetterSetter> generatePropetyMap(
			final LttlTransform component,
			HashMap<StateProperty<?>, TweenGetterSetter> map)
	{
		// Position
		map.put(positionX, TweenGetterSetter.getVector2(component.position, 0));
		map.put(positionY, TweenGetterSetter.getVector2(component.position, 1));

		// Scale
		map.put(scaleX, TweenGetterSetter.getVector2(component.scale, 0));
		map.put(scaleY, TweenGetterSetter.getVector2(component.scale, 1));

		// Rotation
		map.put(rotation, component.getTweenGetterSetter(3));

		// zPos
		map.put(zPos, component.getTweenGetterSetter(1));

		return map;
	}

	@Override
	protected void beforeUpdate(LttlTransform comp)
	{
	}
}
