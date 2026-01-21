package com.api_gateway.config;

import com.api_gateway.filter.LoggingFilter;
import com.api_gateway.filter.RateLimitFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRouteConfig {

    private final LoggingFilter loggingFilter;
    private final RateLimitFilter rateLimitFilter;

    public GatewayRouteConfig(LoggingFilter loggingFilter, RateLimitFilter rateLimitFilter) {
        this.loggingFilter = loggingFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-service", r -> r
                        .path("/api/users/**")
                        .filters(f -> f
                                .stripPrefix(2)
                                .filter(loggingFilter.apply(new LoggingFilter.Config()))
                                .filter(rateLimitFilter.apply(new RateLimitFilter.Config())))
                        .uri("lb://user-service"))

                .route("restaurant-service", r -> r
                        .path("/api/restaurants/**")
                        .filters(f -> f
                                .stripPrefix(2)
                                .filter(loggingFilter.apply(new LoggingFilter.Config()))
                                .filter(rateLimitFilter.apply(new RateLimitFilter.Config())))
                        .uri("lb://restaurant-service"))

                .route("order-service", r -> r
                        .path("/api/orders/**")
                        .filters(f -> f
                                .stripPrefix(2)
                                .filter(loggingFilter.apply(new LoggingFilter.Config()))
                                .filter(rateLimitFilter.apply(new RateLimitFilter.Config())))
                        .uri("lb://order-service"))

                .route("delivery-service", r -> r
                        .path("/api/deliveries/**")
                        .filters(f -> f
                                .stripPrefix(2)
                                .filter(loggingFilter.apply(new LoggingFilter.Config()))
                                .filter(rateLimitFilter.apply(new RateLimitFilter.Config())))
                        .uri("lb://delivery-service"))

                .route("payment-service", r -> r
                        .path("/api/payments/**")
                        .filters(f -> f
                                .stripPrefix(2)
                                .filter(loggingFilter.apply(new LoggingFilter.Config()))
                                .filter(rateLimitFilter.apply(new RateLimitFilter.Config())))
                        .uri("lb://payment-service"))

                .route("notification-service", r -> r
                        .path("/api/notifications/**")
                        .filters(f -> f
                                .stripPrefix(2)
                                .filter(loggingFilter.apply(new LoggingFilter.Config()))
                                .filter(rateLimitFilter.apply(new RateLimitFilter.Config())))
                        .uri("lb://notification-service"))

                .route("analytics-service", r -> r
                        .path("/api/analytics/**")
                        .filters(f -> f
                                .stripPrefix(2)
                                .filter(loggingFilter.apply(new LoggingFilter.Config()))
                                .filter(rateLimitFilter.apply(new RateLimitFilter.Config())))
                        .uri("lb://analytics-service"))

                .build();
    }
}