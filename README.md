# Library Management System

A full-stack library management application with a Spring Boot REST API and a React web interface. The backend supports JWT authentication and role-based access for administrators, librarians, and members.

## Features

- Login with JWT authentication
- User, book, author, and category management, including search
- Book borrowing endpoint and dashboard
- Role-based access control

## Technology

- Backend: Java 17, Spring Boot, Spring Security, JWT, Hibernate/JPA, Flyway, Maven, PostgreSQL
- Frontend: React, JavaScript, Vite, Axios, Bootstrap
- Backend tests: JUnit/Spring test with an in-memory H2 database (Flyway is disabled in tests)

## Roles

| Role | Access |
| --- | --- |
| ADMIN | User management and administrative operations; can read and manage books, authors, and categories |
| LIBRARIAN | Read and manage books, authors, and categories |
| MEMBER | Read books, authors, and categories; access borrowing and dashboard endpoints |

The `/login` endpoint is public. Other API routes require authentication according to the backend security configuration. Profile endpoints have additional route-specific access rules.

## Project structure

```text
Library/
├── library-backend/       Spring Boot application and tests
│   ├── src/main/java/     API, security, services, repositories, and entities
│   ├── src/main/resources/
│   │   └── db/migration/  Flyway versioned SQL migrations
│   └── src/test/          H2-backed tests
├── library-frontend/      React/Vite application
│   ├── src/
│   └── public/
├── .env.example           Safe template for backend environment variables
├── .gitignore
└── README.md
```

## Prerequisites

- Java 17
- PostgreSQL, with a database and schema matching the JPA entities
- Node.js and npm compatible with the installed Vite version
- Windows users can use the included Maven wrapper `mvnw.cmd`; other platforms can use `./mvnw` or an installed Maven

## Local startup

Terminal 1 - Start the backend:
```powershell
.\run-local.ps1 backend
```

Terminal 2 - Start the frontend:
```powershell
.\run-local.ps1 frontend
```

### Environment configuration

A root-level `Library/.env` file stores local credentials. It must **never** be committed to Git.

- `.env` stores local credentials (database URL, username, password, JWT secret, CORS origin, API URL) and must never be committed.
- `.env.example` contains safe placeholder values and can be committed to Git.
- The `.gitignore` file protects `.env` and preserves `.env.example` for repository sharing.
- To change PostgreSQL connection details, edit the `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` values in `Library/.env`.
- The JWT secret is generated securely using Java's `SecureRandom` (32+ bytes).
- `VITE_API_BASE_URL` is read by the frontend at start time from `Library/.env`.

### Running the application

1. Ensure PostgreSQL is running with the `library` database.
2. Run the backend in Terminal 1: `.\run-local.ps1 backend`
3. Run the frontend in Terminal 2: `.\run-local.ps1 frontend`

## PostgreSQL and backend configuration

Flyway owns database schema changes. On startup it reads the versioned SQL files in `library-backend/src/main/resources/db/migration`, applies any migrations that are not recorded in the `flyway_schema_history` table, and then Hibernate `validate` checks that the resulting schema matches the entities. Hibernate never creates or updates tables (`ddl-auto=validate`).

Environment variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `APP_CORS_ALLOWED_ORIGIN`, and `VITE_API_BASE_URL` are read from `Library/.env` at launch time.

Set these environment variables in the shell where you run the backend (or rely on `Library/.env`):

| Variable | Purpose | Example shape |
| --- | --- | --- |
| `DB_URL` | JDBC connection URL | `jdbc:postgresql://localhost:5432/library` |
| `DB_USERNAME` | PostgreSQL username | Your local database user |
| `DB_PASSWORD` | PostgreSQL password | Your local database password |
| `JWT_SECRET` | Secret used to sign JWTs | A private random value of at least 32 bytes |
| `APP_CORS_ALLOWED_ORIGIN` | Exact frontend origin allowed by CORS | `http://localhost:5173` |

`.env.example` documents the variable names and safe placeholder values. Spring Boot does not load `.env` files automatically; provide the values to the process environment. Never commit real credentials.

On macOS/Linux, export the same variables in the shell and run `cd library-backend && ../mvnw spring-boot:run`.

## Database migrations with Flyway

### What Flyway does

Flyway applies ordered SQL migration files to PostgreSQL. It records each applied migration in a table named `flyway_schema_history`, so the same migration is never applied twice. The backend runs Flyway before Hibernate, then Hibernate `validate` checks the schema. This project never uses `ddl-auto=update` or `ddl-auto=create`.

### Where migrations are stored

```text
library-backend/src/main/resources/db/migration/
├── V1__baseline_existing_schema.sql
└── V2__add_book_copies_and_loan_history.sql
```

The `V1` file describes the schema that already existed before Flyway was introduced. It is the baseline, not a new feature migration.

`V2` adds nullable book metadata columns (`description`, `isbn`, `publisher`, `published_year`, `language`, `page_count`), the `book_copy` table, and the `loan` table. It also creates one `LEGACY-<book_id>` copy per existing book and one `ACTIVE` legacy loan per currently borrowed book. Legacy loans keep their dates `NULL` because the old system did not record them. `book.user_id` is intentionally kept for backward compatibility.

### Existing database: safe baseline

The backend is configured with:

```properties
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=1
spring.flyway.clean-disabled=true
```

When Flyway connects to the existing `Library` database, it sees a non-empty schema with no `flyway_schema_history` table. It creates the history table and records version `1` as a **BASELINE**. It does **not** execute `V1__baseline_existing_schema.sql`, so existing tables and rows are not recreated or changed.

To confirm after the first startup:

```sql
SELECT installed_rank, version, description, type, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

The first row should have `version = 1`, `type = BASELINE`, and `success = true`.

### Fresh database: create from the baseline

Create an empty PostgreSQL database, point `DB_URL` at it, and start the backend:

```powershell
createdb -h localhost -U your_user library_fresh
$env:DB_URL = "jdbc:postgresql://localhost:5432/library_fresh"
cd library-backend
..\mvnw.cmd spring-boot:run
```

Because the schema is empty, Flyway skips the baseline operation and executes `V1__baseline_existing_schema.sql`. Hibernate then validates the new schema. The first history row will have `type = SQL`.

### Creating a new migration

1. Choose the next version number. If the latest file is `V2__...`, the next is `V3`.
2. Create a file in `library-backend/src/main/resources/db/migration/`.
3. Use the naming pattern `V<number>__<short_description>.sql`, for example:

   ```text
   V3__add_book_reservations.sql
   ```

4. Write only the changes for that version. Do not repeat the whole schema.
5. Test the migration on a disposable copy of the database first.
6. Never edit a migration that has already been applied. Add a new migration instead.

Flyway rejects migrations that are out of order because `spring.flyway.out-of-order=false`. Always increase the version number.

### Backups and safety rules

> **Warning:** Always back up the database before applying migrations, especially on a database that contains real users or borrowing records.

Example PostgreSQL backup:

```powershell
pg_dump -h localhost -U your_user -Fc -d Library -f library_backup.dump
```

Safety rules:

- Never run `flyway clean`. It is disabled by `spring.flyway.clean-disabled=true`.
- Never edit `V1` after it has been applied.
- Never point a fresh-database initialization at a database that already contains data.
- Test every new migration against a restored copy before running it on the real database.
- Keep database credentials in environment variables. Do not hard-code them in `application.properties`.

### Tests and Flyway

The H2 test configuration disables Flyway because the baseline SQL is PostgreSQL-specific:

```properties
spring.flyway.enabled=false
```

The unit and integration tests therefore create their own H2 schema with `ddl-auto=create-drop`. PostgreSQL migration behavior must be verified separately with a disposable PostgreSQL database.

## Frontend setup

The frontend uses Vite and connects to the backend API via `VITE_API_BASE_URL`.
If not specified, it defaults to `http://localhost:8080`.

For local development with the default backend port, the `run-local.ps1` script reads `VITE_API_BASE_URL` from `Library/.env`.

To configure a custom backend URL, set the environment variable or define it in a local `.env` file inside `library-frontend/`:

```text
VITE_API_BASE_URL=http://localhost:8080
```

When building for production:

```powershell
cd library-frontend
npm run build
```

Vite prints the local development URL, normally `http://localhost:5173`. Set `APP_CORS_ALLOWED_ORIGIN` to that exact origin for local development. Do not put database credentials or JWT signing secrets in frontend configuration; browser code is public.

## Tests and frontend checks

Run the backend tests from `library-backend`:

```powershell
..\mvnw.cmd clean test
```

The tests use H2 and do not require PostgreSQL. Flyway is disabled in the H2 test profile, so these tests do not verify PostgreSQL migrations. Run the existing-database and fresh-database checks against disposable PostgreSQL databases before deploying a schema change.

Run frontend lint and production build from `library-frontend`:

```powershell
npm run lint
npm run build
```

## API overview

The REST API runs on port `8080` by default. Login is `POST /login`; it returns a JWT used by the frontend for authenticated requests. The main resource routes include:

- Users: `/users`, `/user`, `/user/{id}`, `/user/search`
- Books: `/books`, `/book`, `/book/{id}`, `/book/search`, `/bookbr`
- Authors: `/authors`, `/author`, `/author/{id}`, `/author/search`, `/author/books/{id}`
- Categories: `/categories`, `/category`, `/category/{id}`, `/category/search`, `/category/books/{id}`
- Dashboard: `/dashboard`
- Profiles: `/saveprofile`, `/getall`

Use the HTTP methods implemented by each route (GET, POST, PUT, or DELETE). Most routes require a bearer JWT; see the backend security configuration for route and role rules. `/bookbr` is the borrowing endpoint.

## Current limitations

- Flyway currently contains `V1` (baseline) and `V2` (book metadata, copies, loans). New schema changes must be added as `V3` or later migrations. The new tables are not yet mapped by JPA entities or exposed through API endpoints.
- The H2 regression suite does not exercise PostgreSQL migration behavior. PostgreSQL checks must be run against a disposable database.
- There is no Docker/Compose configuration; verification uses a local disposable PostgreSQL database.
- The project has no Git repository initialized yet.
