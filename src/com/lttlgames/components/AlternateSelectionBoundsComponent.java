package com.lttlgames.components;

import com.lttlgames.editor.Lttl;
import com.lttlgames.editor.LttlComponent;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.editor.interfaces.AlternateSelectionBounds;

/**
 * Implements the {@link AlternateSelectionBounds} interface and automatically adds on start removes on destroy the
 * alternate selection bounds.
 */
@Persist(-90143)
public abstract class AlternateSelectionBoundsComponent extends LttlComponent
		implements AlternateSelectionBounds
{

	@Override
	public void onStart()
	{
		super.onStart();
		if (Lttl.game.inEditor())
		{
			t().setAlternateSelectionBounds(this);
		}
	}

	@Override
	public void onEditorStart()
	{
		super.onEditorStart();
		t().setAlternateSelectionBounds(this);
	}

	@Override
	public void onDestroyComp()
	{
		super.onDestroyComp();
		if (Lttl.game.inEditor())
		{
			t().setAlternateSelectionBounds(null);
		}
	}

	@Override
	public void onEditorDestroyComp()
	{
		super.onDestroyComp();
		t().setAlternateSelectionBounds(null);
	}
}
