package com.gym.management.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.management.exception.BusinessException;
import com.gym.management.model.MemberFaceEmbedding;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FaceDescriptorMath {

    private static final TypeReference<List<Double>> DESCRIPTOR_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public double[] parseDescriptor(List<Double> raw) {
        if (raw == null || raw.size() != MemberFaceEmbedding.DESCRIPTOR_LENGTH) {
            throw new BusinessException(
                    "Descriptor facial inválido: se requieren " + MemberFaceEmbedding.DESCRIPTOR_LENGTH + " valores");
        }
        double[] values = new double[MemberFaceEmbedding.DESCRIPTOR_LENGTH];
        for (int i = 0; i < raw.size(); i++) {
            values[i] = raw.get(i);
        }
        return values;
    }

    public String serialize(double[] descriptor) {
        try {
            Double[] boxed = new Double[descriptor.length];
            for (int i = 0; i < descriptor.length; i++) {
                boxed[i] = descriptor[i];
            }
            return objectMapper.writeValueAsString(boxed);
        } catch (Exception ex) {
            throw new BusinessException("No se pudo guardar el descriptor facial");
        }
    }

    public double[] parseStored(String json) {
        try {
            List<Double> list = objectMapper.readValue(json, DESCRIPTOR_TYPE);
            return parseDescriptor(list);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("Descriptor facial almacenado corrupto");
        }
    }

    /** Distancia euclidiana (face-api: menor = más parecido). */
    public double euclideanDistance(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            double d = a[i] - b[i];
            sum += d * d;
        }
        return Math.sqrt(sum);
    }
}
