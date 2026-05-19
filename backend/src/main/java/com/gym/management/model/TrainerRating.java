package com.gym.management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
        name = "trainer_ratings",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_trainer_rating_monthly",
                        columnNames = {"employee_id", "identification_number", "rating_year", "rating_month"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainerRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private int score;

    @Column(name = "identification_number", nullable = false, length = 20)
    private String identificationNumber;

    @Column(name = "rating_year", nullable = false)
    private int ratingYear;

    @Column(name = "rating_month", nullable = false)
    private int ratingMonth;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
