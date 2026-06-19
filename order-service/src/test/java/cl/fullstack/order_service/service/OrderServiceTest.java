package cl.fullstack.order_service.service;

import cl.fullstack.order_service.dto.OrderRequestDTO;
import cl.fullstack.order_service.model.Order;
import cl.fullstack.order_service.model.OrderStatus;
import cl.fullstack.order_service.repository.OrderRepository;
import cl.fullstack.order_service.saga.OrderSagaOrchestrator;
import cl.fullstack.order_service.strategy.OrderRoleStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderRoleStrategy orderRoleStrategy;

    @Mock
    private OrderSagaOrchestrator orderSagaOrchestrator;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, List.of(orderRoleStrategy), orderSagaOrchestrator);
    }

    @Test
    void createOrder_delegatesCreationToSagaOrchestrator() {
        // Arrange
        UUID userId = UUID.randomUUID();
        OrderRequestDTO request = new OrderRequestDTO();
        Order order = order(userId);
        when(orderSagaOrchestrator.createOrder(request, userId)).thenReturn(order);

        // Act
        Order response = orderService.createOrder(request, userId);

        // Assert
        assertEquals(order.getId(), response.getId());
        verify(orderSagaOrchestrator).createOrder(request, userId);
    }

    @Test
    void listOrders_whenRoleIsSupported_usesMatchingStrategy() {
        // Arrange
        UUID userId = UUID.randomUUID();
        Order order = order(userId);
        when(orderRoleStrategy.supports("CLIENT")).thenReturn(true);
        when(orderRoleStrategy.listOrders(userId)).thenReturn(List.of(order));

        // Act
        List<Order> response = orderService.listOrders(userId, "CLIENT");

        // Assert
        assertEquals(1, response.size());
        assertEquals(order.getId(), response.get(0).getId());
    }

    @Test
    void getOrderById_whenRoleIsNotSupported_throwsForbidden() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        when(orderRoleStrategy.supports("GUEST")).thenReturn(false);

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> orderService.getOrderById(userId, "GUEST", orderId)
        );

        // Assert
        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void updateOrderStatus_whenRoleIsSupported_returnsUpdatedOrder() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Order order = order(userId);
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRoleStrategy.supports("ADMIN")).thenReturn(true);
        when(orderRoleStrategy.updateOrderStatus(userId, orderId, OrderStatus.CANCELLED)).thenReturn(order);

        // Act
        Order response = orderService.updateOrderStatus(userId, "ADMIN", orderId, OrderStatus.CANCELLED);

        // Assert
        assertEquals(OrderStatus.CANCELLED, response.getStatus());
        verify(orderRoleStrategy).updateOrderStatus(userId, orderId, OrderStatus.CANCELLED);
    }

    @Test
    void markDelivered_whenOrderExists_setsShipmentAndDeliveredStatus() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID shipmentId = UUID.randomUUID();
        Order order = order(UUID.randomUUID());
        order.setId(orderId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        // Act
        Order response = orderService.markDelivered(orderId, shipmentId);

        // Assert
        assertEquals(OrderStatus.DELIVERED, response.getStatus());
        assertEquals(shipmentId, response.getShipmentId());
        verify(orderRepository).save(order);
    }

    @Test
    void markDelivered_whenOrderDoesNotExist_throwsNotFound() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // Act
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> orderService.markDelivered(orderId, UUID.randomUUID())
        );

        // Assert
        assertEquals(404, exception.getStatusCode().value());
    }

    private Order order(UUID userId) {
        return Order.builder()
                .id(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .userId(userId)
                .quantity(1)
                .total(BigDecimal.valueOf(12990))
                .status(OrderStatus.CREATED)
                .build();
    }
}
