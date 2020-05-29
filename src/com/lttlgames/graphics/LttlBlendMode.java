package com.lttlgames.graphics;

import com.lttlgames.editor.annotations.Persist;

@Persist(-9042)
public enum LttlBlendMode
{
	// PREDEFINED BLENDS
	NONE, ALPHA, ONE_MULTIPLY, /**
	 * Requires textures to be premultiplied with alpha
	 */
	MULTIPLY, ADDITIVE, DST_ALPHA_MASK, DST_INVERSE_ALPHA_MASK, REPLACE, /**
	 * Require textures to be premultiplied with
	 * alpha
	 */
	SCREEN
}
