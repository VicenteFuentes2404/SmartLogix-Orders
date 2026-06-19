package cl.fullstack.order_service.saga;

import cl.fullstack.order_service.model.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class OrderItemPreparation {

    private final List<OrderItem> items;
    private final BigDecimal productsTotal;
    private final int totalQuantity;
}
