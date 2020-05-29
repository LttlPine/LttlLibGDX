package com.lttlgames.editor;

import java.util.ArrayList;
import java.util.HashMap;

import com.lttlgames.editor.annotations.GuiHide;
import com.lttlgames.editor.annotations.Persist;

@Persist(-9044)
public class LttlSceneCore
{
	@Persist(904401)
	@GuiHide
	private int id = -1;

	@Persist(904402)
	@GuiHide
	String name;

	boolean isPendingUnload = false;
	private LttlScene lttSceneContainer;

	/**
	 * Stores the most top level transforms. Needs to be generated on scene creation/load and maintained.
	 */
	@Persist(904405)
	@GuiHide
	ArrayList<LttlTransform> transformHiearchy = new ArrayList<LttlTransform>();

	// NOTE could implement an IntMap here, but I don't think it's stable enough, especially with using nested
	// iterators. HashMap is more RELIABLE for now and already DONE and WORKING, and maybe performance isn't a huge
	// deal. If you do want to change to IntMap, will need to use interfaces serializable and deserializable... but you
	// don't want to because you don't have the time!!!
	@Persist(904403)
	@GuiHide
	HashMap<Integer, LttlComponent> componentMap = new HashMap<Integer, LttlComponent>();

	@Persist(904404)
	@GuiHide
	LttlTextureManager textureManager = new LttlTextureManager();
	LttlAudioManager audioManager = new LttlAudioManager();

	// for json creation
	LttlSceneCore()
	{

	}

	/**
	 * Constructor used when creating new scene
	 * 
	 * @param id
	 */
	LttlSceneCore(int id)
	{
		this.id = id;
	}

	/**
	 * Sets up scene Texture, Audio, and Music managers.
	 */
	void setupResources()
	{
		// Manager setups
		textureManager.setup(this);
		audioManager.setup(this);

		// if in editor and not playing, then make sure all directories exist (create them)
		if (Lttl.game.inEditor() && !Lttl.game.isPlaying())
		{
			checkDirectories();
		}

		// Manager processes
		textureManager.initialLoad();
		audioManager.initialLoad();
	}

	/**
	 * ONLY RAN IN EDITOR WHILE NOT PLAYING
	 */
	private void checkDirectories()
	{
		// create the textures to atlas input folder
		textureManager.inputAtlasTexturesDirRelative.mkdirs();
		// create the textures to atlas output folder (in assets)
		textureManager.outputAtlasTexturesDirRelative.mkdirs();
		// create the non atlas textures folder in android assets
		textureManager.nonAtlasTexturesDirRelative.mkdirs();

		// create audio folders
		audioManager.musicDirRelative.mkdirs();
		audioManager.soundDirRelative.mkdirs();
	}

	/**
	 * @param name
	 * @return True if was successful
	 */
	boolean setName(String name)
	{
		if (Lttl.scenes.sceneNameExists(name))
		{
			Lttl.logNote("LttlScene: Scene name already exists.");
			return false;
		}

		// remove current name from map
		Lttl.game.getWorldCore().sceneNameMap.remove(this.name);

		// update name and add it to map
		this.name = name;
		Lttl.game.getWorldCore().sceneNameMap.put(this.name, this.id);

		if (Lttl.game.inEditor())
		{
			Lttl.editor.getGui().getSelectionController()
					.reloadNode(getLttlScene());
		}

		return true;
	}

	String getName()
	{
		return name;
	}

	int getId()
	{
		return id;
	}

	/**
	 * Creates the LttlScene (container) if not created yet, and returns it. There is only one instance of this object.
	 * 
	 * @return
	 */
	LttlScene getLttlScene()
	{
		if (lttSceneContainer == null)
		{
			lttSceneContainer = new LttlScene(this);
		}
		return lttSceneContainer;
	}

	@Override
	public String toString()
	{
		return getName() + "[" + getId() + "]";
	}
}
