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

package me.lambdaurora.quakecraft.game.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.aperlambda.lambdacommon.utils.Pair;
import org.jetbrains.annotations.NotNull;
import xyz.nucleoid.plasmid.game.map.template.MapTemplate;
import xyz.nucleoid.plasmid.game.map.template.TemplateChunkGenerator;
import xyz.nucleoid.plasmid.util.BlockBounds;

import java.util.List;

/**
 * Represents the Quakecraft map.
 *
 * @author LambdAurora
 * @version 1.4.2
 * @since 1.0.0
 */
public class QuakecraftMap
{
    public static final BlockPos ORIGIN = BlockPos.ORIGIN;
    private final MapTemplate template;
    public final BlockBounds waitingSpawn;
    private final List<Pair<BlockPos, Integer>> spawns;

    public QuakecraftMap(@NotNull MapTemplate template, @NotNull BlockBounds waitingSpawn, @NotNull List<Pair<BlockPos, Integer>> spawns)
    {
        this.template = template;
        this.waitingSpawn = waitingSpawn;
        this.spawns = spawns;
    }

    /**
     * Returns the spawn count.
     *
     * @return The spawn count.
     */
    public int getSpawnCount()
    {
        return this.spawns.size();
    }

    /**
     * Returns a spawn assigned to the specified index.
     *
     * @param index The index of the spawn.
     * @return The spawn position.
     */
    public Pair<BlockPos, Integer> getSpawn(int index)
    {
        return this.spawns.get(index);
    }

    public @NotNull ChunkGenerator asGenerator(@NotNull MinecraftServer server)
    {
        return new TemplateChunkGenerator(server, this.template, ORIGIN);
    }
}
