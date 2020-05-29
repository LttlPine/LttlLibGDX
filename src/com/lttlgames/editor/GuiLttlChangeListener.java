package com.lttlgames.editor;

abstract class GuiLttlChangeListener
{
	/**
	 * <pre>
	 * onChange(int changeId)
	 * {
	 *   changeId == 0; //change by user
	 *   changeId == 1; //change by undo
	 *   changeId == 2; //found non gui change
	 * }
	 * </pre>
	 */
	abstract void onChange(int changeId);
}