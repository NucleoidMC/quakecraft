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

import org.jetbrains.annotations.NotNull;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.widget.SidebarWidget;

import java.util.Comparator;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Represents the Quakecraft scoreboard.
 *
 * @author LambdAurora
 * @version 1.7.0
 * @since 1.0.0
 */
public class QuakecraftScoreboard {
	private final QuakecraftGame game;
	private final SidebarWidget sidebar;

	public QuakecraftScoreboard(@NotNull QuakecraftGame game, GlobalWidgets widgets) {
		this.game = game;
		this.sidebar = widgets.addSidebar(Component.literal("Quakecraft").withStyle(ChatFormatting.GOLD));
	}

	/**
	 * Updates the scoreboard.
	 */
	public void update() {
		this.sidebar.set(content -> {
			var seconds = this.game.getTime() / 20;
			content.add(Component.literal("Time left: ")
					.append(Component.literal(String.format("%d:%02d", seconds / 60, seconds % 60)).withStyle(ChatFormatting.GREEN))
			);
			content.add(Component.empty());

			this.game.getParticipants().stream().sorted(Comparator.reverseOrder()).limit(15).forEach(player -> {
				String playerName = player.name;
                /*if ((playerName + ": 10").length() > 16)
                    playerName = playerName.substring(0, 12);*/
				if (player.hasLeft()) {
					content.add(Component.literal(playerName).withStyle(ChatFormatting.GRAY, ChatFormatting.STRIKETHROUGH),
							Component.literal(String.valueOf(player.getKills())).withStyle(ChatFormatting.AQUA));
				} else {
					content.add(Component.literal(playerName).withStyle(ChatFormatting.GRAY),
							Component.literal(String.valueOf(player.getKills())).withStyle(ChatFormatting.AQUA));
				}
			});
		});
	}
}
