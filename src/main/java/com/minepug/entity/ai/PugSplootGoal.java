package com.minepug.entity.ai;

import com.minepug.PugEntity;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.decoration.Cushion;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

/**
 * Sploot: de vez en cuando el carlino se tumba panza abajo a descansar
 * (con ronquiditos). Prefiere tumbarse encima de una cama (a la altura de
 * la superficie de la cama) o de un cojín (se sube encima); si no hay nada,
 * se tumba donde esté.
 */
public class PugSplootGoal extends Goal {
	private static final int CHECK_CHANCE = 200;
	private static final int MIN_LIE_TIME = 600;
	private static final int MAX_EXTRA_LIE_TIME = 1200;
	private static final int COOLDOWN_TICKS = 600;
	private static final int MAX_EXTRA_COOLDOWN_TICKS = 1200;
	private static final double SPOT_SEARCH_RANGE = 8.0D;
	private static final double BED_ARRIVE_DISTANCE = 1.8D;
	private static final double CUSHION_ARRIVE_DISTANCE = 1.6D;
	private static final int SNORE_INTERVAL = 80;
	private static final int MAX_EXTRA_SNORE_INTERVAL = 60;

	private enum Phase {
		GO_TO_BED,
		GO_TO_CUSHION,
		LYING
	}

	private final PugEntity pug;
	private final ServerLevel level;
	private Phase phase = Phase.LYING;
	private BlockPos bedPos;
	private double bedSurfaceY;
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
		return this.findRestSpot();
	}

	/** Busca sitio para tumbarse: primero una cama, luego un cojín. */
	private boolean findRestSpot() {
		this.bedPos = this.findBed();
		if (this.bedPos != null) {
			return true;
		}
		this.cushion = this.findCushion();
		return this.cushion != null;
	}

	@Override
	public boolean canContinueToUse() {
		if (this.finished) {
			return false;
		}
		if (this.phase != Phase.LYING) {
			return this.pug.canPugRoam();
		}
		return !this.pug.isOrderedToSit()
				&& !this.pug.isInLove()
				&& this.pug.getTarget() == null
				&& (this.pug.onGround() || this.pug.getVehicle() != null);
	}

	@Override
	public void start() {
		this.finished = false;
		this.snoreTicks = SNORE_INTERVAL;
		this.lieTicks = MIN_LIE_TIME + this.pug.getRandom().nextInt(MAX_EXTRA_LIE_TIME);
		if (this.bedPos != null) {
			this.phase = Phase.GO_TO_BED;
		} else if (this.cushion != null && this.cushion.isAlive()) {
			this.phase = Phase.GO_TO_CUSHION;
		} else {
			this.beginLying();
		}
	}

	@Override
	public void tick() {
		switch (this.phase) {
			case GO_TO_BED -> this.tickGoToBed();
			case GO_TO_CUSHION -> this.tickGoToCushion();
			case LYING -> this.tickLying();
		}
	}

	private void tickGoToBed() {
		if (this.bedPos == null) {
			this.beginLying();
			return;
		}
		double centerX = this.bedPos.getX() + 0.5D;
		double centerZ = this.bedPos.getZ() + 0.5D;
		this.pug.getLookControl().setLookAt(centerX, this.bedPos.getY() + 0.5D, centerZ, 20.0F, this.pug.getMaxHeadXRot());
		if (this.pug.distanceToSqr(centerX, this.bedPos.getY() + 1.0D, centerZ) > BED_ARRIVE_DISTANCE * BED_ARRIVE_DISTANCE) {
			this.pug.getNavigation().moveTo(centerX, this.bedPos.getY(), centerZ, 1.0D);
			return;
		}
		// Encima de la cama, a la altura de su superficie.
		this.pug.getNavigation().stop();
		this.pug.snapTo(centerX, this.bedPos.getY() + this.bedSurfaceY, centerZ, this.pug.getYRot(), 0.0F);
		this.beginLying();
	}

	private void tickGoToCushion() {
		if (this.cushion == null || !this.cushion.isAlive()) {
			this.beginLying();
			return;
		}
		this.pug.getLookControl().setLookAt(this.cushion, 10.0F, this.pug.getMaxHeadXRot());
		if (this.pug.distanceTo(this.cushion) > CUSHION_ARRIVE_DISTANCE) {
			this.pug.getNavigation().moveTo(this.cushion, 1.0D);
			return;
		}
		// Se sube al cojín (como cuando un jugador se sienta en él): así queda
		// encima, a la altura correcta, en vez de enterrado.
		this.pug.getNavigation().stop();
		if (!this.pug.startRiding(this.cushion)) {
			// Si no puede montarse, se tumba al lado.
			this.cushion = null;
		}
		this.beginLying();
	}

	private void tickLying() {
		// Ronquiditos de vez en cuando.
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
		if (this.pug.getVehicle() != null) {
			this.pug.stopRiding();
		}
		this.pug.getNavigation().stop();
		this.bedPos = null;
		this.cushion = null;
		this.cooldownTicks = COOLDOWN_TICKS + this.pug.getRandom().nextInt(MAX_EXTRA_COOLDOWN_TICKS);
	}

	/** Busca la cama más cercana y apunta la altura de su superficie. */
	private BlockPos findBed() {
		double range = SPOT_SEARCH_RANGE * this.pug.getAmbientDistanceMultiplier();
		int r = (int) Math.ceil(range);
		BlockPos centerPos = this.pug.blockPosition();
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		double bestSurface = 0.0D;

		for (BlockPos pos : BlockPos.betweenClosed(
				centerPos.offset(-r, -2, -r), centerPos.offset(r, 2, r))) {
			var state = this.level.getBlockState(pos);
			if (state.is(BlockTags.BEDS)) {
				double distance = centerPos.distSqr(pos);
				if (distance < bestDistance) {
					bestDistance = distance;
					best = pos.immutable();
					bestSurface = state.getCollisionShape(this.level, pos).bounds().maxY;
				}
			}
		}
		this.bedSurfaceY = bestSurface;
		return best;
	}

	private Cushion findCushion() {
		AABB area = this.pug.getBoundingBox().inflate(SPOT_SEARCH_RANGE);
		List<Cushion> cushions = this.level.getEntities(EntityTypeTest.forClass(Cushion.class), area, Cushion::isAlive);
		return cushions.stream()
				.filter(cushion -> !cushion.isVehicle())
				.min(Comparator.comparingDouble(this.pug::distanceToSqr))
				.orElse(null);
	}
}
