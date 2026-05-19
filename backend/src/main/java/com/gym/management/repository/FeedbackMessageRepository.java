package com.gym.management.repository;

import com.gym.management.model.FeedbackMessage;
import com.gym.management.model.FeedbackStatus;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackMessageRepository extends JpaRepository<FeedbackMessage, Long> {

    @EntityGraph(attributePaths = "images")
    List<FeedbackMessage> findAllByOrderByCreatedAtDesc();

    long countByStatus(FeedbackStatus status);
}
