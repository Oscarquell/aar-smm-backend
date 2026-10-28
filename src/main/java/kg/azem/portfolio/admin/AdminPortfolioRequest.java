package kg.azem.portfolio.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AdminPortfolioRequest(
        @NotBlank @Pattern(regexp = "PROJECT|CASE_STUDY") String type,
        @NotBlank @Size(max = 120) @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") String slug,
        @NotBlank @Size(max = 180) String title,
        @NotBlank @Size(max = 180) String category,
        @NotBlank @Size(max = 5000) String description,
        @NotEmpty @Size(max = 12) List<@NotBlank @Size(max = 60) String> serviceTags,
        boolean featuredOnHome,
        boolean published,
        @Min(0) int sortOrder
) {
}
