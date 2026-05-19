package com.gym.management.repository;

import com.gym.management.model.CarouselSlide;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarouselSlideRepository extends JpaRepository<CarouselSlide, Long> {

    List<CarouselSlide> findByActiveTrueOrderByDisplayOrderAscIdAsc();

    List<CarouselSlide> findAllByOrderByDisplayOrderAscIdAsc();
}
