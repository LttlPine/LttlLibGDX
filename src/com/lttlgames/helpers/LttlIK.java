package com.lttlgames.helpers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.lttlgames.editor.Lttl;

public class LttlIK
{
	// TODO keep?
	static final float GRAVITY = 0;

	private final Bone[] bones;
	private Vector2 globalCoords = new Vector2();
	private Vector2 endPoint = new Vector2();
	private Vector2 diff = new Vector2();

	public LttlIK(Bone... bones)
	{
		this.bones = bones;
		globalCoords.set(bones[0].position);
	}

	static class Bone
	{
		final float len;
		final Vector2 position = new Vector2();
		final Vector2 inertia = new Vector2();

		public Bone(float x, float y, float len)
		{
			this.position.set(x, y);
			this.len = len;
		}
	}

	public void solveFakeIK(Vector2 target)
	{
		float gravity = Lttl.game.getFixedDeltaTime() * GRAVITY;
		endPoint.set(target);
		bones[0].position.set(endPoint);
		for (int i = 0; i < bones.length - 1; i++)
		{
			Bone bone = bones[i];
			endPoint.set(bone.position);
			diff.set(endPoint.x, endPoint.y).sub(bones[i + 1].position.x,
					bones[i + 1].position.y);
			diff.add(0, gravity);
			diff.add(bones[i + 1].inertia.x, bones[i + 1].inertia.y);
			diff.nor().scl(bones[i + 1].len);
			float x = endPoint.x - diff.x;
			float y = endPoint.y - diff.y;
			float delta = Gdx.graphics.getDeltaTime();
			bones[i + 1].inertia.add((bones[i + 1].position.x - x) * delta,
					(bones[i + 1].position.y - y) * delta).scl(0.99f);
			bones[i + 1].position.set(x, y);
		}
	}

	public Bone[] getBones()
	{
		return bones;
	}

}
