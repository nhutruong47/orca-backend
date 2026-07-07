package org.example.backend.service;

import org.example.backend.dto.*;
import org.example.backend.entity.Cost;
import org.example.backend.entity.CostCategory;
import org.example.backend.entity.User;
import org.example.backend.repository.CostCategoryRepository;
import org.example.backend.repository.CostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

@Service
public class CostService {

    private final CostRepository costRepository;
    private final CostCategoryRepository costCategoryRepository;

    public CostService(CostRepository costRepository, CostCategoryRepository costCategoryRepository) {
        this.costRepository = costRepository;
        this.costCategoryRepository = costCategoryRepository;
    }

    @PostConstruct
    public void seedCostData() {
        if (costCategoryRepository.count() == 0) {
            String[] categories = {"Marketing", "Server", "Cloud", "AI API", "Google", "AWS", "Azure", "Vercel", "Domain", "Email", "SMS", "Lương", "Thưởng", "Thuê văn phòng", "Thiết bị", "Phần mềm", "Khác"};
            List<CostCategory> savedCategories = new ArrayList<>();
            for (String catName : categories) {
                CostCategory cat = new CostCategory();
                cat.setName(catName);
                cat.setDescription("Danh mục " + catName);
                cat.setStatus("ACTIVE");
                savedCategories.add(costCategoryRepository.save(cat));
            }

            // Seed some costs
            Random random = new Random();
            for (int i = 0; i < 50; i++) {
                Cost cost = new Cost();
                CostCategory cat = savedCategories.get(random.nextInt(savedCategories.size()));
                cost.setName("Chi phí " + cat.getName() + " #" + (i + 1));
                cost.setCategory(cat);
                
                // Random amount between 1,000,000 and 50,000,000
                long amount = 1000000L + (long)(random.nextDouble() * 49000000L);
                cost.setAmount(new BigDecimal(amount));
                cost.setCurrency("VND");
                
                // Random date in the last 6 months
                LocalDateTime now = LocalDateTime.now();
                long daysToSubtract = random.nextInt(180);
                cost.setDate(now.minusDays(daysToSubtract));
                
                cost.setPayer("Admin");
                cost.setDescription("Thanh toán cho " + cat.getName());
                
                String[] statuses = {"PAID", "PAID", "PAID", "PENDING", "CANCELLED"};
                cost.setStatus(statuses[random.nextInt(statuses.length)]);
                
                cost.setCreatedBy("system");
                costRepository.save(cost);
            }
        }
    }

    // --- Category Management ---

    @Transactional(readOnly = true)
    public List<CostCategoryDto> getAllCategories() {
        return costCategoryRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toCategoryDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CostCategoryDto createCategory(CostCategoryRequest request) {
        if (costCategoryRepository.findByName(request.getName()).isPresent()) {
            throw new RuntimeException("Category name already exists");
        }
        CostCategory category = new CostCategory();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        return toCategoryDto(costCategoryRepository.save(category));
    }

    @Transactional
    public CostCategoryDto updateCategory(UUID id, CostCategoryRequest request) {
        CostCategory category = costCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        Optional<CostCategory> existing = costCategoryRepository.findByName(request.getName());
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new RuntimeException("Category name already exists");
        }
        
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            category.setStatus(request.getStatus());
        }
        return toCategoryDto(costCategoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(UUID id) {
        costCategoryRepository.deleteById(id);
    }

    // --- Cost Management ---

    @Transactional(readOnly = true)
    public Page<CostDto> searchCosts(String search, UUID categoryId, String status, Pageable pageable) {
        return costRepository.searchCosts(search, categoryId, status, pageable)
                .map(this::toCostDto);
    }

    @Transactional(readOnly = true)
    public CostDto getCostById(UUID id) {
        return costRepository.findById(id)
                .map(this::toCostDto)
                .orElseThrow(() -> new RuntimeException("Cost not found"));
    }

    @Transactional
    public CostDto createCost(CostRequest request, User user) {
        Cost cost = new Cost();
        updateCostFromRequest(cost, request);
        cost.setCreatedBy(user.getUsername());
        return toCostDto(costRepository.save(cost));
    }

    @Transactional
    public CostDto updateCost(UUID id, CostRequest request, User user) {
        Cost cost = costRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cost not found"));
        updateCostFromRequest(cost, request);
        cost.setUpdatedBy(user.getUsername());
        return toCostDto(costRepository.save(cost));
    }

    @Transactional
    public void deleteCost(UUID id) {
        costRepository.deleteById(id);
    }

    private void updateCostFromRequest(Cost cost, CostRequest request) {
        cost.setName(request.getName());
        
        if (request.getCategoryId() != null) {
            CostCategory category = costCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            cost.setCategory(category);
        } else {
            cost.setCategory(null);
        }

        cost.setAmount(request.getAmount());
        cost.setCurrency(request.getCurrency() != null ? request.getCurrency() : "VND");
        cost.setDate(request.getDate() != null ? request.getDate() : LocalDateTime.now());
        cost.setPayer(request.getPayer());
        cost.setDescription(request.getDescription());
        cost.setInvoiceUrl(request.getInvoiceUrl());
        cost.setStatus(request.getStatus() != null ? request.getStatus() : "PENDING");
    }

    // --- Statistics ---

    @Transactional(readOnly = true)
    public CostDashboardStats getDashboardStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime monthStart = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime previousMonthStart = monthStart.minusMonths(1);
        LocalDateTime yearStart = now.withDayOfYear(1).toLocalDate().atStartOfDay();
        LocalDateTime previousYearStart = yearStart.minusYears(1);

        List<Cost> allCosts = costRepository.findAll();
        
        BigDecimal totalSystem = BigDecimal.ZERO;
        BigDecimal totalToday = BigDecimal.ZERO;
        BigDecimal totalMonth = BigDecimal.ZERO;
        BigDecimal totalPreviousMonth = BigDecimal.ZERO;
        BigDecimal totalYear = BigDecimal.ZERO;
        BigDecimal totalPreviousYear = BigDecimal.ZERO;

        for (Cost c : allCosts) {
            if ("CANCELLED".equals(c.getStatus())) continue;
            BigDecimal amt = c.getAmount() != null ? c.getAmount() : BigDecimal.ZERO;
            
            totalSystem = totalSystem.add(amt);
            
            LocalDateTime dt = c.getDate() != null ? c.getDate() : c.getCreatedAt();
            if (dt != null) {
                if (!dt.isBefore(todayStart)) totalToday = totalToday.add(amt);
                if (!dt.isBefore(monthStart)) totalMonth = totalMonth.add(amt);
                if (!dt.isBefore(previousMonthStart) && dt.isBefore(monthStart)) totalPreviousMonth = totalPreviousMonth.add(amt);
                if (!dt.isBefore(yearStart)) totalYear = totalYear.add(amt);
                if (!dt.isBefore(previousYearStart) && dt.isBefore(yearStart)) totalPreviousYear = totalPreviousYear.add(amt);
            }
        }

        CostDashboardStats stats = new CostDashboardStats();
        stats.setTotalSystem(totalSystem);
        stats.setTotalToday(totalToday);
        stats.setTotalMonth(totalMonth);
        stats.setTotalYear(totalYear);

        BigDecimal mom = calculatePercentageChange(totalPreviousMonth, totalMonth);
        BigDecimal yoy = calculatePercentageChange(totalPreviousYear, totalYear);
        stats.setMonthOverMonthChange(mom);
        stats.setYearOverYearChange(yoy);

        // Daily chart (last 30 days)
        stats.setDailyChart(buildDailyChart(allCosts, now.minusDays(30), now));
        
        // Monthly chart (last 12 months)
        stats.setMonthlyChart(buildMonthlyChart(allCosts, now.minusMonths(11).withDayOfMonth(1).toLocalDate().atStartOfDay(), now));
        
        // Category chart
        stats.setCategoryChart(buildCategoryChart(allCosts));

        return stats;
    }

    private BigDecimal calculatePercentageChange(BigDecimal oldVal, BigDecimal newVal) {
        if (oldVal.compareTo(BigDecimal.ZERO) == 0) {
            return newVal.compareTo(BigDecimal.ZERO) > 0 ? new BigDecimal("100") : BigDecimal.ZERO;
        }
        return newVal.subtract(oldVal).divide(oldVal, 4, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
    }

    private List<Map<String, Object>> buildDailyChart(List<Cost> costs, LocalDateTime start, LocalDateTime end) {
        Map<String, BigDecimal> daily = new TreeMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        
        LocalDateTime curr = start;
        while (!curr.isAfter(end)) {
            daily.put(curr.format(fmt), BigDecimal.ZERO);
            curr = curr.plusDays(1);
        }

        for (Cost c : costs) {
            if ("CANCELLED".equals(c.getStatus())) continue;
            LocalDateTime dt = c.getDate() != null ? c.getDate() : c.getCreatedAt();
            if (dt != null && !dt.isBefore(start) && !dt.isAfter(end)) {
                String key = dt.format(fmt);
                if (daily.containsKey(key)) {
                    daily.put(key, daily.get(key).add(c.getAmount() != null ? c.getAmount() : BigDecimal.ZERO));
                }
            }
        }

        return daily.entrySet().stream().map(e -> {
            Map<String, Object> map = new HashMap<>();
            map.put("date", e.getKey());
            map.put("amount", e.getValue().divide(new BigDecimal("1000000"), 2, java.math.RoundingMode.HALF_UP)); // millions
            return map;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildMonthlyChart(List<Cost> costs, LocalDateTime start, LocalDateTime end) {
        Map<String, BigDecimal> monthly = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/yyyy");
        
        LocalDateTime curr = start;
        while (!curr.isAfter(end)) {
            monthly.put(curr.format(fmt), BigDecimal.ZERO);
            curr = curr.plusMonths(1);
        }

        for (Cost c : costs) {
            if ("CANCELLED".equals(c.getStatus())) continue;
            LocalDateTime dt = c.getDate() != null ? c.getDate() : c.getCreatedAt();
            if (dt != null && !dt.isBefore(start) && dt.isBefore(end.plusMonths(1).withDayOfMonth(1).toLocalDate().atStartOfDay())) {
                String key = dt.format(fmt);
                if (monthly.containsKey(key)) {
                    monthly.put(key, monthly.get(key).add(c.getAmount() != null ? c.getAmount() : BigDecimal.ZERO));
                }
            }
        }

        return monthly.entrySet().stream().map(e -> {
            Map<String, Object> map = new HashMap<>();
            map.put("month", e.getKey());
            map.put("amount", e.getValue().divide(new BigDecimal("1000000"), 2, java.math.RoundingMode.HALF_UP));
            return map;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildCategoryChart(List<Cost> costs) {
        Map<String, BigDecimal> catMap = new HashMap<>();
        for (Cost c : costs) {
            if ("CANCELLED".equals(c.getStatus())) continue;
            String catName = c.getCategory() != null ? c.getCategory().getName() : "Khác";
            catMap.put(catName, catMap.getOrDefault(catName, BigDecimal.ZERO).add(c.getAmount() != null ? c.getAmount() : BigDecimal.ZERO));
        }

        return catMap.entrySet().stream()
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", e.getKey());
                    map.put("value", e.getValue().divide(new BigDecimal("1000000"), 2, java.math.RoundingMode.HALF_UP));
                    return map;
                })
                .sorted((a, b) -> ((BigDecimal) b.get("value")).compareTo((BigDecimal) a.get("value")))
                .collect(Collectors.toList());
    }

    // --- Mappers ---

    private CostCategoryDto toCategoryDto(CostCategory category) {
        CostCategoryDto dto = new CostCategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setStatus(category.getStatus());
        dto.setCreatedAt(category.getCreatedAt());
        return dto;
    }

    private CostDto toCostDto(Cost cost) {
        CostDto dto = new CostDto();
        dto.setId(cost.getId());
        dto.setName(cost.getName());
        if (cost.getCategory() != null) {
            dto.setCategory(toCategoryDto(cost.getCategory()));
        }
        dto.setAmount(cost.getAmount());
        dto.setCurrency(cost.getCurrency());
        dto.setDate(cost.getDate());
        dto.setPayer(cost.getPayer());
        dto.setDescription(cost.getDescription());
        dto.setInvoiceUrl(cost.getInvoiceUrl());
        dto.setStatus(cost.getStatus());
        dto.setCreatedAt(cost.getCreatedAt());
        dto.setCreatedBy(cost.getCreatedBy());
        return dto;
    }
}
