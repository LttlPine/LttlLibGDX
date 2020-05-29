package com.lttlgames.components;

import com.lttlgames.editor.Lttl;
import com.lttlgames.editor.LttlCamera;
import com.lttlgames.editor.LttlComponent;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.helpers.LttlMath;

/**
 * Makes a container to put GUI transforms in. The children do not need this component. The transform position,
 * rotation, and scale for this object will be overrided.
 * 
 * @author Josh
 */
@Persist(-9034)
public class LttlGuiContainerComponent extends LttlComponent
{
	@Persist(903401)
	public boolean playCamera = true;
	@Persist(903402)
	public GuiContainerAnchor anchor = GuiContainerAnchor.Center;

	public void onEditorLateUpdate()
	{
		onLateUpdate();
	}

	/**
	 * This should be ran manually if the camera is changed after this lateUpdate(), which is unlikely.
	 */
	public void onLateUpdate()
	{
		if (!playCamera && !Lttl.game.inEditor()) return;

		LttlCamera camera = (playCamera) ? Lttl.game.getCamera() : Lttl.editor
				.getCamera();

		if (anchor == GuiContainerAnchor.Center)
		{
			transform().position.set(camera.position);
		}
		else
		{
			switch (anchor)
			{
				case LeftBottom:
					transform().position.set(camera.getLeft(),
							camera.getBottom());
					break;
				case LeftTop:
					transform().position.set(camera.getLeft(), camera.getTop());
					break;
				case RightBottom:
					transform().position.set(camera.getRight(),
							camera.getBottom());
					break;
				case RightTop:
					transform().position
							.set(camera.getRight(), camera.getTop());
					break;
				case BottomCenter:
					transform().position.set(camera.position.x,
							camera.getBottom());
					break;
				case LeftCenter:
					transform().position.set(camera.getLeft(),
							camera.position.y);
					break;
				case RightCenter:
					transform().position.set(camera.getRight(),
							camera.position.y);
					break;
				case TopCenter:
					transform().position
							.set(camera.position.x, camera.getTop());
					break;
				case Center:
				default:
					break;

			}

			if (camera.rotation != 0)
			{
				LttlMath.RotateAroundPoint(transform().position.x,
						transform().position.y, camera.position.x,
						camera.position.y, camera.rotation,
						transform().position);
			}
		}
		transform().rotation = camera.rotation;
		transform().scale.set(1 / camera.zoom, 1 / camera.zoom);
	}
}
