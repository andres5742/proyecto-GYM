package com.gym.management.config;

import com.gym.management.service.ShiftHandoverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ShiftHandoverSurplusSyncRunner {

    private final ShiftHandoverService shiftHandoverService;

    @Bean
    CommandLineRunner syncMissingHandoverSurplusToBilling() {
        return args -> {
            int created = shiftHandoverService.syncMissingHandoverCashSurplusInBilling();
            if (created > 0) {
                log.info("Sincronización automática: {} sobrante(s) de entrega registrados en Facturación", created);
            }
        };
    }
}
