package com.gym.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class TurnstileGatewayService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String webhookUrl;

    public TurnstileGatewayService(@Value("${app.access.turnstile-webhook-url:}") String webhookUrl) {
        this.webhookUrl = webhookUrl == null ? "" : webhookUrl.trim();
    }

    public boolean openGate(String memberName, Long memberId) {
        if (webhookUrl.isEmpty()) {
            log.info("TORNIQUETE ABIERTO (simulado) — afiliado: {} (id={})", memberName, memberId);
            return true;
        }
        try {
            var body = java.util.Map.of("action", "open", "memberId", memberId, "memberName", memberName);
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity(webhookUrl, new HttpEntity<>(body, headers), String.class);
            log.info("Señal de apertura enviada al torniquete para {}", memberName);
            return true;
        } catch (Exception ex) {
            log.warn("No se pudo activar el torniquete vía webhook: {}", ex.getMessage());
            return false;
        }
    }
}
