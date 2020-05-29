package com.lttlgames.editor;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import com.lttlgames.editor.LttlObjectGraphCrawler.FieldsMode;

public class GuiComponentComponent extends GuiComponentObject
{
	/**
	 * Used when creating a root object (no parent)
	 * 
	 * @param object
	 */
	GuiComponentComponent(Object object)
	{
		super(object, null);
	}

	@Override
	void draw()
	{
		guiGroupMap = null;
		if (checkDrawNull()) return;

		drawComponentFrameworkShared();
		drawComponentFieldsShared();

		final LttlComponent component = (LttlComponent) objectRef;

		// set initial collapse state saved if opened earlier in session
		collapsableGroup.setCollapseState(component.guiCollapsed);

		// add listener for when state changes, check if gui change as soon as uncollapses
		collapsableGroup.getToggleButton().addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				// left click
				if (e.getButton() == MouseEvent.BUTTON1)
				{
					if (Lttl.editor.getGui().getPropertiesController().focusedGuiFieldObjects
							.contains(GuiComponentComponent.this)
							&& Lttl.editor.getInput().isControlSwing())
					{
						Lttl.editor.getGui().getPropertiesController()
								.collapseAll();
						setCollapseState(false);
					}
					else
					{
						setCollapseState(!component.guiCollapsed);
					}
				}
			}
		});
		GuiHelper.SetFontSize(collapsableGroup.getToggleButton(), 11);
		collapsableGroup.getToggleButton().setFont(
				collapsableGroup.getToggleButton().getFont()
						.deriveFont(Font.BOLD));
		updateComponentToggleButton();

		// Create the popup menu.
		popup = new JPopupMenu();

		// ID display
		final JMenuItem idMenuItem = new JMenuItem("ID: " + component.getId());
		idMenuItem.setEnabled(false);
		popup.add(idMenuItem);

		// Add Method Menu Items
		addMethodMenuItems(popup);

		// CAPTURE COMPONENT
		final JMenuItem captureMenuItem = new JMenuItem("Capture");
		captureMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Lttl.editor.getGui().setCaptureComponent(component.getId());
			}
		});
		popup.add(captureMenuItem);

		// ENABLE BUTTON
		final JMenuItem enableMenuItem = new JMenuItem();
		enableMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (enableMenuItem.getText().equals("Enable"))
				{
					component.enable();
				}
				else
				{
					component.disable();
				}
			}
		});
		popup.add(enableMenuItem);

		// Copy button
		final JMenuItem copyMenuItem = new JMenuItem();
		copyMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// check if transform
				if (component == component.transform())
				{
					// Copy whole transform, not a component
					ArrayList<LttlTransform> list = new ArrayList<LttlTransform>();
					list.add(component.transform());
					Lttl.editor.getGui().setCopyTransforms(list);
				}
				else
				{
					Lttl.editor.getGui().setCopyComponent(component, false);
				}
			}
		});
		popup.add(copyMenuItem);

		// Cut button
		final JMenuItem cutMenuItem = new JMenuItem("Cut Component");
		cutMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				// check if not transform
				if (component == component.transform())
				{
					Lttl.Throw();
				}
				Lttl.editor.getGui().setCopyComponent(component, true);
			}
		});
		popup.add(cutMenuItem);

		// Order buttons
		// check if transform, since transform does not change order
		if (component != component.transform())
		{
			final JMenu orderMenu = new JMenu("Order");
			orderMenu
					.setToolTipText("This is the order the component's update methods will be called.");
			final int index = component.transform().components
					.indexOf(component);
			final JMenuItem moveUpMenuItem = new JMenuItem("Move Up");
			moveUpMenuItem.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					component.transform().moveComponentOrder(index, index - 1);
					Lttl.editor.getGui().getPropertiesController().draw(false);
				}
			});
			moveUpMenuItem.setEnabled(index - 1 >= 0);
			orderMenu.add(moveUpMenuItem);
			final JMenuItem moveDownMenuItem = new JMenuItem("Move Down");
			moveDownMenuItem.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					component.transform().moveComponentOrder(index, index + 1);
					Lttl.editor.getGui().getPropertiesController().draw(false);
				}
			});
			moveDownMenuItem
					.setEnabled(index + 1 < component.transform().components
							.size());
			orderMenu.add(moveDownMenuItem);
			orderMenu.setEnabled(moveUpMenuItem.isEnabled()
					|| moveDownMenuItem.isEnabled());
			popup.add(orderMenu);
		}

		// Delete button
		final JMenu deleteMenu = new JMenu();
		JMenuItem confirmDeleteMenuItem = new JMenuItem("Confirm");
		confirmDeleteMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (component.hasDependents())
				{
					GuiHelper
							.showAlert(
									null,
									"Component Required",
									"This component can't be delete because it is required by other components.",
									JOptionPane.WARNING_MESSAGE);
					return;
				}

				// check if there are any dependencies in any scene
				int refDependecyCount = 0;
				// if LttlTransform check the entire tree, otherwise just check the single component
				if (component.getClass() == LttlTransform.class)
				{
					refDependecyCount = ((LttlTransform) component)
							.checkRefDependencyTree(true,
									FieldsMode.AllButIgnore);
				}
				else
				{
					refDependecyCount = component.checkRefDependency(true,
							FieldsMode.AllButIgnore);
				}
				if (refDependecyCount > 0)
				{
					if (refDependecyCount > 0)
					{
						if (GuiHelper
								.showOptionModal(
										"Component Reference Dependencies",
										"There are "
												+ refDependecyCount
												+ " component references (may not be gui) in this or another scene.  Would you like to continue deleting and hard remove the references?") > 0) { return; }
					}
				}

				component.destroyComp(true, false);
			}
		});
		deleteMenu.add(confirmDeleteMenuItem);
		popup.add(deleteMenu);

		// add export/import menu options
		addExportImportMenuItem();

		// Add listener so the popup menu can come up.
		collapsableGroup.getToggleButton().addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				maybeShowPopup(e);
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				maybeShowPopup(e);
			}

			private void maybeShowPopup(MouseEvent e)
			{
				if (e.isPopupTrigger())
				{
					// update state of enable menu item
					enableMenuItem.setText(component.isEnabledSelf() ? "Disable"
							: "Enable");

					// check if it's the transform
					if (component == component.transform())
					{
						deleteMenu.setText("Delete Entire Object");
						copyMenuItem.setText("Copy Object");
						cutMenuItem.setVisible(false);
					}
					else
					{
						deleteMenu.setText("Delete");
						copyMenuItem.setText("Copy Component");
						cutMenuItem.setVisible(true);
					}

					// show popup menu
					popup.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});
	}

	public LttlComponent getComponent()
	{
		return (LttlComponent) objectRef;
	}

	void setCollapseState(boolean guiCollapsed)
	{
		LttlComponent comp = getComponent();
		boolean dif = guiCollapsed != comp.guiCollapsed;
		comp.guiCollapsed = guiCollapsed;
		if (dif && !comp.guiCollapsed)
		{
			checkNonGuiChanged();
		}
		collapsableGroup.setCollapseState(comp.guiCollapsed);
	}
}
