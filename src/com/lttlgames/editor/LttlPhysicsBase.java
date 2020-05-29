package com.lttlgames.editor;

import com.lttlgames.components.interfaces.PhysicsListener;
import com.lttlgames.editor.annotations.GuiButton;
import com.lttlgames.editor.annotations.Persist;

@Persist(-90135)
public abstract class LttlPhysicsBase extends LttlComponent
{
	/**
	 * Returns if this has successfully been initialized into the physics world and has not been destroyed, which
	 * ensures certain methods will return expected results (non null).
	 */
	public abstract boolean isInit();

	/**
	 * {@link #destroyPhysics()} and tries to init. May not init if {@link PhysicsController#isStepping()} when this is
	 * called.
	 */
	@GuiButton(order = 0)
	public abstract void destroyAndInit();

	/**
	 * Destroys the physics object (removing from the physics world), cleans up any references, and processes any
	 * callbacks for onDestroyPhysics.
	 */
	@GuiButton(order = 1)
	public abstract void destroyPhysics();

	/**
	 * May return null if not initialized.
	 * 
	 * @return
	 */
	public abstract LttlPhysicsBody getBodyComp();

	/**
	 * Cleans up references on the component after the optional
	 * {@link PhysicsListener#onDestroyPhysics(LttlPhysicsBase)} callback but before the actual removal of the physics
	 * object from the physics world. That way any callbacks triggerd from destroying this physics object will show that
	 * is it not initialized, which is accurate.<br>
	 * In brief, running this will make {@link #isInit()} return false, and this is the <b>ONLY</b> place that should
	 * make a {@link LttlPhysicsBase} unintialize. <br>
	 * This does NOT clear the user object saved on the physics object, since that is still necessary behind the scene.
	 */
	abstract void cleanUp();
}
