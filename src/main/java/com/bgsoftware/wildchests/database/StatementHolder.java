package com.bgsoftware.wildchests.database;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.utils.Executor;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StatementHolder {

    private static final WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    private static final EnumMap<Query, IncreasableInteger> queryCalls = new EnumMap<>(Query.class);

    private final List<Map<Integer, Object>> batches = new ArrayList<>();

    private final String query;
    private final DatabaseObject databaseObject;
    private final Query queryEnum;
    private final Map<Integer, Object> values = new HashMap<>();
    private int currentIndex = 1;

    private boolean isBatch = false;

    StatementHolder(DatabaseObject databaseObject, Query query){
        this.queryEnum = query;
        this.query = query.getStatement();
        this.databaseObject = databaseObject == null ? DatabaseObject.NULL_DATA : databaseObject;
        this.databaseObject.setModified(query);
    }

    public StatementHolder setString(String value){
        values.put(currentIndex++, value);
        return this;
    }

    public StatementHolder setInt(int value){
        values.put(currentIndex++, value);
        return this;
    }

    public StatementHolder setShort(short value){
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
        values.put(currentIndex++, loc == null ? "" : loc.getWorld().getName() + ", " + loc.getBlockX() + ", " +
                loc.getBlockY() + ", " + loc.getBlockZ());
        return this;
    }

    public StatementHolder setItemStack(ItemStack itemStack){
        values.put(currentIndex++, itemStack == null ? "" : plugin.getNMSAdapter().serialize(itemStack));
        return this;
    }

    public StatementHolder setInventories(Inventory[] inventories){
        values.put(currentIndex++, inventories == null ? "" : plugin.getNMSAdapter().serialize(inventories));
        return this;
    }

    public void addBatch(){
        batches.add(new HashMap<>(values));
        values.clear();
        currentIndex = 1;
    }

    public void prepareBatch(){
        isBatch = true;
    }

    public void execute(boolean async) {
        if(async && !Executor.isDataThread()){
            Executor.data(() -> execute(false));
            return;
        }

        SQLHelper.waitForConnection();

        try {
            StringHolder errorQuery = new StringHolder(query);

            synchronized (SQLHelper.getMutex()) {
                queryCalls.computeIfAbsent(queryEnum, q -> new IncreasableInteger()).increase();
                WildChestsPlugin.debug("Query: " + query);
                SQLHelper.buildStatement(query, preparedStatement -> {
                    if (isBatch) {
                        if (batches.isEmpty()) {
                            isBatch = false;
                            return;
                        }

                        SQLHelper.setAutoCommit(false);

                        for (Map<Integer, Object> values : batches) {
                            for (Map.Entry<Integer, Object> entry : values.entrySet()) {
                                preparedStatement.setObject(entry.getKey(), entry.getValue());
                                errorQuery.value = errorQuery.value.replaceFirst("\\?", entry.getValue() + "");
                            }
                            preparedStatement.addBatch();
                        }

                        preparedStatement.executeBatch();
                        try {
                            SQLHelper.commit();
                        }catch(Throwable ignored){}

                        SQLHelper.setAutoCommit(true);
                    } else {
                        for (Map.Entry<Integer, Object> entry : values.entrySet()) {
                            preparedStatement.setObject(entry.getKey(), entry.getValue());
                            errorQuery.value = errorQuery.value.replaceFirst("\\?", entry.getValue() + "");
                        }
                        preparedStatement.executeUpdate();
                    }

                    databaseObject.setUpdated(queryEnum);
                }, ex -> {
                    WildChestsPlugin.log("&cFailed to execute query " + errorQuery);
                    ex.printStackTrace();

                    databaseObject.setUpdated(queryEnum);
                });
            }
        } finally {
            values.clear();
            databaseObject.setUpdated(queryEnum);
        }
    }

    public static EnumMap<Query, IncreasableInteger> getQueryCalls() {
        return queryCalls;
    }

    private static class StringHolder{

        private String value;

        StringHolder(String value){
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public static final class IncreasableInteger{

        private int value = 0;

        IncreasableInteger(){

        }

        public int get() {
            return value;
        }

        public void increase(){
            value++;
        }

    }

}
