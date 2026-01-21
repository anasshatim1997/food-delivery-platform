package com.api_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class LoggingFilter extends AbstractGatewayFilterFactory<LoggingFilter.Config> {

    public LoggingFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            System.out.println("Request: " + exchange.getRequest().getMethod() + " " + exchange.getRequest().getURI());
            return chain.filter(exchange).then(Mono.fromRunnable(() -> System.out.println("Response: " + exchange.getResponse().getStatusCode())));
        };
    }

    public static class Config {
    }
}