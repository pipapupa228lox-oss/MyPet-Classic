/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2019 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
 *
 * MyPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.commands;

import de.Keyle.MyPet.api.commands.CommandCategory;
import de.Keyle.MyPet.api.commands.CommandOption;
import de.Keyle.MyPet.api.commands.CommandOptionTabCompleter;
import de.Keyle.MyPet.api.commands.CommandTabCompleter;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.locale.Translation;
import de.Keyle.MyPet.commands.admin.*;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import de.Keyle.MyPet.api.util.message.MessageRenderer;

public class CommandAdmin implements CommandTabCompleter {

    private List<String> optionsList = new ArrayList<>();
    private final Map<String, CommandOption> commandOptions = new HashMap<>();

    {
        commandOptions.put("name", new CommandOptionName());
        commandOptions.put("exp", new CommandOptionExp());
        commandOptions.put("exp-rate", new CommandOptionExpRate());
        commandOptions.put("respawn", new CommandOptionRespawn());
        commandOptions.put("skilltree", new CommandOptionSkilltree());
        commandOptions.put("create", new CommandOptionCreate());
        commandOptions.put("clone", new CommandOptionClone());
        commandOptions.put("remove", new CommandOptionRemove());
        commandOptions.put("cleanup", new CommandOptionCleanup());
        commandOptions.put("switch", new CommandOptionSwitch());
        commandOptions.put("info", new CommandOptionInfo());
    }

    public Map<String, CommandOption> getCommandOptions() {
        return commandOptions;
    }

    public void registerOption(String name, CommandOption option) {
        commandOptions.put(name, option);
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            if (!Permissions.has((Player) sender, "MyPet.admin", false)) {
                MessageRenderer.send(sender, Translation.getString("Message.No.Allowed", sender));
                return true;
            }
        }

        if (args.length < 1) {
            MessageRenderer.send(sender, Translation.getString("Message.Command.Help.MissingParameter", sender));
            MessageRenderer.send(sender, " -> " + "<dark_aqua>" + String.join("<reset>" + ", " + "<dark_aqua>", commandOptions.keySet()));
            return false;
        }

        String[] parameter = Arrays.copyOfRange(args, 1, args.length);
        CommandOption option = commandOptions.get(args[0].toLowerCase());

        if (option != null) {
            return option.onCommandOption(sender, parameter);
        }
        MessageRenderer.send(sender, "[" + "<aqua>" + "MyPet" + "<reset>" + "] \"" + "<italic>" + args[0].toLowerCase() + "<reset>" + "\" is not a valid option!");
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        if (commandSender instanceof Player && !Permissions.has((Player) commandSender, "MyPet.admin", false)) {
            return Collections.emptyList();
        }
        if (strings.length == 1) {
            if (optionsList.size() != commandOptions.keySet().size()) {
                optionsList = new ArrayList<>(commandOptions.keySet());
                Collections.sort(optionsList);
            }
            return filterTabCompletionResults(optionsList, strings[0]);
        } else if (strings.length >= 1) {
            CommandOption co = commandOptions.get(strings[0]);
            if (co != null) {
                if (co instanceof CommandOptionTabCompleter) {
                    return ((CommandOptionTabCompleter) co).onTabComplete(commandSender, strings);
                }
            }
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isVisibleTo(Player player) {
        return Permissions.has(player, "MyPet.admin", false);
    }

    @Override
    public CommandCategory getHelpCategory() {
        return CommandCategory.ADMIN;
    }
}
