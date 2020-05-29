package com.lttlgames.helpers;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;
import com.lttlgames.editor.Lttl;
import com.lttlgames.editor.annotations.GuiHide;
import com.lttlgames.editor.annotations.Persist;

/**
 * An efficient ordered array to hold Vector2 objects as float primatives (no pointers).<br>
 * It is not more efficient to call getX and getY together as a get() vector2, since it still needs to call them
 * seperately.
 * 
 * @author Josh
 */
@Persist(-9098)
@GuiHide
public class Vector2Array
{
	@Persist(909800)
	@GuiHide
	FloatArray fArray;
	private static final Vector2 sharedVector = new Vector2();
	private static final Vector2 tmp = new Vector2();

	/**
	 * Creates an array with a capacity of 16.
	 */
	public Vector2Array()
	{
		fArray = new FloatArray();
	}

	public Vector2Array(int capacity)
	{
		fArray = new FloatArray(capacity * 2);
	}

	/**
	 * Makes a copy of the source Vector2Array
	 * 
	 * @param source
	 */
	public Vector2Array(Vector2Array source)
	{
		fArray = new FloatArray(source.fArray);
	}

	/**
	 * Create a Vector2Array based on a float array (needs to be even)
	 * 
	 * @param values
	 */
	public Vector2Array(float... values)
	{
		// check if even
		Lttl.Throw(values.length % 2 != 0);
		fArray = new FloatArray(values);
	}

	/**
	 * Create a Vector2Array based on a vector2 array
	 * 
	 * @param values
	 */
	public Vector2Array(Vector2... values)
	{
		fArray = new FloatArray(values.length * 2);
		for (Vector2 v2 : values)
		{
			fArray.addAll(v2.x, v2.y);
		}
	}

	public Vector2Array(ArrayList<Vector2> values)
	{
		fArray = new FloatArray(values.size() * 2);
		addAll(values);
	}

	public Vector2Array add(Vector2 v)
	{
		fArray.addAll(v.x, v.y);

		return this;
	}

	public Vector2Array add(float x, float y)
	{
		fArray.addAll(x, y);

		return this;
	}

	public Vector2Array remove(int index)
	{
		indexCheck(index);
		fArray.removeRange(index * 2, index * 2 + 1);

		return this;
	}

	public Vector2Array removeLast()
	{
		if (size() == 0) return this;
		remove(size() - 1);

		return this;
	}

	public Vector2Array insert(int index, float x, float y)
	{
		if (index > size() || index < 0)
			throw new IndexOutOfBoundsException("index can't be > size: "
					+ index + " >= " + size());
		index *= 2;
		fArray.insert(index, y);
		fArray.insert(index, x);

		return this;
	}

	public Vector2Array insert(int index, Vector2 v)
	{
		insert(index, v.x, v.y);

		return this;
	}

	public Vector2Array set(int index, Vector2 v)
	{
		set(index, v.x, v.y);

		return this;
	}

	public Vector2Array set(int index, float x, float y)
	{
		indexCheck(index);
		index *= 2;
		fArray.set(index, x);
		fArray.set(index + 1, y);

		return this;
	}

	public Vector2Array setX(int index, float x)
	{
		indexCheck(index);
		index *= 2;
		fArray.set(index, x);

		return this;
	}

	public Vector2Array setY(int index, float y)
	{
		indexCheck(index);
		index *= 2;
		fArray.set(index + 1, y);

		return this;
	}

	/**
	 * Ensure's backing array is resized only once.
	 * 
	 * @param vs
	 */
	public Vector2Array addAll(ArrayList<Vector2> vs)
	{
		fArray.ensureCapacity(vs.size() * 2);
		for (Vector2 v : vs)
		{
			add(v);
		}

		return this;
	}

	/**
	 * Ensure's backing array is resized only once.
	 * 
	 * @param vs
	 */
	public Vector2Array addAll(Vector2... vs)
	{
		fArray.ensureCapacity(vs.length * 2);
		for (Vector2 v : vs)
		{
			add(v);
		}

		return this;
	}

	/**
	 * Ensure's backing array is resized only once.
	 * 
	 * @param vs
	 */
	public Vector2Array addAll(float... vs)
	{
		if (vs.length % 2 == 1)
		{
			Lttl.Throw("Needs to be an even number of floats.");
		}
		fArray.addAll(vs);

		return this;
	}

	public Vector2Array addAll(Vector2Array source)
	{
		fArray.addAll(source.fArray.items, 0, source.fArray.size);

		return this;
	}

	public Vector2Array addAll(Vector2Array source, int start, int end)
	{
		fArray.addAll(source.fArray.items, start * 2, (end - start + 1) * 2);

		return this;
	}

	/**
	 * Clears and adds all from source.
	 */
	public Vector2Array set(Vector2Array source)
	{
		this.clear();
		this.addAll(source);
		return this;
	}

	public float getX(int index)
	{
		indexCheck(index);
		return fArray.get(index * 2);
	}

	public float getY(int index)
	{
		indexCheck(index);
		return fArray.get(index * 2 + 1);
	}

	public float getLastX()
	{
		return getX(size() - 1);
	}

	public float getLastY()
	{
		return getY(size() - 1);
	}

	public float getFirstX()
	{
		return getX(0);
	}

	public float getFirstY()
	{
		return getY(0);
	}

	public Vector2 getLast(Vector2 result)
	{
		return get(size() - 1, result);
	}

	public Vector2 getFirst(Vector2 result)
	{
		return get(0, result);
	}

	/**
	 * Sets the result to the vector specified. Note: This is not any more efficient than calling getX() and getY()
	 * individually.
	 * 
	 * @param index
	 * @param result
	 * @return
	 */
	public Vector2 get(int index, Vector2 result)
	{
		return result.set(getX(index), getY(index));
	}

	/**
	 * Returns a shared Vector2 containing the values at index. Note: This is not any more efficient than calling getX()
	 * and getY() individually.<br>
	 * <b>MAKE SURE IT'S USED RIGHT AWAY! Shared with all other getShared() calls. Should be extracting the x and y
	 * values right away or setting it to another Vector2.</b>
	 * 
	 * @param index
	 * @return
	 */
	public Vector2 getShared(int index)
	{
		return get(index, sharedVector);
	}

	/**
	 * Returns a shared Vector2 containing the values at index. Note: This is not any more efficient than calling getX()
	 * and getY() individually.<br>
	 * <b>MAKE SURE IT'S USED RIGHT AWAY! Shared with all other getShared() calls. Should be extracting the x and y
	 * values right away or setting it to another Vector2.</b>
	 * 
	 * @param index
	 * @return
	 */
	public Vector2 getSharedFirst()
	{
		return getFirst(sharedVector);
	}

	/**
	 * Returns a shared Vector2 containing the values at index. Note: This is not any more efficient than calling getX()
	 * and getY() individually.<br>
	 * <b>MAKE SURE IT'S USED RIGHT AWAY! Shared with all other getShared() calls. Should be extracting the x and y
	 * values right away or setting it to another Vector2.</b>
	 * 
	 * @param index
	 * @return
	 */
	public Vector2 getSharedLast()
	{
		return getLast(sharedVector);
	}

	public int size()
	{
		return fArray.size / 2;
	}

	private void indexCheck(int index)
	{
		if (index >= size() || index < 0)
			if (index >= size())
			{
				throw new IndexOutOfBoundsException("index can't be >= size: "
						+ index + " >= " + size());
			}
			else
			{
				throw new IndexOutOfBoundsException("index can't be < 0: "
						+ index);
			}
	}

	/**
	 * Increases the size of the backing array to accommodate the specified number of additional items. Useful before
	 * adding many items to avoid multiple backing array resizes. <br>
	 * Auto called when doing an addAll()
	 * 
	 * @param additionalCapacity
	 */
	public void ensureCapacity(int additionalCapacity)
	{
		fArray.ensureCapacity(additionalCapacity * 2);
	}

	public Vector2Array clear()
	{
		fArray.clear();

		return this;
	}

	public float[] toArray()
	{
		return fArray.toArray();
	}

	public float[] toArray(int start, int end)
	{
		float[] arr = new float[(end - start + 1) * 2];
		System.arraycopy(fArray.items, start * 2, arr, 0, arr.length);
		return arr;
	}

	/**
	 * Returns the array of the values by using the given array to put the values in.<br>
	 * The size needs to be same size as the expected size of the float array (2*size)
	 * 
	 * @param array
	 * @return
	 */
	public float[] toArray(float[] array)
	{
		Lttl.Throw(array);
		// check if same size
		Lttl.Throw(array.length != this.fArray.size);

		System.arraycopy(this.fArray.items, 0, array, 0, this.fArray.size);

		return array;
	}

	/**
	 * Converts the array to an arrayList of Vector2 objects, good for printing in dump.
	 * 
	 * @return
	 */
	public ArrayList<Vector2> toArrayList()
	{
		ArrayList<Vector2> list = new ArrayList<Vector2>();
		for (int i = 0, s = size(); i < s; i++)
		{
			list.add(get(i, new Vector2()));
		}
		return list;
	}

	/* Mathematics */
	public Vector2Array scl(int index, float x, float y)
	{
		return set(index, getX(index) * x, getY(index) * y);
	}

	public Vector2Array mul(int i, Matrix3 mat)
	{
		get(i, tmp);
		tmp.mul(mat);
		set(i, tmp);

		return this;
	}

	public Vector2Array div(int index, float x, float y)
	{
		return set(index, getX(index) / x, getY(index) / y);
	}

	public Vector2Array sclAll(float v)
	{
		return sclAll(v, v);
	}

	public Vector2Array mulAll(Matrix3 mat)
	{
		for (int i = 0, n = size(); i < n; i++)
		{
			mul(i, mat);
		}
		return this;
	}

	public Vector2Array divAll(float v)
	{
		return divAll(v, v);
	}

	public Vector2Array sclAll(float x, float y)
	{
		for (int i = 0; i < size(); i++)
		{
			scl(i, x, y);
		}

		return this;
	}

	public Vector2Array divAll(float x, float y)
	{
		for (int i = 0; i < size(); i++)
		{
			div(i, x, y);
		}

		return this;
	}

	/**
	 * Offsets the vector2 at the index
	 * 
	 * @param index
	 * @param x
	 * @param y
	 */
	public Vector2Array offset(int index, float x, float y)
	{
		return set(index, getX(index) + x, getY(index) + y);
	}

	/**
	 * Offsets the vector2 at the index
	 * 
	 * @param index
	 * @param offset
	 */
	public Vector2Array offset(int index, Vector2 offset)
	{
		return offset(index, offset.x, offset.y);
	}

	/**
	 * Offsets all the Vector2 with offset
	 * 
	 * @param x
	 * @param y
	 */
	public Vector2Array offsetAll(float x, float y)
	{
		for (int i = 0; i < size(); i++)
		{
			offset(i, x, y);
		}
		return this;
	}

	/**
	 * Offsets all the Vector2s with offset
	 * 
	 * @param offset
	 */
	public Vector2Array offsetAll(Vector2 offset)
	{
		return offsetAll(offset.x, offset.y);
	}

	/**
	 * This helps when something is not CCW.
	 * 
	 * @return
	 */
	public Vector2Array reverse()
	{
		for (int i = 0, lastIndex = size() - 1, n = size() / 2; i < n; i++)
		{
			swap(i, lastIndex - i);
		}

		return this;
	}

	/**
	 * Swaps two points via index
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public Vector2Array swap(int a, int b)
	{
		Lttl.Throw(a == b);

		float tempX = getX(a);
		float tempY = getY(a);
		set(a, getX(b), getY(b));
		set(b, tempX, tempY);

		return this;
	}

	/**
	 * @param output
	 *            if null, will create one
	 * @return
	 */
	public Rectangle getAABB(Rectangle output)
	{
		notEmptyCheck();

		if (output == null) output = new Rectangle();
		float[] minMax = getMinMax();
		output.set(minMax[0], minMax[1], minMax[2] - minMax[0], minMax[3]
				- minMax[1]);
		return output;
	}

	/**
	 * Returns a float array of the min and max values [minX,minY,maxX,maxY]
	 * 
	 * @return
	 * @throws emptyArray
	 */
	public float[] getMinMax()
	{
		notEmptyCheck();

		// init
		float minX = getX(0);
		float minY = getY(0);
		float maxX = minX;
		float maxY = minY;

		for (int i = 0; i < size(); i++)
		{
			float x = getX(i);
			float y = getY(i);
			if (x < minX)
			{
				minX = x;
			}
			if (y < minY)
			{
				minY = y;
			}
			if (x > maxX)
			{
				maxX = x;
			}
			if (y > maxY)
			{
				maxY = y;
			}
		}

		return new float[]
		{ minX, minY, maxX, maxY };
	}

	/**
	 * Calculates the center point between the given vectors
	 * 
	 * @param output
	 * @return
	 * @throws empty
	 */
	public Vector2 getCenter(Vector2 output)
	{
		Lttl.Throw(size() == 0);

		float[] minMax = getMinMax();
		float minX = minMax[0];
		float minY = minMax[1];
		float maxX = minMax[2];
		float maxY = minMax[3];
		float width = maxX - minX;
		float height = maxY - minY;

		return output.set(minX + width / 2, minY + height / 2);
	}

	private void notEmptyCheck()
	{
		Lttl.Throw(size() == 0, "Array can't be empty.");
	}

	/**
	 * Reduces the size of the backing array to the size of the actual items. This is useful to release memory when many
	 * items have been removed, or if it is known that more items will not be added.
	 */
	public void shrink()
	{
		fArray.shrink();
	}

	public void transformPoints(Matrix3 transformMatrix)
	{
		for (int i = 0, n = size(); i < n; i++)
		{
			// transform point
			get(i, sharedVector).mul(transformMatrix);
			// set point
			set(i, sharedVector);
		}
	}

	public void sortByX(boolean ascending)
	{
		LttlHelper.sortFloatArrayByElement(fArray, 2, 0, ascending);
	}

	public void sortByY(boolean ascending)
	{
		LttlHelper.sortFloatArrayByElement(fArray, 2, 1, ascending);
	}

	/**
	 * Checks if this Vector2Array has the samve values as the specified, order
	 * 
	 * @param a
	 * @return
	 */
	public boolean equals(Vector2Array a)
	{
		return LttlHelper.FloatArrayEqual(a.fArray, this.fArray);
	}

	/**
	 * This requires some computation, so possibly cache this.
	 * 
	 * @return
	 */
	public boolean isCounterClockwise()
	{
		return LttlGeometryUtil.isCounterClockwise(this);
	}

	public void debugDrawPoints(float radius, Color color)
	{
		for (int i = 0, n = size(); i < n; i++)
		{
			Lttl.debug.drawCircle(getX(i), getY(i), radius, color);
		}
	}

	public void debugDrawLines(float width, boolean connected, Color color)
	{
		Lttl.debug.drawLines(this, width, connected, color);
	}

	/**
	 * Returns the float array underneath, do not modify.
	 */
	public FloatArray getFloatArray()
	{
		return fArray;
	}

	/**
	 * @see LttlGeometry#intersectsSelf(Vector2Array, boolean)
	 */
	public boolean intersectsSelf(boolean isClosed)
	{
		return LttlGeometry.intersectsSelf(this, isClosed);
	}
}
