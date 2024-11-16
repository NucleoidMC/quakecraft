/*
 * Copyright (c) 2020-2022 LambdAurora <email@lambdaurora.dev>
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

package dev.lambdaurora.quakecraft.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.lambdaurora.quakecraft.game.map.MapConfig;
import xyz.nucleoid.plasmid.api.game.common.config.WaitingLobbyConfig;
import xyz.nucleoid.plasmid.api.game.common.team.GameTeam;

import java.util.List;

public record QuakecraftConfig(MapConfig map, WaitingLobbyConfig players,
                               List<GameTeam> teams, int time) {
	public static final MapCodec<QuakecraftConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			MapConfig.CODEC.fieldOf("map").forGetter(QuakecraftConfig::map),
			WaitingLobbyConfig.CODEC.fieldOf("players").forGetter(QuakecraftConfig::players),
			GameTeam.CODEC.listOf().fieldOf("teams").forGetter(QuakecraftConfig::teams),
			Codec.INT.optionalFieldOf("time", 20 * 60 * 20).forGetter(QuakecraftConfig::time)
	).apply(instance, QuakecraftConfig::new));
}
