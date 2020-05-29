package com.lttlgames.components;

import com.lttlgames.editor.Lttl;
import com.lttlgames.editor.LttlComponent;
import com.lttlgames.editor.LttlMeshGenerator;
import com.lttlgames.editor.LttlSound;
import com.lttlgames.editor.annotations.ComponentRequired;
import com.lttlgames.editor.annotations.Persist;

@Persist(-9048)
@ComponentRequired(LttlMeshGenerator.class)
public class PlaySoundOnClick extends LttlComponent
{
	@Persist(904801)
	public LttlSound sound = new LttlSound();
	@Persist(904802)
	public boolean inEditor = false;

	@Override
	public void onUpdate()
	{
		if (Lttl.input.isMousePressed()
				&& renderer().getMesh() != null
				&& renderer().getMeshBoundingRectTransformedAxisAligned()
						.contains(Lttl.input.getX(), Lttl.input.getY()))

		{
			sound.play();
		}
	}

	@Override
	public void onEditorUpdate()
	{
		if (inEditor)
		{
			onUpdate();
		}
	}
}
