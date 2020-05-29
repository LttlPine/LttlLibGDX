package com.lttlgames.editor;

import com.badlogic.gdx.utils.IntMap;
import com.lttlgames.components.AlternateSelectionBoundsComponent;
import com.lttlgames.components.ColorBlendMode;
import com.lttlgames.components.CustomVertexObject;
import com.lttlgames.components.EmitterShape;
import com.lttlgames.components.FloatOption;
import com.lttlgames.components.GuiContainerAnchor;
import com.lttlgames.components.IntOption;
import com.lttlgames.components.JointSpringSettings;
import com.lttlgames.components.LttlAddComponentOnStart;
import com.lttlgames.components.LttlAnimationManager;
import com.lttlgames.components.LttlCameraController;
import com.lttlgames.components.LttlDemoComponent;
import com.lttlgames.components.LttlDemoComponent2;
import com.lttlgames.components.LttlDemoComponent3;
import com.lttlgames.components.LttlDemoComponent4;
import com.lttlgames.components.LttlFpsUpdater;
import com.lttlgames.components.LttlGuiContainerComponent;
import com.lttlgames.components.LttlMeshCopier;
import com.lttlgames.components.LttlMouseAdapter;
import com.lttlgames.components.LttlParallax;
import com.lttlgames.components.LttlParticleEmitter;
import com.lttlgames.components.LttlParticleEmitterBase;
import com.lttlgames.components.LttlParticleEmitterMultiTex;
import com.lttlgames.components.LttlParticleEmitterTexture;
import com.lttlgames.components.LttlPhysicsAdapter;
import com.lttlgames.components.LttlPhysicsMouseJoint;
import com.lttlgames.components.LttlQuadGeneratorAbstract;
import com.lttlgames.components.LttlQuadMeshGenerator;
import com.lttlgames.components.LttlQuadPoints;
import com.lttlgames.components.LttlQuadRectMeshGenerator;
import com.lttlgames.components.LttlRendererStateManager;
import com.lttlgames.components.LttlShapeGenerator;
import com.lttlgames.components.LttlSimpleGradient;
import com.lttlgames.components.LttlSpriteAnimator;
import com.lttlgames.components.LttlTextureQuadGenerator;
import com.lttlgames.components.LttlTransformStateManager;
import com.lttlgames.components.LttlTriangleGenerator;
import com.lttlgames.components.LttlTrifoldMeshGenerator;
import com.lttlgames.components.LttlUvOffsetRandomizer;
import com.lttlgames.components.MouseJointSettings;
import com.lttlgames.components.PlayMusicOnStart;
import com.lttlgames.components.PlaySoundOnClick;
import com.lttlgames.components.QuadCorners;
import com.lttlgames.components.TextureFoldObject;
import com.lttlgames.components.VertexLock;
import com.lttlgames.graphics.Cap;
import com.lttlgames.graphics.GeometryOperation;
import com.lttlgames.graphics.Joint;
import com.lttlgames.graphics.LttlBlendMode;
import com.lttlgames.graphics.LttlShader;
import com.lttlgames.helpers.CompareOperation;
import com.lttlgames.helpers.FloatRange;
import com.lttlgames.helpers.FloatRangeRandom;
import com.lttlgames.helpers.FloatRangeRandomTimeline;
import com.lttlgames.helpers.IntRange;
import com.lttlgames.helpers.IntRangeRandom;
import com.lttlgames.helpers.IntRangeRandomTimeline;
import com.lttlgames.helpers.LttlCameraTransformState;
import com.lttlgames.helpers.LttlRectangle;
import com.lttlgames.helpers.LttlTimeline;
import com.lttlgames.helpers.RangeRandomTimeline;
import com.lttlgames.helpers.Vector2Array;
import com.lttlgames.helpers.enums.HorizontalAlignment;
import com.lttlgames.helpers.enums.VerticalAlignment;
import com.lttlgames.tweenengine.RepeatTweenType;

/**
 * Meant to be extended for each game project and contain the map of persisted classes to their ids.
 * 
 * @author Josh
 */
public class LttlClassMap
{
	String packageName = null;

	/**
	 * Optionally can set the package name directly, this helps with looking up the components for this project.<br>
	 * <br>
	 * Note: This is not necessary when not in editor mode.
	 * 
	 * @param packageName
	 */
	protected LttlClassMap(String packageName)
	{
		this.packageName = packageName;
	}

	protected LttlClassMap()
	{
		// prevent accidential instantitaion
	}

	// NOTE
	// NOTE
	// NOTE
	// NOTE
	// NOTE
	// NOTE
	// NOTE
	/* MAKE SURE CLASS IS NOT BEING USED ANYWHERE IN GAME */
	// can search for Persist Id in the json or change the id to something else and see if you get errors
	// NOTE
	// NOTE
	// NOTE
	// NOTE
	// NOTE
	// NOTE
	// NOTE
	// NOTE
	public final IntMap<Class<?>> classMap = new IntMap<Class<?>>(280);
	{
		classMap.put(-901, AASettings.class);
		classMap.put(-902, AnimationType.class);
		classMap.put(-903, ComponentCallBackType.class);
		classMap.put(-904, GameState.class);
		classMap.put(-905, LttlCamera.class);
		classMap.put(-906, LttlPhysicsMouseJoint.class);
		classMap.put(-907, LttlComponent.class);
		classMap.put(-908, LttlEditorSettings.class);
		classMap.put(-909, LttlFontGenerator.class);
		classMap.put(-9010, LttlGameSettings.class);
		classMap.put(-9011, LttlMeshGenerator.class);
		classMap.put(-9012, LttlMultiRenderer.class);
		classMap.put(-9013, LttlAddComponentOnStart.class);
		classMap.put(-9014, LttlParticleEmitter.class);
		classMap.put(-9015, LttlRenderer.class);
		classMap.put(-9016, LttlSound.class);
		classMap.put(-9017, LttlTexture.class);
		classMap.put(-9018, LttlTextureAnimation.class);
		classMap.put(-9019, LttlTextureBase.class);
		classMap.put(-9020, LttlTransform.class);
		classMap.put(-9021, LttlParticleEmitterMultiTex.class);
		classMap.put(-9022, ObjectType.class);
		classMap.put(-9023, GuiContainerAnchor.class);
		classMap.put(-9024, SpriteAnimation.class);
		classMap.put(-9025, UVMeshSettings.class);
		classMap.put(-9026, LttlCameraController.class);
		classMap.put(-9027, MouseJointSettings.class);
		classMap.put(-9028, JointSpringSettings.class);
		classMap.put(-9029, LttlDemoComponent.class);
		classMap.put(-9030, LttlDemoComponent2.class);
		classMap.put(-9031, LttlDemoComponent3.class);
		classMap.put(-9032, LttlDemoComponent4.class);
		classMap.put(-9033, LttlFpsUpdater.class);
		classMap.put(-9034, LttlGuiContainerComponent.class);
		classMap.put(-9035, LttlMeshCopier.class);
		classMap.put(-9036, LttlSpriteAnimator.class);
		classMap.put(-9037, LttlTextureQuadGenerator.class);
		classMap.put(-9038, LttlShader.class);
		classMap.put(-9039, LttlMusic.class);
		classMap.put(-9040, Cap.class);
		classMap.put(-9041, Joint.class);
		classMap.put(-9042, LttlBlendMode.class);
		classMap.put(-9043, LttlWorldCore.class);
		classMap.put(-9044, LttlSceneCore.class);
		classMap.put(-9045, LttlTextureManager.class);
		classMap.put(-9046, LttlParticleEmitterTexture.class);
		classMap.put(-9047, LttlEditorCamera.class);
		classMap.put(-9048, PlaySoundOnClick.class);
		classMap.put(-9049, PlayMusicOnStart.class);
		classMap.put(-9050, SelectionOptions.class);
		classMap.put(-9051, LttlCameraTransformState.class);
		classMap.put(-9052, StateBase.class);
		classMap.put(-9053, LttlTransformState.class);
		classMap.put(-9054, StateProperty.class);
		classMap.put(-9055, StatePropertyFloat.class);
		classMap.put(-9056, StatePropertyInteger.class);
		classMap.put(-9057, StatePropertyVector2.class);
		classMap.put(-9058, StatePropertyColor.class);
		classMap.put(-9059, LttlStateManager.class);
		classMap.put(-9060, LttlTransformStateManager.class);
		classMap.put(-9061, LttlRendererState.class);
		classMap.put(-9062, LttlRendererStateManager.class);
		classMap.put(-9063, LttlAnimationManager.class);
		classMap.put(-9064, AnimationObject.class);
		classMap.put(-9065, TimelineNode.class);
		classMap.put(-9066, StateKeyframeNode.class);
		classMap.put(-9067, AnimationCallbackNode.class);
		classMap.put(-9068, RepeatTweenType.class);
		classMap.put(-9069, AnimationSequence.class);
		classMap.put(-9070, StatePropertyOptions.class);
		classMap.put(-9071, EaseMode.class);
		classMap.put(-9072, NoiseOptions.class);
		classMap.put(-9073, DeltaTimeType.class);
		classMap.put(-9074, AnimationHoldNode.class);
		classMap.put(-9075, LttlPath.class);
		classMap.put(-9076, LttlPathControlPoint.class);
		classMap.put(-9077, LttlPathControlPointType.class);
		classMap.put(-9078, LttlPathEditorSettings.class);
		classMap.put(-9079, LttlPathControlPointExtra.class);
		classMap.put(-9080, LttlPathState.class);
		classMap.put(-9081, StatePropertyGroup.class);
		classMap.put(-9082, LttlPathControlPointStatePropertyGroup.class);
		classMap.put(-9083, LttlPathStateManager.class);
		classMap.put(-9084, LttlCustomShape.class);
		classMap.put(-9085, LttlCustomShapeBase.class);
		classMap.put(-9086, LttlCustomLandscape.class);
		classMap.put(-9087, LttlCustomLine.class);
		classMap.put(-9088, AnimatedObject.class);
		classMap.put(-9089, AnimatedComponent.class);
		classMap.put(-9090, FieldKeyframeNode.class);
		classMap.put(-9091, KeyframeNode.class);
		classMap.put(-9092, PhysicsController.class);
		classMap.put(-9093, KeyframeOptions.class);
		classMap.put(-9094, LttlMultiObjectRenderer.class);
		classMap.put(-9095, LttlBasicTextureMeshGenerator.class);
		classMap.put(-9096, FloatRange.class);
		classMap.put(-9097, LttlTimeline.class);
		classMap.put(-9098, Vector2Array.class);
		classMap.put(-9099, FloatRangeRandom.class);
		classMap.put(-90100, IntRangeRandom.class);
		classMap.put(-90101, LttlGradient.class);
		classMap.put(-90102, FloatRangeRandomTimeline.class);
		classMap.put(-90103, IntRangeRandomTimeline.class);
		classMap.put(-90104, IntRange.class);
		classMap.put(-90105, EmitterShape.class);
		classMap.put(-90106, ColorBlendMode.class);
		classMap.put(-90107, RangeRandomTimeline.class);
		classMap.put(-90108, LttlParticleEmitterBase.class);
		classMap.put(-90109, LttlQuadMeshGenerator.class);
		classMap.put(-90110, LttlUvOffsetRandomizer.class);
		classMap.put(-90111, LttlRectangle.class);
		classMap.put(-90112, LttlTriangleGenerator.class);
		classMap.put(-90113, VertexLock.class);
		classMap.put(-90114, LttlQuadGeneratorAbstract.class);
		classMap.put(-90115, LttlParallax.class);
		classMap.put(-90116, LttlQuadPoints.class);
		classMap.put(-90117, FloatOption.class);
		classMap.put(-90118, IntOption.class);
		classMap.put(-90119, CustomVertexObject.class);
		classMap.put(-90120, TextureFoldObject.class);
		classMap.put(-90121, LttlTrifoldMeshGenerator.class);
		classMap.put(-90122, QuadCorners.class);
		classMap.put(-90123, HorizontalAlignment.class);
		classMap.put(-90124, LttlModeOption.class);
		classMap.put(-90125, AnimationProperty.class);
		classMap.put(-90126, LttlShapeGenerator.class);
		classMap.put(-90127, LttlPhysicsFixtureBodyBase.class);
		classMap.put(-90128, LttlPhysicsBody.class);
		classMap.put(-90129, LttlPhysicsFixture.class);
		classMap.put(-90130, LttlPhysicsMesh.class);
		classMap.put(-90131, LttlPhysicsPath.class);
		classMap.put(-90132, LttlPhysicsCircle.class);
		classMap.put(-90133, LttlPhysicsRect.class);
		classMap.put(-90134, GeometryOperation.class);
		classMap.put(-90135, LttlPhysicsBase.class);
		classMap.put(-90136, CompareOperation.class);
		classMap.put(-90137, LttlPhysicsAdapter.class);
		classMap.put(-90138, LttlQuadRectMeshGenerator.class);
		classMap.put(-90139, LttlMouseAdapter.class);
		classMap.put(-90140, LttlPhysicsJointBase.class);
		classMap.put(-90141, LttlPhysicsRevoluteJoint.class);
		classMap.put(-90142, VerticalAlignment.class);
		classMap.put(-90143, AlternateSelectionBoundsComponent.class);
		classMap.put(-90144, LttlSimpleGradient.class);
	}
}
