package com.gym.management.repository;

import com.gym.management.model.AccessLog;
import com.gym.management.model.AccessResult;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {

    @Query(
            """
            SELECT a FROM AccessLog a
            LEFT JOIN FETCH a.member
            LEFT JOIN FETCH a.employee
            ORDER BY a.createdAt DESC
            """)
    List<AccessLog> findRecent();

    @Query(
            """
            SELECT a FROM AccessLog a
            LEFT JOIN FETCH a.member
            LEFT JOIN FETCH a.employee
            WHERE a.result = com.gym.management.model.AccessResult.GRANTED
            ORDER BY a.createdAt DESC
            """)
    List<AccessLog> findRecentGranted();

    @Query(
            """
            SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
            FROM AccessLog a
            WHERE a.member.id = :memberId
              AND a.result = :result
              AND a.createdAt >= :from
              AND a.createdAt < :to
            """)
    boolean existsByMemberIdAndResultBetween(
            @Param("memberId") Long memberId,
            @Param("result") AccessResult result,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
