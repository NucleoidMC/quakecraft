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

package dev.lambdaurora.quakecraft.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a projectile entity that can deal critical damage.
 *
 * @author LambdAurora
 * @version 1.7.0
 * @since 1.3.0
 */
public interface CritableEntity {
	boolean isCritical();

	void setCritical(boolean critical);

	void rollCritical();

	static void spawnCritParticles(@NotNull Level world, double x, double y, double z, Vec3 velocity) {
		for (int i = 0; i < 4; i++) {
			((ServerLevel) world).sendParticles(ParticleTypes.CRIT,
					x + velocity.x() * i / 4.0,
					y + velocity.y() * i / 4.0,
					z + velocity.z() * i / 4.0,
					1,
					-velocity.x(), -velocity.y() + 0.2, -velocity.z(),
					0.5);
		}
	}
}
