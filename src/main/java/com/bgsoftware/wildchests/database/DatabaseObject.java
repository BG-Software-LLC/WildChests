package com.bgsoftware.wildchests.database;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

public abstract class DatabaseObject {

    private final QueryPairList queryPairList = new QueryPairList();
    private final ObjectIdentifier identifier;
    protected ObjectState objectState = ObjectState.UPDATE;

    protected DatabaseObject(ObjectIdentifier identifier){
        this.identifier = identifier;
    }

    protected void saveData(String columnName, Function<Void, Object> dataFunction) {
        if(objectState == ObjectState.UPDATE && queryPairList.add(new QueryPair(columnName, dataFunction)))
            DatabaseQueue.queue(this);
    }

    protected void deleteObject() {
        queryPairList.clear();

        if(objectState == ObjectState.UPDATE) {
            DatabaseQueue.queue(this);
        }
        else{
            DatabaseQueue.pop(this);
        }

        objectState = ObjectState.DELETE;
    }

    QueryPairList getQueryPairList() {
        return queryPairList;
    }

    ObjectIdentifier getIdentifier() {
        return identifier;
    }

    ObjectState getObjectState() {
        return objectState;
    }

    void processQueue(){
        queryPairList.clear();
        objectState = ObjectState.UPDATE;
    }

    public abstract void insertObject();

    public abstract void saveObject();

    protected enum ObjectState {

        INSERT,
        DELETE,
        UPDATE

    }

    protected static final class ObjectIdentifier {

        final String columnName, table;
        final Function<Void, Object> dataFunction;

        public ObjectIdentifier(String table, String columnName, Function<Void, Object> dataFunction){
            this.table = table;
            this.columnName = columnName;
            this.dataFunction = dataFunction;
        }

    }

    static final class QueryPair {

        private final String columnName;
        private final Function<Void, Object> dataFunction;

        QueryPair(String columnName, Function<Void, Object> dataFunction){
            this.columnName = columnName;
            this.dataFunction = dataFunction;
        }

        public String getColumnName() {
            return columnName;
        }

        public Function<Void, Object> getDataFunction() {
            return dataFunction;
        }

    }

    static final class QueryPairList implements Iterable<QueryPair> {

        private final ReadWriteLock lock = new ReentrantReadWriteLock();

        private final List<QueryPair> queryPairs = new ArrayList<>();
        private final Set<String> affectedColumns = Sets.newConcurrentHashSet();

        public QueryPair get(int index) {
            try {
                lock.readLock().lock();
                return queryPairs.get(index);
            } finally {
                lock.readLock().unlock();
            }
        }

        public boolean add(QueryPair element) {
            try {
                lock.writeLock().lock();
                if(affectedColumns.add(element.columnName)){
                    queryPairs.add(element);
                    return true;
                }

                return false;
            } finally {
                lock.writeLock().unlock();
            }
        }

        public void clear() {
            try {
                lock.writeLock().lock();
                affectedColumns.clear();
                queryPairs.clear();
            } finally {
                lock.writeLock().unlock();
            }
        }

        @NotNull
        @Override
        public Iterator<QueryPair> iterator() {
            try {
                lock.readLock().lock();
                return queryPairs.iterator();
            } finally {
                lock.readLock().unlock();
            }
        }

        public int size(){
            try {
                lock.readLock().lock();
                return queryPairs.size();
            } finally {
                lock.readLock().unlock();
            }
        }

    }

}
