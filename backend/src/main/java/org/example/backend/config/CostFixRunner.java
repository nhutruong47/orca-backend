package org.example.backend.config;

import org.example.backend.entity.Cost;
import org.example.backend.repository.CostRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class CostFixRunner implements CommandLineRunner {

    private final CostRepository costRepository;

    public CostFixRunner(CostRepository costRepository) {
        this.costRepository = costRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        List<Cost> costs = costRepository.findAll();
        for (Cost cost : costs) {
            // Đặt số tiền nhỏ hơn nữa (từ 5,000 đến 50,000) để tổng cộng lại chỉ khoảng 1 triệu
            long randomAmount = 5000L + (long)(Math.random() * 45000L);
            cost.setAmount(new BigDecimal(randomAmount));
        }
        costRepository.saveAll(costs);
        System.out.println("CostFixRunner: Đã giảm số tiền của các khoản chi phí lớn.");
    }
}
