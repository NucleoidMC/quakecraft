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

import me.lambdaurora.quakecraft.game.map.QuakecraftMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.NotNull;
import xyz.nucleoid.plasmid.game.map.template.TemplateRegion;
import xyz.nucleoid.plasmid.util.BlockBounds;

import java.util.List;
import java.util.Optional;

/**
 * Represents a door which opens/closes automatically.
 *
 * @author LambdAurora
 * @version 1.5.0
 * @since 1.5.0
 */
public class QuakecraftDoor
{
    private final QuakecraftLogic game;
    private final TemplateRegion region;
    private final BlockBounds bounds;
    private final BlockBounds detectionBounds;
    private final Direction facing;
    private final BlockState closedState;
    private boolean open = false;

    public QuakecraftDoor(@NotNull QuakecraftLogic game,
                          @NotNull TemplateRegion region,
                          @NotNull BlockBounds bounds, @NotNull BlockBounds detectionBounds,
                          @NotNull Direction facing, @NotNull BlockState closedState)
    {
        this.game = game;
        this.region = region;
        this.bounds = bounds;
        this.detectionBounds = detectionBounds;
        this.facing = facing;
        this.closedState = closedState;
    }

    public @NotNull TemplateRegion getRegion()
    {
        return this.region;
    }

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

    public void tick()
    {
        List<ServerPlayerEntity> players = this.game.getWorld().getWorld().getEntitiesByClass(ServerPlayerEntity.class, this.detectionBounds.toBox(),
                player -> this.game.canOpenDoor(this, player));
        if (players.size() == 0 && this.open) {
            this.close();
        } else if (players.size() > 0 && !this.open) {
            this.open();
        }
    }

    /**
     * Opens the door.
     */
    public void open()
    {
        this.getBounds().iterate().forEach(pos -> this.game.getWorld().getWorld().setBlockState(pos, Blocks.AIR.getDefaultState(), 0b0111010));
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
        String serializedDirection = region.getData().getString("facing");
        Direction facing = Direction.byName(serializedDirection);
        if (facing == null)
            return Optional.empty();

        int distance = region.getData().getInt("distance");
        if (distance == 0)
            return Optional.empty();

        BlockPos min = region.getBounds().getMin().offset(facing.getOpposite(), distance);
        BlockPos max = region.getBounds().getMax().offset(facing, distance);
        BlockBounds detectionBounds = new BlockBounds(min, max).offset(QuakecraftMap.ORIGIN);

        // A block must be explicitly defined.
        if (!region.getData().getCompound("block").contains("Name"))
            return Optional.empty();
        BlockState closedState = NbtHelper.toBlockState(region.getData().getCompound("block"));

        QuakecraftDoor door = new QuakecraftDoor(game, region, region.getBounds().offset(QuakecraftMap.ORIGIN), detectionBounds,
                facing, closedState);
        door.close();
        return Optional.of(door);
    }
}
