package kg.azem.portfolio.applications;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateApplicationRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 32)
        @Pattern(regexp = "\\+?[0-9][0-9\\s().-]{6,30}", message = "Укажите корректный номер телефона")
        String phone
) {
}
