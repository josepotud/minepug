package com.minepug.entity.ai;

import com.minepug.PugEntity;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Miedo a los creepers: en vez de perseguirlos (explosión = mal negocio),
 * el carlino corre a esconderse junto a su dueño; si no tiene dueño cerca,
 * huye en dirección contraria.
 */
public class PugFleeCreeperGoal extends Goal {
	private static final double DETECT_RANGE = 8.0D;
	private static final double SAFE_DISTANCE = 12.0D;
	private static final double OWNER_ARRIVE_DISTANCE = 2.5D;
	private static final double FLEE_SPEED = 1.4D;

	private final PugEntity pug;
	private final ServerLevel level;
	private Creeper creeper;

	public PugFleeCreeperGoal(PugEntity pug) {
		this.pug = pug;
		this.level = getServerLevel(pug);
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (!this.pug.canPugRoam()) {
			return false;
		}
		AABB area = this.pug.getBoundingBox().inflate(DETECT_RANGE);
		List<Creeper> creepers = this.level.getEntities(EntityTypeTest.forClass(Creeper.class), area, creeper ->
				creeper.isAlive()
		);
		if (creepers.isEmpty()) {
			return false;
		}
		this.creeper = creepers.stream()
				.min(Comparator.comparingDouble(this.pug::distanceToSqr))
				.orElseThrow();
		return true;
	}

	@Override
	public boolean canContinueToUse() {
		return this.creeper != null
				&& this.creeper.isAlive()
				&& this.pug.canPugRoam()
				&& this.pug.distanceTo(this.creeper) < SAFE_DISTANCE;
	}

	@Override
	public void tick() {
		if (this.creeper == null) {
			return;
		}
		this.pug.getLookControl().setLookAt(this.creeper, 20.0F, this.pug.getMaxHeadXRot());

		LivingEntity owner = this.pug.getOwner();
		if (owner != null) {
			// A esconderse con el dueño.
			if (this.pug.distanceTo(owner) > OWNER_ARRIVE_DISTANCE) {
				this.pug.getNavigation().moveTo(owner, FLEE_SPEED);
			} else {
				this.pug.getNavigation().stop();
			}
			return;
		}

		// Sin dueño: huir en dirección contraria al creeper.
		Vec3 away = this.pug.position().subtract(this.creeper.position());
		if (away.lengthSqr() < 0.01D) {
			away = new Vec3(1.0D, 0.0D, 0.0D);
		}
		away = away.normalize().scale(6.0D);
		this.pug.getNavigation().moveTo(
				this.pug.getX() + away.x,
				this.pug.getY(),
				this.pug.getZ() + away.z,
				FLEE_SPEED
		);
	}

	@Override
	public void stop() {
		this.creeper = null;
	}
}
