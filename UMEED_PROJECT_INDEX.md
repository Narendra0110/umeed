# UMEED – PROJECT INDEX

Version: 2.0

Last updated: 2026-11-12

Project status: FIRST API ENDPOINT WORKING (GET /api/v1/categories) – NEXT IS THE OTP PROVIDER DECISION, THEN AUTHENTICATION

Project folder: H:\Project\Umeed

---

# 1. PROJECT OVERVIEW

Project: Umeed

Description:

A free community donation platform connecting people who want to donate items with people who need them.

Target country: India

Target platforms:

* Android
* iOS
* Admin web application

Business model:

Completely free.

No buying, selling, payments, listing charges or paid claims.

---

# 2. CONFIRMED PRODUCT REQUIREMENTS

## Donation workflow

* Users can list items they want to donate.
* Other users can browse available items.
* Users can request items.
* Donors select receivers from incoming requests.
* Receivers collect items from donors.
* Both parties confirm completion.

## Need request workflow

* Users can post things they need.
* Donors can discover need requests.
* Donors can offer available items.
* Receivers choose an appropriate offer.

## Location

* Launch across India.
* Nearby discovery.
* City-based discovery.
* Distance filtering.
* Approximate public location.
* Private pickup address.

## Safety

* Private messaging.
* Reporting.
* Blocking.
* Moderation.
* Account suspension.
* Pickup address privacy.

---

# 2A. MVP SCOPE

Decided: 2026-11-11

The first release is an MVP. Build only the MVP items until the MVP is working end to end.

## In the MVP

* Phone OTP login and basic profile.
* Post a donation with photos and category.
* Swipe feed of nearby items with a Claim button (see section 2B).
* Donor sees incoming claims and picks a receiver.
* Pickup address revealed only to the chosen receiver.
* Donor and receiver phone numbers shown to each other once a claim is accepted (replaces chat for arranging pickup in the MVP).
* Both sides confirm completion.
* Report a listing.

## Deferred until after the MVP

* Need requests and offers.
* In-app chat.
* Map view and search.
* Admin web dashboard.
* Hindi language.
* Push notifications.
* Remembering skipped ("not interested") items.

## MVP categories

Decided: 2026-11-12

| Slug             | Name                 | Sort order |
| ---------------- | -------------------- | ---------- |
| clothes          | Clothes              | 10         |
| books-stationery | Books and stationery | 20         |
| furniture        | Furniture            | 30         |
| electronics      | Electronics          | 40         |
| kitchen-home     | Kitchen and home     | 50         |
| kids-toys        | Kids and toys        | 60         |
| other            | Other                | 999        |

* Food and medicines are intentionally excluded from the MVP.
* Slugs are permanent codes used by the mobile app. Display names can change through a new migration. Slugs should not.

## API response shape

Decided: 2026-11-12

* Successful responses are plain JSON without a wrapper object. Example: GET /api/v1/categories returns a JSON list of objects with id, slug and name.
* A consistent error response format is still to be designed (global exception handling).

Notes:

* Without the admin dashboard, listing reports are stored by the backend and reviewed directly in the database during the MVP.
* The master instructions describe the full long-term product. This section decides what is built first.
* Moving an item between the two lists requires explicit discussion and approval.

---

# 2B. HOME FEED DESIGN

Decided: 2026-11-11

* The Home tab is an Instagram-style feed.
* Each donated item is shown as one post: photos, title, condition, approximate area and donor name.
* A Claim button sits below the post.
* Swiping up means "not interested" and moves to the next item.
* Nearby items are shown first.
* In the MVP, swiping up only shows the next item. Skipped items are not remembered and can appear again.

---

# 3. FINAL TECHNOLOGY STACK

## Mobile

* React Native
* Expo
* TypeScript
* Expo Router
* TanStack Query
* Zustand
* React Hook Form
* Zod

## Backend

* Java 21
* Spring Boot 4.x compatible stable release
* Maven
* Spring Web
* Spring Security
* Spring Data JPA
* Hibernate
* Jakarta Bean Validation
* Spring Boot Actuator

## Database

* PostgreSQL
* PostGIS
* Flyway

## Admin

* Next.js
* React
* TypeScript

## Infrastructure

Initial:

* Local PostgreSQL.
* Local Spring Boot application.
* Local development tools.

Future, when required:

* Redis.
* RabbitMQ.
* Docker.
* Object storage.
* Firebase Cloud Messaging.
* Production monitoring.

## IDE

* IntelliJ IDEA.

## Version control

* Git.
* GitHub.

---

# 4. ARCHITECTURE DECISIONS

| Area                 | Decision                                |
| -------------------- | --------------------------------------- |
| Backend architecture | Modular monolith                        |
| Future architecture  | Microservices extraction when justified |
| Backend language     | Java 21                                 |
| Backend framework    | Spring Boot                             |
| Mobile               | React Native                            |
| Admin                | Next.js                                 |
| Database             | PostgreSQL                              |
| Location             | PostGIS                                 |
| ORM                  | Hibernate / Spring Data JPA             |
| Migrations           | Flyway                                  |
| Security             | Spring Security                         |
| API                  | REST                                    |
| API version          | /api/v1                                 |
| Build tool           | Maven                                   |
| Development IDE      | IntelliJ IDEA                           |

IMPORTANT:

The project is NOT using NestJS.

The project is NOT using Prisma.

Do not reintroduce the previous Node.js backend architecture.

---

# 5. TARGET REPOSITORY STRUCTURE

Target structure:

Umeed/
|
|-- UMEED_MASTER_INSTRUCTIONS.md
|-- UMEED_PROJECT_INDEX.md
|
|-- apps/
|   |-- mobile/
|   |-- admin/
|
|-- backend/
|   |-- pom.xml
|   |-- src/
|
|-- docs/
|   |-- architecture/
|   |-- api/
|
|-- .gitignore
|
|-- README.md

Backend target package structure:

com.umeed
|
|-- common/
|-- auth/
|-- users/
|-- categories/
|-- donations/
|-- needs/
|-- claims/
|-- offers/
|-- pickup/
|-- chat/
|-- notifications/
|-- reports/
|-- moderation/
|-- admin/

IMPORTANT:

This is the target structure only.

Do not assume any files or folders have been created.

Update the index after actual verification.

---

# 6. DEVELOPMENT PHASES

## Phase 0 – Environment setup

Scope: MVP

Status: IN PROGRESS

Verified from command output on 2026-11-11:

* Node.js v26.13.1.
* npm 11.20.0.
* Git 2.55.0.windows.3.
* Java 21.0.11 LTS.

Reported installed by the project owner, not verified by AI:

* IntelliJ IDEA.
* Android Studio.
* PostgreSQL.
* Git user name and email configuration.

Done:

* Git repository initialized on branch main (2026-11-11).
* Maven is provided by the Maven Wrapper in backend/ (mvnw.cmd). No separate Maven installation is needed.

Not done yet:

* GitHub private repository named umeed. The local repository has no remote yet.
* Expo configuration.
* Adding the PostgreSQL bin folder to PATH (optional). Until then psql is run by its full path: "C:\Program Files\PostgreSQL\18\bin\psql.exe".

Local database, verified from command output on 2026-11-12:

* PostgreSQL 18.6, Windows service postgresql-x64-18.
* Development database: umeed_dev.
* Application login: umeed_app (owner of umeed_dev). The backend never connects as postgres.
* The umeed_app password is read from the environment variable UMEED_DB_PASSWORD. It is not stored in any file. It must be set in each new command window before running the backend: set UMEED_DB_PASSWORD=... then cls.
* PostGIS has not been installed or checked yet. It is needed when nearby discovery is built.

Tasks:

* Install Git.
* Install IntelliJ IDEA.
* Install Java 21.
* Install Maven or use Maven Wrapper.
* Install Node.js.
* Install Android Studio.
* Configure Expo.
* Prepare PostgreSQL development environment.
* Create GitHub repository.

## Phase 1 – Repository foundation

Scope: MVP (admin project is post-MVP)

Status: IN PROGRESS

Done:

* Repository initialized.
* Backend project prepared in backend/.
* Shared documentation established (master instructions and this index).

Not done yet:

* Mobile project.
* Admin project (post-MVP).

Tasks:

* Initialize repository.
* Prepare backend project.
* Prepare mobile project.
* Prepare admin project.
* Configure development environments.
* Establish shared documentation.

## Phase 2 – Backend foundation

Scope: MVP

Status: IN PROGRESS

Done:

* Spring Boot setup (Spring Boot 4.1.2, Java 21, package com.umeed).
* Maven configuration (backend/pom.xml with Spring Web, Actuator and Validation).
* Health endpoint: provided by Spring Boot Actuator at /actuator/health. No custom code file was needed.

* PostgreSQL connection (application.properties with environment variable overrides, Spring Data JPA, PostgreSQL driver).
* Hibernate is set to validate only (spring.jpa.hibernate.ddl-auto=validate). Flyway owns the schema.
* Flyway added. Migrations live in backend/src/main/resources/db/migration.
* Migrations V1 (create categories) and V2 (insert MVP categories) applied and verified in umeed_dev.
* Success response shape decided: plain JSON, no wrapper (section 2A).

Not done yet:

* Global exception handling with a consistent error response format.

Tasks:

* Spring Boot setup.
* Maven configuration.
* Application configuration.
* PostgreSQL connection.
* Flyway.
* Health endpoint.
* Common response structure.
* Global exception handling.

## Phase 3 – Authentication and users

Scope: MVP

Status: NOT STARTED

Tasks:

* Authentication.
* JWT.
* Spring Security.
* User registration.
* User profiles.
* Account status.
* User management.

## Phase 4 – Donations

Scope: MVP

Status: NOT STARTED

Note: the categories module, which donations depend on, is already complete (files 009 and 011 to 016).

Tasks:

* Donation entity.
* Donation migrations.
* Create listing.
* Edit listing.
* Delete listing.
* Image uploads.
* Browse donations.
* Location discovery.

## Phase 5 – Need requests

Scope: POST-MVP

Status: NOT STARTED

Tasks:

* Need request entity.
* Create need request.
* Browse requests.
* Update requests.
* Close requests.
* Donor offers.

## Phase 6 – Claims and offers

Scope: Claims are MVP. Offers are POST-MVP.

Status: NOT STARTED

Tasks:

* Submit claim.
* Donor selection.
* Claim rejection.
* Claim acceptance.
* Offer creation.
* Offer acceptance.
* Concurrency protection.

## Phase 7 – Pickup and communication

Scope: Pickup, address protection, phone number reveal after claim acceptance and listing reports are MVP. Chat and notifications are POST-MVP.

Status: NOT STARTED

Tasks:

* Pickup scheduling.
* Address protection.
* Pickup confirmation.
* Chat.
* Notifications.

## Phase 8 – Mobile development

Scope: MVP screens only: authentication, Home swipe feed, donation screens, claim screens, pickup, profile. Explore, need, offer and chat screens are POST-MVP.

Status: NOT STARTED

Tasks:

* Expo foundation.
* Navigation.
* Authentication screens.
* Home.
* Explore.
* Donation screens.
* Need screens.
* Claim screens.
* Offer screens.
* Pickup.
* Chat.
* Profile.

## Phase 9 – Admin dashboard

Scope: POST-MVP

Status: NOT STARTED

Tasks:

* Admin authentication.
* Dashboard.
* Users.
* Donations.
* Need requests.
* Claims.
* Reports.
* Moderation.
* Analytics.

## Phase 10 – Testing and launch

Scope: MVP

Status: NOT STARTED

Tasks:

* Unit testing.
* Integration testing.
* Security testing.
* Performance testing.
* Deployment.
* Android publishing.
* iOS publishing.

---

# 7. FILE-BY-FILE PROGRESS TRACKER

No hand-written application code files have been created yet. The entries below are repository foundation files and the generated backend project.

| Number | File                         | Status    |
| ------ | ---------------------------- | --------- |
| 001    | .gitignore                   | COMPLETED |
| 002    | UMEED_MASTER_INSTRUCTIONS.md | COMPLETED |
| 003    | UMEED_PROJECT_INDEX.md       | COMPLETED |
| 004    | Spring Boot initialization (approved exception) | COMPLETED |
| 005    | UMEED_PROJECT_INDEX.md (progress update) | COMPLETED |
| 006    | backend/src/main/resources/application.properties (database settings) | COMPLETED |
| 007    | backend/pom.xml (Spring Data JPA and PostgreSQL driver) | COMPLETED |
| 008    | backend/pom.xml (Flyway) | COMPLETED |
| 009    | backend/src/main/resources/db/migration/V1__create_categories.sql | COMPLETED |
| 010    | UMEED_PROJECT_INDEX.md (progress update) | COMPLETED |
| 011    | backend/src/main/resources/db/migration/V2__insert_categories.sql | COMPLETED |
| 012    | backend/src/main/java/com/umeed/categories/Category.java | COMPLETED |
| 013    | backend/src/main/java/com/umeed/categories/CategoryRepository.java | COMPLETED |
| 014    | backend/src/main/java/com/umeed/categories/CategoryResponse.java | COMPLETED |
| 015    | backend/src/main/java/com/umeed/categories/CategoryService.java | COMPLETED |
| 016    | backend/src/main/java/com/umeed/categories/CategoryController.java | COMPLETED |
| 017    | UMEED_PROJECT_INDEX.md (progress update) | COMPLETED |

Notes:

* 001 was created and confirmed present in the project folder. Git reads it correctly: git status showed only the intended files.
* 002 was compared line by line against the original master instructions. Only the project name and the Java version changed.
* 003 is this file.
* 004 was a one-time approved initialization exception. The project was generated with Spring Initializr (start.spring.io) and extracted to backend/. Settings: Maven, Java 21, Spring Boot 4.1.2, group com.umeed, artifact backend, package com.umeed, dependencies Spring Web, Spring Boot Actuator and Validation.
* 004 generated these files, all inspected: backend/pom.xml, backend/mvnw, backend/mvnw.cmd, backend/.mvn/wrapper/maven-wrapper.properties, backend/.gitignore, backend/.gitattributes, backend/src/main/java/com/umeed/BackendApplication.java, backend/src/main/resources/application.properties, backend/src/test/java/com/umeed/BackendApplicationTests.java. backend/HELP.md was also generated but is ignored by Git.
* 004 verification: "mvnw.cmd test" was run by the project owner on 2026-11-11. Result: Tests run: 1, Failures: 0, Errors: 0, BUILD SUCCESS, on Java 21.0.11. "mvnw.cmd spring-boot:run" started the application and it shut down cleanly.
* 004 not confirmed: the browser check of http://localhost:8080/actuator/health returning {"status":"UP"} was not confirmed to the AI.
* 004 accepted as is: the main class is BackendApplication (not UmeedApplication) in package com.umeed, and spring.application.name is backend. This can be changed later with an approved edit.
* 005 is the update of this file that recorded 004.
* 006 added the datasource URL, username and password settings and the two JPA settings. URL and username default to the local database and can be overridden with UMEED_DB_URL and UMEED_DB_USERNAME. The password has no default.
* 007 added spring-boot-starter-data-jpa, postgresql (runtime) and spring-boot-starter-data-jpa-test. Verification of 006 and 007 together: "mvnw.cmd test" run by the project owner on 2026-11-12 showed Tests run: 1, Failures: 0, Errors: 0, BUILD SUCCESS. The application cannot start with JPA unless it reaches the database, so the connection works.
* 008 added spring-boot-starter-flyway, flyway-database-postgresql and spring-boot-starter-flyway-test.
* 009 creates the categories table (id UUID, slug, name, sort_order, is_active, created_at, updated_at, unique slug, slug format check).
* 010 is the update of this file that recorded 006 to 009.
* 011 inserts the seven MVP categories (section 2A).
* Verification of 008, 009 and 011: psql output shown on 2026-11-12 listed the seven category rows in order, and flyway_schema_history showed version 1 (create categories) and version 2 (insert categories), both successful.
* 012 is the read-only JPA entity for categories. 013 is the package-private Spring Data repository with findByActiveTrueOrderBySortOrderAsc. 014 is the public response record with id, slug and name. 015 is the public service with listActiveCategories. 016 is the package-private controller for GET /api/v1/categories.
* Module boundary rule applied in categories: the repository and controller are package-private. Other modules use CategoryService and CategoryResponse only.
* Verification of 012 to 016: on 2026-11-12 the project owner ran the application and opened http://localhost:8080/api/v1/categories in a browser. The screenshot showed all seven categories as JSON in the correct order. The test output for these files was not shown to the AI, but the application cannot start and serve this response unless the code compiles and the entity matches the table.
* 017 is the update of this file that recorded 011 to 016.
* Migration rule: a migration that has run must never be edited. Every later schema change is a new file (V2, V3 and so on).

Git commits:

* 0a9eef6 – Add repository foundation: gitignore, master instructions, project index.
* 9edfa92 – Initialize Spring Boot backend (Spring Boot 4.1.2, Java 21).
* e6f7621 – Update project index after backend initialization.
* e3237c9 – Add database connection settings to application.properties.
* f3115ad – Connect backend to PostgreSQL: datasource settings, JPA and driver.
* 4aafe5f – Add Flyway for database migrations.
* 9cdc432 – Add V1 migration: create categories table.
* 87604bf – Update project index after database setup.
* a487145 – Add V2 migration: insert MVP categories.
* 5486e99 – Add Category entity.
* a0f4b9d – Add CategoryRepository.
* 914fe5e – Add categories module: entity, repository, response DTO.
* dccba50 – Add categories module with GET /api/v1/categories.
* The 017 update of this index is NOT committed yet.

Allowed statuses:

* NOT STARTED
* PROPOSED
* APPROVED
* IN PROGRESS
* COMPLETED
* BLOCKED
* NEEDS REVIEW

A file can only be marked COMPLETED after actual creation and verification.

---

# 8. IMPORTANT ARCHITECTURAL RULES

1. Start with a modular monolith.
2. Maintain clear module boundaries.
3. Avoid circular dependencies.
4. Do not access another module's repositories directly.
5. Use service interfaces for cross-module communication.
6. Use Flyway for database changes.
7. Use DTOs for API communication.
8. Do not expose JPA entities directly.
9. Use transactions for critical business operations.
10. Protect private pickup addresses.
11. Enforce authorization server-side.
12. Design for future service extraction without deploying microservices prematurely.

---

# 9. BUSINESS RULES THAT MUST NEVER CHANGE WITHOUT APPROVAL

* Umeed is free.
* No buying or selling.
* No payments between users.
* Donor chooses the receiver for donation listings.
* Receiver chooses an offer for their need request.
* Receiver collects from donor.
* Exact addresses are private.
* Completed donations cannot be claimed again.
* Donors cannot claim their own listings.

Any change to these rules requires explicit discussion and approval.

---

# 10. CURRENT PROJECT STATUS

Planning completed:

* MVP scope.
* Home feed design.
* Product concept.
* Product requirements.
* Mobile technology.
* Backend technology.
* Database technology.
* Architecture strategy.
* Development workflow.
* AI master instructions.
* Project index.

Implementation completed:

* Repository foundation files 001 to 003.
* Spring Boot backend initialized, builds and starts (004).
* Backend connected to the local PostgreSQL database umeed_dev (006, 007).
* Flyway added, migrations V1 and V2 applied and verified (008, 009, 011).
* Categories module complete and working: GET /api/v1/categories (012 to 016).

Repository initialized:

* Yes, locally, on branch main with thirteen commits. No GitHub repository or remote exists yet.

First hand-written code file:

* Done: Category.java (012).

---

# 11. OUTSTANDING DECISIONS

These decisions must be discussed before implementation reaches the relevant feature:

* Final authentication provider and OTP delivery provider. THIS IS THE NEXT DECISION. Options discussed on 2026-11-12: Firebase Authentication (less work, handles sending, checking and abuse protection, backend verifies the Firebase ID token) or an Indian SMS provider such as MSG91 or 2Factor (more backend work, needs DLT sender and template registration). The AI leaned towards Firebase for the MVP. Both charge per SMS and current prices were not verified. The project owner has not decided yet.
* Production hosting provider.
* Object storage provider.
* Exact database-per-module strategy.
* Chat implementation approach.
* Location and maps provider.
* Production monitoring.
* Backup strategy.

Do not block unrelated development on decisions that can safely be deferred.

---

# 12. LAST SESSION SUMMARY

Date: 2026-11-12

Completed:

* Committed the 005 index update (e6f7621).
* Confirmed PostgreSQL 18.6 and created the umeed_app login and the umeed_dev database.
* Added database settings to application.properties (006, e3237c9).
* Added Spring Data JPA and the PostgreSQL driver (007, f3115ad).
* Added Flyway (008, 4aafe5f).
* Added migrations V1 and V2 for categories (009, 9cdc432 and 011, a487145). Verified in the database.
* Decided the MVP category list and the plain JSON response shape.
* Built the categories module (012 to 016). GET /api/v1/categories returns the seven categories, verified in a browser.
* Updated this index (010 and 017).

RESUME HERE – next steps, in order, each needing approval:

1. Commit the 017 index update: git add UMEED_PROJECT_INDEX.md, then git commit -m "Update project index after categories module".
2. Decide the OTP provider (section 11). Check current SMS pricing against the monthly budget of about 2,000 rupees first.
3. Design the users table and propose it as migration V3.
4. Build authentication one approved file at a time: Spring Security dependency, user entity, token verification, JWT, profile endpoints.
5. Global exception handling with a consistent error response format in com.umeed.common. This can be done before or alongside authentication.

Open points:

* The health endpoint browser check ({"status":"UP"}) was not confirmed. The application is known to run, because the categories endpoint responds.
* No GitHub repository exists yet. Creating the private repository umeed and pushing is still to do. Until then the only copy of the code is on the PC.
* The project owner has not granted a standing approval for index-only updates. Each index update still needs approval.
* The AI's shell on the PC cannot start because of low disk space on the PC, so commands are run by the project owner and pasted back.
* Three times a file written by the AI did not reach the disk on the first attempt (the 005 and 010 index updates and the 008 pom.xml change). Cause: when the AI rewrote a file and transferred it immediately, the transfer picked up the previous version. It was not caused by IntelliJ. The AI now waits before transferring and checks the file size on disk after every write.
* PostGIS has not been installed or checked yet.
* There are no automated tests for the categories module yet. Only the generated context-load test exists.

Earlier session, 2026-11-11:

Completed:

* Renamed the app from GiveLoop to Umeed everywhere: app name, package com.umeed, repository name umeed and document filenames.
* Changed the backend from Java 25 to Java 21 (already installed, LTS).
* Moved the project home to H:\Project\Umeed. The old folder H:\Project\giveloop holds only the original planning files and can be deleted by the project owner.
* Verified Node.js, npm, Git and Java versions from command output.
* Decided the MVP scope (section 2A).
* Decided the Home feed design (section 2B).
* Created files 001 to 003 and committed them (0a9eef6).
* Initialized Git on branch main.
* Generated the Spring Boot backend through an approved initialization exception (004), verified it with a passing test run, and committed it (9edfa92).
* Updated this index (005).

Earlier session, 2026-11-09:

* Replaced NestJS architecture with Java Spring Boot.
* Selected IntelliJ IDEA.
* Selected modular monolith architecture.
* Retained React Native + Expo.
* Selected PostgreSQL + PostGIS.
* Selected Spring Data JPA and Hibernate.
* Selected Flyway.
* Defined one-file-at-a-time workflow.

---

# 13. INSTRUCTIONS FOR RESUMING DEVELOPMENT

At the beginning of every new AI session:

1. Read UMEED_MASTER_INSTRUCTIONS.md.
2. Read this entire index.
3. Inspect actual repository files.
4. Check Git status.
5. Verify installed technologies where relevant.
6. Compare actual project files with this index.
7. Identify the last completed file.
8. Explain current progress.
9. Propose exactly one next file.
10. Wait for my explicit approval.

Never recreate completed files.

Never assume that previous conversation history exists.

The actual repository is the source of truth.

The project index is the navigation and progress record.

---

# 14. MANDATORY DEVELOPMENT WORKFLOW

PROPOSE ONE FILE.

WAIT FOR APPROVAL.

CREATE ONE FILE.

VERIFY ONE FILE.

UPDATE PROJECT INDEX WITH APPROVAL.

STOP AND WAIT.

This rule applies throughout the entire Umeed project.
