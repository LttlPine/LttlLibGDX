package com.lttlgames.editor;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.lttlgames.helpers.LttlHelper;
import com.lttlgames.helpers.LttlMath;

@SuppressWarnings("serial")
class GuiLttlSlider extends JSlider
{
	private float stepSize;
	private int numDecimals;
	private float min;
	private float max;
	private boolean tooltip;

	/**
	 * @param orientation
	 *            either <code>SwingConstants.VERTICAL</code> or <code>SwingConstants.HORIZONTAL</code>
	 * @param stepSize
	 * @param min
	 * @param max
	 * @param value
	 */
	GuiLttlSlider(int orientation, float stepSize, float min, float max,
			float value, boolean tooltip)
	{
		super(orientation, LttlMath.floor(min / stepSize), LttlMath.ceil(max
				/ stepSize), LttlMath.floor(value / stepSize));

		this.stepSize = stepSize;
		this.min = min;
		this.max = max;
		this.tooltip = tooltip;
		numDecimals = LttlHelper.getNumberOfDecimalPlaces(stepSize);

		updateToolTip();
	}

	@Override
	public void addChangeListener(final ChangeListener l)
	{
		super.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent e)
			{
				updateToolTip();

				// call original change event
				l.stateChanged(e);
			}
		});
	}

	public float getValueFloat()
	{
		return LttlMath.clamp(super.getValue() * stepSize, min, max);
	}

	private void updateToolTip()
	{
		if (tooltip)
		{
			setToolTipText(LttlHelper.formatFloat(numDecimals, getValueFloat()));
		}
	}
}
