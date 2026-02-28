package com.user_service.repository;

import com.user_service.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {

    List<Address> findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(UUID customerId);

    Optional<Address> findByIdAndCustomerId(UUID id, UUID customerId);

    int countByCustomerId(UUID customerId);

    Optional<Address> findByCustomerIdAndIsDefaultTrue(UUID customerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.customer.id = :customerId AND a.id <> :excludeId")
    void clearDefaultExcept(@Param("customerId") UUID customerId, @Param("excludeId") UUID excludeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.customer.id = :customerId")
    void clearAllDefaults(@Param("customerId") UUID customerId);
}