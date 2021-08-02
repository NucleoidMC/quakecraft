/*
 * Copyright (c) 2020 LambdAurora <aurora42lambda@gmail.com>
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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import xyz.nucleoid.plasmid.game.GameSpace;

/**
 * Represents a weapon that shoot.
 *
 * @author LambdAurora
 * @version 1.6.0
 * @since 1.0.0
 */
public class ShooterWeapon extends Weapon {
    public ShooterWeapon(@NotNull Identifier id, @NotNull Item item, @NotNull Settings settings) {
        super(id, item, settings);
    }

    @Override
    public @NotNull ActionResult onPrimary(@NotNull GameSpace world, @NotNull ServerPlayerEntity player, @NotNull Hand hand) {
        var result = RayUtils.raycastEntities(player, 80.0, 0.25, QuakecraftConstants.PLAYER_PREDICATE,
                entity -> {
                    ServerPlayerEntity hitPlayer = (ServerPlayerEntity) entity;
                    hitPlayer.setAttacker(player);
                    player.setAttacking(hitPlayer);
                    hitPlayer.kill();
                });
        RayUtils.drawRay(world, player, Math.abs(result));

        if (result < 0.0)
            return ActionResult.SUCCESS;

        return super.onPrimary(world, player, hand);
    }

    @Override
    public @NotNull ActionResult onSecondary(@NotNull GameSpace world, @NotNull ServerPlayerEntity player, @NotNull ItemStack stack) {
        var rotationVec = player.getRotationVec(1.0F);
        var yVelocity = player.getVelocity().y;
        player.setVelocity(new Vec3d(rotationVec.x * QuakecraftConstants.DASH_VELOCITY, yVelocity, rotationVec.z * QuakecraftConstants.DASH_VELOCITY));
        player.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(player));

        player.playSound(SoundEvents.ENTITY_BAT_TAKEOFF, SoundCategory.MASTER, 1.0F, 0.5F);
        return super.onSecondary(world, player, stack);
    }
}
