# 📋 Implementation Tracker — Event-Driven E-Commerce Platform

> **Last Updated:** 2026-10-08
> **Stack:** Java 21 · Spring Boot 4.1.1 · PostgreSQL · Kafka · Redis · Docker · JWT · OAuth2
> **Status:** 🚧 Active Development — Auth, Product & Inventory (REST) stable; Order Service Cart & Wishlist complete (dual storage: Postgres + Redis); Order Checkout & Kafka pending

---

## 🗺️ Overall Progress Summary

| Layer | Status | Completion |
|---|---|---|
| Infrastructure (Docker / DBs / Redis) | ✅ Done | ~90% |
| Auth Service | ✅ Done | ~95% |
| Product Service | ✅ Done | ~90% |
| Inventory Service | 🟢 In Progress (REST Complete) | ~70% |
| Order Service | 🟢 In Progress (Cart & Wishlist Complete) | ~50% |
| Payment Service | 🔴 Stub only | ~3% |
| Notification Service | 🔴 Stub only | ~3% |
| API Gateway | 🔴 Stub only | ~3% |
| Kafka / Event Bus | 🔴 Not started | ~0% |
| Transactional Outbox | 🔴 Not started | ~0% |
| Saga Workflows | 🔴 Not started | ~0% |
| Idempotency | 🔴 Not started | ~0% |
| Observability | 🟡 Partial | ~15% |
| Testing | 🟡 Partial | ~20% |

---

## 🏗️ Phase 1 — Infrastructure

### Docker & Compose

- [x] `docker-compose.yaml` created
- [x] `auth-db` container (PostgreSQL 18.6 → port 5433)
- [x] `product-db` container (PostgreSQL 18.6 → port 5434)
- [x] `order-db` container (PostgreSQL 18.6 → port 5435)
- [x] `inventory-db` container (PostgreSQL 18.6 → port 5436)
- [x] `payment-db` container (PostgreSQL 18.6 → port 5437)
- [x] `pgadmin` container (port 5050)
- [x] `redis` container (redis:7-alpine → port 6379)
- [ ] `kafka` container (broker not yet in docker-compose)
- [ ] `zookeeper` / KRaft mode for Kafka
- [ ] `kafka-ui` or similar tooling for observability

### Project Build Setup

- [x] Multi-project Gradle Kotlin DSL (`settings.gradle.kts`)
- [x] Root `build.gradle.kts` with shared config
- [x] Java 21 toolchain configured for all services
- [x] `libs` version catalog directory present
- [x] `run-service.sh` helper script present
- [ ] Shared `libs.versions.toml` version catalog (currently each service pins its own deps)

---

## 🔐 Phase 2 — Auth Service (`services/auth-service` · port 8081)

### Database / Migrations

- [x] Flyway enabled, pointing to `auth_db`
- [x] `V1__create_auth_schema.sql` — full schema: `users`, `roles`, `user_roles`, `oauth_accounts`, `email_verification_tokens`, `password_reset_tokens`, `refresh_sessions`
- [x] `ddl-auto: validate` — Hibernate validates, Flyway manages
- [x] Indexes: `user_roles_role_id`, `email_verification_user_id`, `password_reset_user_id`, `refresh_session_user_id`, `refresh_session_expires_at`, `refresh_session_revoked_at`
- [x] Seed data: `USER` and `ADMIN` roles inserted via migration

### Domain Entities

- [x] `User` entity (UUID PK, email, password_hash, email_verified, status, last_login_at, timestamps)
- [x] `Role` entity
- [x] `UserRole` entity (composite key `UserRoleId`)
- [x] `OAuthAccount` entity
- [x] `EmailVerificationToken` entity
- [x] `PasswordResetToken` entity
- [x] `RefreshSession` entity (with revocation fields, device_info, ip_address)
- [x] `UserStatus` enum (`ACTIVE`, `DISABLED`)
- [x] `OAuthProvider` enum (`GOOGLE`)

### Repositories

- [x] `UserRepository`
- [x] `RoleRepository`
- [x] `UserRoleRepository`
- [x] `OAuthAccountRepository`
- [x] `EmailVerificationTokenRepository`
- [x] `PasswordResetTokenRepository`
- [x] `RefreshSessionRepository`

### Security Infrastructure

- [x] `JwtTokenService` — generate & parse/validate signed JWTs
- [x] `JwtAuthenticationFilter` — reads JWT from request, sets `SecurityContext`
- [x] `TokenService` — generates secure random tokens
- [x] `TokenHashService` — SHA-256 hashing of tokens before DB storage
- [x] `SecurityConfig` — filter chain wiring, public vs. protected routes
- [x] `PasswordConfig` — BCrypt bean
- [x] `CustomAccessDeniedHandler` — structured 403 responses
- [x] `CustomAuthenticationEntryPoint` — structured 401 responses
- [x] `OAuth2AuthenticationSuccessHandler` — post-OAuth2 login hook
- [x] `@EnableMethodSecurity` — method-level `@PreAuthorize` support
- [x] HttpOnly cookie delivery for access + refresh tokens (`AuthCookiesService`)
- [ ] CORS configuration (not yet visible in `SecurityConfig`)

### Authentication Endpoints (`/api/v1/auth/*`)

- [x] `POST /register` — email/password registration + email verification token
- [x] `POST /verify-email` — email verification with token hash check + expiry
- [x] `POST /resend-verification` — invalidates old tokens, issues new one
- [x] `POST /login` — credential validation, JWT + refresh session creation
- [x] `POST /refresh` — refresh token rotation (revoke old, issue new)
- [x] `POST /logout` — refresh session revocation with reason `LOGOUT`
- [x] `POST /forgot-password` — issues password-reset token (timed, hashed)
- [x] `POST /reset-password` — validates reset token, updates password hash, revokes all sessions
- [x] `GET /oauth2/authorization/google` — Google OAuth2 initiation
- [ ] `GET /me` — current authenticated user profile endpoint

### Email Service

- [x] `EmailService` interface
- [x] `EmailServiceImpl` — implementation present
- [ ] SMTP / mail provider configured in `application.yaml` (likely a dev stub/console logger)
- [ ] Email templates (HTML vs. plaintext)

### Rate Limiting

- [x] `RateLimitService` — Redis-backed rate limiter
- [x] Redis configured in `application.yaml` (`localhost:6379`)
- [ ] Rate limit applied to endpoints (need to verify filter/interceptor wiring)
- [ ] Rate limit config values (max requests, window) externalized

### JWT Configuration

- [x] Custom `JwtProperties` config class (secret, expiry, issuer)
- [x] `JWT_SECRET` from environment variable
- [x] Access token expiry: 900,000 ms (15 min)
- [x] Issuer: `auth-service`
- [ ] Refresh token expiry configurable via env (hardcoded 30 days in service impl)

### Exception Handling

- [x] `GlobalExceptionHandler` with `@ControllerAdvice`
- [x] `ApiErrorResponse` structured error DTO
- [x] Custom exceptions: `AccountDisabledException`, `EmailAlreadyRegisteredException`, `EmailAlreadyVerifiedException`, `EmailNotVerifiedException`, `InvalidCredentialsException`, `InvalidEmailVerificationTokenException`, `InvalidPasswordResetTokenException`, `InvalidRefreshTokenException`, `PasswordResetTokenNotFoundException`, `RateLimitExceededException`, `UserNotFoundException`

### Actuator / Observability

- [x] `spring-boot-starter-actuator` dependency present
- [x] Endpoints `health` and `info` exposed
- [x] `show-details: always` for health endpoint
- [ ] Metrics endpoint exposed
- [ ] Custom health indicators

---

## 📦 Phase 3 — Product Service (`services/product-service` · port 8082)

### Database / Migrations

- [x] Flyway enabled, pointing to `product_db`
- [x] `V1__create_products_table.sql` — initial products schema
- [x] `V2__add_image_url_to_products.sql` — `image_url` column added
- [x] `V3__remove_stock_quantity_from_products.sql` — stock separated to Inventory Service
- [x] `ddl-auto: validate`

### Domain Entity

- [x] `Product` entity (UUID PK, name, description, price, currency, active flag, imageUrl, timestamps)

### Repository

- [x] `ProductRepository` — `findByIdAndActiveTrue()`, `findByActiveTrue()`

### Service Layer (`ProductService`)

- [x] `createProduct` — create + persist new product
- [x] `getActiveProduct(UUID)` — customer-facing, active-only
- [x] `getProduct(UUID)` — admin-facing, any status
- [x] `getAllProducts` — admin list (all)
- [x] `getAllActiveProducts` — customer list (active only)
- [x] `deleteProduct(UUID)` — soft delete (`active = false`)
- [x] `updateProduct(UUID, UpdateProductRequest)` — full PUT replacement
- [x] `patchProduct(UUID, PatchProductRequest)` — partial PATCH (null-safe field updates)

### Controllers

- [x] `ProductController` — customer-facing API
  - [x] `GET /api/v1/products`
  - [x] `GET /api/v1/products/{id}`
- [x] `AdminProductController` — admin API
  - [x] `POST /api/v1/admin/products`
  - [x] `GET /api/v1/admin/products`
  - [x] `GET /api/v1/admin/products/{id}`
  - [x] `PUT /api/v1/admin/products/{id}`
  - [x] `PATCH /api/v1/admin/products/{id}`
  - [x] `DELETE /api/v1/admin/products/{id}`

### DTOs

- [x] `CreateProductRequest`
- [x] `UpdateProductRequest`
- [x] `PatchProductRequest`
- [x] `ProductResponse`

### Mapper

- [x] `ProductMapper` — entity to DTO mapping

### Security (JWT Validation)

- [x] `JwtConfig` + `JwtProperties` — reads shared `JWT_SECRET` from env
- [x] `JwtTokenService` — local JWT validation (no network call to Auth Service)
- [x] `JwtAuthenticationFilter` — parse JWT from cookie/header, set `SecurityContext`
- [x] `SecurityConfig` — public routes (`GET /api/v1/products/**`), admin routes require auth
- [x] `@EnableMethodSecurity` present
- [ ] `@PreAuthorize("hasRole('ADMIN')")` on admin controller methods (needs verification)
- [ ] Role-based access control fully enforced on admin endpoints

### Exception Handling

- [x] `GlobalExceptionHandler`
- [x] `ApiErrorResponse`
- [x] `ProductNotFoundException`
- [x] `ProductServiceException`

### Kafka (Dependency Present, Not Wired)

- [x] `spring-boot-starter-kafka` in `build.gradle.kts`
- [ ] Kafka producer configuration in `application.yaml`
- [ ] `ProductCreated` event definition
- [ ] `ProductUpdated` event definition
- [ ] `ProductDeleted` (soft-delete) event definition
- [ ] Event publishing on product mutations

---

## 📦 Phase 4 — Inventory Service (`services/inventory-service` · port TBD)

### Database / Migrations

- [x] Flyway enabled, pointing to `inventory_db`
- [x] `V1__create_inventory_table.sql` — `inventory` table with `available_quantity`, `reserved_quantity`, optimistic-lock `version`, timestamps
- [x] `ddl-auto: validate`
- [x] `CHECK` constraints on quantities (non-negative)

### Domain Entity

- [x] `Inventory` entity (UUID PK, product_id UNIQUE, available_quantity, reserved_quantity, version, timestamps)

### Repository

- [x] `InventoryRepository` — `findByProductId(UUID)`

### Service Layer (`InventoryService`)

- [x] `createInventory(CreateInventoryRequest)` — creates inventory record for a product
- [x] `getInventoryByProductId(UUID)` — fetch stock by product
- [x] `reserveInventory(ReserveInventoryRequest)` — decrements available, increments reserved (marked `@Transactional`)
- [x] `releaseInventory(ReleaseInventoryRequest)` — revert reservation on failure/cancel (marked `@Transactional`)
- [x] `confirmInventory(ConfirmInventoryRequest)` — finalize reservation post-payment (marked `@Transactional`)
- [x] `addStock(UUID, AddStockRequest)` — replenish stock quantity (marked `@Transactional`)
- [x] `updateStock(UUID, UpdateStockRequest)` — set exact available stock quantity (marked `@Transactional`)
- [x] Optimistic locking applied on reservation (entity `@Version` + conflict handling in `GlobalExceptionHandler`)

### Controller

- [x] `InventoryController`
  - [x] `POST /api/v1/inventory` (ADMIN only)
  - [x] `GET /api/v1/inventory/products/{productId}`
  - [x] `POST /api/v1/inventory/reserve`
  - [x] `POST /api/v1/inventory/release`
  - [x] `POST /api/v1/inventory/confirm`
  - [x] `PATCH /api/v1/inventory/products/{productId}` (ADMIN only — update available stock)
  - [x] `POST /api/v1/inventory/products/{productId}/add-stock` (ADMIN only — add stock)

### DTOs

- [x] `CreateInventoryRequest`
- [x] `ReserveInventoryRequest`
- [x] `ReleaseInventoryRequest`
- [x] `ConfirmInventoryRequest`
- [x] `AddStockRequest`
- [x] `UpdateStockRequest`
- [x] `InventoryResponse`

### Security

- [x] `JwtConfig` + `JwtProperties` — local JWT validation wired
- [x] `JwtTokenService`
- [x] `JwtAuthenticationFilter`
- [x] `SecurityConfig`
- [x] `POST /api/v1/inventory` requires `ROLE_ADMIN` (`@PreAuthorize`)
- [x] Admin stock management endpoints require `ROLE_ADMIN` (`@PreAuthorize`)
- [x] Auth applied to reserve/release/confirm endpoints (`anyRequest().authenticated()`)
- [x] `application.yaml` — port (8085), datasource, JWT secret wired

### Exception Handling

- [x] `GlobalExceptionHandler`
- [x] `ApiErrorResponse`
- [x] `InsufficientStockException`
- [x] `InsufficientReservedStockException`
- [x] `InventoryAlreadyExistsException`
- [x] `InventoryNotFoundException`
- [x] `InventoryServiceException`
- [x] `ObjectOptimisticLockingFailureException` & `OptimisticLockingFailureException` (returns 409 Conflict)

### Kafka (Dependency Present, Not Wired)

- [x] `spring-boot-starter-kafka` in `build.gradle.kts`
- [ ] `InventoryReserved` event publishing
- [ ] `InventoryReservationFailed` event publishing
- [ ] `InventoryReleased` event publishing
- [ ] Kafka consumer for `OrderCreated` event

---

## 🛒 Phase 5 — Order Service (`services/order-service` · port 8083)

> **Status: 🟢 In Progress** — Cart and Wishlist features complete with Dual Strategy (PostgreSQL + Redis). Order placement, checkout conversion, and state machine are pending.

### Scaffold & Configuration

- [x] `OrderServiceApplication.java` with `@ConfigurationPropertiesScan`
- [x] `application.yaml` (port 8083, datasource pointing to `order_db:5435`, Redis `6379`, JWT secret, guest TTL 7 days)
- [x] `build.gradle.kts` — Spring WebMvc, JPA, Flyway, Redis, Security, JJWT, Validation, PostgreSQL, Lombok, Jackson

### Security Infrastructure

- [x] `JwtConfig` + `JwtProperties`
- [x] `JwtTokenService`
- [x] `JwtAuthenticationFilter` (supports both HttpOnly cookies and `Authorization: Bearer` headers)
- [x] `SecurityConfig` (public cart access with optional guest cart ID, authenticated wishlist & cart merge)

### Cart & Wishlist Domain & Persistence (Dual Strategy: PostgreSQL + Redis)

- [x] Flyway `V1__create_carts_and_items_table.sql` (`carts`, `cart_items` with unique product constraint and check constraints)
- [x] Flyway `V2__create_wishlists_and_items_table.sql` (`wishlists`, `wishlist_items` with unique product constraint)
- [x] Domain entities: `Cart`, `CartItem`, `Wishlist`, `WishlistItem`
- [x] Repositories: `CartRepository`, `CartItemRepository`, `WishlistRepository`, `WishlistItemRepository` (with N+1 fetch join queries)
- [x] Redis Guest Cart: `GuestCart`, `GuestCartItem`, `GuestCartRedisService` with configurable TTL

### Cart & Wishlist Service Layer

- [x] `CartService`:
  - [x] Dual strategy dispatch: authenticated user → PostgreSQL, guest user → Redis (`X-Guest-Cart-Id`)
  - [x] `getCart` (authenticated & guest)
  - [x] `addItem` (add new item or increment quantity)
  - [x] `updateItemQuantity` (update or remove if 0)
  - [x] `removeItem`
  - [x] `clearCart` (DB items clear or Redis key delete)
  - [x] `mergeGuestCart` (merges Redis guest cart into PostgreSQL user cart on login and purges Redis key)
  - [x] `moveToWishlist` (atomic transfer from cart to wishlist)
- [x] `WishlistService`:
  - [x] `getWishlist`
  - [x] `addItem` (idempotent addition)
  - [x] `removeItem`
  - [x] `moveToCart` (transfer from wishlist to user cart)

### Controllers & DTOs

- [x] `CartController`:
  - [x] `GET /api/v1/cart`
  - [x] `POST /api/v1/cart/items`
  - [x] `PATCH /api/v1/cart/items/{productId}`
  - [x] `DELETE /api/v1/cart/items/{productId}`
  - [x] `DELETE /api/v1/cart`
  - [x] `POST /api/v1/cart/merge`
  - [x] `POST /api/v1/cart/items/{productId}/move-to-wishlist`
- [x] `WishlistController`:
  - [x] `GET /api/v1/wishlist`
  - [x] `POST /api/v1/wishlist/items`
  - [x] `DELETE /api/v1/wishlist/items/{productId}`
  - [x] `POST /api/v1/wishlist/items/{productId}/move-to-cart`
- [x] DTOs: `AddToCartRequest`, `UpdateCartItemRequest`, `CartResponse`, `CartItemResponse`, `MergeCartRequest`, `AddToWishlistRequest`, `MoveWishlistItemToCartRequest`, `WishlistResponse`, `WishlistItemResponse`
- [x] Exception handling: `GlobalExceptionHandler`, `CartNotFoundException`, `CartItemNotFoundException`, `WishlistItemNotFoundException`, `InvalidCartOperationException`, `UnauthorizedCartAccessException`

### Order Management (Upcoming)

- [ ] Flyway migration: `V3__create_orders_table.sql`
- [ ] Flyway migration: `V4__create_order_items_table.sql`
- [ ] `Order` entity & `OrderItem` entity
- [ ] `OrderStatus` enum (`PENDING`, `CONFIRMED`, `CANCELLED`, `COMPLETED`)
- [ ] `OrderRepository` & `OrderItemRepository`
- [ ] `createOrderFromCart(UUID customerId)` — convert active cart to order, clear cart
- [ ] `getOrder(UUID orderId, UUID customerId)`
- [ ] `cancelOrder(UUID orderId)`
- [ ] Order status state machine
- [ ] `POST /api/v1/orders/checkout` & `GET /api/v1/orders`

### Kafka Integration (Upcoming)

- [ ] Publish `OrderCreated` event
- [ ] Consume `InventoryReserved` / `InventoryReservationFailed`
- [ ] Consume `PaymentConfirmed` / `PaymentFailed`
- [ ] Update order status based on events

---

## 💳 Phase 6 — Payment Service (`services/payment-service` · port TBD)

> **Status: Stub Only** — Only the Spring Boot application entry point and a minimal `application.yaml` exist.

### Scaffold

- [x] `PaymentServiceApplication.java` (main class only)
- [x] `application.yaml` (empty, only app name)
- [ ] `build.gradle.kts` — full dependency list

### Database / Migrations

- [x] `payment-db` container present in `docker-compose.yaml`
- [ ] Flyway migration: `V1__create_payments_table.sql`

### Domain

- [ ] `Payment` entity (id, order_id, amount, currency, status, provider_ref, timestamps)
- [ ] `PaymentStatus` enum (`PENDING`, `CONFIRMED`, `FAILED`, `REFUNDED`)

### Service Layer

- [ ] `initiatePayment(InitiatePaymentRequest)`
- [ ] `confirmPayment(UUID paymentId)`
- [ ] `failPayment(UUID paymentId, String reason)`
- [ ] External payment provider integration boundary (e.g., Stripe/mock)

### Controller

- [ ] `POST /api/v1/payments`
- [ ] `GET /api/v1/payments/{id}`

### Kafka Integration

- [ ] Consume `InventoryReserved` event — initiate payment
- [ ] Publish `PaymentConfirmed` event
- [ ] Publish `PaymentFailed` event

---

## 🔔 Phase 7 — Notification Service (`services/notification-service` · port TBD)

> **Status: Stub Only** — Only the Spring Boot application entry point and a minimal `application.yaml` exist.

### Scaffold

- [x] `NotificationServiceApplication.java` (main class only)
- [x] `application.yaml` (empty)
- [ ] `build.gradle.kts` — full dependency list (Spring Web, Kafka, Mail)

### Domain

- [ ] `Notification` entity (id, type, recipient, status, payload, timestamps)
- [ ] `NotificationType` enum (`ORDER_CONFIRMED`, `PAYMENT_CONFIRMED`, `PAYMENT_FAILED`, etc.)

### Service Layer

- [ ] `sendOrderConfirmationEmail(UUID orderId)`
- [ ] `sendPaymentConfirmationEmail(UUID paymentId)`
- [ ] `sendPaymentFailureEmail(UUID paymentId)`

### Kafka Integration

- [ ] Consume `OrderCreated` event
- [ ] Consume `PaymentConfirmed` event
- [ ] Consume `PaymentFailed` event

### Email Delivery

- [ ] SMTP / mail provider configured
- [ ] Email templates (HTML)

---

## 🌐 Phase 8 — API Gateway (`services/api-gateway` · port TBD)

> **Status: Stub Only** — Only the Spring Boot application entry point and a minimal `application.yaml` exist.

### Scaffold

- [x] `ApiGatewayApplication.java` (main class only)
- [x] `application.yaml` (empty)
- [ ] `build.gradle.kts` — Spring Cloud Gateway dependency

### Routing

- [ ] Route: `auth-service` (`/auth/**`)
- [ ] Route: `product-service` (`/products/**`)
- [ ] Route: `order-service` (`/orders/**`)
- [ ] Route: `inventory-service` (`/inventory/**`)
- [ ] Route: `payment-service` (`/payments/**`)

### Edge Concerns

- [ ] JWT validation at gateway level (optional — currently done per-service)
- [ ] Rate limiting at gateway
- [ ] Request/response logging filter
- [ ] CORS headers at gateway
- [ ] Circuit breaker (Resilience4j)

---

## 📨 Phase 9 — Kafka & Event-Driven Architecture

> **Status: Not Started** — Kafka dependencies are in product-service and inventory-service `build.gradle.kts`, but no producers, consumers, or topic configs exist yet.

### Infrastructure

- [ ] Kafka broker added to `docker-compose.yaml`
- [ ] Kafka topic configuration / auto-creation strategy
- [ ] Kafka admin bean for topic management

### Event Contracts (Topic Definitions)

- [ ] `order.created` — published by Order Service
- [ ] `inventory.reserved` — published by Inventory Service
- [ ] `inventory.reservation.failed` — published by Inventory Service
- [ ] `inventory.released` — published by Inventory Service
- [ ] `payment.initiated` — published by Payment Service
- [ ] `payment.confirmed` — published by Payment Service
- [ ] `payment.failed` — published by Payment Service
- [ ] `order.completed` — published by Order Service
- [ ] `order.cancelled` — published by Order Service

### Producers

- [ ] Order Service → `order.created`
- [ ] Inventory Service → `inventory.reserved`, `inventory.reservation.failed`, `inventory.released`
- [ ] Payment Service → `payment.confirmed`, `payment.failed`
- [ ] Order Service → `order.completed`, `order.cancelled`

### Consumers

- [ ] Inventory Service ← `order.created`
- [ ] Payment Service ← `inventory.reserved`
- [ ] Order Service ← `inventory.reservation.failed`
- [ ] Order Service ← `payment.confirmed`, `payment.failed`
- [ ] Notification Service ← `order.created`, `payment.confirmed`, `payment.failed`

---

## 📤 Phase 10 — Transactional Outbox

> **Status: Not Started**

- [ ] `outbox_events` table in Order Service DB (event_id, aggregate_id, event_type, payload, published_at, created_at)
- [ ] `outbox_events` table in Inventory Service DB
- [ ] `outbox_events` table in Payment Service DB
- [ ] Domain mutation + outbox record written in same DB transaction
- [ ] Outbox poller / relay (scheduled task or CDC via Debezium)
- [ ] Outbox publisher — reads unpublished events, publishes to Kafka, marks `published_at`
- [ ] Retry logic for failed publishes

---

## 🔁 Phase 11 — Saga Workflows (Distributed Transactions)

> **Status: Not Started**

### Order Placement Saga (Choreography)

- [ ] Step 1: Order Service creates order → publishes `order.created`
- [ ] Step 2: Inventory Service reserves stock → publishes `inventory.reserved` or `inventory.reservation.failed`
- [ ] Step 3a (success): Payment Service initiates payment → publishes `payment.confirmed` or `payment.failed`
- [ ] Step 3b (failure): Order Service receives `inventory.reservation.failed` → cancels order
- [ ] Step 4a (success): Order Service marks order `COMPLETED`
- [ ] Step 4b (failure): Order Service receives `payment.failed` → triggers `inventory.released` compensation
- [ ] Compensation: Inventory Service releases reservation

### Compensation Flows

- [ ] Inventory release on payment failure
- [ ] Order cancellation on any saga failure
- [ ] Dead-letter queue for unprocessable events

---

## 🔒 Phase 12 — Idempotency

> **Status: Not Started**

- [ ] Idempotency key strategy defined (e.g., `X-Idempotency-Key` header or event ID)
- [ ] `processed_events` table per consuming service (event_id, processed_at)
- [ ] Consumer checks `processed_events` before processing
- [ ] Duplicate event — safe no-op, not double-processed
- [ ] Idempotency enforced in Inventory Service consumer
- [ ] Idempotency enforced in Payment Service consumer
- [ ] Idempotency enforced in Order Service consumer
- [ ] Idempotency enforced in Notification Service consumer

---

## 🧪 Phase 13 — Testing

### Auth Service Tests

- [x] `AuthServiceApplicationTests` — context load test (stub)
- [x] `JwtTokenServiceTest` — unit test file exists
  - [x] Token generation assertion written
  - [x] Claims validation (`sub`, `email`, expiration, issuedAt) written
  - [ ] **BUG:** `jwtTokenService` field not assigned in `@BeforeEach` — will throw NPE at runtime
  - [ ] Roles claim assertion commented out
- [ ] Unit tests for `TokenHashService`
- [ ] Unit tests for `AuthServiceImpl` (register, login, refresh, logout, forgot/reset password)
- [ ] Integration tests for auth endpoints (full HTTP stack with test DB)
- [ ] Security tests: unauthenticated access, invalid JWT, expired JWT, revoked refresh token
- [ ] Rate limit tests

### Product Service Tests

- [x] `ProductServiceApplicationTests` — context load test (stub)
- [ ] Unit tests for `ProductService`
- [ ] Integration tests for customer API
- [ ] Integration tests for admin API
- [ ] Security tests: unauthorized admin access, valid admin JWT

### Inventory Service Tests

- [x] `InventoryServiceApplicationTests` — context load test (stub)
- [x] Unit tests for `InventoryService` (`InventoryServiceTest` covering create, reserve, release, confirm, add, update, error cases)
- [ ] Integration tests for inventory endpoints
- [ ] Concurrent reservation tests (optimistic lock / race condition)

### Order Service Tests

- [x] `OrderServiceApplicationTests` — context load test (stub)
- [x] Unit tests for `CartService` (`CartServiceTest` — 17 unit tests covering authenticated Postgres flow, guest Redis flow, TTL, cart merge, move to wishlist, error handling)
- [x] Unit tests for `WishlistService` (`WishlistServiceTest` — 10 unit tests covering get, idempotent add, remove, move to cart)
- [x] Live end-to-end integration verified: Guest cart (Redis TTL), Auth cart (Postgres), Cart Merge, Wishlist move-between, 400 validation, 401 unauthenticated check
- [ ] Unit tests for Order checkout & state machine (when implemented)
- [ ] Integration tests for Order endpoints

### Payment / Notification Tests

- [x] Context load stubs only
- [ ] No meaningful tests yet

### Kafka Tests (All Pending)

- [ ] Event publication tests (producer)
- [ ] Event consumption tests (consumer)
- [ ] Duplicate event / idempotency tests
- [ ] Retry behavior tests
- [ ] Saga compensation tests
- [ ] End-to-end Kafka flow test with embedded/TestContainers Kafka

### End-to-End Tests (All Pending)

- [ ] Register → Verify → Login → Create Order → Reserve Inventory → Process Payment → Complete Order → Send Notification flow

---

## 🔍 Phase 14 — Observability & Production Hardening

### Actuator (Partial)

- [x] Actuator dependency in Auth Service
- [x] `health` and `info` endpoints exposed
- [ ] Actuator configured in Product, Inventory, Order, Payment, Notification Services
- [ ] `metrics` endpoint exposed
- [ ] Prometheus scrape endpoint (`/actuator/prometheus`)

### Distributed Tracing

- [ ] Micrometer Tracing / OpenTelemetry dependency
- [ ] Trace ID propagated across service calls and Kafka events
- [ ] Jaeger or Zipkin container in `docker-compose.yaml`

### Metrics & Dashboards

- [ ] Prometheus container in `docker-compose.yaml`
- [ ] Grafana container with pre-built dashboards
- [ ] Business metrics (orders per minute, payment success rate, etc.)

### Logging

- [x] `application.yaml` logging levels configured in Auth Service and Product Service
- [ ] Structured JSON logging (Logback with JSON encoder)
- [ ] Correlation ID / Trace ID in log output
- [ ] Centralized log aggregation (ELK / Loki — optional)

### Resilience

- [ ] Circuit breaker on inter-service HTTP calls (Resilience4j)
- [ ] Retry policies for Kafka consumers
- [ ] Dead-letter topics for unprocessable messages
- [ ] Health checks exposed for readiness/liveness (Kubernetes-ready)

### Security Hardening

- [ ] CORS configuration in all services
- [x] Secrets management via env vars — no hardcoded secrets committed
- [ ] HTTPS / TLS for local dev (optional)
- [ ] Cookie `Secure` flag set to `true` for production (`false` in dev — acceptable)

---

## 🚧 Known Issues & Technical Debt

| # | Issue | Severity | Location |
|---|---|---|---|
| 1 | `jwtTokenService` field not assigned in `@BeforeEach` — NPE at test runtime | High | `JwtTokenServiceTest.java` |
| 2 | ~~`reserveInventory` method missing `@Transactional`~~ (Resolved) | None | `InventoryService.java` |
| 3 | Refresh token expiry (30 days) hardcoded, not configurable via env | Low | `AuthServiceImpl.java:220` |
| 4 | Password reset token expiry (5 min) hardcoded | Low | `AuthServiceImpl.java:332` |
| 5 | Admin endpoint `@PreAuthorize` on Product Service needs explicit verification | Medium | `AdminProductController.java` |
| 6 | SMTP not configured — email delivery is likely a stub or console logger | Medium | `EmailServiceImpl.java` |
| 7 | Kafka not in `docker-compose.yaml` despite being a declared dependency | High | `docker-compose.yaml` |
| 8 | ~~Order Service `application.yaml` nearly empty — missing datasource, port~~ (Resolved) | None | `order-service/application.yaml` |
| 9 | `roles` claim assertion in JWT test is commented out | Low | `JwtTokenServiceTest.java:58` |
| 10 | No CORS config in any service | Medium | All services |

---

## 📅 Recommended Implementation Order

```text
NOW (Complete Order & Checkout foundation)
 ├── Order entity + OrderItem entity + Flyway migrations (V3 & V4)
 ├── Order status state machine (PENDING, CONFIRMED, CANCELLED, COMPLETED)
 ├── Checkout flow: convert active Cart -> Order & clear Cart
 ├── Order REST endpoints (POST /api/v1/orders/checkout, GET /api/v1/orders)
 └── Fix JwtTokenServiceTest NPE bug in Auth Service

NEXT (Kafka infrastructure)
 ├── Add Kafka (KRaft mode) to docker-compose.yaml
 ├── Define shared event contract POJOs (OrderCreated, InventoryReserved, etc.)
 ├── Order Service → publish OrderCreated
 ├── Inventory Service → consume OrderCreated, publish InventoryReserved
 └── Payment Service foundation + consume InventoryReserved

THEN (Kafka infrastructure)
 ├── Add Kafka to docker-compose.yaml
 ├── Define event contract POJOs
 ├── Order Service → publish OrderCreated
 ├── Inventory Service → consume OrderCreated, publish InventoryReserved
 └── Payment Service foundation + consume InventoryReserved

THEN (Distributed patterns)
 ├── Transactional Outbox per service
 ├── Idempotency tables + checks per consumer
 └── Saga compensation flows

FINALLY (Hardening)
 ├── Observability stack (Prometheus, Grafana, Tracing)
 ├── Full test coverage
 └── API Gateway routing + edge concerns
```

---

## 🔗 Quick Reference — Service Ports

| Service | Port | Database | DB Port |
|---|---|---|---|
| Auth Service | 8081 | auth_db | 5433 |
| Product Service | 8082 | product_db | 5434 |
| Order Service | 8083 | order_db | 5435 |
| Inventory Service | 8085 | inventory_db | 5436 |
| Payment Service | TBD | payment_db | 5437 |
| API Gateway | TBD | — | — |
| pgAdmin | 5050 | — | — |
| Redis | — | — | 6379 |
| Kafka | TBD | — | TBD |
