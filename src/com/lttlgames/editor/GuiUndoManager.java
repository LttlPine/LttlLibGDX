package com.lttlgames.editor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;

class GuiUndoManager
{
	private int currentStateIndex = -1;
	private ArrayList<UndoState> undoStates = new ArrayList<UndoState>();

	/**
	 * Registers a new UndoState and clears any states that are after the current state.
	 * 
	 * @param undoState
	 */
	void registerUndoState(UndoState undoState)
	{
		undoStates.subList(currentStateIndex + 1, undoStates.size()).clear();

		undoStates.add(undoState);
		currentStateIndex = undoStates.size() - 1;
		updateLabel();
	}

	void reset()
	{
		clearAll();
		updateLabel();
	}

	boolean canUndo()
	{
		return currentStateIndex >= 0;
	}

	void undo()
	{
		if (!canUndo()) { return; }

		UndoState undoState = undoStates.get(currentStateIndex);
		boolean atleastOneTransformHandle = false;
		for (UndoField undoField : undoState.fields)
		{
			if (undoField.gfo != null)
			{
				// GUI
				if (undoField.field == null)
				{
					// it is in an arraylist
					// make sure it is the current arraylist before processing by grabbing the parent's value, even
					// though the gfo's may be old, they still can communicate with objects
					undoField.gfo.hostObject = undoField.gfo.parent.getValue();
				}
				undoField.gfo.processUndoRedo(undoField.gfo
						.cast(undoField.undoValue));
			}
			// HANDLES and custom undos
			else if (undoField.setter != null)
			{
				if (undoField.componentId != -1)
				{
					// if it is an undo component, then get the transform
					LttlComponent comp = Lttl.scenes
							.findComponentByIdAllScenes(undoField.componentId);
					if (comp != null)
					{
						if (comp.getClass() == LttlTransform.class)
						{
							atleastOneTransformHandle = true;
						}
						undoField.setter.set(comp, undoField.undoValue);
					}
				}
				else
				{
					// non component undo, like play camera
					undoField.setter.set(null, undoField.undoValue);
				}
			}
		}

		// decrement state
		currentStateIndex--;

		// set selection
		setSelection(undoState);
		if (atleastOneTransformHandle)
		{
			Lttl.editor.getGui().getSelectionController().updateHandles();
		}
		updateLabel();
	}

	boolean canRedo()
	{
		return currentStateIndex < undoStates.size() - 1;
	}

	void redo()
	{
		if (!canRedo()) { return; }

		// increment state
		currentStateIndex++;

		UndoState undoState = undoStates.get(currentStateIndex);
		boolean atleastOneTransformHandle = false;
		for (UndoField undoField : undoState.fields)
		{
			if (undoField.gfo != null)
			{
				// GUI
				if (undoField.field == null)
				{
					// it is in an arraylist
					// make sure it is the current arraylist before processing by grabbing the parent's value, even
					// though the gfo's may be old, they still can communicate with objects
					undoField.gfo.hostObject = undoField.gfo.parent.getValue();
				}
				undoField.gfo.processUndoRedo(undoField.gfo
						.cast(undoField.redoValue));
			}
			// HANDLES and cusom redos
			else if (undoField.setter != null)
			{
				if (undoField.componentId != -1)
				{
					// if it is an undo component, then get the transform
					LttlComponent comp = Lttl.scenes
							.findComponentByIdAllScenes(undoField.componentId);
					if (comp != null)
					{
						if (comp.getClass() == LttlTransform.class)
						{
							atleastOneTransformHandle = true;
						}
						undoField.setter.set(comp, undoField.redoValue);
					}
				}
				else
				{
					// non component undo, like play camera
					undoField.setter.set(null, undoField.redoValue);
				}
			}
		}

		// set selection
		setSelection(undoState);
		if (atleastOneTransformHandle)
		{
			Lttl.editor.getGui().getSelectionController().updateHandles();
		}
		updateLabel();
	}

	private void updateLabel()
	{
		if (currentStateIndex >= 0)
		{
			Lttl.editor
					.getGui()
					.getMenuBarController()
					.getUndoStatusLabel()
					.setText(
							"[" + undoStates.get(currentStateIndex).description
									+ "]");
		}
		else
		{
			Lttl.editor.getGui().getMenuBarController().getUndoStatusLabel()
					.setText("");
		}
	}

	/**
	 * Sets selection for the changed undostate
	 * 
	 * @param undoState
	 */
	private void setSelection(UndoState undoState)
	{
		ArrayList<LttlTransform> selection = new ArrayList<LttlTransform>();
		for (UndoField undoField : undoState.fields)
		{
			// skip undo fields that are not associated with a component
			if (undoField.componentId == -1 || undoField.sceneId == -1)
			{
				continue;
			}

			LttlScene scene = Lttl.scenes.get(undoField.sceneId);
			LttlTransform transform = scene.findComponentById(
					undoField.componentId).transform();
			if (!selection.contains(transform))
			{
				selection.add(transform);
			}
		}

		if (selection.size() > 0)
		{
			Lttl.editor.getGui().getSelectionController()
					.setSelection(selection);
		}
	}

	void clearAll()
	{
		undoStates.clear();
		currentStateIndex = -1;
	}

	/**
	 * Removes all undo fields that have the following scene id in it, other fields may remain.
	 * 
	 * @param sceneId
	 */
	void removeScenesUndos(int sceneId)
	{
		removeUndosShared(-1, sceneId);
	}

	/**
	 * Removes all undo fields that have the following component in it, other fields may remain.
	 * 
	 * @param sceneId
	 */
	void removeComponentUndos(int compId)
	{
		removeUndosShared(compId, -1);
	}

	/**
	 * Unregisters specific undo states
	 * 
	 * @param undoState
	 */
	void unregisterUndoStates(ArrayList<UndoState> undoStates)
	{
		for (UndoState us : undoStates)
		{
			unregisterUndoState(us);
		}
	}

	/**
	 * Unregister a specific undo state
	 * 
	 * @param undoState
	 */
	void unregisterUndoState(UndoState undoState)
	{
		int index = undoStates.indexOf(undoState);
		if (index >= 0)
		{
			// remove
			undoStates.remove(undoState);

			// make sure currentStateIndex remains accurate
			if (index <= currentStateIndex)
			{
				currentStateIndex--;
			}
			updateLabel();
		}
	}

	/**
	 * Removes all undo fields that have the following component and/or scene in it, other fields may remain. If no
	 * fields remain, then removes entire undo state.
	 * 
	 * @param compId
	 * @param sceneId
	 */
	private void removeUndosShared(int compId, int sceneId)
	{
		int index = -1;
		for (Iterator<UndoState> it = undoStates.iterator(); it.hasNext();)
		{
			UndoState us = it.next();
			// iterate through undo fields and remove them based on compId and sceneId
			for (Iterator<UndoField> it2 = us.fields.iterator(); it2.hasNext();)
			{
				UndoField uf = it2.next();

				// skip undo fields that are not associated with a component
				if (uf.componentId == -1 || uf.sceneId == -1)
				{
					continue;
				}

				if (compId != -1 && uf.componentId == compId)
				{
					it2.remove();
				}
				if (sceneId != -1 && uf.sceneId == sceneId)
				{
					it2.remove();
				}
			}

			// check if there are no fields on this undo state, if so remove the undo state
			index++;
			if (us.fields.size() == 0)
			{
				// remove undo state
				it.remove();

				// decrement the currentStateIndex if removing an undostate (at or below it's index)
				if (index <= currentStateIndex)
				{
					currentStateIndex--;
				}
				index--;
			}
		}
		updateLabel();
	}

	String getUndoDescription()
	{
		if (!canUndo()) { return ""; }
		return undoStates.get(currentStateIndex).description;
	}

	String getRedoDescription()
	{
		if (!canRedo()) { return ""; }
		return undoStates.get(currentStateIndex + 1).description;
	}
}

class UndoState
{
	public UndoState(GuiFieldObject<?> gfo)
	{
		this(gfo.getTreeFieldNameFormatted(), new UndoField(gfo));
	}

	public UndoState(GuiFieldObject<?> gfo, Object redoValue)
	{
		this(gfo.getTreeFieldNameFormatted(), new UndoField(gfo, redoValue));
	}

	public UndoState(String description, ArrayList<UndoField> fields)
	{
		this.description = description;
		this.fields = fields;
	}

	public UndoState(String description, UndoField field)
	{
		this.description = description;
		this.fields = new ArrayList<UndoField>();
		this.fields.add(field);
	}

	public String description;
	public ArrayList<UndoField> fields;
}

class UndoField
{
	public GuiFieldObject<?> gfo;
	public int componentId = -1;
	public int sceneId = -1;
	public Object object;
	public Field field;
	public Object undoValue;
	public Object redoValue;
	public UndoSetter setter;

	/**
	 * @param comp
	 *            can be null if not on component, allows undos to be removed by component
	 * @param undoValue
	 *            (make a copy of object if it is a pointer, like a vector2 or something)
	 * @param redoValue
	 * @param setter
	 */
	public UndoField(LttlComponent comp, Object undoValue, Object redoValue,
			UndoSetter setter)
	{
		if (comp != null)
		{
			this.componentId = comp.getId();
			this.sceneId = comp.getScene().getId();
		}
		this.undoValue = undoValue;
		this.redoValue = redoValue;
		this.setter = setter;
	}

	public UndoField(GuiFieldObject<?> gfo)
	{
		this.gfo = gfo;
		// only set sceneId and componentId if it is in a lttlcomponent, otherwise it could be a settings or camera
		// object on the world scene
		LttlComponent comp = gfo.getParentComponent();
		if (comp != null)
		{
			this.componentId = comp.getId();
			this.sceneId = comp.getScene().getId();
		}
		this.object = gfo.hostObject;
		this.field = gfo.getField();
		this.undoValue = gfo.undoValue;
		this.redoValue = gfo.getValue();
	}

	/**
	 * Specify a specific redo value not automatically obtained from gfo's getValue()
	 * 
	 * @param gfo
	 * @param redoValue
	 */
	public UndoField(GuiFieldObject<?> gfo, Object redoValue)
	{
		this.gfo = gfo;
		LttlComponent comp = gfo.getParentComponent();
		if (comp != null)
		{
			this.componentId = comp.getId();
			this.sceneId = comp.getScene().getId();
		}
		this.object = gfo.hostObject;
		this.field = gfo.getField();
		this.undoValue = gfo.undoValue;
		this.redoValue = redoValue;
	}
}