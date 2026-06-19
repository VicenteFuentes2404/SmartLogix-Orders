package cl.fullstack.order_service.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ResilienceConfig {

    @Value("${smartlogix.circuitbreaker.failure-rate-threshold:50}")
    private float failureRateThreshold;

    @Value("${smartlogix.circuitbreaker.sliding-window-size:6}")
    private int slidingWindowSize;

    @Value("${smartlogix.circuitbreaker.minimum-number-of-calls:3}")
    private int minimumNumberOfCalls;

    @Value("${smartlogix.circuitbreaker.wait-duration-in-open-state-seconds:10}")
    private int waitDurationInOpenStateSeconds;

    @Value("${smartlogix.circuitbreaker.permitted-calls-in-half-open:2}")
    private int permittedCallsInHalfOpen;

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(failureRateThreshold)
                .slidingWindowSize(slidingWindowSize)
                .minimumNumberOfCalls(minimumNumberOfCalls)
                .waitDurationInOpenState(Duration.ofSeconds(waitDurationInOpenStateSeconds))
                .permittedNumberOfCallsInHalfOpenState(permittedCallsInHalfOpen)
                .build();

        return CircuitBreakerRegistry.of(config);
    }
}
