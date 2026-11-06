package kg.azem.portfolio.birthpinger;

import java.time.LocalDate;

public record BirthdayPerson(long id, String fullName, String phone, LocalDate birthDate,
                             String description, boolean active) {
}
