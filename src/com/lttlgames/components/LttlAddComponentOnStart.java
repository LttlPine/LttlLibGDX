package com.lttlgames.components;

import com.lttlgames.editor.LttlComponent;
import com.lttlgames.editor.annotations.Persist;

/**
 * Adds a component to the transform on start. This good for using the same component on different objects.
 * 
 * @author Josh
 */
@Persist(-9013)
public class LttlAddComponentOnStart extends LttlComponent
{
	@Persist(901300)
	public LttlComponent source;

	@Override
	public void onStart()
	{
		if (source != null)
		{
			t().addComponentCopy(source);
		}
	}
}
