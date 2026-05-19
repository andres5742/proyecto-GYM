package com.gym.management.repository;

import com.gym.management.model.MemberFingerprint;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MemberFingerprintRepository extends JpaRepository<MemberFingerprint, Long> {

    Optional<MemberFingerprint> findByFingerprintUserId(String fingerprintUserId);

    Optional<MemberFingerprint> findByMemberId(Long memberId);

    boolean existsByFingerprintUserId(String fingerprintUserId);

    boolean existsByFingerprintUserIdAndMemberIdNot(String fingerprintUserId, Long memberId);

    @Query("SELECT f FROM MemberFingerprint f JOIN FETCH f.member m LEFT JOIN FETCH m.plan")
    java.util.List<MemberFingerprint> findAllWithMember();
}
