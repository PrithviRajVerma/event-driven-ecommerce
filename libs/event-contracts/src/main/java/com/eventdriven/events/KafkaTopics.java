package com.eventdriven.events;

public final class KafkaTopics {

    private KafkaTopics() {
        // Prevent instantiation
    }

    // Order topics
    public static final String ORDER_CREATED = "order.created";
    public static final String ORDER_CONFIRMED = "order.confirmed";
    public static final String ORDER_CANCELLED = "order.cancelled";

    // Inventory topics
    public static final String INVENTORY_RESERVED = "inventory.reserved";
    public static final String INVENTORY_RESERVATION_FAILED = "inventory.reservation.failed";
    public static final String INVENTORY_RELEASED = "inventory.released";

    // Payment topics
    public static final String PAYMENT_INITIATED = "payment.initiated";
    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String PAYMENT_FAILED = "payment.failed";

    // Notification topics
    public static final String NOTIFICATION_SEND = "notification.send";
}
