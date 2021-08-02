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

package dev.lambdaurora.quakecraft.util;

import net.minecraft.entity.Entity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Represents a ray utilities class.
 *
 * @author LambdAurora, Gegy
 * @version 1.7.0
 * @since 1.0.0
 */
public final class RayUtils {
    private RayUtils() {
        throw new UnsupportedOperationException("RayUtils only contains static definitions.");
    }

    /**
     * Casts a ray through entities and stops if the range is hit or a block is hit.
     *
     * @param source the source entity
     * @param range the maximum range
     * @param margin the margin of entity detection
     * @param predicate the predicate to determine if the entity should be hit or not
     * @param consumer the consumer of hit entities
     * @return the absolute distance between the source and the most far hit. The sign bit is used as a boolean to represent a success or not
     */
    public static double raycastEntities(Entity source, double range, double margin, Predicate<Entity> predicate, Consumer<Entity> consumer) {
        World world = source.getEntityWorld();

        final Vec3d origin = source.getCameraPosVec(1.0F);
        final Vec3d delta = source.getRotationVec(1.0F).multiply(range);

        final Vec3d target = origin.add(delta);

        final double testMargin = Math.max(1.0, margin);
        final Box testBox = source.getBoundingBox()
                .stretch(delta)
                .expand(testMargin, testMargin, testMargin);

        BlockHitResult blockHitResult = null;
        double blockDistance = -1.0;
        double distance = -1.0;

        boolean success = false;

        for (Entity entity : world.getOtherEntities(source, testBox, predicate)) {
            Box targetBox = entity.getBoundingBox().expand(Math.max(entity.getTargetingMargin(), margin));
            double entityDistance = source.squaredDistanceTo(entity);

            if (targetBox.contains(origin) || targetBox.raycast(origin, target).isPresent()) {
                if (blockHitResult == null) {
                    ((RayAccessor) source).quakecraft$setRaycasting(true);
                    blockHitResult = world.raycast(new RaycastContext(origin, target, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, source));
                    ((RayAccessor) source).quakecraft$setRaycasting(false);
                }

                if (blockHitResult.getType() != HitResult.Type.MISS) {
                    if (blockDistance < 0.0) {
                        blockDistance = source.squaredDistanceTo(blockHitResult.getPos());
                    }

                    if (entityDistance > blockDistance)
                        continue;
                }

                success = true;
                consumer.accept(entity);

                if (entityDistance > distance)
                    distance = entityDistance;
            }
        }

        if (distance < 0.0) distance = range;
        else distance = Math.sqrt(distance);

        return success ? -distance : distance;
    }

    /**
     * Thanks FarmyFeud (https://github.com/NucleoidMC/farmy-feud/blob/1.16.2/src/main/java/xyz/nucleoid/farmyfeud/game/active/EntityRayTrace.java)
     */
    public static @Nullable EntityHitResult raycastEntity(Entity source, double range, double margin, Predicate<Entity> predicate) {
        World world = source.getEntityWorld();

        Vec3d origin = source.getCameraPosVec(1.0F);
        Vec3d delta = source.getRotationVec(1.0F).multiply(range);

        Vec3d target = origin.add(delta);

        double testMargin = Math.max(1.0, margin);

        Box testBox = source.getBoundingBox()
                .stretch(delta)
                .expand(testMargin, testMargin, testMargin);

        double minDistance2 = range * range;
        Entity hitEntity = null;
        Vec3d hitPoint = null;

        for (Entity entity : world.getOtherEntities(source, testBox, predicate)) {
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

        ((RayAccessor) source).quakecraft$setRaycasting(true);
        BlockHitResult blockHitResult = world.raycast(new RaycastContext(origin, target, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, source));
        ((RayAccessor) source).quakecraft$setRaycasting(false);
        if (blockHitResult.getType() != HitResult.Type.MISS) {
            return null;
        }

        return new EntityHitResult(hitEntity, hitPoint);
    }

    public static void drawRay(ServerWorld world, Entity source, double range) {
        Vec3d origin = source.getCameraPosVec(1.f).subtract(0, 0.5, 0);
        Vec3d delta = source.getRotationVec(1.f).multiply(range);

        Vec3d target = origin.add(delta);

        ((RayAccessor) source).quakecraft$setRaycasting(true);
        BlockHitResult blockHitResult = world.raycast(new RaycastContext(origin, target, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, source));
        ((RayAccessor) source).quakecraft$setRaycasting(false);
        if (blockHitResult.getType() != HitResult.Type.MISS) {
            target = blockHitResult.getPos();
        }

        drawRay(world, origin, target);
    }

    public static void drawRay(ServerWorld world, Entity source, Entity target) {
        Vec3d origin = source.getCameraPosVec(1.f).subtract(0, 0.5, 0);

        Vec3d end = target.getCameraPosVec(1.f).subtract(0, 0.5, 0);

        ((RayAccessor) source).quakecraft$setRaycasting(true);
        BlockHitResult blockHitResult = world.raycast(new RaycastContext(origin, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, source));
        ((RayAccessor) source).quakecraft$setRaycasting(false);
        if (blockHitResult.getType() != HitResult.Type.MISS) {
            end = blockHitResult.getPos();
        }

        drawRay(world, origin, end);
    }

    public static void drawRay(ServerWorld world, Vec3d origin, Vec3d target) {
        Vec3d delta = target.subtract(origin);
        double length = delta.length();
        double stepX = delta.x / length;
        double stepY = delta.y / length;
        double stepZ = delta.z / length;

        for (double d = 0.0; d <= length; d += 0.5) {
            double x = origin.x + stepX * d;
            double y = origin.y + stepY * d;
            double z = origin.z + stepZ * d;

            world.spawnParticles(new DustParticleEffect(new Vec3f(1.f, 0.647f, 0.f), .75f),
                    x, y, z, 3, 0.f, 0.f, 0.f, 1.f);
        }
    }
}
