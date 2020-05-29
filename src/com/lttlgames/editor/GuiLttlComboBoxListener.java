package com.lttlgames.editor;

abstract class GuiLttlComboBoxListener
{
	/**
	 * A click or enter key has submitted a selection. May not have changed. If there is a SelectionChanged, it will
	 * fire before this.
	 * 
	 * @param gsoc
	 */
	public void selectionSubmitted(GuiSelectOptionContainer gsoc)
	{
	}

	/**
	 * The selection has changed by typing in combo box, using arrow keys, or clicking on an option.
	 * 
	 * @param gsoc
	 */
	public void selectionChanged(GuiSelectOptionContainer gsoc)
	{
	}
}
