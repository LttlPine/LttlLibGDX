package com.lttlgames.editor;

import com.badlogic.gdx.graphics.Color;
import com.lttlgames.components.LttlCameraController;
import com.lttlgames.components.LttlFpsUpdater;
import com.lttlgames.components.LttlGuiContainerComponent;
import com.lttlgames.components.LttlParticleEmitter;
import com.lttlgames.components.LttlQuadMeshGenerator;
import com.lttlgames.components.LttlSpriteAnimator;
import com.lttlgames.graphics.LttlBlendMode;
import com.lttlgames.graphics.LttlShader;

public class LttlObjectFactory
{
	public static LttlTransform AddObject(ObjectType type, LttlScene scene,
			String name)
	{
		LttlTransform newTransform = AddObjectNoCallback(type, scene, name);

		// process onstart callback
		ComponentHelper.callBackTransformTree(newTransform,
				ComponentCallBackType.onStart);

		return newTransform;
	}

	/**
	 * Creates a full object based on ObjectType.
	 * 
	 * @param type
	 * @param scene
	 * @param name
	 * @return
	 */
	static LttlTransform AddObjectNoCallback(ObjectType type, LttlScene scene,
			String name)
	{
		switch (type)
		{
			case Standard:
				return scene.addNewTransformNoCallback(name);
			case SpriteAnimation:
				return AddSpriteAnimation(scene, name);
			case BasicTexture:
				return AddBasicTexture(scene, name);
			case CameraController:
				return AddCameraController(scene, name);
			case Text:
				return AddText(scene, name);
			case FpsViewer:
				return AddFpsViewer(scene, name);
			case GuiContainer:
				return AddGuiContainer(scene, name);
			case CustomShape:
				return AddCustomShape(scene, name);
			case ParticleEmitter:
				return AddParticleEmitter(scene, name);
			case BasicQuad:
				return AddBasicQuad(scene, name);
			default:
				break;

		}
		return null;
	}

	/* OBJECT TYPE FUNCTIONS */
	// NOTE: be sure to use addNewTransformNoCallback()

	private static LttlTransform AddText(LttlScene scene, String name)
	{
		LttlTransform lt = scene.addNewTransformNoCallback(name);
		lt.addComponent(LttlRenderer.class);
		lt.addComponent(LttlFontGenerator.class);
		lt.renderer().shader = LttlShader.TextureShader;
		((LttlFontGenerator) lt.r().generator()).refreshFont();
		return lt;
	}

	private static LttlTransform AddBasicTexture(LttlScene scene, String name)
	{
		LttlTransform lt = scene.addNewTransformNoCallback(name);
		lt.addComponent(LttlRenderer.class);
		lt.addComponent(LttlBasicTextureMeshGenerator.class);
		lt.renderer().shader = LttlShader.TextureShader;
		return lt;
	}

	private static LttlTransform AddSpriteAnimation(LttlScene scene, String name)
	{
		LttlTransform lt = scene.addNewTransformNoCallback(name);
		lt.addComponents(LttlRenderer.class, LttlSpriteAnimator.class);
		lt.addComponent(LttlBasicTextureMeshGenerator.class);
		lt.renderer().shader = LttlShader.TextureShader;
		return lt;
	}

	private static LttlTransform AddCameraController(LttlScene scene,
			String name)
	{
		if (name == null || name.isEmpty()) name = "_CameraController";
		LttlTransform lt = scene.addNewTransformNoCallback(name);
		lt.addComponents(LttlCameraController.class);
		return lt;
	}

	private static LttlTransform AddGuiContainer(LttlScene scene, String name)
	{
		LttlTransform lt = scene.addNewTransformNoCallback(name);
		lt.addComponents(LttlGuiContainerComponent.class);
		return lt;
	}

	private static LttlTransform AddFpsViewer(LttlScene scene, String name)
	{
		LttlTransform parent = AddGuiContainer(scene, name + "GUI Container");
		LttlTransform lt = AddText(scene, name);
		parent.setChild(lt, false);
		lt.addComponents(LttlFpsUpdater.class);
		lt.r().color.set(Color.BLACK);
		return lt;
	}

	private static LttlTransform AddCustomShape(LttlScene scene, String name)
	{
		LttlTransform lt = scene.addNewTransformNoCallback(name);
		LttlRenderer r = lt.addComponent(LttlRenderer.class);
		r.color.set(199 / 255f, 57 / 255f, 45 / 255f, 1);
		r.shader = LttlShader.SimpleColorShader;
		lt.addComponent(LttlPath.class);
		lt.addComponent(LttlCustomShape.class);
		return lt;
	}

	private static LttlTransform AddParticleEmitter(LttlScene scene, String name)
	{
		LttlTransform lt = scene.addNewTransformNoCallback(name);
		LttlRenderer r = lt.addComponent(LttlParticleEmitter.class);
		r.shader = LttlShader.TextureShader;
		r.blendMode = LttlBlendMode.ADDITIVE;
		lt.addComponent(LttlBasicTextureMeshGenerator.class);
		return lt;
	}

	private static LttlTransform AddBasicQuad(LttlScene scene, String name)
	{
		LttlTransform lt = scene.addNewTransformNoCallback(name);
		LttlRenderer r = lt.addComponent(LttlRenderer.class);
		r.shader = LttlShader.SimpleColorShader;
		r.color.set(Color.PURPLE);
		lt.addComponent(LttlQuadMeshGenerator.class);
		return lt;
	}
}
