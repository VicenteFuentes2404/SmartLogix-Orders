package cl.fullstack.order_service.client;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public class PaymentClient {

    private final RestTemplate restTemplate;
    private final CircuitBreaker paymentsCircuitBreaker;

    @Value("${payments.base-url}")
    private String paymentsBaseUrl;

    public PaymentClient(RestTemplate restTemplate, CircuitBreakerRegistry circuitBreakerRegistry) {
        this.restTemplate = restTemplate;
        this.paymentsCircuitBreaker = circuitBreakerRegistry.circuitBreaker("payments");
    }

    public PaymentResponse processPayment(UUID orderId, UUID userId, UUID productId, Integer quantity, BigDecimal total, boolean approved) {
        Supplier<PaymentResponse> decoratedCall = CircuitBreaker.decorateSupplier(
                paymentsCircuitBreaker,
                () -> {
                    PaymentRequest request = new PaymentRequest(orderId, userId, productId, quantity, total, approved);
                    return restTemplate.postForObject(
                            paymentsBaseUrl + "/process",
                            request,
                            PaymentResponse.class
                    );
                }
        );

        try {
            return decoratedCall.get();
        } catch (Exception exception) {
            return processPaymentFallback(orderId, userId, productId, quantity, total, approved, exception);
        }
    }

    public void markRefundPending(UUID paymentId) {
        Runnable decoratedCall = CircuitBreaker.decorateRunnable(
                paymentsCircuitBreaker,
                () -> restTemplate.exchange(
                        paymentsBaseUrl + "/" + paymentId + "/refund-pending",
                        HttpMethod.PATCH,
                        HttpEntity.EMPTY,
                        PaymentResponse.class
                )
        );

        try {
            decoratedCall.run();
        } catch (Exception exception) {
            markRefundPendingFallback(paymentId, exception);
        }
    }

    public PaymentResponse processPaymentFallback(UUID orderId, UUID userId, UUID productId, Integer quantity, BigDecimal total, boolean approved, Throwable throwable) {
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Payments no disponible para procesar el pago",
                throwable
        );
    }

    public void markRefundPendingFallback(UUID paymentId, Throwable throwable) {
        System.out.println("ORDERS CLIENT: Payments no disponible para marcar refund pending: " + throwable.getMessage());
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentRequest {
        private UUID orderId;
        private UUID userId;
        private UUID productId;
        private Integer quantity;
        private BigDecimal total;
        private boolean approved;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentResponse {
        private UUID paymentId;
        private UUID orderId;
        private UUID userId;
        private UUID productId;
        private Integer quantity;
        private BigDecimal total;
        private String status;
        private boolean approved;
    }
}
