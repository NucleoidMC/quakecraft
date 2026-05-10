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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import net.minecraft.core.Rotations;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Represents a grenade entity.
 *
 * @author LambdAurora
 * @version 1.7.0
 * @since 1.0.0
 */
public class GrenadeEntity extends ArmorStand implements CritableEntity {
	private final int lifetime;
	private UUID ownerUuid;
	private int ownerEntityId;
	private boolean leftOwner = false;
	private int life = 0;
	private boolean critical = false;
	private float prevYaw;
	private float prevPitch;

	public GrenadeEntity(@NotNull Level world, @NotNull LivingEntity owner, int lifetime) {
		super(world, owner.getX(), owner.getEyeY() - 0.10000000149011612D, owner.getZ());
		this.setOwner(owner);
		this.lifetime = lifetime;
		this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.MAGMA_BLOCK));
		this.setSmall(true);
		this.setNoBasePlate(true);
		this.setHeadPose(new Rotations(180, this.getYHeadRot(), 0));
		this.setInvisible(true);
	}

	public void setOwner(@Nullable Entity entity) {
		if (entity != null) {
			this.ownerUuid = entity.getUUID();
			this.ownerEntityId = entity.getId();
		}
	}

	public @Nullable Entity getOwner() {
		if (this.ownerUuid != null && this.level() instanceof ServerLevel) {
			return ((ServerLevel) this.level()).getEntity(this.ownerUuid);
		} else {
			return this.ownerEntityId != 0 ? this.level().getEntity(this.ownerEntityId) : null;
		}
	}

	public void setProperties(Entity user, float pitch, float yaw, float roll, float modifierZ, float modifierXYZ) {
		float f = -Mth.sin(yaw * 0.017453292F) * Mth.cos(pitch * 0.017453292F);
		float g = -Mth.sin((pitch + roll) * 0.017453292F);
		float h = Mth.cos(yaw * 0.017453292F) * Mth.cos(pitch * 0.017453292F);
		this.setVelocity(f, g, h, modifierZ, modifierXYZ);
		var vec3d = user.getDeltaMovement();
		this.setDeltaMovement(this.getDeltaMovement().add(vec3d.x, user.onGround() ? 0.0D : vec3d.y, vec3d.z));

		this.rollCritical();
	}

	public void setVelocity(double x, double y, double z, float speed, float divergence) {
		var vec3d = (new Vec3(x, y, z)).normalize()
				.add(this.random.nextGaussian() * 0.007499999832361937D * (double) divergence,
						this.random.nextGaussian() * 0.007499999832361937D * (double) divergence,
						this.random.nextGaussian() * 0.007499999832361937D * (double) divergence)
				.scale(speed);
		this.setDeltaMovement(vec3d);
		float f = (float) Math.sqrt(this.distanceToSqr(vec3d));
		this.setYRot((float) (Mth.atan2(vec3d.x, vec3d.z) * 57.2957763671875D));
		this.setXRot((float) (Mth.atan2(vec3d.y, f) * 57.2957763671875D));
		this.prevYaw = this.getYRot();
		this.prevPitch = this.getXRot();
	}

	public void detonate(ServerLevel world) {
		this.kill(world);
		world.explode(this, this.getX(), this.getEyeY(), this.getZ(), critical ? 2.5f : 1.5f,
				Level.ExplosionInteraction.NONE);
	}

	@Override
	public void tick() {
		if (!this.isNoGravity()) {
			this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.04D, 0.0D));
		}

		this.move(MoverType.SELF, this.getDeltaMovement());
		this.setDeltaMovement(this.getDeltaMovement().scale(0.98D));
		if (this.onGround()) {
			this.setDeltaMovement(this.getDeltaMovement().multiply(0.7D, -0.5D, 0.7D));
		}

		this.life++;
		if (this.life >= this.lifetime) {
			this.detonate((ServerLevel) this.level());
			return;
		} else {
			this.updateFluidInteraction();
		}

		if (this.isCritical()) {
			CritableEntity.spawnCritParticles(this.level(), this.getX(), this.getY(), this.getZ(), this.getDeltaMovement());
		}

		if (!this.leftOwner) {
			this.leftOwner = this.checkOwnerLeft();
		}

		var hitResult = ProjectileUtil.getEntityHitResult(this.level(), this, this.position(), this.position().add(this.getDeltaMovement()),
				this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0D), entity -> {
					if (!entity.isSpectator() && entity.isAlive() && entity.isPickable()) {
						Entity entity2 = this.getOwner();
						return entity2 == null || this.leftOwner || !entity2.isPassengerOfSameVehicle(entity);
					} else {
						return false;
					}
				}, ProjectileUtil.computeMargin(this));
		if (hitResult != null) {
			this.onEntityHit(hitResult);
		}
	}

	private boolean checkOwnerLeft() {
		var owner = this.getOwner();
		if (owner != null) {
			for (var other : this.level().getEntities(this, this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0D),
					other -> !other.isSpectator() && other.isPickable())) {
				if (other.getRootVehicle() == owner.getRootVehicle()) {
					return false;
				}
			}
		}

		return true;
	}

	protected void onEntityHit(@NotNull EntityHitResult hitResult) {
		this.detonate((ServerLevel) this.level());
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
		this.setCritical(this.random.nextInt(6) == 0);
	}

	@Override
	public Vec3 trackingPosition() {
		return super.trackingPosition().subtract(0, 0.35, 0);
	}
}
