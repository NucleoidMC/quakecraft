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

package dev.lambdaurora.quakecraft.game.map;

import dev.lambdaurora.quakecraft.block.LaunchPadBlock;
import dev.lambdaurora.quakecraft.block.TeamBarrierBlock;
import dev.lambdaurora.quakecraft.game.QuakecraftLogic;
import dev.lambdaurora.quakecraft.game.environment.QuakecraftDoor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;
import xyz.nucleoid.plasmid.game.world.generator.TemplateChunkGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Represents the Quakecraft map.
 *
 * @author LambdAurora
 * @version 1.7.3
 * @since 1.0.0
 */
public class QuakecraftMap {
	private final MapTemplate template;
	public final BlockBounds waitingSpawn;
	private final List<MapSpawn> spawns;
	private final List<QuakecraftDoor> doors = new ArrayList<>();

	public QuakecraftMap(MapTemplate template, BlockBounds waitingSpawn, List<MapSpawn> spawns) {
		this.template = template;
		this.waitingSpawn = waitingSpawn;
		this.spawns = spawns;
	}

	/**
	 * Returns the spawn count.
	 *
	 * @return the spawn count
	 */
	public int getSpawnCount() {
		return this.spawns.size();
	}

	/**
	 * Returns a spawn assigned to the specified index.
	 *
	 * @param index the index of the spawn
	 * @return the spawn position
	 */
	public MapSpawn getSpawn(int index) {
		return this.spawns.get(index);
	}

	/**
	 * Streams the spawns.
	 *
	 * @return the spawn stream
	 */
	public Stream<MapSpawn> streamSpawns() {
		return this.spawns.stream();
	}

	/**
	 * Returns the door activation bounds.
	 *
	 * @param id the identifier of the door activation bounds
	 * @return the bounds if found, else {@code null}
	 */
	public @Nullable BlockBounds getDoorActivationBounds(String id) {
		return this.template.getMetadata().getRegions("door_activation")
				.filter(region -> id.equals(region.getData().getString("id")))
				.map(TemplateRegion::getBounds)
				.findFirst().orElse(null);
	}

	public void tick() {
		this.doors.forEach(QuakecraftDoor::tick);
	}

	public void init(ServerWorld world) {
		this.initLaunchPads(world);
	}

	private void initLaunchPads(ServerWorld world) {
		record LaunchPad(BlockBounds bounds, BlockState state) {
		}

		this.template.getMetadata().getRegions("launchpad")
				.map(region -> {
					var state = LaunchPadBlock.fromNbt(region.getData());
					if (state == null)
						return null;
					return new LaunchPad(region.getBounds(), state);
				})
				.filter(Objects::nonNull)
				.forEach(region -> region.bounds()
						.forEach(pos -> world.setBlockState(pos, region.state(),
								Block.SKIP_DROPS | Block.FORCE_STATE | Block.REDRAW_ON_MAIN_THREAD | Block.NOTIFY_ALL)));
	}

	public void postInit(QuakecraftLogic game) {
		this.template.getMetadata().getRegions("door").map(region -> QuakecraftDoor.fromRegion(game, region).orElse(null))
				.filter(Objects::nonNull).forEach(this.doors::add);

		if (game.getTeams().size() != 0) {
			this.template.getMetadata().getRegions("team_barrier").forEach(region -> {
				GameTeam team = game.getTeam(region.getData().getString("team"));

				if (team != null) {
					region.getBounds().forEach(pos -> TeamBarrierBlock.createAt(game.world(), pos, team));
				}
			});
		}
	}

	public ChunkGenerator asGenerator(MinecraftServer server) {
		return new TemplateChunkGenerator(server, this.template);
	}
}
