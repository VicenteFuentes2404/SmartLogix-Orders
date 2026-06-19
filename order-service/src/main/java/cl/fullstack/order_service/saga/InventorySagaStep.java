package cl.fullstack.order_service.saga;

import cl.fullstack.order_service.client.InventoryClient;
import cl.fullstack.order_service.dto.OrderItemRequestDTO;
import cl.fullstack.order_service.model.Order;
import cl.fullstack.order_service.model.OrderItem;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class InventorySagaStep implements OrderSagaStep {

    private final InventoryClient inventoryClient;

    public InventorySagaStep(InventoryClient inventoryClient) {
        this.inventoryClient = inventoryClient;
    }

    @Override
    public String name() {
        return "inventory";
    }

    public OrderItemPreparation prepareItems(List<OrderItemRequestDTO> requestedItems) {
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal productsTotal = BigDecimal.ZERO;
        int totalQuantity = 0;

        for (OrderItemRequestDTO itemRequest : requestedItems) {
            InventoryClient.ProductResponse product = inventoryClient.getProduct(itemRequest.getProductId());

            if (product == null || !Boolean.TRUE.equals(product.getActive())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El producto no esta activo");
            }

            if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El producto no tiene precio valido");
            }

            InventoryClient.StockCheckResponse stock = inventoryClient.checkStock(
                    itemRequest.getProductId(),
                    itemRequest.getQuantity()
            );

            if (stock == null || !stock.isAvailable()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock insuficiente para el producto");
            }

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            productsTotal = productsTotal.add(subtotal);
            totalQuantity += itemRequest.getQuantity();

            orderItems.add(OrderItem.builder()
                    .productId(itemRequest.getProductId())
                    .productName(product.getName() == null ? "Producto" : product.getName())
                    .productSku(product.getSku())
                    .imageUrl(product.getImageUrl())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getPrice())
                    .subtotal(subtotal)
                    .build());
        }

        return new OrderItemPreparation(orderItems, productsTotal, totalQuantity);
    }

    public void discountStock(Order order) {
        for (OrderItem item : order.getItems()) {
            inventoryClient.discountStock(item.getProductId(), item.getQuantity());
        }
    }
}
