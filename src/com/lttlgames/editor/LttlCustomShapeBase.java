package com.lttlgames.editor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.IntArray;
import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.GuiGroup;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.graphics.LttlAntiAliaser;
import com.lttlgames.graphics.LttlMesh;
import com.lttlgames.graphics.LttlMeshFactory;
import com.lttlgames.helpers.LttlGeometryUtil.PolygonContainer;
import com.lttlgames.helpers.LttlHelper;
import com.lttlgames.helpers.LttlMath;
import com.lttlgames.helpers.Vector2Array;

//06
@Persist(-9085)
abstract public class LttlCustomShapeBase extends LttlMeshGenerator implements
		LttlModifiedListener
{
	protected boolean pathWarning = false;

	// NOTE NO custom tSamples because it's a waste of time and not efficient, instead just make a copy of LttlPath (in
	// runtime or editor) and modify it or use LttlPath.generatePath();

	/**
	 * forces users to use {@link #setPath(LttlPath)} because it manages listeners.
	 */
	@Persist(908500)
	@GuiCallback("guiChangePath")
	protected LttlPath path;

	@Persist(908501)
	@GuiGroup("Settings")
	public boolean autoUpdateOnPathModified = true;

	/**
	 * updates path and length on path modified
	 */
	@Persist(908502)
	@GuiGroup("Editor Settings")
	public boolean editorAutoUpdateOnPathModified = true;

	/**
	 * If there is a texture0 then use the atlas region bounds scaled to the path UVs
	 */
	@Persist(908503)
	@GuiGroup("Settings")
	@GuiCallback("onGuiUpdateMesh")
	public boolean useTextureUVs = true;

	/**
	 * Use the custom UVs in each control point's extras Experimental, only kind of works with sharp control points, but
	 * needs more sub divisions
	 */
	@Persist(908504)
	@GuiGroup("Settings")
	@GuiCallback("onGuiUpdateMesh")
	public boolean useCustomUVs = false;

	/**
	 * Use the custom alpha and colors on each control point's extra object, if this is true, will always set the alpha
	 * and color on the vertices, so don't need autoUpdateColorAlpha
	 */
	@Persist(908505)
	@GuiGroup("Settings")
	@GuiCallback("onGuiUpdateMesh")
	public boolean useCustomAlphaAndColors = false;
	/**
	 * Delaunay Triangulation allows for holes, more uniform triangles. If false, will still use Delaunay if there are
	 * holes, though.
	 */
	@GuiGroup("Settings")
	@GuiCallback("onGuiUpdateMesh")
	@Persist(908506)
	public boolean useDelaunay = false;

	/* GUI */
	private int prevPathId = -1;

	@Override
	public void onStart()
	{
		super.onStart();
		initModifiedListeners();
	}

	@Override
	public void onEnable()
	{
		super.onEnable();
		initModifiedListeners();
	}

	@Override
	public void onEditorEnable()
	{
		super.onEditorEnable();
		initModifiedListeners();
	}

	@Override
	public void onEditorStart()
	{
		super.onStart();
		initModifiedListeners();
	}

	/**
	 * ran onStart to initialize the listeners
	 */
	protected void initModifiedListeners()
	{
		if (path != null)
		{
			path.addModifiedListener(this);
		}
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

	protected void processOnDestroyComp()
	{
		// cleanup remove as a listener
		if (path != null)
		{
			path.removeModifiedListener(this);
		}
	}

	@Override
	public void onEditorCreate()
	{
		// see if you can find a LttlPath component on this transform
		setPath(t().getComponent(LttlPath.class, true));
	}

	@SuppressWarnings("unused")
	private void guiChangePath()
	{
		// if path exists
		if (path != null)
		{
			// but it didn't last frame
			if (prevPathId == -1)
			{
				// add to component list
				path.addModifiedListener(this);
			}
			// a different one existed last frame
			else if (prevPathId != path.getId())
			{
				// remove from previous path
				LttlPath oldPath = (LttlPath) Lttl.scenes
						.findComponentByIdAllScenes(prevPathId);
				if (oldPath != null)
				{
					oldPath.removeModifiedListener(this);
				}
				// add to new path
				path.addModifiedListener(this);
			}
			prevPathId = path.getId();
		}
		// path does not exist
		else
		{
			// it did exist last frame
			if (prevPathId != -1)
			{
				// remove from previous path
				LttlPath oldPath = (LttlPath) Lttl.scenes
						.findComponentByIdAllScenes(prevPathId);
				if (oldPath != null)
				{
					oldPath.removeModifiedListener(this);
				}
			}
			prevPathId = -1;
		}
		updateMesh();
	}

	@Override
	public void onModified(LttlComponent source)
	{
		// path has modified
		// check if should autoUpdate
		if (!isEnabled() || !isAutoUpdating()) return;
		updateMesh();
	}

	@Override
	protected void onGuiUpdateMesh()
	{
		updateMesh();
	}

	public LttlPath getPath()
	{
		return path;
	}

	/**
	 * sets the path and updates {@link LttlModifiedListener}
	 * 
	 * @param path
	 */
	public void setPath(LttlPath path)
	{
		// remove from old path
		if (this.path != null)
		{
			this.path.removeModifiedListener(this);
		}

		// add to new path
		if (path != null)
		{
			path.addModifiedListener(this);
		}

		// save path
		this.path = path;
	}

	boolean isAutoUpdating()
	{
		return (Lttl.game.isPlaying() && autoUpdateOnPathModified)
				|| (!Lttl.game.isPlaying() && editorAutoUpdateOnPathModified);
	}

	/**
	 * Modifys UV (customUVS, textureUVs, uv transformations), adds AA if any, creates and sets mesh.
	 * 
	 * @param mesh
	 *            nonNull
	 * @param pathPoints
	 *            only required if meshParts is null
	 * @param alreadyGenerated
	 *            already generated mesh vertices and indices Used if you want to modify the vertices and indices
	 *            before.
	 */
	public void finalizeMesh(LttlMesh mesh, Vector2Array pathPoints,
			boolean alreadyGenerated)
	{
		finalizeMesh(mesh, pathPoints == null ? null : new PolygonContainer(
				pathPoints), alreadyGenerated);
	}

	protected void finalizeMesh(LttlMesh mesh, PolygonContainer polyCont,
			boolean alreadyGenerated)
	{
		// should not be null, should have been generated before this
		Lttl.Throw(mesh);

		// modify the UVs first?
		if (alreadyGenerated || useCustomUVs || useCustomAlphaAndColors
				|| uvMeshSettings != null
				|| (useTextureUVs && r().getTex0().getAR() != null))
		{
			// get the mesh parts so we can modify them
			// SKIP AA, will add after modify UVs
			if (!alreadyGenerated)
			{
				LttlMeshFactory.GeneratePolygon(mesh, polyCont, useDelaunay);
			}

			// define the UVs based on the static UV size
			if (uvMeshSettings != null && uvMeshSettings.staticUvSize)
			{
				float width = uvMeshSettings.maxDim.x - uvMeshSettings.minDim.x;
				float height = uvMeshSettings.maxDim.y
						- uvMeshSettings.minDim.y;
				for (int i = 0, n = mesh.getVertexCount(); i < n; i++)
				{
					mesh.setUV(i, (mesh.getX(i) - uvMeshSettings.minDim.x)
							/ width, (mesh.getY(i) - uvMeshSettings.minDim.y)
							/ height);
				}
			}

			if (useCustomAlphaAndColors)
			{
				IntArray controlPointsPathIndexArray = path
						.getControlPointPathIndexArray();

				if (r().autoUpdateMeshColorAlpha)
				{
					Lttl.logNote("Mesh Generator: Using custom vertex colors and/or alphas with autoUpdateMeshColorAlpha enabled.");
				}

				// alpha and color extras
				for (int i = 1; i <= controlPointsPathIndexArray.size; i++)
				{
					// get start and end control points
					int aCpIndex = i - 1;
					LttlPathControlPoint aCP = path.controlPoints.get(aCpIndex);
					int bCpIndex = (i == controlPointsPathIndexArray.size) ? 0
							: i;
					LttlPathControlPoint bCP = path.controlPoints.get(bCpIndex);

					Color colorA = aCP.extra != null && aCP.extra.color != null ? aCP.extra.color
							: r().getColor();
					Color colorB = bCP.extra != null && bCP.extra.color != null ? bCP.extra.color
							: r().getColor();
					float alphaA = aCP.extra != null ? aCP.extra.alpha : 1;
					float alphaB = bCP.extra != null ? bCP.extra.alpha : 1;
					float worldAlpha = r().getWorldAlpha(false);
					// iterate through the vertices that are between the two control points (inclusive)
					// if on the last control point, which is really the first that connects to the last, then go
					// through all the rest of the vertices, not to the normal vertice index since it would be the
					// first
					int j = controlPointsPathIndexArray.get(aCpIndex);
					int n = (bCpIndex == 0) ? mesh.getVertexCount() - 1
							: controlPointsPathIndexArray.get(bCpIndex);
					float startLength = path.getPathLength(j);
					float endLength = path.getPathLength(n);
					if (j != n)
					{
						for (; j <= n; j++)
						{
							float perc = 1 - ((endLength - path
									.getPathLength(j)) / (endLength - startLength));

							// set alpha and color
							mesh.setAlpha(j,
									LttlMath.Lerp(alphaA, alphaB, perc)
											* worldAlpha);
							mesh.setColor(j,
									colorA == colorB ? colorA.toFloatBits()
											: LttlHelper.tmpColor.set(colorA)
													.lerp(colorB, perc)
													.toFloatBits());
						}
					}
				}
			}

			// modifications to vertices' UV
			if (useCustomUVs
					|| (useTextureUVs && r().getTex0().getAR() != null))
			{
				IntArray controlPointsPathIndexArray = path
						.getControlPointPathIndexArray();
				// implement custom UVs, data is stored in control points extra UV
				if (useCustomUVs)
				{
					// all paths can safely be assumed they are close
					for (int i = 1; i <= controlPointsPathIndexArray.size; i++)
					{
						// get start and end control points
						int aCpIndex = i - 1;
						LttlPathControlPoint aCP = path.controlPoints
								.get(aCpIndex);
						int bCpIndex = (i == controlPointsPathIndexArray.size) ? 0
								: i;
						LttlPathControlPoint bCP = path.controlPoints
								.get(bCpIndex);

						// if both have no custom UVs, skip
						if (aCP.extra == null && bCP.extra == null)
						{
							continue;
						}

						// get start and end values of UVs and positions
						float aX = aCP.pos.x;
						float aY = aCP.pos.y;
						float aUVx = (aCP.extra != null) ? aCP.extra.uv.x
								: mesh.getU(controlPointsPathIndexArray
										.get(aCpIndex));
						float aUVy = (aCP.extra != null) ? aCP.extra.uv.y
								: mesh.getV(controlPointsPathIndexArray
										.get(aCpIndex));
						float bX = bCP.pos.x;
						float bY = bCP.pos.y;
						float bUVx = (bCP.extra != null) ? bCP.extra.uv.x
								: mesh.getU(controlPointsPathIndexArray
										.get(bCpIndex));
						float bUVy = (bCP.extra != null) ? bCP.extra.uv.y
								: mesh.getV(controlPointsPathIndexArray
										.get(bCpIndex));

						// iterate through the vertices that are between the two control points (inclusive) and modify
						// them to the custom UVs
						// if on the last control point, which is really the first that connects to the last, then go
						// through all the rest of the vertices, not to the normal vertice index since it would be the
						// first
						for (int j = controlPointsPathIndexArray.get(aCpIndex), n = (bCpIndex == 0) ? mesh
								.getVertexCount() - 1
								: controlPointsPathIndexArray.get(bCpIndex); j <= n; j++)
						{
							// get position of vertex
							float x = mesh.getX(j);
							float y = mesh.getY(j);

							// calculate the interpolation percent based on position of vertex
							float pX = (x - aX) / (bX - aX);
							float pY = (y - aY) / (bY - aY);

							// just use the other inerpolation percent if the start and end x's are the same
							if (bX == aX)
							{
								pX = pY;
							}
							if (bY == aY)
							{
								pY = pX;
							}

							// set new UVs
							mesh.setUV(j, LttlMath.Lerp(aUVx, bUVx, pX),
									LttlMath.Lerp(aUVy, bUVy, pY));
						}
					}
				}

				// modify the UVs to be for the texture's atlas region
				if (useTextureUVs && r().getTex0().getAR() != null)
				{
					float aUVx = r().getTex0().getAR().getU();
					float aUVy = r().getTex0().getAR().getV();
					float bUVx = r().getTex0().getAR().getU2();
					float bUVy = r().getTex0().getAR().getV2();
					for (int i = 0, n = mesh.getVertexCount(); i < n; i++)
					{
						// set the new vertex UVs by using the original as the interpolation value between the start and
						// end UVs of the atlas region
						float pX = mesh.getU(i);
						float pY = mesh.getV(i);
						mesh.setUV(i, LttlMath.Lerp(aUVx, bUVx, pX),
								LttlMath.Lerp(aUVy, bUVy, pY));

					}
				}
			}

			// apply UV transformations (really just used for gradient shaders as of now)
			if (uvMeshSettings != null)
			{
				mesh.transformUVs(uvMeshSettings);
			}
		}
		else
		{
			mesh = LttlMeshFactory.GeneratePolygon(mesh, polyCont, useDelaunay);
		}
		r().setMesh(mesh);
		updateMeshAA();
	}

	@Override
	final public void updateMeshAA(float calculatedAA)
	{
		if (r().getMesh() == null) return;

		if (calculatedAA > 0)
		{
			LttlAntiAliaser.AddAntiAliasingEither(r().getMesh(), calculatedAA,
					aaSettings);
			r().setMesh(r().getMesh());
		}
	}

	/**
	 * checks if path is not closed or has less than 3 points
	 * 
	 * @param p
	 * @return
	 */
	boolean isPathValid(LttlPath p)
	{
		if (p == null || !p.closed)
		{
			if (p != null && (!p.closed || p.controlPoints.size() < 3)
					&& !pathWarning)
			{
				Lttl.logNote("Path or Cutout Path on "
						+ toString()
						+ " must be closed and have more than 3 points to create mesh.");
				r().setMesh(null);
				pathWarning = true;
			}
			return false;
		}
		return true;
	}
}
