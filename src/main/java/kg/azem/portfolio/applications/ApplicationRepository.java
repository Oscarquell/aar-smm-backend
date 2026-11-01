package kg.azem.portfolio.applications;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ApplicationRepository {
    private final JdbcTemplate jdbcTemplate;

    public ApplicationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public CreatedApplicationResponse create(CreateApplicationRequest request) {
        return jdbcTemplate.queryForObject("""
                        INSERT INTO applications (name, phone)
                        VALUES (?, ?)
                        RETURNING id, status
                        """,
                (rs, rowNum) -> new CreatedApplicationResponse(
                        rs.getLong("id"), ApplicationStatus.valueOf(rs.getString("status"))
                ),
                request.name().trim(), request.phone().trim());
    }

    public List<ApplicationRecord> findAll() {
        return jdbcTemplate.query("""
                SELECT id, name, phone, description, status, created_at, updated_at
                FROM applications
                ORDER BY created_at DESC, id DESC
                LIMIT 500
                """, applicationRowMapper());
    }

    public Optional<ApplicationRecord> update(long id, UpdateApplicationRequest request) {
        List<ApplicationRecord> updated = jdbcTemplate.query("""
                        UPDATE applications
                        SET status = COALESCE(?, status),
                            description = COALESCE(?, description),
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        RETURNING id, name, phone, description, status, created_at, updated_at
                        """,
                applicationRowMapper(),
                request.status() == null ? null : request.status().name(),
                request.description(), id);
        return updated.stream().findFirst();
    }

    private RowMapper<ApplicationRecord> applicationRowMapper() {
        return (rs, rowNum) -> new ApplicationRecord(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("phone"),
                rs.getString("description"),
                ApplicationStatus.valueOf(rs.getString("status")),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
