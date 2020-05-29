package com.lttlgames.editor;

import com.lttlgames.editor.annotations.Persist;

@Persist(-9082)
public class LttlPathControlPointStatePropertyGroup extends StatePropertyGroup
{
	@Persist(908200)
	public StatePropertyFloat posX = new StatePropertyFloat(true);
	@Persist(908201)
	public StatePropertyFloat posY = new StatePropertyFloat(true);
	@Persist(908202)
	public StatePropertyFloat leftPosX = new StatePropertyFloat(true);
	@Persist(908203)
	public StatePropertyFloat leftPosY = new StatePropertyFloat(true);
	@Persist(908204)
	public StatePropertyFloat rightPosX = new StatePropertyFloat(true);
	@Persist(908205)
	public StatePropertyFloat rightPosY = new StatePropertyFloat(true);
}
