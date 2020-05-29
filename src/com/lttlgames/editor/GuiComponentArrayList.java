package com.lttlgames.editor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JToggleButton;

import org.jdesktop.swingx.JXCollapsiblePane;

import com.lttlgames.editor.annotations.GuiHideArrayListControls;
import com.lttlgames.editor.annotations.GuiReadOnly;
import com.lttlgames.helpers.LttlHelper;

public class GuiComponentArrayList extends GuiComponentObject
{
	ArrayList<CollectionItem> collectionItems;
	private CollectionItem movingCI;
	private JButton addButton;

	GuiComponentArrayList(ProcessedFieldType pft, Object hostObject, int index,
			GuiComponentObject parent)
	{
		super(pft, hostObject, index, parent);
	}

	@Override
	void draw()
	{
		if (checkDrawNull()) return;

		drawComponentFrameworkShared();
		drawComponentObjectShared();

		// disable popup menu if read only
		if (getAnnotationOnAncestorsAndSelf(GuiReadOnly.class) == null)
		{
			createPopupMenu();
		}

		GridBagConstraints gbc = GuiHelper.GetGridBagConstraintsFieldValue();
		gbc.gridx = 1;
		gbc.weightx = 0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.EAST;

		GuiHideArrayListControls ann = getField().getAnnotation(
				GuiHideArrayListControls.class);
		if (ann == null || ann.canAdd())
		{
			addButton = new JButton("Add");
			addButton.setToolTipText("Add a "
					+ LttlHelper.toTitleCase(pft.getParam().getCurrentClass()
							.getSimpleName()));
			GuiHelper.SetFontSize(addButton, GuiHelper.fieldFontSize);
			addButton.setMargin(new Insets(-3, -2, -3, -2));
			// add button
			addButton.addActionListener(new ActionListener()
			{
				@SuppressWarnings("rawtypes")
				@Override
				public void actionPerformed(ActionEvent e)
				{
					if (shouldAutoUndo())
					{
						// save old value
						undoValue = new ArrayList((ArrayList) getValue());
					}

					Object newObj = null;
					if (!LttlComponent.class.isAssignableFrom(pft.getParam()
							.getCurrentClass()))
					{
						newObj = LttlObjectGraphCrawler.newInstance(pft
								.getParam().getCurrentClass());
						Lttl.Throw(newObj);
					}
					getAL().add(newObj);
					add(true);

					if (shouldAutoUndo())
					{
						// create undo
						registerUndo(new UndoState(GuiComponentArrayList.this,
								new ArrayList((ArrayList) getValue())));
					}
					onEditorValueChange();
				}
			});
			addButton.setPreferredSize(null);
			collapsableGroup.getHeaderPanel().add(addButton, gbc);
		}

		// maintain original collectionsItem list if this is a redraw, since we don't want to recreate GFOs
		if (collectionItems == null)
		{
			collectionItems = new ArrayList<CollectionItem>();
		}
		ArrayList<CollectionItem> newCollectionItems = new ArrayList<CollectionItem>();

		ArrayList<?> list = getAL();
		for (int i = 0; i < list.size(); i++)
		{
			Object o = list.get(i);

			// error out on forbidden classes
			if (o != null && !GuiHelper.isClassDrawnGui(o.getClass()))
			{
				Lttl.Throw("Forbidden class " + o.getClass().getSimpleName()
						+ " in an arrayList.  Add @GuiHide to this field.");
			}

			// try to maintain GFOs by checking to see if one already exists
			// this will just update order and add and remove any items, but all GFOs will be the same ones, so they
			// will still be open, etc
			CollectionItem ci = getCollectionItem(o, true);
			if (ci == null)
			{
				// create the new collection item and GFO
				ci = new CollectionItem(generateChildGuiFieldObject(
						this.pft.getParam(0), i), ann);
			}
			else
			{
				// update index and label
				ci.gfo.index = i;
				ci.gfo.updateLabel();
			}

			// add to new collection list
			newCollectionItems.add(ci);
			collapsableGroup.getCollapsePanel().add(ci.panel, gbcStatic);
			children.add(ci.gfo);
		}
		// set the new list
		collectionItems = newCollectionItems;
	}

	/**
	 * Returns the collection item and removes it optionally
	 */
	CollectionItem getCollectionItem(Object object, boolean remove)
	{
		if (collectionItems == null || collectionItems.size() == 0)
			return null;

		for (Iterator<CollectionItem> it = collectionItems.iterator(); it
				.hasNext();)
		{
			CollectionItem ci = it.next();

			// don't use getValue(), use objectRef, because getValue() uses the arrayList that is modified, and does not
			// represent the actual GFO
			if ((ci.gfo.getObjectClass() == String.class && ci.gfo.objectRef
					.equals(object))
					|| (ci.gfo.getObjectClass() != String.class && ci.gfo.objectRef == object))
			{
				if (remove) it.remove();
				return ci;
			}
		}
		return null;
	}

	@Override
	void createPopupMenu()
	{
		super.createPopupMenu(collapsableGroup.getToggleButton());

		// add set to null menu option
		addSetToNullMenuItem();

		GuiHideArrayListControls ann = getField().getAnnotation(
				GuiHideArrayListControls.class);

		if ((ann == null || (ann.canDelete() && ann.canClear()))
				&& getAL() != null)
		{
			JMenuItem clearMenuItem = new JMenuItem("Clear");
			clearMenuItem.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					getAL().clear();
					onEditorValueChange();
				}
			});

			popup.add(clearMenuItem);
		}
		// can't have save since exportJSON can't work with an arraylist
	}

	class CollectionItem
	{
		public JXCollapsiblePane panel = new JXCollapsiblePane();
		{
			panel.setAnimated(false);
		}
		public JButton deleteButton = new JButton();
		public JToggleButton moveToggle = new JToggleButton("", false);
		public GuiFieldObject<?> gfo;
		CollectionItem selfCI = this;
		private boolean pendingRemoval = false;

		public CollectionItem(GuiFieldObject<?> child,
				GuiHideArrayListControls ann)
		{
			this.gfo = child;

			// add child field to panel
			panel.setLayout(new GridBagLayout());
			GridBagConstraints gbc = GuiHelper
					.GetGridBagConstraintsFieldLabel();
			gbc.weightx = 1;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			panel.add(child.getPanel(), gbc);

			panel.addPropertyChangeListener("collapsed",
					new PropertyChangeListener()
					{
						@Override
						public void propertyChange(PropertyChangeEvent evt)
						{
							if (collapsableGroup.getCollapsePanel()
									.isCollapsed() && pendingRemoval)
							{
								collapsableGroup.getCollapsePanel().remove(
										panel);
								collapsableGroup.getCollapsePanel()
										.revalidate();
								collapsableGroup.getCollapsePanel().repaint();
							}
						}
					});

			if (ann == null || ann.canMove())
			{
				// move toggle
				moveToggle.addActionListener(new ActionListener()
				{
					@SuppressWarnings("rawtypes")
					@Override
					public void actionPerformed(ActionEvent e)
					{
						if (!moveToggle.isSelected())
						{
							movingCI = null;
							return;
						}

						if (movingCI == null)
						{
							movingCI = selfCI;
						}
						else
						{
							// save old value
							undoValue = new ArrayList((ArrayList) getValue());

							GuiComponentArrayList.this.move(movingCI.gfo.index,
									selfCI.gfo.index);

							// create undo
							registerUndo(new UndoState(
									GuiComponentArrayList.this, new ArrayList(
											(ArrayList) getValue())));
							onEditorValueChange();
							movingCI = null;
						}
					}
				});
				gbc = GuiHelper.GetGridBagConstraintsFieldValue();
				gbc.weightx = 0;
				gbc.fill = GridBagConstraints.NONE;
				moveToggle.setPreferredSize(new Dimension(15, 15));
				panel.add(moveToggle, gbc);
			}

			if (ann == null || ann.canDelete())
			{
				// delete button
				// create mouse listener for when pressed
				deleteButton.setBackground(new Color(1, .15f, 0, 1));
				deleteButton.addActionListener(new ActionListener()
				{
					@SuppressWarnings("rawtypes")
					@Override
					public void actionPerformed(ActionEvent e)
					{
						// save old value
						undoValue = new ArrayList((ArrayList) getValue());

						if (movingCI == selfCI)
						{
							movingCI = null;
						}
						GuiComponentArrayList.this.remove(selfCI);
						getAL().remove(selfCI.gfo.index);

						// create undo
						registerUndo(new UndoState(GuiComponentArrayList.this,
								new ArrayList((ArrayList) getValue())));
						onEditorValueChange();
					}
				});
				gbc = GuiHelper.GetGridBagConstraintsFieldValue();
				gbc.gridx = 2;
				gbc.weightx = 0;
				gbc.fill = GridBagConstraints.NONE;
				gbc.insets.right = 7;
				gbc.insets.left = 3;
				deleteButton.setPreferredSize(new Dimension(15, 15));
				panel.add(deleteButton, gbc);
			}
		}
	}

	void move(int startIndex, int destIndex)
	{
		Collections.swap(children, startIndex, destIndex);
		Collections.swap(collectionItems, startIndex, destIndex);
		Collections.swap(getAL(), startIndex, destIndex);

		collapsableGroup.getCollapsePanel().removeAll();

		for (int i = 0; i < collectionItems.size(); i++)
		{
			collectionItems.get(i).gfo.index = i;
			// need to updateLabel because if it is based on index, index has changed
			collectionItems.get(i).gfo.updateLabel();
			collectionItems.get(i).moveToggle.setSelected(false);
			collapsableGroup.getCollapsePanel().add(
					collectionItems.get(i).panel, gbcStatic);
		}

		collapsableGroup.getCollapsePanel().revalidate();
		collapsableGroup.getCollapsePanel().repaint();
	}

	/**
	 * Call this after you add to the array list
	 * 
	 * @param repaint
	 * @return
	 */
	CollectionItem add(boolean repaint)
	{
		CollectionItem ci = new CollectionItem(generateChildGuiFieldObject(
				this.pft.getParam(0), getAL().size() - 1), getField()
				.getAnnotation(GuiHideArrayListControls.class));
		collectionItems.add(ci);
		children.add(ci.gfo);
		collapsableGroup.getCollapsePanel().add(ci.panel, gbcStatic);

		if (repaint)
		{
			collapsableGroup.getCollapsePanel().revalidate();
			collapsableGroup.getCollapsePanel().repaint();
		}
		return ci;
	}

	/**
	 * Call this after you remove one from the array list
	 * 
	 * @param ci
	 */
	void remove(CollectionItem ci)
	{
		collectionItems.remove(ci);
		children.remove(ci.gfo);
		ci.panel.setCollapsed(true);

		// update list now
		for (int i = 0; i < collectionItems.size(); i++)
		{
			collectionItems.get(i).gfo.index = i;
			collectionItems.get(i).gfo.updateLabel();
		}
	}

	@Override
	void checkNonGuiChanged()
	{
		// save reference of old objectRef
		Object currentObject = objectRef;

		setObjectRef();

		// don't check null objects, if first time being null, then redraw field in gui
		if (objectRef == null)
		{
			if (!isNull())
			{
				draw();
				processChangeCallback(2);
			}
			return;
		}

		if (currentObject != objectRef)
		{
			// if arraylist is pointing to different object, redraw
			draw();
			processChangeCallback(2);
		}
		else
		{
			ArrayList<Object> al = getAL();

			// this checks if the order has changed or if any item has been added or removed
			// if it is a string, this will use equals instead of ==
			if (collectionItems.size() != al.size()
					|| (!LttlHelper
							.ArrayListItemsSame(
									al,
									getReferenceList(),
									true,
									this.pft.getParam(0).getCurrentClass() != String.class)))
			{
				// be sure to open back up if it was open, since draw() always collapses
				boolean isCollapsed = collapsableGroup.isCollapsed();
				// redraw, which will maintain any GFOs that remain
				draw();
				collapsableGroup.setCollapseState(isCollapsed);
				processChangeCallback(2);
			}

			// check children for changes too, this is normal for any GFO with children
			for (GuiFieldObject<?> child : children)
			{
				child.checkNonGuiChanged();
			}
		}
	}

	private ArrayList<Object> getAL()
	{
		return (ArrayList<Object>) objectRef;
	}

	@SuppressWarnings("rawtypes")
	@Override
	void processUndoRedo(Object value)
	{
		if (value == null)
		{
			setValue(null);
		}
		else
		{
			ArrayList newList = (ArrayList) value;
			ArrayList currentList = (ArrayList) getValue();
			if (currentList == null)
			{
				setValue(new ArrayList(newList));
			}
			else
			{
				currentList.clear();
				currentList.addAll(newList);
			}
		}
		callbacks();
		processChangeCallback(1);
	}

	/**
	 * Generates an arraylist that reflects the objects and order inside the collectionItems list which is what is being
	 * shown in gui
	 */
	private ArrayList<Object> getReferenceList()
	{
		ArrayList<Object> refList = new ArrayList<Object>(
				collectionItems.size());
		for (CollectionItem ci : collectionItems)
		{
			try
			{
				refList.add(ci.gfo.getValue());
			}
			catch (IndexOutOfBoundsException e)
			{
				refList.add(null);
			}
		}

		return refList;
	}
}
