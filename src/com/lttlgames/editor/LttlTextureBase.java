package com.lttlgames.editor;

import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.Persist;

@Persist(-9019)
abstract class LttlTextureBase
{
	LttlTextureBase()
	{

	}

	@GuiCallback("refresh")
	@Persist(901901)
	public String textureRegionName = "";

	public abstract void clearReference();
}
