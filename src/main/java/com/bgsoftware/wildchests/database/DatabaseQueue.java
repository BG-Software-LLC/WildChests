package com.bgsoftware.wildchests.database;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

public final class DatabaseQueue {

    private static final ScheduledExecutorService queueService = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat("WildChests Database Thread").build());
    private static final WildChestsPlugin plugin = WildChestsPlugin.getPlugin();

    private static final ReadWriteLock lock = new ReentrantReadWriteLock();
    private static final long QUEUE_INTERVAL = 15;

    private static final Set<DatabaseObject> queuedObjects = Sets.newConcurrentHashSet();

    public static void queue(DatabaseObject databaseObject){
        try {
            lock.readLock().lock();
            queuedObjects.remove(databaseObject);
            queuedObjects.add(databaseObject);
        } finally {
            lock.readLock().unlock();
        }
    }

    public static void pop(DatabaseObject databaseObject){
        try {
            lock.readLock().lock();
            queuedObjects.remove(databaseObject);
        } finally {
            lock.readLock().unlock();
        }
    }

    static void start() {
        queueService.scheduleAtFixedRate(DatabaseQueue::processQueue, QUEUE_INTERVAL, QUEUE_INTERVAL, TimeUnit.SECONDS);
    }

    static void stop(){
        // Stopping the queue timer, and calling the process queue manually
        queueService.shutdownNow();
        processQueue();
    }

    private static void processQueue(){
        if(queuedObjects.isEmpty())
            return;

        try{
            lock.writeLock().lock();

            Database.startTransaction();

            for(DatabaseObject databaseObject : queuedObjects){
                DatabaseObject.ObjectIdentifier identifier = databaseObject.getIdentifier();

                switch (databaseObject.getObjectState()){
                    case DELETE: {
                        String query = String.format("DELETE FROM %s WHERE %s = ?", identifier.table, identifier.columnName);
                        Database.executeUpdate(query, preparedStatement ->
                                preparedStatement.setObject(1, getObject(identifier.dataFunction)));
                        break;
                    }
                    case INSERT: {
                        StringBuilder columns = new StringBuilder(), parameters = new StringBuilder();

                        for(DatabaseObject.QueryPair queryPair : databaseObject.getQueryPairList()){
                            columns.append(",").append(queryPair.getColumnName());
                            parameters.append(",?");
                        }

                        String query = String.format("REPLACE INTO %s(%s) VALUES(%s)", identifier.table,
                                columns.substring(1), parameters.substring(1));

                        Database.executeUpdate(query, preparedStatement -> {
                            int index = 0;
                            for (DatabaseObject.QueryPair queryPair : databaseObject.getQueryPairList()) {
                                Object data = getObject(queryPair.getDataFunction());
                                preparedStatement.setObject(++index, data);
                            }
                        });

                        databaseObject.processQueue();
                        break;
                    }
                    case UPDATE: {
                        StringBuilder columns = new StringBuilder();

                        for(DatabaseObject.QueryPair queryPair : databaseObject.getQueryPairList()){
                            columns.append(",").append(queryPair.getColumnName()).append("=?");
                        }

                        String query = String.format("UPDATE %s SET %s WHERE %s = ?", identifier.table,
                                columns.substring(1), identifier.columnName);

                        Database.executeUpdate(query, preparedStatement -> {
                            int index = 0;
                            for (DatabaseObject.QueryPair queryPair : databaseObject.getQueryPairList()) {
                                preparedStatement.setObject(++index, getObject(queryPair.getDataFunction()));
                            }

                            preparedStatement.setObject(++index, getObject(identifier.dataFunction));
                        });

                        databaseObject.processQueue();
                        break;
                    }
                }
            }

        } finally {
            Database.commitTransaction();
            queuedObjects.clear();
            lock.writeLock().unlock();
        }
    }

    private static Object getObject(Function<Void, Object> function){
        Object data = function.apply(null);

        if(data == null){
            return "";
        }
        else if(data instanceof ItemStack){
            return plugin.getNMSAdapter().serialize((ItemStack) data);
        }
        else if(data instanceof Location){
            Location loc = (Location) data;
            return loc.getWorld().getName() + ", " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
        }
        else if(data instanceof Inventory[]){
            return plugin.getNMSAdapter().serialize((Inventory[]) data);
        }
        else{
            return data;
        }
    }

}
