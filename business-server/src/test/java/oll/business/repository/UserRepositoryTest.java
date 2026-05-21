package oll.business.repository;

import oll.business.model.Department;
import oll.business.model.Role;
import oll.business.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired private UserRepository userRepository;
    @Autowired private DepartmentRepository departmentRepository;

    private Department it;
    private Department hr;

    @BeforeEach
    void setUp() {
        departmentRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        it = departmentRepository.save(new Department("IT", null));
        hr = departmentRepository.save(new Department("HR", null));
        departmentRepository.flush();

        User admin = new User("admin_ut", "hash_admin", Role.ADMIN, "Admin", "User");
        admin.setDepartment(it);
        userRepository.save(admin);

        User analyst = new User("analyst_ut", "hash_analyst", Role.ANALYST, "Analyst", "User");
        analyst.setDepartment(it);
        userRepository.save(analyst);

        User executor = new User("executor_ut", "hash_exec", Role.EXECUTOR, "Exec", "User");
        executor.setDepartment(hr);
        userRepository.save(executor);

        userRepository.flush();
    }

    @Test
    void findByUsername_shouldReturnUser_whenExists() {
        Optional<User> result = userRepository.findByUsername("admin_ut");

        assertTrue(result.isPresent());
        assertEquals("admin_ut", result.get().getUsername());
        assertEquals(Role.ADMIN, result.get().getRole());
    }

    @Test
    void findByUsername_shouldReturnEmpty_whenNotExists() {
        Optional<User> result = userRepository.findByUsername("nonexistent");

        assertTrue(result.isEmpty());
    }

    @Test
    void existsByUsername_shouldReturnTrue_whenExists() {
        assertTrue(userRepository.existsByUsername("analyst_ut"));
    }

    @Test
    void existsByUsername_shouldReturnFalse_whenNotExists() {
        assertFalse(userRepository.existsByUsername("ghost"));
    }

    @Test
    void findByDepartmentId_shouldReturnUsersInDepartment() {
        List<User> result = userRepository.findByDepartmentId(it.getId());

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(u -> u.getDepartment().getId().equals(it.getId())));
    }

    @Test
    void findByDepartmentId_shouldReturnEmpty_whenNoUsers() {
        Department emptyDept = departmentRepository.save(new Department("Empty", null));

        List<User> result = userRepository.findByDepartmentId(emptyDept.getId());

        assertTrue(result.isEmpty());
    }

    @Test
    void findByRole_shouldReturnUsersWithGivenRole() {
        List<User> result = userRepository.findByRole(Role.ANALYST);

        assertEquals(1, result.size());
        assertEquals("analyst_ut", result.getFirst().getUsername());
    }

    @Test
    void findByRole_shouldReturnEmpty_whenNoMatch() {
        List<User> result = userRepository.findByRole(Role.MANAGER);

        assertTrue(result.isEmpty());
    }

    @Test
    void findByRole_shouldReturnMultipleUsers() {
        User secondAdmin = new User("admin2_ut", "hash2", Role.ADMIN, "Admin2", "User");
        secondAdmin.setDepartment(it);
        userRepository.save(secondAdmin);
        userRepository.flush();

        List<User> result = userRepository.findByRole(Role.ADMIN);

        assertEquals(2, result.size());
    }

    @Test
    void save_shouldPersistUser() {
        User newUser = new User("newguy_ut", "hash_new", Role.MANAGER, "New", "Guy");
        newUser.setDepartment(hr);
        userRepository.saveAndFlush(newUser);

        Optional<User> found = userRepository.findByUsername("newguy_ut");
        assertTrue(found.isPresent());
        assertEquals(Role.MANAGER, found.get().getRole());
        assertEquals(hr.getId(), found.get().getDepartment().getId());
    }

    @Test
    void delete_shouldRemoveUser() {
        Optional<User> adminOpt = userRepository.findByUsername("admin_ut");
        assertTrue(adminOpt.isPresent());

        userRepository.delete(adminOpt.get());
        userRepository.flush();

        assertFalse(userRepository.existsByUsername("admin_ut"));
        assertEquals(2, userRepository.count());
    }

    @Test
    void findById_shouldReturnUser() {
        Optional<User> adminOpt = userRepository.findByUsername("admin_ut");
        assertTrue(adminOpt.isPresent());

        Optional<User> result = userRepository.findById(adminOpt.get().getId());

        assertTrue(result.isPresent());
        assertEquals("admin_ut", result.get().getUsername());
    }

    @Test
    void findById_shouldReturnEmpty_whenNotFound() {
        Optional<User> result = userRepository.findById(999L);

        assertTrue(result.isEmpty());
    }

    @Test
    void existsByUsername_shouldBeCaseSensitive() {
        assertFalse(userRepository.existsByUsername("ADMIN_UT"));
    }

    @Test
    void findByDepartmentId_shouldNotIncludeUsersFromOtherDepartments() {
        List<User> result = userRepository.findByDepartmentId(hr.getId());

        assertEquals(1, result.size());
        assertEquals("executor_ut", result.getFirst().getUsername());
    }
}
