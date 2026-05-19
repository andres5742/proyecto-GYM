package com.gym.management.repository;

import com.gym.management.model.TrainerRating;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrainerRatingRepository extends JpaRepository<TrainerRating, Long> {

    boolean existsByEmployeeIdAndIdentificationNumberAndRatingYearAndRatingMonth(
            Long employeeId, String identificationNumber, int ratingYear, int ratingMonth);

    @Query(
            """
            SELECT e.id, e.firstName, e.lastName, e.photoUrl, AVG(tr.score), COUNT(tr)
            FROM TrainerRating tr
            JOIN tr.employee e
            WHERE e.ratingEligible = true
              AND tr.ratingYear = :year
              AND tr.ratingMonth = :month
            GROUP BY e.id, e.firstName, e.lastName, e.photoUrl
            ORDER BY AVG(tr.score) DESC, COUNT(tr) DESC
            """)
    List<Object[]> findMonthlyRanking(@Param("year") int year, @Param("month") int month);
}
