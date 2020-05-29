package com.lttlgames.editor;

import java.awt.GridBagConstraints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import org.apache.commons.lang3.StringEscapeUtils;

import com.lttlgames.editor.annotations.GuiTextArea;
import com.lttlgames.editor.annotations.GuiUnescape;

public class GuiComponentString extends GuiFieldObject<String>
{
	GuiComponentString(ProcessedFieldType pft, Object hostObject, int index,
			GuiComponentObject parent)
	{
		super(pft, hostObject, index, parent);
	}

	private JTextComponent textComponent;
	private boolean skipCallback = false;
	private boolean unescape = true;
	private boolean isArea;

	@Override
	void draw()
	{
		if (checkDrawNull()) return;

		drawBeginningDefault();

		super.createPopupMenu(getLabel());
		addSetToNullMenuItem();

		unescape = getFirstAncestorWithField().getField().isAnnotationPresent(
				GuiUnescape.class);
		isArea = getFirstAncestorWithField().getField().isAnnotationPresent(
				GuiTextArea.class);

		if (isArea)
		{
			JTextArea textArea = new JTextArea(getValue());
			textComponent = textArea;
			// needs to wrap or will make scroll pane wider
			textArea.setLineWrap(true);
		}
		else
		{
			// if unescaping, then escape the value before setting it so it appears accurately
			textComponent = new JTextField(
					unescape ? StringEscapeUtils.escapeJava(getValue())
							: getValue());

			// keeps it from resizing the whole scroll panel if it is long
			textComponent.setPreferredSize(GuiHelper.defaultFieldDimension);
			textComponent.addKeyListener(new KeyAdapter()
			{
				@Override
				public void keyPressed(KeyEvent e)
				{
					if (e.getKeyCode() == KeyEvent.VK_ENTER)
					{
						GuiHelper.unfocus(textComponent);
					}
				}
			});
		}
		textComponent.addFocusListener(new FocusListener()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				if (shouldAutoUndo())
				{
					// create undo if value changed
					if (!undoValue.equals(getValue()))
					{
						registerUndo(new UndoState(GuiComponentString.this));
					}
				}
			}

			@Override
			public void focusGained(FocusEvent e)
			{
				if (shouldAutoUndo())
				{
					// save undo value
					undoValue = getValue();
				}
			}
		});
		textComponent.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void removeUpdate(DocumentEvent e)
			{
				changeState();
			}

			@Override
			public void insertUpdate(DocumentEvent e)
			{
				changeState();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
			}
		});
		GridBagConstraints gbc = GuiHelper.GetGridBagConstraintsFieldValue();
		getPanel().add(textComponent, gbc);
	}

	private void changeState()
	{
		// this means this callback was triggered by checking the field value
		if (skipCallback)
		{
			skipCallback = false;
			return;
		}

		String text = textComponent.getText();
		if (unescape && !isArea)
		{
			text = StringEscapeUtils.unescapeJava(text);
		}
		setValue(text);
		onEditorValueChange();
	}

	@Override
	void updatePrimativeValue()
	{
		skipCallback = true;
		textComponent.setText(getValue());
	}
}
