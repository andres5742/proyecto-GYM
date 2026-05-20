package com.gym.management.config;

import com.gym.management.model.BiometricCredentialType;
import com.gym.management.model.MemberBiometricCredential;
import com.gym.management.model.MemberFingerprint;
import com.gym.management.repository.MemberBiometricCredentialRepository;
import com.gym.management.repository.MemberFingerprintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Copia enrollments legacy de huella a la tabla unificada (una sola vez). */
@Configuration
@RequiredArgsConstructor
public class BiometricCredentialMigration {

    private final MemberFingerprintRepository legacyFingerprintRepository;
    private final MemberBiometricCredentialRepository biometricCredentialRepository;

    @Bean
    CommandLineRunner migrateLegacyFingerprints() {
        return args -> {
            if (biometricCredentialRepository.count() > 0) {
                return;
            }
            for (MemberFingerprint legacy : legacyFingerprintRepository.findAllWithMember()) {
                biometricCredentialRepository.save(MemberBiometricCredential.builder()
                        .member(legacy.getMember())
                        .credentialType(BiometricCredentialType.FINGERPRINT)
                        .deviceUserId(legacy.getFingerprintUserId())
                        .deviceLabel(legacy.getDeviceLabel())
                        .enrolledAt(legacy.getEnrolledAt())
                        .build());
            }
        };
    }
}
