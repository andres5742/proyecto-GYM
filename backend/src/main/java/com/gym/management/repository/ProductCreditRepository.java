package com.gym.management.repository;

import com.gym.management.model.ProductCredit;
import com.gym.management.model.ProductCreditStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductCreditRepository extends JpaRepository<ProductCredit, Long> {

    @Query(
            """
            SELECT c FROM ProductCredit c
            JOIN FETCH c.member
            JOIN FETCH c.product
            JOIN FETCH c.employee
            JOIN FETCH c.workShift
            ORDER BY c.creditedAt DESC, c.createdAt DESC
            """)
    List<ProductCredit> findAllWithRelations();

    @Query(
            """
            SELECT c FROM ProductCredit c
            JOIN FETCH c.member
            JOIN FETCH c.product
            JOIN FETCH c.employee
            JOIN FETCH c.workShift
            WHERE c.status = :status
            ORDER BY c.creditedAt DESC, c.createdAt DESC
            """)
    List<ProductCredit> findByStatusWithRelations(@Param("status") ProductCreditStatus status);

    List<ProductCredit> findByMemberIdOrderByCreditedAtDescCreatedAtDesc(Long memberId);

    @Query(
            """
            SELECT c FROM ProductCredit c
            JOIN FETCH c.member
            JOIN FETCH c.product
            JOIN FETCH c.employee
            JOIN FETCH c.workShift
            LEFT JOIN FETCH c.payments p
            WHERE c.id = :id
            """)
    java.util.Optional<ProductCredit> findDetailedById(@Param("id") Long id);

    boolean existsByOriginSaleId(Long originSaleId);

    List<ProductCredit> findByMemberIdAndStatusOrderByCreditedAtAsc(
            Long memberId, ProductCreditStatus status);
}
