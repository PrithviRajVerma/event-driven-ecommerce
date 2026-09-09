rootProject.name = "event-driven-ecommerce"

include(
    "services:api-gateway",
    "services:auth-service",
    "services:inventory-service",
    "services:notification-service",
    "services:order-service",
    "services:product-service",
    "services:payment-service",
)
