package oll.business.service;

import oll.business.model.Department;
import oll.business.model.Role;
import oll.business.model.User;
import oll.business.repository.DepartmentRepository;
import oll.business.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private LogService logService;

    @Captor private ArgumentCaptor<User> userCaptor;

    private UserService userService;

    private User existingUser;
    private Department department;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, departmentRepository, passwordEncoder, logService);

        department = new Department("IT", null);
        department.setId(10L);

        existingUser = new User("john", "encodedPass", Role.ANALYST, "John", "Doe");
        existingUser.setId(1L);
        existingUser.setDepartment(department);
    }

    @Test
    void findAll_shouldReturnUsersSortedByIdAsc() {
        when(userRepository.findAll(Sort.by(Sort.Direction.ASC, "id")))
                .thenReturn(List.of(existingUser));

        List<User> result = userService.findAll();

        assertEquals(1, result.size());
        assertEquals("john", result.getFirst().getUsername());
        verify(userRepository).findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Test
    void findById_shouldReturnUser_whenFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        User result = userService.findById(1L);

        assertEquals("john", result.getUsername());
        assertEquals(Role.ANALYST, result.getRole());
    }

    @Test
    void findById_shouldThrow_whenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.findById(99L));
        assertEquals("User not found: 99", ex.getMessage());
    }

    @Test
    void findByUsername_shouldReturnUser_whenFound() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(existingUser));

        User result = userService.findByUsername("john");

        assertEquals(1L, result.getId());
    }

    @Test
    void findByUsername_shouldThrow_whenNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.findByUsername("unknown"));
        assertEquals("User not found: unknown", ex.getMessage());
    }

    @Test
    void findByDepartmentId_shouldReturnUsers() {
        when(userRepository.findByDepartmentId(10L)).thenReturn(List.of(existingUser));

        List<User> result = userService.findByDepartmentId(10L);

        assertEquals(1, result.size());
        assertEquals("john", result.getFirst().getUsername());
    }

    @Test
    void findByRole_shouldReturnUsers() {
        when(userRepository.findByRole(Role.MANAGER)).thenReturn(List.of());

        List<User> result = userService.findByRole(Role.MANAGER);

        assertTrue(result.isEmpty());
    }

    @Test
    void create_shouldSaveUserWithHashedPassword() {
        UserService.UserRequest request = new UserService.UserRequest(
                "alice", "rawPass", Role.EXECUTOR, "Alice", "Smith", null);

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("rawPass")).thenReturn("$2a$10hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(2L);
            return u;
        });

        User result = userService.create(request);

        assertEquals("alice", result.getUsername());
        assertEquals("$2a$10hashed", result.getPasswordHash());
        assertEquals(Role.EXECUTOR, result.getRole());
        verify(userRepository).save(userCaptor.capture());
        assertNull(userCaptor.getValue().getDepartment());
        verify(logService).logInfo(eq("User created: alice (id=2, role=EXECUTOR)"), eq("UserService"), eq("create"), eq(2L), isNull());
    }

    @Test
    void create_shouldSetDepartment_whenProvided() {
        UserService.UserRequest request = new UserService.UserRequest(
                "bob", "pass", Role.ADMIN, "Bob", "Brown", 10L);

        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("hash");
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(3L);
            return u;
        });

        User result = userService.create(request);

        assertNotNull(result.getDepartment());
        assertEquals("IT", result.getDepartment().getName());
    }

    @Test
    void create_shouldThrow_whenUsernameExists() {
        UserService.UserRequest request = new UserService.UserRequest(
                "john", "pass", Role.ADMIN, "John", "Dupont", null);

        when(userRepository.existsByUsername("john")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.create(request));
        assertEquals("Username already exists", ex.getMessage());
        verify(logService).logWarn("User creation failed: username already exists", "UserService", "create");
        verify(userRepository, never()).save(any());
    }

    @Test
    void create_shouldThrow_whenDepartmentNotFound() {
        UserService.UserRequest request = new UserService.UserRequest(
                "bob", "pass", Role.ADMIN, "Bob", "Brown", 99L);

        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.create(request));
        assertEquals("Department not found: 99", ex.getMessage());
    }

    @Test
    void update_shouldUpdateAllProvidedFields() {
        UserService.UserRequest request = new UserService.UserRequest(
                "john", "ignored", Role.MANAGER, "Jonathan", "Doherty", 10L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(department));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.update(1L, request);

        assertEquals("Jonathan", result.getFirstName());
        assertEquals("Doherty", result.getLastName());
        assertEquals(Role.MANAGER, result.getRole());
        assertEquals(department, result.getDepartment());
        verify(logService).logInfo("User updated: id=1", "UserService", "update", 1L, null);
    }

    @Test
    void update_shouldIgnoreNullFields() {
        UserService.UserRequest request = new UserService.UserRequest(
                null, null, null, null, "Doe Jr", null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.update(1L, request);

        assertEquals("john", result.getUsername());
        assertEquals("John", result.getFirstName());
        assertEquals("Doe Jr", result.getLastName());
        assertEquals(Role.ANALYST, result.getRole());
        assertEquals(department, result.getDepartment());
    }

    @Test
    void update_shouldThrow_whenUserNotFound() {
        UserService.UserRequest request = new UserService.UserRequest(
                null, null, null, null, null, null);

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.update(99L, request));
    }

    @Test
    void update_shouldThrow_whenDepartmentNotFound() {
        UserService.UserRequest request = new UserService.UserRequest(
                null, null, null, null, null, 99L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.update(1L, request));
        assertEquals("Department not found: 99", ex.getMessage());
    }

    @Test
    void updatePassword_shouldEncodeAndSave() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode("newSecret")).thenReturn("$2a$10newHash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updatePassword(1L, "newSecret");

        assertEquals("$2a$10newHash", result.getPasswordHash());
        verify(logService).logInfo("Password changed for user id=1", "UserService", "updatePassword", 1L, null);
    }

    @Test
    void updatePassword_shouldThrow_whenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.updatePassword(99L, "pass"));
    }

    @Test
    void delete_shouldRemoveUserAndLog() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        userService.delete(1L);

        verify(userRepository).delete(existingUser);
        verify(logService).logInfo("User deleted: john (id=1)", "UserService", "delete", 1L, null);
    }

    @Test
    void delete_shouldThrow_whenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.delete(99L));
        verify(userRepository, never()).delete(any());
    }
}
