package com.bgsoftware.wildchests.database;

public enum Query {

    REGULAR_CHEST_INSERT("INSERT INTO chests VALUES(?, ?, ?, ?);"),
    REGULAR_CHEST_UPDATE("UPDATE chests SET placer=?,chest_data=?,inventories=? WHERE location=?;"),
    REGULAR_CHEST_UPDATE_INVENTORY("UPDATE chests SET inventories=? WHERE location=?;"),
    REGULAR_CHEST_DELETE("DELETE FROM chests WHERE location=?;"),
    REGULAR_CHEST_SELECT("SELECT * FROM chests WHERE location=?;"),

    LINKED_CHEST_INSERT("INSERT INTO linked_chests VALUES(?, ?, ?, ?, ?);"),
    LINKED_CHEST_UPDATE("UPDATE linked_chests SET placer=?,chest_data=?,inventories=?,linked_chest=? WHERE location=?;"),
    LINKED_CHEST_UPDATE_INVENTORY("UPDATE linked_chests SET inventories=? WHERE location=?;"),
    LINKED_CHEST_UPDATE_TARGET("UPDATE linked_chests SET linked_chest=? WHERE location=?;"),
    LINKED_CHEST_DELETE("DELETE FROM linked_chests WHERE location=?;"),
    LINKED_CHEST_SELECT("DELETE FROM linked_chests WHERE location=?;"),

    STORAGE_UNIT_INSERT("INSERT INTO storage_units VALUES(?, ?, ?, ?, ?, ?);"),
    STORAGE_UNIT_UPDATE("UPDATE storage_units SET placer=?,chest_data=?,item=?,amount=?,max_amount=? WHERE location=?;"),
    STORAGE_UNIT_UPDATE_INVENTORY("UPDATE storage_units SET item=?,amount=? WHERE location=?;"),
    STORAGE_UNIT_DELETE("DELETE FROM storage_units WHERE location=?;"),
    STORAGE_UNIT_SELECT("SELECT * FROM storage_units WHERE location=?;");

    private String query;

    Query(String query) {
        this.query = query;
    }

    public String getStatement(){
        return query;
    }

    public StatementHolder getStatementHolder(){
        return new StatementHolder(this);
    }
}
