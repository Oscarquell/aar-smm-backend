package kg.azem.portfolio.portfolio;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import kg.azem.portfolio.admin.AdminMediaUpdateRequest;
import kg.azem.portfolio.admin.AdminPortfolioRequest;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class PortfolioRepository {
    private final JdbcTemplate jdbcTemplate;

    public PortfolioRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PortfolioItem> findAllPublished() {
        return findItems("where published = true order by sort_order, id", new Object[]{});
    }

    public List<PortfolioItem> findHomeFeatured() {
        return findItems(
                "where published = true and featured_on_home = true order by sort_order, id limit 2",
                new Object[]{}
        );
    }

    public List<PortfolioItem> findAllForAdmin() {
        return findItems("order by sort_order, id");
    }

    public Optional<PortfolioItem> findForAdmin(long id) {
        return findItems("where id = ?", id).stream().findFirst();
    }

    public int countFeaturedOnHomeExcept(long id) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from portfolio_items where featured_on_home = true and id <> ?",
                Integer.class,
                id
        );
        return count == null ? 0 : count;
    }

    public long create(AdminPortfolioRequest request) {
        return jdbcTemplate.execute((ConnectionCallback<Long>) connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    insert into portfolio_items (
                        slug, type, title, category, description, service_tags, external_url,
                        featured_on_home, published, sort_order
                    ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) returning id
                    """)) {
                setItemParameters(statement, connection, request, false, 0);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) throw new SQLException("Insert did not return an id");
                    return result.getLong(1);
                }
            }
        });
    }

    public boolean update(long id, AdminPortfolioRequest request) {
        Integer count = jdbcTemplate.execute((ConnectionCallback<Integer>) connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    update portfolio_items set
                        slug = ?, type = ?, title = ?, category = ?, description = ?, service_tags = ?, external_url = ?,
                        featured_on_home = ?, published = ?, sort_order = ?, updated_at = now()
                    where id = ?
                    """)) {
                setItemParameters(statement, connection, request, true, id);
                return statement.executeUpdate();
            }
        });
        return count != null && count > 0;
    }

    private void setItemParameters(
            PreparedStatement statement,
            Connection connection,
            AdminPortfolioRequest request,
            boolean update,
            long id
    ) throws SQLException {
        statement.setString(1, request.slug());
        statement.setString(2, request.type());
        statement.setString(3, request.title().trim());
        statement.setString(4, request.category().trim());
        statement.setString(5, request.description().trim());
        statement.setArray(6, connection.createArrayOf("text", request.serviceTags().toArray(String[]::new)));
        statement.setString(7, request.externalUrl());
        statement.setBoolean(8, request.featuredOnHome());
        statement.setBoolean(9, request.published());
        statement.setInt(10, request.sortOrder());
        if (update) statement.setLong(11, id);
    }

    public List<String> deleteItem(long id) {
        List<String> mediaUrls = jdbcTemplate.query(
                "select url from portfolio_media where portfolio_item_id = ?",
                (rs, rowNum) -> rs.getString("url"), id
        );
        jdbcTemplate.update("delete from portfolio_items where id = ?", id);
        return mediaUrls;
    }

    public PortfolioMedia insertMedia(long itemId, String type, String url, String altText, String posterUrl, int sortOrder) {
        Long id = jdbcTemplate.queryForObject("""
                        insert into portfolio_media (portfolio_item_id, media_type, url, alt_text, poster_url, sort_order)
                        values (?, ?, ?, ?, ?, ?) returning id
                        """,
                Long.class, itemId, type, url, altText, posterUrl, sortOrder);
        return new PortfolioMedia(id, type, url, altText, posterUrl, sortOrder);
    }

    public Optional<PortfolioMedia> findMediaForItem(long itemId, long mediaId) {
        return jdbcTemplate.query("""
                        select id, media_type, url, alt_text, poster_url, sort_order
                        from portfolio_media where portfolio_item_id = ? and id = ?
                        """,
                (rs, rowNum) -> new PortfolioMedia(
                        rs.getLong("id"), rs.getString("media_type"), rs.getString("url"),
                        rs.getString("alt_text"), rs.getString("poster_url"), rs.getInt("sort_order")
                ), itemId, mediaId).stream().findFirst();
    }

    public boolean updateMedia(long itemId, long mediaId, AdminMediaUpdateRequest request) {
        return jdbcTemplate.update("""
                        update portfolio_media set alt_text = ?, poster_url = ?, sort_order = ?
                        where portfolio_item_id = ? and id = ?
                        """,
                request.altText() == null ? "" : request.altText().trim(),
                request.posterUrl() == null || request.posterUrl().isBlank() ? null : request.posterUrl().trim(),
                request.sortOrder(), itemId, mediaId) > 0;
    }

    public Optional<String> deleteMedia(long itemId, long mediaId) {
        Optional<String> url = jdbcTemplate.query("""
                        select url from portfolio_media where portfolio_item_id = ? and id = ?
                        """,
                (rs, rowNum) -> rs.getString("url"), itemId, mediaId).stream().findFirst();
        if (url.isPresent()) {
            jdbcTemplate.update("delete from portfolio_media where portfolio_item_id = ? and id = ?", itemId, mediaId);
        }
        return url;
    }

    private List<PortfolioItem> findItems(String filter, Object... arguments) {
        String sql = """
                select id, slug, type, title, category, description, service_tags,
                       external_url, featured_on_home, published, sort_order
                from portfolio_items
                """ + filter;

        return jdbcTemplate.query(sql, itemRowMapper(), arguments);
    }

    private RowMapper<PortfolioItem> itemRowMapper() {
        return (rs, rowNum) -> new PortfolioItem(
                rs.getLong("id"),
                rs.getString("slug"),
                rs.getString("type"),
                rs.getString("title"),
                rs.getString("category"),
                rs.getString("description"),
                rs.getString("external_url"),
                readTags(rs),
                rs.getBoolean("featured_on_home"),
                rs.getBoolean("published"),
                rs.getInt("sort_order"),
                findMedia(rs.getLong("id"))
        );
    }

    private List<String> readTags(ResultSet rs) throws SQLException {
        Array tags = rs.getArray("service_tags");
        if (tags == null) {
            return List.of();
        }
        Object values = tags.getArray();
        return Arrays.asList((String[]) values);
    }

    private List<PortfolioMedia> findMedia(long itemId) {
        return jdbcTemplate.query("""
                        select id, media_type, url, alt_text, poster_url, sort_order
                        from portfolio_media
                        where portfolio_item_id = ?
                        order by sort_order, id
                        """,
                (rs, rowNum) -> new PortfolioMedia(
                        rs.getLong("id"),
                        rs.getString("media_type"),
                        rs.getString("url"),
                        rs.getString("alt_text"),
                        rs.getString("poster_url"),
                        rs.getInt("sort_order")
                ), itemId);
    }
}
