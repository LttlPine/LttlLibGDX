package com.lttlgames.components;

import com.badlogic.gdx.math.Vector2;
import com.lttlgames.editor.LttlTransform;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.graphics.LttlMeshFactory;

@Persist(-90113)
public class VertexLock
{
	@Persist(9011300)
	public LttlQuadGeneratorAbstract target;
	@Persist(9011301)
	public QuadCorners targetVertexPos = QuadCorners.TopLeft;
	@Persist(9011302)
	public Vector2 adjustment = new Vector2();

	/**
	 * Gets the vertex position from the target generator, then converts it to the render local of the to LttlTransform
	 * 
	 * @param to
	 * @param container
	 * @return
	 */
	public Vector2 getPos(LttlTransform to, Vector2 container)
	{
		// OPTIMIZE?
		target.r()
				.getMesh()
				.getPos(LttlMeshFactory.getDensifiedQuadIndex(targetVertexPos,
						target.densifySteps), container);
		target.t().renderToWorldPosition(container, true);
		to.worldToRenderPosition(container, true);
		return container;
	}
}
