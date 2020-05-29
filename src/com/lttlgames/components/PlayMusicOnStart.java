package com.lttlgames.components;

import com.lttlgames.editor.LttlComponent;
import com.lttlgames.editor.LttlMusic;
import com.lttlgames.editor.annotations.Persist;

@Persist(-9049)
public class PlayMusicOnStart extends LttlComponent
{
	@Persist(904901)
	public LttlMusic music = new LttlMusic();
	@Persist(904902)
	public boolean inEditor = false;

	@Override
	final public void onStart()
	{
		music.play();
	}

	@Override
	final public void onEditorStart()
	{
		if (inEditor)
		{
			onStart();
		}
	}
}
