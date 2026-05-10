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

import com.mojang.serialization.MapCodec;
import dev.lambdaurora.quakecraft.Quakecraft;
import dev.lambdaurora.quakecraft.QuakecraftRegistry;
import dev.lambdaurora.quakecraft.block.entity.TeamBarrierBlockEntity;
import dev.lambdaurora.quakecraft.game.QuakecraftPlayer;
import dev.lambdaurora.quakecraft.util.RayAccessor;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeam;

/**
 * Represents a team barrier block.
 * <p>
 * The block collisions only with players of a different team.
 *
 * @author LambdAurora
 * @version 1.7.3
 * @since 1.5.0
 */
public class TeamBarrierBlock extends BaseEntityBlock implements PolymerBlock {
	public TeamBarrierBlock(BlockBehaviour.Properties settings) {
		super(settings.mapColor(MapColor.NONE).strength(-1.0F, 3600000.0F)
				.noOcclusion().dynamicShape().noLootTable());
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return null;
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		TeamBarrierBlockEntity blockEntity = QuakecraftRegistry.TEAM_BARRIER_BLOCK_ENTITY.getBlockEntity(world, pos);
		GameTeam team = blockEntity == null ? null : blockEntity.getTeam();

		if (team == null)
			return Shapes.empty();

		if (context instanceof EntityCollisionContext entityCollisionContext) {
			if (entityCollisionContext.getEntity() instanceof ServerPlayer player && !((RayAccessor) player).quakecraft$isRaycasting()) {
				var quakecraft = Quakecraft.get();
				if (quakecraft.isPlayerActive(player)) {
					for (var game : quakecraft.getActiveGames()) {
						if (game.getTeams().size() != 0 && game.getSpace().getPlayers().contains(player)) {
							var pTeam = game.getOptParticipant(player).map(QuakecraftPlayer::getTeam).orElse(null);
							if (pTeam != null) {
								if (team != pTeam) {
									return Shapes.block();
								} else {
									return Shapes.empty();
								}
							}
						}
					}
				}
			}
		}
		return Shapes.empty();
	}

	@Override
	public BlockState getPolymerBlockState(BlockState state, PacketContext context) {
		return Blocks.AIR.defaultBlockState();
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return QuakecraftRegistry.TEAM_BARRIER_BLOCK_ENTITY.create(pos, state);
	}

	public static void createAt(ServerLevel world, BlockPos pos, @Nullable GameTeam team) {
		var block = QuakecraftRegistry.TEAM_BARRIER_BLOCK;

		world.setBlock(pos, block.defaultBlockState(),
				Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_IMMEDIATE | Block.UPDATE_ALL);
		var blockEntity = QuakecraftRegistry.TEAM_BARRIER_BLOCK_ENTITY.getBlockEntity(world, pos);
		blockEntity.setTeam(team);
	}
}
