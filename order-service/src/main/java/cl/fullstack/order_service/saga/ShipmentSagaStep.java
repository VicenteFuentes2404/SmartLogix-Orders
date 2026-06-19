package cl.fullstack.order_service.saga;

import cl.fullstack.order_service.client.ShipmentClient;
import cl.fullstack.order_service.model.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ShipmentSagaStep implements OrderSagaStep {

    private final ShipmentClient shipmentClient;

    public ShipmentSagaStep(ShipmentClient shipmentClient) {
        this.shipmentClient = shipmentClient;
    }

    @Override
    public String name() {
        return "shipment";
    }

    public ShipmentClient.ShipmentResponse createShipment(Order order, UUID userId) {
        return shipmentClient.createShipment(order.getId(), userId, order);
    }
}
