package com.minepug;

import com.minepug.entity.ai.PugAlertGoal;
import com.minepug.entity.ai.PugCuriosityGoal;
import com.minepug.entity.ai.PugEatDroppedFoodGoal;
import com.minepug.entity.ai.PugZoomiesGoal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * El carlino (pug): un perro que nace al criar lobos domesticados de varias generaciones.
 * <p>
 * Extiende {@link Wolf}, así que hereda la IA y el comportamiento de mascota
 * (sentarse, seguir al dueño...), con estas diferencias:
 * <ul>
 *   <li>Hace menos daño y no puede llevar armadura.</li>
 *   <li>Ladra (más agudo que el del lobo) cuando detecta monstruos cerca, sentado o no.</li>
 *   <li>Se come la comida que se cae al suelo (con un breve retraso).</li>
 *   <li>Se acerca con curiosidad a jugadores, animales, monstruos y objetos.</li>
 *   <li>Hace zoomies al encontrarse con mobs pacíficos, al reencontrarse con
 *       perros que no veía desde hace rato, o de forma aleatoria poco frecuente.</li>
 * </ul>
 */
public class PugEntity extends Wolf {
	public static final Identifier PUG_TEXTURE = Minepug.id("textures/entity/pug.png");

	private static final EntityDataAccessor<Boolean> DATA_ZOOMIES_SITTING =
			SynchedEntityData.defineId(PugEntity.class, EntityDataSerializers.BOOLEAN);

	public PugEntity(EntityType<? extends Wolf> entityType, Level level) {
		super(entityType, level);
	}

	/**
	 * Atributos de lobo, pero con menos daño y un 30 % más pequeño.
	 */
	public static AttributeSupplier.Builder createAttributes() {
		return Wolf.createAttributes()
				.add(Attributes.SCALE, 0.7D)
				.add(Attributes.ATTACK_DAMAGE, 2.0D);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_ZOOMIES_SITTING, false);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(3, new PugAlertGoal(this));
		this.goalSelector.addGoal(4, new PugZoomiesGoal(this));
		this.goalSelector.addGoal(5, new PugEatDroppedFoodGoal(this));
		this.goalSelector.addGoal(5, new PugCuriosityGoal(this));
	}

	/**
	 * El renderer usa esta textura en lugar de las variantes del lobo.
	 */
	@Override
	public Identifier getTexture() {
		return PUG_TEXTURE;
	}

	/**
	 * Los carlinos no pueden llevar armadura de lobo.
	 */
	@Override
	public boolean isEquippableInSlot(ItemStack stack, EquipmentSlot slot) {
		if (slot == EquipmentSlot.BODY) {
			return false;
		}
		return super.isEquippableInSlot(stack, slot);
	}

	/**
	 * Comida que el carlino recoge del suelo: la comida de lobo (huesos)
	 * y cualquier alimento con propiedades de comida (carne, etc.).
	 */
	public boolean isPugSnack(ItemStack stack) {
		return stack.is(ItemTags.WOLF_FOOD) || stack.has(DataComponents.FOOD);
	}

	/**
	 * El carlino puede vagar (comer, curiosear, zoomies) solo si no está
	 * sentado por orden, no está en fase de descanso, no está en celo y no
	 * tiene un objetivo de combate.
	 */
	public boolean canPugRoam() {
		return !this.isInSittingPose()
				&& !this.isZoomiesSitting()
				&& !this.isInLove()
				&& this.getTarget() == null;
	}

	/**
	 * Fase "sentado" de los zoomies: visualmente sentado, pero sin contar
	 * como la orden de sentarse del jugador.
	 */
	public boolean isZoomiesSitting() {
		return this.entityData.get(DATA_ZOOMIES_SITTING);
	}

	public void setZoomiesSitting(boolean zoomiesSitting) {
		this.entityData.set(DATA_ZOOMIES_SITTING, zoomiesSitting);
	}

	/**
	 * Memoria de mobs vistos recientemente (para detectar reencuentros).
	 * Solo se usa en el servidor.
	 */
	private final Map<UUID, Integer> pugKnownMobs = new HashMap<>();

	/** ¿Se vio a este mob hace menos de {@code ticks}? */
	public boolean pugHasSeenRecently(UUID id, int ticks) {
		Integer lastSeen = this.pugKnownMobs.get(id);
		return lastSeen != null && (this.tickCount - lastSeen) < ticks;
	}

	/** Registra que el carlino acaba de ver a este mob. */
	public void pugMarkSeen(UUID id) {
		if (this.pugKnownMobs.size() > 64 && !this.pugKnownMobs.containsKey(id)) {
			this.pugKnownMobs.entrySet().stream()
					.min(Comparator.comparingInt(Map.Entry::getValue))
					.ifPresent(entry -> this.pugKnownMobs.remove(entry.getKey()));
		}
		this.pugKnownMobs.put(id, this.tickCount);
	}

	/**
	 * Cuando se usa un huevo generador sobre un carlino adulto, el juego pide la
	 * cría con el propio animal como "pareja" ({@code partner == this}). En ese
	 * caso la cría debe ser un carlino y no un lobo. La cría por reproducción
	 * normal pasa por {@code super} y la decide {@code AnimalMixin}.
	 */
	@Override
	public Wolf getBreedOffspring(ServerLevel level, AgeableMob partner) {
		if (partner == this) {
			PugEntity baby = MinepugEntityTypes.PUG.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
			if (baby == null) {
				return null;
			}
			if (this.isTame()) {
				baby.setOwnerReference(this.getOwnerReference());
				baby.setTame(true, true);
				((MinepugWolf) baby).minepug$setCollarColor(this.getCollarColor());
			}
			return baby;
		}
		return super.getBreedOffspring(level, partner);
	}
}
