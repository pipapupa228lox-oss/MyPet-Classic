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

package de.Keyle.MyPet.commands.admin.npc;

import de.Keyle.MyPet.MyPetApi;
import de.Keyle.MyPet.api.commands.CommandOptionTabCompleter;
import de.Keyle.MyPet.api.util.service.types.ShopService;
import de.Keyle.MyPet.util.hooks.citizens.ShopTrait;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;

import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import de.Keyle.MyPet.api.util.message.MessageRenderer;

public class CommandOptionShop implements CommandOptionTabCompleter {

    @Override
    public boolean onCommandOption(CommandSender sender, String[] args) {
        if (args.length >= 1) {
            NPC selectedNPC = CitizensAPI.getDefaultNPCSelector().getSelected(sender);
            if (selectedNPC == null) {
                MessageRenderer.send(sender, "[" + "<aqua>" + "MyPet" + "<reset>" + "] No NPC seleced!");
                return true;
            }

            if (!selectedNPC.hasTrait(ShopTrait.class)) {
                MessageRenderer.send(sender, "[" + "<aqua>" + "MyPet" + "<reset>" + "] This NPC doesn't has the " + "<gold>" + "mypet-shop" + "<reset>" + " trait!");
                return true;
            }

            String shop = args[0];

            List<ShopService> shopServiceList = MyPetApi.getServiceManager().getServices(ShopService.class);
            if (!shopServiceList.isEmpty()) {
                boolean shopFound = false;
                for (ShopService shopService : shopServiceList) {
                    if (shopService.getShopNames().contains(shop)) {
                        shopFound = true;
                        break;
                    }
                }
                if (!shopFound) {
                    MessageRenderer.send(sender, "[" + "<aqua>" + "MyPet" + "<reset>" + "] No shop with this name found: " + shop);
                    return true;
                }
            }

            ShopTrait trait = selectedNPC.getTrait(ShopTrait.class);

            trait.setShop(shop);

            MessageRenderer.send(sender, "[" + "<aqua>" + "MyPet" + "<reset>" + "] Shop trait updated.");
            return true;
        }
        MessageRenderer.send(sender, "[" + "<aqua>" + "MyPet" + "<reset>" + "] Please enter a shopname!");
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, String[] strings) {
        if (strings.length == 3) {
            List<ShopService> shopServiceList = MyPetApi.getServiceManager().getServices(ShopService.class);
            if (!shopServiceList.isEmpty()) {
                return new ArrayList<>(shopServiceList.get(0).getShopNames());
            }
        }
        return Collections.emptyList();
    }
}
