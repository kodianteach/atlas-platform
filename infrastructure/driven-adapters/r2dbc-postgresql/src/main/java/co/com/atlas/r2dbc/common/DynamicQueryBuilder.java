package co.com.atlas.r2dbc.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for building dynamic SQL queries with optional filters.
 * Supports WHERE clauses with AND conditions, ORDER BY, and LIMIT.
 *
 * <p>Usage example:</p>
 * <pre>
 * DynamicQueryBuilder builder = DynamicQueryBuilder.from("invitations")
 *     .where("organization_id", orgId)
 *     .whereOptional("status", status)
 *     .whereOptional("type", type)
 *     .whereOptionalIlike("email", searchTerm)
 *     .whereOptionalBetween("created_at", dateFrom, dateTo)
 *     .orderBy("created_at", "DESC")
 *     .limit(50);
 *
 * String sql = builder.buildSelect();
 * Map&lt;String, Object&gt; bindings = builder.getBindings();
 * </pre>
 */
public class DynamicQueryBuilder {

    private final String tableName;
    private final List<String> conditions;
    private final Map<String, Object> bindings;
    private String orderByClause;
    private Integer limitValue;
    private Integer offsetValue;
    private int paramCounter;

    private DynamicQueryBuilder(String tableName) {
        this.tableName = tableName;
        this.conditions = new ArrayList<>();
        this.bindings = new HashMap<>();
        this.paramCounter = 0;
    }

    /**
     * Creates a new DynamicQueryBuilder for the specified table.
     *
     * @param tableName the database table name
     * @return new builder instance
     */
    public static DynamicQueryBuilder from(String tableName) {
        return new DynamicQueryBuilder(tableName);
    }

    /**
     * Adds a mandatory WHERE condition.
     *
     * @param column the column name
     * @param value  the value to filter by
     * @return this builder for chaining
     */
    public DynamicQueryBuilder where(String column, Object value) {
        String paramName = generateParamName(column);
        conditions.add(column + " = :" + paramName);
        bindings.put(paramName, value);
        return this;
    }

    /**
     * Adds an optional WHERE condition. Skipped if value is null.
     *
     * @param column the column name
     * @param value  the value to filter by, or null to skip
     * @return this builder for chaining
     */
    public DynamicQueryBuilder whereOptional(String column, Object value) {
        if (value != null) {
            where(column, value);
        }
        return this;
    }

    /**
     * Adds an optional ILIKE condition for text search. Skipped if value is null or blank.
     *
     * @param column the column name
     * @param value  the search term, or null to skip
     * @return this builder for chaining
     */
    public DynamicQueryBuilder whereOptionalIlike(String column, String value) {
        if (value != null && !value.isBlank()) {
            String paramName = generateParamName(column);
            conditions.add(column + " ILIKE :" + paramName);
            bindings.put(paramName, "%" + value + "%");
        }
        return this;
    }

    /**
     * Adds an optional ILIKE prefix condition. Skipped if value is null or blank.
     *
     * @param column the column name
     * @param prefix the prefix to search for
     * @return this builder for chaining
     */
    public DynamicQueryBuilder whereOptionalIlikePrefix(String column, String prefix) {
        if (prefix != null && !prefix.isBlank()) {
            String paramName = generateParamName(column);
            conditions.add(column + " ILIKE :" + paramName);
            bindings.put(paramName, prefix + "%");
        }
        return this;
    }

    /**
     * Adds an optional BETWEEN condition for date/time ranges.
     * Supports partial ranges (only from, only to, or both).
     *
     * @param column the column name
     * @param from   the start value (inclusive), or null
     * @param to     the end value (inclusive), or null
     * @return this builder for chaining
     */
    public DynamicQueryBuilder whereOptionalBetween(String column, Object from, Object to) {
        if (from != null) {
            String paramName = generateParamName(column + "_from");
            conditions.add(column + " >= :" + paramName);
            bindings.put(paramName, from);
        }
        if (to != null) {
            String paramName = generateParamName(column + "_to");
            conditions.add(column + " <= :" + paramName);
            bindings.put(paramName, to);
        }
        return this;
    }

    /**
     * Sets the ORDER BY clause.
     *
     * @param column    the column to order by
     * @param direction ASC or DESC
     * @return this builder for chaining
     */
    public DynamicQueryBuilder orderBy(String column, String direction) {
        this.orderByClause = column + " " + direction;
        return this;
    }

    /**
     * Sets a LIMIT on the number of results.
     *
     * @param limit maximum number of rows
     * @return this builder for chaining
     */
    public DynamicQueryBuilder limit(int limit) {
        this.limitValue = limit;
        return this;
    }

    /**
     * Sets an OFFSET for pagination.
     *
     * @param offset number of rows to skip
     * @return this builder for chaining
     */
    public DynamicQueryBuilder offset(int offset) {
        this.offsetValue = offset;
        return this;
    }

    /**
     * Adds an optional IN condition for filtering by multiple values.
     * Skipped if values is null or empty.
     *
     * @param column the column name
     * @param values the collection of values to match
     * @return this builder for chaining
     */
    public DynamicQueryBuilder whereOptionalIn(String column, Collection<?> values) {
        if (values != null && !values.isEmpty()) {
            List<String> paramNames = new ArrayList<>();
            int index = 0;
            for (Object value : values) {
                String paramName = generateParamName(column + "_in_" + index);
                paramNames.add(":" + paramName);
                bindings.put(paramName, value);
                index++;
            }
            conditions.add(column + " IN (" + String.join(", ", paramNames) + ")");
        }
        return this;
    }

    /**
     * Adds an optional ILIKE condition searching multiple columns.
     * Skipped if value is null or blank.
     *
     * @param columns the columns to search in
     * @param value   the search term
     * @return this builder for chaining
     */
    public DynamicQueryBuilder whereOptionalIlikeMultiple(List<String> columns, String value) {
        if (value != null && !value.isBlank() && !columns.isEmpty()) {
            List<String> ilikeConditions = new ArrayList<>();
            for (String column : columns) {
                String paramName = generateParamName(column);
                ilikeConditions.add(column + " ILIKE :" + paramName);
                bindings.put(paramName, "%" + value + "%");
            }
            conditions.add("(" + String.join(" OR ", ilikeConditions) + ")");
        }
        return this;
    }

    /**
     * Builds a SELECT * query with all configured conditions.
     *
     * @return the complete SQL query string
     */
    public String buildSelect() {
        return buildSelect("*");
    }

    /**
     * Builds a SELECT query with specified columns and all configured conditions.
     *
     * @param columns the columns to select (e.g., "id, name, status")
     * @return the complete SQL query string
     */
    public String buildSelect(String columns) {
        StringBuilder sql = new StringBuilder("SELECT ")
                .append(columns)
                .append(" FROM ")
                .append(tableName);

        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        if (orderByClause != null) {
            sql.append(" ORDER BY ").append(orderByClause);
        }

        if (limitValue != null) {
            sql.append(" LIMIT ").append(limitValue);
        }

        if (offsetValue != null) {
            sql.append(" OFFSET ").append(offsetValue);
        }

        return sql.toString();
    }

    /**
     * Builds a SELECT COUNT(*) query with all configured WHERE conditions.
     * Ignores ORDER BY, LIMIT, and OFFSET.
     *
     * @return the count SQL query string
     */
    public String buildCount() {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ").append(tableName);

        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }

        return sql.toString();
    }

    /**
     * Returns the parameter bindings for use with DatabaseClient.
     *
     * @return map of parameter name to value
     */
    public Map<String, Object> getBindings() {
        return Map.copyOf(bindings);
    }

    private String generateParamName(String column) {
        paramCounter++;
        return column.replace(".", "_") + "_" + paramCounter;
    }
}
