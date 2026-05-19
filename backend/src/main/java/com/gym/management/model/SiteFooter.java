package com.gym.management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "site_footer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SiteFooter {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(length = 300)
    private String tagline;

    @Column(length = 300)
    private String address;

    @Column(length = 30)
    private String phone;

    @Column(length = 300)
    private String instagramUrl;

    @Column(length = 300)
    private String facebookUrl;

    @Column(length = 300)
    private String tiktokUrl;

    @Column(length = 300)
    private String youtubeUrl;

    @Column(length = 300)
    private String whatsappUrl;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
