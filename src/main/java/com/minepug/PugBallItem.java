package com.minepug;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Pelota de carlino: se lanza a distancia con clic derecho (como una bola de
 * nieve) pero no se rompe: al aterrizar queda como objeto en el suelo y el
 * carlino puede ir a buscarla y traerla.
 */
public class PugBallItem extends Item {

	public PugBallItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		var stack = player.getItemInHand(hand);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.SNOWBALL_THROW, player.getSoundSource(), 0.6F, 1.1F);

		if (!level.isClientSide()) {
			// Se lanza como objeto con velocidad en la dirección de la mirada:
			// vuela, aterriza y se queda en el suelo (no se rompe).
			ItemEntity thrown = new ItemEntity(level,
					player.getX(), player.getEyeY() - 0.3D, player.getZ(),
					stack.copyWithCount(1));
			Vec3 look = player.getLookAngle();
			thrown.setDeltaMovement(look.x * 1.5D, look.y * 1.5D + 0.3D, look.z * 1.5D);
			thrown.setPickUpDelay(20);
			level.addFreshEntity(thrown);
		}

		stack.consume(1, player);
		return InteractionResult.SUCCESS;
	}
}
