package com.bgsoftware.wildchests.hooks.listener;

import com.bgsoftware.wildchests.api.objects.chests.Chest;
import org.bukkit.OfflinePlayer;

import javax.annotation.Nullable;

public interface IChestBreakListener {

    void breakChest(@Nullable OfflinePlayer offlinePlayer, Chest chest);

}
