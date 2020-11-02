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

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.lambdaurora.quakecraft.game.map.MapConfig;
import org.jetbrains.annotations.NotNull;
import xyz.nucleoid.plasmid.game.config.PlayerConfig;
import xyz.nucleoid.plasmid.game.player.GameTeam;

import java.util.List;

public class QuakecraftConfig
{
    public static final Codec<QuakecraftConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MapConfig.CODEC.fieldOf("map").forGetter(config -> config.map),
            PlayerConfig.CODEC.fieldOf("players").forGetter(config -> config.players),
            GameTeam.CODEC.listOf().fieldOf("teams").forGetter(config -> config.teams),
            Codec.INT.optionalFieldOf("time", 20 * 60 * 20).forGetter(config -> config.time)
    ).apply(instance, QuakecraftConfig::new));

    public final MapConfig map;
    public final PlayerConfig players;
    public final List<GameTeam> teams;
    public final int time;

    public QuakecraftConfig(@NotNull MapConfig map,
                            @NotNull PlayerConfig players,
                            @NotNull List<GameTeam> teams,
                            int time)
    {
        this.map = map;
        this.players = players;
        this.teams = teams;
        this.time = time;
    }
}
