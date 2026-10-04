package com.minepug;

import com.minepug.entity.ai.PugAlertGoal;
import com.minepug.entity.ai.PugChaseThreatGoal;
import com.minepug.entity.ai.PugClingyGoal;
import com.minepug.entity.ai.PugCuriosityGoal;
import com.minepug.entity.ai.PugDisobedienceGoal;
import com.minepug.entity.ai.PugEatDroppedFoodGoal;
import com.minepug.entity.ai.PugWatchFurnaceGoal;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * El carlino (pug): un perro que nace al criar lobos domesticados de varias generaciones.
 * <p>
 * Extiende {@link Wolf} y añade su propia personalidad:
 * <ul>
 *   <li>Hace menos daño y no puede llevar armadura.</li>
 *   <li>Ladra (más agudo que el del lobo) cuando detecta monstruos cerca, sentado o no,
 *       y persigue a los que puede alcanzar sin llegar a atacarlos.</li>
 *   <li>Olisquea los objetos del suelo y se come la comida que se cae (con un breve retraso).</li>
 *   <li>Si hay comida en un horno cercano, se sienta a mirarlo.</li>
 *   <li>Se acerca con curiosidad a jugadores, animales, monstruos y objetos.</li>
 *   <li>Hace zoomies al encontrarse con mobs pacíficos, al reencontrarse con perros
 *       que no veía desde hace rato, o de forma aleatoria poco frecuente.</li>
 * </ul>
 * Además cada carlino tiene dos niveles ocultos que deciden su carácter:
 * <ul>
 *   <li><b>Faldero</b> (0 = independiente, 1 = pegado al dueño): cuanto más faldero,
 *       más cerca se queda del usuario y menos caso hace a las cosas ambientales.</li>
 *   <li><b>Obediencia</b> (0 = desobediente, 1 = obediente): con poca obediencia es más
 *       fácil que se levante a hacer cosas aunque le hayas mandado sentarse; si además
 *       es faldero, se irá detrás de ti si puede alcanzarte.</li>
 * </ul>
 * A pesar de todo... es un carlino.
 */
public class PugEntity extends Wolf {
	public static final Identifier PUG_TEXTURE = Minepug.id("textures/entity/pug.png");

	private static final EntityDataAccessor<Boolean> DATA_VOLUNTARY_SITTING =
			SynchedEntityData.defineId(PugEntity.class, EntityDataSerializers.BOOLEAN);

	/** Nivel oculto de faldero: 0 = independiente, 1 = faldero total. */
	private float clinginess;
	/** Nivel oculto de obediencia: 0 = desobediente, 1 = obediente total. */
	private float obedience;

	public PugEntity(EntityType<? extends Wolf> entityType, Level level) {
		super(entityType, level);
		this.clinginess = this.random.nextFloat();
		this.obedience = this.random.nextFloat();
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
		builder.define(DATA_VOLUNTARY_SITTING, false);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		// La persecución va por delante de la alerta: si puede perseguir, persigue;
		// si está sentado (bandera MOVE bloqueada), solo alerta con la mirada.
		this.goalSelector.addGoal(3, new PugChaseThreatGoal(this));
		this.goalSelector.addGoal(3, new PugDisobedienceGoal(this));
		this.goalSelector.addGoal(4, new PugAlertGoal(this));
		this.goalSelector.addGoal(4, new PugZoomiesGoal(this));
		this.goalSelector.addGoal(4, new PugWatchFurnaceGoal(this));
		this.goalSelector.addGoal(5, new PugEatDroppedFoodGoal(this));
		this.goalSelector.addGoal(5, new PugCuriosityGoal(this));
		this.goalSelector.addGoal(7, new PugClingyGoal(this));
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
	 * Comida que el carlino reconoce: la comida de lobo (huesos)
	 * y cualquier alimento con propiedades de comida (carne, etc.).
	 */
	public boolean isPugSnack(ItemStack stack) {
		return stack.is(ItemTags.WOLF_FOOD) || stack.has(DataComponents.FOOD);
	}

	/** ¿Es comida (para mirar si hay en un horno)? */
	public boolean isPugFood(ItemStack stack) {
		return !stack.isEmpty() && stack.has(DataComponents.FOOD);
	}

	public float getClinginess() {
		return this.clinginess;
	}

	public float getObedience() {
		return this.obedience;
	}

	/**
	 * Escala de distancias para las actividades ambientales: cuanto menos faldero
	 * y menos obediente, más lejos se va a hacer sus cosas.
	 */
	public double getAmbientDistanceMultiplier() {
		return (0.5D + 0.6D * (1.0D - this.clinginess)) * (0.6D + 0.6D * (1.0D - this.obedience));
	}

	/**
	 * ¿Es un faldero desobediente con dueño cerca de quien no debería separarse?
	 * (Si puede llegar hasta él, se irá detrás.)
	 */
	public boolean shouldGoToOwner() {
		if (this.clinginess <= 0.5F || this.obedience >= 0.5F) {
			return false;
		}
		LivingEntity owner = this.getOwner();
		return owner != null && this.distanceTo(owner) < 24.0D;
	}

	/**
	 * Fase "sentado voluntario" (mirando el horno o descansando tras los zoomies):
	 * visualmente sentado, pero sin contar como la orden de sentarse del jugador.
	 */
	public boolean isVoluntarySitting() {
		return this.entityData.get(DATA_VOLUNTARY_SITTING);
	}

	public void setVoluntarySitting(boolean voluntarySitting) {
		this.entityData.set(DATA_VOLUNTARY_SITTING, voluntarySitting);
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

	/** Cooldown compartido de ladrido (lo usan el goal de alerta y el de persecución). */
	private int pugLastBarkTick = -1000;

	public boolean canPugBark(int cooldownTicks) {
		return this.tickCount - this.pugLastBarkTick >= cooldownTicks;
	}

	public void markPugBarked() {
		this.pugLastBarkTick = this.tickCount;
	}

	/**
	 * El carlino puede vagar (comer, curiosear, zoomies, vigilar el horno)
	 * solo si no está sentado por orden, no está en fase de descanso, no está
	 * en celo y no tiene un objetivo de combate.
	 */
	public boolean canPugRoam() {
		return !this.isInSittingPose()
				&& !this.isVoluntarySitting()
				&& !this.isInLove()
				&& this.getTarget() == null;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putFloat("minepug_clinginess", this.clinginess);
		output.putFloat("minepug_obedience", this.obedience);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.clinginess = input.getFloatOr("minepug_clinginess", this.clinginess);
		this.obedience = input.getFloatOr("minepug_obedience", this.obedience);
	}

	/**
	 * Cuando se usa un huevo generador sobre un carlino adulto, el juego pide la
	 * cría con el propio animal como "pareja" ({@code partner == this}). En ese
	 * caso la cría debe ser un carlino y no un lobo. La cría por reproducción
	 * normal pasa por {@code super} y la decide {@code AnimalMixin}.
	 * <p>
	 * Además, como red de seguridad, si la pareja es otro carlino la cría
	 * siempre es un carlino.
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
		if (partner instanceof PugEntity) {
			PugEntity baby = MinepugEntityTypes.PUG.create(level, EntitySpawnReason.BREEDING);
			if (baby != null) {
				if (this.isTame()) {
					baby.setOwnerReference(this.getOwnerReference());
					baby.setTame(true, true);
					((MinepugWolf) baby).minepug$setCollarColor(this.getCollarColor());
				}
				((MinepugWolf) baby).minepug$setGeneration(((MinepugWolf) this).minepug$getGeneration() + 1);
			}
			return baby;
		}
		return super.getBreedOffspring(level, partner);
	}
}
