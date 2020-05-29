package com.lttlgames.editor;

import java.util.Iterator;

import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.lttlgames.editor.annotations.GuiButton;
import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.GuiGroup;
import com.lttlgames.editor.annotations.GuiShow;
import com.lttlgames.editor.annotations.GuiReadOnly;
import com.lttlgames.editor.annotations.IgnoreCrawl;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.helpers.LttlMath;

/**
 * Properties that can't be edited by individual MultiObjects: meshScale.
 */
@Persist(-9094)
public abstract class LttlMultiObjectRenderer<T extends MultiObject> extends
		LttlMultiRenderer
{
	/**
	 * Called in subClass constructpr. Needs to set the MutliRenderer object class here when extending
	 */
	protected LttlMultiObjectRenderer(Class<T> clazz)
	{
		this.multiObjectClass = clazz;
	}

	/**
	 * if less than 0, will be MaxValue
	 */
	@Persist(909400)
	@GuiGroup("Settings")
	@GuiCallback("updatePoolSize")
	int maxPoolSize = 100;
	/**
	 * TODO Experimental, not sure if helps.
	 */
	@Persist(909401)
	@GuiGroup("Settings")
	boolean autoFillPool = false;
	@GuiShow
	@GuiReadOnly
	@GuiGroup("Stats")
	private int count = 0;
	@GuiShow
	@GuiReadOnly
	@GuiGroup("Stats")
	private int countPeak = 0;
	@GuiShow
	@GuiReadOnly
	@GuiGroup("Stats")
	private int poolPeak = 0;

	private Class<T> multiObjectClass;

	/**
	 * These hold the non pooled objects. unordered
	 */
	// TODO can probably remove this annotation
	@IgnoreCrawl
	private Array<T> activePooled;
	// TODO can probably remove this annotation
	@IgnoreCrawl
	private Pool<T> pool;

	final public Array<T> getActivePooled()
	{
		if (activePooled == null)
		{
			activePooled = new Array<T>(false, maxPoolSize, multiObjectClass);
		}
		return activePooled;
	}

	private T newObject()
	{
		T mo = null;
		mo = LttlObjectGraphCrawler.newInstance(multiObjectClass);
		mo.reset();
		onNewObject(mo);
		return mo;
	}

	final public void updatePoolSize()
	{
		getActivePooled().ensureCapacity(maxPoolSize - getActivePooled().size);
		pool = new Pool<T>(0, maxPoolSize < 0 ? Integer.MAX_VALUE : maxPoolSize)
		{
			@Override
			protected T newObject()
			{
				return LttlMultiObjectRenderer.this.newObject();
			}
		};
		// TODO not sure if works
		if (autoFillPool)
		{
			for (int i = 0; i < maxPoolSize; i++)
			{
				pool.free(this.newObject());
			}
		}
	}

	final public Pool<T> getPool()
	{
		if (pool == null)
		{
			updatePoolSize();
		}
		return pool;
	}

	/**
	 * check if in editor before calling this
	 */
	protected void updateGui()
	{
		poolPeak = getPool().peak;
		count = getActivePooled().size;
		countPeak = LttlMath.max(count, countPeak);
	}

	@GuiButton
	private void resetPeaks()
	{
		getPool().peak = 0;
		countPeak = 0;
	}

	/**
	 * Obtains an object from pool and adds it to activePooled array and runs
	 * {@link LttlMultiObjectRenderer#onNewObject(MultiObject)}.<br>
	 * Object will always have reset ran.
	 * 
	 * @return
	 */
	final public T obtain()
	{
		T mo = getPool().obtain();
		getActivePooled().add(mo);
		onNewObject(mo);
		return mo;
	}

	/**
	 * Frees an object and adds it to pool (resets it) and removes from activePooled array.
	 * 
	 * @param obj
	 * @param it
	 *            (optional) activePooled iterator, can be left null if not iterating, allows removing while iterating
	 */
	final public void free(T obj, Iterator<T> it)
	{
		if (it != null)
		{
			it.remove();
		}
		else
		{
			getActivePooled().removeValue(obj, true);
		}
		pool.free(obj);
	}

	/**
	 * Sets if the backing multiObject active array is ordered or not, if it is, it requires a memory copy. Default is
	 * not ord.ered
	 * 
	 * @param ordered
	 */
	public void setOrdered(boolean ordered)
	{
		getActivePooled().ordered = ordered;
	}

	/**
	 * Calculates the multiObject's local matrix and renders it with the given worldMatrix
	 * 
	 * @param multiObject
	 * @param worldMatrix
	 *            the already calculated world matrix
	 * @param tmpM4
	 * @param isGlobal
	 */
	final public void renderDraw(T multiObject, Matrix4 worldMatrix,
			Matrix4 tmpM4, boolean isGlobal)
	{
		// check alpha before creating renderMatrix
		if (multiObject.alpha <= 0) return;
		renderDraw(getRenderMatrix(multiObject.position.x,
				multiObject.position.y, multiObject.scale.x,
				multiObject.scale.y, multiObject.origin.x,
				multiObject.origin.y, multiObject.rotation,
				multiObject.shear.x, multiObject.shear.y, worldMatrix, tmpM4,
				isGlobal));
	}

	/**
	 * Returns the render matrix, this can be sent straight to graphics card. Generates the world and local matrix.
	 * 
	 * @param multiObject
	 * @param tmpM4
	 * @param isGlobal
	 * @return
	 */
	final public Matrix4 getRenderMatrix(T multiObject, Matrix4 tmpM4,
			boolean isGlobal)
	{
		return getRenderMatrix(multiObject.position.x, multiObject.position.y,
				multiObject.scale.x, multiObject.scale.y, multiObject.origin.x,
				multiObject.origin.y, multiObject.rotation,
				multiObject.shear.x, multiObject.shear.y, tmpM4, isGlobal);
	}

	/**
	 * Returns the render matrix, this can be sent straight to graphics card. Generates the world and local matrix.
	 * 
	 * @param multiObject
	 * @param worldMatrix
	 * @param tmpM4
	 * @param isGlobal
	 * @return
	 */
	final public Matrix4 getRenderMatrix(T multiObject, Matrix4 worldMatrix,
			Matrix4 tmpM4, boolean isGlobal)
	{
		return getRenderMatrix(multiObject.position.x, multiObject.position.y,
				multiObject.scale.x, multiObject.scale.y, multiObject.origin.x,
				multiObject.origin.y, multiObject.rotation,
				multiObject.shear.x, multiObject.shear.y, worldMatrix, tmpM4,
				isGlobal);
	}

	/**
	 * Returns the local transform matrix for this MultiObject, takes into consideration if isGlobal
	 * 
	 * @param multiObject
	 * @return
	 */
	final protected Matrix3 getLocalTransformMatrix(T multiObject,
			Matrix3 output, boolean isGlobal)
	{
		// just create local transform
		return LttlMath.GenerateTransormMatrix(multiObject.position.x,
				multiObject.position.y, multiObject.scale.x,
				multiObject.scale.y, multiObject.origin.x,
				multiObject.origin.y, multiObject.rotation,
				useLookupForRotation, multiObject.shear.x, multiObject.shear.y,
				(isGlobal) ? null : transform(), output);
	}

	final protected float getWorldAlpha(T multiObject)
	{
		// don't need to check world alpha because this would only be running really in rendering loop
		return r().getWorldAlpha(false) * multiObject.alpha;
	}

	/**
	 * This is called whenever a new MutliObject is obtained and added to the activeArray and after it's reset() is ran.<br>
	 * Meant to be overridden where you can set initial values.
	 * 
	 * @param newObj
	 */
	protected void onNewObject(T newObj)
	{
	}

	/**
	 * This is called right before a MutliObject renders. This is a good place to make changes to the renderer.
	 * 
	 * @param obj
	 */
	protected void prepareRender(T obj)
	{
	}

}
