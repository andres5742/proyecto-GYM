package com.gym.management.config;

import com.gym.management.service.WallPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WallPostCleanupScheduler {

    private final WallPostService wallPostService;

    @Scheduled(cron = "0 0 3 * * *")
    public void purgeExpiredPosts() {
        int removed = wallPostService.purgeExpired();
        if (removed > 0) {
            log.info("Publicaciones expiradas eliminadas: {}", removed);
        }
    }
}
