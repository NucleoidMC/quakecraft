/*
 * Copyright (c) 2020-2022 LambdAurora <email@lambdaurora.dev>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.lambdaurora.quakecraft.weapon;

import dev.lambdaurora.quakecraft.QuakecraftConstants;
import dev.lambdaurora.quakecraft.util.RayUtils;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.plasmid.api.util.PlayerUtil;

/**
 * Represents a weapon that shoot.
 *
 * @author LambdAurora
 * @version 1.7.0
 * @since 1.0.0
 */
public class ShooterWeapon extends Weapon {
	public ShooterWeapon(Identifier id, Item item, Settings settings) {
		super(id, item, settings);
	}

	@Override
	public InteractionResult onPrimary(ServerLevel world, ServerPlayer player, InteractionHand hand) {
		var result = RayUtils.raycastEntities(player, 80.0, 0.25, QuakecraftConstants.PLAYER_PREDICATE,
				entity -> {
					var hitPlayer = (ServerPlayer) entity;
					hitPlayer.setLastHurtByMob(player);
					player.setLastHurtByPlayer(hitPlayer, 200);
					hitPlayer.kill(world);
				});
		RayUtils.drawRay(world, player, Math.abs(result));

		if (result < 0.0)
			return InteractionResult.SUCCESS;

		return super.onPrimary(world, player, hand);
	}

	@Override
	public InteractionResult onSecondary(ServerLevel world, ServerPlayer player, ItemStack stack) {
		var rotationVec = player.getViewVector(1.0F);
		var yVelocity = player.getDeltaMovement().y;
		player.setDeltaMovement(new Vec3(
				rotationVec.x * QuakecraftConstants.DASH_VELOCITY,
				yVelocity,
				rotationVec.z * QuakecraftConstants.DASH_VELOCITY
		));
		player.connection.send(new ClientboundSetEntityMotionPacket(player));

		PlayerUtil.playSoundToPlayer(player, SoundEvents.BAT_TAKEOFF, SoundSource.MASTER, 1.0F, 0.5F);
		return super.onSecondary(world, player, stack);
	}
}
