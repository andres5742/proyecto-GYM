package com.gym.management.repository;

import com.gym.management.model.BillingCashRegister;
import com.gym.management.model.ShiftStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillingCashRegisterRepository extends JpaRepository<BillingCashRegister, Long> {

    @EntityGraph(attributePaths = "openedBy")
    Optional<BillingCashRegister> findFirstByRegisterDateOrderByOpenedAtDesc(LocalDate registerDate);

    @EntityGraph(attributePaths = "openedBy")
    List<BillingCashRegister> findAllByRegisterDateOrderByOpenedAtDesc(LocalDate registerDate);

    @EntityGraph(attributePaths = "openedBy")
    Optional<BillingCashRegister> findFirstByRegisterDateAndStatusOrderByOpenedAtDesc(
            LocalDate registerDate, ShiftStatus status);

    @Query(
            """
            SELECT r FROM BillingCashRegister r
            JOIN FETCH r.openedBy
            WHERE r.id = :id
            """)
    Optional<BillingCashRegister> findByIdWithEmployee(@Param("id") Long id);

    @Query(
            """
            SELECT COUNT(r) FROM BillingCashRegister r
            WHERE r.registerDate BETWEEN :start AND :end
            """)
    long countByRegisterDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
