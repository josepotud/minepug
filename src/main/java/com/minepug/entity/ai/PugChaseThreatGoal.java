package com.minepug.entity.ai;

import com.minepug.PugEntity;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

/**
 * Persigue a los monstruos cercanos sin llegar a atacarlos: les ladra
 * (más agudo que el lobo) y los sigue de cerca para ahuyentarlos.
 * <p>
 * No persigue a los que no conviene: creeper, ghast y warden solo se
 * vigilan con la mirada (eso lo hace {@link PugAlertGoal}, que sigue
 * funcionando cuando este goal no puede correr, por ejemplo si el
 * carlino está sentado).
 */
public class PugChaseThreatGoal extends Goal {
	private static final Identifier WOLF_GROWL = Identifier.withDefaultNamespace("entity.wolf.growl");
	private static final double DETECT_RANGE = 12.0D;
	private static final double ABANDON_RANGE = 20.0D;
	private static final double KEEP_DISTANCE = 3.5D;
	private static final int RESCAN_INTERVAL = 20;
	private static final int BARK_COOLDOWN = 60;
	private static final double CHASE_SPEED = 1.35D;

	private final PugEntity pug;
	private final ServerLevel level;
	private Mob threat;
	private int rescanTicks;

	public PugChaseThreatGoal(PugEntity pug) {
		this.pug = pug;
		this.level = getServerLevel(pug);
		this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (!this.pug.canPugRoam()) {
			return false;
		}
		this.threat = this.findThreat(DETECT_RANGE);
		return this.threat != null;
	}

	@Override
	public boolean canContinueToUse() {
		return this.threat != null
				&& this.threat.isAlive()
				&& this.pug.canPugRoam()
				&& this.pug.distanceToSqr(this.threat) <= ABANDON_RANGE * ABANDON_RANGE;
	}

	@Override
	public void start() {
		this.rescanTicks = 0;
		this.pug.setIsInterested(true);
		this.bark();
	}

	@Override
	public void tick() {
		this.rescanTicks--;
		if (this.rescanTicks <= 0) {
			Mob nearest = this.findThreat(DETECT_RANGE);
			if (nearest != null) {
				this.threat = nearest;
			}
			this.rescanTicks = RESCAN_INTERVAL;
		}

		if (this.threat == null || !this.threat.isAlive()) {
			return;
		}

		this.pug.getLookControl().setLookAt(this.threat, 20.0F, this.pug.getMaxHeadXRot());
		if (this.pug.distanceTo(this.threat) > KEEP_DISTANCE) {
			this.pug.getNavigation().moveTo(this.threat, CHASE_SPEED);
		} else {
			this.pug.getNavigation().stop();
		}
		this.bark();
	}

	@Override
	public void stop() {
		this.threat = null;
		this.pug.setIsInterested(false);
		this.pug.getNavigation().stop();
	}

	/** Ladrido de aviso, compartido con el goal de alerta (con enfriamiento). */
	private void bark() {
		if (!this.pug.canPugBark(BARK_COOLDOWN)) {
			return;
		}
		this.pug.markPugBarked();
		this.pug.level().registryAccess().lookupOrThrow(Registries.SOUND_EVENT)
				.get(WOLF_GROWL)
				.ifPresent(holder -> this.pug.playSound(holder.value(), 1.0F, 1.35F));
	}

	/**
	 * Busca el monstruo más cercano que sí conviene perseguir. Los que no
	 * (creeper, ghast, warden) se dejan para la alerta de la mirada.
	 */
	private Mob findThreat(double range) {
		AABB area = this.pug.getBoundingBox().inflate(range);
		List<Mob> hostiles = this.level.getEntities(EntityTypeTest.forClass(Mob.class), area, mob ->
				mob != this.pug
						&& mob.isAlive()
						&& mob.getType().getCategory() == MobCategory.MONSTER
						&& !(mob instanceof Creeper)
						&& !(mob instanceof Ghast)
						&& !(mob instanceof Warden)
		);
		if (hostiles.isEmpty()) {
			return null;
		}
		return hostiles.stream()
				.min(Comparator.comparingDouble(this.pug::distanceToSqr))
				.orElseThrow();
	}
}
