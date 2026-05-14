# Repository Guidelines

## Project Structure & Module Organization

Spring Boot auction backend. Main code lives in `src/main/java/com/thanh/auction_server`: rules in `service`, persistence in `repository`, API contracts in `dto`, and JPA models in `entity`. Tests belong in `src/test/java/com/thanh/auction_server`. Config is in `src/main/resources`; do not edit `target/`.

## Business Logic Overview

Core flow: user creates product, admin approves it, seller creates an auction session, bidders place max/proxy bids, schedulers start/end sessions, winner receives invoice, VNPay marks payment, seller ships, buyer confirms or disputes, and feedback updates reputation. Preserve these invariants:

- Products start as `WAITING_FOR_APPROVAL`; only `ACTIVE` products can be auctioned.
- A product can have only one auction session.
- Reserve-price auctions require a `LISTING_FEE` invoice equal to `reservePrice * LISTING_FEE_PERCENT`; session stays `WAITING_PAYMENT` until VNPay marks it paid.
- Auctions move `SCHEDULED -> ACTIVE -> ENDED` by 30-second schedulers, or to `FAILED` when no winner meets reserve.
- Bids use pessimistic locking, 10-second cooldown, tiered increments, and proxy/max-bid logic. Sellers cannot bid or buy. First bid disables buy-now.
- `reportNonPayment` creates negative feedback, lowers reputation, adds a strike, and can block/deactivate users.
- `AUCTION_SALE` invoices require an address snapshot before payment; `LISTING_FEE` invoices do not.
- Shipping invoices become `COMPLETED` by buyer confirmation or daily auto-finish after `INVOICE_AUTO_COMPLETED_DAYS`.
- Disputes pause completion. Admin resolves to either `REFUNDED` with VNPay refund or `COMPLETED` releasing the sale.

## Build, Test, and Development Commands

Use the Maven wrapper:

- `./mvnw spring-boot:run` or `.\mvnw.cmd spring-boot:run`: start the API.
- `./mvnw test`: run JUnit/Spring Boot tests.
- `./mvnw clean package`: compile, test, and build the artifact.

API: `8081` under `/api/v1`; Socket.IO: `9092`; default profile: `dev`.

## Coding Style & Naming Conventions

Use Java 23, Spring Boot 3.4.x, Lombok, and MapStruct. Use 4-space indentation, PascalCase classes, camelCase methods/fields, and UPPER_SNAKE_CASE constants. Keep controllers thin; enforce transitions in services.

## Testing Guidelines

Uses `spring-boot-starter-test` with JUnit 5. Name test classes with `Tests` suffix. Prioritize state transitions, payment callbacks, bid locking/proxy calculations, schedulers, and authorization checks. Run `./mvnw test` before PRs.

## Commit & Pull Request Guidelines

Recent history uses short, lower-case summaries such as `fix payment`. Prefer specific commits like `fix invoice timezone handling`. PRs need description, changed endpoints/config keys, and test results.

## Security & Configuration Tips

Local development needs MySQL, Redis, OAuth, mail, Cloudinary, and VNPay. Timezone is `Asia/Ho_Chi_Minh`. Keep secrets in env vars such as `GOOGLE_CLIENT_SECRET`, `AUCTION_JWT_SIGNER_KEY`, `CLOUDINARY_SECRET`, and `VNPAY_HASH_SECRET`. Do not commit credentials.

## Learned Rules After Compact

- Before removing files or dependencies, prove they are unused with repo-wide search plus compile/test checks. Spring-scanned controllers, configs, schedulers, repositories, mappers, entities, and DTOs may not have direct references but are still part of the app.
- The Google login flow is custom: `/auth/outbound/authenticate` uses OpenFeign clients and OAuth config values directly. Do not re-add Spring `oauth2Login()` or `spring-boot-starter-oauth2-client` unless the app intentionally moves to Spring-managed OAuth login.
- Redis is used through `RedisTemplate`, not Redis repositories. Keep Redis available, but keep Redis repository scanning disabled with `spring.data.redis.repositories.enabled=false` unless Redis repository types are introduced.
- Keep noisy routine logs out of schedulers, Cloudinary upload/delete, invoice creation, refund success, and startup parameter initialization. Preserve warn/error logs that signal failures, rejected security events, duplicate invoices, blocked bidders, and account penalties.
- `mvn -DskipTests compile` is the primary quick verification. `mvn test` can fail locally when MySQL at `localhost:3307` is not running for `AuctionServerApplicationTests`; run focused tests such as `mvn -Dtest=BidServiceTests test` when DB-backed context tests are blocked.
- The Maven wrapper may fail in this Windows sandbox with `Cannot start maven from wrapper`; using installed `mvn` is acceptable for verification here. Network-restricted Maven commands may need approval to fetch dependencies.
