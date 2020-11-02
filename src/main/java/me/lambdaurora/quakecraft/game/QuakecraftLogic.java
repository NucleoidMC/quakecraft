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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.game.GameWorld;
import xyz.nucleoid.plasmid.game.player.GameTeam;

import java.util.List;

/**
 * Represents an instance of Quakecraft.
 *
 * @author LambdAurora
 * @version 1.5.0
 * @since 1.5.0
 */
public abstract class QuakecraftLogic
{
    private final GameWorld world;
    private final QuakecraftConfig config;
    private final QuakecraftMap map;

    public QuakecraftLogic(@NotNull GameWorld world, @NotNull QuakecraftConfig config, @NotNull QuakecraftMap map)
    {
        this.world = world;
        this.config = config;
        this.map = map;
    }

    /**
     * Returns the game world.
     *
     * @return The world.
     */
    public GameWorld getWorld()
    {
        return this.world;
    }

    public QuakecraftConfig getConfig()
    {
        return this.config;
    }

    public List<GameTeam> getTeams()
    {
        return this.config.teams;
    }

    public @Nullable GameTeam getTeam(String key)
    {
        if (this.getTeams().size() == 0)
            return null;
        for (GameTeam team : this.getTeams()) {
            if (team.getKey().equals(key))
                return team;
        }
        return null;
    }

    public QuakecraftMap getMap()
    {
        return this.map;
    }

    public void tick()
    {
        this.map.tick();
    }

    /**
     * Returns whether a player can open the specified door.
     * @param door The door.
     * @param player The player.
     * @return True if the player can open the door, else false.
     */
    public boolean canOpenDoor(@NotNull QuakecraftDoor door, @NotNull ServerPlayerEntity player)
    {
        return this.world.containsPlayer(player) && player.interactionManager.getGameMode() != GameMode.SPECTATOR;
    }
}
