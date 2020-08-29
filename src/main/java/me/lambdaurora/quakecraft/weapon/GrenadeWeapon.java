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

import me.lambdaurora.quakecraft.entity.GrenadeEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.NotNull;
import xyz.nucleoid.plasmid.game.GameWorld;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;

/**
 * Represents a grenade weapon.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class GrenadeWeapon extends Weapon
{
    public GrenadeWeapon(@NotNull Item item, int cooldown)
    {
        super(item, cooldown);
    }

    @Override
    public @NotNull ActionResult onUse(@NotNull GameWorld world, @NotNull ServerPlayerEntity player, @NotNull Hand hand)
    {
        ItemStack heldStack = player.getStackInHand(hand);

        GrenadeEntity grenade = new GrenadeEntity(world.getWorld(), player);
        grenade.setItem(heldStack);
        grenade.setProperties(player, player.pitch, player.yaw, 0.f, 1.5f, 1.f);
        world.getWorld().spawnEntity(grenade);

        return super.onUse(world, player, hand);
    }

    @Override
    public @NotNull ItemStackBuilder stackBuilder()
    {
        return super.stackBuilder().setCount(64);
    }
}
