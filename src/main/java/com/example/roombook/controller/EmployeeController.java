package com.example.roombook.controller;

import com.example.roombook.entity.Employee;
import com.example.roombook.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/employees")
@CrossOrigin
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(
            EmployeeService employeeService) {

        this.employeeService =
                employeeService;
    }


    @PostMapping
    public ResponseEntity<Employee> createEmployee(
            @Valid @RequestBody Employee employee) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        employeeService
                                .createEmployee(employee)
                );
    }


    @GetMapping
    public List<Employee> getAllEmployees() {

        return employeeService
                .getAllEmployees();
    }


    @GetMapping("/{id}")
    public Employee getEmployee(
            @PathVariable Long id) {

        return employeeService
                .getEmployeeById(id);
    }


    @PutMapping("/{id}")
    public Employee updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody Employee employee) {

        return employeeService
                .updateEmployee(id, employee);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteEmployee(
            @PathVariable Long id) {

        employeeService.deleteEmployee(id);

        return ResponseEntity.ok(
                "Employee deleted successfully"
        );
    }
}