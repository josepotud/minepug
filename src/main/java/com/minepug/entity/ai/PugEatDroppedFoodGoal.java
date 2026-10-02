package com.minepug.entity.ai;

import com.minepug.PugEntity;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

/**
 * Si se cae comida cerca, el carlino se acerca, la mira un instante
 * (breve retraso) y se la come.
 */
public class PugEatDroppedFoodGoal extends Goal {
	private static final double SEARCH_RANGE = 10.0D;
	private static final double REACH_DISTANCE = 1.5D;
	private static final int EAT_DELAY_TICKS = 25;
	private static final int EAT_COOLDOWN_TICKS = 200;

	private final PugEntity pug;
	private final ServerLevel level;
	private ItemEntity targetItem;
	private int delayTicks;
	private int cooldownTicks;
	private boolean finished;

	public PugEatDroppedFoodGoal(PugEntity pug) {
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

		AABB area = this.pug.getBoundingBox().inflate(SEARCH_RANGE);
		List<ItemEntity> snacks = this.level.getEntities(EntityTypeTest.forClass(ItemEntity.class), area, item ->
				item.isAlive() && this.pug.isPugSnack(item.getItem())
		);
		if (snacks.isEmpty()) {
			return false;
		}

		this.targetItem = snacks.stream()
				.min(Comparator.comparingDouble(this.pug::distanceToSqr))
				.orElseThrow();
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return !this.finished
				&& this.targetItem != null
				&& this.targetItem.isAlive()
				&& !this.targetItem.isRemoved()
				&& this.pug.canPugRoam();
	}

	@Override
	public void start() {
		this.delayTicks = 0;
		this.finished = false;
	}

	@Override
	public void tick() {
		if (this.targetItem == null) {
			this.finished = true;
			return;
		}

		this.pug.getLookControl().setLookAt(this.targetItem, 10.0F, this.pug.getMaxHeadXRot());
		double distance = this.pug.distanceTo(this.targetItem);

		if (distance > REACH_DISTANCE) {
			this.pug.getNavigation().moveTo(this.targetItem, 1.2D);
			this.delayTicks = 0;
		} else {
			this.pug.getNavigation().stop();
			this.delayTicks++;
			if (this.delayTicks >= EAT_DELAY_TICKS) {
				this.targetItem.discard();
				this.pug.playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, 1.2F);
				this.cooldownTicks = EAT_COOLDOWN_TICKS;
				this.finished = true;
			}
		}
	}

	@Override
	public void stop() {
		this.targetItem = null;
		this.delayTicks = 0;
		this.finished = false;
	}
}
