package com.minepug.mixin;

import com.minepug.MinepugConfig;
import com.minepug.MinepugEntityTypes;
import com.minepug.MinepugWolf;
import com.minepug.PugEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Intercepta la cría de animales justo cuando se va a crear la cría de lobo:
 * si ambos padres son lobos (o carlinos), calcula las generaciones que llevan
 * detrás y, con la probabilidad correspondiente, cambia la cría por un carlino.
 */
@Mixin(Animal.class)
public abstract class AnimalMixin {

	@Redirect(
			method = "spawnChildFromBreeding",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/entity/animal/Animal;getBreedOffspring(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/AgeableMob;)Lnet/minecraft/world/entity/AgeableMob;"
			)
	)
	private AgeableMob minepug$maybePugOffspring(Animal self, ServerLevel level, AgeableMob partner) {
		AgeableMob offspring = self.getBreedOffspring(level, partner);
		if (offspring == null) {
			return null;
		}

		// El mod solo altera la cría de lobos (y carlinos, que son lobos).
		if (!(self instanceof Wolf mother) || !(partner instanceof Wolf father)) {
			return offspring;
		}

		int parentsGeneration = Math.max(
				((MinepugWolf) mother).minepug$getGeneration(),
				((MinepugWolf) father).minepug$getGeneration()
		);
		int childGeneration = parentsGeneration + 1;

		// Dos carlinos siempre tienen carlinos; mezcla con lobo (o lobo+lobo)
		// usa la probabilidad por generaciones.
		boolean bothPugs = mother instanceof PugEntity && father instanceof PugEntity;
		boolean isPug = bothPugs || level.getRandom().nextDouble() < MinepugConfig.pugChance(parentsGeneration);
		if (!isPug) {
			// Cachorro de lobo normal: se sigue contando el linaje.
			((MinepugWolf) offspring).minepug$setGeneration(childGeneration);
			return offspring;
		}
		if (offspring instanceof PugEntity existingPug) {
			// La cría ya es un carlino (padres carlinos): solo ajusta la generación.
			((MinepugWolf) existingPug).minepug$setGeneration(childGeneration);
			return existingPug;
		}

		PugEntity pug = MinepugEntityTypes.PUG.create(level, EntitySpawnReason.BREEDING);
		if (pug == null) {
			((MinepugWolf) offspring).minepug$setGeneration(childGeneration);
			return offspring;
		}

		// El carlino hereda lo mismo que el cachorro de lobo descartado:
		// dueño, domesticación y color de collar mezclado de los padres.
		if (mother.isTame()) {
			pug.setOwnerReference(mother.getOwnerReference());
			pug.setTame(true, true);
			((MinepugWolf) pug).minepug$setCollarColor(
					DyeColor.getMixedColor(level, mother.getCollarColor(), father.getCollarColor())
			);
		}
		((MinepugWolf) pug).minepug$setGeneration(childGeneration);

		return pug;
	}
}
