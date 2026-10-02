package com.minepug.client.render;

import com.minepug.Minepug;
import com.minepug.client.model.PugModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;

/**
 * Collar teñido para el carlino domesticado. Como la capa del lobo vanilla:
 * vuelve a dibujar el modelo con una textura transparente que solo contiene
 * la banda del collar (en la zona del cuello del modelo del carlino) y la
 * tiñe del color del collar.
 */
public class PugCollarLayer extends RenderLayer<WolfRenderState, PugModel> {
	private static final Identifier PUG_COLLAR_LOCATION = Minepug.id("textures/entity/pug_collar.png");

	public PugCollarLayer(RenderLayerParent<WolfRenderState, PugModel> renderer) {
		super(renderer);
	}

	@Override
	public void submit(
			PoseStack poseStack,
			SubmitNodeCollector submitNodeCollector,
			int lightCoords,
			WolfRenderState state,
			float yRot,
			float xRot
	) {
		DyeColor collarColor = state.collarColor;
		if (collarColor != null && !state.isInvisible) {
			int color = collarColor.getTextureDiffuseColor();
			submitNodeCollector.order(1)
					.submitModel(
							this.getParentModel(),
							state,
							poseStack,
							RenderTypes.entityCutout(PUG_COLLAR_LOCATION),
							lightCoords,
							OverlayTexture.NO_OVERLAY,
							color,
							null,
							state.outlineColor
					);
		}
	}
}
