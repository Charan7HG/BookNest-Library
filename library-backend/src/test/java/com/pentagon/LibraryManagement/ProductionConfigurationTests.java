package com.pentagon.LibraryManagement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class ProductionConfigurationTests {

	@Test
	void productionSettingsRequireSecretsAndAvoidSchemaMutationAndSensitiveSqlLogging() throws IOException {
		Properties properties = new Properties();
		try (InputStream input = Files.newInputStream(Path.of("src/main/resources/application.properties"))) {
			properties.load(input);
		}

		assertEquals("${DB_URL}", properties.getProperty("spring.datasource.url"));
		assertEquals("${DB_USERNAME}", properties.getProperty("spring.datasource.username"));
		assertEquals("${DB_PASSWORD}", properties.getProperty("spring.datasource.password"));
		assertEquals("${JWT_SECRET}", properties.getProperty("jwt.secret"));
		assertEquals("${APP_CORS_ALLOWED_ORIGIN}", properties.getProperty("app.cors.allowed-origin"));
		assertEquals("validate", properties.getProperty("spring.jpa.hibernate.ddl-auto"));
		assertEquals("false", properties.getProperty("spring.jpa.show-sql"));
		assertFalse(properties.containsKey("logging.level.org.hibernate.SQL"));
		assertFalse(properties.containsKey("logging.level.org.hibernate.orm.jdbc.bind"));
	}

	@Test
	void gitignoreExcludesLocalEnvironmentAndCrashLogFiles() throws IOException {
		String gitignore = Files.readString(Path.of(".gitignore"));

		assertTrue(gitignore.lines().anyMatch(line -> line.trim().equals(".env")));
		assertTrue(gitignore.lines().anyMatch(line -> line.trim().equals(".env.*")));
		assertTrue(gitignore.lines().anyMatch(line -> line.trim().equals("hs_err_pid*.log")));
		assertTrue(gitignore.lines().anyMatch(line -> line.trim().equals("replay_pid*.log")));
	}
}
