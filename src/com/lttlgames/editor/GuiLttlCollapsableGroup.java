package com.lttlgames.editor;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.jdesktop.swingx.JXCollapsiblePane;

import com.lttlgames.helpers.LttlCallback;

public class GuiLttlCollapsableGroup
{
	private JPanel panel;
	private JPanel headerPanel;
	private JPanel headerExtraPanel;
	public String label;
	private JButton toggleButton;
	private JXCollapsiblePane collapsePanel;
	private ArrayList<LttlCallback> listeners = new ArrayList<LttlCallback>();

	/**
	 * @param name
	 * @param startCollapsed
	 * @param useCustomMouseListener
	 *            if true, will not set a mouse listener on {@link #getToggleButton()}, that will be done manually
	 */
	public GuiLttlCollapsableGroup(String name, boolean startCollapsed,
			boolean useCustomMouseListener)
	{
		this(name, startCollapsed, false, 5, true, false, false);
	}

	/**
	 * @param name
	 * @param startCollapsed
	 * @param useCustomMouseListener
	 *            if true, will not set a mouse listener on {@link #getToggleButton()}, that will be done manually
	 * @param indent
	 * @param border
	 * @param addHeadExtra
	 *            will add an extra panel to the right of the normal header to add extra components, this also restricts
	 *            the group label right click area to itself
	 * @param headExtraRightAligned
	 */
	public GuiLttlCollapsableGroup(String name, boolean startCollapsed,
			boolean useCustomMouseListener, int indent, boolean border,
			boolean addHeadExtra, boolean headExtraRightAligned)
	{
		GridBagConstraints gbcbody = new GridBagConstraints(0, -1, 1, 0, 1, 0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, indent, 0, 0), 0, 0);

		// set name
		this.label = name;

		// create panels
		panel = new JPanel(new GridBagLayout());
		headerPanel = new JPanel(new GridBagLayout());

		// create toggle button
		toggleButton = new JButton();
		toggleButton.setContentAreaFilled(false);
		toggleButton.setMargin(new Insets(-6, -13, -5, 0));
		toggleButton.setHorizontalAlignment(SwingConstants.LEFT);
		if (addHeadExtra)
		{
			GridBagConstraints gbc = new GridBagConstraints();
			// add toggle button, then header extra, then filler
			headerPanel.add(toggleButton, gbc);

			gbc.fill = GridBagConstraints.HORIZONTAL;

			if (headExtraRightAligned)
			{
				gbc.weightx = 1;
				headerPanel.add(new JLabel(), gbc);
			}

			gbc.weightx = 0;
			headerPanel.add(headerExtraPanel = new JPanel(new GridBagLayout()),
					gbc);

			if (!headExtraRightAligned)
			{
				gbc.weightx = 1;
				headerPanel.add(new JLabel(), gbc);
			}
		}
		else
		{
			// just the toggle button
			headerPanel.add(toggleButton, new GridBagConstraints(0, 0, 1, 1, 1,
					0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
					new Insets(0, 0, 0, 0), 0, 0));
		}

		// add header panel
		getPanel().add(
				headerPanel,
				new GridBagConstraints(0, 0, 1, 1, 1, 0,
						GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
						new Insets(0, 0, 0, 0), 0, 0));

		// setup internal collapsable panel
		collapsePanel = new JXCollapsiblePane();
		collapsePanel.setAnimated(false);
		collapsePanel.setLayout(new GridBagLayout());
		if (border)
		{
			collapsePanel.setBorder(BorderFactory.createEtchedBorder());
		}
		// add listener for when collapse pane is entirely collapsed, then resize it to nothing so it doesn't affect
		// scrollpane
		collapsePanel.addPropertyChangeListener("collapsed",
				new PropertyChangeListener()
				{
					@Override
					public void propertyChange(PropertyChangeEvent evt)
					{
						if (collapsePanel.isCollapsed())
						{
							collapsePanel.setPreferredSize(new Dimension(0, 0));
						}
					}
				});
		getPanel().add(collapsePanel, gbcbody);

		// set collapse state
		setCollapseState(startCollapsed);

		// add listener for when state changes
		if (!useCustomMouseListener)
		{
			toggleButton.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mousePressed(MouseEvent e)
				{
					// left click
					if (e.getButton() == MouseEvent.BUTTON1)
					{
						setCollapseState(!collapsePanel.isCollapsed());
					}
				}
			});
		}

		updateLabel();
	}

	/**
	 * Only does a change if necessary. Get CollapseState from {@link #isCollapsed()}
	 * 
	 * @param guiCollapsed
	 */
	void setCollapseState(boolean guiCollapsed)
	{
		if (guiCollapsed == isCollapsed()) return;

		if (guiCollapsed)
		{
			// collapse
			collapsePanel.setCollapsed(true);
			for (LttlCallback l : listeners)
			{
				l.callback(-1);
			}
			updateLabel();
		}
		else
		{
			// open
			collapsePanel.setPreferredSize(null); // resize collapse pane before showing
			collapsePanel.setCollapsed(false);
			for (LttlCallback l : listeners)
			{
				l.callback(-1);
			}
			updateLabel();
		}
	}

	void updateLabel()
	{
		if (collapsePanel.isCollapsed())
		{
			toggleButton.setText("+ " + label);
		}
		else
		{
			toggleButton.setText("- " + label);
		}
	}

	public JButton getToggleButton()
	{
		return toggleButton;
	}

	public JPanel getPanel()
	{
		return panel;
	}

	public JXCollapsiblePane getCollapsePanel()
	{
		return collapsePanel;
	}

	public JPanel getHeaderPanel()
	{
		return headerPanel;
	}

	public JPanel getHeaderExtraPanel()
	{
		return headerExtraPanel;
	}

	public boolean isCollapsed()
	{
		return collapsePanel.isCollapsed();
	}

	public void addCollapseListener(LttlCallback listener)
	{
		listeners.add(listener);
	}

	/**
	 * Sets Label and repaints.
	 * 
	 * @param label
	 */
	public void setLabel(String label)
	{
		this.label = label;
		updateLabel();
	}

	public String getLabel()
	{
		return this.label;
	}
}
