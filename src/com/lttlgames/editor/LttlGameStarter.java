package com.lttlgames.editor;

import com.badlogic.gdx.utils.IntMap;

public class LttlGameStarter
{
	private final LttlClassMap classMap;
	private final Class<? extends LttlProcessing> postProcessingClass;
	private static LttlGameStarter singleton;
	private int logLevel = 0;

	/**
	 * @param classMap
	 *            this is the object that extends LttlClassMap to initialize all the persisited classes for the game
	 *            project. Note: This needs to be in the game package since it uses the class to get the current
	 *            project's package name (used when getting all LttlComponents)
	 * @param postProcessingClass
	 *            this can be null, used to handle post processing.
	 * @param logLevel
	 *            [-1 none, 0 all, 1 note, 2 error]
	 */
	public LttlGameStarter(LttlClassMap classMap,
			Class<? extends LttlProcessing> postProcessingClass, int logLevel)
	{
		LttlGameStarter.singleton = this;

		Lttl.Throw(classMap);
		this.logLevel = logLevel;
		this.classMap = classMap;
		this.postProcessingClass = postProcessingClass;
	}

	/**
	 * Returns the static singleton of LttlGameStarter
	 * 
	 * @return
	 */
	public static LttlGameStarter get()
	{
		return singleton;
	}

	/**
	 * Returns the name of the package that the project is in. This maxes out at 3 packages deep.
	 * 
	 * @return
	 */
	public String getProjectPackageName()
	{
		if (classMap.packageName != null && !classMap.packageName.isEmpty()) { return classMap.packageName; }

		String name = classMap.getClass().getPackage().getName();
		String[] splits = name.split("\\.");
		if (splits.length > 3)
		{
			return splits[0] + "." + splits[1] + "." + splits[2];
		}
		else
		{
			return name;
		}
	}

	/**
	 * This is used to map classes to their persist ids. LttlEngine and the current game's classes should both be in
	 * here.
	 * 
	 * @return
	 */
	public IntMap<Class<?>> getClassMap()
	{
		return classMap.classMap;
	}

	protected Class<? extends LttlProcessing> getPostProcessingClass()
	{
		return postProcessingClass;
	}

	/**
	 * [-1 none, 0 all, 1 note, 2 error]
	 * 
	 * @return
	 */
	public int getLogLevel()
	{
		return logLevel;
	}
}
