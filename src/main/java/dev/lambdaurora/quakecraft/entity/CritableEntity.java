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

package dev.lambdaurora.quakecraft.entity;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
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

    static void spawnCritParticles(@NotNull World world, double x, double y, double z, Vec3d velocity) {
        for (int i = 0; i < 4; i++) {
            if (world.isClient()) {
                world.addParticle(ParticleTypes.CRIT,
                        x + velocity.getX() * i / 4.0D,
                        y + velocity.getY() * i / 4.0D,
                        z + velocity.getZ() * i / 4.0D,
                        -velocity.getX(), -velocity.getY() + 0.2D, -velocity.getZ());
            } else {
                ((ServerWorld) world).spawnParticles(ParticleTypes.CRIT,
                        x + velocity.getX() * i / 4.0,
                        y + velocity.getY() * i / 4.0,
                        z + velocity.getZ() * i / 4.0,
                        1,
                        -velocity.getX(), -velocity.getY() + 0.2, -velocity.getZ(),
                        0.5);
            }
        }
    }
}
