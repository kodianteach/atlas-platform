package co.com.atlas.r2dbc.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicQueryBuilderTest {

    @Test
    @DisplayName("Should build simple SELECT with mandatory WHERE clause")
    void shouldBuildSimpleSelectWithWhere() {
        DynamicQueryBuilder builder = DynamicQueryBuilder.from("invitations")
                .where("organization_id", 1L);

        String sql = builder.buildSelect();
        Map<String, Object> bindings = builder.getBindings();

        assertThat(sql).contains("SELECT * FROM invitations WHERE");
        assertThat(sql).contains("organization_id = :organization_id_1");
        assertThat(bindings).containsValue(1L);
    }

    @Test
    @DisplayName("Should skip optional WHERE when value is null")
    void shouldSkipOptionalWhereWhenNull() {
        DynamicQueryBuilder builder = DynamicQueryBuilder.from("invitations")
                .where("organization_id", 1L)
                .whereOptional("status", null);

        String sql = builder.buildSelect();

        assertThat(sql).doesNotContain("status");
    }

    @Test
    @DisplayName("Should include optional WHERE when value is not null")
    void shouldIncludeOptionalWhereWhenNotNull() {
        DynamicQueryBuilder builder = DynamicQueryBuilder.from("invitations")
                .where("organization_id", 1L)
                .whereOptional("status", "PENDING");

        String sql = builder.buildSelect();
        Map<String, Object> bindings = builder.getBindings();

        assertThat(sql).contains("status = :status_2");
        assertThat(bindings).containsValue("PENDING");
    }

    @Test
    @DisplayName("Should build ILIKE condition for text search")
    void shouldBuildIlikeCondition() {
        DynamicQueryBuilder builder = DynamicQueryBuilder.from("invitations")
                .whereOptionalIlike("email", "test");

        String sql = builder.buildSelect();
        Map<String, Object> bindings = builder.getBindings();

        assertThat(sql).contains("email ILIKE :email_1");
        assertThat(bindings).containsValue("%test%");
    }

    @Test
    @DisplayName("Should skip ILIKE when value is blank")
    void shouldSkipIlikeWhenBlank() {
        DynamicQueryBuilder builder = DynamicQueryBuilder.from("invitations")
                .whereOptionalIlike("email", "  ");

        String sql = builder.buildSelect();

        assertThat(sql).doesNotContain("ILIKE");
    }

    @Test
    @DisplayName("Should build ILIKE prefix condition")
    void shouldBuildIlikePrefixCondition() {
        DynamicQueryBuilder builder = DynamicQueryBuilder.from("units")
                .whereOptionalIlikePrefix("code", "A-1");

        String sql = builder.buildSelect();
        Map<String, Object> bindings = builder.getBindings();

        assertThat(sql).contains("code ILIKE :code_1");
        assertThat(bindings).containsValue("A-1%");
    }

    @Test
    @DisplayName("Should build BETWEEN condition with both bounds")
    void shouldBuildBetweenWithBothBounds() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-12-31T23:59:59Z");

        DynamicQueryBuilder builder = DynamicQueryBuilder.from("invitations")
                .whereOptionalBetween("created_at", from, to);

        String sql = builder.buildSelect();
        Map<String, Object> bindings = builder.getBindings();

        assertThat(sql).contains("created_at >= :created_at_from_1");
        assertThat(sql).contains("created_at <= :created_at_to_2");
        assertThat(bindings).hasSize(2);
    }

    @Test
    @DisplayName("Should build partial BETWEEN with only from")
    void shouldBuildPartialBetweenFrom() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");

        DynamicQueryBuilder builder = DynamicQueryBuilder.from("invitations")
                .whereOptionalBetween("created_at", from, null);

        String sql = builder.buildSelect();

        assertThat(sql).contains("created_at >=");
        assertThat(sql).doesNotContain("created_at <=");
    }

    @Test
    @DisplayName("Should add ORDER BY clause")
    void shouldAddOrderByClause() {
        DynamicQueryBuilder builder = DynamicQueryBuilder.from("invitations")
                .orderBy("created_at", "DESC");

        String sql = builder.buildSelect();

        assertThat(sql).contains("ORDER BY created_at DESC");
    }

    @Test
    @DisplayName("Should add LIMIT clause")
    void shouldAddLimitClause() {
        DynamicQueryBuilder builder = DynamicQueryBuilder.from("units")
                .limit(20);

        String sql = builder.buildSelect();

        assertThat(sql).contains("LIMIT 20");
    }

    @Test
    @DisplayName("Should build complex query with multiple filters")
    void shouldBuildComplexQuery() {
        DynamicQueryBuilder builder = DynamicQueryBuilder.from("invitations")
                .where("organization_id", 1L)
                .whereOptional("type", "OWNER_SELF_REGISTER")
                .whereOptional("status", "PENDING")
                .whereOptionalIlike("email", "user")
                .whereOptionalBetween("created_at", Instant.now(), null)
                .orderBy("created_at", "DESC")
                .limit(50);

        String sql = builder.buildSelect();
        Map<String, Object> bindings = builder.getBindings();

        assertThat(sql).startsWith("SELECT * FROM invitations WHERE");
        assertThat(sql).contains("organization_id =");
        assertThat(sql).contains("type =");
        assertThat(sql).contains("status =");
        assertThat(sql).contains("email ILIKE");
        assertThat(sql).contains("created_at >=");
        assertThat(sql).contains("ORDER BY created_at DESC");
        assertThat(sql).contains("LIMIT 50");
        assertThat(bindings).hasSize(5);
    }

    @Test
    @DisplayName("Should build SELECT with custom columns")
    void shouldBuildSelectWithCustomColumns() {
        DynamicQueryBuilder builder = DynamicQueryBuilder.from("invitations")
                .where("organization_id", 1L);

        String sql = builder.buildSelect("id, type, status");

        assertThat(sql).startsWith("SELECT id, type, status FROM invitations");
    }

    @Test
    @DisplayName("Should return immutable bindings map")
    void shouldReturnImmutableBindings() {
        DynamicQueryBuilder builder = DynamicQueryBuilder.from("invitations")
                .where("organization_id", 1L);

        Map<String, Object> bindings = builder.getBindings();

        assertThat(bindings).isNotNull();
        assertThat(bindings).hasSize(1);
    }
}
