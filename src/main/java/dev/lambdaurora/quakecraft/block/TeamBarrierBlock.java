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
import dev.lambdaurora.quakecraft.block.entity.TeamBarrierBlockEntity;
import dev.lambdaurora.quakecraft.game.QuakecraftPlayer;
import dev.lambdaurora.quakecraft.util.RayAccessor;
import dev.lambdaurora.quakecraft.util.UsefulEntityShapeContext;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;

/**
 * Represents a team barrier block.
 * <p>
 * The block collisions only with players of a different team.
 *
 * @author LambdAurora
 * @version 1.7.3
 * @since 1.5.0
 */
public class TeamBarrierBlock extends BlockWithEntity implements PolymerBlock {
	public TeamBarrierBlock() {
		super(FabricBlockSettings.create().mapColor(MapColor.NONE).strength(-1.0F, 3600000.0F)
				.nonOpaque().collidable(true).dropsNothing());
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		TeamBarrierBlockEntity blockEntity = QuakecraftRegistry.TEAM_BARRIER_BLOCK_ENTITY.get(world, pos);
		GameTeam team = blockEntity == null ? null : blockEntity.getTeam();

		if (team == null)
			return VoxelShapes.empty();

		if (context instanceof UsefulEntityShapeContext) {
			var entity = ((UsefulEntityShapeContext) context).quakecraft$getEntity();
			if (entity instanceof ServerPlayerEntity player && !((RayAccessor) entity).quakecraft$isRaycasting()) {
				var quakecraft = Quakecraft.get();
				if (quakecraft.isPlayerActive(player)) {
					for (var game : quakecraft.getActiveGames()) {
						if (game.getTeams().size() != 0 && game.getSpace().getPlayers().contains(player)) {
							var pTeam = game.getOptParticipant(player).map(QuakecraftPlayer::getTeam).orElse(null);
							if (pTeam != null) {
								if (team != pTeam) {
									return VoxelShapes.fullCube();
								} else {
									return VoxelShapes.empty();
								}
							}
						}
					}
				}
			}
		}
		return VoxelShapes.empty();
	}

	@Override
	public Block getPolymerBlock(BlockState state) {
		return Blocks.AIR;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return QuakecraftRegistry.TEAM_BARRIER_BLOCK_ENTITY.instantiate(pos, state);
	}

	public static void createAt(ServerWorld world, BlockPos pos, @Nullable GameTeam team) {
		var block = QuakecraftRegistry.TEAM_BARRIER_BLOCK;

		world.setBlockState(pos, block.getDefaultState(),
				Block.SKIP_DROPS | Block.FORCE_STATE | Block.REDRAW_ON_MAIN_THREAD | Block.NOTIFY_ALL);
		var blockEntity = QuakecraftRegistry.TEAM_BARRIER_BLOCK_ENTITY.get(world, pos);
		blockEntity.setTeam(team);
	}
}
