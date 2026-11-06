package kg.azem.portfolio.birthpinger;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BirthdayService {
    private final BirthdayRepository repository;
    public BirthdayService(BirthdayRepository repository) { this.repository = repository; }
    public List<BirthdayPerson> findAll() { return repository.findAll(); }
    public BirthdayPerson findById(long id) {
        return repository.findById(id, true)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Запись не найдена"));
    }
    public BirthdayPerson create(BirthdayRequest request) { return repository.save(request, null); }
    public BirthdayPerson update(long id, BirthdayRequest request) {
        if (repository.findById(id).isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Запись не найдена");
        return repository.save(request, id);
    }
    public void delete(long id) {
        if (repository.findById(id).isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Запись не найдена");
        repository.delete(id);
    }
    public int importRows(List<BirthdayImportRow> rows) { return repository.importRows(rows); }
}
