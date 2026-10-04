package com.minepug.entity.ai;

import com.minepug.PugEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/**
 * Desobediencia: con poca obediencia, es más fácil que el carlino se levante
 * a hacer sus cosas aunque el jugador le haya mandado sentarse. Si además es
 * faldero y puede llegar hasta su dueño, se va detrás de él; si no, se pone a
 * callejear un rato dentro de un radio que depende de su obediencia.
 * <p>
 * Este goal no reclama banderas de control: solo "suelta" la orden de sentarse
 * y deja que los demás goals hagan el resto durante un ratito.
 */
public class PugDisobedienceGoal extends Goal {
	private static final int CHECK_INTERVAL = 200;
	private static final int MIN_EPISODE = 100;
	private static final int MAX_EXTRA_EPISODE = 200;
	private static final int COOLDOWN_TICKS = 400;
	private static final int MAX_EXTRA_COOLDOWN_TICKS = 800;
	private static final float MAX_DISOBEY_CHANCE = 0.7F;
	private static final double MIN_WANDER_RADIUS = 3.0D;
	private static final double EXTRA_WANDER_RADIUS = 7.0D;
	private static final double OWNER_ARRIVE_DISTANCE = 2.5D;

	private final PugEntity pug;
	private int checkTicks;
	private int cooldownTicks;
	private int episodeTicks;
	private boolean disobeying;
	private boolean goToOwner;
	private Vec3 wanderTarget;

	public PugDisobedienceGoal(PugEntity pug) {
		this.pug = pug;
		this.checkTicks = CHECK_INTERVAL + pug.getRandom().nextInt(CHECK_INTERVAL);
	}

	@Override
	public boolean canUse() {
		if (this.cooldownTicks > 0) {
			this.cooldownTicks--;
			return false;
		}
		if (!this.pug.isOrderedToSit()) {
			return false;
		}
		this.checkTicks--;
		if (this.checkTicks > 0) {
			return false;
		}
		this.checkTicks = CHECK_INTERVAL;

		float chance = (1.0F - this.pug.getObedience()) * MAX_DISOBEY_CHANCE;
		return this.pug.getRandom().nextFloat() < chance;
	}

	@Override
	public boolean canContinueToUse() {
		return this.disobeying && this.episodeTicks > 0;
	}

	@Override
	public void start() {
		this.disobeying = true;
		this.pug.setOrderedToSit(false);
		this.pug.setInSittingPose(false);
		this.episodeTicks = MIN_EPISODE + this.pug.getRandom().nextInt(MAX_EXTRA_EPISODE);
		this.goToOwner = this.pug.shouldGoToOwner();
		this.wanderTarget = null;
		if (this.pug.getClinginess() <= 0.5F) {
			this.pickWanderTarget();
		}
	}

	@Override
	public void tick() {
		this.episodeTicks--;

		if (this.goToOwner) {
			LivingEntity owner = this.pug.getOwner();
			if (owner == null) {
				this.episodeTicks = 0;
				return;
			}
			this.pug.getLookControl().setLookAt(owner, 20.0F, this.pug.getMaxHeadXRot());
			if (this.pug.distanceTo(owner) > OWNER_ARRIVE_DISTANCE) {
				this.pug.getNavigation().moveTo(owner, 1.1D);
			} else {
				this.pug.getNavigation().stop();
			}
		} else {
			if (this.wanderTarget == null || this.pug.getNavigation().isDone()) {
				this.pickWanderTarget();
			}
			if (this.wanderTarget != null) {
				this.pug.getLookControl().setLookAt(this.wanderTarget.x, this.wanderTarget.y, this.wanderTarget.z);
				this.pug.getNavigation().moveTo(this.wanderTarget.x, this.wanderTarget.y, this.wanderTarget.z, 0.95D);
			}
		}

		if (this.episodeTicks <= 0) {
			this.episodeTicks = 0;
		}
	}

	private void pickWanderTarget() {
		double radius = MIN_WANDER_RADIUS + (1.0D - this.pug.getObedience()) * EXTRA_WANDER_RADIUS;
		double angle = this.pug.getRandom().nextDouble() * Math.PI * 2.0;
		this.wanderTarget = new Vec3(
				this.pug.getX() + Math.cos(angle) * radius,
				this.pug.getY(),
				this.pug.getZ() + Math.sin(angle) * radius
		);
	}

	@Override
	public void stop() {
		if (this.disobeying) {
			// Vuelve a sentarse: la orden del jugador sigue en pie.
			this.pug.setOrderedToSit(true);
			this.disobeying = false;
		}
		this.wanderTarget = null;
		this.cooldownTicks = COOLDOWN_TICKS + this.pug.getRandom().nextInt(MAX_EXTRA_COOLDOWN_TICKS);
	}
}
