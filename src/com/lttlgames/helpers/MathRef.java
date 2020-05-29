package com.lttlgames.helpers;

import com.badlogic.gdx.math.MathUtils;

class MathRef
{
	/* LIBGDX MathUtils CONSTANTS */
	static public final float nanoToSec = MathUtils.nanoToSec;
	static public final float FLOAT_ROUNDING_ERROR = MathUtils.FLOAT_ROUNDING_ERROR;
	static public final float PI = MathUtils.PI;
	static public final float PI2 = MathUtils.PI2;

	static public final float E = MathUtils.E;
	/** multiply by this to convert from radians to degrees */
	static public final float radiansToDegrees = MathUtils.radDeg;
	/** multiply by this to convert from degrees to radians */
	static public final float degreesToRadians = MathUtils.degRad;

	/* ------ */
	/* ------ */
	/*  JAVA  */
	/* ------ */
	/* ------ */
	public static int abs(int a)
	{
		return Math.abs(a);
	}

	public static float abs(float a)
	{
		return Math.abs(a);
	}

	public static float max(float a, float b)
	{
		return Math.max(a, b);
	}

	public static int max(int a, int b)
	{
		return Math.max(a, b);
	}

	public static float min(float a, float b)
	{
		return Math.min(a, b);
	}

	public static int min(int a, int b)
	{
		return Math.min(a, b);
	}

	/**
	 * calculated strictly
	 */
	public static float hypot(float x, float y)
	{
		return (float) Math.hypot(x, y);
	}

	public static float pow(float a, float b)
	{
		return (float) Math.pow(a, b);
	}

	/**
	 * calculated strictly
	 */
	public static float sqrt(float a)
	{
		return (float) Math.sqrt(a);
	}

	/**
	 * calculated strictly
	 */
	public static float asinStrict(float a)
	{
		return (float) Math.asin(a);
	}

	/**
	 * calculated strictly
	 */
	public static float sinStrict(float a)
	{
		return (float) Math.sin(a);
	}

	/**
	 * calculated strictly
	 */
	public static float cosStrict(float a)
	{
		return (float) Math.cos(a);
	}

	/**
	 * calculated strictly
	 */
	public static float tanStrict(float a)
	{
		return (float) Math.tan(a);
	}

	/**
	 * calculated strictly
	 */
	public static float acosStrict(float a)
	{
		return (float) Math.acos(a);
	}

	/**
	 * calculated strictly
	 */
	public static float atan2Strict(float x, float y)
	{
		return (float) Math.atan2(x, y);
	}

	/**
	 * calculated strictly
	 */
	public static float atanStrict(float a)
	{
		return (float) Math.atan(a);
	}

	/* ------ */
	/* ------ */
	/* LIBGDX */
	/* ------ */
	/* ------ */

	/** Returns -1 or 1, randomly. */
	static public int randomSign()
	{
		return MathUtils.randomSign();
	}

	/**
	 * Returns a triangularly distributed random number between -1.0 (exclusive) and 1.0 (exclusive), where values
	 * around zero are more likely.
	 * <p>
	 * This is an optimized version of {@link #randomTriangular(float, float, float) randomTriangular(-1, 1, 0)}
	 */
	public static float randomTriangular()
	{
		return MathUtils.randomTriangular();
	}

	/**
	 * Returns a triangularly distributed random number between {@code -max} (exclusive) and {@code max} (exclusive),
	 * where values around zero are more likely.
	 * <p>
	 * This is an optimized version of {@link #randomTriangular(float, float, float) randomTriangular(-max, max, 0)}
	 * 
	 * @param max
	 *            the upper limit
	 */
	public static float randomTriangular(float max)
	{
		return MathUtils.randomTriangular(max);
	}

	/**
	 * Returns a triangularly distributed random number between {@code min} (inclusive) and {@code max} (exclusive),
	 * where the {@code mode} argument defaults to the midpoint between the bounds, giving a symmetric distribution.
	 * <p>
	 * This method is equivalent of {@link #randomTriangular(float, float, float) randomTriangular(min, max, (max - min)
	 * * .5f)}
	 * 
	 * @param min
	 *            the lower limit
	 * @param max
	 *            the upper limit
	 */
	public static float randomTriangular(float min, float max)
	{
		return MathUtils.randomTriangular(min, max);
	}

	/**
	 * Returns a triangularly distributed random number between {@code min} (inclusive) and {@code max} (exclusive),
	 * where values around {@code mode} are more likely.
	 * 
	 * @param min
	 *            the lower limit
	 * @param max
	 *            the upper limit
	 * @param mode
	 *            the point around which the values are more likely
	 */
	public static float randomTriangular(float min, float max, float mode)
	{
		return MathUtils.randomTriangular(min, max, mode);
	}

	/** Returns the sine in radians from a lookup table. */
	static public float sin(float radians)
	{
		return MathUtils.sin(radians);
	}

	/** Returns the cosine in radians from a lookup table. */
	static public float cos(float radians)
	{
		return MathUtils.cos(radians);
	}

	/** Returns the sine in degrees from a lookup table. */
	static public float sinDeg(float degrees)
	{
		return MathUtils.sinDeg(degrees);
	}

	/** Returns the cosine in degrees from a lookup table. */
	static public float cosDeg(float degrees)
	{
		return MathUtils.cosDeg(degrees);
	}

	/** Returns atan2 in radians from a lookup table. */
	static public float atan2(float y, float x)
	{
		return MathUtils.atan2(y, x);
	}

	/** Returns a random number between 0 (inclusive) and the specified value (inclusive). */
	static public int random(int range)
	{
		return MathUtils.random(range);
	}

	/** Returns a random number between start (inclusive) and end (inclusive). */
	static public int random(int start, int end)
	{
		return MathUtils.random(Math.min(start, end), end);
	}

	/** Returns a random boolean value. */
	static public boolean randomBoolean()
	{
		return MathUtils.randomBoolean();
	}

	/** Returns true if a random value between 0 and 1 is less than the specified value. */
	static public boolean randomBoolean(float chance)
	{
		return MathUtils.randomBoolean(chance);
	}

	/** Returns random number between 0.0 (inclusive) and 1.0 (exclusive). */
	static public float random()
	{
		return MathUtils.random();
	}

	/** Returns a random number between 0 (inclusive) and the specified value (exclusive). */
	static public float random(float range)
	{
		return MathUtils.random(range);
	}

	/** Returns a random number between start (inclusive) and end (exclusive). */
	static public float random(float start, float end)
	{
		return MathUtils.random(start, end);
	}

	/** Returns the next power of two. Returns the specified value if the value is already a power of two. */
	static public int nextPowerOfTwo(int value)
	{
		return MathUtils.nextPowerOfTwo(value);
	}

	static public boolean isPowerOfTwo(int value)
	{
		return MathUtils.isPowerOfTwo(value);
	}

	static public int clamp(int value, int min, int max)
	{
		return MathUtils.clamp(value, min, max);
	}

	static public short clamp(short value, short min, short max)
	{
		return MathUtils.clamp(value, min, max);
	}

	static public float clamp(float value, float min, float max)
	{
		return MathUtils.clamp(value, min, max);
	}

	/**
	 * Returns the largest integer less than or equal to the specified float. This method will only properly floor
	 * floats from -(2^14) to (Float.MAX_VALUE - 2^14).
	 */
	static public int floor(float x)
	{
		return MathUtils.floor(x);
	}

	/**
	 * Returns the largest integer less than or equal to the specified float. This method will only properly floor
	 * floats that are positive. Note this method simply casts the float to int.
	 */
	static public int floorPositive(float x)
	{
		return MathUtils.floorPositive(x);
	}

	/**
	 * Returns the smallest integer greater than or equal to the specified float. This method will only properly ceil
	 * floats from -(2^14) to (Float.MAX_VALUE - 2^14).
	 */
	static public int ceil(float x)
	{
		return MathUtils.ceil(x);
	}

	/**
	 * Returns the smallest integer greater than or equal to the specified float. This method will only properly ceil
	 * floats that are positive.
	 */
	static public int ceilPositive(float x)
	{
		return MathUtils.ceilPositive(x);
	}

	/**
	 * Returns the closest integer to the specified float. This method will only properly round floats from -(2^14) to
	 * (Float.MAX_VALUE - 2^14).
	 */
	static public int round(float x)
	{
		return MathUtils.round(x);
	}

	/**
	 * Returns the closest integer to the specified float. This method will only properly round floats that are
	 * positive.
	 */
	static public int roundPositive(float x)
	{
		return MathUtils.roundPositive(x);
	}

	/** Returns true if the value is zero (using the default tolerance as upper bound) */
	static public boolean isZero(float value)
	{
		return MathUtils.isZero(value);
	}

	/**
	 * Returns true if the value is zero.
	 * 
	 * @param tolerance
	 *            represent an upper bound below which the value is considered zero.
	 */
	static public boolean isZero(float value, float tolerance)
	{
		return MathUtils.isZero(value, tolerance);
	}

	/**
	 * Returns true if a is nearly equal to b. The function uses the default floating error tolerance.
	 * 
	 * @param a
	 *            the first value.
	 * @param b
	 *            the second value.
	 */
	static public boolean isEqual(float a, float b)
	{
		return MathUtils.isEqual(a, b);
	}

	/**
	 * Returns true if a is nearly equal to b.
	 * 
	 * @param a
	 *            the first value.
	 * @param b
	 *            the second value.
	 * @param tolerance
	 *            represent an upper bound below which the two values are considered equal.
	 */
	static public boolean isEqual(float a, float b, float tolerance)
	{
		return MathUtils.isEqual(a, b, tolerance);
	}

	static public float getEpsilon()
	{
		return FLOAT_ROUNDING_ERROR;
	}
}
