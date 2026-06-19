package cl.fullstack.order_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Direccion de despacho guardada como snapshot dentro de la orden")
public class ShippingAddressDTO {

    @Schema(description = "Region", example = "Metropolitana de Santiago")
    @Size(max = 80, message = "La region no puede superar 80 caracteres")
    private String region;

    @Schema(description = "Comuna", example = "Providencia")
    @Size(max = 80, message = "La comuna no puede superar 80 caracteres")
    private String comuna;

    @Schema(description = "Calle", example = "Los Leones")
    @Size(max = 120, message = "La calle no puede superar 120 caracteres")
    private String calle;

    @Schema(description = "Numero", example = "123")
    @Pattern(regexp = "^$|^[0-9]+[A-Za-z]?$", message = "El numero debe tener formato valido, por ejemplo 123 o 123B")
    private String numero;

    @Schema(description = "Detalle adicional", example = "Depto 404")
    @Size(max = 200, message = "El detalle no puede superar 200 caracteres")
    private String detalle;
}
