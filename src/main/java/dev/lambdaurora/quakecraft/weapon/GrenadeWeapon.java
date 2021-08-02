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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

/**
 * Represents a grenade weapon.
 *
 * @author LambdAurora
 * @version 1.7.0
 * @since 1.0.0
 */
public class GrenadeWeapon extends Weapon {
    public GrenadeWeapon(Identifier id, Item item, Settings settings) {
        super(id, item, settings);
    }

    @Override
    public ActionResult onPrimary(ServerWorld world, ServerPlayerEntity player, Hand hand) {
        var grenade = new GrenadeEntity(world, player, 40);
        grenade.setProperties(player, player.getPitch(), player.getYaw(), 0.f, 1.5f, 1.f);
        grenade.rollCritical();
        world.spawnEntity(grenade);

        return super.onPrimary(world, player, hand);
    }
}
