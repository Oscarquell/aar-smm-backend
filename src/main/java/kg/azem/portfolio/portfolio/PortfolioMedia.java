package kg.azem.portfolio.portfolio;

public record PortfolioMedia(
        Long id,
        String type,
        String url,
        String altText,
        String posterUrl,
        int sortOrder
) {
}
