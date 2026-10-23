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
import de.Keyle.MyPet.api.commands.CommandCategory;
import de.Keyle.MyPet.api.commands.CommandOptionTabCompleter;
import de.Keyle.MyPet.api.entity.MyPetType;
import de.Keyle.MyPet.api.exceptions.MyPetTypeNotFoundException;
import de.Keyle.MyPet.api.util.ConfigItem;
import de.Keyle.MyPet.api.util.locale.Translation;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import de.Keyle.MyPet.api.util.message.MessageRenderer;

public class CommandOptionInfo implements CommandOptionTabCompleter {

    public static final List<String> COMMAND_OPTIONS = new ArrayList<>();

    static {
        COMMAND_OPTIONS.add("item");
        COMMAND_OPTIONS.add("leashitem");
    }

    @Override
    public boolean onCommandOption(CommandSender sender, String[] args) {
        if (args.length == 0) {
            MessageRenderer.send(sender, Translation.getString("Message.Command.Help.MissingParameter", sender));
            MessageRenderer.send(sender, " -> " + "<dark_aqua>" + "/petadmin info " + "<red>" + "<what info you want to see>");
            return false;
        }
        switch (args[0].toLowerCase()) {
            case "item":
                if (sender instanceof Player) {
                    ItemStack itemStack;
                    if (MyPetApi.getCompatUtil().isCompatible("1.9")) {
                        itemStack = ((Player) sender).getInventory().getItemInMainHand();
                    } else {
                        itemStack = ((Player) sender).getInventory().getItemInHand();
                    }
                    MessageRenderer.send(sender, "See server logs for the result.");
                    if (itemStack != null && itemStack.getType() != Material.AIR) {
                        String itemString = MyPetApi.getPlatformHelper().itemstackToString(itemStack);
                        MyPetApi.getLogger().info("MyPet Info Item: " + itemString);
                    } else {
                        MyPetApi.getLogger().info("MyPet Info Item: air");
                    }
                } else {
                    MessageRenderer.send(sender, "You can't use this command from server console!");
                }
                break;
            case "leashitem": {
                if (args.length >= 2) {
                    try {
                        MyPetType type = MyPetType.byName(args[1], true);
                        ConfigItem configItem = MyPetApi.getMyPetInfo().getLeashItem(type);
                        ItemStack configItemStack = configItem.getItem();
                        String itemString = "air";
                        if (configItemStack != null) {
                            itemString = MyPetApi.getPlatformHelper().itemstackToString(configItemStack);
                        }
                        MyPetApi.getLogger().info("MyPet Leash Item (" + type + "): " + itemString);
                        if (sender instanceof Player) {
                            ((Player) sender).getInventory().addItem(configItemStack);
                        }
                    } catch (MyPetTypeNotFoundException e) {
                        MessageRenderer.send(sender, Translation.getString("Message.Command.PetType.Unknown", sender));
                    }
                } else {
                    MessageRenderer.send(sender, Translation.getString("Message.Command.Help.MissingParameter", sender));
                    MessageRenderer.send(sender, " -> " + "<dark_aqua>" + "/petadmin info leashitem " + "<red>" + "<pet type>");
                }
                break;
            }
            default:
                MessageRenderer.send(sender, " -> " + "<dark_aqua>" + "/petadmin info " + "<red>" + "<what info you want to see>");
        }
        return true;
    }


    @Override
    public List<String> onTabComplete(CommandSender commandSender, String[] strings) {
        if (strings.length > 2) {
            return Collections.emptyList();
        } else {
            return filterTabCompletionResults(COMMAND_OPTIONS, strings[1]);
        }
    }

    @Override
    public String getHelpCommand() {
        return "/petadmin info";
    }

    @Override
    public CommandCategory getHelpCategory() {
        return CommandCategory.ADMIN;
    }

    @Override
    public String getHelpDescription() {
        return "Shows detailed pet info";
    }

    @Override
    public int getHelpOrder() {
        return 40;
    }
}