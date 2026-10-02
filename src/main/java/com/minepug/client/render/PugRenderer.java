package com.minepug.client.render;

import com.minepug.PugEntity;
import com.minepug.client.model.PugModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Renderer del carlino: usa el {@link PugModel} propio (cuerpo rechoncho,
 * cabeza grande, cola rizada) con el estado de render del lobo, para
 * aprovechar las animaciones de andar, sentarse y sacudirse.
 * Añade la capa del collar teñido para los carlinos domesticados.
 */
public class PugRenderer extends MobRenderer<PugEntity, WolfRenderState, PugModel> {

	public PugRenderer(EntityRendererProvider.Context context) {
		super(context, new PugModel(PugModel.createBodyLayer().bakeRoot()), 0.5F);
		this.addLayer(new PugCollarLayer(this));
	}

	@Override
	public WolfRenderState createRenderState() {
		return new WolfRenderState();
	}

	@Override
	public void extractRenderState(PugEntity entity, WolfRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.isAngry = entity.isAngry();
		state.isSitting = entity.isInSittingPose() || entity.isZoomiesSitting();
		state.tailAngle = entity.getTailAngle();
		state.headRollAngle = entity.getHeadRollAngle(partialTicks);
		state.shakeAnim = entity.getShakeAnim(partialTicks);
		state.texture = entity.getTexture();
		state.wetShade = entity.getWetShade(partialTicks);
		state.collarColor = entity.isTame() ? entity.getCollarColor() : null;
		// Sin armadura: la textura del carlino es propia.
		state.bodyArmorItem = ItemStack.EMPTY;
	}

	@Override
	public Identifier getTextureLocation(WolfRenderState state) {
		return state.texture;
	}
}
