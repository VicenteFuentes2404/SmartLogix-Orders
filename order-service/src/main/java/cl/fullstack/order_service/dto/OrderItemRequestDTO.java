package cl.fullstack.order_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Item del carrito incluido en una orden")
public class OrderItemRequestDTO {

    @Schema(description = "ID del producto", example = "3bc3098b-c839-4f19-b4f0-93991f9c1c24")
    @NotNull(message = "El ID del producto es obligatorio")
    private UUID productId;

    @Schema(description = "Cantidad solicitada", example = "2")
    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser mayor a 0")
    private Integer quantity;
}
