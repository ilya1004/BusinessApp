package oll.business.service;

import oll.business.dto.*;
import oll.business.model.*;
import oll.business.model.ProcessInstance.ProcessStatus;
import oll.business.model.Task.TaskStatus;
import oll.business.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KpiService {

    private static final BigDecimal MAX_COST_RATE = new BigDecimal("2.0");
    private static final BigDecimal DEFAULT_WEIGHT = new BigDecimal("0.34");

    private final ProcessInstanceRepository instanceRepository;
    private final TaskRepository taskRepository;
    private final ProcessModelRepository modelRepository;
    private final TaskDefinitionRepository taskDefRepository;
    private final KpiWeightsRepository kpiWeightsRepository;
    private final UserRepository userRepository;
    private final LogService logService;

    public KpiService(ProcessInstanceRepository instanceRepository,
                      TaskRepository taskRepository,
                      ProcessModelRepository modelRepository,
                      TaskDefinitionRepository taskDefRepository,
                      KpiWeightsRepository kpiWeightsRepository,
                      UserRepository userRepository,
                      LogService logService) {
        this.instanceRepository = instanceRepository;
        this.taskRepository = taskRepository;
        this.modelRepository = modelRepository;
        this.taskDefRepository = taskDefRepository;
        this.kpiWeightsRepository = kpiWeightsRepository;
        this.userRepository = userRepository;
        this.logService = logService;
    }

    public KpiModelDto getModelKpi(Long modelId) {
        ProcessModel model = modelRepository.findById(modelId)
                .orElseThrow(() -> new RuntimeException("Model not found: " + modelId));

        List<ProcessInstance> completed = instanceRepository.findByModelId(modelId).stream()
                .filter(inst -> inst.getStatus() == ProcessStatus.COMPLETED)
                .collect(Collectors.toList());

        List<ProcessInstance> all = instanceRepository.findByModelId(modelId);

        KpiWeights weights = getWeightsForModel(modelId);

        double avgDuration = calcAvgDuration(completed);
        double delayRate = calcDelayRate(completed);
        double rating = calcRating(model, weights, completed, delayRate);

        KpiModelDto dto = new KpiModelDto();
        dto.setModelId(modelId);
        dto.setModelName(model.getName());
        dto.setAvgDuration(round(avgDuration, 1));
        dto.setDelayRate(round(delayRate, 3));
        dto.setRating(round(rating, 3));
        dto.setTotalInstances(all.size());
        dto.setCompletedInstances(completed.size());

        List<TaskDefinition> defs = taskDefRepository.findByModelId(modelId);
        List<KpiModelDto.KpiTaskDto> taskDtos = defs.stream().map(td -> {
            KpiModelDto.KpiTaskDto t = new KpiModelDto.KpiTaskDto();
            t.setElementId(td.getBpmnElementId());
            t.setName(td.getName());
            t.setKpiWeight(td.getKpiWeight());
            return t;
        }).collect(Collectors.toList());
        dto.setTasks(taskDtos);

        return dto;
    }

    public KpiInstanceDto getInstanceKpi(Long instanceId) {
        ProcessInstance inst = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new RuntimeException("Instance not found: " + instanceId));

        KpiWeights weights = getWeightsForModel(inst.getModel().getId());

        List<Task> tasks = taskRepository.findByInstanceId(instanceId);

        int totalPlanned = 0;
        int totalActual = 0;
        List<KpiInstanceDto.KpiInstanceTaskDto> taskDtos = new ArrayList<>();

        for (Task t : tasks) {
            TaskDefinition def = t.getTaskDefinition();
            int planned = t.getPlannedDuration() != null ? t.getPlannedDuration() : 0;
            int actual = t.getActualDuration() != null ? t.getActualDuration() : 0;

            totalPlanned += planned;
            if (t.getStatus() == TaskStatus.COMPLETED) {
                totalActual += actual;
            }

            double deviation = planned > 0 ? ((double) (actual - planned) / planned) * 100.0 : 0.0;

            KpiInstanceDto.KpiInstanceTaskDto td = new KpiInstanceDto.KpiInstanceTaskDto();
            td.setTaskId(t.getId());
            td.setElementId(def.getBpmnElementId());
            td.setTaskName(def.getName());
            td.setStatus(t.getStatus().name());
            td.setPlannedDuration(planned);
            td.setActualDuration(actual);
            td.setDeviationPercent(round(deviation, 1));
            td.setKpiWeight(def.getKpiWeight());
            taskDtos.add(td);
        }

        double devPercent = totalPlanned > 0 ? ((double) (totalActual - totalPlanned) / totalPlanned) * 100.0 : 0.0;

        KpiInstanceDto dto = new KpiInstanceDto();
        dto.setInstanceId(instanceId);
        dto.setModelId(inst.getModel().getName());
        dto.setStatus(inst.getStatus().name());
        dto.setStartedAt(inst.getStartedAt());
        dto.setFinishedAt(inst.getFinishedAt());
        dto.setPlannedDuration(totalPlanned);
        dto.setActualDuration(totalActual);
        dto.setDeviationPercent(round(devPercent, 1));
        dto.setTasks(taskDtos);

        return dto;
    }

    public KpiUserStatsDto getUserKpi(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        List<Task> userTasks = taskRepository.findByAssigneeId(userId);
        List<Task> completed = userTasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                .collect(Collectors.toList());

        KpiWeights globalWeights = kpiWeightsRepository.findByModelIdIsNull().stream()
                .findFirst()
                .orElse(KpiWeights.defaultWeights());

        double rating = calcUserRating(globalWeights, completed, userTasks);

        LocalDate weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        long weeklyCompleted = completed.stream()
                .filter(t -> t.getCompletedAt() != null &&
                        !t.getCompletedAt().toLocalDate().isBefore(weekStart) &&
                        !t.getCompletedAt().toLocalDate().isAfter(weekEnd))
                .count();

        int totalActive = userTasks.stream()
                .filter(t -> t.getStatus() != TaskStatus.COMPLETED && t.getStatus() != TaskStatus.CANCELLED)
                .mapToInt(t -> t.getPlannedDuration() != null ? t.getPlannedDuration() : 0)
                .sum();
        int totalCompleted = completed.stream()
                .mapToInt(t -> t.getActualDuration() != null ? t.getActualDuration() : 0)
                .sum();
        double loadPercent = (totalActive + totalCompleted) > 0 ?
                ((double) totalCompleted / (totalActive + totalCompleted)) * 100.0 : 0.0;

        List<KpiUserStatsDto.RatingHistoryPoint> history = buildRatingHistory(userId, completed);

        KpiUserStatsDto dto = new KpiUserStatsDto();
        dto.setUserId(userId);
        dto.setUsername(user.getUsername());
        dto.setRating(round(rating, 3));
        dto.setWeeklyCompleted((int) weeklyCompleted);
        dto.setLoadPercent(round(loadPercent, 1));
        dto.setRatingHistory(history);

        return dto;
    }

    public KpiWeightsDto getWeights(Long modelId) {
        KpiWeights w = getWeightsForModel(modelId);
        KpiWeightsDto dto = new KpiWeightsDto();
        dto.setModelId(modelId);
        dto.setW1(w.getW1());
        dto.setW2(w.getW2());
        dto.setW3(w.getW3());
        return dto;
    }

    @Transactional
    public KpiWeightsDto saveWeights(KpiWeightsDto request) {
        BigDecimal sum = request.getSum();
        if (sum.compareTo(BigDecimal.ONE) != 0) {
            throw new IllegalArgumentException("Weights must sum to 1.0, got " + sum);
        }

        KpiWeights w;
        if (request.getModelId() != null) {
            w = kpiWeightsRepository.findByModelId(request.getModelId())
                    .orElse(new KpiWeights());
            if (w.getId() == null) {
                ProcessModel model = modelRepository.findById(request.getModelId())
                        .orElseThrow(() -> new RuntimeException("Model not found: " + request.getModelId()));
                w.setModel(model);
            }
        } else {
            w = kpiWeightsRepository.findByModelIdIsNull().stream()
                    .findFirst()
                    .orElse(new KpiWeights());
        }

        w.setW1(request.getW1());
        w.setW2(request.getW2());
        w.setW3(request.getW3());
        w.setUpdatedAt(LocalDateTime.now());

        kpiWeightsRepository.save(w);

        recalculateModelWeights(request.getModelId());

        logService.logInfo("KPI weights saved for modelId=" + request.getModelId()
                + " (w1=" + request.getW1() + ", w2=" + request.getW2() + ", w3=" + request.getW3() + ")",
                "KpiService", "saveWeights");

        KpiWeightsDto dto = new KpiWeightsDto();
        dto.setModelId(request.getModelId());
        dto.setW1(w.getW1());
        dto.setW2(w.getW2());
        dto.setW3(w.getW3());
        return dto;
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void scheduledRecalculate() {
        logService.logInfo("Starting scheduled KPI recalculation", "KpiService", "scheduledRecalculate");
        List<ProcessModel> models = modelRepository.findAll();
        for (ProcessModel m : models) {
            try {
                recalculateModelWeights(m.getId());
            } catch (Exception e) {
                logService.logError("KPI recalculation failed for model " + m.getId() + ": " + e.getMessage(), "KpiService", "scheduledRecalculate");
            }
        }
        logService.logInfo("KPI recalculation complete", "KpiService", "scheduledRecalculate");
    }

    @Transactional
    public void recalculateModelWeights(Long modelId) {
        List<TaskDefinition> defs = taskDefRepository.findByModelId(modelId);
        if (defs.isEmpty()) return;

        KpiWeights weights = getWeightsForModel(modelId);
        List<ProcessInstance> completed = instanceRepository.findByModelId(modelId).stream()
                .filter(i -> i.getStatus() == ProcessStatus.COMPLETED)
                .collect(Collectors.toList());

        for (TaskDefinition def : defs) {
            double efficiency = calcTaskEfficiency(def, completed);
            BigDecimal weight = BigDecimal.valueOf(efficiency)
                    .setScale(2, RoundingMode.HALF_UP);
            def.setKpiWeight(weight);
            taskDefRepository.save(def);
        }
    }

    private KpiWeights getWeightsForModel(Long modelId) {
        if (modelId != null) {
            Optional<KpiWeights> modelWeights = kpiWeightsRepository.findByModelId(modelId);
            if (modelWeights.isPresent()) return modelWeights.get();
        }
        return kpiWeightsRepository.findByModelIdIsNull().stream()
                .findFirst()
                .orElse(KpiWeights.defaultWeights());
    }

    private double calcAvgDuration(List<ProcessInstance> completed) {
        if (completed.isEmpty()) return 0.0;
        return completed.stream()
                .filter(i -> i.getStartedAt() != null && i.getFinishedAt() != null)
                .mapToLong(i -> java.time.Duration.between(i.getStartedAt(), i.getFinishedAt()).toMinutes())
                .average()
                .orElse(0.0);
    }

    private double calcDelayRate(List<ProcessInstance> completed) {
        if (completed.isEmpty()) return 0.0;
        List<Task> tasks = new ArrayList<>();
        for (ProcessInstance inst : completed) {
            tasks.addAll(taskRepository.findByInstanceId(inst.getId()));
        }
        if (tasks.isEmpty()) return 0.0;
        long delayed = tasks.stream()
                .filter(t -> t.getActualDuration() != null && t.getPlannedDuration() != null &&
                        t.getActualDuration() > t.getPlannedDuration())
                .count();
        return (double) delayed / tasks.size();
    }

    private double calcRating(ProcessModel model, KpiWeights weights, List<ProcessInstance> completed, double delayRate) {
        double delayScore = 1.0 - Math.min(delayRate, 1.0);

        double efficiency = 1.0;
        if (!completed.isEmpty()) {
            List<Task> allTasks = new ArrayList<>();
            for (ProcessInstance inst : completed) {
                allTasks.addAll(taskRepository.findByInstanceId(inst.getId()));
            }
            if (!allTasks.isEmpty()) {
                double totalDeviation = 0;
                int count = 0;
                for (Task t : allTasks) {
                    if (t.getActualDuration() != null && t.getPlannedDuration() != null && t.getPlannedDuration() > 0) {
                        double ratio = (double) t.getActualDuration() / t.getPlannedDuration();
                        totalDeviation += Math.max(0, ratio - 1.0);
                        count++;
                    }
                }
                double avgOvershoot = count > 0 ? totalDeviation / count : 0;
                efficiency = 1.0 - Math.min(avgOvershoot, 1.0);
            }
        }

        double costRate = calcCostRate(completed);
        double costScore = 1.0 - Math.min(costRate, 1.0);

        BigDecimal w1 = weights.getW1() != null ? weights.getW1() : DEFAULT_WEIGHT;
        BigDecimal w2 = weights.getW2() != null ? weights.getW2() : DEFAULT_WEIGHT;
        BigDecimal w3 = weights.getW3() != null ? weights.getW3() : DEFAULT_WEIGHT;

        return w1.doubleValue() * delayScore +
               w2.doubleValue() * costScore +
               w3.doubleValue() * efficiency;
    }

    private double calcCostRate(List<ProcessInstance> completed) {
        if (completed.isEmpty()) return 0.0;
        BigDecimal totalPlannedCost = BigDecimal.ZERO;
        BigDecimal totalActualCost = BigDecimal.ZERO;

        for (ProcessInstance inst : completed) {
            List<Task> tasks = taskRepository.findByInstanceId(inst.getId());
            for (Task t : tasks) {
                TaskDefinition def = t.getTaskDefinition();
                if (def != null && def.getExpectedCost() != null) {
                    totalPlannedCost = totalPlannedCost.add(def.getExpectedCost());
                    if (t.getActualDuration() != null && t.getPlannedDuration() != null && t.getPlannedDuration() > 0) {
                        BigDecimal ratio = BigDecimal.valueOf(t.getActualDuration())
                                .divide(BigDecimal.valueOf(t.getPlannedDuration()), 4, RoundingMode.HALF_UP);
                        totalActualCost = totalActualCost.add(def.getExpectedCost().multiply(ratio));
                    } else {
                        totalActualCost = totalActualCost.add(def.getExpectedCost());
                    }
                }
            }
        }

        if (totalPlannedCost.compareTo(BigDecimal.ZERO) == 0) return 0.0;

        BigDecimal rate = totalActualCost.divide(totalPlannedCost, 4, RoundingMode.HALF_UP);
        return Math.min(rate.doubleValue(), MAX_COST_RATE.doubleValue());
    }

    private double calcTaskEfficiency(TaskDefinition def, List<ProcessInstance> completed) {
        if (completed.isEmpty()) return 1.0;

        int totalPlanned = 0;
        int totalActual = 0;
        int count = 0;

        for (ProcessInstance inst : completed) {
            List<Task> tasks = taskRepository.findByInstanceId(inst.getId());
            for (Task t : tasks) {
                if (t.getTaskDefinition().getId().equals(def.getId()) &&
                        t.getPlannedDuration() != null && t.getActualDuration() != null) {
                    totalPlanned += t.getPlannedDuration();
                    totalActual += t.getActualDuration();
                    count++;
                }
            }
        }

        if (count == 0 || totalPlanned == 0) return 1.0;
        double ratio = (double) totalActual / totalPlanned;
        return Math.max(0.0, Math.min(1.0, 1.0 - Math.max(0.0, ratio - 1.0)));
    }

    private double calcUserRating(KpiWeights weights, List<Task> completed, List<Task> allTasks) {
        if (completed.isEmpty()) return 0.0;

        long onTime = completed.stream()
                .filter(t -> t.getActualDuration() != null && t.getPlannedDuration() != null &&
                        t.getActualDuration() <= t.getPlannedDuration())
                .count();
        double efficiency = (double) onTime / completed.size();

        long delayed = completed.stream()
                .filter(t -> t.getActualDuration() != null && t.getPlannedDuration() != null &&
                        t.getActualDuration() > t.getPlannedDuration())
                .count();
        double delayRate = (double) delayed / completed.size();
        double delayScore = 1.0 - delayRate;

        BigDecimal costTotal = BigDecimal.ZERO;
        BigDecimal plannedCost = BigDecimal.ZERO;
        for (Task t : completed) {
            TaskDefinition def = t.getTaskDefinition();
            if (def != null && def.getExpectedCost() != null) {
                plannedCost = plannedCost.add(def.getExpectedCost());
                if (t.getActualDuration() != null && t.getPlannedDuration() != null && t.getPlannedDuration() > 0) {
                    BigDecimal ratio = BigDecimal.valueOf(t.getActualDuration())
                            .divide(BigDecimal.valueOf(t.getPlannedDuration()), 4, RoundingMode.HALF_UP);
                    costTotal = costTotal.add(def.getExpectedCost().multiply(ratio));
                } else {
                    costTotal = costTotal.add(def.getExpectedCost());
                }
            }
        }
        double costScore = plannedCost.compareTo(BigDecimal.ZERO) > 0 ?
                1.0 - Math.min(costTotal.divide(plannedCost, 4, RoundingMode.HALF_UP).doubleValue(), 1.0) : 1.0;

        BigDecimal w1 = weights.getW1() != null ? weights.getW1() : DEFAULT_WEIGHT;
        BigDecimal w2 = weights.getW2() != null ? weights.getW2() : DEFAULT_WEIGHT;
        BigDecimal w3 = weights.getW3() != null ? weights.getW3() : DEFAULT_WEIGHT;

        return w1.doubleValue() * delayScore +
               w2.doubleValue() * costScore +
               w3.doubleValue() * efficiency;
    }

    private List<KpiUserStatsDto.RatingHistoryPoint> buildRatingHistory(Long userId, List<Task> completed) {
        List<KpiUserStatsDto.RatingHistoryPoint> history = new ArrayList<>();
        KpiWeights globalWeights = kpiWeightsRepository.findByModelIdIsNull().stream()
                .findFirst()
                .orElse(KpiWeights.defaultWeights());

        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            List<Task> tasksByDate = completed.stream()
                    .filter(t -> t.getCompletedAt() != null && t.getCompletedAt().toLocalDate().isEqual(date))
                    .collect(Collectors.toList());

            double dayRating = calcUserRating(globalWeights, tasksByDate, completed);
            KpiUserStatsDto.RatingHistoryPoint point = new KpiUserStatsDto.RatingHistoryPoint();
            point.setDate(date.toString());
            point.setRating(round(dayRating, 3));
            history.add(point);
        }
        return history;
    }

    private double round(double value, int places) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
