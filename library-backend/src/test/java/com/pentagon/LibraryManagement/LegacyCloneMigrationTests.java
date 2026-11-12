package com.pentagon.LibraryManagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Runs migrations against a disposable clone of the existing Library database.
 * This test never writes application data; it only lets Flyway apply V1-V3 and
 * verifies that the pre-existing borrowing records survived unchanged.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "LIBRARY_PG_CLONE_URL", matches = ".+")
class LegacyCloneMigrationTests {

    @DynamicPropertySource
    static void cloneProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("LIBRARY_PG_CLONE_URL"));
        registry.add("spring.datasource.username",
                () -> System.getenv().getOrDefault("LIBRARY_PG_CLONE_USERNAME", "postgres"));
        registry.add("spring.datasource.password",
                () -> System.getenv().getOrDefault("LIBRARY_PG_CLONE_PASSWORD", ""));
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        // src/test/resources/application.properties replaces the main file, so the
        // production baseline settings must be repeated here. This is exactly the
        // path a real pre-Flyway Library database takes: V1 is recorded as the
        // baseline and V2/V3 are applied on top.
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.flyway.baseline-version", () -> "1");
        registry.add("spring.flyway.baseline-description", () -> "Existing Library schema baseline");
        registry.add("spring.flyway.validate-on-migrate", () -> "true");
        registry.add("spring.flyway.out-of-order", () -> "false");
        registry.add("spring.flyway.clean-disabled", () -> "true");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationsKeepLegacyBorrowersCopiesAndLoansIntact() {
        List<String> versions = jdbcTemplate.queryForList(
                "select version from flyway_schema_history where success = true order by installed_rank",
                String.class);
        assertEquals(List.of("1", "2", "3"), versions);

        List<Integer> legacyBorrowedBooks = jdbcTemplate.queryForList(
                "select id from book where user_id = 13 order by id", Integer.class);
        assertEquals(List.of(46), legacyBorrowedBooks);

        Integer legacyLoans = jdbcTemplate.queryForObject(
                "select count(*) from loan where is_legacy = true and status = 'ACTIVE'",
                Integer.class);
        assertEquals(3, legacyLoans);

        Integer loanForBook46 = jdbcTemplate.queryForObject(
                "select count(*) from loan l join book_copy c on c.id = l.copy_id "
                        + "where l.is_legacy = true and l.status = 'ACTIVE' "
                        + "and c.book_id = 46 and l.member_id = 13",
                Integer.class);
        assertEquals(1, loanForBook46);

        Integer availableCopies = jdbcTemplate.queryForObject(
                "select count(*) from book_copy where status = 'AVAILABLE'", Integer.class);
        assertEquals(2, availableCopies);

        Integer onLoanCopies = jdbcTemplate.queryForObject(
                "select count(*) from book_copy where status = 'ON_LOAN'", Integer.class);
        assertEquals(3, onLoanCopies);

        Integer requests = jdbcTemplate.queryForObject(
                "select count(*) from borrow_request", Integer.class);
        assertNotNull(requests);
        assertEquals(0, requests);
    }
}
