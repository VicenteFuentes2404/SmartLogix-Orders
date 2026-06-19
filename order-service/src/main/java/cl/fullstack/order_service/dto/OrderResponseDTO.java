package cl.fullstack.order_service.dto;

import cl.fullstack.order_service.model.Order;
import cl.fullstack.order_service.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@Schema(description = "Pedido retornado por Orders sin exponer entidad JPA")
public class OrderResponseDTO {

    @Schema(description = "ID de la orden", example = "6be24555-59a4-4681-a423-2e7120ed5eff")
    private UUID id;
    @Schema(description = "Primer producto de la orden por compatibilidad", example = "3bc3098b-c839-4f19-b4f0-93991f9c1c24")
    private UUID productId;
    @Schema(description = "Usuario comprador", example = "60246ea1-1104-4e89-a224-a46c93cc7f25")
    private UUID userId;
    @Schema(description = "Cantidad total de unidades", example = "3")
    private Integer quantity;
    @Schema(description = "Total de productos mas envio", example = "21990")
    private BigDecimal total;
    @Schema(description = "Estado actual del pedido", example = "SHIPMENT_CREATED")
    private OrderStatus status;
    @Schema(description = "Envio asociado si existe", example = "f1bd0d58-c47f-4917-b03f-0d8d40d9bc12")
    private UUID shipmentId;
    @Schema(description = "Items del pedido")
    private List<OrderItemResponseDTO> items;
    private CustomerSnapshotDTO customer;
    private ShippingAddressDTO address;
    private UUID addressId;
    private String paymentMethod;
    private BigDecimal shippingCost;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OrderResponseDTO fromEntity(Order order) {
        List<OrderItemResponseDTO> items = order.getItems() == null
                ? List.of()
                : order.getItems().stream()
                        .map(OrderItemResponseDTO::fromEntity)
                        .toList();

        return OrderResponseDTO.builder()
                .id(order.getId())
                .productId(order.getProductId())
                .userId(order.getUserId())
                .quantity(order.getQuantity())
                .total(order.getTotal())
                .status(order.getStatus())
                .shipmentId(order.getShipmentId())
                .items(items)
                .customer(CustomerSnapshotDTO.builder()
                        .nombre(order.getCustomerName())
                        .apellido(order.getCustomerLastname())
                        .email(order.getCustomerEmail())
                        .telefono(order.getCustomerPhone())
                        .build())
                .address(ShippingAddressDTO.builder()
                        .region(order.getAddressRegion())
                        .comuna(order.getAddressComuna())
                        .calle(order.getAddressCalle())
                        .numero(order.getAddressNumero())
                        .detalle(order.getAddressDetalle())
                        .build())
                .addressId(order.getAddressId())
                .paymentMethod(order.getPaymentMethod())
                .shippingCost(order.getShippingCost())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
