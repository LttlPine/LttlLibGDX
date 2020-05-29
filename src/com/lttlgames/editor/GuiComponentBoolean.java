package com.lttlgames.editor;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JToggleButton;
import javax.swing.JToggleButton.ToggleButtonModel;

import com.lttlgames.editor.annotations.GuiToggleOnly;

public class GuiComponentBoolean extends GuiFieldObject<Boolean>
{
	JToggleButton toggle;
	private boolean skipCallback = false;

	GuiComponentBoolean(ProcessedFieldType pft, Object hostObject, int index,
			GuiComponentObject parent)
	{
		super(pft, hostObject, index, parent);
	}

	@Override
	void draw()
	{
		drawBeginningDefault();

		toggle = new JToggleButton("", getValue());
		disableComponentFromAnnotation(toggle);
		toggle.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// this means this callback was triggered by checking the field value
				if (skipCallback)
				{
					skipCallback = false;
					return;
				}
				if (shouldAutoUndo())
				{
					// save undoValue
					undoValue = getValue();
				}

				setValue(getModel().isSelected());

				if (shouldAutoUndo())
				{
					// create undo
					registerUndo(new UndoState(GuiComponentBoolean.this));
				}

				onEditorValueChange();
			}
		});
		getPanel().addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (e.getButton() == 1)
				{
					toggle.doClick();
				}
			}
		});
		GridBagConstraints gbc = GuiHelper.GetGridBagConstraintsFieldValue();
		gbc.fill = GridBagConstraints.NONE;
		getPanel().add(toggle, gbc);

		if (pft.getField().isAnnotationPresent(GuiToggleOnly.class))
		{
			toggle.setText(getLabelText());
			toggle.setPreferredSize(new Dimension(
					toggle.getText().length() * 8 + 10, 20));
			GuiHelper.SetFontSize(toggle, 10);
			getPanel().remove(label);
		}
		else
		{
			toggle.setPreferredSize(new Dimension(20, 20));
		}
	}

	private ToggleButtonModel getModel()
	{
		return (ToggleButtonModel) toggle.getModel();
	}

	@Override
	void updatePrimativeValue()
	{
		skipCallback = true;
		toggle.setSelected(getValue());
	}
}
