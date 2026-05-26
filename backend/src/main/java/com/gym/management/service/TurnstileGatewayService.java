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
    private final String webhookKey;

    public TurnstileGatewayService(
            @Value("${app.access.turnstile-webhook-url:}") String webhookUrl,
            @Value("${app.access.turnstile-webhook-key:}") String webhookKey) {
        this.webhookUrl = webhookUrl == null ? "" : webhookUrl.trim();
        this.webhookKey = webhookKey == null ? "" : webhookKey.trim();
    }

    public boolean openGate(String memberName, Long memberId) {
        return sendGateAction("open", memberName, memberId);
    }

    /** Refuerza el bloqueo del torniquete cuando el acceso es denegado. */
    public boolean lockGate(String memberName, Long memberId) {
        return sendGateAction("lock", memberName, memberId);
    }

    private boolean sendGateAction(String action, String memberName, Long memberId) {
        if (webhookUrl.isEmpty()) {
            if ("open".equals(action)) {
                log.info("TORNIQUETE ABIERTO (simulado) — afiliado: {} (id={})", memberName, memberId);
            } else {
                log.info("TORNIQUETE BLOQUEADO (simulado) — afiliado: {} (id={})", memberName, memberId);
            }
            return true;
        }
        try {
            var body = new java.util.HashMap<String, Object>();
            body.put("action", action);
            if (memberId != null) {
                body.put("memberId", memberId);
            }
            if (memberName != null && !memberName.isBlank()) {
                body.put("memberName", memberName);
            }
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (!webhookKey.isEmpty()) {
                headers.set("X-Device-Key", webhookKey);
            }
            restTemplate.postForEntity(webhookUrl, new HttpEntity<>(body, headers), String.class);
            log.info("Señal {} enviada al torniquete para {}", action, memberName);
            return true;
        } catch (Exception ex) {
            log.warn("No se pudo enviar señal {} al torniquete vía webhook: {}", action, ex.getMessage());
            return false;
        }
    }
}
