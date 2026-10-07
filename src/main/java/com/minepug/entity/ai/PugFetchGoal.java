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
 * lejos del dueño, va a por él y lo coge.
 * <ul>
 *   <li>El 40 % de los juguetones lo <b>trae</b> de vuelta al dueño.</li>
 *   <li>El resto se pone a <b>dar vueltas</b> alrededor del dueño con el
 *       juguete en la boca un rato y luego lo suelta (también lo suelta si
 *       lleva mucho tiempo sin que se lo quiten).</li>
 *   <li>Si otro carlino quiere la misma pelota, se <b>pelean sin hacerse
 *       daño</b> (gruñidos, saltitos y caras) y uno se rinde.</li>
 *   <li>Si ya hay un juguete disponible cerca del dueño, no traen más:
 *       usan el que hay.</li>
 * </ul>
 */
public class PugFetchGoal extends Goal {
	private static final double SEARCH_RANGE = 12.0D;
	private static final double OWNER_RANGE = 24.0D;
	private static final double TOY_REACH_DISTANCE = 1.2D;
	private static final double RETURN_DISTANCE = 2.5D;
	private static final double MIN_THROW_DISTANCE_FROM_OWNER = 3.0D;
	private static final double AVAILABLE_NEAR_OWNER_RANGE = 10.0D;
	private static final int CIRCLE_TIME = 80;
	private static final int MAX_EXTRA_CIRCLE_TIME = 60;
	private static final double CIRCLE_RADIUS = 2.5D;
	private static final double CIRCLE_SPEED = 1.5D;
	private static final double ANGLE_STEP = Math.PI / 45.0;
	private static final int COOLDOWN_TICKS = 200;
	private static final int MAX_EXTRA_COOLDOWN_TICKS = 200;
	private static final int FIGHT_COOLDOWN_TICKS = 600;
	private static final int MAX_EXTRA_FIGHT_COOLDOWN = 400;
	private static final int FIGHT_TIME = 60;
	private static final int MAX_EXTRA_FIGHT_TIME = 40;
	private static final double FIGHT_DISTANCE = 1.3D;
	private static final double RIVAL_RANGE = 10.0D;
	private static final float FIGHT_WIN_CHANCE = 0.5F;
	private static final int FIGHT_BARK_COOLDOWN = 30;
	/** Tiempo máximo de posesión del juguete: luego lo tira. */
	private static final int MAX_CARRY_TIME = 400;

	private enum Phase {
		GO_TO_TOY,
		FIGHTING,
		RETURNING,
		CIRCLING
	}

	private final PugEntity pug;
	private final ServerLevel level;
	private ItemEntity toy;
	private LivingEntity owner;
	private PugEntity rival;
	private Phase phase = Phase.GO_TO_TOY;
	private int cooldownTicks;
	private int circleTicks;
	private int carryTicks;
	private int fightTicks;
	private int barkTicks;
	private double circleAngle;
	private double circleDirection;
	private boolean carrying;
	private boolean fought;
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
		// Si ya hay un juguete disponible cerca del dueño, no traen más:
		// juegan con el que ya está.
		if (this.anyToyNearOwner()) {
			return false;
		}
		this.toy = this.findToy();
		return this.toy != null;
	}

	/** ¿Hay algún juguete ya disponible cerca del dueño? */
	private boolean anyToyNearOwner() {
		if (this.owner == null) {
			return false;
		}
		AABB area = this.owner.getBoundingBox().inflate(AVAILABLE_NEAR_OWNER_RANGE);
		List<ItemEntity> toys = this.level.getEntities(EntityTypeTest.forClass(ItemEntity.class), area, item ->
				item.isAlive() && this.pug.isFetchToy(item.getItem())
		);
		return !toys.isEmpty();
	}

	/** Busca el juguete más cercano que esté "lanzado" (lejos del dueño). */
	private ItemEntity findToy() {
		double range = SEARCH_RANGE * this.pug.getAmbientDistanceMultiplier();
		AABB area = this.pug.getBoundingBox().inflate(range);
		List<ItemEntity> toys = this.level.getEntities(EntityTypeTest.forClass(ItemEntity.class), area, item ->
				item.isAlive()
						&& !item.isRemoved()
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
		this.fought = false;
		this.rival = null;
		this.phase = Phase.GO_TO_TOY;
		this.circleAngle = this.pug.getRandom().nextDouble() * Math.PI * 2.0;
		this.circleDirection = this.pug.getRandom().nextBoolean() ? 1.0D : -1.0D;
		this.pug.setPugFetchTarget(this.toy);
	}

	@Override
	public void tick() {
		if (this.toy == null) {
			this.finished = true;
			return;
		}

		// ¿Otro carlino quiere la misma pelota? Pelea sin daño.
		if (this.phase != Phase.FIGHTING) {
			PugEntity other = this.findRival();
			if (other != null) {
				this.startFight(other);
			}
		}

		switch (this.phase) {
			case GO_TO_TOY -> this.tickGoToToy();
			case FIGHTING -> this.tickFighting();
			case RETURNING -> this.tickReturning();
			case CIRCLING -> this.tickCircling();
		}
	}

	/**
	 * Busca otro carlino que quiera la misma pelota: o la está persiguiendo
	 * (lo sabemos por su objetivo de buscar) o la lleva encima.
	 */
	private PugEntity findRival() {
		if (this.toy.getVehicle() instanceof PugEntity carrier && carrier != this.pug) {
			return carrier;
		}
		AABB area = this.pug.getBoundingBox().inflate(RIVAL_RANGE);
		List<PugEntity> pugs = this.level.getEntities(EntityTypeTest.forClass(PugEntity.class), area, other ->
				other != this.pug
						&& other.isAlive()
						&& other.getPugFetchTarget() == this.toy
		);
		return pugs.stream()
				.min(Comparator.comparingDouble(this.pug::distanceToSqr))
				.orElse(null);
	}

	private void startFight(PugEntity other) {
		this.rival = other;
		this.fought = true;
		this.phase = Phase.FIGHTING;
		this.fightTicks = FIGHT_TIME + this.pug.getRandom().nextInt(MAX_EXTRA_FIGHT_TIME);
		this.barkTicks = 0;
		this.pug.getNavigation().stop();
	}

	private void tickFighting() {
		if (this.rival == null || !this.rival.isAlive()) {
			this.rival = null;
			this.phase = Phase.GO_TO_TOY;
			return;
		}

		this.pug.getLookControl().setLookAt(this.rival, 20.0F, this.pug.getMaxHeadXRot());
		if (this.pug.distanceTo(this.rival) > FIGHT_DISTANCE) {
			this.pug.getNavigation().moveTo(this.rival, 1.2D);
		} else {
			this.pug.getNavigation().stop();
		}

		// Gruñidos, saltitos y caras: pelea de mentira, sin daño.
		this.barkTicks--;
		if (this.barkTicks <= 0) {
			this.barkTicks = FIGHT_BARK_COOLDOWN;
			this.pug.pugPlaySound(Identifier.withDefaultNamespace("entity.wolf.growl"), 0.9F, 1.6F);
			if (this.pug.onGround()) {
				this.pug.getJumpControl().jump();
			}
		}

		this.fightTicks--;
		if (this.fightTicks <= 0) {
			boolean won = this.pug.getRandom().nextFloat() < FIGHT_WIN_CHANCE;
			boolean toyTakenByRival = this.toy.getVehicle() instanceof PugEntity carrier && carrier != this.pug;
			this.rival = null;
			if (won && !toyTakenByRival) {
				this.phase = Phase.GO_TO_TOY;
			} else {
				// Se rinde: a otra cosa.
				this.finished = true;
			}
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
			// Si no se la has cogido, la tira.
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
		this.pug.setPugFetchTarget(null);
		this.pug.getMoveControl().setWait();
		this.pug.getNavigation().stop();
		this.toy = null;
		this.rival = null;
		this.finished = false;
		if (this.fought) {
			this.cooldownTicks = FIGHT_COOLDOWN_TICKS + this.pug.getRandom().nextInt(MAX_EXTRA_FIGHT_COOLDOWN);
		} else {
			this.cooldownTicks = COOLDOWN_TICKS + this.pug.getRandom().nextInt(MAX_EXTRA_COOLDOWN_TICKS);
		}
	}
}
