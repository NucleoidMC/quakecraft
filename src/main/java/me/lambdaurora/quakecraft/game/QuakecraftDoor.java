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

package me.lambdaurora.quakecraft.game;

import me.lambdaurora.quakecraft.block.TeamBarrierBlock;
import me.lambdaurora.quakecraft.game.map.QuakecraftMap;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.game.map.template.TemplateRegion;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.util.BlockBounds;

import java.util.List;
import java.util.Optional;

/**
 * Represents a door which opens/closes automatically.
 *
 * @author LambdAurora
 * @version 1.5.2
 * @since 1.5.0
 */
public class QuakecraftDoor
{
    private final QuakecraftLogic game;
    private final TemplateRegion region;
    private final BlockBounds bounds;
    private final BlockBounds detectionBounds;
    private final Direction.Axis axis;
    private final BlockState openState;
    private final BlockState closedState;
    private final GameTeam team;
    private boolean open = false;
    private int openTicks = 0;

    public QuakecraftDoor(@NotNull QuakecraftLogic game,
                          @NotNull TemplateRegion region,
                          @NotNull BlockBounds bounds, @NotNull BlockBounds detectionBounds,
                          @NotNull Direction.Axis axis, @NotNull BlockState closedState,
                          @Nullable GameTeam team)
    {
        this.game = game;
        this.region = region;
        this.bounds = bounds;
        this.detectionBounds = detectionBounds;
        this.axis = axis;
        this.openState = TeamBarrierBlock.of(team).getDefaultState();
        this.closedState = closedState;
        this.team = team;
    }

    /**
     * Returns the region assigned to this door.
     *
     * @return The region.
     */
    public @NotNull TemplateRegion getRegion()
    {
        return this.region;
    }

    /**
     * The bounds of the door.
     * <p>
     * All positions inside those bounds are replaced with blocks whether the door is closed or not.
     *
     * @return The bounds of the door.
     */
    public @NotNull BlockBounds getBounds()
    {
        return this.bounds;
    }

    /**
     * Returns the detection bounds. The door will open if any allowed player is in the detection bounds.
     *
     * @return The detection bounds.
     */
    public @NotNull BlockBounds getDetectionBounds()
    {
        return this.detectionBounds;
    }

    /**
     * Returns the axis of the door.
     *
     * @return The axis of the door.
     */
    public Direction.Axis getAxis()
    {
        return this.axis;
    }

    /**
     * Returns the team assigned to this door. The team mays be null.
     *
     * @return The assigned team.
     */
    public GameTeam getTeam()
    {
        return this.team;
    }

    /**
     * Returns whether the door is open or not.
     *
     * @return True if the door is open, else false.
     */
    public boolean isOpen()
    {
        return this.open;
    }

    public void tick()
    {
        List<ServerPlayerEntity> players = this.game.getWorld().getWorld().getEntitiesByClass(ServerPlayerEntity.class, this.detectionBounds.toBox(),
                player -> this.game.canOpenDoor(this, player));
        if (players.size() > 0) {
            if (!this.open) {
                this.open();
            }
            this.openTicks = 2;
        }

        if (this.openTicks == 0) {
            this.close();
        } else this.openTicks--;
    }

    /**
     * Opens the door.
     */
    public void open()
    {
        this.getBounds().iterate().forEach(pos -> this.game.getWorld().getWorld().setBlockState(pos, this.openState, 0b0111010));
        this.open = true;
    }

    /**
     * Closes the door.
     */
    public void close()
    {
        this.getBounds().iterate().forEach(pos -> this.game.getWorld().getWorld().setBlockState(pos, this.closedState, 0b0111010));
        this.open = false;
    }

    public static @NotNull Optional<QuakecraftDoor> fromRegion(@NotNull QuakecraftLogic game, @NotNull TemplateRegion region)
    {
        BlockBounds bounds = region.getBounds().offset(QuakecraftMap.ORIGIN);

        Direction.Axis axis = Direction.Axis.fromName(region.getData().getString("axis"));
        if (axis == null)
            return Optional.empty();

        BlockBounds detectionBounds = null;

        if (region.getData().contains("activation", NbtType.STRING)) {
            detectionBounds = game.getMap().getDoorActivationBounds(region.getData().getString("activation"));
        }

        if (detectionBounds == null && region.getData().contains("distance", NbtType.INT)) {
            int distance = region.getData().getInt("distance");
            if (distance == 0)
                return Optional.empty();

            BlockPos min = bounds.getMin().offset(Direction.from(axis, Direction.AxisDirection.NEGATIVE), distance);
            BlockPos max = bounds.getMax().offset(Direction.from(axis, Direction.AxisDirection.POSITIVE), distance);

            detectionBounds = new BlockBounds(min, max);
        }

        if (detectionBounds == null)
            return Optional.empty();

        // A block must be explicitly defined.
        if (!region.getData().getCompound("block").contains("Name"))
            return Optional.empty();
        BlockState closedState = NbtHelper.toBlockState(region.getData().getCompound("block"));

        GameTeam team = game.getTeam(region.getData().getString("team"));

        QuakecraftDoor door = new QuakecraftDoor(game, region, bounds, detectionBounds, axis, closedState, team);
        door.close();
        return Optional.of(door);
    }
}
