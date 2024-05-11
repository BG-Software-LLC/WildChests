package com.bgsoftware.wildchests.command.commands;

import com.bgsoftware.wildchests.Locale;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.command.ICommand;
import com.bgsoftware.wildchests.objects.data.WChestData;
import com.bgsoftware.wildchests.utils.ItemUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.Recipe;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public final class CommandInfo implements ICommand {

    @Override
    public String getLabel() {
        return "info";
    }

    @Override
    public String getUsage() {
        return "chests info <chest-name>";
    }

    @Override
    public String getPermission() {
        return "wildchests.info";
    }

    @Override
    public String getDescription() {
        return "Shows information about a chest.";
    }

    @Override
    public int getMinArgs() {
        return 2;
    }

    @Override
    public int getMaxArgs() {
        return 2;
    }

    @Override
    public void perform(WildChestsPlugin plugin, CommandSender sender, String[] args) {
        ChestData chestData = plugin.getChestsManager().getChestData(args[1]);

        if (chestData == null) {
            Locale.INVALID_CHEST.send(sender, args[1]);
            return;
        }

        //Header
        Locale.CHEST_INFO_HEADER.send(sender);

        //Sections which applied to all chests
        Locale.CHEST_INFO_NAME.send(sender, chestData.getName(), ((WChestData) chestData).getItemRaw().getItemMeta().getDisplayName());
        Locale.CHEST_INFO_TYPE.send(sender, chestData.getChestType());
        Locale.CHEST_INFO_SIZE.send(sender, chestData.getDefaultSize());
        Locale.CHEST_INFO_DEFAULT_TITLE.send(sender, chestData.getDefaultTitle());
        Locale.CHEST_INFO_SELL_MODE.send(sender, chestData.isSellMode());

        //Optional sections
        if (chestData.isAutoCrafter())
            Locale.CHEST_INFO_RECIPES.send(sender, getRecipesAsString(chestData.getRecipes()));

        //Footer
        Locale.CHEST_INFO_FOOTER.send(sender);
    }

    @Override
    public List<String> tabComplete(WildChestsPlugin plugin, CommandSender sender, String[] args) {
        if (!sender.hasPermission(getPermission()))
            return Collections.emptyList();

        if (args.length == 2) {
            List<String> list = new LinkedList<>();
            for (ChestData chestData : plugin.getChestsManager().getAllChestData())
                if (chestData.getName().startsWith(args[1]))
                    list.add(chestData.getName());
            return list;
        }

        if (args.length >= 2) {
            return Collections.emptyList();
        }

        return null;
    }

    private String getRecipesAsString(Iterator<Recipe> recipes) {
        StringBuilder string = new StringBuilder();

        while (recipes.hasNext()) {
            Recipe recipe = recipes.next();
            String formattedItem = ItemUtils.getFormattedType(recipe.getResult().getType().name());
            if (!string.toString().contains(formattedItem))
                string.append(", ").append(formattedItem);
        }

        return string.substring(2);
    }

}
