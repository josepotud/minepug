package com.minepug.entity.ai;

import com.minepug.PugEntity;
import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * Zoomies: de vez en cuando el carlino corre en círculos unos segundos,
 * luego se sienta unos segundos y después retoma su actividad normal.
 */
public class PugZoomiesGoal extends Goal {
	private static final int MIN_COOLDOWN = 400;
	private static final int MAX_EXTRA_COOLDOWN = 800;
	private static final int RUN_TIME = 80;
	private static final int MAX_EXTRA_RUN_TIME = 60;
	private static final int SIT_TIME = 60;
	private static final int MAX_EXTRA_SIT_TIME = 40;

	private final PugEntity pug;
	private final ServerLevel level;
	private int cooldownTicks;
	private int runTicks;
	private int sitTicks;
	private int nextTurnTicks;
	private boolean sitting;
	private boolean done;
	private Vec3 center;

	public PugZoomiesGoal(PugEntity pug) {
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
		return this.pug.canPugRoam() && !this.pug.isInWater() && this.pug.onGround();
	}

	@Override
	public boolean canContinueToUse() {
		return !this.done && !this.pug.isInWater() && !this.pug.isOrderedToSit();
	}

	@Override
	public void start() {
		this.done = false;
		this.sitting = false;
		this.runTicks = RUN_TIME + this.pug.getRandom().nextInt(MAX_EXTRA_RUN_TIME);
		this.sitTicks = SIT_TIME + this.pug.getRandom().nextInt(MAX_EXTRA_SIT_TIME);
		this.nextTurnTicks = 0;
		this.center = this.pug.position();
		this.pickNextCirclePoint();
	}

	@Override
	public void tick() {
		if (this.done) {
			return;
		}

		if (this.sitting) {
			this.pug.getNavigation().stop();
			this.sitTicks--;
			if (this.sitTicks <= 0) {
				this.pug.setZoomiesSitting(false);
				this.done = true;
				this.cooldownTicks = MIN_COOLDOWN + this.pug.getRandom().nextInt(MAX_EXTRA_COOLDOWN);
			}
			return;
		}

		this.nextTurnTicks--;
		if (this.nextTurnTicks <= 0) {
			this.pickNextCirclePoint();
		}

		this.runTicks--;
		if (this.runTicks <= 0) {
			this.sitting = true;
			this.pug.setZoomiesSitting(true);
			this.pug.getNavigation().stop();
		}
	}

	private void pickNextCirclePoint() {
		double angle = this.pug.getRandom().nextDouble() * Math.PI * 2.0;
		double radius = 2.5D + this.pug.getRandom().nextDouble() * 2.5D;
		double x = this.center.x + Math.cos(angle) * radius;
		double z = this.center.z + Math.sin(angle) * radius;
		this.pug.getNavigation().moveTo(x, this.center.y, z, 2.5D);
		this.nextTurnTicks = 15 + this.pug.getRandom().nextInt(15);
	}

	@Override
	public void stop() {
		if (this.sitting) {
			this.pug.setZoomiesSitting(false);
		}
		this.pug.getNavigation().stop();
		this.sitting = false;
		this.done = false;
	}
}
