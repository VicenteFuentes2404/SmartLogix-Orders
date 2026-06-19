package cl.fullstack.order_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
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
@Schema(description = "Datos del cliente guardados como snapshot dentro de la orden")
public class CustomerSnapshotDTO {

    @Schema(description = "Nombre del comprador", example = "Vicente")
    @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
    private String nombre;

    @Schema(description = "Apellido del comprador", example = "Perez")
    @Size(max = 80, message = "El apellido no puede superar 80 caracteres")
    private String apellido;

    @Schema(description = "Correo del comprador", example = "cliente@smartlogix.cl")
    @Email(message = "El correo debe tener un formato valido")
    private String email;

    @Schema(description = "Telefono del comprador", example = "+56912345678")
    @Pattern(regexp = "^$|^(\\+56\\s?9\\s?\\d{4}\\s?\\d{4}|\\+569\\d{8}|9\\d{8})$", message = "El telefono debe ser chileno valido")
    private String telefono;
}
