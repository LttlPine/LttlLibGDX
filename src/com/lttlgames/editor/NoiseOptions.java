package com.lttlgames.editor;

import com.lttlgames.editor.annotations.Persist;

@Persist(-9072)
public class NoiseOptions
{
	@Persist(907201)
	public float stepSizePerSecond = 2f;
	@Persist(907202)
	public int lod = 2;
	@Persist(907203)
	public float startRange = -1;
	@Persist(907204)
	public float endRange = 1;
	@Persist(907205)
	public boolean smooth = false;
	@Persist(907206)
	public boolean fadeIn = true;
	@Persist(907207)
	public boolean fadeOut = true;
}
