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

package me.lambdaurora.quakecraft;

import me.lambdaurora.quakecraft.block.LaunchPadBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

/**
 * The Quakecraft registry.
 * <p>
 * Contains static definitions of custom blocks, items, etc.
 *
 * @author LambdAurora
 * @version 1.6.1
 * @since 1.6.1
 */
public class QuakecraftRegistry
{
    public static LaunchPadBlock STONE_LAUNCHPAD_BLOCK = register("stone_launchpad", new LaunchPadBlock(Blocks.STONE_PRESSURE_PLATE));
    public static LaunchPadBlock OAK_LAUNCHPAD_BLOCK = register("oak_launchpad", new LaunchPadBlock(Blocks.OAK_PRESSURE_PLATE));
    public static LaunchPadBlock SPRUCE_LAUNCHPAD_BLOCK = register("spruce_launchpad", new LaunchPadBlock(Blocks.SPRUCE_PRESSURE_PLATE));
    public static LaunchPadBlock BIRCH_LAUNCHPAD_BLOCK = register("birch_launchpad", new LaunchPadBlock(Blocks.BIRCH_PRESSURE_PLATE));
    public static LaunchPadBlock JUNGLE_LAUNCHPAD_BLOCK = register("jungle_launchpad", new LaunchPadBlock(Blocks.JUNGLE_PRESSURE_PLATE));
    public static LaunchPadBlock ACACIA_LAUNCHPAD_BLOCK = register("acacia_launchpad", new LaunchPadBlock(Blocks.ACACIA_PRESSURE_PLATE));
    public static LaunchPadBlock DARK_OAK_LAUNCHPAD_BLOCK = register("dark_oak_launchpad", new LaunchPadBlock(Blocks.DARK_OAK_PRESSURE_PLATE));
    public static LaunchPadBlock LIGHT_WEIGHTED_LAUNCHPAD_BLOCK = register("light_weighted_launchpad", new LaunchPadBlock(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE));
    public static LaunchPadBlock HEAVY_WEIGHTED_LAUNCHPAD_BLOCK = register("heavy_weighted_launchpad", new LaunchPadBlock(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE));
    public static LaunchPadBlock CRIMSON_LAUNCHPAD_BLOCK = register("crimson_launchpad", new LaunchPadBlock(Blocks.CRIMSON_PRESSURE_PLATE));
    public static LaunchPadBlock WARPED_LAUNCHPAD_BLOCK = register("warped_launchpad", new LaunchPadBlock(Blocks.WARPED_PRESSURE_PLATE));
    public static LaunchPadBlock POLISHED_BLACKSTONE_LAUNCHPAD_BLOCK = register("polished_blackstone_launchpad", new LaunchPadBlock(Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE));

    private static <T extends Block> T register(String identifier, T block)
    {
        return register(Registry.BLOCK, identifier, block);
    }

    private static <P, T extends P> T register(Registry<P> registry, String identifier, T item)
    {
        return Registry.register(registry, new Identifier(Quakecraft.NAMESPACE, identifier), item);
    }
}
