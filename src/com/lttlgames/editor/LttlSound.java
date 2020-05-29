package com.lttlgames.editor;

import com.badlogic.gdx.audio.Sound;
import com.lttlgames.editor.annotations.GuiButton;
import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.GuiGroup;
import com.lttlgames.editor.annotations.GuiMax;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.Persist;

@Persist(-9016)
public class LttlSound
{
	private Sound ref;

	@GuiCallback("preview")
	@Persist(901601)
	public String name = "";

	@GuiGroup("Settings")
	@GuiMin(0)
	@GuiMax(1)
	@GuiCallback("preview")
	@Persist(901602)
	public float volume = 1;

	@GuiGroup("Settings")
	@GuiMin(.5f)
	@GuiMax(2.0f)
	@GuiCallback("preview")
	@Persist(901603)
	public float pitch = 1;

	@GuiGroup("Settings")
	@GuiMin(-1)
	@GuiMax(1)
	@GuiCallback("preview")
	@Persist(901604)
	public float pan = 0;

	public LttlSound()
	{

	}

	public LttlSound(String name)
	{
		this.name = name;
	}

	public LttlSound(LttlSound sound)
	{
		this.ref = sound.ref;
		this.name = sound.name;
		this.volume = sound.volume;
		this.pitch = sound.pitch;
		this.pan = sound.pan;
	}

	/**
	 * Set sound name and refresh
	 * 
	 * @param name
	 */
	public void setAndRefresh(String name)
	{
		this.name = name;
		refresh();
	}

	/**
	 * Gets the sound file, looks it up by name and saves local reference.
	 * 
	 * @return
	 */
	public void refresh()
	{
		ref = null;
		if (name != null && !name.isEmpty())
		{
			ref = LttlAudioManager.findSoundAll(name);
			if (ref == null)
			{
				Lttl.logNote("Sound not found: Searching for " + name);
			}
		}
	}

	/**
	 * Returns the {@link Sound} refernce, if none has been found, will refresh.
	 * 
	 * @return null if none found
	 */
	public Sound get()
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
			ref.play(volume, pitch, pan);
		}
	}

	/**
	 * @return the id of the sound instance if successful, or -1 on failure
	 */
	public long play()
	{
		if (ref != null)
		{
			return ref.play(volume, pitch, pan);
		}
		else
		{
			Lttl.logNote("Trying to play sound '" + name
					+ "' but no reference.");
		}
		return -1;
	}

	@GuiButton
	public void stop()
	{
		if (ref != null)
		{
			ref.stop();
		}
	}

	@SuppressWarnings("unused")
	private void preview()
	{
		stop();
		refresh();
		play();
	}
}
