package com.bgsoftware.wildchests.hooks;

import com.bgsoftware.wildchests.WildChestsPlugin;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import su.nightexpress.quantumshop.QuantumShop;
import su.nightexpress.quantumshop.modules.list.gui.GUIShop;
import su.nightexpress.quantumshop.modules.list.gui.objects.ShopGUI;
import su.nightexpress.quantumshop.modules.list.gui.objects.ShopProduct;

public final class PricesProvider_QuantumShop implements PricesProvider {

    private final GUIShop guiShop;

    public PricesProvider_QuantumShop(){
        WildChestsPlugin.log(" - Using QuantumShop as PricesProvider.");
        guiShop = QuantumShop.instance.getGUIShop();
    }

    @Override
    public double getPrice(OfflinePlayer offlinePlayer, ItemStack itemStack) {
        for(ShopGUI shop : guiShop.getShops()){
            ShopProduct shopProduct = shop.getProducts().values().stream()
                    .filter(_shopProduct -> isValidItem(_shopProduct, itemStack)).findFirst().orElse(null);
            if(shopProduct != null)
                return shopProduct.getSellPrice();
        }

        return -1;
    }

    private boolean isValidItem(ShopProduct product, ItemStack itemStack){
        ItemStack check = product.getBuyItem();
        if (guiShop.generalIgnoreMetaSell) {
            return check.getType() == itemStack.getType();
        } else {
            return check.isSimilar(itemStack);
        }
    }

}
