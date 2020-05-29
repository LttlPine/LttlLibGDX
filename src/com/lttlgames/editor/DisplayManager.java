package com.lttlgames.editor;

/**
 * Handles primarily camera dimension changes.
 * 
 * @author Josh
 */
class DisplayManager
{
	int resizeCount = 0;

	/**
	 * This mainly updates camera(s). This handles the resizing of the game window, which are the two cameras in the
	 * editor or the single if not in editor.
	 */
	void onResize()
	{
		if (Lttl.game.inEditor())
		{
			// must resize play camera before editor camera
			Lttl.game.getCamera().onResize();

			// resize editor camera
			Lttl.editor.getCamera().onResize();
		}
		else
		{
			// just resize play camera
			Lttl.game.getCamera().onResize();
		}
		resizeCount++;
	}

	public int getResizeCount()
	{
		return resizeCount;
	}
}
