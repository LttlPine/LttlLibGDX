package com.lttlgames.editor;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

/**
 * Avoids having to overide everything
 * 
 * @author Josh
 */
public abstract class GuiComponentListener implements ComponentListener
{
	/**
	 * Invoked when the component's size changes.
	 */
	public void componentResized(ComponentEvent e)
	{
	}

	/**
	 * Invoked when the component's position changes.
	 */
	public void componentMoved(ComponentEvent e)
	{
	}

	/**
	 * Invoked when the component has been made visible.
	 */
	public void componentShown(ComponentEvent e)
	{
	}

	/**
	 * Invoked when the component has been made invisible.
	 */
	public void componentHidden(ComponentEvent e)
	{
	}
}
