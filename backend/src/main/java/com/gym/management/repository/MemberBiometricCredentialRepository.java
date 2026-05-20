package com.gym.management.repository;

import com.gym.management.model.BiometricCredentialType;
import com.gym.management.model.MemberBiometricCredential;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MemberBiometricCredentialRepository extends JpaRepository<MemberBiometricCredential, Long> {

    Optional<MemberBiometricCredential> findByCredentialTypeAndDeviceUserId(
            BiometricCredentialType credentialType, String deviceUserId);

    Optional<MemberBiometricCredential> findByMemberIdAndCredentialType(
            Long memberId, BiometricCredentialType credentialType);

    List<MemberBiometricCredential> findByMemberId(Long memberId);

    void deleteByMemberId(Long memberId);

    void deleteByMemberIdAndCredentialType(Long memberId, BiometricCredentialType credentialType);

    @Query(
            "SELECT c FROM MemberBiometricCredential c JOIN FETCH c.member m LEFT JOIN FETCH m.plan ORDER BY c.credentialType, m.lastName")
    List<MemberBiometricCredential> findAllWithMember();
}
