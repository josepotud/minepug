package com.minepug;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minepug — cría lobos domesticados durante generaciones para conseguir un carlino (pug).
 */
public class Minepug implements ModInitializer {
	public static final String MOD_ID = "minepug";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** Pestaña "Huevos generadores" del inventario creativo. */
	private static final ResourceKey<CreativeModeTab> SPAWN_EGGS_TAB =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("spawn_eggs"));

	/** Pestaña "Herramientas y utilidades" del inventario creativo. */
	private static final ResourceKey<CreativeModeTab> TOOLS_TAB =
			ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("tools_and_utilities"));

	@Override
	public void onInitialize() {
		MinepugEntityTypes.register();
		MinepugItems.register();
		MinepugCommands.register();
		MinepugLoot.register();

		// Añade el huevo de carlino a la pestaña de huevos generadores, tras el del lobo.
		CreativeModeTabEvents.modifyOutputEvent(SPAWN_EGGS_TAB).register(output -> {
			output.insertAfter(Items.WOLF_SPAWN_EGG, MinepugItems.PUG_SPAWN_EGG);
		});

		// La pelota de carlino aparece en herramientas y utilidades.
		CreativeModeTabEvents.modifyOutputEvent(TOOLS_TAB).register(output -> {
			output.accept(MinepugItems.PUG_BALL);
		});

		LOGGER.info("¡Minepug listo! Cría lobos domesticados durante generaciones para conseguir un carlino.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
