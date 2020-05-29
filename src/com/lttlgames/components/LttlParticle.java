package com.lttlgames.components;


public class LttlParticle extends LttlParticleBase
{
	protected float scaleLow, scaleHigh;
	protected float xOriginLow, xOriginHigh;
	protected float yOriginLow, yOriginHigh;
	protected float rotationLow, rotationHigh;
	protected float speedLow, speedHigh;
	protected float angleLow, angleHigh;
	/**
	 * This is starting angle stuff
	 */
	protected float angleCos, angleSin;
	protected float xForceLow, xForceHigh;
	protected float yForceLow, yForceHigh;
	/**
	 * only relevant if it is on a {@link #LttlParticleEmitterMultiTex}
	 */
	protected int texIndex;

	public LttlParticle()
	{

	}

	@Override
	public void reset()
	{
		// don't need to waste time resetting these since onNewObject() sets them, and if not, it's because they will
		// not be used
		// life = currentLife = 0;
		// scaleLow = scaleDiff = rotationLow = rotationDiff = speed = speedDiff = angle = angleDiff = angleCos =
		// angleSin = xForceLow = xForceHigh = yForceLow = yForceHigh;
		texIndex = 0;
		super.reset();
	}

	final public float getLifePercent()
	{
		return 1 - currentLife / life;
	}
}
