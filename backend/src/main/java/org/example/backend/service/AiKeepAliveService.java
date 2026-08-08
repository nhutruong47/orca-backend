package org.example.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AiKeepAliveService {

    private static final Logger logger = LoggerFactory.getLogger(AiKeepAliveService.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.v2.service-url:http://127.0.0.1:8000}")
    private String aiServiceUrl;

    // Chạy mỗi 10 phút (600000 ms) để đánh thức dịch vụ AI trên Render
    @Scheduled(fixedRate = 600000)
    public void pingAiService() {
        if (aiServiceUrl.contains("127.0.0.1") || aiServiceUrl.contains("localhost")) {
            return; // Bỏ qua nếu đang chạy local
        }
        
        try {
            logger.info("Pinging AI service to keep it alive: {}", aiServiceUrl);
            restTemplate.getForObject(aiServiceUrl + "/health", String.class);
            logger.info("AI service ping successful.");
        } catch (Exception e) {
            logger.warn("Failed to ping AI service: {}", e.getMessage());
        }
    }
}
