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

@Service
public class AiServiceClient {

    @Value("${ai.service.api-key:}")
    private String geminiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private final TeamMemberRepository teamMemberRepository;

    public AiServiceClient(TeamMemberRepository teamMemberRepository) {
        this.teamMemberRepository = teamMemberRepository;
    }

    /**
     * Phân tích ngôn ngữ tự nhiên để lấy thông tin mục tiêu và tự động chia thành
     * các task nhỏ.
     */
    public AiParseResult generateTaskPlan(String outputTarget, String deadline, Integer priority,
            List<String> memberNames) {
        String input = outputTarget;
        if (deadline != null && !deadline.isEmpty()) {
            input += ", hạn chót: " + deadline;
        }

        return parseTask(input, null);
    }

    public AiParseResult parseTask(String text, java.util.UUID teamId) {
        AiParseResult result = null;
        if (geminiApiKey != null && !geminiApiKey.isEmpty()) {
            try {
                result = parseWithGemini(text);
            } catch (Exception e) {
                System.out.println("⚠️ Lỗi gọi Gemini: " + e.getMessage() + ". Đang dùng Regex fallback...");
            }
        }
        if (result == null) {
            result = parseWithRegex(text);
        }

        // If tasks is null (e.g. from regex fallback), create a generic task list
        if (result.getTasks() == null || result.getTasks().isEmpty()) {
            List<Map<String, Object>> fallbackTasks = new ArrayList<>();
            Map<String, Object> genericTask = new HashMap<>();
            genericTask.put("title", "Xử lý yêu cầu: " + (result.getMainGoal() != null ? result.getMainGoal() : text));
            genericTask.put("description", result.getDescription() != null ? result.getDescription() : "Thực hiện công việc theo yêu cầu.");
            genericTask.put("workload", 8.0);
            genericTask.put("priority", 2);
            genericTask.put("assigneeRole", "Senior"); // Fallback
            fallbackTasks.add(genericTask);
            result.setTasks(fallbackTasks);
        }
        
        return result;
    }

    private AiParseResult parseWithGemini(String text) throws Exception {
        String prompt = "Vai trò: Bạn là Hệ Thống AI Điều Phối Sản Xuất Đa Tầng (Multi-Level Coffee Factory Orchestrator) - bộ não điều hành xưởng cà phê, có khả năng lập kế hoạch chiến lược và xử lý biến động nhân sự.\n"
                + "1. Quy Tắc Cơ Bản:\n"
                + "- Tầm nhìn (Giai đoạn): AI phải xác định Giai đoạn (phase) như Năm/Tháng/Tuần/Ngày.\n"
                + "- Nhân sự (Role): AI phân chia công việc theo cấp bậc 'Senior' (Chính), 'Junior' (Phụ/Học việc), 'Intern' (Thực tập). \n"
                + "- Đào tạo: Khi Senior làm việc, hãy tự động tạo 1 task 'Quan sát/Hỗ trợ' cho Junior/Intern.\n\n"
                + "2. Logic Xử lý Biến động (Dynamic Crisis Handling):\n"
                + "Khi phát hiện có yếu tố khẩn cấp (gấp, thiếu người, sự cố), Áp dụng các Rule sau:\n"
                + "- Rule 1 (Chặn Task phụ): Dừng/giảm thời lượng các task dọn dẹp, chuẩn bị của nhân sự phụ.\n"
                + "- Rule 2 (Thăng cấp tạm thời): Đôn Junior lên làm thay Senior nếu cần, tăng thời gian (workload) lên và ép thêm Task Kiểm soát chất lượng (QC).\n"
                + "- Rule 3 (Tái cấu trúc): Ưu tiên đơn khẩn > VIP > Thường.\n"
                + "- Rule 4 (Contingency): Phải luôn xuất ra một phương án dự phòng vào trường `contingency` (vd: thuê ngoài, dời hạn).\n\n"
                + "3. Format JSON Bắt buộc (Chỉ trả về JSON thuần):\n"
                + "{\n"
                + "  \"phase\": \"Giai đoạn (Ngày/Tuần/Tháng/Năm)\",\n"
                + "  \"mainGoal\": \"Mục tiêu chính (Sản lượng/Chất lượng)\",\n"
                + "  \"contingency\": \"Phương án dự phòng (Nếu A nghỉ -> B thay thế...)\",\n"
                + "  \"needsClarification\": false (chỉ true nếu thiếu quá nhiều dữ kiện bắt buộc như khối lượng, hạn chót),\n"
                + "  \"description\": \"Giải thích chiến lược hoặc hỏi thêm user\",\n"
                + "  \"tasks\": [\n"
                + "    {\n"
                + "       \"title\": \"Tên nhiệm vụ\",\n"
                + "       \"description\": \"Mô tả chi tiết nhiệm vụ (dựa trên Rules)\",\n"
                + "       \"workload\": số (thời gian dự tính bằng giờ, vd: 4.5),\n"
                + "       \"priority\": số (1=Thấp, 2=Vừa, 3=Cao),\n"
                + "       \"assigneeRole\": \"Senior hoặc Junior hoặc Intern\"\n"
                + "    }\n"
                + "  ]\n"
                + "}\n\n"
                + "Câu của người dùng (tình huống hoặc yêu cầu sx): \"" + text + "\"";

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key="
                + geminiApiKey;

        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> contentMap = new HashMap<>();
        Map<String, Object> partMap = new HashMap<>();
        partMap.put("text", prompt);
        contentMap.put("parts", Collections.singletonList(partMap));
        requestBody.put("contents", Collections.singletonList(contentMap));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.1);
        requestBody.put("generationConfig", generationConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        JsonNode rootNode = objectMapper.readTree(response.getBody());

        JsonNode candidates = rootNode.path("candidates");
        if (candidates.isMissingNode() || !candidates.isArray() || candidates.size() == 0) {
            throw new RuntimeException("Invalid Gemini response format");
        }

        String responseText = candidates.get(0)
                .path("content")
                .path("parts")
                .get(0)
                .path("text")
                .asText();

        // Extract JSON using regex from markdown response:
        Pattern jsonPattern = Pattern.compile("\\{.*\\}", Pattern.DOTALL);
        Matcher matcher = jsonPattern.matcher(responseText);
        if (matcher.find()) {
            String jsonStr = matcher.group();
            AiParseResult result = objectMapper.readValue(jsonStr, AiParseResult.class);
            result.setSource("gemini");
            return result;
        } else {
            throw new RuntimeException("Could not extract JSON from Gemini text: " + responseText);
        }
    }

    private AiParseResult parseWithRegex(String text) {
        AiParseResult result = new AiParseResult();
        result.setSource("regex");
        result.setPhase("Ngày");
        result.setMainGoal(text.substring(0, Math.min(text.length(), 50)));
        result.setContingency("Giữ nguyên kế hoạch");
        result.setDescription("Hệ thống tự động biên dịch do AI Model gặp lỗi hoặc không khả dụng.");
        result.setNeedsClarification(false);
        return result;
    }
}
