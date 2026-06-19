package cl.fullstack.order_service.saga;

import cl.fullstack.order_service.client.PaymentClient;
import cl.fullstack.order_service.client.ShipmentClient;
import cl.fullstack.order_service.dto.CustomerSnapshotDTO;
import cl.fullstack.order_service.dto.OrderItemRequestDTO;
import cl.fullstack.order_service.dto.OrderRequestDTO;
import cl.fullstack.order_service.dto.ShippingAddressDTO;
import cl.fullstack.order_service.model.Order;
import cl.fullstack.order_service.model.OrderItem;
import cl.fullstack.order_service.model.OrderStatus;
import cl.fullstack.order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderSagaOrchestratorTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventorySagaStep inventoryStep;

    @Mock
    private PaymentSagaStep paymentStep;

    @Mock
    private ShipmentSagaStep shipmentStep;

    private OrderSagaOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new OrderSagaOrchestrator(orderRepository, inventoryStep, paymentStep, shipmentStep);
    }

    @Test
    void createOrder_whenSagaSucceeds_returnsShipmentCreatedOrderWithItemsAndSnapshots() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID firstProductId = UUID.randomUUID();
        UUID secondProductId = UUID.randomUUID();
        UUID shipmentId = UUID.randomUUID();
        OrderRequestDTO request = orderRequest(firstProductId, secondProductId);
        PaymentClient.PaymentResponse payment = approvedPayment();
        saveReturnsArgumentWithId();
        when(inventoryStep.prepareItems(anyList())).thenReturn(preparation(firstProductId, secondProductId));
        when(paymentStep.process(any(Order.class), eq(userId), eq(request))).thenReturn(payment);
        when(shipmentStep.createShipment(any(Order.class), eq(userId))).thenReturn(shipment(shipmentId));

        // Act
        Order response = orchestrator.createOrder(request, userId);

        // Assert
        assertEquals(OrderStatus.SHIPMENT_CREATED, response.getStatus());
        assertEquals(shipmentId, response.getShipmentId());
        assertEquals(0, BigDecimal.valueOf(21990).compareTo(response.getTotal()));
        assertEquals(3, response.getQuantity());
        assertEquals(2, response.getItems().size());
        assertEquals("Cliente", response.getCustomerName());
        assertEquals("Providencia", response.getAddressComuna());
        verify(inventoryStep).discountStock(response);
    }

    @Test
    void createOrder_whenPaymentIsRejected_returnsPaymentRejectedWithoutStockDiscount() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID firstProductId = UUID.randomUUID();
        UUID secondProductId = UUID.randomUUID();
        OrderRequestDTO request = orderRequest(firstProductId, secondProductId);
        saveReturnsArgumentWithId();
        when(inventoryStep.prepareItems(anyList())).thenReturn(preparation(firstProductId, secondProductId));
        when(paymentStep.process(any(Order.class), eq(userId), eq(request))).thenReturn(rejectedPayment());

        // Act
        Order response = orchestrator.createOrder(request, userId);

        // Assert
        assertEquals(OrderStatus.PAYMENT_REJECTED, response.getStatus());
        verify(inventoryStep, never()).discountStock(any(Order.class));
        verify(shipmentStep, never()).createShipment(any(Order.class), eq(userId));
    }

    @Test
    void createOrder_whenPaymentFails_returnsPaymentPending() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID firstProductId = UUID.randomUUID();
        UUID secondProductId = UUID.randomUUID();
        OrderRequestDTO request = orderRequest(firstProductId, secondProductId);
        saveReturnsArgumentWithId();
        when(inventoryStep.prepareItems(anyList())).thenReturn(preparation(firstProductId, secondProductId));
        when(paymentStep.process(any(Order.class), eq(userId), eq(request))).thenThrow(new RuntimeException("payments down"));

        // Act
        Order response = orchestrator.createOrder(request, userId);

        // Assert
        assertEquals(OrderStatus.PAYMENT_PENDING, response.getStatus());
        verify(inventoryStep, never()).discountStock(any(Order.class));
        verify(shipmentStep, never()).createShipment(any(Order.class), eq(userId));
    }

    @Test
    void createOrder_whenStockDiscountFails_marksRefundPendingAndReturnsStockValidationPending() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID firstProductId = UUID.randomUUID();
        UUID secondProductId = UUID.randomUUID();
        OrderRequestDTO request = orderRequest(firstProductId, secondProductId);
        PaymentClient.PaymentResponse payment = approvedPayment();
        saveReturnsArgumentWithId();
        when(inventoryStep.prepareItems(anyList())).thenReturn(preparation(firstProductId, secondProductId));
        when(paymentStep.process(any(Order.class), eq(userId), eq(request))).thenReturn(payment);
        doThrow(new RuntimeException("inventory down")).when(inventoryStep).discountStock(any(Order.class));

        // Act
        Order response = orchestrator.createOrder(request, userId);

        // Assert
        assertEquals(OrderStatus.STOCK_VALIDATION_PENDING, response.getStatus());
        verify(paymentStep).markRefundPending(payment);
        verify(shipmentStep, never()).createShipment(any(Order.class), eq(userId));
    }

    @Test
    void createOrder_whenShipmentFails_returnsShipmentPendingAfterPaymentAndStock() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID firstProductId = UUID.randomUUID();
        UUID secondProductId = UUID.randomUUID();
        OrderRequestDTO request = orderRequest(firstProductId, secondProductId);
        saveReturnsArgumentWithId();
        when(inventoryStep.prepareItems(anyList())).thenReturn(preparation(firstProductId, secondProductId));
        when(paymentStep.process(any(Order.class), eq(userId), eq(request))).thenReturn(approvedPayment());
        when(shipmentStep.createShipment(any(Order.class), eq(userId))).thenThrow(new RuntimeException("shipments down"));

        // Act
        Order response = orchestrator.createOrder(request, userId);

        // Assert
        assertEquals(OrderStatus.SHIPMENT_PENDING, response.getStatus());
        assertNotNull(response.getId());
        verify(inventoryStep).discountStock(response);
    }

    @Test
    void createOrder_whenInventoryServiceIsUnavailable_savesStockValidationPendingOrder() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID firstProductId = UUID.randomUUID();
        UUID secondProductId = UUID.randomUUID();
        OrderRequestDTO request = orderRequest(firstProductId, secondProductId);
        saveReturnsArgumentWithId();
        when(inventoryStep.prepareItems(anyList())).thenThrow(new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Inventory no disponible"
        ));

        // Act
        Order response = orchestrator.createOrder(request, userId);

        // Assert
        assertEquals(OrderStatus.STOCK_VALIDATION_PENDING, response.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getTotal()));
        assertEquals("Pendiente de validacion", response.getItems().get(0).getProductName());
        verify(paymentStep, never()).process(any(Order.class), eq(userId), eq(request));
    }

    private void saveReturnsArgumentWithId() {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                order.setId(UUID.randomUUID());
            }
            return order;
        });
    }

    private OrderRequestDTO orderRequest(UUID firstProductId, UUID secondProductId) {
        OrderRequestDTO request = new OrderRequestDTO();
        request.setItems(List.of(
                new OrderItemRequestDTO(firstProductId, 1),
                new OrderItemRequestDTO(secondProductId, 2)
        ));
        request.setCustomer(CustomerSnapshotDTO.builder()
                .nombre(" Cliente ")
                .apellido(" SmartLogix ")
                .email(" cliente@smartlogix.cl ")
                .telefono(" +56912345678 ")
                .build());
        request.setAddress(ShippingAddressDTO.builder()
                .region(" Metropolitana de Santiago ")
                .comuna(" Providencia ")
                .calle(" Los Leones ")
                .numero("123")
                .detalle("Depto 404")
                .build());
        request.setAddressId(UUID.randomUUID());
        request.setPaymentMethod("webpay");
        request.setShippingCost(BigDecimal.valueOf(1990));
        request.setApproved(true);
        return request;
    }

    private OrderItemPreparation preparation(UUID firstProductId, UUID secondProductId) {
        OrderItem firstItem = OrderItem.builder()
                .productId(firstProductId)
                .productName("Notebook")
                .productSku("NB-001")
                .imageUrl("https://smartlogix.cl/notebook.png")
                .quantity(1)
                .unitPrice(BigDecimal.valueOf(10000))
                .subtotal(BigDecimal.valueOf(10000))
                .build();
        OrderItem secondItem = OrderItem.builder()
                .productId(secondProductId)
                .productName("Mouse")
                .productSku("MS-001")
                .imageUrl("https://smartlogix.cl/mouse.png")
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(5000))
                .subtotal(BigDecimal.valueOf(10000))
                .build();
        return new OrderItemPreparation(List.of(firstItem, secondItem), BigDecimal.valueOf(20000), 3);
    }

    private PaymentClient.PaymentResponse approvedPayment() {
        return new PaymentClient.PaymentResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                3,
                BigDecimal.valueOf(21990),
                "APPROVED",
                true
        );
    }

    private PaymentClient.PaymentResponse rejectedPayment() {
        return new PaymentClient.PaymentResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                3,
                BigDecimal.valueOf(21990),
                "REJECTED",
                false
        );
    }

    private ShipmentClient.ShipmentResponse shipment(UUID shipmentId) {
        return new ShipmentClient.ShipmentResponse(
                shipmentId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "CREATED",
                "SmartShip",
                "SLX-TRACK",
                "Metropolitana de Santiago",
                "Providencia",
                "Los Leones",
                "123",
                "Depto 404"
        );
    }
}
