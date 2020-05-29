package com.lttlgames.helpers;

import com.badlogic.gdx.math.Vector2;
import com.lttlgames.editor.LttlCamera;
import com.lttlgames.editor.annotations.Persist;

//2
@Persist(-9051)
public class LttlCameraTransformState
{
	@Persist(905100)
	public Vector2 position = new Vector2();
	@Persist(905101)
	public float zoom;
	@Persist(905102)
	public float rotation;

	public LttlCameraTransformState()
	{
	}

	public static LttlCameraTransformState getTransformState(LttlCamera camera)
	{
		LttlCameraTransformState state = new LttlCameraTransformState();
		state.position.set(camera.position);
		state.zoom = camera.zoom;
		state.rotation = camera.rotation;
		return state;
	}
}
