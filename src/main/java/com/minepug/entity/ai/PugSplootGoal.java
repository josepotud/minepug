package com.minepug.entity.ai;

import com.minepug.PugEntity;
import java.util.Comparator;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.decoration.Cushion;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

/**
 * Sploot: de vez en cuando el carlino se tumba panza abajo a descansar
 * (con ronquiditos). Si hay un cojín cerca, prefiere tumbarse encima.
 */
public class PugSplootGoal extends Goal {
	private static final int CHECK_CHANCE = 200;
	private static final int MIN_LIE_TIME = 600;
	private static final int MAX_EXTRA_LIE_TIME = 1200;
	private static final int COOLDOWN_TICKS = 600;
	private static final int MAX_EXTRA_COOLDOWN_TICKS = 1200;
	private static final double CUSHION_SEARCH_RANGE = 8.0D;
	private static final double CUSHION_ARRIVE_DISTANCE = 1.2D;
	private static final int SNORE_INTERVAL = 80;
	private static final int MAX_EXTRA_SNORE_INTERVAL = 60;

	private enum Phase {
		GO_TO_CUSHION,
		LYING
	}

	private final PugEntity pug;
	private final ServerLevel level;
	private Phase phase = Phase.LYING;
	private Cushion cushion;
	private int lieTicks;
	private int cooldownTicks;
	private int snoreTicks;
	private boolean finished;

	public PugSplootGoal(PugEntity pug) {
		this.pug = pug;
		this.level = getServerLevel(pug);
	}

	@Override
	public boolean canUse() {
		if (this.cooldownTicks > 0) {
			this.cooldownTicks--;
			return false;
		}
		if (!this.pug.canPugRoam() || !this.pug.onGround() || this.pug.isInWater()) {
			return false;
		}
		if (!this.pug.getNavigation().isDone()) {
			return false;
		}
		if (this.pug.getRandom().nextInt(CHECK_CHANCE) != 0) {
			return false;
		}
		this.cushion = this.findCushion();
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		if (this.finished) {
			return false;
		}
		if (this.phase == Phase.GO_TO_CUSHION) {
			return this.pug.canPugRoam();
		}
		return !this.pug.isOrderedToSit()
				&& !this.pug.isInLove()
				&& this.pug.getTarget() == null
				&& this.pug.onGround();
	}

	@Override
	public void start() {
		this.finished = false;
		this.snoreTicks = SNORE_INTERVAL;
		this.lieTicks = MIN_LIE_TIME + this.pug.getRandom().nextInt(MAX_EXTRA_LIE_TIME);
		if (this.cushion != null && this.cushion.isAlive()) {
			this.phase = Phase.GO_TO_CUSHION;
		} else {
			this.beginLying();
		}
	}

	@Override
	public void tick() {
		if (this.phase == Phase.GO_TO_CUSHION) {
			if (this.cushion == null || !this.cushion.isAlive()) {
				this.beginLying();
				return;
			}
			this.pug.getLookControl().setLookAt(this.cushion, 10.0F, this.pug.getMaxHeadXRot());
			if (this.pug.distanceTo(this.cushion) > CUSHION_ARRIVE_DISTANCE) {
				this.pug.getNavigation().moveTo(this.cushion, 1.0D);
			} else {
				this.pug.getNavigation().stop();
				this.pug.snapTo(
						this.cushion.getX(), this.cushion.getY(), this.cushion.getZ(),
						this.pug.getYRot(), this.pug.getXRot()
				);
				this.beginLying();
			}
			return;
		}

		// Tumbado: ronquiditos de vez en cuando.
		this.snoreTicks--;
		if (this.snoreTicks <= 0) {
			this.snoreTicks = SNORE_INTERVAL + this.pug.getRandom().nextInt(MAX_EXTRA_SNORE_INTERVAL);
			this.pug.pugPlaySound(
					net.minecraft.resources.Identifier.withDefaultNamespace("entity.wolf.pant"),
					0.6F, 0.55F
			);
		}

		this.lieTicks--;
		if (this.lieTicks <= 0) {
			this.finished = true;
		}
	}

	private void beginLying() {
		this.pug.setVoluntaryLying(true);
		this.phase = Phase.LYING;
	}

	@Override
	public void stop() {
		this.pug.setVoluntaryLying(false);
		this.pug.getNavigation().stop();
		this.cushion = null;
		this.cooldownTicks = COOLDOWN_TICKS + this.pug.getRandom().nextInt(MAX_EXTRA_COOLDOWN_TICKS);
	}

	private Cushion findCushion() {
		AABB area = this.pug.getBoundingBox().inflate(CUSHION_SEARCH_RANGE);
		List<Cushion> cushions = this.level.getEntities(EntityTypeTest.forClass(Cushion.class), area, Cushion::isAlive);
		return cushions.stream()
				.min(Comparator.comparingDouble(this.pug::distanceToSqr))
				.orElse(null);
	}
}
