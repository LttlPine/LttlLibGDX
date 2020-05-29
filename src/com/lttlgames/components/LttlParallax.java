package com.lttlgames.components;

import com.badlogic.gdx.math.Vector2;
import com.lttlgames.editor.Lttl;
import com.lttlgames.editor.LttlComponent;
import com.lttlgames.editor.annotations.GuiGroup;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.helpers.LttlMath;

@Persist(-90115)
public class LttlParallax extends LttlComponent
{
	@Persist(9011500)
	@GuiGroup("X Parallax")
	public boolean enableX = true;
	@Persist(9011501)
	@GuiGroup("X Parallax")
	public float originX = 0;
	@Persist(9011502)
	@GuiGroup("X Parallax")
	public boolean onStartDefineOriginX = true;
	@Persist(9011503)
	@GuiGroup("X Parallax")
	public float xSpeed = 1;
	@Persist(9011509)
	@GuiGroup("X Parallax")
	public float maxDistX = -1;

	@Persist(9011504)
	@GuiGroup("Y Parallax")
	public boolean enableY = true;
	@Persist(9011505)
	@GuiGroup("Y Parallax")
	public float originY = 0;
	@Persist(9011506)
	@GuiGroup("Y Parallax")
	public boolean onStartDefineOriginY = true;
	@Persist(9011507)
	@GuiGroup("Y Parallax")
	public float ySpeed = 1;
	@Persist(9011508)
	@GuiGroup("Y Parallax")
	public float maxDistY = -1;

	private static Vector2 cameraLocal = new Vector2();

	@Override
	public void onEditorCreate()
	{
		originX = t().position.x;
		originY = t().position.y;
	}

	@Override
	public void onStart()
	{
		if (onStartDefineOriginX)
		{
			originX = t().position.x;
		}
		if (onStartDefineOriginY)
		{
			originY = t().position.y;
		}
	}

	@Override
	public void onUpdate()
	{
		if (enableX || enableY)
		{
			t().worldToLocalPosition(
					cameraLocal.set(Lttl.game.getCamera().position), false);

			if (enableX)
			{
				if (maxDistX < 0
						|| maxDistX > LttlMath.abs(originX - cameraLocal.x))
				{
					t().position.x = originX + xSpeed
							* (originX - cameraLocal.x);
				}
			}

			if (enableY
					&& (maxDistY < 0 || maxDistY > LttlMath.abs(originY
							- cameraLocal.y)))
			{
				t().position.y = originY + ySpeed * (originY - cameraLocal.y);
			}
		}
	}
}
