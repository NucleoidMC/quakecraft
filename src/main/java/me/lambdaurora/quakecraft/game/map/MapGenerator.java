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

import me.lambdaurora.quakecraft.Quakecraft;
import net.minecraft.text.LiteralText;
import org.jetbrains.annotations.NotNull;
import xyz.nucleoid.plasmid.game.GameOpenException;
import xyz.nucleoid.plasmid.game.map.template.MapTemplateSerializer;
import xyz.nucleoid.plasmid.util.BlockBounds;

import java.util.concurrent.CompletableFuture;

public class MapGenerator
{
    private final MapConfig config;

    public MapGenerator(@NotNull MapConfig config)
    {
        this.config = config;
    }

    public CompletableFuture<QuakecraftMap> create() throws GameOpenException
    {
        return MapTemplateSerializer.INSTANCE.load(this.config.id).thenApply(template -> {
            BlockBounds spawn = template.getFirstRegion("spawn");
            if (spawn == null) {
                Quakecraft.get().logger.error("No spawn is defined on the map! The game will not work.");
                throw new GameOpenException(new LiteralText("no spawn defined"));
            }

            QuakecraftMap map = new QuakecraftMap(template, spawn.offset(QuakecraftMap.ORIGIN));

            //template.setBiome(BuiltinBiomes.PLAINS);

            return map;
        });
    }
}
