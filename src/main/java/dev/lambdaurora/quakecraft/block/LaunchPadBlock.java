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
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.registry.Registry;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import xyz.nucleoid.packettweaker.PacketContext;

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
	public static final IntProperty POWER = IntProperty.of("power", POWER_MIN, POWER_MAX);
	protected static final VoxelShape SHAPE = Block.createCuboidShape(0.0, 0.0, 0.0, 16.0, 5.0, 16.0);
	private final Block proxy;

	public LaunchPadBlock(AbstractBlock.Settings settings, Block proxy) {
		super(settings.noCollision().dropsNothing());
		this.setDefaultState(this.stateManager.getDefaultState()
				.with(Properties.HORIZONTAL_FACING, Direction.NORTH)
				.with(POWER, 3));
		this.proxy = proxy;
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		super.appendProperties(builder);
		builder.add(Properties.HORIZONTAL_FACING)
				.add(POWER);
	}

	@Override
	public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
		if (world.isClient())
			return;
		var direction = state.get(Properties.HORIZONTAL_FACING);
		int angle = switch (direction) {
			case EAST -> 270;
			case WEST -> 90;
			case NORTH -> 180;
			default -> 0;
		};
		var vector = getVector(angle, entity.getPitch(1.f), entity.getYaw(1.f), state.get(POWER));
		entity.setVelocity(vector.getX(), vector.getY(), vector.getZ());
		if (entity instanceof ServerPlayerEntity) {
			((ServerPlayerEntity) entity).networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(entity));
		}
	}

	protected final Vec3d getVector(float angle, float pitch, float yaw, int power) {
		final int maxAngleOffset = 25;
		if (yaw < 0)
			yaw += 360.f;
		if (yaw > (angle + 90) || yaw < (angle - 90))
			yaw = angle;
		else
			yaw = MathHelper.clamp(yaw % 360.f, angle - maxAngleOffset, angle + maxAngleOffset);
		float f = pitch * 0.017453292F;
		float g = -yaw * 0.017453292F;
		float h = MathHelper.cos(g);
		float i = MathHelper.sin(g);
		float j = MathHelper.cos(f);

		float coefficient = power * 0.25f;
		if (power > 4)
			coefficient = power;

		return new Vec3d(i * j * (1 + coefficient), MathHelper.clamp(coefficient, 0.75, 1), h * j * (1 + coefficient));
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
		return this.proxy.getDefaultState();
	}

	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.INVISIBLE;
	}

	public static BlockState fromNbt(NbtCompound data) {
		Block block = QuakecraftRegistry.STONE_LAUNCHPAD_BLOCK;
		if (data.contains("type", NbtType.STRING)) {
			block = Registries.BLOCK.getOptionalValue(Quakecraft.id(data.getString("type") + "_launchpad")).orElse(QuakecraftRegistry.STONE_LAUNCHPAD_BLOCK);
		}
		var state = block.getDefaultState();
		Direction direction = Quakecraft.getDirectionByName(data.getString("direction"));
		if (direction.getAxis().isVertical())
			return state;
		state = state.with(Properties.HORIZONTAL_FACING, direction);
		if (data.contains("power", NbtType.INT)) {
			state = state.with(POWER, MathHelper.clamp(data.getInt("power"), POWER_MIN, POWER_MAX));
		}
		return state;
	}
}
