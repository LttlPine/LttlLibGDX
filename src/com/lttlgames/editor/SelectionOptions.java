package com.lttlgames.editor;

import com.lttlgames.editor.annotations.GuiToolTip;
import com.lttlgames.editor.annotations.Persist;

@Persist(-9050)
public class SelectionOptions
{
	@Persist(905001)
	@GuiToolTip("If a descendant is selected, this transform will be selected instead.")
	public boolean selectionGroup = false;
	@Persist(905002)
	@GuiToolTip("No descendant can be selected.")
	public boolean unselectableTree = false;
	@Persist(905003)
	@GuiToolTip("This transform can't be selected.")
	public boolean unselectableSelf = false;
}
