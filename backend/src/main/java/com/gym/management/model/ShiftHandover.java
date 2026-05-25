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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "shift_handovers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftHandover {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_shift_id", nullable = false, unique = true)
    private WorkShift workShift;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    @Builder.Default
    private int bill2000 = 0;

    @Column(nullable = false)
    @Builder.Default
    private int bill5000 = 0;

    @Column(nullable = false)
    @Builder.Default
    private int bill10000 = 0;

    @Column(nullable = false)
    @Builder.Default
    private int bill20000 = 0;

    @Column(nullable = false)
    @Builder.Default
    private int bill50000 = 0;

    @Column(nullable = false)
    @Builder.Default
    private int bill100000 = 0;

    @Column(nullable = false)
    @Builder.Default
    private int coin1000 = 0;

    @Column(nullable = false)
    @Builder.Default
    private int coin500 = 0;

    @Column(nullable = false)
    @Builder.Default
    private int coin200 = 0;

    @Column(nullable = false)
    @Builder.Default
    private int coin100 = 0;

    @Column(nullable = false)
    @Builder.Default
    private int coin50 = 0;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal auxAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal nequiAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal bankAmount = BigDecimal.ZERO;

    @Column(length = 1000)
    private String notes;

    /** Conteo físico de productos al entregar turno (JSON). */
    @Column(name = "inventory_delivered_json", columnDefinition = "TEXT")
    private String inventoryDeliveredJson;

    @Column(nullable = false)
    private Instant submittedAt;

    @OneToMany(mappedBy = "handover", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ShiftHandoverExpense> expenses = new ArrayList<>();

    @OneToMany(mappedBy = "handover", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ShiftHandoverPriorPayment> priorPayments = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
