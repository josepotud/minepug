package com.minepug;

import net.minecraft.world.item.DyeColor;

/**
 * Puente de acceso entre el código del mod y los métodos que el mixin
 * {@code WolfMixin} añade a {@code net.minecraft.world.entity.animal.wolf.Wolf}.
 * <p>
 * Todas las instancias de {@code Wolf} (incluidos los {@link PugEntity}) implementan
 * esta interfaz en tiempo de ejecución gracias al mixin.
 */
public interface MinepugWolf {
	/** Generación de cría: 0 para un lobo recién domesticado, +1 por cada generación criada. */
	int minepug$getGeneration();

	void minepug$setGeneration(int generation);

	/** Acceso al setter privado del color de collar de los lobos. */
	void minepug$setCollarColor(DyeColor color);
}
