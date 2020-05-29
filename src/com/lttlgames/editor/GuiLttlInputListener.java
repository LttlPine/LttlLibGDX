package com.lttlgames.editor;

import java.awt.event.KeyEvent;

abstract class GuiLttlInputListener
{
	public abstract void onKeyPressed(KeyEvent e);

	public abstract void onKeyReleased(KeyEvent e);
}
