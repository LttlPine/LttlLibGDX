package com.lttlgames.editor;

import java.awt.Component;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;

import com.lttlgames.helpers.LttlCallback;
import com.lttlgames.helpers.LttlMath;

@SuppressWarnings("serial")
public class GuiLttlMultiSlider extends JComponent
{
	private LttlCallback listener;
	private int min;
	private int max;
	private int threshold;

	/**
	 * CallBack [0=rightclicked on no node (returns [mouse event]); 1=rightclicked on node (returns [mouse
	 * event,JSlider]); 2=double left clicked node (return [mouse event, JSlider]); 3=node state changed [jslider];
	 * 4=node start drag [jslider]; 5=node end drag [jslider];]
	 * 
	 * @param listener
	 */
	public GuiLttlMultiSlider(int min, int max, LttlCallback listener)
	{
		super.setLayout(null);
		this.min = min;
		this.max = max;
		threshold = LttlMath.floor(max / 100f);
		this.listener = listener;
		addSlider(0);
	}

	public JSlider getSlider(int index)
	{
		return (JSlider) getComponent(index);
	}

	public void setValue(int index, int value)
	{
		getSlider(index).setValue(value);
	}

	public void setValue(JSlider slider, int value)
	{
		slider.setValue(value);
	}

	public void addValue(int value)
	{
		addSlider(value);
	}

	@Override
	public boolean isOptimizedDrawingEnabled()
	{
		return false;
	}

	@Override
	public void doLayout()
	{
		Insets i = getInsets();
		int x = i.left, y = i.top, width = getWidth() - x - i.right, height = getHeight()
				- y - i.bottom;
		for (int ix = 0, n = getComponentCount(); ix < n; ix++)
			getComponent(ix).setBounds(x, y, width, height);
	}

	class SubSlider extends JSlider
	{
		public SubSlider()
		{
			super(min, max);
		}

		private SubSlider active;

		@Override
		protected void processMouseEvent(MouseEvent e)
		{
			SubSlider sl = getClosestSlider(e);
			if (e.getID() == MouseEvent.MOUSE_PRESSED)
			{
				active = sl;
				if (active != null)
				{
					listener.callback(4, active);
				}
			}
			else if (e.getID() == MouseEvent.MOUSE_RELEASED)
			{
				if (active != null)
				{
					listener.callback(5, active);
				}
				active = null;
			}
			if (e.getID() == MouseEvent.MOUSE_CLICKED)
			{
				if (e.getButton() == MouseEvent.BUTTON3)
				{
					if (sl == null)
					{
						// right clicked not on any node
						listener.callback(0, e);
					}
					else
					{
						// right clicked on node
						listener.callback(1, e, sl);
					}
				}
				else
				{
					if (sl != null && e.getClickCount() == 2)
					{
						listener.callback(2, e, sl);
						return;
					}
				}
			}
			if (sl != null) sl.realProcessMouseEvent(e);
		}

		private void realProcessMouseEvent(MouseEvent e)
		{
			e.setSource(this);
			super.processMouseEvent(e);
		}

		@Override
		protected void processMouseMotionEvent(MouseEvent e)
		{
			if (e.getID() == MouseEvent.MOUSE_MOVED) toAllSliders(e);
			else
			{
				if (active == null) active = getClosestSlider(e);
				if (active != null) active.realProcessMouseMotionEvent(e);
			}
		}

		private void realProcessMouseMotionEvent(MouseEvent e)
		{
			e.setSource(this);
			super.processMouseMotionEvent(e);
		}
	}

	final void toAllSliders(MouseEvent e)
	{
		for (int ix = 0, n = getComponentCount(); ix < n; ix++)
		{
			SubSlider sub = (SubSlider) getComponent(ix);
			sub.realProcessMouseMotionEvent(e);
		}
	}

	public void removeSlider(int index)
	{
		removeSlider(getSlider(index));
	}

	public void removeSlider(JSlider sl)
	{
		if (getComponentCount() <= 1) return;// must keep the last slider
		remove(sl);
		JSlider slider = (JSlider) getComponent(getComponentCount() - 1);
		slider.setOpaque(true);
		slider.setPaintTrack(true);
		revalidate();
		repaint();
	}

	final SubSlider getClosestSlider(MouseEvent e)
	{
		SubSlider closestSlider = (SubSlider) getComponent(0);
		int value = getValueForXPosition(e.getX());
		for (int ix = 1, n = getComponentCount(); ix < n; ix++)
		{
			SubSlider s = (SubSlider) getComponent(ix);
			if (LttlMath.abs(s.getValue() - value) < LttlMath.abs(closestSlider
					.getValue() - value))
			{
				closestSlider = s;
			}
		}
		if (LttlMath.abs(closestSlider.getValue() - value) <= threshold) { return closestSlider; }
		return null;
	}

	public int getValueForXPosition(int x)
	{
		SubSlider s = (SubSlider) getComponent(0);
		BasicSliderUI bsUI = (BasicSliderUI) s.getUI();
		return bsUI.valueForXPosition(x);
	}

	void addSlider(Point point)
	{
		BasicSliderUI bsUI = (BasicSliderUI) ((JSlider) getComponent(0))
				.getUI();
		addSlider(bsUI.valueForXPosition(point.x));
	}

	JSlider addSlider(int value)
	{
		final JSlider slider = new SubSlider();

		// add change listener
		slider.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent e)
			{
				listener.callback(3, slider);
			}
		});

		slider.setFocusable(false);
		slider.setValue(value);
		if (getComponentCount() != 0)
		{
			slider.setOpaque(false);
			slider.setPaintTrack(false);
		}
		super.add(slider, 0);
		revalidate();
		repaint();

		return slider;
	}

	@Override
	public synchronized void addMouseMotionListener(MouseMotionListener l)
	{
		for (Component s : getComponents())
		{
			s.addMouseMotionListener(l);
		}
	}

	public int getSliderCount()
	{
		return getComponentCount();
	}
}