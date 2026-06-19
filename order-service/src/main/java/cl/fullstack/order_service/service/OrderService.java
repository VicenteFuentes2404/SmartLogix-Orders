package cl.fullstack.order_service.service;

import cl.fullstack.order_service.dto.OrderRequestDTO;
import cl.fullstack.order_service.model.Order;
import cl.fullstack.order_service.model.OrderStatus;
import cl.fullstack.order_service.repository.OrderRepository;
import cl.fullstack.order_service.saga.OrderSagaOrchestrator;
import cl.fullstack.order_service.strategy.OrderRoleStrategy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final List<OrderRoleStrategy> orderRoleStrategies;
    private final OrderSagaOrchestrator orderSagaOrchestrator;

    public OrderService(
            OrderRepository orderRepository,
            List<OrderRoleStrategy> orderRoleStrategies,
            OrderSagaOrchestrator orderSagaOrchestrator
    ) {
        this.orderRepository = orderRepository;
        this.orderRoleStrategies = orderRoleStrategies;
        this.orderSagaOrchestrator = orderSagaOrchestrator;
    }

    public Order createOrder(OrderRequestDTO request, UUID userId) {
        return orderSagaOrchestrator.createOrder(request, userId);
    }

    public List<Order> listOrders(UUID userId, String role) {
        return getStrategy(role).listOrders(userId);
    }

    public Order getOrderById(UUID userId, String role, UUID orderId) {
        return getStrategy(role).getOrderById(userId, orderId);
    }

    public Order updateOrderStatus(UUID userId, String role, UUID orderId, OrderStatus status) {
        return getStrategy(role).updateOrderStatus(userId, orderId, status);
    }

    public Order markDelivered(UUID orderId, UUID shipmentId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Pedido no encontrado con ID: " + orderId
                ));

        order.setShipmentId(shipmentId);
        order.setStatus(OrderStatus.DELIVERED);
        return orderRepository.save(order);
    }

    private OrderRoleStrategy getStrategy(String role) {
        return orderRoleStrategies.stream()
                .filter(strategy -> strategy.supports(role))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Rol no autorizado para pedidos"
                ));
    }
}
