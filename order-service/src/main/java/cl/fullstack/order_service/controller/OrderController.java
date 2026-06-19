package cl.fullstack.order_service.controller;

import cl.fullstack.order_service.dto.OrderDeliveredEvent;
import cl.fullstack.order_service.dto.OrderRequestDTO;
import cl.fullstack.order_service.dto.OrderResponseDTO;
import cl.fullstack.order_service.model.Order;
import cl.fullstack.order_service.model.OrderStatus;
import cl.fullstack.order_service.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Gestion de pedidos, items multiples y saga de compra")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Crear pedido", description = "Crea una orden con OrderRequestDTO, items multiples, snapshot de cliente y direccion.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido creado y respondido como OrderResponseDTO"),
            @ApiResponse(responseCode = "400", description = "Request invalido"),
            @ApiResponse(responseCode = "409", description = "Stock insuficiente")
    })
    public ResponseEntity<OrderResponseDTO> createOrder(
            @Valid @RequestBody OrderRequestDTO request,
            @RequestHeader("X-User-Id") UUID userId
    ) {
        Order order = orderService.createOrder(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponseDTO.fromEntity(order));
    }

    @GetMapping
    @Operation(summary = "Listar pedidos", description = "Lista pedidos segun el rol recibido desde el API Gateway.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedidos encontrados"),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado")
    })
    public ResponseEntity<?> getAll(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-Role") String role
    ) {
        return ResponseEntity.ok(
                orderService.listOrders(userId, role)
                        .stream()
                        .map(OrderResponseDTO::fromEntity)
                        .collect(Collectors.toList())
        );
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Buscar pedido por ID", description = "Retorna un pedido como OrderResponseDTO respetando permisos por rol.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido encontrado"),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado"),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado")
    })
    public ResponseEntity<OrderResponseDTO> getById(
            @PathVariable UUID orderId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-Role") String role
    ) {
        Order order = orderService.getOrderById(userId, role, orderId);
        return ResponseEntity.ok(OrderResponseDTO.fromEntity(order));
    }

    @PatchMapping("/{orderId}/status")
    @Operation(summary = "Actualizar estado del pedido", description = "Operacion administrativa para cambiar el estado de una orden.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado actualizado"),
            @ApiResponse(responseCode = "403", description = "Requiere permisos de administrador"),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado")
    })
    public ResponseEntity<OrderResponseDTO> updateStatus(
            @PathVariable UUID orderId,
            @RequestParam OrderStatus status,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-Role") String role
    ) {
        Order order = orderService.updateOrderStatus(userId, role, orderId, status);
        return ResponseEntity.ok(OrderResponseDTO.fromEntity(order));
    }

    @PostMapping("/events/order-delivered")
    @Operation(summary = "Evento de pedido entregado", description = "Endpoint interno usado por Shipments para marcar una orden como DELIVERED.")
    @ApiResponse(responseCode = "200", description = "Evento procesado")
    public ResponseEntity<Void> orderDelivered(@RequestBody OrderDeliveredEvent event) {
        orderService.markDelivered(event.getOrderId(), event.getShipmentId());
        return ResponseEntity.ok().build();
    }
}
