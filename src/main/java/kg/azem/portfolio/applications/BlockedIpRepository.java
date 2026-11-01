package kg.azem.portfolio.applications;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BlockedIpRepository {
    private final JdbcTemplate jdbcTemplate;

    public BlockedIpRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isBlocked(String ip) {
        Boolean blocked = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM blocked_ip
                    WHERE ip = CAST(? AS inet) AND deleted = FALSE
                )
                """, Boolean.class, ip);
        return Boolean.TRUE.equals(blocked);
    }

    public void block(String ip) {
        jdbcTemplate.update("""
                INSERT INTO blocked_ip (ip)
                VALUES (CAST(? AS inet))
                ON CONFLICT (ip) WHERE deleted = FALSE DO NOTHING
                """, ip);
    }

    public int releaseExpiredBlocks() {
        return jdbcTemplate.update("""
                UPDATE blocked_ip
                SET deleted = TRUE
                WHERE deleted = FALSE
                  AND created_at <= CURRENT_TIMESTAMP - INTERVAL '1 hour'
                """);
    }
}
