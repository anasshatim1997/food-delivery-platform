package com.user_service.security;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.web.util.HtmlUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class XSSRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public XSSRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        byte[] rawBody = request.getInputStream().readAllBytes();
        String body = new String(rawBody, StandardCharsets.UTF_8);
        this.cachedBody = sanitize(body).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(cachedBody);
        return new ServletInputStream() {
            @Override public boolean isFinished()                           { return byteStream.available() == 0; }
            @Override public boolean isReady()                             { return true; }
            @Override public void setReadListener(ReadListener listener)   {}
            @Override public int read()                                    { return byteStream.read(); }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(cachedBody), StandardCharsets.UTF_8));
    }

    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return value == null ? null : sanitize(value);
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) return null;
        String[] sanitized = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            sanitized[i] = sanitize(values[i]);
        }
        return sanitized;
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
        return value == null ? null : HtmlUtils.htmlEscape(value, "UTF-8");
    }
}