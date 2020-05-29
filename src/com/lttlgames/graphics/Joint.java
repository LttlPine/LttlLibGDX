package com.lttlgames.graphics;

import com.lttlgames.editor.annotations.Persist;
import com.vividsolutions.jts.operation.buffer.BufferParameters;

@Persist(-9041)
public enum Joint
{
	MITER(BufferParameters.JOIN_MITRE), BEVEL(BufferParameters.JOIN_BEVEL), ROUND(
			BufferParameters.JOIN_ROUND);

	private final int value;

	private Joint(int i)
	{
		value = i;
	}

	public int getValue()
	{
		return value;
	}
}