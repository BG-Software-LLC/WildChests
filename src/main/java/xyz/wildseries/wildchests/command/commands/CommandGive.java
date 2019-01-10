package xyz.wildseries.wildchests.command.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import xyz.wildseries.wildchests.Locale;
import xyz.wildseries.wildchests.WildChestsPlugin;
import xyz.wildseries.wildchests.api.objects.chests.Chest;
import xyz.wildseries.wildchests.api.objects.data.ChestData;
import xyz.wildseries.wildchests.command.ICommand;
import xyz.wildseries.wildchests.utils.ItemUtils;

import java.util.ArrayList;
import java.util.List;

public final class CommandGive implements ICommand {

    @Override
    public String getLabel() {
        return "give";
    }

    @Override
    public String getUsage() {
        return "chests give <player-name> <chest-name> [amount]";
    }

    @Override
    public String getPermission() {
        return "wildchests.give";
    }

    @Override
    public String getDescription() {
        return "Give a custom chest to a player.";
    }

    @Override
    public int getMinArgs() {
        return 3;
    }

    @Override
    public int getMaxArgs() {
        return 4;
    }

    @Override
    public void perform(WildChestsPlugin plugin, CommandSender sender, String[] args) {
        Player target = Bukkit.getPlayer(args[1]);

        if(target == null){
            Locale.INVALID_PLAYER.send(sender, args[1]);
            return;
        }

        ChestData chestData = plugin.getChestsManager().getChestData(args[2]);

        if(chestData == null){
            Locale.INVALID_CHEST.send(sender, args[2]);
            return;
        }

        ItemStack chestItem = chestData.getItemStack();

        if(args.length == 4){
            try{
                chestItem.setAmount(Integer.valueOf(args[3]));
            }catch(IllegalArgumentException ex){
                Locale.INVALID_AMOUNT.send(sender);
                return;
            }
        }

        ItemUtils.addItem(chestItem, target.getInventory(), target.getLocation());
        Locale.CHEST_GIVE_PLAYER.send(sender, chestItem.getAmount(), chestData.getName(), target.getName());
        Locale.CHEST_RECIEVE.send(target, chestItem.getAmount(), chestData.getName(), sender.getName());
    }

    @Override
    public List<String> tabComplete(WildChestsPlugin plugin, CommandSender sender, String[] args) {
        if(!sender.hasPermission(getPermission()))
            return new ArrayList<>();

        if (args.length == 3) {
            List<String> list = new ArrayList<>();
            for(ChestData chestData : plugin.getChestsManager().getAllChestData())
                if(chestData.getName().startsWith(args[2]))
                    list.add(chestData.getName());
            return list;
        }

        if (args.length >= 4) {
            return new ArrayList<>();
        }

        return null;
    }

}
