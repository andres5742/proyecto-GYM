package com.gym.management.repository;

import com.gym.management.model.BiometricCredentialType;
import com.gym.management.model.EmployeeBiometricCredential;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeBiometricCredentialRepository extends JpaRepository<EmployeeBiometricCredential, Long> {

    Optional<EmployeeBiometricCredential> findByCredentialTypeAndDeviceUserId(
            BiometricCredentialType credentialType, String deviceUserId);

    Optional<EmployeeBiometricCredential> findByEmployeeIdAndCredentialType(
            Long employeeId, BiometricCredentialType credentialType);

    void deleteByEmployeeIdAndCredentialType(Long employeeId, BiometricCredentialType credentialType);

    @Query(
            "SELECT c FROM EmployeeBiometricCredential c JOIN FETCH c.employee e WHERE e.active = true ORDER BY e.lastName, e.firstName")
    List<EmployeeBiometricCredential> findAllWithActiveEmployee();

    @Query(
            """
            SELECT c FROM EmployeeBiometricCredential c JOIN FETCH c.employee e
            WHERE c.credentialType = com.gym.management.model.BiometricCredentialType.CARD
            AND (c.deviceUserId = :pin OR c.deviceUserId LIKE CONCAT(:pin, '|%'))
            """)
    List<EmployeeBiometricCredential> findStaffCardsByReaderPin(@Param("pin") String pin);
}
