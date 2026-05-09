package oll.business.controller;

import jakarta.validation.Valid;
import oll.business.dto.SimulationRequest;
import oll.business.dto.SimulationResponse;
import oll.business.service.SimulationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/simulations")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('ANALYST','ADMIN')")
@Validated
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostMapping("/run")
    public ResponseEntity<SimulationResponse> runSimulation(@Valid @RequestBody SimulationRequest request) {
        try {
            SimulationResponse response = simulationService.runSimulation(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
