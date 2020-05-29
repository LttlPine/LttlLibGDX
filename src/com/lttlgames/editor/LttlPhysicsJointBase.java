package com.lttlgames.editor;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Joint;
import com.badlogic.gdx.physics.box2d.JointDef;
import com.lttlgames.editor.LttlTransform.GuiTransformListener;
import com.lttlgames.editor.annotations.GuiButton;
import com.lttlgames.editor.annotations.GuiCallback;
import com.lttlgames.editor.annotations.GuiGroup;
import com.lttlgames.editor.annotations.Persist;

//3
@Persist(-90140)
public abstract class LttlPhysicsJointBase<T extends Joint> extends
		LttlPhysicsBase
{
	/* STATIC */
	protected static Vector2 tmp = new Vector2();

	/* MEMBER */
	@GuiCallback("destroyAndInit")
	@Persist(9014003)
	public LttlPhysicsBody bodyA;

	@GuiCallback("destroyAndInit")
	@Persist(9014000)
	public LttlPhysicsBody bodyB;

	/**
	 * will try to init on {@link LttlComponent#onEarlyUpdate()} because bodies will be created onStart()
	 */
	@GuiGroup("General")
	@Persist(9014001)
	public boolean initOnStart = true;
	private boolean attemptedInit = false;
	private boolean initNextEarlyUpdate = false;

	@GuiCallback("destroyAndInit")
	@Persist(9014002)
	public boolean collideConnected = false;

	protected T joint;
	private GuiTransformListener guiTransformListener;

	private void addGuiTransformListenerNonPlaying()
	{
		if (!Lttl.game.inEditor()) return;
		// if already exists, probably some error with destroying
		Lttl.Throw(guiTransformListener != null);

		t().addGuiListener(
				guiTransformListener = new GuiTransformListener(true)
				{
					private void doo()
					{
						// don't allow transform to change physics body when playing with physics enabled or not
						// initialized yet
						if (!isInit()
								|| (Lttl.game.getPhysics().enabled && Lttl.game
										.isPlaying())) return;
						destroyAndInit();
					}

					@Override
					public void onScale(boolean handle,
							LttlTransform sourceTransform)
					{
						doo();
					}

					@Override
					public void onRotation(boolean handle,
							LttlTransform sourceTransform)
					{
						doo();
					}

					@Override
					public void onPosition(boolean handle,
							LttlTransform ancestorChange)
					{
						doo();
					}

					@Override
					public void onOrigin(LttlTransform sourceTransform)
					{
						doo();
					}
				});
	}

	@Override
	public void onEditorCreate()
	{
		bodyA = t().getComponent(LttlPhysicsBody.class, true);
	}

	@Override
	public void onEarlyUpdate()
	{
		// only try to auto init on the first early update if initOnStart is true or if set to initNextEarlyUpdate
		if ((initNextEarlyUpdate || initOnStart) && !attemptedInit)
		{
			autoInit();
			attemptedInit = true;
			if (!isInit())
			{
				// if not init now, then throw an error, since it was meant to initOnStart
				Lttl.logNote("Could not initialize joint on start because at least one body is not initalized: "
						+ toString());
			}
		}
		initNextEarlyUpdate = false;
	}

	@Override
	public void onEditorEarlyUpdate()
	{
		// always try to initialize joint when in editor, as soon as both bodies are initialized
		autoInit();
	}

	/**
	 * checks if not already init and if it has everything needed to complete an initialization, if not, then just does
	 * nothing
	 */
	private void autoInit()
	{
		if (!isInit() && bodyB != null && bodyB.isInit() && bodyA != null
				&& bodyA.isInit())
		{
			initInternal(bodyA, bodyB);
			addGuiTransformListenerNonPlaying();
		}
	}

	@Override
	public boolean isInit()
	{
		return joint != null;
	}

	/**
	 * Initializes/creates the join, both this join
	 * 
	 * @throws RuntimeException
	 *             if already initialized
	 */
	@GuiButton(order = 2)
	public void init()
	{
		Lttl.Throw(isInit());
		autoInit();
		// check if still not init, then probably an error
		if (!isInit())
		{
			Lttl.logNote("Joint initialization failed because at least one of the bodies are not initialized.");
		}
	}

	/**
	 * This is the actual initialization and creation of joint. Body components can be assumed to initialized.
	 */
	protected abstract void initInternal(LttlPhysicsBody bodyCompA,
			LttlPhysicsBody bodyCompB);

	protected void processJointDefinition(JointDef def)
	{
		def.collideConnected = collideConnected;
	}

	/**
	 * Returns the body component at A
	 */
	@Override
	public LttlPhysicsBody getBodyComp()
	{
		if (!isInit()) return null;

		return LttlPhysicsHelper.getBodyCompA(joint);
	}

	/**
	 * Returns the body component at B
	 */
	public LttlPhysicsBody getBodyCompB()
	{
		if (!isInit()) return null;

		return LttlPhysicsHelper.getBodyCompB(joint);
	}

	@Override
	public void destroyAndInit()
	{
		destroyPhysics();
		init();
	}

	@Override
	public void destroyPhysics()
	{
		// if not initialized, then just skip destroying
		if (!isInit()) return;

		// process destroy callbacks on both bodies
		getBodyComp().processDestroyCallbacks(this);
		getBodyCompB().processDestroyCallbacks(this);

		// save reference to joint before cleaning up
		Joint joint = getJoint();
		cleanUp();

		// destroy joint/remove from physics world
		Lttl.game.getPhysics().getWorld().destroyJoint(joint);
	}

	public T getJoint()
	{
		return joint;
	}

	@Override
	void cleanUp()
	{
		if (Lttl.game.inEditor() && guiTransformListener != null)
		{
			t().removeGuiListener(guiTransformListener);
			guiTransformListener = null;
		}
		joint = null;
	}

	/**
	 * Marks this joint to try and initialize on the next {@link LttlComponent#onEarlyUpdate()}. This is mainly used
	 * when destroy and initing one or both bodies and want the joint to auto init ASAP. The only problem with this is
	 * if the destroyInit is being done between two steps or in the EarlyUpdate, in those cases, just init the joint
	 * manually after both bodies have been initialized and disregard this method.
	 */
	public void initNextEarlyUpdate()
	{
		initNextEarlyUpdate = true;
		attemptedInit = false;
	}

	final void onGuiChange()
	{
		if (!isInit()) return;

		updateSettingsOnJoint();
	}

	/**
	 * all settings on joint will be updated on actual physics joint, if initialized<br>
	 * it seems to be more efficient to not check if the values are different.
	 */
	abstract public void updateSettingsOnJoint();
}
