package com.lttlgames.editor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class GuiComponentColor extends
		GuiFieldObject<com.badlogic.gdx.graphics.Color>
{
	private JButton colorButton;
	private Color originalColor;
	private Color currentColor;
	private boolean open;

	GuiComponentColor(ProcessedFieldType pft, Object hostObject, int index,
			GuiComponentObject parent)
	{
		super(pft, hostObject, index, parent);
	}

	@Override
	void draw()
	{
		drawBeginningDefault();

		// additional beginning defaults
		if (parent != null)
		{
			parent.getPanel().revalidate();
			parent.getPanel().repaint();
		}

		if (checkDrawNull()) return;

		createPopupMenu();

		open = false;
		colorButton = new JButton("A");
		colorButton.setMargin(new Insets(-5, -5, -5, -5));
		colorButton.setPreferredSize(new Dimension(30, 20));
		colorButton.setFocusPainted(false);
		updateButtonColor();
		colorButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (!open)
				{
					open = true;

					if (shouldAutoUndo())
					{
						// save undo value
						undoValue = new com.badlogic.gdx.graphics.Color(
								getValue());
					}

					createColorDialog();
				}
			}
		});
		GridBagConstraints gbc = GuiHelper.GetGridBagConstraintsFieldValue();
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.NONE;
		getPanel().add(colorButton, gbc);
	}

	private void createPopupMenu()
	{
		createPopupMenu(getLabel());
		addExportImportMenuItem();
		addSetToNullMenuItem();
	}

	private void createColorDialog()
	{
		originalColor = GuiHelper.ConvertColorToAwt(getValue(), true);
		currentColor = GuiHelper.ConvertColorToAwt(getValue(), true);

		JPanel dialogContentPane = new JPanel(new GridBagLayout());
		final JColorChooser colorChooser = new JColorChooser(currentColor);

		// create the preview panel colors
		final JPanel previewPanel = new JPanel(new GridBagLayout());
		final JPanel newColorPanel = new JPanel();
		newColorPanel.setBackground(originalColor);
		JPanel originalColorPanel = new JPanel();
		originalColorPanel.setBackground(originalColor);
		originalColorPanel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				colorChooser.setColor(originalColor);
				colorChooser.repaint();
				previewPanel.repaint();
			}
		});

		// remove unwanted color models
		AbstractColorChooserPanel[] choosers = colorChooser.getChooserPanels();
		colorChooser.removeChooserPanel(choosers[0]);
		colorChooser.removeChooserPanel(choosers[1]);
		colorChooser.removeChooserPanel(choosers[4]);

		// create listener for when color changes
		colorChooser.getSelectionModel().addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent e)
			{
				currentColor = colorChooser.getColor();
				getValue().set(currentColor.getRed() / 255f,
						currentColor.getGreen() / 255f,
						currentColor.getBlue() / 255f,
						currentColor.getAlpha() / 255f);
				onEditorValueChange();
				updateButtonColor();
				newColorPanel.setBackground(currentColor);
				previewPanel.repaint();
			}
		});

		// add the preview color panels to the preview panel
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridwidth = 1;
		gbc.weighty = 1;
		previewPanel.add(newColorPanel, gbc);
		gbc.gridx = 1;
		previewPanel.add(originalColorPanel, gbc);
		gbc.weightx = 1;
		gbc.gridx = 0;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.NONE;
		dialogContentPane.add(colorChooser, gbc);
		gbc.gridy = 1;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.BOTH;
		dialogContentPane.add(previewPanel, gbc);
		// remove the preview panel
		colorChooser.getPreviewPanel().getParent().setVisible(false);

		// create the frame
		JFrame dialogFrame = new JFrame("Color Chooser- " + getLabelText());
		dialogFrame.setContentPane(dialogContentPane);
		int dialogFrameHeight = 360;
		int dialogFrameWidth = 665;
		dialogFrame.setSize(dialogFrameWidth, dialogFrameHeight);
		dialogFrame.setLocation(
				Toolkit.getDefaultToolkit().getScreenSize().width
						- dialogFrameWidth, Toolkit.getDefaultToolkit()
						.getScreenSize().height - dialogFrameHeight);
		dialogFrame.setAlwaysOnTop(true);
		dialogFrame.setVisible(true);
		dialogFrame.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				open = false;

				if (shouldAutoUndo())
				{
					// create undo if value changed
					if (!undoValue.equals(getValue()))
					{
						registerUndo(new UndoState(GuiComponentColor.this,
								new com.badlogic.gdx.graphics.Color(getValue())));
					}
				}
			}
		});
	}

	void updateButtonColor()
	{
		if (getValue() != null)
		{
			colorButton.setBackground(GuiHelper.ConvertColorToAwt(
					new com.badlogic.gdx.graphics.Color(GuiHelper.bgColor)
							.lerp(getValue(), getValue().a), false));
			GuiHelper.SetFont(colorButton, 10, new Color(0, 0, 0,
					1 - getValue().a), 0);
		}
	}

	void updatePrimativeValue()
	{
		updateButtonColor();
	}

	@Override
	void processUndoRedo(Object value)
	{
		if (value == null)
		{
			setValue(null);
		}
		else if (getValue() == null)
		{
			setValue(new com.badlogic.gdx.graphics.Color(
					(com.badlogic.gdx.graphics.Color) value));
		}
		else
		{
			setValue(getValue().set((com.badlogic.gdx.graphics.Color) value));
		}
		updateButtonColor();
		callbacks();
		processChangeCallback(1);
	}

	@Override
	protected void onCreate(com.badlogic.gdx.graphics.Color obj)
	{
		obj.set(com.badlogic.gdx.graphics.Color.WHITE);
	}
}
