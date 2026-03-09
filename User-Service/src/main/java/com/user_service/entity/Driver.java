package com.user_service.entity;

import com.user_service.enums.VehicleType;
import com.user_service.enums.VerificationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "drivers")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Driver {

    @Id
    @Column(name = "user_id")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private String profileImage;
    private String licenseImage;

    @NotNull
    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType;

    @NotBlank
    private String vehicleNumber;

    @NotBlank
    private String licenseNumber;

    @Column(nullable = false)
    private Boolean isAvailable = false;

    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private BigDecimal currentLat;

    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private BigDecimal currentLng;

    @DecimalMin("0.0")
    @DecimalMax("5.0")
    private BigDecimal rating = BigDecimal.ZERO;

    @Min(0)
    private Integer totalDeliveries = 0;

    @DecimalMin("0.0")
    private BigDecimal walletBalance = BigDecimal.ZERO;

    @NotNull
    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus;

    @ElementCollection
    @CollectionTable(
            name = "driver_verification_documents",
            joinColumns = @JoinColumn(name = "driver_id")
    )
    @MapKeyColumn(name = "document_type")
    @Column(name = "document_url")
    private Map<String, String> verificationDocuments;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oClass = o instanceof HibernateProxy
                ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
        if (getClass() != oClass) return false;
        Driver driver = (Driver) o;
        return id != null && Objects.equals(id, driver.id);
    }

    @Override
    public final int hashCode() {
        return getClass().hashCode();
    }
}
