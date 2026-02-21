package co.com.atlas.r2dbc.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
