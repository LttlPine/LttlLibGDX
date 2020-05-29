package com.lttlgames.editor;

import com.lttlgames.editor.annotations.Persist;

@Persist(-9071)
public enum EaseMode
{
	In, InOut, Out, /**
	 * Fixed means it stays at 1 the entire time
	 */
	Fixed
}
