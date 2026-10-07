package com.minepug;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

/**
 * La pelota de carlino no se puede fabricar: aparece raramente (≈4 %) en el
 * botín de algunos cofres (mazmorras, aldeas, minas abandonadas), para que
 * los carlinos juguetones no la traigan todo el rato.
 */
public final class MinepugLoot {
	private static final int PUG_BALL_WEIGHT = 4;
	private static final int EMPTY_WEIGHT = 96;

	private MinepugLoot() {
	}

	public static void register() {
		LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
			if (!source.isBuiltin()) {
				return;
			}
			String path = key.identifier().getPath();
			if (!path.equals("chests/simple_dungeon")
					&& !path.equals("chests/village/village_plains_house")
					&& !path.equals("chests/village/village_savanna_house")
					&& !path.equals("chests/abandoned_mineshaft")) {
				return;
			}

			// Un pool extra que casi siempre sale vacío (96 %) y a veces
			// contiene la pelota (4 %).
			tableBuilder.withPool(LootPool.lootPool()
					.setRolls(ContextIntProviders.exactly(1))
					.add(LootItem.lootTableItem(MinepugItems.PUG_BALL).setWeight(PUG_BALL_WEIGHT))
					.add(EmptyLootItem.emptyItem().setWeight(EMPTY_WEIGHT)));
		});
	}
}
