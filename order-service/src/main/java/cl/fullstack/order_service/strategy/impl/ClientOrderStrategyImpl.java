package cl.fullstack.order_service.strategy.impl;

import cl.fullstack.order_service.model.Order;
import cl.fullstack.order_service.model.OrderStatus;
import cl.fullstack.order_service.repository.OrderRepository;
import cl.fullstack.order_service.strategy.OrderRoleStrategy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Component
public class ClientOrderStrategyImpl implements OrderRoleStrategy {

    private final OrderRepository orderRepository;

    public ClientOrderStrategyImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public boolean supports(String role) {
        return "CLIENTE".equalsIgnoreCase(role)
                || "CLIENT".equalsIgnoreCase(role)
                || "CUSTOMER".equalsIgnoreCase(role)
                || "USER".equalsIgnoreCase(role);
    }

    @Override
    public List<Order> listOrders(UUID userId) {
        return orderRepository.findByUserId(userId);
    }

    @Override
    public Order getOrderById(UUID userId, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Pedido no encontrado con ID: " + orderId
                ));

        if (!order.getUserId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tienes permiso para ver este pedido"
            );
        }

        return order;
    }

    @Override
    public Order updateOrderStatus(UUID userId, UUID orderId, OrderStatus status) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "El cliente no puede cambiar el estado del pedido"
        );
    }
}
