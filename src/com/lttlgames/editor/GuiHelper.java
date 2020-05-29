package com.lttlgames.editor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntArray;
import com.lttlgames.editor.annotations.GuiHide;
import com.lttlgames.editor.annotations.GuiShow;
import com.lttlgames.helpers.LttlCallback;
import com.lttlgames.helpers.LttlHelper;
import com.lttlgames.helpers.LttlMutatableBoolean;
import com.lttlgames.helpers.LttlMutatableInt;
import com.lttlgames.helpers.LttlMutatableString;

public class GuiHelper
{
	static final private GridBagConstraints gbc = new GridBagConstraints();
	static final float fieldFontSize = 10f;
	static final float buttonFontSize = 10f;
	static final Dimension defaultFieldDimension = new Dimension(50, 25);
	static final com.badlogic.gdx.graphics.Color bgColor = new com.badlogic.gdx.graphics.Color(
			173 / 255f, 173 / 255f, 173 / 255f, 1);

	/**
	 * List the classes here that you want to persist but do no want to draw gui for, this is really only relevant for
	 * library classes that you can't put a HideGui on the class
	 */
	static final ArrayList<Class<?>> forbiddenLibraryGuiClasses = new ArrayList<Class<?>>();
	static
	{
		forbiddenLibraryGuiClasses.add(HashMap.class);
		forbiddenLibraryGuiClasses.add(FloatArray.class);
		forbiddenLibraryGuiClasses.add(IntArray.class);
	}

	static JLabel GetFieldLabel(String labelText, GuiFieldObject<?> target)
	{
		JLabel label = new JLabel(labelText);
		SetFontSize(label, fieldFontSize);
		if (target != null && target.parent != null
				&& target.parent.getClass() != GuiComponentArrayList.class
				&& target.isTwoColumn())
		{
			// double column label
			label.setPreferredSize(new Dimension(40, 15));
		}
		else if (target.index != -1
				|| target.hostObject.getClass() == Vector2.class)
		{
			// it must be in an array
			label.setPreferredSize(new Dimension(20, 15));
		}
		else
		{
			// single column label
			label.setPreferredSize(new Dimension(90, 15));
		}
		return label;
	}

	private static ArrayList<Class<?>> twoColumnObjectClasses;

	static ArrayList<Class<?>> GetTwoColumnObjectClasses()
	{
		if (twoColumnObjectClasses == null)
		{
			twoColumnObjectClasses = new ArrayList<Class<?>>();
			// twoColumnObjectClasses.add(Vector2.class);
		}
		return twoColumnObjectClasses;
	}

	static GridBagConstraints GetGridBagConstraintsFieldLabel()
	{
		gbc.gridx = 0;
		gbc.gridwidth = 1;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets.set(0, 0, 0, 0);
		return gbc;
	}

	static GridBagConstraints GetGridBagConstraintsFieldValue()
	{
		gbc.gridx = 1;
		gbc.gridwidth = 1;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets.set(0, 0, 0, 0);
		return gbc;
	}

	static double GetDividerLocationRelative(JSplitPane pane)
	{
		int absoluteLocation = pane.getDividerLocation();
		double relativeLocation;
		if (pane.getOrientation() == JSplitPane.VERTICAL_SPLIT)
		{
			relativeLocation = (double) absoluteLocation
					/ (pane.getHeight() - pane.getDividerSize());
			relativeLocation = (double) absoluteLocation / pane.getHeight();
		}
		else
		{
			relativeLocation = (double) absoluteLocation
					/ (pane.getWidth() - pane.getDividerSize());
			relativeLocation = (double) absoluteLocation / pane.getWidth();
		}
		return relativeLocation;
	}

	static Font DeriveFontColor(Font font, Color color)
	{
		return DeriveFontColor(font, color, 0);
	}

	/**
	 * @param font
	 * @param color
	 * @param weight
	 *            TextAttribute.WEIGHT_EXTRA_LIGHT, WEIGHT_LIGHT, WEIGHT_DEMILIGHT, WEIGHT_REGULAR, WEIGHT_SEMIBOLD,
	 *            WEIGHT_MEDIUM, WEIGHT_DEMIBOLD, WEIGHT_BOLD, WEIGHT_HEAVY, WEIGHT_EXTRABOLD, and WEIGHT_ULTRABOLD.<br>
	 *            <b>0 means ignores weight</b>
	 * @return
	 */
	static Font DeriveFontColor(Font font, Color color, float weight)
	{
		HashMap<TextAttribute, Object> map = new HashMap<TextAttribute, Object>();
		map.put(TextAttribute.FOREGROUND, color);
		if (weight != 0)
		{
			map.put(TextAttribute.WEIGHT, weight);
		}
		return font.deriveFont(map);
	}

	static Font DeriveFontSize(Font font, float size)
	{
		return font.deriveFont(size);
	}

	/**
	 * Sets the font on the component
	 * 
	 * @param component
	 * @param fontSize
	 * @param color
	 * @param weight
	 *            TextAttribute.WEIGHT_EXTRA_LIGHT, WEIGHT_LIGHT, WEIGHT_DEMILIGHT, WEIGHT_REGULAR, WEIGHT_SEMIBOLD,
	 *            WEIGHT_MEDIUM, WEIGHT_DEMIBOLD, WEIGHT_BOLD, WEIGHT_HEAVY, WEIGHT_EXTRABOLD, and WEIGHT_ULTRABOLD.<br>
	 *            <b>0 means ignores weight</b>
	 * @return component for chaining
	 */
	static <T extends JComponent> T SetFont(T component, float fontSize,
			Color color, float weight)
	{
		component.setFont(GuiHelper.DeriveFontColor(component.getFont(), color,
				weight));
		component.setFont(GuiHelper.DeriveFontSize(component.getFont(),
				fontSize));
		return component;
	}

	/**
	 * Sets the font size on the component
	 * 
	 * @param component
	 * @param fontSize
	 * @return component for chaining
	 */
	static <T extends JComponent> T SetFontSize(T component, float fontSize)
	{
		component.setFont(GuiHelper.DeriveFontSize(component.getFont(),
				fontSize));
		return component;
	}

	/**
	 * Sets the font color on the component
	 * 
	 * @param component
	 * @param color
	 * @return component for chaining
	 */
	static <T extends JComponent> T SetFontColor(T component, Color color)
	{
		component
				.setFont(GuiHelper.DeriveFontColor(component.getFont(), color));
		return component;
	}

	static MouseAdapter GetPopUpMenuMouseAdapter(final JPopupMenu popup)
	{
		return new MouseAdapter()
		{
			public void mousePressed(MouseEvent e)
			{
				maybeShowPopup(e);
			}

			public void mouseReleased(MouseEvent e)
			{
				maybeShowPopup(e);
			}

			private void maybeShowPopup(MouseEvent e)
			{
				if (e.isPopupTrigger())
				{
					popup.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		};
	}

	/**
	 * Checks if this class should be drawn in GUI
	 * 
	 * @param clazz
	 * @return
	 */
	static boolean isClassDrawnGui(Class<?> clazz)
	{
		if (LttlObjectGraphCrawler.isPrimative(clazz)) { return true; }
		// this allows anonymouse classes that are objects to be drawn, which is helpful for when you want to draw
		// something in gui but it needs to be in an object first
		if (clazz.isAnonymousClass() && clazz.getSuperclass() == Object.class)
			return true;
		return !clazz.isArray()
				&& !isClassLibraryHidden(clazz)
				&& (LttlObjectGraphCrawler.isClassPersisted(clazz) || clazz
						.isAnnotationPresent(GuiShow.class))
				&& !clazz.isAnnotationPresent(GuiHide.class);
	}

	static boolean isClassLibraryHidden(Class<?> clazz)
	{
		for (Class<?> c : forbiddenLibraryGuiClasses)
		{
			if (c.isAssignableFrom(clazz)) { return true; }
		}
		return false;
	}

	/**
	 * Checks field's annotations and it's type
	 * 
	 * @param pft
	 * @return
	 */
	static boolean isFieldDrawnGui(ProcessedFieldType pft)
	{
		// check if the field has a persist annotation OR if it is on a libraryPersistedClass object and public, and
		// does not have a GuiHide, or it just has a GuiShow
		Field field = pft.getField();
		if ((LttlObjectGraphCrawler.isFieldPersisted(pft) && !field
				.isAnnotationPresent(GuiHide.class))
				|| field.isAnnotationPresent(GuiShow.class))
		{
			if (!isClassDrawnGui(pft.getCurrentClass()))
			{
				Lttl.Throw("Can't draw field "
						+ field.getName()
						+ " because it's class "
						+ field.getType().getSimpleName()
						+ " is not allowed to be drawn, but the field is persisted. Add @GuiHide.");
				return false;
			}
			return true;
		}
		return false;

	}

	static Color ConvertColorToAwt(com.badlogic.gdx.graphics.Color color,
			boolean alpha)
	{
		return new Color(color.r, color.g, color.b, alpha ? color.a : 1);
	}

	static com.badlogic.gdx.graphics.Color ConvertAwtToColor(Color c,
			boolean alpha)
	{
		return new com.badlogic.gdx.graphics.Color(c.getRed() / 255f,
				c.getGreen() / 255f, c.getBlue() / 255f,
				alpha ? c.getAlpha() / 255f : 1);
	}

	static boolean isPrimativeGui(Class<?> clazz)
	{
		return LttlObjectGraphCrawler.isPrimative(clazz)
				|| clazz == Color.class || clazz.isEnum();
	}

	/**
	 * Creates a standard frame
	 * 
	 * @param name
	 * @param width
	 * @param height
	 * @param center
	 * @param alwaysOnTop
	 * @param contentPane
	 * @return
	 */
	static JFrame createFrame(String name, int width, int height,
			boolean center, boolean alwaysOnTop, JPanel contentPane)
	{
		// create the frame
		JFrame frame = new JFrame(name);
		frame.setContentPane(contentPane);
		frame.setSize(width, height);
		if (center)
		{
			frame.setLocation(
					(int) (Toolkit.getDefaultToolkit().getScreenSize().width / 2f - width / 2f),
					(int) (Toolkit.getDefaultToolkit().getScreenSize().height / 2f - height / 2f));
		}
		frame.setAlwaysOnTop(alwaysOnTop);
		frame.setVisible(true);

		return frame;
	}

	/**
	 * Creates a dialog (modal). Still need to run setVisible() when you are all done adding stuff to contentPane
	 * 
	 * @param name
	 * @param width
	 * @param height
	 * @param center
	 * @param alwaysOnTop
	 * @param modalType
	 * @param contentPanel
	 * @return
	 */
	static JDialog createDialog(String name, int width, int height,
			boolean center, boolean alwaysOnTop, ModalityType modalType,
			JPanel contentPanel)
	{
		return createDialog(name, width, height, center, alwaysOnTop,
				modalType, contentPanel, null);
	}

	/**
	 * Creates a dialog (modal). Still need to run setVisible() when you are all done adding stuff to contentPane
	 * 
	 * @param name
	 * @param width
	 * @param height
	 * @param center
	 * @param alwaysOnTop
	 * @param modalType
	 *            ModalityType.APPLICATION_MODAL forces program to wait for dialog to dispose, good for returning
	 *            response
	 * @param contentPanel
	 * @param callbacks
	 *            [0 = on open, 1 = on close by escape, 2 = close by exit button]
	 * @return
	 */
	static JDialog createDialog(String name, int width, int height,
			boolean center, boolean alwaysOnTop, ModalityType modalType,
			JPanel contentPanel, final LttlCallback callbacks)
	{
		// onstart callback
		if (callbacks != null)
		{
			callbacks.callback(0);
		}

		// create the frame
		final JDialog dialog = new JDialog();
		dialog.setTitle(name);
		dialog.setModalityType(modalType);
		dialog.setSize(width, height);
		dialog.setContentPane(contentPanel);

		// escape closes dialog
		contentPanel.registerKeyboardAction(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// on escape close
				if (callbacks != null)
				{
					callbacks.callback(1);
				}
				dialog.dispose();
			}
		}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

		dialog.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				if (callbacks != null)
				{
					callbacks.callback(2);
				}
			}
		});

		dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		dialog.setAlwaysOnTop(alwaysOnTop);
		if (center)
		{
			dialog.setLocation(
					(int) (Toolkit.getDefaultToolkit().getScreenSize().width / 2f - width / 2f),
					(int) (Toolkit.getDefaultToolkit().getScreenSize().height / 2f - height / 2f));
		}

		return dialog;
	}

	/**
	 * Shows an alert dialog.
	 * 
	 * @param parentComponent
	 *            can be null
	 * @param title
	 * @param message
	 * @param alertType
	 *            JOptionPane.ERROR_MESSAGE, INFORMATION_MESSAGE, WARNING_MESSAGE, QUESTION_MESSAGE, or PLAIN_MESSAGE
	 */
	public static void showAlert(Component parentComponent, String title,
			String message, int alertType)
	{
		JOptionPane.showMessageDialog(parentComponent, message, title,
				JOptionPane.WARNING_MESSAGE);
	}

	/**
	 * Creates and shows a modal with the title and text and a Yes/No button.
	 * 
	 * @param title
	 * @param text
	 * @return the response id [0=YES,1=NO,2=ESCAPE]
	 */
	public static int showOptionModal(String title, String text)
	{
		final LttlMutatableInt responseId = new LttlMutatableInt();
		JPanel panel = new JPanel(new GridBagLayout());
		final JDialog dialog = createDialog(title, 450, 150, true, true,
				ModalityType.APPLICATION_MODAL, panel, new LttlCallback()
				{
					@Override
					public void callback(int id, Object... objects)
					{
						if (id == 1 || id == 2)
						{
							responseId.value = 2;
						}
					}
				});
		GridBagConstraints gbc = new GridBagConstraints(0, 0, 2, 1, 1, 1,
				GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
				new Insets(10, 10, 10, 10), 0, 0);

		// add text label
		JLabel label = new JLabel("<html>" + text + "</html>");
		panel.add(label, gbc);

		// Add Yes Button
		JPanel buttonPanel = new JPanel();
		final JButton yesButton = new JButton("Yes");
		yesButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				responseId.value = 0;
				dialog.dispose();
			}
		});
		yesButton.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					yesButton.doClick();
				}
			}
		});
		buttonPanel.add(yesButton);

		// add No button
		final JButton noButton = new JButton("No");
		noButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				responseId.value = 1;
				dialog.dispose();
			}
		});
		noButton.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					noButton.doClick();
				}
			}
		});
		buttonPanel.add(noButton);

		gbc.gridwidth = 0;
		gbc.gridy = 1;
		gbc.weightx = 0;
		gbc.anchor = GridBagConstraints.EAST;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(0, 0, 0, 0);
		panel.add(buttonPanel, gbc);

		dialog.setResizable(false);
		dialog.setVisible(true);
		return responseId.value;
	}

	/**
	 * Creates and shows a modal with the title and description and an int field.<br>
	 * Must press enter to submit value.
	 * 
	 * @param title
	 * @param description
	 * @return Integer, or null if escaped
	 */
	public static Integer showIntegerModal(String title, String description)
	{
		final LttlMutatableBoolean isNull = new LttlMutatableBoolean(false);
		final LttlMutatableBoolean enterPressed = new LttlMutatableBoolean(
				false);

		JPanel panel = new JPanel(new GridBagLayout());
		final JDialog dialog = createDialog(title, 450,
				(description == null || description.isEmpty()) ? 100 : 150,
				true, true, ModalityType.APPLICATION_MODAL, panel,
				new LttlCallback()
				{
					@Override
					public void callback(int id, Object... objects)
					{
						if (id == 1 || id == 2)
						{
							isNull.value = true;
						}
					}
				});
		GridBagConstraints gbc = new GridBagConstraints(0, 0, 2, 1, 1, 1,
				GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
				new Insets(10, 10, 10, 10), 0, 0);

		if (description != null && !description.isEmpty())
		{
			// add text label
			JLabel label = new JLabel("<html>" + description + "</html>");
			panel.add(label, gbc);
		}

		final JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, null,
				null, 1));
		final JFormattedTextField editor = (JFormattedTextField) spinner
				.getEditor().getComponent(0);
		final LttlMutatableString lastText = new LttlMutatableString(
				editor.getText());
		spinner.addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent event)
			{
				lastText.value = "" + spinner.getValue();
				if (enterPressed.value)
				{
					dialog.dispose();
				}
			}
		});
		editor.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					enterPressed.value = true;
					// if text equals the same value as the last value then close dialog now, since a stateChange wll no
					// be called on spinner
					if (editor.getText().equals(lastText.value))
					{
						dialog.dispose();
					}
				}
			}
		});

		gbc.gridwidth = 1;
		gbc.gridy = 1;
		gbc.weightx = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 0, 0);
		panel.add(spinner, gbc);

		dialog.setResizable(false);
		dialog.setVisible(true);

		return (Integer) (isNull.value ? null : spinner.getValue());
	}

	/**
	 * Creates and shows a modal with the title and description and a textare.
	 * 
	 * @param title
	 * @param description
	 * @param width
	 * @param height
	 * @return response string, or null if escaped or canceled
	 */
	public static String showTextAreaModal(String title, String description,
			int width, int height)
	{
		final LttlMutatableString response = new LttlMutatableString();
		JPanel panel = new JPanel(new GridBagLayout());
		final JDialog dialog = createDialog(title, width, height, true, true,
				ModalityType.APPLICATION_MODAL, panel, new LttlCallback()
				{
					@Override
					public void callback(int id, Object... objects)
					{
						if (id == 1 || id == 2)
						{
							response.value = null;
						}
					}
				});
		GridBagConstraints gbc = new GridBagConstraints(0, 0, 2, 1, 1, 1,
				GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
				new Insets(0, 0, 0, 0), 0, 0);

		if (description != null && !description.isEmpty())
		{
			// add text label
			JLabel label = new JLabel("<html>" + description + "</html>");
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.gridwidth = 2;
			gbc.weightx = 1;
			gbc.weighty = 0;
			gbc.insets.set(10, 10, 10, 10);
			panel.add(label, gbc);
		}

		final JTextArea textArea = new JTextArea();
		textArea.setLineWrap(true);
		final JScrollPane scrollPane = new JScrollPane(textArea);
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 2;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets.set(0, 0, 0, 0);
		panel.add(scrollPane, gbc);

		JButton submitButton = new JButton("Submit");
		submitButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				response.value = textArea.getText();
				dialog.dispose();
			}
		});
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 1;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets.set(0, 0, 0, 0);
		panel.add(submitButton, gbc);

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				response.value = null;
				dialog.dispose();
			}
		});
		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.gridwidth = 1;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets.set(0, 0, 0, 0);
		panel.add(cancelButton, gbc);

		dialog.setResizable(false);
		dialog.setVisible(true);

		return response.value;
	}

	/**
	 * Creates and shows a modal with the title and description and a textfield.
	 * 
	 * @param title
	 * @param description
	 * @return response string, or null if escaped
	 */
	public static String showTextFieldModal(String title, String description)
	{
		final LttlMutatableString response = new LttlMutatableString();
		JPanel panel = new JPanel(new GridBagLayout());
		final JDialog dialog = createDialog(title, 450,
				(description == null || description.isEmpty()) ? 100 : 150,
				true, true, ModalityType.APPLICATION_MODAL, panel,
				new LttlCallback()
				{
					@Override
					public void callback(int id, Object... objects)
					{
						if (id == 1 || id == 2)
						{
							response.value = null;
						}
					}
				});
		GridBagConstraints gbc = new GridBagConstraints(0, 0, 2, 1, 1, 1,
				GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
				new Insets(10, 10, 10, 10), 0, 0);

		if (description != null && !description.isEmpty())
		{
			// add text label
			JLabel label = new JLabel("<html>" + description + "</html>");
			panel.add(label, gbc);
		}

		final JTextField textField = new JTextField();
		textField.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					dialog.dispose();
					response.value = textField.getText();
				}
			}
		});

		gbc.gridwidth = 1;
		gbc.gridy = 1;
		gbc.weightx = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 0, 0);
		panel.add(textField, gbc);

		dialog.setResizable(false);
		dialog.setVisible(true);

		return response.value;
	}

	/**
	 * Creates and shows a modal with the title and description and a combobox, populats with options, can allow custom
	 * or not.
	 * 
	 * @param title
	 * @param description
	 * @param allowCustom
	 *            if true, if there are options, you will be able to write a custom string
	 * @param submitOnSelect
	 *            if allowCustom is disabled, should it submit when an option is selected, otherwise will be when enter
	 *            is pressed.
	 * @param firstOptionIsBlank
	 *            makes initial option blank (if allowCustom is false), this initial option can't be submitted
	 * @param options
	 * @return response string, or null if escaped
	 */
	public static String showComboboxModal(String title, String description,
			final boolean allowCustom, boolean submitOnSelect,
			final boolean firstOptionIsBlank, String... options)
	{
		final LttlMutatableString response = new LttlMutatableString();
		JPanel panel = new JPanel(new GridBagLayout());
		final JDialog dialog = createDialog(title, 450,
				(description == null || description.isEmpty()) ? 100 : 150,
				true, true, ModalityType.APPLICATION_MODAL, panel,
				new LttlCallback()
				{
					@Override
					public void callback(int id, Object... objects)
					{
						if (id == 1 || id == 2)
						{
							response.value = null;
						}
					}
				});
		GridBagConstraints gbc = new GridBagConstraints(0, 0, 2, 1, 1, 1,
				GridBagConstraints.NORTH, GridBagConstraints.HORIZONTAL,
				new Insets(10, 10, 10, 10), 0, 0);

		// add text label
		if (description != null && !description.isEmpty())
		{
			JLabel label = new JLabel("<html>" + description + "</html>");
			panel.add(label, gbc);
		}

		// add blank value in options
		if (!allowCustom && firstOptionIsBlank)
		{
			String[] newOptions = new String[options.length + 1];
			newOptions[0] = "";
			for (int i = 1; i < newOptions.length; i++)
			{
				newOptions[i] = options[i - 1];
			}
			options = newOptions;
		}

		final JComboBox<String> comboBox = new JComboBox<String>(options);

		comboBox.setEditable(allowCustom);

		// get the textfield (if editable) or the combox, so we can add key adapter to it for enter
		final Component component = allowCustom ? ((JTextField) comboBox
				.getComponents()[2]) : comboBox;

		// if allow custom, then have to do enter key to submit
		if (allowCustom || !submitOnSelect)
		{
			component.addKeyListener(new KeyAdapter()
			{
				@Override
				public void keyPressed(KeyEvent e)
				{
					if (e.getKeyCode() == KeyEvent.VK_ENTER)
					{
						if (firstOptionIsBlank && !allowCustom)
						{
							if (((String) comboBox.getSelectedItem()).isEmpty()) { return; }
						}

						dialog.dispose();
						if (allowCustom)
						{
							response.value = ((JTextField) component).getText();
						}
						else
						{
							response.value = (String) comboBox
									.getSelectedItem();
						}
					}
				}
			});
		}
		else
		{
			// submit on select
			comboBox.addItemListener(new ItemListener()
			{

				@Override
				public void itemStateChanged(ItemEvent e)
				{
					if (firstOptionIsBlank)
					{
						if (((String) comboBox.getSelectedItem()).isEmpty()) { return; }
					}

					dialog.dispose();
					response.value = (String) comboBox.getSelectedItem();
				}
			});
		}

		gbc.gridwidth = 1;
		gbc.gridy = 1;
		gbc.weightx = 1;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 0, 0);
		panel.add(comboBox, gbc);

		dialog.setResizable(false);
		dialog.setVisible(true);

		return response.value;
	}

	/**
	 * This checks if this transform can be selected, but could still be in a selection group.
	 * 
	 * @param transform
	 * @return
	 */
	static boolean isSelectable(LttlTransform transform)
	{
		if (transform.selectionOptions.unselectableSelf) { return false; }
		LttlTransform parent = transform.getParent();
		while (parent != null)
		{
			if (parent.selectionOptions.unselectableTree) { return false; }
			parent = parent.getParent();
		}
		return true;
	}

	/**
	 * Gets the selection group transform for this transform and returns it
	 * 
	 * @param transform
	 * @return
	 */
	static LttlTransform getSelectionGroupTransform(LttlTransform transform)
	{
		LttlTransform current = transform;
		while (current != null)
		{
			if (current.selectionOptions.selectionGroup) { return current; }
			current = current.getParent();
		}
		return null;
	}

	/**
	 * collapses all the menus on a menu bar
	 * 
	 * @param bar
	 */
	static void hideMenuBar(JMenuBar bar)
	{
		if (!Lttl.editor.getGui().getMenuBarController().getMenuBar()
				.isSelected()) return;

		for (Component c : Lttl.editor.getGui().getMenuBarController()
				.getMenuBar().getComponents())
		{
			if (c.getClass() == JMenu.class)
			{
				hideMenuTree((JMenu) c);
			}
		}
	}

	/**
	 * collapes a menu tree if it is open and if any menus within it are open too
	 * 
	 * @param menu
	 */
	static void hideMenuTree(JMenu menu)
	{
		if (menu.isSelected())
		{
			menu.setSelected(false);
			menu.getPopupMenu().setVisible(false);
		}
		for (Component c : menu.getMenuComponents())
		{
			if (c.getClass() == JMenu.class)
			{
				hideMenuTree((JMenu) c);
			}
		}
	}

	static void unfocus(Component comp)
	{
		comp.setFocusable(false);
		comp.setFocusable(true);
	}

	/**
	 * Does any math operations in text and returns the string of the new number.<br>
	 * does not care about doing multiply/division before addition/subtraction
	 * 
	 * @param text
	 * @return
	 */
	static String processMathString(String text)
	{
		String originalText = text;
		FloatArray nums = null;
		IntArray opIds = null;

		// loop through and find all numbers and operations
		while (true)
		{
			int[] result = processMathIteration(text);

			// no operation found, must be last number
			if (result == null)
			{
				// if no operations found, then just return text, nothing changed
				if (opIds == null) { return text; }
				try
				{
					nums.add(Float.parseFloat(text));
				}
				catch (NumberFormatException e)
				{
					// if it fails, just GTFO nicely and pretend you weren't here
					return originalText;
				}
				break;
			}
			else if (nums == null)
			{
				// init arrays since operation found
				nums = new FloatArray();
				opIds = new IntArray();
			}

			// add the number and operation
			try
			{
				nums.add(Float.parseFloat(text.substring(0, result[0])));
			}
			catch (NumberFormatException e)
			{
				// if it fails, just GTFO nicely and pretend you weren't here
				return originalText;
			}
			opIds.add(result[1]);

			// adjust the text to not include the found number and operation
			text = text.substring(result[0] + 1);
		}

		// set starting number
		float num = nums.get(0);
		// iterate though all operation ids and perform operations
		for (int i = 0; i < opIds.size; i++)
		{
			// do operation
			int opId = opIds.get(i);
			switch (opId)
			{
				case 0:
					num += nums.get(i + 1);
					break;
				case 1:
					num -= nums.get(i + 1);
					break;
				case 2:
					num *= nums.get(i + 1);
					break;
				case 3:
					num /= nums.get(i + 1);
					break;
			}
		}

		return Float.toString(num);
	}

	/**
	 * @param text
	 * @return a int[], [0] is the index of the next operation, [1] is the operation id (0=+,1=-,2=*,3=/), null means no
	 *         operation found, at end
	 */
	private static int[] processMathIteration(String text)
	{
		// don't check the first index, since could be a minus if the first time running and each number has to have at
		// least one digit to it
		int opId = -1;
		int opIndex = Integer.MAX_VALUE;
		{
			int i = text.indexOf("+", 1);
			if (i < opIndex && i > -1)
			{
				opIndex = i;
				opId = 0;
			}
		}
		{
			int i = text.indexOf("-", 1);
			if (i < opIndex && i > -1)
			{
				opIndex = i;
				opId = 1;
			}
		}
		{
			int i = text.indexOf("*", 1);
			if (i < opIndex && i > -1)
			{
				opIndex = i;
				opId = 2;
			}
		}
		{
			int i = text.indexOf("/", 1);
			if (i < opIndex && i > -1)
			{
				opIndex = i;
				opId = 3;
			}
		}

		// found next operation
		if (opIndex < Integer.MAX_VALUE) { return new int[]
		{ opIndex, opId }; }

		return null;
	}

	static String getTagText(short tags)
	{
		if (tags == -1)
		{
			return "All";
		}
		else if (tags == 0)
		{
			return "None";
		}
		else
		{
			String text = "";
			for (int i = 0; i < 15; i++)
			{
				if (!LttlHelper.bitHasInt(tags, i)) continue;

				String t = Lttl.game.getSettings().getTagName(i);
				if (!text.isEmpty())
				{
					text += ", ";
				}
				text += t;
			}
			return text;
		}
	}
}
