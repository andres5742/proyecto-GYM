package com.gym.management.config;

import com.gym.management.service.UploadReferenceCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class UploadReferenceCleanupRunner {

    private final UploadReferenceCleanupService uploadReferenceCleanupService;

    @Bean
    CommandLineRunner purgeOrphanUploadReferences() {
        return args -> uploadReferenceCleanupService.purgeMissingUploadReferences();
    }
}
