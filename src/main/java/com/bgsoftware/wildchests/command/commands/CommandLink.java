package com.bgsoftware.wildchests.command.commands;

import com.bgsoftware.wildchests.Locale;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.chests.LinkedChest;
import com.bgsoftware.wildchests.command.ICommand;
import com.bgsoftware.wildchests.scheduler.Scheduler;
import com.bgsoftware.wildchests.utils.LinkedChestInteractEvent;
import com.bgsoftware.wildchests.utils.LocationUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CommandLink implements ICommand {

    private static final Set<Material> TRANSPARENT_TYPES = createTransparentTypes();

    private static Set<Material> createTransparentTypes() {
        Set<Material> materialSet = EnumSet.noneOf(Material.class);
        for (Material material : Material.values()) {
            if (material.isTransparent())
                materialSet.add(material);
        }
        return materialSet;
    }

    private final Map<UUID, Location> players = new HashMap<>();

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
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return;
        }

        Player player = (Player) sender;

        Block targetBlock = player.getTargetBlock(TRANSPARENT_TYPES, 5);

        if (targetBlock == null || targetBlock.getType() != Material.CHEST) {
            Locale.INVALID_BLOCK_CHEST.send(player);
            return;
        }

        LinkedChestInteractEvent linkedChestInteractEvent = new LinkedChestInteractEvent(player, targetBlock);
        Bukkit.getPluginManager().callEvent(linkedChestInteractEvent);

        if (linkedChestInteractEvent.isCancelled()) {
            Locale.NOT_LINKED_CHEST.send(player);
            return;
        }

        Chest chest = plugin.getChestsManager().getChest(targetBlock.getLocation());

        if (!(chest instanceof LinkedChest)) {
            Locale.NOT_LINKED_CHEST.send(player);
            return;
        }

        LinkedChest linkedChest = (LinkedChest) chest;

        Location targetLinkLocation = players.remove(player.getUniqueId());
        if (targetLinkLocation == null) {
            players.put(player.getUniqueId(), linkedChest.getLocation());
            Scheduler.runTask(() -> players.remove(player.getUniqueId()), 6000L);
            Locale.SELECT_ANOTHER_CHEST.send(player);
            return;
        }

        LinkedChest originalChest = plugin.getChestsManager().getLinkedChest(targetLinkLocation);

        if (originalChest == null || originalChest.getLocation().equals(linkedChest.getLocation()) ||
                originalChest.equals(linkedChest.getLinkedChest())) {
            Locale.NOT_LINKED_CHEST.send(player);
            return;
        }

        List<ItemStack> toMove = new ArrayList<>();

        for (int page = 0; page < originalChest.getPagesAmount(); page++) {
            Inventory inventory = originalChest.getPage(page);
            for (ItemStack itemStack : inventory.getContents()) {
                if (itemStack != null && itemStack.getType() != Material.AIR) {
                    toMove.add(itemStack);
                }
            }
            inventory.clear();
        }

        originalChest.linkIntoChest(linkedChest);

        Locale.LINKED_SUCCEED.send(player, LocationUtils.toString(originalChest.getLocation()));

        if (!toMove.isEmpty()) {
            linkedChest.addItems(toMove.toArray(new ItemStack[]{}));
            Locale.LEFTOVERS_ITEMS_WARNING.send(player);
        }
    }

    @Override
    public List<String> tabComplete(WildChestsPlugin plugin, CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

}
