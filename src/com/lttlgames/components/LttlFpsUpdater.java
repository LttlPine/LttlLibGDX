package com.lttlgames.components;

import com.badlogic.gdx.Gdx;
import com.lttlgames.editor.LttlComponent;
import com.lttlgames.editor.LttlFontGenerator;
import com.lttlgames.editor.annotations.ComponentRequired;
import com.lttlgames.editor.annotations.Persist;

/**
 * Updates a LttlFontRender's text with the FPS
 * 
 * @author Josh
 */
@ComponentRequired(LttlFontGenerator.class)
@Persist(-9033)
public class LttlFpsUpdater extends LttlComponent
{
	@Persist(903301)
	public boolean showInEditor = true;
	@Persist(903302)
	public boolean showInPlay = true;

	@Override
	public void onUpdate()
	{
		if (showInPlay)
		{
			update();
		}
	}

	@Override
	public void onEditorUpdate()
	{
		if (showInEditor)
		{
			update();
		}
	}

	private void update()
	{
		((LttlFontGenerator) r().generator()).updateText(Gdx.graphics
				.getFramesPerSecond() + "");
	}
}
