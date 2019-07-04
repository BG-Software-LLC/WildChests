package com.bgsoftware.wildchests.database;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.objects.WLocation;
import com.bgsoftware.wildchests.utils.Executor;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
public class StatementHolder {

    private static WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    private final String query;
    private final Map<Integer, Object> values = new HashMap<>();
    private int currentIndex = 1;

    StatementHolder(Query query){
        this.query = query.getStatement();
    }

    public StatementHolder setString(String value){
        values.put(currentIndex++, value);
        return this;
    }

    public StatementHolder setInt(int value){
        values.put(currentIndex++, value);
        return this;
    }

    public StatementHolder setDouble(double value){
        values.put(currentIndex++, value);
        return this;
    }

    public StatementHolder setBoolean(boolean value){
        values.put(currentIndex++, value);
        return this;
    }

    public StatementHolder setLocation(Location loc){
        return setLocation(loc == null ? null : WLocation.of(loc));
    }

    public StatementHolder setLocation(WLocation loc){
        values.put(currentIndex++, loc == null ? "" : loc.toString());
        return this;
    }

    public StatementHolder setInventories(Inventory[] inventories){
        values.put(currentIndex++, plugin.getNMSAdapter().serialize(inventories));
        return this;
    }

    public StatementHolder setItemStack(ItemStack itemStack){
        values.put(currentIndex++, plugin.getNMSAdapter().serialize(itemStack));
        return this;
    }

    public PreparedStatement getStatement() throws SQLException{
        PreparedStatement preparedStatement = SQLHelper.buildStatement(query);
        for(Map.Entry<Integer, Object> entry : values.entrySet()) {
            preparedStatement.setObject(entry.getKey(), entry.getValue());
        }
        return preparedStatement;
    }

    public void execute(boolean async) {
        if(async){
            Executor.data(() -> execute(false));
            return;
        }

        String errorQuery = query;
        try(PreparedStatement preparedStatement = SQLHelper.buildStatement(query)){
            for(Map.Entry<Integer, Object> entry : values.entrySet()) {
                preparedStatement.setObject(entry.getKey(), entry.getValue());
                errorQuery = errorQuery.replaceFirst("\\?", entry.getValue() + "");
            }

            preparedStatement.executeUpdate();
        }catch(SQLException ex){
            WildChestsPlugin.log("Failed to execute query " + errorQuery);
            ex.printStackTrace();
        }
    }

}
