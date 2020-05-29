package com.lttlgames.editor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;

/**
 * Loads all music and sound files on scene load. Returns the actual
 */
public class LttlAudioManager extends LttlResourceManager
{
	final private HashMap<String, Sound> soundMap = new HashMap<String, Sound>();
	final private HashMap<String, Music> musicMap = new HashMap<String, Music>();

	/**
	 * this will be used in play mode too, and is relative to platform's asset folder
	 */
	FileHandle soundDirInternal;
	/**
	 * this will be used in play mode too, and is relative to platform's asset folder
	 */
	FileHandle musicDirInternal;
	/**
	 * this will be used in editor only, and checks/creates folders
	 */
	FileHandle soundDirRelative;
	/**
	 * this will be used in editor only, and checks/creates folders
	 */
	FileHandle musicDirRelative;

	LttlAudioManager()
	{

	}

	/**
	 * Load sounds and music
	 */
	void initialLoad()
	{
		loadSound();
		loadMusic();
	}

	/**
	 * Defines the sound folders based on this scene.
	 */
	@Override
	void setupFolderNames()
	{
		soundDirInternal = getFileHandleInternalAssets("resources/"
				+ scene.getId() + "/sound");
		musicDirInternal = getFileHandleInternalAssets("resources/"
				+ scene.getId() + "/music");

		if (Lttl.game.inEditor())
		{
			soundDirRelative = getFileHandle("resources/" + scene.getId()
					+ "/sound", FileType.Absolute, true);
			musicDirRelative = getFileHandle("resources/" + scene.getId()
					+ "/music", FileType.Absolute, true);
		}
	}

	private void loadSound()
	{
		// dispose resources and clear map
		disposeSound();
		soundMap.clear();

		// convert each file in this folder to a sound object and add to map
		soundCrawl(soundDirInternal, "");
	}

	private void soundCrawl(FileHandle dir, String parentPath)
	{
		for (FileHandle fh : dir.list())
		{
			if (fh.isDirectory())
			{
				soundCrawl(fh, parentPath + fh.name() + "/");
			}
			else
			{
				soundMap.put(parentPath + fh.nameWithoutExtension(),
						Gdx.audio.newSound(fh));
			}
		}
	}

	private void loadMusic()
	{
		// dispose resources and clear map
		disposeMusic();
		musicMap.clear();

		// convert each file in this folder to a music object and add to map
		musicCrawl(musicDirInternal, "");
	}

	private void musicCrawl(FileHandle dir, String parentPath)
	{
		for (FileHandle fh : dir.list())
		{
			if (fh.isDirectory())
			{
				soundCrawl(fh, parentPath + dir.name() + "/");
			}
			else
			{
				musicMap.put(parentPath + fh.nameWithoutExtension(),
						Gdx.audio.newMusic(fh));
			}
		}
	}

	/**
	 * Find sound names that starts with String (good for finding stuff in folders). Only searches this scene and world.
	 */
	public ArrayList<String> findSoundNamesStartWith(String startsWith,
			ArrayList<String> container)
	{
		if (container == null)
		{
			container = new ArrayList<String>(0);
		}

		for (String s : soundMap.keySet())
		{
			if (s.startsWith(startsWith))
			{
				container.add(s);
			}
		}

		// search world if this scene is not the world (already would have searched)
		if (scene.getId() != Lttl.scenes.WORLD_ID)
		{
			Lttl.scenes.getWorld().getAudioManager()
					.findSoundNamesStartWith(startsWith, container);
		}
		return container;
	}

	/**
	 * Finds a sound loaded in this scene or in the world scene.
	 * 
	 * @param name
	 * @return
	 */
	public Sound findSound(String name)
	{
		Sound sound = soundMap.get(name);
		// search world if this scene is not the world (already would have searched)
		if (sound == null && scene.getId() != Lttl.scenes.WORLD_ID)
		{
			sound = Lttl.scenes.getWorld().getAudioManager().findSound(name);
		}
		return sound;
	}

	/**
	 * Finds sound in all loaded scenes.
	 * 
	 * @param name
	 * @return
	 */
	public static Sound findSoundAll(String name)
	{
		Sound s = null;
		for (LttlScene scene : Lttl.scenes.getAllLoaded(true))
		{
			s = scene.getAudioManager().soundMap.get(name);
			if (s != null)
			{
				break;
			}
		}
		return s;
	}

	/**
	 * Finds a music loaded in this scene or in the world scene.
	 * 
	 * @param name
	 * @return
	 */
	public Music findMusic(String name)
	{
		Music music = musicMap.get(name);
		// search world if this scene is not the world (already would have searched)
		if (music == null && scene.getId() != Lttl.scenes.WORLD_ID)
		{
			music = Lttl.scenes.getWorld().getAudioManager().findMusic(name);
		}
		return music;
	}

	/**
	 * Find music names that starts with String (good for finding stuff in folders). Only searches this scene and world.
	 */
	public ArrayList<String> findMusicNamesStartWith(String startsWith,
			ArrayList<String> container)
	{
		if (container == null)
		{
			container = new ArrayList<String>(0);
		}

		for (String s : soundMap.keySet())
		{
			if (s.startsWith(startsWith))
			{
				container.add(s);
			}
		}

		// search world if this scene is not the world (already would have searched)
		if (scene.getId() != Lttl.scenes.WORLD_ID)
		{
			Lttl.scenes.getWorld().getAudioManager()
					.findMusicNamesStartWith(startsWith, container);
		}
		return container;
	}

	/**
	 * Finds music in all loaded scenes.
	 * 
	 * @param name
	 * @return
	 */
	public static Music findMusicAll(String name)
	{
		Music m = null;
		for (LttlScene scene : Lttl.scenes.getAllLoaded(true))
		{
			m = scene.getAudioManager().musicMap.get(name);
			if (m != null)
			{
				break;
			}
		}
		return m;
	}

	private void disposeSound()
	{
		for (Sound a : soundMap.values())
		{
			a.stop();
			a.dispose();
		}
	}

	private void disposeMusic()
	{
		for (Music m : musicMap.values())
		{
			m.stop();
			m.dispose();
		}
	}

	void dispose()
	{
		disposeSound();
		disposeMusic();
	}

	/**
	 * Get the names of all the sounds in this loaded scene (alpha sorted).
	 * 
	 * @return
	 */
	ArrayList<String> getSoundNames()
	{
		ArrayList<String> names = new ArrayList<String>(soundMap.keySet());
		Collections.sort(names);
		return names;
	}

	/**
	 * Get the names of all the music in this loaded scene (alpha sorted).
	 * 
	 * @return
	 */
	ArrayList<String> getMusicNames()
	{
		ArrayList<String> names = new ArrayList<String>(musicMap.keySet());
		Collections.sort(names);
		return names;
	}

	/**
	 * Get the names of all the sounds in the loaded scenes sorted by scenes and alpha.
	 * 
	 * @return
	 */
	public static ArrayList<String> getSoundNamesAll()
	{
		ArrayList<String> list = new ArrayList<String>();
		for (LttlScene scene : Lttl.scenes.getAllLoaded(true))
		{
			list.addAll(scene.getAudioManager().getSoundNames());
		}
		return list;
	}

	/**
	 * Get the names of all the sounds in the loaded scenes sorted by scenes and alpha.
	 * 
	 * @return
	 */
	public static ArrayList<String> getMusicNamesAll()
	{
		ArrayList<String> list = new ArrayList<String>();
		for (LttlScene scene : Lttl.scenes.getAllLoaded(true))
		{
			list.addAll(scene.getAudioManager().getMusicNames());
		}
		return list;
	}

	static void editorStopAll()
	{
		for (LttlScene scene : Lttl.scenes.getAllLoaded(true))
		{
			for (Sound s : scene.getAudioManager().soundMap.values())
			{
				s.stop();
			}
			for (Music m : scene.getAudioManager().musicMap.values())
			{
				m.stop();
			}
		}
	}

	static void editorPauseAll()
	{
		for (LttlScene scene : Lttl.scenes.getAllLoaded(true))
		{
			for (Sound s : scene.getAudioManager().soundMap.values())
			{
				s.pause();
			}
			for (Music m : scene.getAudioManager().musicMap.values())
			{
				m.pause();
			}
		}
	}

	static void editorResumeAll()
	{
		for (LttlScene scene : Lttl.scenes.getAllLoaded(true))
		{
			for (Sound s : scene.getAudioManager().soundMap.values())
			{
				s.resume();
			}
			for (Music m : scene.getAudioManager().musicMap.values())
			{
				if (m.isPlaying())
				{
					m.play();
				}
			}
		}
	}
}
