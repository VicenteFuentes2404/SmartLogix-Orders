package cl.fullstack.order_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMqConfig {

    @Value("${smartlogix.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${smartlogix.rabbitmq.shipment-delivered.queue}")
    private String shipmentDeliveredQueue;

    @Value("${smartlogix.rabbitmq.shipment-delivered.routing-key}")
    private String shipmentDeliveredRoutingKey;

    @Bean
    public TopicExchange smartLogixExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Queue shipmentDeliveredQueue() {
        return new Queue(shipmentDeliveredQueue, true);
    }

    @Bean
    public Binding shipmentDeliveredBinding(Queue shipmentDeliveredQueue, TopicExchange smartLogixExchange) {
        return BindingBuilder
                .bind(shipmentDeliveredQueue)
                .to(smartLogixExchange)
                .with(shipmentDeliveredRoutingKey);
    }
}
