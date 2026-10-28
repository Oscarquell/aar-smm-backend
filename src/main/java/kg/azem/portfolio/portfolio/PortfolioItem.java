package kg.azem.portfolio.portfolio;

import java.util.List;

public record PortfolioItem(
        Long id,
        String slug,
        String type,
        String title,
        String category,
        String description,
        List<String> serviceTags,
        boolean featuredOnHome,
        boolean published,
        int sortOrder,
        List<PortfolioMedia> media
) {
}
