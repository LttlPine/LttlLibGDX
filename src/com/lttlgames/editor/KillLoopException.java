package com.lttlgames.editor;

/**
 * When thrown, it immediatly stops running {@link LoopManager#loop()}. This is useful when reloading the game and
 * whenever it's called, you want to the loop to stop.
 */
@SuppressWarnings("serial")
public class KillLoopException extends RuntimeException
{

}
