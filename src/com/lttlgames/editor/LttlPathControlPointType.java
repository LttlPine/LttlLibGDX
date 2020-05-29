package com.lttlgames.editor;

import com.lttlgames.editor.annotations.Persist;

@Persist(-9077)
public enum LttlPathControlPointType
{
	/**
	 * Uses no external influence affecting this control point's curvature
	 */
	Sharp,
	/**
	 * Uses the proximity control points to influence the point's curvature
	 */
	Proximity, /**
	 * Uses handles to influence the point's curvature
	 */
	Handles
}
