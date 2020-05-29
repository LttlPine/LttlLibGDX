package com.lttlgames.exceptions;

import com.lttlgames.helpers.LttlGeometryUtil;

/**
 * An exception thrown when {@link LttlGeometryUtil#bufferPolygon} or {@link LttlGeometryUtil#bufferPath} resulted in
 * multiple polygons.
 */
@SuppressWarnings("serial")
public class MultiplePolygonsException extends Exception
{
	public MultiplePolygonsException()
	{
		super();
	}

	public MultiplePolygonsException(String message)
	{
		super(message);
	}
}
