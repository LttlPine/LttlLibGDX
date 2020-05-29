package com.lttlgames.components;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.lttlgames.editor.HandleRect;
import com.lttlgames.editor.Lttl;
import com.lttlgames.editor.LttlComponent;
import com.lttlgames.editor.LttlModeOption;
import com.lttlgames.editor.annotations.GuiButton;
import com.lttlgames.editor.annotations.Persist;

/**
 * This allows the camera to be controlled as if it was a LttlTransform. Position and rotation and zoom (scale.x) will
 * be transfered to the camera if this LttlComponent is enabled.
 * 
 * @author Josh
 */
@Persist(-9026)
public class LttlCameraController extends LttlComponent
{
	private static LttlCameraController singleton;

	@Persist(902607)
	public LttlModeOption enabled = LttlModeOption.Editor;
	@Persist(902601)
	public boolean position = true;
	@Persist(902602)
	public boolean rotation = true;
	@Persist(902603)
	public boolean zoom = true;
	@Persist(902604)
	public boolean updateWhenNotPlaying = true;
	/**
	 * Recommended to be set to false if updating camera manually, so you can minimize camera updates. Just be sure
	 * {@link #updateCamera()} is called after you modify this transforms position.
	 */
	@Persist(902605)
	public boolean updateWhenPlaying = true;
	/**
	 * Allows you to click to select the camera controller.
	 */
	@Persist(902606)
	public boolean drawQuickSelectIcon = true;

	private HandleRect handle;

	@Override
	public void onStart()
	{
		singleton = this;
	}

	@Override
	public void onEditorStart()
	{
		singleton = this;
	}

	public static LttlCameraController get()
	{
		return singleton;
	}

	@Override
	public void onEditorCreate()
	{
		t().setWorldPosition(Lttl.game.getCamera().position);
		t().setWorldScale(Lttl.game.getCamera().zoom,
				Lttl.game.getCamera().zoom);
		t().setWorldRotation(Lttl.game.getCamera().rotation);
	}

	@Override
	public void onUpdate()
	{
		if (enabled == LttlModeOption.Editor) return;
		process();
	}

	@Override
	public void onEditorUpdate()
	{
		if (enabled == LttlModeOption.Play) return;
		process();
	}

	private void process()
	{
		if ((!Lttl.game.isPlaying() && Lttl.game.inEditor() && updateWhenNotPlaying)
				|| (Lttl.game.isPlaying() && updateWhenPlaying))
		{
			updateCamera();
		}

		if (Lttl.game.inEditor())
		{
			if (drawQuickSelectIcon)
			{
				if (handle == null)
				{
					t().updateWorldValues();
					handle = new HandleRect(t().getWorldPosition(false), -999,
							new Vector2(20, 20), true, t().getWorldRotation(
									true), false, false, true, false,
							Color.RED, 0, null)
					{
						@Override
						public void onDown()
						{
							this.visible = false;
							Lttl.editor.getGui().getSelectionController()
									.setSelection(t());
						}
					};
					handle.register();
				}
				else
				{
					handle.visible = !Lttl.editor.isSelected(t());
					if (!handle.visible)
					{
						t().updateWorldValues();
						handle.position.set(t().getWorldPosition(false));
						handle.rotation = t().getWorldRotation(false);
					}
				}
			}
			else if (handle != null)
			{
				handle.unregister();
				handle = null;
			}
		}
	}

	/**
	 * Forces the camera to update right now based on host transform. It is best to call this right after you make an
	 * update to the transform position to be most accurate and efficient, and before any camera reliant components,
	 * like GuiContainer update.
	 */
	@GuiButton(tooltip = "Forces the camera to update right now based on host transform")
	public void updateCamera()
	{
		if (position || rotation || zoom)
		{
			t().updateWorldValues();

			if (position)
				Lttl.game.getCamera().position.set(t().getWorldPosition(false));
			if (rotation)
				Lttl.game.getCamera().rotation = t().getWorldRotation(false);
			if (zoom) Lttl.game.getCamera().zoom = t().getWorldScale(false).x;
		}
	}

	@Override
	public void onDestroyComp()
	{
		processOnDestroy();
	}

	@Override
	public void onEditorDestroyComp()
	{
		processOnDestroy();
	}

	private void processOnDestroy()
	{
		if (handle != null)
		{
			handle.unregister();
			handle = null;
		}
	}
}
