package com.lttlgames.editor;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;

public final class LttlGame
{
	LttlWorldCore world;

	// helpers, need to start as null, and be created via the createShared function because of restarts and game modes
	LttlFontManager fontManager = null;

	GameState state = GameState.SETTINGUP;

	private boolean isPaused = false;
	private boolean inEditor = false;
	private boolean isPlaying = false;
	/**
	 * This is used to know if this is the first time the editor has loaded or not, if it is, then does some stuff
	 * differently.
	 */
	private int loadCount = 0;
	/**
	 * includes only unpaused game time and included timeFactor manipulation
	 */
	float gameTime = 0;
	float gameTimeFixed = 0;
	float fixedAccumulator = 0;
	/**
	 * includes all time program has been running and ignores timeFactor manipulation
	 */
	float rawGameTime = 0;
	int frameCount = 0;
	int fixedFrameCount = 0;
	int rawframeCount = 0;
	DisplayManager displayManager;

	LttlGame(boolean editor)
	{
		inEditor = editor;
	}

	void resize()
	{
		// don't process a resize (update cameras and stu
		if (state == GameState.SETTINGUP) return;

		displayManager.onResize();
		Lttl.loop.onResize();
	}

	void render()
	{
		if (state == GameState.SETTINGUP)
		{
			state = GameState.STARTING;
			firstIterationSetup();
		}

		state = GameState.STARTING;
		Lttl.loop.loop();
	}

	private void firstIterationSetup()
	{
		// if first iteration, then run a resize, since the screen has been initialized, this prevents
		// multiple resize() methods being called when the screen is still setting up, giving false screen dimensions
		// and onResize component callbacks
		resize();

		// run onStart for all loaded scenes once, need to do it here for same reasons, game just fully initialized
		Lttl.scenes.callBackScenes(ComponentCallBackType.onStart);
	}

	LttlWorldCore getWorldCore()
	{
		return world;
	}

	public LttlCamera getCamera()
	{
		return getWorldCore().camera;
	}

	public LttlCamera getEditorCamera()
	{
		return getWorldCore().editorCamera;
	}

	/**
	 * Playing mode (NOT IN EDITOR)
	 */
	void startPlayingMode()
	{
		// set mode
		isPlaying = true;

		// common procedures
		initShared();

		// null out editor camera
		getWorldCore().editorCamera = null;
	}

	/**
	 * Plays the game in the editor.</br> Temporary save all scenes first, adjust gui for play mode.
	 */
	void startPlayingInEditorMode()
	{
		// save all scenes to temporary folder, so we know what to reload back once done playing, since some scenes may
		// have been edited but not saved yet, and we don't want to overwrite any of the saved scenes even if there are
		// changes from editing in runtime
		Lttl.scenes.temporarySaveAllScenes();

		// change the mode
		isPlaying = true;

		// reset editor independent
		Lttl.editor.resetIndependent();

		// common procedures
		initShared();

		// reset editor dependent
		Lttl.editor.resetDependent();

		// load all editor scenes on start
		Lttl.scenes.loadOnStartEditorScenes();

		// maintain selection from editor mode
		Lttl.editor.getGui().getSelectionController().reselect();
	}

	/**
	 * Enters editor mode.
	 */
	void stopPlayingInEditorMode(boolean reselect)
	{
		// reset editor independent
		Lttl.editor.resetIndependent();

		// clean up any resources
		cleanUpResources();

		// load editor mode
		startEditorMode();

		// reset editor dependent
		Lttl.editor.resetDependent();

		if (reselect)
		{
			// maintain selection from editor mode
			Lttl.editor.getGui().getSelectionController().reselect();
		}
	}

	private void cleanUpResources()
	{
		// OPTIMIZE if in games with lots of textures and audio, if it lags for a long time when starting and stopping
		// playing, then try not disposing the scene's, instead start new scenes but transfer the Texture and
		// Sound/Music object instead of reloading, can all work the same but right before loading the resource, check
		// if it was already loaded previously and if so, don't load it
		// NOTE after a few tests, doesn't appear to make a difference if there are lots of resources to load
		for (LttlScene scene : Lttl.scenes.getAllLoaded(true))
		{
			scene.getAudioManager().dispose();
			scene.getTextureManager().dispose();
		}
		Lttl.loop.dispose();
	}

	/**
	 * Loads world scene, then loads initial scenes from scenesOnEditorStart, starts GuiController
	 */
	void startEditorMode()
	{
		// change the mode
		isPlaying = false;

		initShared();

		// if in editor, then loads onStartEditorScenes from temp folder, otherwise if it is playing out of editor just
		// load world
		Lttl.scenes.loadOnStartEditorScenes();
	}

	/**
	 * Creates all new instances of ScenesManager and LoopManager and loads world only and sets up camera. This run
	 * every time game is loaded (editor or not, regardless of play mode)
	 */
	private void initShared()
	{
		state = GameState.SETTINGUP;

		loadCount++;

		// reset game times
		rawGameTime = 0;
		gameTime = 0;
		gameTimeFixed = 0;

		// reset frames
		frameCount = 0;
		rawframeCount = 0;

		// create helpers
		Lttl.scenes = new ScenesManager();
		Lttl.loop = new LoopManager();
		Lttl.input = new LttlInput();
		Lttl.tween = new LttlTween();

		// import or create a new world (includes camera)
		world = Lttl.scenes.loadWorld();
		world.setup();

		// create post processing object
		Lttl.loop.createPostProcessingObject();

		// create display manager
		displayManager = new DisplayManager();
		// create font manager, which needs to happen after world scene is full loaded since the fonts are stored in
		// there
		fontManager = new LttlFontManager();
	}

	/**
	 * This just means the game is in play mode, does not reflect if game {@link #isPaused()}
	 * 
	 * @return
	 */
	public boolean isPlaying()
	{
		return isPlaying;
	}

	/**
	 * Checks if it is in the editor and desktop.
	 * 
	 * @return
	 */
	public boolean inEditor()
	{
		return inEditor && Gdx.app.getType() == ApplicationType.Desktop;
	}

	public boolean isPlayingEditor()
	{
		return isPlaying && inEditor();
	}

	public LttlGameSettings getSettings()
	{
		return world.gameSettings;
	}

	public PhysicsController getPhysics()
	{
		return world.physicsController;
	}

	/**
	 * Is game paused. Only non pauseable transform and their components will receive update and other callbacks.
	 * 
	 * @return
	 */
	public boolean isPaused()
	{
		return isPaused;
	}

	/**
	 * @see GameState#SETTINGUP
	 */
	public boolean isSettingUp()
	{
		return getState() == GameState.SETTINGUP;
	}

	/**
	 * This pauses the game.<br>
	 * All core update callbacks, tweens, and colliders' auto detect will be paused if they involve a paused component.
	 */
	public void pause()
	{
		isPaused = true;
	}

	/**
	 * Resumes a game from pause.
	 */
	public void resume()
	{
		isPaused = false;
	}

	/**
	 * The current state of the LttlEngine.
	 * 
	 * @return
	 */
	public GameState getState()
	{
		return state;
	}

	/**
	 * Returns the fixed delta time as defined in game settings multiplied by the timeFactor.
	 * 
	 * @return
	 */
	public float getFixedDeltaTime()
	{
		return getWorldCore().gameSettings.fixedDeltaTime
				* Lttl.game.getSettings().timeFactor;
	}

	/**
	 * Returns the smoothed delta time multiplied by the timeFactor.
	 * 
	 * @return
	 */
	public float getDeltaTime()
	{
		return Gdx.graphics.getDeltaTime() * Lttl.game.getSettings().timeFactor;
	}

	/**
	 * Returns the raw (non-smoothed) delta time multiplied by the timeFactor.
	 * 
	 * @return
	 */
	public float getRawDeltaTime()
	{
		return Gdx.graphics.getRawDeltaTime()
				* Lttl.game.getSettings().timeFactor;
	}

	public float getDeltaTime(DeltaTimeType type)
	{
		switch (type)
		{
			case Fixed:
				return getFixedDeltaTime();
			case Raw:
				return getRawDeltaTime();
			case Smooth:
			default:
				return getDeltaTime();
		}
	}

	/**
	 * The amount of time that has passed since the game started. This includes any time manipulation (timeFactor) and
	 * does not include the time during pauses.
	 * 
	 * @return
	 */
	public float getTime()
	{
		return gameTime;
	}

	/**
	 * The amount of time (using fixedDeltaTime) that has passed since the game started. This includes any time
	 * manipulation (timeFactor) and does not include the time during pauses. This can be called on
	 * {@link LttlComponent#onFixedUpdate()} to get the time relative to the fixedUpdate.
	 * 
	 * @return
	 */
	public float getTimeFixed()
	{
		return gameTimeFixed;
	}

	/**
	 * The amount of time that has passed since the game started. This does not includes any time manipulation
	 * (timeFactor) and does include the time during pauses.
	 * 
	 * @return
	 */
	public float getRawTime()
	{
		return rawGameTime;
	}

	/**
	 * Returns the number of frames (including the current) not including when paused.
	 * 
	 * @return
	 */
	public int getFrameCount()
	{
		return frameCount;
	}

	/**
	 * Returns the number of fixed frames (including the current) not including when paused.
	 * 
	 * @return
	 */
	public int getFixedFrameCount()
	{
		return fixedFrameCount;
	}

	/**
	 * Returns the number of frames (including the current) elapsed including when paused.
	 * 
	 * @return
	 */
	public int getRawFrameCount()
	{
		return rawframeCount;
	}

	/**
	 * True if LttlGame has only been created once or is in the process of being created once.
	 * 
	 * @return
	 */
	boolean isFirstLoad()
	{
		return loadCount <= 1;
	}

	int getLoadCount()
	{
		return loadCount;
	}

	/**
	 * Returns the number of screen resizes, including the initial screen size when game starts.
	 * 
	 * @return
	 */
	public int getResizeCount()
	{
		return Lttl.loop.resizeCount;
	}

	public LttlFontManager getFontManager()
	{
		return fontManager;
	}

	void dispose()
	{
	}

	public DisplayManager getDisplayManager()
	{
		return displayManager;
	}
}
