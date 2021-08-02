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

package dev.lambdaurora.quakecraft.game.map;

import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.map_templates.TemplateRegion;

/**
 * Represents a spawn.
 *
 * @author LambdAurora
 * @version 1.7.0
 * @since 1.4.7
 */
public class MapSpawn {
    private final TemplateRegion region;
    private final BlockPos pos;
    private final int direction;

    public MapSpawn(TemplateRegion region) {
        this.region = region;
        this.pos = new BlockPos(region.getBounds().center());
        this.direction = region.getData().getInt("direction");
    }

    /**
     * Returns the region.
     *
     * @return the region
     */
    public TemplateRegion region() {
        return this.region;
    }

    /**
     * Returns the spawn position.
     *
     * @return the position
     */
    public BlockPos pos() {
        return this.pos;
    }

    /**
     * Returns the direction the spawn is facing.
     *
     * @return the facing direction
     */
    public int direction() {
        return this.direction;
    }
}
