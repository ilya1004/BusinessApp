package oll.business.controller;

import oll.business.dto.KpiInstanceDto;
import oll.business.dto.KpiModelDto;
import oll.business.dto.KpiUserStatsDto;
import oll.business.model.User;
import oll.business.repository.UserRepository;
import oll.business.service.KpiService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kpi")
@CrossOrigin(origins = "*")
public class KpiController {

    private final KpiService kpiService;
    private final UserRepository userRepository;

    public KpiController(KpiService kpiService, UserRepository userRepository) {
        this.kpiService = kpiService;
        this.userRepository = userRepository;
    }

    @GetMapping("/models/{id}")
    public KpiModelDto getModelKpi(@PathVariable Long id) {
        return kpiService.getModelKpi(id);
    }

    @GetMapping("/instances/{id}")
    public KpiInstanceDto getInstanceKpi(@PathVariable Long id) {
        return kpiService.getInstanceKpi(id);
    }

    @GetMapping("/users/{id}")
    public KpiUserStatsDto getUserKpi(@PathVariable Long id) {
        return kpiService.getUserKpi(id);
    }

    @GetMapping("/users/me")
    public KpiUserStatsDto getCurrentUserKpi(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return kpiService.getUserKpi(user.getId());
    }
}
