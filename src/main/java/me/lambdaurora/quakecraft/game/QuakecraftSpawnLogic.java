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

package me.lambdaurora.quakecraft.game;

import me.lambdaurora.quakecraft.Quakecraft;
import me.lambdaurora.quakecraft.game.map.QuakecraftMap;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;
import org.aperlambda.lambdacommon.utils.Pair;
import org.jetbrains.annotations.NotNull;
import xyz.nucleoid.plasmid.game.GameWorld;
import xyz.nucleoid.plasmid.util.BlockBounds;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;

import java.util.Random;

/**
 * Represents the Quakecraft spawn logic.
 *
 * @author LambdAurora
 * @version 1.1.0
 * @since 1.0.0
 */
public class QuakecraftSpawnLogic
{
    private static final Random        RANDOM = new Random();
    private final        GameWorld     world;
    private final        QuakecraftMap map;
    private final        SpawnCache    spawnCache;

    public QuakecraftSpawnLogic(@NotNull GameWorld world, @NotNull QuakecraftMap map)
    {
        this.world = world;
        this.map = map;
        this.spawnCache = new SpawnCache(map.getSpawnCount() / 2);
    }

    public void spawnPlayer(@NotNull ServerPlayerEntity player)
    {
        int index = this.spawnCache.rollNextSpawn();

        Pair<BlockPos, Integer> spawnPos = this.map.getSpawn(index);
        player.teleport(this.world.getWorld(), spawnPos.getFirst().getX(), spawnPos.getFirst().getY(), spawnPos.getFirst().getZ(), spawnPos.getSecond(), 0.f);
    }

    public void resetWaitingPlayer(@NotNull ServerPlayerEntity player)
    {
        player.setGameMode(GameMode.ADVENTURE);
        player.inventory.clear();

        ItemStack leaveGame = ItemStackBuilder.of(Items.RED_BED)
                .setName(new LiteralText("Leave Lobby").styled(style -> style.withItalic(false).withColor(Formatting.YELLOW)))
                .build();
        player.inventory.insertStack(8, leaveGame);

        Quakecraft.applySpeed(player);
    }

    /**
     * Spawns a player in the waiting room.
     *
     * @param player The player to spawn.
     */
    public void spawnWaitingPlayer(@NotNull ServerPlayerEntity player)
    {
        ServerWorld world = this.world.getWorld();

        BlockBounds bounds = this.map.waitingSpawn;
        BlockPos min = bounds.getMin();
        BlockPos max = bounds.getMax();

        double x = MathHelper.nextDouble(player.getRandom(), min.getX(), max.getX());
        double z = MathHelper.nextDouble(player.getRandom(), min.getZ(), max.getZ());
        double y = min.getY() + 0.5;

        player.teleport(world, x, y, z, 0.f, 0.f);
    }

    /**
     * Represents a spawn cache.
     *
     * @version 1.0.1
     * @since 1.0.1
     */
    public class SpawnCache
    {
        private final int   size;
        private       int[] lastSpawns;

        public SpawnCache(int size)
        {
            this.size = size;
            this.lastSpawns = new int[this.size];

            for (int index = 0; index < this.size; index++) {
                this.lastSpawns[index] = -1;
            }
        }

        /**
         * Returns whether the spawn index is in the last spawn cache or not.
         *
         * @param spawn The spawn index to check.
         * @return True if the spawn index is in the cache, else false.
         */
        public boolean contains(int spawn)
        {
            for (int cached : this.lastSpawns)
                if (spawn == cached)
                    return true;
            return false;
        }

        /**
         * Rolls the next spawn.
         *
         * @return The next spawn index.
         */
        public int rollNextSpawn()
        {
            int index = 0;
            if (map.getSpawnCount() > 1) {
                index = RANDOM.nextInt(map.getSpawnCount() - 1);

                int tries = 0;
                while (this.contains(index) && tries <= this.size) {
                    index++;

                    if (index >= map.getSpawnCount())
                        index = 0;

                    tries++;
                }
            }

            this.push(index);

            return index;
        }

        /**
         * Pushes a new last spawn index.
         *
         * @param spawn The spawn index.
         */
        public void push(int spawn)
        {
            if (this.size - 1 >= 0) System.arraycopy(this.lastSpawns, 0, this.lastSpawns, 1, this.size - 1);

            this.lastSpawns[0] = spawn;
        }
    }
}
