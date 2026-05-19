package com.gym.management.service;

import com.gym.management.dto.EmployeeRequest;
import com.gym.management.dto.EmployeeResponse;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.mapper.EmployeeMapper;
import com.gym.management.model.Employee;
import com.gym.management.model.UserRole;
import com.gym.management.repository.EmployeeRepository;
import com.gym.management.security.SecurityUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<EmployeeResponse> findAll() {
        return employeeRepository.findAll().stream()
                .map(EmployeeMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> findActive() {
        return employeeRepository.findByActiveTrueOrderByFirstNameAsc().stream()
                .map(EmployeeMapper::toResponse)
                .toList();
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        validatePaymentInfo(request);
        validateAccessFields(request, null);
        Employee employee = Employee.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(blankToNull(request.phone()))
                .username(blankToNull(request.username()))
                .passwordHash(encodePassword(request.password()))
                .role(request.role())
                .nequiNumber(blankToNull(request.nequiNumber()))
                .bankName(blankToNull(request.bankName()))
                .bankAccountNumber(blankToNull(request.bankAccountNumber()))
                .active(request.active() != null ? request.active() : true)
                .build();
        validateLoginConfigured(employee);
        return EmployeeMapper.toResponse(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = getEmployee(id);
        String nequi = coalescePayment(blankToNull(request.nequiNumber()), employee.getNequiNumber());
        String bankName = coalescePayment(blankToNull(request.bankName()), employee.getBankName());
        String bankAccount =
                coalescePayment(blankToNull(request.bankAccountNumber()), employee.getBankAccountNumber());
        validatePaymentInfo(nequi, bankName, bankAccount);
        validateAccessFields(request, id);
        employee.setFirstName(request.firstName());
        employee.setLastName(request.lastName());
        employee.setPhone(blankToNull(request.phone()));
        employee.setUsername(blankToNull(request.username()));
        if (request.password() != null && !request.password().isBlank()) {
            employee.setPasswordHash(passwordEncoder.encode(request.password().trim()));
        }
        employee.setRole(request.role());
        employee.setNequiNumber(nequi);
        employee.setBankName(bankName);
        employee.setBankAccountNumber(bankAccount);
        if (request.active() != null) {
            employee.setActive(request.active());
        }
        validateLoginConfigured(employee);
        return EmployeeMapper.toResponse(employeeRepository.save(employee));
    }

    private void validateAccessFields(EmployeeRequest request, Long employeeId) {
        String username = blankToNull(request.username());
        String password = blankToNull(request.password());
        UserRole role = request.role();

        if (username != null) {
            boolean usernameTaken = employeeId == null
                    ? employeeRepository.existsByUsernameIgnoreCase(username)
                    : employeeRepository.existsByUsernameIgnoreCaseAndIdNot(username, employeeId);
            if (usernameTaken) {
                throw new BusinessException("El nombre de usuario ya está en uso");
            }
            if (employeeId == null && password == null) {
                throw new BusinessException("Debe asignar una contraseña para el acceso");
            }
            if (role == null) {
                throw new BusinessException("Debe asignar un rol al entrenador");
            }
            if (role == UserRole.SUPER_ADMIN && !SecurityUtils.isSuperAdmin()) {
                throw new BusinessException("Solo un super administrador puede crear otro super administrador");
            }
        } else if (password != null || role != null) {
            throw new BusinessException("Si asigna contraseña o rol, debe indicar el usuario de acceso");
        }
    }

    private void validateLoginConfigured(Employee employee) {
        if (employee.getUsername() != null
                && (employee.getPasswordHash() == null || employee.getRole() == null)) {
            throw new BusinessException("Usuario de acceso incompleto: falta contraseña o rol");
        }
    }

    private String encodePassword(String password) {
        String value = blankToNull(password);
        if (value == null) {
            return null;
        }
        return passwordEncoder.encode(value);
    }

    private String coalescePayment(String requestValue, String existingValue) {
        return requestValue != null ? requestValue : existingValue;
    }

    private void validatePaymentInfo(EmployeeRequest request) {
        validatePaymentInfo(
                blankToNull(request.nequiNumber()),
                blankToNull(request.bankName()),
                blankToNull(request.bankAccountNumber()));
    }

    private void validatePaymentInfo(String nequi, String bankName, String bankAccount) {

        boolean hasNequi = nequi != null;

        if (bankName != null && bankAccount == null) {
            throw new BusinessException("Si indica el banco, debe ingresar el número de cuenta");
        }
        if (bankAccount != null && bankName == null) {
            throw new BusinessException("Si indica la cuenta, debe ingresar el nombre del banco");
        }

        boolean hasBank = bankName != null && bankAccount != null;
        if (!hasNequi && !hasBank) {
            throw new BusinessException(
                    "Registre el Nequi o la cuenta bancaria donde se consignará el pago del entrenador");
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    @Transactional
    public void delete(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Entrenador no encontrado: " + id);
        }
        employeeRepository.deleteById(id);
    }

    public Employee getEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrenador no encontrado: " + id));
    }
}
