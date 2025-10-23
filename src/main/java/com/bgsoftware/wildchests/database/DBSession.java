package com.bgsoftware.wildchests.database;

import com.bgsoftware.common.databasebridge.DatabaseSessionFactory;
import com.bgsoftware.common.databasebridge.logger.ILogger;
import com.bgsoftware.common.databasebridge.session.IDatabaseSession;
import com.bgsoftware.common.databasebridge.sql.query.Column;
import com.bgsoftware.common.databasebridge.sql.query.QueryResult;
import com.bgsoftware.common.databasebridge.sql.session.MariaDBDatabaseSession;
import com.bgsoftware.common.databasebridge.sql.session.MySQLDatabaseSession;
import com.bgsoftware.common.databasebridge.sql.session.SQLDatabaseSession;
import com.bgsoftware.common.databasebridge.sql.session.SQLiteDatabaseSession;
import com.bgsoftware.common.databasebridge.transaction.DatabaseTransactionsExecutor;
import com.bgsoftware.common.databasebridge.transaction.IDatabaseTransaction;
import com.bgsoftware.wildchests.WildChestsPlugin;

import java.io.File;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class DBSession {

    private static final ILogger LOGGER = new ILogger() {
        @Override
        public void error(String message, Throwable error) {
            WildChestsPlugin.log(message);
            error.printStackTrace();
        }

        @Override
        public boolean hasDebugEnabled() {
            return false;
        }

        @Override
        public void debug(String message) {
            // Do nothing
        }

        @Override
        public void info(String message) {
            WildChestsPlugin.log(message);
        }
    };

    private static SQLDatabaseSession<?> globalSession = null;

    private DBSession() {

    }

    public static boolean isReady() {
        return globalSession != null;
    }

    public static boolean createConnection(WildChestsPlugin plugin) {
        SQLDatabaseSession<?> session = createSessionInternal(plugin, true);

        if (session.connect()) {
            globalSession = session;
            return true;
        }

        return false;
    }

    public static CompletableFuture<Void> execute(IDatabaseTransaction transaction) {
        return globalSession.execute(transaction);
    }

    public static CompletableFuture<Void> execute(IDatabaseTransaction... transactions) {
        return globalSession.execute(transactions);
    }

    public static CompletableFuture<Void> execute(Collection<IDatabaseTransaction> transactions) {
        return globalSession.execute(transactions);
    }

    public static void createTable(String tableName, Column... columns) {
        if (isReady())
            globalSession.createTable(tableName, columns, QueryResult.EMPTY_VOID_QUERY_RESULT);
    }

    public static void select(String tableName, String filters, QueryResult<ResultSet> queryResult) {
        if (isReady())
            globalSession.select(tableName, filters, queryResult);
    }

    public static void modifyColumnType(String tableName, String columnName, String newType) {
        if (isReady())
            globalSession.modifyColumnType(tableName, columnName, newType, QueryResult.EMPTY_VOID_QUERY_RESULT);
    }

    public static void close() {
        if (isReady()) {
            DatabaseTransactionsExecutor.stopActiveExecutors();
            globalSession.close();
        }
    }

    private static SQLDatabaseSession<?> createSessionInternal(WildChestsPlugin plugin, boolean logging) {
        IDatabaseSession.Args args;
        switch (plugin.getSettings().databaseType) {
            case "MYSQL":
                args = new MySQLDatabaseSession.Args(plugin.getSettings().databaseMySQLAddress,
                        plugin.getSettings().databaseMySQLPort, plugin.getSettings().databaseMySQLDBName,
                        plugin.getSettings().databaseMySQLUsername, plugin.getSettings().databaseMySQLPassword,
                        plugin.getSettings().databaseMySQLPrefix, plugin.getSettings().databaseMySQLSSL,
                        plugin.getSettings().databaseMySQLPublicKeyRetrieval,
                        plugin.getSettings().databaseMySQLWaitTimeout, plugin.getSettings().databaseMySQLMaxLifetime,
                        "WildChests Database Thread", LOGGER);
                break;
            case "MARIADB":
                args = new MariaDBDatabaseSession.Args(plugin.getSettings().databaseMySQLAddress,
                        plugin.getSettings().databaseMySQLPort, plugin.getSettings().databaseMySQLDBName,
                        plugin.getSettings().databaseMySQLUsername, plugin.getSettings().databaseMySQLPassword,
                        plugin.getSettings().databaseMySQLPrefix, plugin.getSettings().databaseMySQLSSL,
                        plugin.getSettings().databaseMySQLPublicKeyRetrieval,
                        plugin.getSettings().databaseMySQLWaitTimeout, plugin.getSettings().databaseMySQLMaxLifetime,
                        "WildChests Database Thread", LOGGER);
                break;
            default:
                File databaseFile = new File(plugin.getDataFolder(), "database.db");
                args = new SQLiteDatabaseSession.Args(databaseFile,
                        "WildChests Database Thread", LOGGER);
                break;
        }

        SQLDatabaseSession<?> session = (SQLDatabaseSession<?>) DatabaseSessionFactory.createSession(args);
        if (logging)
            session.setLogging(true);
        return session;
    }

}
