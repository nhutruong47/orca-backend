package org.example.backend.dto;

public record PlanUsageDTO(
        String planId,
        String planName,
        long usersUsed,
        int maxUsers,
        long workshopsUsed,
        int maxWorkshops,
        boolean canAddMember,
        boolean canCreateWorkshop) {
}
