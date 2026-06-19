package cl.fullstack.order_service.consumer;

import cl.fullstack.order_service.service.OrderService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class ShipmentDeliveredConsumer {

    private final OrderService orderService;

    public ShipmentDeliveredConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(queues = "${smartlogix.rabbitmq.shipment-delivered.queue}")
    public void consume(Map<String, Object> payload) {
        try {
            UUID orderId = UUID.fromString(String.valueOf(payload.get("orderId")));
            UUID shipmentId = UUID.fromString(String.valueOf(payload.get("shipmentId")));

            orderService.markDelivered(orderId, shipmentId);
            System.out.println("ORDERS RABBITMQ: Pedido marcado como entregado " + orderId);
        } catch (Exception exception) {
            System.out.println("ORDERS RABBITMQ: No se pudo consumir pedido entregado: " + exception.getMessage());
        }
    }
}
