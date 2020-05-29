package com.lttlgames.editor;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JTextField;

public class GuiComponentTagBit<T> extends GuiFieldObject<T>
{

	private JTextField textField;

	GuiComponentTagBit(ProcessedFieldType pft, Object hostObject, int index,
			GuiComponentObject parent)
	{
		super(pft, hostObject, index, parent);
	}

	@Override
	void draw()
	{
		drawBeginningDefault();

		// create text field that will hold the bit text
		textField = new JTextField(GuiHelper.getTagText((Short) objectRef));
		textField.setEnabled(false);

		createPopupMenu();

		// add to panel
		GridBagConstraints gbc = GuiHelper.GetGridBagConstraintsFieldValue();
		getPanel().add(textField, gbc);
	}

	void createPopupMenu()
	{
		super.createPopupMenu(getPanel());

		JMenuItem editMenuItem = new JMenuItem("Edit");
		editMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				setValue(Lttl.editor.getGui().editTagBitDialog(
						(Short) getValue()));
				textField.setText(GuiHelper.getTagText((Short) getValue()));
			}
		});

		popup.add(editMenuItem);
	}

}
