package com.api_gateway.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends AbstractGatewayFilterFactory<RateLimitFilter.Config> {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    public RateLimitFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String key = getClientKey(exchange);
            Bucket bucket = cache.computeIfAbsent(key, k -> createBucket());

            if (bucket.tryConsume(1)) {
                return chain.filter(exchange);
            } else {
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                exchange.getResponse().getHeaders().add("X-Rate-Limit-Retry-After-Seconds", "60");
                return exchange.getResponse().setComplete();
            }
        };
    }

    private String getClientKey(ServerWebExchange exchange) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        if (userId != null) {
            return "user:" + userId;
        }

        String ipAddress = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
        return "ip:" + ipAddress;
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(100)
                .refillIntervally(100, Duration.ofMinutes(1))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    public static class Config {
    }
}