package com.lttlgames.helpers;

public enum EaseType
{
	QuadIn, QuadOut, QuadInOut, CubicIn, CubicOut, CubicInOut, QuartIn, QuartOut, QuartInOut, QuintIn, QuintOut, QuintInOut, SineIn, SineOut, SineInOut, ExpoIn, ExpoOut, ExpoInOut, CircIn, CircOut, CircInOut, Linear, Spring, BounceIn, BounceOut, BounceInOut, BackIn, BackOut, BackInOut, ElasticIn, ElasticOut, ElasticInOut, /**
	 * 
	 * the target value is the addtive magnitude, it will always return to original value.<br>
	 * <br>
	 * Params (optional):<br>
	 * 0 = period [.3f]
	 */
	Punch, /**
	 * the target value is the additive magnitude
	 */
	ShakeIn, /**
	 * the target value is the additive magnitude
	 */
	ShakeOut, /**
	 * the target value is the additive magnitude
	 */
	ShakeInOut, /**
	 * the target value is the additive magnitude, neither fades in or out, constant shake
	 */
	ShakeFixed, FADE, /**
	 * Params (optional):<br>
	 * 0 = scale (how much it swings past) [5]
	 */
	SwingIn, /**
	 * Params (optional):<br>
	 * 0 = scale (how much it swings past) [5]
	 */
	SwingOut, /**
	 * Params (optional):<br>
	 * 0 = scale (how much it swings past) [5]
	 */
	SwingInOut
}