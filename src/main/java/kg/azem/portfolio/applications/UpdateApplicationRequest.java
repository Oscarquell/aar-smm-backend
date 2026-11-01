package kg.azem.portfolio.applications;

import jakarta.validation.constraints.Size;

public record UpdateApplicationRequest(
        ApplicationStatus status,
        @Size(max = 5000) String description
) {
}
