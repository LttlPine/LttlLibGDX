package com.lttlgames.components;

import com.badlogic.gdx.graphics.Color;
import com.lttlgames.editor.Lttl;
import com.lttlgames.editor.LttlGradient;
import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.GuiCallbackDescendants;
import com.lttlgames.editor.annotations.GuiCanNull;
import com.lttlgames.editor.annotations.GuiGroup;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.GuiStringData;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.helpers.FloatRangeRandom;
import com.lttlgames.helpers.FloatRangeRandomTimeline;
import com.lttlgames.helpers.IntRangeRandomTimeline;
import com.lttlgames.helpers.LttlMath;
import com.lttlgames.helpers.LttlTimeline;

//NOTE not sure if this can be extended with an object that extends LttlParticle, may just need to cast when updating particles and have a constrcutor that gives the new class
//49
@Persist(-9014)
public class LttlParticleEmitter extends LttlParticleEmitterBase<LttlParticle>
{
	public LttlParticleEmitter()
	{
		super(LttlParticle.class);
	}

	public LttlParticleEmitter(Class<LttlParticle> particleClass)
	{
		super(particleClass);
	}

	/* STATIC */
	static protected final int UPDATE_SCALE = 1 << 0;
	static protected final int UPDATE_ANGLE = 1 << 1;
	static protected final int UPDATE_ROTATION = 1 << 2;
	static protected final int UPDATE_SPEED = 1 << 3;
	static protected final int UPDATE_XFORCE = 1 << 4;
	static protected final int UPDATE_YFORCE = 1 << 5;
	static protected final int UPDATE_COLOR = 1 << 6;
	static protected final int UPDATE_XORIGIN = 1 << 7;
	static protected final int UPDATE_YORIGIN = 1 << 8;

	/*** MEMEBERS ***/
	/* PUBLIC */
	@GuiGroup("Emitter")
	@Persist(901403)
	@GuiMin(0)
	@GuiStringData(value = "Duration", id = 0)
	@GuiCallbackDescendants("restart")
	public IntRangeRandomTimeline emissionRangeTimeline = new IntRangeRandomTimeline();
	{
		emissionRangeTimeline.high.base = 15;
	}
	@GuiGroup("Emitter")
	@Persist(901405)
	@GuiMin(0)
	@GuiStringData(value = "Duration", id = 0)
	@GuiCallbackDescendants("restart")
	public FloatRangeRandomTimeline lifeRangeTimeline = new FloatRangeRandomTimeline();
	{
		lifeRangeTimeline.high.base = 1;
	}
	@GuiGroup("Emitter")
	@Persist(901407)
	@GuiMin(0)
	@GuiStringData(value = "Duration", id = 0)
	@GuiCanNull("guiCanNull")
	@GuiCallbackDescendants("restart")
	public FloatRangeRandomTimeline lifeOffsetRangeTimeline;
	@GuiGroup("Emitter")
	@Persist(9014029)
	@GuiCanNull("guiCanNull")
	@GuiCallbackDescendants("restart")
	@GuiStringData(value = "Duration", id = 0)
	public FloatRangeRandomTimeline xOffsetRangeEmitterTimeline;
	@GuiGroup("Emitter")
	@Persist(9014030)
	@GuiCanNull("guiCanNull")
	@GuiCallbackDescendants("restart")
	@GuiStringData(value = "Duration", id = 0)
	public FloatRangeRandomTimeline yOffsetRangeEmitterTimeline;
	@GuiGroup(
	{ "Emitter", "Color" })
	@Persist(9014016)
	public ColorBlendMode emitterColorBlend = ColorBlendMode.None;
	/**
	 * can be used to blend with the particle color, or if no particleColor is set (null), then uses this emitterColor
	 */
	@GuiGroup(
	{ "Emitter", "Color" })
	@Persist(9014017)
	@GuiCanNull("guiCanNull")
	@GuiCallback("restart")
	public LttlGradient emitterColor;
	@GuiGroup(
	{ "Emitter", "Spawn Shape" })
	@Persist(9014010)
	@GuiCallback("guiSpawnShape")
	public EmitterShape spawnShape = EmitterShape.Point;
	@GuiGroup(
	{ "Emitter", "Spawn Shape" })
	@Persist(9014011)
	@GuiCanNull("guiCanNull")
	@GuiStringData(value = "Duration", id = 0)
	@GuiCallbackDescendants("restart")
	public FloatRangeRandomTimeline spawnWidthRangeTimeline;
	@GuiGroup(
	{ "Emitter", "Spawn Shape" })
	@Persist(9014013)
	@GuiCanNull("guiCanNull")
	@GuiStringData(value = "Duration", id = 0)
	@GuiCallbackDescendants("restart")
	public FloatRangeRandomTimeline spawnHeightRangeTimeline;

	/* PARTICLE */
	@GuiGroup("Particle")
	@Persist(9014018)
	@GuiMin(.001f)
	@GuiStringData(value = "Life", id = 0)
	@GuiCanNull("guiCanNull")
	public FloatRangeRandomTimeline scaleRangeTimeline = new FloatRangeRandomTimeline();
	{
		scaleRangeTimeline.high.base = 1;
		scaleRangeTimeline.low.base = .5f;
	}
	@GuiGroup("Particle")
	@Persist(9014019)
	@GuiCanNull("guiCanNull")
	@GuiStringData(value = "Life", id = 0)
	public FloatRangeRandomTimeline speedRangeTimeline = new FloatRangeRandomTimeline();
	{
		speedRangeTimeline.high.base = 35;
	}
	@GuiGroup("Particle")
	@Persist(9014021)
	@GuiCanNull("guiCanNull")
	@GuiStringData(value = "Life", id = 0)
	public IntRangeRandomTimeline angleRangeTimeline = new IntRangeRandomTimeline();
	{
		angleRangeTimeline.high.base = 90;
		angleRangeTimeline.high.random = 15;
	}
	@GuiGroup("Particle")
	@Persist(9014023)
	@GuiCanNull("guiCanNull")
	@GuiStringData(value = "Life", id = 0)
	public IntRangeRandomTimeline rotationRangeTimeline;
	@GuiGroup("Particle")
	@Persist(9014015)
	@GuiCanNull("guiCanNull")
	public LttlGradient particleColor = new LttlGradient();
	@GuiGroup("Particle")
	@Persist(9014036)
	@GuiStringData(value = "Life", id = 0)
	public LttlTimeline transparencyTimeline = newTransparencyTimeline();
	@GuiGroup(
	{ "Particle", "Force" })
	@Persist(9014025)
	@GuiCanNull("guiCanNull")
	@GuiStringData(value = "Life", id = 0)
	public FloatRangeRandomTimeline xForceRangeTimeline;
	@GuiGroup(
	{ "Particle", "Force" })
	@Persist(9014027)
	@GuiCanNull("guiCanNull")
	@GuiStringData(value = "Life", id = 0)
	public FloatRangeRandomTimeline yForceRangeTimeline;
	@GuiGroup(
	{ "Particle", "Origin" })
	@Persist(9014037)
	@GuiCanNull("guiCanNull")
	@GuiStringData(value = "Life", id = 0)
	public FloatRangeRandomTimeline xOriginRangeTimeline;
	@GuiGroup(
	{ "Particle", "Origin" })
	@Persist(9014039)
	@GuiCanNull("guiCanNull")
	@GuiStringData(value = "Life", id = 0)
	public FloatRangeRandomTimeline yOriginRangeTimeline;
	@GuiGroup(
	{ "Particle", "Offset" })
	@Persist(901408)
	@GuiCanNull("guiCanNull")
	public FloatRangeRandom xOffsetRangeRandom;
	@GuiGroup(
	{ "Particle", "Offset" })
	@Persist(901409)
	@GuiCanNull("guiCanNull")
	public FloatRangeRandom yOffsetRangeRandom;

	/* GLOBAL VALUES */
	// meant to be animated externally
	@GuiGroup("Global")
	@Persist(9014047)
	public float xGlobalForce = 0;
	@GuiGroup("Global")
	@Persist(9014048)
	public float yGlobalForce = 0;

	/* SETTINGS */
	/**
	 * if true, each life will be be based on new random values
	 */
	@GuiGroup("Settings")
	@Persist(9014032)
	public boolean uniqueLife = false;
	/**
	 * aligned with the angle
	 */
	@GuiGroup("Settings")
	@Persist(9014035)
	public boolean alignedAngle = false;
	/**
	 * angle is aligned with force direction
	 */
	@GuiGroup("Settings")
	@Persist(9014044)
	public boolean alignedVelocity = false;
	/**
	 * when particle is created, it's starting angle will start at the angle of the transform
	 */
	@GuiGroup("Settings")
	@Persist(9014046)
	public boolean startAtTransformAngle = false;
	@GuiGroup("Editor Settings")
	@Persist(9014049)
	public boolean drawSpawnShape = false;

	/* PRIVATE */
	private int emissionLow, emissionHigh;
	private float lifeLow, lifeHigh;
	private int updateFlags;
	private float xOffsetEmitter, yOffsetEmitter;

	private float lifeOffsetLow, lifeOffsetHigh;
	private float xOffsetEmitterLow, xOffsetEmitterHigh;
	private float yOffsetEmitterLow, yOffsetEmitterHigh;
	private float spawnWidthLow, spawnWidthHigh;
	private float spawnHeightLow, spawnHeightHigh;

	/* TEMP */
	protected static Color colorTemp = new Color();

	@Override
	protected boolean updateParticle(LttlParticle particle, float delta)
	{
		// update life and check if dead
		float life = particle.currentLife - delta;
		if (life <= 0) return false;
		particle.currentLife = life;

		float particlePercent = particle.getLifePercent();
		float emitterPercent = getPercentComplete();
		int updateFlags = this.updateFlags;

		// SCALE
		if ((updateFlags & UPDATE_SCALE) != 0)
		{
			float scale = scaleRangeTimeline.lerp(particle.scaleLow,
					particle.scaleHigh, particlePercent);
			particle.scale.set(scale, scale);
		}

		// ORIGIN (compensate position so it stays in visually same location)
		if ((updateFlags & UPDATE_XORIGIN) != 0)
		{
			float xOrigin = xOriginRangeTimeline.lerp(particle.xOriginLow,
					particle.xOriginHigh, particlePercent);
			particle.position.x -= xOrigin - particle.origin.x;
			particle.origin.x = xOrigin;
		}
		if ((updateFlags & UPDATE_YORIGIN) != 0)
		{
			float yOrigin = yOriginRangeTimeline.lerp(particle.yOriginLow,
					particle.yOriginHigh, particlePercent);
			particle.position.y -= yOrigin - particle.origin.y;
			particle.origin.y = yOrigin;
		}

		// FORCES and MOVEMENT (speed, angle, forces)
		float velocityX = xGlobalForce, velocityY = yGlobalForce, angle = 0;
		if ((updateFlags & UPDATE_SPEED) != 0)
		{
			float speed = speedRangeTimeline.lerp(particle.speedLow,
					particle.speedHigh, particlePercent) * delta;

			if ((updateFlags & UPDATE_ANGLE) != 0)
			{
				angle = angleRangeTimeline.lerpFloat(particle.angleLow,
						particle.angleHigh, particlePercent);
				velocityX += speed * LttlMath.cosDeg(angle);
				velocityY += speed * LttlMath.sinDeg(angle);
			}
			else
			{
				velocityX = speed * particle.angleCos;
				velocityY = speed * particle.angleSin;
			}
		}
		if ((updateFlags & UPDATE_XFORCE) != 0)
		{
			velocityX += xForceRangeTimeline.lerp(particle.xForceLow,
					particle.xForceHigh, particlePercent) * delta;
		}
		if ((updateFlags & UPDATE_YFORCE) != 0)
		{
			velocityY += yForceRangeTimeline.lerp(particle.yForceLow,
					particle.yForceHigh, particlePercent) * delta;
		}
		particle.position.add(velocityX, velocityY);

		// ROTATION
		float rotation = 0;
		if ((updateFlags & UPDATE_ROTATION) != 0)
		{
			rotation = rotationRangeTimeline.lerpFloat(particle.rotationLow,
					particle.rotationHigh, particlePercent);
		}
		if (alignedAngle)
		{
			rotation += -angle;
		}
		if (alignedVelocity)
		{
			rotation += LttlMath.atan2(velocityX, velocityY)
					* LttlMath.radiansToDegrees;
		}
		particle.rotation = rotation;

		// COLOR
		if ((updateFlags & UPDATE_COLOR) != 0)
		{
			particleColor.lerp(particlePercent, particle.color);
			if (emitterColor != null)
			{
				emitterColor.lerp(emitterPercent, colorTemp);
				ColorBlendMode.blend(emitterColorBlend, particle.color,
						colorTemp);
			}
		}
		else if (emitterColor != null)
		{
			// if not updating color, then just use the emitter color and do not blend
			emitterColor.lerp(emitterPercent, particle.color);
		}

		// TRANSPARENCY
		particle.alpha = transparencyTimeline.getValue(particlePercent);

		return true;
	}

	@Override
	protected void restart()
	{
		super.restart();

		// always active
		emissionLow = LttlMath.abs(emissionRangeTimeline.newLow());
		emissionHigh = LttlMath.abs(emissionRangeTimeline.newHigh(emissionLow));

		// always active
		lifeLow = LttlMath.abs(lifeRangeTimeline.newLow());
		lifeHigh = LttlMath.abs(lifeRangeTimeline.newHigh(lifeLow));

		// Defining property ranges for emitter for this instance (play)
		// these are used when creating new particles, they don't animate
		if (lifeOffsetRangeTimeline != null && lifeOffsetRangeTimeline.isActive)
		{
			lifeOffsetLow = lifeOffsetRangeTimeline.newLow();
			lifeOffsetHigh = lifeOffsetRangeTimeline.newHigh();
		}
		if (spawnWidthRangeTimeline != null && spawnWidthRangeTimeline.isActive
				&& spawnShape != EmitterShape.Point)
		{
			spawnWidthLow = spawnWidthRangeTimeline.newLow();
			spawnWidthHigh = spawnWidthRangeTimeline.newHigh(spawnWidthLow);
		}
		if (spawnHeightRangeTimeline != null
				&& spawnHeightRangeTimeline.isActive
				&& spawnShape != EmitterShape.Point)
		{
			spawnHeightLow = spawnHeightRangeTimeline.newLow();
			spawnHeightHigh = spawnHeightRangeTimeline.newHigh(spawnHeightLow);
		}
		// reset emitter offset to 0, so if not active, it's centered
		xOffsetEmitter = 0;
		if (xOffsetRangeEmitterTimeline != null
				&& xOffsetRangeEmitterTimeline.isActive)
		{
			xOffsetEmitterLow = xOffsetRangeEmitterTimeline.newLow();
			xOffsetEmitterHigh = xOffsetRangeEmitterTimeline
					.newHigh(xOffsetEmitterLow);
		}
		yOffsetEmitter = 0;
		if (yOffsetRangeEmitterTimeline != null
				&& yOffsetRangeEmitterTimeline.isActive)
		{
			yOffsetEmitterLow = yOffsetRangeEmitterTimeline.newLow();
			yOffsetEmitterHigh = yOffsetRangeEmitterTimeline
					.newHigh(yOffsetEmitterLow);
		}

		// particle flags
		updateFlags = 0;
		if (angleRangeTimeline != null && angleRangeTimeline.isActive)
		{
			// even if timeline has 1, animates
			updateFlags |= UPDATE_ANGLE;
		}
		if (speedRangeTimeline != null && speedRangeTimeline.isActive)
		{
			// even if timeline has 1, animates
			updateFlags |= UPDATE_SPEED;
		}
		if (scaleRangeTimeline != null && scaleRangeTimeline.isActive
				&& scaleRangeTimeline.timeline.size() > 1)
		{
			updateFlags |= UPDATE_SCALE;
		}
		if (rotationRangeTimeline != null && rotationRangeTimeline.isActive)
		{
			updateFlags |= UPDATE_ROTATION;
		}
		if (xForceRangeTimeline != null && xForceRangeTimeline.isActive)
		{
			// even if timeline has 1, animates
			updateFlags |= UPDATE_XFORCE;
		}
		if (yForceRangeTimeline != null && yForceRangeTimeline.isActive)
		{
			// even if timeline has 1, animates
			updateFlags |= UPDATE_YFORCE;
		}
		if (particleColor != null
				&& (particleColor.size() > 1 || emitterColor != null))
		{
			updateFlags |= UPDATE_COLOR;
		}
		if (xOriginRangeTimeline != null && xOriginRangeTimeline.isActive
				&& xOriginRangeTimeline.timeline.size() > 1)
		{
			updateFlags |= UPDATE_XORIGIN;
		}
		if (yOriginRangeTimeline != null && yOriginRangeTimeline.isActive
				&& yOriginRangeTimeline.timeline.size() > 1)
		{
			updateFlags |= UPDATE_YORIGIN;
		}
	}

	@Override
	protected void onNewObject(LttlParticle newObj)
	{
		super.onNewObject(newObj);

		float emitterPercent = getPercentComplete();
		int updateFlags = this.updateFlags;

		// life
		if (uniqueLife)
		{
			float low = Math.abs(lifeRangeTimeline.newLow());
			float high = Math.abs(lifeRangeTimeline.newHigh(low));
			newObj.currentLife = newObj.life = lifeRangeTimeline.lerp(low,
					high, emitterPercent);
		}
		else
		{
			newObj.currentLife = newObj.life = lifeRangeTimeline.lerp(lifeLow,
					lifeHigh, emitterPercent);
		}

		// some properties like scale and rotation are reset() prior to this via the pool on MultiObjectRenderer, so
		// dont' need to worry about setting default values if a propety does not exist

		// speed
		if ((updateFlags & UPDATE_SPEED) != 0)
		{
			newObj.speedLow = speedRangeTimeline.newLow();
			newObj.speedHigh = speedRangeTimeline.newHigh(newObj.speedLow);
			// don't need to set initial value because it is only used in updateParticle()
		}

		// angle
		float angle = 0;
		if (startAtTransformAngle)
		{
			angle = t().getWorldRotation(false);
		}
		if ((updateFlags & UPDATE_ANGLE) != 0)
		{
			// be sure to add any offsetted angle to low
			newObj.angleLow = angleRangeTimeline.newLow() + angle;
			newObj.angleHigh = angleRangeTimeline.newHigh(newObj.angleLow);
			angle = angleRangeTimeline.lerpFloat(newObj.angleLow,
					newObj.angleHigh, 0);
		}
		else
		{
			// this sets the inital direction if no angle animating
			newObj.angleCos = LttlMath.cosDeg(angle);
			newObj.angleSin = LttlMath.sinDeg(angle);
		}

		// scale
		if (scaleRangeTimeline != null && scaleRangeTimeline.isActive)
		{
			newObj.scaleLow = scaleRangeTimeline.newLow();
			newObj.scaleHigh = scaleRangeTimeline.newHigh(newObj.scaleLow);
			// set to initial value
			float scale = scaleRangeTimeline.lerp(newObj.scaleLow,
					newObj.scaleHigh, 0);
			newObj.scale.set(scale, scale);
		}

		// rotation
		float rotation = 0;
		if (rotationRangeTimeline != null && rotationRangeTimeline.isActive)
		{
			newObj.rotationLow = rotationRangeTimeline.newLow();
			newObj.rotationHigh = rotationRangeTimeline
					.newHigh(newObj.rotationLow);

			// set to initial value
			rotation = rotationRangeTimeline.lerpFloat(newObj.rotationLow,
					newObj.rotationHigh, 0);
		}
		// can be aligned even if rotation is not active, just sets rotation to 0
		if (alignedAngle)
		{
			rotation += angle;
		}
		newObj.rotation = rotation;

		// x force
		if (xForceRangeTimeline != null && xForceRangeTimeline.isActive)
		{
			newObj.xForceLow = xForceRangeTimeline.newLow();
			newObj.xForceHigh = xForceRangeTimeline.newHigh(newObj.xForceLow);
		}

		// y force
		if (yForceRangeTimeline != null && yForceRangeTimeline.isActive)
		{
			newObj.yForceLow = yForceRangeTimeline.newLow();
			newObj.yForceHigh = yForceRangeTimeline.newHigh(newObj.yForceLow);
		}

		// Spawn (set position)
		if (inheritTransform)
		{
			// set particle position to be relative to transform
			newObj.position.set(0, 0);
		}
		else
		{
			// set particle position to be relative to world
			// there is no guarantee for when this onNewObject will run, but if playing, then it will always be in
			// render loop
			newObj.position.set(t().getWorldPosition(false));
		}
		newObj.position.add(xOffsetEmitter, yOffsetEmitter);
		if (xOffsetRangeRandom != null)
		{
			newObj.position.x += xOffsetRangeRandom.newValue();
		}
		if (yOffsetRangeRandom != null)
		{
			newObj.position.y += yOffsetRangeRandom.newValue();
		}
		{
			float width = spawnWidthRangeTimeline != null
					&& spawnWidthRangeTimeline.isActive ? spawnWidthRangeTimeline
					.lerp(spawnWidthLow, spawnWidthHigh, emitterPercent) : 0;
			float height = spawnHeightRangeTimeline != null
					&& spawnHeightRangeTimeline.isActive ? spawnHeightRangeTimeline
					.lerp(spawnHeightLow, spawnHeightHigh, emitterPercent) : 0;
			switch (spawnShape)
			{
				case Elipse:
				case ElipseEdgesBoth:
				case ElipseEdgesBottom:
				case ElipseEdgesTop:
				{
					float radiusX = width / 2;
					float radiusY = height / 2;
					if (radiusX == 0 || radiusY == 0) break;
					float scaleY = radiusX / (float) radiusY;
					if (spawnShape != EmitterShape.Elipse)
					{
						// edges
						float spawnAngle;
						switch (spawnShape)
						{
							case ElipseEdgesTop:
								spawnAngle = -LttlMath.random(179f);
								break;
							case ElipseEdgesBottom:
								spawnAngle = LttlMath.random(179f);
								break;
							default:
								// both
								spawnAngle = LttlMath.random(360f);
								break;
						}
						float cosDeg = LttlMath.cosDeg(spawnAngle);
						float sinDeg = LttlMath.sinDeg(spawnAngle);
						newObj.position.x += cosDeg * radiusX;
						newObj.position.y += sinDeg * radiusX / scaleY;
						if ((updateFlags & UPDATE_ANGLE) == 0)
						{
							newObj.angleLow = spawnAngle;
							newObj.angleCos = cosDeg;
							newObj.angleSin = sinDeg;
						}
					}
					else
					{
						// inside
						float radius2 = radiusX * radiusX;
						while (true)
						{
							float px = LttlMath.random(width) - radiusX;
							float py = LttlMath.random(width) - radiusX;
							if (px * px + py * py <= radius2)
							{
								newObj.position.x += px;
								newObj.position.y += py / scaleY;
								break;
							}
						}
					}
					break;
				}
				case Line:
				{
					if (width != 0)
					{
						float lineX = width * LttlMath.random();
						newObj.position.x += lineX;
						newObj.position.y += lineX * (height / (float) width);
					}
					else
					{
						newObj.position.y += height * LttlMath.random();
					}
					break;
				}
				case Square:
				{
					newObj.position.x += LttlMath.random(width) - width / 2;
					newObj.position.y += LttlMath.random(height) - height / 2;
					break;
				}
				case Point:
				default:
					break;
			}

			// particle color
			if (particleColor != null)
			{
				// set initial color
				particleColor.lerp(0, newObj.color);
				if (emitterColor != null)
				{
					emitterColor.lerp(emitterPercent, colorTemp);
					ColorBlendMode.blend(emitterColorBlend, newObj.color,
							colorTemp);
				}
			}
			else if (emitterColor != null)
			{
				// if no particle color, then just use the emitter color and do not blend
				emitterColor.lerp(emitterPercent, newObj.color);
			}

			// origin (compensate position so it stays, visually, in same location)
			if (xOriginRangeTimeline != null && xOriginRangeTimeline.isActive)
			{
				newObj.xOriginLow = xOriginRangeTimeline.newLow();
				newObj.xOriginHigh = xOriginRangeTimeline
						.newHigh(newObj.xOriginLow);

				// set to initial value
				float xOrigin = xOriginRangeTimeline.lerp(newObj.xOriginLow,
						newObj.xOriginHigh, 0);
				newObj.position.x -= xOrigin;
				newObj.origin.x = xOrigin;
			}
			if (yOriginRangeTimeline != null && yOriginRangeTimeline.isActive)
			{
				newObj.yOriginLow = yOriginRangeTimeline.newLow();
				newObj.yOriginHigh = yOriginRangeTimeline
						.newHigh(newObj.yOriginLow);

				// set to initial value
				float yOrigin = yOriginRangeTimeline.lerp(newObj.yOriginLow,
						newObj.yOriginHigh, 0);
				newObj.position.y -= yOrigin;
				newObj.origin.y = yOrigin;
			}

			// life offset
			if (lifeOffsetRangeTimeline != null
					&& lifeOffsetRangeTimeline.isActive)
			{
				float offsetTime = lifeOffsetRangeTimeline.lerp(lifeOffsetLow,
						lifeOffsetHigh, emitterPercent);
				if (offsetTime > 0)
				{
					if (offsetTime >= newObj.currentLife)
					{
						offsetTime = newObj.currentLife - 1;
					}
					updateParticle(newObj, offsetTime);
				}
			}
		}
	}

	@Override
	protected void prepareRender(LttlParticle obj)
	{
	}

	/* GUI CALLBACKS */
	@SuppressWarnings("unused")
	private void guiSpawnShape()
	{
		switch (spawnShape)
		{
			case Elipse:
			case ElipseEdgesBoth:
			case ElipseEdgesBottom:
			case ElipseEdgesTop:
			case Line:
			case Square:
				if (spawnHeightRangeTimeline == null)
				{
					spawnHeightRangeTimeline = new FloatRangeRandomTimeline();
					spawnHeightRangeTimeline.high.base = 10;
				}
				if (spawnWidthRangeTimeline == null)
				{
					spawnWidthRangeTimeline = new FloatRangeRandomTimeline();
					spawnWidthRangeTimeline.high.base = 10;
				}
				break;
			default:
				break;

		}
	}

	@Override
	public void debugDraw()
	{
		super.debugDraw();

		if (drawSpawnShape)
		{
			float percent = getPercentComplete();
			float width = spawnWidthRangeTimeline != null
					&& spawnWidthRangeTimeline.isActive ? spawnWidthRangeTimeline
					.lerp(spawnWidthLow, spawnWidthHigh, percent) : 0;
			float height = spawnHeightRangeTimeline != null
					&& spawnHeightRangeTimeline.isActive ? spawnHeightRangeTimeline
					.lerp(spawnHeightLow, spawnHeightHigh, percent) : 0;
			switch (spawnShape)
			{
				case Elipse:
				case ElipseEdgesBoth:
				case ElipseEdgesBottom:
				case ElipseEdgesTop:
				{
					float radiusX = width / 2;
					float radiusY = height / 2;
					if (radiusX == 0 || radiusY == 0) break;
					Lttl.debug.drawElipseOutline(t().getWorldPosition(false).x
							+ xOffsetEmitter, t().getWorldPosition(false).y
							+ yOffsetEmitter, radiusX, radiusY, -1,
							Lttl.editor.getSettings().colorMeshBounding);
					break;
				}
				case Line:
				{
					if (width != 0)
					{
						Lttl.debug.drawLine(t().getWorldPosition(false).x
								+ xOffsetEmitter, t().getWorldPosition(false).y
								+ yOffsetEmitter, t().getWorldPosition(false).x
								+ xOffsetEmitter + width,
								t().getWorldPosition(false).y + yOffsetEmitter,
								0, Lttl.editor.getSettings().colorMeshBounding);
					}
					else
					{
						Lttl.debug.drawLine(t().getWorldPosition(false).x
								+ xOffsetEmitter, t().getWorldPosition(false).y
								+ yOffsetEmitter, t().getWorldPosition(false).x
								+ xOffsetEmitter, t().getWorldPosition(false).y
								+ yOffsetEmitter + height, 0,
								Lttl.editor.getSettings().colorMeshBounding);
					}
					break;
				}
				case Square:
				{
					Lttl.debug.drawRectOutline(t().getWorldPosition(false).x
							+ xOffsetEmitter, t().getWorldPosition(false).y
							+ yOffsetEmitter, width, height, 0,
							Lttl.editor.getSettings().colorMeshBounding);
					break;
				}
				case Point:
				default:
					break;
			}
		}
	}

	public abstract class ParticleEmitterCallback
	{
		public void onComplete()
		{
		}

		public void onRestart()
		{
		}

		public void onClearAndComplete()
		{
		}
	}

	@Override
	protected int updateEmitter(float delta)
	{
		// update emitter offset
		if ((xOffsetRangeEmitterTimeline != null && xOffsetRangeEmitterTimeline.isActive)
				|| (yOffsetRangeEmitterTimeline != null && yOffsetRangeEmitterTimeline.isActive))
		{
			float percent = getPercentComplete();
			if (xOffsetRangeEmitterTimeline != null
					&& xOffsetRangeEmitterTimeline.isActive)
			{
				xOffsetEmitter = xOffsetRangeEmitterTimeline.lerp(
						xOffsetEmitterLow, xOffsetEmitterHigh, percent);
			}
			if (yOffsetRangeEmitterTimeline != null
					&& yOffsetRangeEmitterTimeline.isActive)
			{
				yOffsetEmitter = yOffsetRangeEmitterTimeline.lerp(
						yOffsetEmitterLow, yOffsetEmitterHigh, percent);
			}
		}

		return emissionRangeTimeline.lerp(emissionLow, emissionHigh,
				getPercentComplete());
	}
}
