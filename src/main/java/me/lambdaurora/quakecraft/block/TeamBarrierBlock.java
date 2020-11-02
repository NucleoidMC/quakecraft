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

package me.lambdaurora.quakecraft.block;

import me.lambdaurora.quakecraft.Quakecraft;
import me.lambdaurora.quakecraft.game.QuakecraftLogic;
import me.lambdaurora.quakecraft.game.QuakecraftPlayer;
import me.lambdaurora.quakecraft.util.RayAccessor;
import me.lambdaurora.quakecraft.util.UsefulEntityShapeContext;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.fake.FakeBlock;
import xyz.nucleoid.plasmid.game.player.GameTeam;

/**
 * Represents a team barrier block.
 * <p>
 * The block collisions only with players of a different team.
 *
 * @author LambdAurora
 * @version 1.5.0
 * @since 1.5.0
 */
public class TeamBarrierBlock extends Block implements FakeBlock
{
    private final GameTeam team;

    public TeamBarrierBlock(@Nullable GameTeam team)
    {
        super(FabricBlockSettings.of(Material.BARRIER, MaterialColor.CLEAR).breakByHand(false).nonOpaque().collidable(true).dropsNothing());
        this.team = team;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context)
    {
        if (this.team == null)
            return VoxelShapes.empty();

        if (context instanceof UsefulEntityShapeContext) {
            Entity entity = ((UsefulEntityShapeContext) context).quakecraft$getEntity();
            if (entity instanceof ServerPlayerEntity && !((RayAccessor) entity).quakecraft$isRaycasting()) {
                ServerPlayerEntity player = (ServerPlayerEntity) entity;
                Quakecraft quakecraft = Quakecraft.get();
                if (quakecraft.isPlayerActive(player)) {
                    for (QuakecraftLogic game : quakecraft.getActiveGames()) {
                        if (game.getTeams().size() != 0 && game.getWorld().containsPlayer(player)) {
                            GameTeam team = game.getOptParticipant(player).map(QuakecraftPlayer::getTeam).orElse(null);
                            if (team != null) {
                                if (team != this.team) {
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
    public Block asProxy()
    {
        return Blocks.AIR;
    }

    @Override
    public BlockState asProxy(BlockState state)
    {
        return this.asProxy().getDefaultState();
    }

    public static TeamBarrierBlock of(@Nullable GameTeam team)
    {
        return new TeamBarrierBlock(team);
    }
}
