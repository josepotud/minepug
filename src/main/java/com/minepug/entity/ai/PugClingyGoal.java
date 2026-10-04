package com.minepug.entity.ai;

import com.minepug.PugEntity;
import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Faldero: cuanto más faldero es un carlino, más cerca se queda de su dueño.
 * Con faldero bajo apenas se nota (el lobo vanilla ya sigue a su dueño a
 * cierta distancia); con faldero alto te sigue pegado a dos bloques.
 */
public class PugClingyGoal extends Goal {
	private static final double BASE_STAY_DISTANCE = 2.0D;
	private static final double EXTRA_STAY_DISTANCE = 8.0D;
	private static final double ARRIVE_DISTANCE = 2.5D;
	private static final double FOLLOW_SPEED = 1.0D;

	private final PugEntity pug;
	private LivingEntity owner;

	public PugClingyGoal(PugEntity pug) {
		this.pug = pug;
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (!this.pug.canPugRoam()) {
			return false;
		}
		this.owner = this.pug.getOwner();
		if (this.owner == null) {
			return false;
		}
		double stayDistance = BASE_STAY_DISTANCE + (1.0D - this.pug.getClinginess()) * EXTRA_STAY_DISTANCE;
		return this.pug.distanceTo(this.owner) > stayDistance;
	}

	@Override
	public boolean canContinueToUse() {
		return this.owner != null
				&& this.owner.isAlive()
				&& this.pug.canPugRoam()
				&& this.pug.distanceTo(this.owner) > ARRIVE_DISTANCE;
	}

	@Override
	public void tick() {
		if (this.owner == null) {
			return;
		}
		this.pug.getLookControl().setLookAt(this.owner, 10.0F, this.pug.getMaxHeadXRot());
		if (this.pug.distanceTo(this.owner) > ARRIVE_DISTANCE) {
			this.pug.getNavigation().moveTo(this.owner, FOLLOW_SPEED);
		} else {
			this.pug.getNavigation().stop();
		}
	}
}
