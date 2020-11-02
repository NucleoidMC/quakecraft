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
import xyz.nucleoid.plasmid.widget.SidebarWidget;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Represents the Quakecraft scoreboard.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class QuakecraftScoreboard implements AutoCloseable
{
    private final SidebarWidget  sidebar;
    private final QuakecraftGame game;

    public QuakecraftScoreboard(@NotNull QuakecraftGame game)
    {
        this.sidebar = SidebarWidget.open(new LiteralText("Quakecraft").formatted(Formatting.GOLD),
                game.getWorld().getPlayerSet());
        this.game = game;
    }

    /**
     * Updates the scoreboard.
     */
    public void update()
    {
        List<String> lines = new ArrayList<>();

        int seconds = this.game.getTime() / 20;
        lines.add(String.format("Time left: %s%d:%d", Formatting.GREEN, seconds / 60, seconds % 60));
        lines.add("");

        this.game.getParticipants().stream().sorted(Comparator.reverseOrder()).forEach(player -> {
            if (player.hasLeft()) {
                lines.add(String.format("%s%s%s%s: %s%d",
                        Formatting.GRAY,
                        Formatting.STRIKETHROUGH,
                        player.name,
                        Formatting.RESET,
                        Formatting.AQUA,
                        player.getKills()));
            } else {
                lines.add(String.format("%s%s%s: %s%d",
                        Formatting.GRAY,
                        player.name,
                        Formatting.RESET,
                        Formatting.AQUA,
                        player.getKills()));
            }
        });

        this.sidebar.set(lines.toArray(new String[0]));
    }

    @Override
    public void close()
    {
        this.sidebar.close();
    }
}
