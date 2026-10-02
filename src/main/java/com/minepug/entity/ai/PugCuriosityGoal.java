package com.minepug.entity.ai;

import com.minepug.PugEntity;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

/**
 * El carlino se acerca con curiosidad a jugadores, animales, monstruos y
 * objetos tirados en el suelo. Al fijarse en algo se planta y lo mira
 * girando la cabeza e inclinándola, sin partículas.
 */
public class PugCuriosityGoal extends Goal {
	private static final double SEARCH_RANGE = 10.0D;
	private static final int MIN_CURIOSITY_TIME = 100;
	private static final int MAX_EXTRA_CURIOSITY_TIME = 100;
	private static final int MIN_COOLDOWN = 200;
	private static final int MAX_EXTRA_COOLDOWN = 200;
	private static final float LOOK_SPEED = 30.0F;

	private final PugEntity pug;
	private final ServerLevel level;
	private Entity curiosityTarget;
	private double stopDistance;
	private int curiosityTime;
	private int cooldownTicks;

	public PugCuriosityGoal(PugEntity pug) {
		this.pug = pug;
		this.level = getServerLevel(pug);
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		this.cooldownTicks = MIN_COOLDOWN + pug.getRandom().nextInt(MAX_EXTRA_COOLDOWN);
	}

	@Override
	public boolean canUse() {
		if (this.cooldownTicks > 0) {
			this.cooldownTicks--;
			return false;
		}
		if (!this.pug.canPugRoam() || this.pug.getRandom().nextInt(60) != 0) {
			return false;
		}

		List<Entity> candidates = new ArrayList<>();
		AABB area = this.pug.getBoundingBox().inflate(SEARCH_RANGE);

		this.level.getEntities(EntityTypeTest.forClass(Entity.class), area, entity ->
				entity != this.pug
						&& entity.isAlive()
						&& (entity instanceof Player
								|| entity instanceof Animal
								|| entity instanceof ItemEntity
								|| (entity instanceof Mob mob && mob.getType().getCategory() == MobCategory.MONSTER))
		).forEach(candidates::add);

		if (candidates.isEmpty()) {
			return false;
		}

		this.curiosityTarget = candidates.get(this.pug.getRandom().nextInt(candidates.size()));
		this.stopDistance = this.curiosityTarget instanceof Mob mob
				&& mob.getType().getCategory() == MobCategory.MONSTER ? 3.5D : 2.5D;
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return this.curiosityTime > 0
				&& this.curiosityTarget != null
				&& this.curiosityTarget.isAlive()
				&& !this.curiosityTarget.isRemoved()
				&& this.pug.canPugRoam();
	}

	@Override
	public void start() {
		this.curiosityTime = MIN_CURIOSITY_TIME + this.pug.getRandom().nextInt(MAX_EXTRA_CURIOSITY_TIME);
		this.pug.setIsInterested(true);
	}

	@Override
	public void tick() {
		if (this.curiosityTarget == null) {
			this.curiosityTime = 0;
			return;
		}

		this.pug.getLookControl().setLookAt(this.curiosityTarget, LOOK_SPEED, this.pug.getMaxHeadXRot());
		if (this.pug.distanceTo(this.curiosityTarget) > this.stopDistance) {
			this.pug.getNavigation().moveTo(this.curiosityTarget, 1.0D);
		} else {
			this.pug.getNavigation().stop();
		}
		this.curiosityTime--;
	}

	@Override
	public void stop() {
		this.pug.setIsInterested(false);
		this.curiosityTarget = null;
		this.cooldownTicks = MIN_COOLDOWN + this.pug.getRandom().nextInt(MAX_EXTRA_COOLDOWN);
	}
}
