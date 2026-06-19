package cl.fullstack.order_service.strategy;

import cl.fullstack.order_service.model.Order;
import cl.fullstack.order_service.model.OrderStatus;

import java.util.List;
import java.util.UUID;

public interface OrderRoleStrategy {

    boolean supports(String role);

    List<Order> listOrders(UUID userId);

    Order getOrderById(UUID userId, UUID orderId);

    Order updateOrderStatus(UUID userId, UUID orderId, OrderStatus status);
}