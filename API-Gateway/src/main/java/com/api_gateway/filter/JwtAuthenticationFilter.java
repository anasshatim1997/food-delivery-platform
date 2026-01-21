package com.api_gateway.filter;

import com.api_gateway.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    final private JwtUtil jwtUtil;

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (isPublicEndpoint(request.getPath().toString())) {
                return chain.filter(exchange);
            }

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "Missing Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Invalid Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                if (!jwtUtil.isTokenValid(token)) {
                    return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
                }

                String userId = jwtUtil.extractUserId(token);
                String role = jwtUtil.extractRole(token);
                String email = jwtUtil.extractUsername(token);

                ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                        .header("X-User-Id", userId)
                        .header("X-User-Role", role)
                        .header("X-User-Email", email)
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (Exception e) {
                return onError(exchange, "JWT validation failed: " + e.getMessage(), HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private boolean isPublicEndpoint(String path) {
        List<String> publicPaths = List.of(
                "/api/users/register",
                "/api/users/login",
                "/api/users/refresh-token",
                "/api/restaurants/search",
                "/api/restaurants/.*/menu",
                "/actuator/health",
                "/swagger-ui",
                "/v3/api-docs"
        );

        return publicPaths.stream().anyMatch(path::matches);
    }

    private Mono<Void> onError(ServerWebExchange exchange, String error, HttpStatus httpStatus) {
        exchange.getResponse().setStatusCode(httpStatus);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        String errorMessage = String.format("{\"error\": \"%s\"}", error);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(errorMessage.getBytes()))
        );
    }

    public static class Config {
    }
}