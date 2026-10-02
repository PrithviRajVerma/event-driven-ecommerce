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
                              │     Planned      │
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

------------------------------------------------------------------------

# 🚧 Project Status

The project is being developed incrementally.

### Implemented

-   Authentication and security foundation
-   Email/password registration and login
-   Email verification
-   Refresh-token rotation
-   Logout/session revocation
-   Password reset
-   Google OAuth2 login
-   RBAC foundation
-   Redis rate limiting
-   PostgreSQL database-per-service foundation
-   Flyway migrations
-   Product Service
-   Product CRUD
-   Product soft delete
-   Product PUT/PATCH APIs
-   Product image URL support
-   Customer/admin product API separation

### Currently being developed

-   Product Service JWT authentication
-   Product Service authorization
-   Order Service
-   Inventory Service
-   Payment Service
-   Kafka event-driven workflows
-   Transactional Outbox
-   Saga workflows
-   Idempotency
-   Notification Service
-   API Gateway
-   Observability
-   Production hardening
-   Full end-to-end testing

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

Inventory Service will own stock and reservation state.

Planned responsibilities:

-   Stock management
-   Availability
-   Stock reservation
-   Stock release
-   Inventory confirmation
-   Inventory-related events

The final architecture makes Inventory the source of truth for stock.

------------------------------------------------------------------------

# Order Service

Order Service will own order state and lifecycle.

Planned responsibilities:

-   Order creation
-   Order retrieval
-   Order status transitions
-   Order item snapshots
-   Customer ownership
-   Coordination with Inventory and Payment

------------------------------------------------------------------------

# Payment Service

Payment Service will own payment state.

Planned responsibilities:

-   Payment creation
-   Payment status
-   Payment confirmation
-   Payment failure
-   Payment events
-   External payment-provider integration boundary

------------------------------------------------------------------------

# Notification Service

Notification Service will consume business events and handle
asynchronous notifications.

Planned responsibilities:

-   Email notifications
-   Order confirmation
-   Payment notifications
-   Failure notifications

The notification workflow should not block the core business
transaction.

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

# Observability

Spring Boot Actuator is part of the production-hardening layer.

Planned observability includes:

-   Health checks
-   Readiness/liveness
-   Application metrics
-   Operational visibility
-   Distributed workflow visibility

------------------------------------------------------------------------

# Project Structure

``` text
event-driven-ecommerce/
│
├── auth-service/
├── product-service/
├── order-service/
├── inventory-service/
├── payment-service/
├── notification-service/
├── api-gateway/
├── docker-compose.yml
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
