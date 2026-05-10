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

import dev.lambdaurora.quakecraft.entity.RocketEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Represents a rocket launcher.
 *
 * @author LambdAurora
 * @version 1.7.0
 * @since 1.3.0
 */
public class RocketLauncherWeapon extends Weapon {
	public RocketLauncherWeapon(Identifier id, Item item, Settings settings) {
		super(id, item, settings);
	}

	@Override
	public InteractionResult onPrimary(ServerLevel world, ServerPlayer player, InteractionHand hand) {
		var rocket = new RocketEntity(world, player, 0, 0, 0);

		var origin = player.getEyePosition(1.0F);
		var delta = player.getViewVector(1.0F).scale(0.25);

		var target = origin.add(delta);
		rocket.setPosRaw(target.x(), target.y(), target.z());

		rocket.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.f, 1.5f, 1.f);
		rocket.setDeltaMovement(rocket.getDeltaMovement().scale(0.75));
		rocket.setItem(new ItemStack(Items.FIRE_CHARGE));
		rocket.rollCritical();
		world.addFreshEntity(rocket);

		return super.onPrimary(world, player, hand);
	}
}
