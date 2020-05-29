package com.lttlgames.editor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import com.lttlgames.editor.annotations.IgnoreCrawl;

@IgnoreCrawl
public abstract class AnimGuiBase
{
	/**
	 * only necessary if field sequence/object
	 */
	public ProcessedFieldType pft;
	public AnimGuiObject parentGui;
	public GuiLttlCollapsableGroup group;
	public GuiLttlMultiSlider multiSlider;
	GuiAnimationEditor editor;

	public AnimGuiBase(GuiAnimationEditor editor)
	{
		this.editor = editor;
	}

	void updateCollapseMap(HashMap<GuiLttlCollapsableGroup, Boolean> map)
	{
		map.put(group, group.isCollapsed());
	}

	public boolean isList()
	{
		return pft != null
				&& (pft.getCurrentClass() == ArrayList.class || pft
						.getCurrentClass().isArray());
	}

	public boolean isListItem()
	{
		return parentGui != null && parentGui.isList();
	}

	public abstract int getAnimId();

	public abstract void updateLabel();

	protected abstract void listItemModifyIndex(int newIndex);

	final void setIndexMenu(JPopupMenu popupMenu)
	{
		if (isListItem())
		{
			final JMenuItem setIndex = new JMenuItem("Set Index");
			setIndex.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					Integer newIndex = GuiHelper.showIntegerModal("New Index",
							null);

					// user escaped or closed out of window, do nothing
					if (newIndex == null) return;

					if (newIndex < 0)
					{
						GuiHelper.showAlert(editor.mainPanel, "Invalid Index",
								"Index can't be less than 0.",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
					if (parentGui.animObject.children.containsKey(newIndex))
					{
						GuiHelper
								.showAlert(editor.mainPanel, "Invalid Index",
										"Index " + newIndex
												+ " is already being used.",
										JOptionPane.ERROR_MESSAGE);
						return;
					}

					// remove old hashmap entry, add new one with new index
					listItemModifyIndex(newIndex);

					updateLabel();

					// OPTIMIZE not sure why this needs to be done twice for the changes to go into effect
					editor.updateAnim();
					editor.updateAnim();
				}
			});
			popupMenu.add(setIndex);
		}
	}
}
