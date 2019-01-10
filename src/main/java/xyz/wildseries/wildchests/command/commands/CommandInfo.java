package xyz.wildseries.wildchests.command.commands;

import org.bukkit.command.CommandSender;
import xyz.wildseries.wildchests.Locale;
import xyz.wildseries.wildchests.WildChestsPlugin;
import xyz.wildseries.wildchests.api.objects.data.ChestData;
import xyz.wildseries.wildchests.command.ICommand;
import xyz.wildseries.wildchests.utils.ItemUtils;

import java.util.ArrayList;
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

        if(chestData == null){
            Locale.INVALID_CHEST.send(sender, args[1]);
            return;
        }

        //Header
        Locale.CHEST_INFO_HEADER.send(sender);

        //Sections which applied to all chests
        Locale.CHEST_INFO_NAME.send(sender, chestData.getName());
        Locale.CHEST_INFO_TYPE.send(sender, chestData.getChestType());
        Locale.CHEST_INFO_SIZE.send(sender, chestData.getDefaultSize());
        Locale.CHEST_INFO_DEFAULT_TITLE.send(sender, chestData.getDefaultTitle());
        Locale.CHEST_INFO_SELL_MODE.send(sender, chestData.isSellMode());

        //Optional sections
        if(chestData.isAutoCrafter())
            Locale.CHEST_INFO_RECIPES.send(sender, getListAsString(chestData.getRecipes(), true));

        //Footer
        Locale.CHEST_INFO_FOOTER.send(sender);
    }

    @Override
    public List<String> tabComplete(WildChestsPlugin plugin, CommandSender sender, String[] args) {
        if(!sender.hasPermission(getPermission()))
            return new ArrayList<>();

        if (args.length == 2) {
            List<String> list = new ArrayList<>();
            for(ChestData chestData : plugin.getChestsManager().getAllChestData())
                if(chestData.getName().startsWith(args[1]))
                    list.add(chestData.getName());
            return list;
        }

        if (args.length >= 2) {
            return new ArrayList<>();
        }

        return null;
    }

    @SuppressWarnings("SameParameterValue")
    private String getListAsString(List<String> list, boolean formatted){
        StringBuilder string = new StringBuilder();

        for(String line : list){
            string.append(", ").append(formatted ? ItemUtils.getFormattedType(line) : line);
        }

        return string.substring(2);
    }

}
