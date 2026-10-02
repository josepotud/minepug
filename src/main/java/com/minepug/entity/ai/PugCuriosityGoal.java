package com.minepug.entity.ai;

import com.minepug.PugEntity;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
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
 * objetos tirados en el suelo, los mira e inclina la cabeza (interés).
 * Mientras observa suelta chispas de curiosidad, y al fijarse en algo
 * aparecen chispas sobre el objetivo para que se note a qué atiende.
 */
public class PugCuriosityGoal extends Goal {
	private static final double SEARCH_RANGE = 10.0D;
	private static final int MIN_CURIOSITY_TIME = 80;
	private static final int MAX_EXTRA_CURIOSITY_TIME = 80;
	private static final int MIN_COOLDOWN = 200;
	private static final int MAX_EXTRA_COOLDOWN = 200;
	private static final int PARTICLE_INTERVAL = 15;

	private final PugEntity pug;
	private final ServerLevel level;
	private Entity curiosityTarget;
	private double stopDistance;
	private int curiosityTime;
	private int cooldownTicks;
	private int particleTicks;

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
		this.particleTicks = 0;
		this.pug.setIsInterested(true);

		// Chispas sobre el objetivo: deja claro a qué le está prestando atención.
		if (this.curiosityTarget != null) {
			this.level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
					this.curiosityTarget.getX(), this.curiosityTarget.getEyeY() + 0.3D, this.curiosityTarget.getZ(),
					5, 0.3D, 0.3D, 0.3D, 0.0D);
		}
	}

	@Override
	public void tick() {
		if (this.curiosityTarget == null) {
			this.curiosityTime = 0;
			return;
		}

		this.pug.getLookControl().setLookAt(this.curiosityTarget, 10.0F, this.pug.getMaxHeadXRot());
		if (this.pug.distanceTo(this.curiosityTarget) > this.stopDistance) {
			this.pug.getNavigation().moveTo(this.curiosityTarget, 1.0D);
		} else {
			this.pug.getNavigation().stop();
		}
		this.curiosityTime--;

		// Chispas sobre la cabeza del carlino mientras observa.
		this.particleTicks++;
		if (this.particleTicks % PARTICLE_INTERVAL == 0) {
			this.level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
					this.pug.getX(), this.pug.getY() + this.pug.getBbHeight() + 0.2D, this.pug.getZ(),
					1, 0.25D, 0.15D, 0.25D, 0.0D);
		}
	}

	@Override
	public void stop() {
		this.pug.setIsInterested(false);
		this.curiosityTarget = null;
		this.cooldownTicks = MIN_COOLDOWN + this.pug.getRandom().nextInt(MAX_EXTRA_COOLDOWN);
	}
}
