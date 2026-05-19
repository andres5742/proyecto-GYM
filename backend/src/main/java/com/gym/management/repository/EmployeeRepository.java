package com.gym.management.repository;

import com.gym.management.model.Employee;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByActiveTrueOrderByFirstNameAsc();

    List<Employee> findByRatingEligibleTrueAndActiveTrueOrderByFirstNameAsc();

    List<Employee> findAllByOrderByFirstNameAsc();

    Optional<Employee> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);
}
