package cl.fullstack.order_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class OrderDeliveredEvent {

    private UUID orderId;
    private UUID shipmentId;
    private String status;
}