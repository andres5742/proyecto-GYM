package com.gym.management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(length = 20)
    private String phone;

    private LocalDate birthDate;

    /** Número Nequi donde se consigna el pago de nómina. */
    @Column(length = 20)
    private String nequiNumber;

    @Column(length = 80)
    private String bankName;

    @Column(length = 40)
    private String bankAccountNumber;

    @Column(unique = true, length = 50)
    private String username;

    @Column(length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserRole role;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /** Foto para calificación pública de entrenadores. */
    @Column(length = 500)
    private String photoUrl;

    /** Si participa en "Califica a tu entrenador" (solo super admin lo activa). */
    @Column(nullable = false)
    @Builder.Default
    private Boolean ratingEligible = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
