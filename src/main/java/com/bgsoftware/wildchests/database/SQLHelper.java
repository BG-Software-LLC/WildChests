package com.bgsoftware.wildchests.database;

import com.bgsoftware.wildchests.WildChestsPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class SQLHelper {

    private static final CompletableFuture<Void> ready = new CompletableFuture<>();
    private static final Object mutex = new Object();
    private static Connection connection = null;

    private SQLHelper(){

    }

    public static void waitForConnection(){
        try {
            ready.get();
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public static Object getMutex() {
        return mutex;
    }

    public static boolean createConnection(WildChestsPlugin plugin){
        try {
            WildChestsPlugin.log("Trying to connect to SQLite database...");

            File databaseFile = new File(plugin.getDataFolder(), "database.db");
            Class.forName("org.sqlite.JDBC");
            String sqlURL = "jdbc:sqlite:" + databaseFile.getAbsolutePath().replace("\\", "/");
            connection = DriverManager.getConnection(sqlURL);

            WildChestsPlugin.log("Successfully established connection with SQLite database!");

            ready.complete(null);

            return true;
        }catch(Exception ignored){}

        return false;
    }

    public static void executeUpdate(String statement){
        PreparedStatement preparedStatement = null;
        try{
            preparedStatement = connection.prepareStatement(statement);
            preparedStatement.executeUpdate();
        }catch(SQLException ex){
            System.out.println(statement);
            ex.printStackTrace();
        } finally {
            close(preparedStatement);
        }
    }

    public static boolean doesConditionExist(String statement){
        boolean ret = false;

        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try{
            preparedStatement = connection.prepareStatement(statement);
            resultSet = preparedStatement.executeQuery();
            ret = resultSet.next();
        }catch(SQLException ex){
            ex.printStackTrace();
        } finally {
            close(resultSet);
            close(preparedStatement);
        }

        return ret;
    }

    public static void executeQuery(String statement, QueryConsumer<ResultSet> callback){
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try{
            preparedStatement = connection.prepareStatement(statement);
            resultSet = preparedStatement.executeQuery();
            callback.accept(resultSet);
        }catch(SQLException ex){
            ex.printStackTrace();
        } finally {
            close(resultSet);
            close(preparedStatement);
        }
    }

    public static void close(){
        try {
            connection.close();
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public static void buildStatement(String query, QueryConsumer<PreparedStatement> consumer, Consumer<SQLException> failure){
        PreparedStatement preparedStatement = null;
        try{
            preparedStatement = connection.prepareStatement(query);
            consumer.accept(preparedStatement);
        }catch(SQLException ex){
            failure.accept(ex);
        } finally {
          close(preparedStatement);
        }
    }

    private static void close(AutoCloseable closeable){
        if(closeable != null){
            try {
                closeable.close();
            } catch (Exception ignored) {}
        }
    }

    public static void setAutoCommit(boolean autoCommit){
        try {
            connection.setAutoCommit(autoCommit);
        }catch(SQLException ex){
            ex.printStackTrace();
        }
    }

    public static void commit() throws SQLException {
        connection.commit();
    }

    public interface QueryConsumer<T>{

        void accept(T value) throws SQLException;

    }

}

