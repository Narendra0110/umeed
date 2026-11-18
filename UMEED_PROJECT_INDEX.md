# UMEED – PROJECT INDEX

Version: 2.0

Last updated: 2026-11-17

Project status: MOBILE APP RUNS ON A REAL ANDROID PHONE (2026-11-18); THE MAIN MVP FLOW (LOGIN, POST, CLAIM, ACCEPT, PICKUP, BOTH CONFIRMATIONS) WORKS END TO END; EVERY MVP FLOW TESTED ON THE PHONE; LAST SHIPPED COMMIT df214cc (F01, 2026-11-18) – PLAN: BUILD THE WHOLE APP (F01–F07), THEN HOST – F01 SHIPPED (PHONE TEST LATER) – F02 PUSH NOTIFICATIONS SHIPPED 5b448a1 (PHONE TEST LATER) – F03 CHAT SHIPPED 949d2a1 (PHONE TEST LATER) – F04 NEED REQUESTS AND OFFERS SHIPPED 9107613 (PHONE TEST LATER) – F05 EXPLORE SHIPPED 3cd5771 (PHONE TEST LATER) – F06A ADMIN API SHIPPED 7650427 – F06B ADMIN WEB APP WRITTEN AND BUILT, WAITING FOR SHIP – NEXT: F07 HINDI

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

## Donation decisions

Decided: 2026-11-15

* Item condition values: NEW, LIKE_NEW, GOOD, USABLE.
* Donation status values: AVAILABLE, RESERVED, COMPLETED, REMOVED. A donation is never deleted from the table.
* Photos: 1 to 5 per donation, JPEG, PNG or WebP, at most 5 MB each. The first photo is the cover.
* Posting a donation is one request that carries the details and the photos together (multipart form). A separate photo upload step was considered and rejected for the MVP.
* Photo storage during development: a local folder (default: umeed-storage inside the Windows user folder) behind the FileStorage interface. A cloud provider can replace it later by adding one class.
* Location: each donation stores city, area and approximate latitude and longitude as plain numbers. Coordinates are rounded to two decimals (about one kilometre) before saving. PostGIS is deferred until after the MVP. This is an agreed exception to the technology stack.
* The public donation response never contains the pickup address or the donor phone number.
* A donor must have completed their profile (display name set) before posting.
* Login first, decided on 2026-11-15: the feed, a single donation and photos are visible only to logged-in users. Nothing about donations is public. Only GET /api/v1/categories and /actuator/health stay public.

## Claim decisions

Decided: 2026-11-16

* There is no limit on how many open claims one person can have.
* Any number of people can claim the same item until the donor accepts one claim.
* When the donor accepts a claim: that claim becomes ACCEPTED, every other pending claim on the item becomes REJECTED, and the item's status becomes RESERVED. A reserved item leaves the feed. It is still listed under the donor's "my donations" and the claimants' "my claims", where the app can show it in its own section by status.
* The project owner's words were that after acceptance the item "will be added under other category". Confirmed by the project owner on 2026-11-16: this means the status-based section described above (for example a "Reserved" list in the app), not a change of the item's product category.
* A person can claim a given item once. Withdrawing a pending claim is final for that item in the MVP (AI suggestion; the project owner did not object).
* A donor cannot claim their own item. Only AVAILABLE items can be claimed. The claimant needs a completed profile.
* The donor sees claimants' names and messages only. Phone numbers and the pickup address are revealed in the pickup batch (B05).
* When a donor removes a listing, its pending claims are rejected automatically.

## Pickup decisions

Decided: 2026-11-16

* After a claim is accepted, the donor and the accepted receiver, and nobody else, can read the pickup address and each other's name and phone number. This is the only response in the API with an address or another person's phone number.
* Each side confirms the handover separately. When both have confirmed, the donation becomes COMPLETED. The database refuses COMPLETED without both confirmations.
* Cancelling a reservation is part of the MVP (AI recommendation; the project owner approved the batch without objecting). The donor or the accepted receiver can cancel while the item is RESERVED. The item returns to AVAILABLE and to the feed, and any confirmation is cleared. The claims that were rejected by that acceptance become PENDING again. The cancelled claim is closed: WITHDRAWN if the receiver cancelled, REJECTED if the donor did, and that person cannot claim the same item again.
* Pickup time scheduling (propose, accept, reschedule) is not in the MVP. The two sides arrange the time by phone.

## Report decisions

Decided: 2026-11-17

* Any logged-in user can report a listing once. A donor cannot report their own listing.
* Reasons: SELLING_OR_ASKING_MONEY, PROHIBITED_ITEM, FAKE_OR_MISLEADING, OFFENSIVE, OTHER. Details are optional (up to 500 characters) but required for OTHER.
* No automatic hiding in the MVP (AI suggestion, approved by the project owner). A reported listing stays visible until the project owner reviews it.
* During the MVP, reports are reviewed by hand in the database (see "Reviewing reports" in section 12A). A report status is OPEN, REVIEWED or DISMISSED.
* The response to the reporter never says who else reported the listing, and the donor is not told who reported them.

---

# 2B. HOME FEED DESIGN

Decided: 2026-11-11

* The Home tab is an Instagram-style feed.
* Each donated item is shown as one post: photos, title, condition, approximate area and donor name.
* A Claim button sits below the post.
* Swiping up means "not interested" and moves to the next item.
* Nearby items are shown first.
* In the MVP, swiping up only shows the next item. Skipped items are not remembered and can appear again.
* Decided 2026-11-18 by the project owner: Home shows only items the user can claim. The user's own items never appear there (they are under Activity, My donations), and items the user has already claimed, in any claim status, leave Home (they are under Activity, My claims). The server applies this rule to both feed orders. After a claim, a short "Claimed" banner appears and the feed moves on to the next item.

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
* GitHub repository created and pushed on 2026-11-12: https://github.com/Narendra0110/umeed (remote origin, branch main tracks origin/main). The project owner was asked to confirm it is Private; that confirmation was not shown to the AI. After every git commit, run git push.

Not done yet:

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

Status: IN PROGRESS

Done:

* users table (V3).
* Spring Security with OAuth2 resource server, verifying Firebase ID tokens for project umeed-2e018 (issuer and audience checked).
* Security rules: GET /api/v1/categories and /actuator/health are public. Everything else needs a valid token. Stateless, CSRF off.
* Users module: entity, repository, response, service (find or register from a verified token), GET /api/v1/users/me.
* End-to-end test with a real Firebase ID token passed on 2026-11-15.
* Profile setup: PUT /api/v1/users/me sets the display name, with validation. Suspended and deleted accounts are refused with 403.

Not done yet:

* Account status enforcement for suspended users in the other modules (listings, claims), as those are built.
* Account deletion.
* Logout needs no backend work (the app discards its token).

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

Status: IN PROGRESS

Done:

* donations and donation_images tables (V4, V5).
* Donation and DonationImage entities and their repositories.
* FileStorage interface and the local folder implementation.
* CreateDonationRequest and DonationResponse.
* User lookups for other modules in UserService (requireActiveUser, displayNamesByIds).

Done in batch B02, verified by automated tests:

* CategoryService.isActiveCategory.
* DonationService: create a donation with photos, list the feed, view one donation.
* DonationController and FileController (serves photos).
* Upload size limits in application.properties.
* Automated tests for donations.

Not done yet:

* Nearby-first sorting of the feed.
* My donations, edit and remove a listing.

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
| 018    | backend/src/main/resources/db/migration/V3__create_users.sql | COMPLETED |
| 019    | UMEED_PROJECT_INDEX.md (end of session save) | COMPLETED |
| 020    | backend/pom.xml (Spring Security with OAuth2 resource server) | COMPLETED |
| 021    | UMEED_PROJECT_INDEX.md (end of session save) | COMPLETED |
| 022    | backend/src/main/resources/application.properties (Firebase token settings) | COMPLETED |
| 023    | backend/src/main/java/com/umeed/auth/SecurityConfig.java | COMPLETED |
| 024    | backend/src/main/java/com/umeed/users/User.java | COMPLETED |
| 025    | backend/src/main/java/com/umeed/users/UserRepository.java | COMPLETED |
| 026    | backend/src/main/java/com/umeed/users/UserResponse.java | COMPLETED |
| 027    | backend/src/main/java/com/umeed/users/UserService.java | COMPLETED |
| 028    | backend/src/main/java/com/umeed/users/UserController.java | COMPLETED |
| 029    | UMEED_PROJECT_INDEX.md (end of session save) | COMPLETED |
| 030    | backend/src/main/java/com/umeed/users/User.java (changeDisplayName) | COMPLETED |
| 031    | backend/src/main/java/com/umeed/users/UpdateProfileRequest.java | COMPLETED |
| 032    | backend/src/main/java/com/umeed/users/UserService.java (updateDisplayName) | COMPLETED |
| 033    | backend/src/main/java/com/umeed/users/UserController.java (PUT /me) | COMPLETED |
| 034    | UMEED_PROJECT_INDEX.md (progress update) | COMPLETED |
| 035    | backend/src/main/java/com/umeed/common/ApiError.java | COMPLETED |
| 036    | backend/src/main/java/com/umeed/common/GlobalExceptionHandler.java | COMPLETED |
| 037    | backend/src/main/resources/db/migration/V4__create_donations.sql | COMPLETED |
| 038    | backend/src/main/resources/db/migration/V5__create_donation_images.sql | COMPLETED |
| 039    | backend/src/main/java/com/umeed/donations/Donation.java | COMPLETED |
| 040    | backend/src/main/java/com/umeed/donations/DonationImage.java | COMPLETED |
| 041    | backend/src/main/java/com/umeed/donations/DonationRepository.java | COMPLETED |
| 042    | backend/src/main/java/com/umeed/donations/DonationImageRepository.java | COMPLETED |
| 043    | backend/src/main/java/com/umeed/storage/FileStorage.java | COMPLETED |
| 044    | backend/src/main/java/com/umeed/storage/LocalFileStorage.java | COMPLETED |
| 045    | backend/src/main/java/com/umeed/donations/CreateDonationRequest.java | COMPLETED |
| 046    | backend/src/main/java/com/umeed/donations/DonationResponse.java | COMPLETED |
| 047    | backend/src/main/java/com/umeed/users/UserService.java (requireActiveUser, displayNamesByIds) | COMPLETED |

From 2026-11-15 work is tracked by feature batch, not by file number.

| Batch | Contents | Status |
| ----- | -------- | ------ |
| B01   | Workflow change: UMEED_MASTER_INSTRUCTIONS.md version 3.0, ship.cmd, first automated API tests (CategoryApiTests, SecurityRulesTests, UserApiTests), this index | COMPLETED |
| B02   | Post and browse donations: CategoryService.isActiveCategory, DonationService, DonationController, FileController, PageResponse, upload limits, GlobalExceptionHandler additions, DonationApiTests | COMPLETED |
| B03   | Nearby-first feed, my donations, remove a listing: DonationRepository, Donation.markRemoved, DonationService, DonationController, 4 new tests in DonationApiTests | COMPLETED |
| B04   | Claims: V6__create_claims.sql; claims module (Claim, ClaimRepository, CreateClaimRequest, ClaimResponse, ClaimService, ClaimController); donations additions (DonationSummary, DonationRemovedEvent, row locks, reserve, markReserved); ClaimApiTests | COMPLETED |
| B05   | Pickup: V7__add_handover_confirmation.sql; pickup module (PickupResponse, PickupService, PickupController); additions to Donation, DonationService, DonationPickupDetails, ClaimRepository, ClaimService, UserService, UserContact; PickupApiTests | COMPLETED |
| B06   | Report a listing: V8__create_reports.sql; reports module (Report, ReportRepository, CreateReportRequest, ReportResponse, ReportService, ReportController); ReportApiTests | COMPLETED |
| M01   | Mobile app setup: Expo project in apps/mobile, Firebase phone login, profile setup screen. Approved 2026-11-17 with app id com.umeed.app and a local Android development build. Split into part 1 (environment and Expo project generation, done by the project owner) and part 2 (screens and API client, written by the AI after inspecting the generated project). | WRITTEN, NOT YET BUILT ON A DEVICE |
| M02   | The rest of the MVP screens: four tabs (Home feed, Donate, Activity, Profile), item details, claims on my item, pickup details, report a listing; photo, location and test libraries; first mobile tests; ship.cmd runs the mobile type check and tests. Approved 2026-11-17. | WRITTEN, NOT YET BUILT ON A DEVICE |
| F01   | Delete account and block a user: V9__create_user_blocks.sql; account module; blocks module; BlockedUsers interface in users; feed and claim changes; delete-account and blocked screens; BlockApiTests, AccountDeletionApiTests, account-and-blocks.test.ts. Approved 2026-11-18. | SHIPPED df214cc, PHONE TEST PENDING |
| F02   | Push notifications: V10__create_device_tokens.sql; notifications module (device tokens, FCM sender, after-commit dispatcher, listener); four events from claims and pickup; lib/push.ts and push banner; messaging and Google auth libraries; NotificationApiTests, PushDispatcherTests, NotificationListenerTests, push.test.ts. Approved 2026-11-18. | SHIPPED 5b448a1; PHONE TEST PENDING (NEEDS APP REBUILD) |
| F03   | In-app chat: V11__create_chat_messages.sql; chat module; ClaimParties and claimId in pickup details; chat notification; Chats tab and chat screen; ChatApiTests, chat.test.ts. Approved 2026-11-18. | SHIPPED 949d2a1; PHONE TEST PENDING |
| F04   | Need requests and offers: V12__create_need_requests_and_offers.sql; needs module; ClaimService.acceptForNeed; block, account and notification additions; Items/Needs on Home, Ask for something on Donate, need and offer screens, Needs view in Activity; NeedApiTests, needs.test.ts. Approved 2026-11-18. | SHIPPED 9107613; PHONE TEST PENDING |
| F05   | Explore: V13__create_donation_skips.sql; search endpoint with filters, Not interested and clear; search screen with list and Google map (react-native-maps, app.config.js); Not interested on Home, Show hidden items again in Profile; ExploreApiTests, explore.test.ts. Approved 2026-11-18. | SHIPPED 3cd5771; PHONE TEST PENDING (NEEDS REBUILD AND MAPS KEY) |
| F06A  | Admin API: V14__admin_actions_and_need_reports.sql; admin module (stats, reports review, remove listing, close need, user search, suspend/unsuspend, admin log); report a need; suspended accounts hidden and told clearly in the app; AdminApiTests, reports-and-suspension.test.ts. Approved 2026-11-18. | SHIPPED 7650427 |
| F06B  | Admin web app (apps/admin, Next.js 16, Firebase phone login, admin role checked by the backend): overview, reports, listing, need, users, user, admin log; ship.cmd builds it. Approved 2026-11-18. | WRITTEN AND BUILT, WAITING FOR SHIP; REAL LOGIN NOT YET TESTED |

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
* 018 creates the users table: id, firebase_uid, phone_number, display_name, role, status, created_at, updated_at, deleted_at, with unique, role, status, phone format and identity-present constraints. Verified on 2026-11-14: psql showed the nine columns, all constraints, and flyway_schema_history versions 1, 2 and 3 successful. It has run, so it must never be edited.
* 020 added spring-boot-starter-security-oauth2-resource-server and its -test starter. Both artifact names are confirmed correct for Spring Boot 4.1.2 by a passing build.
* 021 is the end of session save of this file, requested by the project owner on 2026-11-13.
* 022 added umeed.firebase.project-id (default umeed-2e018, override with UMEED_FIREBASE_PROJECT_ID), the JWT issuer-uri https://securetoken.google.com/{project id} and the JWT audience. Verification of 020 and 022: mvnw.cmd test showed BUILD SUCCESS on 2026-11-14.
* 023 is the security configuration in package com.umeed.auth. Verification: the project owner reported that the categories endpoint and the health endpoint worked in the browser after this file. The 401 for an unknown protected address was taken as included in that report but no screenshot was shown.
* 024 is the User entity with nested Role and Status enums, a package-private register(firebaseUid, phoneNumber) factory, automatic timestamps, and no setters yet. 025 is the package-private repository with findByFirebaseUid. 026 is the public response record for a user's own profile (id, phoneNumber, displayName, profileComplete, role, status). It contains the phone number, so it must only be returned to the user it belongs to. 027 is the public service with findOrRegister, which handles two simultaneous first requests. Verification of 024 to 027: mvnw.cmd test showed BUILD SUCCESS after each file on 2026-11-14 (027 reported by the project owner, the others shown).
* 028 is the package-private controller for GET /api/v1/users/me. It takes the Firebase UID from the token subject and the phone number from the phone_number claim, and answers 403 if the token has no phone number. Verified on 2026-11-15 with a real Firebase ID token for the test phone number: HTTP 200, role USER, status ACTIVE, displayName null, profileComplete false, and the same id on a second call, so the user is created once and then found. The psql check of the users row was not completed (password mistyped at the prompt); the identical id on the second call already shows the row was saved. A request without a token answering 401 was not shown to the AI.
* 030 added the package-private changeDisplayName method to User (strips leading and trailing spaces). 031 is the package-private request record with displayName, required, 2 to 80 characters. 032 added the public, transactional updateDisplayName to UserService: looks the user up by the verified Firebase UID, answers 404 if not found and 403 if the account is not ACTIVE. 033 added PUT /api/v1/users/me with @Valid.
* Verification of 030 to 033 on 2026-11-15 with a real token: PUT with "Narendra" returned 200 with displayName Narendra and profileComplete true; PUT with an empty name returned 400; a following GET returned 200 and still showed Narendra.
* Known issue found in that test: the 400 body is Spring's generic one (timestamp, status, error, path) and does not include the validation message. To be fixed by global error handling.
* 034 is the update of this file that recorded 028 and 030 to 033.
* 035 is the common error shape (timestamp, status, error, message, path, fieldErrors). 036 is the handler that produces it for validation errors, unreadable bodies, deliberate errors, access denied, Spring's standard errors and unexpected errors (500 with a generic message, cause logged only). Verified on 2026-11-15 with a real token: empty name gives 400 with both displayName messages; broken body gives 400; valid name gives 200; unknown address gives 404 in the new shape. A request without a token is refused by Spring Security before any controller, so that 401 has an empty body.
* 037 and 038 verified on 2026-11-15 with psql: donations has fourteen columns, four indexes, four check constraints and two foreign keys; donation_images has five columns, two unique constraints, the 0 to 4 check and one foreign key; flyway_schema_history shows versions 1 to 5 successful. Both have run and must never be edited.
* 039 to 042 verified by BUILD SUCCESS on 2026-11-15 (entities match their tables, repository method names valid). 043 and 044 verified by BUILD SUCCESS (compile and Spring wiring only; saving a real file is not tested yet).
* 045, 046 and 047 are verified by the B01 ship run (they compile and the application starts with them).
* Batch B01: the test classes use MockMvc with Spring Security's simulated verified token, so no real Firebase token is needed. UserApiTests runs in a rolled-back transaction against the development database. Verified on 2026-11-15: ship reported Tests run: 11, Failures: 0, Errors: 0, BUILD SUCCESS, then committed and pushed as 4664d77. The spring-security-test library is available through the existing test starter.
* 029 is the end of session save of this file. The project owner said the session would continue tomorrow; the AI saved the index following the two earlier end of session saves.
* Known gap in 027: if a phone number already belongs to a user but arrives with a different Firebase UID, the request fails with a server error. To be handled with global exception handling.
* 019 is the end of session save of this file, requested by the project owner on 2026-11-12.
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
* All commits were pushed to GitHub on 2026-11-12. The AI did not see whether the 017 index update was committed before that push. If git status shows UMEED_PROJECT_INDEX.md as modified, it is covered by the 019 commit.
* Commits seen on 2026-11-14, all pushed to origin/main: 5dab79f (Configure Firebase ID token verification), 0c81fed (Add User entity), 0c3377e (Add UserRepository), bb7c82f (Add UserService), 21c77dc (Add GET /api/v1/users/me). Other commits in between (V3 migration, Spring Security pom change, SecurityConfig, UserResponse, index saves) were made by the project owner but their hashes were not all shown.
* The AI did not see the commits for 030 to 033 or for the 029 index save. The project owner was given the commands.
* Commits seen on 2026-11-15, pushed: 2172d80 (donations migrations, entities and repositories), df68a95 (FileStorage interface and local folder implementation). Earlier commits of that day were made by the project owner without all hashes being shown.
* 4664d77 – Switch to feature batches: ship script, API tests, master instructions 3.0 (pushed). It followed 3f7cca9.
* The index update that records the B01 result and the B02 plan is NOT committed yet. The next ship run commits it.

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

* Yes, on branch main, pushed to the GitHub repository Narendra0110/umeed.

First hand-written code file:

* Done: Category.java (012).

---

# 10. WORKING API ENDPOINTS

As of 2026-11-15, all verified by hand:

| Method | Address              | Access                | Purpose |
| ------ | -------------------- | --------------------- | ------- |
| GET    | /actuator/health     | Public                | Health check |
| GET    | /api/v1/categories   | Public                | List of active categories (id, slug, name) |
| GET    | /api/v1/users/me     | Firebase token needed | The caller's own profile. Creates the user on first call. |
| PUT    | /api/v1/users/me     | Firebase token needed | Set the display name. Body: {"displayName": "..."} |
| POST   | /api/v1/donations    | Firebase token needed | Post a donation. Multipart form: categoryId, title, description, itemCondition, city, area, latitude, longitude, pickupAddress, and 1 to 5 files named photos. Answers 201. |
| GET    | /api/v1/donations    | Firebase token needed | The feed: available donations the caller can claim (not their own, not already claimed by them; since 2026-11-18), newest first. Parameters page (default 0) and size (default 20, maximum 50). Answers items, page, size, totalItems, totalPages, hasNext. |
| GET    | /api/v1/donations/{id} | Firebase token needed | One donation. 404 if unknown or removed. |
| GET    | /api/v1/donations?lat=..&lng=.. | Firebase token needed | The feed sorted nearest first, then newest. Both lat and lng are needed. The caller's location is not stored. |
| GET    | /api/v1/donations/mine | Firebase token needed | The caller's own donations in every status except removed. |
| DELETE | /api/v1/donations/{id} | Firebase token needed | Remove a listing. Only the donor, only while AVAILABLE. Answers 204. The row is kept with status REMOVED. |
| POST   | /api/v1/donations/{id}/claims | Firebase token needed | Claim an item. Optional body {"message": "..."} up to 300 characters. Answers 201. |
| GET    | /api/v1/donations/{id}/claims | Firebase token needed | Donor only: all claims on the item, oldest first, with claimant names. |
| POST   | /api/v1/claims/{id}/accept | Firebase token needed | Donor only: accept one claim. Rejects the others and reserves the item. |
| POST   | /api/v1/claims/{id}/withdraw | Firebase token needed | Claimant only: withdraw a pending claim. |
| GET    | /api/v1/claims/mine  | Firebase token needed | The caller's own claims, newest first, paged. |
| GET    | /api/v1/donations/{id}/pickup | Firebase token needed | PRIVATE. Donor and accepted receiver only: pickup address, both names and phone numbers, who has confirmed. |
| POST   | /api/v1/donations/{id}/pickup/confirm | Firebase token needed | Donor or accepted receiver confirms the handover. Both confirmations complete the donation. |
| POST   | /api/v1/donations/{id}/pickup/cancel | Firebase token needed | Donor or accepted receiver cancels the reservation. Answers 204. |
| POST   | /api/v1/donations/{id}/reports | Firebase token needed | Report a listing. Body {"reason": "...", "details": "..."}. Answers 201. |
| POST   | /api/v1/needs/{id}/reports | Firebase token needed | Report a need (F06A). Same body and rules. Answers 201. |
| GET, POST | /api/v1/admin/... | Firebase token of an ADMIN | Admin dashboard API (F06A): me, stats, reports, reports/{id}/review, donations/{id}, donations/{id}/remove, needs/{id}, needs/{id}/close, users, users/{id}, users/{id}/suspend, users/{id}/unsuspend, actions. Anyone else gets 403. |
| GET    | /api/v1/files/...    | Firebase token needed | A stored photo, at the address given in imageUrls. |
| DELETE | /api/v1/users/me     | Firebase token needed | Delete the caller's account (F01). 409 while a pickup is in progress. Answers 204. |
| GET    | /api/v1/blocks       | Firebase token needed | The people the caller blocked: userId, name, blockedAt (F01). |
| POST   | /api/v1/blocks       | Firebase token needed | Block a user. Body {"userId": "..."}. 400 for oneself, 404 unknown, 409 during a shared pickup. Answers 204 (F01). |
| DELETE | /api/v1/blocks/{userId} | Firebase token needed | Unblock. Answers 204 (F01). |
| PUT    | /api/v1/devices      | Firebase token needed | Register this phone for notifications. Body {"token": "...", "platform": "ANDROID" or "IOS"}. Answers 204 (F02). |
| POST   | /api/v1/devices/unregister | Firebase token needed | Forget this phone (before logout). Body {"token": "..."}. Answers 204 (F02). |
| GET    | /api/v1/chats        | Firebase token needed | The caller's conversations: claimId, item, other person, open, last message, unread count (F03). |
| GET    | /api/v1/claims/{claimId}/messages | Firebase token needed | One conversation, oldest first; ?after={messageId} for newer only. Donor and accepted receiver only (F03). |
| POST   | /api/v1/claims/{claimId}/messages | Firebase token needed | Send {"body": "..."}, 1 to 1000 characters, only while the pickup is in progress. Answers 201 (F03). |
| POST   | /api/v1/claims/{claimId}/messages/read | Firebase token needed | Mark the other person's messages as read. Answers 204 (F03). |
| POST   | /api/v1/needs        | Firebase token needed | Post a need. JSON: categoryId, title, description, city, area, latitude, longitude. Answers 201 (F04). |
| GET    | /api/v1/needs        | Firebase token needed | Open needs from others (not own, not blocked); ?lat&lng for nearest first; paged (F04). |
| GET    | /api/v1/needs/mine   | Firebase token needed | The caller's needs in every status, with pendingOfferCount (F04). |
| GET    | /api/v1/needs/{id}   | Firebase token needed | One need. 404 if unknown or blocked either way (F04). |
| POST   | /api/v1/needs/{id}/close | Firebase token needed | The person who asked closes it; waiting offers declined (F04). |
| POST   | /api/v1/needs/{id}/offers | Firebase token needed | Offer one of the caller's AVAILABLE items. Body {"donationId": "...", "message": "..."}. Answers 201 (F04). |
| GET    | /api/v1/needs/{id}/offers | Firebase token needed | All offers on the need; the person who asked only (F04). |
| GET    | /api/v1/offers/mine  | Firebase token needed | The caller's offers, newest first, paged (F04). |
| POST   | /api/v1/offers/{id}/accept | Firebase token needed | The person who asked chooses the offer: item reserved for them, need fulfilled. Answers {donationId, claimId} (F04). |
| POST   | /api/v1/offers/{id}/withdraw | Firebase token needed | The donor withdraws a waiting offer. Answers 204 (F04). |
| GET    | /api/v1/donations/search | Firebase token needed | Search: q, categoryId, city, lat+lng (nearest first), maxKm 1 to 500 (needs location); paged (F05). |
| POST   | /api/v1/donations/{id}/skip | Firebase token needed | "Not interested": hide from the caller's Home. Answers 204 (F05). |
| DELETE | /api/v1/donations/skips | Firebase token needed | Show hidden items again. Answers 204 (F05). |

* Every other address answers 401 without a valid token.
* The token is sent in the header: Authorization: Bearer followed by the Firebase ID token.

---

# 10A. AUTHENTICATION DESIGN

Decided: 2026-11-12

* OTP provider: Firebase Authentication (phone sign-in).
* Flow: the mobile app asks Firebase to send and check the OTP. Firebase gives the app a signed ID token. The app sends that token with every request. The backend verifies the token signature against Google's public keys, then finds or creates the matching user in the users table.
* Firebase project ID: umeed-2e018 (given by the project owner on 2026-11-13). Verified on 2026-11-14 through the Firebase REST API: Phone sign-in is enabled and the test phone number is accepted (sendVerificationCode returned a sessionInfo). The SMS region policy had to be set to allow India first (Authentication, Settings, SMS region policy). A web app named umeed-test was registered in the Firebase project to obtain the Web API Key. The Web API Key is kept by the project owner and is not stored in the repository.
* Token checks the backend must make: signature against Google public keys, issuer https://securetoken.google.com/umeed-2e018, audience umeed-2e018, and expiry.
* The backend needs only the Firebase project ID, which is not a secret. No Firebase service account key is stored on the server or in Git.
* The Firebase ID token is a JWT, which satisfies the JWT requirement in the master instructions.
* The backend never trusts a user ID or role sent by the client. The user is always resolved from the verified token.
* During development, Firebase test phone numbers with fixed codes are used, so no real SMS is sent.
* Not verified: current Firebase SMS pricing and whether a billing plan upgrade is needed before real SMS can be sent. Check before launch against the monthly budget of about 2,000 rupees.

---

# 11. OUTSTANDING DECISIONS

These decisions must be discussed before implementation reaches the relevant feature:

* Push notification provider details, when notifications are built (post-MVP).
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

Date: 2026-11-16

Completed:

* Wrote batch B02 exactly to the plan approved on 2026-11-15 (login first).

Batch B02 notes:

* One file was added beyond the plan: com.umeed.common.PageResponse, a stable shape for paged lists (items, page, size, totalItems, totalPages, hasNext). Reason: returning Spring's own page object as JSON is not a supported, stable format.
* GlobalExceptionHandler now also handles form binding errors (same 400 shape as JSON validation) and oversized uploads (413). A value that cannot be read at all, such as a malformed id, is reported as "Invalid value".
* Photos are checked by their first bytes, not by file name or the type the client claims, so a renamed non-image file is rejected.
* If saving fails part way, photos already stored for that donation are deleted again.
* Login first has a consequence for the mobile app: photo addresses need the token too, so the app's image component must send the Authorization header when loading photos.
* Relative photo addresses: imageUrls contain paths such as /api/v1/files/donations/2026/11/x.jpg. The app adds the server address in front.
* Not covered by automated tests: the 5 MB limit enforced by the server itself (the test tool does not enforce upload limits), a real upload from a phone, and a real Firebase token on the new endpoints.
* The tests store photos in a temporary folder (umeed-test-storage inside the system temp folder) and delete it after each test.

Batch B02 result: shipped on 2026-11-15 (clock time on the PC). ship reported Tests run: 20, Failures: 0, Errors: 0, BUILD SUCCESS, committed and pushed as e11bf4e. It passed on the first run.

Batch B03 was approved and written on 2026-11-16. No migration. Editing a listing was deliberately left out of the MVP: a donor removes the post and creates a new one. Photos of a removed listing are kept in storage for now.

Batch B03 result: ship reported Tests run: 24, Failures: 0, Errors: 0, BUILD SUCCESS, committed and pushed as a0449fe. It passed on the first run, including the hand-written distance query.

Batch B04 was approved on 2026-11-16 (the migration was shown in full first) and written. Notes:

* Safety against double acceptance has two layers: the donation row is locked while a claim is accepted, and the database has a partial unique index allowing only one ACCEPTED claim per donation.
* Claim status changes use guarded updates ("change only if still PENDING"), so a claim withdrawn at the same moment is never accepted by mistake.
* Submitting a claim takes a shared lock on the donation, so a claim cannot slip in while the donor is accepting another one.
* Modules stay separate: claims talk to donations only through DonationService (getSummary, lockClaimable, reserve, titlesByIds) and react to DonationRemovedEvent.
* Not covered by automated tests: true simultaneous requests from two users (the tests run one request at a time).
* V6 has run (applied by the B04 ship run). It must never be edited.

Batch B04 result: ship reported Tests run: 30, Failures: 0, Errors: 0, BUILD SUCCESS, committed and pushed as 793487a. It passed on the first run.

Batch B05 was approved on 2026-11-16 (the private response and the migration were shown in full first) and written. V7 is exactly the migration that was shown. New module: com.umeed.pickup, which uses the donations, claims and users services and has no table of its own.

Batch B05 result: ship reported Tests run: 35, Failures: 0, Errors: 0, BUILD SUCCESS, committed and pushed as 7b6e65c. It passed on the first run. V7 has run and must never be edited. The session ended here because the project owner's usage limit was almost reached.

Batch B06 was approved on 2026-11-17 (the migration was shown in full first) and written. V8 is exactly the migration that was shown.

Batch B06 result: ship reported Tests run: 38, Failures: 0, Errors: 0, BUILD SUCCESS, committed and pushed as 94a7637. It passed on the first run. V8 has run and must never be edited. The backend now covers the whole MVP scope in section 2A.

RESUME HERE – next steps, in order:

1. Done on 2026-11-15 (clock time on the PC): manual check with real Firebase tokens for two users (donor and receiver, the second using a second Firebase test phone number). The project owner reported that every step matched: profile, posting with a real photo (location rounded, no pickup address in the reply), nearby feed, photo download, own-claim refused, claim, claims visible to the donor only, pickup details hidden before acceptance, accept, pickup details for both parties, both confirmations completing the donation, report and duplicate report, 401 without a token. Lessons: a Firebase ID token expires after one hour (the backend correctly answers 401 "Jwt expired"); a phone number that is not saved as a Firebase test number makes Firebase try a real SMS and fail with BILLING_NOT_ENABLED.
2. Batch M01 approved on 2026-11-17: app identifier com.umeed.app (Android and iOS); test on a real Android phone with a development build made locally (Expo Go cannot run React Native Firebase).
3. M01 part 1, done by the project owner (steps given on 2026-11-17; session ended at step 4, the Firebase Android app, on 2026-11-17; steps 1 to 3 status unknown, ask): install JDK 17 (winget install Microsoft.OpenJDK.17); in Android Studio SDK Manager install Android SDK Platform 36; set ANDROID_HOME to %LOCALAPPDATA%\Android\Sdk and add its platform-tools folder to Path; set GRADLE_USER_HOME to a folder on H: to keep the large Gradle cache off the full C: drive; add an Android app com.umeed.app in the Firebase console (Project settings, General, the "Add app" button at the top right of the Your apps section, then the Android icon; the project so far only has the umeed-test web app) and save google-services.json into apps/mobile (it is ignored by Git); generate the project with npx create-expo-app@latest mobile --template default@sdk-58 inside H:\Project\Umeed\apps. After that the AI inspects the generated files and reports before anything is committed.
4. M01 part 2, written by the AI: remove the template's example screens; install expo-dev-client, @react-native-firebase/app, @react-native-firebase/auth and the agreed libraries; app.json plugins and googleServicesFile; screens for phone number, OTP code, profile setup and a placeholder home; an API client that sends the Firebase token and reads ApiError; the backend address from an environment variable. Then: npx expo prebuild --clean, get the debug SHA-1 and add it to the Firebase Android app, npx expo run:android with the phone connected by USB.
5. iOS builds need a Mac or Expo's cloud build service; later.
6. Batch M02 (the rest of the MVP screens) was approved and written on 2026-11-17; see "Batch M02" below. Done: shipped together with M01 as 7678c65.
7. Android build fixed and built on 2026-11-18 (see "Build attempts" below, attempt 3). Shipped as a8ae8bd. Next (the project owner plans to connect the phone on 2026-11-19): install on the phone over USB with run-android. Manual checks on the phone: the whole MVP flow with two Firebase test numbers, as in step 1, and how each screen looks.

Batch M02, written by the AI on 2026-11-17:

* Decisions in the approved plan: four tabs (Home, Donate, Activity, Profile); Explore is left out because map and search are deferred. Posting needs location permission because the backend needs coordinates; the feed works without it (newest first). City and area are typed by hand. Photos are shrunk on the phone to at most 1600 pixels on the longest side, JPEG quality 0.7, to stay under the 5 MB limit. Editing a donation is not included (no backend endpoint).
* Libraries added with npx expo install: expo-image-picker ~58.0.10, expo-image-manipulator ~58.0.16, expo-location ~58.0.22; for tests jest-expo ^58.0.15, jest ^30.5.2, @testing-library/react-native ^14.1.0, @types/jest ^30.0.0. app.json gained the expo-image-picker plugin (photo and camera permission texts, microphone off) and the expo-location plugin (when-in-use permission text). package.json gained the scripts test and typecheck and a jest section (preset jest-expo, @/ path alias, 30 second test timeout because the first test loads React Native slowly from the H: hard disk).
* Navigation: src/app/(tabs)/_layout.tsx uses Expo Router native tabs (expo-router/native-tabs) with SF Symbols on iOS and Material Symbols on Android; it sends logged-out users to login. src/app/home.tsx (the M01 placeholder) was deleted; index.tsx and profile-setup.tsx now go to /feed. The root stack lists the tabs and the four item screens.
* Screens: (tabs)/feed.tsx (one item per screen, swipe up for the next, sideways through photos, Claim with an optional message, Report; own items show "See claims"; nearest first when location is allowed, with the distance shown); (tabs)/donate.tsx (photos from camera or gallery, title, description, category, condition, city, area, private pickup address, location status); (tabs)/activity.tsx (My donations grouped Available, Reserved, Given away; My claims grouped Accepted, Waiting, Closed; withdraw a pending claim); (tabs)/profile.tsx (name with change, phone number, log out); donation/[id]/index.tsx (item details; donor: See claims, Remove listing, Pickup details; others: Claim, Report); donation/[id]/claims.tsx (donor chooses one claimant, with a confirmation); donation/[id]/pickup.tsx (address, the other person's name and phone with tap to call, both confirmations, I have handed it over or I have received it, cancel reservation); donation/[id]/report.tsx (reason and details; details required for Something else).
* Shared code: src/lib/types.ts (response shapes and enum values matching the backend), format.ts (labels, grouping by status, distance, time ago), schemas.ts (form rules repeating the backend's validation), donations.ts, claims.ts, pickup.ts, reports.ts (TanStack Query hooks; donationFormData builds the multipart form), location.ts, photos.ts, session.ts (token headers for photos, log out). api.ts now sends FormData bodies as multipart and has fileUrl for the relative photo addresses. components/ui.tsx gained a scrollable Screen, multi-line fields with hints, a danger button, LinkButton, ChoiceChips, Segmented, Pill, EmptyState and ErrorState; new components/photos.tsx (AuthImage sends the Firebase token; PhotoCarousel), list-row.tsx and claim-sheet.tsx.
* The pickup screen decides whether the user is the donor or the receiver by comparing phone numbers, because the pickup response has no user ids. Every screen has loading, empty, error and success states.
* Tests in src/__tests__ (39 tests, 5 files): api (token header, JSON and multipart bodies, 204, error shape with field errors, network failure, no user), format, schemas, donations-and-pickup (multipart fields and photos, pickup role), ui (ErrorState, ChoiceChips, Segmented). Result on 2026-11-17, run by the AI: npx tsc --noEmit no errors; npx jest --ci 5 suites, 39 tests passed. Lint was not run (ESLint is not set up in the project yet).
* ship.cmd now also runs npx tsc --noEmit and npx jest --ci in apps/mobile after the backend tests, and commits only if all pass.
* First ship run (2026-11-17) failed in the backend: "class file version 65.0 ... only recognizes up to 61". The JDK 17 installer had set the machine JAVA_HOME to jdk-17 and put its bin first on the machine Path, so the Maven wrapper ran on Java 17. Fix given to the project owner: machine JAVA_HOME back to C:\Program Files\Java\jdk-21.0.11 and the jdk-17 entry removed from the machine Path (run-android.cmd finds JDK 17 by its folder). Keep JAVA_HOME on 21.
* Second ship run: the backend passed, then the mobile type check failed with 151 errors in the test files (describe, it, expect, jest not found). Cause: the AI ran the type check before writing the tests and did not repeat it. TypeScript 6 no longer loads @types packages automatically, so tsconfig.json now has "types": ["jest"]; the tests use globalThis instead of global and type caught errors as ApiError. Re-checked by the AI: npx tsc --noEmit no errors, npx jest --ci 39 passed.
* Batch M01 and M02 result: the third ship run passed (backend tests, mobile type check, 39 mobile tests) and was committed and pushed as 7678c65 on 2026-11-17. This is the first commit of apps/mobile. This index update is NOT committed yet; the next ship run commits it.
* Not verified: nothing has run on a device. The screens' appearance, the camera, gallery and location permissions, photo loading with the token, and the native tabs all need the phone build.

M01 part 1 progress on 2026-11-17:

* Done: Firebase Android app com.umeed.app registered, google-services.json saved in apps/mobile; Expo project generated in apps/mobile with npx create-expo-app@latest mobile --template default@sdk-58 (create-expo-app 5.0.0). Not committed yet.
* Inspected by the AI: Expo SDK 58 (expo ~58.0.25), React Native 0.88.1, React 19.3.0, TypeScript ~6.0.3, Expo Router with routes in src/app, path alias @/* for src. The template's .gitignore excludes the generated android and ios folders. The template added example screens (src/app/index.tsx, explore.tsx, components, hooks, demo images), a reset-project script, an MIT LICENSE file, AGENTS.md (notes for AI coding tools) and .claude/settings.json (turns on an Expo plugin for Claude Code).
* Problems seen: java -version still shows 21 (JDK 17 not installed or not selected; do NOT set JAVA_HOME to 17 globally, because the backend's Maven wrapper uses JAVA_HOME and needs 21); adb not recognized (Path not set or platform-tools missing). npm audit reported 3 moderate issues; do not run npm audit fix --force (it breaks SDK-matched versions).
* Decision 2026-11-17: test on the Android emulator instead of a real phone (project owner's preference). The emulator reaches the PC's backend at http://10.0.2.2:8080. Use a system image with Google Play. Put emulator files on H: (ANDROID_AVD_HOME).
* Done by the project owner: npm run reset-project (examples deleted), .claude and LICENSE removed. JDK 17 is installed at C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot. adb.exe exists in %LOCALAPPDATA%\Android\Sdk\platform-tools but that folder is not on Path yet. ANDROID_AVD_HOME set to H:\android-avd. C: had about 11 GB free.
* Emulator problem: a Pixel 10, API 36.1 (Android 16) x86_64 virtual device was created (before ANDROID_AVD_HOME was set) and "The emulator process for AVD Pixel_10 has terminated" on start. Diagnosis steps given: run the emulator from the command line to see the error; check hardware acceleration (emulator -accel-check, Task Manager shows Virtualization Enabled, Windows features Windows Hypervisor Platform); recreate the device after restarting Android Studio, with a Google Play image and 2048 MB RAM.
* M01 part 2 written by the AI on 2026-11-17 (not yet built or run): app.json (name Umeed, slug umeed, scheme umeed, light mode, package and bundle id com.umeed.app, android.googleServicesFile, plugins @react-native-firebase/app and @react-native-firebase/auth); run-android.cmd (selects JDK 17 only for the Android build, adds platform-tools to Path for that window; options clean and sha1); src/lib/api.ts (backend at EXPO_PUBLIC_API_URL, default http://10.0.2.2:8080, sends the Firebase token, reads the ApiError shape); src/lib/auth.ts; src/lib/login-store.ts (Zustand); src/lib/profile.ts (TanStack Query); src/components/ui.tsx; screens src/app/_layout.tsx, index.tsx (routing gate), login.tsx (+91 and 10 digits), verify.tsx (6-digit code), profile-setup.tsx, home.tsx (placeholder with log out). Forms use React Hook Form and Zod; form screens use the 'use no memo' directive because the template turns on the React Compiler.
* Packages installed by the AI on 2026-11-17 with npx expo install: expo-dev-client ~58.0.20, @react-native-firebase/app and /auth ^27.1.1, @tanstack/react-query ^5.107.1, zustand ^5.1.0, react-hook-form ^7.92.0, zod ^4.6.7, @hookform/resolvers ^7.0.7. Result: npx tsc --noEmit passed with no errors; npx expo-doctor 20/20 checks passed. npm audit now reports 9 issues (3 moderate, 6 high); do not run npm audit fix --force.
* A Pixel_7 virtual device now exists in H:\android-avd. The android folder has not been generated yet.
* Emulator diagnosis by the AI on 2026-11-17: the project owner reported it would not start. Hardware acceleration is fine (emulator -accel-check: WHPX installed and usable). Started from the command line, Pixel_7 (Android 16, API 36, Google Play 16 KB page size image, 2048 MB RAM, software graphics) did not crash. The first cold boot took about 11 minutes and then completed (sys.boot_completed = 1). Likely causes of the failed start in Android Studio: it was opened before ANDROID_AVD_HOME was set, or its start timed out. The PC has 7.9 GB RAM, so expect slow builds while the emulator runs.
* Added apps/mobile/start-emulator.cmd (starts Pixel_7 without Android Studio; option cold for a full restart).
* First build attempt on 2026-11-17: run-android clean stalled in npx expo prebuild at "Creating native directory" (after the template was unpacked, debug event prebuild:gitignore_check), twice, with the Expo process idle. Cause found: memory exhaustion, not Expo. Windows had 18.2 GB committed on 7.9 GB RAM, and the page file is F:\pagefile.sys (16.2 GB) on the same Toshiba hard disk as H: (disk 1, also E: and G:). That disk was 100% busy (queue around 20, about 120 random reads per second, matching the page read rate), so everything on H: (the project, node_modules, the emulator files, the Gradle cache) crawled. Largest memory users: vmmem 3.1 GB (the WSL virtual machine, although the Ubuntu distribution showed Stopped; Postgres runs natively as a Windows service, so WSL is not needed), Chrome 2.4 GB, IntelliJ IDEA 2.3 GB (open since 2026-11-12), the Pixel_7 emulator about 2 GB, Postman 0.5 GB. The half-generated android folder is left in place; run-android clean recreates it.
* To build on this PC: free memory first (wsl --shutdown; close IntelliJ, Postman and most Chrome tabs; the backend can run from a terminal with mvnw.cmd spring-boot:run), start the emulator only after the build has started or finished, or test on a real phone over USB, which needs no emulator memory. Longer term: more RAM, or move the page file and the project to the SSD once C: has space.
* Retry on 2026-11-17 after the project owner freed memory (committed memory 18.2 GB → 12.2 GB; vmmem still held 3 GB with no WSL distribution running): npx expo prebuild --clean --platform android finished in 28 seconds and generated the android folder. Debug signing key SHA-1 (the standard Expo template debug keystore): 5E:8F:16:06:2E:A3:CD:2C:4A:0D:54:78:76:BA:A6:F3:8C:AB:F6:25. The project owner added it to the Firebase Android app. The google-services.json in apps/mobile is still the 2026-11-15 file; for phone login that should be enough, because the SHA-1 is checked by Firebase and the file only changes for Google Sign-In.
* Decision 2026-11-17 (reverses the emulator decision above): test on a real Android phone connected by USB. The app's default backend address is now http://localhost:8080, and run-android.cmd runs adb reverse tcp:8080 tcp:8080 so the phone reaches the PC's backend over the cable. For the emulator, set EXPO_PUBLIC_API_URL=http://10.0.2.2:8080 in a .env file. First Gradle build started with gradlew app:assembleDebug -PreactNativeArchitectures=arm64-v8a.
* Build attempts on 2026-11-17 (app:assembleDebug for arm64-v8a, run from apps\mobile\android with JDK 17):
  * Attempt 1 failed after 32 minutes in react-native-firebase_auth:compileDebugKotlin: firebase-auth 25.0.0 (Firebase BoM 35.0.2, used by React Native Firebase 27.1.1) has Kotlin 2.4.0 metadata and the project compiled with Kotlin 2.2. React Native Firebase 27 requires the Kotlin Gradle plugin 2.4.0 or newer (rnfirebase.io/platforms and rnfirebase.io/migrating-to-v27). Fixed: expo-build-properties ~58.0.12 installed and app.json sets android.kotlinVersion to 2.4.0 (verified in the generated android/gradle.properties). The next build got past this step.
  * Attempt 2 failed in app:buildCMakeDebug[arm64-v8a]: ninja: "Filename longer than 260 characters" for the object file of react-native-gesture-handler/shared/.../RNGestureHandlerDetectorShadowNode.cpp. The object path (277 characters) embeds the full source path H_/Project/Umeed/apps/mobile/node_modules/... Installed: CMake 3.22.1 with Ninja 1.10.2; Windows LongPathsEnabled is 0.
  * A subst drive does not help: U: mapped to apps\mobile breaks Expo autolinking (its package.json search never checks a drive root), and U: mapped to apps (U:\mobile) still produced the H_/Project/... path, because part of the build resolves the real path. The subst mapping was removed. That run also showed "e: Daemon compilation failed" in expo-dev-launcher:compileDebugKotlin, cause not identified (no out-of-memory message in the log).
  * Agreed fix, not yet done: the project owner sets LongPathsEnabled to 1 (administrator, then restart Windows) and installs a newer CMake (3.31 or 4.x) in the Android Studio SDK Manager; the AI then checks that its Ninja is 1.12 or newer and sets android.cmakeVersion with expo-build-properties. The alternative is moving the project to a very short folder (for example H:\u), which must save at least 18 characters.
  * Decision 2026-11-17: the Android build is paused. The project owner wants the whole MVP user interface written first, then one build installed on the phone.
  * Attempt 3, 2026-11-18, SUCCEEDED: checked by the AI first: LongPathsEnabled is 1 and Windows was restarted after it (boot 2026-11-17 23:32); CMake 4.1.2 is installed in the SDK and its Ninja is 1.12.1. app.json now sets android.cmakeVersion 4.1.2 through expo-build-properties (verified in the generated android/gradle.properties). npx expo prebuild --clean --platform android, then gradlew app:assembleDebug -PreactNativeArchitectures=arm64-v8a with JDK 17: BUILD SUCCESSFUL in 26 minutes 41 seconds, app-debug.apk about 94 MB. Only warnings (a deprecated DefaultReactActivityDelegate constructor in the generated MainActivity.kt, Gradle 10 deprecations). Shipped on 2026-11-18 as a8ae8bd (backend tests, mobile type check and 39 mobile tests passed, pushed). That ship run showed a Jest warning "A worker process has failed to exit gracefully and has been force exited" after all tests passed; harmless for now, likely an open timer in a test (find with npx jest --detectOpenHandles when convenient). This index update is NOT committed yet; the next ship run commits it.
  * Installed on the phone on 2026-11-18: Xiaomi/Redmi 22120RN86I, Android 14, arm64-v8a. The first adb install failed with INSTALL_FAILED_USER_RESTRICTED; Xiaomi phones need Developer options, "Install via USB" turned on, and the install prompt accepted on the phone. Then adb install -r app-debug.apk succeeded. Ports forwarded with adb reverse tcp:8080 tcp:8080 (backend) and tcp:8081 tcp:8081 (Metro). Metro started with npx expo start --dev-client --localhost; first bundle 37 seconds, 1726 modules, no errors. The app opened on the Log in screen ("Welcome to Umeed", +91 field, Send code), verified by a phone screenshot. Then phone login with a Firebase test number worked (reported by the project owner); the app went straight to the Home feed, which showed "Nearest first" (location allowed) and the empty state "Nothing to claim right now" with Donate an item, verified by a screenshot. The floating gear button at the top right is Expo's development tools button, present only in development builds; it covers the "Nearest first" label. The other MVP steps on the phone are tested below as they happen.
  * How to run it again later (no rebuild needed while only JavaScript changes): start the backend; connect the phone; adb reverse tcp:8080 tcp:8080 and adb reverse tcp:8081 tcp:8081; in apps\mobile run npx expo start --dev-client --localhost; open Umeed on the phone. A rebuild (run-android) is needed only after native changes (app.json plugins, new native libraries).
  * Earlier note: adb showed no device on 2026-11-18 before the phone was connected.
  * Bug found on the phone on 2026-11-18, posting a donation: "Cannot reach the Umeed server", and the request never reached the backend. Cause: Expo SDK 58 replaces the global fetch with expo/fetch (node_modules/expo/src/winter/runtime.native.ts), whose multipart encoder rejects React Native's old { uri, name, type } file parts ("Unsupported FormDataPart implementation") before anything is sent. The unit tests mock fetch, so they could not catch it. Fix (small fix within M02, not separately approved): donations.ts appends each photo as new File(uri) from expo-file-system; expo-file-system ~58.0.14 added to package.json with npx expo install (its native module was already in the APK as part of expo, so no rebuild); the donationFormData test now checks that photos are File objects; api.ts logs the underlying network error with console.warn in development builds only. Type check passed, 39 tests passed. Escape hatch if expo/fetch causes other problems: EXPO_PUBLIC_USE_RN_FETCH=1 switches back to React Native's fetch.
  * Second problem the same day: the phone's USB connection kept resetting (adb transport_id reached 21), and every reset deletes the adb reverse port forwarding, so the app could not reach the backend or Metro and looked hung. Workaround used: a background loop that re-runs adb reverse for 8080 and 8081 whenever they disappear. Possible causes: the cable, the USB port, or MIUI USB power saving. Metro also stopped answering after npx expo install changed node_modules while it ran; restarting Metro fixed it (a rebuild of the bundle then took 1.7 seconds).
  * After the fix, posting worked on the phone (2026-11-18): donation "Mouse", AVAILABLE, Rewa, Shiv Nagar, coordinates rounded to 24.56, 81.32, one photo stored at 97 KB (shrunk on the phone). Verified in the database and the storage folder. NOT committed yet; the next ship run commits it.
  * Rest of the MVP flow on the phone, 2026-11-18, one phone with two Firebase test numbers (log out and in to switch): the project owner reported each step worked: Activity lists the item under Available; item details for the donor; the second account sees the item in the feed and claims it; the donor sees the claim, accepts it, and the item moves to Reserved; pickup details for both sides with address, name and phone; both handover confirmations. Database check by the AI: "Mouse" COMPLETED, claim ACCEPTED. Both test accounts have the display name Narendra, which makes the two sides hard to tell apart; renaming the second one is suggested.
  * Not yet tested on the phone: Report (the project owner skipped it; reporting was verified through the API on 2026-11-15), cancelling a reservation, withdrawing a claim, removing a listing, and photo swiping with several photos.
  * Small issue found by reading the code: donation/[id]/index.tsx showed "This item has already been reserved for someone." to every non-donor once the item was not AVAILABLE, including the accepted receiver. Fixed on 2026-11-18 (approved by the project owner): for a non-donor on a RESERVED or COMPLETED item, the screen asks GET /pickup without retries (usePickup gained enabled and probe options); a success means the user is the accepted receiver, who sees "This item is reserved for you. Call the donor to arrange the pickup." or "You received this item." with a Pickup details button. Everyone else sees "already been reserved for someone else" or "already been given away".
  * Second issue found on the phone the same day: My claims kept a completed item under "Accepted – arrange pickup", because a claim stays ACCEPTED after the handover and the claim response had no item status. Fix approved by the project owner and written: backend ClaimResponse gained donationStatus (the item's status; nothing private); DonationService.titlesByIds was replaced by summariesByIds (title and status; only the claims module used it); ClaimService fills donationStatus in every claim response. Tests: ClaimApiTests checks AVAILABLE on submit, RESERVED on accept and on the rejected claim; PickupApiTests checks that after both confirmations the receiver's claim is still ACCEPTED with donationStatus COMPLETED. Mobile: Claim type has donationStatus; groupMyClaims puts ACCEPTED claims on COMPLETED items in a new "Received" section after "Waiting for the donor"; the Activity row for a received item shows a "Received" pill, opens the item screen and has no Pickup details button; new format test. Result on 2026-11-18, run by the AI: backend mvnw test 38 tests, 0 failures; mobile npx tsc --noEmit no errors, npx jest --ci 40 tests passed. The running backend was restarted with the change. Checked on the phone by the project owner on 2026-11-18: "Mouse" is under Received in My claims, and its item screen says "You received this item." with Pickup details. API change: GET /api/v1/claims/mine, GET /donations/{id}/claims, claim submit, accept and withdraw responses now include donationStatus.
  * Pending phone tests, 2026-11-18, steps 1 to 9 passed (reported by the project owner): posting two items, one with 3 photos; sideways photo swipe and swipe up in the feed; claiming two items; Report with "Something else" needing details; a second report refused; withdrawing a claim. Not yet done: accept then donor cancel, remove listing, the claimant's view afterwards, receiver cancel.
  * Feed rule change requested by the project owner during that test and approved as a plan the same day (see section 2B): Home shows only items the user can claim. Backend: new public interface donations.ClaimedDonations (donation ids a user has claimed, any status), implemented by claims.ClaimedDonationsLookup, which uses only ClaimRepository.findDonationIdsByClaimantId; this keeps the dependency one way (claims to donations) and avoids a cycle with ClaimService. DonationRepository's feed queries were replaced by findClaimableNewestFirst (JPQL) and findClaimableNearestFirst (native, count query updated too); both exclude donor_id = caller and the claimed ids. An empty "not in" list is avoided by passing the all-zero UUID. DonationService.listFeed and listFeedNearest now take the caller's Firebase UID (requireActiveUser), and the controller passes the token subject. Tests: DonationApiTests now reads the feed as a second registered user and has a new test that the donor never sees their own item in either feed order; ClaimApiTests has a new test that a claimed item leaves the claimant's feed (both orders), stays in another user's, stays out after withdrawing, and can still be opened by id. App: feed.tsx no longer has own-item or claimed-item branches (no "Your item" pill, no See claims, no Claimed box); after a claim it shows a "Claimed … Find it under Activity." banner for 3.5 seconds while the refreshed feed drops the item; the empty-feed text points to Activity. Results on 2026-11-18, run by the AI: backend 40 tests, 0 failures; mobile type check no errors, 40 tests passed. Backend restarted with the change. Checked on the phone on 2026-11-18: the new empty-feed text shows, and Home is empty for both accounts, which is correct: the two available items (Table, Bottle) belong to one account and the other has claimed both (Table REJECTED after the donor cancelled the reservation, so Part 3 accept and donor cancel also worked; Bottle WITHDRAWN). Confirmed by the project owner. Still to test on the phone: remove listing, receiver cancel, and the "Claimed" banner with a fresh item.
  * Consequence for the test steps: claiming a withdrawn item again from Home is no longer possible because the item is not shown there; the server still refuses it and a backend test covers it.
  * First ship run of this work (2026-11-18) failed on one test: PickupApiTests line 254 expected Asha first in the donor's claim list but got Bilal. Cause (present since B04/B05, not from this change): the list is ordered by created_at only, and two claims made a fraction of a millisecond apart can get the same timestamp on Windows, so their order was random. The AI's own runs had passed by chance. Fix: ClaimRepository.findByDonationIdOrderByCreatedAtAscIdAsc (claim id as tie-breaker, so the order is stable between calls); the tests in ClaimApiTests and PickupApiTests now find each claim by claimant name instead of by position. Backend tests then run three times in a row by the AI: 40 tests, 0 failures each time.
  * Shipped on 2026-11-18 in two commits, both pushed: a9fb06c "Fix photo upload; show received items in My claims" (photo upload fix, receiver text, Received section) and a5d77b4 "Fix photo upload; Received section; Home shows only claimable items" (feed rule, claim order fix, index; 12 files). The a5d77b4 ship run passed the backend tests, the mobile type check and 40 mobile tests. This index update (recording the commits) is NOT committed yet; the next ship run commits it.
  * Found on the phone right after: "Remove listing" could not be reached. My donations rows opened the claims list (available) or pickup details (reserved, given away), and the item screen with Remove listing was reachable only by tapping one's own item on Home, which the new feed rule removed. Fix (small consequence fix): in activity.tsx, My donations rows now open the item screen /donation/{id}, which has See claims and Remove listing while available and Pickup details afterwards. Type check no errors, 40 mobile tests passed. Checked on the phone on 2026-11-18: "Bottle" opened from My donations, Remove listing worked and it left the list (reported by the project owner). Shipped as 09aa790 on 2026-11-18. Still to test on the phone: receiver cancel and the "Claimed" banner.
  * Report on 2026-11-18 that a new item did not appear as expected: database check showed no bug. The two test accounts are Dharmendra (+917389965193) and Test Receiver (+916267641951). The new "Table" (14:05) had been posted from Test Receiver, and Dharmendra had already claimed it (PENDING), so it was correctly absent from both Home screens. The listing removed earlier was Dharmendra's older "Table", not "Bottle" (Bottle is still AVAILABLE). Showing the logged-in name on Home was offered; the project owner declined.
  * Last phone checks on 2026-11-18, reported working by the project owner and confirmed in the database: "Iphone" posted by Dharmendra appeared on Test Receiver's Home and was claimed there (PENDING; the "Claimed" banner flow); on "Table" (by Test Receiver) the donor accepted Dharmendra's claim and Dharmendra cancelled the reservation, so the claim is WITHDRAWN and Table is AVAILABLE again. With this, every MVP flow in section 2A has been run on a real Android phone: login, profile, post with photos, Home feed (claimable items only, nearest first, photo swipe), claim, withdraw, donor sees claims, accept, pickup details with address and phone for both sides, both confirmations completing the item, donor cancel, receiver cancel, remove listing, report and duplicate report.
  * That ship run printed a long yellow console.warn block from api.test.ts (the network-failure test triggers the development-only warning added to api.ts). Not a failure. The test now silences console.warn and checks that the warning is logged with the cause. Type check no errors, 40 tests passed, no warning output. Shipped as 67bdbf0 on 2026-11-18 together with the full-MVP-test notes (40 backend tests, 40 mobile tests, clean output).
  * Plan proposed on 2026-11-18 for "run Umeed without the PC" (not approved yet; waiting for the project owner's decisions on hosting, domain and testers): H01 decisions; H02 backend production readiness (Cloudflare R2 photo storage behind FileStorage, prod settings); H03 server setup and deploy (HTTPS, daily database backups); M03 standalone Android release build pointing at the hosted backend. Recommended hosting: one small VPS in India (DigitalOcean Bangalore or similar, about $12 to $18 a month) running the backend and PostgreSQL, with photos on Cloudflare R2 (10 GB free). Alternative: Google Cloud Run (Mumbai) plus Neon Postgres, cheaper at low traffic but with slow cold starts for Spring Boot. Prices from third-party pages on 2026-11-18, to be checked on the providers' own pages before buying. SMS warning: Firebase phone sign-in costs about $0.07 per SMS to India beyond 10 free per day (Blaze plan required), so real-number logins can exceed the monthly budget quickly; fine for a few testers.
  * Follow-up the same day: the project owner asked about Hostinger. Answer: buy a Hostinger KVM VPS, not "Web hosting" or "Cloud hosting" (those cannot run the Java backend). Suggested: KVM 1 (1 vCPU, 4 GB RAM, 50 GB disk), about ₹599/month on a 24-month term, renewing at about ₹999/month, plus 18% GST (third-party figures, check the cart); India location; plain Ubuntu 24.04 LTS, no control panel; skip add-ons. Domain also from Hostinger. Photos still on Cloudflare R2. The server password must never be sent to the AI.
  * Play Store findings (2026-11-18): Play Console account, about $25 one time. A personal account created after 13 November 2023 must run a closed test with at least 12 testers opted in for 14 consecutive days before production access (organization accounts are exempt). Missing for Play: in-app and web account deletion, a privacy policy page, an AAB release build, the data safety form, content rating and store listing. Real-number logins for testers need the Firebase Blaze plan.
  * Updated order agreed in discussion, NOT approved as a batch yet: H02 backend production readiness, H03 Hostinger VPS setup and deploy, M03 standalone Android build, M04 Play readiness (account deletion, privacy policy, AAB, listing drafts), then the 14-day closed test with 12 testers.
  * (Superseded on 2026-11-18, see below.) Paused earlier waiting for the Hostinger VPS, domain and tester decisions.
  * NEW DIRECTION (project owner, 2026-11-18): build the whole application first, then host. Approved order of feature batches: F01 account and safety basics (delete account, block a user), F02 push notifications, F03 in-app chat, F04 need requests and offers, F05 explore (search, filters, map, remembering skipped items), F06 admin dashboard (separate Next.js web app: reports, remove listings, suspend users, statistics), F07 Hindi. Then hosting as before: H02, H03, M03, M04 (M04 now also includes the web page for deletion requests). Each batch is still proposed in detail and approved before writing. Decisions to take when reached: F03 live updates or polling (AI recommends polling first); F05 map provider; F06 admin login. iOS stays out of scope.
  * Batch F01, approved on 2026-11-18 (migration V9 shown in full first) and written by the AI. V9 (user_blocks table; donations.pickup_address may be null) is exactly the migration that was shown.
    * Delete account: DELETE /api/v1/users/me in a new account module (AccountService, AccountController). Refused with 409 while the caller has a pickup in progress, as donor or receiver. Otherwise, in one transaction: pending claims withdrawn first (so none can be accepted afterwards), claim messages erased, available listings removed (their claims closed by the usual event), all pickup addresses erased, photos of all removed listings deleted (database rows now, files only after the commit), blocks removed both ways, and the user row cleared (Firebase id, phone, name null; status DELETED; deleted_at set). Handed-over items keep their photos and show "Umeed user". Reports the user filed are kept. The same number can register again as a new account.
    * The app (delete-account screen, reached from Profile) asks for a fresh SMS code first (Firebase only deletes a recent sign-in; Android may verify the code by itself, detected with onIdTokenChanged and lastSignInTime), then calls the server, then deletes the Firebase sign-in and signs out. If Firebase cannot delete the sign-in, the user is signed out anyway.
    * Block: new blocks module (UserBlock, UserBlockRepository, BlockService, BlockController, BlockedUsersLookup). The users module has a BlockedUsers interface, implemented by the blocks module, so donations and claims can use it without a dependency cycle (same pattern as ClaimedDonations). A block works both ways: the feed (both orders) leaves out each other's items, claiming is refused with 403, and open claims between the two are closed (the blocked person's claims on the blocker's items REJECTED, the blocker's own claims WITHDRAWN). Blocking is refused with 409 while the two share a pickup. Blocking twice is harmless. Unblocking shows items again; closed claims stay closed.
    * App: "Block {name}" on the item screen (blocks the donor) and on each row of the claims list (blocks the claimant); Profile has "Blocked people" (list with Unblock) and a "Delete my account" link.
    * Tests: BlockApiTests (4) and AccountDeletionApiTests (4); 48 backend tests passed on 2026-11-18. Mobile: account-and-blocks.test.ts (6); type check no errors; 46 mobile tests passed. Running the backend tests applied V9 to umeed_dev; it must never be edited.
    * Mistake fixed during the batch: running "npx expo lint" installed ESLint and created eslint.config.js without approval. Both were reverted at once (package.json and package-lock.json restored from Git, npm install, config file deleted). Lint is not set up in this project; adding ESLint needs approval.
    * No new libraries and no native change: no rebuild is needed. Each deletion test sends one real SMS.
  * F01 shipped on 2026-11-18 as df214cc (ship passed: backend tests, mobile type check, mobile tests). The phone test is postponed by the project owner ("we will test later"): block from an item and from claims, the feed hides items both ways, unblock, delete a test account with the SMS code, log in again with the same number as a new account.
  * Batch F02, approved on 2026-11-18 (the two libraries and migration V10 shown first) and written by the AI. V10 (device_tokens table) is exactly the migration that was shown.
    * New libraries: mobile @react-native-firebase/messaging ^27.1.1 (installed with npx expo install, which also added its config plugin to app.json); backend com.google.auth:google-auth-library-oauth2-http 1.48.0. app.json also lists android.permission.POST_NOTIFICATIONS. NATIVE CHANGE: the app must be rebuilt (run-android) and reinstalled before the phone test.
    * Backend notifications module: DeviceToken, DeviceTokenRepository, DeviceService and DeviceController (PUT /api/v1/devices registers a phone and moves its token if another user had it; POST /api/v1/devices/unregister removes it, only for its owner), Notice, PushSender (FcmPushSender: FCM HTTP v1 signed with the service-account key; LoggingPushSender when no key), PushConfig, PushDispatcher (queues sending only after the transaction commits, sends on a small background executor of its own, deletes tokens Firebase reports as unregistered, never throws), NotificationListener (builds the texts; never notifies the actor or anyone blocked either way).
    * Events published inside the existing transactions: ClaimSubmittedEvent and ClaimAcceptedEvent (claims module), PickupCancelledEvent and HandoverConfirmedEvent (pickup module). Notifications: new claim to the donor (opens claims), accepted to the receiver (opens pickup), not chosen to the other claimants (opens the item), pickup cancelled to the other person (opens the item), first handover confirmation to the other person (opens pickup). The second confirmation, which completes the item, sends nothing. Texts never contain phone numbers or addresses.
    * Account deletion now also removes the user's device tokens.
    * Setting: umeed.notifications.credentials-file=${UMEED_FIREBASE_CREDENTIALS:}. Empty means log only, which is what tests and normal development use. A missing or broken key never stops the backend from starting. The key file must stay outside the project folder and must never be sent to the AI. The root .gitignore now also ignores *firebase-adminsdk*.json and umeed-firebase-key*.json as a safety net.
    * App: lib/push.ts (registers the phone after reaching the tabs, re-registers on token refresh, unregisters just before logging out, refreshes data and shows a banner for notifications that arrive while the app is open, opens the right screen when one is tapped, including from a closed app), components/push-banner.tsx (shown from the root layout). Android 13 and later ask for notification permission once.
    * Tests: NotificationApiTests (5, with the dispatcher mocked), PushDispatcherTests (3, real transactions that are always rolled back), NotificationListenerTests (1, plain unit test of the block rule); 57 backend tests passed on 2026-11-18. Mobile push.test.ts (6); type check no errors; 52 mobile tests passed. Running the tests applied V10 to umeed_dev; it must never be edited.
    * Small fix during the batch: DeviceService flushes after moving a token to another user (the test reads the table with plain SQL inside the same transaction).
    * Before real notifications can be sent, the project owner sets up the key (steps given in chat on 2026-11-18): Firebase console, Project settings, Service accounts, Generate new private key; save it OUTSIDE the project (for example H:\secrets\umeed-firebase-key.json); set the user environment variable UMEED_FIREBASE_CREDENTIALS to that path; restart the backend. The startup log then no longer says "notifications are written to the log only".
  * F02 shipped on 2026-11-18 as 5b448a1 (ship passed). An earlier screenshot showed the F01 ship banner, not F02; Git confirmed F02 was still uncommitted before this run.
  * Firebase key set up by the project owner on 2026-11-18: the key file is at H:\secrets\umeed-firebase-key.json (outside the project; the AI checked only that the file exists, never its contents) and the user environment variable UMEED_FIREBASE_CREDENTIALS points to it (set with setx in Command Prompt; only windows opened afterwards see it). The backend must be started from a new window to use it.
  * Batch F03, approved on 2026-11-18 (migration V11 shown in full first; polling chosen over live updates; a fifth tab "Chats") and written by the AI. V11 (chat_messages table) is exactly the migration that was shown. No new libraries, no native change.
    * Backend chat module: ChatMessage, ChatMessageRepository, ChatService, ChatController, ChatResponses (Message, Thread, Conversation), SendMessageRequest, ChatMessageSentEvent. A conversation belongs to one accepted claim (claimId). Readable only by the donor and that claimant (others 403), and only if the claim is accepted now or messages exist (else 404). Writable only while the claim is ACCEPTED and the item RESERVED (else 409); read-only after handover or cancellation. Body 1 to 1000 characters. GET .../messages?after={messageId} returns only newer messages (400 if the id is not in this conversation). Unread = the other person's messages without read_at; POST .../read marks them.
    * Claims module additions: ClaimParties record, ClaimService.partiesOf, claimIdsInvolving, acceptedClaimId. PickupResponse now has claimId (the accepted claim), used by the Message button.
    * Notification on a new message to the other person: "{name} sent you a message" / "About “{title}”. Open Umeed to reply." with screen chat and claimId. The message text is never in a notification.
    * Account deletion also deletes the user's own messages; the other person's messages stay, shown with "Umeed user".
    * App: Chats tab (5 tabs now; unread badge on the tab, list refreshed every 30 seconds), chat screen /chat/{claimId} (checks every 5 seconds, downloads only newer messages, marks read, read-only notice when closed), "Message {name}" button on Pickup details, notification taps open the chat. lib/chat.ts holds the helpers.
    * Tests: ChatApiTests (4); 61 backend tests passed on 2026-11-18. Mobile chat.test.ts (6) and one more push test; type check no errors; 59 mobile tests passed. Running the tests applied V11 to umeed_dev; it must never be edited.
    * Note for later: typed routes are regenerated only by a running dev server on a FREE port 8081; if an earlier Metro still holds the port, the new one fails silently and tsc reports unknown routes. Read receipts are stored but not shown in the app (polling fetches only newer messages, so they would go stale).
  * F03 shipped on 2026-11-18 as 949d2a1 (ship passed).
  * Batch F04, approved on 2026-11-18 (migration V12 shown in full first) and written by the AI. V12 (need_requests and need_offers) is exactly the migration that was shown. No new libraries, no native change.
    * Backend needs module: NeedRequest, NeedOffer, their repositories, CreateNeedRequest, CreateOfferRequest, NeedResponse (pendingOfferCount only for the person who asked), OfferResponse (with donationStatus and first photo), AcceptOfferResponse, NeedService, NeedController, NeedOfferMadeEvent, NeedOfferAcceptedEvent. Needs: OPEN, FULFILLED, CLOSED; location rounded to 2 decimals like items; no address. Offers: PENDING, ACCEPTED, DECLINED, WITHDRAWN; only the donor's own AVAILABLE items; one offer per item per need.
    * Choosing an offer (POST /api/v1/offers/{id}/accept, person who asked only, need locked): ClaimService.acceptForNeed reserves the item through an accepted claim for the person who asked (creating one, or reusing an earlier claim of theirs on that item), rejects the item's other pending claims exactly like accept(), then the offer is ACCEPTED, the other waiting offers DECLINED and the need FULFILLED. 409 before anything is written if the item is no longer available. Pickup details, chat and both confirmations then work as for any claim. accept() and acceptForNeed share finishAcceptance.
    * ClaimAcceptedEvent has a new field notifyReceiver (false when the receiver chose an offer themselves).
    * Removing a listing withdraws its waiting offers (listener on DonationRemovedEvent). Blocking declines or withdraws waiting offers between the two and hides needs both ways (list, nearest list, details 404, offering 403). Account deletion closes the user's open needs (their waiting offers declined) and withdraws the user's waiting offers.
    * Notifications: new offer to the person who asked (opens the need), offer chosen to the donor (opens pickup), other waiting offers' donors told another was chosen (opens the need); claimants of the item who were not chosen get the usual message.
    * App: Home has an Items / Needs switch (needs list, nearest first); Donate has Give an item / Ask for something (NeedForm); need screen /need/{id} (offers with Choose, Close, or Offer an item, Block); offer screen /need/{id}/offer (choose one of my available items plus a message); Activity has a third view "Needs" (What I asked for, What I offered, with Withdraw); notification taps open needs. lib/needs.ts holds the helpers.
    * Tests: NeedApiTests (8); 69 backend tests passed on 2026-11-18, first run. Mobile needs.test.ts (7) and one more push test; type check no errors; 67 mobile tests passed. Running the tests applied V12 to umeed_dev; it must never be edited.
    * Known limits: the offer screen lists only the first 20 of the donor's items; a need cannot be reported yet (planned with the admin dashboard, F06).
  * F04 shipped on 2026-11-18 as 9107613 (ship passed).
  * Batch F05, approved on 2026-11-18 "with map" (migration V13 shown in full first; Google Maps through react-native-maps) and written by the AI. V13 (donation_skips) is exactly the migration that was shown.
    * New library: react-native-maps 1.29.11 (npx expo install). NATIVE CHANGE: rebuild needed. New file apps/mobile/app.config.js: keeps everything in app.json and only adds the react-native-maps plugin with androidGoogleMapsApiKey from the environment variable GOOGLE_MAPS_ANDROID_API_KEY (empty means the app builds and runs but the map stays blank). The key is read when the app is built (run-android), not at run time.
    * Backend: GET /api/v1/donations/search (q, categoryId, city, lat+lng, maxKm 1 to 500 needing a location; same exclusions as Home except "Not interested"; nearest first with a location, newest first without). Optional filters are passed as neutral values, never null ('%' patterns, empty category, maxKm 100000 for "any"), because PostgreSQL cannot type null parameters in native queries. Words are matched in order with ILIKE; %, _ and \ in user input are escaped (DonationSearch.likePattern). POST /api/v1/donations/{id}/skip ("Not interested", idempotent, 404 if unknown or removed) and DELETE /api/v1/donations/skips (clear). Home (both orders) leaves out skipped items; search still finds them. Account deletion forgets the user's skips. New classes DonationSkip, DonationSkipRepository, DonationSearch.
    * App: search screen /explore (opened from "🔍 Search" on Home): words with a short typing pause, filters (category, distance 2/5/10/25 km or any, city), List / Map switch; map pins at the approximate location, items sharing a point spread on a ~150 m circle (spreadPins), callout opens the item. "Not interested" link on each Home item (notice: show it again from Profile). Profile: "Show hidden items again". lib/explore.ts holds the helpers.
    * Tests: ExploreApiTests (5); 74 backend tests passed on 2026-11-18 (the first run failed only because MockMvc does not decode "+" or an escaped %20 in a hand-written query string; the test now uses .param). Mobile explore.test.ts (5); type check no errors; 72 mobile tests passed. Running the tests applied V13 to umeed_dev; it must never be edited.
    * Google Maps key, to be set up by the project owner (steps given in chat on 2026-11-18): Google Cloud console, the Firebase project, enable "Maps SDK for Android" (Google asks for a billing account; map loads in mobile apps are free), create an API key restricted to Android apps (package com.umeed.app plus the debug SHA-1 from run-android sha1) and to the Maps SDK for Android, then setx GOOGLE_MAPS_ANDROID_API_KEY in Command Prompt, open a new window and rebuild with run-android. The release build (M03) will need the release SHA-1 added to the same key.
  * F05 shipped on 2026-11-18 as 3cd5771 (ship passed). The project owner then paused for a break.
  * Batch F06 (admin dashboard), approved on 2026-11-18 ("Approve F06"). Split into F06A (backend admin API plus reporting a need) and F06B (Next.js admin web app, apps/admin). Admin login decided: Firebase phone login on the web; the backend accepts only users whose users.role is ADMIN (read from the database, never from the token). The project owner makes themself admin with one SQL UPDATE on their own phone number (command given when F06B is ready).
  * Batch F06A, written by the AI on 2026-11-18. No new libraries.
    * V14__admin_actions_and_need_reports.sql: reports.donation_id optional, new reports.need_id (FK need_requests), reviewed_by, reviewed_at, review_note; check that exactly one of donation_id and need_id is set; one report per person per need (partial unique index); new table admin_actions (admin_id, action, target_type, target_id, note, created_at) with checks on action and target type.
    * Reports: POST /api/v1/needs/{id}/reports (same body and rules as listings: not your own need 403, once per person 409, OTHER needs details). Report.aboutDonation / aboutNeed; ReportResponse gains needId. ReportService.review, resolveOpenForDonation, resolveOpenForNeed for the admin module. NeedSummary and NeedService.getSummary (public).
    * New admin module (com.umeed.admin): AdminController, AdminService, AdminQueries (read-only SQL across tables with NamedParameterJdbcTemplate; every change still goes through the owning module's service), AdminAction, AdminActionRepository, AdminViews (records). Every endpoint calls UserService.requireAdmin (active user with role ADMIN, else 403 "Admins only").
    * Endpoints under /api/v1/admin: GET me, GET stats, GET reports?status=OPEN|REVIEWED|DISMISSED (open oldest first, handled newest first; each row has the listing or need title and status, owner, reporter and the number of reports on that target), POST reports/{id}/review {decision REVIEWED|DISMISSED, note optional} (409 if already handled), GET donations/{id} (any status, with its reports), POST donations/{id}/remove {note required} (available only, 409 otherwise; same effects as the donor removing it; its open reports become REVIEWED with the note), GET needs/{id}, POST needs/{id}/close {note required} (open only; waiting offers declined; open reports REVIEWED), GET users?q=&status= (name or phone fragment, LIKE wildcards escaped, deleted accounts left out), GET users/{id} (profile including phone number, counts, last 20 admin steps about them), POST users/{id}/suspend and /unsuspend {note required}, GET actions?targetId= (admin log, newest first). Every change writes one admin_actions row.
    * Suspension: UserService.suspend / unsuspend (admins cannot be suspended, 403; only an active account, 409). Suspended users' listings and needs are left out of Home, search and the needs list (they are added to the hidden owners next to blocked people), and come back when the suspension is lifted; nothing is deleted. GET /users/me still answers with status SUSPENDED; everything else answers 403 "Account is not active".
    * App: Report link on the need screen (new route /need/{id}/report; the report form is now the shared components/report-form.tsx used by both report routes); the start screen shows "Account suspended" with Check again and Log out; the 403 "Account is not active" is shown everywhere as a clear suspension message (lib/api.ts isAccountNotActive, SUSPENDED_MESSAGE).
    * Tests: AdminApiTests (8) and one need-report test in ReportApiTests; 83 backend tests passed on 2026-11-18 (the first run failed only in AdminApiTests, because the test set the admin role with plain SQL while Hibernate still held the cached user; the test now flushes and clears). Mobile reports-and-suspension.test.ts (3); typed routes regenerated; type check no errors; 75 mobile tests passed (jest printed its usual note about a worker not exiting cleanly). Running the tests applied V14 to umeed_dev; it must never be edited.
    * Known limits: an admin cannot remove a reserved listing (its two people finish or cancel the pickup; suspending the donor stops them acting); removing a listing sends no notification to its claimants (their claims are rejected as when the donor removes it); a suspended user's pending claims on other people's items stay pending.
  * F06A shipped on 2026-11-18 as 7650427 (ship passed).
  * Batch F06B (admin web app), versions approved on 2026-11-18 ("Approve versions") and written by the AI.
    * New project apps/admin, installed with plain npm install (no create-next-app, no ESLint, no UI or test library): next 16.4.6, react and react-dom 19.3.1, firebase 13.4.0; dev: typescript ~6.0.3 (7.0.2 is the new native compiler and was not used, to match the mobile app), @types/react and @types/react-dom 19.3.0, @types/node 26.6.6. npm 11 did not run the install scripts of @firebase/util and protobufjs (not approved, and the build and the login page work without them). npm audit reports 4 high issues, all in @grpc/grpc-js under Firestore, which the dashboard does not import; do not run npm audit fix --force (it would downgrade firebase to 9).
    * next.config.ts forwards /api/* to the backend (UMEED_API_URL, default http://localhost:8080), so the browser talks to one address and the backend needs no CORS; it also sends no-frame, nosniff, same-origin referrer and noindex headers. Photos are fetched with the token and shown from temporary blob URLs.
    * Login: Firebase phone login on the web with an invisible reCAPTCHA (src/app/login). Firebase Web app config goes in apps/admin/.env.local (template .env.example; .env.local is git-ignored and must not be pasted to the AI). Without it the login page explains what to set up.
    * Pages (all client-side, inside src/app/(dashboard)/layout.tsx, which sends visitors to /login and shows only a refusal to non-admins; the backend checks the role again on every call): / overview (numbers, open reports first), /reports (Open / Reviewed / Dismissed, Mark reviewed or Dismiss with an optional note), /donations/{id} (photos, Remove listing with a required reason, its reports), /needs/{id} (Close need with a reason, its reports), /users (search by name or phone, status filter), /users/{id} (counts, Suspend or Lift suspension with a reason, admin steps about them), /log (admin log).
    * ship.cmd now also runs the admin build (npm run build, which type-checks) with Next telemetry turned off for that run.
    * Checked by the AI on 2026-11-18: npm run build passed (type check clean, 9 routes); next start opened / which went to /login and showed the setup message; no console errors. Not checked: real login and the pages with data (needs .env.local and an admin account).
    * Run it: start the backend, then in apps/admin run npm run dev and open http://localhost:3000.
    * Make an account admin (project owner only; log in to the mobile app with that number at least once first, so the user exists), in cmd: "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U umeed_app -d umeed_dev -c "UPDATE users SET role = 'ADMIN' WHERE phone_number = '+91XXXXXXXXXX';"
    * Firebase console setup (project owner): Project settings > Your apps > Add app > Web; copy apiKey, authDomain, projectId and appId into .env.local; localhost is an authorized domain by default (the production domain must be added at hosting time, H03).
  * Machine note (2026-11-18): drive C: was found completely full (0 GB free). Largest folders measured: Android SDK 20.4 GB, user Temp 5.0 GB, .gradle 2.6 GB, Google 1.9 GB, npm-cache 1.5 GB, .m2 0.9 GB. Nothing was deleted by the AI; the project owner decides what to clear.
  * RESUME HERE (2026-11-18): F06B written and built, NOT YET SHIPPED; the project owner frees space on C:, then runs ship "F06B: admin web app". Next: F07 (Hindi). Phone tests of F01 to F06 and a real dashboard login are postponed; F02 and F05 need the rebuilt app (run-android), F05's map needs the Google Maps key, and the tests use two phones or a phone plus the emulator.
  * End of the phone session on 2026-11-18: the USB port-forwarding loop and Metro were stopped so the phone could be unplugged; the backend was left running. The installed app is a development build: it opens only while Metro runs on the PC and the phone is connected (see "How to run it again later" above). Next: connect the phone by USB with USB debugging on, check with adb devices, start the backend, then run-android from apps\mobile (no clean needed; it reuses the built android folder) and do the manual MVP checks.
* GRADLE_USER_HOME is H:\gradle-cache. C: has only 4.2 GB free. Unused system images on C: could be removed in the Android Studio SDK Manager: android-37.1 (8.2 GB) and android-36.1 (2.8 GB). Pixel_7 uses android-36, which must stay.
* Then: npx tsc --noEmit; run-android clean (generates the android folder); run-android sha1 and add that SHA-1 to the Firebase Android app, download google-services.json again into apps/mobile; run-android clean again; log in on the emulator with a Firebase test number while the backend runs.

Mobile setup facts checked on 2026-11-17 (they change often, check again if a step fails):

* Expo: SDK 58 was being introduced; npx create-expo-app@latest without a template still created SDK 57, and --template default@sdk-58 creates SDK 58. The default template has Expo Router and TypeScript plus example screens. Source: docs.expo.dev/more/create-expo
* React Native Firebase does not run in Expo Go. Needs expo-dev-client, the plugins @react-native-firebase/app and @react-native-firebase/auth in app.json, then npx expo prebuild --clean and npx expo run:android. Sources: docs.expo.dev/guides/using-firebase, rnfirebase.io/auth/phone-auth
* React Native Firebase v26 (July 2026) removed the old namespaced API (firebase.auth()); only the modular API (getAuth, signInWithPhoneNumber) remains, and it requires the React Native New Architecture.
* Phone auth on Android: add the debug signing key's SHA-1 to the Firebase Android app. Test phone numbers sign in without an SMS.
* Local Android builds on Windows: JDK 17, ANDROID_HOME, Android SDK Platform 36 (Android 16), USB debugging on the phone. Source: docs.expo.dev/get-started/set-up-your-environment
* The phone reaches the backend through the PC's Wi-Fi address, not localhost. Windows Firewall may ask to allow Java on private networks.

Manual check with a real token (cmd). Get FB_TOKEN with the recipe below first, and keep the backend running in a second window (cd /d H:\Project\Umeed\backend, then mvnw.cmd spring-boot:run).

* 1. Profile: curl -s -i http://localhost:8080/api/v1/users/me -H "Authorization: Bearer %FB_TOKEN%" – expect 200 and displayName Narendra.
* 2. Post a donation with a real photo (replace the photo path with any JPG or PNG under 5 MB; the categoryId is the Clothes category):
  curl -s -i -X POST http://localhost:8080/api/v1/donations -H "Authorization: Bearer %FB_TOKEN%" -F "categoryId=e063e312-9fc5-4fda-967b-c88ebe074d15" -F "title=Study table" -F "description=Wooden study table in good shape" -F "itemCondition=GOOD" -F "city=Indore" -F "area=Vijay Nagar" -F "latitude=22.7535" -F "longitude=75.8937" -F "pickupAddress=Flat 12, Test Residency, Indore" -F "photos=@C:\path\to\photo.jpg"
  Expect 201, status AVAILABLE, latitude 22.75, one address in imageUrls, and no pickupAddress in the reply.
* 3. Feed: curl -s http://localhost:8080/api/v1/donations -H "Authorization: Bearer %FB_TOKEN%" – expect the new item in items.
* 4. Photo: curl -s -o "%TEMP%\umeed-check.jpg" http://localhost:8080 followed by the address from imageUrls, with the same Authorization header, then open that file – expect the same picture. The stored copy is in the umeed-storage folder inside the Windows user folder.
* 5. Mine: curl -s http://localhost:8080/api/v1/donations/mine -H "Authorization: Bearer %FB_TOKEN%" – expect the item.
* 6. Own claim refused: curl -s -i -X POST http://localhost:8080/api/v1/donations/DONATION_ID/claims -H "Authorization: Bearer %FB_TOKEN%" – expect 403 "You cannot claim your own donation".
* 7. Without a token: curl -s -i http://localhost:8080/api/v1/donations – expect 401.
* 8. Claim, accept and pickup need a second user. Add a second test phone number in the Firebase console (Authentication, Sign-in method, Phone, Phone numbers for testing), get a second token in another window, set a display name for that user with PUT /api/v1/users/me, claim with the second token, accept and read /pickup with the first.
* 9. Clean up: DELETE /api/v1/donations/DONATION_ID removes the test listing from the feed (only while it is AVAILABLE).

How to get a Firebase ID token for testing (all in cmd; the Web API Key, the token and passwords must not be pasted to the AI):

* A. Second window, start the backend: cd /d H:\Project\Umeed\backend, set UMEED_DB_PASSWORD=..., cls, mvnw.cmd spring-boot:run. Wait for "Started BackendApplication". Internet is needed.
* B. First window, get a token:
  * set FB_KEY=web api key
  * set FB_PHONE=test number with +91
  * set FB_CODE=test code
  * cls
  * curl -s -X POST "https://identitytoolkit.googleapis.com/v1/accounts:sendVerificationCode?key=%FB_KEY%" -H "Content-Type: application/json" -d "{\"phoneNumber\":\"%FB_PHONE%\"}"
  * set FB_SESSION=the sessionInfo value from the reply (it expires after a few minutes)
  * curl -s -X POST "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPhoneNumber?key=%FB_KEY%" -H "Content-Type: application/json" -d "{\"sessionInfo\":\"%FB_SESSION%\",\"code\":\"%FB_CODE%\"}" -o "%TEMP%\fb_token.json"
  * for /f "delims=" %i in ('powershell -NoProfile -Command "(Get-Content $env:TEMP\fb_token.json | ConvertFrom-Json).idToken"') do set FB_TOKEN=%i
  * echo %FB_TOKEN:~0,10%   (expect eyJhbGciOi)
* C. Use it: curl -s -i http://localhost:8080/api/v1/users/me -H "Authorization: Bearer %FB_TOKEN%". A token lasts about one hour. Afterwards: del "%TEMP%\fb_token.json".

Open points:

* The AI's shell on the PC cannot start because of low disk space on the PC, so commands are run by the project owner and pasted back.
* The AI's own environment cannot reach Maven Central, so it cannot check dependency names or versions itself.
* When the AI rewrites a file and transfers it immediately, the transfer can pick up the previous version. The AI waits before transferring and checks the file size on disk after every write.
* PostGIS has not been installed or checked yet.
* There are no automated tests for the categories or users modules yet. Only the generated context-load test exists. All verification so far is by hand.
* UMEED_DB_PASSWORD must be set again in every new command window before running the backend.
* Whether the GitHub repository is Private was not confirmed to the AI.
* Design shortcut in claims: when a reservation is cancelled, the claims to reopen are found by their timestamp, because accepting a claim stamps the accepted claim and the claims it rejects with the same moment. It avoids a new status and a new column, and a test covers it, but a dedicated column would be more explicit. Consider when the claims table next needs a migration.
* A completed donation's pickup details stay readable by the two parties. Consider hiding them after some time.
* Not verified: Firebase SMS pricing and whether a billing plan upgrade is needed before real SMS can be sent.
* A name such as " A " passes the 2 character check and is stored as "A" after trimming. Accepted as harmless for now.

Earlier session, 2026-11-14:

Completed:

* Verified and committed the users table (018).
* Added Spring Security and Firebase token settings (020, 022).
* Added the security configuration (023).
* Built the users module (024 to 028), including GET /api/v1/users/me.
* Set the Firebase SMS region policy to allow India and confirmed the test phone number works through the REST API.
* Updated this index (029).

Earlier session, 2026-11-13:

Completed:

* Project owner created the Firebase project. Project ID: umeed-2e018.
* File 018, V3__create_users.sql, approved and created. Not yet run.
* File 020 proposed (not approved).
* Updated this index (021).
* Session ended by the project owner after file 021.

Earlier session, 2026-11-12:

Completed:

* Committed the 005 index update (e6f7621).
* Confirmed PostgreSQL 18.6 and created the umeed_app login and the umeed_dev database.
* Added database settings to application.properties (006, e3237c9).
* Added Spring Data JPA and the PostgreSQL driver (007, f3115ad).
* Added Flyway (008, 4aafe5f).
* Added migrations V1 and V2 for categories (009, 9cdc432 and 011, a487145). Verified in the database.
* Decided the MVP category list and the plain JSON response shape.
* Built the categories module (012 to 016). GET /api/v1/categories returns the seven categories, verified in a browser.
* Updated this index (010, 017 and 019).
* Created the private GitHub repository Narendra0110/umeed and pushed all commits.
* Decided the OTP provider: Firebase Authentication (section 10A).
* Session ended by the project owner after file 019.

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

# 12A. HOW WE WORK (FROM 2026-11-15)

The one-file-at-a-time workflow was replaced by feature batches at the project owner's request. The full rules are in section 5 of UMEED_MASTER_INSTRUCTIONS.md (version 3.0). In short:

* The AI proposes one feature batch as a short plan. The project owner approves once. The AI writes all the files, including automated tests.
* Migrations and anything touching security rules or private data are shown in full before they are written.
* The project owner verifies with one command from the project root: ship "commit message". It runs the build and all tests and, only if they pass, commits everything and pushes.
* UMEED_DB_PASSWORD is set once with setx, so it no longer has to be typed in every window.
* Standing approval: the AI updates this index after each batch without asking and reports what changed. Changes to the master instructions still need explicit approval.
* Manual checks are requested only when a test cannot cover something.

Reviewing reports by hand in the database (still works; the admin dashboard, F06B, is the normal way now), in cmd:

* List open reports with the listing title and the reporter's name:
  "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U umeed_app -d umeed_dev -c "SELECT r.id, r.created_at, r.reason, r.details, d.id AS donation_id, d.title, d.status, u.display_name AS reporter FROM reports r JOIN donations d ON d.id = r.donation_id JOIN users u ON u.id = r.reporter_id WHERE r.status = 'OPEN' ORDER BY r.created_at;"
* Mark a report as handled: UPDATE reports SET status = 'REVIEWED' WHERE id = '...'; (or 'DISMISSED').
* Taking a listing down by hand: UPDATE donations SET status = 'REMOVED' WHERE id = '...' AND status = 'AVAILABLE'; Note that this bypasses the code, so its pending claims are not rejected automatically; also run UPDATE claims SET status = 'REJECTED' WHERE donation_id = '...' AND status = 'PENDING';

---

# 13. INSTRUCTIONS FOR RESUMING DEVELOPMENT

At the beginning of every new AI session:

1. Read UMEED_MASTER_INSTRUCTIONS.md.
2. Read this entire index.
3. Inspect actual repository files.
4. Check Git status.
5. Verify installed technologies where relevant.
6. Compare actual project files with this index.
7. Identify the last completed batch.
8. Explain current progress briefly.
9. Propose the next feature batch as a short plan.
10. Wait for my explicit approval.

Never recreate completed files.

Never assume that previous conversation history exists.

The actual repository is the source of truth.

The project index is the navigation and progress record.

---

# 14. MANDATORY DEVELOPMENT WORKFLOW

PROPOSE ONE BATCH AS A SHORT PLAN.

WAIT FOR APPROVAL.

WRITE THE BATCH WITH ITS TESTS.

VERIFY ONCE WITH ship.

UPDATE THE PROJECT INDEX.

REPORT BRIEFLY AND STOP.

This rule applies throughout the entire Umeed project.
