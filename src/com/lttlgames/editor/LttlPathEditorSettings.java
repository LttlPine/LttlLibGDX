package com.lttlgames.editor;

import com.badlogic.gdx.graphics.Color;
import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.GuiGroup;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.Persist;

//13
@Persist(-9078)
public class LttlPathEditorSettings
{
	@GuiCallback("updateAll")
	@GuiMin(0)
	@Persist(907800)
	public float controlPointSize = 1.7f;
	@GuiCallback("updateAll")
	@GuiMin(0)
	@Persist(9078010)
	public float handleSize = 0.29f;
	/**
	 * how long are the handles in game units, does not affect the curvature if handles are not modified
	 */
	@Persist(9078011)
	@GuiCallback("updateAll")
	public float handleLength = .8f;
	@GuiMin(0)
	@Persist(907801)
	public float lineWidth = 0f;
	@GuiGroup("Color")
	@Persist(907805)
	public Color pathColor = new Color(Color.GRAY);
	@GuiGroup("Color")
	@Persist(9078013)
	public Color focusedPathColor = new Color(Color.PINK);
	@GuiGroup("Color")
	@Persist(907802)
	public Color controlPointDebugColor = new Color(0.5f, 0.5f, 0.5f, .15f);
	@GuiCallback("updateAll")
	@GuiGroup("Color")
	@Persist(907809)
	public Color controlPointEditorColor = new Color(1, 1, 0, .8f);
	@GuiCallback("updateAll")
	@GuiGroup("Color")
	@Persist(907803)
	public Color selectedControlPointColor = new Color(0, 0, 1, .9f);
	@GuiCallback("updateAll")
	@GuiGroup("Color")
	@Persist(907804)
	public Color handleColor = new Color(0.5f, 0, 0, 0.9f);
	@GuiGroup("Color")
	@Persist(907806)
	public Color handleLockedLineColor = new Color(Color.DARK_GRAY);
	@GuiGroup("Color")
	@Persist(907807)
	public Color rightHandleLineColor = new Color(Color.CYAN);
	@GuiGroup("Color")
	@Persist(907808)
	public Color leftHandleLineColor = new Color(Color.ORANGE);
	@GuiGroup("Color")
	@Persist(9078012)
	public Color pathPointsColor = new Color(0, 0, 0, .5f);

	@SuppressWarnings("unused")
	private void updateAll()
	{
		for (LttlPath lp : Lttl.scenes.findComponentsAllScenes(LttlPath.class,
				true))
		{
			if (lp.isEnabled() && lp.isEditing())
			{
				lp.editorInitHandles();
			}
		}
	}
}
