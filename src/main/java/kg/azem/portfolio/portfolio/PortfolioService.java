package kg.azem.portfolio.portfolio;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PortfolioService {
    private final PortfolioRepository repository;

    public PortfolioService(PortfolioRepository repository) {
        this.repository = repository;
    }

    public List<PortfolioItem> getAllPublished() {
        return repository.findAllPublished();
    }

    public List<PortfolioItem> getHomeFeatured() {
        return repository.findHomeFeatured();
    }
}
