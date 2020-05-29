package com.lttlgames.editor;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.helpers.LttlMath;

//4
@Persist(-90133)
public class LttlPhysicsRect extends LttlPhysicsFixture
{
	private static Rectangle rect = new Rectangle();

	@Persist(9013300)
	@GuiCallback("onGuiShape")
	public float sizeX = 4;
	@Persist(9013301)
	@GuiCallback("onGuiShape")
	public float sizeY = 4;
	@Persist(9013304)
	@GuiCallback("onGuiShape")
	public boolean isSquare = false;
	@Persist(9013302)
	@GuiCallback("onGuiShape")
	public boolean useOrigin = true;

	@Override
	public void onEditorCreate()
	{
		sizeX *= Lttl.game.getSettings().getWidthFactor();
		sizeY *= Lttl.game.getSettings().getWidthFactor();
	}

	@Override
	public void getShapes(Vector2 bodyOrigin)
	{
		t().updateWorldValues();

		rect.width = sizeX;
		rect.height = isSquare ? sizeX : sizeY;
		float centerX = (useOrigin ? -t().originRenderMesh.x : 0);
		float centerY = (useOrigin ? -t().originRenderMesh.y : 0);
		rect.setCenter(centerX, centerY);
		LttlMath.GetRectFourCorners(rect, pointsShared);

		processRadiusBuffer(pointsShared);

		pointsShared.mulAll(t().getWorldTransform(true))
				.sclAll(Lttl.game.getPhysics().scaling)
				.offsetAll(-bodyOrigin.x, -bodyOrigin.y);

		// set the polygonShape
		PolygonShape shape = Lttl.game.getPhysics().getPolygonShapePool()
				.obtain();
		shape.set(pointsShared.toArray());
		shape.setRadius(radius);

		shapesListShared.add(shape);
	}
}
