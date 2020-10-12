package com.bgsoftware.wildchests.database;

import com.bgsoftware.wildchests.WildChestsPlugin;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class QueryParameters {

    private static final WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    private final Query query;
    private final List<Object> parameters;

    public QueryParameters(Query query){
        this.query = query;
        this.parameters = new ArrayList<>(query.getParametersCount());
    }

    public Query getQuery() {
        return query;
    }

    public void executeQuery(PreparedStatement preparedStatement) throws SQLException {
        for(int i = 0; i < parameters.size(); i++)
            preparedStatement.setObject(i + 1, parameters.get(i));
    }

    public void queue(Object caller){
        DatabaseQueue.queue(caller, this);
    }

    public QueryParameters setItemStack(ItemStack itemStack){
        return setObject(itemStack == null ? "" : plugin.getNMSAdapter().serialize(itemStack));
    }

    public QueryParameters setLocation(Location loc){
        return setObject(loc == null ? "" : loc.getWorld().getName() + ", " + loc.getBlockX() + ", " +
                loc.getBlockY() + ", " + loc.getBlockZ());
    }

    public QueryParameters setInventories(Inventory[] inventories){
        return setObject(inventories == null ? "" : plugin.getNMSAdapter().serialize(inventories));
    }

    public QueryParameters setObject(Object object){
        parameters.add(object);
        return this;
    }

    @Override
    public String toString() {
        return "QueryParameters{" +
                "query=" + query +
                ", parameters=" + parameters +
                '}';
    }

}
