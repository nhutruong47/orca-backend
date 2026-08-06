package org.example.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.backend.entity.InventoryItem;
import org.example.backend.entity.ProductionOrder;
import org.example.backend.entity.Team;
import org.example.backend.entity.TeamMember;
import org.example.backend.entity.User;
import org.example.backend.repository.InventoryRepository;
import org.example.backend.repository.ProductionOrderRepository;
import org.example.backend.repository.TeamMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps the AI service's RAG knowledge base in sync with the live data
 * the backend owns (inventory, orders, products, teams, users).
 *
 * Behavior:
 *   * Listens for in-process CRUD events fired by other services.
 *   * Pushes the affected entity as an ingest record to the AI service
 *     via {@code POST /api/rag/ingest/{source}}.
 *   * Debounces: a per-{team, source} debounce window coalesces a burst
 *     of mutations into a single push to avoid hammering the AI service.
 *
 * The AI service is treated as eventually-consistent; if the AI push
 * fails we log and continue — the next mutation for the same record will
 * retry.
 */
// Disabled: Ver4 RAG indexing was replaced by the stable Ver3 AI workflow.
// Keep event record types available for callers, but do not register this as a Spring service.
@Transactional
public class AsyncIndexerService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncIndexerService.class);

    /** Debounce window per (team, source). */
    private static final long DEBOUNCE_MS = 1_500;

    private final InventoryRepository inventoryRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.service.base-url:http://localhost:8000}")
    private String aiServiceBaseUrl;

    /** Debounce bookkeeping: key "teamId|source" -> last scheduled at (ms epoch). */
    private final Map<String, Long> lastSchedule = new ConcurrentHashMap<>();

    public AsyncIndexerService(InventoryRepository inventoryRepository,
                               ProductionOrderRepository productionOrderRepository,
                               TeamMemberRepository teamMemberRepository) {
        this.inventoryRepository = inventoryRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.teamMemberRepository = teamMemberRepository;
    }

    // ------------------------------------------------------------------
    // Event types — kept simple, just inner records fired via Spring.
    // ------------------------------------------------------------------

    public record InventoryChanged(UUID teamId, UUID itemId) {}
    public record OrderChanged(UUID teamId, UUID orderId) {}
    public record ProductChanged(UUID productId) {}
    public record TeamChanged(UUID teamId) {}
    public record UserChanged(UUID userId) {}

    @EventListener
    public void onInventoryChanged(InventoryChanged ev) {
        schedule("inventory", ev.teamId());
    }

    @EventListener
    public void onOrderChanged(OrderChanged ev) {
        schedule("orders", ev.teamId());
    }

    @EventListener
    public void onProductChanged(ProductChanged ev) {
        schedule("products", null);
    }

    @EventListener
    public void onTeamChanged(TeamChanged ev) {
        schedule("teams", ev.teamId());
    }

    @EventListener
    public void onUserChanged(UserChanged ev) {
        schedule("users", null);
    }

    /**
     * When the application finishes starting up, schedule an initial
     * reindex for all sources we know about. This is fire-and-forget
     * and won't block startup.
     */
    @Async
    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshed(ContextRefreshedEvent ev) {
        if (!Boolean.parseBoolean(System.getenv().getOrDefault("ORCA_AUTO_INDEX_ON_BOOT", "false"))) {
            return;
        }
        logger.info("Triggering startup RAG reindex for inventory/orders/products/teams/users.");
        reindexAllAsync();
    }

    public void reindexAllAsync() {
        try {
            inventoryRepository.findAll().forEach(i -> schedule("inventory", i.getTeam().getId()));
            productionOrderRepository.findAll().forEach(o -> {
                if (o.getTeam() != null) {
                    schedule("orders", o.getTeam().getId());
                }
            });
            schedule("products", null);
            schedule("teams", null);
            schedule("users", null);
        } catch (Exception exc) {
            logger.warn("Failed to schedule startup reindex: {}", exc.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Scheduling
    // ------------------------------------------------------------------

    private void schedule(String source, UUID teamId) {
        String key = (teamId == null ? "_" : teamId.toString()) + "|" + source;
        long now = System.currentTimeMillis();
        Long last = lastSchedule.get(key);
        if (last != null && now - last < DEBOUNCE_MS) {
            return; // coalesce
        }
        lastSchedule.put(key, now);
        flushAsync(source, teamId);
    }

    @Async
    public void flushAsync(String source, UUID teamId) {
        try {
            Thread.sleep(DEBOUNCE_MS); // give more changes a chance to coalesce
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        try {
            switch (source) {
                case "inventory" -> pushInventory(teamId);
                case "orders" -> pushOrders(teamId);
                case "products" -> pushProducts();
                case "teams" -> pushTeams(teamId);
                case "users" -> pushUsers();
                default -> logger.warn("Unknown source '{}' — skipping", source);
            }
        } catch (Exception exc) {
            logger.warn("Background indexing for {}({}) failed: {}", source, teamId, exc.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Push helpers
    // ------------------------------------------------------------------

    private void pushInventory(UUID teamId) {
        List<InventoryItem> items = teamId == null
                ? inventoryRepository.findAll()
                : inventoryRepository.findByTeamIdOrderByLastUpdatedDesc(teamId);
        if (items.isEmpty()) {
            return;
        }
        for (InventoryItem item : items) {
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("id", "inv-" + item.getId());
            record.put("title", "Kho: " + (item.getName() == null ? item.getId().toString() : item.getName()));
            StringBuilder body = new StringBuilder();
            body.append("Tồn kho: ").append(item.getQuantity()).append(" ").append(item.getUnit() == null ? "" : item.getUnit());
            body.append(" của sản phẩm ").append(item.getName());
            body.append(", trạng thái ").append(item.getStockStatus());
            if (item.getTeam() != null) {
                body.append(", team ").append(item.getTeam().getId());
            }
            body.append(". Cập nhật ").append(LocalDateTime.now());
            record.put("content", body.toString());
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("category", "inventory");
            meta.put("team_id", item.getTeam() == null ? null : item.getTeam().getId().toString());
            meta.put("last_updated", LocalDateTime.now().toString());
            record.put("metadata", meta);

            pushOne("inventory", teamId == null && item.getTeam() != null
                    ? item.getTeam().getId()
                    : teamId, List.of(record));
        }
    }

    private void pushOrders(UUID teamId) {
        List<ProductionOrder> orders = teamId == null
                ? productionOrderRepository.findAll()
                : productionOrderRepository.findByTeamIdOrderByCreatedAtDesc(teamId);
        if (orders.isEmpty()) {
            return;
        }
        java.util.List<Map<String, Object>> records = new java.util.ArrayList<>();
        for (ProductionOrder o : orders) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", "order-" + o.getId());
            r.put("title", "Đơn hàng " + (o.getOrderCode() == null ? o.getId().toString() : o.getOrderCode()));
            StringBuilder body = new StringBuilder();
            body.append("Đơn sản xuất ");
            if (o.getOrderCode() != null) body.append(o.getOrderCode());
            if (o.getTitle() != null) body.append(" — ").append(o.getTitle());
            if (o.getProductType() != null) body.append(", sản phẩm ").append(o.getProductType());
            if (o.getOutputTarget() != null) body.append(", sản lượng mục tiêu ").append(o.getOutputTarget());
            if (o.getUnit() != null) body.append(" ").append(o.getUnit());
            if (o.getStatus() != null) body.append(", trạng thái ").append(o.getStatus());
            if (o.getInternalDeadline() != null) body.append(", hạn nội bộ ").append(o.getInternalDeadline());
            r.put("content", body.toString());
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("category", "orders");
            meta.put("team_id", o.getTeam() == null ? null : o.getTeam().getId().toString());
            meta.put("status", o.getStatus());
            meta.put("last_updated", LocalDateTime.now().toString());
            r.put("metadata", meta);
            records.add(r);
        }
        UUID effectiveTeam = teamId == null && orders.get(0).getTeam() != null
                ? orders.get(0).getTeam().getId()
                : teamId;
        pushOne("orders", effectiveTeam, records);
    }

    private void pushProducts() {
        // Products are kept global; we simply notify the AI service to reindex.
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("team_id", null);
        body.put("replace", false);
        // Send an empty ingestion request so the AI service knows products
        // may have changed; if there is a periodic cron it can repopulate.
        try {
            restTemplate.exchange(
                    aiServiceBaseUrl + "/api/rag/ingest/products",
                    HttpMethod.POST,
                    new HttpEntity<>(Map.of("records", List.of()), jsonHeaders()),
                    Map.class
            );
        } catch (RestClientException exc) {
            logger.debug("products ingest notify failed: {}", exc.getMessage());
        }
    }

    private void pushTeams(UUID teamId) {
        // Same as products — passive notify.
        try {
            restTemplate.exchange(
                    aiServiceBaseUrl + "/api/rag/ingest/teams",
                    HttpMethod.POST,
                    new HttpEntity<>(Map.of("records", List.of()), jsonHeaders()),
                    Map.class
            );
        } catch (RestClientException exc) {
            logger.debug("teams ingest notify failed: {}", exc.getMessage());
        }
    }

    private void pushUsers() {
        try {
            restTemplate.exchange(
                    aiServiceBaseUrl + "/api/rag/ingest/users",
                    HttpMethod.POST,
                    new HttpEntity<>(Map.of("records", List.of()), jsonHeaders()),
                    Map.class
            );
        } catch (RestClientException exc) {
            logger.debug("users ingest notify failed: {}", exc.getMessage());
        }
    }

    private void pushOne(String source, UUID teamId, List<Map<String, Object>> records) {
        if (records.isEmpty()) return;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("records", records);
        body.put("team_id", teamId == null ? null : teamId.toString());
        body.put("replace", false);
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    aiServiceBaseUrl + "/api/rag/ingest/" + source,
                    HttpMethod.POST,
                    new HttpEntity<>(body, jsonHeaders()),
                    Map.class
            );
            logger.info("Pushed {} record(s) to AI /api/rag/ingest/{} -> status={}", records.size(), source, resp.getStatusCode());
        } catch (RestClientException exc) {
            logger.warn("AI ingest push for source={} failed: {}", source, exc.getMessage());
        }
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("User-Agent", "ORCA-Backend/1.0");
        return h;
    }
}
