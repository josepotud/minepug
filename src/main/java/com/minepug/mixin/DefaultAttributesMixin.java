package com.minepug.mixin;

import com.minepug.MinepugEntityTypes;
import com.minepug.PugEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Proporciona los atributos del carlino (lobo + escala reducida), ya que
 * {@link DefaultAttributes} no tiene API pública de registro en 26.3.
 */
@Mixin(DefaultAttributes.class)
public abstract class DefaultAttributesMixin {

	@Inject(method = "getSupplier", at = @At("HEAD"), cancellable = true)
	private static void minepug$supplyPug(EntityType<? extends LivingEntity> entityType, CallbackInfoReturnable<AttributeSupplier> cir) {
		if (entityType == MinepugEntityTypes.PUG) {
			cir.setReturnValue(PugEntity.createAttributes().build());
		}
	}
}
