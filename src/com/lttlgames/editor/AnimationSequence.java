package com.lttlgames.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.lttlgames.editor.annotations.GuiHide;
import com.lttlgames.editor.annotations.Persist;

@Persist(-9069)
@GuiHide
public class AnimationSequence
{
	/**
	 * Optional name of sequence, used for organizing and for animation callbacks
	 */
	@Persist(906901)
	public String name = "";
	@Persist(906902)
	public boolean active = true;
	@Persist(906903)
	public ArrayList<TimelineNode> nodes = new ArrayList<TimelineNode>();

	public float getLatestNodeTime()
	{
		if (nodes.size() == 0) return 0;
		float greatestTime = nodes.get(0).time;
		for (TimelineNode tl : nodes)
		{
			if (tl.time > greatestTime)
			{
				greatestTime = tl.time;
			}
		}
		return greatestTime;
	}

	public float getLatestKeyframeCallbackTime()
	{
		if (nodes.size() == 0) return 0;
		float greatestTime = nodes.get(0).time;
		for (TimelineNode tl : nodes)
		{
			if (tl.getClass() == TimelineNode.class) continue;
			if (tl.time > greatestTime)
			{
				greatestTime = tl.time;
			}
		}
		return greatestTime;
	}

	/**
	 * Sorts all the nodes by their time. (earliest to latest)
	 */
	public void sortNodes()
	{
		Collections.sort(nodes, new Comparator<TimelineNode>()
		{
			@Override
			public int compare(TimelineNode o1, TimelineNode o2)
			{
				return (o1.time == o2.time) ? 0
						: ((o1.time < o2.time) ? -1 : 1);
			}
		});
	}

	public ArrayList<String> getAllStateNames()
	{
		ArrayList<String> names = new ArrayList<String>();
		for (TimelineNode t : nodes)
		{
			if (t.getClass() == StateKeyframeNode.class)
			{
				String stateName = ((StateKeyframeNode) t).stateName;
				if (!names.contains(stateName))
				{
					names.add(stateName);
				}
			}
		}
		return names;
	}
}
