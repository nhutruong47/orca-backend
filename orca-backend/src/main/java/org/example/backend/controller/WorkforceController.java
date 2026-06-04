package org.example.backend.controller;

import org.example.backend.entity.Skill;
import org.example.backend.entity.Team;
import org.example.backend.entity.WorkerSkill;
import org.example.backend.repository.SkillRepository;
import org.example.backend.repository.TeamMemberRepository;
import org.example.backend.repository.TeamRepository;
import org.example.backend.repository.WorkerSkillRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/workforce")
public class WorkforceController {

    private final SkillRepository skillRepo;
    private final WorkerSkillRepository workerSkillRepo;
    private final TeamRepository teamRepo;
    private final TeamMemberRepository teamMemberRepo;

    public WorkforceController(SkillRepository skillRepo,
            WorkerSkillRepository workerSkillRepo,
            TeamRepository teamRepo,
            TeamMemberRepository teamMemberRepo) {
        this.skillRepo = skillRepo;
        this.workerSkillRepo = workerSkillRepo;
        this.teamRepo = teamRepo;
        this.teamMemberRepo = teamMemberRepo;
    }

    @GetMapping("/teams/{teamId}/skills")
    public ResponseEntity<?> getSkills(@PathVariable UUID teamId) {
        return ResponseEntity.ok(skillRepo.findByTeamIdOrderByNameAsc(teamId));
    }

    @PostMapping("/teams/{teamId}/skills")
    public ResponseEntity<?> createSkill(@PathVariable UUID teamId, @RequestBody Skill skill) {
        Optional<Team> team = teamRepo.findById(teamId);
        if (team.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Team not found"));
        }
        skill.setTeam(team.get());
        return ResponseEntity.ok(skillRepo.save(skill));
    }

    @GetMapping("/teams/{teamId}/skill-matrix")
    public ResponseEntity<?> getSkillMatrix(@PathVariable UUID teamId) {
        var skills = skillRepo.findByTeamIdOrderByNameAsc(teamId);
        var members = teamMemberRepo.findByTeamId(teamId);
        var workerSkills = workerSkillRepo.findByTeamMemberTeamId(teamId);
        Map<String, Integer> levels = new HashMap<>();
        workerSkills.forEach(ws -> levels.put(ws.getTeamMember().getId() + ":" + ws.getSkill().getId(), ws.getLevel()));

        return ResponseEntity.ok(members.stream().map(member -> {
            Map<String, Object> row = new HashMap<>();
            row.put("teamMemberId", member.getId().toString());
            row.put("userId", member.getUser().getId().toString());
            row.put("name", member.getUser().getFullName() != null ? member.getUser().getFullName() : member.getUser().getUsername());
            row.put("skills", skills.stream().map(skill -> {
                Map<String, Object> s = new HashMap<>();
                s.put("skillId", skill.getId().toString());
                s.put("name", skill.getName());
                s.put("level", levels.getOrDefault(member.getId() + ":" + skill.getId(), 0));
                return s;
            }).collect(Collectors.toList()));
            return row;
        }).collect(Collectors.toList()));
    }

    @PutMapping("/members/{teamMemberId}/skills/{skillId}")
    public ResponseEntity<?> setWorkerSkill(@PathVariable UUID teamMemberId,
            @PathVariable UUID skillId,
            @RequestBody Map<String, Integer> body) {
        var member = teamMemberRepo.findById(teamMemberId).orElse(null);
        var skill = skillRepo.findById(skillId).orElse(null);
        if (member == null || skill == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Member or skill not found"));
        }
        WorkerSkill workerSkill = workerSkillRepo.findByTeamMemberIdAndSkillId(teamMemberId, skillId).orElseGet(WorkerSkill::new);
        workerSkill.setTeamMember(member);
        workerSkill.setSkill(skill);
        workerSkill.setLevel(body.getOrDefault("level", 1));
        return ResponseEntity.ok(workerSkillRepo.save(workerSkill));
    }
}
