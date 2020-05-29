package com.lttlgames.editor;

import com.lttlgames.editor.annotations.Persist;

@Persist(-903)
enum ComponentCallBackType
{
	onStart(1 << 0, false, true), OnEarlyUpdate(1 << 1, true, true), onUpdate(
			1 << 2, true, true), onLateUpdate(1 << 3, true, true), onDestroy(
			1 << 4, false, false), onResize(1 << 5, true, true), DebugDraw(
			1 << 6, true, true), onSaveScene(1 << 7, false, true), onEditorCreate(
			1 << 8, false, true), onFixedUpdate(1 << 9, true, true), onLateFixedUpdate(
			1 << 10, true, true), onEnable(1 << 1, false, true), onDisable(
			1 << 12, false, true);

	private final int id;
	private final boolean checkEnabled;
	private final boolean checkDestroyPending;

	ComponentCallBackType(int id, boolean checkEnabled,
			boolean checkDestroyPending)
	{
		this.id = id;
		this.checkEnabled = checkEnabled;
		this.checkDestroyPending = checkDestroyPending;
	}

	public int getValue()
	{
		return id;
	}

	public boolean isCheckEnabled()
	{
		return checkEnabled;
	}

	public boolean isCheckDestroyPending()
	{
		return checkDestroyPending;
	}
}