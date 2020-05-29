package com.lttlgames.helpers;

public class LttlMutatableClass
{
	public LttlMutatableClass()
	{

	}

	public LttlMutatableClass(Class<?> startValue)
	{
		this.value = startValue;
	}

	public Class<?> value;
}
