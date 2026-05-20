package com.gym.management.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record FaceWebcamVerifyRequest(@NotNull @Size(min = 128, max = 128) List<Double> descriptor) {}
