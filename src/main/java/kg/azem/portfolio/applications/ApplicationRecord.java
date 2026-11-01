package kg.azem.portfolio.applications;

import java.time.Instant;

public record ApplicationRecord(
        long id,
        String name,
        String phone,
        String description,
        ApplicationStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
