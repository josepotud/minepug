package com.minepug.entity.ai;

import com.minepug.PugEntity;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/**
 * Pedigüeño: si el dueño lleva comida en la mano, el carlino se acerca,
 * se sienta solo a pedir y da saltitos de vez en cuando.
 */
public class PugBegGoal extends Goal {
	private static final double RANGE = 10.0D;
	private static final double ARRIVE_DISTANCE = 2.5D;
	private static final int HOP_INTERVAL = 60;
	private static final int MAX_EXTRA_HOP_INTERVAL = 60;

	private final PugEntity pug;
	private LivingEntity owner;
	private int hopTicks;

	public PugBegGoal(PugEntity pug) {
		this.pug = pug;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (!this.pug.canPugRoam()) {
			return false;
		}
		this.owner = this.pug.getOwner();
		return this.owner != null
				&& this.pug.distanceTo(this.owner) <= RANGE
				&& this.ownerHoldsFood();
	}

	@Override
	public boolean canContinueToUse() {
		return this.owner != null
				&& this.owner.isAlive()
				&& !this.pug.isOrderedToSit()
				&& !this.pug.isInLove()
				&& this.pug.getTarget() == null
				&& this.pug.distanceTo(this.owner) <= RANGE + 4.0D
				&& this.ownerHoldsFood();
	}

	@Override
	public void start() {
		this.hopTicks = HOP_INTERVAL + this.pug.getRandom().nextInt(MAX_EXTRA_HOP_INTERVAL);
	}

	@Override
	public void tick() {
		if (this.owner == null) {
			return;
		}
		this.pug.getLookControl().setLookAt(this.owner, 20.0F, this.pug.getMaxHeadXRot());

		if (this.pug.distanceTo(this.owner) > ARRIVE_DISTANCE) {
			this.pug.setVoluntarySitting(false);
			this.pug.getNavigation().moveTo(this.owner, 1.1D);
			return;
		}

		// Ya está al lado: se sienta a pedir.
		this.pug.getNavigation().stop();
		this.pug.setVoluntarySitting(true);

		this.hopTicks--;
		if (this.hopTicks <= 0) {
			this.hopTicks = HOP_INTERVAL + this.pug.getRandom().nextInt(MAX_EXTRA_HOP_INTERVAL);
			if (this.pug.onGround()) {
				this.pug.getJumpControl().jump();
			}
		}
	}

	@Override
	public void stop() {
		this.pug.setVoluntarySitting(false);
		this.owner = null;
	}

	private boolean ownerHoldsFood() {
		if (!(this.owner instanceof Player)) {
			return false;
		}
		return this.pug.isPugSnack(this.owner.getMainHandItem())
				|| this.pug.isPugSnack(this.owner.getOffhandItem());
	}
}
