package kg.azem.portfolio.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record AdminMediaUpdateRequest(
        @Size(max = 300) String altText,
        @Size(max = 1000) String posterUrl,
        @Min(0) int sortOrder
) {
}
