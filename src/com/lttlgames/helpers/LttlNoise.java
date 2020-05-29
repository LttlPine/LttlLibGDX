package com.lttlgames.helpers;

import java.util.Random;

import com.badlogic.gdx.graphics.Color;
import com.lttlgames.editor.Lttl;

/**
 * Almost entirely derived from Processing.
 * 
 * @author Josh
 */
public class LttlNoise
{
	public static final LttlNoise staticNoise = new LttlNoise();

	private Random noiseRandom;
	static final int PERLIN_YWRAPB = 4;
	static final int PERLIN_YWRAP = 1 << PERLIN_YWRAPB;
	static final int PERLIN_ZWRAPB = 8;
	static final int PERLIN_ZWRAP = 1 << PERLIN_ZWRAPB;
	static final int PERLIN_SIZE = 4095;

	// params
	private int perlin_octaves = 4; // default to medium smooth
	private float perlin_amp_falloff = 0.5f; // 50% reduction/octave

	int perlin_TWOPI, perlin_PI;
	float[] perlin;
	private float currentStep = 0;
	private float stepSize = .006f;
	private boolean smooth = true;

	public LttlNoise()
	{
		// create unique random generator
		noiseRandom = new Random();

		// set some standard crap
		perlin_TWOPI = perlin_PI = LttlMath.SINCOS_LENGTH;
		perlin_PI >>= 1;
	}

	public void resetStep()
	{
		currentStep = 0;
	}

	/**
	 * Sets if should use cosine smoothing to make the transitions a bit more smooth. Default is true.<br>
	 * <br>
	 * <b>Note:</b> Disabling smooth is a lot more efficient.
	 * 
	 * @param isSmooth
	 * @return
	 */
	public LttlNoise setSmooth(boolean isSmooth)
	{
		this.smooth = isSmooth;
		return this;
	}

	/**
	 * Smaller means smoother changes, while larger will give very jagged changes. Used when calling getNextStep().
	 * 
	 * @param stepSize
	 *            default .006, small means less dramatic changes
	 * @return self for chaining
	 */
	public LttlNoise setStepSize(float stepSize)
	{
		this.stepSize = stepSize;
		return this;
	}

	public float getNextStep(float start, float end)
	{
		float n = noise(currentStep, 0, 0, start, end);
		currentStep += stepSize;
		return n;
	}

	public float getNextStep()
	{
		float n = noise(currentStep, 0, 0);
		currentStep += stepSize;
		return n;
	}

	// optimized by limiting the smooth calls noise_fsc

	/**
	 * <b>Warning:</b> better off running once and getting what you need and saving that OR tween to the result and evey
	 * time it ends, get the next noise value.<br>
	 * <br>
	 * Returns the Perlin noise value at specified coordinates. Perlin noise is a random sequence generator producing a
	 * more natural ordered, harmonic succession of numbers compared to the standard <b>random()</b> function. It was
	 * invented by Ken Perlin in the 1980s and been used since in graphical applications to produce procedural textures,
	 * natural motion, shapes, terrains etc.<br>
	 * <br>
	 * The main difference to the <b>random()</b> function is that Perlin noise is defined in an infinite n-dimensional
	 * space where each pair of coordinates corresponds to a fixed semi-random value (fixed only for the lifespan of the
	 * program). The resulting value will always be between 0.0 and 1.0. Processing can compute 1D, 2D and 3D noise,
	 * depending on the number of coordinates given. The noise value can be animated by moving through the noise space
	 * as demonstrated in the example above. The 2nd and 3rd dimension can also be interpreted as time.<br>
	 * <br>
	 * The actual noise is structured similar to an audio signal, in respect to the function's use of frequencies.
	 * Similar to the concept of harmonics in physics, perlin noise is computed over several octaves which are added
	 * together for the final result. <br>
	 * <br>
	 * Another way to adjust the character of the resulting sequence is the scale of the input coordinates. As the
	 * function works within an infinite space the value of the coordinates doesn't matter as such, only the distance
	 * between successive coordinates does (eg. when using <b>noise()</b> within a loop). As a general rule the smaller
	 * the difference between coordinates, the smoother the resulting noise sequence will be. Steps of 0.005-0.03 work
	 * best for most applications, but this will differ depending on use.
	 * 
	 * @param x
	 * @param start
	 * @param end
	 * @return a value between 0, 1
	 */
	public float noise1D(float x, float start, float end)
	{
		return noise(x, 0, 0, start, end);
	}

	/**
	 * <b>Warning:</b> better off running once and getting what you need and saving that OR tween to the result and evey
	 * time it ends, get the next noise value.<br>
	 * <br>
	 * Returns the Perlin noise value at specified coordinates. Perlin noise is a random sequence generator producing a
	 * more natural ordered, harmonic succession of numbers compared to the standard <b>random()</b> function. It was
	 * invented by Ken Perlin in the 1980s and been used since in graphical applications to produce procedural textures,
	 * natural motion, shapes, terrains etc.<br>
	 * <br>
	 * The main difference to the <b>random()</b> function is that Perlin noise is defined in an infinite n-dimensional
	 * space where each pair of coordinates corresponds to a fixed semi-random value (fixed only for the lifespan of the
	 * program). The resulting value will always be between 0.0 and 1.0. Processing can compute 1D, 2D and 3D noise,
	 * depending on the number of coordinates given. The noise value can be animated by moving through the noise space
	 * as demonstrated in the example above. The 2nd and 3rd dimension can also be interpreted as time.<br>
	 * <br>
	 * The actual noise is structured similar to an audio signal, in respect to the function's use of frequencies.
	 * Similar to the concept of harmonics in physics, perlin noise is computed over several octaves which are added
	 * together for the final result. <br>
	 * <br>
	 * Another way to adjust the character of the resulting sequence is the scale of the input coordinates. As the
	 * function works within an infinite space the value of the coordinates doesn't matter as such, only the distance
	 * between successive coordinates does (eg. when using <b>noise()</b> within a loop). As a general rule the smaller
	 * the difference between coordinates, the smoother the resulting noise sequence will be. Steps of 0.005-0.03 work
	 * best for most applications, but this will differ depending on use.
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @return a value between 0, 1
	 */
	public float noise(float x, float y, float z)
	{
		if (perlin == null)
		{
			generatePerlinArray();
		}

		if (x < 0) x = -x;
		if (y < 0) y = -y;
		if (z < 0) z = -z;

		int xi = (int) x, yi = (int) y, zi = (int) z;
		float xf = x - xi;
		float yf = y - yi;
		float zf = z - zi;
		float rxf, ryf;

		float r = 0;
		float ampl = 0.5f;

		float n1, n2, n3;

		for (int i = 0; i < perlin_octaves; i++)
		{
			int of = xi + (yi << PERLIN_YWRAPB) + (zi << PERLIN_ZWRAPB);

			rxf = (xf <= 0) ? xf : noise_fsc(xf);
			ryf = (yf <= 0) ? yf : noise_fsc(yf);

			n1 = perlin[of & PERLIN_SIZE];
			n1 += rxf * (perlin[(of + 1) & PERLIN_SIZE] - n1);
			n2 = perlin[(of + PERLIN_YWRAP) & PERLIN_SIZE];
			n2 += rxf * (perlin[(of + PERLIN_YWRAP + 1) & PERLIN_SIZE] - n2);
			n1 += ryf * (n2 - n1);

			of += PERLIN_ZWRAP;
			n2 = perlin[of & PERLIN_SIZE];
			n2 += rxf * (perlin[(of + 1) & PERLIN_SIZE] - n2);
			n3 = perlin[(of + PERLIN_YWRAP) & PERLIN_SIZE];
			n3 += rxf * (perlin[(of + PERLIN_YWRAP + 1) & PERLIN_SIZE] - n3);
			n2 += ryf * (n3 - n2);

			n1 += ((zf <= 0) ? 1 : noise_fsc(zf)) * (n2 - n1);

			r += n1 * ampl;
			ampl *= perlin_amp_falloff;
			xi <<= 1;
			xf *= 2;
			yi <<= 1;
			yf *= 2;
			zi <<= 1;
			zf *= 2;

			if (xf >= 1.0f)
			{
				xi++;
				xf--;
			}
			if (yf >= 1.0f)
			{
				yi++;
				yf--;
			}
			if (zf >= 1.0f)
			{
				zi++;
				zf--;
			}
		}

		return r;
	}

	/**
	 * I think this smoothes stuff out, maybe could optionally remove this if wanted more jitter???
	 * 
	 * @param i
	 * @return
	 */
	private float noise_fsc(float i)
	{
		if (smooth)
		{
			i = 0.5f * (1.0f - LttlMath.cosLUT[(int) (i * perlin_PI)
					% perlin_TWOPI]);
		}
		return i;
	}

	/**
	 * @param x
	 * @param y
	 * @param z
	 * @param start
	 *            start range (inclusive)
	 * @param end
	 *            end range(exclusive)
	 * @return
	 */
	public float noise(float x, float y, float z, float start, float end)
	{
		float noise = noise(x, y, z);
		return start + (noise) * (end - start);
	}

	/**
	 * make perlin noise quality user controlled to allow for different levels of detail. lower values will produce
	 * smoother results as higher octaves are surpressedAdjusts the character and level of detail produced by the Perlin
	 * noise function.<br>
	 * <br>
	 * Similar to harmonics in physics, noise is computed over several octaves. Lower octaves contribute more to the
	 * output signal and as such define the overal intensity of the noise, whereas higher octaves create finer grained
	 * details in the noise sequence. By default, noise is computed over 4 octaves with each octave contributing exactly
	 * half than its predecessor, starting at 50% strength for the 1st octave. This falloff amount can be changed by
	 * adding an additional function parameter. Eg. a falloff factor of 0.75 means each octave will now have 75% impact
	 * (25% less) of the previous lower octave. Any value between 0.0 and 1.0 is valid, however note that values greater
	 * than 0.5 might result in greater than 1.0 values returned by <b>noise()</b>.<br />
	 * <br
	   * />
	 * By changing these parameters, the signal created by the <b>noise()</b> function can be adapted to fit very
	 * specific needs and characteristics.
	 * 
	 * @param lod
	 *            number of octaves to be used by the noise number of octaves to be used by the noise <b>at least 2</b>
	 *            [default 4]
	 * @param falloff
	 *            falloff factor for each octave
	 * @return self for chaining
	 */
	public LttlNoise setDetail(int lod)
	{
		perlin_octaves = LttlMath.max(2, lod);
		return this;
	}

	/**
	 * @param lod
	 *            number of octaves to be used by the noise at least 2 [default 4]
	 * @param falloff
	 *            falloff factor for each octave [default .5]
	 * @return self for chaining
	 */
	public LttlNoise setDetail(int lod, float falloff)
	{
		perlin_octaves = LttlMath.max(2, lod);
		if (falloff > 0) perlin_amp_falloff = falloff;
		return this;
	}

	/**
	 * Sets the seed value for <b>noise()</b>. By default, <b>noise()</b> produces different results each time the
	 * program is run. Set the <b>value</b> parameter to a constant to return the same pseudo-random numbers each time
	 * the software is run.
	 * 
	 * @param seed
	 *            seed value
	 */
	public void setNoiseSeed(long seed)
	{
		if (noiseRandom == null)
		{
			noiseRandom = new Random(seed);
		}
		else
		{
			noiseRandom.setSeed(seed);
		}
		// force table reset after changing the random number seed
		perlin = null;
	}

	private void generatePerlinArray()
	{
		// populate the perlin noise random nodes
		perlin = new float[PERLIN_SIZE + 1];
		for (int i = 0; i < PERLIN_SIZE + 1; i++)
		{
			perlin[i] = noiseRandom.nextFloat();
		}
	}

	/**
	 * Generates an array of noise that can be looped through, good for interpolation.
	 * 
	 * @param startStep
	 *            this is the starting step, normally this should be 0, animate this to move along the noise (can
	 *            generate lots of noise from same LttlNoise), or put in some random positive number (not too big) to go
	 *            to some random place on noise, so it can be reused<br>
	 *            Will use the set stepSize for this.
	 * @param size
	 *            number of points
	 * @param start
	 *            range
	 * @param end
	 *            range
	 * @param fadePercentage
	 *            how much of the array will be fading back to start for looping, 0 means no fade
	 * @return
	 */
	public float[] generateNoiseArray(float startStep, int size, float start,
			float end, float fadePercentage)
	{
		fadePercentage = LttlMath.Clamp01(fadePercentage);
		int fadeStartIndex = LttlMath.floor((size - 1) * (1 - fadePercentage));
		float fadeIndexRange = (size - 1) - fadeStartIndex;

		float s = startStep;
		float[] arr = new float[size];
		for (int i = 0; i < arr.length; i++)
		{
			float n = noise(s, 0, 0, start, end);

			if (fadePercentage > 0 && i >= fadeStartIndex)
			{
				n = LttlMath.interp(n, arr[0], (i - fadeStartIndex)
						/ fadeIndexRange, EaseType.QuadInOut);
			}

			arr[i] = n;
			s += stepSize;
		}
		return arr;
	}

	/**
	 * @param startStep
	 *            this is the starting step, normally this should be 0, animate this to move along the noise (can
	 *            generate lots of noise from same LttlNoise), or put in some random positive number to go to some
	 *            random place on noise random positive value
	 * @param xStep
	 *            how far in game units will the points be apart
	 * @param xOffset
	 *            a number added to each x position
	 * @param start
	 *            this is the range of y values start
	 * @param end
	 *            this is the range of y values end
	 * @param numPoints
	 *            how many points to generate
	 * @param fadePercentage
	 *            how much of the array will be fading back to start for looping, 0 means no fade
	 * @param color
	 *            color of line
	 * @param width
	 *            width of line
	 * @return the points used to draw noise array too
	 */
	public float[] drawNoise(float startStep, float xStep, float xOffset,
			float start, float end, int numPoints, float fadePercentage,
			Color color, float width)
	{
		float[] points = generateNoisePointsArray(startStep, xStep, xOffset,
				start, end, numPoints, fadePercentage);
		Lttl.debug.drawLines(points, width, false, color);
		return points;
	}

	/**
	 * @param startStep
	 *            this is the starting step, normally this should be 0, animate this to move along the noise (can
	 *            generate lots of noise from same LttlNoise), or put in some random positive number to go to some
	 *            random place on noise random positive value
	 * @param xStep
	 *            how far in game units will the points be apart
	 * @param xOffset
	 *            a number added to each x position
	 * @param start
	 *            this is the range of y values start
	 * @param end
	 *            this is the range of y values end
	 * @param numPoints
	 *            how many points to generate
	 * @param fadePercentage
	 *            how much of the array will be fading back to start for looping, 0 means no fade
	 * @return
	 */
	public float[] generateNoisePointsArray(float startStep, float xStep,
			float xOffset, float start, float end, int numPoints,
			float fadePercentage)
	{
		startStep = LttlMath.abs(startStep);
		float[] noiseArr = generateNoiseArray(startStep, numPoints, start, end,
				fadePercentage);
		float[] points = new float[noiseArr.length * 2];
		float x = 0;
		for (int i = 0; i < points.length; i += 2)
		{
			points[i] = x + xOffset;
			points[i + 1] = noiseArr[i / 2];
			x += xStep;
		}

		return points;
	}
}
