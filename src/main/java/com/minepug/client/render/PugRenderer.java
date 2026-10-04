package com.minepug.client.render;

import com.minepug.PugEntity;
import com.minepug.client.model.PugModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Renderer del carlino: usa el {@link PugModel} propio (cuerpo rechoncho,
 * cabeza grande, cola rizada) con un estado de render extendido que añade
 * la pose de tumbado (sploot).
 */
public class PugRenderer extends MobRenderer<PugEntity, PugRenderState, PugModel> {

	public PugRenderer(EntityRendererProvider.Context context) {
		super(context, new PugModel(PugModel.createBodyLayer().bakeRoot()), 0.5F);
	}

	@Override
	public PugRenderState createRenderState() {
		return new PugRenderState();
	}

	@Override
	public void extractRenderState(PugEntity entity, PugRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.isAngry = entity.isAngry();
		state.isSitting = entity.isInSittingPose() || entity.isVoluntarySitting();
		state.lying = entity.isVoluntaryLying();
		state.tailAngle = entity.getTailAngle();
		state.headRollAngle = entity.getHeadRollAngle(partialTicks);
		state.shakeAnim = entity.getShakeAnim(partialTicks);
		state.texture = entity.getTexture();
		state.wetShade = entity.getWetShade(partialTicks);
		// Sin collar ni armadura: todos los carlinos son mascotas.
		state.collarColor = null;
		state.bodyArmorItem = ItemStack.EMPTY;
	}

	@Override
	public Identifier getTextureLocation(PugRenderState state) {
		return state.texture;
	}
}
