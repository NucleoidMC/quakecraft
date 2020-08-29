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

package me.lambdaurora.quakecraft.util;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.game.GameWorld;

import java.util.Optional;
import java.util.function.Predicate;

public final class RayUtils
{
    private RayUtils()
    {
        throw new UnsupportedOperationException("RayUtils only contains static definitions.");
    }

    /**
     * Thanks FarmyFeud (https://github.com/NucleoidMC/farmy-feud/blob/1.16.2/src/main/java/xyz/nucleoid/farmyfeud/game/active/EntityRayTrace.java)
     */
    public static @Nullable EntityHitResult raycastEntity(@NotNull Entity sourceEntity, double range, double margin, Predicate<Entity> predicate)
    {
        World world = sourceEntity.getEntityWorld();

        Vec3d origin = sourceEntity.getCameraPosVec(1.0F);
        Vec3d delta = sourceEntity.getRotationVec(1.0F).multiply(range);

        Vec3d target = origin.add(delta);

        double testMargin = Math.max(1.0, margin);

        Box testBox = sourceEntity.getBoundingBox()
                .stretch(delta)
                .expand(testMargin, testMargin, testMargin);

        double minDistance2 = range * range;
        Entity hitEntity = null;
        Vec3d hitPoint = null;

        for (Entity entity : world.getOtherEntities(sourceEntity, testBox, predicate)) {
            Box targetBox = entity.getBoundingBox().expand(Math.max(entity.getTargetingMargin(), margin));

            Optional<Vec3d> traceResult = targetBox.raycast(origin, target);
            if (targetBox.contains(origin)) {
                return new EntityHitResult(entity, traceResult.orElse(origin));
            }

            if (traceResult.isPresent()) {
                Vec3d tracePoint = traceResult.get();
                double distance2 = origin.squaredDistanceTo(tracePoint);

                if (distance2 < minDistance2) {
                    hitEntity = entity;
                    hitPoint = tracePoint;
                    minDistance2 = distance2;
                }
            }
        }

        if (hitEntity == null) {
            return null;
        }

        BlockHitResult blockHitResult = world.raycast(new RaycastContext(origin, target, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, sourceEntity));
        if (blockHitResult.getType() != HitResult.Type.MISS) {
            return null;
        }

        return new EntityHitResult(hitEntity, hitPoint);
    }

    public static void drawRay(@NotNull GameWorld world, @NotNull Entity source, double range)
    {
        Vec3d origin = source.getCameraPosVec(1.f).subtract(0, 0.5, 0);
        Vec3d delta = source.getRotationVec(1.f).multiply(range);

        Vec3d target = origin.add(delta);

        BlockHitResult blockHitResult = world.getWorld().raycast(new RaycastContext(origin, target, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, source));
        if (blockHitResult.getType() != HitResult.Type.MISS) {
            target = blockHitResult.getPos();
        }

        drawRay(world, origin, target);
    }

    public static void drawRay(@NotNull GameWorld world, @NotNull Entity source, @NotNull Entity target)
    {
        Vec3d origin = source.getCameraPosVec(1.f).subtract(0, 0.5, 0);

        Vec3d end = target.getCameraPosVec(1.f).subtract(0, 0.5, 0);

        BlockHitResult blockHitResult = world.getWorld().raycast(new RaycastContext(origin, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, source));
        if (blockHitResult.getType() != HitResult.Type.MISS) {
            end = blockHitResult.getPos();
        }

        drawRay(world, origin, end);
    }

    public static void drawRay(@NotNull GameWorld world, @NotNull Vec3d origin, @NotNull Vec3d target)
    {
        Vec3d delta = target.subtract(origin);
        double length = delta.length();
        double stepX = delta.x / length;
        double stepY = delta.y / length;
        double stepZ = delta.z / length;

        for (double d = 0.0; d <= length; d += 0.5) {
            double x = origin.x + stepX * d;
            double y = origin.y + stepY * d;
            double z = origin.z + stepZ * d;

            ParticleS2CPacket packet = new ParticleS2CPacket(new DustParticleEffect(1.f, 0.647f, 0.f, .5f), false, x, y, z,
                    0.f, 0.f, 0.f, 0.5f, 3);
            world.getPlayerSet().sendPacket(packet);
        }
    }
}
