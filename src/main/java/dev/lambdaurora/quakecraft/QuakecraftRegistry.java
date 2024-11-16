/*
 * Copyright (c) 2022 LambdAurora <email@lambdaurora.dev>
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

package dev.lambdaurora.quakecraft;

import dev.lambdaurora.quakecraft.block.LaunchPadBlock;
import dev.lambdaurora.quakecraft.block.TeamBarrierBlock;
import dev.lambdaurora.quakecraft.block.entity.TeamBarrierBlockEntity;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

import java.util.function.Function;

/**
 * The Quakecraft registry.
 * <p>
 * Contains static definitions of custom blocks, items, etc.
 *
 * @author LambdAurora
 * @version 1.7.3
 * @since 1.6.1
 */
public class QuakecraftRegistry {
	public static LaunchPadBlock STONE_LAUNCHPAD_BLOCK = register("stone_launchpad", settings -> new LaunchPadBlock(settings, Blocks.STONE_PRESSURE_PLATE));
	public static LaunchPadBlock OAK_LAUNCHPAD_BLOCK = register("oak_launchpad", settings -> new LaunchPadBlock(settings, Blocks.OAK_PRESSURE_PLATE));
	public static LaunchPadBlock SPRUCE_LAUNCHPAD_BLOCK = register("spruce_launchpad", settings -> new LaunchPadBlock(settings, Blocks.SPRUCE_PRESSURE_PLATE));
	public static LaunchPadBlock BIRCH_LAUNCHPAD_BLOCK = register("birch_launchpad", settings -> new LaunchPadBlock(settings, Blocks.BIRCH_PRESSURE_PLATE));
	public static LaunchPadBlock JUNGLE_LAUNCHPAD_BLOCK = register("jungle_launchpad", settings -> new LaunchPadBlock(settings, Blocks.JUNGLE_PRESSURE_PLATE));
	public static LaunchPadBlock ACACIA_LAUNCHPAD_BLOCK = register("acacia_launchpad", settings -> new LaunchPadBlock(settings, Blocks.ACACIA_PRESSURE_PLATE));
	public static LaunchPadBlock DARK_OAK_LAUNCHPAD_BLOCK = register("dark_oak_launchpad", settings -> new LaunchPadBlock(settings, Blocks.DARK_OAK_PRESSURE_PLATE));
	public static LaunchPadBlock LIGHT_WEIGHTED_LAUNCHPAD_BLOCK = register("light_weighted_launchpad", settings -> new LaunchPadBlock(settings, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE));
	public static LaunchPadBlock HEAVY_WEIGHTED_LAUNCHPAD_BLOCK = register("heavy_weighted_launchpad", settings -> new LaunchPadBlock(settings, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE));
	public static LaunchPadBlock CRIMSON_LAUNCHPAD_BLOCK = register("crimson_launchpad", settings -> new LaunchPadBlock(settings, Blocks.CRIMSON_PRESSURE_PLATE));
	public static LaunchPadBlock WARPED_LAUNCHPAD_BLOCK = register("warped_launchpad", settings -> new LaunchPadBlock(settings, Blocks.WARPED_PRESSURE_PLATE));
	public static LaunchPadBlock POLISHED_BLACKSTONE_LAUNCHPAD_BLOCK = register("polished_blackstone_launchpad", settings -> new LaunchPadBlock(settings, Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE));

	public static TeamBarrierBlock TEAM_BARRIER_BLOCK = register("team_barrier", TeamBarrierBlock::new);

	public static BlockEntityType<TeamBarrierBlockEntity> TEAM_BARRIER_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE,
			Quakecraft.id("team_barrier"),
			FabricBlockEntityTypeBuilder.create(TeamBarrierBlockEntity::new, TEAM_BARRIER_BLOCK).build()
	);

	private static <T extends Block> T register(String identifier, Function<AbstractBlock.Settings, T> function) {
		return register(Registries.BLOCK, identifier, function.apply(AbstractBlock.Settings.create().registryKey(RegistryKey.of(RegistryKeys.BLOCK, Quakecraft.id(identifier)))));
	}

	private static <P, T extends P> T register(Registry<P> registry, String name, T item) {
		return Registry.register(registry, Quakecraft.id(name), item);
	}

	public static void init() {
		Quakecraft.get().log("Registered custom blocks, items...");
	}

	static {
		PolymerBlockUtils.registerBlockEntity(TEAM_BARRIER_BLOCK_ENTITY);
	}
}
