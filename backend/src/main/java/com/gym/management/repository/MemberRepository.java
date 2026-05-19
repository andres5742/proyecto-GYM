package com.gym.management.repository;

import com.gym.management.model.Member;
import com.gym.management.model.MembershipStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    Optional<Member> findByDocumentId(String documentId);

    List<Member> findByStatusAndMembershipEndBefore(MembershipStatus status, LocalDate date);

    @Modifying
    @Query("UPDATE Member m SET m.plan = null WHERE m.plan.id = :planId")
    void detachPlanFromMembers(@Param("planId") Long planId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            UPDATE Member m SET m.status = com.gym.management.model.MembershipStatus.EXPIRED
            WHERE m.status = com.gym.management.model.MembershipStatus.ACTIVE
            AND m.membershipEnd IS NOT NULL
            AND m.membershipEnd < :today
            """)
    int markExpiredActiveMembers(@Param("today") LocalDate today);
}
