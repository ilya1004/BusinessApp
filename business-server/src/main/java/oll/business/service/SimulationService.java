package oll.business.service;

import oll.business.dto.*;
import oll.business.model.ProcessModel;
import oll.business.model.TaskDefinition;
import oll.business.repository.ProcessModelRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class SimulationService {

    private final ProcessModelRepository modelRepository;

    public SimulationService(ProcessModelRepository modelRepository) {
        this.modelRepository = modelRepository;
    }

    /**
     * Runs an in-memory simulation of a process model with given scenario parameters.
     * No database writes - purely virtual execution.
     *
     * Algorithm (3.5.4):
     * 1. Clone model task definitions
     * 2. Apply duration multiplier, resources, parallelism
     * 3. Simulate virtual execution timeline
     * 4. Collect metrics: cycle time, cost, resource load
     */
    public SimulationResponse runSimulation(SimulationRequest request) {
        // Step 1: Load and clone model
        ProcessModel model = modelRepository.findById(request.getModelId())
                .orElseThrow(() -> new RuntimeException("Model not found: " + request.getModelId()));

        // Deep clone task definitions for in-memory manipulation
        List<SimTask> simTasks = new ArrayList<>();
        for (TaskDefinition td : model.getTaskDefinitions()) {
            SimTask sim = new SimTask();
            sim.elementId = td.getBpmnElementId();
            sim.name = td.getName();
            sim.baseDuration = td.getDefaultDuration().doubleValue();
            sim.baseCost = td.getExpectedCost().doubleValue();
            simTasks.add(sim);
        }

        if (simTasks.isEmpty()) {
            SimulationResponse empty = new SimulationResponse();
            empty.setModelId(model.getId());
            empty.setModelName(model.getName());
            empty.setCycleTime(new KpiPair(0.0, 0.0));
            empty.setTotalCost(new KpiPair(0.0, 0.0));
            empty.setResourceLoad(new KpiPair(0.0, 0.0));
            empty.setTaskPredictions(List.of());
            return empty;
        }

        double durationMultiplier = request.getDurationMultiplier();
        int resourcesPerTask = request.getResourcesPerTask();
        int parallelismFactor = request.getParallelismFactor();

        // Step 2: Apply scenario multipliers to each task
        double totalBaseDuration = 0;
        double totalScenarioDuration = 0;
        double totalBaseCost = 0;
        double totalScenarioCost = 0;

        List<TaskPrediction> predictions = new ArrayList<>();

        for (SimTask sim : simTasks) {
            // Scenario duration scales by multiplier and inversely by resources
            double scenarioDur = sim.baseDuration * durationMultiplier / resourcesPerTask;
            // Scenario cost increases with more resources (resource cost premium)
            double scenarioCost = sim.baseCost * Math.sqrt(resourcesPerTask);

            sim.scenarioDuration = scenarioDur;
            sim.scenarioCost = scenarioCost;

            totalBaseDuration += sim.baseDuration;
            totalScenarioDuration += scenarioDur;
            totalBaseCost += sim.baseCost;
            totalScenarioCost += scenarioCost;

            TaskPrediction tp = new TaskPrediction();
            tp.setTaskName(sim.name);
            tp.setElementId(sim.elementId);
            tp.setBaseDuration(Math.round(sim.baseDuration * 10.0) / 10.0);
            tp.setScenarioDuration(Math.round(scenarioDur * 10.0) / 10.0);
            tp.setBaseCost(Math.round(sim.baseCost * 100.0) / 100.0);
            tp.setScenarioCost(Math.round(scenarioCost * 100.0) / 100.0);
            predictions.add(tp);
        }

        // Step 3: Simulate cycle time with parallelism
        // Baseline: sequential execution (no parallelism assumed)
        double baseCycleTime = totalBaseDuration;

        // Scenario: tasks can run in parallel up to parallelismFactor
        // Approximation: sort by duration, pack into parallelismFactor lanes
        double scenarioCycleTime = simulateParallelCycleTime(simTasks, parallelismFactor);

        // Step 4: Calculate resource load percentage
        // Load = total scenario work / (parallelismFactor * cycleTime) * 100
        double baseLoad = totalBaseDuration > 0 ?
                Math.min(100.0, (totalBaseDuration / baseCycleTime) * 100.0) : 0.0;
        double scenarioLoad = scenarioCycleTime > 0 ?
                Math.min(100.0, (totalScenarioDuration / (parallelismFactor * scenarioCycleTime)) * 100.0) : 0.0;

        // Assign resource load per task
        for (int i = 0; i < predictions.size(); i++) {
            SimTask sim = simTasks.get(i);
            TaskPrediction tp = predictions.get(i);
            // Individual task load: scenario duration relative to its share of cycle time
            double taskLoad = scenarioCycleTime > 0 ?
                    (sim.scenarioDuration / scenarioCycleTime) * 100.0 : 0.0;
            tp.setResourceLoadPercent(Math.round(Math.min(100.0, taskLoad) * 10.0) / 10.0);
        }

        // Build response
        SimulationResponse response = new SimulationResponse();
        response.setModelId(model.getId());
        response.setModelName(model.getName());
        response.setCycleTime(new KpiPair(
                Math.round(baseCycleTime * 10.0) / 10.0,
                Math.round(scenarioCycleTime * 10.0) / 10.0
        ));
        response.setTotalCost(new KpiPair(
                Math.round(totalBaseCost * 100.0) / 100.0,
                Math.round(totalScenarioCost * 100.0) / 100.0
        ));
        response.setResourceLoad(new KpiPair(
                Math.round(baseLoad * 10.0) / 10.0,
                Math.round(scenarioLoad * 10.0) / 10.0
        ));
        response.setTaskPredictions(predictions);

        return response;
    }

    /**
     * Simulates parallel execution by distributing tasks across parallelismFactor lanes.
     * Uses a greedy bin-packing approach: assigns each task to the lane with the earliest finish time.
     */
    private double simulateParallelCycleTime(List<SimTask> tasks, int parallelismFactor) {
        if (parallelismFactor <= 0) parallelismFactor = 1;

        double[] laneFinishTimes = new double[parallelismFactor];

        // Sort tasks by scenario duration descending for better packing
        List<SimTask> sorted = new ArrayList<>(tasks);
        sorted.sort(Comparator.comparingDouble((SimTask s) -> s.scenarioDuration).reversed());

        for (SimTask task : sorted) {
            // Find the lane that finishes earliest
            int bestLane = 0;
            for (int l = 1; l < parallelismFactor; l++) {
                if (laneFinishTimes[l] < laneFinishTimes[bestLane]) {
                    bestLane = l;
                }
            }
            laneFinishTimes[bestLane] += task.scenarioDuration;
        }

        // Cycle time = max of all lane finish times
        double maxTime = 0;
        for (double t : laneFinishTimes) {
            if (t > maxTime) maxTime = t;
        }
        return maxTime;
    }

    // In-memory mutable copy of a task for simulation
    private static class SimTask {
        String elementId;
        String name;
        double baseDuration;
        double baseCost;
        double scenarioDuration;
        double scenarioCost;
    }
}
