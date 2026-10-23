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

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.Configuration;
import de.Keyle.MyPet.api.Util;
import de.Keyle.MyPet.api.WorldGroup;
import de.Keyle.MyPet.api.commands.CommandTabCompleter;
import de.Keyle.MyPet.api.entity.MyPet;
import de.Keyle.MyPet.api.player.Permissions;
import de.Keyle.MyPet.api.util.Colorizer;
import de.Keyle.MyPet.api.util.NameFilter;
import de.Keyle.MyPet.api.util.locale.Translation;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import de.Keyle.MyPet.api.util.message.MessageRenderer;

public class CommandName implements CommandTabCompleter {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player petOwner = (Player) sender;
            if (WorldGroup.getGroupByWorld(petOwner.getWorld()).isDisabled()) {
                MessageRenderer.send(petOwner, Translation.getString("Message.No.AllowedHere", petOwner));
                return true;
            }
            if (MyPetApi.getMyPetManager().hasActiveMyPet(petOwner)) {
                if (args.length < 1) {
                    return false;
                }

                MyPet myPet = MyPetApi.getMyPetManager().getMyPet(petOwner);
                if (!Permissions.has(petOwner, "MyPet.command.name")) {
                    myPet.getOwner().sendMessage(Translation.getString("Message.No.CanUse", petOwner));
                    return true;
                }

                StringBuilder name = new StringBuilder();
                for (String arg : args) {
                    if (name.length() > 0) {
                        name.append(" ");
                    }
                    name.append(arg);
                }

                if (!NameFilter.isClean(name.toString())) {
                    MessageRenderer.send(sender, Translation.getString("Message.Command.Name.Filter", petOwner));
                    return true;
                }

                name = new StringBuilder(Colorizer.setColors(name.toString()));

                // close any open formatting so the pet name cannot bleed into the nametag suffix
                if (name.toString().matches(".*(<[a-zA-Z#]|[&§]).*")) {
                    name.append("<reset>");
                }

                String nameWithoutColors = Colorizer.stripColors(name.toString());

                if (nameWithoutColors.length() <= Configuration.Name.MAX_LENGTH) {
                    myPet.setPetName(name.toString());
                    if (Permissions.has(petOwner, "MyPet.command.name.color")) {
                        MessageRenderer.send(sender, Util.formatText(Translation.getString("Message.Command.Name.New", petOwner), name.toString()));
                    } else {
                        MessageRenderer.send(sender, Util.formatText(Translation.getString("Message.Command.Name.New", petOwner), nameWithoutColors));
                    }
                } else {
                    MessageRenderer.send(sender, Util.formatText(Translation.getString("Message.Command.Name.ToLong", petOwner), nameWithoutColors, Configuration.Name.MAX_LENGTH));
                }
            } else {
                MessageRenderer.send(sender, Translation.getString("Message.No.HasPet", petOwner));
            }
            return true;
        }
        MessageRenderer.send(sender, "You can't use this command from server console!");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] strings) {
        return Collections.emptyList();
    }

    @Override
    public String getHelpTranslationKey() {
        return "Message.Command.Help.Name";
    }

    @Override
    public String getHelpCommand() {
        return "/petname";
    }

    @Override
    public boolean isVisibleTo(Player player) {
        return MyPetApi.getMyPetManager().hasActiveMyPet(player)
                && Permissions.has(player, "MyPet.command.name");
    }

    @Override
    public int getHelpOrder() {
        return 90;
    }
}
