package kg.azem.portfolio.portfolio;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portfolio")
public class PortfolioController {
    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public List<PortfolioItem> getAll() {
        return portfolioService.getAllPublished();
    }

    @GetMapping("/home")
    public List<PortfolioItem> getHomeFeatured() {
        return portfolioService.getHomeFeatured();
    }
}
