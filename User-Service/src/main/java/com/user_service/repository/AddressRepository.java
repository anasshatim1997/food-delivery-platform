package com.user_service.repository;

import com.user_service.entity.Address;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {

    Page<Address> findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(UUID customerId, Pageable pageable);

    Optional<Address> findByIdAndCustomerId(UUID id, UUID customerId);

    int countByCustomerId(UUID customerId);

    List<Address> findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(UUID customerId);

    Optional<Address> findByCustomerIdAndIsDefaultTrue(UUID customerId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.customer.id = :customerId AND a.id <> :excludeId")
    void clearDefaultExcept(@Param("customerId") UUID customerId, @Param("excludeId") UUID excludeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.customer.id = :customerId")
    void clearAllDefaults(@Param("customerId") UUID customerId);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("SELECT a FROM Address a WHERE a.customer.id = :customerId")
    List<Address> lockAllByCustomerId(@Param("customerId") UUID customerId);
}