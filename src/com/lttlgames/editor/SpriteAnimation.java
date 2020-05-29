package com.lttlgames.editor;

import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.helpers.LttlMath;

@Persist(-9024)
public class SpriteAnimation
{
	/**
	 * name of the animation, ie walking, running, idle
	 */
	@Persist(902401)
	public String state;

	@Persist(902402)
	public int startFrame = 0;

	@Persist(902403)
	public int endFrame = 0;

	@Persist(902404)
	public AnimationType animType = AnimationType.NORMAL;

	@Persist(902405)
	public float frameDuration = .1f;

	private boolean playing = false;
	private boolean paused = false;
	private float stateTime = 0;

	/**
	 * Returns the index of the frame.
	 * 
	 * @param deltaTime
	 * @return
	 */
	public int update(float deltaTime)
	{
		if (playing)
		{
			// PLAYING
			if (!paused)
			{
				stateTime += deltaTime;
			}
		}
		return getKeyFrameIndex();
	}

	/**
	 * Returns the frame number at the given time
	 * 
	 * @param time
	 * @return current frame number
	 */
	public int getKeyFrameIndex(float time)
	{
		int relativeFrame = (int) (time / frameDuration);
		int frameLength = endFrame - startFrame + 1;
		switch (animType)
		{
			case LOOP:
				relativeFrame = relativeFrame % frameLength;
				break;
			case LOOP_PINGPONG:
				relativeFrame = relativeFrame % ((frameLength * 2) - 2);
				if (relativeFrame >= frameLength)
					relativeFrame = frameLength - 2
							- (relativeFrame - frameLength);
				break;
			case LOOP_RANDOM:
				relativeFrame = LttlMath.random(frameLength - 1);
				break;
			case REVERSED:
				relativeFrame = LttlMath
						.max(frameLength - relativeFrame - 1, 0);
				break;
			case LOOP_REVERSED:
				relativeFrame = relativeFrame % frameLength;
				relativeFrame = frameLength - relativeFrame - 1;
				break;
			default:
			case NORMAL:
				relativeFrame = LttlMath.min(endFrame, relativeFrame);
				break;
		}

		// add start frame to make accurate
		return relativeFrame + startFrame;
	}

	/**
	 * Returns the current frame number.
	 * 
	 * @return current frame number
	 */
	public int getKeyFrameIndex()
	{
		return getKeyFrameIndex(stateTime);
	}

	public void start()
	{
		stateTime = 0;
		playing = true;
		paused = false;
	}

	public void stop()
	{
		stateTime = 0;
		playing = false;
		paused = false;
	}

	public void play()
	{
		playing = true;
		paused = false;
	}

	public void pause()
	{
		paused = true;
	}

	public boolean isPlaying()
	{
		return playing;
	}

	public boolean isPaused()
	{
		return paused;
	}

	public String getState()
	{
		return state;
	}
}