package com.lttlgames.editor;

import java.io.File;

import org.apache.commons.io.FileUtils;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.lttlgames.helpers.LttlHelper;

abstract class LttlResourceManager
{
	LttlScene scene;
	private static String androidAssetPath;
	private static String projectName;

	/**
	 * Generates an Internal FileHandle for for the path given relative to platform's asset folders
	 * 
	 * @param path
	 *            realtive to asset folder for this platform (ie. "audio/2/someSound.wav")
	 * @return null if no file/directory could be found
	 */
	public static FileHandle getFileHandleInternalAssets(String path)
	{
		return getFileHandle(path, FileType.Internal, true);
	}

	/**
	 * Genrates a FileHandle {@link FileType#Classpath} for the path given relative to the LttlGame classpath.
	 * (readonly)
	 * 
	 * @param path
	 * @return
	 */
	public static FileHandle getFileHandleClass(String path)
	{
		return Gdx.files.classpath("com/lttlgames/assets/" + path);
	}

	/**
	 * Generates a FileHandle for for the path given.
	 * 
	 * @param path
	 *            inside asset or local folder for this platform already (ie. "audio/2/someSound.wav")
	 * @param fileType
	 *            does the FileHande need to support java's fileIO? [external/absolute only]
	 * @param inAssetsFolder
	 *            is this path relative to the assetsFolder, automatically true if using internal, otherwise it is
	 *            relative to project
	 * @return null if no file/directory could be found
	 */
	static FileHandle getFileHandle(String path, FileType fileType,
			boolean inAssetsFolder)
	{
		// Desktop Specific
		if (Gdx.app.getType() == ApplicationType.Desktop)
		{
			switch (fileType)
			{
				case Absolute:
					if (inAssetsFolder)
					{
						// uses the android assets folder
						return Gdx.files.absolute(getAndroidAssetsFolder()
								+ path);
					}
					else
					{
						// uses relative folder, which is probably the non platform project folder
						return Gdx.files.absolute(path);
					}
				case Internal:
					if (Lttl.game.inEditor()
							|| (LttlGameStarter.get() instanceof LttlLwjglStarter && ((LttlLwjglStarter) LttlGameStarter
									.get()).inEclipse()))
					{
						// if in the editor or a non editor build but still in eclipse, then link to the assets folder
						// in the android project. This is essentially thea same as Absolute inAssetsFolder
						return Gdx.files.internal(getAndroidAssetsFolder()
								+ path);
					}
					else
					{
						// if not in editor, then assume the assets folders is in same directory as final project
						// REMEMBER WILL NEED TO COPY ALL THE ASSETS FROM ANDROID PROJECT TO DESKTOP PROJECT WHEN
						// DISTRIBUTING
						return Gdx.files.internal(path);
					}
				default:
					break;
			}
		}

		switch (fileType)
		{
			case Absolute:
				return Gdx.files.absolute(path);
			case Classpath:
				return Gdx.files.classpath(path);
			case External:
				return Gdx.files.external(path);
			case Internal:
				return Gdx.files.internal(path);
			case Local:
				return Gdx.files.local(path);
			default:
				break;
		}

		return null;
	}

	static long getFolderSize(String path)
	{
		File folder = new File(path);
		try
		{
			return FileUtils.sizeOfDirectory(folder);
		}
		catch (IllegalArgumentException e)
		{
			// if a file was changed while getting size
			return System.currentTimeMillis();
		}
	}

	/**
	 * only works on desktop inside eclipse!!!
	 * 
	 * @return
	 */
	private static String getAndroidAssetsFolder()
	{
		if (androidAssetPath != null) return androidAssetPath;
		androidAssetPath = "../" + getProjectName() + "-android/assets/";
		if (!new File(androidAssetPath).isDirectory())
		{
			androidAssetPath = "";
			Lttl.logNote("Files: could not find assetFolder in eclipse!");
		}

		return androidAssetPath;
	}

	static String getProjectName()
	{
		if (projectName != null) return projectName;
		String currentFolder = new File("").getAbsolutePath();
		projectName = currentFolder.substring(
				(currentFolder.lastIndexOf("\\") + 1)).replace("-desktop", "");
		return projectName;
	}

	void destroy()
	{
	}

	/**
	 * Defines the host LttlSceneCore reference and sets up folder names
	 * 
	 * @param sceneCore
	 */
	void setup(LttlSceneCore sceneCore)
	{
		// save host scene reference
		this.scene = sceneCore.getLttlScene();
		// create folder names (now using scene id)
		setupFolderNames();
	}

	/**
	 * Defines the resource folders based on this scene. Called on ResourceManager setup.
	 */
	abstract void setupFolderNames();

	/**
	 * Disposes all resources. Done on scene executeUnload().
	 */
	void dispose()
	{
	}

	public static int getFolderFileNamesSum(FileHandle folder)
	{
		int sum = 0;
		for (FileHandle f : folder.list())
		{
			sum += LttlHelper.StringToByteSum(f.name());
		}
		return sum;
	}
}
