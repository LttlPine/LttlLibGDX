package com.lttlgames.editor;

import java.awt.GridBagConstraints;

import com.lttlgames.editor.annotations.GuiReadOnly;

public class GuiComponentGradient extends GuiComponentObject
{
	GuiLttlGradient guiGradient;

	GuiComponentGradient(ProcessedFieldType pft, Object hostObject, int index,
			GuiComponentObject parent)
	{
		super(pft, hostObject, index, parent);
	}

	@SuppressWarnings("serial")
	@Override
	void draw()
	{
		GuiLttlCollapsableGroup oldCollapseGroup = collapsableGroup;

		// create normal GuiComponentObject stuff
		if (checkDrawNull()) return;
		drawComponentFrameworkShared();
		drawComponentObjectShared();

		// disable popup menu if read only
		if (getAnnotationOnAncestorsAndSelf(GuiReadOnly.class) == null)
		{
			createPopupMenu();
		}

		// this is GuiComponentGradient specific stuff
		if (oldCollapseGroup != null)
		{
			collapsableGroup.setCollapseState(oldCollapseGroup.isCollapsed());
		}

		undoValue = LttlCopier.copy(getValue());
		guiGradient = new GuiLttlGradient(getValue())
		{
			@Override
			public void onChange(boolean dragging)
			{
				if (guiGradient == null) return;

				// update LttlGradientValue
				updateLttlGradientObject();

				if (!dragging && shouldAutoUndo())
				{
					registerUndo(new UndoState(GuiComponentGradient.this,
							LttlCopier.copy(getValue())));
				}
				if (!dragging && shouldAutoUndo())
				{
					undoValue = LttlCopier.copy(getValue());
				}
				onEditorValueChange();
			}
		};
		GridBagConstraints gbc = GuiHelper.GetGridBagConstraintsFieldValue();
		gbc.insets.set(5, 0, 0, 0);
		collapsableGroup.getCollapsePanel().add(guiGradient, gbc);
	}

	private void updateLttlGradientObject()
	{
		getValue().guiSelectedIndex = guiGradient.getSelectedIndex();
		getValue().getColors().clear();
		getValue().getColors().addAll(guiGradient.getColors());
		getValue().getTimeline().clear();
		getValue().getTimeline().addAll(guiGradient.getTimeline());
		getValue().modified();
	}

	@Override
	void processUndoRedo(Object value)
	{
		super.processUndoRedo(value);
		updateLttlGradientObject();
	}

	@Override
	LttlGradient getValue()
	{
		return (LttlGradient) super.getValue();
	}
}
