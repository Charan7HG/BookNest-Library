# Library Management System

A full-stack library management application with a Spring Boot REST API and a React web interface. The backend supports JWT authentication and role-based access for administrators, librarians, and members.

## Features

- Login with JWT authentication
- User, book, author, and category management, including search
- Book borrowing endpoint and dashboard
- Staff-assisted password recovery with member-submitted requests and forced password change after login
- Role-based access control

## Technology

- Backend: Java 17, Spring Boot 4.0.6, Spring Security, JWT, Hibernate/JPA, Flyway, Maven, PostgreSQL 18
- Frontend: React 19, JavaScript, Vite 8, Axios, Bootstrap
- Backend tests: JUnit/Spring test with an in-memory H2 database (Flyway is disabled in the default H2 suite)

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

- Java 17 (the backend targets Java 17)
- PostgreSQL 18 (PostgreSQL 14+ is expected to work, but 18 is the tested version)
- Node.js `^20.19.0` or `>=22.12.0` (required by Vite 8) and npm
- A PostgreSQL database named `Library` for local development
- Windows users can use the included Maven wrapper `mvnw.cmd`; other platforms can use `./mvnw` or an installed Maven

To create the local database (run once, outside the application):

```powershell
createdb -h localhost -U your_user Library
```

## Local startup

1. Copy `.env.example` to `.env` in the repository root and replace the placeholder values. Never commit `.env`.
2. Ensure PostgreSQL is running and the `Library` database exists.
3. Run the commands below in two terminals.

Terminal 1 - Start the backend:
```powershell
.\run-local.ps1 backend
```

Terminal 2 - Start the frontend:
```powershell
.\run-local.ps1 frontend
```

The backend starts on `http://localhost:8080` and the frontend on `http://localhost:5173` by default.

### Environment configuration

A root-level `Library/.env` file stores local credentials. It must **never** be committed to Git.

- `.env` stores local credentials (database URL, username, password, JWT secret, CORS origin, API URL) and must never be committed.
- `.env.example` contains safe placeholder values and can be committed to Git.
- The `.gitignore` file protects `.env` and preserves `.env.example` for repository sharing.
- To change PostgreSQL connection details, edit the `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` values in `Library/.env`.
- Generate a JWT secret with `.\gen_secret.ps1` (or `openssl rand -base64 32`) and put the value in `JWT_SECRET`. The temporary file `.jwt-secret-temp.txt` is ignored by Git.
- `PORT` is optional locally (defaults to `8080`) and is used by hosting platforms that inject a port.
- `VITE_API_BASE_URL` is read by the frontend at start time from `Library/.env`.
- In a production build, `VITE_API_BASE_URL` must be set to the public backend URL. If it is omitted, the built frontend calls the same origin, which requires a reverse proxy for the API routes.

### Running the application

1. Ensure PostgreSQL is running with the `Library` database.
2. Run the backend in Terminal 1: `.\run-local.ps1 backend`
3. Run the frontend in Terminal 2: `.\run-local.ps1 frontend`

## PostgreSQL and backend configuration

Flyway owns database schema changes. On startup it reads the versioned SQL files in `library-backend/src/main/resources/db/migration`, applies any migrations that are not recorded in the `flyway_schema_history` table, and then Hibernate `validate` checks that the resulting schema matches the entities. Hibernate never creates or updates tables (`ddl-auto=validate`).

Environment variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `APP_CORS_ALLOWED_ORIGIN`, and `VITE_API_BASE_URL` are read from `Library/.env` at launch time.

Set these environment variables in the shell where you run the backend (or rely on `Library/.env`):

| Variable | Purpose | Example shape |
| --- | --- | --- |
| `DB_URL` | JDBC connection URL | `jdbc:postgresql://localhost:5432/Library` |
| `DB_USERNAME` | PostgreSQL username | Your local database user |
| `DB_PASSWORD` | PostgreSQL password | Your local database password |
| `JWT_SECRET` | Secret used to sign JWTs | A private random value of at least 32 bytes |
| `APP_CORS_ALLOWED_ORIGIN` | Exact frontend origin allowed by CORS | `http://localhost:5173` |
| `PORT` | Optional HTTP port for the backend | `8080` (platforms such as Render or Railway set this automatically) |

`.env.example` documents the variable names and safe placeholder values. Spring Boot does not load `.env` files automatically; provide the values to the process environment. Never commit real credentials.

On macOS/Linux, export the same variables in the shell and run `cd library-backend && ../mvnw spring-boot:run`.

## Database migrations with Flyway

### What Flyway does

Flyway applies ordered SQL migration files to PostgreSQL. It records each applied migration in a table named `flyway_schema_history`, so the same migration is never applied twice. The backend runs Flyway before Hibernate, then Hibernate `validate` checks the schema. This project never uses `ddl-auto=update` or `ddl-auto=create`.

### Where migrations are stored

```text
library-backend/src/main/resources/db/migration/
├── V1__baseline_existing_schema.sql
├── V2__add_book_copies_and_loan_history.sql
├── V3__add_borrow_requests.sql
├── V4__add_password_change_required.sql
└── V5__add_password_reset_requests.sql
```

The `V1` file describes the schema that already existed before Flyway was introduced. It is the baseline, not a new feature migration.

`V2` adds nullable book metadata columns (`description`, `isbn`, `publisher`, `published_year`, `language`, `page_count`), the `book_copy` table, and the `loan` table. It also creates one `LEGACY-<book_id>` copy per existing book and one `ACTIVE` legacy loan per currently borrowed book. Legacy loans keep their dates `NULL` because the old system did not record them. `book.user_id` is intentionally kept for backward compatibility.

`V3` adds the `borrow_request` table used by member requests and staff approval/rejection. Approved requests link to the active loan they created, and completed requests remain as history.

`V4` adds `users.password_change_required` (`boolean NOT NULL DEFAULT false`). Existing users default to `false`, so nobody is forced to change a password by the migration. A staff-assisted reset sets it to `true`, and a successful `POST /change-password` clears it.

`V5` adds the `password_reset_request` table used by member-submitted recovery requests. It records the member, status (`PENDING`, `APPROVED`, `REJECTED`), request time, reviewing staff member, and review time. A partial unique index allows only one `PENDING` request per member while keeping completed requests as history.

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
$env:DB_USERNAME = "your_user"
$env:DB_PASSWORD = "your_password"
$env:JWT_SECRET = "your_private_random_secret"
$env:APP_CORS_ALLOWED_ORIGIN = "http://localhost:5173"
cd library-backend
.\mvnw.cmd spring-boot:run
```

Because the schema is empty, Flyway skips the baseline operation and executes `V1__baseline_existing_schema.sql`. Hibernate then validates the new schema. The first history row will have `type = SQL`.

### Creating a new migration

1. Choose the next version number. If the latest file is `V5__...`, the next is `V6`.
2. Create a file in `library-backend/src/main/resources/db/migration/`.
3. Use the naming pattern `V<number>__<short_description>.sql`, for example:

   ```text
   V6__add_book_reservations.sql
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
$env:VITE_API_BASE_URL = "https://your-backend.example.com"
npm run build
```

The generated static files are written to `library-frontend/dist`. Serve that directory with any static host. Because the app uses client-side routing, configure the host to rewrite unknown paths (for example `/books`) to `index.html`; otherwise refreshing a deep link returns 404. Set `APP_CORS_ALLOWED_ORIGIN` on the backend to the exact public frontend origin. Do not put database credentials or JWT signing secrets in frontend configuration; browser code is public.

## Tests and frontend checks

Run the backend tests from `library-backend`:

```powershell
.\mvnw.cmd test
```

The default tests use H2 and do not require PostgreSQL. Flyway is disabled in the H2 test profile, so these tests do not verify PostgreSQL migrations.

Two PostgreSQL integration tests are disabled unless environment variables point them at disposable databases:

- `LegacyCloneMigrationTests` uses `LIBRARY_PG_CLONE_URL` and expects a clone of the existing `Library` database.
- `PostgresBorrowingIntegrationTests` uses `LIBRARY_PG_IT_URL` and expects an empty disposable database. It truncates its tables between tests.

```powershell
$env:LIBRARY_PG_CLONE_URL = "jdbc:postgresql://localhost:5432/library_clone"
$env:LIBRARY_PG_CLONE_USERNAME = "your_user"
$env:LIBRARY_PG_CLONE_PASSWORD = "your_password"
.\mvnw.cmd -Dtest=LegacyCloneMigrationTests test

$env:LIBRARY_PG_IT_URL = "jdbc:postgresql://localhost:5432/library_fresh_test"
$env:LIBRARY_PG_IT_USERNAME = "your_user"
$env:LIBRARY_PG_IT_PASSWORD = "your_password"
.\mvnw.cmd -Dtest=PostgresBorrowingIntegrationTests test
```

Never point these variables at the real `Library` database: the integration test truncates tables.

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
- Borrowing requests (MEMBER): `POST /borrow-requests`, `GET /borrow-requests/my`
- Borrowing review (ADMIN, LIBRARIAN): `GET /borrow-requests/pending`, `POST /borrow-requests/{id}/approve`, `POST /borrow-requests/{id}/reject`
- Loans (ADMIN, LIBRARIAN): `GET /loans/active`, `POST /loans/{id}/return`
- Password reset requests (public): `POST /password-reset-requests`
- Password reset review (ADMIN, LIBRARIAN): `GET /password-reset-requests/pending`, `GET /password-reset-requests`, `POST /password-reset-requests/{id}/approve`, `POST /password-reset-requests/{id}/reject`
- Change own password (any authenticated role): `POST /change-password`

Use the HTTP methods implemented by each route (GET, POST, PUT, or DELETE). Most routes require a bearer JWT; see the backend security configuration for route and role rules.

### Password recovery

The public email-only reset route (`POST /forgot-password`) has been removed. It allowed anyone who knew an email address to overwrite that account's password, so it is permanently disabled.

Password recovery is in-application and email-free:

1. A member uses **Forgot Password** on the login page and submits their registered email to `POST /password-reset-requests`.
2. The response is always the same generic message, whether the email belongs to a member, to staff, or to nobody. No request is created for non-member or unknown emails.
3. Only one `PENDING` request can exist per member at a time.
4. An ADMIN or LIBRARIAN sees pending requests in the **Password Reset Requests** page and calls `POST /password-reset-requests/{id}/approve` or `.../reject`.
5. Rejection changes nothing. Approval generates a 16-character random temporary password with `SecureRandom`, stores only its BCrypt hash, sets `password_change_required = true`, and returns the plaintext value once to the approving staff member.
6. Staff share the temporary password with the member through an agreed secure channel. The page shows it once and never stores it in the browser's persistent storage.
7. The member logs in and calls `POST /change-password` with `currentPassword` (the temporary one) and `newPassword`.
8. Until that change happens, every protected endpoint except `/change-password` returns `403`. After a successful change the restriction is removed.

Only pending requests can be processed, and the row is locked during approval so two staff members cannot generate two temporary passwords for the same request. Completed requests are immutable and remain as history. There is no direct staff password-reset endpoint; approval of a request is the only path.

## Production deployment requirements

No provider-specific configuration is committed. A conventional deployment needs:

Backend:
- Java 17 runtime
- Build: `cd library-backend && .\mvnw.cmd clean package`
- Start: `java -jar target/LibraryManagement-0.0.1-SNAPSHOT.jar`
- Environment variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `APP_CORS_ALLOWED_ORIGIN` (and `PORT` if the platform provides one)
- A PostgreSQL database reachable from the backend, with a backup taken before the first migration
- The backend does not currently expose an actuator health endpoint; if the platform requires one, add it in a later phase instead of assuming it exists

Frontend:
- Node.js `^20.19.0` or `>=22.12.0`
- Install: `cd library-frontend && npm ci` (or `npm install`)
- Build: `npm run build` with `VITE_API_BASE_URL` set
- Publish directory: `library-frontend/dist`
- SPA rewrite from unknown paths to `index.html`

Database:
- PostgreSQL 18 is the tested version
- Flyway runs automatically at backend startup
- Migrations are forward-only; never edit an applied migration
- Take a `pg_dump` backup before deploying a schema change

## Current limitations

- Flyway currently contains `V1` (baseline), `V2` (book metadata, copies, loans), `V3` (borrow requests), `V4` (`password_change_required`), and `V5` (`password_reset_request`). New schema changes must be added as `V6` or later migrations.
- The default H2 suite does not exercise PostgreSQL migration behavior. The PostgreSQL tests above must be run against disposable databases.
- There is no Docker/Compose configuration; verification uses a local disposable PostgreSQL database.
- No health, metrics, or monitoring endpoint is configured.
- Password recovery is staff-assisted and has no email/OTP delivery. Staff must share the temporary password with the member through an agreed secure channel.
- The temporary password is returned in the approval response body and shown once in the UI. HTTPS is required in production, and response bodies must not be logged by proxies.
- There is no audit log beyond the request row's `reviewed_by` and `reviewed_at`, and no notification when a request is submitted.
- Password policy is unchanged (non-blank only); there is no length or complexity rule for member-chosen passwords.
- Book availability is confirmed by staff at approval time because no copy-availability read endpoint is exposed.
