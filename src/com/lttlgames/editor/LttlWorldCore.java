package com.lttlgames.editor;

import java.util.HashMap;

import com.lttlgames.editor.annotations.GuiHide;
import com.lttlgames.editor.annotations.Persist;

//9
/**
 * User should never have access to this object.</br> Stores the always loaded components, game settings, and other
 * global data that needs to persist.
 */
@Persist(-9043)
final class LttlWorldCore extends LttlSceneCore
{
	// these are for all scenes
	@Persist(904301)
	@GuiHide
	private int lastComponentId = 0;

	@Persist(904302)
	@GuiHide
	private int lastSceneId = 0;

	/**
	 * holds all the scene names and their ID, this can be used for when loading and unloading scenes by name. Key =
	 * Name, Value = id<br>
	 * <b>This also defines which scenes are even available to be loaded.</b>
	 */
	@Persist(904303)
	@GuiHide
	HashMap<String, Integer> sceneNameMap = new HashMap<String, Integer>();

	/**
	 * Stores the scenes to load on start in editor (only loads world when playing outside of editor, which can then
	 * dynamically load other scenes)
	 */
	@Persist(904304)
	@GuiHide
	int[] scenesOnEditorStart = new int[] {};

	@Persist(904305)
	@GuiHide
	LttlGameSettings gameSettings = new LttlGameSettings();

	@Persist(904306)
	@GuiHide
	LttlEditorSettings editorSettings = new LttlEditorSettings();

	@Persist(904307)
	@GuiHide
	LttlCamera camera = new LttlCamera();

	/**
	 * The camera used to move around and see stuff in.
	 */
	@Persist(904308)
	@GuiHide
	LttlEditorCamera editorCamera = new LttlEditorCamera();

	@Persist(904309)
	@GuiHide
	PhysicsController physicsController = new PhysicsController();

	LttlWorldCore()
	{
		super(Lttl.scenes.WORLD_ID); // SET WORLD ID to constant
		this.name = "world";
		sceneNameMap.put(getName(), Lttl.scenes.WORLD_ID); // world to scenes name map
	}

	@Override
	boolean setName(String name)
	{
		Lttl.logNote("LttlWorld: Can't change world's name.");
		return false;
	}

	int nextComponentId()
	{
		lastComponentId++;
		return lastComponentId;
	}

	int nextSceneId()
	{
		lastSceneId++;
		return lastSceneId;
	}

	/**
	 * This is only for seeing lastSceneId to test against it. This is not for creating scenes.
	 * 
	 * @return
	 */
	int getLastSceneId()
	{
		return lastSceneId;
	}

	/**
	 * This is only for seeing lastComponentId to test against it. This is not for creating components.
	 * 
	 * @return
	 */
	int getLastComponentId()
	{
		return lastComponentId;
	}

	/**
	 * series of tasks to run before first update but after world is loaded
	 */
	void setup()
	{
		// inits physics world after the entire world scene has been loaded, that way the settings on PhysicsController
		// will be deserialized and be used for initing the physics world
		physicsController.initWorld();
	}
}
