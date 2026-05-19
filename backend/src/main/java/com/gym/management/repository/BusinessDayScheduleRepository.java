package com.gym.management.repository;

import com.gym.management.model.BusinessDaySchedule;
import java.time.DayOfWeek;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessDayScheduleRepository extends JpaRepository<BusinessDaySchedule, DayOfWeek> {}
