package oll.business.controller;

import oll.business.dto.KpiWeightsDto;
import oll.business.service.KpiService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings/kpi-weights")
@CrossOrigin(origins = "*")
public class KpiSettingsController {

    private final KpiService kpiService;

    public KpiSettingsController(KpiService kpiService) {
        this.kpiService = kpiService;
    }

    @GetMapping
    public KpiWeightsDto getWeights(@RequestParam(required = false) Long modelId) {
        return kpiService.getWeights(modelId);
    }

    @PutMapping
    public KpiWeightsDto saveWeights(@RequestBody KpiWeightsDto request) {
        return kpiService.saveWeights(request);
    }
}
