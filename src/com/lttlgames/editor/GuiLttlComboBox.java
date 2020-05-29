package com.lttlgames.editor;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

import com.lttlgames.helpers.LttlHelper;

@SuppressWarnings("serial")
class GuiLttlComboBox extends JComboBox<GuiSelectOptionContainer>
{
	private ArrayList<GuiSelectOptionContainer> options;
	private ArrayList<GuiLttlComboBoxListener> actionListeners = new ArrayList<GuiLttlComboBoxListener>();
	private ArrayList<GuiLttlFocusListener> focusListeners = new ArrayList<GuiLttlFocusListener>();

	private boolean silentSelect = false;
	private boolean arrowSelect = false;
	private boolean enterSubmit = false;
	private boolean backspaceDown = false;
	private final GuiLttlComboBox combo;
	final JTextField textBox;
	private final Component button;
	private ActionListener actionListener;
	private GuiSelectOptionContainer lastGsoc = null;
	private GuiSelectOptionContainer lastGsocSubmitted = null;
	private ArrayList<GuiSelectOptionContainer> optionsOrderedList;

	/**
	 * @param options
	 *            list of option objects
	 * @param initial
	 *            the initial option object (can be null if none selected initially)
	 * @param alphaNumericSort
	 *            should options be sorted first
	 */
	GuiLttlComboBox(final ArrayList<GuiSelectOptionContainer> options,
			GuiSelectOptionContainer initial, boolean alphaNumericSort)
	{
		super((alphaNumericSort ? LttlHelper.ArrayListSortAlphaNumeric(options)
				: options)
				.toArray(new GuiSelectOptionContainer[options.size()]));
		this.options = options;

		// fix duplicate dispaly names since it breaks the options
		ArrayList<String> displayNames = new ArrayList<String>(options.size());
		for (GuiSelectOptionContainer gsoc : options)
		{
			int count = 0;
			String base = gsoc.display;
			while (displayNames.contains(gsoc.display))
			{
				gsoc.display = base + " (" + ++count + ")";
			}

			displayNames.add(gsoc.display);
		}

		// set the initial, if it exists
		if (LttlHelper.ArrayListContains(options, initial, true))
		{
			lastGsoc = initial;
			this.setSelectedItem(initial, true);
		}
		this.setEditable(true);
		AutoCompleteDecorator.decorate(this);

		combo = this;
		textBox = (JTextField) this.getComponents()[2];
		button = this.getComponents()[0];

		button.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (optionsOrderedList != null
						&& options.size() != optionsOrderedList.size())
				{
					resetDropdown();
					combo.showPopup();
				}
			}
		});

		// add custom focus listeners to the JTextField
		textBox.addFocusListener(new FocusListener()
		{
			@Override
			public void focusLost(FocusEvent e)
			{
				if (getSelectedItem() != lastGsoc)
				{
					Object saved = lastGsoc;
					resetDropdown();
					setSelectedItem(saved, true);
				}

				for (final GuiLttlFocusListener glfl : focusListeners)
				{
					glfl.focusLost(e);
				}
			}

			@Override
			public void focusGained(FocusEvent e)
			{
				for (final GuiLttlFocusListener glfl : focusListeners)
				{
					glfl.focusGained(e);
				}
			}
		});

		// when using arrow keys, will not trigger a selection submitted
		textBox.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_UP
						|| e.getKeyCode() == KeyEvent.VK_DOWN)
				{
					arrowSelect = true;
				}
				else if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					enterSubmit = true;
				}
				else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE)
				{
					backspaceDown = true;
					resetDropdown();
				}
			}

			@Override
			public void keyReleased(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_UP
						|| e.getKeyCode() == KeyEvent.VK_DOWN)
				{
					arrowSelect = false;
				}
				else if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					enterSubmit = false;
				}
				else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE)
				{
					backspaceDown = false;
				}
			}
		});

		// create action listener (magic)
		actionListener = new ActionListener()
		{
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				// this allows to search for item by first letter or any other
				// letters in it
				// it orders the items with most relevant at the top
				// you can use arrow keys to traverse them, and it autoselected
				// teh top (if any) if your search produces
				// at least one result
				if ((e == null || !e.getActionCommand()
						.equals("comboBoxEdited"))
						&& (combo.getEditor().getItem() == null || combo
								.getEditor().getItem().getClass() == String.class))
				{
					// get editor text to use to search through options
					String editorText = null;
					if (!enterSubmit)
					{
						editorText = getEditorText();
						final String editorTextFinal = editorText;

						// update the options with only those that contain the
						// text
						// remove all items
						for (int i = combo.getItemCount() - 1; i >= 0; i--)
						{
							combo.removeItemAt(i);
						}

						if (optionsOrderedList == null)
						{
							optionsOrderedList = new ArrayList<GuiSelectOptionContainer>();
						}
						optionsOrderedList.clear();
						for (GuiSelectOptionContainer gsoc : options)
						{
							if (gsoc.toString().toLowerCase()
									.contains(editorText.toLowerCase()))
							{
								optionsOrderedList.add(gsoc);
							}
						}

						// sort it so the options that match earlier are closer
						// to the top
						Collections.sort(optionsOrderedList,
								new Comparator<GuiSelectOptionContainer>()
								{
									@Override
									public int compare(
											GuiSelectOptionContainer o1,
											GuiSelectOptionContainer o2)
									{
										int index1 = o1
												.toString()
												.toLowerCase()
												.indexOf(
														editorTextFinal
																.toLowerCase());
										int index2 = o2
												.toString()
												.toLowerCase()
												.indexOf(
														editorTextFinal
																.toLowerCase());
										if (index1 == index2)
										{
											return 0;
										}
										else if (index1 < index2)
										{
											return -1;
										}
										else
										{
											return 1;
										}
									}
								});

						// set the options
						for (GuiSelectOptionContainer gsoc : optionsOrderedList)
						{
							combo.addItem(gsoc);
						}

						// make so the popup resizes correctly
						if (combo.isShowing())
						{
							combo.hidePopup();
							combo.showPopup();
						}
					}

					GuiSelectOptionContainer selected = null;
					// check editorText because if it's null or empty, then
					// don't choose the first one
					if (combo.getItemCount() > 0 && editorText != null
							&& !editorText.isEmpty())
					{
						selected = (GuiSelectOptionContainer) combo
								.getItemAt(0);
						if (selected != combo.getSelectedItem())
						{
							combo.setSelectedItem(selected);
						}
					}
					else
					{
						return;
					}
					if (lastGsoc != selected && selected != null
							&& selected.value != null)
					{
						if (!silentSelect && !backspaceDown)
						{
							for (GuiLttlComboBoxListener actionListener : actionListeners)
							{
								actionListener.selectionChanged(selected);
							}
						}
						lastGsoc = selected;
					}
					return;
				}

				GuiSelectOptionContainer selected = null;
				// gets first one in list if doing a search
				if (combo.getSelectedItem() != null
						&& combo.getSelectedItem().getClass() == String.class)
				{
					if (combo.getItemCount() > 0)
					{
						selected = (GuiSelectOptionContainer) combo
								.getItemAt(0);
						setSelectedItem(selected);
					}
					else
					{
						return;
					}
				}
				else
				{
					// otherwise grab the actually selected object
					try
					{
						selected = (GuiSelectOptionContainer) combo
								.getSelectedItem();
					}
					catch (ClassCastException e1)
					{

					}
				}
				if (selected == null) return;

				// CALLBACK to actionListener
				boolean selectionChange = lastGsoc != selected
						&& (combo.getEditor().getItem() != null && combo
								.getEditor().getItem().getClass() != String.class)
						&& (e == null || !e.getActionCommand().equals(
								"comboBoxEdited"));
				lastGsoc = selected;
				// check if selection change is found (by using arrow keys
				// through list or clicking)
				if (selectionChange)
				{
					if (!silentSelect && !backspaceDown)
					{
						for (GuiLttlComboBoxListener actionListener : actionListeners)
						{
							actionListener.selectionChanged(selected);
						}
					}
				}
				else if (lastGsocSubmitted != selected && !arrowSelect)
				{
					// reset the combo box when submitting (enter), also set the
					// selected item
					for (int i = combo.getItemCount() - 1; i >= 0; i--)
					{
						combo.removeItemAt(i);
					}
					for (GuiSelectOptionContainer gsoc : options)
					{
						combo.addItem(gsoc);
					}
					combo.setSelectedItem(selected);
					if (!silentSelect && !backspaceDown)
					{
						for (GuiLttlComboBoxListener actionListener : actionListeners)
						{
							actionListener.selectionSubmitted(selected);
						}
					}
					lastGsocSubmitted = selected;
				}

			}
		};
		super.addActionListener(actionListener);
	}

	public void addLttlActionListener(GuiLttlComboBoxListener actionListener)
	{
		actionListeners.add(actionListener);
	}

	public ArrayList<GuiLttlComboBoxListener> getLttlActionListeners()
	{
		return actionListeners;
	}

	public void addLttlFocusListener(GuiLttlFocusListener focusListener)
	{
		focusListeners.add(focusListener);
	}

	public ArrayList<GuiLttlFocusListener> getLttlFocusListeners()
	{
		return focusListeners;
	}

	public ArrayList<GuiSelectOptionContainer> getOptions()
	{
		return options;
	}

	public GuiSelectOptionContainer getSelected()
	{
		return (GuiSelectOptionContainer) this.getSelectedItem();
	}

	/**
	 * Sets the selected item as normal, but with option to silent it, which will not trigger any callbacks and then
	 * hide the popup.
	 * 
	 * @param anObject
	 * @param silent
	 */
	public void setSelectedItem(Object anObject, boolean silent)
	{
		silentSelect = silent;
		super.setSelectedItem(anObject);
		silentSelect = false;
		if (silent)
		{
			hidePopup();
		}
	}

	private void resetDropdown()
	{
		for (int i = combo.getItemCount() - 1; i >= 0; i--)
		{
			combo.removeItemAt(i);
		}
		combo.silentSelect = true;
		textBox.setText("");
		combo.silentSelect = false;
	}

	private String getEditorText()
	{
		try
		{
			return getDocument().getText(0, getDocument().getLength());
		}
		catch (BadLocationException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	private Document getDocument()
	{
		return ((JTextComponent) combo.getEditor().getEditorComponent())
				.getDocument();
	}
}
