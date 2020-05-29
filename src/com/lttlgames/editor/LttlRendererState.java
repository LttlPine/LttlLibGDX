package com.lttlgames.editor;

import java.util.HashMap;

import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.tweenengine.TweenGetterSetter;

//6
@Persist(-9061)
public class LttlRendererState extends StateBase<LttlRenderer>
{
	/* Properties */
	@Persist(906101)
	public StatePropertyFloat alpha = new StatePropertyFloat(true);
	@Persist(906102)
	public StatePropertyColor color0 = new StatePropertyColor(true);
	@Persist(906103)
	public StatePropertyColor color1 = new StatePropertyColor(true);
	@Persist(906104)
	public StatePropertyVector2 uvOffset = new StatePropertyVector2(false);
	@Persist(906105)
	public StatePropertyVector2 uvScale = new StatePropertyVector2(false);
	@Persist(906106)
	public StatePropertyFloat texMultipliedAlpha = new StatePropertyFloat(false);

	@Override
	protected HashMap<StateProperty<?>, TweenGetterSetter> generatePropetyMap(
			final LttlRenderer component,
			HashMap<StateProperty<?>, TweenGetterSetter> map)
	{
		// alpha
		map.put(alpha, new TweenGetterSetter()
		{

			@Override
			public void set(float[] values)
			{
				component.alpha = values[0];
			}

			@Override
			public float[] get()
			{
				return new float[]
				{ component.alpha };
			}
		});

		// color0
		map.put(color0, TweenGetterSetter.getColor(component.color));

		// uvOffset
		map.put(uvOffset,
				TweenGetterSetter.getVector2(component.uvOffsetShader));

		// uvScale
		map.put(uvScale, TweenGetterSetter.getVector2(component.uvScaleShader));

		return map;
	}

	@Override
	protected void beforeUpdate(LttlRenderer comp)
	{
	}
}
