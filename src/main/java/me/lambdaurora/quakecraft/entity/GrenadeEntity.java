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

package me.lambdaurora.quakecraft.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a grenade entity.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class GrenadeEntity extends SnowballEntity
{
    public GrenadeEntity(@NotNull World world, @NotNull LivingEntity owner)
    {
        super(world, owner);
    }

    public void detonate()
    {
        this.getEntityWorld().createExplosion(this, this.getX(), this.getY(), this.getZ(), 1.25f, Explosion.DestructionType.NONE);
    }

    @Override
    protected void onEntityHit(@NotNull EntityHitResult hitResult)
    {
        this.detonate();
        super.onEntityHit(hitResult);
    }

    @Override
    protected void onCollision(@NotNull HitResult hitResult)
    {
        this.detonate();
        super.onCollision(hitResult);
    }
}
