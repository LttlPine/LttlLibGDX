package com.lttlgames.editor;

import com.lttlgames.editor.annotations.GuiGroup;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.tweenengine.RepeatTweenType;

//11
@Persist(-9093)
class KeyframeOptions
{
	@Persist(909300)
	public boolean isRelative = false;
	@GuiGroup("Repeat")
	@Persist(909309)
	public RepeatTweenType repeatType = RepeatTweenType.None;
	/**
	 * If 0 or less, it does not repeat. No infinity option.
	 */
	@GuiGroup("Repeat")
	@Persist(9093010)
	@GuiMin(0)
	public int repeatCount = 0;
	@GuiGroup("Repeat")
	@Persist(9093011)
	@GuiMin(0)
	public float repeatDelay = 0;
	@GuiGroup("Noise")
	@Persist(909301)
	public boolean addNoise = false;
	@GuiGroup("Noise")
	@Persist(909302)
	public NoiseOptions noiseOptions = new NoiseOptions();
	@GuiGroup("Noise")
	@Persist(909303)
	public boolean generateNewNoiseEachIteration = false;
	@GuiGroup("Shake")
	@Persist(909304)
	public boolean addShake = false;
	@GuiGroup("Shake")
	@Persist(909305)
	public float shakeRangeBottom = -1;
	@GuiGroup("Shake")
	@Persist(909306)
	public float shakeRangeTop = 1;
	@GuiGroup("Shake")
	@Persist(909307)
	/**
	 * how often does it apply shake, don't want every frame
	 */
	@GuiMin(0)
	public float shakeUpdateRate = .5f;
	@GuiGroup("Shake")
	@Persist(909308)
	public EaseMode shakeType = EaseMode.InOut;
}
