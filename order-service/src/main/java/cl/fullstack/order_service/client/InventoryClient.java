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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public class InventoryClient {

    private final RestTemplate restTemplate;
    private final CircuitBreaker inventoryCircuitBreaker;

    @Value("${inventory.base-url}")
    private String inventoryBaseUrl;

    public InventoryClient(RestTemplate restTemplate, CircuitBreakerRegistry circuitBreakerRegistry) {
        this.restTemplate = restTemplate;
        this.inventoryCircuitBreaker = circuitBreakerRegistry.circuitBreaker("inventory");
    }

    public ProductResponse getProduct(UUID productId) {
        Supplier<ProductResponse> decoratedCall = CircuitBreaker.decorateSupplier(
                inventoryCircuitBreaker,
                () -> doGetProduct(productId)
        );

        try {
            return decoratedCall.get();
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            return getProductFallback(productId, exception);
        }
    }

    private ProductResponse doGetProduct(UUID productId) {
        try {
            return restTemplate.getForObject(
                    inventoryBaseUrl + "/" + productId,
                    ProductResponse.class
            );
        } catch (HttpClientErrorException.NotFound exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado");
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error consultando Inventory MS");
        }
    }

    public StockCheckResponse checkStock(UUID productId, Integer quantity) {
        Supplier<StockCheckResponse> decoratedCall = CircuitBreaker.decorateSupplier(
                inventoryCircuitBreaker,
                () -> doCheckStock(productId, quantity)
        );

        try {
            return decoratedCall.get();
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            return checkStockFallback(productId, quantity, exception);
        }
    }

    private StockCheckResponse doCheckStock(UUID productId, Integer quantity) {
        try {
            URI uri = UriComponentsBuilder
                    .fromUriString(inventoryBaseUrl + "/" + productId + "/check-stock")
                    .queryParam("quantity", quantity)
                    .build()
                    .toUri();

            return restTemplate.getForObject(uri, StockCheckResponse.class);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error validando stock");
        }
    }

    public void discountStock(UUID productId, Integer quantity) {
        Runnable decoratedCall = CircuitBreaker.decorateRunnable(
                inventoryCircuitBreaker,
                () -> doDiscountStock(productId, quantity)
        );

        try {
            decoratedCall.run();
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            discountStockFallback(productId, quantity, exception);
        }
    }

    private void doDiscountStock(UUID productId, Integer quantity) {
        try {
            URI uri = UriComponentsBuilder
                    .fromUriString(inventoryBaseUrl + "/" + productId + "/discount")
                    .queryParam("quantity", quantity)
                    .build()
                    .toUri();

            restTemplate.exchange(uri, HttpMethod.PATCH, HttpEntity.EMPTY, ProductResponse.class);
        } catch (HttpClientErrorException.Conflict exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock insuficiente");
        } catch (Exception exception) {
            System.out.println("ORDERS CLIENT: Error llamando discount en Inventory MS: " + exception.getClass().getName() + " - " + exception.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error descontando stock: " + exception.getMessage(), exception);
        }
    }

    public ProductResponse getProductFallback(UUID productId, Throwable throwable) {
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Inventory no disponible para consultar producto",
                throwable
        );
    }

    public StockCheckResponse checkStockFallback(UUID productId, Integer quantity, Throwable throwable) {
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Inventory no disponible para validar stock",
                throwable
        );
    }

    public void discountStockFallback(UUID productId, Integer quantity, Throwable throwable) {
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Inventory no disponible para descontar stock",
                throwable
        );
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductResponse {
        private UUID id;
        private String name;
        private String sku;
        private Integer stock;
        private Integer minimumStock;
        private BigDecimal price;
        private String imageUrl;
        private Boolean active;
        private Boolean lowStock;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockCheckResponse {
        private boolean available;
        private Integer currentStock;
        private Integer requestedQuantity;
    }
}
