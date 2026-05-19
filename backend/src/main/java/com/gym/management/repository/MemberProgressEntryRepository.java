package com.gym.management.repository;

import com.gym.management.model.MemberProgressEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberProgressEntryRepository extends JpaRepository<MemberProgressEntry, Long> {

    @EntityGraph(attributePaths = "photos")
    List<MemberProgressEntry> findByMemberIdOrderByRecordedAtDescIdDesc(Long memberId);

    @EntityGraph(attributePaths = "photos")
    Optional<MemberProgressEntry> findByIdAndMemberId(Long id, Long memberId);
}
