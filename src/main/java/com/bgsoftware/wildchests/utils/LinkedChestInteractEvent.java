package com.bgsoftware.wildchests.utils;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public final class LinkedChestInteractEvent extends PlayerInteractEvent {

    public LinkedChestInteractEvent(Player player, Block block){
        super(player, Action.RIGHT_CLICK_BLOCK, player.getItemInHand(), block, BlockFace.UP);
    }

}
