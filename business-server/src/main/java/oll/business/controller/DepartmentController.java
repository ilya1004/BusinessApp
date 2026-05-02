package oll.business.controller;

import oll.business.model.Department;
import oll.business.repository.DepartmentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;
import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentRepository departmentRepository;

    public DepartmentController(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @GetMapping
    public List<Department> findAll() {
        return departmentRepository.findAll();
    }

    @GetMapping("/{id:\\d+}/children")
    public List<Department> findChildren(@PathVariable Long id) {
        return departmentRepository.findAll().stream()
                .filter(d -> d.getParent() != null && d.getParent().getId().equals(id))
                .toList();
    }

    @PostMapping
    public Department create(@RequestBody DepartmentRequest request) {
        Department department = new Department();
        department.setName(request.name());
        if (request.parentId() != null) {
            department.setParent(departmentRepository.findById(request.parentId()).orElse(null));
        }
        return departmentRepository.save(department);
    }

    @PutMapping("/{id}")
    public Department update(@PathVariable Long id, @RequestBody DepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));
        department.setName(request.name());
        if (request.parentId() != null) {
            department.setParent(departmentRepository.findById(request.parentId()).orElse(null));
        } else {
            department.setParent(null);
        }
        return departmentRepository.save(department);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        List<Department> all = departmentRepository.findAll();
        Long parentId = department.getParent() != null ? department.getParent().getId() : null;

        List<Department> children = all.stream()
                .filter(d -> d.getParent() != null && d.getParent().getId().equals(id))
                .toList();

        for (Department child : children) {
            if (parentId != null) {
                child.setParent(departmentRepository.findById(parentId).orElse(null));
            } else {
                child.setParent(null);
            }
            departmentRepository.save(child);
        }

        departmentRepository.delete(department);
        return ResponseEntity.noContent().build();
    }

    public record DepartmentRequest(String name, Long parentId) {}
}
