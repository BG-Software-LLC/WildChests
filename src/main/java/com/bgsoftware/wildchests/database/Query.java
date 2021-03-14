package com.bgsoftware.wildchests.database;

public enum Query {

    REGULAR_CHEST_INSERT("REPLACE INTO chests(location, placer, chest_data, inventories) VALUES (?, ?, ?, ?)"),
    REGULAR_CHEST_DELETE("DELETE FROM chests WHERE location = ?"),
    REGULAR_CHEST_UPDATE_INVENTORIES("UPDATE chests SET inventories = ? WHERE location = ?"),

    STORAGE_UNIT_INSERT("REPLACE INTO storage_units(location, placer, chest_data, item, amount, max_amount) VALUES (?, ?, ?, ?, ?, ?)"),
    STORAGE_UNIT_DELETE("DELETE from storage_units WHERE location = ?"),
    STORAGE_UNIT_UPDATE_ITEM("UPDATE storage_units SET item = ?, amount = ? WHERE location = ?"),

    LINKED_CHEST_INSERT("REPLACE INTO linked_chests(location, placer, chest_data, inventories, linked_chest) VALUES (?, ?, ?, ?, ?)"),
    LINKED_CHEST_DELETE("DELETE from linked_chests WHERE location = ?"),
    LINKED_CHEST_UPDATE_INVENTORIES("UPDATE linked_chests SET inventories = ? WHERE location = ?"),
    LINKED_CHEST_UPDATE_LINKED_CHEST("UPDATE linked_chests SET linked_chest = ? WHERE location = ?");

    private final String query;

    Query(String query) {
        this.query = query;
    }

    public String getStatement(){
        return query;
    }

    public StatementHolder getStatementHolder(DatabaseObject databaseObject){
        return new StatementHolder(databaseObject, this);
    }
}
