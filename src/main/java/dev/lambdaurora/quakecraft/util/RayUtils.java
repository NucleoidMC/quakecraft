/*
 * Copyright (c) 2022 LambdAurora <email@lambdaurora.dev>
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

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

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
		Level world = source.level();

		final Vec3 origin = source.getEyePosition(1.0F);
		final Vec3 delta = source.getViewVector(1.0F).scale(range);

		final Vec3 target = origin.add(delta);

		final double testMargin = Math.max(1.0, margin);
		final AABB testBox = source.getBoundingBox()
				.expandTowards(delta)
				.inflate(testMargin, testMargin, testMargin);

		BlockHitResult blockHitResult = null;
		double blockDistance = -1.0;
		double distance = -1.0;

		boolean success = false;

		for (Entity entity : world.getEntities(source, testBox, predicate)) {
			AABB targetBox = entity.getBoundingBox().inflate(Math.max(entity.getPickRadius(), margin));
			double entityDistance = source.distanceToSqr(entity);

			if (targetBox.contains(origin) || targetBox.clip(origin, target).isPresent()) {
				if (blockHitResult == null) {
					((RayAccessor) source).quakecraft$setRaycasting(true);
					blockHitResult = world.clip(new ClipContext(origin, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source));
					((RayAccessor) source).quakecraft$setRaycasting(false);
				}

				if (blockHitResult.getType() != HitResult.Type.MISS) {
					if (blockDistance < 0.0) {
						blockDistance = source.distanceToSqr(blockHitResult.getLocation());
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
		Level world = source.level();

		Vec3 origin = source.getEyePosition(1.0F);
		Vec3 delta = source.getViewVector(1.0F).scale(range);

		Vec3 target = origin.add(delta);

		double testMargin = Math.max(1.0, margin);

		AABB testBox = source.getBoundingBox()
				.expandTowards(delta)
				.inflate(testMargin, testMargin, testMargin);

		double minDistance2 = range * range;
		Entity hitEntity = null;
		Vec3 hitPoint = null;

		for (Entity entity : world.getEntities(source, testBox, predicate)) {
			AABB targetBox = entity.getBoundingBox().inflate(Math.max(entity.getPickRadius(), margin));

			Optional<Vec3> traceResult = targetBox.clip(origin, target);
			if (targetBox.contains(origin)) {
				return new EntityHitResult(entity, traceResult.orElse(origin));
			}

			if (traceResult.isPresent()) {
				Vec3 tracePoint = traceResult.get();
				double distance2 = origin.distanceToSqr(tracePoint);

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
		BlockHitResult blockHitResult = world.clip(new ClipContext(origin, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source));
		((RayAccessor) source).quakecraft$setRaycasting(false);
		if (blockHitResult.getType() != HitResult.Type.MISS) {
			return null;
		}

		return new EntityHitResult(hitEntity, hitPoint);
	}

	public static void drawRay(ServerLevel world, Entity source, double range) {
		Vec3 origin = source.getEyePosition(1.f).subtract(0, 0.5, 0);
		Vec3 delta = source.getViewVector(1.f).scale(range);

		Vec3 target = origin.add(delta);

		((RayAccessor) source).quakecraft$setRaycasting(true);
		BlockHitResult blockHitResult = world.clip(new ClipContext(origin, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source));
		((RayAccessor) source).quakecraft$setRaycasting(false);
		if (blockHitResult.getType() != HitResult.Type.MISS) {
			target = blockHitResult.getLocation();
		}

		drawRay(world, origin, target);
	}

	public static void drawRay(ServerLevel world, Entity source, Entity target) {
		Vec3 origin = source.getEyePosition(1.f).subtract(0, 0.5, 0);

		Vec3 end = target.getEyePosition(1.f).subtract(0, 0.5, 0);

		((RayAccessor) source).quakecraft$setRaycasting(true);
		BlockHitResult blockHitResult = world.clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source));
		((RayAccessor) source).quakecraft$setRaycasting(false);
		if (blockHitResult.getType() != HitResult.Type.MISS) {
			end = blockHitResult.getLocation();
		}

		drawRay(world, origin, end);
	}

	public static void drawRay(ServerLevel world, Vec3 origin, Vec3 target) {
		Vec3 delta = target.subtract(origin);
		double length = delta.length();
		double stepX = delta.x / length;
		double stepY = delta.y / length;
		double stepZ = delta.z / length;

		for (double d = 0.0; d <= length; d += 0.5) {
			double x = origin.x + stepX * d;
			double y = origin.y + stepY * d;
			double z = origin.z + stepZ * d;

			world.sendParticles(new DustParticleOptions(ARGB.colorFromFloat (0, 1.f, 0.647f, 0.f), .75f),
					x, y, z, 3, 0.f, 0.f, 0.f, 1.f);
		}
	}
}
