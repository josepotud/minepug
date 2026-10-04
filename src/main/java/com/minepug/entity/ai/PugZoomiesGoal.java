package com.minepug.entity.ai;

import com.minepug.PugEntity;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Zoomies: el carlino corre en círculos unos segundos (con polvo en las
 * patas), luego se sienta unos segundos y después retoma su vida. Se disparan:
 * <ul>
 *   <li>al reencontrarte con él tras un rato sin verlo (te saluda con
 *       ladridos contentos),</li>
 *   <li>al encontrarse con un mob pacífico (probabilidad alta),</li>
 *   <li>al reencontrarse con un perro al que no veía desde hace un rato
 *       (probabilidad media; si es otro carlino, juega a perseguirlo),</li>
 *   <li>y de forma aleatoria poco frecuente (a veces se persigue la cola).</li>
 * </ul>
 */
public class PugZoomiesGoal extends Goal {
	private static final int SIT_TIME = 60;
	private static final int MAX_EXTRA_SIT_TIME = 40;
	private static final int RUN_TIME = 80;
	private static final int MAX_EXTRA_RUN_TIME = 60;
	private static final int COOLDOWN_AFTER_ZOOMIES = 600;
	private static final int MAX_EXTRA_COOLDOWN_AFTER_ZOOMIES = 600;
	private static final double RUN_SPEED = 1.5D;
	private static final double ANGLE_STEP = Math.PI / 45.0;
	private static final double TAIL_CHASE_ANGLE_STEP = Math.PI / 18.0;

	private static final double ENCOUNTER_RANGE = 8.0D;
	private static final int SCAN_INTERVAL = 10;
	private static final int PASSIVE_MEMORY_TICKS = 600;
	private static final int DOG_MEMORY_TICKS = 3600;
	private static final float PASSIVE_ENCOUNTER_CHANCE = 0.75F;
	private static final float DOG_REUNION_CHANCE = 0.5F;
	private static final int RANDOM_ZOOMIES_CHANCE = 1400;
	private static final int OWNER_REUNION_TICKS = 2400;
	private static final float OWNER_REUNION_CHANCE = 0.9F;
	private static final float TAIL_CHASE_RANDOM_CHANCE = 0.3F;

	private enum ZoomiesType {
		CIRCLES,
		TAIL_CHASE
	}

	private final PugEntity pug;
	private final ServerLevel level;
	private int cooldownTicks;
	private int scanTicks;
	private int runTicks;
	private int sitTicks;
	private int particleTicks;
	private boolean sitting;
	private boolean done;
	private boolean ownerReunion;
	private Vec3 center;
	private Vec3 playCenter;
	private double circleAngle;
	private double circleRadius;
	private double circleDirection;
	private double angleStep;
	private ZoomiesType zoomiesType = ZoomiesType.CIRCLES;
	private Wolf playPartner;

	public PugZoomiesGoal(PugEntity pug) {
		this.pug = pug;
		this.level = getServerLevel(pug);
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		this.cooldownTicks = COOLDOWN_AFTER_ZOOMIES + pug.getRandom().nextInt(MAX_EXTRA_COOLDOWN_AFTER_ZOOMIES);
	}

	@Override
	public boolean canUse() {
		if (this.cooldownTicks > 0) {
			this.cooldownTicks--;
			return false;
		}
		if (!this.pug.canPugRoam() || this.pug.isInWater() || !this.pug.onGround()) {
			return false;
		}

		this.ownerReunion = false;
		this.playPartner = null;

		if (this.checkEncounters()) {
			this.zoomiesType = ZoomiesType.CIRCLES;
			return true;
		}

		// Zoomies aleatorios, poco frecuentes (más raros si es faldero).
		int divisor = (int) (RANDOM_ZOOMIES_CHANCE * (1.0D + this.pug.getClinginess()));
		if (this.pug.getRandom().nextInt(divisor) == 0) {
			this.zoomiesType = this.pug.getRandom().nextFloat() < TAIL_CHASE_RANDOM_CHANCE
					? ZoomiesType.TAIL_CHASE
					: ZoomiesType.CIRCLES;
			return true;
		}
		return false;
	}

	/**
	 * Escanea lo que tiene alrededor: el dueño (reencuentro), mobs pacíficos y
	 * perros conocidos o no. Cada mob se recuerda como "visto".
	 */
	private boolean checkEncounters() {
		this.scanTicks--;
		if (this.scanTicks > 0) {
			return false;
		}
		this.scanTicks = SCAN_INTERVAL;

		boolean trigger = false;

		// Reencuentro con el dueño.
		LivingEntity owner = this.pug.getOwner();
		if (owner != null) {
			double ownerDistance = this.pug.distanceTo(owner);
			if (ownerDistance <= ENCOUNTER_RANGE * 1.5D) {
				if (this.pug.pugOwnerAwaySince(OWNER_REUNION_TICKS)
						&& this.pug.getRandom().nextFloat() < OWNER_REUNION_CHANCE) {
					this.ownerReunion = true;
					trigger = true;
				}
				this.pug.pugMarkOwnerSeen();
			}
		}

		double encounterRange = ENCOUNTER_RANGE * this.pug.getAmbientDistanceMultiplier();
		AABB area = this.pug.getBoundingBox().inflate(encounterRange);
		List<Animal> animals = this.level.getEntities(EntityTypeTest.forClass(Animal.class), area, animal ->
				animal != this.pug && animal.isAlive()
		);

		for (Animal animal : animals) {
			boolean isDog = animal instanceof Wolf;
			int memoryTicks = isDog ? DOG_MEMORY_TICKS : PASSIVE_MEMORY_TICKS;
			boolean newEncounter = !this.pug.pugHasSeenRecently(animal.getUUID(), memoryTicks);
			this.pug.pugMarkSeen(animal.getUUID());

			if (newEncounter) {
				// Los falderos hacen menos caso a las cosas ambientales.
				float chance = (isDog ? DOG_REUNION_CHANCE : PASSIVE_ENCOUNTER_CHANCE)
						* (1.0F - 0.5F * this.pug.getClinginess());
				if (this.pug.getRandom().nextFloat() < chance) {
					trigger = true;
					if (isDog) {
						// Juega con el perro: círculos alrededor de él.
						this.playPartner = (Wolf) animal;
					}
				}
			}
		}
		return trigger;
	}

	@Override
	public boolean canContinueToUse() {
		return !this.done && !this.pug.isInWater() && !this.pug.isOrderedToSit();
	}

	@Override
	public void start() {
		this.done = false;
		this.sitting = false;
		this.particleTicks = 0;
		this.center = this.pug.position();
		this.circleAngle = this.pug.getRandom().nextDouble() * Math.PI * 2.0;
		this.circleDirection = this.pug.getRandom().nextBoolean() ? 1.0D : -1.0D;

		if (this.zoomiesType == ZoomiesType.TAIL_CHASE) {
			// Persecución de la cola: círculo pequeñito y rápido.
			this.angleStep = TAIL_CHASE_ANGLE_STEP;
			this.circleRadius = 0.9D;
			this.runTicks = 50 + this.pug.getRandom().nextInt(40);
			this.sitTicks = 40 + this.pug.getRandom().nextInt(20);
		} else {
			this.angleStep = ANGLE_STEP;
			this.circleRadius = this.playPartner != null ? 2.5D : 2.5D + this.pug.getRandom().nextDouble() * 1.5D;
			this.runTicks = RUN_TIME + this.pug.getRandom().nextInt(MAX_EXTRA_RUN_TIME);
			this.sitTicks = SIT_TIME + this.pug.getRandom().nextInt(MAX_EXTRA_SIT_TIME);
		}

		// Saludo contento al reencontrarse contigo.
		if (this.ownerReunion && this.pug.canPugBark(40)) {
			this.pug.markPugBarked();
			this.pug.pugPlaySound(Identifier.withDefaultNamespace("entity.wolf.growl"), 1.0F, 1.5F);
		}

		this.pug.getNavigation().stop();
	}

	@Override
	public void tick() {
		if (this.done) {
			return;
		}

		if (this.sitting) {
			this.sitTicks--;
			if (this.sitTicks <= 0) {
				this.pug.setVoluntarySitting(false);
				this.done = true;
				this.cooldownTicks = COOLDOWN_AFTER_ZOOMIES + this.pug.getRandom().nextInt(MAX_EXTRA_COOLDOWN_AFTER_ZOOMIES);
			}
			return;
		}

		// Si juega con otro perro, el círculo va alrededor de él.
		if (this.playPartner != null && this.playPartner.isAlive()) {
			this.playCenter = this.playPartner.position();
		}
		Vec3 pivot = this.playCenter != null ? this.playCenter : this.center;

		// Movimiento continuo con el controlador de movimiento (sin pathfinder):
		// el objetivo avanza un poquito cada tick, así el círculo sale fluido.
		this.circleAngle += this.circleDirection * this.angleStep;
		double targetX = pivot.x + Math.cos(this.circleAngle) * this.circleRadius;
		double targetZ = pivot.z + Math.sin(this.circleAngle) * this.circleRadius;
		this.pug.getMoveControl().setWantedPosition(targetX, pivot.y, targetZ, RUN_SPEED);

		// Polvo en las patas mientras corre.
		this.particleTicks++;
		if (this.particleTicks % 4 == 0) {
			this.level.sendParticles(ParticleTypes.CLOUD,
					this.pug.getX(), this.pug.getY() + 0.1D, this.pug.getZ(),
					2, 0.25D, 0.05D, 0.25D, 0.01D);
		}

		this.runTicks--;
		if (this.runTicks <= 0) {
			this.sitting = true;
			this.pug.setVoluntarySitting(true);
			this.pug.getNavigation().stop();
			this.pug.getMoveControl().setWait();
		}
	}

	@Override
	public void stop() {
		if (this.sitting) {
			this.pug.setVoluntarySitting(false);
		}
		this.pug.getMoveControl().setWait();
		this.pug.getNavigation().stop();
		this.sitting = false;
		this.done = false;
		this.playPartner = null;
		this.playCenter = null;
	}
}
