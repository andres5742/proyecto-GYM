package com.gym.management.repository;

import com.gym.management.model.AccessLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {

    @Query(
            """
            SELECT a FROM AccessLog a
            LEFT JOIN FETCH a.member
            ORDER BY a.createdAt DESC
            """)
    List<AccessLog> findRecent();

    List<AccessLog> findTop20ByOrderByCreatedAtDesc();
}
