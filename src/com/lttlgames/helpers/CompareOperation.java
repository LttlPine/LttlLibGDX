package com.lttlgames.helpers;

import com.lttlgames.editor.annotations.Persist;
import com.vividsolutions.jts.geom.Geometry;

@Persist(-90136)
public enum CompareOperation
{
	/**
	 * @see {@link Geometry#contains(Geometry)}
	 */
	CONTAINS, /**
	 * @see {@link Geometry#crosses(Geometry)}
	 */
	CROSSES, /**
	 * @see {@link Geometry#disjoint(Geometry)}
	 */
	DISJOINT, /**
	 * @see {@link Geometry#equalsTopo(Geometry)}
	 */
	EQUALS_TOPO, /**
	 * @see {@link Geometry#equalsExact(Geometry)}
	 */
	EQUALS_EXACT, /**
	 * @see {@link Geometry#intersects(Geometry)}
	 */
	INTERSECTS, /**
	 * @see {@link Geometry#overlaps(Geometry)}
	 */
	OVERLAPS, /**
	 * @see {@link Geometry#touches(Geometry)}
	 */
	TOUCHES
}
