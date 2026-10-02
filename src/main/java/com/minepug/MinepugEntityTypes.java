package com.minepug;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Tipos de entidad del mod.
 */
public final class MinepugEntityTypes {
	public static final Identifier PUG_ID = Minepug.id("pug");

	public static final EntityType<PugEntity> PUG = EntityType.Builder.of(PugEntity::new, MobCategory.CREATURE)
			.sized(0.6F, 0.85F)
			.eyeHeight(0.68F)
			.build(ResourceKey.create(Registries.ENTITY_TYPE, PUG_ID));

	private MinepugEntityTypes() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.ENTITY_TYPE, PUG_ID, PUG);
	}
}
