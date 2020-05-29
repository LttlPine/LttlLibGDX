package com.lttlgames.editor;

import com.badlogic.gdx.audio.Music;
import com.lttlgames.editor.annotations.GuiButton;
import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.GuiMax;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.Persist;

/**
 * Holds a reference to a {@link Music} object.
 */
@Persist(-9039)
public class LttlMusic
{
	private Music ref = null;

	@GuiCallback("preview")
	@Persist(903901)
	public String name = "";

	@GuiMin(0)
	@GuiMax(1)
	@GuiCallback("preview")
	@Persist(903902)
	public float volume = 1;

	@GuiMin(-1)
	@GuiMax(1)
	@GuiCallback("preview")
	@Persist(903903)
	public float pan = 0;

	@Persist(903904)
	public boolean isLooping = false;

	public LttlMusic()
	{

	}

	public LttlMusic(String name)
	{
		this.name = name;
	}

	public LttlMusic(LttlMusic music)
	{
		this.ref = music.ref;
		this.name = music.name;
		this.volume = music.volume;
		this.pan = music.pan;
		this.isLooping = music.isLooping;
	}

	/**
	 * Set music name and refresh
	 * 
	 * @param name
	 */
	public void setAndRefresh(String name)
	{
		this.name = name;
		refresh();
	}

	/**
	 * Gets the music file, looks it up by in all loaded scenes and saves local reference.
	 */
	public void refresh()
	{
		ref = null;
		if (name != null && !name.isEmpty())
		{
			ref = LttlAudioManager.findMusicAll(name);
			if (ref == null)
			{
				Lttl.logNote("Music not found: Searching for " + name);
			}
		}
	}

	/**
	 * Returns the {@link Music} refernce, if none has been found, will refresh.
	 * 
	 * @return null if none found
	 */
	public Music get()
	{
		if (ref == null)
		{
			refresh();
		}
		return ref;
	}

	@GuiButton(name = "Play")
	private void playGui()
	{
		refresh();
		if (ref != null)
		{
			ref.setPan(pan, volume);
			ref.setLooping(isLooping);
			ref.play();
		}
	}

	public void play()
	{
		if (ref != null)
		{
			ref.setPan(pan, volume);
			ref.setLooping(isLooping);
			ref.play();
		}
		else
		{
			Lttl.logNote("Trying to play music '" + name
					+ "' but no reference.");
		}
	}

	@GuiButton
	public void stop()
	{
		refresh();
		if (ref != null)
		{
			ref.stop();
		}
	}

	@SuppressWarnings("unused")
	private void preview()
	{
		refresh();
		if (ref == null) return;
		if (ref.isPlaying())
		{
			ref.setPan(pan, volume);
			ref.setLooping(isLooping);
		}
		else
		{
			play();
		}
	}
}
