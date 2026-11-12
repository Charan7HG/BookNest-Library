package com.pentagon.LibraryManagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class FlywayMigrationConfigurationTests {

	@Test
	void flywayUsesSafeBaselineSettingsAndKeepsHibernateValidation() throws IOException {
		Properties properties = new Properties();
		try (InputStream input = Files.newInputStream(Path.of("src/main/resources/application.properties"))) {
			properties.load(input);
		}

		assertEquals("${DB_URL}", properties.getProperty("spring.datasource.url"));
		assertEquals("${DB_USERNAME}", properties.getProperty("spring.datasource.username"));
		assertEquals("${DB_PASSWORD}", properties.getProperty("spring.datasource.password"));

		assertEquals("validate", properties.getProperty("spring.jpa.hibernate.ddl-auto"));
		assertEquals("true", properties.getProperty("spring.flyway.enabled"));
		assertEquals("classpath:db/migration", properties.getProperty("spring.flyway.locations"));
		assertEquals("true", properties.getProperty("spring.flyway.baseline-on-migrate"));
		assertEquals("1", properties.getProperty("spring.flyway.baseline-version"));
		assertEquals("true", properties.getProperty("spring.flyway.validate-on-migrate"));
		assertEquals("true", properties.getProperty("spring.flyway.clean-disabled"));
		assertEquals("false", properties.getProperty("spring.flyway.out-of-order"));
		assertEquals("14", properties.getProperty("app.borrowing.loan-period-days"));
		assertEquals("3", properties.getProperty("app.borrowing.max-active-loans"));
		assertEquals("Asia/Kolkata", properties.getProperty("app.borrowing.timezone"));
	}

	@Test
	void baselineMigrationDescribesTheExistingTablesWithoutDestructiveSql() throws IOException {
		Path migration = Path.of("src/main/resources/db/migration/V1__baseline_existing_schema.sql");
		assertTrue(Files.exists(migration), "Baseline migration must exist");

		String sql = Files.readString(migration).toLowerCase();
		for (String table : List.of("author", "book", "book_category", "category", "profile", "users")) {
			assertTrue(sql.contains("create table public." + table), "Missing table: " + table);
		}

		assertTrue(sql.contains("users_role_check"), "The existing role check constraint must be preserved");
		assertFalse(sql.contains("drop table"));
		assertFalse(sql.contains("truncate"));
		assertFalse(sql.contains("delete from"));
	}

	@Test
	void v2AddsMetadataCopiesAndLoansWithoutDestroyingExistingData() throws IOException {
		Path migration = Path.of("src/main/resources/db/migration/V2__add_book_copies_and_loan_history.sql");
		assertTrue(Files.exists(migration), "V2 migration must exist");

		String sql = Files.readString(migration).toLowerCase();

		// Additive book metadata, all nullable so existing rows are not invented or rewritten.
		for (String column : List.of("description", "isbn", "publisher", "published_year", "language", "page_count")) {
			assertTrue(sql.contains("add column " + column), "Missing book column: " + column);
		}
		assertFalse(sql.contains("add column user_id"));
		assertFalse(sql.contains("drop column"));

		// Copies and loans.
		assertTrue(sql.contains("create table public.book_copy"));
		assertTrue(sql.contains("create table public.loan"));
		for (String status : List.of("available", "on_loan", "lost", "damaged", "withdrawn")) {
			assertTrue(sql.contains("'" + status + "'"), "Missing copy status: " + status);
		}
		assertTrue(sql.contains("'active'"), "Missing ACTIVE loan status");
		assertTrue(sql.contains("'returned'"), "Missing RETURNED loan status");
		assertTrue(sql.contains("is_legacy"), "Missing is_legacy flag");

		// One legacy copy per existing book, using the required code format.
		assertTrue(sql.contains("'legacy-' || b.id"), "Legacy copy code must be LEGACY-<book_id>");
		assertTrue(sql.contains("join public.book_copy c on c.book_id = b.id"));
		assertTrue(sql.contains("where b.user_id is not null"), "Legacy loans must only cover borrowed books");

		// Only one ACTIVE loan per copy, while RETURNED loans remain as history.
		assertTrue(sql.contains("create unique index uq_loan_active_copy"));
		assertTrue(sql.contains("where status = 'active'"));

		assertFalse(sql.contains("drop table"));
		assertFalse(sql.contains("truncate"));
		assertFalse(sql.contains("delete from"));
	}

	@Test
	void v3AddsBorrowRequestsWithPendingOnlyUniqueIndex() throws IOException {
		Path migration = Path.of("src/main/resources/db/migration/V3__add_borrow_requests.sql");
		assertTrue(Files.exists(migration), "V3 migration must exist");

		String sql = Files.readString(migration).toLowerCase();

		assertTrue(sql.contains("create table public.borrow_request"));
		for (String column : List.of("book_id", "member_id", "requested_at", "status",
				"reviewed_by", "reviewed_at", "loan_id")) {
			assertTrue(sql.contains(column), "Missing borrow_request column: " + column);
		}
		for (String status : List.of("pending", "approved", "rejected")) {
			assertTrue(sql.contains("'" + status + "'"), "Missing request status: " + status);
		}
		assertTrue(sql.contains("references public.book(id)"));
		assertTrue(sql.contains("references public.users(id)"));
		assertTrue(sql.contains("references public.loan(id)"));
		assertTrue(sql.contains("create unique index uq_borrow_request_pending_member_book"));
		assertTrue(sql.contains("where status = 'pending'"),
				"The unique index must only cover PENDING requests so history is preserved");

		assertFalse(sql.contains("drop table"));
		assertFalse(sql.contains("truncate"));
		assertFalse(sql.contains("delete from"));
	}
}
