package kg.azem.portfolio.birthpinger;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

public record BirthdayImportRow(@NotBlank @Size(max = 200) String name,
                                @Size(max = 32) @Pattern(regexp = "^$|\\+?[0-9][0-9\\s().-]{6,30}") String phone,
                                @NotNull @PastOrPresent LocalDate birthday,
                                @Size(max = 10000) String description) {
}
