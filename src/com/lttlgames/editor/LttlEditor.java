package com.lttlgames.editor;


/**
 * Stores all editor related functions
 * 
 * @author Josh
 */
/**
 * @author Josh
 */
public class LttlEditor
{
	private GuiController gui;
	private GuiInputController input;
	private GuiHandleManager handleController;
	private GuiUndoManager undoManager;

	/**
	 * Only create an editor object if inEditor. Should never need anything to do with LttlEditor when playing out of
	 * editor mode, if you do, it should be moved.
	 * 
	 * @param gui
	 */
	public LttlEditor(GuiController gui)
	{
		this.gui = gui;
		this.input = new GuiInputController();
		this.handleController = new GuiHandleManager();
		this.undoManager = new GuiUndoManager();
	}

	public GuiController getGui()
	{
		return gui;
	}

	public GuiInputController getInput()
	{
		return input;
	}

	public GuiHandleManager getHandleController()
	{
		return handleController;
	}

	public GuiUndoManager getUndoManager()
	{
		return undoManager;
	}

	public LttlEditorCamera getCamera()
	{
		return Lttl.game.getWorldCore().editorCamera;
	}

	public LttlEditorSettings getSettings()
	{
		return Lttl.scenes.getWorldCore().editorSettings;
	}

	/**
	 * Is the editor paused (in play mode but EVERYTHING is paused).
	 * 
	 * @return
	 */
	public boolean isPaused()
	{
		return Lttl.game.inEditor() && Lttl.loop.isEditorPaused;
	}

	/**
	 * Pauses (or unpauses) the editor, which makes EVERYTHING pause, ignores pauseable property.
	 */
	public void pauseToggle()
	{
		if (Lttl.editor.getGui().getStatusBarController().pauseButton
				.isEnabled())
		{
			Lttl.editor.getGui().getStatusBarController().pauseButton.doClick();
		}
	}

	/**
	 * Pauses editor (if not already, then doesn't do a step update, just pauses), but then allows one frame to update,
	 * then pauses again. pauseToggle() to play again.
	 */
	public void stepOneFrame()
	{
		if (Lttl.editor.getGui().getStatusBarController().stepButton
				.isEnabled())
		{
			Lttl.editor.getGui().getStatusBarController().stepButton.doClick();
		}
	}

	/**
	 * Updates Gui values (editor and handles), this is done before the main loop so any changed values reflect this
	 * frame (ie. if you want an object to stay on your mouse, you should update it before it renders, not afterward).
	 * Handles are not sent to {@link LttlDebug} until the renderLoop, so they will always be on top.
	 */
	void update()
	{
		// Update properties, selection, animation panels
		getGui().update();
		// Check Hotkeys, then check Selection change, Selection Move, Camera movement, Handles update, Zoom rect, check
		// right click menu
		getInput().update();
		// update selection debug draws after the input/handles have updated
		getGui().getSelectionController().update();
	}

	void debugDraw()
	{
		getGui().getSelectionController().debugDraw();
	}

	/**
	 * Resets the editor stuff that is not dependent on the game being fully loaded first
	 */
	void resetIndependent()
	{
		this.gui.initIndependent();
		this.undoManager.reset();
		this.input = new GuiInputController();
		this.handleController = new GuiHandleManager();
	}

	/**
	 * This resets all the editor stuff that is dependent on the game being fully loaded first
	 */
	void resetDependent()
	{
		this.gui.initDependent();
	}

	/**
	 * Returns if a LttlComponent (LttlTransform) is currently selected in the editor. Cache this per frame, try not to
	 * call multiple times in same frame.
	 * 
	 * @param comp
	 * @return
	 */
	public boolean isSelected(LttlComponent comp)
	{
		return getGui().getSelectionController().isSelected(comp.t());
	}

	void onPause()
	{
		LttlAudioManager.editorPauseAll();
	}

	void onResume()
	{
		LttlAudioManager.editorResumeAll();
	}
}
