package oll.business.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import oll.business.model.Role;
import oll.business.model.User;
import oll.business.repository.UserRepository;
import oll.business.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    private MockMvc mvc;

    @Autowired private WebApplicationContext wac;

    @MockitoBean private UserService userService;
    @MockitoBean private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final User user = new User("john", "pass", Role.ANALYST, "John", "Doe");
    private final UserService.UserRequest request =
            new UserService.UserRequest("jane", "secret", Role.EXECUTOR, "Jane", "Smith", null);

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
    }

    @Test
    void findAll_shouldReturnUserList() throws Exception {
        when(userService.findAll()).thenReturn(List.of(user));

        mvc.perform(get("/api/users").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1))
                .andExpect(jsonPath("$[0].username").value("john"));
    }

    @Test
    void findAllRoles_shouldReturnAllRoles() throws Exception {
        mvc.perform(get("/api/users/roles").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(4))
                .andExpect(jsonPath("$[0]").value("ADMIN"))
                .andExpect(jsonPath("$[3]").value("EXECUTOR"));
    }

    @Test
    void findById_shouldReturnUser() throws Exception {
        when(userService.findById(1L)).thenReturn(user);

        mvc.perform(get("/api/users/1").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john"))
                .andExpect(jsonPath("$.role").value("ANALYST"));
    }

    @Test
    void me_shouldReturnCurrentUserInfo() throws Exception {
        when(userRepository.findByUsername("testuser")).thenReturn(java.util.Optional.of(user));

        mvc.perform(get("/api/users/me").with(user("testuser").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john"))
                .andExpect(jsonPath("$.role").value("ANALYST"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    void findByUsername_shouldReturnUser() throws Exception {
        when(userService.findByUsername("john")).thenReturn(user);

        mvc.perform(get("/api/users/username/john").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john"));
    }

    @Test
    void findByDepartmentId_shouldReturnUsers() throws Exception {
        when(userService.findByDepartmentId(10L)).thenReturn(List.of(user));

        mvc.perform(get("/api/users/department/10").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1));
    }

    @Test
    void findByRole_shouldReturnUsers() throws Exception {
        when(userService.findByRole(Role.MANAGER)).thenReturn(List.of());

        mvc.perform(get("/api/users/role/MANAGER").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(0));
    }

    @Test
    void create_shouldReturnCreatedUser() throws Exception {
        User saved = new User("jane", "encoded", Role.EXECUTOR, "Jane", "Smith");
        saved.setId(5L);
        when(userService.create(any(UserService.UserRequest.class))).thenReturn(saved);

        mvc.perform(post("/api/users")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.username").value("jane"));
    }

    @Test
    void update_shouldReturnUpdatedUser() throws Exception {
        User updated = new User("jane", "encoded", Role.EXECUTOR, "Jane", "Smith");
        updated.setId(1L);
        when(userService.update(eq(1L), any(UserService.UserRequest.class))).thenReturn(updated);

        mvc.perform(put("/api/users/1")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("jane"))
                .andExpect(jsonPath("$.role").value("EXECUTOR"));
    }

    @Test
    void updatePassword_shouldReturnUser() throws Exception {
        when(userService.updatePassword(1L, "newPass")).thenReturn(user);

        mvc.perform(put("/api/users/1/password")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("newPassword", "newPass"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john"));
    }

    @Test
    void delete_shouldReturnNoContent() throws Exception {
        doNothing().when(userService).delete(1L);

        mvc.perform(delete("/api/users/1")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(userService).delete(1L);
    }
}
