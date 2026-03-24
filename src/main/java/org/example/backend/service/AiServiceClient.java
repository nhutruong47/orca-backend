package org.example.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.backend.dto.AiParseResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.*;

import org.example.backend.repository.TeamMemberRepository;
import org.example.backend.repository.InventoryRepository;
import org.example.backend.entity.InventoryItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AiServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(AiServiceClient.class);

    @Value("${ai.service.api-key:}")
    private String geminiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private final InventoryRepository inventoryRepository;

    public AiServiceClient(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        if (this.geminiApiKey != null) {
            this.geminiApiKey = this.geminiApiKey.trim();
            logger.info("DEBUG AiServiceClient - Gemini API Key Initialized, Length: {}", this.geminiApiKey.length());
        } else {
            logger.error("DEBUG AiServiceClient - Gemini API Key is MISSING!");
        }
    }

    public AiParseResult generateTaskPlan(String outputTarget, String deadline, Integer priority,
            UUID teamId, Map<String, List<String>> memberLabels) {
        String input = outputTarget;
        if (deadline != null && !deadline.isEmpty()) {
            input += ", hạn chót: " + deadline;
        }

        StringBuilder context = new StringBuilder();
        if (memberLabels != null && !memberLabels.isEmpty()) {
            context.append("--- DANH SÁCH NHÂN SỰ ---\n");
            for (Map.Entry<String, List<String>> entry : memberLabels.entrySet()) {
                String name = entry.getKey();
                List<String> labels = entry.getValue();
                context.append("- ").append(name);
                if (labels != null && !labels.isEmpty()) {
                    context.append(" (Kỹ năng/Nhãn: ").append(String.join(", ", labels)).append(")");
                } else {
                    context.append(" (Chưa gán nhãn)");
                }
                context.append("\n");
            }
        }

        if (teamId != null) {
            List<InventoryItem> items = inventoryRepository.findByTeamIdOrderByLastUpdatedDesc(teamId);
            if (!items.isEmpty()) {
                context.append("\n--- TÌNH TRẠNG KHO HIỆN TẠI ---\n");
                for (InventoryItem item : items) {
                    context.append("- ").append(item.getName())
                           .append(": ").append(item.getQuantity()).append(" ").append(item.getUnit())
                           .append(" (Trạng thái: ").append(item.getStockStatus()).append(")\n");
                }
            }
        }

        return parseTask(input, teamId, context.toString(), null);
    }

    public AiParseResult parseTask(String text, java.util.UUID teamId) {
        return parseTask(text, teamId, null, null);
    }

    public AiParseResult parseTask(String text, java.util.UUID teamId, String memberContext, String historyContext) {
        AiParseResult result = null;
        if (geminiApiKey != null && !geminiApiKey.isEmpty()) {
            try {
                result = parseWithGemini(text, memberContext, historyContext);
            } catch (Exception e) {
                logger.error("⚠️ Lỗi gọi Gemini: {}", e.getMessage(), e);
                result = new AiParseResult();
                result.setTitle("LỖI KẾT NỐI AI");
                result.setDescription("❌ Google Cloud từ chối kết nối. Lỗi chi tiết: **" + e.getMessage() + "**\n\nNếu bạn thấy lỗi 403 hoặc 400, có nghĩa là **API Key này đã bị hỏng/hết hạn hoặc chưa được cấp quyền truy cập AI**.");
                result.setNeedsClarification(true);
            }
        }
        if (result == null) {
            result = parseWithRegex(text);
        }

        if (result.getTasks() == null || result.getTasks().isEmpty()) {
            List<Map<String, Object>> fallbackTasks = new ArrayList<>();
            Map<String, Object> genericTask = new HashMap<>();
            genericTask.put("title", "Xử lý yêu cầu: " + (result.getTitle() != null ? result.getTitle() : text));
            genericTask.put("description", result.getDescription() != null ? result.getDescription() : "Thực hiện công việc theo yêu cầu.");
            genericTask.put("workload", 8.0);
            genericTask.put("priority", 2);
            genericTask.put("assigneeRole", "Senior");
            fallbackTasks.add(genericTask);
            result.setTasks(fallbackTasks);
        }
        
        return result;
    }

    private AiParseResult parseWithGemini(String text, String memberContext, String historyContext) throws Exception {
        String memberSection = "";
        if (memberContext != null && !memberContext.isEmpty()) {
            memberSection = "\n--- DANH SÁCH THÀNH VIÊN VÀ NHÃN DÁN CÔNG VIỆC ---\n"
                    + memberContext + "\n"
                    + "PHÂN CÔNG: Dựa vào Job Labels để gán người phù hợp nhất vào trường 'assignee'.\n\n";
        }

        String historySection = "";
        if (historyContext != null && !historyContext.isEmpty()) {
            historySection = "\n--- LỊCH SỬ CHAT TRƯỚC ĐÓ ---\n"
                    + historyContext + "\n\n"
                    + "QUY TẮC CẬP NHẬT: Ưu tiên giữ cấu trúc kế hoạch cũ, chỉ thay đổi các thông tin người dùng vừa yêu cầu sửa đổi.\n";
        }

        String prompt = "HỆ THỐNG ĐIỀU PHỐI & GIAO VIỆC TỰ ĐỘNG\n"
                + "Vai trò: Bạn là nhân sự thông minh. Nhiệm vụ: Nhận yêu cầu, phân rã công việc và giao việc.\n\n"
                + "1. QUY TẮC TƯƠNG TÁC (Multi-turn):\n"
                + "- LUÔN bắt đầu phản hồi bằng lời chào: 'Chào anh/chị'.\n"
                + "- Xưng hô: Xưng là 'em', gọi User là 'anh/chị'.\n"
                + "- Nếu User thiếu thông tin TRỌNG YẾU (deadline, số lượng) hoặc yêu cầu chưa rõ ràng: Hãy khéo léo ĐẶT CÂU HỎI vào `description` để xin thêm thông tin và đặt `needsClarification` = true.\n"
                + "- LUÔN GỢI Ý CÂU HỎI (Proactive): Ngay cả khi đã có kế hoạch, hãy liệt kê 1-3 câu hỏi gợi ý để tối ưu kế hoạch vào mảng `suggestedQuestions` (ví dụ: 'Anh/chị có muốn bổ sung thông tin cho sản lượng không?', v.v.)\n"
                + "- Nếu User yêu cầu sửa đổi: Cập nhật trường tương ứng và giữ nguyên phần khác.\n"
                + "2. PHÂN RÃ CÔNG VIỆC:\n"
                + "- Trình bày BẢNG ROADMAP (markdown) trong `description` sau lời chào và câu hỏi.\n"
                + "- Từng task ghi rõ [Ca: Sáng/Chiều] ở đầu mô tả.\n\n"
                + "3. PHONG CÁCH: Lịch sự, chuyên nghiệp, hỗ trợ tận tâm.\n\n"
                + "--- CONTEXT ---\n"
                + memberSection
                + historySection
                + "Định dạng JSON Phản hồi (BẮT BUỘC):\n"
                + "{\n"
                + "  \"title\": \"Tên mục tiêu\",\n"
                + "  \"description\": \"Câu trả lời hoặc Bảng roadmap\",\n"
                + "  \"quantity\": \"Số lượng\",\n"
                + "  \"quantityNumber\": 100,\n"
                + "  \"unit\": \"đơn vị\",\n"
                + "  \"deadline\": \"YYYY-MM-DD\",\n"
                + "  \"priority\": \"High/Medium/Low\",\n"
                + "  \"needsClarification\": false,\n"
                + "  \"suggestedQuestions\": [\"Câu hỏi 1?\", \"Câu hỏi 2?\"],\n"
                + "  \"tasks\": [\n"
                + "    { \"title\": \"...\", \"description\": \"[Ca: ...] ...\", \"workload\": 8.0, \"priority\": 2, \"assignee\": \"...\" }\n"
                + "  ]\n"
                + "}\n"
                + "Yêu cầu mới nhất của User: \"" + text + "\"";

        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> contentMap = new HashMap<>();
        Map<String, Object> partMap = new HashMap<>();
        partMap.put("text", prompt);
        contentMap.put("role", "user");
        contentMap.put("parts", Collections.singletonList(partMap));
        requestBody.put("contents", Collections.singletonList(contentMap));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.2);
        generationConfig.put("responseMimeType", "application/json");
        requestBody.put("generationConfig", generationConfig);

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + geminiApiKey;
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        
        String requestJson = objectMapper.writeValueAsString(requestBody);
        HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

        String responseBody;
        try {
            logger.info("📡 Calling Gemini API: {}", url);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            responseBody = response.getBody();
        } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
            logger.error("🛑 Gemini Rate Limit (429) hit: {}", e.getResponseBodyAsString());
            throw new Exception("Hệ thống đang quá tải (Rate limit). Vui lòng thử lại sau giây lát.");
        } catch (Exception e) {
            logger.error("❌ Gemini Call Exception: {}", e.getMessage());
            throw e;
        }

        if (responseBody == null || responseBody.isBlank()) {
            throw new Exception("Gemini API returned an empty response.");
        }

        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(responseBody);
        } catch (Exception e) {
            logger.error("❌ Failed to parse Gemini response as JSON. Body preview: {}", 
                responseBody.substring(0, Math.min(responseBody.length(), 500)));
            throw new Exception("Lỗi xử lý phản hồi từ AI. Bản tin không đúng định dạng.");
        }

        JsonNode candidate = rootNode.path("candidates").get(0);
        if (candidate.isMissingNode()) {
            logger.error("❌ No candidates in Gemini response: {}", responseBody);
            throw new Exception("AI không đưa ra phản hồi phù hợp.");
        }

        String responseText = candidate.path("content").path("parts").get(0).path("text").asText();

        String jsonStr = responseText.trim();
        if (jsonStr.startsWith("```json")) jsonStr = jsonStr.substring(7).trim();
        if (jsonStr.endsWith("```")) jsonStr = jsonStr.substring(0, jsonStr.length() - 3).trim();

        return objectMapper.readValue(jsonStr, AiParseResult.class);
    }

    private AiParseResult parseWithRegex(String text) {
        AiParseResult result = new AiParseResult();
        result.setSource("regex");
        result.setTitle("Kế hoạch: " + text.substring(0, Math.min(text.length(), 50)));
        result.setDescription("Hệ thống tự động biên dịch do AI Model gặp lỗi hoặc không khả dụng.");
        result.setNeedsClarification(false);
        return result;
    }
}
