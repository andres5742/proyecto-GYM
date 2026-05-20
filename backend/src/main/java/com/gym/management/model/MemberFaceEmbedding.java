package com.gym.management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/** Descriptor facial (128 floats) capturado por webcam — sin foto en claro. */
@Entity
@Table(name = "member_face_embeddings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberFaceEmbedding {

    public static final int DESCRIPTOR_LENGTH = 128;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    /** JSON array de 128 números (face-api descriptor). */
    @Column(name = "descriptor_json", nullable = false, columnDefinition = "text")
    private String descriptorJson;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant enrolledAt;
}
