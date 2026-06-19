package cl.fullstack.order_service.model;

public enum OrderStatus {
    CREATED,
    PAYMENT_PENDING,
    PAYMENT_REJECTED,
    STOCK_VALIDATION_PENDING,
    PAID,
    SHIPMENT_CREATED,
    SHIPMENT_PENDING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    FAILED
}
