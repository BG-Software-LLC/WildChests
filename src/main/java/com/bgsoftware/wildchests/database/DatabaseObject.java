package com.bgsoftware.wildchests.database;

import java.util.EnumMap;

public abstract class DatabaseObject {

    public static final DatabaseObject NULL_DATA = new DatabaseObject() {

        @Override
        public void executeInsertStatement(boolean async) {

        }

    };

    private final EnumMap<Query, Integer> modifiedCalls = new EnumMap<>(Query.class);

    public abstract void executeInsertStatement(boolean async);

    public void setModified(Query query){
        modifiedCalls.put(query, modifiedCalls.getOrDefault(query, 0) + 1);
    }

    public void setFullUpdated(Query query){
        modifiedCalls.remove(query);
    }

    public void setUpdated(Query query){
        int calls = modifiedCalls.getOrDefault(query, 0) - 1;

        if(calls <= 0)
            modifiedCalls.remove(query);
        else
            modifiedCalls.put(query, calls);
    }

    public boolean isModified(){
        return !modifiedCalls.isEmpty();
    }

}
