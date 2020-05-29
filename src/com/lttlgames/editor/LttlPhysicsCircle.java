package com.lttlgames.editor;

import com.badlogic.gdx.math.Vector2;
import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.Persist;

@Persist(-90132)
public class LttlPhysicsCircle extends LttlPhysicsFixture
{
	@Persist(9013200)
	@GuiCallback("onGuiShape")
	public boolean useOrigin = true;

	@Override
	public void onEditorCreate()
	{
		radius = 3 * Lttl.game.getSettings().getWidthFactor();
	}

	@Override
	public void getShapes(Vector2 bodyOrigin)
	{
		t().updateWorldValues();

		tmp.set(useOrigin ? t().getWorldRenderPosition(false) : t()
				.getWorldPosition(false));

		Lttl.game
				.getPhysics()
				.getCircleShapeShared()
				.setPosition(
						tmp.scl(Lttl.game.getPhysics().scaling).sub(bodyOrigin));
		Lttl.game
				.getPhysics()
				.getCircleShapeShared()
				.setRadius(
						radius * t().getWorldScale(false).x
								* Lttl.game.getPhysics().scaling);

		shapesListShared.add(Lttl.game.getPhysics().getCircleShapeShared());
	}
}
