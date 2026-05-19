package com.gym.management.repository;

import com.gym.management.model.GymMediaItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GymMediaItemRepository extends JpaRepository<GymMediaItem, Long> {

    List<GymMediaItem> findByActiveTrueOrderByDisplayOrderAscIdAsc();

    List<GymMediaItem> findAllByOrderByDisplayOrderAscIdAsc();
}
