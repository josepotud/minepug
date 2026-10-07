package com.minepug;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

/**
 * Items del mod.
 */
public final class MinepugItems {
	public static final Identifier PUG_SPAWN_EGG_ID = Minepug.id("pug_spawn_egg");
	public static final Identifier PUG_BALL_ID = Minepug.id("pug_ball");

	/**
	 * Etiqueta de objetos que el carlino reconoce como juguetes para buscar
	 * (la pelota y los clásicos palo, hueso y slime ball). Se puede ampliar
	 * con un data pack.
	 */
	public static final TagKey<Item> FETCH_TOYS = TagKey.create(Registries.ITEM, Minepug.id("fetch_toys"));

	public static final Item PUG_SPAWN_EGG = new SpawnEggItem(
			new Item.Properties()
					.setId(ResourceKey.create(Registries.ITEM, PUG_SPAWN_EGG_ID))
					.spawnEgg(MinepugEntityTypes.PUG)
	);

	public static final Item PUG_BALL = new PugBallItem(
			new Item.Properties()
					.setId(ResourceKey.create(Registries.ITEM, PUG_BALL_ID))
	);

	private MinepugItems() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.ITEM, PUG_SPAWN_EGG_ID, PUG_SPAWN_EGG);
		Registry.register(BuiltInRegistries.ITEM, PUG_BALL_ID, PUG_BALL);
	}
}
