/*
 * Common utilities for Java17+ for the CLI (command line interface).
 * Usage: java -cp ../lib/h2-2.2.224.jar SqlExecutor.java
 *
 * Environment: Java 17+ with JDBC driver com.h2database:h2:2.2.224 are required.
 * Licence: Apache License, Version 2.0, Pavel Ponec, https://github.com/pponec/DirectoryBookmarks
 */
package utils;

import java.io.Closeable;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/** Use SQL statements by the SqlParamBuilder class. */
public final class SqlExecutor {
    private final static ConnectionProvider db = ConnectionProvider.forH2("user", "pwd");
    private final LocalDate someDate = LocalDate.parse("2018-09-12");

    public static void main(final String[] args) throws Exception {
        System.out.println("Arguments: " + List.of(args));
        try (Connection dbConnection = db.connection()) {
            new SqlExecutor().mainStart(dbConnection);
        }
    }

    void mainStart(Connection dbConnection) throws Exception {
        try (SqlParamBuilder builder = new SqlParamBuilder(dbConnection)) {
            // CREATE TABLE:
            builder.sql("""
                            CREATE TABLE employee
                            ( id INTEGER PRIMARY KEY
                            , name VARCHAR(256) DEFAULT 'test'
                            , code VARCHAR(1)
                            , created DATE NOT NULL
                            )""")
                    .execute();

            // SINGLE INSERT:
            builder.sql("""
                            INSERT INTO employee
                            ( id, code, created ) VALUES
                            ( :id, :code, :created )
                            """)
                    .bind("id", 1)
                    .bind("code", "T")
                    .bind("created", someDate)
                    .execute();

            // MULTI INSERT:
            builder.sql("""
                            INSERT INTO employee
                            (id,code,created) VALUES
                            (:id1,:code,:created),
                            (:id2,:code,:created)
                            """)
                    .bind("id1", 2)
                    .bind("id2", 3)
                    .bind("code", "T")
                    .bind("created", someDate.plusDays(1))
                    .execute();
            builder.bind("id1", 11)
                    .bind("id2", 12)
                    .bind("code", "V")
                    .execute();

            // SELECT 1:
            List<Employee> employees = builder.sql("""
                            SELECT t.id, t.name, t.created
                            FROM employee t
                            WHERE t.id < :id
                              AND t.code IN (:code)
                            ORDER BY t.id
                            """)
                    .bind("id", 10)
                    .bind("code", "T", "V")
                    .streamMap(rs -> new Employee(
                            rs.getInt(1),
                            rs.getString(2),
                            rs.getObject(3, LocalDate.class)))
                    .toList();

            assertEquals(3, employees.size());
            assertEquals(1, employees.get(0).id);
            assertEquals("test", employees.get(0).name);
            assertEquals(someDate, employees.get(0).created);

            // REUSE THE SELECT:
            List<Employee> employees2 = builder
                    .bind("id", 100)
                    .streamMap(rs -> new Employee(
                            rs.getInt(1),
                            rs.getString(2),
                            rs.getObject(3, LocalDate.class)))
                    .toList();
            assertEquals(5, employees2.size());
        }
    }

    record Employee (int id, String name, LocalDate created) {}

    record ConnectionProvider(String jdbcClass, String jdbcUrl, String user, String passwd) {

        Connection connection() throws SQLException {
            try {
                Class.forName(jdbcClass);
                return DriverManager.getConnection(jdbcUrl, user, passwd);
            } catch (ClassNotFoundException ex) {
                throw new SQLException("Driver class not found: " + jdbcClass, ex);
            }
        }

        public static ConnectionProvider forH2(String user, String passwd) {
            return new ConnectionProvider("org.h2.Driver",
                    "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
                    user, passwd);
        }
    }

    private <T> void assertEquals(T expected, T result) {
        if (!Objects.equals(expected, result)) {
            throw new IllegalStateException("Objects are not equals: '%s' <> '%s'".formatted(expected, result));
        }
    }

    /**
     * 170 lines long class to simplify working with JDBC.
     * Original source: <a href="https://github.com/pponec/DirectoryBookmarks/blob/development/utils/SqlExecutor.java">GitHub</a>
     * @version 1.0.5
     */
    static class SqlParamBuilder implements Closeable {
        /** SQL parameter mark type of {@code :param} */
        private static final Pattern SQL_MARK = Pattern.compile(":(\\w+)");
        private final Connection dbConnection;
        private final Map<String, Object> params = new HashMap<>();
        private String sqlTemplate = null;
        private PreparedStatement preparedStatement = null;
        private ResultSet resultSet = null;

        public SqlParamBuilder(Connection dbConnection) {
            this.dbConnection = dbConnection;
        }

        /** Close statement (if any) and set a new SQL template */
        public SqlParamBuilder sql(String... sqlTemplates ) {
            close();
            this.params.clear();
            this.sqlTemplate = sqlTemplates.length == 1
                    ? sqlTemplates[0] : String.join("\n", sqlTemplates);
            return this;
        }

        /** Assign a SQL value(s) */
        public SqlParamBuilder bind(String key, Object... value) {
            this.params.put(key, value.length == 1 ? value[0] : List.of(value));
            return this;
        }

        private ResultSet executeSelect() throws IllegalStateException {
            try (AutoCloseable rs = resultSet) {
            } catch (Exception e) {
                throw new IllegalStateException("Closing fails", e);
            }
            try {
                resultSet = prepareStatement().executeQuery();
                return resultSet;
            } catch (Exception ex) {
                throw (ex instanceof RuntimeException re) ? re : new IllegalStateException(ex);
            }
        }

        /** Use a {@link #streamMap(SqlFunction)} rather */
        private Stream<ResultSet> stream() {
            final var resultSet = executeSelect();
            final var iterator = new Iterator<ResultSet>() {
                @Override
                public boolean hasNext() {
                    try {
                        return resultSet.next();
                    } catch (SQLException e) {
                        throw new IllegalStateException(e);
                    }
                }
                @Override
                public ResultSet next() {
                    return resultSet;
                }
            };
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
        }

        /** Iterate executed select */
        public void forEach(SqlConsumer<ResultSet> consumer) throws IllegalStateException, SQLException  {
            stream().forEach(consumer);
        }

        public <R> Stream<R> streamMap(SqlFunction<ResultSet, ? extends R> mapper ) {
            return stream().map(mapper);
        }

        public int execute() throws IllegalStateException, SQLException {
            return prepareStatement().executeUpdate();
        }

        public Connection getConnection() {
            return dbConnection;
        }

        /** The method closes a PreparedStatement object with related objects, not the database connection. */
        @Override
        public void close() {
            try (AutoCloseable c1 = resultSet; PreparedStatement c2 = preparedStatement) {
            } catch (Exception e) {
                throw new IllegalStateException("Closing fails", e);
            } finally {
                resultSet = null;
                preparedStatement = null;
            }
        }

        public PreparedStatement prepareStatement() throws SQLException {
            final var sqlValues = new ArrayList<>();
            final var sql = buildSql(sqlValues, false);
            final var result = preparedStatement != null
                    ? preparedStatement
                    : dbConnection.prepareStatement(sql);
            for (int i = 0, max = sqlValues.size(); i < max; i++) {
                result.setObject(i + 1, sqlValues.get(i));
            }
            this.preparedStatement = result;
            return result;
        }

        protected String buildSql( List<Object> sqlValues, boolean toLog) {
            final var result = new StringBuilder(256);
            final var matcher = SQL_MARK.matcher(sqlTemplate);
            final var missingKeys = new HashSet<>();
            final var singleValue = new Object[1];
            while (matcher.find()) {
                final var key = matcher.group(1);
                final var value = params.get(key);
                if (value != null) {
                    matcher.appendReplacement(result, "");
                    singleValue[0] = value;
                    final Object[] values = value instanceof List ? ((List<?>) value).toArray() : singleValue;
                    for (int i = 0; i < values.length; i++) {
                        if (i > 0) result.append(',');
                        result.append(Matcher.quoteReplacement(toLog ? "[" + values[i] + "]" : "?"));
                        sqlValues.add(values[i]);
                    }
                } else {
                    matcher.appendReplacement(result, Matcher.quoteReplacement(":" + key));
                    missingKeys.add(key);
                }
            }
            if (!toLog && !missingKeys.isEmpty()) {
                throw new IllegalArgumentException("Missing value of the keys: " + missingKeys);
            }
            matcher.appendTail(result);
            return result.toString();
        }

        public String sqlTemplate() {
            return sqlTemplate;
        }

        @Override
        public String toString() {
            return buildSql(new ArrayList<>(), true);
        }

        @FunctionalInterface
        public interface SqlFunction<T, R> extends Function<T, R> {
            default R apply(T resultSet) {
                try {
                    return applyRs(resultSet);
                } catch (Exception ex) {
                    throw (ex instanceof RuntimeException re) ? re : new IllegalStateException(ex);
                }
            }
            R applyRs(T resultSet) throws SQLException;
        }

        @FunctionalInterface
        public interface SqlConsumer<T> extends Consumer<T> {
            @Override
            default void accept(final T t) {
                try {
                    acceptResultSet(t);
                } catch (Exception ex) {
                    throw (ex instanceof RuntimeException re) ? re : new IllegalStateException(ex);
                }
            }
            void acceptResultSet(T t) throws Exception;
        }
    }
}