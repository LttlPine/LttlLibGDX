package com.lttlgames.helpers;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.FloatArray;
import com.lttlgames.editor.Lttl;
import com.lttlgames.editor.LttlTransform;

public final class LttlMath extends MathRef
{
	private LttlMath()
	{
	}

	static public final float EPSILON = MathUtils.FLOAT_ROUNDING_ERROR;

	/**
	 * Makes sure a value is greater than {@link #EPSILON}.
	 */
	public static float EpsilonClamp(float v)
	{
		return LttlMath.max(v, EPSILON);
	}

	/**
	 * Get the lerp percentage based on the start and end and the current value
	 * 
	 * @param start
	 * @param end
	 * @param value
	 * @return the non clamped lerp value, may go beyond 0-1
	 */
	public static float ReverseLerp(float start, float end, float value)
	{
		return (value - start) / (end - start);
	}

	/**
	 * Linear interpolates between two float values based on a percent
	 * 
	 * @param start
	 * @param end
	 * @param percent
	 * @return
	 */
	public static float Lerp(float start, float end, float percent)
	{
		return start + percent * (end - start);
	}

	public static final Vector2 One = new Vector2(1, 1);
	public static final Vector2 Zero = new Vector2(0, 0);

	private static final Vector2Array tmpVector2ArrayInternal = new Vector2Array();
	private static boolean tmpV2ArrayInternalCheckedOut0 = false;
	private static final FloatArray tmpFloatArrayInternal = new FloatArray();
	private static boolean tmpFloatArrayInternalCheckedOut0 = false;
	private static final Vector2 tmpV2Internal0 = new Vector2(0, 0);
	private static boolean tmpV2InternalCheckedOut0 = false;
	private static final Vector2 tmpV2Internal1 = new Vector2(0, 0);
	private static boolean tmpV2InternalCheckedOut1 = false;
	private static final Vector2 tmpV2Internal2 = new Vector2(0, 0);
	private static boolean tmpV2InternalCheckedOut2 = false;
	private static final Vector2 tmpV2Internal3 = new Vector2(0, 0);
	private static boolean tmpV2InternalCheckedOut3 = false;
	private static final Vector2 tmpV2Internal4 = new Vector2(0, 0);
	private static boolean tmpV2InternalCheckedOut4 = false;
	private static final ArrayList<Vector2> tmpListV2 = new ArrayList<Vector2>();
	private static boolean tmpListV2CheckedOut = false;
	private static final Vector3 tmpV3Internal = new Vector3(0, 0, 0);
	private static boolean tmpM3InternalCheckedOut = false;
	private static final Matrix3 tmpM3 = new Matrix3();
	private static boolean tmpM3CheckedOut = false;
	private static final Matrix3 tmpM3Internal = new Matrix3();
	private static boolean tmpRect0CheckedOut = false;
	private final static Rectangle tmpRect0 = new Rectangle();
	private static boolean tmpRectInteranal0CheckedOut = false;
	private final static Rectangle tmpRectInternal0 = new Rectangle();

	static Vector2Array CheckoutTmpV2ArrayInternal()
	{
		if (tmpV2ArrayInternalCheckedOut0)
		{
			Lttl.Throw("Getting temporary Vector2Array internal when it is already in use.");
		}
		tmpVector2ArrayInternal.clear();
		tmpV2ArrayInternalCheckedOut0 = true;
		return tmpVector2ArrayInternal;
	}

	static void ReturnTmpV2ArrayInternal()
	{
		tmpVector2ArrayInternal.clear();
		tmpV2ArrayInternalCheckedOut0 = false;
	}

	static FloatArray CheckoutTmpFloatArrayInternal()
	{
		if (tmpFloatArrayInternalCheckedOut0)
		{
			Lttl.Throw("Getting temporary FloatArray internal when it is already in use.");
		}
		tmpFloatArrayInternal.clear();
		tmpFloatArrayInternalCheckedOut0 = true;
		return tmpFloatArrayInternal;
	}

	static void ReturnTmpFloatArrayInternal()
	{
		tmpFloatArrayInternal.clear();
		tmpFloatArrayInternalCheckedOut0 = false;
	}

	public static ArrayList<Vector2> CheckoutTmpListV2()
	{
		if (tmpListV2CheckedOut)
		{
			Lttl.Throw("Getting temporary Vector2 array list object when it is already in use.");
		}
		tmpListV2.clear();
		tmpListV2CheckedOut = true;
		return tmpListV2;
	}

	public static void ReturnTmpListV2()
	{
		tmpListV2.clear();
		tmpListV2CheckedOut = false;
	}

	private static Matrix3 CheckoutTmpM3Internal()
	{
		if (tmpM3InternalCheckedOut)
		{
			Lttl.Throw("Getting temporary internal Matrix3 object when it is already in use.");
		}
		tmpM3Internal.idt();
		tmpM3InternalCheckedOut = true;
		return tmpM3;
	}

	private static void ReturnTmpM3Internal()
	{
		tmpM3Internal.idt();
		tmpM3InternalCheckedOut = false;
	}

	public static Matrix3 CheckoutTmpM3()
	{
		if (tmpM3CheckedOut)
		{
			Lttl.Throw("Getting temporary Matrix3 object when it is already in use.");
		}
		tmpM3.idt();
		tmpM3CheckedOut = true;
		return tmpM3;
	}

	public static void ReturnTmpM3()
	{
		tmpM3.idt();
		tmpM3CheckedOut = false;
	}

	static Vector2 CheckoutTmpV2Internal0()
	{
		if (tmpV2InternalCheckedOut0)
		{
			Lttl.Throw("Getting temporary Vector2 internal object when it is already in use.");
		}
		tmpV2InternalCheckedOut0 = true;
		return tmpV2Internal0;
	}

	static void ReturnTmpV2Internal0()
	{
		tmpV2Internal0.set(0, 0);
		tmpV2InternalCheckedOut0 = false;
	}

	static Vector2 CheckoutTmpV2Internal1()
	{
		if (tmpV2InternalCheckedOut1)
		{
			Lttl.Throw("Getting temporary Vector2 internal 1 object when it is already in use.");
		}
		tmpV2InternalCheckedOut1 = true;
		return tmpV2Internal1;
	}

	static void ReturnTmpV2Internal1()
	{
		tmpV2Internal1.set(0, 0);
		tmpV2InternalCheckedOut1 = false;
	}

	static Vector2 CheckoutTmpV2Internal2()
	{
		if (tmpV2InternalCheckedOut2)
		{
			Lttl.Throw("Getting temporary Vector2 internal 2 object when it is already in use.");
		}
		tmpV2InternalCheckedOut2 = true;
		return tmpV2Internal2;
	}

	static void ReturnTmpV2Internal2()
	{
		tmpV2Internal2.set(0, 0);
		tmpV2InternalCheckedOut2 = false;
	}

	static Vector2 CheckoutTmpV2Internal3()
	{
		if (tmpV2InternalCheckedOut3)
		{
			Lttl.Throw("Getting temporary Vector2 internal 3 object when it is already in use.");
		}
		tmpV2InternalCheckedOut3 = true;
		return tmpV2Internal3;
	}

	static void ReturnTmpV2Internal3()
	{
		tmpV2Internal3.set(0, 0);
		tmpV2InternalCheckedOut3 = false;
	}

	static Vector2 CheckoutTmpV2Internal4()
	{
		if (tmpV2InternalCheckedOut4)
		{
			Lttl.Throw("Getting temporary Vector2 internal 4 object when it is already in use.");
		}
		tmpV2InternalCheckedOut4 = true;
		return tmpV2Internal4;
	}

	static void ReturnTmpV2Internal4()
	{
		tmpV2Internal4.set(0, 0);
		tmpV2InternalCheckedOut4 = false;
	}

	public static Rectangle CheckoutTmpRect0()
	{
		if (tmpRect0CheckedOut)
		{
			Lttl.Throw("Getting temporary rectangle 0 object when it is already in use.");
		}
		tmpRect0CheckedOut = true;
		return tmpRect0;
	}

	public static void ReturnTmpRect0()
	{
		tmpRect0.set(0, 0, 0, 0);
		tmpRect0CheckedOut = false;
	}

	static Rectangle CheckoutTmpRectInternal0()
	{
		if (tmpRectInteranal0CheckedOut)
		{
			Lttl.Throw("Getting temporary rectangle internal 0 object when it is already in use.");
		}
		tmpRectInteranal0CheckedOut = true;
		return tmpRectInternal0;
	}

	static void ReturnTmpRectInternal0()
	{
		tmpRectInternal0.set(0, 0, 0, 0);
		tmpRectInteranal0CheckedOut = false;
	}

	// PROCESSING
	static final protected float sinLUT[];
	static final protected float cosLUT[];
	static final protected float SINCOS_PRECISION = 0.5f;
	static final protected int SINCOS_LENGTH = (int) (360f / SINCOS_PRECISION);
	static
	{
		sinLUT = new float[SINCOS_LENGTH];
		cosLUT = new float[SINCOS_LENGTH];
		for (int i = 0; i < SINCOS_LENGTH; i++)
		{
			sinLUT[i] = (float) sin(i * degreesToRadians * SINCOS_PRECISION);
			cosLUT[i] = (float) cos(i * degreesToRadians * SINCOS_PRECISION);
		}
	}

	/**
	 * Smooth damps from current to target over smoothTime, saving the new positing in output
	 * 
	 * @param current
	 * @param target
	 * @param currentVelocity
	 *            this Vector2 will update with the new current velocity, which should be given back into function next
	 *            time
	 * @param maxSpeed
	 *            limits the speed for x and y, can be null it not limit
	 * @param smoothTime
	 * @param deltaTime
	 * @param output
	 * @return
	 */
	public static Vector2 SmoothDamp(Vector2 current, Vector2 target,
			Vector2 currentVelocity, Vector2 maxSpeed, float smoothTime,
			float deltaTime, Vector2 output)
	{
		smoothTime = max(0.0001f, smoothTime);
		float num = 2f / smoothTime;
		float num2 = num * deltaTime;
		float num3 = 1f / (1f + num2 + 0.48f * num2 * num2 + 0.235f * num2
				* num2 * num2);
		// x
		{
			float num4 = current.x - target.x;
			float num5 = target.x;
			if (maxSpeed != null)
			{
				float num6 = maxSpeed.x * smoothTime;
				num4 = clamp(num4, -num6, num6);
			}
			target.x = current.x - num4;
			float num7 = (currentVelocity.x + num * num4) * deltaTime;
			currentVelocity.x = (currentVelocity.x - num * num7) * num3;
			float num8 = target.x + (num4 + num7) * num3;
			if (num5 - current.x > 0f == num8 > num5)
			{
				num8 = num5;
				currentVelocity.x = (num8 - num5) / deltaTime;
			}
			output.x = num8;
		}
		// y
		{
			float num4 = current.y - target.y;
			float num5 = target.y;
			if (maxSpeed != null)
			{
				float num6 = maxSpeed.y * smoothTime;
				num4 = clamp(num4, -num6, num6);
			}
			target.y = current.y - num4;
			float num7 = (currentVelocity.y + num * num4) * deltaTime;
			currentVelocity.y = (currentVelocity.y - num * num7) * num3;
			float num8 = target.y + (num4 + num7) * num3;
			if (num5 - current.y > 0f == num8 > num5)
			{
				num8 = num5;
				currentVelocity.y = (num8 - num5) / deltaTime;
			}
			output.y = num8;
		}
		return output;
	}

	/**
	 * Smooth dampens values
	 * 
	 * @param current
	 * @param target
	 * @param currentVelocity
	 *            this value should be updated each time
	 * @param smoothTime
	 * @param deltaTime
	 *            Use Gdx.graphics.getDeltaTime();
	 * @return A float array, [0] = new damped value, [1] = currentVelocity (should be updated each frame)
	 */
	public static float[] SmoothDamp(float current, float target,
			float currentVelocity, float smoothTime, float deltaTime)
	{
		float maxSpeed = Float.POSITIVE_INFINITY;
		return SmoothDamp(current, target, currentVelocity, smoothTime,
				deltaTime, maxSpeed);
	}

	/**
	 * Smooth dampens values with maxSpeed
	 * 
	 * @param current
	 * @param target
	 * @param currentVelocity
	 *            this value should be updated each time
	 * @param smoothTime
	 * @param deltaTime
	 * @param maxSpeed
	 * @return A float array, [0] = new damped value, [1] = currentVelocity (should be updated each frame)
	 */
	public static float[] SmoothDamp(float current, float target,
			float currentVelocity, float smoothTime, float deltaTime,
			float maxSpeed)
	{
		smoothTime = max(0.0001f, smoothTime);
		float num = 2f / smoothTime;
		float num2 = num * deltaTime;
		float num3 = 1f / (1f + num2 + 0.48f * num2 * num2 + 0.235f * num2
				* num2 * num2);
		float num4 = current - target;
		float num5 = target;
		if (maxSpeed != Float.POSITIVE_INFINITY)
		{
			float num6 = maxSpeed * smoothTime;
			num4 = clamp(num4, -num6, num6);
		}
		target = current - num4;
		float num7 = (currentVelocity + num * num4) * deltaTime;
		currentVelocity = (currentVelocity - num * num7) * num3;
		float num8 = target + (num4 + num7) * num3;
		if (num5 - current > 0f == num8 > num5)
		{
			num8 = num5;
			currentVelocity = (num8 - num5) / deltaTime;
		}
		return new float[]
		{ num8, currentVelocity };
	}

	public static float Clamp01(float value)
	{
		if (value < 0f) { return 0f; }
		if (value > 1f) { return 1f; }
		return value;
	}

	public static int Clamp01(int value)
	{
		if (value < 0f) { return 0; }
		if (value > 1f) { return 1; }
		return value;
	}

	/**
	 * Smooth Following
	 * 
	 * @param currentPos
	 * @param targetPos
	 * @param followX
	 * @param followY
	 * @param distanceX
	 *            the distance we want the camera to be to the right of the target
	 * @param distanceY
	 *            the height we want the camera to be above the target
	 * @param xDamping
	 * @param yDamping
	 * @param deltaTime
	 * @param target
	 * @return
	 */
	public static Vector2 SmoothFollow(Vector2 currentPos, Vector2 targetPos,
			boolean followX, boolean followY, float distanceX, float distanceY,
			float xDamping, float yDamping, float deltaTime, Vector2 target)
	{
		if (target == null) return target;

		target.set(currentPos.x, currentPos.y);
		if (followX)
		{
			float wantedX = targetPos.x + distanceX;
			float currentX = currentPos.x;

			// Damp the X
			target.x = LttlMath.Lerp(currentX, wantedX, xDamping * deltaTime);
			// System.out.println(xDamping * deltaTime);

			// targetPos.x = clamp(currentX,minX,maxX), targetPos.y, targetPos.z);
		}

		if (followY)
		{
			float wantedY = targetPos.y + distanceY;
			float currentY = currentPos.y;

			// Damp the Y
			target.y = LttlMath.Lerp(currentY, wantedY, yDamping * deltaTime);

			// targetPos = new Vector3(targetPos.x, clamp(currentY,minY,maxY), targetPos.z);
		}
		return target;
	}

	/**
	 * @param nums
	 * @return The minimimum from an array of nums.
	 */
	public static float min(float... nums)
	{
		if (nums.length <= 0) return 0;
		float min = nums[0];
		if (nums.length == 1) return min;
		for (int i = 1; i < nums.length; i++)
		{
			if (nums[i] < min)
			{
				min = nums[i];
			}
		}
		return min;
	}

	/**
	 * @param floats
	 * @return The max value from an array of nums.
	 */
	public static float max(float... nums)
	{
		if (nums.length <= 0) return 0;
		float max = nums[0];
		if (nums.length == 1) return max;
		for (int i = 1; i < nums.length; i++)
		{
			if (nums[i] > max)
			{
				max = nums[i];
			}
		}
		return max;
	}

	/**
	 * @param ints
	 * @return The min value from an array of nums.
	 */
	public static float min(int... nums)
	{
		if (nums.length <= 0) return 0;
		int min = nums[0];
		if (nums.length == 1) return min;
		for (int i = 1; i < nums.length; i++)
		{
			if (nums[i] < min)
			{
				min = nums[i];
			}
		}
		return min;
	}

	/**
	 * @param nums
	 * @return The max value from an array of nums.
	 */
	public static float max(int... nums)
	{
		if (nums.length <= 0) return 0;
		int max = nums[0];
		if (nums.length == 1) return max;
		for (int i = 1; i < nums.length; i++)
		{
			if (nums[i] > max)
			{
				max = nums[i];
			}
		}
		return max;
	}

	/**
	 * Is the number between the two numbers (inclusive)
	 * 
	 * @param v
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean isBetween(float v, float a, float b)
	{
		return v >= a && v <= b || v <= a && v >= b;
	}

	/**
	 * Is the number between the two numbers (exclusive)
	 * 
	 * @param v
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean isBetweenExclusive(float v, float a, float b)
	{
		return v > a && v < b || v < a && v > b;
	}

	/*
	 Copyright (c) 2011 Bob Berkebile (pixelplacment)
	 Please direct any bugs/comments/suggestions to http://pixelplacement.com
	
	 Permission is hereby granted, free of charge, to any person obtaining a copy
	 of this software and associated documentation files (the "Software"), to deal
	 in the Software without restriction, including without limitation the rights
	 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	 copies of the Software, and to permit persons to whom the Software is
	 furnished to do so, subject to the following conditions:
	
	 The above copyright notice and this permission notice shall be included in
	 all copies or substantial portions of the Software.
	
	 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	 THE SOFTWARE.
	/*
	TERMS OF USE - EASING EQUATIONS
	Open source under the BSD License.
	Copyright (c)2001 Robert Penner
	All rights reserved.
	Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
	Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
	Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
	Neither the name of the author nor the names of contributors may be used to endorse or promote products derived from this software without specific prior written permission.
	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
	*/
	/*
	The type of easing to use based on Robert Penner's open source easing equations
	(http://www.robertpenner.com/easing_terms_of_use.html).
	*/
	// **EASE FUNCTIONS**//

	/**
	 * Returns a float from start to end. Halfway through it will reach end and then go backwards the same exact ease to
	 * start
	 * 
	 * @param start
	 * @param end
	 * @param value
	 * @param ease
	 * @return
	 */
	public static float interpBoomerang(float start, float end, float value,
			EaseType ease)
	{
		return start + ((end - start) * interpBoomerang(value, ease));
	}

	/**
	 * Returns a float from 0 to 1. Halfway through it will reach 1 and then go backwards the same exact ease to 0.
	 * 
	 * @param value
	 * @param ease
	 * @return
	 */
	public static float interpBoomerang(float value, EaseType ease)
	{
		value *= 2;
		if (value > 1)
		{
			value = 1 - (value - 1);
		}
		return interp(value, ease);
	}

	/**
	 * Returns a float from 0 to 1.
	 * 
	 * @param value
	 * @param ease
	 * @return
	 */
	public static float interp(float value, EaseType ease)
	{
		return interp(0, 1, value, ease, null);
	}

	/**
	 * Returns a float from specified range. Does not clamp 0-1.
	 * 
	 * @param start
	 * @param end
	 * @param value
	 * @param ease
	 * @return
	 */
	public static float interp(float start, float end, float value,
			EaseType ease)
	{
		return interp(start, end, value, ease, null);
	}

	/**
	 * @param start
	 * @param end
	 * @param output
	 * @param percent
	 * @param ease
	 * @return
	 */
	public static Vector2 interp(Vector2 start, Vector2 end, Vector2 output,
			float percent, EaseType ease)
	{
		output.x = interp(start.x, end.x, percent, ease, null);
		output.y = interp(start.y, end.y, percent, ease, null);
		return output;
	}

	public static float interp(float value, EaseType ease, float... params)
	{
		return interp(0, 1, value, ease, params);
	}

	public static float interp(float start, float end, float value,
			EaseType ease, float... params)
	{
		switch (ease)
		{
			case BackIn:
				return easeInBack(start, end, value);
			case BounceIn:
				return easeInBounce(start, end, value);
			case CircIn:
				return easeInCirc(start, end, value);
			case CubicIn:
				return easeInCubic(start, end, value);
			case ElasticIn:
				return easeInElastic(start, end, value);
			case ExpoIn:
				return easeInExpo(start, end, value);
			case BackInOut:
				return easeInOutBack(start, end, value);
			case BounceInOut:
				return easeInOutBounce(start, end, value);
			case CircInOut:
				return easeInOutCirc(start, end, value);
			case CubicInOut:
				return easeInOutCubic(start, end, value);
			case ElasticInOut:
				return easeInOutElastic(start, end, value);
			case ExpoInOut:
				return easeInOutExpo(start, end, value);
			case QuadInOut:
				return easeInOutQuad(start, end, value);
			case QuartInOut:
				return easeInOutQuart(start, end, value);
			case QuintInOut:
				return easeInOutQuint(start, end, value);
			case SineInOut:
				return easeInOutSine(start, end, value);
			case QuadIn:
				return easeInQuad(start, end, value);
			case QuartIn:
				return easeInQuart(start, end, value);
			case QuintIn:
				return easeInQuint(start, end, value);
			case SineIn:
				return easeInSine(start, end, value);
			case BackOut:
				return easeOutBack(start, end, value);
			case BounceOut:
				return easeOutBounce(start, end, value);
			case CircOut:
				return easeOutCirc(start, end, value);
			case CubicOut:
				return easeOutCubic(start, end, value);
			case ElasticOut:
				return easeOutElastic(start, end, value);
			case ExpoOut:
				return easeOutExpo(start, end, value);
			case QuadOut:
				return easeOutQuad(start, end, value);
			case QuartOut:
				return easeOutQuart(start, end, value);
			case QuintOut:
				return easeOutQuint(start, end, value);
			case SineOut:
				return easeOutSine(start, end, value);
			case Linear:
				return linear(start, end, value);
			case Punch:
				return actionPunch(end, value, params);
			case SwingIn:
				return swingIn(start, end, value, params);
			case SwingOut:
				return swingOut(start, end, value, params);
			case SwingInOut:
				return swingInOut(start, end, value, params);
			case Spring:
				return spring(start, end, value);
			case ShakeIn:
				return actionShakeIn(-end, end, value);
			case ShakeOut:
				return actionShakeOut(-end, end, value);
			case ShakeInOut:
				return actionShakeInOut(-end, end, value);
			case ShakeFixed:
				return actionShakeFixed(-end, end);
			case FADE:
				return fade(start, end, value);
			default:
				return 0;

		}
	}

	public static float swingInOut(float start, float end, float value,
			float... params)
	{
		float scale = 5;
		if (params != null && params.length >= 1)
		{
			scale = params[0];
		}
		scale *= 2;

		float a = value;
		if (a <= 0.5f)
		{
			a *= 2;
			return a * a * ((scale + 1) * a - scale) / 2;
		}
		a--;
		a *= 2;
		return start + (end - start)
				* (a * a * ((scale + 1) * a + scale) / 2 + 1);
	}

	public static float swingOut(float start, float end, float value,
			float... params)
	{
		float scale = 5;
		if (params != null && params.length >= 1)
		{
			scale = params[0];
		}

		float a = value;
		a--;
		return start + (end - start) * (a * a * ((scale + 1) * a + scale) + 1);
	}

	public static float swingIn(float start, float end, float value,
			float... params)
	{
		float scale = 5;
		if (params != null && params.length >= 1)
		{
			scale = params[0];
		}

		float a = value;
		return start + (end - start) * (a * a * ((scale + 1) * a - scale));
	}

	public static float fade(float start, float end, float value)
	{
		float a = value;
		return start + (end - start)
				* clamp(a * a * a * (a * (a * 6 - 15) + 10), 0, 1);
	}

	public static float linear(float start, float end, float value)
	{
		return Lerp(start, end, value);
	}

	public static float clerp(float start, float end, float value)
	{
		float min = 0.0f;
		float max = 360.0f;
		float half = abs((max - min) / 2.0f);
		float retval = 0.0f;
		float diff = 0.0f;
		if ((end - start) < -half)
		{
			diff = ((max - start) + end) * value;
			retval = start + diff;
		}
		else if ((end - start) > half)
		{
			diff = -((max - end) + start) * value;
			retval = start + diff;
		}
		else retval = start + (end - start) * value;
		return retval;
	}

	public static float spring(float start, float end, float value)
	{
		value = Clamp01(value);
		value = (float) ((sin(value * PI
				* (0.2f + 2.5f * value * value * value))
				* pow(1f - value, 2.2f) + value) * (1f + (1.2f * (1f - value))));
		return start + (end - start) * value;
	}

	public static float easeInQuad(float start, float end, float value)
	{
		end -= start;
		return end * value * value + start;
	}

	public static float easeOutQuad(float start, float end, float value)
	{
		end -= start;
		return -end * value * (value - 2) + start;
	}

	public static float easeInOutQuad(float start, float end, float value)
	{
		value /= .5f;
		end -= start;
		if (value < 1) return end / 2 * value * value + start;
		value--;
		return -end / 2 * (value * (value - 2) - 1) + start;
	}

	public static float easeInCubic(float start, float end, float value)
	{
		end -= start;
		return end * value * value * value + start;
	}

	public static float easeOutCubic(float start, float end, float value)
	{
		value--;
		end -= start;
		return end * (value * value * value + 1) + start;
	}

	public static float easeInOutCubic(float start, float end, float value)
	{
		value /= .5f;
		end -= start;
		if (value < 1) return end / 2 * value * value * value + start;
		value -= 2;
		return end / 2 * (value * value * value + 2) + start;
	}

	public static float easeInQuart(float start, float end, float value)
	{
		end -= start;
		return end * value * value * value * value + start;
	}

	public static float easeOutQuart(float start, float end, float value)
	{
		value--;
		end -= start;
		return -end * (value * value * value * value - 1) + start;
	}

	public static float easeInOutQuart(float start, float end, float value)
	{
		value /= .5f;
		end -= start;
		if (value < 1) return end / 2 * value * value * value * value + start;
		value -= 2;
		return -end / 2 * (value * value * value * value - 2) + start;
	}

	public static float easeInQuint(float start, float end, float value)
	{
		end -= start;
		return end * value * value * value * value * value + start;
	}

	public static float easeOutQuint(float start, float end, float value)
	{
		value--;
		end -= start;
		return end * (value * value * value * value * value + 1) + start;
	}

	public static float easeInOutQuint(float start, float end, float value)
	{
		value /= .5f;
		end -= start;
		if (value < 1)
			return end / 2 * value * value * value * value * value + start;
		value -= 2;
		return end / 2 * (value * value * value * value * value + 2) + start;
	}

	public static float easeInSine(float start, float end, float value)
	{
		end -= start;
		return (float) (-end * cos(value / 1 * (PI / 2)) + end + start);
	}

	public static float easeOutSine(float start, float end, float value)
	{
		end -= start;
		return (float) (end * sin(value / 1 * (PI / 2)) + start);
	}

	public static float easeInOutSine(float start, float end, float value)
	{
		end -= start;
		return (float) (-end / 2 * (cos(PI * value / 1) - 1) + start);
	}

	public static float easeInExpo(float start, float end, float value)
	{
		end -= start;
		return (float) (end * pow(2, 10 * (value / 1 - 1)) + start);
	}

	public static float easeOutExpo(float start, float end, float value)
	{
		end -= start;
		return (float) (end * (-pow(2, -10 * value / 1) + 1) + start);
	}

	public static float easeInOutExpo(float start, float end, float value)
	{
		value /= .5f;
		end -= start;
		if (value < 1)
			return (float) (end / 2 * pow(2, 10 * (value - 1)) + start);
		value--;
		return (float) (end / 2 * (-pow(2, -10 * value) + 2) + start);
	}

	public static float easeInCirc(float start, float end, float value)
	{
		end -= start;
		return (float) (-end * (sqrt(1 - value * value) - 1) + start);
	}

	public static float easeOutCirc(float start, float end, float value)
	{
		value--;
		end -= start;
		return (float) (end * sqrt(1 - value * value) + start);
	}

	public static float easeInOutCirc(float start, float end, float value)
	{
		value /= .5f;
		end -= start;
		if (value < 1)
			return (float) (-end / 2 * (sqrt(1 - value * value) - 1) + start);
		value -= 2;
		return (float) (end / 2 * (sqrt(1 - value * value) + 1) + start);
	}

	public static float easeInBounce(float start, float end, float value)
	{
		end -= start;
		float d = 1f;
		return end - easeOutBounce(0, end, d - value) + start;
	}

	public static float easeOutBounce(float start, float end, float value)
	{
		value /= 1f;
		end -= start;
		if (value < (1 / 2.75f))
		{
			return end * (7.5625f * value * value) + start;
		}
		else if (value < (2 / 2.75f))
		{
			value -= (1.5f / 2.75f);
			return end * (7.5625f * (value) * value + .75f) + start;
		}
		else if (value < (2.5 / 2.75))
		{
			value -= (2.25f / 2.75f);
			return end * (7.5625f * (value) * value + .9375f) + start;
		}
		else
		{
			value -= (2.625f / 2.75f);
			return end * (7.5625f * (value) * value + .984375f) + start;
		}
	}

	public static float easeInOutBounce(float start, float end, float value)
	{
		end -= start;
		float d = 1f;
		if (value < d / 2) return easeInBounce(0, end, value * 2) * 0.5f
				+ start;
		else return easeOutBounce(0, end, value * 2 - d) * 0.5f + end * 0.5f
				+ start;
	}

	public static float easeInBack(float start, float end, float value)
	{
		end -= start;
		value /= 1;
		float s = 1.70158f;
		return end * (value) * value * ((s + 1) * value - s) + start;
	}

	public static float easeOutBack(float start, float end, float value)
	{
		float s = 1.70158f;
		end -= start;
		value = (value / 1) - 1;
		return end * ((value) * value * ((s + 1) * value + s) + 1) + start;
	}

	public static float easeInOutBack(float start, float end, float value)
	{
		float s = 1.70158f;
		end -= start;
		value /= .5f;
		if ((value) < 1)
		{
			s *= (1.525f);
			return end / 2 * (value * value * (((s) + 1) * value - s)) + start;
		}
		value -= 2;
		s *= (1.525f);
		return end / 2 * ((value) * value * (((s) + 1) * value + s) + 2)
				+ start;
	}

	public static float easeInElastic(float start, float end, float value)
	{
		end -= start;

		float d = 1f;
		float p = d * .3f;
		float s = 0;
		float a = 0;

		if (value == 0) return start;

		if ((value /= d) == 1) return start + end;

		if (a == 0f || a < abs(end))
		{
			a = end;
			s = p / 4;
		}
		else
		{
			s = (float) (p / (2 * PI) * asinStrict(end / a));
		}

		return (float) (-(a * pow(2, 10 * (value -= 1)) * sin((value * d - s)
				* (2 * PI) / p)) + start);
	}

	public static float easeOutElastic(float start, float end, float value)
	{
		end -= start;

		float d = 1f;
		float p = d * .3f;
		float s = 0;
		float a = 0;

		if (value == 0) return start;

		if ((value /= d) == 1) return start + end;

		if (a == 0f || a < abs(end))
		{
			a = end;
			s = p / 4;
		}
		else
		{
			s = (float) (p / (2 * PI) * asinStrict(end / a));
		}

		return (float) (a * pow(2, -10 * value)
				* sin((value * d - s) * (2 * PI) / p) + end + start);
	}

	public static float easeInOutElastic(float start, float end, float value)
	{
		end -= start;

		float d = 1f;
		float p = d * .3f;
		float s = 0;
		float a = 0;

		if (value == 0) return start;

		if ((value /= d / 2) == 2) return start + end;

		if (a == 0f || a < abs(end))
		{
			a = end;
			s = p / 4;
		}
		else
		{
			s = (float) (p / (2 * PI) * asinStrict(end / a));
		}

		if (value < 1)
			return (float) (-0.5f
					* (a * pow(2, 10 * (value -= 1)) * sin((value * d - s)
							* (2 * PI) / p)) + start);
		return (float) (a * pow(2, -10 * (value -= 1))
				* sin((value * d - s) * (2 * PI) / p) * 0.5f + end + start);
	}

	/**
	 * Checks if this EaseType uses the end value as it's magnitude, will return to original position.
	 * 
	 * @param ease
	 * @return
	 */
	public static boolean isEaseActionWithMagnitude(EaseType ease)
	{
		switch (ease)
		{
			case ShakeIn:
			case ShakeOut:
			case ShakeInOut:
			case ShakeFixed:
			case Punch:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Checks if this EaseType should use a unique interpolation for each value in the tween. This is because it uses a
	 * random.
	 * 
	 * @param ease
	 * @return
	 */
	public static boolean isEaseUnique(EaseType ease)
	{
		switch (ease)
		{
			case ShakeIn:
			case ShakeOut:
			case ShakeInOut:
			case ShakeFixed:
				return true;
			default:
				return false;
		}
	}

	// ** ACTIONS **//
	// These can be used as an easeType in a tween by themselves, but the target value is the magnitude, which controls
	// the intensity of the shake/punch.<br>
	// You can also use them in addition with other eases by calling this ease and updatig the target values in a
	// TweenCallBack's OnStep()
	public static float actionShakeIn(float rangeBottom, float rangeTop,
			float value)
	{
		return rangeBottom + random(0, value) * (rangeTop - rangeBottom);
	}

	public static float actionShakeOut(float rangeBottom, float rangeTop,
			float value)
	{
		value = 1 - value;
		return rangeBottom + random(0, value) * (rangeTop - rangeBottom);
	}

	public static float actionShakeInOut(float rangeBottom, float rangeTop,
			float value)
	{
		value = interpBoomerang(value, EaseType.QuadInOut);
		return rangeBottom + random(0, value) * (rangeTop - rangeBottom);
	}

	public static float actionShakeFixed(float rangeBottom, float rangeTop)
	{
		return rangeBottom + random(0, 1) * (rangeTop - rangeBottom);
	}

	/**
	 * @param magnitude
	 * @param value
	 * @param params
	 *            (optional) can be null<br>
	 *            0 = period [.3f]
	 * @return
	 */
	public static float actionPunch(float magnitude, float value,
			float... params)
	{
		float s = 9;
		if (value == 0) { return 0; }
		if (value == 1) { return 0; }

		// params
		float period = .3f; // default
		if (params != null && params.length >= 1)
		{
			period = params[0];
		}

		s = (float) (period / (2 * PI) * asinStrict(0));
		return (float) (magnitude * pow(2, -10 * value) * sin((value * 1 - s)
				* (2 * PI) / period));
	}

	public static float MaxAbsValueWithSign(float... floats)
	{
		float max = Float.NEGATIVE_INFINITY;
		boolean positive = false;
		for (int i = 0; i < floats.length; i++)
		{
			if (abs(floats[i]) > max)
			{
				max = abs(floats[i]);
				positive = (floats[i] > 0) ? true : false;
			}
		}
		return max * ((positive) ? 1 : -1);
	}

	public static float MinAbsValueWithSign(float... floats)
	{
		float min = Float.POSITIVE_INFINITY;
		boolean positive = false;
		for (int i = 0; i < floats.length; i++)
		{
			if (abs(floats[i]) > min)
			{
				min = abs(floats[i]);
				positive = (floats[i] > 0) ? true : false;
			}
		}
		return min * ((positive) ? 1 : -1);
	}

	public static float sign(float num)
	{
		return (num > 0) ? 1 : -1;
	}

	public static int sign(int num)
	{
		return (num > 0) ? 1 : -1;
	}

	/**
	 * Constrains degrees from 0 to 360.
	 * 
	 * @param degrees
	 * @return
	 */
	public static float ConstrainDegrees360(float degrees)
	{
		degrees = degrees % 360;
		if (degrees < 0)
		{
			degrees += 360;
		}
		return degrees;
	}

	/**
	 * Constrains degrees from -180 to 180.<br>
	 * This is helpful when you want the degrees to be centered around 0.
	 * 
	 * @param degrees
	 * @return
	 */
	public static float ConstrainDegrees180(float degrees)
	{
		// now degrees are between 0 and 360
		degrees = ConstrainDegrees360(degrees);

		if (degrees > 180)
		{
			degrees -= 360;
		}
		return degrees;
	}

	public static float GetArcLength(float degrees, float radius)
	{
		return (PI * 2 * radius) * (degrees / 360f);
	}

	/**
	 * @param point
	 * @param pivot
	 * @param angle
	 * @param output
	 *            the point that is now rotated, can be same object as (point or pivot)
	 * @return
	 */
	public static Vector2 RotateAroundPoint(Vector2 point, Vector2 pivot,
			float angle, Vector2 output)
	{
		return RotateAroundPoint(point.x, point.y, pivot.x, pivot.y, angle,
				output);
	}

	/**
	 * @param pointX
	 * @param pointY
	 * @param pivotX
	 * @param pivotY
	 * @param angle
	 * @param output
	 *            the point that is now rotated
	 * @return
	 */
	public static Vector2 RotateAroundPoint(float pointX, float pointY,
			float pivotX, float pivotY, float angle, Vector2 output)
	{
		// Center the point around the origin
		output.set(pointX - pivotX, pointY - pivotY);

		// Rotate the point
		output.rotate(angle);

		// Move the point back to its original offset.
		output.add(pivotX, pivotY);

		return output;
	}

	/**
	 * Returns the transformed point.
	 * 
	 * @param angle
	 *            (rotates around (0,0)
	 * @param scale
	 * @param position
	 * @param output
	 * @return
	 */
	public static Vector2 TransformPoint(float angle, Vector2 scale,
			Vector2 position, Vector2 output)
	{
		return TransformPoint(angle, scale.x, scale.y, position.x, position.y,
				output);
	}

	/**
	 * Returns the transformed point.
	 * 
	 * @param angle
	 *            (rotates around (0,0)
	 * @param scaleX
	 * @param scaleY
	 * @param posX
	 * @param posY
	 * @param output
	 * @return
	 */
	public static Vector2 TransformPoint(float angle, float scaleX,
			float scaleY, float posX, float posY, Vector2 output)
	{
		// set output as the offset
		output.set(posX, posY);

		// multiply the rotated offset by the rotated scale
		output.scl(scaleX, scaleY);

		// rotate the offset
		output.rotate(angle);

		return output;
	}

	/**
	 * @param rect
	 * @param transformMatrix
	 * @param container
	 *            needs to have a size of 8, if null, creates one.
	 * @return
	 */
	public static float[] TransformRectangle(Rectangle rect,
			Matrix3 transformMatrix, float[] container)
	{
		float[] points = GetRectFourCorners(rect, container);
		Vector2 tmp = CheckoutTmpV2Internal0();
		for (int i = 0; i < 4; i++)
		{
			tmp.set(points[i * 2], points[i * 2 + 1]);
			tmp.mul(transformMatrix);
			points[i * 2] = tmp.x;
			points[i * 2 + 1] = tmp.y;
		}
		ReturnTmpV2Internal0();
		return points;
	}

	/**
	 * Returns the values of a rectanle's points after a rotation and scale
	 * 
	 * @param rect
	 * @param rotation
	 * @param scaleX
	 * @param scaleY
	 * @param container
	 *            needs to have a size of 8, if null, creates one
	 * @return
	 */
	public static float[] TransformRectangle(Rectangle rect, float rotation,
			float scaleX, float scaleY, float[] container)
	{
		if (container == null) container = new float[8];

		Vector2 tmp = CheckoutTmpV2Internal0();
		float width = rect.width * scaleX;
		float height = rect.height * scaleY;
		float rectX = rect.x + rect.width / 2;
		float rectY = rect.y + rect.height / 2;

		// TOP LEFT //
		tmp.set(-(width / 2), (height / 2));
		LttlMath.TransformPoint(rotation, LttlMath.One, tmp, tmp);
		container[0] = tmp.x + rectX;
		container[1] = tmp.y + rectY;

		// TOP RIGHT //
		tmp.set((width / 2), (height / 2));
		LttlMath.TransformPoint(rotation, LttlMath.One, tmp, tmp);
		container[2] = tmp.x + rectX;
		container[3] = tmp.y + rectY;

		// BOTTOM RIGHT //
		tmp.set((width / 2), -(height / 2));
		LttlMath.TransformPoint(rotation, LttlMath.One, tmp, tmp);
		container[4] = tmp.x + rectX;
		container[5] = tmp.y + rectY;

		// BOTTOM LEFT //
		tmp.set(-(width / 2), -(height / 2));
		LttlMath.TransformPoint(rotation, LttlMath.One, tmp, tmp);
		container[6] = tmp.x + rectX;
		container[7] = tmp.y + rectY;

		ReturnTmpV2Internal0();

		return container;
	}

	public static Vector2Array TransformPoints(Vector2Array points,
			Matrix3 transformMatrix)
	{
		points.transformPoints(transformMatrix);
		return points;
	}

	/**
	 * transforms points in array and updates them in same array
	 * 
	 * @param points
	 * @param transformMatrix
	 * @return
	 */
	public static float[] TransformPoints(float[] points,
			Matrix3 transformMatrix)
	{
		// make sure even
		Lttl.Throw(!LttlMath.isEven(points.length));

		Vector2 tmp = new Vector2();
		for (int i = 0, n = points.length / 2; i < n; i++)
		{
			// transform point
			tmp.set(points[i * 2], points[i * 2 + 1]);
			tmp.mul(transformMatrix);
			// set back to array
			points[i * 2] = tmp.x;
			points[i * 2 + 1] = tmp.y;
		}
		return points;
	}

	public static ArrayList<Vector2> TransformPointsList(
			ArrayList<Vector2> inputList, ArrayList<Vector2> outputList,
			Matrix3 transformMatrix)
	{
		if (outputList == null)
		{
			outputList = new ArrayList<Vector2>();
		}

		for (int i = 0; i < inputList.size(); i++)
		{
			Vector2 tmp = CheckoutTmpV2Internal0();
			// transform point
			tmp.set(inputList.get(i).mul(transformMatrix));

			// add point to output list or overwrite one that is already there
			if (outputList.size() <= i)
			{
				// need to add new vector
				outputList.add(new Vector2(tmp));
			}
			else
			{
				outputList.get(i).set(tmp);
			}
			ReturnTmpV2Internal0();
		}
		return outputList;
	}

	/**
	 * Populates the container Rectangle with all the points given.
	 * 
	 * @param container
	 * @param points
	 * @return
	 */
	public static Rectangle getRect(Rectangle container, float[] points)
	{
		// need at least one point
		Lttl.Throw(points.length < 2);

		container.set(points[0], points[1], 0, 0);

		for (int i = 2, n = points.length; i < n; i += 2)
		{
			container.merge(points[i], points[i + 1]);
		}

		return container;
	}

	public static Vector2Array GetRectFourCorners(Rectangle rect,
			Vector2Array container)
	{
		container.clear();
		container.ensureCapacity(4);
		container.add(rect.x, rect.y);
		container.add(rect.x + rect.width, rect.y);
		container.add(rect.x + rect.width, rect.y + rect.height);
		container.add(rect.x, rect.y + rect.height);
		return container;
	}

	/**
	 * Retutns an array of the 4 points to this rectangle.
	 * 
	 * @param rect
	 * @param container
	 *            needs to have a size of 8, if null, creates one
	 * @return float array
	 */
	public static float[] GetRectFourCorners(Rectangle rect, float[] container)
	{
		if (container == null) container = new float[8];

		container[0] = rect.x;
		container[1] = rect.y;
		container[2] = rect.x + rect.width;
		container[3] = rect.y;
		container[4] = rect.x + rect.width;
		container[5] = rect.y + rect.height;
		container[6] = rect.x;
		container[7] = rect.y + rect.height;

		return container;
	}

	/**
	 * Returns an array of the 4 points to this rectangle.
	 * 
	 * @param rect
	 * @return float array
	 */
	public static float[] GetRectFourCorners(Rectangle rect)
	{
		return GetRectFourCorners(rect, new float[8]);
	}

	/**
	 * Returns a axis aligned rectangle that includes all the points. NOTE: it may be better to use GDX's Polygon class
	 * if not using this for a circle.
	 * 
	 * @param pointsShared
	 * @param output
	 *            if null, will create one
	 * @return may return a rect with 0 width/height if not enough values to create rect
	 */
	public static Rectangle GenerateBoundingRect(float[] values,
			Rectangle output)
	{
		float minX = Float.POSITIVE_INFINITY;
		float minY = Float.POSITIVE_INFINITY;
		float maxX = Float.NEGATIVE_INFINITY;
		float maxY = Float.NEGATIVE_INFINITY;

		for (int i = 0; i < values.length; i += 2)
		{
			float x = values[i];
			float y = values[i + 1];
			if (x < minX) minX = x;
			if (x > maxX) maxX = x;

			if (y < minY) minY = y;
			if (y > maxY) maxY = y;
		}

		float width = maxX - minX;
		float height = maxY - minY;

		if (output == null)
		{
			output = new Rectangle();
		}

		output.set(minX, minY, width, height);

		return output;
	}

	public static Matrix3 ShearX(Matrix3 mat, float shear)
	{
		if (shear == 0) return mat;

		Matrix3 tmpM3 = CheckoutTmpM3Internal();
		tmpM3.val[Matrix3.M00] = 1;
		tmpM3.val[Matrix3.M10] = 0;
		tmpM3.val[Matrix3.M20] = 0;

		tmpM3.val[Matrix3.M01] = shear;
		tmpM3.val[Matrix3.M11] = 1;
		tmpM3.val[Matrix3.M21] = 0;

		tmpM3.val[Matrix3.M02] = 0;
		tmpM3.val[Matrix3.M12] = 0;
		tmpM3.val[Matrix3.M22] = 1;

		mat.mul(tmpM3);

		ReturnTmpM3Internal();
		return mat;
	}

	public static Matrix3 ShearY(Matrix3 mat, float shear)
	{
		if (shear == 0) return mat;

		Matrix3 tmpM3 = CheckoutTmpM3Internal();
		tmpM3.val[Matrix3.M00] = 1;
		tmpM3.val[Matrix3.M10] = shear;
		tmpM3.val[Matrix3.M20] = 0;

		tmpM3.val[Matrix3.M01] = 0;
		tmpM3.val[Matrix3.M11] = 1;
		tmpM3.val[Matrix3.M21] = 0;

		tmpM3.val[Matrix3.M02] = 0;
		tmpM3.val[Matrix3.M12] = 0;
		tmpM3.val[Matrix3.M22] = 1;

		mat.mul(tmpM3);

		ReturnTmpM3Internal();
		return mat;
	}

	/**
	 * Postmultiplies this matrix with a (counter-clockwise) rotation matrix.
	 * 
	 * @param mat
	 *            the matrix to multiply
	 * @param degrees
	 *            the angle in degrees
	 * @param lookup
	 *            should use lookup table?
	 * @return This matrix for the purpose of chaining.
	 */
	public static Matrix3 Rotate(Matrix3 mat, float degrees, boolean lookup)
	{
		return RotateRad(mat, MathUtils.degreesToRadians * degrees, lookup);
	}

	public static Vector2 GetMidpoint(Vector2 v0, Vector2 v1, Vector2 output)
	{
		return output.set(v0).add(v1).scl(1 / 2f);
	}

	public static boolean isEven(float num)
	{
		return isEqual(num % 2, 0);
	}

	public static boolean isEven(int num)
	{
		return num % 2 == 0;
	}

	static public boolean isEqual(Vector2 a, Vector2 b)
	{
		return isEqual(a.x, b.x) && isEqual(a.y, b.y);
	}

	public static Rectangle GetAABB(Vector2Array points, Rectangle output)
	{
		return points.getAABB(output);
	}

	/**
	 * Sets a rectangle object that includes all the points
	 * 
	 * @param points
	 *            (at least 2 points)
	 * @param output
	 *            (if null return new Rectangle object)
	 * @return
	 */
	public static Rectangle GetAABB(float[] points, Rectangle output)
	{
		if (points.length < 4 || !isEven(points.length))
		{
			Lttl.Throw("Points need to be even and at least 2 points.");
		}

		if (output == null) output = new Rectangle();

		float minX = points[0];
		float minY = points[1];
		float maxX = minX;
		float maxY = minY;

		for (int i = 0; i < points.length; i += 2)
		{
			float x = points[i];
			float y = points[i + 1];
			if (x < minX)
			{
				minX = x;
			}
			if (x > maxX)
			{
				maxX = x;
			}
			if (y < minY)
			{
				minY = y;
			}
			if (y > maxY)
			{
				maxY = y;
			}
		}

		float width = maxX - minX;
		float height = maxY - minY;
		output.set(minX, minY, width, height);

		return output;
	}

	/**
	 * Returns the set Matrix3 that has all of these transforms.
	 * 
	 * @param position
	 * @param scale
	 *            can be null if no scale (1,1)
	 * @param origin
	 *            can be null if no null (0,0)
	 * @param rotation
	 * @param rotationLookup
	 * @param shear
	 *            can be null if no shear (0,0)
	 * @param parentTransform
	 *            the parent transform to derive the parentScale and parentShear values from, should have world values
	 *            updated and accurate, can be null
	 * @param output
	 * @return
	 */
	public static Matrix3 GenerateTransormMatrix(Vector2 position,
			Vector2 scale, Vector2 origin, float rotation,
			boolean rotationLookup, Vector2 shear,
			LttlTransform parentTransform, Matrix3 output)
	{
		return GenerateTransormMatrix(position, scale, origin, rotation,
				rotationLookup, shear, parentTransform == null ? One
						: parentTransform.getWorldScale(false), output);
	}

	/**
	 * Returns the set Matrix3 that has all of these transforms.
	 * 
	 * @param position
	 * @param scale
	 *            can be null if no scale (1,1)
	 * @param origin
	 *            can be null if no origin (0,0)
	 * @param rotation
	 *            in degrees
	 * @param rotationLookup
	 *            if rotation exists, should it use a lookup table to apply rotation
	 * @param shear
	 *            can be null if no shear (0,0)
	 * @param parentScale
	 *            if this matrix is going to be multiplied to another matrix transform (parent tranform), then specify
	 *            it's scale so rotation can be calcualted accurately by compensating. Rotation should not be affected
	 *            by scale.<br>
	 *            Can be left null, will default to (1,1)
	 * @param output
	 * @return
	 */
	public static Matrix3 GenerateTransormMatrix(Vector2 position,
			Vector2 scale, Vector2 origin, float rotation,
			boolean rotationLookup, Vector2 shear, Vector2 parentScale,
			Matrix3 output)
	{
		return GenerateTransormMatrix(position.x, position.y, scale == null ? 1
				: scale.x, scale == null ? 1 : scale.y, origin == null ? 0
				: origin.x, origin == null ? 0 : origin.y, rotation,
				rotationLookup, shear == null ? 0 : shear.x, shear == null ? 0
						: shear.y, parentScale == null ? 1 : parentScale.x,
				parentScale == null ? 1 : parentScale.y, output);
	}

	/**
	 * Returns the set Matrix3 that has all of these transforms.
	 * 
	 * @param posX
	 * @param posY
	 * @param scaleX
	 *            default 1
	 * @param scaleY
	 *            default 1
	 * @param originX
	 *            default 0
	 * @param originY
	 *            default 0
	 * @param rotation
	 *            in degrees
	 * @param rotationLookup
	 *            if rotation exists, should it use a lookup table to apply rotation
	 * @param shearX
	 *            default 0
	 * @param shearY
	 *            default 0
	 * @param parentTransform
	 *            the parent transform to derive the parentScale values from, should have world values updated and
	 *            accurate, can be null
	 * @param parentShearX
	 * @param parentShearY
	 * @param output
	 * @return
	 */
	public static Matrix3 GenerateTransormMatrix(float posX, float posY,
			float scaleX, float scaleY, float originX, float originY,
			float rotation, boolean rotationLookup, float shearX, float shearY,
			LttlTransform parentTransform, Matrix3 output)
	{
		return GenerateTransormMatrix(
				posX,
				posY,
				scaleX,
				scaleY,
				originX,
				originY,
				rotation,
				rotationLookup,
				shearX,
				shearY,
				parentTransform == null ? 1 : parentTransform
						.getWorldScale(false).x, parentTransform == null ? 1
						: parentTransform.getWorldScale(false).y, output);
	}

	/**
	 * Returns the set Matrix3 that has all of these transforms.
	 * 
	 * @param posX
	 * @param posY
	 * @param scaleX
	 *            default 1
	 * @param scaleY
	 *            default 1
	 * @param originX
	 *            default 0
	 * @param originY
	 *            default 0
	 * @param rotation
	 *            in degrees
	 * @param rotationLookup
	 *            if rotation exists, should it use a lookup table to apply rotation
	 * @param shearX
	 *            default 0
	 * @param shearY
	 *            default 0
	 * @param parentScaleX
	 *            if this matrix is going to be multiplied to another matrix transform (parent tranform), then specify
	 *            it's scale so rotation can be calcualted accurately by compensating. Rotation should not be affected
	 *            by scale.<br>
	 *            Can make 1 if no parent.
	 * @param parentScaleY
	 *            Can make 1 if no parent.
	 * @param output
	 * @return
	 */
	public static Matrix3 GenerateTransormMatrix(float posX, float posY,
			float scaleX, float scaleY, float originX, float originY,
			float rotation, boolean rotationLookup, float shearX, float shearY,
			float parentScaleX, float parentScaleY, Matrix3 output)
	{
		// all of the transformations have to happen in this order
		output.idt();
		if (rotation != 0 || shearX != 0 || shearY != 0)
		{
			if (originX != 0 || originY != 0)
			{
				// offset so rotation is around origin
				output.translate(originX * scaleX, originY * scaleY);
			}

			// don't let parent matrix (scale) affect the rotation
			if (parentScaleX != 1 || parentScaleY != 1)
			{
				// undo parent scale
				output.scale(1 / parentScaleX, 1 / parentScaleY);
			}

			// rotate by post-multiplying, optionally use lookup values
			if (rotation != 0)
			{
				Rotate(output, rotation, rotationLookup);
			}

			if (parentScaleX != 1 || parentScaleY != 1)
			{
				// redo parent scale
				output.scale(parentScaleX, parentScaleY);
			}

			// shear
			if (shearX != 0)
			{
				LttlMath.ShearX(output, shearX);
			}
			if (shearY != 0)
			{
				LttlMath.ShearY(output, shearY);
			}

			if (originX != 0 || originY != 0)
			{
				// return back to original position, need to post multiply so it takes the rotation that was just done
				// into consideration
				output.translate(-originX * scaleX, -originY * scaleY);
			}
		}

		// post mutliply scale (takes rotation into consideration)
		if (scaleX != 1 || scaleY != 1)
		{
			output.scale(scaleX, scaleY);
		}

		// now apply origin which is pre multiplied so it doesn't take rotation into consideration, scale adjustment is
		// added manually
		if (originX != 0 || originY != 0)
		{
			output.trn(-originX * scaleX, -originY * scaleY);
		}

		// translate position (pre-multiply, without modifying the other columns, just moves position without caring
		// about rotation or scale)
		output.trn(posX, posY);

		return output;
	}

	/* RANDOM */

	/**
	 * Generates a random position inside the Rectangle and saves it in the output Vector2.
	 * 
	 * @param rect
	 * @param output
	 * @return
	 */
	public static Vector2 RandomPositionRectangle(Rectangle rect, Vector2 output)
	{
		return output.set(LttlMath.random(rect.x, rect.x + rect.width),
				LttlMath.random(rect.y, rect.y + rect.height));
	}

	/**
	 * generates a random position in a circle/ellipse by using random functions
	 * 
	 * @param width
	 * @param height
	 * @param output
	 * @return
	 */
	static public Vector2 RandomPositionInElipse(float width, float height,
			Vector2 output)
	{
		float radiusX = width / 2;
		float radiusY = height / 2;
		Lttl.Throw(radiusX == 0 || radiusY == 0);
		float scaleY = radiusX / (float) radiusY;
		// hack to avoid doing square root
		float radius2 = radiusX * radiusX;
		while (true)
		{
			float px = LttlMath.random(-radiusX, radiusX);
			float py = LttlMath.random(-radiusX, radiusX);
			// hypoteneus with square root hack
			if (px * px + py * py <= radius2)
			{
				output.x = px;
				output.y = py / scaleY;
				return output;
			}
		}
	}

	/**
	 * Returns a value between -width and width along the normal distribution curve.
	 * 
	 * @param width
	 * @return
	 */
	public static float RandomNormalDistribution(float width)
	{
		// will be given a value between -3 and 3
		return ((float) getRandom().nextGaussian() / 3) * width;
	}

	/**
	 * Returns the Random objecet being used.
	 * 
	 * @return
	 */
	public static Random getRandom()
	{
		return MathUtils.random;
	}

	/**
	 * Returns a random number between 0 and 1, where the probability is based on the random number. <br>
	 * ie. .1f has 10% probably of being returned, etc.
	 * 
	 * @return
	 */
	public static float RandomMonteCarlo()
	{
		// We do this “forever” until we find a qualifying random value.
		while (true)
		{
			// Pick a random value.
			float r1 = random(1);
			// Assign a probability.
			float probability = r1;
			// Pick a second random value.
			float r2 = random(1);

			// [full] Does it qualify? If so, we’re done!
			if (r2 < probability) { return r1; }
		}
	}

	public static int RandomSign()
	{
		if (randomBoolean())
		{
			return -1;
		}
		else
		{
			return 1;
		}
	}

	/**
	 * Picks from a weighted list
	 * 
	 * @param weights
	 *            several POSITIVE floats, symbolizing the weights
	 * @return return the index of the random one
	 */
	public static int RandomWeightPick(float... weights)
	{
		Lttl.Throw(weights.length < 1);
		if (weights.length == 1) return 0;

		float sum = sum(weights);
		float rand = LttlMath.random(sum);
		float cumul = 0;
		float prev = 0;
		for (int i = 0; i < weights.length; i++)
		{
			cumul += weights[i];
			if (rand > prev && rand <= cumul) { return i; }
			prev = cumul;
		}
		// HUH, this shouldn't ever happen
		Lttl.Throw();
		return -1;
	}

	public static float sum(float... nums)
	{
		float sum = 0;
		for (float f : nums)
		{
			sum += f;
		}
		return sum;
	}

	public static int sum(int... nums)
	{
		int sum = 0;
		for (int i : nums)
		{
			sum += i;
		}
		return sum;
	}

	public static float pow2(float num)
	{
		return num * num;
	}

	private static final AtomicLong seedUniquifier = new AtomicLong(
			8682522807148012L);

	/**
	 * Returns a unique long which should can be used with random() to set the seed of it.
	 * 
	 * @return
	 */
	public static long generateSeed()
	{
		long seed;
		for (;;)
		{
			long current = seedUniquifier.get();
			long next = current * 181783497276652981L;
			if (seedUniquifier.compareAndSet(current, next))
			{
				seed = next;
				break;
			}
		}
		seed ^= System.nanoTime();
		return seed;
	}

	private static float catmullRomSplineInterp(float a, float b, float c,
			float d, float t)
	{
		float t1 = (c - a) * 0.5f;
		float t2 = (d - b) * 0.5f;

		float h1 = +2 * t * t * t - 3 * t * t + 1;
		float h2 = -2 * t * t * t + 3 * t * t;
		float h3 = t * t * t - 2 * t * t + t;
		float h4 = t * t * t - t * t;

		return b * h1 + c * h2 + t1 * h3 + t2 * h4;
	}

	/**
	 * Returns a float on a spline from given values (1 dimention) and interpolation.
	 * 
	 * @param t
	 *            the interpolation (0 - 1)
	 * @param values
	 *            the x and y values
	 * @param closedShape
	 *            connects last point with first so it's smooth
	 * @return
	 */
	public static float catmullRomSplineInterp(float t, float[] values,
			boolean closedShape)
	{
		float segmentT = (values.length - ((closedShape) ? 0 : 1)) * t;
		int segment = floor(segmentT);
		t = segmentT - segment;
		int a = catmullPrep(segment - 1, closedShape, values.length);
		int b = catmullPrep(segment, closedShape, values.length);
		int c = catmullPrep(segment + 1, closedShape, values.length);
		int d = catmullPrep(segment + 2, closedShape, values.length);

		return catmullRomSplineInterp(values[a], values[b], values[c],
				values[d], t);
	}

	/**
	 * Generates the number of points (steps) by turning the given points into a catmull rom spline.
	 * 
	 * @param points
	 * @param segmentSteps
	 *            number of steps per segment
	 * @return
	 */
	public static float[] catmullRomSpline(float[] points, int segmentSteps,
			boolean closedShape)
	{
		// split points into an x and y array
		float[] xs = new float[points.length / 2];
		float[] ys = new float[points.length / 2];
		for (int i = 0; i < points.length; i += 2)
		{
			xs[i / 2] = points[i];
			ys[i / 2] = points[i + 1];
		}

		// define step size
		int segments = xs.length;
		float t = 0;
		float step = 1f / segmentSteps;
		float[] arr = new float[segments * segmentSteps * 2];

		// iterate through all points
		for (int i = 0; i < xs.length; i++)
		{
			t = 0;
			for (int ii = 0; ii < segmentSteps; ii++)
			{
				int a = catmullPrep(i - 1, closedShape, xs.length);
				int b = catmullPrep(i, closedShape, xs.length);
				int c = catmullPrep(i + 1, closedShape, xs.length);
				int d = catmullPrep(i + 2, closedShape, xs.length);

				arr[i * segmentSteps * 2 + ii * 2] = catmullRomSplineInterp(
						xs[a], xs[b], xs[c], xs[d], t);
				arr[i * segmentSteps * 2 + ii * 2 + 1] = catmullRomSplineInterp(
						ys[a], ys[b], ys[c], ys[d], t);
				t += step; // step forward
			}
		}

		return arr;
	}

	/**
	 * prepares indexes
	 * 
	 * @param v
	 * @param closedShape
	 * @param arrayLength
	 * @return
	 */
	private static int catmullPrep(int v, boolean closedShape, int arrayLength)
	{
		if (closedShape)
		{
			if (v < 0)
			{
				v += arrayLength;
			}
			else if (v > arrayLength - 1)
			{
				v -= arrayLength;
			}
			return v;
		}
		else
		{
			return clamp(v, 0, arrayLength - 1);
		}
	}

	/**
	 * Sets a position on a spline from given points and interpolation.
	 * 
	 * @param t
	 *            the interpolation (0 - 1)
	 * @param points
	 *            the x and y values
	 * @param pointCount
	 *            how many steps will make up the entire spline
	 * @param output
	 *            ouptut vector
	 * @return
	 */
	public static Vector2 catmullRomSplineInterp(float t, float[] points,
			boolean closedShape, Vector2 output)
	{
		// split points into an x and y array
		float[] xs = new float[points.length / 2];
		float[] ys = new float[points.length / 2];
		for (int i = 0; i < points.length; i += 2)
		{
			xs[i] = points[i];
			xs[i + 1] = points[i + 1];
		}

		float x = catmullRomSplineInterp(t, xs, closedShape);
		float y = catmullRomSplineInterp(t, ys, closedShape);

		return output.set(x, y);
	}

	/**
	 * Adds a points to a rect.
	 * 
	 * @param rect
	 * @param x
	 * @param y
	 * @return
	 */
	public static Rectangle addPointToRectangle(Rectangle rect, float x, float y)
	{
		float minX = min(x, rect.x);
		float maxX = max(rect.x + rect.width, x);
		rect.x = minX;
		rect.width = maxX - minX;

		float minY = min(y, rect.y);
		float maxY = max(rect.y + rect.height, y);
		rect.y = minY;
		rect.height = maxY - minY;
		return rect;
	}

	/**
	 * Checks if a rectangle contains any or all of the points
	 * 
	 * @param rect
	 * @param mustBeAll
	 *            if true, it will only return true if ALL the points are within rect, otherwise it will return true if
	 *            any are found
	 * @param points
	 * @return
	 */
	public static boolean rectangleContainsPoints(Rectangle rect,
			boolean mustBeAll, float... points)
	{
		for (int i = 0; i < points.length; i += 2)
		{
			if (rect.contains(points[i], points[i + 1]))
			{
				if (!mustBeAll) { return true; }
				continue;
			}
			else if (mustBeAll) { return false; }
		}
		return (mustBeAll) ? true : false;
	}

	/*SPLINES*/

	// CATMULL-ROM SPLINE//
	public static Vector2 CatSplineInterp(Vector2Array pts, float t,
			Vector2 result)
	{
		int numSections = pts.size() - 3;
		int currPt = min(floor(t * numSections), numSections - 1);
		// float u = t * (float) numSections - (float) currPt;

		float aX = pts.getX(currPt); // left handle X
		float aY = pts.getY(currPt); // left handle Y
		float bX = pts.getX(currPt + 1); // left point X
		float bY = pts.getY(currPt + 1); // left point Y
		float cX = pts.getX(currPt + 2); // right point X
		float cY = pts.getY(currPt + 2); // right point Y
		float dX = pts.getX(currPt + 3); // right handle X
		float dY = pts.getY(currPt + 3); // right handle Y

		result.x = CatSplineInterpCalc(t, aX, bX, cX, dX);
		result.y = CatSplineInterpCalc(t, aY, bY, cY, dY);

		return result;
	}

	private static float CatSplineInterpCalc(float u, float a, float b,
			float c, float d)
	{
		return 0.5f * ((2 * b) + (-a + c) * u + (2 * a - 5 * b + 4 * c - d) * u
				* u + (-a + 3 * b - 3 * c + d) * u * u * u);
	}

	// TODO
	/*
	// CATMULL-ROM SPLINE (DERIVATIVE)//
	//
	public static Vector3 CatSplineInterpDeriv(Vector3[] pts, float t)
	{
		int numSections = pts.Length - 3;
		int currPt = Mathf.Min(Mathf.FloorToInt(t * (float) numSections),
				numSections - 1);
		float u = t * (float) numSections - (float) currPt;

		Vector3 a = pts[currPt]; // left handle
		Vector3 b = pts[currPt + 1]; // left point
		Vector3 c = pts[currPt + 2]; // right point
		Vector3 d = pts[currPt + 3]; // right handle

		return .5f * (3f * (-a + 3f * b - 3f * c + d) * (u * u) + 2f
				* (2f * a - 5f * b + 4f * c - d) * u + (-a + c));
		// return .5f*((-a+c) + 2f*(2f*a - 5f*b + 4f*c - d)*u + 3f*(-a + 3f*b - 3f*c + d)*(u*u));
	}

	// CATMULL-ROM SPLINE (2nd DERIVATIVE [ACCELERATION]) (UNTESTED)//
	//
	public static Vector3 CatSplineInterpAccel(Vector3[] pts, float t)
	{
		int numSections = pts.Length - 3;
		int currPt = Mathf.Min(Mathf.FloorToInt(t * (float) numSections),
				numSections - 1);
		float u = t * (float) numSections - (float) currPt;

		Vector3 a = pts[currPt]; // left handle
		Vector3 b = pts[currPt + 1]; // left point
		Vector3 c = pts[currPt + 2]; // right point
		Vector3 d = pts[currPt + 3]; // right handle

		return .5f * (6f * (-a + 3f * b - 3f * c + d) * u + 2f * (2f * a - 5f
				* b + 4f * c - d));
	}

	// HERMITE SPLINE//
	// http://www.unifycommunity.com/wiki/index.php?title=Hermite_Spline_Controller
	public static Vector3 HermiteSplineInterp(Vector3[] points, float t)
	{
		float t2 = t * t;
		float t3 = t2 * t;

		Vector3 P0 = points[0];
		Vector3 P1 = points[1];
		Vector3 P2 = points[2];
		Vector3 P3 = points[3];

		float tension = 0.5f; // 0.5 equivale a catmull-rom

		Vector3 T1 = tension * (P2 - P0);
		Vector3 T2 = tension * (P3 - P1);

		float Blend1 = 2 * t3 - 3 * t2 + 1;
		float Blend2 = -2 * t3 + 3 * t2;
		float Blend3 = t3 - 2 * t2 + t;
		float Blend4 = t3 - t2;

		return Blend1 * P1 + Blend2 * P2 + Blend3 * T1 + Blend4 * T2;
	}
	*/

	/* NOTE: VECTORS */

	/**
	 * Postmultiplies this matrix with a (counter-clockwise) rotation matrix.
	 * 
	 * @param mat
	 *            the matrix to multiply
	 * @param radians
	 *            the angle in radians
	 * @param lookup
	 *            should use lookup table?
	 * @return This matrix for the purpose of chaining.
	 */
	public static Matrix3 RotateRad(Matrix3 mat, float radians, boolean lookup)
	{
		// early outs
		if (!lookup) return mat.rotateRad(radians);
		if (radians == 0) return mat;

		// get lookup values
		float cos = cos(radians);
		float sin = sin(radians);

		Matrix3 tmpM3 = CheckoutTmpM3Internal();
		tmpM3.val[Matrix3.M00] = cos;
		tmpM3.val[Matrix3.M10] = sin;
		tmpM3.val[Matrix3.M20] = 0;

		tmpM3.val[Matrix3.M01] = -sin;
		tmpM3.val[Matrix3.M11] = cos;
		tmpM3.val[Matrix3.M21] = 0;

		tmpM3.val[Matrix3.M02] = 0;
		tmpM3.val[Matrix3.M12] = 0;
		tmpM3.val[Matrix3.M22] = 1;

		mat.mul(tmpM3);

		ReturnTmpM3Internal();
		return mat;
	}

	/**
	 * Rotates a vector2, really only necessary if need for lookup, which is only if doing millions of calculations.<br>
	 * Did benchmark for non lookup got 1.5 million with 55 fps, without lookup was > 700 million
	 * 
	 * @param v
	 *            vector2 to rotate
	 * @param degrees
	 * @param lookup
	 *            should use lookup table for trig?
	 * @return updated vector
	 */
	public static Vector2 Rotate(Vector2 v, float degrees, boolean lookup)
	{
		if (lookup)
		{
			float radians = degrees * degreesToRadians;
			float cos = cos(radians);
			float sin = sin(radians);

			float newX = v.x * cos - v.y * sin;
			float newY = v.x * sin + v.y * cos;

			v.x = newX;
			v.y = newY;
		}
		else
		{
			v.rotate(degrees);
		}

		return v;
	}

	/**
	 * Returns the counter-clockwise angle (in degrees) of the vector2 relative to x axis, (1,0) is 0 degrees. The
	 * returned angle will always be between 0 and 360.
	 * 
	 * @param v
	 * @param lookup
	 *            uses lookup table
	 * @return
	 */
	public static float GetAngle(Vector2 v, boolean lookup)
	{
		return GetAngle(v.x, v.y, lookup);
	}

	/**
	 * Returns the counter-clockwise angle (in degrees) of the vector2 relative to x axis, (1,0) is 0 degrees. The
	 * returned angle will always be between 0 and 360.
	 * 
	 * @param x
	 * @param y
	 * @param lookup
	 *            uses lookup table
	 * @return
	 */
	public static float GetAngle(float x, float y, boolean lookup)
	{
		float angle;
		if (lookup)
		{
			angle = atan2(y, x);
		}
		else
		{
			angle = (float) Math.atan2(y, x);
		}
		angle *= radiansToDegrees;
		if (angle < 0) angle += 360;
		return angle;
	}

	/**
	 * Returns the counter-clockwise angle between two points from the origin point's perspective, relative to x axis,
	 * (1,0) is 0 degrees. The returned angle will always be between 0 and 360.
	 * 
	 * @param originX
	 * @param originY
	 * @param pointX
	 * @param pointY
	 * @param lookup
	 *            uses lookup table
	 * @return
	 */
	public static float GetAngleBetweenPoints(float originX, float originY,
			float pointX, float pointY, boolean lookup)
	{
		return GetAngle(pointX - originX, pointY - originY, lookup);
	}

	/**
	 * Returns the counter-clockwise angle between two points from the origin point's perspective, relative to x axis,
	 * (1,0) is 0 degrees. The returned angle will always be between 0 and 360.
	 * 
	 * @param origin
	 * @param point
	 * @param lookup
	 *            uses lookup table
	 * @return
	 */
	public static float GetAngleBetweenPoints(Vector2 origin, Vector2 point,
			boolean lookup)
	{
		return GetAngleBetweenPoints(origin.x, origin.y, point.x, point.y,
				lookup);
	}

	/**
	 * Good for getting the angle of a vector relative to some other vector. In other words, getting the angle of vector
	 * as if you were already facing in the direction of reference.<br>
	 * This is different than {@link #GetAngleBetweenPoints(Vector2, Vector2, boolean)}, because this uses direction not
	 * positions.
	 * 
	 * @see Vector2#angle(Vector2)
	 */
	static public float GetAngleRelative(Vector2 vector, Vector2 reference,
			boolean lookup)
	{
		if (lookup)
		{
			// same as
			// LttlMath.ConstrainDegrees180(GetAngle(reference, true)
			// - GetAngle(vector, true));
			return atan2(vector.crs(reference), vector.dot(reference))
					* MathUtils.radiansToDegrees;
		}
		else
		{
			return vector.angle(reference);
		}
	}

	/**
	 * Returns a float array of the min and max values [minX,minY,maxX,maxY]
	 * 
	 * @param list
	 * @return
	 * @throws emptyList
	 */
	static public float[] getVectorsMinMax(ArrayList<Vector2> list)
	{
		Lttl.Throw(list.size() == 0);

		// init
		float minX = list.get(0).x;
		float minY = list.get(0).y;
		float maxX = minX;
		float maxY = minY;

		for (Vector2 v : list)
		{
			if (v.x < minX)
			{
				minX = v.x;
			}
			if (v.y < minY)
			{
				minY = v.y;
			}
			if (v.x > maxX)
			{
				maxX = v.x;
			}
			if (v.y > maxY)
			{
				maxY = v.y;
			}
		}

		return new float[]
		{ minX, minY, maxX, maxY };
	}

	/**
	 * Returns a float array of the min and max values [minX,minY,maxX,maxY]
	 * 
	 * @param array
	 * @return
	 * @throws emptyArray
	 */
	static public float[] GetVectorsMinMax(Vector2Array array)
	{
		return array.getMinMax();
	}

	/**
	 * Calculates the center point between the given vectors
	 * 
	 * @param list
	 * @param output
	 * @return
	 * @throws emptyList
	 */
	static public Vector2 GetVectorsCenter(ArrayList<Vector2> list,
			Vector2 output)
	{
		Lttl.Throw(list.size() == 0);

		float[] minMax = getVectorsMinMax(list);
		float minX = minMax[0];
		float minY = minMax[1];
		float maxX = minMax[2];
		float maxY = minMax[3];
		float width = maxX - minX;
		float height = maxY - minY;

		return output.set(minX + width / 2, minY + height / 2);
	}

	/**
	 * Calculates the center point between the given vectors
	 * 
	 * @param array
	 * @param output
	 * @return
	 * @throws empty
	 */
	static public Vector2 GetVectorsCenter(Vector2Array array, Vector2 output)
	{
		return array.getCenter(output);
	}

	/**
	 * Sets the lengths of a vector without changing direction
	 * 
	 * @param v
	 * @param len
	 * @return
	 */
	static public Vector2 VectorLengthSet(Vector2 v, float len)
	{
		return v.nor().scl(len);
	}

	/**
	 * Modifies the length of the vector without changing the direction
	 * 
	 * @param v
	 * @param adjust
	 * @return
	 */
	static public Vector2 VectorLengthAdjust(Vector2 v, float adjust)
	{
		float len = v.len();
		return v.nor().scl(len + adjust);
	}

	/**
	 * Gets the Y value for x on this segment. Must exist.
	 * 
	 * @param aX
	 * @param aY
	 * @param bX
	 * @param bY
	 * @param x
	 * @return
	 * @throws throws if x is not between (inclusive) of aX and bX
	 */
	static public float getSegmentY(float aX, float aY, float bX, float bY,
			float x)
	{
		Lttl.Throw(!isBetween(x, aX, bX));

		if (aY == bY) { return aY; }

		float m = (bY - aY) / (bX - aX);
		float b = aY - (aX * m);

		return m * x + b;
	}

	/**
	 * Gets the Y value for x on this segment. Must exist.
	 * 
	 * @param a
	 * @param b
	 * @param x
	 * @return
	 * @throws throws if x is not between (inclusive) of aX and bX
	 */
	static public float getSegmentY(Vector2 a, Vector2 b, float x)
	{
		return getSegmentY(a.x, a.y, b.x, b.y, x);
	}

	/**
	 * Gets the X value for y on this segment. Must exist.
	 * 
	 * @param aX
	 * @param aY
	 * @param bX
	 * @param bY
	 * @param y
	 * @return
	 * @throws throws if y is not between (inclusive) of aY and bY
	 */
	static public float getSegmentX(float aX, float aY, float bX, float bY,
			float y)
	{
		Lttl.Throw(!isBetween(y, aY, bY));

		if (aX == bX) { return aX; }

		float m = (bY - aY) / (bX - aX);
		float b = aY - (aX * m);

		return (y - b) / m;
	}

	/**
	 * Gets the X value for y on this segment. Must exist.
	 * 
	 * @param a
	 * @param b
	 * @param x
	 * @return
	 * @throws throws if y is not between (inclusive) of aY and bY
	 */
	static public float getSegmentX(Vector2 a, Vector2 b, float x)
	{
		return getSegmentX(a.x, a.y, b.x, b.y, x);
	}

	/**
	 * Loops an index. (ie. -1 will give you the last index actually and size() index will give you the first)
	 * 
	 * @param index
	 * @param size
	 *            of list or array, it will subtract 1
	 * @return
	 */
	static public int loopIndex(int index, int size)
	{
		return loopAround(index, 0, size - 1);
	}

	/**
	 * Loops the given number around the min and max values. This is useful for list indexes that go negative and should
	 * loopAround. (ie. num: -6, min: -5, max: 5, would give you 5... not 4)
	 * 
	 * @param num
	 * @param min
	 * @param max
	 * @return
	 */
	static public int loopAround(int num, int min, int max)
	{
		num = num % (max - min + 1);
		if (num < min)
		{
			return num + 1 - min + max;
		}
		else if (num > max)
		{
			return num - 1 - max + min;
		}
		else
		{
			return num;
		}
	}

	/**
	 * The angle (degrees) from a to b, and it takes into consideration if it goes around 360 and if the numbers only
	 * range from 0 to 360.
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	static public float angleBetweenAround(float a, float b)
	{
		return angleRadBetweenAround(a * degreesToRadians, b * degreesToRadians)
				* radiansToDegrees;
	}

	/**
	 * The angle (radian) from a to b, and it takes into consideration if it goes around 2PI and if the numbers only
	 * range from 0 to 2PI.
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	static public float angleRadBetweenAround(float a, float b)
	{
		a = a % LttlMath.PI2;
		b = b % LttlMath.PI2;

		// if b is bigger, then just get difference
		if (b >= a)
		{
			return b - a;
		}
		// if b is smaller, then b must have gone around 360, so get whats left of a to 360 and all of b
		else
		{
			return LttlMath.PI2 - a + b;
		}
	}

	/**
	 * Generates a circle. The first point will not be returned twice.
	 * 
	 * @param radius
	 * @param steps
	 *            need at least 3
	 * @param container
	 * @return
	 */
	public static Vector2Array GenerateCirclePoints(float radius, int steps,
			Vector2Array container)
	{
		GenerateShapePoints(radius, radius, steps, 360, 0, container);
		container.removeLast();
		return container;
	}

	/**
	 * Generates an ellipse. The first point will not be returned twice.
	 * 
	 * @param radiusX
	 * @param radiusY
	 * @param steps
	 *            need at least 3
	 * @param container
	 * @return
	 */
	public static Vector2Array GenerateEllipsePoints(float radiusX,
			float radiusY, int steps, Vector2Array container)
	{
		GenerateShapePoints(radiusX, radiusY, steps, 360, 0, container);
		return container;
	}

	/**
	 * Generates all the points on a shape where all points are equal distance from center based on specified number of
	 * sides, does repeat first point if degrees are -360 or 360. There will always be one more point than number of
	 * sides.
	 * 
	 * @param radiusX
	 * @param radiusY
	 * @param sides
	 *            need at least 3
	 * @param degrees
	 *            between -360 and 360, if abs(360) then the first and last point will be the same!!<br>
	 *            If positive, will be CCW
	 * @param degreesOffset
	 *            default is 0
	 * @param container
	 * @return
	 */
	public static Vector2Array GenerateShapePoints(float radiusX,
			float radiusY, int sides, float degrees, float degreesOffset,
			Vector2Array container)
	{
		Lttl.Throw(sides < 3);

		container.clear();
		container.ensureCapacity(sides);

		degreesOffset = degreesOffset % 360;
		degrees = LttlMath.clamp(degrees, -360, 360);
		float stepSize = degrees / sides;
		// start at zero and go around
		float a = 0;
		for (int i = 0; i <= sides; i++)
		{
			float d = (a + degreesOffset) * LttlMath.degreesToRadians;
			float x = LttlMath.cos(d);
			float y = LttlMath.sin(d);

			x = x * radiusX;
			y = y * radiusY;

			container.add(x, y);

			a += stepSize;
		}

		return container;
	}
}
