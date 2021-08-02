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

import dev.lambdaurora.quakecraft.entity.GrenadeEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import xyz.nucleoid.plasmid.game.GameSpace;

/**
 * Represents a grenade weapon.
 *
 * @author LambdAurora
 * @version 1.6.0
 * @since 1.0.0
 */
public class GrenadeWeapon extends Weapon {
    public GrenadeWeapon(@NotNull Identifier id, @NotNull Item item, @NotNull Settings settings) {
        super(id, item, settings);
    }

    @Override
    public @NotNull ActionResult onPrimary(@NotNull GameSpace world, @NotNull ServerPlayerEntity player, @NotNull Hand hand) {
        var grenade = new GrenadeEntity(world.getWorld(), player, 40);
        grenade.setProperties(player, player.pitch, player.yaw, 0.f, 1.5f, 1.f);
        grenade.rollCritical();
        world.getWorld().spawnEntity(grenade);

        return super.onPrimary(world, player, hand);
    }
}
