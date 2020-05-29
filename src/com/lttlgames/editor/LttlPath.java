package com.lttlgames.editor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JMenuItem;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntArray;
import com.lttlgames.components.interfaces.AnimationCallback;
import com.lttlgames.editor.annotations.AnimateField;
import com.lttlgames.editor.annotations.GuiButton;
import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.GuiCallbackDescendants;
import com.lttlgames.editor.annotations.GuiDecimalPlaces;
import com.lttlgames.editor.annotations.GuiGroup;
import com.lttlgames.editor.annotations.GuiMin;
import com.lttlgames.editor.annotations.GuiReadOnly;
import com.lttlgames.editor.annotations.GuiShow;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.helpers.LttlBezier;
import com.lttlgames.helpers.LttlHelper;
import com.lttlgames.helpers.LttlMath;
import com.lttlgames.helpers.LttlMutatableFloat;
import com.lttlgames.helpers.LttlProfiler;
import com.lttlgames.helpers.Vector2Array;

//17
@Persist(-9075)
public class LttlPath extends LttlComponent implements AnimationCallback
{
	/**
	 * makes it looks curve similar to bezier
	 */
	static private final float catmullromHandleWeight = 6.3f;

	/**
	 * Shows the number of points in the path based on last path update.
	 */
	@GuiReadOnly
	@GuiShow
	public int pointsCount = 0;
	@Persist(907501)
	@GuiCallbackDescendants("guiControlPointsUpdated")
	@GuiCallback("guiControlPointsUpdated")
	@AnimateField(0)
	public ArrayList<LttlPathControlPoint> controlPoints = new ArrayList<LttlPathControlPoint>();

	@Persist(9075016)
	@GuiCallback(
	{ "editorUpdateFocusedHandle", "editorUpdatePath" })
	public boolean useBezier = false;

	@Persist(9075010)
	@GuiGroup("Settings")
	public boolean updatePathOnStart = true;

	/**
	 * Updates path length whenever path is updated.
	 */
	@Persist(9075011)
	@GuiGroup("Settings")
	public boolean autoUpdateLength = true;

	/**
	 * This is for if it's animating.
	 */
	@Persist(9075014)
	@GuiGroup("Settings")
	public boolean updatePathOnStep = true;
	/**
	 * how many points to interpolate through mathematically when calculating the optimal points between each control
	 * point (the greater the tSamples, the more precies the curve will be)
	 */
	@Persist(907504)
	@GuiGroup("Settings")
	@GuiCallback("editorUpdatePath")
	@GuiMin(1)
	public int tSamples = 15;
	/**
	 * what is the min amount of angle change before a new point is created, the smaller the angle the more accurate the
	 * curve, but also the greater number of points
	 */
	@Persist(907505)
	@GuiGroup("Settings")
	@GuiCallback("editorUpdatePath")
	@GuiMin(0)
	@GuiDecimalPlaces(1)
	public float angleChangeThreshold = 0.2f;
	/**
	 * if 3 or more points, creates a closed path
	 */
	@Persist(907506)
	@GuiGroup("Settings")
	@GuiCallback("editorUpdatePath")
	public boolean closed = true;
	/**
	 * The default control point type for when a control point is created.
	 */
	@Persist(907502)
	@GuiGroup("Editor Settings")
	public LttlPathControlPointType defaultControlPointType = LttlPathControlPointType.Proximity;
	/**
	 * when a point is created, are the handles locked or not by default
	 */
	@Persist(907503)
	@GuiGroup("Editor Settings")
	public boolean handlesLockedDefault = true;
	@Persist(9075017)
	@GuiGroup("Editor Settings")
	public boolean showEditorLine = true;
	/**
	 * This draws the control points when not in edit mode.
	 */
	@Persist(907508)
	@GuiGroup("Editor Settings")
	public boolean showControlPoints = false;
	/**
	 * This draws the path line when not in edit mode.
	 */
	@Persist(907509)
	@GuiGroup("Editor Settings")
	public boolean showLine = false;
	/**
	 * shows all the path points
	 */
	@Persist(9075013)
	@GuiGroup("Editor Settings")
	public boolean showPathPoints = false;

	/**
	 * the many points being used to draw the path itself, this is always going to be accurate for the path using it's
	 * own tSample and slopeThreshold values when it is updated, this should be directly accessed via its getter to draw
	 * custom shapes that just want to mimick the path exactly.
	 */
	private Vector2Array pathArray = null;
	/**
	 * This holds all the indexes for the path points that are control points
	 */
	private IntArray controlPointPathIndexArray = null;
	/**
	 * Stores the total length at each point on the path since last updateLength();<br>
	 * at index 0 the length is 0, at index 1, the length is from p0 to p1, and at index 2 the length is from p0 to p2
	 * (cumulative). The last index is the total pathLength, if a closed shape that means it has an extra index comapred
	 * to pathArray.
	 */
	private FloatArray pathLengthArray = null;
	private boolean updatedSinceModified = false;
	private boolean updatedLengthSinceModified = false;

	/* Editing Stuff */
	/**
	 * How we know if it is in edit mode or not
	 */
	private boolean isEditing = false;
	private ArrayList<LttlPathControlPoint> selectedControlPoints;
	private ArrayList<LttlPathControlPoint> originalSelectedControlPoints;
	private Matrix3 matrix3Comparison;
	private HandleRect multiSelectScaleHandle;
	private float deselectionTime = Float.POSITIVE_INFINITY;
	private float doubleClickTime = .5f;
	private boolean justReleasedHandle = false;
	private boolean usingSelectBox = false;

	/**
	 * Used to compare what control points where removed or added when the gui arraylist is modified
	 * (guiControlPointsUpdated is ran), so they can be unregistered
	 */
	private ArrayList<LttlPathControlPoint> comparisonControlPoints;

	private ArrayList<LttlModifiedListener> listeningComponents = new ArrayList<LttlModifiedListener>();

	/* TEMP */
	private Vector2Array prepPoints = new Vector2Array(4);
	private final Vector2 tmp0 = new Vector2();
	private final Vector2 tmp1 = new Vector2();
	private Vector2Array debugPoints;

	@Override
	public void onEditorCreate()
	{
		reset();
	}

	@Override
	public void onEditorStart()
	{
		updatePathIfNecessary();
	}

	@Override
	public void onStart()
	{
		if (updatePathOnStart)
		{
			updatePathIfNecessary();
		}
	}

	@Override
	public void onUpdate()
	{
		if (Lttl.game.inEditor()) onEditorUpdate();
	}

	@Override
	public void onEditorUpdate()
	{
		if (!isEditing()) return;

		/* Check for hotkey commands */
		// need to have at least one selected control point, since could be editing multiple path components at once
		// and need to know which is in focus
		if (selectedControlPoints.size() > 0)
		{
			if (deselectionTime != Float.POSITIVE_INFINITY
					&& Lttl.game.getRawTime() - deselectionTime >= doubleClickTime)
			{
				// don't clear selection if the select box was used
				if (usingSelectBox)
				{
					usingSelectBox = false;
				}
				else
				{
					selectedControlPoints.clear();
					updateMultiSelectHandle();
					editorUpdateAllHandles();
				}
				deselectionTime = Float.POSITIVE_INFINITY;
			}

			// change to Sharp type
			if (Lttl.input.isEditorKeyPressed(Keys.Q))
			{
				saveUndoObject();
				for (LttlPathControlPoint cp : selectedControlPoints)
				{
					cp.type = LttlPathControlPointType.Sharp;
					editorUpdateHandles(cp);
				}
				registerUndoState();
				editorUpdatePath();
			}
			// change to Proximity type
			else if (Lttl.input.isEditorKeyPressed(Keys.W))
			{
				saveUndoObject();
				for (LttlPathControlPoint cp : selectedControlPoints)
				{
					cp.type = LttlPathControlPointType.Proximity;
					editorUpdateHandles(cp);
				}
				registerUndoState();
				editorUpdatePath();
			}
			// change to Handles type
			else if (Lttl.input.isEditorKeyPressed(Keys.E))
			{
				saveUndoObject();
				for (LttlPathControlPoint cp : selectedControlPoints)
				{
					cp.type = LttlPathControlPointType.Handles;
					editorUpdateHandles(cp);
				}
				registerUndoState();
				editorUpdatePath();
			}
			// toggle lock handle if selecting one control point
			if (selectedControlPoints.size() == 1
					&& Lttl.input.isEditorKeyPressed(Keys.L))
			{
				saveUndoObject();
				LttlPathControlPoint cp = selectedControlPoints.get(0);
				cp.handlesLocked = !cp.handlesLocked;

				// if just toggled to lock mode and handles type, force lock from left
				if (cp.handlesLocked && shouldHaveHandles(cp))
				{
					cp.lockFromLeft();
				}
				registerUndoState();
				editorUpdateHandles(cp);
				editorUpdatePath();
			}
			// toggle close shape
			if (Lttl.input.isEditorKeyPressed(Keys.C))
			{
				closed = !closed;
				Lttl.editor.getUndoManager().registerUndoState(
						new UndoState("Modified closed path", new UndoField(
								this, !closed, closed, new UndoSetter()
								{
									@Override
									public void set(LttlComponent comp,
											Object value)
									{
										((LttlPath) comp).closed = (boolean) value;
										editorUpdatePath();
									}
								})));
				editorUpdatePath();
			}
			// toggle bezier
			if (Lttl.input.isEditorKeyPressed(Keys.B))
			{
				saveUndoObject();
				useBezier = !useBezier;
				editorUpdateFocusedHandle();
				registerUndoState();
				editorUpdatePath();
			}
		}

		/* Selection */
		/* Check if using selection box */
		ArrayList<LttlPathControlPoint> lastFrameSelection = new ArrayList<LttlPathControlPoint>(
				selectedControlPoints);
		if (Lttl.editor.getGui().getSelectionController().selectBoxRectangle != null)
		{
			if (originalSelectedControlPoints == null)
			{
				originalSelectedControlPoints = new ArrayList<LttlPathControlPoint>();
				originalSelectedControlPoints.addAll(selectedControlPoints);
			}
			selectedControlPoints.clear();
			if (Lttl.input.isEditorControl())
			{
				selectedControlPoints.addAll(originalSelectedControlPoints);
			}
			for (LttlPathControlPoint cp : controlPoints)
			{
				if (cp.mainHandle.overlapsRectangle(Lttl.editor.getGui()
						.getSelectionController().selectBoxRectangle))
				{
					// mark that the selected controls points was modified by using the select box, this way when mouse
					// up happens it does not deselect all controls points
					usingSelectBox = true;

					if (Lttl.input.isEditorControl()
							&& originalSelectedControlPoints.contains(cp))
					{
						selectedControlPoints.remove(cp);
					}
					else
					{
						selectedControlPoints.add(cp);
					}
					editorUpdateAllHandles();
				}
			}
		}
		else
		{
			// some select box cleanup
			if (originalSelectedControlPoints != null)
			{
				originalSelectedControlPoints = null;
				editorUpdateAllHandles();
			}

			// check if using mouse to select or deleting or creating point
			if (Lttl.input.isEditorMousePressed(0))
			{
				for (LttlPathControlPoint cp : controlPoints)
				{
					// check if mouse on a handle
					if (cp.mainHandle.containsMouse())
					{
						// left button pressed
						if (!selectedControlPoints.contains(cp))
						{
							// if was not selected, then clear selection (unless holding control) and make it
							// the selection
							if (!Lttl.input.isEditorControl())
							{
								selectedControlPoints.clear();
							}
							selectedControlPoints.add(cp);
						}
						else if (Lttl.input.isEditorControl())
						{
							selectedControlPoints.remove(cp);
						}
						editorUpdateAllHandles();
						break;
					}
				}
			}

			// left mouse button released
			if (Lttl.input.isEditorMouseReleased(0))
			{
				// just released a handle, so don't clear selection or create a new point
				if (justReleasedHandle)
				{
					justReleasedHandle = false;
				}
				else
				{
					// check if double clicked to craete point
					if (deselectionTime != Float.POSITIVE_INFINITY
							&& Lttl.game.getRawTime() - deselectionTime < doubleClickTime)
					{
						Vector2 mousePosChild = t().worldToRenderPosition(
								new Vector2(Lttl.input.getEditorMousePos()),
								true);
						Vector2 newPoint = new Vector2(mousePosChild);
						int cpIndex = -1;
						// if open path and there is only on selected control point, and it's an end point, then always
						// add points where clicked
						if (!closed
								&& selectedControlPoints.size() == 1
								&& (selectedControlPoints.get(0) == controlPoints
										.get(0) || selectedControlPoints.get(0) == controlPoints
										.get(controlPoints.size() - 1)))
						{
							if (controlPoints.indexOf(selectedControlPoints
									.get(0)) == 0)
							{
								cpIndex = 0;
							}
							else
							{
								cpIndex = controlPoints.size();
							}
						}
						else
						{
							cpIndex = getControlPointIndexFromPathIndex(getNearestPoint(
									newPoint, null)) + 1;
							if (Lttl.input.isEditorControl())
							{
								newPoint.set(mousePosChild);
							}
						}

						// create control point
						LttlPathControlPoint cp = new LttlPathControlPoint(
								newPoint);

						saveUndoObject();

						// add it
						controlPoints.add(cpIndex, cp);

						registerUndoState();

						// set as selection
						selectedControlPoints.clear();
						selectedControlPoints.add(cp);

						setDefaults(cp);
						editorUpdateComparisonList();
						editorInitHandles(cp);
						editorUpdateAllHandles();

						// update path
						editorUpdatePath();

						deselectionTime = Float.POSITIVE_INFINITY;
					}
					else if (selectedControlPoints.size() > 0)
					{
						deselectionTime = Lttl.game.getRawTime();
						// selectedControlPoints.clear();
						// editorUpdateAllHandles();
					}
				}
			}
		}

		// check if selection changed
		if (!LttlHelper.ArrayListItemsSame(lastFrameSelection,
				selectedControlPoints, true, true))
		{
			updateMultiSelectHandle();
		}
	}

	/**
	 * Returns the control point index that the pathIndex is included in.
	 * 
	 * @param pathIndex
	 * @return
	 */
	public int getControlPointIndexFromPathIndex(int pathIndex)
	{
		// if first index, then has to be the first control point always
		if (pathIndex == 0) { return 0; }
		for (int i = 0; i < controlPoints.size() - 1; i++)
		{
			// if pathIndex is less then the next controlPoint index
			if (pathIndex < controlPointPathIndexArray.get(i + 1)) { return i; }
		}
		return controlPoints.size() - 1;
	}

	private void updateMultiSelectHandle()
	{
		// update multiselect scale handle
		if (selectedControlPoints.size() > 1)
		{
			multiSelectScaleHandle.visible = true;
			Vector2Array array = new Vector2Array(selectedControlPoints.size());
			for (LttlPathControlPoint cp : selectedControlPoints)
			{
				array.add(cp.pos);
			}
			LttlMath.GetVectorsCenter(array, multiSelectScaleHandle.position);
			t().renderToWorldPosition(multiSelectScaleHandle.position, true);
		}
		else
		{
			multiSelectScaleHandle.visible = false;
		}
	}

	@Override
	public void onEditorLateUpdate()
	{
		// check if transform changes
		if (!isEditing()) return;
		if (matrix3Comparison == null)
		{
			matrix3Comparison = new Matrix3(t().getWorldRenderTransform(false));
		}
		else if (!LttlHelper.ArrayItemsSame(matrix3Comparison.getValues(), t()
				.getWorldRenderTransform(false).getValues(), true))
		{
			editorUpdateAllHandles();
			matrix3Comparison = new Matrix3(t().getWorldRenderTransform(false));
		}
	}

	@Override
	public void onLateUpdate()
	{
		if (Lttl.game.inEditor()) onEditorLateUpdate();
	}

	@Override
	public void onEnable()
	{
		super.onEnable();
		updatePathIfNecessary();
	}

	@Override
	public void onEditorEnable()
	{
		super.onEditorEnable();
		updatePathIfNecessary();
	}

	@Override
	public void debugDraw()
	{
		// path line
		boolean alreadyUpdatedWorldValues = false;
		if (((showLine && !isEditing()) || (isEditing() && showEditorLine))
				&& pathArray.size() > 1)
		{
			if (debugPoints == null)
			{
				debugPoints = new Vector2Array(pathArray.size());
			}
			debugPoints.clear();
			debugPoints.addAll(pathArray);
			// offset the points so they take on the transform's transform
			t().updateWorldValues();
			alreadyUpdatedWorldValues = true;
			for (int i = 0; i < debugPoints.size(); i++)
			{
				debugPoints.get(i, tmp0);
				t().renderToWorldPosition(tmp0, false);
				debugPoints.set(i, tmp0);
			}
			if (closed && pathArray.size() > 2)
			{
				Lttl.debug
						.drawPolygonOutline(
								debugPoints,
								Lttl.editor.getSettings().pathSettings.lineWidth
										* Lttl.debug.eF(),
								(selectedControlPoints != null && selectedControlPoints
										.size() > 0) ? Lttl.editor
										.getSettings().pathSettings.focusedPathColor
										: Lttl.editor.getSettings().pathSettings.pathColor);
			}
			else
			{
				Lttl.debug
						.drawLines(
								debugPoints,
								Lttl.editor.getSettings().pathSettings.lineWidth
										* Lttl.debug.eF(),
								false,
								(selectedControlPoints != null && selectedControlPoints
										.size() > 0) ? Lttl.editor
										.getSettings().pathSettings.focusedPathColor
										: Lttl.editor.getSettings().pathSettings.pathColor);
			}
		}

		// control points
		// only show the control points if not editing, since handles will be there if editing
		if (!isEditing() && showControlPoints)
		{
			if (!alreadyUpdatedWorldValues)
			{
				alreadyUpdatedWorldValues = true;
				t().updateWorldValues();
			}
			for (LttlPathControlPoint cp : controlPoints)
			{
				switch (cp.type)
				{
					case Handles:
					case Proximity:
						Lttl.debug
								.drawCircle(
										t().renderToWorldPosition(
												tmp0.set(cp.pos), false),
										Lttl.editor.getSettings().pathSettings.controlPointSize
												/ 2 * Lttl.debug.eF(),
										Lttl.editor.getSettings().pathSettings.controlPointDebugColor);
						break;
					case Sharp:
						Lttl.debug
								.drawRect(
										t().renderToWorldPosition(
												tmp0.set(cp.pos), false),
										Lttl.editor.getSettings().pathSettings.controlPointSize
												* Lttl.debug.eF(),
										Lttl.editor.getSettings().pathSettings.controlPointSize
												* Lttl.debug.eF(),
										0,
										Lttl.editor.getSettings().pathSettings.controlPointDebugColor);
						break;

				}
			}
		}

		// handle lines only shown if single selected
		if (isEditing() && selectedControlPoints.size() == 1)
		{
			LttlPathControlPoint cp = selectedControlPoints.get(0);
			if (shouldHaveHandles(cp))
			{
				Lttl.debug
						.drawLine(
								cp.mainHandle.position,
								cp.leftHandle.position,
								Lttl.editor.getSettings().pathSettings.lineWidth,
								cp.handlesLocked ? Lttl.editor.getSettings().pathSettings.handleLockedLineColor
										: Lttl.editor.getSettings().pathSettings.leftHandleLineColor);
				Lttl.debug
						.drawLine(
								cp.mainHandle.position,
								cp.rightHandle.position,
								Lttl.editor.getSettings().pathSettings.lineWidth,
								cp.handlesLocked ? Lttl.editor.getSettings().pathSettings.handleLockedLineColor
										: Lttl.editor.getSettings().pathSettings.rightHandleLineColor);
			}
		}

		// draw each path array point
		if (showPathPoints && pathArray.size() > 0)
		{
			if (!alreadyUpdatedWorldValues)
			{
				alreadyUpdatedWorldValues = true;
				t().updateWorldValues();
			}
			for (int i = 0; i < pathArray.size(); i++)
			{
				Lttl.debug
						.drawCircle(
								t().renderToWorldPosition(
										pathArray.get(i, tmp0), false),
								Lttl.editor.getSettings().pathSettings.controlPointSize
										/ 4 * Lttl.debug.eF(),
								Lttl.editor.getSettings().pathSettings.pathPointsColor);
			}
		}
	}

	/**
	 * Toggles edit mode for this path
	 */
	@GuiButton(order = 0)
	private void editToggle()
	{
		// if already editing, stop
		if (isEditing())
		{
			editorStop();
			return;
		}
		editorStart();
	}

	/**
	 * Toggles edit mode for this path
	 */
	@GuiButton(order = 1)
	private void editToggleLock()
	{
		// if already editing, stop
		if (isEditing())
		{
			editorStop();
			return;
		}
		Lttl.editor.getGui().getSelectionController().lockSelection();
		editorStart();
	}

	private void editorStart()
	{
		Lttl.logNote("Started editing path.");

		// set start editing values
		isEditing = true;
		if (selectedControlPoints == null)
		{
			selectedControlPoints = new ArrayList<LttlPathControlPoint>();
		}
		selectedControlPoints.clear();
		editorUpdateComparisonList();

		editorInitHandles();
	}

	void editorInitHandles()
	{
		// create handles for each control point
		for (final LttlPathControlPoint cp : controlPoints)
		{
			editorInitHandles(cp);
		}

		// create multiselect scale handle
		if (multiSelectScaleHandle != null)
		{
			multiSelectScaleHandle.unregister();
		}
		multiSelectScaleHandle = new HandleRect(0, 0, -1,
				Lttl.editor.getSettings().pathSettings.controlPointSize * .7f,
				Lttl.editor.getSettings().pathSettings.controlPointSize * .7f,
				true, 0, true, false, true, false,
				Lttl.editor.getSettings().handleSclColor, 0, null)
		{
			Vector2Array originalPositions = new Vector2Array();
			Vector2 cpCenter = new Vector2();
			float scaleFactorX = 0;
			float scaleFactorY = 0;

			private boolean dragged = false;

			@Override
			public void onPressed()
			{
				// SAVE UNDO VALUE
				saveUndoObject();

				// save each selected cp's original position
				originalPositions.clear();
				originalPositions.ensureCapacity(selectedControlPoints.size());
				for (LttlPathControlPoint cp : selectedControlPoints)
				{
					originalPositions.add(cp.pos);
				}
				scaleFactorX = 0;
				scaleFactorY = 0;
				originalPositions.getCenter(cpCenter);
				dragged = false;
			}

			@Override
			public void onReleased()
			{
				justReleasedHandle = true;
				if (dragged)
				{
					registerUndoState();
				}
			}

			@Override
			public void onDrag(float deltaX, float deltaY)
			{
				dragged = true;
				if (!this.canLockAxis || this.getLockedAxis() != LockedAxis.Y)
				{
					if (Lttl.input.getEditorX() <= this.position.x)
					{
						scaleFactorX += deltaX
								/ Lttl.editor.getSettings().handleScaleSmoothness
								/ Lttl.editor.getSettings().handleScaleDecreasingSmoothness
								* Lttl.editor.getCamera().zoom;
					}
					else
					{
						scaleFactorX += deltaX
								/ Lttl.editor.getSettings().handleScaleSmoothness
								* Lttl.editor.getCamera().zoom;
					}
				}
				else
				{
					if (Lttl.input.getEditorX() <= this.position.y)
					{
						scaleFactorY += deltaY
								/ Lttl.editor.getSettings().handleScaleSmoothness
								/ Lttl.editor.getSettings().handleScaleDecreasingSmoothness
								* Lttl.editor.getCamera().zoom;
					}
					else
					{
						scaleFactorY += deltaY
								/ Lttl.editor.getSettings().handleScaleSmoothness
								* Lttl.editor.getCamera().zoom;
					}
				}
				if (!this.canLockAxis
						|| this.getLockedAxis() == LockedAxis.None)
				{
					scaleFactorY = scaleFactorX;
				}

				for (int i = 0; i < originalPositions.size(); i++)
				{
					LttlPathControlPoint cp = selectedControlPoints.get(i);
					originalPositions.get(i, cp.pos).add(
							(cp.pos.x - cpCenter.x) * scaleFactorX,
							(cp.pos.y - cpCenter.y) * scaleFactorY);
					editorUpdateHandles(cp);
				}
				editorUpdatePath();
			}
		};
		multiSelectScaleHandle.visible = false;
	}

	private void editorStop()
	{
		Lttl.editor.getGui().getSelectionController().unlockSelection();

		isEditing = false;

		// unregister all handles
		for (LttlPathControlPoint cp : controlPoints)
		{
			editorUnregisterAllHandles(cp);
		}
		if (multiSelectScaleHandle != null)
		{
			multiSelectScaleHandle.unregister();
			multiSelectScaleHandle = null;
		}
		// and comparison ones just in case
		for (LttlPathControlPoint cp : comparisonControlPoints)
		{
			editorUnregisterAllHandles(cp);
		}
		Lttl.logNote("Stopped editing path.");
	}

	private void editorShowSelectionGUI()
	{
		if (selectedControlPoints.size() == 0) return;

		if (Lttl.editor.getGui().getPropertiesController()
				.getFocusTransformId() != t().getId())
		{
			Lttl.editor.getGui().getSelectionController().setSelection(t());
			if (Lttl.editor.getGui().getPropertiesController()
					.getFocusTransformId() != t().getId()) { return; }
		}

		((GuiComponentComponent) Lttl.editor.getGui().getPropertiesController()
				.findComponentGFO(this)).collapsableGroup
				.setCollapseState(false);

		GuiComponentArrayList gca = getControlPointGFO();
		gca.collapsableGroup.setCollapseState(false);

		for (GuiFieldObject<?> child2 : gca.children)
		{
			GuiComponentObject gco = (GuiComponentObject) child2;
			if (selectedControlPoints.contains(gco.objectRef))
			{
				gco.collapsableGroup.setCollapseState(false);
			}
			else
			{
				gco.collapsableGroup.setCollapseState(true);
			}
		}
		return;
	}

	private GuiComponentArrayList getControlPointGFO()
	{
		GuiFieldObject<?> gfo = Lttl.editor.getGui().getPropertiesController()
				.findComponentGFO(this);
		for (GuiFieldObject<?> child : gfo.children)
		{
			if (child.objectRef == controlPoints) { return (GuiComponentArrayList) child; }
		}
		return null;
	}

	private void editorDelete(LttlPathControlPoint cp)
	{
		if (controlPoints.size() == 2) return;

		saveUndoObject();
		selectedControlPoints.remove(cp);
		controlPoints.remove(cp);
		registerUndoState();
		editorUnregisterAllHandles(cp);
		editorUpdateComparisonList();
		editorUpdatePath();
	}

	/**
	 * this needs to be ran whenever a new control point is added when this path is editing, so it can have handles path
	 * points can be added with double click or via arrayList
	 * 
	 * @param cp
	 */
	private void editorInitHandles(final LttlPathControlPoint cp)
	{
		// unregister old handles if any
		editorUnregisterAllHandles(cp);

		// handle positions and colors will be initialized in the update at end

		// create main handle
		switch (cp.type)
		{
			case Handles:
			case Proximity:
				cp.mainHandle = new HandleCircle(
						0,
						0,
						0,
						Lttl.editor.getSettings().pathSettings.controlPointSize / 2,
						true,
						true,
						true,
						true,
						false,
						Lttl.editor.getSettings().pathSettings.controlPointEditorColor,
						null)
				{
					@Override
					public void onPressed()
					{
						if (Lttl.input.isEditorMousePressed(0))
						{
							saveUndoObject();
						}
					}

					@Override
					public void onReleased()
					{
						justReleasedHandle = true;
						if (Lttl.input.isEditorMouseReleased(1)
								&& selectedControlPoints.contains(cp))
						{
							JMenuItem mi = new JMenuItem("Open GUI");
							mi.addActionListener(new ActionListener()
							{
								@Override
								public void actionPerformed(ActionEvent e)
								{
									editorShowSelectionGUI();
								}
							});
							Lttl.editor.getGui().addRightClickMenu(mi);
						}

						if (Lttl.input.isEditorControl()
								&& Lttl.input.isEditorMouseReleased(0)
								&& Lttl.game.getRawTime()
										- Lttl.input
												.getLastMouseReleasedTime0() < doubleClickTime)
						{
							editorDelete(cp);
						}
						else if (Lttl.input.isEditorMouseReleased(0)
								&& wasDragged())
						{
							registerUndoState();
						}
					}

					@Override
					public void onDrag(float deltaX, float deltaY)
					{
						cp.pos.set(this.position);
						t().worldToRenderPosition(cp.pos, true);
						if (shouldHaveHandles(cp))
						{
							editorUpdateHandles(cp);
						}
						// shift the others in selection
						if (selectedControlPoints.size() > 1)
						{
							for (LttlPathControlPoint cp2 : selectedControlPoints)
							{
								if (cp2 == cp) continue;
								cp2.mainHandle.position.add(deltaX, deltaY);
								cp2.pos.set(cp2.mainHandle.position);
								t().worldToRenderPosition(cp2.pos, false);
								if (shouldHaveHandles(cp2))
								{
									editorUpdateHandles(cp2);
								}
							}
							updateMultiSelectHandle();
						}
						editorUpdatePath();
					}
				};
				break;
			case Sharp:
				cp.mainHandle = new HandleRect(
						0,
						0,
						0,
						Lttl.editor.getSettings().pathSettings.controlPointSize,
						Lttl.editor.getSettings().pathSettings.controlPointSize,
						true,
						0,
						true,
						true,
						true,
						false,
						Lttl.editor.getSettings().pathSettings.controlPointEditorColor,
						0, null)
				{
					// NOTE SAME AS ABOVE
					@Override
					public void onPressed()
					{
						if (Lttl.input.isEditorMousePressed(0))
						{
							saveUndoObject();
						}
					}

					@Override
					public void onReleased()
					{
						justReleasedHandle = true;
						if (Lttl.input.isEditorMouseReleased(1)
								&& selectedControlPoints.contains(cp))
						{
							JMenuItem mi = new JMenuItem("Open GUI");
							mi.addActionListener(new ActionListener()
							{
								@Override
								public void actionPerformed(ActionEvent e)
								{
									editorShowSelectionGUI();
								}
							});
							Lttl.editor.getGui().addRightClickMenu(mi);
						}

						if (Lttl.input.isEditorControl()
								&& Lttl.input.isEditorMouseReleased(0)
								&& Lttl.game.getRawTime()
										- Lttl.input
												.getLastMouseReleasedTime0() < doubleClickTime)
						{
							editorDelete(cp);
						}
						else if (Lttl.input.isEditorMouseReleased(0)
								&& wasDragged())
						{
							registerUndoState();
						}
					}

					@Override
					public void onDrag(float deltaX, float deltaY)
					{
						cp.pos.set(this.position);
						t().worldToRenderPosition(cp.pos, true);
						if (shouldHaveHandles(cp))
						{
							editorUpdateHandles(cp);
						}
						// shift the others in selection
						if (selectedControlPoints.size() > 1)
						{
							for (LttlPathControlPoint cp2 : selectedControlPoints)
							{
								if (cp2 == cp) continue;
								cp2.mainHandle.position.add(deltaX, deltaY);
								cp2.pos.set(cp2.mainHandle.position);
								t().worldToRenderPosition(cp2.pos, false);
								if (shouldHaveHandles(cp2))
								{
									editorUpdateHandles(cp2);
								}
							}
							updateMultiSelectHandle();
						}
						editorUpdatePath();
					}
				};
				break;

		}

		// Create CP Handles
		if (shouldHaveHandles(cp))
		{
			cp.leftHandle = new HandleCircle(0, 0, -1,
					Lttl.editor.getSettings().pathSettings.handleSize * 2,
					true, true, true, true, false,
					Lttl.editor.getSettings().pathSettings.handleColor, null)
			{
				@Override
				public void onPressed()
				{
					if (Lttl.input.isEditorMousePressed(0))
					{
						saveUndoObject();
					}
				}

				@Override
				public void onReleased()
				{
					if (Lttl.input.isEditorMouseReleased(0))
					{
						justReleasedHandle = true;
						if (wasDragged())
						{
							registerUndoState();
						}
					}
					else if (Lttl.input.isEditorMouseReleased(1))
					{
						JMenuItem mi = new JMenuItem("Vertical");
						mi.addActionListener(new ActionListener()
						{
							@Override
							public void actionPerformed(ActionEvent e)
							{
								cp.leftPos.setAngle(90);
								// update other handle if locked
								if (cp.handlesLocked)
								{
									cp.lockFromLeftInternal(0);
								}
								editorUpdateHandles(cp);
								editorUpdatePath();
							}
						});
						Lttl.editor.getGui().addRightClickMenu(mi);
						JMenuItem mii = new JMenuItem("Horizontal");
						mii.addActionListener(new ActionListener()
						{
							@Override
							public void actionPerformed(ActionEvent e)
							{
								cp.leftPos.setAngle(0);
								// update other handle if locked
								if (cp.handlesLocked)
								{
									cp.lockFromLeftInternal(0);
								}
								editorUpdateHandles(cp);
								editorUpdatePath();
							}
						});
						Lttl.editor.getGui().addRightClickMenu(mii);

						JMenuItem miii = new JMenuItem("Other Same Length");
						miii.addActionListener(new ActionListener()
						{
							@Override
							public void actionPerformed(ActionEvent e)
							{
								cp.rightPos.setLength(cp.leftPos.len());
								editorUpdateHandles(cp);
								editorUpdatePath();
							}
						});
						Lttl.editor.getGui().addRightClickMenu(miii);

						JMenuItem miiii = new JMenuItem("Reset Both");
						miiii.addActionListener(new ActionListener()
						{
							@Override
							public void actionPerformed(ActionEvent e)
							{
								cp.resetHandles();
								editorUpdateHandles(cp);
								editorUpdatePath();
							}
						});
						Lttl.editor.getGui().addRightClickMenu(miiii);
					}
				}

				@Override
				public void onDrag(float deltaX, float deltaY)
				{
					float origLen = 0;
					if (cp.handlesLocked && !Lttl.input.isEditorAlt())
					{
						origLen = cp.leftPos.len();
					}

					// update relative position
					cp.leftPos.set(this.position);
					t().worldToRenderPosition(cp.leftPos, true)
							.sub(cp.pos)
							.scl(1 / Lttl.editor.getSettings().pathSettings.handleLength);

					// update other handle if locked
					if (cp.handlesLocked)
					{
						if (Lttl.input.isEditorAlt())
						{
							cp.lockFromLeft();
						}
						else
						{
							cp.lockFromLeftInternal(cp.leftPos.len() - origLen);
						}
						editorUpdateHandles(cp);
					}
					editorUpdatePath();
				}
			};
			cp.rightHandle = new HandleCircle(0, 0, -1,
					Lttl.editor.getSettings().pathSettings.handleSize * 2,
					true, true, true, true, false,
					Lttl.editor.getSettings().pathSettings.handleColor, null)
			{
				@Override
				public void onPressed()
				{
					if (Lttl.input.isEditorMousePressed(0))
					{
						saveUndoObject();
					}
				}

				@Override
				public void onReleased()
				{
					if (Lttl.input.isEditorMouseReleased(0))
					{
						justReleasedHandle = true;
						if (wasDragged())
						{
							registerUndoState();
						}
					}
					else if (Lttl.input.isEditorMouseReleased(1))
					{
						JMenuItem mi = new JMenuItem("Vertical");
						mi.addActionListener(new ActionListener()
						{
							@Override
							public void actionPerformed(ActionEvent e)
							{
								cp.rightPos.setAngle(270);
								// update other handle if locked
								if (cp.handlesLocked)
								{
									cp.lockFromRightInternal(0);
								}
								editorUpdateHandles(cp);
								editorUpdatePath();
							}
						});
						Lttl.editor.getGui().addRightClickMenu(mi);

						JMenuItem mii = new JMenuItem("Horizontal");
						mii.addActionListener(new ActionListener()
						{
							@Override
							public void actionPerformed(ActionEvent e)
							{
								cp.rightPos.setAngle(-180);
								// update other handle if locked
								if (cp.handlesLocked)
								{
									cp.lockFromRightInternal(0);
								}
								editorUpdateHandles(cp);
								editorUpdatePath();
							}
						});
						Lttl.editor.getGui().addRightClickMenu(mii);

						JMenuItem miii = new JMenuItem("Other Same Length");
						miii.addActionListener(new ActionListener()
						{
							@Override
							public void actionPerformed(ActionEvent e)
							{
								cp.leftPos.setLength(cp.rightPos.len());
								editorUpdateHandles(cp);
								editorUpdatePath();
							}
						});
						Lttl.editor.getGui().addRightClickMenu(miii);

						JMenuItem miiii = new JMenuItem("Reset Both");
						miiii.addActionListener(new ActionListener()
						{
							@Override
							public void actionPerformed(ActionEvent e)
							{
								cp.resetHandles();
								editorUpdateHandles(cp);
								editorUpdatePath();
							}
						});
						Lttl.editor.getGui().addRightClickMenu(miiii);
					}
				}

				@Override
				public void onDrag(float deltaX, float deltaY)
				{
					float origLen = 0;
					if (cp.handlesLocked && !Lttl.input.isEditorAlt())
					{
						origLen = cp.rightPos.len();
					}

					// update relative position
					// set to world current handle position (world)
					cp.rightPos.set(this.position);
					t().worldToRenderPosition(cp.rightPos, true)
							.sub(cp.pos)
							.scl(1 / Lttl.editor.getSettings().pathSettings.handleLength);

					// update other handle if locked
					if (cp.handlesLocked)
					{
						if (Lttl.input.isEditorAlt())
						{
							cp.lockFromRight();
						}
						else
						{
							cp.lockFromRightInternal(cp.rightPos.len()
									- origLen);
						}
						editorUpdateHandles(cp);
					}
					editorUpdatePath();
				}
			};

			// if locked, make sure their positions are locked
			if (cp.handlesLocked)
			{
				cp.lockFromLeft();
			}
		}

		// update the handles to give initial color and position settings
		editorUpdateHandles(cp);
	}

	/**
	 * This should callback whenever a control point is modified in the gui, or the arraylist adds, removes, or moves
	 */
	void guiControlPointsUpdated()
	{
		if (!isEditing())
		{
			// if is not editing, then just update the path, since we don't need to do any handles
			editorUpdatePath();
			return;
		}

		// check if added one or remove one from arraylist so we can create or remove handles
		for (LttlPathControlPoint cp : controlPoints)
		{
			// check if this control point is new
			if (comparisonControlPoints.contains(cp))
			{
				comparisonControlPoints.remove(cp);
			}
			else
			{
				// new
				setDefaults(cp);
				editorInitHandles(cp);
			}
		}
		// these are the ones that must have been removed
		for (LttlPathControlPoint cp : comparisonControlPoints)
		{
			editorUnregisterAllHandles(cp);
		}
		editorUpdateComparisonList();

		// update all handles
		editorInitHandles();

		// finally update the path
		editorUpdatePath();
	}

	private LttlPathControlPoint setDefaults(LttlPathControlPoint cp)
	{
		cp.handlesLocked = handlesLockedDefault;
		cp.type = defaultControlPointType;
		return cp;
	}

	/**
	 * resets the control points and initializes with starter points
	 */
	@GuiButton(order = 9)
	public void reset()
	{
		boolean wasEditing = isEditing();
		if (isEditing())
		{
			editorStop();
		}

		controlPoints.clear();
		if (selectedControlPoints != null)
		{
			selectedControlPoints.clear();
		}
		controlPoints.add(new LttlPathControlPoint(new Vector2(-5
				* Lttl.game.getSettings().getWidthFactor(), 0)));
		controlPoints.add(new LttlPathControlPoint(new Vector2(5 * Lttl.game
				.getSettings().getWidthFactor(), 0)));
		closed = false;
		editorUpdateComparisonList();

		editorUpdatePath();

		if (wasEditing)
		{
			editorStart();
		}
	}

	/**
	 * Generates a path array with the default values on path and saves the result to the array provided.<br>
	 * Does not mark as updated.
	 * 
	 * @param result
	 *            stores the new values in the result
	 */
	public Vector2Array generatePath(Vector2Array result)
	{
		return generatePath(tSamples, angleChangeThreshold, result,
				controlPointPathIndexArray);
	}

	/**
	 * Generates a path array with custom tSamples, uses path's angleChangeThreshold, and saves the result to the arrays
	 * provided.<br>
	 * Does not mark as updated.
	 * 
	 * @param c_tSamples
	 *            (interpolation occurs with values > 1)
	 * @param result
	 *            stores the new values in the result
	 * @param controlPointPathIndexArray
	 *            stores the control points' path index's in this, can be null
	 * @return
	 */
	public Vector2Array generatePath(int c_tSamples, Vector2Array result,
			IntArray controlPointPathIndexArray)
	{
		return generatePath(c_tSamples, angleChangeThreshold, result,
				controlPointPathIndexArray);
	}

	/**
	 * Generates a path array with custom values, and saves the result to the arrays provided.<br>
	 * Does not mark as updated.
	 * 
	 * @param c_tSamples
	 *            (interpolation occurs with values > 1)
	 * @param c_angleChangeThreshold
	 * @param result
	 *            stores the new values in the result
	 * @param controlPointPathIndexArray
	 *            stores the control points' path index's in this, can be null
	 * @return
	 */
	public Vector2Array generatePath(int c_tSamples,
			float c_angleChangeThreshold, Vector2Array result,
			IntArray controlPointPathIndexArray)
	{
		LttlProfiler.pathUpdates.add();

		c_angleChangeThreshold = LttlMath.abs(c_angleChangeThreshold);
		if (controlPointPathIndexArray != null)
		{
			controlPointPathIndexArray.clear();
			controlPointPathIndexArray.ensureCapacity(controlPoints.size());
		}

		// prepare results array
		result.clear();
		// can't ensure capacity accurately beyond this because of angleThreshold and c_tSamples
		result.ensureCapacity(controlPoints.size());

		// this holds the previous pathPoint's angle which will be compared to the potential path points angle, only
		// necessary
		float lastAngle = 0;

		// (-1) this keeps it looking like a line when the shape has not been closed yet
		// once the shape closes it allows all the control points, even the last one to be used
		for (int j = 0; j < controlPoints.size() - ((closed) ? 0 : 1); j++)
		{
			LttlPathControlPoint currentPoint = controlPoints.get(j);
			LttlPathControlPoint nextPoint = (closed || j + 1 < controlPoints
					.size()) ? controlPoints
					.get((j + 1) % controlPoints.size()) : null;

			// add the current control point always as a path point regardless of tSamples or angleThreshold
			result.add(currentPoint.pos);
			// set path index for the control point
			if (controlPointPathIndexArray != null)
			{
				controlPointPathIndexArray.add(result.size() - 1);
			}

			if (c_tSamples > 1)
			{
				// only need to update lastAngle if there is interpolation (tSamples > 1)
				// if not the first control point, calculate the lastAngle from the current control point, which was
				// just added above, and whatever point was added before that (there will always be one)
				if (j > 0)
				{
					lastAngle = LttlMath.GetAngle(
							result.getLastX() - result.getX(result.size() - 2),
							result.getLastY() - result.getY(result.size() - 2),
							true);
				}

				// only interpolate if tSamples > 1 and if the currentControl point and the next are not sharp
				if (!(currentPoint.type == LttlPathControlPointType.Sharp && (nextPoint == null || nextPoint.type == LttlPathControlPointType.Sharp)))
				{
					// generate new prep points to use in CatPathInterp
					if (useBezier)
					{
						generateBezierPrepPoints(j);
					}
					else
					{
						generateCatMullRomPrepPoints(j);
					}

					// interpolate through the specified number of (t)Samples between each control point, not adding the
					// first, since it is the first of the next, and the first was added above always
					for (int i = 1; i < c_tSamples; i++)
					{
						float pm = (float) i / c_tSamples;
						// saves the path point into samplePoint
						if (useBezier)
						{
							LttlBezier.cubic(prepPoints, pm, tmp1);
						}
						else
						{
							LttlMath.CatSplineInterp(prepPoints, pm, tmp1);
						}

						// compare angles if the sample point is neither of the control points (first or last)
						// get new angle from the last point to the current one being added
						float newAngle = LttlMath.GetAngle(
								(tmp0.set(tmp1).sub(result.getLastX(),
										result.getLastY())), true);

						// it is optimal if it is checking > c_angleChangeThreshold because if angles are the same and
						// the c_angleChangeThreshold is 0, then we don't want to add a point
						if (LttlMath.abs(newAngle - lastAngle) > c_angleChangeThreshold)
						{
							// if the angle has changed beyond the threshold
							result.add(tmp1);
							// save the new angle
							lastAngle = newAngle;
						}
					}
				}
			}
		}

		// if not a closed path, adds the last control point
		if (!closed)
		{
			result.add(controlPoints.get(controlPoints.size() - 1).pos);
			if (controlPointPathIndexArray != null)
			{
				controlPointPathIndexArray.add(result.size() - 1);
			}
		}

		return result;
	}

	@Override
	public void onEditorDestroyComp()
	{
		processOnDestroyComp();
	}

	@Override
	public void onDestroyComp()
	{
		processOnDestroyComp();
	}

	private void processOnDestroyComp()
	{
		if (isEditing())
		{
			editorStop();
		}
	}

	private int getActualIndex(int index)
	{
		if (closed)
		{
			return LttlMath.loopIndex(index, controlPoints.size());
		}
		else
		{
			return LttlMath.clamp(index, 0, controlPoints.size() - 1);
		}
	}

	/**
	 * generates the 4 points needed for the path equation for the specified control point settings
	 * 
	 * @param controlPointIndex
	 */
	private void generateCatMullRomPrepPoints(int controlPointIndex)
	{
		/* LEFT HANDLE */
		float aX = 0;
		float aY = 0;

		switch (controlPoints.get(controlPointIndex).type)
		{
			case Handles:
			{
				// switch the left and right handle (reciprocal), this way it acts more like a traditional bezier
				// curve with handles
				Vector2 v = tmp0
						.set(controlPoints.get(controlPointIndex).rightPos);
				v.setLength(v.len() * LttlPath.catmullromHandleWeight);
				aX = controlPoints.get(controlPointIndex).pos.x - v.x;
				aY = controlPoints.get(controlPointIndex).pos.y - v.y;
			}
				break;
			case Proximity:
			{
				// use the previous control point as the handle influence
				// the previous control point by default will be the "one previous", however...
				// define the first prep point (w/o handles)
				Vector2 v = controlPoints
						.get(getActualIndex(controlPointIndex - 1)).pos;
				aX = v.x;
				aY = v.y;
			}
				break;
			case Sharp:
			{
				Vector2 v = controlPoints.get(controlPointIndex).pos;
				aX = v.x;
				aY = v.y;
			}
				break;
		}

		/* FIRST POINT */
		// (never going to be a handle)
		Vector2 b = controlPoints.get(controlPointIndex).pos;

		/* SECOND POINT*/
		// should always be the next point
		// by default it is the "next control point" so add 1
		int secondControlPointIndex = getActualIndex(controlPointIndex + 1);
		Vector2 c = controlPoints.get(getActualIndex(controlPointIndex + 1)).pos;

		/* RIGHT HANDLE */
		// (if the handles are not enabled, then this will actually be the next control point)
		float dX = 0;
		float dY = 0;
		switch (controlPoints.get(secondControlPointIndex).type)
		{
			case Handles:
			{
				// flip the handle
				Vector2 v = tmp0
						.set(controlPoints.get(secondControlPointIndex).leftPos);
				v.setLength(v.len() * LttlPath.catmullromHandleWeight);
				dX = controlPoints.get(secondControlPointIndex).pos.x - v.x;
				dY = controlPoints.get(secondControlPointIndex).pos.y - v.y;
			}
				break;
			case Proximity:
			{
				Vector2 v = controlPoints
						.get(getActualIndex(controlPointIndex + 2)).pos;
				dX = v.x;
				dY = v.y;
			}
				break;
			case Sharp:
			{
				// set the right handle on next control point to itself
				Vector2 v = controlPoints.get(secondControlPointIndex).pos;
				dX = v.x;
				dY = v.y;
			}
				break;
		}

		// clear array and add all
		prepPoints.clear();
		prepPoints.addAll(aX, aY, b.x, b.y, c.x, c.y, dX, dY);
	}

	/**
	 * generates the 4 points needed for the path equation for the specified control point settings, assumes all control
	 * points are handles
	 * 
	 * @param controlPointIndex
	 */
	private void generateBezierPrepPoints(int controlPointIndex)
	{
		/* P0 */
		float aX = 0;
		float aY = 0;
		{
			Vector2 v = controlPoints.get(controlPointIndex).pos;
			aX = v.x;
			aY = v.y;
		}

		/*P1*/
		float bX = 0;
		float bY = 0;
		{
			LttlPathControlPoint cp = controlPoints.get(controlPointIndex);
			Vector2 v = cp.type == LttlPathControlPointType.Sharp ? Vector2.Zero
					: cp.rightPos;
			bX = controlPoints.get(controlPointIndex).pos.x + v.x;
			bY = controlPoints.get(controlPointIndex).pos.y + v.y;
		}
		/* P2*/
		float cX = 0;
		float cY = 0;
		int secondControlPointIndex = getActualIndex(controlPointIndex + 1);
		{
			LttlPathControlPoint cp = controlPoints
					.get(secondControlPointIndex);
			Vector2 v = cp.type == LttlPathControlPointType.Sharp ? Vector2.Zero
					: cp.leftPos;
			cX = controlPoints.get(secondControlPointIndex).pos.x + v.x;
			cY = controlPoints.get(secondControlPointIndex).pos.y + v.y;
		}

		/* P3 */
		float dX = 0;
		float dY = 0;
		{
			Vector2 v = controlPoints.get(secondControlPointIndex).pos;
			dX = v.x;
			dY = v.y;
		}

		// clear array and add all
		prepPoints.clear();
		prepPoints.addAll(aX, aY, bX, bY, cX, cY, dX, dY);
	}

	/**
	 * Returns the LOCAL array used to draw this path.<br>
	 * This can be used by several components who want to use the path's points.<br>
	 * Can be null if updatePath() never ran.<br>
	 * <b>DO NOT MODIFY THIS.</b>
	 * 
	 * @return
	 */
	public Vector2Array getPath()
	{
		return pathArray;
	}

	/**
	 * Only updates path if not {@link #isUpdatedSinceModified()}.
	 * 
	 * @see #updatePath()
	 */
	public Vector2Array updatePathIfNecessary()
	{
		if (!isUpdatedSinceModified())
		{
			return updatePath();
		}
		else
		{
			return pathArray;
		}
	}

	/**
	 * Updates (marks it as updated) and generates the path array based on current control points and step and threshold
	 * settings.<br>
	 * Does not check if needs to update, use {@link LttlPath#isUpdatedSinceModified()} to check. After this getPath()
	 * can be used until future changes are made to the path.
	 * 
	 * @return the path array
	 */
	public Vector2Array updatePath()
	{
		if (pathArray == null)
		{
			pathArray = new Vector2Array();
		}
		if (controlPointPathIndexArray == null)
		{
			controlPointPathIndexArray = new IntArray();
		}

		updatedSinceModified = true;
		generatePath(pathArray);
		pointsCount = pathArray.size();

		// update path length always if not playing (and in editor) or if autoUpdateLength is true
		if (!Lttl.game.isPlaying() || autoUpdateLength)
		{
			updateLength();
		}

		return pathArray;
	}

	/**
	 * called whenever something is edited in the editor. calls modified and update path if necessary
	 */
	private void editorUpdatePath()
	{
		// this is set here because it is a common method that runs whenever the path is modified in editor
		modified();
		updatePathIfNecessary();
	}

	/**
	 * Checks if this LttlPath component is being edited
	 * 
	 * @return
	 */
	public boolean isEditing()
	{
		return isEditing;
	}

	/**
	 * populates the comparison list of starting control points before editing
	 */
	private void editorUpdateComparisonList()
	{
		if (comparisonControlPoints == null)
		{
			comparisonControlPoints = new ArrayList<LttlPathControlPoint>();
		}
		comparisonControlPoints.clear();
		comparisonControlPoints.addAll(controlPoints);
	}

	private void editorUnregisterAllHandles()
	{
		for (LttlPathControlPoint cp : controlPoints)
		{
			editorUnregisterAllHandles(cp);
		}
	}

	private void editorUnregisterAllHandles(LttlPathControlPoint cp)
	{
		if (cp.mainHandle != null)
		{
			cp.mainHandle.unregister();
			cp.mainHandle = null;
		}
		if (cp.leftHandle != null)
		{
			cp.leftHandle.unregister();
			cp.leftHandle = null;
		}
		if (cp.rightHandle != null)
		{
			cp.rightHandle.unregister();
			cp.rightHandle = null;
		}
	}

	/**
	 * Runs on handle initialization and when handles ever need to be updated
	 */
	private void editorUpdateHandles(LttlPathControlPoint cp)
	{
		if (cp.mainHandle != null)
		{
			// need to redefine the main handle as the appropriate handle type
			// stop and just unregister all handles and recreate the handles
			if ((cp.type == LttlPathControlPointType.Sharp && !HandleRect.class
					.isAssignableFrom(cp.mainHandle.getClass()))
					|| (cp.type != LttlPathControlPointType.Sharp && !HandleCircle.class
							.isAssignableFrom(cp.mainHandle.getClass())))
			{
				editorInitHandles(cp);
				return;
			}

			/* update handle positions */
			t().updateWorldValues();

			// update main handle position relative to transform
			cp.mainHandle.position.set(cp.pos);
			t().renderToWorldPosition(cp.mainHandle.position, false);

			// update handle color based on if selected
			cp.mainHandle.fillColor
					.set(selectedControlPoints.contains(cp) ? Lttl.editor
							.getSettings().pathSettings.selectedControlPointColor
							: Lttl.editor.getSettings().pathSettings.controlPointEditorColor);

			// CP HANDLES
			// if it's suppose to have left and right handles but doesn't, prob because modifed after init, then
			// reInitialize, if it has left and right handles when it shouldn't, also reInitialize and stop
			if (shouldHaveHandles(cp) ? (cp.leftHandle == null || cp.rightHandle == null)
					: (cp.leftHandle != null || cp.rightHandle != null))
			{
				editorInitHandles(cp);
				return;
			}

			// always show handles if it's not sharp but is using bezier
			if (shouldHaveHandles(cp))
			{
				// if cp is the only one selected, then show left and right handles and update position
				if (selectedControlPoints.contains(cp)
						&& selectedControlPoints.size() == 1)
				{
					// update cp handle position relative to main handle and relative to transform
					// make sure visible
					cp.leftHandle.visible = true;
					cp.leftHandle.position
							.set(cp.leftPos)
							.scl(Lttl.editor.getSettings().pathSettings.handleLength)
							.add(cp.pos);
					t().renderToWorldPosition(cp.leftHandle.position, false);

					cp.rightHandle.visible = true;
					cp.rightHandle.position
							.set(cp.rightPos)
							.scl(Lttl.editor.getSettings().pathSettings.handleLength)
							.add(cp.pos);
					t().renderToWorldPosition(cp.rightHandle.position, false);
				}
				else
				{
					// hide the handles
					if (cp.leftHandle != null) cp.leftHandle.visible = false;
					if (cp.rightHandle != null) cp.rightHandle.visible = false;
				}
			}
		}
	}

	private void editorUpdateFocusedHandle()
	{
		if (selectedControlPoints != null && selectedControlPoints.size() == 1)
		{
			editorUpdateHandles(selectedControlPoints.get(0));
		}
	}

	private void editorUpdateAllHandles()
	{
		for (LttlPathControlPoint cp : controlPoints)
		{
			editorUpdateHandles(cp);
		}
	}

	/**
	 * This will reset this transforms scale and rotation and apply it to the path.
	 */
	@GuiButton(order = 2)
	private void inheritScaleRotation()
	{
		t().updateWorldValues();
		for (LttlPathControlPoint cp : controlPoints)
		{
			t().renderToWorldPosition(cp.rightPos.add(cp.pos), false);
			t().renderToWorldPosition(cp.leftPos.add(cp.pos), false);
			t().renderToWorldPosition(cp.pos, false);
		}
		t().rotation = 0;
		t().scale.set(1, 1);
		t().updateWorldValues();
		for (LttlPathControlPoint cp : controlPoints)
		{
			t().worldToRenderPosition(cp.pos, false);
			t().worldToRenderPosition(cp.rightPos, false).sub(cp.pos);
			t().worldToRenderPosition(cp.leftPos, false).sub(cp.pos);
		}
		editorUpdatePath();
	}

	@GuiButton(order = 3)
	private void centerPath()
	{
		editorUpdatePath();
		center();
		editorUpdatePath();
	}

	/**
	 * Adjusts all the control point positions so the generated path is centered around the origin.<br>
	 * The path does not updatePath() afterward, need to do that manually.<br>
	 * This also assume the path is already updated to latest values, since it uses the pathArray for the center.
	 */
	public void center()
	{
		// get center
		getPath().getCenter(tmp0);

		for (LttlPathControlPoint cp : controlPoints)
		{
			cp.pos.sub(tmp0.x, tmp0.y);
		}
		modified();
	}

	/**
	 * Only updates path length if not {@link #isUpdatedLengthSinceModified()}.
	 * 
	 * @see #updateLength()
	 */
	public void updateLengthIfNecessary()
	{
		if (!isUpdatedLengthSinceModified())
		{
			updateLength();
		}
	}

	/**
	 * Calcualtes the length for this path based on the latest updatePath(). This needs to run AFTER updatePath() to be
	 * accurate. Use getPathLength to get total Length or length at a certain point.
	 */
	public void updateLength()
	{
		if (pathLengthArray == null)
		{
			pathLengthArray = new FloatArray(pathArray.size());
		}

		pathLengthArray.clear();
		pathLengthArray.ensureCapacity(pathArray.size());
		pathLengthArray.add(0);
		float sum = 0;
		for (int i = 1, s = pathArray.size(); i < s; i++)
		{
			sum += Vector2.dst(pathArray.getX(i - 1), pathArray.getY(i - 1),
					pathArray.getX(i), pathArray.getY(i));
			pathLengthArray.add(sum);
		}

		// if closed shape get distance from last point to the beginning
		if (closed && controlPoints.size() > 2)
		{
			sum += Vector2.dst(pathArray.getLastX(), pathArray.getLastY(),
					pathArray.getFirstX(), pathArray.getFirstY());
			pathLengthArray.add(sum);
		}

		updatedLengthSinceModified = true;
	}

	/* FUNCTIONS */

	/**
	 * Returns the length of the path based on the last updatePathLength() from the beginning to the specified index
	 * 
	 * @param index
	 *            the index to get the path length of, index 0 = 0, index 3 = point0 to point3
	 * @return
	 * @throws IndexOutOfBoundsException
	 */
	public float getPathLength(int index)
	{
		Lttl.Throw(pathLengthArray == null, "Length not updated.");
		Lttl.Throw(index < 0 || index > pathLengthArray.size - 1,
				new IndexOutOfBoundsException());
		return pathLengthArray.get(index);
	}

	/**
	 * Returns the entire length of the path based on the last updatePathLength();
	 * 
	 * @return
	 */
	public float getPathLength()
	{
		return getPathLength(pathLengthArray.size - 1);
	}

	/**
	 * Gets the linear interpolatied position on the path based on the given percentage and saves it in the output
	 * vector. Also returns the rotation at that position (derived from the slope). This uses length to provide a smooth
	 * interpolation regardless of point density (ie. curves vs straight parts).<br>
	 * <b>Be sure updatePath() and updateLength() have been ran since any changes.</b>
	 * 
	 * @param percentage
	 * @param positionOutput
	 *            the vector that is updated with the intepolated position
	 * @param slopeOutput
	 *            the vector that is updated with the slope at the interpolated position (note: the x and y have not
	 *            been flipped or anything, just use this to get angle via Vector2.angle() or LMath.angleLookup())
	 * @return positionOutput
	 */
	public Vector2 getPositionAndSlopeLerp(float percentage,
			Vector2 positionOutput, Vector2 slopeOutput)
	{
		// calculate the length to lerp to
		percentage = LttlMath.Clamp01(percentage);
		float lerpLen = getPathLength() * percentage;
		float innerLerp;

		// iterate through the pathLengthArray til you find the the points the interpolation is between
		int index = -1;
		float aLen = 0;
		float bLen = 0;
		// quick outs
		if (percentage == 0)
		{
			index = 0;
			innerLerp = 0;
		}
		else if (percentage == 1)
		{
			index = pathLengthArray.size - 1;
			innerLerp = 1;
		}
		else
		{
			for (int i = 0, n = pathLengthArray.size - 1; i < n; i++)
			{
				// note: the first length is assumed to be 0
				if (lerpLen <= pathLengthArray.get(i + 1))
				{
					aLen = pathLengthArray.get(i);
					bLen = pathLengthArray.get(i + 1);
					index = i;
					break;
				}
			}

			// calculate the innerLerp, what will be used to lerp between the two points
			float over = lerpLen - aLen;
			float dif = bLen - aLen;
			innerLerp = over / dif;
		}

		// set output as the first point
		pathArray.get(index, positionOutput);

		// this is a closed path or an open path where the index is not the last path point
		if (closed || index != pathArray.size() - 1)
		{
			Vector2 sharedNext = pathArray.getShared((index + 1)
					% (pathArray.size()));
			if (slopeOutput != null)
			{
				slopeOutput.set(sharedNext.y - positionOutput.y, sharedNext.x
						- positionOutput.x);
			}
			positionOutput.lerp(sharedNext, innerLerp);
		}
		else
		{
			// open path with index at last path point
			// calculating slope is normally done with next point, but for this, we'll do previous point because there
			// is no next point
			if (slopeOutput != null)
			{
				Vector2 sharedPrev = pathArray.getShared(index - 1);
				slopeOutput.set(positionOutput.y - sharedPrev.y,
						positionOutput.x - sharedPrev.x);
			}
		}

		return positionOutput;
	}

	/**
	 * Gets the linear interpolatied position on the path based on the given percentage and saves it in the output
	 * vector. Also returns the rotation at that position (derived from the slope). This uses length to provide a smooth
	 * interpolation regardless of point density (ie. curves vs straight parts).<br>
	 * <b>Be sure updatePath() and updateLength() have been ran since any changes.</b>
	 * 
	 * @param percentage
	 * @param output
	 *            the vector that is updated with the intepolated position
	 */
	public Vector2 getPositionLerp(float percentage, Vector2 output)
	{
		return getPositionAndSlopeLerp(percentage, output, null);
	}

	/**
	 * Get's the nearest point on the path (may be interpolated between 2 points) to the point given (relative to child
	 * transform) and sets it to that Vector2.<br>
	 * Efficiency: This finds the nearest point by comparing the distance to ALL the segment's nearest point. This is
	 * the most accurate. Best for paths with not many curves or paths that gets very close to itself or whenever the
	 * faster ones aren't working or when efficiency isn't a problem.
	 * 
	 * @param pointResult
	 *            the test point, and where the nearest point gets set to
	 * @param percentageResult
	 *            (optional) if not null, will calculate the percentage on path and set it to mututableFloat,requires
	 *            updatePathLength() and calcPercentage
	 * @return the index of path array point that preceded the nearest point
	 * @throws Lttl.Throw
	 *             if updatePath() has not ran atleast once
	 */
	public int getNearestPoint(Vector2 pointResult,
			LttlMutatableFloat percentageResult)
	{
		Lttl.Throw(pathArray.size() < 2);

		float minDst2 = Float.MAX_VALUE;
		int beginIndex = -1;

		for (int i = 0; i < pathArray.size() - ((closed) ? 0 : 1); i++)
		{
			int npi = LttlMath.loopIndex(i + 1, pathArray.size());
			Intersector.nearestSegmentPoint(pathArray.getX(i),
					pathArray.getY(i), pathArray.getX(npi),
					pathArray.getY(npi), pointResult.x, pointResult.y, tmp0);
			float dst2 = pointResult.dst2(tmp0);
			if (dst2 < minDst2)
			{
				beginIndex = i;
				minDst2 = dst2;
				// save the closest point to tmp2
				tmp1.set(tmp0);
			}
		}

		// save result
		pointResult.set(tmp1);

		if (percentageResult != null)
		{
			percentageResult.value = calcPercentageInternal(beginIndex,
					pointResult);
		}
		return beginIndex;
	}

	/**
	 * Get's the nearest point on the path (may be interpolated between 2 points) to the point given (relative to child
	 * transform) and sets it to that Vector2.<br>
	 * Efficiency: This checks every path point to find the closest, instead of narrowing them down by closest control
	 * point. Then it finds two nearest points, one for each segment connected to closest point. Best for paths that do
	 * not get close to itself or are linear (going in one direction mostly) or have drastic handles.
	 * 
	 * @param pointResult
	 *            updates with the nearest point
	 * @param percentageResult
	 *            (optional) if not null, will calculate the percentage on path and set it to mututableFloat,requires
	 *            updatePathLength() and calcPercentage
	 * @return the index of path array point that preceded the nearest point
	 * @throws Lttl.Throw
	 *             if updatePath() has not ran atleast once
	 */
	public int getNearestPointFast(Vector2 pointResult,
			LttlMutatableFloat percentageResult)
	{
		Lttl.Throw(pathArray.size() < 2);

		float minDst2 = Float.MAX_VALUE;
		int minIndex = -1;

		// find the path point that is closest
		for (int i = 0; i < pathArray.size(); i++)
		{
			float dst2 = pointResult.dst2(pathArray.getX(i), pathArray.getY(i));
			if (dst2 < minDst2)
			{
				minIndex = i;
				minDst2 = dst2;
			}
		}
		return getNearestPointInternal(pointResult, percentageResult, minIndex);
	}

	/**
	 * Get's the nearest point on the path (may be interpolated between 2 points) to the point given (relative to child
	 * transform) and sets it to that Vector2. <br>
	 * Efficiency: This does not check each path point, but narrows them down by closest control point. Then it finds
	 * two nearest points, one for each segment connected to closest point. Best for the simplest paths that do not
	 * overlap or get close to itself or are linear (going in one direction mostly).
	 * 
	 * @param pointResult
	 *            updates with the nearest point
	 * @param percentageResult
	 *            (optional) if not null, will calculate the percentage on path and set it to mututableFloat,requires
	 *            updatePathLength() and calcPercentage
	 * @return the index of path array point that preceded the nearest point
	 * @throws Lttl.Throw
	 *             if updatePath() has not ran atleast once
	 */
	public int getNearestPointFastest(Vector2 pointResult,
			LttlMutatableFloat percentageResult)
	{
		Lttl.Throw(pathArray.size() < 2);

		// find closest control point to given point
		float minDst2 = Float.MAX_VALUE;
		int closestIndex = -1;
		for (int j = 0; j < controlPoints.size(); j++)
		{
			LttlPathControlPoint cp = controlPoints.get(j);
			float dst2 = pointResult.dst2(cp.pos);
			if (dst2 < minDst2)
			{
				closestIndex = j;
				minDst2 = dst2;
			}
		}

		minDst2 = Float.MAX_VALUE;
		int minIndex = -1;

		// find the closest path point between the control point before and after the closet control point
		// before - between the before control point and the closest, there is no before control point if not closed
		// and closest is the first
		if (closed || closestIndex != 0)
		{
			int cpIndex = (closed) ? LttlMath.loopIndex(closestIndex - 1,
					controlPoints.size()) : closestIndex - 1;
			int startIndex = controlPointPathIndexArray.get(cpIndex);
			int endIndex = startIndex
					+ getNumberOfPointsInControlPoint(cpIndex);
			for (int i = startIndex; i <= endIndex; i++)
			{
				float dst2 = pointResult.dst2(pathArray.getX(i),
						pathArray.getY(i));
				if (dst2 < minDst2)
				{
					minIndex = i;
					minDst2 = dst2;
				}
			}
		}
		// after - between the after control point and the closest, there is no after control point if not closed and
		// closest is the last
		if (closed || closestIndex != controlPoints.size() - 1)
		{
			int startIndex = controlPointPathIndexArray.get(closestIndex);
			int endIndex = startIndex
					+ getNumberOfPointsInControlPoint(closestIndex);
			for (int i = startIndex; i <= endIndex; i++)
			{
				float dst2 = pointResult.dst2(pathArray.getX(i),
						pathArray.getY(i));
				if (dst2 < minDst2)
				{
					minIndex = i;
					minDst2 = dst2;
				}
			}
		}

		return getNearestPointInternal(pointResult, percentageResult, minIndex);
	}

	private int getNearestPointInternal(Vector2 point,
			LttlMutatableFloat percentageResult, int minIndex)
	{
		// now find if the prev or next point indexes
		int prev = LttlMath.loopIndex(minIndex - 1, pathArray.size());
		int next = LttlMath.loopIndex(minIndex + 1, pathArray.size());

		if (!closed)
		{
			// if minIndex was the last point, then no next point
			if (minIndex == pathArray.size() - 1)
			{
				next = -1;
			}
			// if minIndex was the first point, then no prev point
			else if (minIndex == 0)
			{
				prev = -1;
			}
		}

		// now get the closest point on each segment (before and after)
		if (prev != -1)
		{
			Intersector.nearestSegmentPoint(pathArray.getX(minIndex),
					pathArray.getY(minIndex), pathArray.getX(prev),
					pathArray.getY(prev), point.x, point.y, tmp0);
		}
		if (next != -1)
		{
			Intersector.nearestSegmentPoint(pathArray.getX(minIndex),
					pathArray.getY(minIndex), pathArray.getX(next),
					pathArray.getY(next), point.x, point.y, tmp1);
		}

		// if only have one or the other, then decision is easy
		if (prev == -1)
		{
			point.set(tmp1);
		}
		else if (next == -1)
		{
			// adjust for accurate percentage below
			minIndex--;
			point.set(tmp0);
		}
		else
		{
			// otherwise, decide which is closer
			if (point.dst2(tmp0) > point.dst2(tmp1))
			{
				point.set(tmp1);
			}
			else
			{
				// adjust for accurate percentage below
				minIndex--;
				point.set(tmp0);
			}
		}

		// optionally calculate the percentage of path
		if (percentageResult != null)
		{
			percentageResult.value = calcPercentageInternal(minIndex, point);
		}

		return minIndex;
	}

	/**
	 * Used internally.
	 * 
	 * @param beginIndex
	 *            the path point index that starts the segment that given point is presumably on
	 * @param point
	 * @return
	 */
	private float calcPercentageInternal(int beginIndex, Vector2 point)
	{
		return calcPercentageInternal(beginIndex, point.x, point.y);
	}

	/**
	 * Used internally.
	 * 
	 * @param beginIndex
	 *            the path point index that starts the segment that given point is presumably on
	 * @param x
	 * @param y
	 * @return
	 */
	private float calcPercentageInternal(int beginIndex, float x, float y)
	{
		Lttl.Throw(pathLengthArray == null);

		// minIndex should have been modified if the closer line segment was previous, then loop/clamp index
		beginIndex = LttlMath.loopIndex(beginIndex, pathArray.size());

		return (getPathLength(beginIndex) + Vector2.dst(
				pathArray.getX(beginIndex), pathArray.getY(beginIndex), x, y))
				/ getPathLength();
	}

	/**
	 * Finds the first position that has given x on the path.<br>
	 * If none found, then calcPercentage will return -1 and result will be populated with Float.NaN values.
	 * 
	 * @param x
	 * @param result
	 * @param calcPercentage
	 * @return optionally return the path percentage, updatePathLength() must have been done
	 */
	public float getFirstPositionAtX(float x, Vector2 result,
			boolean calcPercentage)
	{
		return getFirstPositionAtX(x, result, calcPercentage, 0,
				pathArray.size() - 1);
	}

	/**
	 * Finds the first position within the path points index range that has given x.<br>
	 * If none found, then calcPercentage will return -1 and result will be populated with Float.NaN values.
	 * 
	 * @param x
	 * @param result
	 * @param calcPercentage
	 * @param rangeStart
	 * @param rangeEnd
	 * @return optionally return the path percentage, updatePathLength() must have been done
	 * @throws indexOutOfBounds
	 *             rangeStart and rangeEnd
	 */
	public float getFirstPositionAtX(float x, Vector2 result,
			boolean calcPercentage, int rangeStart, int rangeEnd)
	{
		Lttl.Throw(rangeStart < 0 || rangeEnd >= pathArray.size());

		for (int i = rangeStart; i < rangeEnd + ((closed) ? 1 : 0); i++)
		{
			int npi = LttlMath.loopIndex(i + 1, pathArray.size());
			if (LttlMath.isBetween(x, pathArray.getX(i), pathArray.getX(npi)))
			{
				result.set(x, LttlMath.getSegmentY(pathArray.getX(i),
						pathArray.getY(i), pathArray.getX(npi),
						pathArray.getY(npi), x));
				if (calcPercentage) { return calcPercentageInternal(i, result); }
				return -1;
			}
		}
		result.set(Float.NaN, Float.NaN);
		return -1;
	}

	/**
	 * Finds the first position that has given y on the path.<br>
	 * If none found, then calcPercentage will return -1 and result will be populated with Float.NaN values.
	 * 
	 * @param y
	 * @param result
	 * @param calcPercentage
	 * @return optionally return the path percentage, updatePathLength() must have been done
	 */
	public float getFirstPositionAtY(float y, Vector2 result,
			boolean calcPercentage)
	{
		return getFirstPositionAtY(y, result, calcPercentage, 0,
				pathArray.size() - 1);
	}

	/**
	 * Finds the first position within the path points index range that has given y.<br>
	 * If none found, then calcPercentage will return -1 and result will be populated with Float.NaN values.
	 * 
	 * @param y
	 * @param result
	 * @param calcPercentage
	 * @param rangeStart
	 * @param rangeEnd
	 * @return optionally return the path percentage, updatePathLength() must have been done
	 * @throws indexOutOfBounds
	 *             rangeStart and rangeEnd
	 */
	public float getFirstPositionAtY(float y, Vector2 result,
			boolean calcPercentage, int rangeStart, int rangeEnd)
	{
		Lttl.Throw(rangeStart < 0 || rangeEnd >= pathArray.size());

		for (int i = rangeStart; i < rangeEnd + ((closed) ? 1 : 0); i++)
		{
			int npi = LttlMath.loopIndex(i + 1, pathArray.size());
			if (LttlMath.isBetween(y, pathArray.getY(i), pathArray.getY(npi)))
			{
				result.set(LttlMath.getSegmentX(pathArray.getX(i),
						pathArray.getY(i), pathArray.getX(npi),
						pathArray.getY(npi), y), y);
				if (calcPercentage) { return calcPercentageInternal(i, result); }
				return -1;
			}
		}
		result.set(Float.NaN, Float.NaN);
		return -1;
	}

	/**
	 * Finds all the positions that have the given y.<br>
	 * If none found, then calcPercentage array will be empty and results will be empty.
	 * 
	 * @param x
	 * @param result
	 * @param calcPercentage
	 * @return optionally return the path percentages for each position, updatePathLength() must have been done,
	 *         otherwise null
	 */
	public FloatArray getAllPositionsAtX(float x, Vector2Array result,
			boolean calcPercentage)
	{
		return getAllPositionsAtX(x, result, calcPercentage, 0,
				pathArray.size() - 1);
	}

	/**
	 * Finds all the positions within the path points index range that has given y.<br>
	 * If none found, then calcPercentage array will be empty and results will be empty
	 * 
	 * @param x
	 * @param result
	 * @param calcPercentage
	 * @param rangeStart
	 * @param rangeEnd
	 * @return optionally return the path percentages for each position, updatePathLength() must have been done,
	 *         otherwise null
	 * @throws indexOutOfBounds
	 *             rangeStart and rangeEnd
	 */
	public FloatArray getAllPositionsAtX(float x, Vector2Array result,
			boolean calcPercentage, int rangeStart, int rangeEnd)
	{
		Lttl.Throw(rangeStart < 0 || rangeEnd >= pathArray.size());

		FloatArray fa = null;
		if (calcPercentage)
		{
			fa = new FloatArray();
		}

		for (int i = rangeStart; i < rangeEnd + ((closed) ? 1 : 0); i++)
		{
			int npi = LttlMath.loopIndex(i + 1, pathArray.size());
			if (LttlMath.isBetween(x, pathArray.getX(i), pathArray.getX(npi)))
			{
				result.add(x, LttlMath.getSegmentY(pathArray.getX(i),
						pathArray.getY(i), pathArray.getX(npi),
						pathArray.getY(npi), x));
				if (calcPercentage)
				{
					fa.add(calcPercentageInternal(i, result.getLastX(),
							result.getLastY()));
				}
			}
		}
		return fa;
	}

	/**
	 * Finds all the positions that have the given y.<br>
	 * If none found, then calcPercentage array will be empty and results will be empty.
	 * 
	 * @param y
	 * @param result
	 * @param calcPercentage
	 * @return optionally return the path percentages for each position, updatePathLength() must have been done,
	 *         otherwise null
	 */
	public FloatArray getAllPositionsAtY(float y, Vector2Array result,
			boolean calcPercentage)
	{
		return getAllPositionsAtY(y, result, calcPercentage, 0,
				pathArray.size() - 1);
	}

	/**
	 * Finds all the positions within the path points index range that has given y.<br>
	 * If none found, then calcPercentage array will be empty and results will be empty
	 * 
	 * @param y
	 * @param result
	 * @param calcPercentage
	 * @param rangeStart
	 * @param rangeEnd
	 * @return optionally return the path percentages for each position, updatePathLength() must have been done,
	 *         otherwise null
	 * @throws indexOutOfBounds
	 *             rangeStart and rangeEnd
	 */
	public FloatArray getAllPositionsAtY(float y, Vector2Array result,
			boolean calcPercentage, int rangeStart, int rangeEnd)
	{
		Lttl.Throw(rangeStart < 0 || rangeEnd >= pathArray.size());

		FloatArray fa = null;
		if (calcPercentage)
		{
			fa = new FloatArray();
		}

		for (int i = rangeStart; i < rangeEnd + ((closed) ? 1 : 0); i++)
		{
			int npi = LttlMath.loopIndex(i + 1, pathArray.size());
			if (LttlMath.isBetween(y, pathArray.getY(i), pathArray.getY(npi)))
			{
				result.add(LttlMath.getSegmentX(pathArray.getX(i),
						pathArray.getY(i), pathArray.getX(npi),
						pathArray.getY(npi), y), y);
				if (calcPercentage)
				{
					fa.add(calcPercentageInternal(i, result.getLastX(),
							result.getLastY()));
				}
			}
		}
		return fa;
	}

	/**
	 * Checks if path is intersecting another path.
	 * 
	 * @param otherPath
	 * @param intersections
	 *            if not null will populate with intersections (slower)
	 * @param percentages
	 *            if intersections and percentages are not null, will populate percentages with the path interpolation
	 *            perncentages (slower)
	 * @return if it is intersecting the path, returns true on first intersection but only if intersection and
	 *         percentages are null
	 */
	public boolean intersectingPath(LttlPath otherPath,
			Vector2Array intersections, FloatArray percentages)
	{
		boolean intersecting = false;
		Vector2Array otherArray = otherPath.getPath();
		for (int i = 0, n = otherArray.size() - ((otherPath.closed) ? 0 : 1); i < n; i++)
		{
			if (intersectingSegment(otherArray.getX(i), otherArray.getY(i),
					otherArray.getX(i + 1), otherArray.getY(i + 1),
					intersections, percentages))
			{
				intersecting = true;
				if (intersections == null) { return true; }
			}
		}
		return intersecting;
	}

	/**
	 * Checks if path is intersecting a segment.
	 * 
	 * @param aX
	 * @param aY
	 * @param bX
	 * @param bY
	 * @param intersections
	 *            if not null will populate with intersections (slower)
	 * @param percentages
	 *            if intersections and percentages are not null, will populate percentages with the path interpolation
	 *            perncentages (slower)
	 * @return if it is intersecting a segment, returns true on first intersection, but only if intersection and
	 *         percentages are null
	 */
	public boolean intersectingSegment(float aX, float aY, float bX, float bY,
			Vector2Array intersections, FloatArray percentages)
	{
		boolean intersecting = false;
		for (int i = 0, n = pathArray.size() - ((closed) ? 0 : 1); i < n; i++)
		{
			if (Intersector.intersectSegments(pathArray.getX(i),
					pathArray.getY(i), pathArray.getX(i + 1),
					pathArray.getY(i + 1), aX, aY, bX, bY,
					(intersections == null) ? null : tmp0))
			{
				intersecting = true;
				if (intersections != null)
				{
					intersections.add(tmp0);
					if (percentages != null)
					{
						percentages.add(calcPercentageInternal(i, tmp0));
					}
				}
				else
				{
					return true;
				}
			}
		}
		return intersecting;
	}

	private void registerUndoState()
	{
		Lttl.editor.getUndoManager().registerUndoState(
				new UndoState("Modified path control points", new UndoField(
						this, undoList, LttlCopier.copy(controlPoints),
						new UndoSetter()
						{
							@Override
							public void set(LttlComponent comp, Object value)
							{
								editorUnregisterAllHandles();
								((LttlPath) comp).controlPoints.clear();
								((LttlPath) comp).controlPoints
										.addAll((ArrayList<LttlPathControlPoint>) value);
								selectedControlPoints.clear();
								editorUpdateComparisonList();
								editorInitHandles();
								editorUpdatePath();
							}
						})));
	}

	private ArrayList<LttlPathControlPoint> undoList;

	private void saveUndoObject()
	{
		undoList = LttlCopier.copy(controlPoints);
	}

	@GuiButton(order = 4)
	public void generateRect()
	{
		if (Lttl.game.inEditor())
		{
			saveUndoObject();

			if (isEditing())
			{
				editorUnregisterAllHandles();
				selectedControlPoints.clear();
			}
		}

		controlPoints.clear();

		float size = 5 * Lttl.game.getSettings().getWidthFactor();
		LttlPathControlPoint topLeft = new LttlPathControlPoint(-size, size);
		topLeft.type = LttlPathControlPointType.Sharp;
		LttlPathControlPoint topRight = new LttlPathControlPoint(size, size);
		topRight.type = LttlPathControlPointType.Sharp;
		LttlPathControlPoint bottomRight = new LttlPathControlPoint(size, -size);
		bottomRight.type = LttlPathControlPointType.Sharp;
		LttlPathControlPoint bottomLeft = new LttlPathControlPoint(-size, -size);
		bottomLeft.type = LttlPathControlPointType.Sharp;

		controlPoints.add(topLeft);
		controlPoints.add(topRight);
		controlPoints.add(bottomRight);
		controlPoints.add(bottomLeft);
		closed = true;

		registerUndoState();

		if (isEditing())
		{
			editorInitHandles();
			editorUpdateComparisonList();
		}
		editorUpdatePath();
	}

	@GuiButton(order = 5)
	public void generateTriangle()
	{
		if (Lttl.game.inEditor())
		{
			saveUndoObject();

			if (isEditing())
			{
				editorUnregisterAllHandles();
				selectedControlPoints.clear();
			}
		}

		controlPoints.clear();

		float size = 2 * Lttl.game.getSettings().getWidthFactor();
		LttlPathControlPoint left = new LttlPathControlPoint(-size, 0);
		left.type = LttlPathControlPointType.Sharp;
		LttlPathControlPoint right = new LttlPathControlPoint(size, 0);
		right.type = LttlPathControlPointType.Sharp;
		LttlPathControlPoint top = new LttlPathControlPoint(0, size);
		top.type = LttlPathControlPointType.Sharp;

		controlPoints.add(left);
		controlPoints.add(right);
		controlPoints.add(top);
		closed = true;

		registerUndoState();

		if (isEditing())
		{
			editorInitHandles();
			editorUpdateComparisonList();
		}
		editorUpdatePath();
	}

	@GuiButton(order = 6)
	public void generateCircle()
	{
		if (Lttl.game.inEditor())
		{
			saveUndoObject();

			if (isEditing())
			{
				editorUnregisterAllHandles();
				selectedControlPoints.clear();
			}
		}

		controlPoints.clear();

		float strength = 5.5f;
		float size = 10 * Lttl.game.getSettings().getWidthFactor();
		LttlPathControlPoint top = new LttlPathControlPoint(0, size);
		top.type = LttlPathControlPointType.Handles;
		top.handlesLocked = true;
		top.leftPos.set(-strength, 0);
		top.rightPos.set(strength, 0);
		LttlPathControlPoint right = new LttlPathControlPoint(size, 0);
		right.type = LttlPathControlPointType.Handles;
		right.handlesLocked = true;
		right.leftPos.set(0, strength);
		right.rightPos.set(0, -strength);
		LttlPathControlPoint bottom = new LttlPathControlPoint(0, -size);
		bottom.type = LttlPathControlPointType.Handles;
		bottom.handlesLocked = true;
		bottom.leftPos.set(strength, 0);
		bottom.rightPos.set(-strength, 0);
		LttlPathControlPoint left = new LttlPathControlPoint(-size, 0);
		left.type = LttlPathControlPointType.Handles;
		left.handlesLocked = true;
		left.leftPos.set(0, -strength);
		left.rightPos.set(0, strength);

		controlPoints.add(top);
		controlPoints.add(right);
		controlPoints.add(bottom);
		controlPoints.add(left);
		closed = true;

		useBezier = true;

		if (Lttl.game.inEditor())
		{
			registerUndoState();

			if (isEditing())
			{
				editorInitHandles();
				editorUpdateComparisonList();
			}
		}
		editorUpdatePath();
	}

	@Override
	public void onCallback(String animName, String seqName, String value)
	{
	}

	@Override
	public void onStep(String animName, float iterationPosition)
	{
		if (Lttl.game.inEditor() && isEditing())
		{
			editorUpdateAllHandles();
			editorUpdatePath();
		}
		else
		{
			modified();
			if (updatePathOnStep)
			{
				updatePathIfNecessary();
			}
		}
	}

	/**
	 * sets updatedSinceModified to false and notifies all listening components (default for custom shape mesh
	 * generators).<br>
	 * This should be called whenever the path is modified and you want the other components to know about it.
	 */
	public void modified()
	{
		updatedSinceModified = false;
		updatedLengthSinceModified = false;
		for (LttlModifiedListener listener : listeningComponents)
		{
			listener.onModified(this);
		}
	}

	/**
	 * Will return if the path has ran updatePath() since the most recent modified(). Can use
	 * {@link #updatePathIfNecessary()} to auto check.
	 * 
	 * @return
	 */
	public boolean isUpdatedSinceModified()
	{
		return updatedSinceModified;
	}

	/**
	 * Will return if the path length has been updated since the most recent modified(). Can use
	 * {@link #updateLengthIfNecessary()} to auto check.
	 * 
	 * @return
	 */
	public boolean isUpdatedLengthSinceModified()
	{
		return updatedLengthSinceModified;
	}

	/**
	 * This is an array that holds all the path indexs of each control point based on the last path update using default
	 * path values.<br>
	 * Can be null if never ran udpatePath();<br>
	 * <b>DO NOT MODIFY THIS.</b>
	 * 
	 * @return
	 */
	public IntArray getControlPointPathIndexArray()
	{
		return controlPointPathIndexArray;
	}

	/**
	 * Gets the number of path points after this control point and before the next. This does not include itself.
	 * 
	 * @return
	 */
	public int getNumberOfPointsInControlPoint(int controlPointIndex)
	{
		// if last control point (works for closed and open paths)
		if (controlPointIndex == controlPointPathIndexArray.size - 1)
		{
			// the last path index - the control point's path index
			return (pathArray.size() - 1)
					- controlPointPathIndexArray.get(controlPointIndex);
		}
		else
		{
			// the next control point's path index - 1, which is the last path index inside the desired control
			// point - the control point's path index
			return (controlPointPathIndexArray.get(controlPointIndex + 1) - 1)
					- controlPointPathIndexArray.get(controlPointIndex);
		}
	}

	/**
	 * Usually the path has not been updated yet, so all listeners should run {@link #updatePathIfNecessary()} if they
	 * want the updated path or if using custom settings {@link #generatePath(int, float, Vector2Array, IntArray)}.<br>
	 * Will check for duplicate listeners.
	 * 
	 * @param listener
	 */
	public void addModifiedListener(LttlModifiedListener listener)
	{
		if (!listeningComponents.contains(listener))
		{
			listeningComponents.add(listener);
		}
	}

	public void removeModifiedListener(LttlModifiedListener listener)
	{
		listeningComponents.remove(listener);
	}

	private boolean shouldHaveHandles(LttlPathControlPoint cp)
	{
		return cp.type == LttlPathControlPointType.Handles
				|| (cp.type == LttlPathControlPointType.Proximity && useBezier);
	}
}
