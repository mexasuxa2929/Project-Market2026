package mexa.club.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;

public record TemplateUpdateRequest(
        @NotBlank String subject,
        @NotBlank String bodyTemplate,
        boolean active
) {}
