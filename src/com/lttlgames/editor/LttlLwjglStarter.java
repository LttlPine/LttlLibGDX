package com.lttlgames.editor;

import java.awt.EventQueue;

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.lttlgames.helpers.ProxyPrintStream;

public class LttlLwjglStarter extends LttlGameStarter
{
	private boolean isLogging = false;
	private boolean inEclipse = false;

	/**
	 * @param inEditor
	 *            runs in editor
	 * @param inEclipse
	 *            builds in eclipse still need to get assets from android folders not from internal
	 * @param isLogging
	 *            logs System.out and System.err to files, alternatively run the program from a .bat file if not
	 *            packaing to jar/exe
	 * @param cfg
	 * @see LttlGameStarter#LttlGameStarter(LttlClassMap, Class, int)
	 */
	public LttlLwjglStarter(final boolean inEditor, boolean inEclipse,
			boolean isLogging, int logLevel,
			final LwjglApplicationConfiguration cfg, LttlClassMap classMap,
			Class<? extends LttlProcessing> postProcessingClass)
	{
		super(classMap, postProcessingClass, logLevel);
		this.inEclipse = inEclipse;

		// setup logging
		this.isLogging = isLogging;
		if (this.isLogging)
		{
			System.setOut(new ProxyPrintStream(System.out, "stdout.log"));
			System.setErr(System.out);
		}

		if (inEditor)
		{
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
			{
				if ("Nimbus".equals(info.getName()))
				{
					try
					{
						UIManager.setLookAndFeel(info.getClassName());
					}
					catch (Throwable ignored)
					{
					}
					break;
				}
			}
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					new GuiController(new LttlGameListener(inEditor), cfg);
				}
			});
		}
		else
		{
			new LwjglApplication(new LttlGameListener(inEditor), cfg);
		}
	}

	public boolean isLogging()
	{
		return isLogging;
	}

	public boolean inEclipse()
	{
		return inEclipse;
	}
}
