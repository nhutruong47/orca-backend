package org.example.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.backend.dto.AiParseResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    
    private final TeamMemberRepository teamMemberRepository;
    private final InventoryRepository inventoryRepository;

    public AiServiceClient(TeamMemberRepository teamMemberRepository, InventoryRepository inventoryRepository) {
        this.teamMemberRepository = teamMemberRepository;
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

        return parseTask(input, teamId, context.toString());
    }

    public AiParseResult parseTask(String text, java.util.UUID teamId) {
        return parseTask(text, teamId, null);
    }

    public AiParseResult parseTask(String text, java.util.UUID teamId, String memberContext) {
        AiParseResult result = null;
        if (geminiApiKey != null && !geminiApiKey.isEmpty()) {
            try {
                result = parseWithGemini(text, memberContext);
            } catch (Exception e) {
                logger.error("⚠️ Lỗi gọi Gemini: {}. Đang dùng Regex fallback...", e.getMessage(), e);
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

    private AiParseResult parseWithGemini(String text, String memberContext) throws Exception {
        String memberSection = "";
        if (memberContext != null && !memberContext.isEmpty()) {
            memberSection = "\n4. DANH SÁCH THÀNH VIÊN VÀ NHÃN DÁN CÔNG VIỆC:\n"
                    + memberContext + "\n"
                    + "QUY TẮC PHÂN CÔNG QUAN TRỌNG: Dựa vào Nhãn dán công việc (Job Labels) của từng thành viên ở trên, "
                    + "hãy chỉ định trực tiếp tên thành viên phù hợp nhất vào trường \"assignee\" của mỗi task. "
                    + "Ưu tiên giao việc cho người có nhãn dán liên quan trực tiếp đến nội dung task. "
                    + "Nếu không có ai phù hợp, giao cho người ít việc nhất.\n\n";
        }

        String prompt = "HỆ THỐNG ĐIỀU PHỐI & GIAO VIỆC TỰ ĐỘNG (AUTO-DISPATCHER)\n"
                + "Vai trò: Bạn là Hệ điều hành quản lý nhân sự (HR & Operations Manager). Nhiệm vụ của bạn là quản lý danh sách thành viên, lập lộ trình sản xuất và trực tiếp giao việc cho từng cá nhân theo từng ca làm việc.\n\n"
                + "1. Quản lý Thành viên & Nhãn mác (Member Management):\n"
                + "- Khi có thành viên mới: Yêu cầu người dùng cung cấp [Tên] + [Mác Chính] + [Mác Phụ] vào trường `description` nếu danh sách nhân sự bên dưới chưa đầy đủ thông tin này.\n"
                + "- Lưu trữ Ma trận kỹ năng: Luôn ghi nhớ và đối soát năng lực của từng người (Kỹ thuật/Kho/Sản xuất/QC) để giao đúng việc.\n\n"
                + "2. Logic Phân rã Nhiệm vụ \"Từ Lớn đến Nhỏ\" (Top-Down Tasking):\n"
                + "- Phân tích Mục tiêu: Khi nhận mô tả đơn hàng lớn (Ví dụ: 100 tấn), bạn phải tự động chia thành các đợt sản xuất nhỏ hàng ngày (7-10 tấn/ngày). TRÌNH BÀY BẢNG MASTER ROADMAP VÀO TRƯỜNG `description` dưới dạng Markdown Table (Ngày | Lô SX | Mục tiêu | Ghi chú).\n"
                + "- Lập lịch Ngày: Chia nhỏ công việc thành Ca (Sáng/Chiều) cho từng thành viên. QUY TẮC: Luôn thêm tiền tố \"[Ca: Sáng]\" hoặc \"[Ca: Chiều]\" vào đầu trường `description` của mỗi task.\n\n"
                + "3. Tư duy Quản lý Kho & Cảnh báo:\n"
                + "- Đối soát yêu cầu sản xuất với dư lượng thực tế trong kho.\n"
                + "- Tự động cảnh báo vào trường `description` nếu vật tư không đủ cho đơn hàng.\n\n"
                + "--- DỮ LIỆU CONTEXT (NHÂN SỰ & KHO) ---\n"
                + memberSection + "\n"
                + "4. Định dạng JSON Phản hồi (BẮT BUỘC):\n"
                + "{\n"
                + "  \"title\": \"Tên mục tiêu chính (vd: Sản xuất 100 tấn cà phê)\",\n"
                + "  \"description\": \"Chứa BẢNG MASTER ROADMAP (Markdown) + Các cảnh báo về kho bãi/nhân sự nếu có\",\n"
                + "  \"quantity\": \"100 tấn\",\n"
                + "  \"quantityNumber\": 100,\n"
                + "  \"unit\": \"tấn\",\n"
                + "  \"deadline\": \"YYYY-MM-DD\",\n"
                + "  \"priority\": \"High/Medium/Low\",\n"
                + "  \"needsClarification\": false,\n"
                + "  \"tasks\": [\n"
                + "    {\n"
                + "       \"title\": \"Tên nhiệm vụ ngắn gọn\",\n"
                + "       \"description\": \"[Ca: Sáng] + Mô tả chi tiết nhiệm vụ và tiêu chuẩn đạt được.\",\n"
                + "       \"workload\": 8.5, \n"
                + "       \"priority\": 2,\n"
                + "       \"assigneeRole\": \"Kỹ thuật/Kho/Sản xuất/QC\",\n"
                + "       \"assignee\": \"Tên chính xác của người được giao từ danh sách nhân sự\"\n"
                + "    }\n"
                + "  ]\n"
                + "}\n\n"
                + "LƯU Ý: Tất cả các trường phải có mặt. Trường 'workload' và 'quantityNumber' phải là một số (number), không có dấu ngoặc kép. Trường 'deadline' phải là định dạng ISO (YYYY-MM-DD).\"\n"
                + "Yêu cầu từ người dùng: \"" + text + "\"";

        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> contentMap = new HashMap<>();
        Map<String, Object> partMap = new HashMap<>();
        partMap.put("text", prompt);
        contentMap.put("role", "user");
        contentMap.put("parts", Collections.singletonList(partMap));
        requestBody.put("contents", Collections.singletonList(contentMap));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.1);
        generationConfig.put("responseMimeType", "application/json");
        requestBody.put("generationConfig", generationConfig);

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key="
                + geminiApiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String requestJson = objectMapper.writeValueAsString(requestBody);
        logger.info("DEBUG AiServiceClient - Sending Request JSON to {}: {}", url.split("\\?")[0], requestJson);

        HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        
        logger.info("DEBUG AiServiceClient - Gemini Response Code: {}", response.getStatusCode());
        
        JsonNode rootNode = objectMapper.readTree(response.getBody());

        JsonNode candidates = rootNode.path("candidates");
        if (candidates.isMissingNode() || !candidates.isArray() || candidates.size() == 0) {
            logger.error("DEBUG AiServiceClient - Gemini Response Body: {}", response.getBody());
            throw new RuntimeException("Invalid Gemini response format");
        }

        String responseText = candidates.get(0)
                .path("content")
                .path("parts")
                .get(0)
                .path("text")
                .asText();

        if (responseText == null || responseText.isBlank()) {
            throw new RuntimeException("Empty response from Gemini");
        }

        String jsonStr = responseText.trim();
        if (jsonStr.startsWith("```json")) {
            jsonStr = jsonStr.substring(7).trim();
            if (jsonStr.endsWith("```")) jsonStr = jsonStr.substring(0, jsonStr.length() - 3).trim();
        } else if (jsonStr.startsWith("```")) {
            jsonStr = jsonStr.substring(3).trim();
            if (jsonStr.endsWith("```")) jsonStr = jsonStr.substring(0, jsonStr.length() - 3).trim();
        }

        try {
            AiParseResult result = objectMapper.readValue(jsonStr, AiParseResult.class);
            result.setSource("gemini");
            return result;
        } catch (Exception e) {
            logger.error("DEBUG AiServiceClient - JSON Parsing Error: {}. Raw text: {}", e.getMessage(), responseText);
            Pattern jsonPattern = Pattern.compile("\\{.*\\}", Pattern.DOTALL);
            Matcher matcher = jsonPattern.matcher(responseText);
            if (matcher.find()) {
                String fallbackStr = matcher.group();
                AiParseResult result = objectMapper.readValue(fallbackStr, AiParseResult.class);
                result.setSource("gemini");
                return result;
            } else {
                throw new RuntimeException("Could not parse JSON from Gemini: " + e.getMessage(), e);
            }
        }
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
