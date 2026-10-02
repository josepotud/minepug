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
 * Alerta con ladrido agudo cuando hay monstruos cerca. Solo usa la mirada,
 * así que funciona también cuando el carlino está sentado (la persecución
 * la hace {@link PugChaseThreatGoal} cuando puede moverse).
 */
public class PugAlertGoal extends Goal {
	private static final Identifier WOLF_GROWL = Identifier.withDefaultNamespace("entity.wolf.growl");
	private static final double ALERT_RANGE = 12.0D;
	private static final int BARK_COOLDOWN = 60;

	private final PugEntity pug;
	private final ServerLevel level;
	private Mob threat;

	public PugAlertGoal(PugEntity pug) {
		this.pug = pug;
		this.level = getServerLevel(pug);
		this.setFlags(EnumSet.of(Flag.LOOK));
	}

	@Override
	public boolean canUse() {
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
	public boolean canContinueToUse() {
		return this.threat != null && this.threat.isAlive();
	}

	@Override
	public void start() {
		if (this.pug.canPugBark(BARK_COOLDOWN)) {
			this.pug.markPugBarked();
			this.pug.level().registryAccess().lookupOrThrow(Registries.SOUND_EVENT)
					.get(WOLF_GROWL)
					.ifPresent(holder -> this.pug.playSound(holder.value(), 1.0F, 1.35F));
		}
		this.pug.setIsInterested(true);
	}

	@Override
	public void tick() {
		if (this.threat != null && this.threat.isAlive()) {
			this.pug.getLookControl().setLookAt(this.threat, 20.0F, this.pug.getMaxHeadXRot());
		}
	}

	@Override
	public void stop() {
		this.pug.setIsInterested(false);
		this.threat = null;
	}
}
