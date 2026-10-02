package com.minepug.entity.ai;

import com.minepug.PugEntity;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Zoomies: el carlino corre en círculos unos segundos (con partículas de
 * polvo en las patas para que se note), luego se sienta unos segundos y
 * después retoma su actividad normal. Se disparan:
 * <ul>
 *   <li>al encontrarse con un mob pacífico (probabilidad alta),</li>
 *   <li>al reencontrarse con un perro al que no veía desde hace un rato
 *       (probabilidad media, no siempre),</li>
 *   <li>y de forma aleatoria, poco frecuente.</li>
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

	private static final double ENCOUNTER_RANGE = 8.0D;
	private static final int SCAN_INTERVAL = 10;
	private static final int PASSIVE_MEMORY_TICKS = 600;
	private static final int DOG_MEMORY_TICKS = 3600;
	private static final float PASSIVE_ENCOUNTER_CHANCE = 0.75F;
	private static final float DOG_REUNION_CHANCE = 0.5F;
	private static final int RANDOM_ZOOMIES_CHANCE = 1400;

	private final PugEntity pug;
	private final ServerLevel level;
	private int cooldownTicks;
	private int scanTicks;
	private int runTicks;
	private int sitTicks;
	private int particleTicks;
	private int nextTurnTicks;
	private boolean sitting;
	private boolean done;
	private Vec3 center;
	private double circleAngle;
	private double circleRadius;
	private double circleDirection;

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

		if (this.checkEncounters()) {
			return true;
		}

		// Zoomies aleatorios, poco frecuentes.
		return this.pug.getRandom().nextInt(RANDOM_ZOOMIES_CHANCE) == 0;
	}

	/**
	 * Escanea los mobs cercanos. Cada mob se recuerda como "visto": si vuelve
	 * a encontrárselo tras mucho tiempo sin verlo, cuenta como reencuentro.
	 * Los perros (lobos y carlinos) usan una memoria más larga y una
	 * probabilidad menor que los mobs pacíficos.
	 */
	private boolean checkEncounters() {
		this.scanTicks--;
		if (this.scanTicks > 0) {
			return false;
		}
		this.scanTicks = SCAN_INTERVAL;

		AABB area = this.pug.getBoundingBox().inflate(ENCOUNTER_RANGE);
		List<Animal> animals = this.level.getEntities(EntityTypeTest.forClass(Animal.class), area, animal ->
				animal != this.pug && animal.isAlive()
		);

		boolean trigger = false;
		for (Animal animal : animals) {
			boolean isDog = animal instanceof Wolf;
			int memoryTicks = isDog ? DOG_MEMORY_TICKS : PASSIVE_MEMORY_TICKS;
			boolean newEncounter = !this.pug.pugHasSeenRecently(animal.getUUID(), memoryTicks);
			this.pug.pugMarkSeen(animal.getUUID());

			if (newEncounter) {
				float chance = isDog ? DOG_REUNION_CHANCE : PASSIVE_ENCOUNTER_CHANCE;
				if (this.pug.getRandom().nextFloat() < chance) {
					trigger = true;
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
		this.runTicks = RUN_TIME + this.pug.getRandom().nextInt(MAX_EXTRA_RUN_TIME);
		this.sitTicks = SIT_TIME + this.pug.getRandom().nextInt(MAX_EXTRA_SIT_TIME);
		this.particleTicks = 0;
		this.center = this.pug.position();
		this.circleAngle = this.pug.getRandom().nextDouble() * Math.PI * 2.0;
		this.circleRadius = 2.5D + this.pug.getRandom().nextDouble() * 1.5D;
		this.circleDirection = this.pug.getRandom().nextBoolean() ? 1.0D : -1.0D;
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
				this.cooldownTicks = COOLDOWN_AFTER_ZOOMIES + this.pug.getRandom().nextInt(MAX_EXTRA_COOLDOWN_AFTER_ZOOMIES);
			}
			return;
		}

		// Polvo en las patas y chispas de alegría mientras corre en círculos.
		this.particleTicks++;
		if (this.particleTicks % 4 == 0) {
			this.level.sendParticles(ParticleTypes.CLOUD,
					this.pug.getX(), this.pug.getY() + 0.1D, this.pug.getZ(),
					2, 0.25D, 0.05D, 0.25D, 0.01D);
		}
		if (this.particleTicks % 12 == 0) {
			this.level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
					this.pug.getX(), this.pug.getY() + this.pug.getBbHeight() + 0.2D, this.pug.getZ(),
					2, 0.3D, 0.2D, 0.3D, 0.0D);
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
			this.level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
					this.pug.getX(), this.pug.getY() + this.pug.getBbHeight() + 0.3D, this.pug.getZ(),
					5, 0.35D, 0.25D, 0.35D, 0.0D);
		}
	}

	private void pickNextCirclePoint() {
		// Avanza el ángulo un paso fijo: el recorrido forma un círculo.
		this.circleAngle += this.circleDirection * (Math.PI / 6.0);
		double x = this.center.x + Math.cos(this.circleAngle) * this.circleRadius;
		double z = this.center.z + Math.sin(this.circleAngle) * this.circleRadius;
		this.pug.getNavigation().moveTo(x, this.center.y, z, RUN_SPEED);
		this.nextTurnTicks = 10;
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
