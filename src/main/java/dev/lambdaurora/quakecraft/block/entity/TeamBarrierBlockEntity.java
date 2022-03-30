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

package dev.lambdaurora.quakecraft.block.entity;

import dev.lambdaurora.quakecraft.QuakecraftRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.game.common.team.GameTeam;

/**
 * Represents the team barrier block entity.
 *
 * @author LambdAurora
 * @version 1.7.3
 * @since 1.7.3
 */
public class TeamBarrierBlockEntity extends BlockEntity {
	private @Nullable GameTeam team;

	public TeamBarrierBlockEntity(BlockPos blockPos, BlockState blockState) {
		super(QuakecraftRegistry.TEAM_BARRIER_BLOCK_ENTITY, blockPos, blockState);
	}

	public @Nullable GameTeam getTeam() {
		return this.team;
	}

	public void setTeam(@Nullable GameTeam team) {
		this.team = team;
	}
}
