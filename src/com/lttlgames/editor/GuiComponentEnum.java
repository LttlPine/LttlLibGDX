package com.lttlgames.editor;

import java.awt.event.FocusEvent;
import java.util.ArrayList;

public class GuiComponentEnum extends GuiFieldObject<Enum<?>>
{
	GuiLttlComboBox combo;
	ArrayList<GuiSelectOptionContainer> optionContainers;

	GuiComponentEnum(ProcessedFieldType pft, Object hostObject, int index,
			GuiComponentObject parent)
	{
		super(pft, hostObject, index, parent);
	}

	@Override
	void draw()
	{
		if (checkDrawNull()) return;

		drawBeginningDefault();

		optionContainers = new ArrayList<GuiSelectOptionContainer>();
		GuiSelectOptionContainer initial = null;
		for (Enum<?> e : objectRef.getDeclaringClass().getEnumConstants())
		{
			GuiSelectOptionContainer gsoc = new GuiSelectOptionContainer(e,
					e.name());
			if (objectRef == e)
			{
				initial = gsoc;
			}
			optionContainers.add(gsoc);
		}

		combo = new GuiLttlComboBox(optionContainers, initial, true);

		disableComponentFromAnnotation(combo);
		combo.addLttlActionListener(new GuiLttlComboBoxListener()
		{

			@Override
			public void selectionSubmitted(GuiSelectOptionContainer gsoc)
			{
				if (GuiComponentEnum.this.shouldAutoUndo())
				{
					// if value is different on a submit, then when it was focused or last submit then create undo
					if (undoValue != getValue())
					{
						registerUndo(new UndoState(GuiComponentEnum.this));
					}
					undoValue = getValue();
				}
			}

			@Override
			public void selectionChanged(GuiSelectOptionContainer gsoc)
			{
				setValue((Enum<?>) gsoc.value);
				onEditorValueChange();
			}
		});

		combo.addLttlFocusListener(new GuiLttlFocusListener()
		{

			@Override
			public void focusLost(FocusEvent e)
			{
				if (GuiComponentEnum.this.shouldAutoUndo())
				{
					// if different value from focus gained then create new undo state
					if (undoValue != getValue())
					{
						registerUndo(new UndoState(GuiComponentEnum.this));
					}

					// save new undo value
					undoValue = getValue();
				}
			}

			@Override
			public void focusGained(FocusEvent e)
			{
				if (GuiComponentEnum.this.shouldAutoUndo())
				{
					// save initial undo value
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
		combo.setSelectedItem(objectRef, true);
	}

	/**
	 * Sets the item as selected.
	 * 
	 * @param item
	 */
	void setSelected(Object item)
	{
		for (GuiSelectOptionContainer gsoc : optionContainers)
		{
			if (gsoc.value == item)
			{
				combo.getModel().setSelectedItem(gsoc);
			}
		}
	}
}
