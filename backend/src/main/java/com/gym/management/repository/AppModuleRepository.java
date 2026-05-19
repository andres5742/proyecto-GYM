package com.gym.management.repository;

import com.gym.management.model.AppModule;
import com.gym.management.model.ModuleCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppModuleRepository extends JpaRepository<AppModule, String> {

    List<AppModule> findAllByOrderBySortOrderAsc();

    List<AppModule> findByCategoryOrderBySortOrderAsc(ModuleCategory category);
}
