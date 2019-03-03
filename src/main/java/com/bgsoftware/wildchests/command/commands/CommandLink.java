package com.bgsoftware.wildchests.command.commands;

import com.bgsoftware.wildchests.objects.WLocation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import com.bgsoftware.wildchests.Locale;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.LinkedChest;
import com.bgsoftware.wildchests.command.ICommand;
import com.bgsoftware.wildchests.utils.ItemUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CommandLink implements ICommand {

    private Map<UUID, WLocation> players = new HashMap<>();

    @Override
    public String getLabel() {
        return "link";
    }

    @Override
    public String getUsage() {
        return "chests link";
    }

    @Override
    public String getPermission() {
        return "wildchests.link";
    }

    @Override
    public String getDescription() {
        return "Links a linked chest into another chest.";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return 1;
    }

    @Override
    public void perform(WildChestsPlugin plugin, CommandSender sender, String[] args) {
        if(!(sender instanceof Player)){
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return;
        }

        Player player = (Player) sender;

        Block targetBlock = player.getTargetBlock((Set<Material>) null, 5);

        if(targetBlock == null || targetBlock.getType() != Material.CHEST){
            Locale.INVALID_BLOCK_CHEST.send(player);
            return;
        }

        LinkedChest linkedChest = plugin.getChestsManager().getLinkedChest(targetBlock.getLocation());

        if(linkedChest == null){
            Locale.NOT_LINKED_CHEST.send(player);
            return;
        }

        if(players.containsKey(player.getUniqueId())){
            LinkedChest originalChest = plugin.getChestsManager().getLinkedChest(players.get(player.getUniqueId()).getLocation());
            players.remove(player.getUniqueId());


            if(originalChest == null || originalChest.getLocation().equals(linkedChest.getLocation()) ||
                    originalChest.equals(linkedChest.getLinkedChest())){
                Locale.NOT_LINKED_CHEST.send(player);
                return;
            }

            List<ItemStack> toMove = new ArrayList<>();

            for(int page = 0; page < originalChest.getPagesAmount(); page++){
                Inventory inventory = originalChest.getPage(page);
                for(ItemStack itemStack : inventory.getContents()){
                    if(itemStack != null && itemStack.getType() != Material.AIR){
                        toMove.add(itemStack);
                    }
                }
                inventory.clear();
            }

            originalChest.linkIntoChest(linkedChest);

            Locale.LINKED_SUCCEED.send(player, WLocation.of(originalChest.getLocation()));

            if(!toMove.isEmpty()){
                for(ItemStack itemStack : toMove)
                    ItemUtils.addToChest(linkedChest, itemStack);
                Locale.LEFTOVERS_ITEMS_WARNING.send(player);
            }

            return;
        }

        players.put(player.getUniqueId(), WLocation.of(linkedChest.getLocation()));
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> players.remove(player.getUniqueId()), 6000L);
        Locale.SELECT_ANOTHER_CHEST.send(player);
    }

    @Override
    public List<String> tabComplete(WildChestsPlugin plugin, CommandSender sender, String[] args) {
        return new ArrayList<>();
    }

}
