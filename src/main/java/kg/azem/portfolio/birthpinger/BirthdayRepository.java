package kg.azem.portfolio.birthpinger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class BirthdayRepository {
    private final JdbcTemplate jdbc;
    private final RowMapper<BirthdayPerson> mapper = this::map;

    public BirthdayRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<BirthdayPerson> findAll() {
        return jdbc.query("SELECT id, full_name, phone, birth_date, description, active FROM birthday_people ORDER BY full_name", mapper);
    }

    public List<BirthdayPerson> findBirthdays(LocalDate date) {
        return jdbc.query("""
                SELECT id, full_name, phone, birth_date, description, active FROM birthday_people
                WHERE active = TRUE AND (
                    (EXTRACT(MONTH FROM birth_date) = ? AND EXTRACT(DAY FROM birth_date) = ?)
                    OR (? = 3 AND ? = 1 AND EXTRACT(MONTH FROM birth_date) = 2 AND EXTRACT(DAY FROM birth_date) = 29)
                )
                ORDER BY id
                """, mapper, date.getMonthValue(), date.getDayOfMonth(), date.getMonthValue(), date.getDayOfMonth());
    }

    public Optional<BirthdayPerson> findById(long id) {
        return jdbc.query("SELECT id, full_name, phone, birth_date, description, active FROM birthday_people WHERE id = ?", mapper, id).stream().findFirst();
    }

    public Optional<BirthdayPerson> findById(long id, boolean includeInactive) {
        return jdbc.query("SELECT id, full_name, phone, birth_date, description, active FROM birthday_people WHERE id = ? AND (? = TRUE OR active = TRUE)", mapper, id, includeInactive).stream().findFirst();
    }

    public BirthdayPerson save(BirthdayRequest request, Long id) {
        if (id == null) {
            return jdbc.queryForObject("""
                            INSERT INTO birthday_people (full_name, phone, birth_date, description, active)
                            VALUES (?, ?, ?, ?, ?) RETURNING id, full_name, phone, birth_date, description, active
                            """, mapper, request.fullName().trim(), blankToNull(request.phone()), request.birthDate(),
                    value(request.description()), request.active() == null || request.active());
        }
        return jdbc.queryForObject("""
                        UPDATE birthday_people SET full_name = ?, phone = ?, birth_date = ?, description = ?,
                            active = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?
                        RETURNING id, full_name, phone, birth_date, description, active
                        """, mapper, request.fullName().trim(), blankToNull(request.phone()), request.birthDate(),
                value(request.description()), request.active() == null || request.active(), id);
    }

    public int importRows(List<BirthdayImportRow> rows) {
        int inserted = 0;
        for (BirthdayImportRow row : rows) {
            inserted += jdbc.update("""
                    INSERT INTO birthday_people (full_name, phone, birth_date, description)
                    SELECT ?, ?, ?, ? WHERE NOT EXISTS (
                        SELECT 1 FROM birthday_people WHERE full_name = ? AND birth_date = ?
                    ) ON CONFLICT DO NOTHING
                    """, row.name().trim(), blankToNull(row.phone()), row.birthday(), value(row.description()), row.name().trim(), row.birthday());
        }
        return inserted;
    }

    public void delete(long id) {
        jdbc.update("UPDATE birthday_people SET active = FALSE, updated_at = CURRENT_TIMESTAMP WHERE id = ?", id);
    }

    public boolean claim(long personId, int year) {
        return claim(personId, year, false);
    }

    public boolean claimForResend(long personId, int year) {
        return claim(personId, year, true);
    }

    private boolean claim(long personId, int year, boolean resend) {
        return !jdbc.query("""
                INSERT INTO birthday_message_history (birthday_person_id, birthday_year, status)
                VALUES (?, ?, 'PROCESSING')
                ON CONFLICT (birthday_person_id, birthday_year) DO UPDATE
                  SET status = 'PROCESSING', error_message = NULL, generated_text = NULL,
                      sent_at = NULL, created_at = CURRENT_TIMESTAMP
                  WHERE birthday_message_history.status = 'FAILED'
                     OR (birthday_message_history.status = 'PROCESSING'
                         AND birthday_message_history.created_at < CURRENT_TIMESTAMP - INTERVAL '15 minutes')
                     OR (? = TRUE AND birthday_message_history.status = 'SENT')
                RETURNING id
                """, (rs, rowNum) -> rs.getLong(1), personId, year, resend).isEmpty();
    }

    public void markSent(long personId, int year, String text) {
        jdbc.update("UPDATE birthday_message_history SET status = 'SENT', generated_text = ?, sent_at = CURRENT_TIMESTAMP, error_message = NULL WHERE birthday_person_id = ? AND birthday_year = ?", text, personId, year);
    }

    public void markFailed(long personId, int year, String error) {
        jdbc.update("UPDATE birthday_message_history SET status = 'FAILED', error_message = ? WHERE birthday_person_id = ? AND birthday_year = ?", error == null ? "Unknown error" : error.substring(0, Math.min(1000, error.length())), personId, year);
    }

    private BirthdayPerson map(ResultSet rs, int row) throws SQLException {
        return new BirthdayPerson(rs.getLong("id"), rs.getString("full_name"), rs.getString("phone"), rs.getObject("birth_date", LocalDate.class), rs.getString("description"), rs.getBoolean("active"));
    }

    private String value(String text) {
        return text == null ? "" : text.trim();
    }

    private String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
