package com.minepug.entity.ai;

import com.minepug.PugEntity;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Juego de buscar: si al carlino le gusta (≈20 %) y hay un juguete tirado
 * lejos del dueño (la pelota lanzada, un palo...), va a por él y lo coge.
 * <ul>
 *   <li>El 40 % de los juguetones lo <b>trae</b> de vuelta al dueño.</li>
 *   <li>El resto se pone a <b>dar vueltas</b> alrededor del dueño con el
 *       juguete en la boca durante un rato y luego lo suelta.</li>
 * </ul>
 */
public class PugFetchGoal extends Goal {
	private static final double SEARCH_RANGE = 12.0D;
	private static final double OWNER_RANGE = 24.0D;
	private static final double TOY_REACH_DISTANCE = 1.2D;
	private static final double RETURN_DISTANCE = 2.5D;
	private static final double MIN_THROW_DISTANCE_FROM_OWNER = 3.0D;
	private static final int CIRCLE_TIME = 80;
	private static final int MAX_EXTRA_CIRCLE_TIME = 60;
	private static final double CIRCLE_RADIUS = 2.5D;
	private static final double CIRCLE_SPEED = 1.5D;
	private static final double ANGLE_STEP = Math.PI / 45.0;
	private static final int COOLDOWN_TICKS = 200;
	private static final int MAX_EXTRA_COOLDOWN_TICKS = 200;
	/** Tiempo máximo de posesión del juguete (para no llevarlo para siempre). */
	private static final int MAX_CARRY_TIME = 600;

	private enum Phase {
		GO_TO_TOY,
		RETURNING,
		CIRCLING
	}

	private final PugEntity pug;
	private final ServerLevel level;
	private ItemEntity toy;
	private LivingEntity owner;
	private Phase phase = Phase.GO_TO_TOY;
	private int cooldownTicks;
	private int circleTicks;
	private int carryTicks;
	private double circleAngle;
	private double circleDirection;
	private boolean carrying;
	private boolean finished;

	public PugFetchGoal(PugEntity pug) {
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
		this.owner = this.pug.getOwner();
		if (this.owner == null || this.pug.distanceTo(this.owner) > OWNER_RANGE) {
			return false;
		}
		this.toy = this.findToy();
		return this.toy != null;
	}

	/** Busca el juguete más cercano que esté "lanzado" (lejos del dueño). */
	private ItemEntity findToy() {
		double range = SEARCH_RANGE * this.pug.getAmbientDistanceMultiplier();
		AABB area = this.pug.getBoundingBox().inflate(range);
		List<ItemEntity> toys = this.level.getEntities(EntityTypeTest.forClass(ItemEntity.class), area, item ->
				item.isAlive()
						&& !item.isRemoved()
						&& item.getVehicle() == null
						&& this.pug.isFetchToy(item.getItem())
						&& this.owner != null
						&& this.owner.distanceToSqr(item) > MIN_THROW_DISTANCE_FROM_OWNER * MIN_THROW_DISTANCE_FROM_OWNER
		);
		return toys.stream()
				.min(Comparator.comparingDouble(this.pug::distanceToSqr))
				.orElse(null);
	}

	@Override
	public boolean canContinueToUse() {
		return !this.finished
				&& this.toy != null
				&& this.toy.isAlive()
				&& !this.toy.isRemoved()
				&& this.pug.canPugRoam()
				&& this.carryTicks < MAX_CARRY_TIME;
	}

	@Override
	public void start() {
		this.finished = false;
		this.carrying = false;
		this.carryTicks = 0;
		this.phase = Phase.GO_TO_TOY;
		this.circleAngle = this.pug.getRandom().nextDouble() * Math.PI * 2.0;
		this.circleDirection = this.pug.getRandom().nextBoolean() ? 1.0D : -1.0D;
	}

	@Override
	public void tick() {
		if (this.toy == null) {
			this.finished = true;
			return;
		}

		switch (this.phase) {
			case GO_TO_TOY -> this.tickGoToToy();
			case RETURNING -> this.tickReturning();
			case CIRCLING -> this.tickCircling();
		}
	}

	private void tickGoToToy() {
		this.pug.getLookControl().setLookAt(this.toy, 20.0F, this.pug.getMaxHeadXRot());
		if (this.pug.distanceTo(this.toy) > TOY_REACH_DISTANCE) {
			this.pug.getNavigation().moveTo(this.toy, 1.2D);
			return;
		}

		// Recoge el juguete: se lo lleva "en la boca" (montado encima).
		this.pug.getNavigation().stop();
		if (this.toy.startRiding(this.pug)) {
			this.toy.setPickUpDelay(32767);
			this.carrying = true;
		}
		if (this.pug.isFetchBringer()) {
			this.phase = Phase.RETURNING;
		} else {
			this.phase = Phase.CIRCLING;
			this.circleTicks = CIRCLE_TIME + this.pug.getRandom().nextInt(MAX_EXTRA_CIRCLE_TIME);
		}
	}

	private void tickReturning() {
		this.owner = this.pug.getOwner();
		if (this.owner == null) {
			this.finished = true;
			return;
		}
		this.carryTicks++;
		this.pug.getLookControl().setLookAt(this.owner, 20.0F, this.pug.getMaxHeadXRot());
		if (this.pug.distanceTo(this.owner) > RETURN_DISTANCE) {
			this.pug.getNavigation().moveTo(this.owner, 1.2D);
			return;
		}
		// Deja la pelota a los pies del dueño y se alegra.
		this.pug.getNavigation().stop();
		this.dropToy();
		this.pug.getMoveControl().setWait();
		if (this.pug.canPugBark(40)) {
			this.pug.markPugBarked();
			this.pug.pugPlaySound(Identifier.withDefaultNamespace("entity.wolf.growl"), 1.0F, 1.5F);
		}
		this.finished = true;
	}

	private void tickCircling() {
		this.owner = this.pug.getOwner();
		if (this.owner == null) {
			this.finished = true;
			return;
		}
		this.carryTicks++;

		// Vueltas alrededor del dueño con el juguete, con movimiento fluido.
		Vec3 pivot = this.owner.position();
		this.circleAngle += this.circleDirection * ANGLE_STEP;
		double targetX = pivot.x + Math.cos(this.circleAngle) * CIRCLE_RADIUS;
		double targetZ = pivot.z + Math.sin(this.circleAngle) * CIRCLE_RADIUS;
		this.pug.getMoveControl().setWantedPosition(targetX, this.owner.getY(), targetZ, CIRCLE_SPEED);

		this.circleTicks--;
		if (this.circleTicks <= 0) {
			this.dropToy();
			this.finished = true;
		}
	}

	/** Suelta el juguete en el suelo. */
	private void dropToy() {
		if (this.toy != null && this.toy.getVehicle() == this.pug) {
			this.toy.stopRiding();
			this.toy.setDefaultPickUpDelay();
		}
		this.carrying = false;
	}

	@Override
	public void stop() {
		this.dropToy();
		this.pug.getMoveControl().setWait();
		this.pug.getNavigation().stop();
		this.toy = null;
		this.finished = false;
		this.cooldownTicks = COOLDOWN_TICKS + this.pug.getRandom().nextInt(MAX_EXTRA_COOLDOWN_TICKS);
	}
}
