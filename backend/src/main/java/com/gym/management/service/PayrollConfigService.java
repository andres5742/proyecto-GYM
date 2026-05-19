package com.gym.management.service;

import com.gym.management.dto.PayrollConfigRequest;
import com.gym.management.dto.PayrollConfigResponse;
import com.gym.management.mapper.PayrollConfigMapper;
import com.gym.management.model.PayrollConfig;
import com.gym.management.repository.PayrollConfigRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PayrollConfigService {

    private final PayrollConfigRepository payrollConfigRepository;

    @Transactional(readOnly = true)
    public PayrollConfigResponse get() {
        return PayrollConfigMapper.toResponse(getConfig());
    }

    @Transactional
    public PayrollConfigResponse update(PayrollConfigRequest request) {
        PayrollConfig config = getConfig();
        config.setWeekdayHourlyRate(request.weekdayHourlyRate());
        config.setSundayHourlyRate(request.sundayHourlyRate());
        return PayrollConfigMapper.toResponse(payrollConfigRepository.save(config));
    }

    public PayrollConfig getConfig() {
        return payrollConfigRepository.findById(PayrollConfig.SINGLETON_ID)
                .orElseGet(this::createDefault);
    }

    private PayrollConfig createDefault() {
        PayrollConfig config = PayrollConfig.builder()
                .id(PayrollConfig.SINGLETON_ID)
                .weekdayHourlyRate(new BigDecimal("15000"))
                .sundayHourlyRate(new BigDecimal("20000"))
                .build();
        return payrollConfigRepository.save(config);
    }
}
