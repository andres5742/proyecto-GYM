package com.gym.management.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import java.io.IOException;
import java.math.BigDecimal;

/** Serializa montos en pesos colombianos como enteros (sin decimales). */
public class PesosJsonSerializer extends JsonSerializer<BigDecimal> {

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, com.fasterxml.jackson.databind.SerializerProvider serializers)
            throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        gen.writeNumber(MoneyUtil.roundPesos(value).longValue());
    }
}
