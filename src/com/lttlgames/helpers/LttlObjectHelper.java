package com.lttlgames.helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;
import com.lttlgames.editor.LttlTransform;

/**
 * Helper functions that are pertained to game objects
 */
public class LttlObjectHelper
{
	/**
	 * Calculates the center point between the positions of several transforms
	 * 
	 * @param transforms
	 * @param output
	 *            sets the center value in, if null, creates a new vector2 object
	 * @return
	 */
	public static Vector2 GetCenterOfTransforms(
			ArrayList<LttlTransform> transforms, Vector2 output)
	{
		if (output == null) output = new Vector2();

		if (transforms.size() == 1) { return output
				.set(transforms.get(0).position); }

		FloatArray points = new FloatArray(transforms.size() * 2);

		// iterate through transforms getting relevant points
		for (LttlTransform t : transforms)
		{
			// add origin/center position
			points.add(t.position.x);
			points.add(t.position.y);
		}

		// calculate center
		LttlMath.GetAABB(points.toArray(), LttlMath.CheckoutTmpRectInternal0())
				.getCenter(output);
		LttlMath.ReturnTmpRectInternal0();
		return output;
	}

	/**
	 * Removes any descendants of othe transforms in this list. All transforms left will have no descendants in the
	 * list.
	 * 
	 * @param list
	 * @return number removed
	 */
	public static int RemoveDescendants(ArrayList<LttlTransform> list)
	{
		int removed = 0;
		for (Iterator<LttlTransform> it = list.iterator(); it.hasNext();)
		{
			LttlTransform current = it.next();
			for (LttlTransform lt : list)
			{
				// skip self
				if (lt == current) continue;
				if (current.isDescendent(lt))
				{
					it.remove();
					removed++;
					break;
				}
			}
		}
		return removed;
	}

	/**
	 * Copies the transforms from copyFrom and pastes them as children on pasteTo. This copies in a way that does not
	 * modify the pasteTo transforms until all new copies have been made. This fixes issues when a transform (or
	 * descendant) is in copyFrom and in pasteTo.
	 * 
	 * @param pasteTo
	 * @param copyFrom
	 * @param maintainWorldValues
	 * @param matchLocalTreeReferences
	 */
	public static void copyTransformsToTransforms(
			ArrayList<LttlTransform> pasteTo,
			ArrayList<LttlTransform> copyFrom, boolean maintainWorldValues,
			boolean matchLocalTreeReferences)
	{
		HashMap<LttlTransform, ArrayList<LttlTransform>> tempMap = new HashMap<LttlTransform, ArrayList<LttlTransform>>();
		for (LttlTransform transform : pasteTo)
		{
			ArrayList<LttlTransform> children = new ArrayList<LttlTransform>();
			for (LttlTransform lt : copyFrom)
			{
				// create a copy and add to scene (no parent)
				children.add(transform.getScene().addTransformCopy(lt,
						maintainWorldValues, matchLocalTreeReferences));
				// transform.addTransformCopyAsChild(lt, true);
			}
			// save the children to the hashmap for the selected transform
			tempMap.put(transform, children);
		}
		// iterate through the parent children map and actually set children to the parent
		// this protects copying a tranform state that has been modified becuase of the current
		// copy/paste procedure
		for (Iterator<Entry<LttlTransform, ArrayList<LttlTransform>>> it = tempMap
				.entrySet().iterator(); it.hasNext();)
		{
			Entry<LttlTransform, ArrayList<LttlTransform>> entry = it.next();
			ArrayList<LttlTransform> children = entry.getValue();
			for (LttlTransform child : children)
			{
				entry.getKey().setChild(child, maintainWorldValues);
			}
		}
	}

	public static ArrayList<String> GetNamesOnly(
			ArrayList<LttlTransform> transforms)
	{
		ArrayList<String> names = new ArrayList<String>();
		for (LttlTransform lt : transforms)
		{
			names.add(lt.getName());
		}
		return names;
	}
}
