package com.minepug;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

/**
 * Items del mod.
 */
public final class MinepugItems {
	public static final Identifier PUG_SPAWN_EGG_ID = Minepug.id("pug_spawn_egg");

	public static final Item PUG_SPAWN_EGG = new SpawnEggItem(
			new Item.Properties()
					.setId(ResourceKey.create(Registries.ITEM, PUG_SPAWN_EGG_ID))
					.spawnEgg(MinepugEntityTypes.PUG)
	);

	private MinepugItems() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.ITEM, PUG_SPAWN_EGG_ID, PUG_SPAWN_EGG);
	}
}
