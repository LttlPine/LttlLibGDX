package com.lttlgames.components;

import com.lttlgames.editor.Lttl;
import com.lttlgames.editor.LttlComponent;
import com.lttlgames.editor.annotations.Persist;

@Persist(-9032)
public class LttlDemoComponent4 extends LttlComponent
{

	@Override
	public void onEditorUpdate()
	{
		// benchmark
		for (int i = 0; i < Lttl.editor.getSettings().quickInt; i++)
		{
			if (Lttl.editor.getSettings().quickBoolean)
			{
			}
			else
			{
			}
		}
	}
}
