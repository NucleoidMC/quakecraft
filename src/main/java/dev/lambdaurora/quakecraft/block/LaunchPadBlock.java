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

package dev.lambdaurora.quakecraft.block;

import dev.lambdaurora.quakecraft.Quakecraft;
import dev.lambdaurora.quakecraft.QuakecraftRegistry;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;

/**
 * Represents a launch pad block.
 *
 * @author LambdAurora
 * @version 1.7.0
 * @since 1.6.1
 */
public class LaunchPadBlock extends Block implements PolymerBlock {
	public static final int POWER_MIN = 1;
	public static final int POWER_MAX = 8;
	public static final IntegerProperty POWER = IntegerProperty.create("power", POWER_MIN, POWER_MAX);
	protected static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 5.0, 16.0);
	private final Block proxy;

	public LaunchPadBlock(BlockBehaviour.Properties settings, Block proxy) {
		super(settings.noCollision().noLootTable());
		this.registerDefaultState(this.stateDefinition.any()
				.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
				.setValue(POWER, 3));
		this.proxy = proxy;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(BlockStateProperties.HORIZONTAL_FACING)
				.add(POWER);
	}

	@Override
	protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity, InsideBlockEffectApplier handler, boolean arg) {
		if (world.isClientSide())
			return;
		var direction = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
		int angle = switch (direction) {
			case EAST -> 270;
			case WEST -> 90;
			case NORTH -> 180;
			default -> 0;
		};
		var vector = getVector(angle, entity.getViewXRot(1.f), entity.getViewYRot(1.f), state.getValue(POWER));
		entity.setDeltaMovement(vector.x(), vector.y(), vector.z());
		if (entity instanceof ServerPlayer) {
			((ServerPlayer) entity).connection.send(new ClientboundSetEntityMotionPacket(entity));
		}
	}

	protected final Vec3 getVector(float angle, float pitch, float yaw, int power) {
		final int maxAngleOffset = 25;
		if (yaw < 0)
			yaw += 360.f;
		if (yaw > (angle + 90) || yaw < (angle - 90))
			yaw = angle;
		else
			yaw = Mth.clamp(yaw % 360.f, angle - maxAngleOffset, angle + maxAngleOffset);
		float f = pitch * 0.017453292F;
		float g = -yaw * 0.017453292F;
		float h = Mth.cos(g);
		float i = Mth.sin(g);
		float j = Mth.cos(f);

		float coefficient = power * 0.25f;
		if (power > 4)
			coefficient = power;

		return new Vec3(i * j * (1 + coefficient), Mth.clamp(coefficient, 0.75, 1), h * j * (1 + coefficient));
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
		return this.proxy.defaultBlockState();
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.INVISIBLE;
	}

	public static BlockState fromNbt(CompoundTag data) {
		Block block = QuakecraftRegistry.STONE_LAUNCHPAD_BLOCK;
		if (data.contains("type")) {
			block = BuiltInRegistries.BLOCK.getOptional(Quakecraft.id(data.getStringOr("type", "") + "_launchpad")).orElse(QuakecraftRegistry.STONE_LAUNCHPAD_BLOCK);
		}
		var state = block.defaultBlockState();
		Direction direction = Quakecraft.getDirectionByName(data.getStringOr("direction", ""));
		if (direction.getAxis().isVertical())
			return state;
		state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, direction);
		if (data.contains("power")) {
			state = state.setValue(POWER, Mth.clamp(data.getIntOr("power", 0), POWER_MIN, POWER_MAX));
		}
		return state;
	}
}
