package com.lttlgames.editor;

import com.badlogic.gdx.ApplicationListener;

/**
 * Allows the user to not have access to ApplicationListener's callbacks on LttlGame, which is created in the
 * constructor of this.
 */
public final class LttlGameListener implements ApplicationListener
{
	public LttlGameListener()
	{
		this(false);
	}

	public LttlGameListener(boolean editor)
	{
		Lttl.game = new LttlGame(editor);
	}

	@Override
	public void create()
	{
		if (Lttl.game.inEditor())
		{
			Lttl.game.startEditorMode();
		}
		else
		{
			Lttl.game.startPlayingMode();
		}

		// initialize gui if in editor and not initialized yet
		if (Lttl.game.inEditor() && !Lttl.editor.getGui().isInitialized())
		{
			Lttl.editor.getGui().initializeComponents();
		}
	}

	@Override
	public void resize(int width, int height)
	{
		Lttl.game.resize();
	}

	@Override
	public void render()
	{
		Lttl.game.render();
	}

	@Override
	public void pause()
	{
		Lttl.game.pause();
	}

	@Override
	public void resume()
	{
		Lttl.game.resume();
	}

	@Override
	public void dispose()
	{
		Lttl.game.dispose();
	}

}
