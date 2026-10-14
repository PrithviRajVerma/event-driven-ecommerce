# 📋 Implementation Tracker — Event-Driven E-Commerce Platform

> **Last Updated:** 2026-10-14
> **Stack:** Java 21 · Spring Boot 4.1.1 · PostgreSQL · Kafka · Redis · Docker · JWT · OAuth2
> **Status:** 🚧 Active Development — Auth, Product, Inventory, Order, Payment, Distributed Patterns (Sprint 9), Notification Service (with Resend REST API) and API Gateway (Sprint 10) complete; Sprint 11 (Observability, Hardening & E2E Integration) next

---

## 🗺️ Overall Progress Summary

| Layer | Status | Completion |
|---|---|---|
| Infrastructure (Docker / DBs / Redis / Kafka) | ✅ Done | ~95% |
| Auth Service | ✅ Done | ~95% |
| Product Service | ✅ Done | ~90% |
| Inventory Service | ✅ Done (REST, Sagas & Outbox Complete) | ~95% |
| Order Service | ✅ Done (Cart, Checkout, Sagas & Outbox Complete) | ~95% |
| Payment Service | ✅ Done (REST, Outbox & Idempotency Complete) | ~95% |
| Notification Service | ✅ Done (Postgres, Sagas, Idempotency & Resend REST Complete) | ~95% |
| API Gateway | ✅ Done (Edge Routing, CORS, Rate Limiting & Tracing) | ~95% |
| Kafka / Event Bus | ✅ Done (KRaft, Contracts, Producers & Consumers) | ~95% |
| Transactional Outbox | ✅ Done (Migrations, OutboxService & OutboxPublisher Relays) | ~95% |
| Saga Workflows | ✅ Done (Choreographed Sagas & Compensating Transactions) | ~95% |
| Idempotency | ✅ Done (Processed Events Deduplication Tables & Services) | ~95% |
| Observability | 🟡 Partial | ~25% |
| Testing | 🟢 Passing | ~90% |

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
- [x] `kafka` container (`confluentinc/cp-kafka:7.5.0` in KRaft mode → port 9092 internal, port 9094 external)
- [x] `kafka-ui` container (`provectuslabs/kafka-ui:latest` → port 8090) for web-based topic and consumer observability

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

## 📦 Phase 4 — Inventory Service (`services/inventory-service` · port 8085)

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

### Kafka Integration (Producers & Consumers Complete)

- [x] `spring-boot-starter-kafka` and `libs:event-contracts` in `build.gradle.kts`
- [x] Kafka producer & consumer configuration in `application.yaml`
- [x] `KafkaTopicConfig` bean auto-creating `order.created`, `inventory.reserved`, `inventory.reservation.failed`, `inventory.released`
- [x] `InventoryEventProducer` — publishes `InventoryReservedEvent`, `ReservationFailedEvent`, and `InventoryReleasedEvent`
- [x] `InventoryEventProducerTest` unit tests (3 test cases)
- [x] Kafka consumer for `OrderCreatedEvent` (`OrderCreatedConsumer` in Sprint 7)
- [x] Atomic multi-item order reservation in `InventoryService.reserveOrderInventory` with transactional rollback on stock deficit
- [x] `OrderCreatedConsumerTest` unit tests (4 test cases)

---

## 🛒 Phase 5 — Order Service (`services/order-service` · port 8083)

> **Status: ✅ Complete** — Cart, Wishlist, Checkout conversion, Order lifecycle state machine, Kafka event producers, and saga event consumers complete.

### Scaffold & Configuration

- [x] `OrderServiceApplication.java` with `@ConfigurationPropertiesScan`
- [x] `application.yaml` (port 8083, datasource pointing to `order_db:5435`, Redis `6379`, JWT secret, guest TTL 7 days, Kafka producer/consumer config)
- [x] `build.gradle.kts` — Spring WebMvc, JPA, Flyway, Redis, Security, JJWT, Validation, PostgreSQL, Lombok, Jackson, Spring Kafka, `libs:event-contracts`

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

### Order Management (Completed in Sprint 6)

- [x] Flyway migration: `V3__create_orders_table.sql`
- [x] Flyway migration: `V4__create_order_items_table.sql`
- [x] `Order` entity & `OrderItem` entity
- [x] `OrderStatus` enum (`PENDING`, `CONFIRMED`, `CANCELLED`, `COMPLETED`)
- [x] `OrderRepository` & `OrderItemRepository`
- [x] `createOrderFromCart(UUID customerId)` — convert active cart to order, snapshot item prices, clear cart
- [x] `getOrder(UUID orderId, UUID customerId)`
- [x] `cancelOrder(UUID orderId, UUID customerId, String reason)`
- [x] Order status state machine (`canBeCancelled()` & `updateOrderStatus()`)
- [x] `POST /api/v1/orders/checkout` & `GET /api/v1/orders` & `GET /api/v1/orders/{orderId}` & `POST /api/v1/orders/{orderId}/cancel`
- [x] Unit test suite (`OrderServiceTest` — 13 tests, `OrderControllerTest` — 7 tests)
- [x] Swagger UI & OpenAPI 3.1 schema documentation for Order endpoints
- [x] Publish `OrderCreatedEvent` to Kafka on checkout
- [x] Publish `OrderCancelledEvent` to Kafka on cancellation
- [x] Publish `OrderConfirmedEvent` to Kafka on status update
- [x] Consume `InventoryReserved` / `InventoryReservationFailed` (`InventoryEventConsumer` in Sprint 7)
- [x] Consume `PaymentCompleted` / `PaymentFailed` (`PaymentEventConsumer` in Sprint 7/8)
- [x] Update order status based on consumed saga events (`handleInventoryReserved`, `handleInventoryReservationFailed`, `handlePaymentCompleted`, `handlePaymentFailed`)

---

## 💳 Phase 6 — Payment Service (`services/payment-service` · port 8084)
 
> **Status: ✅ Complete** — Database migrations, domain model, authorization, Kafka consumer (`inventory.reserved`), Kafka producer (`payment.completed` / `payment.failed`), REST endpoints, and test suites implemented.

### Scaffold & Configuration

- [x] `PaymentServiceApplication.java` with `@ConfigurationPropertiesScan`
- [x] `application.yaml` with port `8084`, PostgreSQL datasource (`payment_db:5437`), Kafka, JWT, and OpenAPI specs
- [x] `build.gradle.kts` — full dependencies (JPA, Web, Security, JWT, Flyway, Kafka, Lombok, contracts)

### Database / Migrations

- [x] `payment-db` container in `docker-compose.yaml` (port `5437:5432`)
- [x] Flyway migration `V1__create_payments_table.sql` (`payments` table with check constraints and indexes)

### Domain & Repository

- [x] `Payment` entity (`id`, `orderId`, `customerId`, `amount`, `currency`, `status`, `paymentMethod`, `transactionReference`, `failureReason`, `timestamps`)
- [x] `PaymentStatus` enum (`PENDING`, `COMPLETED`, `FAILED`, `REFUNDED`)
- [x] `PaymentMethod` enum (`CREDIT_CARD`, `DEBIT_CARD`, `PAYPAL`, `MOCK_GATEWAY`)
- [x] `PaymentRepository` (`findByOrderId`, `findByIdAndCustomerId`, `findByOrderIdAndCustomerId`, `findByCustomerIdOrderByCreatedAtDesc`)

### Service Layer

- [x] `processPayment(ProcessPaymentRequest)` with transaction limit validation ($10,000 threshold simulation)
- [x] `processOrderPayment(UUID orderId, UUID customerId, BigDecimal amount, String currency)`
- [x] `getPaymentByOrderId(UUID orderId, UUID customerId)`
- [x] `getPaymentById(UUID id, UUID customerId)`
- [x] `getCustomerPayments(UUID customerId)`
- [x] Custom exceptions and `GlobalExceptionHandler`

### Controller & OpenAPI

- [x] `POST /api/v1/payments/process`
- [x] `GET /api/v1/payments/order/{orderId}`
- [x] `GET /api/v1/payments/{id}`
- [x] `GET /api/v1/payments`
- [x] Swagger UI & OpenAPI 3.1 documentation configured

### Kafka Integration

- [x] `KafkaTopicConfig` defining payment topics
- [x] `InventoryReservedConsumer` listening to `inventory.reserved` (initiates payment)
- [x] `PaymentEventProducer` publishing `payment.completed` and `payment.failed`

---

## 🔔 Phase 7 — Notification Service (`services/notification-service` · port 8086)

> **Status: ✅ Complete** — Event-driven customer communications microservice with PostgreSQL persistence, Flyway migrations, Kafka consumers for order & payment sagas, consumer idempotency, mock email delivery, and REST audit inspection endpoints.

### Scaffold & Configuration

- [x] `NotificationServiceApplication.java`
- [x] `application.yaml` configured on port 8086 with PostgreSQL (`notification_db:5438`), Flyway, and Kafka consumer group
- [x] `build.gradle.kts` — Spring WebMvc, JPA, Flyway, PostgreSQL, Kafka, Event Contracts, Validation, OpenAPI Swagger UI

### Database & Migrations

- [x] `V1__create_notifications_and_processed_events.sql` — `notifications` and `processed_events` deduplication tables with indexes

### Domain & Persistence

- [x] `Notification` entity (id, order_id, customer_id, recipient_email, notification_type, subject, content, status, sent_at, created_at)
- [x] `NotificationType` enum (`ORDER_CREATED`, `PAYMENT_COMPLETED`, `PAYMENT_FAILED`, `ORDER_CANCELLED`)
- [x] `NotificationStatus` enum (`PENDING`, `SENT`, `FAILED`)
- [x] `NotificationRepository` (query by customer ID and order ID)

### Consumer Idempotency

- [x] `ProcessedEvent` entity & `ProcessedEventRepository`
- [x] `IdempotencyService` preventing duplicate email deliveries on Kafka consumer retries or rebalances

### Service & Email Delivery Layer

- [x] `EmailService` interface for pluggable email engines
- [x] `ResendEmailService` integrating with Resend REST API (`POST https://api.resend.com/emails`) using Spring's `RestClient`
- [x] `ResendProperties` type-safe configuration binding API key, sender email, sandbox recipient override, and API endpoint
- [x] `EmailServiceConfig` conditional wiring:
  - `@ConditionalOnExpression` instantiating `ResendEmailService` when `resend.api-key` is non-empty
  - `@ConditionalOnMissingBean` falling back to `MockEmailService` for local development and zero-config CI/CD tests
- [x] Developer sandbox recipient override via `RESEND_DEFAULT_RECIPIENT` (routes all test notifications to account owner's email)
- [x] `MockEmailService` logging formatted email notifications to console
- [x] `NotificationService` handling `handleOrderCreated`, `handlePaymentCompleted`, `handlePaymentFailed`, `handleOrderCancelled`
- [x] Customer and order notification history query methods

### Kafka Integration

- [x] `NotificationEventConsumer` listening to `order.created`, `payment.completed`, `payment.failed`, and `order.cancelled` with idempotency guardrails

### REST Controller & API Docs

- [x] `NotificationController` (`/api/v1/notifications`)
- [x] `GET /api/v1/notifications/customer/{customerId}`
- [x] `GET /api/v1/notifications/order/{orderId}`
- [x] OpenAPI 3.1 Swagger annotations

---

## 🌐 Phase 8 — API Gateway (`services/api-gateway` · port 8080)

> **Status: ✅ Complete** — Unified edge entry point for the e-commerce platform handling dynamic path routing, reverse proxying, centralized CORS, distributed tracing correlation IDs, and Redis-backed rate limiting with in-memory fallback.

### Scaffold & Configuration

- [x] `ApiGatewayApplication.java` with `@ConfigurationPropertiesScan`
- [x] `application.yaml` configured on port 8080 with route definitions, CORS settings, and rate limiting rules
- [x] `build.gradle.kts` — Spring WebMvc, Redis, Validation, Lombok

### Dynamic Routing & Reverse Proxying

- [x] `GatewayProperties` type-safe configuration mapping route prefixes to downstream target URIs
- [x] `GatewayProxyService` resolving routes, stripping hop-by-hop headers, injecting correlation headers, and relaying request/response payloads
- [x] Route: `auth-service` (`/api/v1/auth/**` ➔ port 8081)
- [x] Route: `product-service` (`/api/v1/products/**`, `/api/v1/categories/**` ➔ port 8082)
- [x] Route: `order-service` (`/api/v1/orders/**`, `/api/v1/cart/**`, `/api/v1/wishlist/**` ➔ port 8083)
- [x] Route: `payment-service` (`/api/v1/payments/**` ➔ port 8084)
- [x] Route: `inventory-service` (`/api/v1/inventory/**` ➔ port 8085)
- [x] Route: `notification-service` (`/api/v1/notifications/**` ➔ port 8086)
- [x] Route discovery endpoint (`GET /api/v1/gateway/routes`)

### Edge Concerns

- [x] `CorrelationIdFilter` (`OncePerRequestFilter`) generating and propagating `X-Correlation-ID` header across all downstream calls
- [x] Centralized `CorsConfig` allowing origins, methods, headers, credentials, and exposed headers
- [x] `RateLimiterService` enforcing request throttling (120 req/min) via Redis with in-memory fallback returning HTTP 429
- [x] `GatewayController` mapping all `/api/v1/**` requests with rate limit protection and proxy delegation

---

## 📨 Phase 9 — Kafka & Event-Driven Architecture

> **Status: 🟢 Infrastructure, Producers & Core Consumers Complete** — Kafka broker running in KRaft mode (`:9092`, `:9094`); shared `libs:event-contracts` module with type-safe event models and `KafkaTopics` constants; `OrderEventProducer` and `InventoryEventProducer` fully implemented; `OrderCreatedConsumer`, `InventoryEventConsumer`, and `PaymentEventConsumer` implemented with unit tests.

### Infrastructure

- [x] Kafka broker added to `docker-compose.yaml` (KRaft mode via `confluentinc/cp-kafka:7.5.0`)
- [x] Spring Kafka producer and consumer configurations in `order-service`, `inventory-service`, and `product-service` `application.yaml`
- [x] Kafka topic configuration beans (`KafkaTopicConfig.java` in `order-service` and `inventory-service`)
- [x] Shared Gradle module `libs:event-contracts` (`OrderCreatedEvent`, `OrderConfirmedEvent`, `OrderCancelledEvent`, `InventoryReservedEvent`, `ReservationFailedEvent`, `InventoryReleasedEvent`, `PaymentCompletedEvent`, `PaymentFailedEvent`, `SendNotificationEvent`)
- [x] Centralized topic constants in `libs:event-contracts` (`KafkaTopics.java`)

### Event Contracts (Topic Definitions)

- [x] `order.created` — published by Order Service
- [x] `order.confirmed` — published by Order Service
- [x] `order.cancelled` — published by Order Service
- [x] `inventory.reserved` — published by Inventory Service
- [x] `inventory.reservation.failed` — published by Inventory Service
- [x] `inventory.released` — published by Inventory Service
- [ ] `payment.initiated` — published by Payment Service
- [x] `payment.completed` — published by Payment Service (`PaymentEventProducer`)
- [x] `payment.failed` — published by Payment Service (`PaymentEventProducer`)

### Producers

- [x] Order Service → `OrderEventProducer` (`order.created`, `order.confirmed`, `order.cancelled`)
- [x] Inventory Service → `InventoryEventProducer` (`inventory.reserved`, `inventory.reservation.failed`, `inventory.released`)
- [x] Payment Service → `PaymentEventProducer` (`payment.completed`, `payment.failed`)

### Consumers

- [x] Inventory Service ← `order.created` (`OrderCreatedConsumer`)
- [x] Payment Service ← `inventory.reserved` (`InventoryReservedConsumer`)
- [x] Order Service ← `inventory.reserved` (`InventoryEventConsumer`)
- [x] Order Service ← `inventory.reservation.failed` (`InventoryEventConsumer`)
- [x] Order Service ← `payment.completed`, `payment.failed` (`PaymentEventConsumer`)
- [x] Notification Service ← `order.created`, `payment.completed`, `payment.failed`, `order.cancelled` (`NotificationEventConsumer`)

---

## 📤 Phase 10 — Transactional Outbox
 
> **Status: ✅ Done**
 
- [x] `outbox_events` table in Order Service DB (`V5__create_outbox_and_processed_events.sql`)
- [x] `outbox_events` table in Inventory Service DB (`V2__create_outbox_and_processed_events_and_reservations.sql`)
- [x] `outbox_events` table in Payment Service DB (`V2__create_outbox_and_processed_events.sql`)
- [x] Domain mutation + outbox record written in same DB transaction via `OutboxService`
- [x] Outbox poller / relay (`@Scheduled` in `OutboxPublisher`)
- [x] Outbox publisher — reads pending events, deserializes typed payload, publishes to Kafka, marks `published_at` and `PUBLISHED`
- [x] Retry logic with max retry threshold (5) marking status `FAILED` on exhaustion
 
---
 
## 🔁 Phase 11 — Saga Workflows (Distributed Transactions)
 
> **Status: ✅ Done**
 
### Order Placement Saga (Choreography)
 
- [x] Step 1: Order Service creates order → publishes `order.created` (persisted to outbox)
- [x] Step 2: Inventory Service reserves stock → publishes `inventory.reserved` or `inventory.reservation.failed`
- [x] Step 3a (success): Payment Service initiates payment → publishes `payment.completed` or `payment.failed`
- [x] Step 3b (failure): Order Service receives `inventory.reservation.failed` → cancels order
- [x] Step 4a (success): Order Service receives `payment.completed` → marks order `CONFIRMED`
- [x] Step 4b (failure): Order Service receives `payment.failed` → cancels order
- [x] Compensation: Inventory Service `SagaCompensationConsumer` releases reservations on `payment.failed` or `order.cancelled` and publishes `inventory.released`
 
### Compensation Flows
 
- [x] Inventory release on payment failure (`releaseOrderInventory` restores available stock & marks reservation `RELEASED`)
- [x] Order cancellation on any saga failure
- [x] Deduplication and safe no-op on duplicate compensation triggers
 
---
 
## 🔒 Phase 12 — Idempotency
 
> **Status: ✅ Done**
 
- [x] Idempotency strategy: `event_id` tracking across all consumers via `IdempotencyService`
- [x] `processed_events` table per consuming service (`event_id`, `event_type`, `consumer_group`, `processed_at`)
- [x] Consumer checks `processed_events` before processing (`isAlreadyProcessed`)
- [x] Duplicate event — safe no-op, logged, and skipped without double mutation
- [x] Idempotency enforced in Inventory Service consumer (`OrderCreatedConsumer`, `SagaCompensationConsumer`)
- [x] Idempotency enforced in Payment Service consumer (`InventoryReservedConsumer`)
- [x] Idempotency enforced in Order Service consumer (`InventoryEventConsumer`, `PaymentEventConsumer`)
- [x] Idempotency enforced in Notification Service consumer (`NotificationEventConsumer` with `processed_events` deduplication table)
 
---

## 🧪 Phase 13 — Testing

### Auth Service Tests

- [x] `AuthServiceApplicationTests` — context load test (stub)
- [x] `JwtTokenServiceTest` — unit test file exists
  - [x] Token generation assertion written
  - [x] Claims validation (`sub`, `email`, expiration, issuedAt) written
  - [x] `jwtTokenService` field properly initialized in `@BeforeEach`
  - [x] `roles` claim assertion active and verified
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

- [x] `InventoryServiceApplicationTests` — context load test (stub, disabled without live DB)
- [x] Unit tests for `InventoryService` (`InventoryServiceTest` — 16 tests covering create, reserve, release, confirm, add, update, multi-item order reservation, error cases)
- [x] Unit tests for `InventoryEventProducer` (`InventoryEventProducerTest` — 3 tests for `inventory.reserved`, `inventory.reservation.failed`, `inventory.released`)
- [x] Unit tests for `OrderCreatedConsumer` (`OrderCreatedConsumerTest` — 4 tests for success, insufficient stock, not found, invalid event)
- [ ] Integration tests for inventory endpoints
- [ ] Concurrent reservation tests (optimistic lock / race condition)

### Order Service Tests

- [x] `OrderServiceApplicationTests` — context load test (stub, disabled without live DB)
- [x] Unit tests for `CartService` (`CartServiceTest` — 17 unit tests covering authenticated Postgres flow, guest Redis flow, TTL, cart merge, move to wishlist, error handling)
- [x] Unit tests for `WishlistService` (`WishlistServiceTest` — 10 unit tests covering get, idempotent add, remove, move to cart)
- [x] Unit tests for Order checkout, state machine & saga handlers (`OrderServiceTest` — 19 tests, `OrderControllerTest` — 7 tests)
- [x] Unit tests for `OrderEventProducer` (`OrderEventProducerTest` — 3 tests for `order.created`, `order.cancelled`, `order.confirmed`)
- [x] Unit tests for `InventoryEventConsumer` (`InventoryEventConsumerTest` — 4 tests)
- [x] Unit tests for `PaymentEventConsumer` (`PaymentEventConsumerTest` — 4 tests)
- [x] Live end-to-end integration verified: Guest cart (Redis TTL), Auth cart (Postgres), Cart Merge, Wishlist move-between, 400 validation, 401 unauthenticated check
- [ ] Integration tests for Order endpoints (full HTTP stack with test DB)

### Payment Service Tests

- [x] `PaymentServiceApplicationTests` — context load test (stub, disabled without live DB)
- [x] Unit tests for `PaymentService` (`PaymentServiceTest` — 9 tests covering processing, transaction limit, idempotency, failure, and queries)
- [x] Unit tests for `PaymentEventProducer` (`PaymentEventProducerTest` — 2 tests for `payment.completed` and `payment.failed`)
- [x] Unit tests for `InventoryReservedConsumer` (`InventoryReservedConsumerTest` — 4 tests for event processing, error handling, null guards)
- [x] Controller slice tests for `PaymentController` (`PaymentControllerTest` — 5 tests for process, lookup by order, lookup by id, and customer list)
- [ ] Integration tests for payment endpoints (full HTTP stack with test DB)

### Notification Service Tests

- [x] Unit tests for `NotificationService` (`NotificationServiceTest` — 5 tests covering order created, payment completed, payment failed, order cancelled, and customer notifications)
- [x] Unit tests for `NotificationEventConsumer` (`NotificationEventConsumerTest` — 4 tests verifying Kafka consumer processing and idempotency duplicate skipping)
- [x] Controller slice tests for `NotificationController` (`NotificationControllerTest` — 4 tests for customer lookup, order lookup, and 404 not found handling)
- [x] End-to-end saga simulation test (`EndToEndNotificationSagaTest` — full lifecycle integration test)
- [x] Unit tests for `ResendEmailService` (`ResendEmailServiceTest` — 3 tests covering REST payload format, Bearer authorization, sandbox recipient override, and API error response handling)
- [x] Unit tests for `EmailServiceConfig` (`EmailServiceConfigTest` — 3 tests verifying conditional bean loading of `ResendEmailService` vs. `MockEmailService`)

### Kafka Tests

- [x] Event publication tests (producer) — `OrderEventProducerTest` & `InventoryEventProducerTest`
- [x] Event consumption tests (consumer) — `OrderCreatedConsumerTest`, `InventoryEventConsumerTest`, `PaymentEventConsumerTest`
- [ ] Duplicate event / idempotency tests
- [ ] Retry behavior tests
- [ ] Saga compensation tests
- [ ] End-to-end Kafka flow test with embedded/TestContainers Kafka

### End-to-End Tests (All Pending)

- [ ] Register → Verify → Login → Create Order → Reserve Inventory → Process Payment → Complete Order → Send Notification flow

---

## 🔍 Phase 14 — Observability & Production Hardening

### API Documentation (OpenAPI & Swagger UI)

- [x] `springdoc-openapi-starter-webmvc-ui:2.8.5` integrated across all active services
- [x] OpenAPI 3.1.0 JSON spec (`/v3/api-docs`) enabled
- [x] Interactive Swagger UI (`/swagger-ui.html` / `/swagger-ui/index.html`) enabled with "Try it out"
- [x] JWT Bearer authentication scheme (`bearerAuth`) configured in Swagger UI for authorizing protected endpoints
- [x] Security filter chains updated to permit `/v3/api-docs/**` and `/swagger-ui/**` across all services
- [x] Detailed `@Tag` and `@Operation` annotations on all controllers (Auth, Product, Inventory, Order/Cart/Wishlist)

### Actuator & Metrics (100% Configured)

- [x] Actuator dependency in all 7 services (Auth, Product, Order, Payment, Inventory, Notification, API Gateway)
- [x] `health`, `info`, `metrics`, and `prometheus` endpoints exposed across all services
- [x] Readiness and liveness health probes enabled (`management.endpoint.health.probes.enabled: true`)
- [x] Prometheus scrape endpoint (`/actuator/prometheus`) with Micrometer registry (`micrometer-registry-prometheus`)

### Distributed Tracing & Logging

- [x] Micrometer Tracing Brave bridge (`micrometer-tracing-bridge-brave`) configured in all services
- [x] Trace ID and Span ID MDC propagation in logs: `"%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"`
- [x] Correlation ID (`X-Correlation-ID`) propagation filter in API Gateway and downstream forwarders
- [x] Zipkin container (`openzipkin/zipkin:3.4`) added to `docker-compose.yaml` (port 9411)

### Observability Infrastructure & Dashboards

- [x] Prometheus container (`prom/prometheus:v2.54.1`) added to `docker-compose.yaml` (port 9090)
- [x] `docker/prometheus/prometheus.yml` scrape configuration for all 7 microservices + self
- [x] Grafana container (`grafana/grafana:11.2.0`) added to `docker-compose.yaml` (port 3000)
- [x] Automated Grafana datasource provisioning (`docker/grafana/provisioning/datasources/datasource.yml`)
- [x] Automated Grafana dashboard provider (`docker/grafana/provisioning/dashboards/dashboards.yml`)
- [x] Pre-built E-Commerce Metrics Dashboard (`docker/grafana/dashboards/ecommerce-overview.json`) displaying service health, request rates, max latencies, JVM memory, and DB connection pools

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
| 1 | ~~`jwtTokenService` field not assigned in `@BeforeEach` — NPE at test runtime~~ (Resolved) | None | `JwtTokenServiceTest.java` |
| 2 | ~~`reserveInventory` method missing `@Transactional`~~ (Resolved) | None | `InventoryService.java` |
| 3 | Refresh token expiry (30 days) hardcoded, not configurable via env | Low | `AuthServiceImpl.java:220` |
| 4 | Password reset token expiry (5 min) hardcoded | Low | `AuthServiceImpl.java:332` |
| 5 | Admin endpoint `@PreAuthorize` on Product Service needs explicit verification | Medium | `AdminProductController.java` |
| 6 | SMTP not configured — email delivery is likely a stub or console logger | Medium | `EmailServiceImpl.java` |
| 7 | ~~Kafka not in `docker-compose.yaml` despite being a declared dependency~~ (Resolved) | None | `docker-compose.yaml` |
| 8 | ~~Order Service `application.yaml` nearly empty — missing datasource, port~~ (Resolved) | None | `order-service/application.yaml` |
| 9 | ~~`roles` claim assertion in JWT test is commented out~~ (Resolved) | None | `JwtTokenServiceTest.java:58` |
| 10 | No CORS config in any service | Medium | All services |

---

## 📅 Recommended Implementation Order

```text
COMPLETED (Sprint 7: Kafka Infrastructure, Producers & Core Consumers)
 ├── [x] Order entity + OrderItem entity + Flyway migrations (V3 & V4)
 ├── [x] Order status state machine (PENDING, CONFIRMED, CANCELLED, COMPLETED)
 ├── [x] Checkout flow: convert active Cart -> Order & clear Cart
 ├── [x] Order REST endpoints (POST /api/v1/orders/checkout, GET /api/v1/orders)
 ├── [x] Add Kafka broker (KRaft mode) to docker-compose.yaml
 ├── [x] Shared event contracts library (`libs:event-contracts`) and `KafkaTopics`
 ├── [x] Order Service → publish OrderCreated, OrderConfirmed, OrderCancelled
 ├── [x] Inventory Service → publish InventoryReserved, ReservationFailed, InventoryReleased
 ├── [x] Inventory Service → consume order.created, atomic multi-item stock reservation, publish result
 ├── [x] Order Service → consume inventory.reserved / inventory.reservation.failed, update status
 ├── [x] Order Service → consume payment.completed / payment.failed, update status
 └── [x] Comprehensive test suites for all producers, consumers, and saga state transitions

COMPLETED (Sprint 8: Payment Service & Saga Integration)
 ├── [x] Payment Service foundation (PostgreSQL 5437, Flyway V1, Payment entity, repo, REST)
 ├── [x] Payment Service → consume inventory.reserved, process payment intent, publish result
 ├── [x] Payment Service → publish payment.completed / payment.failed
 └── [x] Comprehensive test suites (21 unit and slice tests passing)

COMPLETED (Sprint 9: Distributed Patterns — Outbox, Sagas & Idempotency)
 ├── [x] Transactional Outbox per service (Order, Inventory, Payment)
 ├── [x] Idempotency tables (`processed_events`) + duplicate checks per consumer
 └── [x] Saga compensation flows (Inventory release on payment failure)

COMPLETED (Sprint 11: Production Hardening & Full E2E Integration)
 ├── [x] Observability stack (Prometheus :9090, Grafana :3000, Zipkin :9411)
 ├── [x] Distributed tracing with Micrometer Tracing Brave bridge & standardized MDC logging
 ├── [x] Actuator & Prometheus scrape endpoints exposed on all 7 services
 └── [x] Comprehensive End-to-End saga and gateway integration test coverage
```

---

## 🔗 Quick Reference — Service Ports

| Service / Tool | Port | Database / Storage | Port |
|---|---|---|---|
| API Gateway | 8080 | Redis (Rate Limiter) | 6379 |
| Auth Service | 8081 | auth_db | 5433 |
| Product Service | 8082 | product_db | 5434 |
| Order Service | 8083 | order_db, Redis | 5435, 6379 |
| Payment Service | 8084 | payment_db | 5437 |
| Inventory Service | 8085 | inventory_db | 5436 |
| Notification Service | 8086 | notification_db | 5438 |
| Prometheus | 9090 | prometheus-data | — |
| Grafana | 3000 | grafana-data | — |
| Zipkin | 9411 | in-memory | — |
| Kafka UI | 8090 | — | — |
| pgAdmin | 5050 | pgadmin-data | — |
| Redis | 6379 | — | — |
| Kafka | 9092 (internal) / 9094 (external) | kafka-data | — |
