package com.gym.management.service;

import com.gym.management.dto.AccessVerifyResponse;
import com.gym.management.dto.FaceWebcamEnrollRequest;
import com.gym.management.dto.FaceWebcamEnrollResponse;
import com.gym.management.dto.FaceWebcamVerifyRequest;
import com.gym.management.exception.ResourceNotFoundException;
import com.gym.management.model.BiometricCredentialType;
import com.gym.management.model.Member;
import com.gym.management.model.MemberFaceEmbedding;
import com.gym.management.repository.MemberFaceEmbeddingRepository;
import com.gym.management.repository.MemberRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FaceWebcamService {

    private final MemberRepository memberRepository;
    private final MemberFaceEmbeddingRepository embeddingRepository;
    private final FaceDescriptorMath faceDescriptorMath;
    private final AccessControlService accessControlService;

    @Value("${app.access.face-match-max-distance:0.55}")
    private double maxMatchDistance;

    @Transactional
    public FaceWebcamEnrollResponse enroll(FaceWebcamEnrollRequest request) {
        Member member = memberRepository
                .findById(request.memberId())
                .orElseThrow(() -> new ResourceNotFoundException("Afiliado no encontrado: " + request.memberId()));

        double[] descriptor = faceDescriptorMath.parseDescriptor(request.descriptor());
        MemberFaceEmbedding embedding = embeddingRepository
                .findByMemberId(member.getId())
                .orElse(MemberFaceEmbedding.builder().member(member).build());
        embedding.setDescriptorJson(faceDescriptorMath.serialize(descriptor));
        embeddingRepository.save(embedding);

        return new FaceWebcamEnrollResponse(
                member.getId(),
                member.getFirstName() + " " + member.getLastName(),
                member.getDocumentId(),
                embedding.getEnrolledAt());
    }

    @Transactional
    public void removeEnrollment(Long memberId) {
        MemberFaceEmbedding embedding = embeddingRepository
                .findByMemberId(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Este afiliado no tiene rostro con webcam registrado"));
        embeddingRepository.delete(embedding);
    }

    @Transactional(readOnly = true)
    public List<FaceWebcamEnrollResponse> listEnrollments() {
        return embeddingRepository.findAllWithMember().stream()
                .map(e -> new FaceWebcamEnrollResponse(
                        e.getMember().getId(),
                        e.getMember().getFirstName() + " " + e.getMember().getLastName(),
                        e.getMember().getDocumentId(),
                        e.getEnrolledAt()))
                .toList();
    }

    @Transactional
    public AccessVerifyResponse verify(FaceWebcamVerifyRequest request) {
        double[] probe = faceDescriptorMath.parseDescriptor(request.descriptor());
        List<MemberFaceEmbedding> enrolled = embeddingRepository.findAllWithMember();
        if (enrolled.isEmpty()) {
            return accessControlService.denyWebcamAccess("BIO", "No hay rostros registrados en el lector biométrico");
        }

        MemberFaceEmbedding best = null;
        double bestDistance = Double.MAX_VALUE;
        for (MemberFaceEmbedding candidate : enrolled) {
            double distance = faceDescriptorMath.euclideanDistance(probe, faceDescriptorMath.parseStored(candidate.getDescriptorJson()));
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }

        if (best == null || bestDistance > maxMatchDistance) {
            String msg = bestDistance > maxMatchDistance
                    ? "Rostro no reconocido. Acércate más a la cámara o regístrate en recepción."
                    : "Rostro no reconocido";
            return accessControlService.denyWebcamAccess("BIO", msg);
        }

        Member member = best.getMember();
        String deviceUserId = "BIO-" + member.getId();
        return accessControlService.processMemberAccess(member, deviceUserId, BiometricCredentialType.FACE, false);
    }
}
