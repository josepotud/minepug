package com.minepug.mixin;

import com.minepug.MinepugWolf;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Añade a todos los lobos (y por herencia a los carlinos) un contador de
 * generaciones persistido en los datos de la entidad, y expone el setter
 * privado del collar para poder copiar el color al nacer un carlino.
 */
@Mixin(Wolf.class)
public abstract class WolfMixin implements MinepugWolf {

	@Unique
	private int minepug$generation;

	@Override
	@Unique
	public int minepug$getGeneration() {
		return this.minepug$generation;
	}

	@Override
	@Unique
	public void minepug$setGeneration(int generation) {
		this.minepug$generation = generation;
	}

	@Invoker("setCollarColor")
	@Override
	public abstract void minepug$setCollarColor(DyeColor color);

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void minepug$saveGeneration(ValueOutput output, CallbackInfo ci) {
		output.putInt("minepug_generation", this.minepug$generation);
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void minepug$loadGeneration(ValueInput input, CallbackInfo ci) {
		this.minepug$generation = input.getIntOr("minepug_generation", 0);
	}
}
