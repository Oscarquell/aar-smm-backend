package kg.azem.portfolio.applications;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ApplicationService {
    private final ApplicationRepository repository;

    public ApplicationService(ApplicationRepository repository) {
        this.repository = repository;
    }

    public CreatedApplicationResponse create(CreateApplicationRequest request) {
        return repository.create(request);
    }

    public List<ApplicationRecord> getAll() {
        return repository.findAll();
    }

    public ApplicationRecord update(long id, UpdateApplicationRequest request) {
        if (request.status() == null && request.description() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите статус или описание для изменения");
        }
        return repository.update(id, request)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Заявка не найдена"));
    }
}
