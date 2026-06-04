package org.example.backend.repository;

import org.example.backend.entity.Machine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MachineRepository extends JpaRepository<Machine, UUID> {
    List<Machine> findByTeamIdOrderByNameAsc(UUID teamId);
}
