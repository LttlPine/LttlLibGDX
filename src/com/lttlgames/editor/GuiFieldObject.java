package com.lttlgames.editor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;

import com.lttlgames.editor.LttlJsonDeserializer.ComponentRef;
import com.lttlgames.editor.LttlObjectGraphCrawler.FieldsMode;
import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.GuiCallbackDescendants;
import com.lttlgames.editor.annotations.GuiCanNull;
import com.lttlgames.editor.annotations.GuiHideLabel;
import com.lttlgames.editor.annotations.GuiListItemNameField;
import com.lttlgames.editor.annotations.GuiMax;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.GuiReadOnly;
import com.lttlgames.editor.annotations.GuiStringData;
import com.lttlgames.editor.annotations.GuiStringDataInherit;
import com.lttlgames.editor.annotations.GuiToolTip;
import com.lttlgames.editor.annotations.GuiTwoColumn;
import com.lttlgames.helpers.LttlHelper;

abstract class GuiFieldObject<T>
{
	/**
	 * This is always a real object, not a group
	 */
	protected GuiComponentObject parent;
	/**
	 * always null unless child is in a group
	 */
	protected GuiComponentObject parentGroup;
	/**
	 * this never include Gui Groups, this will always include real GFOs even if they are inside groups
	 */
	protected ArrayList<GuiFieldObject<?>> children = new ArrayList<GuiFieldObject<?>>();
	protected int index;
	protected Object hostObject;
	protected Class<?> hostObjectClass;
	protected T objectRef;
	protected ProcessedFieldType pft;
	protected boolean isNull = false;
	private JPanel panel;
	protected JLabel label;
	protected String guiGroupName = null;
	protected Object undoValue;
	private boolean autoRegisterUndo = true;
	ArrayList<UndoState> undoStates = new ArrayList<UndoState>();
	private ArrayList<GuiLttlChangeListener> listeners;
	protected JPopupMenu popup;
	private JToggleButton saveToggle;
	boolean hostReplaced = false;
	private JMenuItem importMenuItem;

	/**
	 * Used when creating a child object
	 * 
	 * @param pft
	 * @param hostObject
	 * @param parent
	 */
	GuiFieldObject(ProcessedFieldType pft, Object hostObject, int index,
			GuiComponentObject parent)
	{
		this.pft = pft;
		this.hostObject = hostObject;
		this.parent = parent;
		this.hostObjectClass = hostObject.getClass();
		this.index = index;

		setObjectRef();

		draw();
	}

	/**
	 * Used when creating the root object
	 * 
	 * @param object
	 */
	GuiFieldObject(GuiComponentObject parent, Object object, String guiGroupName)
	{
		// root object can't be null
		Lttl.Throw(object);

		this.pft = new ProcessedFieldType(object.getClass());
		this.hostObject = null;
		this.parent = parent;
		this.guiGroupName = guiGroupName;

		objectRef = (T) object;

		draw();
	}

	/**
	 * Draws based on the objectRef, may need to do setObjectRef
	 */
	void draw()
	{
		// MEANT TO BE OVERIDDEN

		// recursively via reflection create this field and all the children if any
		// ...
	}

	/**
	 * Checks if object reference (or hashcode if primative) from last check/draw is different as in it was modified by
	 * game not Gui and if so redraws or updates<br>
	 * May be overriden
	 */
	void checkNonGuiChanged()
	{
		// don't check null objects, if first time being null, then redraw field in gui
		if (objectRef == null)
		{
			setObjectRef();
			if (objectRef == null)
			{
				if (!isNull())
				{
					draw();
					processChangeCallback(2);
				}
			}
			else
			{
				draw();
				processChangeCallback(2);
			}
			return;
		}

		if (objectRef != null && GuiHelper.isPrimativeGui(objectRef.getClass()))
		{
			// compare current value on field to the current value in gui (objectRef)
			if (!getValue().equals(objectRef))
			{
				updatePrimativeValue();
				processChangeCallback(2);
			}
			setObjectRef();
			return;
		}
		else
		{
			// save reference of old objectRef
			Object currentObject = objectRef;
			setObjectRef();

			// if this is a root object, it will just check children since it will never change

			// check if the actual object is different
			if (currentObject != objectRef)
			{
				// if the current object is different from the referenced one, then draw it (which redraws children
				// and everything too, so don't need to check them)
				draw();
				processChangeCallback(2);
			}
			else
			{
				// check children for changes too
				for (GuiFieldObject<?> child : children)
				{
					child.checkNonGuiChanged();
				}
			}
		}
	}

	/**
	 * Sets the object ref to the current object/value. This is only used when changing values in via editor, so
	 * checkChanged() doesn't return true from editor changes.
	 */
	void setObjectRef()
	{
		objectRef = getValue();
	}

	/**
	 * Some callbacks when ever a gui field is changed (some exceptions for objects)
	 */
	void onEditorValueChange()
	{
		if (objectRef != null && GuiHelper.isPrimativeGui(objectRef.getClass()))
		{
			// prevents check changed from finding a change when user is editing fields
			setObjectRef();
		}
		callbacks();
		processChangeCallback(0);
	}

	void callbacks()
	{
		guiCallback();
		guiCallbackDescendantsStart();
	}

	/**
	 * Returns the panel for this guiFieldObject and generates it if it doesn't exist yet, always access panel via
	 * getPanel(), no layout has been set on it.
	 * 
	 * @return
	 */
	JPanel getPanel()
	{
		if (panel == null)
		{
			panel = new JPanel(new GridBagLayout());
		}

		return panel;
	}

	boolean isNull()
	{
		return isNull;
	}

	/**
	 * Checks if object ref is null, and if it is then clears panel and children and adds null label
	 * 
	 * @return true if null
	 */
	boolean checkDrawNull()
	{
		isNull = false;
		if (objectRef != null) { return false; }

		isNull = true;

		getPanel().removeAll();
		children.clear();

		getPanel().add(
				label = GuiHelper.GetFieldLabel(getLabelFieldText(), this),
				GuiHelper.GetGridBagConstraintsFieldLabel());
		setFieldLabelToolTip();

		getPanel().add(GuiHelper.GetFieldLabel("NULL", this),
				GuiHelper.GetGridBagConstraintsFieldValue());

		// check if can add a button to create the object
		// do not add "create" button if GuiReadOnly
		if (hostObject != null
				&& getAnnotationOnAncestorsAndSelf(GuiReadOnly.class) == null)
		{
			Class<?> clazz = getObjectClass();

			if (clazz == null)
			{
				Lttl.Throw("No class could be found for null object.");
			}

			if (LttlComponent.class.isAssignableFrom(clazz))
			{
				// early out so doesn't create a 'Create' button for LttlComponents
				return true;
			}

			if (clazz.isEnum())
			{
				// if enum check if it has at least one constant
				if (clazz.getEnumConstants().length == 0) { return true; }
			}
			else
			{
				// check if can create a new instance
				if (!LttlObjectGraphCrawler.canNewInstance(clazz))
				{
					// early out if can't create a new instance
					getLabel().setText("NULL - uninstantiable");
					return true;
				}
			}

			// looks like we can, so lets make the button
			JButton addNullButton = new JButton("Create");
			addNullButton.setMargin(new Insets(-3, -2, -3, -2));
			addNullButton.setPreferredSize(null);
			GuiHelper.SetFontSize(addNullButton, 10);
			GridBagConstraints gbc = GuiHelper
					.GetGridBagConstraintsFieldValue();
			gbc.gridx = 2;
			gbc.weightx = 0;
			gbc.fill = GridBagConstraints.NONE;
			gbc.anchor = GridBagConstraints.EAST;
			getPanel().add(addNullButton, gbc);
			final Class<?> finalClazz = clazz;
			addNullButton.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					// save undoValue
					undoValue = null;

					if (finalClazz.isEnum())
					{
						// if eneum set the value to the first constant
						setValue((T) finalClazz.getEnumConstants()[0]);
					}
					else
					{
						T newObj = (T) LttlObjectGraphCrawler
								.newInstance(finalClazz);
						setValue(newObj);
						onCreate(newObj);
					}
					setObjectRef();

					// create undo
					registerUndo(new UndoState(GuiFieldObject.this));

					onEditorValueChange();

					draw();
					getPanel().revalidate();
					getPanel().repaint();
				}
			});
		}

		if (Lttl.game.isPlayingEditor() && isExportable())
		{
			GridBagConstraints gbc = GuiHelper
					.GetGridBagConstraintsFieldLabel();
			gbc.gridx = 3;
			getPanel().add(getSaveToggle(), gbc);
		}

		return true;
	}

	/**
	 * Callback for when an object is created with "Create" button
	 * 
	 * @param obj
	 *            new object
	 */
	protected void onCreate(T obj)
	{
	}

	/**
	 * Generate root gui field object from an object
	 * 
	 * @param object
	 *            (non primative or collection or a forbidden class)
	 * @return
	 */
	static GuiFieldObject<Object> GenerateGuiFieldObject(Object object)
	{
		Class<?> type = object.getClass();
		if (LttlObjectGraphCrawler.isPrimative(type) || type.isArray()
				|| type == ArrayList.class || type == HashMap.class)
		{
			Lttl.Throw("Can't be a primative or collection as the base Gui field object.");
			return null;
		}
		else if (!GuiHelper.isClassDrawnGui(type))
		{
			Lttl.Throw("Class "
					+ type.getSimpleName()
					+ " is not allowed to be drawn.  Either not persisted or needs @GuiShow.");
			return null;
		}
		else if (LttlComponent.class.isAssignableFrom(object.getClass()))
		{
			return new GuiComponentComponent(object);
		}
		else
		{
			return new GuiComponentObject(object, null);
		}
	}

	/**
	 * This updates a primative's value to the object ref. This is mainly called when a value is changed from a non GUI
	 * (ie. undo/redo and runtime). Expected to be overridden.<br>
	 * <br>
	 * update just the value in the gui instead of redrawing everything
	 */
	void updatePrimativeValue()
	{

	}

	/**
	 * Returns the value of the current value (using field or collection/index)<br>
	 * If root object, will return the origin objectRef (self)
	 * 
	 * @return
	 */
	T getValue()
	{
		if (hostObject != null)
		{
			// set null initially
			Object currentObject = null;

			// if (hostObjectClass.isArray())
			// {
			// // ARRAY
			// currentObject = Array.get(hostObject, index);
			// }
			// else
			if (hostObjectClass == ArrayList.class)
			{
				// ARRAYLIST
				currentObject = ((ArrayList<?>) hostObject).get(index);
			}
			else
			{
				// OBJECT
				try
				{
					boolean accessible = true;
					if (!getField().isAccessible())
					{
						accessible = false;
						getField().setAccessible(true);
					}

					currentObject = getField().get(hostObject);
					if (!accessible)
					{
						getField().setAccessible(false);
					}
				}
				catch (IllegalArgumentException e)
				{
					e.printStackTrace();
				}
				catch (IllegalAccessException e)
				{
					e.printStackTrace();
				}
			}
			return (T) currentObject;
		}
		return (T) objectRef;
	}

	/**
	 * This sets the actual value of the field.<br>
	 * You may need to call updatePrimatives() if it is a primative.<br>
	 * This does not call any change listeners, call that manually if desired: processChangeCallback()
	 * 
	 * @param value
	 */
	void setValue(Object value)
	{
		if (hostObject != null)
		{
			if (hostObjectClass.isArray())
			{
				// ARRAY
				Array.set(hostObject, index, value);
			}
			else if (hostObjectClass == ArrayList.class)
			{
				// ARRAYLIST
				((ArrayList<Object>) hostObject).set(index, value);
			}
			else
			{
				// OBJECT
				try
				{
					boolean isPrivate = false;
					if (LttlObjectGraphCrawler
							.isPrivateOrProtectedOrDefault(getField()))
					{
						isPrivate = true;
						getField().setAccessible(true);
					}

					// NOTE: if static, needs to be public

					getField().set(hostObject, (T) value);

					if (isPrivate)
					{
						getField().setAccessible(false);
					}
				}
				catch (IllegalArgumentException | IllegalAccessException e)
				{
					e.printStackTrace();
				}
			}
		}

	}

	void setFieldLabelToolTip()
	{
		if (hostObject == null) return;

		String tooltip = getTooltipText();

		// add tooltip if necessary
		if (!tooltip.isEmpty())
		{
			getPanel().setToolTipText(tooltip);
		}
	}

	String getTooltipText()
	{
		if (getField() != null)
		{
			// OBJECT
			return LttlHelper.toTitleCase(getField().getName())
					+ (getField().isAnnotationPresent(GuiToolTip.class) ? " - "
							+ getField().getAnnotation(GuiToolTip.class)
									.value() : "");
		}
		return "";
	}

	JLabel getLabel()
	{
		return label;
	}

	String getLabelFieldText()
	{
		return getLabelText() + ":";
	}

	String getLabelText()
	{
		if (guiGroupName != null) { return guiGroupName; }
		if (hostObject == null)
		{
			String label;
			if (objectRef instanceof LttlTransform)
			{
				// instead of using lttlTransform class, use the transform name
				label = ((LttlTransform) objectRef).getName();
				if (label.isEmpty())
				{
					label = "UNNAMED";
				}

				// add the tags
				String tagLabel = GuiHelper
						.getTagText(((LttlTransform) objectRef).getTagsBit());
				// if it's none, just leave it blank
				if (tagLabel.equals("None"))
				{
					tagLabel = "";
				}
				label += "  [" + tagLabel + "]";
			}
			else
			{
				label = LttlHelper.toTitleCase(objectRef.getClass()
						.getSimpleName());
			}
			return label;
		}
		else if (hostObjectClass == ArrayList.class
				|| hostObjectClass.isArray())
		{
			// ARRAY and ARRAYLIST
			GuiListItemNameField glinf = parent.getField().getAnnotation(
					GuiListItemNameField.class);
			if (glinf != null && objectRef != null)
			{
				ProcessedFieldType pft = LttlObjectGraphCrawler.getField(
						getObjectClass(), glinf.value());
				if (pft == null)
				{
					Lttl.Throw("GuiListItemNameField: no field "
							+ glinf.value() + " on class "
							+ getObjectClass().getSimpleName());
				}
				else
				{
					try
					{
						Object listNameObject = pft.getField().get(objectRef);
						if (listNameObject.getClass() == String.class)
						{
							return (String) listNameObject;
						}
						else if (listNameObject.getClass().isEnum()) { return ((Enum<?>) listNameObject)
								.name(); }
					}
					catch (IllegalArgumentException | IllegalAccessException e)
					{
						e.printStackTrace();
					}
				}
			}
			return "" + index;
		}
		else if (getField() != null)
		{
			// OBJECT and primatives
			return LttlHelper.toTitleCase(getField().getName());
		}
		return "";
	}

	/**
	 * This is for when the gui callback is started and we want to to tell all ancestors to run their descendant
	 * callbacks too, since it will not actually check this guifieldobject for an annotation cause it's not a descendant
	 */
	void guiCallbackDescendantsStart()
	{
		if (parent != null)
		{
			parent.guiCallbackDescendants();
		}
	}

	void guiCallbackDescendants()
	{
		// actually do the callback
		if (hostObject != null && getField() != null
				&& getField().isAnnotationPresent(GuiCallbackDescendants.class))
		{
			GuiCallbackDescendants gcd = getField().getAnnotation(
					GuiCallbackDescendants.class);

			for (String methodName : gcd.value())
			{
				processCallBack(methodName);
			}
		}

		// check their descendants
		guiCallbackDescendantsStart();
	}

	/**
	 * When the editor GUI is modified it will check to see if that field has a callback it is suppose to run on change.
	 */
	void guiCallback()
	{
		if (hostObject != null && getField() != null
				&& getField().isAnnotationPresent(GuiCallback.class))
		{
			GuiCallback gc = getField().getAnnotation(GuiCallback.class);

			for (String methodName : gc.value())
			{
				processCallBack(methodName);
			}
		}
	}

	void processCallBack(String methodName)
	{
		// get a method with the name with no params
		Method m = LttlObjectGraphCrawler.getMethodAnywhere(
				hostObject.getClass(), methodName);
		// if none found, look for one without any parameters
		// check if method exists
		if (m == null)
		{
			Lttl.Throw("Method '" + methodName + "' cannot be found on class '"
					+ hostObject.getClass().getSimpleName()
					+ "' and it's super classes with no params.");
		}

		// invoke method
		boolean inAccessible = false;
		if (!m.isAccessible())
		{
			inAccessible = true;
			m.setAccessible(true);
		}
		try
		{
			m.invoke(hostObject);
		}
		catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e)
		{
			e.printStackTrace();
		}
		if (inAccessible)
		{
			m.setAccessible(false);
		}
	}

	void updateLabel()
	{
		label.setText(getLabelFieldText());
	}

	/**
	 * Sets if this component should be view only, based on the {@link GuiReadOnly} annotation
	 * 
	 * @param component
	 */
	void disableComponentFromAnnotation(JComponent component)
	{
		if (getAnnotationOnAncestorsAndSelf(GuiReadOnly.class) != null)
		{
			component.setEnabled(false);
		}
	}

	/**
	 * Clears panel adds label and tooltip. (default)
	 */
	void drawBeginningDefault()
	{
		getPanel().removeAll();
		children.clear();

		// add label
		getPanel().add(
				label = GuiHelper.GetFieldLabel(getLabelFieldText(), this),
				GuiHelper.GetGridBagConstraintsFieldLabel());

		if (Lttl.game.isPlayingEditor() && isExportable())
		{
			GridBagConstraints gbc = GuiHelper
					.GetGridBagConstraintsFieldLabel();
			gbc.gridx = 2;
			getPanel().add(getSaveToggle(), gbc);
		}

		if (pft != null && pft.getField() != null
				&& pft.getField().isAnnotationPresent(GuiHideLabel.class))
		{
			label.setVisible(false);
		}

		setFieldLabelToolTip();
	}

	boolean isExportable()
	{
		if (pft != null && pft.getField() != null
				&& !LttlObjectGraphCrawler.isFieldExported(pft)) { return false; }
		if (getParent() != null) { return getParent().isExportable(); }

		return true;
	}

	/**
	 * If this field is inside a LttlComponent, it will return the LttlComponent object, if not it will return null.
	 * 
	 * @return
	 */
	LttlComponent getParentComponent()
	{
		GuiFieldObject<?> highest = getHighestParent();
		if (highest.getClass() == GuiComponentComponent.class) { return (LttlComponent) highest.objectRef; }
		return null;
	}

	String getTreeFieldName()
	{
		if (parent != null)
		{
			return parent.getTreeFieldName() + getLabelText() + ":";
		}
		else if (getClass() == GuiComponentComponent.class)
		{
			return ((LttlComponent) objectRef).transform().getName() + " ("
					+ ((LttlComponent) objectRef).getClass().getSimpleName()
					+ ") - ";
		}
		else if (hostObjectClass != null)
		{
			return hostObjectClass.getSimpleName() + ": " + getLabelText();
		}
		else
		{
			return "?unknown?: ";
		}
	}

	String getTreeFieldNameFormatted()
	{
		String name = getTreeFieldName();
		if (name.endsWith(":")) { return name.substring(0, name.length() - 1); }
		return name;
	}

	GuiFieldObject<?> getHighestParent()
	{
		if (parent != null)
		{
			return parent.getHighestParent();
		}
		else
		{
			return this;
		}
	}

	void replaceHostTree(Object newHost)
	{
		Object self = null;
		if (parent == null)
		{
			self = newHost;
		}
		else
		{
			hostObject = newHost;
			try
			{
				self = getValue();
			}
			catch (IndexOutOfBoundsException w)
			{
			}
		}
		if (self != null)
		{
			for (GuiFieldObject<?> child : children)
			{
				child.replaceHostTree(self);
			}
		}
		hostReplaced = true;
	}

	/**
	 * Called when an undo or redo is being processed, this should be overidden for some gui components
	 * 
	 * @param value
	 *            the value to be set
	 */
	void processUndoRedo(Object value)
	{
		Field f = getField();
		try
		{
			boolean isAccessible = f.isAccessible();
			if (!f.isAccessible())
			{
				f.setAccessible(true);
			}
			f.set(hostObject, value);
			if (!isAccessible)
			{
				f.setAccessible(false);
			}
		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
		}
		callbacks();
		if (objectRef != null && GuiHelper.isPrimativeGui(objectRef.getClass()))
		{
			updatePrimativeValue();
		}
		processChangeCallback(1);
	}

	T cast(Object object)
	{
		return (T) object;
	}

	/**
	 * If false, will not auto register undo states for self and any children. Default is true.<br>
	 * Note: This is helpful when you are creating a dialog or field that doesn't really correlate to an object in the
	 * game.
	 * 
	 * @param autoRegisterUndo
	 */
	void setAutoRegisterUndo(boolean autoRegisterUndo)
	{
		this.autoRegisterUndo = autoRegisterUndo;
	}

	boolean getAutoRegisterUndo()
	{
		return this.autoRegisterUndo;
	}

	@SuppressWarnings("rawtypes")
	boolean shouldAutoUndo()
	{
		GuiFieldObject current = this;
		while (current != null)
		{
			// if it's not suppose to auto undo on self or any ancestor then return false
			if (!current.getAutoRegisterUndo()) { return false; }
			current = current.getParent();
		}
		// no ancestor or self is blocking auto undo
		return true;
	}

	GuiFieldObject<?> getParent()
	{
		return parent;
	}

	/**
	 * Gets the class of the objectRef/value from the ProcessedFieldType
	 * 
	 * @return
	 */
	public Class<?> getObjectClass()
	{
		return pft.getCurrentClass();
	}

	public Field getField()
	{
		return pft.getField();
	}

	public void unregisterAllUndoStates()
	{
		Lttl.editor.getUndoManager().unregisterUndoStates(undoStates);
	}

	public void registerUndo(UndoState us)
	{
		undoStates.add(us);
		Lttl.editor.getUndoManager().registerUndoState(us);
	}

	/**
	 * If an object, be sure it's new. No pointers.
	 * 
	 * @param undoValue
	 */
	public void setUndoValue(T undoValue)
	{
		this.undoValue = undoValue;
	}

	/**
	 * Adds a change listeners for whenever the GuiFieldObject is changed.<br>
	 * Note: This is better than adding a change listener directly to underlying Swing component because this is always
	 * called after the field has been update. There is no guarantee the field is update if use aformentioned method.
	 * 
	 * @param listener
	 */
	public void addChangeListener(GuiLttlChangeListener listener)
	{
		if (listeners == null)
		{
			listeners = new ArrayList<GuiLttlChangeListener>();
		}
		listeners.add(listener);
	}

	protected void processChangeCallback(int changeId)
	{
		if (listeners == null) return;
		for (GuiLttlChangeListener listener : listeners)
		{
			listener.onChange(changeId);
		}
	}

	/**
	 * Finds the first ancestor of this guiFieldObject (going from current guiFieldObject and up) that's objectClass
	 * fits the class type (or extends it)
	 * 
	 * @param clazz
	 * @param subClasses
	 * @return the GuiFieldObject, or null if none found
	 */
	public GuiFieldObject<?> getAncestorByClass(Class<T> clazz,
			boolean subClasses)
	{
		GuiFieldObject<?> current = this;
		while (true)
		{
			if ((subClasses && clazz.isAssignableFrom(current.getObjectClass()))
					|| clazz == current.getObjectClass())
			{
				return current;
			}
			else
			{
				if (current.parent != null)
				{
					current = current.parent;
				}
				else
				{
					return null;
				}
			}
		}
	}

	/**
	 * Gets the min for this GuiFieldObject's field, if none, then checks parent for a GuiMin too (which is an object or
	 * something)
	 * 
	 * @return
	 */
	protected Float getMinAncestor()
	{
		if (getField() != null && getField().isAnnotationPresent(GuiMin.class)) { return getField()
				.getAnnotation(GuiMin.class).value(); }
		if (getParent() != null) { return getParent().getMinAncestor(); }
		return null;
	}

	/**
	 * Gets the max for this GuiFieldObject's field, if none, then checks parent for a GuiMax too (which is an object or
	 * something)
	 * 
	 * @return
	 */
	protected Float getMaxAncestor()
	{
		if (getField() != null && getField().isAnnotationPresent(GuiMax.class)) { return getField()
				.getAnnotation(GuiMax.class).value(); }
		if (getParent() != null) { return getParent().getMaxAncestor(); }
		return null;
	}

	/**
	 * should be ran on parent
	 * 
	 * @param id
	 * @return
	 */
	private String getGuiStringDataInheritted(int id)
	{
		GuiStringData ann = getField() == null ? null : getField()
				.getAnnotation(GuiStringData.class);
		if (ann != null && ann.id() == id)
		{
			// found on this GFO save
			return ann.value();
		}
		else if (getParent() != null)
		{
			// if not found on this GFO, then search parent
			return getParent().getGuiStringDataInheritted(id);
		}
		// not found
		return null;
	}

	/**
	 * Returns the GuiStringData for this field, taking into consideration inheritance
	 * 
	 * @return can be null if not found
	 */
	protected String getGuiStringData()
	{
		String data = null;
		if (getField() != null)
		{
			// check if has inherit first, if so, check parents
			if (getField().isAnnotationPresent(GuiStringDataInherit.class)
					&& getParent() != null)
			{
				data = getParent().getGuiStringDataInheritted(
						getField().getAnnotation(GuiStringDataInherit.class)
								.value());
			}
			// if still haven't found it, may have attempted to get data inherit, then check other annotation
			if (data == null
					&& getField().isAnnotationPresent(GuiStringData.class))
			{
				data = getField().getAnnotation(GuiStringData.class).value();
			}
		}
		return data;
	}

	boolean isTwoColumn()
	{
		return (parent != null && parent.twoColumnObject)
				|| (pft.getField() != null && pft.getField()
						.isAnnotationPresent(GuiTwoColumn.class));
	}

	void createPopupMenu(JComponent popupComp)
	{
		// Create the popup menu.
		popup = new JPopupMenu();

		// Add listener so the popup menu can come up.
		popupComp.addMouseListener(new MouseAdapter()
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
				if (e.isPopupTrigger())
				{
					if (importMenuItem != null)
					{
						importMenuItem.setEnabled(getValue() != null);
					}
					// show popup menu
					popup.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});
	}

	JToggleButton getSaveToggle()
	{
		if (saveToggle == null)
		{
			saveToggle = new JToggleButton("", getSavedGFO() != null);
			saveToggle.setFocusable(false);
			saveToggle.setPreferredSize(new Dimension(15, 15));
			saveToggle.setBackground(saveToggle.isSelected() ? Color.GREEN
					: Color.GRAY);
			saveToggle.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					GuiFieldObject<?> savedGFO = getSavedGFO();
					if (savedGFO != null)
					{
						Lttl.editor.getGui().saveGFOs.remove(savedGFO);
					}
					else
					{
						Lttl.editor.getGui().saveGFOs.add(GuiFieldObject.this);
					}
					saveToggle.setBackground(saveToggle.isSelected() ? Color.GREEN
							: Color.GRAY);
				}
			});

		}

		return saveToggle;
	}

	void addSetToNullMenuItem()
	{
		GuiCanNull anno = null;

		// get class to search for "canNull" method
		Class<?> methodSearchClass = null;
		if (getField() != null
				&& (anno = getField().getAnnotation(GuiCanNull.class)) != null)
		{
			// search for method on host object since it was on the field
			methodSearchClass = hostObjectClass;
		}
		else if ((anno = getObjectClass().getAnnotation(GuiCanNull.class)) != null)
		{
			// search the object's class since it was on the class
			methodSearchClass = getObjectClass();
		}

		if (anno != null)
		{
			final GuiCanNull finalAnno = anno;
			final Class<?> finalMethodSearchClass = methodSearchClass;

			JMenu menu = new JMenu("Set to NULL");
			JMenuItem menuItem = new JMenuItem("Confirm");
			menu.add(menuItem);
			menu.setForeground(new java.awt.Color(1, 0, 0, .5f));
			menuItem.setForeground(menu.getForeground());
			menuItem.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					if (!finalAnno.value().isEmpty()
							&& finalMethodSearchClass != null)
					{
						Method m = LttlObjectGraphCrawler.getMethodAnywhere(
								finalMethodSearchClass, finalAnno.value());

						if (m == null)
						{
							Lttl.Throw("Method '"
									+ finalAnno.value()
									+ "' cannot be found on class '"
									+ getObjectClass().getSimpleName()
									+ "' and it's super classes with no params.");
						}
						else
						{
							// invoke method
							boolean inAccessible = false;
							if (!m.isAccessible())
							{
								inAccessible = true;
								m.setAccessible(true);
							}
							try
							{
								if (!((boolean) m.invoke(hostObject)))
								{
									Lttl.logNote("Can Null Check: Unable to set to null because of method "
											+ m.getName()
											+ " on "
											+ finalMethodSearchClass
													.getSimpleName());
									return;
								}
							}
							catch (IllegalAccessException
									| IllegalArgumentException
									| InvocationTargetException e1)
							{
								e1.printStackTrace();
							}
							if (inAccessible)
							{
								m.setAccessible(false);
							}
						}
					}
					undoValue = getValue();
					setValue(null);
					registerUndo(new UndoState(GuiFieldObject.this));
					setObjectRef();
					onEditorValueChange();
					draw();
					getPanel().revalidate();
					getPanel().repaint();
					return;
				}
			});
			popup.add(menu);
		}
	}

	protected void addExportImportMenuItem()
	{
		JMenuItem exportMenuItem = new JMenuItem("Export");
		exportMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
				Transferable transferable = new StringSelection(LttlCopier
						.toJson(getValue(), FieldsMode.Export, true));
				cb.setContents(transferable, null);
			}
		});
		popup.add(exportMenuItem);

		importMenuItem = new JMenuItem("Import");
		importMenuItem.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				String json = null;

				// auto import by chceking clipboard first
				Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
				Transferable t = cb.getContents(null);
				if (t != null
						&& t.isDataFlavorSupported(DataFlavor.stringFlavor))
				{
					try
					{
						json = (String) t
								.getTransferData(DataFlavor.stringFlavor);
					}
					catch (UnsupportedFlavorException | IOException e1)
					{
						json = null;
					}
				}

				if (json == null || json.isEmpty() || json.indexOf("{") != 0)
				{
					json = GuiHelper.showTextAreaModal("Import",
							"Paste JSON in text area.", 500, 300);
				}
				if (json == null || json.isEmpty()) return;

				Object container = getValue();
				ArrayList<ComponentRef> compRefsList = new ArrayList<LttlJsonDeserializer.ComponentRef>();
				LttlCopier.fromJson(json, container.getClass(), compRefsList,
						container);
				// update component references
				for (ComponentRef cr : compRefsList)
				{
					cr.set(((LttlComponent) container).getSceneCore());
				}
				onEditorValueChange();
			}
		});
		popup.add(importMenuItem);
	}

	protected GuiFieldObject<?> getSavedGFO()
	{
		if (!Lttl.game.isPlayingEditor()) return null;
		for (GuiFieldObject<?> savedGFO : Lttl.editor.getGui().saveGFOs)
		{
			if (!areGFOsSame(this, savedGFO)) continue;

			return savedGFO;
		}
		return null;
	}

	/**
	 * compares if GFOs are the same but different instance
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	static protected boolean areGFOsSame(GuiFieldObject<?> a,
			GuiFieldObject<?> b)
	{
		if (a.getClass() != b.getClass()) return false;
		if (a.guiGroupName != null)
		{
			// could be group
			// if 'a' is a group and 'b' is not, or it is, but differen names, return false
			if (!a.guiGroupName.equals(b.guiGroupName)) return false;
			// if both are groups, and have same name, compare their parents
			return areGFOsSame(a.parent, b.parent);
		}
		else if (a.hostObject == null)
		{
			// it' a root object, so compare the actual object references
			if (b.objectRef != a.objectRef) return false;
		}
		else
		{
			// it is a field/list item then compare the host object and field
			if (b.hostObject != a.hostObject) return false;
			// if not a field item (list), then these will both be null
			if (b.getField() != a.getField()) return false;
			// if not a list item, these will be the same
			if (b.index != a.index) return false;
		}

		return true;
	}

	/**
	 * makes sure all parent GFOs are not collapsed
	 */
	final void forceVisible()
	{
		if (parentGroup != null)
		{
			parentGroup.forceVisible();
			parentGroup.collapsableGroup.setCollapseState(false);
		}
		else if (parent != null)
		{
			parent.forceVisible();
			parent.collapsableGroup.setCollapseState(false);
		}
	}

	/**
	 * This checks it's then any ancestors and returns the first one that has a field. This helps for getting annotatons
	 * when the object is in an arraylist.
	 * 
	 * @return
	 */
	GuiFieldObject<?> getFirstAncestorWithField()
	{
		if (getField() != null) return this;
		return getParent().getFirstAncestorWithField();
	}

	/**
	 * returns first annotation found on self or ancestors, going up
	 * 
	 * @return null if not found
	 */
	<TT extends Annotation> TT getAnnotationOnAncestorsAndSelf(
			Class<TT> annClass)
	{
		TT ann = getField() != null ? getField().getAnnotation(annClass) : null;
		if (ann == null)
		{
			ann = getAnnotationOnAncestors(annClass);
		}
		return ann;
	}

	/**
	 * returns first annotation found on ancestors, going up
	 * 
	 * @return null if not found
	 */
	<TT extends Annotation> TT getAnnotationOnAncestors(Class<TT> annClass)
	{
		if (getParent() == null) return null;

		TT ann = null;
		// if parent has a field, then check it for an annotation
		if (getParent().getField() != null)
		{
			ann = getParent().getField().getAnnotation(annClass);
		}
		// if still no annotation found, then check the parent's ancestors
		if (ann == null)
		{
			ann = getParent().getAnnotationOnAncestors(annClass);
		}
		return ann;
	}
}
