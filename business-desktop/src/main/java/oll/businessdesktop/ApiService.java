package oll.businessdesktop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import oll.businessdesktop.model.Department;
import oll.businessdesktop.model.LoginRequest;
import oll.businessdesktop.model.AuthResponse;
import oll.businessdesktop.model.User;
import oll.businessdesktop.model.ProcessModel;
import oll.businessdesktop.model.TaskDefinition;
import oll.businessdesktop.model.ProcessInstance;
import oll.businessdesktop.model.Task;
import oll.businessdesktop.model.KpiModelData;
import oll.businessdesktop.model.KpiInstanceData;
import oll.businessdesktop.model.KpiUserStats;
import oll.businessdesktop.model.KpiWeights;
import oll.businessdesktop.model.AppLog;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class ApiService {
    private static final String BASE_URL = getBaseUrl();

    private static String getBaseUrl() {
        String serverUrl = System.getenv("SERVER_URL");
        if (serverUrl == null || serverUrl.isBlank()) {
            serverUrl = "http://localhost:8080";
        }
        return serverUrl + "/api";
    }
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    private static String authToken;
    private static String currentUserRole;

    public static AuthResponse login(String username, String password) throws IOException, InterruptedException {
        LoginRequest request = new LoginRequest(username, password);
        String json = objectMapper.writeValueAsString(request);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            AuthResponse authResponse = objectMapper.readValue(response.body(), AuthResponse.class);
            authToken = authResponse.token();
            User user = getCurrentUser();
            currentUserRole = user != null ? user.role() : null;
            return authResponse;
        } else {
            throw new RuntimeException("Login failed: " + response.body());
        }
    }

    public static String getCurrentUserRole() {
        return currentUserRole;
    }

    public static User getCurrentUser() throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/users/me"))
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), User.class);
        } else {
            throw new RuntimeException("Failed to get user: " + response.body());
        }
    }

    public static String getAuthToken() {
        return authToken;
    }

    public static void setAuthToken(String token) {
        authToken = token;
    }

    public static java.util.List<User> getAllUsers() throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/users"))
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            var typeRef = new com.fasterxml.jackson.core.type.TypeReference<java.util.List<User>>() {};
            return objectMapper.readValue(response.body(), typeRef);
        } else {
            throw new RuntimeException("Failed to get users: " + response.body());
        }
    }

    public static java.util.List<String> getRoles() throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/users/roles"))
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            var typeRef = new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {};
            return objectMapper.readValue(response.body(), typeRef);
        } else {
            throw new RuntimeException("Failed to get roles: " + response.body());
        }
    }

    public static User createUser(String username, String password, String role, String firstName, String lastName) throws IOException, InterruptedException {
        return createUser(username, password, role, firstName, lastName, null);
    }

    public static User createUser(String username, String password, String role, String firstName, String lastName, Long departmentId) throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "username", username,
                "password", password,
                "role", role,
                "firstName", firstName,
                "lastName", lastName,
                "departmentId", departmentId
        ));

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/users"))
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            return objectMapper.readValue(response.body(), User.class);
        } else {
            throw new RuntimeException("Failed to create user: " + response.body());
        }
    }

    public static User updateUser(Long userId, String firstName, String lastName, String role, Long departmentId) throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "firstName", firstName,
                "lastName", lastName,
                "role", role,
                "departmentId", departmentId
        ));

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/users/" + userId))
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), User.class);
        } else {
            throw new RuntimeException("Failed to update user: " + response.body());
        }
    }

    public static void deleteUser(Long userId) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/users/" + userId))
                .header("Authorization", "Bearer " + authToken)
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 204) {
            throw new RuntimeException("Failed to delete user: " + response.body());
        }
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static java.util.List<Department> getAllDepartments() throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/departments"))
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            var typeRef = new com.fasterxml.jackson.core.type.TypeReference<java.util.List<Department>>() {};
            return objectMapper.readValue(response.body(), typeRef);
        } else {
            throw new RuntimeException("Failed to get departments: " + response.body());
        }
    }

    public static Department createDepartment(String name, Long parentId) throws IOException, InterruptedException {
        String requestBody = """
                {"name":"%s","parentId":%s}
                """.formatted(escapeJson(name), parentId == null ? "null" : parentId);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/departments"))
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            return objectMapper.readValue(response.body(), Department.class);
        } else {
            throw new RuntimeException("Failed to create department: " + response.body());
        }
    }

    public static Department updateDepartment(Long id, String name, Long parentId) throws IOException, InterruptedException {
        String requestBody = """
                {"name":"%s","parentId":%s}
                """.formatted(escapeJson(name), parentId == null ? "null" : parentId);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/departments/" + id))
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), Department.class);
        } else {
            throw new RuntimeException("Failed to update department: " + response.body());
        }
    }

    public static void deleteDepartment(Long departmentId) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/departments/" + departmentId))
                .header("Authorization", "Bearer " + authToken)
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 204) {
            throw new RuntimeException("Failed to delete department: " + response.body());
        }
    }

    public static java.util.List<Department> getDepartmentChildren(Long departmentId) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/departments/" + departmentId + "/children"))
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            var typeRef = new com.fasterxml.jackson.core.type.TypeReference<java.util.List<Department>>() {};
            return objectMapper.readValue(response.body(), typeRef);
        } else {
            throw new RuntimeException("Failed to get department children: " + response.body());
        }
    }

    public static ProcessModel findProcessModelByName(String name) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/process-models/find-by-name?name=" + java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8)))
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            String body = response.body();
            if (body.isBlank() || body.equals("null")) {
                return null;
            }
            return objectMapper.readValue(body, ProcessModel.class);
        } else {
            throw new RuntimeException("Failed to find process model: " + response.body());
        }
    }

    public static java.util.List<ProcessModel> getAllProcessModels() throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/process-models"))
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            var typeRef = new com.fasterxml.jackson.core.type.TypeReference<java.util.List<ProcessModel>>() {};
            return objectMapper.readValue(response.body(), typeRef);
        } else {
            throw new RuntimeException("Failed to get process models: " + response.body());
        }
    }

    public static ProcessModel createProcessModel(String name, String bpmnXml, Long authorId, java.util.List<TaskDefinition> taskDefinitions) throws IOException, InterruptedException {
        String authorIdJson = authorId != null ? authorId.toString() : "null";
        String taskDefsJson = serializeTaskDefinitions(taskDefinitions);
        String requestBody = """
                {"name":"%s","bpmnXml":"%s","authorId":%s,"taskDefinitions":%s}
                """.formatted(escapeJson(name), escapeJson(bpmnXml), authorIdJson, taskDefsJson);

        System.out.println("[API] Creating ProcessModel with payload length: " + requestBody.length());
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/process-models"))
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            return objectMapper.readValue(response.body(), ProcessModel.class);
        } else {
            throw new RuntimeException("Failed to create process model: " + response.body());
        }
    }

    public static ProcessModel updateProcessModel(Long id, String name, String bpmnXml, Long authorId, java.util.List<TaskDefinition> taskDefinitions) throws IOException, InterruptedException {
        String authorIdJson = authorId != null ? authorId.toString() : "null";
        String taskDefsJson = serializeTaskDefinitions(taskDefinitions);
        String requestBody = """
                {"name":"%s","bpmnXml":"%s","authorId":%s,"taskDefinitions":%s}
                """.formatted(escapeJson(name), escapeJson(bpmnXml), authorIdJson, taskDefsJson);

        System.out.println("[API] Updating ProcessModel id=" + id + " with payload length: " + requestBody.length());

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/process-models/" + id))
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), ProcessModel.class);
        } else {
            throw new RuntimeException("Failed to update process model: " + response.body());
        }
    }

    public static TaskDefinition updateTaskDefinition(Long id, String name, Integer defaultDuration, BigDecimal expectedCost, BigDecimal kpiWeight) throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(new TaskDefinitionUpdateRequest(name, defaultDuration, expectedCost, kpiWeight));
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/task-definitions/" + id))
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), TaskDefinition.class);
        } else {
            throw new RuntimeException("Failed to update task definition: " + response.body());
        }
    }

    public record TaskDefinitionUpdateRequest(String name, Integer defaultDuration, BigDecimal expectedCost, BigDecimal kpiWeight) {}

    private static String serializeTaskDefinitions(java.util.List<TaskDefinition> taskDefinitions) {
        if (taskDefinitions == null || taskDefinitions.isEmpty()) {
            return "[]";
        }
        try {
            String json = objectMapper.writeValueAsString(taskDefinitions);
            System.out.println("[API] Serialized TaskDefinitions: " + json);
            return json;
        } catch (Exception e) {
            System.err.println("Failed to serialize TaskDefinitions: " + e.getMessage());
            return "[]";
        }
    }

    public static java.util.List<ProcessInstance> getAllProcessInstances() throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/process-instances"))
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            var typeRef = new com.fasterxml.jackson.core.type.TypeReference<java.util.List<ProcessInstance>>() {};
            return objectMapper.readValue(response.body(), typeRef);
        } else {
            throw new RuntimeException("Failed to get process instances: " + response.body());
        }
    }

    public static ProcessInstance getProcessInstanceById(Long id) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/process-instances/" + id))
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), ProcessInstance.class);
        } else {
            throw new RuntimeException("Failed to get process instance: " + response.body());
        }
    }

    public static ProcessInstance createProcessInstance(Long processModelId, String instanceName) throws IOException, InterruptedException {
        String requestBody = objectMapper.writeValueAsString(
                java.util.Map.of("processModelId", processModelId, "instanceName", instanceName));

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/process-instances"))
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            return objectMapper.readValue(response.body(), ProcessInstance.class);
        } else {
            throw new RuntimeException("Failed to create process instance: " + response.body());
        }
    }

    public static java.util.List<Task> getTasksByInstance(Long instanceId) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/process-instances/" + instanceId + "/tasks"))
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            String body = response.body();
            System.out.println("[ApiService] getTasksByInstance(" + instanceId + ") response: " + body);
            var typeRef = new com.fasterxml.jackson.core.type.TypeReference<java.util.List<Task>>() {};
            return objectMapper.readValue(body, typeRef);
        } else {
            throw new RuntimeException("Failed to get tasks: " + response.body());
        }
    }

    public static java.util.List<Task> getMyTasks() throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/my"))
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            var typeRef = new com.fasterxml.jackson.core.type.TypeReference<java.util.List<Task>>() {};
            return objectMapper.readValue(response.body(), typeRef);
        } else {
            throw new RuntimeException("Failed to get my tasks: " + response.body());
        }
    }

    public static Task startTask(Long taskId) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/" + taskId + "/start"))
                .header("Authorization", "Bearer " + authToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), Task.class);
        } else {
            throw new RuntimeException("Failed to start task: " + response.body());
        }
    }

    public static Task completeTask(Long taskId) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/" + taskId + "/complete"))
                .header("Authorization", "Bearer " + authToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), Task.class);
        } else {
            throw new RuntimeException("Failed to complete task: " + response.body());
        }
    }

    public static Task cancelTask(Long taskId) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/" + taskId + "/cancel"))
                .header("Authorization", "Bearer " + authToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), Task.class);
        } else {
            throw new RuntimeException("Failed to cancel task: " + response.body());
        }
    }

    public static Task logTime(Long taskId, int minutes) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/" + taskId + "/log-time?minutes=" + minutes))
                .header("Authorization", "Bearer " + authToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), Task.class);
        } else {
            throw new RuntimeException("Failed to log time: " + response.body());
        }
    }

    public static Task updateTaskStatus(Long taskId, String status) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/process-instances/tasks/" + taskId + "/status?status=" + status))
                .header("Authorization", "Bearer " + authToken)
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), Task.class);
        } else {
            throw new RuntimeException("Failed to update task status: " + response.body());
        }
    }

    public static Task assignTask(Long taskId, Long assigneeId) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/" + taskId + "/assign?assigneeId=" + assigneeId))
                .header("Authorization", "Bearer " + authToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), Task.class);
        } else {
            throw new RuntimeException("Failed to assign task: " + response.body());
        }
    }

    public static Task unassignTask(Long taskId) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/tasks/" + taskId + "/unassign"))
                .header("Authorization", "Bearer " + authToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), Task.class);
        } else {
            throw new RuntimeException("Failed to unassign task: " + response.body());
        }
    }

    public static KpiModelData getModelKpi(Long modelId) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/kpi/models/" + modelId))
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), KpiModelData.class);
        } else {
            throw new RuntimeException("Failed to get model KPI: " + response.body());
        }
    }

    public static KpiInstanceData getInstanceKpi(Long instanceId) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/kpi/instances/" + instanceId))
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), KpiInstanceData.class);
        } else {
            throw new RuntimeException("Failed to get instance KPI: " + response.body());
        }
    }

    public static KpiUserStats getUserStats(Long userId) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/kpi/users/" + userId))
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), KpiUserStats.class);
        } else {
            throw new RuntimeException("Failed to get user stats: " + response.body());
        }
    }

    public static KpiUserStats getCurrentUserStats() throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/kpi/users/me"))
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), KpiUserStats.class);
        } else {
            throw new RuntimeException("Failed to get current user stats: " + response.body());
        }
    }

    public static KpiWeights getKpiWeights(Long modelId) throws IOException, InterruptedException {
        String url = BASE_URL + "/settings/kpi-weights";
        if (modelId != null) {
            url += "?modelId=" + modelId;
        }
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + authToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), KpiWeights.class);
        } else {
            throw new RuntimeException("Failed to get KPI weights: " + response.body());
        }
    }

    public static KpiWeights saveKpiWeights(Long modelId, java.math.BigDecimal w1, java.math.BigDecimal w2, java.math.BigDecimal w3) throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(
                java.util.Map.of("modelId", modelId, "w1", w1, "w2", w2, "w3", w3));

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/settings/kpi-weights"))
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), KpiWeights.class);
        } else {
            throw new RuntimeException("Failed to save KPI weights: " + response.body());
        }
    }

    public static oll.businessdesktop.model.SimulationResponse runSimulation(Long modelId, double durationMultiplier,
            int resourcesPerTask, int parallelismFactor) throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "modelId", modelId,
                "durationMultiplierX10", (int) Math.round(durationMultiplier * 10),
                "resourcesPerTask", resourcesPerTask,
                "parallelismFactor", parallelismFactor
        ));

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/simulations/run"))
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), oll.businessdesktop.model.SimulationResponse.class);
        } else {
            throw new RuntimeException("Simulation failed: " + response.body());
        }
    }

    public static Map<String, Object> getLogs(int page, int size, String level) throws Exception {
        String levelParam = (level != null && !level.equals("ALL")) ? "&level=" + level : "";
        String url = BASE_URL + "/logs?page=" + page + "&size=" + size + levelParam;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + getAuthToken())
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            Map<String, Object> result = objectMapper.readValue(response.body(), Map.class);
            List<Map<String, Object>> content = (List<Map<String, Object>>) result.get("content");
            List<AppLog> logs = content.stream()
                    .map(m -> objectMapper.convertValue(m, AppLog.class))
                    .toList();
            int totalElements = ((Number) result.get("totalElements")).intValue();
            return Map.of("logs", logs, "totalElements", totalElements);
        } else {
            throw new RuntimeException("Failed to get logs: " + response.body());
        }
    }

    public static List<oll.businessdesktop.model.AppLog> getRecentLogs(int limit) throws Exception {
        String url = BASE_URL + "/logs/recent?limit=" + limit;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + getAuthToken())
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, oll.businessdesktop.model.AppLog.class));
        } else {
            throw new RuntimeException("Failed to get recent logs: " + response.body());
        }
    }
}
