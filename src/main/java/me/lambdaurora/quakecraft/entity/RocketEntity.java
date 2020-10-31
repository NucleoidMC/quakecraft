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

package me.lambdaurora.quakecraft.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

/**
 * Represents a rocket entity.
 *
 * @author LambdAurora
 * @version 1.4.9
 * @since 1.3.0
 */
public class RocketEntity extends FireballEntity implements CritableEntity
{
    private boolean critical = false;

    public RocketEntity(World world, LivingEntity owner, double velocityX, double velocityY, double velocityZ)
    {
        super(world, owner, velocityX, velocityY, velocityZ);
    }

    public void detonate()
    {
        this.remove();
        this.getEntityWorld().createExplosion(this, this.getX(), this.getEyeY(), this.getZ(), critical ? 2.75f : 1.75f,
                Explosion.DestructionType.NONE);
    }

    @Override
    public void tick()
    {
        super.tick();

        if (this.isCritical()) {
            CritableEntity.spawnCritParticles(this.world, this.getX(), this.getY(), this.getZ(), this.getVelocity());
        }
    }

    @Override
    protected boolean isBurning()
    {
        return false;
    }

    @Override
    protected float getDrag()
    {
        return 1.f;
    }

    @Override
    protected void onCollision(HitResult hitResult)
    {
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            if (((EntityHitResult) hitResult).getEntity() instanceof RocketEntity) {
                ((EntityHitResult) hitResult).getEntity().remove();
                this.detonate();
                return;
            }

            this.onEntityHit((EntityHitResult) hitResult);
        }

        this.detonate();
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult)
    {
        super.onEntityHit(entityHitResult);
    }

    @Override
    public boolean damage(DamageSource source, float amount)
    {
        if (this.isInvulnerableTo(source))
            return false;
        if (source.isExplosive() && source.getSource() instanceof ServerPlayerEntity && source.getSource() == this.getOwner())
            return false;
        this.detonate();
        return true;
    }

    @Override
    public boolean isCritical()
    {
        return this.critical;
    }

    @Override
    public void setCritical(boolean critical)
    {
        this.critical = critical;
    }

    @Override
    public void rollCritical()
    {
        this.setCritical(this.random.nextInt(4) == 0);
    }
}
