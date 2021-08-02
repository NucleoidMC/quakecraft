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

import com.mojang.datafixers.util.Pair;
import dev.lambdaurora.quakecraft.block.LaunchPadBlock;
import dev.lambdaurora.quakecraft.block.TeamBarrierBlock;
import dev.lambdaurora.quakecraft.game.QuakecraftLogic;
import dev.lambdaurora.quakecraft.game.environment.QuakecraftDoor;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.map.template.MapTemplate;
import xyz.nucleoid.plasmid.map.template.TemplateChunkGenerator;
import xyz.nucleoid.plasmid.map.template.TemplateRegion;
import xyz.nucleoid.plasmid.util.BlockBounds;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Represents the Quakecraft map.
 *
 * @author LambdAurora
 * @version 1.6.3
 * @since 1.0.0
 */
public class QuakecraftMap {
    private final MapTemplate template;
    public final BlockBounds waitingSpawn;
    private final List<MapSpawn> spawns;
    private final List<QuakecraftDoor> doors = new ArrayList<>();

    public QuakecraftMap(@NotNull MapTemplate template, @NotNull BlockBounds waitingSpawn, @NotNull List<MapSpawn> spawns) {
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
    public @Nullable BlockBounds getDoorActivationBounds(@NotNull String id) {
        return this.template.getMetadata().getRegions("door_activation")
                .filter(region -> id.equals(region.getData().getString("id")))
                .map(TemplateRegion::getBounds)
                .findFirst().orElse(null);
    }

    public void tick() {
        this.doors.forEach(QuakecraftDoor::tick);
    }

    public void init(@NotNull ServerWorld world) {
        this.initLaunchPads(world);
    }

    private void initLaunchPads(@NotNull ServerWorld world) {
        this.template.getMetadata().getRegions("launchpad")
                .map(region -> {
                    var state = LaunchPadBlock.fromTag(region.getData());
                    if (state == null)
                        return null;
                    // @TODO replace with local record
                    return new Pair<>(region.getBounds(), state);
                })
                .filter(Objects::nonNull)
                .forEach(region -> region.getFirst()
                        .forEach(pos -> world.setBlockState(pos, region.getSecond(), 0b0111010)));
    }

    public void postInit(@NotNull QuakecraftLogic game) {
        this.template.getMetadata().getRegions("door").map(region -> QuakecraftDoor.fromRegion(game, region).orElse(null))
                .filter(Objects::nonNull).forEach(this.doors::add);

        if (game.getTeams().size() != 0) {
            this.template.getMetadata().getRegions("team_barrier").forEach(region -> {
                GameTeam team = game.getTeam(region.getData().getString("team"));
                if (team != null) {
                    BlockState state = TeamBarrierBlock.of(team).getDefaultState();
                    region.getBounds().forEach(pos -> game.getSpace().getWorld().setBlockState(pos, state, 0b0111010));
                }
            });
        }
    }

    public @NotNull ChunkGenerator asGenerator(@NotNull MinecraftServer server) {
        return new TemplateChunkGenerator(server, this.template);
    }
}
