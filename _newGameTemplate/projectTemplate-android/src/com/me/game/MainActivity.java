package com.me.game;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.lttlgames.editor.LttlGameListener;
import com.lttlgames.editor.LttlGameStarter;
import com.me.game.helpers.ClassMap;
import com.me.game.helpers.Processing;

public class MainActivity extends AndroidApplication
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();
		cfg.useAccelerometer = false;
		cfg.useCompass = false;

		new LttlGameStarter(new ClassMap(), Processing.class, 0);
		initialize(new LttlGameListener(), cfg);
	}
}