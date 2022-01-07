package com.bgsoftware.wildchests.hooks;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Modules.Worth.WorthItem;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.hooks.PricesProvider;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

public final class PricesProvider_CMI implements PricesProvider {

    public PricesProvider_CMI(){
        WildChestsPlugin.log(" - Using CMI as PricesProvider.");
    }

    @Override
    public double getPrice(OfflinePlayer offlinePlayer, ItemStack itemStack) {
        WorthItem worth = CMI.getInstance().getWorthManager().getWorth(itemStack);
        return worth == null ? 0 : worth.getSellPrice() * itemStack.getAmount();
    }

}
