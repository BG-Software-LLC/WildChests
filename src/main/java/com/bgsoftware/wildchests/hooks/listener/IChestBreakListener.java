package com.bgsoftware.wildchests.hooks.listener;

import com.bgsoftware.wildchests.api.objects.chests.Chest;
import org.bukkit.OfflinePlayer;

public interface IChestBreakListener {

    void breakChest(OfflinePlayer offlinePlayer, Chest chest);

}
