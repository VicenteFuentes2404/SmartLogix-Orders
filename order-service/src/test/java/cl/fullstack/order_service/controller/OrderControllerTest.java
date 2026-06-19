package cl.fullstack.order_service.controller;

import cl.fullstack.order_service.dto.OrderRequestDTO;
import cl.fullstack.order_service.model.Order;
import cl.fullstack.order_service.model.OrderItem;
import cl.fullstack.order_service.model.OrderStatus;
import cl.fullstack.order_service.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(orderService)).build();
    }

    @Test
    void createOrder_returnsOrderResponseDtoWithItemsAndSnapshots() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        Order order = order(userId);
        when(orderService.createOrder(any(OrderRequestDTO.class), eq(userId))).thenReturn(order);

        // Act
        var result = mockMvc.perform(post("/api/orders")
                .header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson(order.getItems().get(0).getProductId())));

        // Assert
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(order.getId().toString()))
                .andExpect(jsonPath("$.status").value("SHIPMENT_CREATED"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productName").value("Notebook"))
                .andExpect(jsonPath("$.customer.email").value("cliente@smartlogix.cl"))
                .andExpect(jsonPath("$.address.comuna").value("Providencia"));
        verify(orderService).createOrder(any(OrderRequestDTO.class), eq(userId));
    }

    @Test
    void getAll_returnsOrderResponseDtoList() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        Order order = order(userId);
        when(orderService.listOrders(userId, "CLIENT")).thenReturn(List.of(order));

        // Act
        var result = mockMvc.perform(get("/api/orders")
                .header("X-User-Id", userId)
                .header("X-Role", "CLIENT"));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].total").value(21990))
                .andExpect(jsonPath("$[0].items[0].sku").doesNotExist())
                .andExpect(jsonPath("$[0].items[0].productSku").value("NB-001"));
    }

    @Test
    void getById_returnsOrderResponseDto() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Order order = order(userId);
        order.setId(orderId);
        when(orderService.getOrderById(userId, "CLIENT", orderId)).thenReturn(order);

        // Act
        var result = mockMvc.perform(get("/api/orders/{orderId}", orderId)
                .header("X-User-Id", userId)
                .header("X-Role", "CLIENT"));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.paymentMethod").value("webpay"));
    }

    @Test
    void updateStatus_returnsUpdatedOrderResponseDto() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Order order = order(userId);
        order.setId(orderId);
        order.setStatus(OrderStatus.CANCELLED);
        when(orderService.updateOrderStatus(userId, "ADMIN", orderId, OrderStatus.CANCELLED)).thenReturn(order);

        // Act
        var result = mockMvc.perform(patch("/api/orders/{orderId}/status", orderId)
                .param("status", "CANCELLED")
                .header("X-User-Id", userId)
                .header("X-Role", "ADMIN"));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        verify(orderService).updateOrderStatus(userId, "ADMIN", orderId, OrderStatus.CANCELLED);
    }

    @Test
    void orderDelivered_marksOrderAsDelivered() throws Exception {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID shipmentId = UUID.randomUUID();
        String json = """
                {
                  "orderId": "%s",
                  "shipmentId": "%s"
                }
                """.formatted(orderId, shipmentId);

        // Act
        var result = mockMvc.perform(post("/api/orders/events/order-delivered")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));

        // Assert
        result.andExpect(status().isOk());
        verify(orderService).markDelivered(orderId, shipmentId);
    }

    private String orderJson(UUID productId) {
        return """
                {
                  "items": [
                    {
                      "productId": "%s",
                      "quantity": 1
                    }
                  ],
                  "customer": {
                    "nombre": "Cliente",
                    "apellido": "SmartLogix",
                    "email": "cliente@smartlogix.cl",
                    "telefono": "+56912345678"
                  },
                  "address": {
                    "region": "Metropolitana de Santiago",
                    "comuna": "Providencia",
                    "calle": "Los Leones",
                    "numero": "123",
                    "detalle": "Depto 404"
                  },
                  "paymentMethod": "webpay",
                  "shippingCost": 1990,
                  "approved": true
                }
                """.formatted(productId);
    }

    private Order order(UUID userId) {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .userId(userId)
                .quantity(1)
                .total(BigDecimal.valueOf(21990))
                .shippingCost(BigDecimal.valueOf(1990))
                .paymentMethod("webpay")
                .status(OrderStatus.SHIPMENT_CREATED)
                .shipmentId(UUID.randomUUID())
                .customerName("Cliente")
                .customerLastname("SmartLogix")
                .customerEmail("cliente@smartlogix.cl")
                .customerPhone("+56912345678")
                .addressRegion("Metropolitana de Santiago")
                .addressComuna("Providencia")
                .addressCalle("Los Leones")
                .addressNumero("123")
                .addressDetalle("Depto 404")
                .addressId(UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .build();

        order.addItem(OrderItem.builder()
                .id(UUID.randomUUID())
                .productId(order.getProductId())
                .productName("Notebook")
                .productSku("NB-001")
                .imageUrl("https://smartlogix.cl/notebook.png")
                .quantity(1)
                .unitPrice(BigDecimal.valueOf(20000))
                .subtotal(BigDecimal.valueOf(20000))
                .build());
        return order;
    }
}
