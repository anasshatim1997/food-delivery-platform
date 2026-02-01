package com.user_service.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;

@Component
public class XSSFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(new XSSRequestWrapper(request), response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/v1/") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs") ||
                path.equals("/actuator/health");
    }

    private static class XSSRequestWrapper extends HttpServletRequestWrapper {
        public XSSRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String[] getParameterValues(String parameter) {
            String[] values = super.getParameterValues(parameter);
            if (values == null) return null;
            String[] encodedValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                encodedValues[i] = sanitize(values[i]);
            }
            return encodedValues;
        }

        @Override
        public String getParameter(String parameter) {
            return sanitize(super.getParameter(parameter));
        }

        @Override
        public String getHeader(String name) {
            return sanitize(super.getHeader(name));
        }

        private String sanitize(String value) {
            if (value == null) return null;
            value = HtmlUtils.htmlEscape(value);
            value = value.replaceAll("(?i)<script>", "")
                    .replaceAll("(?i)</script>", "")
                    .replaceAll("(?i)<iframe>", "")
                    .replaceAll("(?i)</iframe>", "")
                    .replaceAll("(?i)javascript:", "")
                    .replaceAll("(?i)onerror=", "")
                    .replaceAll("(?i)onload=", "")
                    .replaceAll("(?i)eval\\(", "");
            return value;
        }
    }
}