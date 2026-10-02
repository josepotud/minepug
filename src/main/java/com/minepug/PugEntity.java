package com.minepug;

import com.minepug.MinepugWolf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.Level;

/**
 * El carlino (pug): un perro que nace al criar lobos domesticados de varias generaciones.
 * <p>
 * Extiende {@link Wolf}, así que hereda toda la IA, el comportamiento de mascota
 * (sentarse, seguir al dueño, collar, armadura de lobo...) y la capacidad de criar.
 * Lo único que cambia es la textura, el tamaño (atributo {@code scale}) y el
 * seguimiento de generaciones que hace el propio mod.
 */
public class PugEntity extends Wolf {
	public static final Identifier PUG_TEXTURE = Minepug.id("textures/entity/pug.png");

	public PugEntity(EntityType<? extends Wolf> entityType, Level level) {
		super(entityType, level);
	}

	/**
	 * Atributos de lobo, pero un 30 % más pequeño.
	 */
	public static AttributeSupplier.Builder createAttributes() {
		return Wolf.createAttributes().add(Attributes.SCALE, 0.7D);
	}

	/**
	 * El renderer usa esta textura en lugar de las variantes del lobo.
	 */
	@Override
	public Identifier getTexture() {
		return PUG_TEXTURE;
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
