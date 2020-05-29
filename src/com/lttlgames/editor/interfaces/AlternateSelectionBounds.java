package com.lttlgames.editor.interfaces;

import com.badlogic.gdx.math.Rectangle;
import com.lttlgames.helpers.LttlGeometryUtil.PolygonContainer;

public interface AlternateSelectionBounds
{
	/**
	 * Return the alternate selection AABB (world values). Not necessary to cache this since <b>seldomly ran</b>. This
	 * will be ran once when left clicked or once when left clicked drag.
	 */
	Rectangle getSelectionAABB();

	/**
	 * Return the alternate transformed selection bounding rect (world values). Not necessary to cache this since
	 * <b>seldomly ran</b>.
	 */
	float[] getSelectionBoundingRectTransformed();

	/**
	 * Return the alternate selection polygon (world values). Not necessary to cache this since <b>seldomly ran</b>.
	 * This will be ran once when left clicked or once when left clicked drag is started and it's within AABB.
	 */
	PolygonContainer getSelectionPolygon();
}
