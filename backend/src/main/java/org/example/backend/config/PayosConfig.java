package org.example.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

@Configuration
public class PayosConfig {

    @Value("${payos.client-id:test}")
    private String clientId;

    @Value("${payos.api-key:test}")
    private String apiKey;

    @Value("${payos.checksum-key:test}")
    private String checksumKey;

    @Bean
    public PayOS payOS() {
        return new PayOS(clientId, apiKey, checksumKey);
    }
}
