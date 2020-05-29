package com.lttlgames.editor;

import java.util.ArrayList;
import java.util.HashMap;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.lttlgames.editor.LttlObjectGraphCrawler.FieldsMode;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.helpers.LttlHelper;
import com.lttlgames.helpers.LttlProfiler;

/**
 * Holds AtlasRegion refernces to be called by LttlTextures.<br>
 * Hold Texture references to generated graphics textures. (TODO)<br>
 * Manages when game loses focus and textures are lost, might be automatic, idk (TODO)
 * 
 * @author Josh
 */
@Persist(-9045)
public final class LttlTextureManager extends LttlResourceManager
{
	private TextureAtlas atlas;
	/**
	 * All available textures in this scene (atlas and single)
	 */
	private ArrayList<String> textureNames;
	private ArrayList<String> animationTextureNames;
	private HashMap<String, AtlasRegion> nonAtlasTexturesMap;
	private HashMap<String, AtlasRegion> atlasTexturesMap;
	private HashMap<String, ArrayList<AtlasRegion>> atlasAnimationTexturesMap;

	/**
	 * This is for editor use only.
	 */
	FileHandle inputAtlasTexturesDirRelative;
	/**
	 * This is for editor use only.
	 */
	FileHandle outputAtlasTexturesDirRelative;
	FileHandle outputDirInternal;
	/**
	 * this will be used in play mode too, and is relative to platform's asset folder
	 */
	FileHandle nonAtlasTexturesDirInternal;
	/**
	 * This is only used by editor to check folder size.
	 */
	FileHandle nonAtlasTexturesDirRelative;
	private String packFileName = "pack";
	/**
	 * This is persisted so when game loads (in editor) and there are no changes to textures, it doesn't process atlas
	 * again
	 */
	@Persist(904501)
	private long lastTexturesToAtlasFolderSize = 0;
	@Persist(904502)
	private long lastNonAtlasTexturesFolderSize = 0;
	/**
	 * Used for detecting file name changes in a folder
	 */
	@Persist(904503)
	private int lastAtlasFilenameSum = 0;
	/**
	 * Used for detecting file name changes in a folder
	 */
	@Persist(904504)
	private int lastNonAtlasFilenameSum = 0;

	LttlTextureManager()
	{
	}

	private boolean firstLoad;

	void initialLoad()
	{
		firstLoad = true;

		if (!Lttl.game.inEditor())
		{
			// just load, don't check if change
			loadTextureAtlas();
			loadNonAtlasTextures();

			// update names
			getTextureNamesUpdate();
			getAnimationTextureNamesUpdate();
		}
		else
		{
			// will always load textures and atlas since first time
			loadAndBuildTextures(true, true);
		}

		/**
		 * No need to refresh references on initial load because all textures should auto refresh on start from their
		 * host components.
		 */
	}

	/**
	 * Defines the texture folders based on this scene.
	 */
	@Override
	void setupFolderNames()
	{
		outputDirInternal = getFileHandleInternalAssets("resources/"
				+ scene.getId() + "/atlas-textures");
		nonAtlasTexturesDirInternal = getFileHandleInternalAssets("resources/"
				+ scene.getId() + "/textures");

		if (Lttl.game.inEditor())
		{
			inputAtlasTexturesDirRelative = LttlResourceManager.getFileHandle(
					"../" + LttlResourceManager.getProjectName() + "/etc/"
							+ scene.getId() + "/textures-to-atlas",
					FileType.Absolute, false);
			outputAtlasTexturesDirRelative = getFileHandle(
					"resources/" + scene.getId() + "/atlas-textures",
					FileType.Absolute, true);
			nonAtlasTexturesDirRelative = getFileHandle(
					"resources/" + scene.getId() + "/textures",
					FileType.Absolute, true);
		}
	}

	/**
	 * Updates and returns a list of texture names (atlas and non atlas)
	 * 
	 * @return
	 */
	private ArrayList<String> getTextureNamesUpdate()
	{
		if (textureNames != null) return textureNames;

		textureNames = new ArrayList<String>();
		textureNames.addAll(atlasTexturesMap.keySet());
		textureNames.addAll(nonAtlasTexturesMap.keySet());

		return textureNames;
	}

	/**
	 * Updates and returns a list of animation texture names (atlas)
	 * 
	 * @return
	 */
	private ArrayList<String> getAnimationTextureNamesUpdate()
	{
		if (animationTextureNames != null) return animationTextureNames;

		animationTextureNames = new ArrayList<String>();
		animationTextureNames.addAll(atlasAnimationTexturesMap.keySet());

		return animationTextureNames;
	}

	/**
	 * returns a list of texture names on this scene. (this is useful for GUI dropdown)
	 * 
	 * @return
	 */
	public ArrayList<String> getTextureNames(boolean andWorld)
	{
		ArrayList<String> names = new ArrayList<String>(getTextureNamesUpdate());

		// if not the world scene, but wants the world too, then add world texture names
		if (andWorld && scene.getId() != Lttl.scenes.WORLD_ID)
		{
			names.addAll(Lttl.scenes.getWorld().getTextureManager()
					.getTextureNames(false));
		}
		return names;
	}

	/**
	 * returns a list of all animation texture names on this scene. (this is useful for GUI dropdown)
	 * 
	 * @return
	 */
	public ArrayList<String> getAnimationTextureNames(boolean andWorld)
	{
		ArrayList<String> names = new ArrayList<String>(animationTextureNames);

		// if not the world scene, but wants the world too, then add world texture names
		if (andWorld && scene.getId() != Lttl.scenes.WORLD_ID)
		{
			names.addAll(Lttl.scenes.getWorld().getTextureManager()
					.getAnimationTextureNames(false));
		}
		return names;
	}

	/**
	 * Returns a unique id for this texture (sceneId-textureIndex) by searching current scene
	 * 
	 * @param ar
	 * @return
	 */
	public String getTextureId(AtlasRegion ar)
	{
		if (ar.getTexture() == null) { return "N/A"; }

		int count = -1;

		// search atlas textures
		if (atlas != null)
		{
			for (Texture t : atlas.getTextures())
			{
				count++;
				if (t == ar.getTexture()) { return scene.getId() + "-" + count; }
			}
		}

		// search non atlas textures
		for (AtlasRegion ar1 : nonAtlasTexturesMap.values())
		{
			count++;
			if (ar.getTexture() == ar1.getTexture()) { return scene.getId()
					+ "-" + count; }
		}

		// did not find it
		return "ERROR";
	}

	/**
	 * Returns a unique id for this texture (sceneId-textureIndex) by searching all loaded scenes
	 * 
	 * @param ar
	 * @return
	 */
	public static String getTextureIdAll(AtlasRegion ar)
	{
		for (LttlScene ls : Lttl.scenes.getAllLoaded(true))
		{
			String id = ls.getTextureManager().getTextureId(ar);
			if (!id.equals("ERROR")) { return id; }
		}

		return "ERROR";
	}

	/**
	 * loads non atlas textures and rebuilds and loads atlas if change, if no change then it just loads each. Also
	 * updates names afterward.
	 * 
	 * @param checkAtlas
	 * @param checkNonAtlas
	 * @return if found changes, may not find changes but still load because first time
	 */
	boolean loadAndBuildTextures(boolean checkAtlas, boolean checkNonAtlas)
	{
		boolean modified = false;

		if (rebuildTexturePacker(checkAtlas))
		{
			Lttl.logNote("Building Atlas: atlas textures built on scene "
					+ scene.toString());
			loadTextureAtlas();
			modified = true;
		}
		else if (firstLoad)
		{
			// if not modified and first time, then still load
			loadTextureAtlas();
		}

		if (checkNonAtlasTextures(checkNonAtlas))
		{
			Lttl.logNote("Refresh Resources: Importing non atlas textures on scene "
					+ scene.toString());
			loadNonAtlasTextures();
			modified = true;
		}
		else if (firstLoad)
		{
			// if not modified and first time, then load them
			loadNonAtlasTextures();
		}

		// if loaded any (modified or first time) then update names map
		if (modified || firstLoad)
		{
			// update names
			textureNames = null;
			animationTextureNames = null;
			getTextureNamesUpdate();
			getAnimationTextureNamesUpdate();
		}

		firstLoad = false;
		return modified;
	}

	/**
	 * Checks and if changes rebuilds the texturePacker, does not load texures ONLY RAN IN EDITOR
	 * 
	 * @param checkIfModified
	 *            compares the size of the folder since the last run, if different, then runs again
	 * @return true if change (or if checkIfModified was false)
	 */
	private boolean rebuildTexturePacker(boolean checkIfModified)
	{
		if (!inputAtlasTexturesDirRelative.exists()
				|| !outputAtlasTexturesDirRelative.exists())
		{
			Lttl.Throw("The Input our Output Directory does not exist for atlas textures on scene "
					+ scene.getId());
			return false;
		}

		int currentAtlasFilenameSum = LttlResourceManager
				.getFolderFileNamesSum(inputAtlasTexturesDirRelative);
		long currentInputDirectorySize = getFolderSize(inputAtlasTexturesDirRelative
				.path());
		if (checkIfModified && currentAtlasFilenameSum == lastAtlasFilenameSum
				&& currentInputDirectorySize == lastTexturesToAtlasFolderSize)
		{
			lastAtlasFilenameSum = currentAtlasFilenameSum;
			lastTexturesToAtlasFolderSize = currentInputDirectorySize;
			return false;
		}
		lastAtlasFilenameSum = currentAtlasFilenameSum;
		lastTexturesToAtlasFolderSize = currentInputDirectorySize;

		TexturePacker.process(inputAtlasTexturesDirRelative.path(),
				outputAtlasTexturesDirRelative.path(), packFileName);
		return true;
	}

	/**
	 * checks all non atlas textures from textures folderm does not load them
	 * 
	 * @param checkIfModified
	 */
	private boolean checkNonAtlasTextures(boolean checkIfModified)
	{
		if (!nonAtlasTexturesDirRelative.exists())
		{
			Lttl.Throw("The non atlas textures directory does not exsist for scene "
					+ scene.getId());
			return false;
		}

		int currentNonAtlasFilenameSum = LttlResourceManager
				.getFolderFileNamesSum(nonAtlasTexturesDirRelative);
		long currentInputDirectorySize = getFolderSize(nonAtlasTexturesDirRelative
				.path());
		if (checkIfModified
				&& currentNonAtlasFilenameSum == lastNonAtlasFilenameSum
				&& currentInputDirectorySize == lastNonAtlasTexturesFolderSize)
		{
			lastNonAtlasFilenameSum = currentNonAtlasFilenameSum;
			lastNonAtlasTexturesFolderSize = currentInputDirectorySize;
			return false;
		}
		lastNonAtlasFilenameSum = currentNonAtlasFilenameSum;
		lastNonAtlasTexturesFolderSize = currentInputDirectorySize;
		return true;
	}

	private void loadNonAtlasTextures()
	{
		// need to dispose old ones
		disposeNonAtlasTextures();

		// creates fresh empty map
		if (nonAtlasTexturesMap == null)
		{
			nonAtlasTexturesMap = new HashMap<String, TextureAtlas.AtlasRegion>();
		}
		nonAtlasTexturesMap.clear();

		// convert each file in this folder to an atlas region and add to the map
		for (FileHandle fh : nonAtlasTexturesDirInternal.list())
		{
			String name = fh.nameWithoutExtension();

			// defaults
			Format format = Format.RGBA8888; // f
			TextureWrap textureWrapU = TextureWrap.ClampToEdge; // wu or w for both
			TextureWrap textureWrapV = TextureWrap.ClampToEdge; // wv or w for both
			TextureFilter texFilterMin = TextureFilter.Linear; // min
			TextureFilter texFilterMax = TextureFilter.Linear; // max
			boolean mipMaps = false; // mip-true

			// ie image_name==f-RGB565_w-MirroredRepeat_min-MipMapNearestLinear_max-Linear_mip-true

			// parse the name for import settings
			if (name.contains("=="))
			{
				String[] ss = name.split("==");
				name = ss[0];
				String properties = ss[1];
				for (String s : properties.split("_"))
				{
					if (s.startsWith("f-"))
					{
						format = Enum
								.valueOf(Format.class, s.replace("f-", ""));
					}
					else if (s.startsWith("w-"))
					{
						textureWrapU = Enum.valueOf(TextureWrap.class,
								s.replace("w-", ""));
						textureWrapV = textureWrapU;
					}
					else if (s.startsWith("wu-"))
					{
						textureWrapU = Enum.valueOf(TextureWrap.class,
								s.replace("wu-", ""));
					}
					else if (s.startsWith("wv-"))
					{
						textureWrapV = Enum.valueOf(TextureWrap.class,
								s.replace("wv-", ""));
					}
					else if (s.startsWith("min-"))
					{
						texFilterMin = Enum.valueOf(TextureFilter.class,
								s.replace("min-", ""));
					}
					else if (s.startsWith("max-"))
					{
						texFilterMax = Enum.valueOf(TextureFilter.class,
								s.replace("max-", ""));
					}
					else if (s.startsWith("mip-"))
					{
						mipMaps = s.replace("mip-", "").equals("true");
					}
					else
					{
						Lttl.Throw("Unknown texture property: " + s);
					}
				}
			}

			AtlasRegion ar = TextureToAtlasRegion(fh, format, textureWrapU,
					textureWrapV, texFilterMin, texFilterMax, mipMaps);
			ar.flip(false, true);
			ar.name = name;
			nonAtlasTexturesMap.put(name, ar);
		}

		// Lttl.logNote("Loading Textures: " + nonAtlasTexturesMap.size()
		// + " textures loaded on " + scene.getName() + "["
		// + scene.getId() + "]");
	}

	void dispose()
	{
		// dispose resources
		disposeNonAtlasTextures();
		disposeAtlasTextures();
	}

	/**
	 * ran on scene executeUnload
	 */
	private void disposeNonAtlasTextures()
	{
		if (nonAtlasTexturesMap != null)
		{
			for (AtlasRegion ar : nonAtlasTexturesMap.values())
			{
				ar.getTexture().dispose();
			}
		}
	}

	/**
	 * ran on scene executeUnload
	 */
	private void disposeAtlasTextures()
	{
		if (atlas != null)
		{
			atlas.dispose();
		}
	}

	/**
	 * Disposes old atlas if it exists and loads a new one, updates texture names, flips all atlas regions
	 * 
	 * @return
	 */
	private TextureAtlas loadTextureAtlas()
	{
		// dispose textures (if atlas exists)
		disposeAtlasTextures();

		// clear and make name maps, even if no atlas
		if (atlasTexturesMap == null)
		{
			atlasTexturesMap = new HashMap<String, TextureAtlas.AtlasRegion>();
		}
		atlasTexturesMap.clear();
		if (atlasAnimationTexturesMap == null)
		{
			atlasAnimationTexturesMap = new HashMap<String, ArrayList<AtlasRegion>>();
		}
		atlasAnimationTexturesMap.clear();

		// check to see if .pack file exists
		FileHandle packFileHandle = Gdx.files.internal(outputDirInternal.path()
				+ "/" + packFileName + ".atlas");
		if (packFileHandle == null || !packFileHandle.exists())
		{
			Lttl.logNote("Loading Texture Atlas: No texture atlas found on "
					+ scene.getName() + "[" + scene.getId() + "]");
			return null;
		}

		// get new atlas
		atlas = new TextureAtlas(packFileHandle);
		// populate the atlasTexturesMap with names and atlas regions
		// populate the atlasAnimationTexturesMap with names and atlas region arrays
		for (AtlasRegion ar : atlas.getRegions())
		{
			// check to see if this is a group of textures (animation)
			if (ar.index != -1)
			{
				if (!atlasAnimationTexturesMap.containsKey(ar.name))
				{
					atlasAnimationTexturesMap.put(ar.name, LttlHelper
							.ConvertToArrayList(atlas.findRegions(ar.name)));
				}
				continue;
			}

			if (atlasTexturesMap.containsKey(ar.name))
			{
				Lttl.logNote("Loading Texture Atlas: Two atlas regions have same name ("
						+ ar.name + ") on scene " + scene.getId());
				continue;
			}
			atlasTexturesMap.put(ar.name, ar);
		}

		// Lttl.logNote("Loading Atlas Textures: " + atlasTexturesMap.size()
		// + " atlas textures loaded on " + scene.getName() + "["
		// + scene.getId() + "]");

		// flip all atlas regions (just works better with coords)
		for (AtlasRegion ar : atlasTexturesMap.values())
		{
			ar.flip(false, true);
		}
		for (ArrayList<AtlasRegion> ars : atlasAnimationTexturesMap.values())
		{
			for (AtlasRegion ar : ars)
			{
				ar.flip(false, true);
			}
		}
		return atlas;
	}

	/**
	 * Finds an atlas region loaded on this scene (could be a nonAtlasTexture too)
	 * 
	 * @param name
	 *            up to first underscore
	 * @param checkWorld
	 *            should the world scene be checked if unable to find it on this scene?
	 * @return
	 */
	AtlasRegion findAtlasRegion(String name, boolean checkWorld)
	{
		LttlProfiler.textureRefreshes.add();

		AtlasRegion ar = null;

		// search atlas
		if (ar == null && atlas != null)
		{
			ar = atlas.findRegion(name);
		}
		// search the nonAtlasTextures
		if (ar == null && nonAtlasTexturesMap != null)
		{
			ar = nonAtlasTexturesMap.get(name);
		}
		// search world if this scene is not the world (already would have searched)
		if (checkWorld && ar == null && scene.getId() != Lttl.scenes.WORLD_ID)
		{
			ar = Lttl.scenes.getWorld().getTextureManager()
					.findAtlasRegion(name, false);
		}

		return ar;
	}

	/**
	 * Finds the atlas regions for an animation loaded on this scene.
	 * 
	 * @param name
	 *            up to first underscore
	 * @param checkWorld
	 *            should the world scene be checked if unable to find it on this scene?
	 * @return
	 */
	ArrayList<AtlasRegion> findAtlasRegions(String name, boolean checkWorld)
	{
		LttlProfiler.textureRefreshes.add();

		// search atlas
		ArrayList<AtlasRegion> list = atlasAnimationTexturesMap.get(name);

		// search world if this scene is not the world (already would have searched)
		if (checkWorld && list == null && scene.getId() != Lttl.scenes.WORLD_ID)
		{
			list = Lttl.scenes.getWorld().getTextureManager()
					.findAtlasRegions(name, false);
		}

		list = (list == null) ? null : new ArrayList<TextureAtlas.AtlasRegion>(
				list);

		return list;
	}

	/**
	 * Creates an atlas region from the path of an internal texture file. This is useful when you don't want something
	 * to be packed on to an atlas, but want it to be an atlas region to fit with rest of framework.
	 * 
	 * @param internalFile
	 * @param format
	 * @return
	 */
	public static AtlasRegion TextureToAtlasRegion(FileHandle internalFile,
			Format format)
	{
		return TextureToAtlasRegion(internalFile, format, TextureWrap.Repeat,
				TextureWrap.Repeat, TextureFilter.Linear, TextureFilter.Linear,
				false);
	}

	/**
	 * Creates an atlas region from the path of an internal texture file. This is useful when you don't want something
	 * to be packed on to an atlas, but want it to be an atlas region to fit with rest of framework.
	 * 
	 * @param internalFile
	 * @param format
	 * @param u
	 *            (TextureWrap)
	 * @param v
	 *            (TextureWrap)
	 * @param min
	 *            (TextureFilter)
	 * @param max
	 *            (TextureFilter)
	 * @param useMipMaps
	 * @return
	 */
	public static AtlasRegion TextureToAtlasRegion(FileHandle internalFile,
			Format format, TextureWrap u, TextureWrap v, TextureFilter min,
			TextureFilter max, boolean useMipMaps)
	{
		Texture texture = new Texture(internalFile, format, useMipMaps);
		texture.setWrap(u, v);
		texture.setFilter(min, max);
		return new AtlasRegion(texture, 0, 0, texture.getWidth(),
				texture.getHeight());
	}

	/**
	 * @param a
	 * @param b
	 * @return true if same texture
	 */
	public static boolean checkSameTexture(AtlasRegion a, AtlasRegion b)
	{
		return a.getTexture() == b.getTexture();
	}

	/**
	 * Returns all textures in all loaded scenes
	 * 
	 * @return
	 */
	public static ArrayList<AtlasRegion> getAllLoadedScenesTextures()
	{
		ArrayList<AtlasRegion> list = new ArrayList<AtlasRegion>();

		for (LttlScene ls : Lttl.scenes.getAllLoaded(true))
		{
			list.addAll(ls.getTextureManager().getAllTextures(false));
		}

		return list;
	}

	/**
	 * Returns all animation textures in all loaded scenes
	 * 
	 * @return
	 */
	public static ArrayList<ArrayList<AtlasRegion>> getAllLoadedScenesTextureAnimations()
	{
		ArrayList<ArrayList<AtlasRegion>> list = new ArrayList<ArrayList<AtlasRegion>>();

		for (LttlScene ls : Lttl.scenes.getAllLoaded(true))
		{
			list.addAll(ls.getTextureManager().getAllTextureAnimations(false));
		}

		return list;
	}

	/**
	 * Returns all the textures (atlas and non atlas) loaded in this scene.
	 * 
	 * @param andWorld
	 *            if true, also adds the world textures (if not world scene already)
	 * @return
	 */
	public ArrayList<AtlasRegion> getAllTextures(boolean andWorld)
	{
		ArrayList<AtlasRegion> list = new ArrayList<AtlasRegion>();

		for (AtlasRegion ar : atlasTexturesMap.values())
		{
			list.add(ar);
		}
		for (AtlasRegion ar : nonAtlasTexturesMap.values())
		{
			list.add(ar);
		}

		// add world textures if not world and want to
		if (andWorld && scene.getId() != Lttl.scenes.WORLD_ID)
		{
			for (AtlasRegion ar : Lttl.scenes.getWorld().getTextureManager().atlasTexturesMap
					.values())
			{
				list.add(ar);
			}
			for (AtlasRegion ar : Lttl.scenes.getWorld().getTextureManager().nonAtlasTexturesMap
					.values())
			{
				list.add(ar);
			}
		}

		return list;
	}

	/**
	 * Returns all the texture animations loaded in this scene.
	 * 
	 * @param andWorld
	 *            if true, also adds the world textures (if not world scene already)
	 * @return
	 */
	public ArrayList<ArrayList<AtlasRegion>> getAllTextureAnimations(
			boolean andWorld)
	{
		ArrayList<ArrayList<AtlasRegion>> list = new ArrayList<ArrayList<AtlasRegion>>();

		for (ArrayList<AtlasRegion> ars : atlasAnimationTexturesMap.values())
		{
			list.add(ars);
		}

		// add world texture animtaions if not world and want to
		if (andWorld && scene.getId() != Lttl.scenes.WORLD_ID)
		{
			for (ArrayList<AtlasRegion> ars : Lttl.scenes.getWorld()
					.getTextureManager().atlasAnimationTexturesMap.values())
			{
				list.add(ars);
			}
		}

		return list;
	}

	/**
	 * Refreshes all LttlTexture/LttlTextureAnimation in all scenes. This is expensive and should really only be done in
	 * editor when auto refreshing modified scene textures.<br>
	 * Also forces all generators to rebuild meshes.
	 */
	static void refreshAllReferences()
	{
		Lttl.logNote("Refreshing Texture References...");

		for (LttlScene scene : Lttl.scenes.getAllLoaded(true))
		{
			for (LttlTextureBase tex : ComponentHelper
					.getScenesComponentProperties(scene.getRef(),
							LttlTextureBase.class, true, true,
							FieldsMode.AllButIgnore))
			{
				if (LttlTexture.class == tex.getClass())
				{
					((LttlTexture) tex).refresh();
				}
				else if (LttlTextureAnimation.class == tex.getClass())
				{
					((LttlTextureAnimation) tex).refresh();
				}
			}
		}

		Lttl.logNote("Updating Meshes...");

		// update all meshes so they use the new textures
		for (LttlMeshGenerator mg : Lttl.scenes.findComponentsAllScenes(
				LttlMeshGenerator.class, true))
		{
			// only update those that already have a mesh
			if (mg.r().getMesh() != null)
			{
				mg.updateMesh();
			}
		}

		Lttl.logNote("Done.");
	}

	/**
	 * Finds LttlTexture/LttlTextureAnimation objects on components in OTHER scenes, and if their atlas region(s) is in
	 * this scene, then it clear it and posts a log, since this is usually not desirable
	 */
	void clearReferences()
	{
		Lttl.logNote("Clearing Texture References...");

		final ArrayList<AtlasRegion> arList = getAllTextures(false);
		final ArrayList<ArrayList<AtlasRegion>> animList = getAllTextureAnimations(false);

		// iterate through all components in all scenes except this one
		for (LttlScene scene : Lttl.scenes.getAllLoaded(true))
		{
			// skip own scene
			if (scene == this.scene)
			{
				continue;
			}

			for (LttlComponent comp : scene.getAllComponents())
			{
				for (LttlTextureBase tex : ComponentHelper
						.getComponentProperties(comp, LttlTextureBase.class,
								true, true, FieldsMode.AllButIgnore))
				{
					boolean cleared = false;
					if (LttlTexture.class == tex.getClass())
					{
						LttlTexture lt = ((LttlTexture) tex);
						if (arList.contains(lt.getAR()))
						{
							lt.clearReference();
							cleared = true;
						}
					}
					else if (LttlTextureAnimation.class == tex.getClass())
					{
						LttlTextureAnimation lta = ((LttlTextureAnimation) tex);
						if (animList.contains(lta.getARs()))
						{
							lta.clearReference();
							cleared = true;
						}
					}
					if (cleared)
					{
						Lttl.logNote("Clearing Texture References: reference cleared on "
								+ comp.toString());
					}
				}
			}
		}

		Lttl.logNote("Done.");
	}

	HashMap<String, ArrayList<AtlasRegion>> getAtlasAnimationTexturesHashmap()
	{
		return atlasAnimationTexturesMap;
	}
}
