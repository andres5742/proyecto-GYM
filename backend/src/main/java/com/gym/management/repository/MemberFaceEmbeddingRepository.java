package com.gym.management.repository;

import com.gym.management.model.MemberFaceEmbedding;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MemberFaceEmbeddingRepository extends JpaRepository<MemberFaceEmbedding, Long> {

    Optional<MemberFaceEmbedding> findByMemberId(Long memberId);

    void deleteByMemberId(Long memberId);

    @Query("SELECT e FROM MemberFaceEmbedding e JOIN FETCH e.member m LEFT JOIN FETCH m.plan")
    List<MemberFaceEmbedding> findAllWithMember();
}
