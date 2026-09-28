package com.example.roombook.service;

import com.example.roombook.entity.Employee;
import com.example.roombook.exception.ResourceNotFoundException;
import com.example.roombook.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeService(
            EmployeeRepository employeeRepository) {

        this.employeeRepository = employeeRepository;
    }


    public Employee createEmployee(Employee employee) {

        if (employeeRepository.existsByEmail(
                employee.getEmail())) {

            throw new IllegalArgumentException(
                    "Employee email already exists"
            );
        }

        return employeeRepository.save(employee);
    }


    public List<Employee> getAllEmployees() {

        return employeeRepository.findAll();
    }


    public Employee getEmployeeById(Long id) {

        return employeeRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found with id: " + id
                        )
                );
    }


    public Employee updateEmployee(
            Long id,
            Employee employee) {

        Employee existing =
                getEmployeeById(id);

        existing.setName(
                employee.getName()
        );

        existing.setEmail(
                employee.getEmail()
        );

        existing.setDepartment(
                employee.getDepartment()
        );

        return employeeRepository.save(existing);
    }


    public void deleteEmployee(Long id) {

        Employee employee =
                getEmployeeById(id);

        employeeRepository.delete(employee);
    }
}