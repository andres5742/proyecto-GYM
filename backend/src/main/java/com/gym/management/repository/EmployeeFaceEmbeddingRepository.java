package com.gym.management.repository;

import com.gym.management.model.EmployeeFaceEmbedding;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EmployeeFaceEmbeddingRepository extends JpaRepository<EmployeeFaceEmbedding, Long> {

    Optional<EmployeeFaceEmbedding> findByEmployeeId(Long employeeId);

    @Query("SELECT e FROM EmployeeFaceEmbedding e JOIN FETCH e.employee emp WHERE emp.active = true")
    List<EmployeeFaceEmbedding> findAllWithActiveEmployee();
}
