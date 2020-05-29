package com.lttlgames.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jdesktop.swingx.JXStatusBar;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.TimeUtils;

class GuiStatusBarController
{
	private static JXStatusBar statusBar;
	private JLabel fpsLabel;
	private JLabel mouseLabel;
	private GuiLttlSlider cameraRatioSlider;
	private long updateIntervalMillis = 100;
	private long nextUpdateTime = 0;
	private JPanel transformNavPanel;
	private JPanel modePanel;
	JButton playStopButton;
	JToggleButton pauseButton;
	JButton stepButton;
	JButton transformPrev;
	JButton transformNext;
	private int currentTransformHistory = 0;
	private boolean transformHistoryNavPressed = false;
	private ArrayList<int[]> transformHistory = new ArrayList<int[]>();
	private JPanel handlePanel;
	JToggleButton handlePosButton;
	JToggleButton handleSclButton;
	JToggleButton handleRotButton;
	private float saved = -1;

	GuiStatusBarController()
	{
		// create and add status bar to frame if first time
		if (statusBar == null)
		{
			statusBar = new JXStatusBar();
			Lttl.editor.getGui().leftPanel.add(statusBar, BorderLayout.SOUTH);
		}

		updateLeftPanelBackgroundColor();
		createComponents();
	}

	private void createComponents()
	{
		// clear old stuff if any
		statusBar.removeAll();

		// Fill with no inserts
		GridBagConstraints gbc = new GridBagConstraints(0, 0, 1, 1, .5f, 0,
				GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
				new Insets(0, 0, 0, 0), 0, 0);
		JXStatusBar.Constraint c = new JXStatusBar.Constraint();
		Insets insets = null;

		{
			// FPS label
			fpsLabel = new JLabel();
			c.setFixedWidth(45);
			statusBar.add(fpsLabel, c);
		}

		{
			// ratio slider
			cameraRatioSlider = new GuiLttlSlider(SwingConstants.HORIZONTAL,
					.01f, 0, 1, Lttl.editor.getSettings().editorViewRatio, true);
			cameraRatioSlider.addChangeListener(new ChangeListener()
			{
				@Override
				public void stateChanged(ChangeEvent e)
				{
					Lttl.editor.getSettings().editorViewRatio = cameraRatioSlider
							.getValueFloat();
					Lttl.editor.getSettings().onGuiEditorViewRatio();
				}
			});
			c = new JXStatusBar.Constraint();
			c.setFixedWidth(100);
			statusBar.add(cameraRatioSlider, c);
		}

		{
			// transform previous and next buttons
			transformNavPanel = new JPanel(new GridBagLayout());
			transformPrev = new JButton("<");
			insets = new Insets(-3, -3, -3, -3);
			transformPrev.setMargin(insets);
			transformPrev.setEnabled(false);
			transformPrev.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					transformHistoryNavPressed = true;
					currentTransformHistory--;
					setSelectionToCurrentTransformHistory();
				}
			});
			transformNavPanel.add(transformPrev, gbc);
			transformNext = new JButton(">");
			transformNext.setMargin(insets);
			transformNext.setEnabled(false);
			transformNext.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					transformHistoryNavPressed = true;
					currentTransformHistory++;
					setSelectionToCurrentTransformHistory();
				}
			});
			gbc.gridx = 1;
			transformNavPanel.add(transformNext, gbc);
			c = new JXStatusBar.Constraint();
			c.setFixedWidth(60);
			statusBar.add(transformNavPanel, c);
		}

		{
			// Play/Stop,Pause,Step Buttons
			modePanel = new JPanel(new GridBagLayout());
			gbc = new GridBagConstraints(0, 0, 1, 1, .5f, 0,
					GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
					new Insets(0, 0, 0, 0), 0, 0);
			// PLAY/STOP Button
			playStopButton = new JButton(Lttl.game.isPlaying() ? "Stop"
					: "Play");
			insets = new Insets(-3, -3, -3, -3);
			playStopButton.setMargin(insets);
			playStopButton.setEnabled(true);
			playStopButton.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					if (!Lttl.game.isPlaying())
					{
						Lttl.game.startPlayingInEditorMode();
						playStopButton.setText("Stop");
						pauseButton.setSelected(false);
						pauseButton.setEnabled(true);
						stepButton.setEnabled(true);
					}
					else
					{
						Lttl.game.stopPlayingInEditorMode(true);
						playStopButton.setText("Play");
						pauseButton.setSelected(false);
						pauseButton.setEnabled(false);
						stepButton.setEnabled(false);

					}
					updateLeftPanelBackgroundColor();
				}
			});
			modePanel.add(playStopButton, gbc);

			// Pause Button
			pauseButton = new JToggleButton("Pause", false);
			pauseButton.setMargin(insets);
			pauseButton.setEnabled(Lttl.game.isPlaying());
			pauseButton.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					if (pauseButton.isSelected())
					{
						Lttl.loop.isEditorPaused = true;
						Lttl.loop.stepOneFrame = false;
						Lttl.editor.onPause();
					}
					else
					{
						Lttl.loop.isEditorPaused = false;
						Lttl.loop.stepOneFrame = false;
						Lttl.editor.onResume();
					}
					updateLeftPanelBackgroundColor();
				}
			});
			gbc.gridx = 1;
			modePanel.add(pauseButton, gbc);

			// STEP Button
			stepButton = new JButton("Step");
			stepButton.setMargin(insets);
			stepButton.setEnabled(Lttl.game.isPlaying());
			stepButton.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					if (Lttl.editor.isPaused())
					{
						Lttl.loop.stepOneFrame = true;
					}
					else
					{
						pauseButton.doClick();
					}
					updateLeftPanelBackgroundColor();
				}
			});
			gbc.gridx = 2;
			modePanel.add(stepButton, gbc);

			// add modePanel to status bar
			c = new JXStatusBar.Constraint();
			c.setFixedWidth(170);
			statusBar.add(modePanel, c);
		}

		{
			// HANDLE TOGGLES
			handlePanel = new JPanel(new GridBagLayout());
			gbc = new GridBagConstraints(0, 0, 1, 1, .5f, 0,
					GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
					new Insets(0, 0, 0, 0), 0, 0);
			insets = new Insets(-3, -3, -3, -3);
			Dimension dimension = new Dimension(1, 20);

			// POS HANDLE
			handlePosButton = new JToggleButton("", true);
			handlePosButton.setPreferredSize(dimension);
			handlePosButton.setBackground(GuiHelper.ConvertColorToAwt(
					Lttl.editor.getSettings().handlePosColor, false));
			handlePosButton.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					handleRotButton.setSelected(false);
					handleSclButton.setSelected(false);
					handlePosButton.setSelected(true);

					Lttl.editor.getGui().getSelectionController()
							.updateHandles();
				}
			});
			handlePanel.add(handlePosButton, gbc);

			// SCL HANDLE
			handleSclButton = new JToggleButton("", false);
			handleSclButton.setPreferredSize(dimension);
			handleSclButton.setBackground(GuiHelper.ConvertColorToAwt(
					Lttl.editor.getSettings().handleSclColor, false));
			handleSclButton.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					handleRotButton.setSelected(false);
					handlePosButton.setSelected(false);
					handleSclButton.setSelected(true);

					Lttl.editor.getGui().getSelectionController()
							.updateHandles();
				}
			});
			gbc.gridx = 1;
			handlePanel.add(handleSclButton, gbc);

			// ROT HANDLE
			handleRotButton = new JToggleButton("", false);
			handleRotButton.setPreferredSize(dimension);
			handleRotButton.setBackground(GuiHelper.ConvertColorToAwt(
					Lttl.editor.getSettings().handleRotColor, false));
			handleRotButton.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					handleSclButton.setSelected(false);
					handlePosButton.setSelected(false);
					handleRotButton.setSelected(true);
					Lttl.editor.getGui().getSelectionController()
							.updateHandles();
				}
			});
			gbc.gridx = 2;
			handlePanel.add(handleRotButton, gbc);

			// add modePanel to status bar
			c = new JXStatusBar.Constraint();
			c.setFixedWidth(80);
			statusBar.add(handlePanel, c);
		}

		{
			// Mouse Position label
			mouseLabel = new JLabel();
			c = new JXStatusBar.Constraint(
					JXStatusBar.Constraint.ResizeBehavior.FILL);
			statusBar.add(mouseLabel, c);
		}
	}

	void update()
	{
		if (TimeUtils.millis() < nextUpdateTime) return;
		nextUpdateTime = TimeUtils.millis() + updateIntervalMillis;

		// Update FPS
		fpsLabel.setText("FPS: " + Gdx.graphics.getFramesPerSecond());

		// Update Mouse Label
		if (Lttl.input.isMouseInEditorViewport())
		{
			mouseLabel.setText("("
					+ String.format("%.3f", Lttl.input.getEditorX()) + ","
					+ String.format("%.3f", Lttl.input.getEditorY()) + ")");
		}
		else if (Lttl.input.isMouseInPlayViewport())
		{
			mouseLabel.setText("(" + String.format("%.3f", Lttl.input.getX())
					+ "," + String.format("%.3f", Lttl.input.getY()) + ")");
		}

		if (saved >= 0)
		{
			if (saved == 0)
			{
				// return normal color
				updateLeftPanelBackgroundColor();
			}
			saved--;
		}
	}

	/**
	 * This is called whenever a single selected transform changes
	 */
	void updateTransformHistory(ArrayList<LttlTransform> selectedTransforms)
	{
		if (transformHistoryNavPressed)
		{
			// this was triggered by user clicking transform history nav buttons, so do nothing
		}
		else
		{
			// this is if it wasn't triggered by user clicking transform history nav buttons (could be tree or in
			// editor, etc)

			// need to clear all history after last transform selected in history
			for (int i = transformHistory.size() - 1; i > currentTransformHistory; i--)
			{
				transformHistory.remove(i);
			}

			// add new transforms to end
			int[] ids = new int[selectedTransforms.size()];
			for (int i = 0; i < selectedTransforms.size(); i++)
			{
				ids[i] = selectedTransforms.get(i).getId();
			}
			transformHistory.add(ids);

			// move the current index to end
			currentTransformHistory = transformHistory.size() - 1;
		}

		// update buttons
		updateTransformNavButtons();

		transformHistoryNavPressed = false;
	}

	private void updateTransformNavButtons()
	{
		transformNext.setEnabled(false);
		transformPrev.setEnabled(false);
		if (transformHistory.size() == 0) return;

		if (currentTransformHistory < transformHistory.size() - 1)
		{
			transformNext.setEnabled(true);
		}

		if (currentTransformHistory > 0)
		{
			transformPrev.setEnabled(true);
		}
	}

	void updateLeftPanelBackgroundColor()
	{
		if (Lttl.game.isPlaying())
		{
			if (Lttl.editor.isPaused())
			{
				Lttl.editor.getGui().leftPanel.setBackground(GuiHelper
						.ConvertColorToAwt(
								Lttl.editor.getSettings().pauseModeBackground,
								true));
			}
			else
			{
				Lttl.editor.getGui().leftPanel.setBackground(GuiHelper
						.ConvertColorToAwt(
								Lttl.editor.getSettings().playModeBackground,
								true));
			}
		}
		else
		{
			Lttl.editor.getGui().leftPanel.setBackground(null);
		}
	}

	public void saved()
	{
		saved = 2;
		Lttl.editor.getGui().leftPanel.setBackground(Color.GREEN);
	}

	private void setSelectionToCurrentTransformHistory()
	{
		ArrayList<LttlTransform> selection = new ArrayList<LttlTransform>();
		int[] ids = transformHistory.get(currentTransformHistory);
		for (int i = 0; i < ids.length; i++)
		{
			LttlTransform transform = (LttlTransform) Lttl.scenes
					.findComponentByIdAllScenes(ids[i]);
			if (transform != null)
			{
				selection.add(transform);
			}
		}
		Lttl.editor.getGui().getSelectionController().setSelection(selection);
	}
}
