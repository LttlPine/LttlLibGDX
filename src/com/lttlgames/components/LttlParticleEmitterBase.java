package com.lttlgames.components;

import java.util.Iterator;

import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.lttlgames.components.LttlParticleEmitter.ParticleEmitterCallback;
import com.lttlgames.editor.DeltaTimeType;
import com.lttlgames.editor.Lttl;
import com.lttlgames.editor.LttlMultiObjectRenderer;
import com.lttlgames.editor.annotations.GuiButton;
import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.GuiCallbackDescendants;
import com.lttlgames.editor.annotations.GuiGroup;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.GuiShow;
import com.lttlgames.editor.annotations.GuiReadOnly;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.graphics.LttlShader;
import com.lttlgames.helpers.FloatRangeRandom;
import com.lttlgames.helpers.IntRange;
import com.lttlgames.helpers.LttlMath;
import com.lttlgames.helpers.LttlTimeline;

//46
/**
 * Only maintains the time elements (duration and delay) and the start and stop controls and actually emitting the
 * particles.
 * 
 * @author Josh
 * @param <T>
 */
@Persist(-90108)
public abstract class LttlParticleEmitterBase<T extends LttlParticleBase>
		extends LttlMultiObjectRenderer<T>
{
	/* STATIC */
	static protected Matrix4 tmpM4 = new Matrix4();
	static protected Matrix3 tmpM3a = new Matrix3();
	static protected Matrix3 tmpM3b = new Matrix3();

	public LttlParticleEmitterBase(Class<T> particleClass)
	{
		// set the LttlParticle class and max pool size
		super(particleClass);
	}

	/*** PUBLIC MEMEBERS ***/
	/* STATS */
	@GuiGroup("Stats")
	@GuiShow
	@GuiReadOnly
	private float percentComplete;

	/* EMITTER */
	@GuiGroup("Emitter")
	@Persist(9010800)
	@GuiMin(0)
	public IntRange countRange = new IntRange();
	{
		countRange.max = 20000;
	}
	@GuiGroup("Emitter")
	@Persist(9010801)
	@GuiMin(0)
	@GuiCallbackDescendants("restart")
	public FloatRangeRandom delayRangeRandom = new FloatRangeRandom();
	@GuiGroup("Emitter")
	@Persist(9010802)
	@GuiCallbackDescendants("restart")
	public FloatRangeRandom durationRangeRandom = new FloatRangeRandom();
	{
		durationRangeRandom.base = 3f;
	}

	/* SETTINGS */
	@GuiGroup("Settings")
	@Persist(90108033)
	public boolean inheritTransform = false;
	@GuiGroup("Settings")
	@Persist(90108041)
	public boolean startOnStart = false;
	@GuiGroup("Settings")
	@Persist(90108042)
	public boolean startContinousOnStart = false;
	@GuiGroup("Editor Settings")
	@Persist(9010806)
	@GuiCallback("guiStartContinousWhenSelected")
	public boolean startContinousWhenSelected = true;
	@GuiGroup("Editor Settings")
	@Persist(90108046)
	public boolean startContinousOnStartEditor = false;
	@GuiGroup("Editor Settings")
	@Persist(90108043)
	public boolean startOnClickWhileSelected = false;
	@GuiGroup("Editor Settings")
	@Persist(90108045)
	public boolean goToMouseOnDownWhileSelected = false;

	/* PRIVATE */
	private float delay, delayTimer;
	private float emissionDelta;
	private boolean continuous = false;
	private boolean done = true;
	private float duration, durationTimer;
	private ParticleEmitterCallback callback;
	private Matrix4 cachedWorldRenderMatrix;

	@Override
	public void onEditorCreate()
	{
		super.onEditorCreate();
		shader = LttlShader.TextureShader;
		autoUpdateMeshColorAlpha = false;
	}

	@Override
	public void onEditorUpdate()
	{
		if (!Lttl.game.inEditor()) return;
		super.onEditorUpdate();
		if (goToMouseOnDownWhileSelected && isFocusedInEditor()
				&& Lttl.input.isMouseDown())
		{
			t().setWorldPosition(Lttl.input.getMousePos());
		}
		if (startOnClickWhileSelected && isFocusedInEditor()
				&& Lttl.input.isMousePressed())
		{
			start();
		}
		if (startContinousWhenSelected)
		{
			if (!isPlaying() && isFocusedInEditor())
			{
				startContinous();
			}
			else if (isPlaying() && !isFocusedInEditor())
			{
				stop();
			}
		}

		// update
		updateGui();
		update(Lttl.game.getDeltaTime(DeltaTimeType.Fixed));
	}

	@Override
	public void onStart()
	{
		super.onStart();
		if (startContinousOnStart)
		{
			startContinous();
		}
		else if (startOnStart)
		{
			start();
		}
		else
		{
			restart();
		}
	}

	@Override
	public void onEditorStart()
	{
		super.onEditorStart();
		if (startContinousOnStartEditor)
		{
			startContinous();
		}
	}

	@Override
	public void onUpdate()
	{
		if (Lttl.game.inEditor())
		{
			updateGui();
		}
		update(Lttl.game.getDeltaTime(DeltaTimeType.Fixed));
	}

	/**
	 * This updates the emitter and particles. This exist to adjust where the updates happen (early,normal, or late, or
	 * at all). Default is {@link #onUpdate()}
	 * 
	 * @param delta
	 */
	final protected void update(float delta)
	{
		updateEmitterInternal(delta);
		updateParticlesInternal(delta);
	}

	private void updateParticlesInternal(float delta)
	{
		for (Iterator<T> it = getActivePooled().iterator(); it.hasNext();)
		{
			T particle = it.next();
			/* UPDATE PARTICLE */
			// remove if complete and do not render
			if (!updateParticle(particle, delta))
			{
				free(particle, it);
				continue;
			}
		}
	}

	/**
	 * This emits the number of particles. Does not need to be running or anything.
	 * 
	 * @param num
	 */
	public final void emit(int count)
	{
		// always restart so values are defined for these emitted particles, the only issue is when you have particles
		// that update based on emitter duration, not just their own life will this become an issue
		restart();
		emitInternal(count);
	}

	final private void emitInternal(int count)
	{
		count = LttlMath.min(count, countRange.max - getActivePooled().size);
		// count = LttlMath.max(count, countRange.min - activeCount);
		if (count <= 0) return;

		// create particles
		for (int i = 0; i < count; i++)
		{
			obtain();
		}
	}

	/**
	 * Used to initialize the particle emitter. Use this to define emitter values for this instance.<br>
	 * Called on {@link #emit(int)} and {@link #start()} or {@link #startContinous()}
	 */
	protected void restart()
	{
		delay = LttlMath.abs(delayRangeRandom.newValue());
		delayTimer = 0;

		// do not rest the durationTimer, since that should only happen start, stop, or continious

		// generate new duration
		// having duration be 0 is not good, since in the updateEmitter it never actually makes it to the part of
		// emitting particles based on deltaTime, and with 0 duration and continous we still want that
		// it also screws up any division by it, so better just do this
		duration = LttlMath.max(LttlMath.EPSILON,
				LttlMath.abs(durationRangeRandom.newValue()));
	}

	/**
	 * Updates the emitter. Only runs if it is playing. To get percent call {@link #getPercentComplete()}. This is
	 * called on this component's {@link #onUpdate()}. It already updates it's time and checks for completion and
	 * eveything. All you have to do is worry about modifying the emitter itself.
	 * 
	 * @param delta
	 * @return the emit count
	 */
	abstract protected int updateEmitter(float delta);

	private void updateEmitterInternal(float delta)
	{
		if (done)
		{
			if (callback != null && isClear())
			{
				callback.onClearAndComplete();
				callback = null;
			}
			return;
		}

		int activeCount = getActivePooled().size;

		// check for delay
		if (delayTimer < delay)
		{
			// waiting for delay
			delayTimer += delta;
		}
		else
		{
			// update elapsed time
			if (durationTimer < duration)
			{
				// still running
				durationTimer += delta;
			}
			else
			{
				// not running, decide if repeating or not
				if (!continuous)
				{
					done = true;
					if (callback != null)
					{
						callback.onComplete();
					}
					return;
				}
				else
				{
					// it's continous, so restart and maintain the durationTimer for accuracy
					durationTimer -= duration;
					if (callback != null)
					{
						callback.onRestart();
					}
					restart();
				}
			}

			// update the emitter, overidden, and gives us the emission count this frame
			int emissionCount = updateEmitter(delta);

			// actually emit particles
			emissionDelta += delta;
			if (emissionCount > 0)
			{
				float timePerParticle = 1f / emissionCount;
				// check to see if have enough time to do any emissions
				if (emissionDelta >= timePerParticle)
				{
					int emitCount = (int) (emissionDelta / timePerParticle);
					emitCount = LttlMath.min(emitCount, countRange.max
							- activeCount);
					emissionDelta %= timePerParticle;
					emitInternal(emitCount);
				}
			}
			// emit enough particles to reach min
			if (activeCount < countRange.min)
			{
				emitInternal(countRange.min - activeCount);
			}
		}
	}

	@Override
	protected void updateGui()
	{
		super.updateGui();
		percentComplete = getPercentComplete();
	}

	public int getParticleCount()
	{
		return getActivePooled().size;
	}

	/**
	 * Is this emitter player
	 * 
	 * @return
	 */
	public boolean isPlaying()
	{
		return !done;
	}

	/**
	 * This is really only valid if the emitter is playing
	 * 
	 * @return
	 */
	public boolean isContinous()
	{
		return continuous;
	}

	/**
	 * Are all the particles gone
	 * 
	 * @return
	 */
	public boolean isClear()
	{
		return getParticleCount() == 0;
	}

	public float getPercentComplete()
	{
		if (delayTimer < delay) return 0;
		return LttlMath.min(1, duration <= 0 ? 0 : durationTimer / duration);
	}

	@GuiButton(order = 0)
	/**
	 * Starts continous emitter, if already running resets time back to 0
	 */
	public void startContinous()
	{
		startContinous(null);
	}

	/**
	 * Starts continous emitter, if already running resets time back to 0
	 */
	public void startContinous(ParticleEmitterCallback callback)
	{
		continuous = true;
		start(callback);
	}

	@GuiButton(order = 1)
	/**
	 * Starts the emitter that will stop after the duration, if already running resets time back to 0
	 */
	public void start()
	{
		start(null);
	}

	/**
	 * Starts the emitter that will stop after the duration, if already running resets time back to 0.
	 */
	public void start(ParticleEmitterCallback callback)
	{
		this.callback = callback;
		done = false;
		emissionDelta = 0;
		durationTimer = 0;
		restart();
	}

	/**
	 * Updates the emitter and particles as if this amount of time had passed, does not start emitter or make any
	 * particles
	 * 
	 * @param time
	 */
	public void fastForward(float time)
	{
		float fixed = Lttl.game.getFixedDeltaTime();
		int iterations = (int) (time / fixed);
		float leftOver = time - (iterations * fixed);
		for (int i = 0; i < iterations; i++)
		{
			fastForwardInternal(fixed);
		}
		fastForwardInternal(leftOver);
	}

	private void fastForwardInternal(float delta)
	{
		updateEmitterInternal(delta);
		for (Iterator<T> it = getActivePooled().iterator(); it.hasNext();)
		{
			T particle = it.next();
			// remove if complete and do not render
			if (!updateParticle(particle, delta))
			{
				free(particle, it);
				continue;
			}
		}
	}

	@GuiButton(order = 2)
	/**
	 * Stops emitter where it's at, particles will finish out life if already emitted.<br>
	 * Could use {@link #clear()} to remove all particles.
	 */
	public void stop()
	{
		// this will end the emitter naturally when it updates next
		durationTimer = duration;
		// forces it to not restart it it was intending to
		continuous = false;
	}

	/**
	 * if playing in continous, this stops by letting emitter finish current cycle
	 */
	public void stopContinous()
	{
		continuous = false;
	}

	@GuiButton(order = 3)
	/**
	 * removes all particles, does not stop
	 */
	final public void clear()
	{
		getActivePooled().clear();
	}

	/**
	 * This is where the particle is updated. This is called on this components {@link #onUpdate()}.
	 * 
	 * @param particle
	 * @param delta
	 * @return true if still alive
	 */
	abstract protected boolean updateParticle(T particle, float delta);

	// updating and rendering here, so only iterate through particles once (slightly more efficient)
	// guarantees any modifications to the ParticleEmitter will be used

	@Override
	final protected void render()
	{
		// prepare for rendering
		if (getActivePooled().size > 0)
		{
			// if not premultiplying mesh, then get the world matrix to multiply each particle's local transform matrix
			// by
			if (!preMultiplyWorldMesh)
			{
				// generate the cachedWorldRenderMatrix, which includes camera and this worldTransformMatrix if global
				if (cachedWorldRenderMatrix == null)
				{
					cachedWorldRenderMatrix = new Matrix4();
				}
				getWorldMatrix(cachedWorldRenderMatrix, !inheritTransform);
			}

			// then iterate through each particle, update it
			// if it is still alive, then update the renderer's mesh or worldRendermatrix and render it
			for (T particle : getActivePooled())
			{
				/* RENDER PARTICLE */
				// before render
				prepareRender(particle);

				// update the alpha and color of mesh's local vertices
				getMesh().updateColorAlpha(particle.color,
						getWorldAlpha(particle));

				if (preMultiplyWorldMesh)
				{
					// premulity mesh's local vertices to generate world vertices
					// update mesh with world vertices (gets any updated alpha and color values too)
					if (inheritTransform)
					{
						getMesh().updateWorldVertices(
								tmpM3a.set(t().getWorldRenderTransform(false)).mul(
										getLocalTransformMatrix(particle,
												tmpM3b, false)));
					}
					else
					{
						getMesh()
								.updateWorldVertices(
										getLocalTransformMatrix(particle,
												tmpM3a, true));
					}

					// render, no matrix, just use world mesh
					renderDraw(null);
				}
				else
				{
					// render
					// This should really never happen, since it defies the pupose of a multiRender since each
					// particle has to be batched by itself, however, there may be some situations where it's faster, so
					// worth testing
					renderDraw(particle, cachedWorldRenderMatrix, tmpM4,
							!inheritTransform);
				}
			}
		}
	}

	/**
	 * Generates a transparency timeline with default values
	 * 
	 * @return
	 */
	static final public LttlTimeline newTransparencyTimeline()
	{
		return new LttlTimeline(0f, 0f, .3f, 1f, .7f, .8f, 1f, 0f);
	}

	@SuppressWarnings("unused")
	private void guiStartContinousWhenSelected()
	{
		if (!startContinousWhenSelected && t().isFocusedInEditor())
		{
			stop();
		}
	}

	final protected boolean guiCanNull()
	{
		clear();
		stop();
		return true;
	}
}
