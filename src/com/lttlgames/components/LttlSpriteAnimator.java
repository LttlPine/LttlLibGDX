package com.lttlgames.components;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.lttlgames.editor.Lttl;
import com.lttlgames.editor.LttlComponent;
import com.lttlgames.editor.LttlRenderer;
import com.lttlgames.editor.LttlTextureAnimation;
import com.lttlgames.editor.SpriteAnimation;
import com.lttlgames.editor.annotations.ComponentRequired;
import com.lttlgames.editor.annotations.Persist;

@ComponentRequired(LttlRenderer.class)
@Persist(-9036)
public class LttlSpriteAnimator extends LttlComponent
{
	@Persist(903601)
	public LttlTextureAnimation lttlTexture = new LttlTextureAnimation();

	@Persist(903602)
	public ArrayList<SpriteAnimation> animations = new ArrayList<SpriteAnimation>();
	private SpriteAnimation currentAnimation;
	@Persist(903603)
	public boolean playFirstOnStart = false;
	@Persist(903604)
	public boolean useFixedDeltaTime = true;
	@Persist(903605)
	public boolean runInEditor = false;
	@Persist(903606)
	public boolean autoSetTextureToRenderer = true;

	@Override
	final public void onEditorStart()
	{
		if (runInEditor)
		{
			onStart();
		}
	}

	@Override
	final public void onStart()
	{
		if (playFirstOnStart && animations.size() > 0)
		{
			switchState(0);
		}
	}

	@Override
	final public void onEditorUpdate()
	{
		if (runInEditor)
		{
			onUpdate();
		}
	}

	@Override
	final public void onUpdate()
	{
		// updates the currentAnim
		if (currentAnimation != null)
		{
			currentAnimation.update(useFixedDeltaTime ? Lttl.game
					.getFixedDeltaTime() : Lttl.game.getDeltaTime());

			if (autoSetTextureToRenderer)
			{
				AtlasRegion ar = getKeyFrameAtlasRegion();
				if (ar != null)
				{
					transform().renderer().getTex0().setAR(ar);
				}
			}
		}
	}

	private void outOfRangeError()
	{
		Lttl.logError("getCurrentFrameAtlasRegion: The index of the atlas region for this frame is out of bounds.  Maybe check the SpriteAnimation and make sure it matches the number of frames.");
	}

	/**
	 * Get any animations AtlasRegion at the given animation time
	 * 
	 * @param animation
	 * @param time
	 * @return atlas region or null if out of range
	 */
	public AtlasRegion getKeyFrameAtlasRegion(SpriteAnimation animation,
			float time)
	{
		if (lttlTexture.getARs() != null)
		{
			int index = animation.getKeyFrameIndex(time);
			if (index >= lttlTexture.getARs().size())
			{
				outOfRangeError();
				return null;
			}
			return lttlTexture.getARs().get(index);
		}
		return null;
	}

	/**
	 * Returns the atlas region for the current frame, will return null if no current frame or if frame is out of index
	 * range
	 * 
	 * @return
	 */
	public AtlasRegion getKeyFrameAtlasRegion()
	{
		if (currentAnimation != null && lttlTexture.getARs() != null)
		{
			int index = getKeyFrameIndex();
			if (index >= lttlTexture.getARs().size())
			{
				outOfRangeError();
				return null;
			}

			return lttlTexture.getARs().get(index);
		}
		return null;
	}

	/**
	 * Returns the current keyframe index based on the currentAnimation
	 * 
	 * @return -1 if no current animation
	 */
	public int getKeyFrameIndex()
	{
		if (currentAnimation != null) { return currentAnimation
				.getKeyFrameIndex(); }
		return -1;
	}

	public void switchState(int index)
	{
		switchState(animations.get(index));
	}

	public void switchState(String state)
	{
		for (SpriteAnimation lsa : animations)
		{
			if (lsa.getState().equals(state))
			{
				switchState(lsa);
				return;
			}
		}
		Lttl.Throw("No state " + state + " on " + transform().getName());
	}

	/**
	 * Switches to the state and starts playing it. Stops previous state.
	 * 
	 * @param animIndex
	 */
	public void switchState(SpriteAnimation anim)
	{
		Lttl.Throw(anim);

		// stop any current animation
		if (currentAnimation != null)
		{
			currentAnimation.stop();
		}

		// set ands start new animation
		currentAnimation = anim;
		currentAnimation.start();
	}

	/**
	 * Pauses current animation
	 */
	public void pause()
	{
		if (currentAnimation != null)
		{
			currentAnimation.pause();
		}
	}

	/**
	 * Stops the curent state. It's like pause, but it resets.
	 */
	public void stop()
	{
		if (currentAnimation != null)
		{
			currentAnimation.stop();
		}
	}

	/**
	 * Plays the curent state from wherevere it is at (paused or stopped).
	 */
	public void play()
	{
		if (currentAnimation != null)
		{
			currentAnimation.play();
		}
	}
}
