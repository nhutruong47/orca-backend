import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.backend.dto.ai.AiPlanDraftResponse;
import java.io.File;

public class TestJackson {
    public static void main(String[] args) throws Exception {
        String json = "{\"goalTitle\":\"Kế hoạch\",\"outputTarget\":\"Hoàn thành\",\"deadline\":null,\"priority\":3,\"tasks\":[{\"title\":\"T1\",\"description\":\"D1\",\"priority\":3,\"workload\":1.5,\"suggestedAssigneeId\":null,\"suggestedAssigneeName\":null,\"suggestedReason\":null}],\"aiNote\":null}";
        ObjectMapper mapper = new ObjectMapper();
        AiPlanDraftResponse draft = mapper.readValue(json, AiPlanDraftResponse.class);
        System.out.println("Title: " + draft.getGoalTitle());
        System.out.println("Tasks count: " + draft.getTasks().size());
    }
}
