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

package me.lambdaurora.quakecraft.game;

import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import xyz.nucleoid.plasmid.widget.GlobalWidgets;
import xyz.nucleoid.plasmid.widget.SidebarWidget;

import java.util.Comparator;

/**
 * Represents the Quakecraft scoreboard.
 *
 * @author LambdAurora
 * @version 1.6.1
 * @since 1.0.0
 */
public class QuakecraftScoreboard
{
    private final QuakecraftGame game;
    private final SidebarWidget sidebar;

    public QuakecraftScoreboard(@NotNull QuakecraftGame game, GlobalWidgets widgets)
    {
        this.game = game;
        this.sidebar = widgets.addSidebar(new LiteralText("Quakecraft").formatted(Formatting.GOLD));
    }

    /**
     * Updates the scoreboard.
     */
    public void update()
    {
        this.sidebar.set(content -> {
            var seconds = this.game.getTime() / 20;
            content.writeLine(String.format("Time left: %s%d:%d", Formatting.GREEN, seconds / 60, seconds % 60));
            content.writeLine("");

            this.game.getParticipants().stream().sorted(Comparator.reverseOrder()).limit(15).forEach(player -> {
                String playerName = player.name;
                /*if ((playerName + ": 10").length() > 16)
                    playerName = playerName.substring(0, 12);*/
                if (player.hasLeft()) {
                    content.writeLine(String.format("%s%s%s%s: %s%d",
                            Formatting.GRAY,
                            Formatting.STRIKETHROUGH,
                            playerName,
                            Formatting.RESET,
                            Formatting.AQUA,
                            player.getKills()));
                } else {
                    content.writeLine(String.format("%s%s%s: %s%d",
                            Formatting.GRAY,
                            playerName,
                            Formatting.RESET,
                            Formatting.AQUA,
                            player.getKills()));
                }
            });
        });
    }
}
