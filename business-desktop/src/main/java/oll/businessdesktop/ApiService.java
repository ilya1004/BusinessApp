package oll.businessdesktop;

import com.fasterxml.jackson.databind.ObjectMapper;
import oll.businessdesktop.model.Department;
import oll.businessdesktop.model.LoginRequest;
import oll.businessdesktop.model.AuthResponse;
import oll.businessdesktop.model.User;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ApiService {
    private static final String BASE_URL = "http://localhost:8080/api";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    private static String authToken;

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
            return authResponse;
        } else {
            throw new RuntimeException("Login failed: " + response.body());
        }
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
        String requestBody = """
                {"username":"%s","password":"%s","role":"%s","firstName":"%s","lastName":"%s","departmentId":null}
                """.formatted(
                escapeJson(username),
                escapeJson(password),
                escapeJson(role),
                escapeJson(firstName),
                escapeJson(lastName)
        );

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/users"))
                .header("Authorization", "Bearer " + authToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            return objectMapper.readValue(response.body(), User.class);
        } else {
            throw new RuntimeException("Failed to create user: " + response.body());
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
}
