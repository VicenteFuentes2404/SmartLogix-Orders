package cl.fullstack.order_service.saga;

import cl.fullstack.order_service.client.PaymentClient;
import cl.fullstack.order_service.client.ShipmentClient;
import cl.fullstack.order_service.dto.OrderItemRequestDTO;
import cl.fullstack.order_service.dto.OrderRequestDTO;
import cl.fullstack.order_service.model.Order;
import cl.fullstack.order_service.model.OrderItem;
import cl.fullstack.order_service.model.OrderStatus;
import cl.fullstack.order_service.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class OrderSagaOrchestrator {

    private final OrderRepository orderRepository;
    private final InventorySagaStep inventoryStep;
    private final PaymentSagaStep paymentStep;
    private final ShipmentSagaStep shipmentStep;

    public OrderSagaOrchestrator(
            OrderRepository orderRepository,
            InventorySagaStep inventoryStep,
            PaymentSagaStep paymentStep,
            ShipmentSagaStep shipmentStep
    ) {
        this.orderRepository = orderRepository;
        this.inventoryStep = inventoryStep;
        this.paymentStep = paymentStep;
        this.shipmentStep = shipmentStep;
    }

    public Order createOrder(OrderRequestDTO request, UUID userId) {
        List<OrderItemRequestDTO> requestedItems = normalizeItems(request);
        BigDecimal shippingCost = request.getShippingCost() == null ? BigDecimal.ZERO : request.getShippingCost();

        try {
            OrderItemPreparation preparation = inventoryStep.prepareItems(requestedItems);
            Order savedOrder = orderRepository.save(buildPreparedOrder(request, userId, preparation, shippingCost));
            return runSaga(savedOrder, request, userId);
        } catch (ResponseStatusException exception) {
            if (!isInventoryUnavailable(exception)) {
                throw exception;
            }

            System.out.println("ORDERS SAGA: Inventario pendiente al crear pedido: " + exception.getReason());
            Order pendingOrder = buildStockValidationPendingOrder(request, userId, requestedItems, shippingCost);
            return orderRepository.save(pendingOrder);
        }
    }

    private Order buildPreparedOrder(
            OrderRequestDTO request,
            UUID userId,
            OrderItemPreparation preparation,
            BigDecimal shippingCost
    ) {
        OrderItem firstItem = preparation.getItems().get(0);

        Order order = Order.builder()
                .productId(firstItem.getProductId())
                .userId(userId)
                .quantity(preparation.getTotalQuantity())
                .total(preparation.getProductsTotal().add(shippingCost))
                .shippingCost(shippingCost)
                .paymentMethod(cleanOrDefault(request.getPaymentMethod(), "webpay"))
                .status(OrderStatus.CREATED)
                .build();

        applyCustomerSnapshot(order, request);
        applyAddressSnapshot(order, request);
        preparation.getItems().forEach(order::addItem);
        return order;
    }

    private Order buildStockValidationPendingOrder(
            OrderRequestDTO request,
            UUID userId,
            List<OrderItemRequestDTO> requestedItems,
            BigDecimal shippingCost
    ) {
        int totalQuantity = requestedItems.stream()
                .mapToInt(OrderItemRequestDTO::getQuantity)
                .sum();

        Order order = Order.builder()
                .productId(requestedItems.get(0).getProductId())
                .userId(userId)
                .quantity(totalQuantity)
                .total(BigDecimal.ZERO)
                .shippingCost(shippingCost)
                .paymentMethod(cleanOrDefault(request.getPaymentMethod(), "webpay"))
                .status(OrderStatus.STOCK_VALIDATION_PENDING)
                .build();

        applyCustomerSnapshot(order, request);
        applyAddressSnapshot(order, request);

        requestedItems.forEach(item -> order.addItem(OrderItem.builder()
                .productId(item.getProductId())
                .productName("Pendiente de validacion")
                .quantity(item.getQuantity())
                .unitPrice(BigDecimal.ZERO)
                .subtotal(BigDecimal.ZERO)
                .build()));

        return order;
    }

    private boolean isInventoryUnavailable(ResponseStatusException exception) {
        return exception.getStatusCode().is5xxServerError();
    }

    private Order runSaga(Order order, OrderRequestDTO request, UUID userId) {
        PaymentClient.PaymentResponse payment;

        try {
            payment = paymentStep.process(order, userId, request);
        } catch (Exception exception) {
            System.out.println("ORDERS SAGA: Pago pendiente por error: " + exception.getMessage());
            order.setStatus(OrderStatus.PAYMENT_PENDING);
            return orderRepository.save(order);
        }

        if (payment == null || !payment.isApproved()) {
            System.out.println("ORDERS SAGA: Pago rechazado para pedido " + order.getId());
            order.setStatus(OrderStatus.PAYMENT_REJECTED);
            return orderRepository.save(order);
        }

        try {
            inventoryStep.discountStock(order);
        } catch (Exception exception) {
            System.out.println("ORDERS SAGA: Inventario pendiente por error: " + exception.getMessage());
            paymentStep.markRefundPending(payment);
            order.setStatus(OrderStatus.STOCK_VALIDATION_PENDING);
            return orderRepository.save(order);
        }

        order.setStatus(OrderStatus.PAID);
        order = orderRepository.save(order);

        try {
            ShipmentClient.ShipmentResponse shipment = shipmentStep.createShipment(order, userId);
            if (shipment == null || shipment.getId() == null) {
                order.setStatus(OrderStatus.SHIPMENT_PENDING);
                return orderRepository.save(order);
            }

            order.setShipmentId(shipment.getId());
            order.setStatus(OrderStatus.SHIPMENT_CREATED);
        } catch (Exception exception) {
            System.out.println("ORDERS SAGA: Envio pendiente por error: " + exception.getMessage());
            order.setStatus(OrderStatus.SHIPMENT_PENDING);
        }

        return orderRepository.save(order);
    }

    private List<OrderItemRequestDTO> normalizeItems(OrderRequestDTO request) {
        List<OrderItemRequestDTO> sourceItems = request.getItems() != null && !request.getItems().isEmpty()
                ? request.getItems()
                : List.of(new OrderItemRequestDTO(request.getProductId(), request.getQuantity()));

        Map<UUID, Integer> quantitiesByProduct = new LinkedHashMap<>();

        for (OrderItemRequestDTO item : sourceItems) {
            if (item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cada item debe incluir producto y cantidad valida");
            }

            quantitiesByProduct.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }

        return quantitiesByProduct.entrySet().stream()
                .map(entry -> new OrderItemRequestDTO(entry.getKey(), entry.getValue()))
                .toList();
    }

    private void applyCustomerSnapshot(Order order, OrderRequestDTO request) {
        if (request.getCustomer() == null) return;

        order.setCustomerName(cleanNullable(request.getCustomer().getNombre()));
        order.setCustomerLastname(cleanNullable(request.getCustomer().getApellido()));
        order.setCustomerEmail(cleanNullable(request.getCustomer().getEmail()));
        order.setCustomerPhone(cleanNullable(request.getCustomer().getTelefono()));
    }

    private void applyAddressSnapshot(Order order, OrderRequestDTO request) {
        if (request.getAddress() == null) return;

        order.setAddressId(request.getAddressId());
        order.setAddressRegion(cleanNullable(request.getAddress().getRegion()));
        order.setAddressComuna(cleanNullable(request.getAddress().getComuna()));
        order.setAddressCalle(cleanNullable(request.getAddress().getCalle()));
        order.setAddressNumero(cleanNullable(request.getAddress().getNumero()));
        order.setAddressDetalle(cleanNullable(request.getAddress().getDetalle()));
    }

    private String cleanOrDefault(String value, String fallback) {
        String clean = cleanNullable(value);
        return clean == null ? fallback : clean;
    }

    private String cleanNullable(String value) {
        if (value == null) return null;
        String clean = value.trim();
        return clean.isEmpty() ? null : clean;
    }
}
