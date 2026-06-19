package cl.fullstack.order_service.client;

import cl.fullstack.order_service.model.Order;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.function.Supplier;

@Component
public class ShipmentClient {

    private final RestTemplate restTemplate;
    private final CircuitBreaker shipmentsCircuitBreaker;

    @Value("${shipments.base-url}")
    private String shipmentsBaseUrl;

    public ShipmentClient(RestTemplate restTemplate, CircuitBreakerRegistry circuitBreakerRegistry) {
        this.restTemplate = restTemplate;
        this.shipmentsCircuitBreaker = circuitBreakerRegistry.circuitBreaker("shipments");
    }

    public ShipmentResponse createShipment(UUID orderId, UUID userId, Order order) {
        Supplier<ShipmentResponse> decoratedCall = CircuitBreaker.decorateSupplier(
                shipmentsCircuitBreaker,
                () -> {
                    ShipmentRequest request = new ShipmentRequest(
                            orderId,
                            userId,
                            order.getAddressRegion(),
                            order.getAddressComuna(),
                            order.getAddressCalle(),
                            order.getAddressNumero(),
                            order.getAddressDetalle()
                    );
                    return restTemplate.postForObject(
                            shipmentsBaseUrl,
                            request,
                            ShipmentResponse.class
                    );
                }
        );

        try {
            return decoratedCall.get();
        } catch (Exception exception) {
            return createShipmentFallback(orderId, userId, order, exception);
        }
    }

    public ShipmentResponse createShipmentFallback(UUID orderId, UUID userId, Order order, Throwable throwable) {
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Shipments no disponible para crear envio",
                throwable
        );
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShipmentRequest {
        private UUID orderId;
        private UUID userId;
        private String addressRegion;
        private String addressComuna;
        private String addressCalle;
        private String addressNumero;
        private String addressDetalle;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShipmentResponse {
        private UUID id;
        private UUID orderId;
        private UUID userId;
        private String status;
        private String carrier;
        private String trackingCode;
        private String addressRegion;
        private String addressComuna;
        private String addressCalle;
        private String addressNumero;
        private String addressDetalle;
    }
}
