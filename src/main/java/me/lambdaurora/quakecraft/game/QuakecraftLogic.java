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

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.lambdaurora.quakecraft.Quakecraft;
import me.lambdaurora.quakecraft.game.map.QuakecraftMap;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.game.GameLogic;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.player.GameTeam;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents an instance of Quakecraft.
 *
 * @author LambdAurora
 * @version 1.6.0
 * @since 1.5.0
 */
public abstract class QuakecraftLogic
{
    private final GameSpace space;
    private final QuakecraftConfig config;
    private final QuakecraftMap map;
    protected final Object2ObjectMap<UUID, QuakecraftPlayer> participants = new Object2ObjectOpenHashMap<>();
    private GameStage stage;

    public QuakecraftLogic(@NotNull GameLogic logic, @NotNull QuakecraftConfig config, @NotNull QuakecraftMap map)
    {
        this.space = logic.getSpace();
        this.config = config;
        this.map = map;
        this.stage = GameStage.ROUND_START;

        this.getSpace().getPlayers().forEach(player ->
                this.participants.put(player.getUuid(), new QuakecraftPlayer(player, null))
        );
    }

    /**
     * Returns the game space.
     *
     * @return the game space
     */
    public GameSpace getSpace()
    {
        return this.space;
    }

    public ServerWorld getWorld()
    {
        return this.getSpace().getWorld();
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

    protected void onOpen()
    {
        Quakecraft.get().addActiveGame(this);
    }

    protected void onClose()
    {
        Quakecraft.get().removeActiveGame(this);
    }

    public void tick()
    {
        this.map.tick();
    }

    /**
     * Returns whether a player can open the specified door.
     *
     * @param door the door
     * @param player the player
     * @return {@code true} if the player can open the door, else {@code false}
     */
    public boolean canOpenDoor(@NotNull QuakecraftDoor door, @NotNull ServerPlayerEntity player)
    {
        if (!this.getSpace().containsPlayer(player) || player.interactionManager.getGameMode() == GameMode.SPECTATOR)
            return false;
        GameTeam team = this.getOptParticipant(player).map(QuakecraftPlayer::getTeam).orElse(null);
        return door.getTeam() == null || team == null || team == door.getTeam();
    }

    public @Nullable QuakecraftPlayer getParticipant(@NotNull ServerPlayerEntity player)
    {
        return this.participants.get(player.getUuid());
    }

    public @NotNull Optional<QuakecraftPlayer> getOptParticipant(@NotNull ServerPlayerEntity player)
    {
        return Optional.ofNullable(this.getParticipant(player));
    }

    public @NotNull Collection<QuakecraftPlayer> getParticipants()
    {
        return this.participants.values();
    }
}
