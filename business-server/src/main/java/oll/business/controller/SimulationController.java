package oll.business.controller;

import jakarta.validation.Valid;
import oll.business.dto.SimulationRequest;
import oll.business.dto.SimulationResponse;
import oll.business.service.LogService;
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
    private final LogService logService;

    public SimulationController(SimulationService simulationService, LogService logService) {
        this.simulationService = simulationService;
        this.logService = logService;
    }

    @PostMapping("/run")
    public ResponseEntity<SimulationResponse> runSimulation(@Valid @RequestBody SimulationRequest request) {
        logService.logInfo("Running simulation for model id=" + request.getModelId(), "SimulationController", "runSimulation");
        try {
            SimulationResponse response = simulationService.runSimulation(request);
            logService.logInfo("Simulation completed for model id=" + request.getModelId(), "SimulationController", "runSimulation");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logService.logError("Simulation failed for model id=" + request.getModelId() + ": " + e.getMessage(), "SimulationController", "runSimulation");
            return ResponseEntity.internalServerError().build();
        }
    }
}
