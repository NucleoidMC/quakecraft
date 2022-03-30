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

package dev.lambdaurora.quakecraft.game.map;

import dev.lambdaurora.quakecraft.Quakecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.MapTemplateSerializer;
import xyz.nucleoid.plasmid.game.GameOpenException;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages map loading.
 *
 * @author LambdAurora
 * @version 1.7.0
 * @since 1.0.0
 */
public record MapBuilder(MapConfig config) {
	public QuakecraftMap create(MinecraftServer server) throws GameOpenException {
		MapTemplate template;
		try {
			template = MapTemplateSerializer.loadFromResource(server, this.config.id());
		} catch (IOException e) {
			throw new GameOpenException(new TranslatableText("quakecraft.error.load_map", this.config.id().toString()), e);
		}

		BlockBounds spawn = template.getMetadata().getFirstRegionBounds("waiting_spawn");
		if (spawn == null) {
			spawn = template.getMetadata().getFirstRegionBounds("spawn");
			if (spawn == null) {
				Quakecraft.get().logger.error("No spawn is defined on the map! The game will not work.");
				throw new GameOpenException(new LiteralText("No spawn defined."));
			}
		}

		List<MapSpawn> spawns = template.getMetadata().getRegions("spawn").map(MapSpawn::new).collect(Collectors.toList());

		if (spawns.size() == 0) {
			Quakecraft.get().logger.error("No player spawns are defined on the map! The game will not work.");
			throw new GameOpenException(new LiteralText("No player spawn defined."));
		}

		var map = new QuakecraftMap(template, spawn, spawns);

		//template.setBiome(BuiltinBiomes.PLAINS);

		return map;
	}
}
