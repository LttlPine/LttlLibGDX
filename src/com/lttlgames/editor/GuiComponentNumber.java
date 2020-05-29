package com.lttlgames.editor;

import java.awt.AWTException;
import java.awt.Component;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;

import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.JSpinner.NumberEditor;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputListener;

import com.lttlgames.editor.annotations.GuiDecimalPlaces;
import com.lttlgames.editor.annotations.GuiStepSize;
import com.lttlgames.helpers.LttlMath;
import com.lttlgames.helpers.LttlMutatableFloat;

public class GuiComponentNumber<T extends Number> extends GuiFieldObject<T>
		implements MouseInputListener
{
	private static float shiftDragMultiplier = 10;
	private static int defaultDecimalPlaces = 3;

	private boolean dontConstrainMinMax = false;
	private JSpinner spinner;
	private int prevX;
	private boolean dragging = false;
	private float stepSize;

	/**
	 * Can be used to define a mututable float to be used as the min (overrides any annotations)
	 */
	public LttlMutatableFloat minMut;
	/**
	 * Can be used to define a mututable float to be used as the max (overrides any annotations)
	 */
	public LttlMutatableFloat maxMut;

	GuiComponentNumber(ProcessedFieldType pft, Object hostObject, int index,
			GuiComponentObject parent)
	{
		super(pft, hostObject, index, parent);
	}

	@SuppressWarnings("serial")
	@Override
	void draw()
	{
		drawBeginningDefault();

		// set default options and check annotations for options
		GuiStepSize stepSizeAnn = getFirstAncestorWithField().getField()
				.getAnnotation(GuiStepSize.class);
		final float stepSize = stepSizeAnn != null ? LttlMath.abs(stepSizeAnn
				.value()) : LttlObjectGraphCrawler
				.isIntegerLikePrimative(getObjectClass()) ? 1 : Lttl.editor
				.getSettings().spinnerStepSize;
		this.stepSize = stepSize;

		// get min and max values if defined with annotations, on this field or any of it's ancestors
		Float minAncestors = getMinAncestor();
		final float min = minAncestors != null ? minAncestors
				: Float.NEGATIVE_INFINITY;
		Float maxAncestors = getMaxAncestor();
		final float max = maxAncestors != null ? maxAncestors
				: Float.POSITIVE_INFINITY;

		if (LttlObjectGraphCrawler
				.isIntegerLikePrimative(getValue().getClass()))
		{
			spinner = new JSpinner(new SpinnerNumberModel((Number) getValue(),
					null, null, stepSize));
		}
		else
		{
			GuiDecimalPlaces decimalPlacesAnn = getFirstAncestorWithField()
					.getField().getAnnotation(GuiDecimalPlaces.class);
			final int decimalPlaces = decimalPlacesAnn != null ? LttlMath.max(
					0, decimalPlacesAnn.value()) : defaultDecimalPlaces;
			spinner = new JSpinner(new SpinnerNumberModel((Number) getValue(),
					null, null, stepSize))
			{
				protected javax.swing.JComponent createEditor(
						javax.swing.SpinnerModel model)
				{
					String decimalString = "";
					for (int i = 0; i < decimalPlaces; i++)
					{
						decimalString += "0";
					}
					return new NumberEditor(this, "0." + decimalString);
				}
			};
		}

		// add key listener to number text field to check for any math operations, does not care about doing
		// multiply/division before addition/subtraction
		for (Component c : spinner.getComponents())
		{
			if (!(c instanceof NumberEditor)) continue;
			NumberEditor ne = (NumberEditor) c;
			for (Component cc : ne.getComponents())
			{
				final JFormattedTextField t = (JFormattedTextField) cc;
				t.addKeyListener(new KeyListener()
				{
					@Override
					public void keyTyped(KeyEvent e)
					{
					}

					@Override
					public void keyReleased(KeyEvent e)
					{
					}

					@Override
					public void keyPressed(KeyEvent e)
					{
						if (e.getKeyCode() == KeyEvent.VK_ENTER)
						{
							String text = t.getText();
							String opText = GuiHelper.processMathString(text);
							if (!text.equals(opText))
							{
								// if number is different, then change text
								// this still appears as if the number was only changed once, only one callback will be
								// triggered for when this number changes, this is all normal
								t.setText(opText);
							}
						}
					}
				});
			}

		}

		spinner.setPreferredSize(GuiHelper.defaultFieldDimension);
		disableComponentFromAnnotation(spinner);
		spinner.addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent event)
			{
				// this means this callback was triggered by checking the field value so don't constrain with min and
				// max, since in play mode there will be no editor to constraints and do set the value since we know
				// it's accurate
				if (dontConstrainMinMax)
				{
					spinner.revalidate();
					spinner.repaint();
					dontConstrainMinMax = false;
					return;
				}

				float mi = (minMut == null) ? min : minMut.value;
				float ma = (maxMut == null) ? max : maxMut.value;
				if (((Number) spinner.getValue()).floatValue() < mi)
				{
					spinner.setValue(mi);
				}
				else if (((Number) spinner.getValue()).floatValue() > ma)
				{
					spinner.setValue(ma);
				}

				Number value = (Number) spinner.getValue();
				if (LttlObjectGraphCrawler.isIntegerLikePrimative(objectRef
						.getClass())
						&& !LttlObjectGraphCrawler.isIntegerLikePrimative(value
								.getClass()))
				{
					value = LttlMath.floor((Float) value);
				}

				// save undo before it changes
				if (!dragging)
				{
					if (shouldAutoUndo())
					{
						undoValue = getValue();
					}
				}

				setValue((T) value);

				// create undo
				if (!dragging)
				{
					if (shouldAutoUndo())
					{
						registerUndo();
					}
				}

				onEditorValueChange();

				spinner.revalidate();
				spinner.repaint();
			}
		});
		getPanel().add(spinner, GuiHelper.GetGridBagConstraintsFieldValue());

		getPanel().addMouseMotionListener(this);
		getPanel().addMouseListener(this);
	}

	@Override
	void updatePrimativeValue()
	{
		dontConstrainMinMax = true;
		spinner.setValue(getValue());
	}

	private SpinnerNumberModel getModel()
	{
		return (SpinnerNumberModel) spinner.getModel();
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
		dragging = true;

		if (shouldAutoUndo())
		{
			// save undoValue
			undoValue = getValue();
		}

		prevX = e.getXOnScreen();
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
		dragging = false;

		if (shouldAutoUndo())
		{
			registerUndo();
		}
	}

	@Override
	public void mouseEntered(MouseEvent e)
	{
	}

	@Override
	public void mouseExited(MouseEvent e)
	{
	}

	// NOTE this is where you chaneg screen resolution for mouse loop
	private int screenX = Toolkit.getDefaultToolkit().getScreenSize().width;
	@SuppressWarnings("unused")
	private int screenY = Toolkit.getDefaultToolkit().getScreenSize().height;

	private int leftStart = 1;
	private int rightStart = screenX - 2;

	@Override
	public void mouseDragged(MouseEvent e)
	{
		if (!spinner.isEnabled()) return;

		// divide difference in pixels by 2 so it is more user friendly
		float value = (float) (((Number) getModel().getValue()).floatValue() + ((e
				.getXOnScreen() - prevX) / 2)
				* ((e.isShiftDown() ? shiftDragMultiplier : 1) * getModel()
						.getStepSize().floatValue()));
		getModel().setValue(value);

		// check if mouse position is at edge of screen
		if (e.getXOnScreen() >= screenX - 1)
		{
			try
			{
				new Robot().mouseMove(leftStart, e.getYOnScreen());
			}
			catch (AWTException e1)
			{
				e1.printStackTrace();
			}
			prevX = leftStart;
		}
		else if (e.getXOnScreen() <= 0)
		{
			try
			{
				new Robot().mouseMove(rightStart, e.getYOnScreen());
			}
			catch (AWTException e1)
			{
				e1.printStackTrace();
			}
			prevX = rightStart;
		}
		else
		{
			prevX = e.getXOnScreen();
		}
	}

	@Override
	public void mouseMoved(MouseEvent e)
	{
	}

	public JSpinner getSpinner()
	{
		return spinner;
	}

	/**
	 * creates an undo for this GuiComponentNumber, but first checks to see if the difference is greater than stepSize.
	 * This is necessary ebcause the spinner sends mutliple change callbacks, one for rounding, and one for the
	 * incremental value
	 */
	public void registerUndo()
	{
		if (undoValue == null) return;
		// create undo
		if (LttlMath.abs(((Number) undoValue).floatValue()
				- ((Number) getValue()).floatValue()) >= stepSize
				- LttlMath.getEpsilon())
		{
			registerUndo(new UndoState(GuiComponentNumber.this));
		}
	}
}
