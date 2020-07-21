package com.bgsoftware.wildchests.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;

@SuppressWarnings("WeakerAccess")
public class SQLHelper {

    private static final Object mutex = new Object();
    private static Connection conn;

    private SQLHelper(){}

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void init(File file) throws ClassNotFoundException, SQLException {
        if(!file.exists()){
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }catch(Exception ex){
                ex.printStackTrace();
                return;
            }
        }

        Class.forName("org.sqlite.JDBC");
        String sqlURL = "jdbc:sqlite:" + file.getAbsolutePath().replace("\\", "/");
        conn = DriverManager.getConnection(sqlURL);
    }

    public static Object getMutex(){
        return mutex;
    }

    public static void executeUpdate(String statement){
        try(PreparedStatement preparedStatement = conn.prepareStatement(statement)){
            preparedStatement.executeUpdate();
        }catch(SQLException ex){
            ex.printStackTrace();
        }
    }

    public static boolean doesConditionExist(StatementHolder statementHolder){
        boolean ret = false;

        try(PreparedStatement preparedStatement = statementHolder.getStatement(); ResultSet resultSet = preparedStatement.executeQuery()){
            ret = resultSet.next();
        }catch(SQLException ex){
            ex.printStackTrace();
        }

        return ret;
    }

    public static void executeQuery(String statement, QueryCallback callback){
        executeQuery(statement, callback, null);
    }

    public static void executeQuery(String statement, QueryCallback callback, Consumer<SQLException> onFailure){
        try(PreparedStatement preparedStatement = conn.prepareStatement(statement); ResultSet resultSet = preparedStatement.executeQuery()){
            callback.run(resultSet);
        }catch(SQLException ex){
            if(onFailure == null)
                ex.printStackTrace();
            else
                onFailure.accept(ex);
        }
    }

    public static void setAutoCommit(boolean autoCommit){
        try {
            conn.setAutoCommit(autoCommit);
        }catch(SQLException ex){
            ex.printStackTrace();
        }
    }

    public static void commit(){
        try {
            conn.commit();
        }catch(SQLException ex){
            ex.printStackTrace();
        }
    }

    public static void close(){
        try{
            conn.close();
        }catch(SQLException ex){
            ex.printStackTrace();
        }
    }

    public static PreparedStatement buildStatement(String query) throws SQLException{
        return conn.prepareStatement(query);
    }

    public interface QueryCallback{

        void run(ResultSet resultSet) throws SQLException;

    }

}

