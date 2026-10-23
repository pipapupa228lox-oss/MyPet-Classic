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

package de.Keyle.MyPet.commands.admin;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.commands.CommandCategory;
import de.Keyle.MyPet.api.commands.CommandOptionTabCompleter;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.util.Colorizer;
import de.Keyle.MyPet.api.util.locale.Translation;
import org.bukkit.Bukkit;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import de.Keyle.MyPet.api.util.message.MessageRenderer;

public class CommandOptionName implements CommandOptionTabCompleter {
    @Override
    public boolean onCommandOption(CommandSender sender, String[] args) {
        if (args.length == 0) {
            MessageRenderer.send(sender, Translation.getString("Message.Command.Help.MissingParameter", sender));
            MessageRenderer.send(sender, " -> " + "<dark_aqua>" + "/petadmin name " + "<red>" + "<a player name>");
            return false;
        }
        if (args.length < 2) {
            MessageRenderer.send(sender, Translation.getString("Message.Command.Help.MissingParameter", sender));
            MessageRenderer.send(sender, " -> " + "<dark_aqua>" + "/petadmin name " + args[0] + " " + "<red>" + "<new name>");
            return false;
        }

        String lang = MyPetApi.getPlatformHelper().getCommandSenderLanguage(sender);
        Player petOwner = Bukkit.getServer().getPlayer(args[0]);

        if (petOwner == null || !petOwner.isOnline()) {
            MessageRenderer.send(sender, "[" + "<aqua>" + "MyPet" + "<reset>" + "] " + Translation.getString("Message.No.PlayerOnline", lang));
            return true;
        } else if (!MyPetApi.getMyPetManager().hasActiveMyPet(petOwner)) {
            MessageRenderer.send(sender, "[" + "<aqua>" + "MyPet" + "<reset>" + "] " + Util.formatText(Translation.getString("Message.No.UserHavePet", lang), petOwner.getName()));
            return true;
        }
        MyPet myPet = MyPetApi.getMyPetManager().getMyPet(petOwner);

        StringBuilder name = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (name.length() > 0) {
                name.append(" ");
            }
            name.append(args[i]);
        }
        name = new StringBuilder(Colorizer.setColors(name.toString()));

        // close any open formatting so the pet name cannot bleed into the nametag suffix
        if (name.toString().matches(".*(<[a-zA-Z#]|[&§]).*")) {
            name.append("<reset>");
        }

        myPet.setPetName(name.toString());
        MessageRenderer.send(sender, "[" + "<aqua>" + "MyPet" + "<reset>" + "] new name is now: " + name);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, String[] strings) {
        if (strings.length == 2) {
            return null;
        }
        return Collections.emptyList();
    }

    @Override
    public String getHelpCommand() {
        return "/petadmin name";
    }

    @Override
    public CommandCategory getHelpCategory() {
        return CommandCategory.ADMIN;
    }

    @Override
    public String getHelpDescription() {
        return "Renames a player's pet";
    }

    @Override
    public int getHelpOrder() {
        return 24;
    }
}