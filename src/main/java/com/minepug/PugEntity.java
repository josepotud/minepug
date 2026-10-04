package com.minepug;

import com.minepug.entity.ai.PugAlertGoal;
import com.minepug.entity.ai.PugBegGoal;
import com.minepug.entity.ai.PugChaseThreatGoal;
import com.minepug.entity.ai.PugClingyGoal;
import com.minepug.entity.ai.PugCoolOffGoal;
import com.minepug.entity.ai.PugCuriosityGoal;
import com.minepug.entity.ai.PugDisobedienceGoal;
import com.minepug.entity.ai.PugEatDroppedFoodGoal;
import com.minepug.entity.ai.PugFleeCreeperGoal;
import com.minepug.entity.ai.PugSplootGoal;
import com.minepug.entity.ai.PugWatchFurnaceGoal;
import com.minepug.entity.ai.PugZoomiesGoal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
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
 *   <li>Olisquea los objetos del suelo; si son comida, se los come.</li>
 *   <li>Si hay comida en un horno cercano, se sienta a mirarlo.</li>
 *   <li>Se acerca con curiosidad a jugadores, animales, monstruos y objetos.</li>
 *   <li>Hace zoomies al encontrarte tras un rato sin verte, al encontrarse con mobs
 *       pacíficos, al reencontrarse con perros, o de forma aleatoria (y a veces se
 *       persigue la cola).</li>
 *   <li>Se tumba panza abajo (sploot) cuando está tranquilo.</li>
 * </ul>
 * Cada carlino tiene dos niveles ocultos que deciden su carácter:
 * <ul>
 *   <li><b>Faldero</b> (0 = independiente, 1 = pegado al dueño).</li>
 *   <li><b>Obediencia</b> (0 = desobediente, 1 = obediente).</li>
 * </ul>
 * Ambos se heredan de los padres (promedio + variación). También puede ser
 * <b>carlino negro</b>, una variante más rara que se hereda igualmente.
 * A pesar de todo... es un carlino.
 */
public class PugEntity extends Wolf {
	public static final Identifier PUG_TEXTURE = Minepug.id("textures/entity/pug.png");
	public static final Identifier PUG_BLACK_TEXTURE = Minepug.id("textures/entity/pug_black.png");

	private static final EntityDataAccessor<Boolean> DATA_VOLUNTARY_SITTING =
			SynchedEntityData.defineId(PugEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DATA_VOLUNTARY_LYING =
			SynchedEntityData.defineId(PugEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DATA_BLACK =
			SynchedEntityData.defineId(PugEntity.class, EntityDataSerializers.BOOLEAN);

	/** Nivel oculto de faldero: 0 = independiente, 1 = faldero total. */
	private float clinginess;
	/** Nivel oculto de obediencia: 0 = desobediente, 1 = obediente total. */
	private float obedience;

	public PugEntity(EntityType<? extends Wolf> entityType, Level level) {
		super(entityType, level);
		this.clinginess = this.random.nextFloat();
		this.obedience = this.random.nextFloat();
		// Al nacer (huevo, comandos...) hay una probabilidad pequeña de salir negro.
		this.setBlack(this.random.nextFloat() < 0.15F);
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
		builder.define(DATA_VOLUNTARY_LYING, false);
		builder.define(DATA_BLACK, false);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		// La persecución va por delante de la alerta: si puede perseguir, persigue;
		// si está sentado (bandera MOVE bloqueada), solo alerta con la mirada.
		// El miedo a los creepers gana a todo lo demás.
		this.goalSelector.addGoal(3, new PugFleeCreeperGoal(this));
		this.goalSelector.addGoal(3, new PugChaseThreatGoal(this));
		this.goalSelector.addGoal(3, new PugDisobedienceGoal(this));
		this.goalSelector.addGoal(4, new PugAlertGoal(this));
		this.goalSelector.addGoal(4, new PugZoomiesGoal(this));
		this.goalSelector.addGoal(4, new PugWatchFurnaceGoal(this));
		this.goalSelector.addGoal(4, new PugBegGoal(this));
		this.goalSelector.addGoal(5, new PugEatDroppedFoodGoal(this));
		this.goalSelector.addGoal(5, new PugCuriosityGoal(this));
		this.goalSelector.addGoal(5, new PugCoolOffGoal(this));
		this.goalSelector.addGoal(6, new PugSplootGoal(this));
		this.goalSelector.addGoal(7, new PugClingyGoal(this));
	}

	/**
	 * El renderer usa esta textura en lugar de las variantes del lobo.
	 */
	@Override
	public Identifier getTexture() {
		return this.isBlack() ? PUG_BLACK_TEXTURE : PUG_TEXTURE;
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

	/** Reproduce un sonido vanilla (del lobo) por su identificador. */
	public void pugPlaySound(Identifier soundId, float volume, float pitch) {
		this.level().registryAccess().lookupOrThrow(Registries.SOUND_EVENT)
				.get(soundId)
				.ifPresent(holder -> this.playSound(holder.value(), volume, pitch));
	}

	/** Voz más aguda que la del lobo (el grito dramático del carlino). */
	@Override
	public float getVoicePitch() {
		return 1.4F;
	}

	/** ¿Tiene calor? (bioma cálido, de día y sin lluvia) */
	public boolean isFeelingHot() {
		float temperature = this.level().getBiome(this.blockPosition()).value().getBaseTemperature();
		boolean day = this.level().getDefaultClockTime() % 24000L < 12000L;
		return temperature >= 0.9F && day && !this.level().isRaining();
	}

	/** ¿Tiene frío o está mojado? (bioma frío o lluvia donde está) */
	public boolean isColdOrWet() {
		float temperature = this.level().getBiome(this.blockPosition()).value().getBaseTemperature();
		return temperature <= 0.15F || this.level().isRainingAt(this.blockPosition());
	}

	/** Pedo ocasional cuando está tumbado o sentado tranquilamente. */
	private int pugFartTicks = 1200;

	private void pugMaybeFart() {
		if (this.level().isClientSide()) {
			return;
		}
		if (!this.isVoluntaryLying() && !this.isVoluntarySitting()) {
			return;
		}
		this.pugFartTicks--;
		if (this.pugFartTicks > 0) {
			return;
		}
		this.pugFartTicks = 1200 + this.random.nextInt(2400);
		this.pugPlaySound(Identifier.withDefaultNamespace("entity.wolf.pant"), 0.5F, 0.45F);
		if (this.level() instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.CLOUD,
					this.getX(), this.getY() + 0.2D, this.getZ(),
					2, 0.15D, 0.05D, 0.15D, 0.01D);
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();
		this.pugMaybeFart();
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

	// ------------------------------------------------------------------
	// Variante negra
	// ------------------------------------------------------------------

	public boolean isBlack() {
		return this.entityData.get(DATA_BLACK);
	}

	public void setBlack(boolean black) {
		this.entityData.set(DATA_BLACK, black);
	}

	// ------------------------------------------------------------------
	// Herencia de personalidad y color
	// ------------------------------------------------------------------

	/** Herencia de dos padres lobos (uno o los dos pueden ser carlinos). */
	public void inheritFromParents(Wolf mother, Wolf father) {
		float clinginessSum = 0.0F;
		float obedienceSum = 0.0F;
		int pugParents = 0;
		boolean motherBlack = false;
		boolean fatherBlack = false;

		if (mother instanceof PugEntity motherPug) {
			clinginessSum += motherPug.clinginess;
			obedienceSum += motherPug.obedience;
			motherBlack = motherPug.isBlack();
			pugParents++;
		}
		if (father instanceof PugEntity fatherPug) {
			clinginessSum += fatherPug.clinginess;
			obedienceSum += fatherPug.obedience;
			fatherBlack = fatherPug.isBlack();
			pugParents++;
		}

		if (pugParents > 0) {
			this.clinginess = Mth.clamp(
					clinginessSum / pugParents + (this.random.nextFloat() - 0.5F) * 0.3F, 0.0F, 1.0F);
			this.obedience = Mth.clamp(
					obedienceSum / pugParents + (this.random.nextFloat() - 0.5F) * 0.3F, 0.0F, 1.0F);
		}

		if (motherBlack && fatherBlack) {
			this.setBlack(this.random.nextFloat() < 0.75F);
		} else if (motherBlack || fatherBlack) {
			this.setBlack(this.random.nextFloat() < 0.3F);
		} else {
			this.setBlack(this.random.nextFloat() < 0.1F);
		}
	}

	/** Herencia de un único padre carlino (huevo sobre el animal). */
	public void inheritFromSingleParent(PugEntity parent) {
		this.clinginess = Mth.clamp(
				parent.clinginess + (this.random.nextFloat() - 0.5F) * 0.3F, 0.0F, 1.0F);
		this.obedience = Mth.clamp(
				parent.obedience + (this.random.nextFloat() - 0.5F) * 0.3F, 0.0F, 1.0F);
		this.setBlack(this.random.nextFloat() < (parent.isBlack() ? 0.5F : 0.15F));
	}

	// ------------------------------------------------------------------
	// Sentado / tumbado voluntario
	// ------------------------------------------------------------------

	/** Sentado voluntario (vigilando el horno o descansando tras los zoomies). */
	public boolean isVoluntarySitting() {
		return this.entityData.get(DATA_VOLUNTARY_SITTING);
	}

	public void setVoluntarySitting(boolean voluntarySitting) {
		this.entityData.set(DATA_VOLUNTARY_SITTING, voluntarySitting);
	}

	/** Tumbado voluntario (sploot, panza abajo). */
	public boolean isVoluntaryLying() {
		return this.entityData.get(DATA_VOLUNTARY_LYING);
	}

	public void setVoluntaryLying(boolean voluntaryLying) {
		this.entityData.set(DATA_VOLUNTARY_LYING, voluntaryLying);
	}

	// ------------------------------------------------------------------
	// Memoria de mobs y de dueño
	// ------------------------------------------------------------------

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

	private int pugLastOwnerSeenTick = -100000;

	/** ¿Lleva el carlino sin ver a su dueño más de {@code ticks}? */
	public boolean pugOwnerAwaySince(int ticks) {
		return this.tickCount - this.pugLastOwnerSeenTick >= ticks;
	}

	public void pugMarkOwnerSeen() {
		this.pugLastOwnerSeenTick = this.tickCount;
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
	 * El carlino puede vagar (comer, curiosear, zoomies, vigilar el horno...)
	 * solo si no está sentado por orden, no está sentado/tumbado voluntariamente,
	 * no está en celo y no tiene un objetivo de combate.
	 */
	public boolean canPugRoam() {
		return !this.isInSittingPose()
				&& !this.isVoluntarySitting()
				&& !this.isVoluntaryLying()
				&& !this.isInLove()
				&& this.getTarget() == null;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putFloat("minepug_clinginess", this.clinginess);
		output.putFloat("minepug_obedience", this.obedience);
		output.putBoolean("minepug_black", this.isBlack());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.clinginess = input.getFloatOr("minepug_clinginess", this.clinginess);
		this.obedience = input.getFloatOr("minepug_obedience", this.obedience);
		this.setBlack(input.getBooleanOr("minepug_black", this.isBlack()));
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
			baby.inheritFromSingleParent(this);
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
				baby.inheritFromParents(this, (Wolf) partner);
			}
			return baby;
		}
		return super.getBreedOffspring(level, partner);
	}
}
