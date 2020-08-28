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

import org.jetbrains.annotations.NotNull;
import xyz.nucleoid.plasmid.game.GameOpenException;
import xyz.nucleoid.plasmid.game.map.template.MapTemplateSerializer;

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
            QuakecraftMap map = new QuakecraftMap(template);
            //template.setBiome(BuiltinBiomes.PLAINS);

            return map;
        });
    }
}
