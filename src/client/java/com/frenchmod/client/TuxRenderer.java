package com.frenchmod.client;

import com.frenchmod.FrenchMod;
import com.frenchmod.tux.Tux;
import net.minecraft.client.model.animal.feline.AbstractFelineModel;
import net.minecraft.client.model.animal.feline.AdultCatModel;
import net.minecraft.client.model.animal.feline.BabyCatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.CatRenderState;
import net.minecraft.resources.Identifier;

/** Tux uses the vanilla cat model with Tux's own fur (tools/tux.py). */
public class TuxRenderer extends AgeableMobRenderer<Tux, CatRenderState, AbstractFelineModel<CatRenderState>> {
	private static final Identifier TEXTURE = FrenchMod.id("textures/entity/tux.png");

	public TuxRenderer(EntityRendererProvider.Context context) {
		super(context, new AdultCatModel(context.bakeLayer(ModelLayers.CAT)), new BabyCatModel(context.bakeLayer(ModelLayers.CAT_BABY)), 0.4F);
	}

	@Override
	public Identifier getTextureLocation(CatRenderState state) {
		return TEXTURE;
	}

	@Override
	public CatRenderState createRenderState() {
		return new CatRenderState();
	}

	@Override
	public void extractRenderState(Tux entity, CatRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.texture = TEXTURE;
		state.isSprinting = entity.isSprinting();
	}
}
