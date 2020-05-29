package com.me.game.components;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.lttlgames.editor.Lttl;
import com.lttlgames.editor.LttlComponent;
import com.lttlgames.editor.annotations.GuiGroup;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.helpers.EaseType;
import com.lttlgames.helpers.LttlMath;

//15
@Persist(-1)
public class ProcessingHelper extends LttlComponent
{
	static private ProcessingHelper singleton;

	@Persist(101)
	public boolean postProcess = true;
	@Persist(106)
	public Vector2 range = new Vector2(10, 5);
	@Persist(100)
	@GuiGroup("Debug")
	public boolean drawDebug = true;
	@Persist(1010)
	@GuiGroup("Debug")
	public Color debugColor = new Color(Color.GREEN);
	@Persist(1015)
	@GuiGroup("Debug")
	public Color preMapColor = new Color(110 / 255f, 110 / 255f, 110 / 255f,
			81 / 255f);
	@Persist(1011)
	@GuiGroup("Debug")
	public float radius = .8f;
	@Persist(109)
	@GuiMin(1)
	@GuiGroup("Debug")
	public int stepsX = 10;
	@Persist(1012)
	@GuiMin(1)
	@GuiGroup("Debug")
	public int stepsY = 10;
	@Persist(103)
	public float xFactor = .5f;
	@Persist(104)
	public EaseType xEase = EaseType.CubicIn;
	@Persist(102)
	public float yFactor = .5f;
	@Persist(105)
	public EaseType yEase = EaseType.CubicIn;
	@Persist(107)
	public float yOffset = -2f;
	@Persist(108)
	public EaseType yOffsetEase = EaseType.CubicIn;
	@Persist(1013)
	public float yHorizon = 0;
	/**
	 * the amount of xOffset based on percentage and the distance above the yHorizon
	 */
	@Persist(1014)
	public float defaultXoffsetRate = 2f;

	@Override
	public void onEditorStart()
	{
		singleton = this;
	}

	@Override
	public void onStart()
	{
		singleton = this;
	}

	public static ProcessingHelper get()
	{
		return singleton;
	}

	/**
	 * Returns a value between -1 and 1 (0=center, -1=left edge, 1=right edge), interpolated linearly
	 * 
	 * @param worldX
	 * @param clamp
	 *            between -1 and 1
	 * @return
	 */
	public float getPercentage(float worldX, boolean clamp)
	{
		float p = (worldX - Lttl.game.getCamera().position.x) / range.x;
		return clamp ? LttlMath.clamp(p, -1, 1) : p;
	}

	/**
	 * Returns a value between -1 and 1 (0=center, -1=left edge, 1=right edge), interpolated based on the xEase
	 * 
	 * @param worldX
	 * @param clamp
	 * @return
	 */
	public float getPercentageInterp(float worldX, boolean clamp)
	{
		float percentage = getPercentage(worldX, clamp);
		return LttlMath.interp(-1, 1, (percentage + 1) / 2f, xEase);
	}
}
