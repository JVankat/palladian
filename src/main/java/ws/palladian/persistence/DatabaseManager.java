package ws.palladian.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import ws.palladian.helper.collection.CollectionHelper;

/**
 * <p>
 * The {@link DatabaseManager} provides general database specific functionality. This implementation aims on wrapping
 * all ugly SQL specific details like {@link SQLException}s and automatically closes resources for you where applicable.
 * If you need to create your own application specific persistence layer, you may create your own subclass.
 * </p>
 * 
 * <p>
 * Instances of the DatabaseManager or its subclasses are created using the {@link DatabaseManagerFactory}, which takes
 * care of injecting the {@link ConnectionManager}, which provides pooled database connections.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 * @author Klemens Muthmann
 */
public class DatabaseManager {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class);

    /**
     * The manager handling database connections to the underlying database.
     */
    private ConnectionManager connectionManager;

    /**
     * <p>
     * Creates a new {@code DatabaseManager} connected to a database over a {@code DatabaseManager}. The constructor is
     * not exposed since new objects of this type must be constructed using the {@link DatabaseManagerFactory}.
     * </p>
     * 
     * @param connectionManager The manager handling database connections to the underlying database.
     */
    protected DatabaseManager(final ConnectionManager connectionManager) {
        super();
        this.connectionManager = connectionManager;
    }

    /**
     * <p>
     * Get a {@link Connection} from the {@link ConnectionManager}. If you use this method, e.g. in your subclass, it's
     * your responsibility to close all database resources after work has been done. This can be done conveniently by
     * using one of the various close methods offered by this class.
     * </p>
     * 
     * @return
     * @throws SQLException
     */
    protected final Connection getConnection() throws SQLException {
        return connectionManager.getConnection();
    }

    /**
     * <p>
     * Check, whether an item for the specified query exists.
     * </p>
     * 
     * @param sql Query statement which may contain parameter markers.
     * @param args (Optional) arguments for parameter markers in query.
     * @return <code>true</code> if at least on item exists, <code>false</code> otherwise.
     */
    public final boolean entryExists(String sql, List<Object> args) {
        return entryExists(sql, args.toArray());
    }

    /**
     * <p>
     * Check, whether an item for the specified query exists.
     * </p>
     * 
     * @param sql Query statement which may contain parameter markers.
     * @param args (Optional) arguments for parameter markers in query.
     * @return <code>true</code> if at least on item exists, <code>false</code> otherwise.
     */
    public final boolean entryExists(String sql, Object... args) {
        return runSingleQuery(new NopRowConverter(), sql, args) != null;
    }

    /**
     * <p>
     * Run a batch insertion and return the generated insert IDs.
     * </p>
     * 
     * @param sql Update statement which may contain parameter markers.
     * @param provider A callback, which provides the necessary data for the insertion.
     * @return Array with generated IDs for the data provided by the provider. This means, the size of the returned
     *         array reflects the number of batch insertions. If a specific row was not inserted, the array will contain
     *         a 0 value.
     */
    public final int[] runBatchInsertReturnIds(String sql, BatchDataProvider provider) {

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<Integer> generatedIds = new ArrayList<Integer>();

        try {

            connection = getConnection();
            connection.setAutoCommit(false);
            ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            for (int i = 0; i < provider.getCount(); i++) {
                List<Object> args = provider.getData(i);
                fillPreparedStatement(ps, args);
                ps.addBatch();
            }

            int[] batchResult = ps.executeBatch();
            connection.commit();
            connection.setAutoCommit(true);

            // obtain the generated IDs for the inserted items
            // where no item was inserted, return -1 as ID
            rs = ps.getGeneratedKeys();
            for (int result : batchResult) {
                int id = -1;
                if (result > 0 && rs.next()) {
                    id = rs.getInt(1);
                }
                generatedIds.add(id);
            }

        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        } finally {
            close(connection, ps, rs);
        }

        Integer[] array = generatedIds.toArray(new Integer[generatedIds.size()]);
        return CollectionHelper.toIntArray(array);
    }

    /**
     * <p>
     * Run a batch insertion and return the generated insert IDs.
     * </p>
     * 
     * @param sql Update statement which may contain parameter markers.
     * @param batchArgs List of arguments for the batch insertion. Arguments are supplied parameter lists.
     * @return Array with generated IDs for the data provided by the provider. This means, the size of the returned
     *         array reflects the number of batch insertions. If a specific row was not inserted, the array will contain
     *         a 0 value.
     */
    public final int[] runBatchInsertReturnIds(String sql, final List<List<Object>> batchArgs) {

        BatchDataProvider provider = new BatchDataProvider() {

            @Override
            public int getCount() {
                return batchArgs.size();
            }

            @Override
            public List<Object> getData(int number) {
                List<Object> args = batchArgs.get(number);
                return args;
            }
        };

        return runBatchInsertReturnIds(sql, provider);
    }

    public final int[] runBatchUpdate(String sql, BatchDataProvider provider) {

        Connection connection = null;
        PreparedStatement ps = null;
        int[] result = new int[0];

        try {

            connection = getConnection();
            connection.setAutoCommit(false);
            ps = connection.prepareStatement(sql);

            for (int i = 0; i < provider.getCount(); i++) {
                List<Object> args = provider.getData(i);
                fillPreparedStatement(ps, args);
                ps.addBatch();
            }

            result = ps.executeBatch();
            connection.commit();
            connection.setAutoCommit(true);

        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        } finally {
            close(connection, ps);
        }

        return result;
    }

    public final int[] runBatchUpdate(String sql, final List<List<Object>> batchArgs) {

        BatchDataProvider provider = new BatchDataProvider() {

            @Override
            public int getCount() {
                return batchArgs.size();
            }

            @Override
            public List<Object> getData(int number) {
                List<Object> args = batchArgs.get(number);
                return args;
            }
        };

        return runBatchUpdate(sql, provider);
    }

    /**
     * <p>
     * Run a query which only uses exactly one COUNT. The method then returns the value of that count. For example,
     * "SELECT COUNT(*) FROM feeds WHERE id > 342".
     * </p>
     * 
     * @param countQuery The query string with the COUNT.
     * @return The result of the COUNT query. -1 means that there was nothing to count.
     */
    public final int runCountQuery(String countQuery) {

        RowConverter<Integer> converter = new RowConverter<Integer>() {

            @Override
            public Integer convert(ResultSet resultSet) throws SQLException {
                return resultSet.getInt(1);
            }
        };

        int count = -1;
        Integer result = runSingleQuery(converter, countQuery);
        if (result != null) {
            count = result;
        }

        return count;
    }

    /**
     * <p>
     * Run an insert operation and return the generated insert ID.
     * </p>
     * 
     * @param sql Update statement which may contain parameter markers.
     * @param args Arguments for parameter markers in updateStatement, if any.
     * @return The generated ID, or 0 if no id was generated, or -1 if an error occurred.
     */
    public final int runInsertReturnId(String sql, List<Object> args) {
        return runInsertReturnId(sql, args.toArray());
    }

    /**
     * <p>
     * Run an insert operation and return the generated insert ID.
     * </p>
     * 
     * @param sql Update statement which may contain parameter markers.
     * @param args Arguments for parameter markers in updateStatement, if any.
     * @return The generated ID, or 0 if no id was generated, or -1 if an error occurred.
     */
    public final int runInsertReturnId(String sql, Object... args) {

        int generatedId;
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            connection = getConnection();
            ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            fillPreparedStatement(ps, args);
            ps.executeUpdate();

            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                generatedId = rs.getInt(1);
            } else {
                generatedId = 0;
            }

        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
            generatedId = -1;
        } finally {
            close(connection, ps, rs);
        }

        return generatedId;
    }

    public final Object[] runOneResultLineQuery(String query, final int entries, Object... args) {

        final Object[] resultEntries = new Object[entries];

        ResultSetCallback callback = new ResultSetCallback() {

            @Override
            public void processResult(ResultSet resultSet, int number) throws SQLException {
                for (int i = 1; i <= entries; i++) {
                    resultEntries[i - 1] = resultSet.getObject(i);
                }

            }
        };

        runQuery(callback, query, args);

        return resultEntries;
    }

    /**
     * <p>
     * Run a query operation on the database, process the result using a callback.
     * </p>
     * 
     * @param <T> Type of the processed objects.
     * @param callback The callback which is triggered for each result row of the query.
     * @param converter Converter for transforming the {@link ResultSet} to the desired type.
     * @param sql Query statement which may contain parameter markers.
     * @param args (Optional) arguments for parameter markers in query.
     * @return Number of processed results.
     */
    public final <T> int runQuery(ResultCallback<T> callback, RowConverter<T> converter, String sql, Object... args) {

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        int counter = 0;

        try {

            connection = getConnection();
            ps = connection.prepareStatement(sql);
            fillPreparedStatement(ps, args);
            rs = ps.executeQuery();

            while (rs.next() && callback.isLooping()) {
                T item = converter.convert(rs);
                callback.processResult(item, ++counter);
            }

        } catch (SQLException e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            close(connection, ps, rs);
        }

        return counter;
    }

    /**
     * <p>
     * Run a query operation on the database, process the result using a callback.
     * </p>
     * 
     * @param callback The callback which is triggered for each result row of the query.
     * @param sql Query statement which may contain parameter markers.
     * @param args (Optional) arguments for parameter markers in query.
     * @return Number of processed results.
     */
    public final int runQuery(ResultSetCallback callback, String sql, Object... args) {
        return runQuery(callback, new NopRowConverter(), sql, args);
    }

    /**
     * Run a query operation on the database, return the result as List.
     * 
     * @param <T> Type of the processed objects.
     * @param converter Converter for transforming the {@link ResultSet} to the desired type.
     * @param sql Query statement which may contain parameter markers.
     * @param args (Optional) arguments for parameter markers in query.
     * @return List with results.
     */
    public final <T> List<T> runQuery(RowConverter<T> converter, String sql, List<Object> args) {
        return runQuery(converter, sql, args.toArray());
    }

    /**
     * <p>
     * Run a query operation on the database, return the result as List.
     * </p>
     * 
     * @param <T> Type of the processed objects.
     * @param converter Converter for transforming the {@link ResultSet} to the desired type.
     * @param sql Query statement which may contain parameter markers.
     * @param args (Optional) arguments for parameter markers in query.
     * @return List with results.
     */
    public final <T> List<T> runQuery(RowConverter<T> converter, String sql, Object... args) {

        final List<T> result = new ArrayList<T>();

        ResultCallback<T> callback = new ResultCallback<T>() {

            @Override
            public void processResult(T object, int number) {
                result.add(object);
            }

        };

        runQuery(callback, converter, sql, args);
        return result;
    }

    /**
     * <p>
     * Run a query operation on the database, return the result as Iterator. The underlying Iterator implementation does
     * not allow modifications, so invoking {@link ResultIterator#remove()} will cause an
     * {@link UnsupportedOperationException}. Database resources used by the implementation are closed, after the last
     * element has been retrieved. If you break the iteration loop, you <b>must</b> manually call
     * {@link ResultIterator#close()}. In general, you should prefer using
     * {@link #runQuery(ResultCallback, RowConverter, String, Object...)}, or
     * {@link #runQuery(ResultSetCallback, String, Object...)}, which will guarantee closing all database resources.
     * </p>
     * 
     * @param <T> Type of the processed objects.
     * @param converter Converter for transforming the {@link ResultSet} to the desired type.
     * @param sql Query statement which may contain parameter markers.
     * @param args (Optional) arguments for parameter markers in query.
     * @return Iterator for iterating over results.
     */
    public final <T> ResultIterator<T> runQueryWithIterator(RowConverter<T> converter, String sql, List<Object> args) {
        return runQueryWithIterator(converter, sql, args.toArray());
    }

    /**
     * <p>
     * Run a query operation on the database, return the result as Iterator. The underlying Iterator implementation does
     * not allow modifications, so invoking {@link ResultIterator#remove()} will cause an
     * {@link UnsupportedOperationException}. Database resources used by the implementation are closed, after the last
     * element has been retrieved. If you break the iteration loop, you <b>must</b> manually call
     * {@link ResultIterator#close()}. In general, you should prefer using
     * {@link #runQuery(ResultCallback, RowConverter, String, Object...)}, or
     * {@link #runQuery(ResultSetCallback, String, Object...)}, which will guarantee closing all database resources.
     * </p>
     * 
     * @param <T> Type of the processed objects.
     * @param converter Converter for transforming the {@link ResultSet} to the desired type.
     * @param sql Query statement which may contain parameter markers.
     * @param args (Optional) arguments for parameter markers in query.
     * @return Iterator for iterating over results.
     */
    public final <T> ResultIterator<T> runQueryWithIterator(RowConverter<T> converter, String sql, Object... args) {

        @SuppressWarnings("unchecked")
        ResultIterator<T> result = ResultIterator.NULL_ITERATOR;
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet resultSet = null;

        try {

            connection = getConnection();
            ps = connection.prepareStatement(sql);

            // do not buffer the whole ResultSet in memory, but use streaming to save memory
            // http://webmoli.com/2009/02/01/jdbc-performance-tuning-with-optimal-fetch-size/
            // TODO make this a global option?
            // ps.setFetchSize(Integer.MIN_VALUE);
            ps.setFetchSize(1);

            fillPreparedStatement(ps, args);

            resultSet = ps.executeQuery();
            result = new ResultIterator<T>(connection, ps, resultSet, converter);

        } catch (SQLException e) {
            LOGGER.error(e);
            close(connection, ps, resultSet);
        }

        return result;
    }

    /**
     * <p>
     * Run a query operation for a single item in the database.
     * </p>
     * 
     * @param <T> Type of the processed object.
     * @param converter Converter for transforming the {@link ResultSet} to the desired type.
     * @param sql Query statement which may contain parameter markers.
     * @param args (Optional) arguments for parameter markers in query.
     * @return The <i>first</i> retrieved item for the given query, or <code>null</code> no item found.
     */
    public final <T> T runSingleQuery(RowConverter<T> converter, String sql, List<Object> args) {
        return runSingleQuery(converter, sql, args.toArray());
    }

    /**
     * <p>
     * Run a query operation for a single item in the database.
     * </p>
     * 
     * @param <T> Type of the processed object.
     * @param converter Converter for transforming the {@link ResultSet} to the desired type.
     * @param sql Query statement which may contain parameter markers.
     * @param args (Optional) arguments for parameter markers in query.
     * @return The <i>first</i> retrieved item for the given query, or <code>null</code> no item found.
     */
    @SuppressWarnings("unchecked")
    public final <T> T runSingleQuery(RowConverter<T> converter, String sql, Object... args) {

        final Object[] result = new Object[1];

        ResultCallback<T> callback = new ResultCallback<T>() {

            @Override
            public void processResult(T object, int number) {
                result[0] = object;
                breakLoop();
            }
        };

        runQuery(callback, converter, sql, args);
        return (T) result[0];
    }

    /**
     * <p>
     * Run an update operation and return the number of affected rows.
     * </p>
     * 
     * @param sql Update statement which may contain parameter markers.
     * @param args Arguments for parameter markers in updateStatement, if any.
     * @return The number of affected rows, or -1 if an error occurred.
     */
    public final int runUpdate(String sql, List<Object> args) {
        return runUpdate(sql, args.toArray());
    }

    /**
     * <p>
     * Run an update operation and return the number of affected rows.
     * </p>
     * 
     * @param sql Update statement which may contain parameter markers.
     * @param args Arguments for parameter markers in updateStatement, if any.
     * @return The number of affected rows, or -1 if an error occurred.
     */
    public final int runUpdate(String sql, Object... args) {

        int affectedRows;
        Connection connection = null;
        PreparedStatement ps = null;

        try {

            connection = getConnection();
            ps = connection.prepareStatement(sql);
            fillPreparedStatement(ps, args);

            affectedRows = ps.executeUpdate();

        } catch (SQLException e) {
            LOGGER.error("Could not update sql \"" + sql + "\" with args \"" + CollectionHelper.getPrint(args)
                    + "\", error: " + e.getMessage());
            affectedRows = -1;
        } finally {
            close(connection, ps);
        }

        return affectedRows;
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Helper methods
    // //////////////////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Convenience method to close database resources. This method will perform <code>null</code> checking, close
     * resources where applicable and swallow all {@link SQLException}s.
     * </p>
     * 
     * @param connection
     */
    protected static final void close(Connection connection) {
        close(connection, null, null);
    }

    /**
     * <p>
     * Convenience method to close database resources. This method will perform <code>null</code> checking, close
     * resources where applicable and swallow all {@link SQLException}s.
     * </p>
     * 
     * @param connection
     * @param resultSet
     */
    protected static final void close(Connection connection, ResultSet resultSet) {
        close(connection, null, resultSet);
    }

    /**
     * <p>
     * Convenience method to close database resources. This method will perform <code>null</code> checking, close
     * resources where applicable and swallow all {@link SQLException}s.
     * </p>
     * 
     * @param connection
     * @param statement
     */
    protected static final void close(Connection connection, Statement statement) {
        close(connection, statement, null);
    }

    /**
     * <p>
     * Convenience method to close database resources. This method will perform <code>null</code> checking, close
     * resources where applicable and swallow all {@link SQLException}s.
     * </p>
     * 
     * @param connection
     * @param statement
     * @param resultSet
     */
    protected static final void close(Connection connection, Statement statement, ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                LOGGER.error("error closing ResultSet : " + e.getMessage());
            }
        }
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                LOGGER.error("error closing Statement : " + e.getMessage());
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                LOGGER.error("error closing Connection : " + e.getMessage());
            }
        }
    }

    /**
     * <p>
     * Sets {@link PreparedStatement} parameters based on the supplied arguments.
     * </p>
     * 
     * @param ps
     * @param args
     * @throws SQLException
     */
    protected static final void fillPreparedStatement(PreparedStatement ps, List<Object> args) throws SQLException {
        fillPreparedStatement(ps, args.toArray());
    }

    /**
     * <p>
     * Sets {@link PreparedStatement} parameters based on the supplied arguments.
     * </p>
     * 
     * @param ps
     * @param args
     * @throws SQLException
     */
    protected static final void fillPreparedStatement(PreparedStatement ps, Object... args) throws SQLException {

        // do we need a special treatment for NULL values here?
        // if you should stumble across this comment while debugging,
        // the answer is likely: yes, we do!
        for (int i = 0; i < args.length; i++) {
            ps.setObject(i + 1, args[i]);
        }
    }

}