package com.gym.management.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "member_progress_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberProgressEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private LocalDate recordedAt;

    @Column(precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Column(precision = 6, scale = 2)
    private BigDecimal chestCm;

    @Column(precision = 6, scale = 2)
    private BigDecimal waistCm;

    @Column(precision = 6, scale = 2)
    private BigDecimal hipsCm;

    @Column(precision = 6, scale = 2)
    private BigDecimal armRightCm;

    @Column(precision = 6, scale = 2)
    private BigDecimal armLeftCm;

    @Column(precision = 6, scale = 2)
    private BigDecimal thighUpperRightCm;

    @Column(precision = 6, scale = 2)
    private BigDecimal thighUpperLeftCm;

    @Column(precision = 6, scale = 2)
    private BigDecimal thighLowerRightCm;

    @Column(precision = 6, scale = 2)
    private BigDecimal thighLowerLeftCm;

    @Column(precision = 6, scale = 2)
    private BigDecimal calfRightCm;

    @Column(precision = 6, scale = 2)
    private BigDecimal calfLeftCm;

    @Column(precision = 5, scale = 2)
    private BigDecimal bodyFatPercent;

    @Column(length = 500)
    private String notes;

    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<MemberProgressPhoto> photos = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
