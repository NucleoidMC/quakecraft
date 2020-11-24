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

package me.lambdaurora.quakecraft.mixin;

import me.lambdaurora.quakecraft.Quakecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity
{
    @Shadow
    public abstract boolean hasStatusEffect(StatusEffect effect);

    @Shadow
    public abstract boolean isClimbing();

    public LivingEntityMixin(EntityType<?> type, World world)
    {
        super(type, world);
    }

    private final ThreadLocal<Vec3d> preTravelVelocity = new ThreadLocal<>();

    @Inject(
            method = "travel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void onPostMove(Vec3d movementInput, CallbackInfo ci)
    {
        this.preTravelVelocity.set(this.getVelocity());
    }

    @Inject(
            method = "travel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/LivingEntity;method_26317(DZLnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;",
                    shift = At.Shift.BEFORE
            )
    )
    private void onTravel(Vec3d movementInput, CallbackInfo ci)
    {
        if (this.isTouchingWater() && !this.hasStatusEffect(StatusEffects.DOLPHINS_GRACE)) {
            if (((Object) this) instanceof ServerPlayerEntity && Quakecraft.get().isPlayerActive((ServerPlayerEntity) (Object) this)) {
                var vec3d = this.preTravelVelocity.get();

                if (this.horizontalCollision && this.isClimbing()) {
                    vec3d = new Vec3d(vec3d.x, 0.2D, vec3d.z);
                }

                double j = 0.96;

                this.setVelocity(vec3d.multiply(j, 0.800000011920929D, j));
            }
        }
    }
}
