package com.gym.management.repository;

import com.gym.management.model.MembershipObligation;
import com.gym.management.model.MembershipObligationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MembershipObligationRepository extends JpaRepository<MembershipObligation, Long> {

    Optional<MembershipObligation> findByMemberIdAndStatus(Long memberId, MembershipObligationStatus status);

    boolean existsByMemberIdAndStatus(Long memberId, MembershipObligationStatus status);

    @Query(
            """
            SELECT o FROM MembershipObligation o
            JOIN FETCH o.member
            JOIN FETCH o.plan
            WHERE o.id = :id
            """)
    Optional<MembershipObligation> findByIdWithDetails(@Param("id") Long id);

    @Query(
            """
            SELECT o FROM MembershipObligation o
            JOIN FETCH o.member
            JOIN FETCH o.plan
            WHERE o.member.id = :memberId AND o.status = :status
            """)
    Optional<MembershipObligation> findOpenByMemberIdWithDetails(
            @Param("memberId") Long memberId, @Param("status") MembershipObligationStatus status);

    @Query(
            """
            SELECT o FROM MembershipObligation o
            JOIN FETCH o.member
            JOIN FETCH o.plan
            WHERE o.status = :status
            """)
    List<MembershipObligation> findAllOpenWithDetails(@Param("status") MembershipObligationStatus status);
}
