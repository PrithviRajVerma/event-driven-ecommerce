# Event-Driven E-Commerce Microservices Platform

A production-oriented e-commerce backend built with **Java 21** and
**Spring Boot**, designed to demonstrate secure authentication,
microservices architecture, event-driven communication, distributed
transactions, idempotency, and fault-tolerant workflows.

**Status:** 🚧 Active development

**Tech:** Java 21 · Spring Boot · PostgreSQL · Kafka · Redis · Docker ·
JWT · OAuth2

------------------------------------------------------------------------

## Architecture

``` text
                              ┌──────────────────┐
                              │   API Gateway    │
                              │    (port 8080)   │
                              └────────┬─────────┘
                                       │
              ┌────────────────────────┼────────────────────────┐
              │                        │                        │
              ▼                        ▼                        ▼
       ┌──────────────┐        ┌────────────────┐        ┌──────────────┐
       │ Auth Service │        │ Product Service │        │ Order Service│
       └──────┬───────┘        └───────┬────────┘        └──────┬───────┘
              │                         │                        │
          auth_db                  product_db                order_db
                                                                  │
                                    ┌─────────────────────────────┤
                                    │                             │
                                    ▼                             ▼
                           ┌─────────────────┐           ┌────────────────┐
                           │ Inventory       │           │ Payment        │
                           │ Service         │           │ Service        │
                           └───────┬─────────┘           └───────┬────────┘
                                   │                             │
                             inventory_db                    payment_db
                                   │                             │
                                   └──────────────┬──────────────┘
                                                  ▼
                                           ┌─────────────┐
                                           │    Kafka    │
                                           │ Event Bus   │
                                           └──────┬──────┘
                                                  │
                                                  ▼
                                       ┌────────────────────┐
                                       │ Notification       │
                                       │ Service            │
                                       └────────────────────┘

                         Redis
                  Rate limiting / temporary state
```

The architecture follows a **database-per-service** model. Each service
owns its data and business logic.

------------------------------------------------------------------------

## Project Goals

This project is being built to demonstrate production-oriented
microservice and distributed-system concepts:

-   Secure authentication and authorization
-   JWT-based security
-   HttpOnly cookie-based authentication
-   Email/password authentication
-   Email verification
-   Password reset
-   Google OAuth2
-   Refresh-token rotation and session revocation
-   Role-based access control
-   Database-per-service architecture
-   RESTful APIs
-   Product/catalog management
-   Inventory ownership
-   Apache Kafka event-driven communication
-   Transactional Outbox
-   Saga-style distributed workflows
-   Idempotency
-   Failure recovery
-   Redis-based rate limiting
-   Flyway schema migrations
-   Observability
-   Automated and integration testing

See [decision.md](file:///home/assassin/MY_WORK/Code/resume-projects/event-driven-eccomerce/decision.md) for detailed Architectural Decision Records (ADRs) explaining every major design decision and trade-off in this codebase.

------------------------------------------------------------------------

# 🚧 Project Status

The project is being developed incrementally.

### Implemented
-   **Auth Service (`:8081`):** Email/password registration, login, email verification, refresh-token rotation, session revocation, password reset, Google OAuth2, RBAC, HttpOnly cookies.
-   **Product Service (`:8082`):** Category/product CRUD, pagination, sorting, soft deletion, search, image URL support, customer/admin API separation.
-   **Inventory Service (`:8085`):** Stock level tracking, reserved stock management, low-stock threshold alerting, saga-driven reservations, and compensating stock releases.
-   **Order Service (`:8083`):** Redis-backed Cart & Wishlist, checkout orchestration, order state machine, snapshots, cancellation workflows.
-   **Payment Service (`:8084`):** Payment lifecycle management, mock payment gateway, transaction logs, success/failure event publishing.
-   **Notification Service (`:8086`):** Asynchronous email notifications for order & payment sagas, PostgreSQL delivery audit logs, Resend REST API integration, and developer sandbox safety overrides.
-   **API Gateway (`:8080`):** Centralized edge reverse proxy, dynamic route discovery, CORS configuration, Redis token-bucket rate limiting (120 req/min), and `X-Correlation-ID` distributed tracing injection.
-   **Kafka Event-Driven Backbone:** KRaft-based Apache Kafka broker, typed `event-contracts` library, event producers and consumers.
-   **Transactional Outbox Pattern:** Outbox tables in Order, Inventory, and Payment services with scheduled relay background workers guaranteeing at-least-once message delivery.
-   **Choreographed Sagas:** Distributed order placement saga (`OrderCreated` ➔ `InventoryReserved` ➔ `PaymentCompleted` ➔ `OrderCompleted` + `Notifications`) with compensating transactions (`InventoryReservationFailed` / `PaymentFailed` ➔ `ReleaseInventory` + `OrderCancelled`).
-   **Consumer Idempotency:** Event deduplication tables (`processed_events`) and idempotency services in every consuming service preventing duplicate execution.
-   **Kafka Web UI (`:8090`):** Real-time web dashboard for browsing topics, messages, consumer lag, and broker partitions.
-   **Full-Stack Containerization:** Multi-stage production Dockerfiles for all 7 microservices (`eclipse-temurin:21-jre-alpine`, non-root user, container memory limits) + `docker-compose.full.yml` running the entire stack.
-   **CI/CD Automation:** Production GitHub Actions workflow template (`deploy/ci.yml`) automatically validating Gradle tests, bootJar builds, and Docker container builds on every push/PR.
-   **Observability & Tracing:** Actuator Prometheus metrics scraping (`:9090`), Zipkin distributed tracing (`:9411`), and automated Grafana dashboards (`:3000`).
-   **Consumer Resilience:** Poison-pill isolation and retry backoff policies configured via `KafkaConsumerConfig`.

### Future Enhancements / Extensions
-   Resilience4j Circuit Breakers & Bulkheads (external email / payment integrations)
-   Elasticsearch cluster for full-text product search and faceted filtering
-   Kubernetes Helm charts for cloud-native enterprise deployments

------------------------------------------------------------------------

# Technology Stack

  Category           Technology
  ------------------ -----------------------------
  Language           Java 21
  Framework          Spring Boot 4.1.1
  Build              Gradle Kotlin DSL
  Web                Spring Web MVC
  Security           Spring Security
  Authentication     JWT / JJWT
  OAuth              Google OAuth2
  Password hashing   BCrypt
  Persistence        Spring Data JPA / Hibernate
  Database           PostgreSQL 18.6
  Migrations         Flyway
  Messaging          Apache Kafka
  Rate limiting      Redis
  Containers         Docker / Docker Compose
  API testing        Postman

------------------------------------------------------------------------

# Services

## Auth Service

The Auth Service owns identity and authentication state.

### Responsibilities

-   User registration
-   Email verification
-   Resend verification
-   Login
-   JWT access-token generation
-   Refresh-token rotation
-   Refresh-session persistence
-   Logout and session revocation
-   Forgot-password
-   Password reset
-   Google OAuth2
-   Role assignment
-   Account status
-   Authentication rate limiting

### Authentication Flow

``` text
Client
  │
  │ Login
  ▼
Auth Service
  │
  ├── Access JWT
  └── Refresh Token
          │
          ▼
     HttpOnly Cookies
```

The application uses HttpOnly cookies for access and refresh tokens.

### JWT

Access tokens contain:

``` text
sub   → user UUID
email → user email
roles → user roles
iss   → auth-service
iat   → issued-at
exp   → expiration
```

Downstream services validate JWTs locally rather than making a network
call to Auth Service for every request.

### Refresh Token Rotation

``` text
Refresh Token A
      │
      ▼
Validate session
      │
      ▼
Revoke A
      │
      ▼
Generate Refresh Token B
      │
      ▼
Store hash(B)
      │
      ▼
Return B
```

Refresh tokens are stored as hashes in PostgreSQL.

------------------------------------------------------------------------

# Product Service

Product Service owns product/catalog information.

### Customer API

``` http
GET /api/v1/products
GET /api/v1/products/{id}
```

Only active products are exposed through the customer catalog.

### Admin API

``` http
POST   /api/v1/admin/products
GET    /api/v1/admin/products
GET    /api/v1/admin/products/{id}
PUT    /api/v1/admin/products/{id}
PATCH  /api/v1/admin/products/{id}
DELETE /api/v1/admin/products/{id}
```

### Product Data

Product Service owns:

``` text
id
name
description
price
currency
active
imageUrl
createdAt
updatedAt
```

Inventory will own:

``` text
productId
stockQuantity
reservation data
```

This separates catalog ownership from inventory ownership.

### PUT vs PATCH

`PUT` updates the complete editable product representation.

`PATCH` updates only the fields supplied by the client.

Example:

``` json
{
  "price": 6999.99
}
```

### Soft Delete

Products are soft deleted:

``` text
DELETE /products/{id}
        │
        ▼
active = false
```

The product remains available to administrative/history-related
workflows.

------------------------------------------------------------------------

# Inventory Service

Inventory Service owns stock and reservation state, acting as the single source of truth for stock quantities.

**Implemented Capabilities:**
-   **Stock Management:** Real-time stock levels, reserved quantities, and low-stock alerting (`inventory_db:5436`).
-   **REST Endpoints:** `GET /api/v1/inventory/{productId}`, `PUT /api/v1/inventory/{productId}`, `GET /api/v1/inventory/low-stock`.
-   **Saga Orchestration:**
    -   Consumes `OrderCreatedEvent` (`order.created`) with `processed_events` idempotency guardrails.
    -   Reserves inventory atomically and writes `InventoryReservedEvent` to `outbox_events`.
    -   If stock is insufficient, emits `InventoryReservationFailedEvent` (`inventory.reservation.failed`).
    -   Consumes `OrderCancelledEvent` (`order.cancelled`) to execute compensating inventory releases.

------------------------------------------------------------------------

# Order Service

Order Service manages customer carts, wishlists, and order lifecycle state machines.

**Implemented Capabilities:**
-   **Cart & Wishlist:** Redis-backed temporary state management (`localhost:6379`).
-   **Order Management:** Checkout orchestration, immutable order item price/name snapshots, customer ownership verification (`order_db:5435`).
-   **REST Endpoints:** Checkout (`POST /api/v1/orders/checkout`), order lookup (`GET /api/v1/orders/{id}`), customer history (`GET /api/v1/orders/customer/{customerId}`), order cancellation (`POST /api/v1/orders/{id}/cancel`).
-   **Transactional Outbox & Saga Coordination:**
    -   Order placement and `OrderCreatedEvent` written in a single database transaction.
    -   Scheduled `OutboxPublisher` background worker relays outbox events to Kafka (`order.created`).
    -   Consumes `InventoryReservedEvent`, `InventoryReservationFailedEvent`, `PaymentCompletedEvent`, and `PaymentFailedEvent` with idempotency deduplication.
    -   Transitions order state (`PENDING` ➔ `CONFIRMED` or `CANCELLED`).

------------------------------------------------------------------------

# Payment Service

Payment Service manages payment transactions and external payment-provider integration boundaries.

**Implemented Capabilities:**
-   **Transaction Records:** Idempotent payment processing, currency tracking, transaction limits (`payment_db:5437`).
-   **REST Endpoints:** Process payment (`POST /api/v1/payments/process`), lookup by ID (`GET /api/v1/payments/{paymentId}`), lookup by order (`GET /api/v1/payments/order/{orderId}`).
-   **Saga Integration:**
    -   Consumes `InventoryReservedEvent` (`inventory.reserved`) with idempotent deduplication.
    -   Executes payment rules ($5,000 transaction limits, card simulations).
    -   Writes payment records and `PaymentCompletedEvent` / `PaymentFailedEvent` to `outbox_events` atomically.
    -   Scheduled `OutboxPublisher` broadcasts events to Kafka.

------------------------------------------------------------------------

# Notification Service

Notification Service consumes asynchronous business events from Kafka and handles
customer email dispatches without blocking the core business transaction.

**Implemented Capabilities:**
-   **Kafka Consumers:** Listens to `OrderCreatedEvent`, `PaymentCompletedEvent`, `PaymentFailedEvent`, and `OrderCancelledEvent`.
-   **Audit & Tracking:** Persists notification history to `notification_db` using Flyway migrations with customer/order lookups.
-   **Pluggable Email Engine:**
    -   **Resend Integration (`ResendEmailService`):** Sends real emails via the Resend REST API (`POST https://api.resend.com/emails`) using Spring's `RestClient`.
    -   **Sandbox Safety (`defaultRecipient`):** Routes emails to your verified account address when testing in the free developer sandbox.
    -   **Zero-Config Fallback (`MockEmailService`):** Automatically active when `RESEND_API_KEY` is omitted, logging dispatch events locally without failing offline builds or CI/CD pipelines.

**Email Configuration:**
```bash
# Optional: Set to send live emails via Resend
export RESEND_API_KEY="re_123456789"
export RESEND_FROM_EMAIL="onboarding@resend.dev" # or verified domain e.g. orders@yourdomain.com
export RESEND_DEFAULT_RECIPIENT="you@example.com" # overrides recipient for sandbox testing
```

------------------------------------------------------------------------

# API Gateway

The API Gateway (`port 8080`) provides a unified edge entry point for all client requests across the microservices platform.

**Implemented Capabilities:**
-   **Dynamic Reverse Proxy:** Routes requests to downstream microservices based on prefix matching:
    -   `/api/v1/auth/**` ➔ Auth Service (`:8081`)
    -   `/api/v1/products/**`, `/api/v1/categories/**` ➔ Product Service (`:8082`)
    -   `/api/v1/orders/**`, `/api/v1/cart/**`, `/api/v1/wishlist/**` ➔ Order Service (`:8083`)
    -   `/api/v1/payments/**` ➔ Payment Service (`:8084`)
    -   `/api/v1/inventory/**` ➔ Inventory Service (`:8085`)
    -   `/api/v1/notifications/**` ➔ Notification Service (`:8086`)
-   **Route Discovery:** `GET /api/v1/gateway/routes` exposes active routes and target URIs.
-   **Edge Rate Limiting:** Enforces token-bucket rate limiting (120 req/min) via Redis with in-memory fallback, returning HTTP 429 Too Many Requests when limits are exceeded.
-   **Centralized CORS:** Pre-configured cross-origin resource sharing allowing frontend clients with credentials support.
-   **Distributed Tracing Propagation:** `CorrelationIdFilter` generates and injects `X-Correlation-ID` headers to trace requests across downstream services.

------------------------------------------------------------------------

# Event-Driven Architecture

Kafka will act as the asynchronous event backbone.

A future workflow can look like:

``` text
Order Service
      │
      │ OrderCreated
      ▼
    Kafka
      │
      ├──────────► Inventory Service
      │
      ├──────────► Payment Service
      │
      └──────────► Notification Service
```

Services retain ownership of their own domain state while communicating
through explicit event contracts.

------------------------------------------------------------------------

# Distributed Transactions

The project avoids distributed database transactions.

Cross-service workflows will use:

-   Events
-   Saga-style coordination
-   Transactional Outbox
-   Idempotent consumers
-   Retry/recovery strategies

Example:

``` text
Create Order
     │
     ▼
Reserve Inventory
     │
     ├── success ──► Process Payment
     │
     └── failure ──► Compensate / cancel order
```

------------------------------------------------------------------------

# Transactional Outbox

The Transactional Outbox pattern will provide reliable event
publication.

``` text
Business Transaction
        │
        ├── Domain Data
        │
        └── Outbox Event
                │
                ▼
         Outbox Publisher
                │
                ▼
              Kafka
```

The domain change and outgoing event are persisted in the same database
transaction before the event is published.

------------------------------------------------------------------------

# Idempotency

Event consumers and retried operations must tolerate duplicate delivery.

``` text
Incoming Event
      │
      ▼
Check Idempotency State
      │
      ├── Already processed → Ignore safely
      │
      └── New event → Process
```

This prevents duplicate messages from causing duplicate business
operations.

------------------------------------------------------------------------

# Redis

Redis is used only where it provides a clear architectural benefit.

Current use:

-   API rate limiting
-   Short-lived counters
-   Temporary state where appropriate

Redis is **not** the source of truth for:

-   Users
-   Products
-   Orders
-   Inventory
-   Payments
-   Refresh sessions

PostgreSQL remains the persistent source of truth for service-owned
data.

------------------------------------------------------------------------

# Database Architecture

Each service owns a separate PostgreSQL database.

``` text
Auth       → auth_db       → localhost:5433
Product    → product_db    → localhost:5434
Order      → order_db      → localhost:5435
Inventory  → inventory_db  → localhost:5436
Payment    → payment_db    → localhost:5437
```

The database-per-service model prevents services from directly sharing
persistence state.

------------------------------------------------------------------------

# Key Engineering Decisions

## Database per service

Each service owns its database and persistence model.

## Local JWT validation

Downstream services validate JWTs locally using the signing secret
instead of calling Auth Service for every request.

## HttpOnly cookies

Access and refresh tokens are stored in HttpOnly cookies so application
JavaScript cannot directly access them.

## Redis is selective

Redis is used for rate limiting and short-lived state rather than
becoming a primary database.

## Inventory owns stock

Product Service owns catalog data. Inventory Service owns stock and
reservations.

## Flyway + schema validation

Flyway manages explicit schema evolution while Hibernate validates the
database schema:

``` yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

## Thin API Gateway

The gateway is intended for edge concerns. Downstream services remain
responsible for authentication, authorization, and business rules.

## Incremental architecture

The project is developed in vertical slices. Stable CRUD and security
foundations are established before adding distributed event-driven
complexity.

------------------------------------------------------------------------

# Database Migrations

Flyway manages schema changes.

Example:

``` text
V1__create_products_table.sql
V2__add_image_url_to_products.sql
V3__remove_stock_quantity_from_products.sql
```

Applied migrations should not be edited retroactively.

New schema changes should be introduced through new migrations.

------------------------------------------------------------------------

# Local Development

## Prerequisites

-   Java 21
-   Docker
-   Docker Compose
-   Git
-   Postman

## Start Infrastructure

``` bash
docker compose up -d
```

Check containers:

``` bash
docker compose ps
```

## Environment Variables

Sensitive configuration is provided through environment variables.

Important variables include:

``` text
JWT_SECRET
GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET
```

Never commit real credentials or secrets to Git.

------------------------------------------------------------------------

# API Testing

Postman is used for API testing during development.

Current Product Service endpoints:

``` text
GET    http://localhost:8082/api/v1/products
GET    http://localhost:8082/api/v1/products/{id}

POST   http://localhost:8082/api/v1/admin/products
GET    http://localhost:8082/api/v1/admin/products
GET    http://localhost:8082/api/v1/admin/products/{id}
PUT    http://localhost:8082/api/v1/admin/products/{id}
PATCH  http://localhost:8082/api/v1/admin/products/{id}
DELETE http://localhost:8082/api/v1/admin/products/{id}
```

------------------------------------------------------------------------

# Testing Strategy

## Unit Tests

Business logic such as:

-   JWT handling
-   Token hashing
-   Service logic
-   Validation-related behavior

## Persistence Tests

-   JPA mappings
-   Repository behavior
-   PostgreSQL-specific types
-   Flyway migrations

## Integration Tests

``` text
HTTP Request
     │
     ▼
Controller
     │
     ▼
Service
     │
     ▼
Repository
     │
     ▼
PostgreSQL
```

## Security Tests

Planned/implemented coverage includes:

-   Unauthenticated access
-   Authenticated access
-   Role restrictions
-   Invalid JWT
-   Expired JWT
-   Refresh rotation
-   Revoked refresh tokens
-   Password-reset session revocation

## Kafka Tests

Planned:

-   Event publication
-   Event consumption
-   Duplicate event handling
-   Retry behavior
-   Failure recovery

## End-to-End Tests

The eventual order workflow will cover:

``` text
Register
   ↓
Verify
   ↓
Login
   ↓
Create Order
   ↓
Reserve Inventory
   ↓
Process Payment
   ↓
Complete Order
   ↓
Send Notification
```

------------------------------------------------------------------------

# Service Discovery

Eureka is intentionally not used.

Local development uses explicit service URLs.

When deployed to Kubernetes, native Kubernetes Service DNS can provide
service discovery without introducing another registry.

------------------------------------------------------------------------

# Observability & Distributed Tracing

The platform features an end-to-end observability stack combining Spring Boot Actuator, Micrometer, Prometheus, Grafana, and Zipkin:

-   **Metrics Scraping (Prometheus `:9090`):** Automatically scrapes `/actuator/prometheus` across all 7 services every 5 seconds.
-   **Distributed Tracing (Zipkin `:9411`):** Every request receives an `X-Correlation-ID` at the API Gateway. Downstream HTTP calls and Kafka event headers propagate Brave trace/span contexts to Zipkin.
-   **Visual Dashboards (Grafana `:3000`):** Pre-provisioned Prometheus & Zipkin datasources with a live overview dashboard tracking service health, request rates, P95/P99 latencies, JVM memory, and HikariCP connection pools.

------------------------------------------------------------------------

# How to Run & Deploy

### Option 1: Full-Stack Docker Deployment (Recommended)

Run all 6 databases, Kafka, Redis, Prometheus, Grafana, Zipkin, Kafka UI, and all 7 Spring Boot microservices together in isolated containers:

```bash
# 1. Compile all service JARs
./gradlew bootJar -x test

# 2. Boot the complete platform with health checks
docker compose -f docker-compose.full.yml up --build -d

# 3. View running services
docker compose -f docker-compose.full.yml ps
```

**Port & Documentation Matrix:**
| Component | Port | Endpoint / URL | Swagger UI / Documentation |
| :--- | :--- | :--- | :--- |
| **API Gateway** | 8080 | http://localhost:8080 | Edge reverse proxy for all `/api/v1/*` routes |
| **Auth Service** | 8081 | http://localhost:8081 | [Swagger UI](http://localhost:8081/swagger-ui.html) (`/v3/api-docs`) |
| **Product Service** | 8082 | http://localhost:8082 | [Swagger UI](http://localhost:8082/swagger-ui.html) (`/v3/api-docs`) |
| **Order Service** | 8083 | http://localhost:8083 | [Swagger UI](http://localhost:8083/swagger-ui.html) (`/v3/api-docs`) |
| **Payment Service** | 8084 | http://localhost:8084 | [Swagger UI](http://localhost:8084/swagger-ui.html) (`/v3/api-docs`) |
| **Inventory Service** | 8085 | http://localhost:8085 | [Swagger UI](http://localhost:8085/swagger-ui.html) (`/v3/api-docs`) |
| **Notification Service** | 8086 | http://localhost:8086 | Resend REST API / Audit Delivery Logs |
| **Kafka Web UI** | 8090 | http://localhost:8090 | Topic browser, partitions & consumer lag |
| **Grafana Dashboards** | 3000 | http://localhost:3000 | Pre-built dashboards (`admin`/`admin`) |
| **Prometheus** | 9090 | http://localhost:9090 | Metric scrapers & target health explorer |
| **Zipkin Tracing** | 9411 | http://localhost:9411 | Distributed trace waterfall visualization |
| **pgAdmin** | 5050 | http://localhost:5050 | Database UI (`admin@ecommerce.com` / `admin`) |

### Option 2: Local Microservices with Docker Infrastructure

If running microservices locally via IDE or Gradle:

```bash
# 1. Start supporting infrastructure
docker compose up -d

# 2. Run any service via Gradle
./gradlew :services:api-gateway:bootRun
./gradlew :services:auth-service:bootRun
./gradlew :services:order-service:bootRun
```

### Option 3: 100% Free Managed Cloud PaaS (Render / Railway + Neon + Upstash)

Deploy the entire architecture across managed cloud platforms with **zero Linux server administration**:
- **PostgreSQL Databases:** [Neon.tech](https://neon.tech) (Serverless, SSL)
- **Redis Cache & Rate Limiter:** [Upstash Redis](https://upstash.com) (Serverless, TLS)
- **Kafka Event Broker:** [Upstash Kafka](https://upstash.com) (Serverless, SASL_SSL)
- **Microservice Containers:** [Render.com](https://render.com) (via [`render.yaml`](file:///home/assassin/MY_WORK/Code/resume-projects/event-driven-eccomerce/render.yaml)) or [Railway.app](https://railway.app)
- **Email Service:** [Resend](https://resend.com)

See the complete step-by-step guide: [Managed Cloud PaaS Deployment Guide](file:///home/assassin/MY_WORK/Code/resume-projects/event-driven-eccomerce/deploy/MANAGED_CLOUD_PAAS_GUIDE.md).

------------------------------------------------------------------------

# Project Structure

``` text
event-driven-ecommerce/
│
├── libs/
│   └── event-contracts/        # Shared event contracts, topics & payload POJOs
│
├── services/
│   ├── api-gateway/            # Spring Cloud Gateway edge reverse proxy (:8080)
│   ├── auth-service/           # JWT, OAuth2, RBAC, session management (:8081)
│   ├── product-service/        # Catalog, categories, pagination, soft-delete (:8082)
│   ├── order-service/          # Carts, checkouts, saga initiator (:8083)
│   ├── payment-service/        # Payment processing & saga worker (:8084)
│   ├── inventory-service/      # Optimistic locking stock reservation (:8085)
│   └── notification-service/   # Resend email notifications & delivery logs (:8086)
│
├── deploy/
│   ├── ci.yml                  # GitHub Actions CI/CD pipeline template
│   └── MANAGED_CLOUD_PAAS_GUIDE.md # Cloud PaaS deployment guide (Neon, Upstash, Render)
│
├── docker/                     # Prometheus scrape configs & Grafana dashboards
├── docker-compose.yaml         # Local infrastructure (Databases, Kafka, Redis, UI)
├── docker-compose.full.yml     # Complete full-stack local orchestration
├── render.yaml                 # 1-Click Render.com Blueprint (IaC)
├── decision.md                 # Architecture Decision Records (ADRs)
├── implementation.md           # Implementation tracker & sprint history
└── README.md
```

Each service owns its:

-   application code
-   configuration
-   database
-   migrations
-   domain logic
-   persistence layer

Shared code is intentionally limited, especially for domain models.

------------------------------------------------------------------------

# Roadmap

``` text
AUTH / SECURITY
       │
       ▼
PRODUCT SERVICE
       │
       ▼
ORDER SERVICE
       │
       ▼
INVENTORY + PAYMENT
       │
       ▼
KAFKA
       │
       ▼
OUTBOX + SAGA
       │
       ▼
IDEMPOTENCY
       │
       ▼
NOTIFICATIONS
       │
       ▼
API GATEWAY
       │
       ▼
OBSERVABILITY + TESTING
       │
       ▼
PRODUCTION HARDENING
```

The implementation intentionally introduces distributed-system
complexity only after the underlying service foundations are stable.

------------------------------------------------------------------------

# Why This Project?

This project goes beyond basic CRUD APIs to demonstrate practical
distributed-system engineering.

It focuses on:

-   Authentication across services
-   Authorization and RBAC
-   Secure session management
-   Database ownership
-   Asynchronous communication
-   Reliable event publication
-   Distributed transaction coordination
-   Duplicate message handling
-   Failure recovery
-   Schema evolution
-   Observability
-   Production-oriented testing

The goal is to demonstrate both strong backend engineering fundamentals
and practical microservice/distributed-system design.

------------------------------------------------------------------------

## License

This project is currently intended as a personal portfolio and learning
project.
