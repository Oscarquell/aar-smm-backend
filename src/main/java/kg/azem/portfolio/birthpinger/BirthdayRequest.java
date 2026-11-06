package kg.azem.portfolio.birthpinger;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record BirthdayRequest(
        @NotBlank @Size(max = 200) String fullName,
        @Size(max = 32) @Pattern(regexp = "^$|\\+?[0-9][0-9\\s().-]{6,30}") String phone,
        @NotNull @PastOrPresent LocalDate birthDate,
        @Size(max = 10000) String description,
        Boolean active
) {
}
