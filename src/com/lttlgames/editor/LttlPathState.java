package com.lttlgames.editor;

import java.util.ArrayList;
import java.util.HashMap;

import com.lttlgames.editor.annotations.GuiHideArrayListControls;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.helpers.LttlMath;
import com.lttlgames.tweenengine.TweenGetterSetter;

@Persist(-9080)
public class LttlPathState extends StateBase<LttlPath>
{
	/* Properties */
	@Persist(908000)
	@GuiHideArrayListControls(canAdd = false, canDelete = false, canMove = false, canClear = false)
	public ArrayList<LttlPathControlPointStatePropertyGroup> points = new ArrayList<LttlPathControlPointStatePropertyGroup>();

	// default step callback, this way the path can update accurately
	{
		this.stepCallback = true;
	}

	@Override
	protected HashMap<StateProperty<?>, TweenGetterSetter> generatePropetyMap(
			final LttlPath component,
			HashMap<StateProperty<?>, TweenGetterSetter> map)
	{
		// define the minimum number of points to tween, that way no indexOutOfBounds
		int size = LttlMath.min(points.size(), component.controlPoints.size());
		for (int i = 0; i < size; i++)
		{
			LttlPathControlPointStatePropertyGroup g = points.get(i);

			// skip if control point is not active to animate
			if (!g.active) continue;

			LttlPathControlPoint cp = component.controlPoints.get(i);

			// position
			map.put(g.posX, TweenGetterSetter.getVector2(cp.pos, 0));
			map.put(g.posY, TweenGetterSetter.getVector2(cp.pos, 1));

			// only animate or populate values for the handles if is handle type, pretty much overrides the state
			// property's active variable
			if (cp.type == LttlPathControlPointType.Handles)
			{
				// left handle
				map.put(g.leftPosX, TweenGetterSetter.getVector2(cp.leftPos, 0));
				map.put(g.leftPosY, TweenGetterSetter.getVector2(cp.leftPos, 1));
				// right handle
				map.put(g.rightPosX,
						TweenGetterSetter.getVector2(cp.rightPos, 0));
				map.put(g.rightPosY,
						TweenGetterSetter.getVector2(cp.rightPos, 1));
			}
		}
		return map;
	}

	@Override
	protected void beforeUpdate(LttlPath comp)
	{
		points.clear();
		for (int i = 0; i < comp.controlPoints.size(); i++)
		{
			points.add(new LttlPathControlPointStatePropertyGroup());
		}
	}
}
