package com.me.game;

import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.lttlgames.editor.LttlLwjglStarter;
import com.me.game.helpers.ClassMap;
import com.me.game.helpers.Processing;

public class Main
{
	public static void main(String[] args)
	{
		final LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
		cfg.title = "LttlDemo";
		cfg.initialBackgroundColor = Color.BLACK;
		// cfg.resizable = false;
		cfg.x = 0; // position
		cfg.y = 0;
		cfg.fullscreen = false;
		cfg.width = 1680 / 2;
		cfg.height = 1050 / 2;

		boolean inEclipse = true;
		int logLevel = 0;
		new LttlLwjglStarter(true, inEclipse, false, logLevel, cfg,
				new ClassMap(), Processing.class);
	}
}
