package com.minepug;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.entity.EntityTypeTest;

/**
 * Comando {@code /minepug info}: revela los rasgos ocultos del carlino
 * más cercano (generación, faldero, obediencia y si es negro).
 */
public final class MinepugCommands {
	private static final double SEARCH_RANGE = 10.0D;

	private MinepugCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, selection) ->
				dispatcher.register(Commands.literal("minepug")
						.then(Commands.literal("info")
								.executes(context -> showInfo(context.getSource())))));
	}

	private static int showInfo(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		if (!(player.level() instanceof ServerLevel level)) {
			return 0;
		}

		List<PugEntity> pugs = level.getEntities(
				EntityTypeTest.forClass(PugEntity.class),
				player.getBoundingBox().inflate(SEARCH_RANGE),
				PugEntity::isAlive
		);
		if (pugs.isEmpty()) {
			source.sendFailure(Component.literal("No hay ningún carlino cerca."));
			return 0;
		}

		PugEntity pug = pugs.stream()
				.min(Comparator.comparingDouble(player::distanceToSqr))
				.orElseThrow();
		int generation = ((MinepugWolf) pug).minepug$getGeneration();

		source.sendSuccess(() -> Component.literal(String.format(
				"%s | generación %d | faldero %.0f%% | obediencia %.0f%% | %s | %s",
				pug.getName().getString(),
				generation,
				pug.getClinginess() * 100.0F,
				pug.getObedience() * 100.0F,
				pug.isBlack() ? "carlino negro" : "carlino leonado",
				!pug.likesFetch()
						? "no le gusta buscar juguetes"
						: (pug.isFetchBringer()
								? "busca juguetes y los trae"
								: "busca juguetes y da vueltas con ellos")
		)), false);
		return 1;
	}
}
