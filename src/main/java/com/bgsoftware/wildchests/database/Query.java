package com.bgsoftware.wildchests.database;

public enum Query {

    REGULAR_CHEST_INSERT("REPLACE INTO chests VALUES(?, ?, ?, ?);", 4),
    REGULAR_CHEST_UPDATE("UPDATE chests SET placer=?,chest_data=?,inventories=? WHERE location=?;", 4),
    REGULAR_CHEST_UPDATE_INVENTORY("UPDATE chests SET inventories=? WHERE location=?;", 2),
    REGULAR_CHEST_DELETE("DELETE FROM chests WHERE location=?;", 1),
    REGULAR_CHEST_SELECT("SELECT * FROM chests WHERE location=?;", 1),

    LINKED_CHEST_INSERT("REPLACE INTO linked_chests VALUES(?, ?, ?, ?, ?);", 5),
    LINKED_CHEST_UPDATE("UPDATE linked_chests SET placer=?,chest_data=?,inventories=?,linked_chest=? WHERE location=?;", 5),
    LINKED_CHEST_UPDATE_INVENTORY("UPDATE linked_chests SET inventories=? WHERE location=?;", 2),
    LINKED_CHEST_UPDATE_TARGET("UPDATE linked_chests SET linked_chest=? WHERE location=?;", 2),
    LINKED_CHEST_DELETE("DELETE FROM linked_chests WHERE location=?;", 1),
    LINKED_CHEST_SELECT("SELECT * FROM linked_chests WHERE location=?;", 1),

    STORAGE_UNIT_INSERT("REPLACE INTO storage_units VALUES(?, ?, ?, ?, ?, ?);", 6),
    STORAGE_UNIT_UPDATE("UPDATE storage_units SET placer=?,chest_data=?,item=?,amount=?,max_amount=? WHERE location=?;", 6),
    STORAGE_UNIT_UPDATE_INVENTORY("UPDATE storage_units SET item=?,amount=? WHERE location=?;", 3),
    STORAGE_UNIT_DELETE("DELETE FROM storage_units WHERE location=?;", 1),
    STORAGE_UNIT_SELECT("SELECT * FROM storage_units WHERE location=?;", 1);

    private final String query;
    private final int parametersCount;

    Query(String query, int parametersCount) {
        this.query = query;
        this.parametersCount = parametersCount;
    }

    String getStatement(){
        return query;
    }

    int getParametersCount() {
        return parametersCount;
    }

    public StatementHolder getStatementHolder(DatabaseObject databaseObject){
        return new StatementHolder(databaseObject, this);
    }

}
