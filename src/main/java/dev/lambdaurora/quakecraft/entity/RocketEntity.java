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

package dev.lambdaurora.quakecraft.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Represents a rocket entity.
 *
 * @author LambdAurora
 * @version 1.7.0
 * @since 1.3.0
 */
public class RocketEntity extends LargeFireball implements CritableEntity {
	private boolean critical = false;

	public RocketEntity(Level world, LivingEntity owner, double velocityX, double velocityY, double velocityZ) {
		super(world, owner, new Vec3(velocityX, velocityY, velocityZ), 1);
	}

	public void detonate(ServerLevel world) {
		this.kill(world);
		world.explode(this, this.getX(), this.getEyeY(), this.getZ(), critical ? 2.75f : 1.75f,
				Level.ExplosionInteraction.NONE);
	}

	@Override
	public void tick() {
		super.tick();

		if (this.isCritical()) {
			CritableEntity.spawnCritParticles(this.level(), this.getX(), this.getY(), this.getZ(), this.getDeltaMovement());
		}
	}

	@Override
	protected boolean shouldBurn() {
		return false;
	}

	@Override
	protected float getInertia() {
		return 1.f;
	}

	@Override
	protected void onHit(HitResult hitResult) {
		if (hitResult.getType() == HitResult.Type.ENTITY) {
			if (((EntityHitResult) hitResult).getEntity() instanceof RocketEntity) {
				((EntityHitResult) hitResult).getEntity().kill((ServerLevel) this.level());
				this.detonate((ServerLevel) this.level());
				return;
			}

			this.onHitEntity((EntityHitResult) hitResult);
		}

		this.detonate((ServerLevel) this.level());
	}

	@Override
	protected void onHitEntity(EntityHitResult entityHitResult) {
		super.onHitEntity(entityHitResult);
	}

	@Override
	public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {;
		if (source.is(DamageTypeTags.IS_EXPLOSION))
			return false;
		this.detonate(world);
		return true;
	}

	@Override
	public boolean isCritical() {
		return this.critical;
	}

	@Override
	public void setCritical(boolean critical) {
		this.critical = critical;
	}

	@Override
	public void rollCritical() {
		this.setCritical(this.random.nextInt(4) == 0);
	}
}
