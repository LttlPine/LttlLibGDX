package com.lttlgames.helpers;

import java.util.ArrayList;

import com.badlogic.gdx.math.Vector2;

public final class LttlLog
{
	public static void log(String m)
	{
		System.out.println(m);
	}

	public static void log(float m)
	{
		System.out.println(m);
	}

	public static void log(int m)
	{
		System.out.println(m);
	}

	public static void log(boolean m)
	{
		System.out.println(m);
	}

	public static void log(short[] m)
	{
		for (int i = 0; i < m.length; i++)
		{
			System.out.print(m[i] + ",");
		}
		System.out.println();
	}

	public static void log(float[] m)
	{
		for (int i = 0; i < m.length; i++)
		{
			System.out.print(m[i] + ",");
		}
		System.out.println();
	}

	public static void log(int[] m)
	{
		for (int i = 0; i < m.length; i++)
		{
			System.out.print(m[i] + ",");
		}
		System.out.println();
	}

	public static void log(Vector2[] m)
	{
		for (int i = 0; i < m.length; i++)
		{
			System.out.print("[" + m[i].x + "," + m[i].y + "]");
		}
		System.out.println();
	}

	public static void logListShort(ArrayList<Short> list)
	{
		for (Short s : list)
		{
			System.out.print(s + ", ");
		}
		System.out.println();
	}

	public static void logListFloat(ArrayList<Float> list)
	{
		for (float s : list)
		{
			System.out.print(s + ", ");
		}
		System.out.println();
	}

	public static void logListInteger(ArrayList<Integer> list)
	{
		for (int s : list)
		{
			System.out.print(s + ", ");
		}
		System.out.println();
	}
}
