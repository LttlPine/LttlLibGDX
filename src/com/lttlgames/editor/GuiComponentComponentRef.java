package com.lttlgames.editor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import com.lttlgames.editor.GuiMenuBarController.CameraAction;
import com.lttlgames.helpers.EaseType;

public class GuiComponentComponentRef extends GuiFieldObject<LttlComponent>
{
	// JLabel componentRefLabel;
	JTextField componentRefLabel;

	GuiComponentComponentRef(ProcessedFieldType pft, Object hostObject,
			int index, GuiComponentObject parent)
	{
		super(pft, hostObject, index, parent);
	}

	@Override
	void draw()
	{
		drawBeginningDefault();

		componentRefLabel = new JTextField();
		// componentRefLabel.setIcon(UIManager.getIcon("Tree.leafIcon"));
		GuiHelper.SetFontSize(componentRefLabel, GuiHelper.fieldFontSize);
		// componentRefLabel.setBorder(BorderFactory.createEtchedBorder());
		componentRefLabel.setBackground(new Color(1, 1, 1, .5f));
		componentRefLabel.setFocusable(false);
		componentRefLabel.setPreferredSize(new Dimension(0, 25));
		componentRefLabel.setEditable(false);

		// Set Label Text
		if (objectRef == null)
		{
			componentRefLabel.setText("None");
			isNull = true;
		}
		else if (Lttl.scenes.findComponentByIdAllScenes(objectRef.getId()) != objectRef)
		{
			// if the object is not suppose to be loaded or pointing to a different object then null out objectRef and
			// redraw, especially because of the Undo feature with arraylist
			objectRef = null;
			draw();
			return;
		}
		else
		{
			componentRefLabel.setText(((LttlComponent) objectRef).transform()
					.getName());
		}

		// set tooltip for what type of component is accepted
		if (objectRef != null)
		{
			componentRefLabel.setToolTipText(objectRef.getClass()
					.getSimpleName() + " [" + objectRef.getId() + "]");
		}
		else
		{
			componentRefLabel.setToolTipText(null);
		}

		componentRefLabel.setText(componentRefLabel.getText() + " ("
				+ getObjectClass().getSimpleName() + ")");

		getPanel().add(componentRefLabel,
				GuiHelper.GetGridBagConstraintsFieldValue());

		// Create the popup menu.
		// final LttlComponent objectRefFinal = objectRef;
		popup = new JPopupMenu();

		// ID display
		final JMenuItem idMenuItem = new JMenuItem();
		idMenuItem.setEnabled(false);
		popup.add(idMenuItem);

		// Set Button
		// only enable if a single capture component and it matches the type (include subclasses)
		final JMenuItem setMenuItem = new JMenuItem("Set");
		setMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (shouldAutoUndo())
				{
					// save previous value
					undoValue = (objectRef == null) ? -1 : objectRef.getId();
				}

				setValue(Lttl.editor.getGui().getCaptureComponent(
						(Class<? extends LttlComponent>) getObjectClass()));
				setObjectRef();
				onEditorValueChange();
				draw();

				if (shouldAutoUndo())
				{
					// create undo
					registerUndo(new UndoState(GuiComponentComponentRef.this,
							getValue().getId()));
				}
			}
		});
		popup.add(setMenuItem);

		// Select Button
		final JMenuItem selectMenuItem = new JMenuItem("Select");
		selectMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Lttl.editor.getGui().getSelectionController()
						.setSelection(objectRef.t());
			}
		});
		popup.add(selectMenuItem);

		// Capture Button
		final JMenuItem captureMenuItem = new JMenuItem("Capture");
		captureMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Lttl.editor.getGui().setCaptureComponent(objectRef);
			}
		});
		popup.add(captureMenuItem);

		// Search Button
		final JMenuItem searchMenuItem = new JMenuItem("Search");
		searchMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (shouldAutoUndo())
				{
					// save previous value
					undoValue = (objectRef == null) ? -1 : objectRef.getId();
				}

				LttlComponent response = (LttlComponent) Lttl.editor
						.getGui()
						.getMenuBarController()
						.findComponentDialog(
								true,
								CameraAction.Nothing,
								false,
								"Search",
								getObjectClass(),
								Lttl.editor.getGui().getPropertiesController()
										.getFocusedTransforms().get(0));

				if (response != null)
				{
					setValue(response);
					setObjectRef();
					onEditorValueChange();
					draw();

					if (shouldAutoUndo())
					{
						// create undo
						registerUndo(new UndoState(
								GuiComponentComponentRef.this, getValue()
										.getId()));
					}
				}
			}
		});
		popup.add(searchMenuItem);

		// LOOK AT
		final JMenuItem lookAtMenuItem = new JMenuItem("Look At");
		lookAtMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Lttl.editor.getCamera().lookAt(objectRef.t(), EaseType.QuadOut,
						1f);
			}
		});
		popup.add(lookAtMenuItem);

		// Go To
		final JMenuItem goToMenuItem = new JMenuItem("Go To");
		goToMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Lttl.editor.getCamera().goTo(objectRef.t(), EaseType.QuadInOut,
						1);
			}
		});
		popup.add(goToMenuItem);

		// MOVE TO VIEW
		final JMenuItem moveToViewMenuItem = new JMenuItem("Move To View");
		moveToViewMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				objectRef
						.t()
						.tweenPosTo(
								objectRef.t().worldToLocalPosition(
										Lttl.editor.getCamera().position, true),
								1).setEase(EaseType.QuadInOut).start();
			}
		});
		popup.add(moveToViewMenuItem);

		// Clear Button
		final JMenuItem clearMenuItem = new JMenuItem("Clear");
		clearMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// save previous value
				undoValue = (getValue() == null) ? -1 : getValue().getId();

				setValue(null);
				setObjectRef();
				onEditorValueChange();
				draw();

				// create undo
				registerUndo(new UndoState(GuiComponentComponentRef.this, -1));
			}
		});
		popup.add(clearMenuItem);

		// Add listener so the popup menu can come up.
		componentRefLabel.addMouseListener(new MouseAdapter()
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
				// right click only
				if (!e.isPopupTrigger())
				{
					// double click also fires as if clicking the select menu item
					if (e.getClickCount() == 2 && objectRef != null)
					{
						selectMenuItem.doClick();

						// then isolates the component
						if (objectRef.t().isFocusedInEditor())
						{
							Lttl.editor.getGui().getPropertiesController()
									.collapseAll();
							for (GuiFieldObject<?> gfo : Lttl.editor.getGui()
									.getPropertiesController().focusedGuiFieldObjects)
							{
								if (gfo instanceof GuiComponentComponent)
								{
									GuiComponentComponent gcc = (GuiComponentComponent) gfo;
									if (gcc.getComponent() == objectRef)
									{
										gcc.setCollapseState(false);
										break;
									}
								}
							}
						}
					}
					return;
				}

				// check if there is a single capturedComponent and if it is the right class
				setMenuItem
						.setEnabled(Lttl.editor
								.getGui()
								.getCaptureComponent(
										(Class<? extends LttlComponent>) getObjectClass()) != null);

				// hide or show ID and capture menu items
				if (objectRef == null)
				{
					captureMenuItem.setEnabled(false);
					selectMenuItem.setEnabled(false);
					moveToViewMenuItem.setEnabled(false);
					goToMenuItem.setEnabled(false);
					lookAtMenuItem.setEnabled(false);
					clearMenuItem.setEnabled(false);
					idMenuItem.setText("ID: null");
				}
				else
				{
					captureMenuItem.setEnabled(true);
					selectMenuItem.setEnabled(true);
					moveToViewMenuItem.setEnabled(true);
					goToMenuItem.setEnabled(true);
					lookAtMenuItem.setEnabled(true);
					clearMenuItem.setEnabled(true);
					idMenuItem.setText("ID: " + objectRef.getId());
				}

				// show popup menu
				popup.show(e.getComponent(), e.getX(), e.getY());
			}
		});

		getPanel().revalidate();
		getPanel().repaint();
	}

	@Override
	void processUndoRedo(Object value)
	{
		if ((Integer) value == -1)
		{
			setValue(null);
		}
		else
		{
			setValue(Lttl.scenes.findComponentByIdAllScenes((Integer) value));
		}
		callbacks();
		processChangeCallback(1);
	}
}
