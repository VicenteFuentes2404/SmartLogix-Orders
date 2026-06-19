package cl.fullstack.order_service.saga;

import cl.fullstack.order_service.client.PaymentClient;
import cl.fullstack.order_service.dto.OrderRequestDTO;
import cl.fullstack.order_service.model.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PaymentSagaStep implements OrderSagaStep {

    private final PaymentClient paymentClient;

    public PaymentSagaStep(PaymentClient paymentClient) {
        this.paymentClient = paymentClient;
    }

    @Override
    public String name() {
        return "payment";
    }

    public PaymentClient.PaymentResponse process(Order order, UUID userId, OrderRequestDTO request) {
        return paymentClient.processPayment(
                order.getId(),
                userId,
                order.getProductId(),
                order.getQuantity(),
                order.getTotal(),
                request.isApproved()
        );
    }

    public void markRefundPending(PaymentClient.PaymentResponse payment) {
        if (payment == null || payment.getPaymentId() == null) {
            return;
        }

        try {
            paymentClient.markRefundPending(payment.getPaymentId());
        } catch (Exception exception) {
            System.out.println("ORDERS SAGA: No se pudo marcar refund pending: " + exception.getMessage());
        }
    }
}
