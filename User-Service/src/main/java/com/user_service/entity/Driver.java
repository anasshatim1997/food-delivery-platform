package com.user_service.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.user_service.enums.VehicleType;
import com.user_service.enums.VerificationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

@Entity
@Table(name = "drivers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@PrimaryKeyJoinColumn(name = "user_id")
public class Driver extends User {

    @NotNull
    @Column(name = "vehicle_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType;

    @NotBlank
    @Column(name = "vehicle_number", nullable = false)
    private String vehicleNumber;

    @NotBlank
    @Column(name = "license_number", nullable = false)
    private String licenseNumber;

    @Column(name = "is_available", nullable = false)
    private boolean isAvailable = false;

    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    @Column(name = "current_lat")
    private BigDecimal currentLat;

    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    @Column(name = "current_lng")
    private BigDecimal currentLng;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "5.0")
    @Column(name = "rating", nullable = false)
    private BigDecimal rating = BigDecimal.ZERO;

    @Min(0)
    @Column(name = "total_deliveries", nullable = false)
    private Integer totalDeliveries = 0;

    @DecimalMin(value = "0.0")
    @Column(name = "wallet_balance", nullable = false)
    private BigDecimal walletBalance = BigDecimal.ZERO;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    private VerificationStatus verificationStatus;

    @JsonProperty("verificationDocuments")
    @NotEmpty
    @ElementCollection
    @CollectionTable(name = "driver_verification_documents", joinColumns = @JoinColumn(name = "driver_id"))
    @MapKeyColumn(name = "document_type")
    @Column(name = "document_url")
    private Map<@NotBlank String, @NotBlank @URL String> verificationDocuments;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Driver driver = (Driver) o;
        return getId() != null && Objects.equals(getId(), driver.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}