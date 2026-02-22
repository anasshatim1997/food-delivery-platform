package com.user_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Slf4j
@Component
public class XSSFilter extends OncePerRequestFilter {

    private static final Pattern[] XSS_PATTERNS = {
            Pattern.compile("<script>(.*?)</script>",    Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("</script>",                 Pattern.CASE_INSENSITIVE),
            Pattern.compile("<script(.*?)>",             Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("<iframe(.*?)>",             Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
            Pattern.compile("</iframe>",                 Pattern.CASE_INSENSITIVE),
            Pattern.compile("javascript:",               Pattern.CASE_INSENSITIVE),
            Pattern.compile("vbscript:",                 Pattern.CASE_INSENSITIVE),
            Pattern.compile("on\\w+\\s*=",               Pattern.CASE_INSENSITIVE),
            Pattern.compile("eval\\s*\\(",               Pattern.CASE_INSENSITIVE),
            Pattern.compile("expression\\s*\\(",         Pattern.CASE_INSENSITIVE),
            Pattern.compile("src\\s*=\\s*['\"]\\s*javascript:", Pattern.CASE_INSENSITIVE),
            Pattern.compile("<!--(.*?)-->",              Pattern.DOTALL),
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(new XSSRequestWrapper(request), response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/actuator/health");
    }

    private static class XSSRequestWrapper extends HttpServletRequestWrapper {

        private final byte[] cachedBody;

        public XSSRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            byte[] rawBody = request.getInputStream().readAllBytes();
            String body = new String(rawBody, StandardCharsets.UTF_8);
            this.cachedBody = sanitize(body).getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cachedBody);
            return new ServletInputStream() {
                @Override public boolean isFinished() { return byteArrayInputStream.available() == 0; }
                @Override public boolean isReady()    { return true; }
                @Override public void setReadListener(ReadListener listener) {}
                @Override public int read()           { return byteArrayInputStream.read(); }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(
                    new ByteArrayInputStream(cachedBody), StandardCharsets.UTF_8));
        }

        @Override
        public String[] getParameterValues(String parameter) {
            String[] values = super.getParameterValues(parameter);
            if (values == null) return null;
            String[] sanitized = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                sanitized[i] = sanitize(values[i]);
            }
            return sanitized;
        }

        @Override
        public String getParameter(String parameter) {
            String value = super.getParameter(parameter);
            return value == null ? null : sanitize(value);
        }

        @Override
        public String getHeader(String name) {
            String value = super.getHeader(name);
            if (value == null) return null;
            if (name.equalsIgnoreCase("Authorization")
                    || name.equalsIgnoreCase("Content-Type")
                    || name.equalsIgnoreCase("Accept")) {
                return value;
            }
            return sanitize(value);
        }

        private static String sanitize(String value) {
            if (value == null) return null;
            String result = value;
            for (Pattern pattern : XSS_PATTERNS) {
                result = pattern.matcher(result).replaceAll("");
            }
            return result;
        }
    }
}