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

package dev.lambdaurora.quakecraft;

import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * Represents constants used in Quakecraft.
 *
 * @author LambdAurora
 * @version 1.1.0
 * @since 1.0.0
 */
public class QuakecraftConstants {
	public static final int RESPAWN_SECONDS = 5;
	public static final int RESPAWN_TICKS = RESPAWN_SECONDS * 20;

	/**
	 * Represents the dash velocity.
	 */
	public static final double DASH_VELOCITY = 1.2;

	public static final Predicate<Entity> PLAYER_PREDICATE = entity -> entity instanceof ServerPlayerEntity
			&& ((ServerPlayerEntity) entity).interactionManager.getGameMode() != GameMode.SPECTATOR;

	public static final EntityAttributeModifier PLAYER_MOVEMENT_SPEED_MODIFIER = new EntityAttributeModifier(UUID.fromString("91AEAA56-376B-4498-935B-2F7F68070635"),
			"quakecraft.movement.speed",
			2 * 0.20000000298023224D,
			EntityAttributeModifier.Operation.MULTIPLY_TOTAL);
}
