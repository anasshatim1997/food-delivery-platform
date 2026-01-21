package com.user_service.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;

@Component
public class XSSFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        chain.doFilter(new XSSRequestWrapper((HttpServletRequest) request), response);
    }

    private static class XSSRequestWrapper extends HttpServletRequestWrapper {

        public XSSRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String[] getParameterValues(String parameter) {
            String[] values = super.getParameterValues(parameter);
            if (values == null) {
                return null;
            }
            int count = values.length;
            String[] encodedValues = new String[count];
            for (int i = 0; i < count; i++) {
                encodedValues[i] = sanitize(values[i]);
            }
            return encodedValues;
        }

        @Override
        public String getParameter(String parameter) {
            String value = super.getParameter(parameter);
            return sanitize(value);
        }

        @Override
        public String getHeader(String name) {
            String value = super.getHeader(name);
            return sanitize(value);
        }

        private String sanitize(String value) {
            if (value == null) {
                return null;
            }
            value = HtmlUtils.htmlEscape(value);
            value = value.replaceAll("<script>", "");
            value = value.replaceAll("</script>", "");
            value = value.replaceAll("<iframe>", "");
            value = value.replaceAll("</iframe>", "");
            value = value.replaceAll("javascript:", "");
            value = value.replaceAll("onerror=", "");
            value = value.replaceAll("onload=", "");
            value = value.replaceAll("eval\\(", "");
            return value;
        }
    }
}