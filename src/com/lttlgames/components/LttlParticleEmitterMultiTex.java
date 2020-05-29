package com.lttlgames.components;

import java.util.ArrayList;
import java.util.Iterator;

import com.lttlgames.editor.annotations.GuiCallbackDescendants;
import com.lttlgames.editor.annotations.Persist;
import com.lttlgames.helpers.LttlMath;

@Persist(-9021)
public class LttlParticleEmitterMultiTex extends LttlParticleEmitter
{
	@Persist(902100)
	@GuiCallbackDescendants("clearTextureCaches")
	public ArrayList<LttlParticleEmitterTexture> textures = new ArrayList<LttlParticleEmitterTexture>();
	@Persist(902101)
	public boolean randomTextures = true;
	/**
	 * if true, each texture will generate their own mesh (ie. LttlBasicTextureMeshGenerator)<br>
	 * This really can only would be false if the different textures all had same UV positions, aka non atlas textures,
	 * and even then it's a stretch because the meshScale would have to be same.
	 */
	@Persist(902102)
	public boolean uniqueMeshes = true;

	private float[] weights;

	@Override
	public void onEditorStart()
	{
		super.onEditorStart();
		refreshMultiTextures();
	}

	@Override
	public void onStart()
	{
		super.onStart();
		refreshMultiTextures();
	}

	private void refreshMultiTextures()
	{
		for (LttlParticleEmitterTexture pTex : textures)
		{
			pTex.texture.refresh();
		}
	}

	/**
	 * Clears each texture's cached mesh, forcing it to generate it again on render, removes null textures in list, and
	 * gets weights for randomizing.
	 */
	public void clearCache()
	{
		for (Iterator<LttlParticleEmitterTexture> it = textures.iterator(); it
				.hasNext();)
		{
			LttlParticleEmitterTexture ptex = it.next();
			if (ptex == null)
			{
				it.remove();
				continue;
			}
			ptex.clearCache();
		}
		weights = null;
	}

	private float[] getWeights()
	{
		if (weights == null)
		{
			weights = new float[textures.size()];
			for (int i = 0; i < textures.size(); i++)
			{
				weights[i] = textures.get(i).randomWeight;
			}
		}
		return weights;
	}

	@Override
	public void onUpdate()
	{
		// initially sets the texture to something so LttlMeshGenerator can create the mesh
		if (getTex0().getAR() == null && textures.size() > 0)
		{
			getTex0().setAR(textures.get(0).texture.getAR());
		}
	}

	@Override
	protected void onNewObject(LttlParticle newObj)
	{
		super.onNewObject(newObj);
		newObj.texIndex = getRandomTextureIndex();
	}

	private int getRandomTextureIndex()
	{
		if (textures.size() == 0) return 0;

		return LttlMath.RandomWeightPick(getWeights());
	}

	@Override
	protected void prepareRender(LttlParticle p)
	{
		if (p.texIndex >= textures.size()) return;

		// get texture
		LttlParticleEmitterTexture pTex = textures.get(p.texIndex);

		// set the texture on the renderer
		getTex0().setAR(pTex.texture.getAR());

		// are we using uniqueMeshes, if not then not even messing with mesh
		if (uniqueMeshes && generator() != null)
		{
			// generate new mesh if none cached yet
			if (pTex.mesh == null)
			{
				// clear mesh if more than 1 texture so you can be sure this is a unique mesh object
				setMesh(null);
				generator().updateMesh();
				pTex.mesh = getMesh();
			}

			// set renderer mesh the cached mesh for this texture
			setMesh(pTex.mesh);
		}
	}
}
