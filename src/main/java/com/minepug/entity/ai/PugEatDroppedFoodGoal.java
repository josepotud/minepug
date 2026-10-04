package com.minepug.entity.ai;

import com.minepug.PugEntity;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

/**
 * Objetos en el suelo: el carlino se acerca y los olisquea con la cabeza
 * gacha (mostrando interés). Si además es comida, se la come con un breve
 * retraso; si no, tras olisquearla pierde el interés.
 */
public class PugEatDroppedFoodGoal extends Goal {
	private static final double SEARCH_RANGE = 10.0D;
	private static final double REACH_DISTANCE = 1.5D;
	private static final int SNIFF_TICKS = 30;
	private static final int EAT_TICKS = 15;
	private static final int COOLDOWN_TICKS = 200;

	private final PugEntity pug;
	private final ServerLevel level;
	private ItemEntity targetItem;
	private int sniffTicks;
	private int eatTicks;
	private boolean sniffed;
	private boolean finished;
	private int cooldownTicks;

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

		double range = SEARCH_RANGE * this.pug.getAmbientDistanceMultiplier();
		AABB area = this.pug.getBoundingBox().inflate(range);
		List<ItemEntity> items = this.level.getEntities(EntityTypeTest.forClass(ItemEntity.class), area, item ->
				item.isAlive()
		);
		if (items.isEmpty()) {
			return false;
		}

		this.targetItem = items.stream()
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
		this.sniffTicks = 0;
		this.eatTicks = 0;
		this.sniffed = false;
		this.finished = false;
	}

	@Override
	public void tick() {
		if (this.targetItem == null) {
			this.finished = true;
			return;
		}

		// Mirar al objeto (con la cabeza gacha si está en el suelo): el olisqueo.
		this.pug.getLookControl().setLookAt(this.targetItem, 20.0F, this.pug.getMaxHeadXRot());
		double distance = this.pug.distanceTo(this.targetItem);

		if (distance > REACH_DISTANCE) {
			this.pug.getNavigation().moveTo(this.targetItem, 1.2D);
			this.sniffTicks = 0;
			this.sniffed = false;
			return;
		}

		this.pug.getNavigation().stop();

		// Fase 1: olisquear el objeto (con un bufido al empezar).
		if (!this.sniffed) {
			if (this.sniffTicks == 0) {
				this.pug.pugPlaySound(Identifier.withDefaultNamespace("entity.wolf.pant"), 0.6F, 0.7F);
			}
			this.sniffTicks++;
			if (this.sniffTicks >= SNIFF_TICKS) {
				this.sniffed = true;
				if (!this.pug.isPugSnack(this.targetItem.getItem())) {
					// No es comida: pierde el interés.
					this.cooldownTicks = COOLDOWN_TICKS;
					this.finished = true;
				}
			}
			return;
		}

		// Fase 2: es comida, se la come con un breve retraso más.
		this.eatTicks++;
		if (this.eatTicks >= EAT_TICKS) {
			this.targetItem.discard();
			this.pug.playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, 1.2F);
			this.cooldownTicks = COOLDOWN_TICKS;
			this.finished = true;
		}
	}

	@Override
	public void stop() {
		this.targetItem = null;
		this.sniffTicks = 0;
		this.eatTicks = 0;
		this.sniffed = false;
		this.finished = false;
	}
}
