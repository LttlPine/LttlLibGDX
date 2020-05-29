package com.lttlgames.graphics;

import com.lttlgames.editor.annotations.Persist;
import com.vividsolutions.jts.operation.buffer.BufferParameters;

@Persist(-9040)
public enum Cap
{
	NONE(BufferParameters.CAP_FLAT), ROUND(BufferParameters.CAP_ROUND), SQUARE(
			BufferParameters.CAP_SQUARE);

	private final int value;

	private Cap(int i)
	{
		value = i;
	}

	public int getValue()
	{
		return value;
	}
}