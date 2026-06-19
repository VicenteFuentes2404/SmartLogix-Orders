package cl.fullstack.order_service.dto;

import cl.fullstack.order_service.model.OrderItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@Schema(description = "Item de pedido con snapshot de producto")
public class OrderItemResponseDTO {

    @Schema(description = "ID del item", example = "8ba1c1ac-9327-49d0-955d-985878a02755")
    private UUID id;
    @Schema(description = "ID del producto", example = "3bc3098b-c839-4f19-b4f0-93991f9c1c24")
    private UUID productId;
    @Schema(description = "Nombre del producto al comprar", example = "Notebook Lenovo")
    private String productName;
    @Schema(description = "SKU del producto", example = "NB-LEN-001")
    private String productSku;
    @Schema(description = "Imagen del producto al comprar", example = "https://smartlogix.cl/img/notebook.png")
    private String imageUrl;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    public static OrderItemResponseDTO fromEntity(OrderItem item) {
        return OrderItemResponseDTO.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .productSku(item.getProductSku())
                .imageUrl(item.getImageUrl())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal())
                .build();
    }
}
