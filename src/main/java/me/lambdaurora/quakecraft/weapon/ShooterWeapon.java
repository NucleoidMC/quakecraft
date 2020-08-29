/*
 *  Copyright (c) 2020 LambdAurora <aurora42lambda@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.lambdaurora.quakecraft.weapon;

import me.lambdaurora.quakecraft.QuakecraftConstants;
import me.lambdaurora.quakecraft.util.RayUtils;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import org.jetbrains.annotations.NotNull;
import xyz.nucleoid.plasmid.game.GameWorld;

public class ShooterWeapon extends Weapon
{
    public ShooterWeapon(@NotNull Item item, int cooldown)
    {
        super(item, cooldown);
    }

    @Override
    public @NotNull ActionResult onUse(@NotNull GameWorld world, @NotNull ServerPlayerEntity player, @NotNull Hand hand)
    {
        EntityHitResult result = RayUtils.raycastEntity(player, 80.0, 0.5, QuakecraftConstants.PLAYER_PREDICATE);

        if (result != null) {
            ServerPlayerEntity hitPlayer = (ServerPlayerEntity) result.getEntity();

            RayUtils.drawRay(world, player, hitPlayer);

            hitPlayer.setAttacker(player);
            player.setAttacking(hitPlayer);
            hitPlayer.kill();
            return ActionResult.SUCCESS;
        }

        RayUtils.drawRay(world, player, 80.0);

        return super.onUse(world, player, hand);
    }
}
