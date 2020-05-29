package com.lttlgames.editor;

import java.awt.Dimension;
import java.awt.GridBagConstraints;

import com.lttlgames.editor.annotations.GuiReadOnly;
import com.lttlgames.helpers.LttlTimeline;
import com.lttlgames.helpers.Vector2Array;

public class GuiComponentTimeline extends GuiComponentObject
{
	private GuiLttlChart chart;

	GuiComponentTimeline(ProcessedFieldType pft, Object hostObject, int index,
			GuiComponentObject parent)
	{
		super(pft, hostObject, index, parent);
	}

	@SuppressWarnings("serial")
	@Override
	void draw()
	{
		// create normal GuiComponentObject stuff
		if (checkDrawNull()) return;
		drawComponentFrameworkShared();
		drawComponentObjectShared();

		// disable popup menu if read only
		if (getAnnotationOnAncestorsAndSelf(GuiReadOnly.class) == null)
		{
			createPopupMenu();
		}

		updateUndoValue();
		chart = new GuiLttlChart(getValue(), getGuiStringData())
		{
			@Override
			void onChange()
			{
				if (shouldAutoUndo())
				{
					registerUndo(new UndoState(GuiComponentTimeline.this,
							new Vector2Array(getValue().getPoints())));
					updateUndoValue();
				}
				onEditorValueChange();
			}

		};
		chart.setValues(getValue().getPoints());
		chart.setPreferredSize(new Dimension(150, 50));
		chart.setToolTipText(getTooltipText());
		GridBagConstraints gbc = GuiHelper.GetGridBagConstraintsFieldValue();
		collapsableGroup.getCollapsePanel().add(chart, gbc);
	}

	@Override
	void processUndoRedo(Object value)
	{
		if (getValue() != null)
		{
			getValue().getPoints().clear();
			getValue().getPoints().addAll((Vector2Array) value);
			chart.setValues(getValue().getPoints());
			chart.repaint();
			updateUndoValue();
			processChangeCallback(1);
		}
	}

	@Override
	void checkNonGuiChanged()
	{
		super.checkNonGuiChanged();
		if (getValue() != null
				&& !chart.getValuesVector2Array()
						.equals(getValue().getPoints()))
		{
			chart.setValues(getValue().getPoints());
			chart.repaint();
		}
	}

	private void updateUndoValue()
	{
		undoValue = new Vector2Array(getValue().getPoints());
	}

	@Override
	LttlTimeline getValue()
	{
		return (LttlTimeline) super.getValue();
	}
}
