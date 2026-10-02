package com.minepug.entity.ai;

import com.minepug.PugEntity;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

/**
 * Avisa con un ladrido agudo cuando hay monstruos cerca.
 * <p>
 * Usa el gruñido del lobo vanilla con el tono subido ("más agudo que el del
 * perro"). Funciona también cuando el carlino está sentado (solo usa la
 * bandera LOOK) y no depende de estar cerca del punto de aparición del jugador.
 */
public class PugAlertGoal extends Goal {
	private static final Identifier WOLF_GROWL = Identifier.withDefaultNamespace("entity.wolf.growl");
	private static final double ALERT_RANGE = 12.0D;
	private static final int ALERT_COOLDOWN = 80;

	private final PugEntity pug;
	private final ServerLevel level;
	private Mob threat;
	private int alertCooldown;

	public PugAlertGoal(PugEntity pug) {
		this.pug = pug;
		this.level = getServerLevel(pug);
		this.setFlags(EnumSet.of(Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (this.alertCooldown > 0) {
			this.alertCooldown--;
			return false;
		}

		AABB area = this.pug.getBoundingBox().inflate(ALERT_RANGE);
		List<Mob> hostiles = this.level.getEntities(EntityTypeTest.forClass(Mob.class), area, mob ->
				mob != this.pug
						&& mob.isAlive()
						&& mob.getType().getCategory() == MobCategory.MONSTER
		);
		if (hostiles.isEmpty()) {
			return false;
		}

		this.threat = hostiles.stream()
				.min(Comparator.comparingDouble(this.pug::distanceToSqr))
				.orElseThrow();
		return true;
	}

	@Override
	public void start() {
		// Ladrido de alerta: el gruñido del lobo, más agudo.
		this.pug.level().registryAccess().lookupOrThrow(Registries.SOUND_EVENT)
				.get(WOLF_GROWL)
				.ifPresent(holder -> this.pug.playSound(holder.value(), 1.0F, 1.35F));
		this.pug.setIsInterested(true);
		this.alertCooldown = ALERT_COOLDOWN;
	}

	@Override
	public void tick() {
		if (this.threat != null && this.threat.isAlive()) {
			this.pug.getLookControl().setLookAt(this.threat, 10.0F, this.pug.getMaxHeadXRot());
		}
	}

	@Override
	public void stop() {
		this.pug.setIsInterested(false);
		this.threat = null;
	}
}
