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

import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.EulerAngle;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Represents a grenade entity.
 *
 * @author LambdAurora
 * @version 1.7.0
 * @since 1.0.0
 */
public class GrenadeEntity extends ArmorStandEntity implements CritableEntity {
    private final int lifetime;
    private UUID ownerUuid;
    private int ownerEntityId;
    private boolean leftOwner = false;
    private int life = 0;
    private boolean critical = false;

    public GrenadeEntity(@NotNull World world, @NotNull LivingEntity owner, int lifetime) {
        super(world, owner.getX(), owner.getEyeY() - 0.10000000149011612D, owner.getZ());
        this.setOwner(owner);
        this.lifetime = lifetime;
        this.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.MAGMA_BLOCK));
        this.setSmall(true);
        this.setHideBasePlate(true);
        this.setHeadRotation(new EulerAngle(180, this.getHeadYaw(), 0));
        this.setInvisible(true);
    }

    public void setOwner(@Nullable Entity entity) {
        if (entity != null) {
            this.ownerUuid = entity.getUuid();
            this.ownerEntityId = entity.getId();
        }
    }

    public @Nullable Entity getOwner() {
        if (this.ownerUuid != null && this.world instanceof ServerWorld) {
            return ((ServerWorld) this.world).getEntity(this.ownerUuid);
        } else {
            return this.ownerEntityId != 0 ? this.world.getEntityById(this.ownerEntityId) : null;
        }
    }

    public void setProperties(Entity user, float pitch, float yaw, float roll, float modifierZ, float modifierXYZ) {
        float f = -MathHelper.sin(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F);
        float g = -MathHelper.sin((pitch + roll) * 0.017453292F);
        float h = MathHelper.cos(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F);
        this.setVelocity(f, g, h, modifierZ, modifierXYZ);
        var vec3d = user.getVelocity();
        this.setVelocity(this.getVelocity().add(vec3d.x, user.isOnGround() ? 0.0D : vec3d.y, vec3d.z));

        this.rollCritical();
    }

    public void setVelocity(double x, double y, double z, float speed, float divergence) {
        var vec3d = (new Vec3d(x, y, z)).normalize()
                .add(this.random.nextGaussian() * 0.007499999832361937D * (double) divergence,
                        this.random.nextGaussian() * 0.007499999832361937D * (double) divergence,
                        this.random.nextGaussian() * 0.007499999832361937D * (double) divergence)
                .multiply(speed);
        this.setVelocity(vec3d);
        float f = (float) Math.sqrt(this.squaredDistanceTo(vec3d));
        this.setYaw((float) (MathHelper.atan2(vec3d.x, vec3d.z) * 57.2957763671875D));
        this.setPitch((float) (MathHelper.atan2(vec3d.y, f) * 57.2957763671875D));
        this.prevYaw = this.getYaw();
        this.prevPitch = this.getPitch();
    }

    public void detonate() {
        this.kill();
        this.getEntityWorld().createExplosion(this, this.getX(), this.getEyeY(), this.getZ(), critical ? 2.5f : 1.5f,
                Explosion.DestructionType.NONE);
    }

    @Override
    public void tick() {
        if (!this.hasNoGravity()) {
            this.setVelocity(this.getVelocity().add(0.0D, -0.04D, 0.0D));
        }

        this.move(MovementType.SELF, this.getVelocity());
        this.setVelocity(this.getVelocity().multiply(0.98D));
        if (this.onGround) {
            this.setVelocity(this.getVelocity().multiply(0.7D, -0.5D, 0.7D));
        }

        this.life++;
        if (this.life >= this.lifetime) {
            this.detonate();
            return;
        } else {
            this.updateWaterState();
            if (this.world.isClient) {
                this.world.addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5D, this.getZ(), 0.0D, 0.0D, 0.0D);
            }
        }

        if (this.isCritical()) {
            CritableEntity.spawnCritParticles(this.world, this.getX(), this.getY(), this.getZ(), this.getVelocity());
        }

        if (!this.leftOwner) {
            this.leftOwner = this.checkOwnerLeft();
        }

        var hitResult = ProjectileUtil.getEntityCollision(this.getEntityWorld(), this, this.getPos(), this.getPos().add(this.getVelocity()),
                this.getBoundingBox().stretch(this.getVelocity()).expand(1.0D), entity -> {
                    if (!entity.isSpectator() && entity.isAlive() && entity.collides()) {
                        Entity entity2 = this.getOwner();
                        return entity2 == null || this.leftOwner || !entity2.isConnectedThroughVehicle(entity);
                    } else {
                        return false;
                    }
                });
        if (hitResult != null) {
            this.onEntityHit(hitResult);
        }
    }

    private boolean checkOwnerLeft() {
        var owner = this.getOwner();
        if (owner != null) {
            for (var other : this.world.getOtherEntities(this, this.getBoundingBox().stretch(this.getVelocity()).expand(1.0D),
                    other -> !other.isSpectator() && other.collides())) {
                if (other.getRootVehicle() == owner.getRootVehicle()) {
                    return false;
                }
            }
        }

        return true;
    }

    protected void onEntityHit(@NotNull EntityHitResult hitResult) {
        this.detonate();
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
}
