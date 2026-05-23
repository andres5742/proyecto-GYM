package com.gym.management.repository;

import com.gym.management.model.AccessLog;
import com.gym.management.model.AccessResult;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
            SELECT a FROM AccessLog a
            LEFT JOIN FETCH a.member
            LEFT JOIN FETCH a.employee
            WHERE a.createdAt > :since
            ORDER BY a.createdAt ASC, a.id ASC
            """)
    List<AccessLog> findSince(@Param("since") Instant since);

    @Query(
            """
            SELECT a FROM AccessLog a
            LEFT JOIN FETCH a.member
            LEFT JOIN FETCH a.employee
            WHERE a.id > :afterId
            ORDER BY a.id ASC
            """)
    List<AccessLog> findAfterId(@Param("afterId") long afterId);

    Optional<AccessLog> findFirstByCreatedAtAfterOrderByIdDesc(Instant since);

    Optional<AccessLog> findFirstByResultAndCreatedAtAfterOrderByIdDesc(
            AccessResult result, Instant since);

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

    @Query(
            """
            SELECT COUNT(a) FROM AccessLog a
            WHERE a.member.id = :memberId
              AND a.result = :result
              AND a.createdAt >= :from
              AND a.createdAt < :to
            """)
    long countByMemberIdAndResultBetween(
            @Param("memberId") Long memberId,
            @Param("result") AccessResult result,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query(
            """
            SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
            FROM AccessLog a
            WHERE a.fingerprintUserId = :deviceUserId
              AND a.result = :result
              AND a.createdAt >= :from
              AND a.createdAt < :to
            """)
    boolean existsByFingerprintUserIdAndResultBetween(
            @Param("deviceUserId") String deviceUserId,
            @Param("result") AccessResult result,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
