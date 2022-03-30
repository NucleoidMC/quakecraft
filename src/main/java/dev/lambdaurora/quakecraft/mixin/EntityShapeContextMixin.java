/*
 * Copyright (c) 2020-2022 LambdAurora <email@lambdaurora.dev>
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

package dev.lambdaurora.quakecraft.mixin;

import dev.lambdaurora.quakecraft.util.UsefulEntityShapeContext;
import net.minecraft.block.EntityShapeContext;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityShapeContext.class)
public class EntityShapeContextMixin implements UsefulEntityShapeContext {
	@Unique
	private Entity quakecraft$entity;

	@Inject(method = "<init>(Lnet/minecraft/entity/Entity;)V", at = @At("RETURN"))
	private void onInit(Entity entity, CallbackInfo ci) {
		this.quakecraft$entity = entity;
	}

	@Override
	public @Nullable Entity quakecraft$getEntity() {
		return this.quakecraft$entity;
	}
}
