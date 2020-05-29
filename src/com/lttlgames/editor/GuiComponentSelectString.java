package com.lttlgames.editor;

import java.awt.event.FocusEvent;
import java.util.ArrayList;

public abstract class GuiComponentSelectString extends GuiFieldObject<String>
{
	GuiLttlComboBox combo;
	ArrayList<GuiSelectOptionContainer> optionContainers;

	GuiComponentSelectString(ProcessedFieldType pft, Object hostObject,
			int index, GuiComponentObject parent)
	{
		super(pft, hostObject, index, parent);
	}

	@Override
	void draw()
	{
		if (checkDrawNull()) return;

		drawBeginningDefault();

		loadOptions();
		GuiSelectOptionContainer initial = null;
		for (GuiSelectOptionContainer gsoc : optionContainers)
		{
			if (gsoc.value.equals(objectRef))
			{
				initial = gsoc;
				break;
			}
		}
		combo = new GuiLttlComboBox(optionContainers, initial, false);
		// setSelected(objectRef);
		disableComponentFromAnnotation(combo);
		combo.addLttlActionListener(new GuiLttlComboBoxListener()
		{
			@Override
			public void selectionSubmitted(GuiSelectOptionContainer gsoc)
			{
				if (GuiComponentSelectString.this.shouldAutoUndo())
				{
					// if value is different on a submit, then when it was focused or last submit then create undo
					if (!undoValue.equals(getValue()))
					{
						registerUndo(new UndoState(
								GuiComponentSelectString.this));
					}

					// save new undo value
					undoValue = getValue();
				}
			}

			@Override
			public void selectionChanged(GuiSelectOptionContainer gsoc)
			{
				setValue((String) gsoc.value);
				onEditorValueChange();
			}
		});

		combo.addLttlFocusListener(new GuiLttlFocusListener()
		{
			@Override
			public void focusGained(FocusEvent e)
			{
				if (GuiComponentSelectString.this.shouldAutoUndo())
				{
					// save initial undo value
					undoValue = getValue();
				}
			}

			@Override
			public void focusLost(FocusEvent e)
			{
				if (GuiComponentSelectString.this.shouldAutoUndo())
				{
					// if different value from focus gained then create new undo state
					if (!undoValue.equals(getValue()))
					{
						registerUndo(new UndoState(
								GuiComponentSelectString.this));
					}
					undoValue = getValue();
				}
			}
		});

		// keeps it from resizing the whole scroll panel if it is long
		combo.setPreferredSize(GuiHelper.defaultFieldDimension);
		getPanel().add(combo, GuiHelper.GetGridBagConstraintsFieldValue());
	}

	@Override
	void updatePrimativeValue()
	{
		if (objectRef == null) return;

		setSelected(objectRef, true);
	}

	/**
	 * Sets the item as selected.
	 * 
	 * @param item
	 * @param silent
	 *            no callbacks from combobox
	 */
	void setSelected(Object item, boolean silent)
	{
		// have to find the gsoc object to set first
		for (GuiSelectOptionContainer gsoc : optionContainers)
		{
			if (gsoc.value.equals(item))
			{
				combo.setSelectedItem(gsoc, silent);
				break;
			}
		}
	}

	void loadOptions()
	{

	}
}
