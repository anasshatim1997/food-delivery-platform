package com.user_service.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class RoleAnnotations {

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @PreAuthorize("hasRole('CUSTOMER')")
    public @interface IsCustomer {}

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @PreAuthorize("hasRole('DRIVER')")
    public @interface IsDriver {}

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @PreAuthorize("hasRole('RESTAURANT_OWNER')")
    public @interface IsRestaurantOwner {}

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @PreAuthorize("hasRole('ADMIN')")
    public @interface IsAdmin {}

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @PreAuthorize("hasAnyRole('CUSTOMER', 'DRIVER', 'RESTAURANT_OWNER', 'ADMIN')")
    public @interface IsAuthenticated {}

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @PreAuthorize("hasAnyRole('DRIVER', 'CUSTOMER')")
    public @interface IsDriverOrCustomer {}
}