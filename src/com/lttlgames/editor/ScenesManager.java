package com.lttlgames.editor;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Map.Entry;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.lttlgames.editor.LttlJsonDeserializer.ComponentRef;
import com.lttlgames.helpers.LttlHelper;

/**
 * @author Josh
 */
public final class ScenesManager
{
	ScenesManager()
	{
		checkFolderStructure();

		// if this is the first load of the game, and it is in editor not playing, delete all temp scene files
		if (Lttl.game.isFirstLoad() && Lttl.game.inEditor()
				&& !Lttl.game.isPlaying())
		{
			deleteAllTempScenes();
		}
	}

	/**
	 * The constant world scene ID
	 */
	public final int WORLD_ID = 0;

	private ArrayList<LttlSceneCore> scenesAndWorld = new ArrayList<LttlSceneCore>();
	/**
	 * keeps track if scenesAndWorld need to update
	 */
	boolean isScenesAndWorldListDirty = true;

	ArrayList<LttlSceneCore> loadedScenes = new ArrayList<LttlSceneCore>();

	private FileHandle scenesDirRelative;
	private FileHandle backupDirRelative;
	private FileHandle tempDirRelative;
	private FileHandle scenesDirInternal; // only one that needs to be internal because it will be accessed not
											// in editor

	LttlSceneCore getLoadedSceneCore(String sceneName)
	{
		return getLoadedSceneCore(getSceneId(sceneName));
	}

	/**
	 * Return a scene by id. User should never have access to LttlScene object via script.
	 * 
	 * @param sceneId
	 * @return Will return null if not loaded.
	 */
	LttlSceneCore getLoadedSceneCore(int sceneId)
	{
		if (sceneId == Lttl.scenes.WORLD_ID) { return Lttl.game.getWorldCore(); }
		for (LttlSceneCore ls : loadedScenes)
		{
			if (ls.getId() == sceneId) { return ls; }
		}
		return null;
	}

	/**
	 * Get a loaded scene (including world) based on id.
	 * 
	 * @param sceneId
	 * @return null of none found
	 */
	public LttlScene get(int sceneId)
	{
		LttlSceneCore sceneCore = getLoadedSceneCore(sceneId);
		if (sceneCore == null) { return null; }
		return sceneCore.getLttlScene();
	}

	/**
	 * Get a loaded scene based on name.
	 * 
	 * @param name
	 * @return null of none found
	 */
	public LttlScene get(String name)
	{
		LttlSceneCore sceneCore = getLoadedSceneCore(getSceneId(name));
		if (sceneCore == null) { return null; }
		return sceneCore.getLttlScene();
	}

	/**
	 * Returns the World scene.
	 * 
	 * @return
	 */
	public LttlScene getWorld()
	{
		return getWorldCore().getLttlScene();
	}

	/**
	 * Returns all loaded scenes, in the order they were loaded
	 * 
	 * @param includeWorld
	 *            optionally can include the world scene in list (first)
	 * @return
	 */
	public ArrayList<LttlScene> getAllLoaded(boolean includeWorld)
	{
		ArrayList<LttlScene> scenes = new ArrayList<LttlScene>();
		if (includeWorld)
		{
			scenes.add(getWorld());
		}
		for (LttlSceneCore sc : loadedScenes)
		{
			scenes.add(sc.getLttlScene());
		}

		return scenes;
	}

	/**
	 * Get numbers of scenes that exist (not including world)
	 * 
	 * @return
	 */
	public int getSceneCount()
	{
		return Lttl.scenes.getWorldCore().sceneNameMap.size() - 1;
	}

	/**
	 * Get numbers of scenes that are loaded (not including world)
	 * 
	 * @return
	 */
	public int getLoadedSceneCount()
	{
		return Lttl.scenes.loadedScenes.size();
	}

	/**
	 * Returns if all scenes are loaded.
	 * 
	 * @return
	 */
	public boolean areAllScenesLoaded()
	{
		return getSceneCount() == getLoadedSceneCount();
	}

	/**
	 * Checks if scene is currently loaded.
	 * 
	 * @param scene
	 * @return
	 */
	public boolean isSceneLoaded(int sceneId)
	{
		if (sceneId == Lttl.scenes.WORLD_ID) return true;
		for (LttlSceneCore ls : loadedScenes)
		{
			if (ls.getId() == sceneId) { return true; }
		}
		return false;
	}

	/**
	 * Checks if scene is loaded
	 * 
	 * @param sceneName
	 * @return
	 */
	public boolean isSceneLoaded(String sceneName)
	{
		return isSceneLoaded(getSceneId(sceneName));
	}

	/**
	 * Load a scene based on the name.
	 * 
	 * @param name
	 * @return the loaded scene, null if load failed
	 */
	public LttlScene loadScene(String name)
	{
		return loadScene(getSceneId(name));
	}

	/**
	 * Loads scene from json file using scene's id and runs OnStart callback.
	 * 
	 * @param sceneId
	 * @return the loaded scene, null if load failed
	 */
	public LttlScene loadScene(int sceneId)
	{
		// since this function is able to be ran by user, check this
		if (sceneId == Lttl.scenes.WORLD_ID)
		{
			Lttl.Throw("Can't load world scene.");
		}

		Lttl.logNote("Loading Scene: " + getSceneName(sceneId) + "[" + sceneId
				+ "]");

		LttlSceneCore scene = loadSceneNoCallBack(sceneId, LttlSceneCore.class);
		if (scene == null) { return null; }

		// callback all trasnforms
		processSceneOnStartCallBack(scene);

		return scene.getLttlScene();
	}

	/**
	 * sets world value on LttlGame if world
	 * 
	 * @param sceneId
	 * @param sceneClass
	 * @return can return null if no scene is found
	 */
	private LttlSceneCore loadSceneNoCallBack(int sceneId,
			Class<? extends LttlSceneCore> sceneClass)
	{
		// check a bunch of stuff, but only if it's not loading the world, since a lot of it references the world
		if (sceneId != Lttl.scenes.WORLD_ID)
		{
			if (!getWorldCore().sceneNameMap.containsValue(sceneId))
			{
				Lttl.Throw("No scene with ID " + sceneId
						+ " exists for loading.");
			}
			if (sceneId != WORLD_ID
					&& sceneId > getWorldCore().getLastSceneId())
			{
				Lttl.Throw("Inconsistency with scene ids. Scene id " + sceneId
						+ " is out of range.");
			}
			if (isSceneLoaded(sceneId))
			{
				Lttl.Throw("Scene id " + sceneId + " already loaded.");
			}
		}

		// get json string, if in editor, tries to get it from temp folder first
		String jsonString;
		if (Lttl.game.inEditor())
		{
			jsonString = getSceneJsonStringTryTempDir(sceneId);
		}
		else
		{
			jsonString = getSceneJsonStringScenesDir(sceneId);
		}
		// check if jsonString failed, return a null scene
		if (jsonString == null)
		{
			Lttl.logNote("Loading Scene: Can't find scene file for scene id "
					+ sceneId + ".");
			return null;
		}

		// create LttlScene object from json
		ArrayList<ComponentRef> compRefsList = new ArrayList<ComponentRef>(50);
		LttlSceneCore scene = LttlCopier.fromJson(jsonString, sceneClass,
				compRefsList, null);
		if (sceneClass != LttlWorldCore.class)
		{
			// add scene to game, don't want to add LttlWorld to loadedScenes and don't need to update scenesAndWorld,
			// since if it's just teh world scene it'll automatically update when you get it.
			loadSceneShared(scene);
		}
		else
		{
			// set world on game
			Lttl.game.world = (LttlWorldCore) scene;
		}

		// update references and run caclulations and other preparations
		prepareLoadedScene(scene, compRefsList);

		return scene;
	}

	/**
	 * Loads the world from saved json or creates a new one, does not do an OnStart callback
	 * 
	 * @return
	 */
	LttlWorldCore loadWorld()
	{
		LttlWorldCore lw = null;

		// if no file could be found for world scene in temp or scene dir, then create a new one, this is just checking,
		// which is only done in editor, since a world would always be in a game
		if (Lttl.game.inEditor()
				&& getSceneJsonStringTryTempDir(WORLD_ID) == null)
		{
			// create world object
			lw = new LttlWorldCore();

			// set world on game
			Lttl.game.world = lw;

			// update references and run caclulations and other preparations
			prepareLoadedScene(lw, new ArrayList<ComponentRef>(0));
			return lw;
		}
		else
		{
			// otherwise create it from the json (like normal)
			lw = (LttlWorldCore) loadSceneNoCallBack(WORLD_ID,
					LttlWorldCore.class);

			if (lw == null)
			{
				Lttl.Throw("Loading Scene World - Can't find scene file for world.");
				return null;
			}
		}

		return lw;
	}

	/**
	 * Loads all the scenes from scenesOnEditorStart array on LttlWorld.
	 */
	void loadOnStartEditorScenes()
	{
		for (int sceneId : getWorldCore().scenesOnEditorStart)
		{
			loadSceneNoCallBack(sceneId, LttlSceneCore.class);
		}
	}

	/**
	 * Updates references (id to component) and some other preps
	 * 
	 * @param scene
	 * @param compRefsList
	 */
	void prepareLoadedScene(final LttlSceneCore scene,
			ArrayList<ComponentRef> compRefsList)
	{
		// sets up resources on scene
		scene.setupResources();

		// update references in each the component references that came from deserializing
		for (ComponentRef cr : compRefsList)
		{
			cr.set(scene);
		}

		// OLD
		// for (Iterator<LttlComponent> it = scene.componentMap.values()
		// .iterator(); it.hasNext();)
		// {
		// LttlComponent lc = it.next();
		// if (lc.getId() > getWorldCore().getLastComponentId())
		// {
		// Lttl.Throw("Inconsistency with component ids.  Loading component id "
		// + lc.getId()
		// + " which is out of range for component ids.");
		// }
		// ComponentHelper.updateReferencesFromId(lc, scene);
		// }
		//
		// // update the references in the transform hiearchy, since these were imported, and then add any new ones
		// prepTransformHierarchy(scene);

		if (Lttl.game.inEditor())
		{
			Lttl.editor.getGui().getSelectionController()
					.addSceneTree(scene.getLttlScene());
		}

		// then run initial calculations on all transforms and add to z ordered list
		initialSceneUpdate(scene);
	}

	void processSceneOnStartCallBack(LttlSceneCore scene)
	{
		for (LttlTransform lt : scene.transformHiearchy)
		{
			ComponentHelper.callBackTransformTree(lt,
					ComponentCallBackType.onStart);
		}

	}

	/**
	 * Calculates initial matrices and stuff and adds to ordered z list for all the top level transforms and children
	 * recursively.<br>
	 * <b>This requires that the scene's transform hierarchy has been populated.</b>
	 * 
	 * @param scene
	 */
	private void initialSceneUpdate(LttlSceneCore scene)
	{
		for (LttlTransform lt : scene.transformHiearchy)
		{
			ComponentHelper.initialPrepTree(lt);
		}
	}

	private void prepTransformHierarchy(LttlSceneCore ls)
	{
		// copy transform hiearchy
		ArrayList<LttlTransform> originalList = new ArrayList<LttlTransform>(
				ls.transformHiearchy);

		// clear transformHiearchy
		ls.transformHiearchy.clear();

		// update
		for (LttlTransform lt : originalList)
		{
			LttlTransform realLt = (LttlTransform) ls.componentMap.get(lt
					.getId());
			// only add it if it's not null
			if (realLt != null)
			{
				ls.transformHiearchy.add(realLt);
			}
		}

		// populate with new components (this would really only happen if modifying JSON)
		for (LttlComponent lc : ls.componentMap.values())
		{
			if (lc.getClass() == LttlTransform.class)
			{
				LttlTransform lt = (LttlTransform) lc;
				if (lt.getParent() == null
						&& !ls.transformHiearchy.contains(lt))
				{
					ls.transformHiearchy.add(lt);
				}
			}
		}
	}

	private String getSceneJsonStringShared(int sceneId, FileHandle directory)
	{
		FileHandle file = getJsonFile(sceneId, directory);
		if (file != null)
		{
			return file.readString();
		}
		else
		{
			return null;
		}
	}

	/**
	 * Finds the latest saved json file for this scene and returns the contents. This should be useable by all
	 * platforms.
	 * 
	 * @param sceneId
	 * @return The scene's json string, if can't be found, then returns a blank string.
	 * @throws RuntimeException
	 *             if can't find scene file or if directy does not exist
	 */
	private String getSceneJsonStringScenesDir(int sceneId)
	{
		String json = getSceneJsonStringShared(sceneId, scenesDirInternal);
		if (json != null) { return json; }
		Lttl.Throw("Can't find scene file for scene id " + sceneId + ".");
		return null;
	}

	/**
	 * Finds the json file in the temp directory for this scene and returns the contents. This should be useable by all
	 * platforms.
	 * 
	 * @param sceneId
	 * @return The scene's json string, if can't be found, then returns a blank string.
	 * @throws RuntimeException
	 *             if can't find scene file or if directy does not exist
	 */
	private String getSceneJsonStringTempDir(int sceneId)
	{
		String json = getSceneJsonStringShared(sceneId, tempDirRelative);
		if (json != null) { return json; }
		Lttl.Throw("Can't find scene file for scene id " + sceneId + ".");
		return null;
	}

	/**
	 * Returns the json file for the scene id by first trying to find it in the temp directory, if none found, then
	 * tries in the scenes directory, if none their either, then returns null. This is so loadWorld can know if a world
	 * exists or not without throwing anything.
	 * 
	 * @param sceneId
	 * @return
	 */
	private String getSceneJsonStringTryTempDir(int sceneId)
	{
		String json = null;
		if ((json = getSceneJsonStringShared(sceneId, tempDirRelative)) != null
				|| (json = getSceneJsonStringShared(sceneId, scenesDirInternal)) != null) { return json; }
		return null;
	}

	/**
	 * Returns the json file for the scene id, none found, then returns null. This is so loadWorld can know if a world
	 * exists or not without throwing anything.
	 * 
	 * @param sceneId
	 * @param directory
	 *            either scenes or temp
	 * @return
	 */
	private FileHandle getJsonFile(int sceneId, FileHandle directory)
	{
		// if (!directory.exists())
		// {
		// Lttl.Throw("Directory " + directory.path()
		// + " does not exist when trying to load scene.");
		// }

		FileHandle[] files = directory.list();
		// grab the 'current' file
		if (files != null)
		{
			for (FileHandle file : files)
			{
				// System.out.println(file.name());
				if (file.name().startsWith(sceneId + "_")) { return file; }
			}
		}
		return null;
	}

	/**
	 * Usually triggered by Ctrl + S, if scene is already unloaded but it has a temporary one saved then it will use
	 * that. The temp folder should be empty after this.
	 */
	void saveAllScenes()
	{
		if (Lttl.game.isPlaying())
		{
			Lttl.logNote("Saving All Scenes: Not allowed while playing.");
			return;
		}

		if (Lttl.game.inEditor() && !Lttl.game.isPlaying())
		{
			for (LttlSceneCore ls : getScenesAndWorld())
			{
				saveScene(ls, false);
			}
		}

		// clean up delete scenes //
		// delete any temporary saves
		for (FileHandle f : tempDirRelative.list())
		{
			if (!f.isDirectory())
			{
				int sceneId = Integer.parseInt((f.name().split("_"))[0]);
				if (!Lttl.scenes.getWorldCore().sceneNameMap.values().contains(
						sceneId))
				{
					f.delete();
				}
			}
		}
		// move all current saves to backup directory
		for (FileHandle f : scenesDirRelative.list())
		{
			if (!f.isDirectory())
			{
				int sceneId = Integer.parseInt((f.name().split("_"))[0]);
				if (!Lttl.scenes.getWorldCore().sceneNameMap.values().contains(
						sceneId))
				{
					FileHandle sceneBackupDirRelative = LttlResourceManager
							.getFileHandle(backupDirRelative.path() + "/"
									+ sceneId, FileType.Absolute, false);
					f.moveTo(new FileHandle(sceneBackupDirRelative.path() + "/"
							+ f.name()));
				}
			}
		}

		// save any non loaded scenes with temporary files
		saveTempScenes();
	}

	/**
	 * Saves the specified scene into temp folder. This is called when a scene is unloaded only, and it is saved into
	 * the temp folder.
	 * 
	 * @param scene
	 * @return if changes were found
	 */
	boolean temporarySaveScene(LttlSceneCore scene)
	{
		Lttl.scenes.callBackScene(scene, ComponentCallBackType.onSaveScene);
		return setSceneJsonString(scene.getId(), true, LttlCopier.toJson(scene));
	}

	/**
	 * Saves all loaded scenes and the world to the temporary folder.
	 */
	void temporarySaveAllScenes()
	{
		if (Lttl.game.isPlaying())
		{
			Lttl.logNote("Temporary Saving All Scenes: Not allowed while playing.");
			return;
		}
		for (LttlSceneCore ls : getScenesAndWorld())
		{
			temporarySaveScene(ls);
		}
	}

	boolean saveScene(LttlSceneCore scene, boolean temp)
	{
		if (Lttl.game.isPlaying())
		{
			Lttl.logNote("Saving Scene: Not allowed while playing.");
			return false;
		}
		Lttl.scenes.callBackScene(scene, ComponentCallBackType.onSaveScene);
		return setSceneJsonString(scene.getId(), temp, LttlCopier.toJson(scene));
	}

	boolean saveScene(int id, boolean temp)
	{
		return saveScene(getLoadedSceneCore(id), temp);
	}

	/**
	 * Deletes all the temporary saved scenes in the temp folder, primarily called when editor loads for the first time.
	 */
	void deleteAllTempScenes()
	{
		if (tempDirRelative.exists())
		{
			for (FileHandle f : tempDirRelative.list())
			{
				f.delete();
			}
		}
	}

	/**
	 * All temporary saved scenes in the temp folder will be saved into the main folder, and the temp folder contents
	 * will be deleted. This is called after a saveAllScenes(), since some scenes may not be loaded, but when unloaded a
	 * temporary save was made, so when the user actually wants to save, it will be able to take those temp scenes json
	 * data and save it as if it was actually loaded.
	 */
	private void saveTempScenes()
	{
		// just get the json string from each temp file and use it in normal setSceneJsonString as if it was generated
		// and delete itself
		for (FileHandle f : tempDirRelative.list())
		{
			if (!f.isDirectory())
			{
				int sceneId = Integer.parseInt((f.name().split("_"))[0]);
				setSceneJsonString(sceneId, false, f.readString());
				f.delete();
			}
		}
	}

	/**
	 * Sets the Json String into a txt file. First it compares the jsonString with last saved scene on file, and if
	 * same, just updates timestamp on file name. If it is new, then moves old into the backup folder, inside a folder
	 * with that scene id.<br>
	 * <br>
	 * As of now only works on desktop.
	 * 
	 * @param id
	 * @param temp
	 *            if true will save this scene in the temp folder (overwrite if already there), if false then will save
	 *            in main directory and check if this scene is in the temp folder and delete it
	 * @param jsonString
	 * @return boolean true, if changes were found, or false if no changes or error
	 */
	private boolean setSceneJsonString(int id, boolean temp, String jsonString)
	{
		// can only make directories and stuff on desktop
		if (Lttl.game.inEditor() && !Lttl.game.isPlaying()
				&& Gdx.app.getType() != ApplicationType.Desktop)
		{
			Lttl.Throw("Can only save scene in desktop.");
			return false;
		}

		// decides if saving in temp folder or not
		FileHandle currentDirFile = (temp) ? tempDirRelative
				: scenesDirRelative;

		// search for any previous file for this scene id
		FileHandle currentFile = null; // could be temp
		for (FileHandle f : currentDirFile.list())
		{
			if (!f.isDirectory() && f.name().startsWith(id + "_"))
			{
				currentFile = f;
				break;
			}
		}

		// if not saving to temp folder, then check and remove any of this scene from the temp folder
		if (!temp)
		{
			for (FileHandle f : tempDirRelative.list())
			{
				if (!f.isDirectory() && f.name().startsWith(id + "_"))
				{
					f.delete();
					break;
				}
			}
		}

		// generate new name for file
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
		String date = dateFormat.format(new Date());
		String newFileName = id + "_" + date + ".lttl";

		// if found previous file for scene id
		if (currentFile != null)
		{
			// compare contents
			if (currentFile.readString().equals(jsonString))
			{
				Lttl.logNote("Saving " + ((temp) ? "Temp " : "") + "Scene "
						+ Lttl.scenes.getSceneName(id) + " [" + id
						+ "]: No changes found.");
				return false; // no changes, don't write new file
			}
			else
			// othwise the files are different
			{
				if (temp)
				{
					// if saving into temp folder and the files are different, then just delete it and save a new one
					currentFile.delete();
				}
				else
				{
					// if not saving to temp folder, then move file to it's backup directory so new file can be made in
					// main directory
					FileHandle sceneBackupDirRelative = LttlResourceManager
							.getFileHandle(backupDirRelative.path() + "/" + id,
									FileType.Absolute, false);
					if (!sceneBackupDirRelative.exists())
					{
						sceneBackupDirRelative.mkdirs();
					}
					currentFile.moveTo(new FileHandle(sceneBackupDirRelative
							.path() + "/" + currentFile.name()));
				}

			}
		}

		// save to new file in currentDirFile (temp or main)
		PrintWriter out = null;
		try
		{
			out = new PrintWriter(new BufferedWriter(new FileWriter(
					currentDirFile.path() + "/" + newFileName, false)));
		}
		catch (IOException e)
		{
			e.printStackTrace();
			Lttl.Throw(new RuntimeException("Error saving scene. "
					+ Lttl.scenes.getSceneName(id) + "[" + id + "]"));
		}
		if (out != null)
		{
			out.print(jsonString);
			out.flush();
			out.close();
		}
		else
		{
			Lttl.Throw(new RuntimeException("Error saving scene. [2]"));
		}

		Lttl.logNote("Saving " + ((temp) ? "Temp " : "") + "Scene "
				+ Lttl.scenes.getSceneName(id) + " [" + id
				+ "]: Changes found [new file].");
		return true;
	}

	/**
	 * Checks and creates necessary folders, also defines the internal folder to locate scene when not in editor on
	 * platform
	 */
	private void checkFolderStructure()
	{
		// scenes folder
		if (Lttl.game.inEditor())
		{
			scenesDirRelative = LttlResourceManager.getFileHandle("scenes",
					FileType.Absolute, true);
			if (!scenesDirRelative.exists())
			{
				scenesDirRelative.mkdirs();
			}
		}
		scenesDirInternal = LttlResourceManager
				.getFileHandleInternalAssets("scenes");

		// backup folder (only in editor)
		if (Lttl.game.inEditor())
		{
			backupDirRelative = LttlResourceManager.getFileHandle(
					"scenes/backups", FileType.Absolute, true);
			if (!backupDirRelative.exists())
			{
				backupDirRelative.mkdirs();
			}
		}

		// temp folder (only in editor)
		if (Lttl.game.inEditor())
		{
			tempDirRelative = LttlResourceManager.getFileHandle("scenes/temp",
					FileType.Absolute, true);
			if (!tempDirRelative.exists())
			{
				tempDirRelative.mkdirs();
			}
		}
	}

	/**
	 * Actually removes scene from loadedScenes and some other final stuff. This is ran right before all queued
	 * components are destroyed in the stage().
	 * 
	 * @param scene
	 */
	void executeUnloadScene(LttlScene scene)
	{
		// clear texture referernces (if any)
		scene.getTextureManager().clearReferences();
		// TODO could clear audio resources same way, but I don't think it's as big of an issue

		// dispose all scene specific resources
		scene.getTextureManager().dispose();
		scene.getAudioManager().dispose();

		// this makes sure there are no references of LttlSceneCore, can't use LttlScene (container) anymore now
		scene.nullRef();

		if (Lttl.game.inEditor() && !Lttl.game.isPlaying())
		{
			// update scene from loadOnStart, now that it has been removed from loadedScenes
			updateScenesOnEditorStart();
			// temporary save world at the state it is after scene unloaded
			temporarySaveScene(getWorldCore());
		}
		isScenesAndWorldListDirty = true;
	}

	/**
	 * Ran when a scene is loaded or unloaded. World scene is first.
	 */
	private void updateScenesAndWorld()
	{
		isScenesAndWorldListDirty = false;
		scenesAndWorld.clear();
		scenesAndWorld.add(getWorldCore());
		scenesAndWorld.addAll(loadedScenes);
	}

	/**
	 * Returns the scene ID based on a scene name (regardless of being loaded)
	 * 
	 * @param name
	 * @return -1 if none found
	 */
	public int getSceneId(String name)
	{
		Integer sceneId = -1;
		if (name.equals(Lttl.game.getWorldCore().getName())) { return Lttl.scenes.WORLD_ID; }

		sceneId = Lttl.game.getWorldCore().sceneNameMap.get(name);
		if (sceneId == null)
		{
			return -1;
		}
		else
		{
			return sceneId.intValue();
		}
	}

	/**
	 * @param id
	 * @return Null if scene could not be found with that id.
	 */
	public String getSceneName(int id)
	{
		if (id == Lttl.scenes.WORLD_ID) { return Lttl.game.getWorldCore()
				.getName(); }
		String name = (String) LttlHelper.GetHashMapFirstKey(
				Lttl.game.getWorldCore().sceneNameMap, id, false);
		if (name == null)
		{
			Lttl.logNote("getSceneId: No scene found with id: " + id);
		}
		return name;
	}

	/**
	 * Returns an ArrayList of the world scene core (first) and all loaded scenes core.
	 * 
	 * @return
	 */
	ArrayList<LttlSceneCore> getScenesAndWorld()
	{
		if (isScenesAndWorldListDirty)
		{
			updateScenesAndWorld();
		}
		return scenesAndWorld;
	}

	LttlWorldCore getWorldCore()
	{
		return Lttl.game.getWorldCore();
	}

	/**
	 * updates the array on LttlWorld scenesOnEditorStart with the ids of the currently loaded scenes. This is done
	 * during every user initiated save
	 */
	void updateScenesOnEditorStart()
	{
		if (Lttl.game.inEditor())
		{
			// generate array of scenes to automatically load on next editor
			int[] idArray = new int[loadedScenes.size()];
			for (int i = 0; i < loadedScenes.size(); i++)
			{
				idArray[i] = loadedScenes.get(i).getId();
			}
			getWorldCore().scenesOnEditorStart = idArray;
		}
	}

	/**
	 * Creates a new scene with given name. This can not be called while game is playing.
	 * 
	 * @param name
	 *            must be unique
	 * @return the id of the new scene
	 */
	LttlScene createScene(String name)
	{
		if (Lttl.game.isPlaying())
		{
			Lttl.Throw("Not allowed to create scenes while in play mode.");
		}
		if (sceneNameExists(name))
		{
			Lttl.Throw("Scene name already exists.");
		}

		// create scene object
		LttlSceneCore newScene = new LttlSceneCore(getWorldCore().nextSceneId());
		newScene.setName(name);

		// setup resources and other stuff
		newScene.setupResources();

		loadSceneShared(newScene);

		if (Lttl.game.inEditor())
		{
			Lttl.editor.getGui().getSelectionController()
					.addSceneTree(newScene.getLttlScene());
		}

		return newScene.getLttlScene();
	}

	/**
	 * Adds LttlScene object (created or loaded) to loadedScenes, updates scenesAndWorld, and updatesScenesOnEditorStart
	 * array
	 * 
	 * @param scene
	 */
	private void loadSceneShared(LttlSceneCore scene)
	{
		loadedScenes.add(scene);
		isScenesAndWorldListDirty = true;
		updateScenesOnEditorStart();
	}

	/**
	 * Deletes a scene from the entire game. This is a permanent deleting of a scene, not like unloadScene(). Should
	 * only be ran in editor while not playing.
	 * 
	 * @param sceneId
	 */
	public void deleteScene(int sceneId)
	{
		if (!Lttl.game.inEditor() || Lttl.game.isPlaying())
		{
			Lttl.Throw("Can't delete a scene while playing or in editor.");
		}

		// check if scene is loaded, if so then unload it first
		LttlSceneCore scene = getLoadedSceneCore(sceneId);
		if (scene != null)
		{
			scene.getLttlScene().unload(true, true);
		}

		Lttl.logNote("Deleting Scene: " + scene.getName() + "[" + scene.getId()
				+ "]");

		// need to iterate through sceneNameMap and remove this sceneID's name map
		for (Iterator<Entry<String, Integer>> it = getWorldCore().sceneNameMap
				.entrySet().iterator(); it.hasNext();)
		{
			Entry<String, Integer> pair = it.next();
			if (pair.getValue() == sceneId)
			{
				getWorldCore().sceneNameMap.remove(pair.getKey());
				break;
			}
		}

		// now temporary save world, since the sceneNameMap has been updated; however, it will be temporaryily saved
		// again when the unload is executed
		temporarySaveScene(getWorldCore());
	}

	/**
	 * Checks if scene name already exists (whether loaded or not)
	 * 
	 * @param name
	 * @return true if scene name already exists
	 */
	public boolean sceneNameExists(String name)
	{
		return getSceneId(name) != -1;
	}

	/**
	 * Callback all loaded scenes
	 * 
	 * @param methodType
	 */
	void callBackScenes(ComponentCallBackType methodType)
	{
		// iterate through all loaded scenes and world
		for (LttlSceneCore ls : Lttl.scenes.getScenesAndWorld())
		{
			callBackScene(ls, methodType);
		}
	}

	/**
	 * Iterate through all the loaded scenes transforms (recursively) and run all the components' update method.
	 */
	private void callBackScene(LttlSceneCore ls,
			ComponentCallBackType methodType)
	{
		for (LttlTransform lt : ls.getLttlScene().getTopLevelTransforms())
		{
			ComponentHelper.callBackTransformTree(lt, methodType);
		}
	}

	/**
	 * Returns the first transform found when searching all scenes and world. Very slow, save returned reference.<br>
	 * <b>DO NOT RUN EVERY FRAME!</b>
	 * 
	 * @param name
	 * @return found transform, or null if not found
	 */
	public LttlTransform findTransformAllScenes(String name)
	{
		LttlTransform finded = null;
		for (LttlScene ls : Lttl.scenes.getAllLoaded(true))
		{
			if ((finded = ls.findTransform(name)) != null) { return finded; }
		}
		return null;
	}

	/**
	 * Returns the transforms found when searching all scenes and world. Very slow, save returned reference.<br>
	 * <b>DO NOT RUN EVERY FRAME!</b>
	 * 
	 * @param name
	 * @return found transform, or null if not found
	 */
	public ArrayList<LttlTransform> findTransformsAllScenes(String name)
	{
		ArrayList<LttlTransform> finds = new ArrayList<LttlTransform>();
		for (LttlSceneCore lsc : getScenesAndWorld())
		{
			finds.addAll(lsc.getLttlScene().findTransforms(name));
		}
		return finds;
	}

	/**
	 * Returns the first component found when searching all scenes and world. Slow, save returned reference.<br>
	 * <b>DO NOT RUN EVERY FRAME!</b>
	 * 
	 * @param theClass
	 *            class of component searching for
	 * @param includeSubclasses
	 *            should get subclasses
	 * @return found component, or null if not found
	 */
	public <T> T findComponentAllScenes(Class<T> theClass,
			boolean includeSubclasses)
	{
		T finded = null;
		for (LttlSceneCore lsc : getScenesAndWorld())
		{
			if ((finded = lsc.getLttlScene().findComponent(theClass,
					includeSubclasses)) != null) { return finded; }
		}
		return null;
	}

	/**
	 * Returns all the found when searching all scenes and world. The found components are in no reliable order. Slow,
	 * save returned references.<br>
	 * <b>DO NOT RUN EVERY FRAME!</b>
	 * 
	 * @param theClass
	 *            class of component searching for
	 * @param includeSubClasses
	 *            should get subclasses
	 * @param containerList
	 *            adds the components to this list, does not clear beforehand
	 * @return found components
	 */
	public <T> ArrayList<T> findComponentsAllScenes(Class<T> theClass,
			boolean includeSubClasses, ArrayList<T> containerList)
	{
		for (LttlSceneCore lsc : getScenesAndWorld())
		{
			containerList.addAll(lsc.getLttlScene().findComponents(theClass,
					includeSubClasses));
		}
		return containerList;
	}

	/**
	 * @see #findComponentsAllScenes(Class, boolean, ArrayList)
	 */
	public <T> ArrayList<T> findComponentsAllScenes(Class<T> theClass,
			boolean extendedClasses)
	{
		return findComponentsAllScenes(theClass, extendedClasses,
				new ArrayList<T>());
	}

	/**
	 * Searches all loaded scenes for the component with specified id and returns it.
	 * 
	 * @param id
	 * @return null if none found
	 */
	public LttlComponent findComponentByIdAllScenes(int id)
	{
		LttlComponent lc = null;
		for (LttlScene scene : getAllLoaded(true))
		{
			lc = scene.findComponentById(id);
			if (lc != null)
			{
				break;
			}
		}
		return lc;
	}

	/**
	 * Returns all the components in all the loaded scenes.
	 * 
	 * @return
	 */
	public ArrayList<LttlComponent> findAllComponents()
	{
		ArrayList<LttlComponent> allComponents = new ArrayList<LttlComponent>();
		for (LttlScene ls : Lttl.scenes.getAllLoaded(true))
		{
			allComponents.addAll(ls.getRef().componentMap.values());
		}
		return allComponents;
	}
}
