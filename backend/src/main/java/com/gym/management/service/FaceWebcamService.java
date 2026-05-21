package com.gym.management.service;

import com.gym.management.dto.AccessVerifyResponse;
import com.gym.management.dto.FaceWebcamEnrollRequest;
import com.gym.management.dto.FaceWebcamEnrollResponse;
import com.gym.management.dto.FaceWebcamVerifyRequest;
import com.gym.management.exception.BusinessException;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.model.AccessPersonType;
import com.gym.management.model.BiometricCredentialType;
import com.gym.management.model.Employee;
import com.gym.management.model.EmployeeFaceEmbedding;
import com.gym.management.model.Member;
import com.gym.management.model.MemberFaceEmbedding;
import com.gym.management.repository.EmployeeFaceEmbeddingRepository;
import com.gym.management.repository.EmployeeRepository;
import com.gym.management.repository.MemberFaceEmbeddingRepository;
import com.gym.management.repository.MemberRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FaceWebcamService {

    private final MemberRepository memberRepository;
    private final EmployeeRepository employeeRepository;
    private final MemberFaceEmbeddingRepository memberEmbeddingRepository;
    private final EmployeeFaceEmbeddingRepository employeeEmbeddingRepository;
    private final FaceDescriptorMath faceDescriptorMath;
    private final AccessControlService accessControlService;

    @Value("${app.access.face-match-max-distance:0.55}")
    private double maxMatchDistance;

    @Transactional
    public FaceWebcamEnrollResponse enroll(FaceWebcamEnrollRequest request) {
        double[] descriptor = faceDescriptorMath.parseDescriptor(request.descriptor());

        if (request.employeeId() != null) {
            return enrollStaff(request.employeeId(), descriptor);
        }
        if (request.memberId() == null) {
            throw new BusinessException("Indica el afiliado o el entrenador a registrar.");
        }
        return enrollMember(request.memberId(), descriptor);
    }

    private FaceWebcamEnrollResponse enrollMember(Long memberId, double[] descriptor) {
        Member member = memberRepository
                .findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Afiliado no encontrado: " + memberId));

        MemberFaceEmbedding embedding = memberEmbeddingRepository
                .findByMemberId(member.getId())
                .orElse(MemberFaceEmbedding.builder().member(member).build());
        embedding.setDescriptorJson(faceDescriptorMath.serialize(descriptor));
        memberEmbeddingRepository.save(embedding);

        return toMemberResponse(member, embedding.getEnrolledAt());
    }

    private FaceWebcamEnrollResponse enrollStaff(Long employeeId, double[] descriptor) {
        Employee employee = employeeRepository
                .findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Entrenador no encontrado: " + employeeId));

        EmployeeFaceEmbedding embedding = employeeEmbeddingRepository
                .findByEmployeeId(employee.getId())
                .orElse(EmployeeFaceEmbedding.builder().employee(employee).build());
        embedding.setDescriptorJson(faceDescriptorMath.serialize(descriptor));
        employeeEmbeddingRepository.save(embedding);

        return toStaffResponse(employee, embedding.getEnrolledAt());
    }

    @Transactional
    public void removeEnrollment(Long memberId) {
        MemberFaceEmbedding embedding = memberEmbeddingRepository
                .findByMemberId(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Este afiliado no tiene rostro con webcam registrado"));
        memberEmbeddingRepository.delete(embedding);
    }

    @Transactional
    public void removeStaffEnrollment(Long employeeId) {
        EmployeeFaceEmbedding embedding = employeeEmbeddingRepository
                .findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Este entrenador no tiene rostro con webcam registrado"));
        employeeEmbeddingRepository.delete(embedding);
    }

    @Transactional(readOnly = true)
    public List<FaceWebcamEnrollResponse> listEnrollments() {
        List<FaceWebcamEnrollResponse> all = new ArrayList<>();
        memberEmbeddingRepository.findAllWithMember().stream()
                .map(e -> toMemberResponse(e.getMember(), e.getEnrolledAt()))
                .forEach(all::add);
        employeeEmbeddingRepository.findAllWithActiveEmployee().stream()
                .filter(e -> EmployeeVisibility.visibleInTeamDirectory(e.getEmployee()))
                .map(e -> toStaffResponse(e.getEmployee(), e.getEnrolledAt()))
                .forEach(all::add);
        return all;
    }

    @Transactional
    public AccessVerifyResponse verify(FaceWebcamVerifyRequest request) {
        double[] probe = faceDescriptorMath.parseDescriptor(request.descriptor());

        MemberFaceEmbedding bestMember = null;
        double bestMemberDistance = Double.MAX_VALUE;
        for (MemberFaceEmbedding candidate : memberEmbeddingRepository.findAllWithMember()) {
            double distance = faceDescriptorMath.euclideanDistance(
                    probe, faceDescriptorMath.parseStored(candidate.getDescriptorJson()));
            if (distance < bestMemberDistance) {
                bestMemberDistance = distance;
                bestMember = candidate;
            }
        }

        EmployeeFaceEmbedding bestStaff = null;
        double bestStaffDistance = Double.MAX_VALUE;
        for (EmployeeFaceEmbedding candidate : employeeEmbeddingRepository.findAllWithActiveEmployee()) {
            double distance = faceDescriptorMath.euclideanDistance(
                    probe, faceDescriptorMath.parseStored(candidate.getDescriptorJson()));
            if (distance < bestStaffDistance) {
                bestStaffDistance = distance;
                bestStaff = candidate;
            }
        }

        if (bestMember == null && bestStaff == null) {
            return accessControlService.denyWebcamAccess("BIO", "No hay rostros registrados en el lector biométrico");
        }

        boolean memberWins = bestMember != null
                && (bestStaff == null || bestMemberDistance <= bestStaffDistance);
        double bestDistance = memberWins ? bestMemberDistance : bestStaffDistance;

        if (bestDistance > maxMatchDistance) {
            return accessControlService.denyWebcamAccess(
                    "BIO",
                    "Rostro no reconocido. Acércate más a la cámara o regístrate en recepción.");
        }

        if (memberWins && bestMember != null) {
            Member member = bestMember.getMember();
            return accessControlService.processMemberAccess(
                    member, "BIO-M-" + member.getId(), BiometricCredentialType.FACE, false);
        }

        if (bestStaff != null) {
            Employee employee = bestStaff.getEmployee();
            return accessControlService.processStaffAccess(
                    employee, "BIO-E-" + employee.getId(), BiometricCredentialType.FACE, false);
        }

        return accessControlService.denyWebcamAccess("BIO", "Rostro no reconocido");
    }

    private static FaceWebcamEnrollResponse toMemberResponse(Member member, java.time.Instant enrolledAt) {
        return new FaceWebcamEnrollResponse(
                member.getId(),
                null,
                AccessPersonType.MEMBER,
                member.getFirstName() + " " + member.getLastName(),
                member.getDocumentId(),
                enrolledAt);
    }

    private static FaceWebcamEnrollResponse toStaffResponse(Employee employee, java.time.Instant enrolledAt) {
        return new FaceWebcamEnrollResponse(
                null,
                employee.getId(),
                AccessPersonType.STAFF,
                employee.getFirstName() + " " + employee.getLastName(),
                null,
                enrolledAt);
    }
}
