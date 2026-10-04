package com.minepug.entity.ai;

import com.minepug.PugEntity;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * Los carlinos sufren con el calor: cuando hace calor (bioma cálido, de día
 * y sin lluvia) buscan agua cercana y se tumban a la fresca. Si no hay agua,
 * se tumban igualmente donde estén.
 */
public class PugCoolOffGoal extends Goal {
	private static final double SCAN_RANGE = 8.0D;
	private static final double ARRIVE_DISTANCE = 2.0D;
	private static final int LIE_TIME = 200;
	private static final int MAX_EXTRA_LIE_TIME = 200;
	private static final int COOLDOWN_TICKS = 600;
	private static final int MAX_EXTRA_COOLDOWN_TICKS = 600;
	private static final int RESCAN_INTERVAL = 60;
	private static final int RANDOM_CHANCE = 60;

	private final PugEntity pug;
	private final ServerLevel level;
	private BlockPos waterPos;
	private int lieTicks;
	private int cooldownTicks;
	private int rescanTicks;
	private boolean lying;
	private boolean finished;

	public PugCoolOffGoal(PugEntity pug) {
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
		if (!this.pug.canPugRoam() || !this.pug.onGround() || !this.pug.isFeelingHot()) {
			return false;
		}
		if (this.pug.getRandom().nextInt(RANDOM_CHANCE) != 0) {
			return false;
		}
		this.waterPos = this.findWater();
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		if (this.finished) {
			return false;
		}
		if (!this.lying) {
			return this.pug.canPugRoam() && this.pug.isFeelingHot();
		}
		return !this.pug.isOrderedToSit()
				&& !this.pug.isInLove()
				&& this.pug.getTarget() == null
				&& this.pug.onGround()
				&& this.pug.isFeelingHot();
	}

	@Override
	public void start() {
		this.finished = false;
		this.lying = false;
		this.rescanTicks = 0;
		this.lieTicks = LIE_TIME + this.pug.getRandom().nextInt(MAX_EXTRA_LIE_TIME);
	}

	@Override
	public void tick() {
		this.rescanTicks--;
		if (this.rescanTicks <= 0) {
			this.rescanTicks = RESCAN_INTERVAL;
			if (!this.pug.isFeelingHot()) {
				this.finished = true;
				return;
			}
			if (this.waterPos == null) {
				this.waterPos = this.findWater();
			}
		}

		if (!this.lying) {
			if (this.waterPos != null) {
				Vec3 center = Vec3.atCenterOf(this.waterPos);
				this.pug.getLookControl().setLookAt(center.x, center.y, center.z, 20.0F, this.pug.getMaxHeadXRot());
				if (this.pug.distanceToSqr(center.x, center.y, center.z) <= ARRIVE_DISTANCE * ARRIVE_DISTANCE) {
					this.pug.getNavigation().stop();
					this.pug.setVoluntaryLying(true);
					this.lying = true;
				} else {
					this.pug.getNavigation().moveTo(center.x, center.y, center.z, 1.0D);
				}
				return;
			}
			// Sin agua cerca: a la fresca donde esté.
			this.pug.setVoluntaryLying(true);
			this.lying = true;
		}

		this.lieTicks--;
		if (this.lieTicks <= 0) {
			this.finished = true;
		}
	}

	@Override
	public void stop() {
		if (this.lying) {
			this.pug.setVoluntaryLying(false);
		}
		this.pug.getNavigation().stop();
		this.waterPos = null;
		this.lying = false;
		this.cooldownTicks = COOLDOWN_TICKS + this.pug.getRandom().nextInt(MAX_EXTRA_COOLDOWN_TICKS);
	}

	/** Busca el bloque de agua más cercano. */
	private BlockPos findWater() {
		double range = SCAN_RANGE * this.pug.getAmbientDistanceMultiplier();
		int r = (int) Math.ceil(range);
		BlockPos centerPos = this.pug.blockPosition();
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;

		for (BlockPos pos : BlockPos.betweenClosed(
				centerPos.offset(-r, -1, -r), centerPos.offset(r, 2, r))) {
			if (this.level.getBlockState(pos).getFluidState().is(FluidTags.WATER)) {
				double distance = centerPos.distSqr(pos);
				if (distance < bestDistance) {
					bestDistance = distance;
					best = pos.immutable();
				}
			}
		}
		return best;
	}
}
