package com.minepug.entity.ai;

import com.minepug.PugEntity;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Si hay comida en un horno cercano ({@link AbstractFurnaceBlockEntity}:
 * horno, ahumador o alto horno), el carlino se acerca y se queda sentado
 * mirándolo mientras se cocina. Si es faldero y desobediente y puede
 * llegar hasta su dueño, abandona la vigilancia y se va tras él.
 */
public class PugWatchFurnaceGoal extends Goal {
	private static final double SCAN_RANGE = 6.0D;
	private static final double ARRIVE_DISTANCE = 3.5D;
	private static final double OWNER_ARRIVE_DISTANCE = 2.5D;
	private static final int WATCH_TIME = 200;
	private static final int MAX_EXTRA_WATCH_TIME = 200;
	private static final int COOLDOWN_TICKS = 300;
	private static final int MAX_EXTRA_COOLDOWN_TICKS = 600;
	private static final int RESCAN_INTERVAL = 40;
	private static final int OWNER_CHASE_TIMEOUT = 200;
	private static final double OWNER_ABANDON_MAX_DISTANCE = 40.0D;

	private enum Phase {
		GO_TO_FURNACE,
		WATCHING,
		GO_TO_OWNER
	}

	private final PugEntity pug;
	private final ServerLevel level;
	private BlockPos furnacePos;
	private Phase phase = Phase.GO_TO_FURNACE;
	private int watchTicks;
	private int cooldownTicks;
	private int rescanTicks;
	private int ownerChaseTicks;
	private boolean finished;

	public PugWatchFurnaceGoal(PugEntity pug) {
		this.pug = pug;
		this.level = getServerLevel(pug);
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (this.cooldownTicks > 0) {
			this.cooldownTicks--;
			return false;
		}
		if (!this.pug.canPugRoam()) {
			return false;
		}
		this.furnacePos = this.findFurnaceWithFood();
		return this.furnacePos != null;
	}

	@Override
	public boolean canContinueToUse() {
		return !this.finished && this.furnacePos != null;
	}

	@Override
	public void start() {
		this.finished = false;
		this.phase = Phase.GO_TO_FURNACE;
		this.watchTicks = WATCH_TIME + this.pug.getRandom().nextInt(MAX_EXTRA_WATCH_TIME);
		this.rescanTicks = 0;
	}

	@Override
	public void tick() {
		switch (this.phase) {
			case GO_TO_FURNACE -> this.tickGoToFurnace();
			case WATCHING -> this.tickWatching();
			case GO_TO_OWNER -> this.tickGoToOwner();
		}
	}

	private void tickGoToFurnace() {
		this.rescanTicks--;
		if (this.rescanTicks <= 0) {
			this.rescanTicks = RESCAN_INTERVAL;
			if (!this.furnaceHasFood(this.furnacePos)) {
				this.finished = true;
				return;
			}
		}

		Vec3 center = Vec3.atCenterOf(this.furnacePos);
		this.pug.getLookControl().setLookAt(center.x, center.y, center.z, 20.0F, this.pug.getMaxHeadXRot());
		if (this.pug.distanceToSqr(center.x, center.y, center.z) <= ARRIVE_DISTANCE * ARRIVE_DISTANCE) {
			this.pug.getNavigation().stop();
			this.pug.setVoluntarySitting(true);
			this.phase = Phase.WATCHING;
		} else {
			this.pug.getNavigation().moveTo(center.x, center.y, center.z, 1.0D);
		}
	}

	private void tickWatching() {
		this.pug.setVoluntarySitting(true);
		Vec3 center = Vec3.atCenterOf(this.furnacePos);
		this.pug.getLookControl().setLookAt(center.x, center.y, center.z, 20.0F, this.pug.getMaxHeadXRot());

		this.rescanTicks--;
		if (this.rescanTicks <= 0) {
			this.rescanTicks = RESCAN_INTERVAL;
			if (!this.furnaceHasFood(this.furnacePos)) {
				this.finished = true;
				return;
			}
		}

		// Si el dueño se aleja (sobre todo si es faldero), o es un faldero
		// desobediente que puede llegar hasta él, deja el horno y se va detrás.
		LivingEntity owner = this.pug.getOwner();
		if (owner != null) {
			double ownerDistance = this.pug.distanceTo(owner);
			boolean ownerGone = ownerDistance > this.pug.getOwnerFollowDistance()
					&& ownerDistance < OWNER_ABANDON_MAX_DISTANCE;
			if (ownerGone || this.pug.shouldGoToOwner()) {
				this.pug.setVoluntarySitting(false);
				this.phase = Phase.GO_TO_OWNER;
				this.ownerChaseTicks = OWNER_CHASE_TIMEOUT;
				return;
			}
		}

		this.watchTicks--;
		if (this.watchTicks <= 0) {
			this.finished = true;
		}
	}

	private void tickGoToOwner() {
		LivingEntity owner = this.pug.getOwner();
		this.ownerChaseTicks--;
		if (owner == null || this.ownerChaseTicks <= 0) {
			this.finished = true;
			return;
		}
		this.pug.getLookControl().setLookAt(owner, 20.0F, this.pug.getMaxHeadXRot());
		if (this.pug.distanceTo(owner) > OWNER_ARRIVE_DISTANCE) {
			this.pug.getNavigation().moveTo(owner, 1.1D);
		} else {
			this.pug.getNavigation().stop();
			this.finished = true;
		}
	}

	@Override
	public void stop() {
		this.pug.setVoluntarySitting(false);
		this.pug.getNavigation().stop();
		this.furnacePos = null;
		this.cooldownTicks = COOLDOWN_TICKS + this.pug.getRandom().nextInt(MAX_EXTRA_COOLDOWN_TICKS);
	}

	private BlockPos findFurnaceWithFood() {
		double range = SCAN_RANGE * this.pug.getAmbientDistanceMultiplier();
		int r = (int) Math.ceil(range);
		BlockPos centerPos = this.pug.blockPosition();
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;

		for (BlockPos pos : BlockPos.betweenClosed(
				centerPos.offset(-r, -2, -r), centerPos.offset(r, 2, r))) {
			if (this.furnaceHasFood(pos)) {
				double distance = centerPos.distSqr(pos);
				if (distance < bestDistance) {
					bestDistance = distance;
					best = pos.immutable();
				}
			}
		}
		return best;
	}

	private boolean furnaceHasFood(BlockPos pos) {
		if (pos == null) {
			return false;
		}
		if (this.level.getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity furnace) {
			return this.isFood(furnace.getItem(0)) || this.isFood(furnace.getItem(2));
		}
		return false;
	}

	private boolean isFood(ItemStack stack) {
		return !stack.isEmpty() && stack.has(DataComponents.FOOD);
	}
}
