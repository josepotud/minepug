package com.minepug.mixin.client;

import com.google.common.collect.ImmutableMap;
import com.minepug.MinepugEntityTypes;
import com.minepug.client.render.PugRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * Registra el renderer del carlino (modelo propio de pug).
 * La textura la decide {@code PugEntity#getTexture()}.
 */
@Mixin(EntityRenderers.class)
public abstract class EntityRenderersMixin {

	@Inject(method = "createEntityRenderers", at = @At("RETURN"), cancellable = true)
	private static void minepug$addPugRenderer(
			EntityRendererProvider.Context context,
			CallbackInfoReturnable<Map<EntityType<?>, EntityRenderer<?, ?>>> cir
	) {
		ImmutableMap.Builder<EntityType<?>, EntityRenderer<?, ?>> builder = ImmutableMap.builder();
		builder.putAll(cir.getReturnValue());
		builder.put(MinepugEntityTypes.PUG, new PugRenderer(context));
		cir.setReturnValue(builder.build());
	}
}
