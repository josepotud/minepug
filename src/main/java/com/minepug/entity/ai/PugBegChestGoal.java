package com.minepug.entity.ai;

import com.minepug.PugEntity;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Si el carlino juguetón quiere la pelota y está guardada en un cofre
 * (o barril), se sienta enfrente y ladra pidiéndola.
 */
public class PugBegChestGoal extends Goal {
	private static final double SCAN_RANGE = 6.0D;
	private static final double ARRIVE_DISTANCE = 2.5D;
	private static final double FREE_TOY_RANGE = 10.0D;
	private static final int BEG_TIME = 300;
	private static final int MAX_EXTRA_BEG_TIME = 200;
	private static final int BARK_COOLDOWN = 50;
	private static final int COOLDOWN_TICKS = 600;
	private static final int MAX_EXTRA_COOLDOWN_TICKS = 600;
	private static final int RESCAN_INTERVAL = 40;

	private final PugEntity pug;
	private final ServerLevel level;
	private BlockPos chestPos;
	private int begTicks;
	private int barkTicks;
	private int cooldownTicks;
	private int rescanTicks;
	private boolean finished;

	public PugBegChestGoal(PugEntity pug) {
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
		if (!this.pug.likesFetch() || !this.pug.canPugRoam()) {
			return false;
		}
		// Si ya hay un juguete suelto cerca, juega con él en vez de pedir.
		if (this.anyFreeToyNearby()) {
			return false;
		}
		this.chestPos = this.findChestWithToy();
		return this.chestPos != null;
	}

	@Override
	public boolean canContinueToUse() {
		return !this.finished
				&& !this.pug.isOrderedToSit()
				&& !this.pug.isInLove()
				&& this.pug.getTarget() == null;
	}

	@Override
	public void start() {
		this.finished = false;
		this.begTicks = BEG_TIME + this.pug.getRandom().nextInt(MAX_EXTRA_BEG_TIME);
		this.barkTicks = 0;
		this.rescanTicks = 0;
		this.pug.setIsInterested(true);
	}

	@Override
	public void tick() {
		if (this.chestPos == null) {
			this.finished = true;
			return;
		}

		Vec3 center = Vec3.atCenterOf(this.chestPos);
		this.pug.getLookControl().setLookAt(center.x, center.y, center.z, 20.0F, this.pug.getMaxHeadXRot());

		if (this.pug.distanceToSqr(center.x, center.y, center.z) > ARRIVE_DISTANCE * ARRIVE_DISTANCE) {
			this.pug.getNavigation().moveTo(center.x, center.y, center.z, 1.0D);
			return;
		}

		// Enfrente del cofre: se sienta a pedir y ladra de vez en cuando.
		this.pug.getNavigation().stop();
		this.pug.setVoluntarySitting(true);

		this.barkTicks--;
		if (this.barkTicks <= 0) {
			this.barkTicks = BARK_COOLDOWN;
			this.pug.pugPlaySound(Identifier.withDefaultNamespace("entity.wolf.growl"), 0.9F, 1.6F);
		}

		this.rescanTicks--;
		if (this.rescanTicks <= 0) {
			this.rescanTicks = RESCAN_INTERVAL;
			if (!this.chestStillHasToy(this.chestPos)) {
				// Ya no está: o se la han dado o se la han llevado.
				this.finished = true;
				return;
			}
			if (this.anyFreeToyNearby()) {
				// Hay una pelota suelta: mejor a por ella.
				this.finished = true;
				return;
			}
		}

		this.begTicks--;
		if (this.begTicks <= 0) {
			this.finished = true;
		}
	}

	@Override
	public void stop() {
		this.pug.setVoluntarySitting(false);
		this.pug.setIsInterested(false);
		this.pug.getNavigation().stop();
		this.chestPos = null;
		this.finished = false;
		this.cooldownTicks = COOLDOWN_TICKS + this.pug.getRandom().nextInt(MAX_EXTRA_COOLDOWN_TICKS);
	}

	/** ¿Hay algún juguete suelto por el suelo cerca? */
	private boolean anyFreeToyNearby() {
		AABB area = this.pug.getBoundingBox().inflate(FREE_TOY_RANGE);
		List<ItemEntity> toys = this.level.getEntities(EntityTypeTest.forClass(ItemEntity.class), area, item ->
				item.isAlive() && this.pug.isFetchToy(item.getItem())
		);
		return !toys.isEmpty();
	}

	/** Busca el cofre (o barril) más cercano con un juguete dentro. */
	private BlockPos findChestWithToy() {
		double range = SCAN_RANGE * this.pug.getAmbientDistanceMultiplier();
		int r = (int) Math.ceil(range);
		BlockPos centerPos = this.pug.blockPosition();
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;

		for (BlockPos pos : BlockPos.betweenClosed(
				centerPos.offset(-r, -2, -r), centerPos.offset(r, 2, r))) {
			if (this.chestStillHasToy(pos)) {
				double distance = centerPos.distSqr(pos);
				if (distance < bestDistance) {
					bestDistance = distance;
					best = pos.immutable();
				}
			}
		}
		return best;
	}

	/** ¿El contenedor de esta posición guarda algún juguete de buscar? */
	private boolean chestStillHasToy(BlockPos pos) {
		if (pos == null) {
			return false;
		}
		if (this.level.getBlockEntity(pos) instanceof Container container) {
			for (int slot = 0; slot < container.getContainerSize(); slot++) {
				if (this.pug.isFetchToy(container.getItem(slot))) {
					return true;
				}
			}
		}
		return false;
	}
}
