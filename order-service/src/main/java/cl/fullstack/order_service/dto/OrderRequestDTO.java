package cl.fullstack.order_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Schema(description = "Request para crear una orden; soporta items multiples y snapshots de cliente/direccion")
public class OrderRequestDTO {

    @Schema(description = "Producto unico para compatibilidad con flujo antiguo", example = "3bc3098b-c839-4f19-b4f0-93991f9c1c24")
    private UUID productId;

    @Schema(description = "Cantidad para flujo antiguo de un solo producto", example = "2")
    @Positive(message = "La cantidad debe ser mayor a 0")
    private Integer quantity;

    @Schema(description = "Lista de productos del carrito")
    @Valid
    private List<OrderItemRequestDTO> items = new ArrayList<>();

    @Schema(description = "Snapshot del cliente al comprar")
    @Valid
    private CustomerSnapshotDTO customer;

    @Schema(description = "Snapshot de la direccion de despacho")
    @Valid
    private ShippingAddressDTO address;

    @Schema(description = "ID de direccion guardada si el usuario eligio una existente", example = "7dfddf73-3ea5-4dd3-9f9a-5ef0e3e51b13")
    private UUID addressId;

    @Schema(description = "Metodo de pago", example = "webpay")
    private String paymentMethod = "webpay";

    @Schema(description = "Costo de envio", example = "1990")
    @DecimalMin(value = "0.00", message = "El costo de envio no puede ser negativo")
    private BigDecimal shippingCost = BigDecimal.ZERO;

    @Schema(description = "Bandera usada por pruebas/demo para simular aprobacion o rechazo del pago", example = "true")
    private boolean approved = true;

    @AssertTrue(message = "Debe incluir al menos un producto")
    public boolean isValidProductSelection() {
        return (items != null && !items.isEmpty())
                || (productId != null && quantity != null);
    }
}
