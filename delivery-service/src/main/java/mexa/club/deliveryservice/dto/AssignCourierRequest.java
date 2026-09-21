package mexa.club.deliveryservice.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignCourierRequest(@NotNull UUID courierId) {}
